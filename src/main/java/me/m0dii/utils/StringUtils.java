package me.m0dii.utils;

import java.util.ArrayList;
import java.util.List;

public final class StringUtils {
    private StringUtils() {
    }

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static String preserve(String value) {
        return value == null ? "" : value;
    }

    public static String firstLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String normalized = normalizeMultilineInput(raw);
        int idx = normalized.indexOf('\n');
        return idx < 0 ? normalized : normalized.substring(0, idx);
    }

    public static List<String> splitLinesRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of("");
        }
        String normalized = normalizeMultilineInput(raw);
        String[] parts = normalized.split("\\n", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            out.add(p == null ? "" : p);
        }
        return out;
    }

    public static String normalizeMultilineInput(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    public static String normalizeSingleLineInput(String raw) {
        String normalized = normalizeMultilineInput(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replace('\n', ' ');
    }

    public static int lineStart(String text, int index) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cursor = Math.clamp(index, 0, text.length());
        if (cursor <= 0) {
            return 0;
        }
        int prevNewline = text.lastIndexOf('\n', cursor - 1);
        return prevNewline < 0 ? 0 : prevNewline + 1;
    }

    public static int lineEnd(String text, int index) {
        int cursor = Math.clamp(index, 0, text.length());
        int nextNewline = text.indexOf('\n', cursor);
        return nextNewline < 0 ? text.length() : nextNewline;
    }

    public static int moveCursorVertical(String text, int index, int dir) {
        int cursor = Math.clamp(index, 0, text.length());
        int start = lineStart(text, cursor);
        int col = cursor - start;

        if (dir < 0) {
            if (start == 0) {
                return cursor;
            }
            int prevEnd = start - 1;
            int prevStart = lineStart(text, prevEnd);
            return Math.min(prevStart + col, prevEnd);
        }

        int end = lineEnd(text, cursor);
        if (end >= text.length()) {
            return cursor;
        }
        int nextStart = end + 1;
        int nextEnd = lineEnd(text, nextStart);
        return Math.min(nextStart + col, nextEnd);
    }
}
