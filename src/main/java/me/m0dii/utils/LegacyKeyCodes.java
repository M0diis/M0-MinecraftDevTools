package me.m0dii.utils;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts GLFW key codes (Minecraft 1.21.x and earlier) to the SDL scancodes Minecraft uses since 26.x.
 * Configs written by older versions of the mod store raw GLFW codes and must be translated once on load.
 */
public final class LegacyKeyCodes {
    private static final Map<Integer, Integer> GLFW_TO_SCANCODE = new HashMap<>();

    static {
        map(32, InputConstants.KEY_SPACE);
        map(39, InputConstants.KEY_APOSTROPHE);
        map(44, InputConstants.KEY_COMMA);
        map(45, InputConstants.KEY_MINUS);
        map(46, InputConstants.KEY_PERIOD);
        map(47, InputConstants.KEY_SLASH);
        map(48, InputConstants.KEY_0);
        for (int digit = 1; digit <= 9; digit++) {
            map(48 + digit, InputConstants.KEY_1 + digit - 1);
        }
        map(59, InputConstants.KEY_SEMICOLON);
        map(61, InputConstants.KEY_EQUALS);
        for (int letter = 0; letter < 26; letter++) {
            map(65 + letter, InputConstants.KEY_A + letter);
        }
        map(91, InputConstants.KEY_LBRACKET);
        map(92, InputConstants.KEY_BACKSLASH);
        map(93, InputConstants.KEY_RBRACKET);
        map(96, InputConstants.KEY_GRAVE);
        map(161, 100); // GLFW_KEY_WORLD_1 -> SDL_SCANCODE_NONUSBACKSLASH
        map(256, InputConstants.KEY_ESCAPE);
        map(257, InputConstants.KEY_RETURN);
        map(258, InputConstants.KEY_TAB);
        map(259, InputConstants.KEY_BACKSPACE);
        map(260, InputConstants.KEY_INSERT);
        map(261, InputConstants.KEY_DELETE);
        map(262, InputConstants.KEY_RIGHT);
        map(263, InputConstants.KEY_LEFT);
        map(264, InputConstants.KEY_DOWN);
        map(265, InputConstants.KEY_UP);
        map(266, InputConstants.KEY_PAGEUP);
        map(267, InputConstants.KEY_PAGEDOWN);
        map(268, InputConstants.KEY_HOME);
        map(269, InputConstants.KEY_END);
        map(280, InputConstants.KEY_CAPSLOCK);
        map(281, InputConstants.KEY_SCROLLLOCK);
        map(282, InputConstants.KEY_NUMLOCK);
        map(283, InputConstants.KEY_PRINTSCREEN);
        map(284, InputConstants.KEY_PAUSE);
        for (int fn = 1; fn <= 12; fn++) {
            map(289 + fn, InputConstants.KEY_F1 + fn - 1);
        }
        for (int fn = 13; fn <= 24; fn++) {
            map(289 + fn, InputConstants.KEY_F13 + fn - 13);
        }
        map(320, InputConstants.KEY_NUMPAD0);
        for (int digit = 1; digit <= 9; digit++) {
            map(320 + digit, InputConstants.KEY_NUMPAD1 + digit - 1);
        }
        map(330, 99); // GLFW_KEY_KP_DECIMAL -> SDL_SCANCODE_KP_PERIOD
        map(331, 84); // GLFW_KEY_KP_DIVIDE -> SDL_SCANCODE_KP_DIVIDE
        map(332, InputConstants.KEY_MULTIPLY);
        map(333, 86); // GLFW_KEY_KP_SUBTRACT -> SDL_SCANCODE_KP_MINUS
        map(334, InputConstants.KEY_ADD);
        map(335, InputConstants.KEY_NUMPADENTER);
        map(336, InputConstants.KEY_NUMPADEQUALS);
        map(340, InputConstants.KEY_LSHIFT);
        map(341, InputConstants.KEY_LCONTROL);
        map(342, InputConstants.KEY_LALT);
        map(343, InputConstants.KEY_LGUI);
        map(344, InputConstants.KEY_RSHIFT);
        map(345, InputConstants.KEY_RCONTROL);
        map(346, InputConstants.KEY_RALT);
        map(347, InputConstants.KEY_RGUI);
        map(348, 101); // GLFW_KEY_MENU -> SDL_SCANCODE_APPLICATION
    }

    private LegacyKeyCodes() {
    }

    private static void map(int glfw, int scancode) {
        GLFW_TO_SCANCODE.put(glfw, scancode);
    }

    /**
     * @return the SDL scancode for a GLFW key code, or -1 (unbound) when the key has no equivalent.
     * Negative input (already unbound) is returned unchanged.
     */
    public static int fromGlfw(int glfwKeyCode) {
        if (glfwKeyCode < 0) {
            return glfwKeyCode;
        }
        return GLFW_TO_SCANCODE.getOrDefault(glfwKeyCode, -1);
    }
}
