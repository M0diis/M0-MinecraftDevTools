package me.m0dii.gui.local;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class ListPanel {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public ListPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, TextRenderer textRenderer, List<String> rows, int selectedIndex, int startIndex, int rowHeight) {
        UiForms.drawPanel(context, x, y, width, height);
        if (rows == null || rows.isEmpty()) {
            return;
        }

        int drawY = y + 4;
        int endY = y + height - 6;
        for (int i = startIndex; i < rows.size() && drawY <= endY; i++) {
            boolean selected = i == selectedIndex;
            if (selected) {
                context.fill(x + 2, drawY - 1, x + width - 2, drawY + rowHeight - 2, 0x402B5C85);
            }
            context.drawTextWithShadow(textRenderer, rows.get(i), x + 6, drawY, selected ? 0xFFDDF1FF : UiTheme.TEXT_MUTED);
            drawY += rowHeight;
        }
    }
}

