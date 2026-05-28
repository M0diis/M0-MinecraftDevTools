package me.m0dii.utils;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeybindCatalog {
    private KeybindCatalog() {
    }

    public record KeybindSpec(String translationKey, int defaultKey) {
    }

    public static final KeybindSpec OPEN_MODULES_SCREEN = new KeybindSpec("key.m0-dev-tools.open_modules_screen", GLFW.GLFW_KEY_O);
    public static final KeybindSpec CLICKGUI_TOGGLE = new KeybindSpec("key.m0-dev-tools.clickgui", GLFW.GLFW_KEY_GRAVE_ACCENT);
    public static final KeybindSpec COMMAND_HISTORY = new KeybindSpec("key.m0-dev-tools.command_history", GLFW.GLFW_KEY_SEMICOLON);
    public static final KeybindSpec MESSAGE_HISTORY = new KeybindSpec("key.m0-dev-tools.message_history", GLFW.GLFW_KEY_APOSTROPHE);
    public static final KeybindSpec FREECAM_TOGGLE = new KeybindSpec("key.m0-dev-tools.freecam", GLFW.GLFW_KEY_U);
    public static final KeybindSpec BLOCK_INSPECTOR_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_block_inspector", GLFW.GLFW_KEY_F10);
    public static final KeybindSpec ENTITY_RADAR_SCREEN = new KeybindSpec("key.m0-dev-tools.open_entity_radar_screen", GLFW.GLFW_KEY_L);
    public static final KeybindSpec XRAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.xray_toggle", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeybindSpec XRAY_MENU = new KeybindSpec("key.m0-dev-tools.xray_menu", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeybindSpec SECONDARY_CHAT_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_secondary_chat", GLFW.GLFW_KEY_P);
    public static final KeybindSpec FULLBRIGHT_TOGGLE = new KeybindSpec("key.m0-dev-tools.fullbright", GLFW.GLFW_KEY_J);
    public static final KeybindSpec ZOOM_HOLD = new KeybindSpec("key.m0-dev-tools.zoom", GLFW.GLFW_KEY_C);

    public static final KeybindSpec QUICK_TP_UP = new KeybindSpec("key.m0-dev-tools.teleport_up", GLFW.GLFW_KEY_KP_8);
    public static final KeybindSpec QUICK_TP_DOWN = new KeybindSpec("key.m0-dev-tools.teleport_down", GLFW.GLFW_KEY_KP_2);
    public static final KeybindSpec QUICK_TP_FORWARD = new KeybindSpec("key.m0-dev-tools.teleport_forward", GLFW.GLFW_KEY_KP_5);
    public static final KeybindSpec QUICK_TP_LEFT = new KeybindSpec("key.m0-dev-tools.teleport_left", GLFW.GLFW_KEY_KP_4);
    public static final KeybindSpec QUICK_TP_RIGHT = new KeybindSpec("key.m0-dev-tools.teleport_right", GLFW.GLFW_KEY_KP_6);

    public static final KeybindSpec WAYPOINT_ADD = new KeybindSpec("key.m0-dev-tools.add_waypoint", GLFW.GLFW_KEY_KP_ADD);
    public static final KeybindSpec WAYPOINT_LIST = new KeybindSpec("key.m0-dev-tools.list_waypoints", GLFW.GLFW_KEY_KP_MULTIPLY);
    public static final KeybindSpec WAYPOINT_GUI = new KeybindSpec("key.m0-dev-tools.open_waypoint_gui", GLFW.GLFW_KEY_KP_DIVIDE);
    public static final KeybindSpec WAYPOINT_RENDER_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_waypoint_render", GLFW.GLFW_KEY_KP_SUBTRACT);

    public static final KeybindSpec LIGHT_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_light_overlay", GLFW.GLFW_KEY_F9);
    public static final KeybindSpec REDSTONE_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_redstone_overlay", GLFW.GLFW_KEY_F7);
    public static final KeybindSpec SLIME_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_slime_overlay", GLFW.GLFW_KEY_F9);
    public static final KeybindSpec COMMAND_BLOCK_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.command_block_overlay", GLFW.GLFW_KEY_X);
    public static final KeybindSpec REDSTONE_BUD_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_redstone_bud", GLFW.GLFW_KEY_B);

    public static final KeybindSpec OPEN_MACRO_GUI = new KeybindSpec("key.m0-dev-tools.open_macro_gui", GLFW.GLFW_KEY_M);
    public static final KeybindSpec OPEN_MACRO_HUD_EDITOR = new KeybindSpec("key.m0-dev-tools.open_macro_hud_editor", GLFW.GLFW_KEY_K);
    public static final KeybindSpec OPEN_MACRO_KEYBOARD_LAYOUT = new KeybindSpec("key.m0-dev-tools.open_macro_keyboard_layout", GLFW.GLFW_KEY_U);
    public static final KeybindSpec OPEN_SCRIPT_EDITOR = new KeybindSpec("key.m0-dev-tools.openscripteditor", GLFW.GLFW_KEY_COMMA);

    // Single place for quick overview and future validation tooling.
    public static List<KeybindSpec> all() {
        return List.of(
                OPEN_MODULES_SCREEN,
                CLICKGUI_TOGGLE,
                COMMAND_HISTORY,
                MESSAGE_HISTORY,
                FREECAM_TOGGLE,
                BLOCK_INSPECTOR_TOGGLE,
                ENTITY_RADAR_SCREEN,
                XRAY_TOGGLE,
                XRAY_MENU,
                SECONDARY_CHAT_TOGGLE,
                FULLBRIGHT_TOGGLE,
                ZOOM_HOLD,
                QUICK_TP_UP,
                QUICK_TP_DOWN,
                QUICK_TP_FORWARD,
                QUICK_TP_LEFT,
                QUICK_TP_RIGHT,
                WAYPOINT_ADD,
                WAYPOINT_LIST,
                WAYPOINT_GUI,
                WAYPOINT_RENDER_TOGGLE,
                LIGHT_OVERLAY_TOGGLE,
                REDSTONE_OVERLAY_TOGGLE,
                SLIME_OVERLAY_TOGGLE,
                COMMAND_BLOCK_OVERLAY_TOGGLE,
                REDSTONE_BUD_TOGGLE,
                OPEN_MACRO_GUI,
                OPEN_MACRO_HUD_EDITOR,
                OPEN_MACRO_KEYBOARD_LAYOUT,
                OPEN_SCRIPT_EDITOR
        );
    }
}

