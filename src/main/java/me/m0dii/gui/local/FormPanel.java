package me.m0dii.gui.local;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FormPanel {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public FormPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public FormPanel(UiRect bounds) {
        this(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    public void extractRenderState(GuiGraphicsExtractor context) {
        UiForms.drawPanel(context, x, y, width, height);
    }

    public void drawLabel(GuiGraphicsExtractor context, Font textRenderer, String label, int offsetX, int offsetY) {
        context.text(textRenderer, label, x + offsetX, y + offsetY, UiTheme.TEXT_MUTED);
    }
}


