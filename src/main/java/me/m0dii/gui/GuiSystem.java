package me.m0dii.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class GuiSystem {

    private GuiSystem() {
    }

    public static final class Colors {
        public static final int PANEL_BG = 0xEE1A1A1A;
        public static final int PANEL_TOP = 0x80FFFFFF;
        public static final int BUTTON_BG = 0xFF2A2A2A;
        public static final int BUTTON_BG_HOVER = 0xFF404040;
        public static final int BUTTON_TOP = 0x60FFFFFF;
        public static final int BUTTON_TOP_HOVER = 0x90FFFFFF;
        public static final int BUTTON_BOTTOM = 0x70000000;
        public static final int BUTTON_BOTTOM_HOVER = 0x90000000;
        public static final int BUTTON_TEXT = 0xFFFFFFFF;
        public static final int BUTTON_TEXT_HOVER = 0xFFFFFFAA;

        private Colors() {
        }
    }

    public static boolean contains(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return x >= boxX && x <= boxX + boxW && y >= boxY && y <= boxY + boxH;
    }

    public static void drawPanel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, Colors.PANEL_BG);
        context.fill(x, y, x + w, y + 1, Colors.PANEL_TOP);
    }

    public static void drawOutline(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
    }

    public static void drawButton(DrawContext context,
                                  TextRenderer textRenderer,
                                  int x,
                                  int y,
                                  int w,
                                  int h,
                                  String label,
                                  boolean hovered,
                                  boolean active) {
        int bg = hovered ? Colors.BUTTON_BG_HOVER : Colors.BUTTON_BG;
        int top = hovered ? Colors.BUTTON_TOP_HOVER : Colors.BUTTON_TOP;
        int bottom = hovered ? Colors.BUTTON_BOTTOM_HOVER : Colors.BUTTON_BOTTOM;
        int text = hovered ? Colors.BUTTON_TEXT_HOVER : Colors.BUTTON_TEXT;
        if (!active) {
            bg = 0xFF1E1E1E;
            top = 0x30FFFFFF;
            bottom = 0x50000000;
            text = 0xFF808080;
        }

        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, top);
        context.fill(x, y + h - 1, x + w, y + h, bottom);
        context.drawCenteredTextWithShadow(textRenderer, label, x + (w / 2), y + 5, text);
    }
}

