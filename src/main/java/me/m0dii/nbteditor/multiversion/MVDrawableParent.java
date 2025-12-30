package me.m0dii.nbteditor.multiversion;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.util.math.MatrixStack;

public interface MVDrawableParent {
    default void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.render((Drawable) this, matrices, mouseX, mouseY, delta);
    }
}
