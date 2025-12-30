package me.m0dii.nbteditor.screens.util;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.IgnoreCloseScreenPacket;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class FancyConfirmScreen extends ConfirmScreen implements IgnoreCloseScreenPacket {

    private Screen parent;

    public FancyConfirmScreen(BooleanConsumer callback, Text title, Text message, Text yesTranslated, Text noTranslated) {
        super(callback, title, message, yesTranslated, noTranslated);
        parent = MiscUtil.client.currentScreen;
    }

    public FancyConfirmScreen(BooleanConsumer callback, Text title, Text message) {
        super(callback, title, message);
        parent = MiscUtil.client.currentScreen;
    }

    public FancyConfirmScreen setParent(Screen parent) {
        this.parent = parent;
        return this;
    }

    @Override
    protected void init() {
        if (parent != null) {
            parent.init(client, width, height);
        }

        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(matrices, -314, -314, delta);
        }

        matrices.push();
        matrices.translate(0.0, 0.0, 500.0);
        DrawableHelper.superRender(FancyConfirmScreen.class, this, matrices, mouseX, mouseY, delta);
        matrices.pop();
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MiscUtil.client.world == null) {
            super.renderBackground(context, mouseX, mouseY, delta);
        } else {
            renderInGameBackground(context);
        }
    }

}
