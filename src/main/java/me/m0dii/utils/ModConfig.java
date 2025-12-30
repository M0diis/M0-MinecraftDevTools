package me.m0dii.utils;


import eu.midnightdust.lib.config.MidnightConfig;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"CanBeFinal", "unused", "java:S1444"})
public class ModConfig extends MidnightConfig {

    public static final String CATEGORY_GENERAL = "General Settings";

    @MidnightConfig.Entry(name = "Fullbright Gamma", category = CATEGORY_GENERAL, isSlider = true, min = 1, max = 100)
    public static int fullbrightGamma = 100;

    @MidnightConfig.Entry(name = "Command History Limit", category = CATEGORY_GENERAL, isSlider = true, min = 10, max = 500)
    public static int commandHistoryLimit = 500;


    public static final String CATEGORY_OVERLAY = "Overlay Settings";

    @MidnightConfig.Entry(name = "Overlay XZ Radius", category = CATEGORY_OVERLAY, isSlider = true, min = 1, max = 64)
    public static int overlayXZradius = 16;

    @MidnightConfig.Entry(name = "Overlay Y Radius", category = CATEGORY_OVERLAY, isSlider = true, min = 1, max = 64)
    public static int overlayYradius = 4;

    @MidnightConfig.Entry(name = "Entity Radar Radius", category = CATEGORY_OVERLAY, isSlider = true, min = 16, max = 256)
    public static int entityRadarRadius = 64;

    // --- Inspector-specific HUD overlay settings ---
    public static final String CATEGORY_HUD_OVERLAY = "Hud Overlay Settings";

    public enum OverlayAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    @MidnightConfig.Entry(name = "Inspector Text Scale", category = CATEGORY_HUD_OVERLAY)
    public static double overlayInspectorTextScale = 0.85;

    @MidnightConfig.Entry(name = "Inspector Line Height", category = CATEGORY_HUD_OVERLAY)
    public static int overlayInspectorLineHeight = 9;

    @MidnightConfig.Entry(name = "Inspector Padding", category = CATEGORY_HUD_OVERLAY)
    public static int overlayInspectorPadding = 4;

    @MidnightConfig.Entry(name = "Inspector Margin X", category = CATEGORY_HUD_OVERLAY)
    public static int overlayInspectorMarginX = 6;

    @MidnightConfig.Entry(name = "Inspector Margin Y", category = CATEGORY_HUD_OVERLAY)
    public static int overlayInspectorMarginY = 6;

    // --- Macro overlay-specific settings (for HUD overlays like keybinds/pending macros) ---
    @MidnightConfig.Entry(name = "Macro Overlay Text Scale", category = CATEGORY_HUD_OVERLAY)
    public static double macroOverlayTextScale = 0.85;

