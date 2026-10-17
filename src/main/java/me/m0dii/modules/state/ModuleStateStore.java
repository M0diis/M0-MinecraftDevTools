package me.m0dii.modules.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.clickgui.ModuleRegistry;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.utils.ConfigFileSafety;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remembers which modules are on, their settings, ClickHUD favourites and named profiles.
 * <ul>
 *     <li>Settings are applied at startup.</li>
 *     <li>On/off states are applied when you first join a world (most modules need a world to switch on).</li>
 *     <li>Modules that need server opt-in (reach, xray, instant break, freecam, radar) are never switched on
 *     automatically, and short-lived modes like freecam and zoom are not remembered.</li>
 * </ul>
 */
public final class ModuleStateStore {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-modules.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Modes that make no sense to restore after a restart. */
    private static final Set<String> TRANSIENT = Set.of("freecam", "zoom");

    private static ModuleStateData data = new ModuleStateData();
    private static boolean restoring;
    private static boolean restoredThisSession;
    private static boolean dirty;
    /** Nothing is saved before the file was read, so early toggles during start-up can't overwrite it. */
    private static boolean loaded;

    private ModuleStateStore() {
    }

    public static void register() {
        load();
        loaded = true;
        applySettings(data.settings);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!restoredThisSession) {
                restoredThisSession = true;
                applyEnabled(data.enabled);
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            captureAllSettings();
            saveIfDirty();
        });
    }

    // ---- hooks ---------------------------------------------------------------------------------------------

    /** Called by {@link Module#setEnabled(boolean)} after a state change. */
    public static void onModuleToggled(Module module) {
        if (restoring || !loaded || !isRemembered(module)) {
            return;
        }
        data.enabled.put(module.getId(), module.isEnabled());
        dirty = true;
        saveIfDirty();
    }

    /** Called after a module's settings changed (e.g. from ClickHUD). */
    public static void onSettingsChanged(Module module) {
        if (restoring || !loaded || !(module instanceof PersistentSettings persistent)) {
            return;
        }
        try {
            data.settings.put(module.getId(), persistent.saveSettings());
            dirty = true;
            saveIfDirty();
        } catch (RuntimeException e) {
            M0DevTools.LOGGER.warn("Could not save settings of {}: {}", module.getId(), e.getMessage());
        }
    }

    // ---- favourites ----------------------------------------------------------------------------------------

    public static boolean isFavorite(Module module) {
        return module != null && data.isFavorite(module.getId());
    }

    public static boolean toggleFavorite(Module module) {
        boolean result = data.toggleFavorite(module.getId());
        dirty = true;
        saveIfDirty();
        return result;
    }

    public static List<Module> favoriteModules() {
        List<Module> modules = new ArrayList<>();
        for (String id : data.favorites) {
            Module module = ModuleRegistry.findModuleById(id);
            if (module != null) {
                modules.add(module);
            }
        }
        return modules;
    }

    // ---- profiles ------------------------------------------------------------------------------------------

    public static List<String> profileNames() {
        return new ArrayList<>(data.profiles.keySet());
    }

    public static String activeProfile() {
        return data.activeProfile;
    }

    public static String saveProfile(String rawName) {
        String name = ModuleStateData.normalizeProfileName(rawName);
        if (name.isBlank()) {
            return "Give the profile a name.";
        }
        captureAllSettings();
        captureAllEnabled();
        data.saveProfile(name);
        dirty = true;
        saveIfDirty();
        return "Saved profile '" + name + "' (" + data.enabled.size() + " modules).";
    }

    public static String loadProfile(String rawName) {
        String name = ModuleStateData.normalizeProfileName(rawName);
        ModuleStateData.Profile profile = data.profiles.get(name);
        if (profile == null) {
            return "No profile named '" + name + "'.";
        }
        applySettings(profile.settings);
        int skipped = applyEnabled(profile.enabled);
        data.activeProfile = name;
        captureAllSettings();
        captureAllEnabled();
        dirty = true;
        saveIfDirty();
        return "Loaded profile '" + name + "'." + (skipped > 0
                ? " " + skipped + " module(s) need server opt-in and were left as they are."
                : "");
    }

    public static String cycleProfile() {
        String next = data.nextProfileName();
        if (next == null) {
            return "No profiles yet. Save one with /m0profile save <name>.";
        }
        return loadProfile(next);
    }

    public static String deleteProfile(String rawName) {
        String name = ModuleStateData.normalizeProfileName(rawName);
        if (data.profiles.remove(name) == null) {
            return "No profile named '" + name + "'.";
        }
        if (name.equals(data.activeProfile)) {
            data.activeProfile = null;
        }
        dirty = true;
        saveIfDirty();
        return "Deleted profile '" + name + "'.";
    }

    // ---- internals -----------------------------------------------------------------------------------------

    static boolean isRemembered(Module module) {
        return module != null
                && !module.requiresServerSideOptIn()
                && !(module instanceof FreecamModule)
                && !TRANSIENT.contains(module.getId());
    }

    private static void applySettings(Map<String, JsonObject> settings) {
        restoring = true;
        try {
            settings.forEach((id, json) -> {
                Module module = ModuleRegistry.findModuleById(id);
                if (module instanceof PersistentSettings persistent && json != null) {
                    try {
                        persistent.loadSettings(json);
                    } catch (RuntimeException e) {
                        M0DevTools.LOGGER.warn("Could not apply saved settings of {}: {}", id, e.getMessage());
                    }
                }
            });
        } finally {
            restoring = false;
        }
    }

    /** Applies on/off states; returns how many were skipped because they need server opt-in. */
    private static int applyEnabled(Map<String, Boolean> enabled) {
        int skipped = 0;
        restoring = true;
        try {
            for (Map.Entry<String, Boolean> entry : enabled.entrySet()) {
                Module module = ModuleRegistry.findModuleById(entry.getKey());
                if (module == null || entry.getValue() == null) {
                    continue;
                }
                if (!isRemembered(module)) {
                    if (entry.getValue() && !module.isEnabled()) {
                        skipped++;
                    }
                    continue;
                }
                if (module.isEnabled() != entry.getValue()) {
                    try {
                        module.setEnabled(entry.getValue());
                    } catch (RuntimeException e) {
                        M0DevTools.LOGGER.warn("Could not restore {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        } finally {
            restoring = false;
        }
        return skipped;
    }

    private static void captureAllSettings() {
        for (Module module : ModuleRegistry.allModules()) {
            if (module instanceof PersistentSettings persistent) {
                try {
                    data.settings.put(module.getId(), persistent.saveSettings());
                } catch (RuntimeException ignored) {
                    // keep the previously saved settings
                }
            }
        }
        dirty = true;
    }

    private static void captureAllEnabled() {
        for (Module module : ModuleRegistry.allModules()) {
            if (isRemembered(module)) {
                data.enabled.put(module.getId(), module.isEnabled());
            }
        }
        dirty = true;
    }

    private static void load() {
        if (!Files.exists(FILE)) {
            data = new ModuleStateData();
            return;
        }
        try {
            ModuleStateData loaded = GSON.fromJson(Files.readString(FILE), ModuleStateData.class);
            data = loaded == null ? new ModuleStateData() : loaded.normalized();
        } catch (IOException | RuntimeException e) {
            ConfigFileSafety.backupUnreadable(FILE, e);
            data = new ModuleStateData();
        }
    }

    private static void saveIfDirty() {
        if (!dirty || !loaded) {
            return;
        }
        try {
            ConfigFileSafety.writeAtomically(FILE, GSON.toJson(data));
            dirty = false;
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save module states: {}", e.getMessage());
        }
    }
}
