package me.m0dii.modules.macros.gui;

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
import java.util.UUID;

public final class MacroPanelDataHandler {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macro-panel.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static PanelConfig config = new PanelConfig();

    private MacroPanelDataHandler() {
    }

    public static final class PanelButton {
        public String id = shortId();
        public String label = "Button";
        public String macroId = "";
        public int x = 20;
        public int y = 40;
        public int width = 110;
        public int height = 20;
    }

    public static final class PanelConfig {
        public String title = "Macro Panel";
        public boolean enabled = true;
        public List<PanelButton> buttons = new ArrayList<>();
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            config = defaults();
            save();
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            PanelConfig loaded = GSON.fromJson(json, PanelConfig.class);
            config = loaded != null ? sanitize(loaded) : defaults();
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load macro panel config: {}", e.getMessage());
            config = defaults();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(config)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save macro panel config: {}", e.getMessage());
        }
    }

    public static @NotNull PanelConfig getConfig() {
        if (config == null) {
            config = defaults();
        }
        return sanitize(config);
    }

    public static void setConfig(@NotNull PanelConfig next) {
        config = sanitize(next);
        save();
    }

    public static @NotNull PanelButton createDefaultButton() {
        PanelButton button = new PanelButton();
        button.id = shortId();
        return button;
    }

    private static PanelConfig sanitize(@NotNull PanelConfig source) {
        PanelConfig cleaned = new PanelConfig();
        cleaned.title = source.title == null || source.title.isBlank() ? "Macro Panel" : source.title.trim();
        cleaned.enabled = source.enabled;

        if (source.buttons != null) {
            for (PanelButton raw : source.buttons) {
                if (raw == null) {
                    continue;
                }
                PanelButton b = new PanelButton();
                b.id = (raw.id == null || raw.id.isBlank()) ? shortId() : raw.id.trim();
                b.label = (raw.label == null || raw.label.isBlank()) ? "Button" : raw.label.trim();
                b.macroId = raw.macroId == null ? "" : raw.macroId.trim();
                b.width = Math.clamp(raw.width, 50, 240);
                b.height = Math.clamp(raw.height, 18, 40);
                b.x = Math.max(0, raw.x);
                b.y = Math.max(0, raw.y);
                cleaned.buttons.add(b);
            }
        }

        return cleaned;
    }

    private static PanelConfig defaults() {
        PanelConfig cfg = new PanelConfig();
        cfg.title = "Macro Panel";
        cfg.enabled = true;
        cfg.buttons = new ArrayList<>();
        return cfg;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

