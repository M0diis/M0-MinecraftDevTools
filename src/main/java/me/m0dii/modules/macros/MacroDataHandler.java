package me.m0dii.modules.macros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import me.m0dii.M0DevTools;
import me.m0dii.utils.LegacyKeyCodes;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MacroDataHandler {

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macros.json");
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /**
     * 2 = key codes are SDL scancodes (Minecraft 26.x). Files without a version are the legacy bare map
     * written by 1.21.x builds, whose key codes are GLFW codes.
     */
    static final int FORMAT_VERSION = 2;
    private static final TypeToken<Map<String, MacroEntry>> MACRO_MAP_TYPE = new TypeToken<>() {
    };

    private static Map<String, MacroEntry> macros = new LinkedHashMap<>();

    @Builder
    public static class MacroEntry {
        public final String name;
        public final List<String> commands;
        public final int keyCode;
        public final String modifierKey;
        public final int delayTicks;
        public final boolean showInOverlay;
    }

    public static void loadMacros() {
        Path configFile = configFile();
        if (!Files.exists(configFile)) {
            saveMacros();
            return;
        }

        try {
            LoadResult result = parse(Files.readString(configFile));
            macros = result.macros();
            if (result.convertedLegacyKeyCodes()) {
                Path backup = configFile.resolveSibling(configFile.getFileName() + ".pre-26.bak");
                Files.copy(configFile, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                saveMacros();
                M0DevTools.LOGGER.info("Converted {} macro key binding(s) to the 26.x key format (backup: {})",
                        macros.size(), backup.getFileName());
            }
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load macros: {}", e.getMessage());
        }
    }

    record LoadResult(Map<String, MacroEntry> macros, boolean convertedLegacyKeyCodes) {
    }

    static LoadResult parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (root == null || !root.isJsonObject()) {
            return new LoadResult(new LinkedHashMap<>(), false);
        }
        JsonObject object = root.getAsJsonObject();
        if (object.has("formatVersion")) {
            Map<String, MacroEntry> loaded = GSON.fromJson(object.get("macros"), MACRO_MAP_TYPE.getType());
            return new LoadResult(loaded == null ? new LinkedHashMap<>() : new LinkedHashMap<>(loaded), false);
        }

        Map<String, MacroEntry> legacy = GSON.fromJson(object, MACRO_MAP_TYPE.getType());
        Map<String, MacroEntry> converted = new LinkedHashMap<>();
        if (legacy != null) {
            legacy.forEach((id, entry) -> converted.put(id, withKeyCode(entry, LegacyKeyCodes.fromGlfw(entry.keyCode))));
        }
        return new LoadResult(converted, true);
    }

    private static MacroEntry withKeyCode(MacroEntry entry, int keyCode) {
        return MacroEntry.builder()
                .name(entry.name)
                .commands(entry.commands)
                .keyCode(keyCode)
                .modifierKey(entry.modifierKey)
                .delayTicks(entry.delayTicks)
                .showInOverlay(entry.showInOverlay)
                .build();
    }

    public static void saveMacros() {
        try {
            Files.writeString(configFile(), serialize(macros));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save macros: {}", e.getMessage());
        }
    }

    static String serialize(Map<String, MacroEntry> entries) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", FORMAT_VERSION);
        root.add("macros", GSON.toJsonTree(entries, MACRO_MAP_TYPE.getType()));
        return GSON.toJson(root);
    }

    public static void addMacro(@NotNull String id,
                                @NotNull String name,
                                @Nullable List<String> commands,
                                int keyCode,
                                @Nullable String modifierKey,
                                int delayTicks,
                                boolean showInOverlay) {
        macros.put(id, MacroEntry.builder()
                .name(name)
                .commands(Optional.ofNullable(commands)
                        .orElse(new ArrayList<>()))
                .keyCode(keyCode)
                .modifierKey(modifierKey)
                .delayTicks(delayTicks)
                .showInOverlay(showInOverlay)
                .build());

        saveMacros();
    }

    public static void removeMacro(String id) {
        macros.remove(id);
        saveMacros();
    }

    public static MacroEntry getMacro(@NotNull String id) {
        return macros.get(id);
    }

    public static Map<String, MacroEntry> getAllMacros() {
        return new LinkedHashMap<>(macros);
    }

    public static boolean hasMacro(@NotNull String id) {
        return macros.containsKey(id);
    }

    public static void updateMacro(@NotNull String id,
                                   @NotNull String name,
                                   @Nullable List<String> commands,
                                   int keyCode,
                                   @Nullable String modifierKey,
                                   int delayTicks,
                                   boolean showInOverlay) {
        if (macros.containsKey(id)) {
            addMacro(id, name, commands, keyCode, modifierKey, delayTicks, showInOverlay);
        }
    }

    public static void setAllMacros(@Nullable Map<String, MacroEntry> next) {
        macros = next == null ? new LinkedHashMap<>() : new LinkedHashMap<>(next);
        saveMacros();
    }
}
