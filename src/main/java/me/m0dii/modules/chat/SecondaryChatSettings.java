package me.m0dii.modules.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SecondaryChatSettings {
    public enum InterceptMode {
        COPY,
        MOVE
    }

    public static final class Data {
        public boolean enabled = false;
        public InterceptMode interceptMode = InterceptMode.COPY;
        public int maxLines = 100;
        public boolean showOverlay = true;
        public boolean showWhileGuiOpen = true;
        public boolean routeOutgoing = false;
        public List<String> regexList = new ArrayList<>();
        public String outgoingRegex = "";
        public boolean fadeEnabled = true;
        public int fadeDurationMs = 30000;
        public int minAlpha = 32;
        public boolean resetTransparencyWhenHovered = true;
        public boolean noTransparencyWhenChatOpen = true;
    }

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-secondary-chat.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Data data = new Data();
    private static boolean loaded = false;

    private SecondaryChatSettings() {
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
            M0DevTools.LOGGER.error("Failed to load secondary chat settings: {}", e.getMessage());
            data = new Data();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(data)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save secondary chat settings: {}", e.getMessage());
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
        clean.enabled = raw.enabled;
        clean.interceptMode = raw.interceptMode == null ? InterceptMode.COPY : raw.interceptMode;
        clean.maxLines = Math.clamp(raw.maxLines, 10, 500);
        clean.showOverlay = raw.showOverlay;
        clean.showWhileGuiOpen = raw.showWhileGuiOpen;
        clean.routeOutgoing = raw.routeOutgoing;
        clean.regexList = new ArrayList<>();
        if (raw.regexList != null) {
            for (String regex : raw.regexList) {
                if (regex == null) {
                    continue;
                }
                String trimmed = regex.trim();
                if (!trimmed.isEmpty()) {
                    clean.regexList.add(trimmed);
                }
            }
        }
        clean.outgoingRegex = raw.outgoingRegex == null ? "" : raw.outgoingRegex;
        clean.fadeEnabled = raw.fadeEnabled;
        clean.fadeDurationMs = Math.clamp(raw.fadeDurationMs, 1000, 120000);
        clean.minAlpha = Math.clamp(raw.minAlpha, 0, 255);
        clean.resetTransparencyWhenHovered = raw.resetTransparencyWhenHovered;
        clean.noTransparencyWhenChatOpen = raw.noTransparencyWhenChatOpen;
        return clean;
    }
}

