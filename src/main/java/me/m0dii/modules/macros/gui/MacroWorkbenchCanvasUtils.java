package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

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
        };
    }

    public static String keyLabel(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L.C";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R.C";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L.A";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R.A";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L.S";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R.S";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Dn";
            case GLFW.GLFW_KEY_LEFT -> "Lt";
            case GLFW.GLFW_KEY_RIGHT -> "Rt";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_KP_ADD -> "Num+";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "Num-";
            case GLFW.GLFW_KEY_KP_MULTIPLY -> "Num*";
            case GLFW.GLFW_KEY_KP_DIVIDE -> "Num/";
            case GLFW.GLFW_KEY_KP_ENTER -> "NumE";
            case GLFW.GLFW_KEY_KP_DECIMAL -> "Num.";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "M1";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "M2";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "M3";
            case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
            case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
            default -> {
                try {
                    String s = InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
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
            InputUtil.Key key = InputUtil.fromTranslationKey(translationKey.toLowerCase(Locale.ROOT));
            if (key == null) {
                return translationKey;
            }
            String text = key.getLocalizedText().getString();
            return text == null || text.isBlank() ? translationKey : text;
        } catch (Exception ignored) {
            return translationKey;
        }
    }
}
