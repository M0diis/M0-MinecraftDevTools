package me.m0dii.gui.local;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class UiBoundTextField {
    private final TextFieldWidget widget;
    private Consumer<String> saveConsumer = ignored -> {
    };

    public UiBoundTextField(TextRenderer textRenderer,
                            int x,
                            int y,
                            int width,
                            int height,
                            String placeholder) {
        this.widget = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal(placeholder == null ? "" : placeholder));
        this.widget.setChangedListener(this::handleChanged);
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
        this.widget.setText(value == null ? "" : value);
        return this;
    }

    public String getText() {
        return this.widget.getText();
    }

    public TextFieldWidget widget() {
        return this.widget;
    }

    private void handleChanged(String value) {
        this.saveConsumer.accept(value == null ? "" : value);
    }
}
