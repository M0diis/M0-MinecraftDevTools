package me.m0dii.modules.pickup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PickupFeedSettings {
    public enum Direction {
        UP,
        DOWN
    }

    public static final class Data {
        public int durationMs = 4500;
        public int maxLines = 12;
        public float iconScale = 1.0f;
        public Direction direction = Direction.UP;
    }

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-pickup-feed.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Data data = new Data();
    private static boolean loaded = false;

    private PickupFeedSettings() {
    }

    public static synchronized @NotNull Data get() {
        ensureLoaded();
        data = sanitize(data);
        return data;
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
            data = new Data();
            save();
            return;
        }

        try {
            Data loadedData = GSON.fromJson(Files.readString(CONFIG_FILE), Data.class);
            data = sanitize(loadedData == null ? new Data() : loadedData);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load pickup feed settings: {}", e.getMessage());
            data = new Data();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(data)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save pickup feed settings: {}", e.getMessage());
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static Data sanitize(Data raw) {
        Data clean = new Data();
        if (raw == null) {
            return clean;
        }
        clean.durationMs = Math.clamp(raw.durationMs, 1000, 15000);
        clean.maxLines = Math.clamp(raw.maxLines, 1, 40);
        clean.iconScale = Math.clamp(raw.iconScale, 0.5f, 2.5f);
        clean.direction = raw.direction == null ? Direction.UP : raw.direction;
        return clean;
    }
}

