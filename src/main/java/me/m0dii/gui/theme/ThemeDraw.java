package me.m0dii.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Drawing primitives for the flat theme: softly rounded boxes, outlines, switches and text that never overflows.
 */
public final class ThemeDraw {
    private static final String ELLIPSIS = "…";

    private ThemeDraw() {
    }

    /** Filled box with 1px-cut corners, which reads as slightly rounded at Minecraft's pixel scale. */
    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (w < 3 || h < 3) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** 1px outline that follows {@link #box}'s rounded corners. */
    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w < 3 || h < 3) {
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + 1, color);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Raised card: surface fill with a subtle border. */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, Theme.SURFACE);
        outline(g, x, y, w, h, Theme.BORDER);
    }

    public static void hDivider(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, Theme.DIVIDER);
    }

    public static void vDivider(GuiGraphicsExtractor g, int x, int y, int h) {
        g.fill(x, y, x + 1, y + h, Theme.DIVIDER);
    }

    /** Small uppercase-style label used above groups of controls. */
    public static void sectionLabel(GuiGraphicsExtractor g, Font font, String text, int x, int y, int maxWidth) {
        g.text(font, fit(font, text, maxWidth), x, y, Theme.TEXT_DIM, false);
    }

    /** Pill switch. {@code w} should be about twice {@code h}. */
    public static void toggle(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean on, boolean enabled) {
        int track = on ? (enabled ? Theme.ACCENT : Theme.ACCENT_PRESSED) : Theme.SWITCH_OFF;
        box(g, x, y, w, h, track);
        int knob = h - 4;
        int knobX = on ? x + w - knob - 2 : x + 2;
        box(g, knobX, y + 2, knob, knob, enabled ? Theme.KNOB : Theme.TEXT_DIM);
    }

    /** Truncates {@code text} with an ellipsis so it fits {@code maxWidth}. */
    public static String fit(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsisW = font.width(ELLIPSIS);
        if (ellipsisW > maxWidth) {
            return "";
        }
        return font.plainSubstrByWidth(text, maxWidth - ellipsisW) + ELLIPSIS;
    }

    public static void textCentered(GuiGraphicsExtractor g, Font font, String text, int x, int y, int w, int h, int color) {
        String shown = fit(font, text, w - 4);
        int tx = x + (w - font.width(shown)) / 2;
        int ty = y + (h - 8) / 2;
        g.text(font, shown, tx, ty, color, false);
    }

    /**
     * Centered label that shrinks (down to half size) instead of truncating when the box is too narrow,
     * for compact labels such as keycaps.
     */
    public static void textCenteredScaled(GuiGraphicsExtractor g, Font font, String text, int x, int y, int w, int h, int color) {
        int textW = font.width(text);
        int room = w - 2;
        if (textW <= room || textW == 0) {
            g.text(font, text, x + (w - textW) / 2, y + (h - 8) / 2, color, false);
            return;
        }
        float scale = Math.max(0.5f, room / (float) textW);
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2.0f, y + h / 2.0f);
        g.pose().scale(scale, scale);
        String shown = scale > 0.5f ? text : fit(font, text, (int) (room / scale));
        g.text(font, shown, -font.width(shown) / 2, -4, color, false);
        g.pose().popMatrix();
    }

    public static void textLeft(GuiGraphicsExtractor g, Font font, String text, int x, int y, int w, int h, int color) {
        g.text(font, fit(font, text, w), x, y + (h - 8) / 2, color, false);
    }
}
