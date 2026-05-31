package me.m0dii.modules.macros.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MacroHudDataHandler {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macro-hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HudConfig config = defaults();

    private MacroHudDataHandler() {
    }

    public enum ElementType {
        BUTTON,
        TEXT,
        MACRO_KEYBINDS,
        ICON,
        BAR,
        VALUE,
        LIST,
        SHAPE,
        STATE_BADGE
    }

    public enum VisibilityMode {
        ALWAYS,
        CHAT,
        INVENTORY,
        CONTAINER,
        CHEST,
        SCREEN
    }

    public enum ButtonExecutionMode {
        COMMAND,
        GROOVY_SCRIPT,
        KOTLIN_SCRIPT
    }

    public enum BorderMode {
        FULL,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
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
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
        MIDDLE_CENTER,
        // Legacy center anchor kept for old saved configs.
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
        public ButtonExecutionMode buttonExecutionMode = ButtonExecutionMode.COMMAND;
        public boolean runScriptsAsync = true;
        // Position offsets relative to the selected anchor.
        public int x = 20;
        public int y = 60;
        public Anchor anchor = Anchor.TOP_LEFT;
        public int width = 110;
        public int height = 20;
        public int lineHeight = 9;
        public float fontScale = 1.0f;
        public int backgroundColor = 0xAA101010;
        public int backgroundAlpha = 0xAA;
        public int borderColor = 0xFFFFFFFF;
        public int textColor = 0xFFFFFFFF;
        public boolean drawBackground = true;
        public boolean backgroundOpaque = false;
        public boolean drawBorder = true;
        public BorderMode borderMode = BorderMode.FULL;
        public HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;
        public VerticalAlign verticalAlign = VerticalAlign.CENTER;
        public VisibilityMode visibilityMode = VisibilityMode.ALWAYS;
        public String visibilityScreenType = "";
        public boolean visible = true;

        // Shared data source/config for advanced widget types.
        public String sourceToken = "";
        public String sourceTokenMax = "";
        public String prefix = "";
        public String suffix = "";
        public double minValue = 0.0;
        public double maxValue = 100.0;

        // BAR / VALUE coloring and thresholds.
        public int colorStart = 0xFF44FF44;
        public int colorEnd = 0xFFFF4444;
        public int colorWarn = 0xFFFFFF44;
        public int colorCrit = 0xFFFF4444;
        public double warnThreshold = 50.0;
        public double critThreshold = 20.0;
        public boolean segmented = false;
        public int segments = 10;

        // LIST behavior.
        public int maxLines = 6;
        public int listScroll = 0;

        // ICON behavior.
        public String iconKind = "item"; // item|block|entity|entity_model
        public String iconId = "minecraft:stone";
        public boolean iconShowCount = true;
        public boolean iconShowDurability = true;
        public boolean iconShowCooldown = true;
        public float modelZoom = 0.85f;
        public float modelYaw = 0.0f;
        public float modelPitch = 0.0f;
        public int modelOffsetX = 0;
        public int modelOffsetY = 0;
        public boolean modelAutoFit = true;
        public boolean modelFollowLook = true;

        // SHAPE behavior.
        public String shapeType = "rounded_rect"; // rect|rounded_rect|circle|line|triangle|cross|diamond
        public boolean shapeFilled = true;
        public int shapeRadius = 6;
        public int shapeThickness = 2;

        // STATE badge behavior.
        public String stateOnText = "ON";
        public String stateOffText = "OFF";
        public String stateTrueValues = "true,on,yes,1,enabled,active";
        public String stateFalseValues = "false,off,no,0,disabled,idle";
        public boolean stateShowValue = true;
    }

    public static final class HudConfig {
        public boolean enabled = true;
        public String activePresetId = "default";
        public Map<String, List<HudElement>> presetElements = new LinkedHashMap<>();
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

    public static String getActivePresetId() {
        return sanitize(config).activePresetId;
    }

    public static List<String> listPresetIds() {
        HudConfig cleaned = sanitize(config);
        return new ArrayList<>(cleaned.presetElements.keySet());
    }

    public static void setActivePresetId(String presetId) {
        HudConfig cleaned = sanitize(config);
        String id = sanitizePresetId(presetId, cleaned.activePresetId);
        if (!cleaned.presetElements.containsKey(id)) {
            return;
        }
        cleaned.activePresetId = id;
        cleaned.elements = deepCopyElements(cleaned.presetElements.get(id));
        config = cleaned;
        save();
    }

    public static void createPreset(String presetId, boolean copyActive) {
        HudConfig cleaned = sanitize(config);
        String id = sanitizePresetId(presetId, "preset_" + (cleaned.presetElements.size() + 1));
        if (cleaned.presetElements.containsKey(id)) {
            return;
        }
        List<HudElement> seed = copyActive
                ? deepCopyElements(cleaned.presetElements.getOrDefault(cleaned.activePresetId, List.of()))
                : new ArrayList<>();
        cleaned.presetElements.put(id, seed);
        cleaned.activePresetId = id;
        cleaned.elements = deepCopyElements(seed);
        config = cleaned;
        save();
    }

    public static void renamePreset(String fromId, String toId) {
        HudConfig cleaned = sanitize(config);
        if (cleaned.presetElements.size() <= 0) {
            return;
        }
        String from = sanitizePresetId(fromId, cleaned.activePresetId);
        if (!cleaned.presetElements.containsKey(from)) {
            return;
        }
        String target = sanitizePresetId(toId, from);
        if (from.equals(target) || cleaned.presetElements.containsKey(target)) {
            return;
        }
        Map<String, List<HudElement>> renamed = new LinkedHashMap<>();
        for (Map.Entry<String, List<HudElement>> e : cleaned.presetElements.entrySet()) {
            if (e.getKey().equals(from)) {
                renamed.put(target, deepCopyElements(e.getValue()));
            } else {
                renamed.put(e.getKey(), deepCopyElements(e.getValue()));
            }
        }
        cleaned.presetElements = renamed;
        if (cleaned.activePresetId.equals(from)) {
            cleaned.activePresetId = target;
        }
        cleaned.elements = deepCopyElements(cleaned.presetElements.get(cleaned.activePresetId));
        config = cleaned;
        save();
    }

    public static void deletePreset(String presetId) {
        HudConfig cleaned = sanitize(config);
        if (cleaned.presetElements.size() <= 1) {
            return;
        }
        String id = sanitizePresetId(presetId, cleaned.activePresetId);
        cleaned.presetElements.remove(id);
        if (!cleaned.presetElements.containsKey(cleaned.activePresetId)) {
            cleaned.activePresetId = cleaned.presetElements.keySet().stream().findFirst().orElse("default");
        }
        cleaned.elements = deepCopyElements(cleaned.presetElements.getOrDefault(cleaned.activePresetId, List.of()));
        config = cleaned;
        save();
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
        if (type == ElementType.ICON) {
            element.label = "Icon";
            element.width = 22;
            element.height = 30;
            element.drawBackground = true;
            element.drawBorder = true;
            element.backgroundColor = 0xAA101010;
            element.borderColor = 0xFFFFFFFF;
            element.iconKind = "item";
            element.iconId = "minecraft:stone";
        }
        if (type == ElementType.BAR) {
            element.label = "Bar";
            element.width = 150;
            element.height = 18;
            element.sourceToken = "hp";
            element.sourceTokenMax = "max_hp";
            element.minValue = 0.0;
            element.maxValue = 20.0;
            element.colorStart = 0xFF44FF44;
            element.colorEnd = 0xFFFF4444;
            element.segmented = false;
            element.segments = 10;
        }
        if (type == ElementType.VALUE) {
            element.label = "Value";
            element.width = 140;
            element.height = 24;
            element.sourceToken = "hp";
            element.prefix = "HP: ";
            element.warnThreshold = 10.0;
            element.critThreshold = 5.0;
            element.colorWarn = 0xFFFFFF55;
            element.colorCrit = 0xFFFF5555;
            element.textColor = 0xFF55FF55;
            element.fontScale = 1.5f;
        }
        if (type == ElementType.LIST) {
            element.label = "List";
            element.width = 220;
            element.height = 96;
            element.sourceToken = "players.nearby.5.with_distance";
            element.maxLines = 6;
            element.drawBackground = true;
            element.drawBorder = true;
            element.horizontalAlign = HorizontalAlign.LEFT;
            element.verticalAlign = VerticalAlign.TOP;
        }
        if (type == ElementType.SHAPE) {
            element.label = "Shape";
            element.width = 120;
            element.height = 40;
            element.shapeType = "rounded_rect";
            element.shapeFilled = true;
            element.shapeRadius = 8;
            element.shapeThickness = 2;
            element.drawBackground = true;
            element.drawBorder = true;
            element.backgroundColor = 0x88202020;
            element.borderColor = 0xFFFFFFFF;
            element.text = "";
        }
        if (type == ElementType.STATE_BADGE) {
            element.label = "State";
            element.width = 90;
            element.height = 18;
            element.sourceToken = "player.sprinting";
            element.stateOnText = "ON";
            element.stateOffText = "OFF";
            element.stateShowValue = true;
            element.colorStart = 0xFF3FBF3F;
            element.colorEnd = 0xFFBF3F3F;
            element.textColor = 0xFFFFFFFF;
            element.drawBackground = true;
            element.drawBorder = true;
        }
        element.backgroundAlpha = (element.backgroundColor >>> 24) & 0xFF;
        return element;
    }

    private static HudConfig sanitize(@NotNull HudConfig source) {
        HudConfig cleaned = new HudConfig();
        cleaned.enabled = source.enabled;

        cleaned.presetElements = new LinkedHashMap<>();
        if (source.presetElements != null) {
            for (Map.Entry<String, List<HudElement>> entry : source.presetElements.entrySet()) {
                String id = sanitizePresetId(entry.getKey(), "preset_" + (cleaned.presetElements.size() + 1));
                if (!cleaned.presetElements.containsKey(id)) {
                    cleaned.presetElements.put(id, sanitizeElements(entry.getValue()));
                }
            }
        }

        List<HudElement> legacyElements = sanitizeElements(source.elements);
        if (cleaned.presetElements.isEmpty()) {
            cleaned.presetElements.put("default", legacyElements);
        }

        cleaned.activePresetId = sanitizePresetId(source.activePresetId, "default");
        if (!cleaned.presetElements.containsKey(cleaned.activePresetId)) {
            cleaned.activePresetId = cleaned.presetElements.keySet().stream().findFirst().orElse("default");
        }
        cleaned.elements = deepCopyElements(cleaned.presetElements.getOrDefault(cleaned.activePresetId, List.of()));

        return cleaned;
    }

    private static List<HudElement> sanitizeElements(List<HudElement> rawElements) {
        List<HudElement> cleaned = new ArrayList<>();
        if (rawElements == null) {
            return cleaned;
        }
        for (HudElement raw : rawElements) {
            if (raw == null) {
                continue;
            }
            HudElement e = new HudElement();
            e.id = (raw.id == null || raw.id.isBlank()) ? shortId() : raw.id.trim();
            e.type = raw.type == null ? ElementType.BUTTON : raw.type;
            if (e.type == ElementType.ICON) {
                // Keep explicit empty labels for icon widgets; only null falls back to default.
                e.label = raw.label == null ? "Icon" : raw.label.trim();
            } else {
                e.label = safe(raw.label, "Button");
            }
            e.text = safe(raw.text, "Text");
            e.macroId = raw.macroId == null ? "" : raw.macroId.trim();
            e.buttonAction = raw.buttonAction == null ? "" : raw.buttonAction.trim();
            e.buttonExecutionMode = raw.buttonExecutionMode == null ? ButtonExecutionMode.COMMAND : raw.buttonExecutionMode;
            e.runScriptsAsync = raw.runScriptsAsync;
            if (e.buttonExecutionMode == ButtonExecutionMode.COMMAND && e.type == ElementType.BUTTON) {
                String actionLower = e.buttonAction.toLowerCase(Locale.ROOT);
                if (actionLower.startsWith("kotlin:") || actionLower.startsWith("kts:")) {
                    e.buttonExecutionMode = ButtonExecutionMode.KOTLIN_SCRIPT;
                } else if (actionLower.startsWith("groovy:")) {
                    e.buttonExecutionMode = ButtonExecutionMode.GROOVY_SCRIPT;
                }
            }
            e.x = Math.clamp(raw.x, -10000, 10000);
            e.y = Math.clamp(raw.y, -10000, 10000);
            e.anchor = raw.anchor == null ? Anchor.TOP_LEFT : raw.anchor;
            e.width = Math.clamp(raw.width, 1, 2000);
            e.height = Math.clamp(raw.height, 1, 1200);
            e.lineHeight = Math.clamp(raw.lineHeight, 6, 24);
            e.fontScale = Math.clamp(raw.fontScale, 0.5f, 4.0f);
            e.backgroundColor = raw.backgroundColor;
            int alphaFromColor = (raw.backgroundColor >>> 24) & 0xFF;
            e.backgroundAlpha = Math.clamp(raw.backgroundAlpha <= 0 ? alphaFromColor : raw.backgroundAlpha, 0, 255);
            e.borderColor = (raw.borderColor >>> 24) == 0 ? 0xFFFFFFFF : raw.borderColor;
            e.textColor = raw.textColor;
            e.drawBackground = raw.drawBackground;
            e.backgroundOpaque = raw.backgroundOpaque;
            e.drawBorder = raw.drawBorder;
            e.borderMode = raw.borderMode == null ? BorderMode.FULL : raw.borderMode;
            e.horizontalAlign = raw.horizontalAlign == null ? HorizontalAlign.CENTER : raw.horizontalAlign;
            e.verticalAlign = raw.verticalAlign == null ? VerticalAlign.CENTER : raw.verticalAlign;
            if (raw.visibilityMode == null || raw.visibilityMode == VisibilityMode.CHAT) {
                // Older configs used CHAT_ONLY semantics; keep that behavior.
                e.visibilityMode = raw.visibilityMode == null ? VisibilityMode.ALWAYS : VisibilityMode.CHAT;
            } else {
                e.visibilityMode = raw.visibilityMode;
            }
            e.visibilityScreenType = raw.visibilityScreenType == null ? "" : raw.visibilityScreenType.trim();
            e.visible = raw.visible;
            e.sourceToken = raw.sourceToken == null ? "" : raw.sourceToken.trim();
            e.sourceTokenMax = raw.sourceTokenMax == null ? "" : raw.sourceTokenMax.trim();
            e.prefix = raw.prefix == null ? "" : raw.prefix;
            e.suffix = raw.suffix == null ? "" : raw.suffix;
            e.minValue = Double.isFinite(raw.minValue) ? raw.minValue : 0.0;
            e.maxValue = Double.isFinite(raw.maxValue) ? raw.maxValue : 100.0;
            e.colorStart = raw.colorStart;
            e.colorEnd = raw.colorEnd;
            e.colorWarn = raw.colorWarn;
            e.colorCrit = raw.colorCrit;
            e.warnThreshold = Double.isFinite(raw.warnThreshold) ? raw.warnThreshold : 50.0;
            e.critThreshold = Double.isFinite(raw.critThreshold) ? raw.critThreshold : 20.0;
            e.segmented = raw.segmented;
            e.segments = Math.clamp(raw.segments, 1, 120);
            e.maxLines = Math.clamp(raw.maxLines, 1, 200);
            e.listScroll = Math.max(0, raw.listScroll);
            e.iconKind = safe(raw.iconKind, "item").toLowerCase();
            e.iconId = safe(raw.iconId, "minecraft:stone");
            if ("entity_model".equals(e.iconKind)
                    && (e.iconId.isBlank() || "minecraft:stone".equalsIgnoreCase(e.iconId))) {
                e.iconId = "minecraft:player";
            }
            e.iconShowCount = raw.iconShowCount;
            e.iconShowDurability = raw.iconShowDurability;
            e.iconShowCooldown = raw.iconShowCooldown;
            e.modelZoom = raw.modelZoom <= 0.0f ? 0.85f : Math.clamp(raw.modelZoom, 0.2f, 2.5f);
            e.modelYaw = Math.clamp(raw.modelYaw, -180.0f, 180.0f);
            e.modelPitch = Math.clamp(raw.modelPitch, -90.0f, 90.0f);
            e.modelOffsetX = Math.clamp(raw.modelOffsetX, -200, 200);
            e.modelOffsetY = Math.clamp(raw.modelOffsetY, -200, 200);
            e.modelAutoFit = raw.modelAutoFit || raw.modelZoom <= 0.0f;
            e.modelFollowLook = raw.modelFollowLook || raw.modelZoom <= 0.0f;
            e.shapeType = safe(raw.shapeType, "rounded_rect").toLowerCase();
            e.shapeFilled = raw.shapeFilled;
            e.shapeRadius = Math.clamp(raw.shapeRadius, 0, 64);
            e.shapeThickness = Math.clamp(raw.shapeThickness, 1, 24);
            e.stateOnText = safe(raw.stateOnText, "ON");
            e.stateOffText = safe(raw.stateOffText, "OFF");
            e.stateTrueValues = safe(raw.stateTrueValues, "true,on,yes,1,enabled,active");
            e.stateFalseValues = safe(raw.stateFalseValues, "false,off,no,0,disabled,idle");
            e.stateShowValue = raw.stateShowValue;
            if (e.type == ElementType.TEXT && "Open chat to click HUD buttons".equalsIgnoreCase(e.text)) {
                continue;
            }
            cleaned.add(e);
        }
        return cleaned;
    }

    private static String sanitizePresetId(String raw, String fallback) {
        String base = raw == null ? "" : raw.trim();
        if (base.isBlank()) {
            base = fallback == null || fallback.isBlank() ? "preset" : fallback.trim();
        }
        base = base.replaceAll("[^a-zA-Z0-9 _.-]", "_").trim();
        if (base.isBlank()) {
            base = "preset";
        }
        return base;
    }

    private static HudConfig deepCopy(HudConfig source) {
        HudConfig copy = new HudConfig();
        copy.enabled = source.enabled;
        copy.activePresetId = source.activePresetId;
        copy.presetElements = new LinkedHashMap<>();
        for (Map.Entry<String, List<HudElement>> entry : source.presetElements.entrySet()) {
            copy.presetElements.put(entry.getKey(), deepCopyElements(entry.getValue()));
        }
        copy.elements = new ArrayList<>();
        for (HudElement e : source.elements) {
            HudElement cloned = cloneElement(e);
            copy.elements.add(cloned);
        }
        return copy;
    }

    private static List<HudElement> deepCopyElements(List<HudElement> elements) {
        List<HudElement> out = new ArrayList<>();
        if (elements == null) {
            return out;
        }
        for (HudElement e : elements) {
            out.add(cloneElement(e));
        }
        return out;
    }

    private static HudElement cloneElement(HudElement e) {
        HudElement cloned = new HudElement();
        cloned.id = e.id;
        cloned.type = e.type;
        cloned.label = e.label;
        cloned.text = e.text;
        cloned.macroId = e.macroId;
        cloned.buttonAction = e.buttonAction;
        cloned.buttonExecutionMode = e.buttonExecutionMode;
        cloned.runScriptsAsync = e.runScriptsAsync;
        cloned.x = e.x;
        cloned.y = e.y;
        cloned.anchor = e.anchor;
        cloned.width = e.width;
        cloned.height = e.height;
        cloned.lineHeight = e.lineHeight;
        cloned.fontScale = e.fontScale;
        cloned.backgroundColor = e.backgroundColor;
        cloned.backgroundAlpha = e.backgroundAlpha;
        cloned.borderColor = e.borderColor;
        cloned.textColor = e.textColor;
        cloned.drawBackground = e.drawBackground;
        cloned.backgroundOpaque = e.backgroundOpaque;
        cloned.drawBorder = e.drawBorder;
        cloned.borderMode = e.borderMode;
        cloned.horizontalAlign = e.horizontalAlign;
        cloned.verticalAlign = e.verticalAlign;
        cloned.visibilityMode = e.visibilityMode;
        cloned.visibilityScreenType = e.visibilityScreenType;
        cloned.visible = e.visible;
        cloned.sourceToken = e.sourceToken;
        cloned.sourceTokenMax = e.sourceTokenMax;
        cloned.prefix = e.prefix;
        cloned.suffix = e.suffix;
        cloned.minValue = e.minValue;
        cloned.maxValue = e.maxValue;
        cloned.colorStart = e.colorStart;
        cloned.colorEnd = e.colorEnd;
        cloned.colorWarn = e.colorWarn;
        cloned.colorCrit = e.colorCrit;
        cloned.warnThreshold = e.warnThreshold;
        cloned.critThreshold = e.critThreshold;
        cloned.segmented = e.segmented;
        cloned.segments = e.segments;
        cloned.maxLines = e.maxLines;
        cloned.listScroll = e.listScroll;
        cloned.iconKind = e.iconKind;
        cloned.iconId = e.iconId;
        cloned.iconShowCount = e.iconShowCount;
        cloned.iconShowDurability = e.iconShowDurability;
        cloned.iconShowCooldown = e.iconShowCooldown;
        cloned.modelZoom = e.modelZoom;
        cloned.modelYaw = e.modelYaw;
        cloned.modelPitch = e.modelPitch;
        cloned.modelOffsetX = e.modelOffsetX;
        cloned.modelOffsetY = e.modelOffsetY;
        cloned.modelAutoFit = e.modelAutoFit;
        cloned.modelFollowLook = e.modelFollowLook;
        cloned.shapeType = e.shapeType;
        cloned.shapeFilled = e.shapeFilled;
        cloned.shapeRadius = e.shapeRadius;
        cloned.shapeThickness = e.shapeThickness;
        cloned.stateOnText = e.stateOnText;
        cloned.stateOffText = e.stateOffText;
        cloned.stateTrueValues = e.stateTrueValues;
        cloned.stateFalseValues = e.stateFalseValues;
        cloned.stateShowValue = e.stateShowValue;
        return cloned;
    }

    private static HudConfig defaults() {
        HudConfig cfg = new HudConfig();
        cfg.enabled = true;
        cfg.activePresetId = "default";
        cfg.presetElements = new LinkedHashMap<>();
        cfg.elements = new ArrayList<>();


        HudElement button = createElement(ElementType.BUTTON);
        button.label = "Example Macro";
        button.macroId = "";
        button.x = 10;
        button.y = 26;
        cfg.elements.add(button);
        cfg.presetElements.put("default", deepCopyElements(cfg.elements));

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

