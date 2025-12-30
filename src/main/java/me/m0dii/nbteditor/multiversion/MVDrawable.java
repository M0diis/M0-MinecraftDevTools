package me.m0dii.nbteditor.multiversion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;

public interface MVDrawable extends Drawable {
    @Override
    default void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    void render(MatrixStack matrices, int mouseX, int mouseY, float delta);
}
