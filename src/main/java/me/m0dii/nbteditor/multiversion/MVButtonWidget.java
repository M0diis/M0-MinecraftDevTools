package me.m0dii.nbteditor.multiversion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class MVButtonWidget extends PressableWidget {

    private final PressAction onPress;
    private final MVTooltip tooltip;

    public MVButtonWidget(int x, int y, int width, int height, Text text, PressAction onPress, MVTooltip tooltip) {
        super(x, y, width, height, text);
        this.onPress = onPress;
        this.tooltip = tooltip;

        if (tooltip != null) {
            setTooltip(tooltip.toNewTooltip());
        }
    }

    public MVButtonWidget(int x, int y, int width, int height, Text text, PressAction onPress) {
        this(x, y, width, height, text, onPress, null);
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.superRender(MVButtonWidget.class, this, matrices, mouseX, mouseY, delta);
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderWidget(DrawableHelper.getDrawContext(matrices), mouseX, mouseY, delta);
    }

    @Override
    protected final void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        renderButton(DrawableHelper.getMatrices(context), mouseX, mouseY, delta);
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(MVButtonWidget button);
    }

}
