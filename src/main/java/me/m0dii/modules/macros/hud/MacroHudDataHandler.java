package me.m0dii.modules.macros.hud;

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

public final class MacroHudDataHandler {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macro-hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HudConfig config = defaults();

    private MacroHudDataHandler() {
    }

    public enum ElementType {
        BUTTON,
        TEXT,
        MACRO_KEYBINDS
    }

    public enum VisibilityMode {
        ALWAYS,
        CHAT_ONLY
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

    public enum Anchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    public static final class HudElement {
        public String id = shortId();
        public ElementType type = ElementType.BUTTON;
        public String label = "Button";
        public String text = "Text";
        public String macroId = "";
        // Optional direct action for button elements (e.g. "/say hi", "msg:hello", "cmd:/spawn").
        public String buttonAction = "";
        // Position offsets relative to the selected anchor.
        public int x = 20;
        public int y = 60;
        public Anchor anchor = Anchor.TOP_LEFT;
        public int width = 110;
        public int height = 20;
        public int lineHeight = 9;
        public float fontScale = 1.0f;
        public int backgroundColor = 0xAA101010;
        public int borderColor = 0xFFFFFFFF;
        public int textColor = 0xFFFFFFFF;
        public boolean drawBackground = true;
        public boolean drawBorder = true;
        public HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;
        public VerticalAlign verticalAlign = VerticalAlign.CENTER;
        public VisibilityMode visibilityMode = VisibilityMode.ALWAYS;
        public boolean visible = true;
    }

    public static final class HudConfig {
        public boolean enabled = true;
        public List<HudElement> elements = new ArrayList<>();
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            config = defaults();
            save();
            return;
        }

        try {
            HudConfig loaded = GSON.fromJson(Files.readString(CONFIG_FILE), HudConfig.class);
            config = sanitize(loaded == null ? defaults() : loaded);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load macro HUD config: {}", e.getMessage());
            config = defaults();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(config)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save macro HUD config: {}", e.getMessage());
        }
    }

    public static @NotNull HudConfig getConfigCopy() {
        return deepCopy(sanitize(config));
    }

    public static void setConfig(@NotNull HudConfig next) {
        config = sanitize(next);
        save();
    }

    public static @NotNull HudElement createElement(ElementType type) {
        HudElement element = new HudElement();
        element.id = shortId();
        element.type = type;
        if (type == ElementType.TEXT) {
            element.width = 120;
            element.height = 12;
            element.lineHeight = 9;
            element.fontScale = 1.0f;
            element.backgroundColor = 0x00000000;
            element.borderColor = 0x00000000;
            element.drawBackground = false;
            element.drawBorder = false;
            element.horizontalAlign = HorizontalAlign.LEFT;
            element.verticalAlign = VerticalAlign.TOP;
        }
        if (type == ElementType.MACRO_KEYBINDS) {
            element.text = "Macro Keybinds";
            element.width = 220;
            element.height = 120;
            element.lineHeight = 9;
            element.fontScale = 1.0f;
            element.drawBackground = true;
            element.drawBorder = true;
            element.borderColor = 0xFFFFFFFF;
            element.horizontalAlign = HorizontalAlign.LEFT;
            element.verticalAlign = VerticalAlign.TOP;
        }
        return element;
    }

    private static HudConfig sanitize(@NotNull HudConfig source) {
        HudConfig cleaned = new HudConfig();
        cleaned.enabled = source.enabled;

        if (source.elements != null) {
            for (HudElement raw : source.elements) {
                if (raw == null) {
                    continue;
                }
                HudElement e = new HudElement();
                e.id = (raw.id == null || raw.id.isBlank()) ? shortId() : raw.id.trim();
                e.type = raw.type == null ? ElementType.BUTTON : raw.type;
                e.label = safe(raw.label, "Button");
                e.text = safe(raw.text, "Text");
                e.macroId = raw.macroId == null ? "" : raw.macroId.trim();
                e.buttonAction = raw.buttonAction == null ? "" : raw.buttonAction.trim();
                e.x = Math.clamp(raw.x, -10000, 10000);
                e.y = Math.clamp(raw.y, -10000, 10000);
                e.anchor = raw.anchor == null ? Anchor.TOP_LEFT : raw.anchor;
                // Keep sizes stable across saves; editor can intentionally use large panels.
                e.width = Math.clamp(raw.width, 40, 2000);
                e.height = Math.clamp(raw.height, 12, 1200);
                e.lineHeight = Math.clamp(raw.lineHeight, 6, 24);
                e.fontScale = Math.clamp(raw.fontScale, 0.5f, 4.0f);
                e.backgroundColor = raw.backgroundColor;
                e.borderColor = (raw.borderColor >>> 24) == 0 ? 0xFFFFFFFF : raw.borderColor;
                e.textColor = raw.textColor;
                e.drawBackground = raw.drawBackground;
                e.drawBorder = raw.drawBorder;
                e.horizontalAlign = raw.horizontalAlign == null ? HorizontalAlign.CENTER : raw.horizontalAlign;
                e.verticalAlign = raw.verticalAlign == null ? VerticalAlign.CENTER : raw.verticalAlign;
                e.visibilityMode = raw.visibilityMode == null ? VisibilityMode.ALWAYS : raw.visibilityMode;
                e.visible = raw.visible;

                // Legacy migration: remove the temporary helper text introduced in early builds.
                if (e.type == ElementType.TEXT && "Open chat to click HUD buttons".equalsIgnoreCase(e.text)) {
                    continue;
                }

                cleaned.elements.add(e);
            }
        }

        return cleaned;
    }

    private static HudConfig deepCopy(HudConfig source) {
        HudConfig copy = new HudConfig();
        copy.enabled = source.enabled;
        copy.elements = new ArrayList<>();
        for (HudElement e : source.elements) {
            HudElement cloned = new HudElement();
            cloned.id = e.id;
            cloned.type = e.type;
            cloned.label = e.label;
            cloned.text = e.text;
            cloned.macroId = e.macroId;
            cloned.buttonAction = e.buttonAction;
            cloned.x = e.x;
            cloned.y = e.y;
            cloned.anchor = e.anchor;
            cloned.width = e.width;
            cloned.height = e.height;
            cloned.lineHeight = e.lineHeight;
            cloned.fontScale = e.fontScale;
            cloned.backgroundColor = e.backgroundColor;
            cloned.borderColor = e.borderColor;
            cloned.textColor = e.textColor;
            cloned.drawBackground = e.drawBackground;
            cloned.drawBorder = e.drawBorder;
            cloned.horizontalAlign = e.horizontalAlign;
            cloned.verticalAlign = e.verticalAlign;
            cloned.visibilityMode = e.visibilityMode;
            cloned.visible = e.visible;
            copy.elements.add(cloned);
        }
        return copy;
    }

    private static HudConfig defaults() {
        HudConfig cfg = new HudConfig();
        cfg.enabled = true;
        cfg.elements = new ArrayList<>();


        HudElement button = createElement(ElementType.BUTTON);
        button.label = "Example Macro";
        button.macroId = "";
        button.x = 10;
        button.y = 26;
        cfg.elements.add(button);

        return cfg;
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String safe(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }
}

