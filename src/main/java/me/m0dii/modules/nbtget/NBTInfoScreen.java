package me.m0dii.modules.nbtget;

import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.screens.OverlaySupportingScreen;
import me.m0dii.nbteditor.screens.widgets.MultiLineTextFieldWidget;
import me.m0dii.nbteditor.util.NbtFormatter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class NBTInfoScreen extends OverlaySupportingScreen {

    private final Screen parent;
    private final NbtFormatter.Impl formatter;
    private String text;
    private MultiLineTextFieldWidget textArea;

    public NBTInfoScreen(Screen parent, String text, NbtFormatter.Impl formatter) {
        super(TextInst.of("NBT"));
        this.parent = parent;
        this.text = text;
        this.formatter = formatter;
    }

    @Override
    protected void init() {
        super.init();

        textArea = addDrawableChild(MultiLineTextFieldWidget.create(textArea, 20, 20, width - 40, height - 70, text, formatter == null ? null : str -> {
            NbtFormatter.FormatterResult formattedText = formatter.formatSafely(str);
            return formattedText.text();
        }, true, newText -> text = newText));

        setInitialFocus(textArea);
    }

    @Override
    public void renderMain(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);
        super.renderMain(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getOverlay() == null && textArea.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

}
