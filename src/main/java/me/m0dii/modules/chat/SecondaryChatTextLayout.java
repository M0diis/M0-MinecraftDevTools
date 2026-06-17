package me.m0dii.modules.chat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class SecondaryChatTextLayout {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    private SecondaryChatTextLayout() {
    }

    record Layout(SecondaryChatSettings.TabConfig tab,
                  List<VisibleLine> visibleLines,
                  int totalLines,
                  int maxVisible,
                  int maxScroll,
                  double scroll,
                  int x,
                  int y,
                  int contentBottom,
                  int lineHeight,
                  int scaledWidth) {
    }

    record VisibleLine(SecondaryChatManager.ChatMessage message,
                       OrderedText text,
                       int x,
                       int y,
                       int width,
                       int height) {
        boolean contains(double logicalMouseX, double logicalMouseY) {
            return logicalMouseX >= x
                    && logicalMouseX <= x + Math.max(1, width)
                    && logicalMouseY >= y
                    && logicalMouseY < y + height;
        }
    }

    record Hit(VisibleLine line, Style style) {
    }

    static Layout layout(SecondaryChatWindowLayout.Frame frame, TextRenderer textRenderer) {
        SecondaryChatSettings.TabConfig selected = SecondaryChatManager.selectedTab(frame.window);
        if (selected == null) {
            return emptyLayout(null, frame);
        }

        List<WrappedLine> wrappedLines = new ArrayList<>();
        int scaledW = Math.max(20, Math.round(frame.contentWidth() / frame.scale));
        for (SecondaryChatManager.ChatMessage message : SecondaryChatManager.snapshot(frame.window.id, selected.id)) {
            MutableText display = displayText(message, frame.window.showTimestamps, frame.window.compactRepeats);
            for (OrderedText line : textRenderer.wrapLines(display, scaledW)) {
                wrappedLines.add(new WrappedLine(message, line, textRenderer.getWidth(line)));
            }
        }

        int x = Math.round(frame.contentX() / frame.scale);
        int y = Math.round(frame.contentY() / frame.scale);
        int contentBottom = Math.round((frame.contentY() + frame.contentHeight()) / frame.scale);
        int lineHeight = Math.max(6, frame.lineHeight);
        int maxVisible = Math.max(1, (contentBottom - y) / lineHeight);
        int maxScroll = Math.max(0, wrappedLines.size() - maxVisible);
        double scroll = Math.clamp(SecondaryChatManager.scrollOffset(frame.window.id, selected.id), 0.0, maxScroll);
        int lineScroll = (int) Math.ceil(scroll);
        double fractionalOffset = scroll == 0.0 ? 0.0 : -(lineScroll - scroll) * lineHeight;

        List<VisibleLine> visibleLines = new ArrayList<>();
        int start = Math.max(0, wrappedLines.size() - maxVisible - lineScroll);
        int end = Math.min(wrappedLines.size(), start + maxVisible + (fractionalOffset < 0 ? 1 : 0));
        int drawY = y + (int) Math.round(fractionalOffset);
        for (int i = start; i < end; i++) {
            if (drawY + lineHeight > contentBottom) {
                break;
            }
            WrappedLine wrapped = wrappedLines.get(i);
            visibleLines.add(new VisibleLine(wrapped.message, wrapped.text, x, drawY, wrapped.width, lineHeight));
            drawY += lineHeight;
        }

        return new Layout(selected, visibleLines, wrappedLines.size(), maxVisible, maxScroll, scroll,
                x, y, contentBottom, lineHeight, scaledW);
    }

    static Hit hitAt(SecondaryChatWindowLayout.Frame frame,
                     TextRenderer textRenderer,
                     double mouseX,
                     double mouseY) {
        if (mouseX < frame.contentX()
                || mouseX > frame.contentX() + frame.contentWidth()
                || mouseY < frame.contentY()
                || mouseY > frame.contentY() + frame.contentHeight()) {
            return null;
        }

        Layout layout = layout(frame, textRenderer);
        double logicalMouseX = mouseX / frame.scale;
        double logicalMouseY = mouseY / frame.scale;
        for (VisibleLine line : layout.visibleLines()) {
            if (!line.contains(logicalMouseX, logicalMouseY)) {
                continue;
            }

            int localX = Math.max(0, (int) Math.floor(logicalMouseX - line.x()));
            return new Hit(line, styleAt(textRenderer, line.text(), localX));
        }
        return null;
    }

    static MutableText displayText(SecondaryChatManager.ChatMessage message,
                                   boolean showTimestamp,
                                   boolean compactRepeats) {
        MutableText display = Text.empty();
        if (showTimestamp) {
            display.append(Text.literal("[" + TIME_FORMAT.format(message.receivedAt()) + "] ")
                    .formatted(Formatting.DARK_GRAY));
        }
        display.append(message.text().copy());
        if (compactRepeats && message.repeats() > 1) {
            display.append(Text.literal(" x" + message.repeats()).formatted(Formatting.GRAY));
        }
        return display;
    }

    private static Layout emptyLayout(SecondaryChatSettings.TabConfig tab, SecondaryChatWindowLayout.Frame frame) {
        int x = Math.round(frame.contentX() / frame.scale);
        int y = Math.round(frame.contentY() / frame.scale);
        int contentBottom = Math.round((frame.contentY() + frame.contentHeight()) / frame.scale);
        int lineHeight = Math.max(6, frame.lineHeight);
        int maxVisible = Math.max(1, (contentBottom - y) / lineHeight);
        int scaledW = Math.max(20, Math.round(frame.contentWidth() / frame.scale));
        return new Layout(tab, List.of(), 0, maxVisible, 0, 0, x, y, contentBottom, lineHeight, scaledW);
    }

    private static Style styleAt(TextRenderer textRenderer, OrderedText line, int localX) {
        final int[] cursorX = {0};
        final Style[] result = {Style.EMPTY};
        line.accept((index, style, codePoint) -> {
            int charWidth = Math.max(1, textRenderer.getWidth(OrderedText.styled(codePoint, style)));
            if (localX >= cursorX[0] && localX < cursorX[0] + charWidth) {
                result[0] = style;
                return false;
            }
            cursorX[0] += charWidth;
            return true;
        });
        return result[0];
    }

    private record WrappedLine(SecondaryChatManager.ChatMessage message,
                               OrderedText text,
                               int width) {
    }
}
