package me.m0dii.modules.hudcanvas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class HudCanvasDataHandler {

    public static final String ELEMENT_NBT_INSPECTOR = "nbt_inspector";
    public static final String ELEMENT_SECONDARY_CHAT = "secondary_chat";
    public static final String ELEMENT_MACRO_KEYBINDS = "macro_keybinds";
    public static final String ELEMENT_PICKUP_NOTIFIER = "pickup_notifier";

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-hud-canvas.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HudCanvasConfig config = new HudCanvasConfig();
    private static boolean loaded = false;

    private HudCanvasDataHandler() {
    }

    public static final class HudCanvasElement {
        public enum Anchor {
            TOP_LEFT,
            TOP_CENTER,
            TOP_RIGHT,
            MIDDLE_LEFT,
            MIDDLE_CENTER,
            MIDDLE_RIGHT,
            BOTTOM_LEFT,
            BOTTOM_CENTER,
            BOTTOM_RIGHT
        }

        public enum HorizontalAlign {
            LEFT,
            CENTER,
            RIGHT
        }

        public enum VerticalAlign {
            TOP,
            CENTER,
            BOTTOM
        }

        public int x = 8;
        public int y = 8;
        public int width = 240;
        public int height = 120;
        public int padding = 4;
        public int lineHeight = 9;
        public float fontScale = 1.0f;
        public int zIndex = 0;
        public int backgroundColor = 0x88000000;
        public int textColor = 0xFFE0E0E0;
        public int borderColor = 0xFFFFFFFF;
        public boolean drawBackground = true;
        public boolean drawBorder = false;
        public boolean visible = true;
        // Position offsets are interpreted relative to this anchor.
        public Anchor anchor = Anchor.TOP_LEFT;
        // Text alignment inside the panel bounds.
        public HorizontalAlign horizontalAlign = HorizontalAlign.LEFT;
        public VerticalAlign verticalAlign = VerticalAlign.TOP;
    }

    public static final class HudCanvasConfig {
        public Map<String, HudCanvasElement> elements = new LinkedHashMap<>();
    }

    public static synchronized void load() {
        loaded = true;
        if (!Files.exists(CONFIG_FILE)) {
            config = new HudCanvasConfig();
            save();
            return;
        }

        try {
            HudCanvasConfig loadedConfig = GSON.fromJson(Files.readString(CONFIG_FILE), HudCanvasConfig.class);
            config = sanitize(loadedConfig == null ? new HudCanvasConfig() : loadedConfig);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load HUD canvas config: {}", e.getMessage());
            config = new HudCanvasConfig();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(config)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save HUD canvas config: {}", e.getMessage());
        }
    }

    public static synchronized @NotNull HudCanvasElement getMutableElement(@NotNull String id,
                                                                           @NotNull Supplier<HudCanvasElement> defaultFactory) {
        ensureLoaded();
        HudCanvasElement existing = config.elements.get(id);
        if (existing != null) {
            HudCanvasElement cleaned = sanitizeElement(existing);
            config.elements.put(id, cleaned);
            return cleaned;
        }

        HudCanvasElement created = sanitizeElement(defaultFactory.get());
        config.elements.put(id, created);
        save();
        return created;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static HudCanvasConfig sanitize(HudCanvasConfig source) {
        HudCanvasConfig cleaned = new HudCanvasConfig();
        if (source.elements == null) {
            return cleaned;
        }
        for (Map.Entry<String, HudCanvasElement> entry : source.elements.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            cleaned.elements.put(entry.getKey().trim(), sanitizeElement(entry.getValue()));
        }
        return cleaned;
    }

    private static HudCanvasElement sanitizeElement(HudCanvasElement raw) {
        HudCanvasElement e = new HudCanvasElement();
        if (raw == null) {
            return e;
        }

        e.x = Math.clamp(raw.x, -10000, 10000);
        e.y = Math.clamp(raw.y, -10000, 10000);
        e.width = Math.clamp(raw.width, 40, 3000);
        e.height = Math.clamp(raw.height, 20, 3000);
        e.padding = Math.clamp(raw.padding, 0, 60);
        e.lineHeight = Math.clamp(raw.lineHeight, 6, 40);
        e.fontScale = Math.clamp(raw.fontScale, 0.25f, 5.0f);
        e.zIndex = Math.clamp(raw.zIndex, -9999, 9999);
        e.backgroundColor = raw.backgroundColor;
        e.textColor = raw.textColor;
        e.borderColor = (raw.borderColor >>> 24) == 0 ? 0xFFFFFFFF : raw.borderColor;
        e.drawBackground = raw.drawBackground;
        e.drawBorder = raw.drawBorder;
        e.visible = raw.visible;
        e.anchor = raw.anchor == null ? HudCanvasElement.Anchor.TOP_LEFT : raw.anchor;
        e.horizontalAlign = raw.horizontalAlign == null ? HudCanvasElement.HorizontalAlign.LEFT : raw.horizontalAlign;
        e.verticalAlign = raw.verticalAlign == null ? HudCanvasElement.VerticalAlign.TOP : raw.verticalAlign;
        return e;
    }
}

