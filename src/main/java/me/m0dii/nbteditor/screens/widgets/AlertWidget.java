package me.m0dii.nbteditor.screens.widgets;

import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AlertWidget extends GroupWidget implements InitializableOverlay<Screen> {

    private final Runnable onClose;
    private final Text[] lines;
    private int x;
    private int y;

    public AlertWidget(Runnable onClose, Text... lines) {
        this.onClose = onClose;
        this.lines = lines;
    }

    @Override
    public void init(Screen parent, int width, int height) {
        clearWidgets();

        x = width / 2;
        y = height / 2 - lines.length * MiscUtil.client.textRenderer.fontHeight / 2;

        addWidget(MVMisc.newButton(width / 2 - 50, height - 28, 100, 20, TextInst.translatable("nbteditor.ok"), btn -> {
            onClose.run();
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.renderBackground(MiscUtil.client.currentScreen, matrices);
        for (int i = 0; i < lines.length; i++) {
            DrawableHelper.drawCenteredTextWithShadow(matrices, MiscUtil.client.textRenderer, lines[i],
                    x, y + i * MiscUtil.client.textRenderer.fontHeight, -1);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            onClose.run();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

}
