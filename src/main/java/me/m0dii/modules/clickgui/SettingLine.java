package me.m0dii.modules.clickgui;

import me.m0dii.gui.theme.Theme;

import java.util.Locale;

/**
 * A module setting line ({@code "Radius: 8"}, {@code "Show pieces: ON"}, {@code "Clear"}) split into a label and a
 * value so the GUI can show the value on the right, coloured by kind.
 */
record SettingLine(String label, String value, Kind kind) {
    enum Kind {
        ON, OFF, NUMBER, TEXT, ACTION
    }

    static SettingLine parse(String raw) {
        String line = raw == null ? "" : raw.strip();
        int colon = line.lastIndexOf(':');
        // Only split on a colon that separates a label from a short value ("Key: key.keyboard.k" counts too).
        if (colon <= 0 || colon == line.length() - 1) {
            return new SettingLine(line, "", Kind.ACTION);
        }
        String label = line.substring(0, colon).strip();
        String value = line.substring(colon + 1).strip();
        return new SettingLine(label, value, kindOf(value));
    }

    private static Kind kindOf(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.equals("on") || lower.equals("true") || lower.equals("yes") || lower.equals("enabled")) {
            return Kind.ON;
        }
        if (lower.equals("off") || lower.equals("false") || lower.equals("no") || lower.equals("disabled")) {
            return Kind.OFF;
        }
        if (!value.isEmpty() && (Character.isDigit(value.charAt(0)) || value.charAt(0) == '-' || value.charAt(0) == '.')) {
            return Kind.NUMBER;
        }
        return Kind.TEXT;
    }

    boolean isToggle() {
        return kind == Kind.ON || kind == Kind.OFF;
    }

    int valueColor() {
        return switch (kind) {
            case ON -> Theme.SUCCESS;
            case OFF -> Theme.TEXT_DIM;
            case NUMBER -> Theme.ACCENT_TEXT;
            case TEXT -> Theme.TEXT_MUTED;
            case ACTION -> Theme.TEXT_DIM;
        };
    }
}
