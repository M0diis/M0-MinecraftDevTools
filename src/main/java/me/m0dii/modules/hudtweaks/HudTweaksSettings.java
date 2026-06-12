package me.m0dii.modules.hudtweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

public final class HudTweaksSettings {

    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 6.0f;

    public enum ElementType {
        ACTION_BAR,
        BOSS_BAR,
        CROSSHAIR,
        DEBUG_SCREEN,
        HOTBAR_GROUP,
        PLAYER_LIST,
        SCOREBOARD,
        SCREEN_TITLE,
        STATUS_EFFECT,
        SUBTITLES,
        TOAST,
        TOOLTIP
    }

    public static final class ElementConfig {
        public boolean display = true;
        public float scale = 1.0f;
        public float opacity = 1.0f;
        public int offsetX = 0;
        public int offsetY = 0;
    }

    public static final class Data {
        public boolean moduleEnabled = true;
        public EnumMap<ElementType, ElementConfig> elements = new EnumMap<>(ElementType.class);
    }

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-hud-tweaks.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Data data = defaults();
    private static boolean loaded = false;

    private HudTweaksSettings() {
    }

    public static synchronized @NotNull Data get() {
        ensureLoaded();
        data = sanitize(data);
        return data;
    }

    public static synchronized @NotNull ElementConfig getElement(@NotNull ElementType type) {
        ensureLoaded();
        data = sanitize(data);
        return data.elements.get(type);
    }

    public static synchronized void updateAndSave(@NotNull Runnable update) {
        ensureLoaded();
        update.run();
        data = sanitize(data);
        save();
    }

    public static synchronized void load() {
        loaded = true;
        if (!Files.exists(CONFIG_FILE)) {
            data = defaults();
            save();
            return;
        }

        try {
            Data loadedData = GSON.fromJson(Files.readString(CONFIG_FILE), Data.class);
            data = sanitize(loadedData == null ? defaults() : loadedData);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load HUD tweaks settings: {}", e.getMessage());
            data = defaults();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(data)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save HUD tweaks settings: {}", e.getMessage());
        }
    }

    public static synchronized void resetElement(@NotNull ElementType type) {
        updateAndSave(() -> get().elements.put(type, new ElementConfig()));
    }

    public static synchronized void resetAll() {
        updateAndSave(() -> data = defaults());
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static Data defaults() {
        Data defaults = new Data();
        for (ElementType type : ElementType.values()) {
            defaults.elements.put(type, new ElementConfig());
        }
        return defaults;
    }

    private static Data sanitize(Data raw) {
        Data clean = defaults();
        if (raw == null) {
            return clean;
        }

        clean.moduleEnabled = raw.moduleEnabled;
        if (raw.elements != null) {
            for (ElementType type : ElementType.values()) {
                ElementConfig source = raw.elements.get(type);
                ElementConfig target = clean.elements.get(type);
                if (source == null || target == null) {
                    continue;
                }
                target.display = source.display;
                target.scale = Math.clamp(source.scale, MIN_SCALE, MAX_SCALE);
                target.opacity = Math.clamp(source.opacity, 0.0f, 1.0f);
                target.offsetX = Math.clamp(source.offsetX, -500, 500);
                target.offsetY = Math.clamp(source.offsetY, -500, 500);
            }
        }
        return clean;
    }
}

