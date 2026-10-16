package me.m0dii.gui;

import me.m0dii.gui.local.UiRect;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GuiSystem {

    private GuiSystem() {
    }

    public static final class Colors {
        public static final int PANEL_BG = 0xEE1A1A1A;
        public static final int PANEL_TOP = 0x80FFFFFF;
        public static final int PANEL_BOTTOM = 0x90000000;
        public static final int PANEL_SIDE = 0x50000000;

        public static final int INPUT_BG = 0xFF161616;
        public static final int INPUT_BG_FOCUSED = 0xFF0F0F0F;
        public static final int INPUT_TOP = 0x60FFFFFF;
        public static final int INPUT_TEXT = 0xFFEAEAEA;

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

    public static boolean contains(double x, double y, UiRect rect) {
        return rect != null && rect.contains(x, y);
    }

    /** Modal / popup panel in the flat theme. */
    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        ThemeDraw.box(context, x, y, w, h, Theme.SURFACE);
        ThemeDraw.outline(context, x, y, w, h, Theme.BORDER_STRONG);
    }

    public static void drawInputField(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean focused) {
        ThemeDraw.box(context, x, y, w, h, Theme.INPUT);
        ThemeDraw.outline(context, x, y, w, h, focused ? Theme.ACCENT : Theme.BORDER);
    }

    public static void drawOutline(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
    }

    public static void drawButton(GuiGraphicsExtractor context,
                                  Font textRenderer,
                                  int x,
                                  int y,
                                  int w,
                                  int h,
                                  String label,
                                  boolean hovered,
                                  boolean active) {
        boolean hot = active && hovered;
        ThemeDraw.box(context, x, y, w, h, !active ? Theme.SURFACE : hot ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED);
        ThemeDraw.outline(context, x, y, w, h, hot ? Theme.BORDER_STRONG : Theme.BORDER);
        ThemeDraw.textCentered(context, textRenderer, label, x, y, w, h, active ? Theme.TEXT : Theme.TEXT_DIM);
    }
}

