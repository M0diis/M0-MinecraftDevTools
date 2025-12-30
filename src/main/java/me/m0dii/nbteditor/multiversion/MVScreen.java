package me.m0dii.nbteditor.multiversion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class MVScreen extends Screen implements OldEventBehavior, PassContainerSlotUpdates {

    protected MVScreen(Text title) {
        super(title);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.superRender(MVScreen.class, this, matrices, mouseX, mouseY, delta);
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    @Override
    public void setInitialFocus(Element element) {
        MVMisc.setInitialFocus(this, element, super::setInitialFocus);
    }

}
