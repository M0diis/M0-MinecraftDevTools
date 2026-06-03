package bettercommandblockui.main.ui;

import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import static bettercommandblockui.main.BetterCommandBlockUI.*;

public class NotchedSlider extends ClickableWidget {
    @Setter
    protected int subdivisions = 4;
    protected double pos = 0.0d;
    protected boolean dragging = false;
    protected int length;
    protected double prevMouseX = 0.0d;
    protected double prevMouseY = 0.0d;

    @Setter
    protected java.util.function.Consumer<Double> changedListener;

    public NotchedSlider(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.length = width;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        //RenderSystem.setShaderTexture(0, SLIDER);
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLIDER,
                512,
                16,
                0,
                0,
                getX() - 2,
                getY(),
                4,
                16
        );
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLIDER,
                512,
                16,
                4,
                0,
                getX() + 2,
                getY(),
                getWidth() - 4,
                16
        );
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLIDER,
                512,
                16,
                508,
                0,
                getX() + getWidth() - 2,
                getY(),
                4,
                16
        );

        //RenderSystem.setShaderTexture(0, SLIDER_NOTCH);
        float step = 1.0f / ((float) subdivisions);
        for (int i = 1; i < subdivisions; i++) {
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    SLIDER_NOTCH,
                    4,
                    16,
                    0,
                    0,
                    (int) (getX() + (i * step * getWidth())) - 2,
                    getY(),
                    4,
                    16
            );
        }

        //RenderSystem.setShaderTexture(0, SLIDER_PICK);
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                SLIDER_PICK.get(true, hovered),
                (int) (getX() + (pos * getWidth()) - 4),
                getY(),
                8,
                16
        );
    }

    public double getValue() {
        return pos;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }

    public void setPos(double value) {
        pos = snap(Math.clamp(value, 0.0d, 1.0d), 1.0d / subdivisions);
    }

    private boolean checkHovered(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + length && mouseY < this.getY() + this.height;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.isValidClickButton(click.buttonInfo()) && checkHovered(click.x(), click.y()) && this.visible) {
            this.dragging = true;
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            this.onClick(click, doubled);
            return true;
        }
        this.dragging = false;
        return false;
    }

    @Override
    public void onRelease(Click click) {
        if (this.visible && this.isValidClickButton(click.buttonInfo())) {
            this.dragging = false;
            super.onRelease(click);
        }
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (!this.visible || !checkHovered(click.x(), click.y())) {
            dragging = false;
            return;
        }
        dragging = true;
        prevMouseX = click.x();
        prevMouseY = click.y();

        pos = snap(Math.clamp((click.x() - getX()) / length, 0.0d, 1.0d), 1.0d / subdivisions);
        if (changedListener != null) {
            changedListener.accept(pos);
        }
    }

    @Override
    public void onDrag(Click click, double distX, double distY) {
        if (dragging) {
            double posBefore = pos;
            pos = snap(Math.clamp((click.x() - getX()) / length, 0.0d, 1.0d), 1.0d / subdivisions);

            if (changedListener != null && Math.abs(posBefore) > 0.0) {
                changedListener.accept(pos);
            }
        }
    }

    double snap(double x, double step) {
        return Math.round(x / step) * step;
    }
}
