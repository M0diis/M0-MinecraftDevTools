package me.m0dii.nbteditor.multiversion;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public interface MVScreenParent {
    default void renderBackground(MatrixStack matrices) {
        DrawableHelper.renderBackground((Screen) this, matrices);
    }
}
