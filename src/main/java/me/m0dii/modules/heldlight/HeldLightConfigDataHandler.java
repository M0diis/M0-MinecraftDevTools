package me.m0dii.modules.heldlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HeldLightConfigDataHandler {
    public static final class Config {
        public int brightness = 14;
        public double falloffDistance = 9.0;
        public List<String> itemWhitelist = new ArrayList<>(List.of(
                "minecraft:torch",
                "minecraft:lantern",
                "minecraft:soul_torch",
                "minecraft:glowstone",
                "minecraft:sea_lantern",
                "minecraft:jack_o_lantern"
        ));
        public List<String> tagWhitelist = new ArrayList<>(List.of("minecraft:torches"));
    }

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-held-light.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config config = new Config();
    private static boolean loaded = false;

    private HeldLightConfigDataHandler() {
    }

    public static synchronized Config get() {
        ensureLoaded();
        config = sanitize(config);
        return config;
    }

    public static synchronized void updateAndSave(Runnable update) {
        ensureLoaded();
        update.run();
        config = sanitize(config);
        save();
    }

    public static synchronized void load() {
        loaded = true;
        if (!Files.exists(CONFIG_FILE)) {
            config = new Config();
            save();
            return;
        }
        try {
            Config loadedConfig = GSON.fromJson(Files.readString(CONFIG_FILE), Config.class);
            config = sanitize(loadedConfig == null ? new Config() : loadedConfig);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load held-light config: {}", e.getMessage());
            config = new Config();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(config)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save held-light config: {}", e.getMessage());
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static Config sanitize(Config raw) {
        Config clean = new Config();
        if (raw == null) {
            return clean;
        }
        clean.brightness = Math.clamp(raw.brightness, 1, 15);
        clean.falloffDistance = Math.clamp(raw.falloffDistance, 1.0, 32.0);
        clean.itemWhitelist = sanitizeList(raw.itemWhitelist);
        clean.tagWhitelist = sanitizeList(raw.tagWhitelist);
        return clean;
    }

    private static List<String> sanitizeList(List<String> input) {
        List<String> out = new ArrayList<>();
        if (input == null) {
            return out;
        }
        for (String value : input) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }
}

