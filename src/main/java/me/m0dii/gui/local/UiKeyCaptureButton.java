package me.m0dii.gui.local;

import lombok.Getter;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;
import java.util.function.IntConsumer;

public final class UiKeyCaptureButton {
    private final ButtonWidget widget;
    private final String label;
    private IntConsumer saveConsumer = ignored -> {
    };
    private Function<Integer, String> labelFormatter = code -> Integer.toString(code);
    private int value;
    @Getter
    private boolean capturing = false;

    public UiKeyCaptureButton(String label,
                              int initialValue,
                              int x,
                              int y,
                              int width,
                              int height) {
        this.label = label == null ? "" : label;
        this.value = initialValue;
        this.widget = ButtonWidget.builder(Text.empty(), button -> {
                    this.capturing = !this.capturing;
                    syncLabel();
                })
                .dimensions(x, y, width, height)
                .build();
        syncLabel();
    }

    public UiKeyCaptureButton setSaveConsumer(IntConsumer consumer) {
        this.saveConsumer = consumer == null ? ignored -> {
        } : consumer;
        return this;
    }

    public UiKeyCaptureButton setLabelFormatter(Function<Integer, String> formatter) {
        this.labelFormatter = formatter == null ? code -> Integer.toString(code) : formatter;
        syncLabel();
        return this;
    }

    public UiKeyCaptureButton setValue(int value) {
        this.value = value;
        syncLabel();
        return this;
    }

    public int value() {
        return this.value;
    }

    public void cancelCapture() {
        this.capturing = false;
        syncLabel();
    }

    public ButtonWidget widget() {
        return this.widget;
    }

    public boolean handleKeyPressed(int keyCode) {
        if (!this.capturing) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            this.value = -1;
        } else {
            this.value = keyCode;
        }
        this.capturing = false;
        this.saveConsumer.accept(this.value);
        syncLabel();
        return true;
    }

    private void syncLabel() {
        String suffix = this.capturing ? "Press key..." : this.labelFormatter.apply(this.value);
        this.widget.setMessage(Text.literal(this.label + ": " + suffix));
    }
}
