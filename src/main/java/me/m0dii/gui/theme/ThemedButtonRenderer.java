package me.m0dii.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flat rendering for vanilla buttons inside {@link ThemedScreen}s. Buttons whose label ends in ": ON"/": OFF" (or YES/NO)
 * are drawn as a label with a switch, which is how this mod expresses toggles.
 */
public final class ThemedButtonRenderer {
    private static final Pattern TOGGLE = Pattern.compile("^(.*?):\\s*(ON|OFF|YES|NO)$");
    private static final int SWITCH_W = 16;
    private static final int SWITCH_H = 9;

    private ThemedButtonRenderer() {
    }

    public static void render(AbstractButton button, GuiGraphicsExtractor g) {
        Font font = Minecraft.getInstance().font;
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        boolean active = button.active;
        boolean hovered = active && button.isHoveredOrFocused();
        ThemedWidgets.Variant variant = ThemedWidgets.of(button);
        String message = button.getMessage().getString();

        Matcher toggle = TOGGLE.matcher(message);
        if (toggle.matches() && variant != ThemedWidgets.Variant.PRIMARY) {
            renderToggle(g, font, x, y, w, h, toggle.group(1).trim(), "ON".equals(toggle.group(2)) || "YES".equals(toggle.group(2)), active, hovered);
            return;
        }

        int bg;
        int border;
        int text;
        switch (variant) {
            case PRIMARY -> {
                bg = !active ? Theme.ACCENT_SOFT : hovered ? Theme.ACCENT_HOVER : Theme.ACCENT;
                border = 0;
                text = active ? 0xFFFFFFFF : Theme.TEXT_DIM;
            }
            case DANGER -> {
                bg = !active ? Theme.SURFACE : hovered ? Theme.DANGER_SOFT : Theme.SURFACE_RAISED;
                border = hovered ? Theme.DANGER : Theme.BORDER;
                text = active ? Theme.DANGER : Theme.TEXT_DIM;
            }
            case GHOST -> {
                bg = hovered ? Theme.SURFACE_HOVER : 0;
                border = 0;
                text = !active ? Theme.TEXT_DIM : hovered ? Theme.TEXT : Theme.TEXT_MUTED;
            }
            case SELECTED -> {
                bg = Theme.ACCENT_SOFT;
                border = Theme.ACCENT;
                text = Theme.ACCENT_TEXT;
            }
            default -> {
                bg = !active ? Theme.SURFACE : hovered ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED;
                border = hovered ? Theme.BORDER_STRONG : Theme.BORDER;
                text = active ? Theme.TEXT : Theme.TEXT_DIM;
            }
        }
        if (bg != 0) {
            ThemeDraw.box(g, x, y, w, h, bg);
        }
        if (border != 0) {
            ThemeDraw.outline(g, x, y, w, h, border);
        }
        ThemeDraw.textCentered(g, font, message, x, y, w, h, text);
    }

    private static void renderToggle(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                                     String label, boolean on, boolean active, boolean hovered) {
        ThemeDraw.box(g, x, y, w, h, !active ? Theme.SURFACE : hovered ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED);
        ThemeDraw.outline(g, x, y, w, h, hovered ? Theme.BORDER_STRONG : Theme.BORDER);

        int padding = 6;
        int labelColor = active ? Theme.TEXT : Theme.TEXT_DIM;
        if (w < SWITCH_W + padding * 3 + 12) {
            // Too narrow for label + switch: show the label tinted by state.
            ThemeDraw.textCentered(g, font, label, x, y, w, h, !active ? Theme.TEXT_DIM : on ? Theme.ACCENT_TEXT : Theme.TEXT_MUTED);
            return;
        }
        int switchX = x + w - padding - SWITCH_W;
        ThemeDraw.textLeft(g, font, label, x + padding, y, switchX - x - padding * 2, h, labelColor);
        ThemeDraw.toggle(g, switchX, y + (h - SWITCH_H) / 2, SWITCH_W, SWITCH_H, on, active);
    }
}
