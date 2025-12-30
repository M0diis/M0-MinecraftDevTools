package me.m0dii.nbteditor.multiversion;

import me.m0dii.nbteditor.mixin.TextFieldWidgetMixin;
import me.m0dii.nbteditor.screens.Tickable;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;

public class MVTextFieldWidget extends TextFieldWidget implements Tickable, MVElement {

    /**
     * The selection highlight doesn't move when {@link MatrixStack#translate(double, double, double)} is called <br />
     * Via {@link TextFieldWidgetMixin}, the vertex calls are redirected to take this matrix into account
     * As of 1.19.4, this is fixed
     */
    public static MVMatrix4f matrix;

    protected MVTooltip tooltip;

    public MVTextFieldWidget(int x, int y, int width, int height, TextFieldWidget copyFrom) {
        super(MiscUtil.client.textRenderer, x, y, width, height, copyFrom, TextInst.of(""));
    }

    public MVTextFieldWidget(int x, int y, int width, int height) {
        super(MiscUtil.client.textRenderer, x, y, width, height, TextInst.of(""));
    }

    public MVTextFieldWidget tooltip(MVTooltip tooltip) {
        this.tooltip = tooltip;
        setTooltip(tooltip == null ? null : tooltip.toNewTooltip());
        return this;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        try {
            matrix = MVMatrix4f.getPositionMatrix(matrices.peek()).copy();
            DrawableHelper.superRender(MVTextFieldWidget.class, this, matrices, mouseX, mouseY, delta);
        } finally {
            matrix = null;
        }
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    @Override
    @Deprecated
    public boolean isFocused() {
        return isMultiFocused();
    }

    @Override
    @Deprecated
    public void setFocused(boolean focused) {
        setMultiFocused(focused);
    }
}