    @MidnightConfig.Entry(name = "Macro Overlay Line Height", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayLineHeight = 9;

    @MidnightConfig.Entry(name = "Macro Overlay Padding", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayPadding = 4;

    @MidnightConfig.Entry(name = "Macro Overlay Margin X", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayMarginX = 6;

    @MidnightConfig.Entry(name = "Macro Overlay Margin Y", category = CATEGORY_HUD_OVERLAY)
    public static int macroOverlayMarginY = 6;

    // --- Macro GUI specific settings ---
    @MidnightConfig.Entry(name = "Macro GUI Width", category = CATEGORY_HUD_OVERLAY)
    public static int macroGuiWidth = 220;

    @MidnightConfig.Entry(name = "Macro GUI Height", category = CATEGORY_HUD_OVERLAY)
    public static int macroGuiHeight = 160;

    @MidnightConfig.Entry(name = "Macro GUI Text Scale", category = CATEGORY_HUD_OVERLAY)
    public static double macroGuiTextScale = 1.0;

    @MidnightConfig.Entry(name = "Macro GUI Line Height", category = CATEGORY_HUD_OVERLAY)
    public static int macroGuiLineHeight = 10;

    @MidnightConfig.Entry(name = "Macro GUI Padding", category = CATEGORY_HUD_OVERLAY)
    public static int macroGuiPadding = 6;

    @MidnightConfig.Entry(name = "Macro Overlay Anchor", category = CATEGORY_HUD_OVERLAY)
    public static OverlayAnchor macroOverlayAnchor = OverlayAnchor.TOP_RIGHT;

    @MidnightConfig.Entry(name = "Pending Macros Anchor", category = CATEGORY_HUD_OVERLAY)
    public static OverlayAnchor pendingMacrosAnchor = OverlayAnchor.TOP_LEFT;

    // --- Secondary chat box ---
    public static final String CATEGORY_SECONDARY_CHAT = "Secondary Chat Settings";

    public enum ChatInterceptMode {
        COPY,
        MOVE
    }

    @MidnightConfig.Entry(name = "Secondary Chat Enabled", category = CATEGORY_SECONDARY_CHAT)
    public static boolean secondaryChatEnabled = false;

    @MidnightConfig.Entry(name = "Secondary Chat Intercept Mode", category = CATEGORY_SECONDARY_CHAT)
    public static ChatInterceptMode secondaryChatInterceptMode = ChatInterceptMode.COPY;

    @MidnightConfig.Entry(name = "Secondary Chat Max Lines", category = CATEGORY_SECONDARY_CHAT, isSlider = true, min = 10, max = 500)
    public static int secondaryChatMaxLines = 100;

    @MidnightConfig.Entry(name = "Secondary Chat Show Overlay", category = CATEGORY_SECONDARY_CHAT)
    public static boolean secondaryChatShowOverlay = true;

    @MidnightConfig.Entry(name = "Secondary Chat Show While GUI Open", category = CATEGORY_SECONDARY_CHAT)
    public static boolean secondaryChatShowWhileGuiOpen = true;

    @MidnightConfig.Entry(name = "Secondary Chat X", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatX = 8;

    @MidnightConfig.Entry(name = "Secondary Chat Y", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatY = 40;

    @MidnightConfig.Entry(name = "Secondary Chat Width", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatWidth = 260;

    @MidnightConfig.Entry(name = "Secondary Chat Height", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatHeight = 120;

    @MidnightConfig.Entry(name = "Secondary Chat Scale", category = CATEGORY_SECONDARY_CHAT)
    public static double secondaryChatScale = 0.85;

    @MidnightConfig.Entry(name = "Secondary Chat Line Height", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatLineHeight = 9;

    @MidnightConfig.Entry(name = "Secondary Chat Padding", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatPadding = 4;

    @MidnightConfig.Entry(name = "Secondary Chat Background Color (ARGB)", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatBackgroundColor = 0x88000000;

    @MidnightConfig.Entry(name = "Secondary Chat Text Color (ARGB)", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatTextColor = 0xFFE0E0E0;

    @MidnightConfig.Entry(name = "Secondary Chat Route Outgoing", category = CATEGORY_SECONDARY_CHAT)
    public static boolean secondaryChatRouteOutgoing = false;

    @MidnightConfig.Entry(name = "Secondary Chat Regex List", category = CATEGORY_SECONDARY_CHAT)
    public static List<String> secondaryChatRegexList = new ArrayList<>();

    @MidnightConfig.Entry(name = "Secondary Chat Outgoing Regex", category = CATEGORY_SECONDARY_CHAT)
    public static String secondaryChatOutgoingRegex = "";

    @MidnightConfig.Entry(name = "Secondary Chat Fade Enabled", category = CATEGORY_SECONDARY_CHAT)
    public static boolean secondaryChatFadeEnabled = true;

    @MidnightConfig.Entry(name = "Secondary Chat Fade Duration (ms)", category = CATEGORY_SECONDARY_CHAT)
    public static int secondaryChatFadeDurationMs = 10000; // 10 seconds

    @MidnightConfig.Entry(name = "Secondary Chat Minimum Alpha", category = CATEGORY_SECONDARY_CHAT, isSlider = true, min = 0, max = 255)
    public static int secondaryChatMinAlpha = 40; // 0-255, minimum alpha when faded
}
