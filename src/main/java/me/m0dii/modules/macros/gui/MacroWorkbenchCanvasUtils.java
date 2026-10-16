package me.m0dii.modules.macros.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MacroWorkbenchCanvasUtils {
    private MacroWorkbenchCanvasUtils() {
    }

    public static Double parseFirstDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim().replace(',', '.');
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            StringBuilder num = new StringBuilder();
            boolean dotSeen = false;
            for (int i = 0; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if ((c >= '0' && c <= '9') || (num.isEmpty() && (c == '-' || c == '+'))) {
                    num.append(c);
                } else if (c == '.' && !dotSeen) {
                    dotSeen = true;
                    num.append(c);
                } else if (!num.isEmpty()) {
                    break;
                }
            }
            if (!num.isEmpty()) {
                try {
                    return Double.parseDouble(num.toString());
                } catch (Exception ignored2) {
                    return null;
                }
            }
            return null;
        }
    }

    public static String formatValue(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static List<String> splitListSource(String src) {
        if (src == null || src.isBlank()) {
            return new ArrayList<>();
        }
        String normalized = src.replace("\\n", "\n").replace("\r", "");
        List<String> out = new ArrayList<>();
        if (normalized.contains("\n")) {
            for (String line : normalized.split("\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        for (String part : normalized.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public static String shortAnchor(MacroHudDataHandler.Anchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "TL";
            case TOP_CENTER -> "TC";
            case TOP_RIGHT -> "TR";
            case MIDDLE_LEFT -> "ML";
            case MIDDLE_RIGHT -> "MR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_CENTER -> "BC";
            case BOTTOM_RIGHT -> "BR";
            case MIDDLE_CENTER -> "MC";
            case CENTER -> "C*";
        };
    }

    public static String shortVisibility(MacroHudDataHandler.VisibilityMode mode) {
        return switch (mode) {
            case ALWAYS -> "ALL";
            case CHAT -> "CHAT";
            case INVENTORY -> "INV";
            case CONTAINER -> "CONT";
            case CHEST -> "CHEST";
            case SCREEN -> "SCR";
        };
    }

    public static String shortExecutionMode(MacroHudDataHandler.ButtonExecutionMode mode) {
        return switch (mode == null ? MacroHudDataHandler.ButtonExecutionMode.COMMAND : mode) {
            case COMMAND -> "Command";
            case GROOVY_SCRIPT -> "Groovy Script";
            case KOTLIN_SCRIPT -> "Kotlin Script";
            case JAVASCRIPT_SCRIPT -> "JavaScript Script";
        };
    }

    /**
     * Keyboard-grid cells for mouse buttons use {@code MOUSE_CELL + button} so they never collide with keyboard
     * scancodes (since 26.x mouse buttons are 1..8, and scancode 4 is the A key).
     */
    public static final int MOUSE_CELL = 0x10000;

    public static int mouseCell(int button) {
        return MOUSE_CELL + button;
    }

    public static boolean isMouseCell(int code) {
        return code >= MOUSE_CELL;
    }

    public static String keyLabel(int keyCode) {
        if (isMouseCell(keyCode)) {
            return switch (keyCode - MOUSE_CELL) {
                case InputConstants.MOUSE_BUTTON_LEFT -> "LMB";
                case InputConstants.MOUSE_BUTTON_RIGHT -> "RMB";
                case InputConstants.MOUSE_BUTTON_MIDDLE -> "MMB";
                default -> "M" + (keyCode - MOUSE_CELL);
            };
        }
        return switch (keyCode) {
            case InputConstants.KEY_ESCAPE -> "Esc";
            case InputConstants.KEY_LCONTROL -> "L.C";
            case InputConstants.KEY_RCONTROL -> "R.C";
            case InputConstants.KEY_LALT -> "L.A";
            case InputConstants.KEY_RALT -> "R.A";
            case InputConstants.KEY_LSHIFT -> "L.S";
            case InputConstants.KEY_RSHIFT -> "R.S";
            case InputConstants.KEY_UP -> "Up";
            case InputConstants.KEY_DOWN -> "Dn";
            case InputConstants.KEY_LEFT -> "Lt";
            case InputConstants.KEY_RIGHT -> "Rt";
            case InputConstants.KEY_BACKSPACE -> "Bksp";
            case InputConstants.KEY_CAPSLOCK -> "Caps";
            case InputConstants.KEY_ADD -> "Num+";
            case org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS -> "Num-";
            case InputConstants.KEY_MULTIPLY -> "Num*";
            case org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_DIVIDE -> "Num/";
            case InputConstants.KEY_NUMPADENTER -> "NumE";
            case org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_PERIOD -> "Num.";
            case InputConstants.KEY_HOME -> "Home";
            case InputConstants.KEY_END -> "End";
            case InputConstants.KEY_PAGEUP -> "PgUp";
            case InputConstants.KEY_PAGEDOWN -> "PgDn";
            case InputConstants.KEY_INSERT -> "Ins";
            case InputConstants.KEY_DELETE -> "Del";
            case InputConstants.KEY_NUMPAD0 -> "0";
            case InputConstants.KEY_NUMPAD1 -> "1";
            case InputConstants.KEY_NUMPAD2 -> "2";
            case InputConstants.KEY_NUMPAD3 -> "3";
            case InputConstants.KEY_NUMPAD4 -> "4";
            case InputConstants.KEY_NUMPAD5 -> "5";
            case InputConstants.KEY_NUMPAD6 -> "6";
            case InputConstants.KEY_NUMPAD7 -> "7";
            case InputConstants.KEY_NUMPAD8 -> "8";
            case InputConstants.KEY_NUMPAD9 -> "9";
            default -> {
                try {
                    String s = InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getDisplayName().getString();
                    yield s.length() > 7 ? s.substring(0, 7) : s;
                } catch (Exception ignored) {
                    yield "?";
                }
            }
        };
    }

    public static String keyTranslationLabel(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return "None";
        }
        try {
            InputConstants.Key key = InputConstants.getKey(translationKey.toLowerCase(Locale.ROOT));
            if (key == null) {
                return translationKey;
            }
            String text = key.getDisplayName().getString();
            return text == null || text.isBlank() ? translationKey : text;
        } catch (Exception ignored) {
            return translationKey;
        }
    }
}
