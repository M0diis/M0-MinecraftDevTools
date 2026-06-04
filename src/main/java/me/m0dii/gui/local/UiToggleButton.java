package me.m0dii.gui.local;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class UiToggleButton {
    private final ButtonWidget widget;
    private final String label;
    private Consumer<Boolean> saveConsumer = ignored -> {
    };
    private boolean value;
    private String onText = "ON";
    private String offText = "OFF";

    public UiToggleButton(String label,
                          boolean initialValue,
                          int x,
                          int y,
                          int width,
                          int height) {
        this.label = label == null ? "" : label;
        this.value = initialValue;
        this.widget = ButtonWidget.builder(Text.empty(), button -> toggle())
                .dimensions(x, y, width, height)
                .build();
        syncLabel();
    }

    public UiToggleButton setSaveConsumer(Consumer<Boolean> consumer) {
        this.saveConsumer = consumer == null ? ignored -> {
        } : consumer;
        return this;
    }

    public UiToggleButton setLabels(String onText, String offText) {
        this.onText = onText == null ? "ON" : onText;
        this.offText = offText == null ? "OFF" : offText;
        syncLabel();
        return this;
    }

    public UiToggleButton setValue(boolean value) {
        this.value = value;
        syncLabel();
        return this;
    }

    public boolean value() {
        return this.value;
    }

    public ButtonWidget widget() {
        return this.widget;
    }

    private void toggle() {
        this.value = !this.value;
        this.saveConsumer.accept(this.value);
        syncLabel();
    }

    private void syncLabel() {
        this.widget.setMessage(Text.literal(this.label + ": " + (this.value ? this.onText : this.offText)));
    }
}
