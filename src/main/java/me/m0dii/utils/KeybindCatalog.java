package me.m0dii.utils;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public final class KeybindCatalog {
    private KeybindCatalog() {
    }

    public record KeybindSpec(String translationKey, int defaultKey) {
    }

    // Defaults avoid every vanilla 26.x key (O = Friends List, P = Social Interactions, L = Advancements,
    // C/X = hotbar toolbars) and each other. Mod hotkeys are ignored while F3 is held, so vanilla's F3+letter debug
    // keys (N, B, H, ...) never trigger them.

    public static final KeybindSpec OPEN_MODULES_SCREEN = new KeybindSpec("key.m0-dev-tools.open_modules_screen", InputConstants.KEY_N);
    public static final KeybindSpec CLICKGUI_TOGGLE = new KeybindSpec("key.m0-dev-tools.clickgui", InputConstants.KEY_GRAVE);
    public static final KeybindSpec COMMAND_HISTORY = new KeybindSpec("key.m0-dev-tools.command_history", InputConstants.KEY_SEMICOLON);
    public static final KeybindSpec MESSAGE_HISTORY = new KeybindSpec("key.m0-dev-tools.message_history", InputConstants.KEY_APOSTROPHE);
    public static final KeybindSpec FREECAM_TOGGLE = new KeybindSpec("key.m0-dev-tools.freecam", InputConstants.KEY_U);
    public static final KeybindSpec BLOCK_INSPECTOR_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_block_inspector", InputConstants.KEY_F10);
    public static final KeybindSpec ENTITY_RADAR_SCREEN = new KeybindSpec("key.m0-dev-tools.open_entity_radar_screen", InputConstants.KEY_R);
    public static final KeybindSpec XRAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.xray_toggle", InputConstants.KEY_RBRACKET);
    public static final KeybindSpec XRAY_MENU = new KeybindSpec("key.m0-dev-tools.xray_menu", InputConstants.KEY_LBRACKET);
    public static final KeybindSpec SECONDARY_CHAT_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_secondary_chat", InputConstants.KEY_Y);
    public static final KeybindSpec FULLBRIGHT_TOGGLE = new KeybindSpec("key.m0-dev-tools.fullbright", InputConstants.KEY_J);
    public static final KeybindSpec ZOOM_HOLD = new KeybindSpec("key.m0-dev-tools.zoom", InputConstants.KEY_Z);

    public static final KeybindSpec QUICK_TP_UP = new KeybindSpec("key.m0-dev-tools.teleport_up", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec QUICK_TP_DOWN = new KeybindSpec("key.m0-dev-tools.teleport_down", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec QUICK_TP_FORWARD = new KeybindSpec("key.m0-dev-tools.teleport_forward", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec QUICK_TP_LEFT = new KeybindSpec("key.m0-dev-tools.teleport_left", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec QUICK_TP_RIGHT = new KeybindSpec("key.m0-dev-tools.teleport_right", InputConstants.UNKNOWN.getValue());

    public static final KeybindSpec WAYPOINT_ADD = new KeybindSpec("key.m0-dev-tools.add_waypoint", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec WAYPOINT_LIST = new KeybindSpec("key.m0-dev-tools.list_waypoints", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec WAYPOINT_GUI = new KeybindSpec("key.m0-dev-tools.open_waypoint_gui", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec WAYPOINT_RENDER_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_waypoint_render", InputConstants.UNKNOWN.getValue());

    public static final KeybindSpec LIGHT_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_light_overlay", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec REDSTONE_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_redstone_overlay", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec SLIME_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_slime_overlay", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec COMMAND_BLOCK_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.command_block_overlay", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec REDSTONE_BUD_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_redstone_bud", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec BIOME_BORDER_OVERLAY_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_biome_border_overlay", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec WATSON_COREPROTECT_TOGGLE = new KeybindSpec("key.m0-dev-tools.toggle_watson_cp", InputConstants.KEY_PERIOD);

    public static final KeybindSpec OPEN_MACRO_GUI = new KeybindSpec("key.m0-dev-tools.open_macro_gui", InputConstants.KEY_M);
    public static final KeybindSpec OPEN_MACRO_HUD_EDITOR = new KeybindSpec("key.m0-dev-tools.open_macro_hud_editor", InputConstants.KEY_K);
    public static final KeybindSpec OPEN_MACRO_KEYBOARD_LAYOUT = new KeybindSpec("key.m0-dev-tools.open_macro_keyboard_layout", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec OPEN_SCRIPT_EDITOR = new KeybindSpec("key.m0-dev-tools.openscripteditor", InputConstants.KEY_COMMA);

    public static final KeybindSpec RULER_MARK = new KeybindSpec("key.m0-dev-tools.ruler_mark", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec PACKET_INSPECTOR = new KeybindSpec("key.m0-dev-tools.packet_inspector", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec FREECAM_CONTROL_PLAYER = new KeybindSpec("key.m0-dev-tools.freecam_control_player", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec FREECAM_SAVE_SPOT = new KeybindSpec("key.m0-dev-tools.freecam_save_spot", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec FREECAM_NEXT_SPOT = new KeybindSpec("key.m0-dev-tools.freecam_next_spot", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec PLACEMENT_RESTRICTION_CYCLE = new KeybindSpec("key.m0-dev-tools.placement_restriction_cycle", InputConstants.UNKNOWN.getValue());
    public static final KeybindSpec CYCLE_PROFILE = new KeybindSpec("key.m0-dev-tools.cycle_profile", InputConstants.UNKNOWN.getValue());

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
                BIOME_BORDER_OVERLAY_TOGGLE,
                RULER_MARK,
                PACKET_INSPECTOR,
                CYCLE_PROFILE,
                FREECAM_CONTROL_PLAYER,
                FREECAM_SAVE_SPOT,
                FREECAM_NEXT_SPOT,
                PLACEMENT_RESTRICTION_CYCLE,
                WATSON_COREPROTECT_TOGGLE,
                OPEN_MACRO_GUI,
                OPEN_MACRO_HUD_EDITOR,
                OPEN_MACRO_KEYBOARD_LAYOUT,
                OPEN_SCRIPT_EDITOR
        );
    }
}

