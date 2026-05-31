package me.m0dii.utils;

public final class TextSelectionUtils {
    private TextSelectionUtils() {
    }

    public static boolean hasSelection(int anchor, int cursor) {
        return anchor >= 0 && anchor != cursor;
    }

    public static int selectionStart(int anchor, int cursor) {
        return Math.min(anchor, cursor);
    }

    public static int selectionEnd(int anchor, int cursor) {
        return Math.max(anchor, cursor);
    }

    public static int clampTextIndex(String text, int index) {
        int len = text == null ? 0 : text.length();
        return Math.clamp(index, 0, len);
    }

    public static int[] clampedSelectionRange(String text, int anchor, int cursor) {
        int start = clampTextIndex(text, selectionStart(anchor, cursor));
        int end = clampTextIndex(text, selectionEnd(anchor, cursor));
        if (end < start) {
            end = start;
        }
        return new int[]{start, end};
    }

    public static String selectedText(String text, int anchor, int cursor) {
        if (!hasSelection(anchor, cursor)) {
            return "";
        }
        String safeText = text == null ? "" : text;
        int[] range = clampedSelectionRange(safeText, anchor, cursor);
        return safeText.substring(range[0], range[1]);
    }

    public static int updateSelectionAnchor(int currentAnchor, int oldCursor, int newCursor, boolean shiftDown) {
        if (!shiftDown) {
            return -1;
        }
        if (oldCursor == newCursor) {
            return currentAnchor;
        }
        return currentAnchor < 0 ? oldCursor : currentAnchor;
    }
}
