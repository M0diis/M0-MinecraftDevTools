package me.m0dii.gui.local;

import me.m0dii.utils.StringUtils;
import me.m0dii.utils.TextSelectionUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class GuiTextEditingUtils {
    private GuiTextEditingUtils() {
    }

    public static int cursorIndexFromPoint(TextRenderer textRenderer, String text, int localX, int localY, int lineHeight) {
        String safeText = text == null ? "" : text;
        List<String> lines = StringUtils.splitLinesRaw(safeText);
        int row = Math.clamp(localY / Math.max(1, lineHeight), 0, Math.max(0, lines.size() - 1));

        int base = 0;
        for (int i = 0; i < row; i++) {
            base += lines.get(i).length() + 1;
        }

        String line = lines.get(row);
        int x = Math.max(0, localX);
        int bestCol = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int col = 0; col <= line.length(); col++) {
            int w = textRenderer.getWidth(line.substring(0, col));
            int d = Math.abs(w - x);
            if (d < bestDist) {
                bestDist = d;
                bestCol = col;
            }
        }
        return Math.clamp(base + bestCol, 0, safeText.length());
    }

    public static int[] cursorPixel(TextRenderer textRenderer, int x, int y, String text, int cursorIndex) {
        String safeText = text == null ? "" : text;
        List<String> lines = StringUtils.splitLinesRaw(safeText.substring(0, Math.clamp(cursorIndex, 0, safeText.length())));
        int row = Math.max(0, lines.size() - 1);
        String last = lines.isEmpty() ? "" : lines.getLast();
        return new int[]{x + textRenderer.getWidth(last), y + row * 9};
    }

    public static int[] cursorPixelWithScroll(TextRenderer textRenderer, int baseX, int baseY, String text, int cursorIndex, int scrollLine) {
        String safeText = text == null ? "" : text;
        int cursor = Math.clamp(cursorIndex, 0, safeText.length());
        int line = cursorLineIndex(safeText, cursor);
        int lineStart = StringUtils.lineStart(safeText, cursor);
        String before = safeText.substring(lineStart, cursor);
        int x = baseX + textRenderer.getWidth(before);
        int y = baseY + Math.max(0, line - Math.max(0, scrollLine)) * 9;
        return new int[]{x, y};
    }

    public static int cursorLineIndex(String text, int cursorIndex) {
        String safeText = text == null ? "" : text;
        int cursor = Math.clamp(cursorIndex, 0, safeText.length());
        int line = 0;
        for (int i = 0; i < cursor; i++) {
            if (safeText.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    public static void drawSingleLineSelection(DrawContext context,
                                               TextRenderer textRenderer,
                                               int textX,
                                               int textY,
                                               String text,
                                               int anchor,
                                               int cursor) {
        if (!TextSelectionUtils.hasSelection(anchor, cursor)) {
            return;
        }
        String safeText = text == null ? "" : text;
        int[] range = TextSelectionUtils.clampedSelectionRange(safeText, anchor, cursor);
        int sx = textX + textRenderer.getWidth(safeText.substring(0, range[0]));
        int ex = textX + textRenderer.getWidth(safeText.substring(0, range[1]));
        if (ex > sx) {
            context.fill(sx, textY, ex, textY + 9, 0x704A7CC7);
        }
    }

    public static void drawMultilineSelection(DrawContext context,
                                              TextRenderer textRenderer,
                                              int textX,
                                              int textY,
                                              int maxY,
                                              String text,
                                              int anchor,
                                              int cursor,
                                              int lineHeight) {
        if (!TextSelectionUtils.hasSelection(anchor, cursor)) {
            return;
        }
        String safeText = text == null ? "" : text;
        int[] range = TextSelectionUtils.clampedSelectionRange(safeText, anchor, cursor);
        int start = range[0];
        int end = range[1];
        List<String> lines = StringUtils.splitLinesRaw(safeText);
        int y = textY;
        int index = 0;
        for (String line : lines) {
            if (y > maxY) {
                break;
            }
            int lineStart = index;
            int lineEnd = lineStart + line.length();
            int selStart = Math.max(start, lineStart);
            int selEnd = Math.min(end, lineEnd);
            if (selEnd > selStart) {
                int sx = textX + textRenderer.getWidth(line.substring(0, selStart - lineStart));
                int ex = textX + textRenderer.getWidth(line.substring(0, selEnd - lineStart));
                context.fill(sx, y, ex, y + 9, 0x704A7CC7);
            }
            y += lineHeight;
            index = lineEnd + 1;
        }
    }

    public static void drawMultilineSelectionWithScroll(DrawContext context,
                                                        TextRenderer textRenderer,
                                                        int textX,
                                                        int textY,
                                                        int maxY,
                                                        String text,
                                                        int anchor,
                                                        int cursor,
                                                        int lineHeight,
                                                        int scrollLines) {
        if (!TextSelectionUtils.hasSelection(anchor, cursor)) {
            return;
        }
        String safeText = text == null ? "" : text;
        int[] range = TextSelectionUtils.clampedSelectionRange(safeText, anchor, cursor);
        int start = range[0];
        int end = range[1];
        List<String> lines = StringUtils.splitLinesRaw(safeText);
        int y = textY;
        int index = 0;
        int firstLine = Math.max(0, scrollLines);
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            int lineStart = index;
            int lineEnd = lineStart + line.length();
            if (lineIdx >= firstLine) {
                if (y > maxY) {
                    break;
                }
                int selStart = Math.max(start, lineStart);
                int selEnd = Math.min(end, lineEnd);
                if (selEnd > selStart) {
                    int sx = textX + textRenderer.getWidth(line.substring(0, selStart - lineStart));
                    int ex = textX + textRenderer.getWidth(line.substring(0, selEnd - lineStart));
                    context.fill(sx, y, ex, y + 9, 0x704A7CC7);
                }
                y += lineHeight;
            }
            index = lineEnd + 1;
        }
    }
}
