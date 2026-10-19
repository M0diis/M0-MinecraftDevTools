package me.m0dii.modules.macros.gui;

import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.network.chat.Component;

/**
 * Value types and pure helpers for the vanilla HUD proxies that can be placed on the workbench canvas.
 */
public final class MacroWorkbenchVanillaHud {
    private MacroWorkbenchVanillaHud() {
    }

    private static final String VANILLA_HUD_PROXY_PREFIX = "__vanilla_hud_";

    public static HudTweaksSettings.ElementType vanillaHudElementType(MacroHudDataHandler.HudElement element) {
        if (element == null || element.id == null || !element.id.startsWith(VANILLA_HUD_PROXY_PREFIX)) {
            return null;
        }
        String raw = element.id.substring(VANILLA_HUD_PROXY_PREFIX.length());
        try {
            return HudTweaksSettings.ElementType.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String vanillaHudProxyId(HudTweaksSettings.ElementType type) {
        return VANILLA_HUD_PROXY_PREFIX + type.name();
    }

    public static HudTweaksSettings.ElementConfig copyHudTweaksConfig(HudTweaksSettings.ElementConfig source) {
        HudTweaksSettings.ElementConfig copy = new HudTweaksSettings.ElementConfig();
        if (source == null) {
            return copy;
        }
        copy.display = source.display;
        copy.scale = Math.clamp(source.scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE);
        copy.opacity = Math.clamp(source.opacity, 0.0f, 1.0f);
        copy.offsetX = Math.clamp(source.offsetX, -500, 500);
        copy.offsetY = Math.clamp(source.offsetY, -500, 500);
        return copy;
    }

    public record VanillaHudDescriptor(String label,
                                       int baseWidth,
                                       int baseHeight,
                                       int baseZ) {
    }

    public record VanillaPlayerListMetrics(int left, int top, int width, int height, int renderWidth) {
    }

    public record VanillaSidebarMetrics(int left, int top, int width, int height) {
    }

    public record VanillaSidebarEntryMetrics(Component name, int scoreWidth) {
    }

    public static VanillaHudDescriptor vanillaHudDescriptor(HudTweaksSettings.ElementType type) {
        return switch (type) {
            case ACTION_BAR -> new VanillaHudDescriptor("Action Bar", 182, 20, 20);
            case BOSS_BAR -> new VanillaHudDescriptor("Boss Bar", 182, 20, 30);
            case CROSSHAIR -> new VanillaHudDescriptor("Crosshair", 15, 15, 50);
            case DEBUG_SCREEN -> new VanillaHudDescriptor("Debug Screen", 220, 120, 60);
            case HOTBAR_GROUP -> new VanillaHudDescriptor("Hotbar Group", 182, 39, 10);
            case PLAYER_LIST -> new VanillaHudDescriptor("Player List", 240, 140, 40);
            case SCOREBOARD -> new VanillaHudDescriptor("Scoreboard", 140, 90, 35);
            case SCREEN_TITLE -> new VanillaHudDescriptor("Screen Title", 240, 68, 25);
            case STATUS_EFFECT -> new VanillaHudDescriptor("Status Effect", 120, 72, 45);
            case SUBTITLES -> new VanillaHudDescriptor("Subtitles", 180, 48, 15);
            case TOAST -> new VanillaHudDescriptor("Toast", 160, 32, 55);
            case TOOLTIP -> new VanillaHudDescriptor("Tooltip", 160, 40, 65);
        };
    }

    public static int vanillaScaledOffset(int value, float scale) {
        return Math.round(value * Math.clamp(scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE));
    }

    public static void applyVanillaProxyStyle(MacroHudDataHandler.HudElement proxy, HudTweaksSettings.ElementConfig config) {
        if (proxy == null || config == null) {
            return;
        }
        proxy.backgroundColor = config.display ? 0x5A101820 : 0x44303030;
        proxy.borderColor = config.display ? 0xFFFFFFFF : 0xFF888888;
        proxy.textColor = config.display ? 0xFFFFFFFF : 0xFFB8B8B8;
    }
}
