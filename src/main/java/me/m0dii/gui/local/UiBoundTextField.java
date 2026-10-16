package me.m0dii.gui.local;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class UiBoundTextField {
    private final EditBox widget;
    private Consumer<String> saveConsumer = ignored -> {
    };

    public UiBoundTextField(Font textRenderer,
                            int x,
                            int y,
                            int width,
                            int height,
                            String placeholder) {
        this.widget = new EditBox(textRenderer, x, y, width, height, Component.literal(placeholder == null ? "" : placeholder));
        this.widget.setResponder(this::handleChanged);
    }

    public UiBoundTextField setSaveConsumer(Consumer<String> consumer) {
        this.saveConsumer = consumer == null ? ignored -> {
        } : consumer;
        return this;
    }

    public UiBoundTextField setMaxLength(int maxLength) {
        this.widget.setMaxLength(Math.max(1, maxLength));
        return this;
    }

    public UiBoundTextField setEditable(boolean editable) {
        this.widget.setEditable(editable);
        return this;
    }

    public UiBoundTextField setText(String value) {
        this.widget.setValue(value == null ? "" : value);
        return this;
    }

    public String getText() {
        return this.widget.getValue();
    }

    public EditBox widget() {
        return this.widget;
    }

    private void handleChanged(String value) {
        this.saveConsumer.accept(value == null ? "" : value);
    }
}
