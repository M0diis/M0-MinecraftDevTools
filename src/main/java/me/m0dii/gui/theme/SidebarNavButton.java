package me.m0dii.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * Left-aligned navigation entry: icon glyph + label, with an accent bar when selected.
 */
public class SidebarNavButton extends AbstractButton {
    private final String icon;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public SidebarNavButton(int x, int y, int width, int height, String icon, String label,
                            BooleanSupplier selected, Runnable onPress) {
        super(x, y, width, height, Component.literal(label));
        this.icon = icon;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Font font = Minecraft.getInstance().font;
        boolean isSelected = this.selected.getAsBoolean();
        boolean hovered = this.active && this.isHoveredOrFocused();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        if (isSelected) {
            ThemeDraw.box(g, x, y, w, h, Theme.ACCENT_SOFT);
            g.fill(x, y + 3, x + 2, y + h - 3, Theme.ACCENT);
        } else if (hovered) {
            ThemeDraw.box(g, x, y, w, h, Theme.SURFACE_HOVER);
        }

        int textY = y + (h - 8) / 2;
        int labelX = x + 8;
        if (this.icon != null && !this.icon.isEmpty()) {
            g.text(font, this.icon, labelX, textY, isSelected ? Theme.ACCENT_TEXT : Theme.TEXT_DIM, false);
            labelX += 14;
        }
        int labelColor = isSelected ? Theme.TEXT : hovered ? Theme.TEXT : Theme.TEXT_MUTED;
        g.text(font, ThemeDraw.fit(font, getMessage().getString(), x + w - labelX - 4), labelX, textY, labelColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
