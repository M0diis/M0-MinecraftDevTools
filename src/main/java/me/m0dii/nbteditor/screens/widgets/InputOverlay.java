package me.m0dii.nbteditor.screens.widgets;

import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.screens.OverlayScreen;
import me.m0dii.nbteditor.screens.OverlaySupportingScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class InputOverlay<T> extends GroupWidget implements InitializableOverlay<Screen> {

    private final Text title;
    private final Input<T> input;
    private final Consumer<T> valueConsumer;
    private final Runnable close;
    private int x;
    private int y;
    private ButtonWidget ok;

    public InputOverlay(Text title, Input<T> input, Consumer<T> valueConsumer, Runnable close) {
        this.title = title;
        this.input = input;
        this.valueConsumer = valueConsumer;
        this.close = close;
    }

    public static <T> void show(Text title, Input<T> input, Consumer<T> valueConsumer) {
        OverlayScreen.setOverlayOrScreen(
                new InputOverlay<>(title, input, valueConsumer, () -> OverlaySupportingScreen.setOverlayStatic(null)), true);
    }

    @Override
    public void init(Screen parent, int width, int height) {
        clearWidgets();

        x = (width - input.getWidth()) / 2;
        y = (height - input.getHeight() - 24) / 2;
        input.init(x, y);
        addWidget(input);
        setFocused(input);

        ok = addWidget(MVMisc.newButton(x, y + input.getHeight() + 4,
                (input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.ok"), btn -> {
                    close.run();
                    valueConsumer.accept(input.getValue());
                }));

        addWidget(MVMisc.newButton(width / 2 + 2, y + input.getHeight() + 4,
                (input.getWidth() - 4) / 2, 20, TextInst.translatable("nbteditor.cancel"), btn -> close.run()));

        ok.active = input.isValid();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        ok.active = input.isValid();

        matrices.push();
        matrices.translate(0.0, 0.0, 500.0);
        MiscUtil.client.currentScreen.renderBackground(matrices);
        if (title != null) {
            DrawableHelper.drawCenteredTextWithShadow(matrices, MiscUtil.client.textRenderer, title,
                    x + input.getWidth() / 2, y - 4 - MiscUtil.client.textRenderer.fontHeight, -1);
        }
        super.render(matrices, mouseX, mouseY, delta);
        matrices.pop();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && ok.active) {
            close.run();
            valueConsumer.accept(input.getValue());
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public interface Input<T> extends MVDrawable, MVElement {
        void init(int x, int y);

        int getWidth();

        int getHeight();

        T getValue();

        boolean isValid();
    }

}
