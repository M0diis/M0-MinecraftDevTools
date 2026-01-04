package me.m0dii.nbteditor.screens.util;

import com.mojang.brigadier.suggestion.Suggestions;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.ScreenTexts;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.screens.OverlaySupportingScreen;
import me.m0dii.nbteditor.screens.widgets.MultiLineTextFieldWidget;
import me.m0dii.nbteditor.util.NbtFormatter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TextAreaScreen extends OverlaySupportingScreen {

    private final Screen parent;
    private final NbtFormatter.Impl formatter;
    private final boolean newLines;
    private final Consumer<String> onDone;
    private String text;
    private MultiLineTextFieldWidget textArea;
    private BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions;

    public TextAreaScreen(Screen parent, String text, NbtFormatter.Impl formatter, boolean newLines, Consumer<String> onDone) {
        super(TextInst.of("Text Area"));
        this.parent = parent;
        this.text = text;
        this.formatter = formatter;
        this.newLines = newLines;
        this.onDone = onDone;
    }

    public TextAreaScreen suggest(BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions) {
        this.suggestions = suggestions;
        if (textArea != null) {
            textArea.suggest(this, suggestions);
        }
        return this;
    }

    @Override
    protected void init() {
        super.init();

        ButtonWidget done;
        this.addDrawableChild(done = MVMisc.newButton(20, 20, Math.min(200, width / 2 - 25), 20, ScreenTexts.DONE, btn -> {
            onDone.accept(text);
            close();
        }));
        // When the end of the second button is near the end of the text field, it looks bad
        if (width - (done.getWidth() * 2 + 50) < 100) {
            done.setWidth(done.getWidth() * 2 / 3);
        }
        this.addDrawableChild(MVMisc.newButton(done.x + done.getWidth() + 10, 20, done.getWidth(), 20, ScreenTexts.CANCEL, btn -> close()));

        textArea = addDrawableChild(MultiLineTextFieldWidget.create(textArea, 20, 50, width - 40, height - 70, text, formatter == null ? null : str -> {
            NbtFormatter.FormatterResult formattedText = formatter.formatSafely(str);
            done.active = formattedText.isSuccess();
            return formattedText.text();
        }, newLines, newText -> text = newText));
        if (suggestions != null) {
            textArea.suggest(this, suggestions);
        }
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
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

}
