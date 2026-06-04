package me.m0dii.gui.local;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public final class UiBoundIntField {
    private final TextFieldWidget widget;
    private IntConsumer saveConsumer = ignored -> {
    };
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private int defaultValue = 0;
    private Integer parsedValue = null;

    public UiBoundIntField(TextRenderer textRenderer,
                           int x,
                           int y,
                           int width,
                           int height,
                           String placeholder) {
        this.widget = new TextFieldWidget(textRenderer, x, y, width, height, Text.literal(placeholder == null ? "" : placeholder));
        this.widget.setTextPredicate(UiBoundIntField::isIntegerCandidate);
        this.widget.setChangedListener(this::handleChanged);
    }

    public UiBoundIntField setSaveConsumer(IntConsumer consumer) {
        this.saveConsumer = consumer == null ? ignored -> {
        } : consumer;
        return this;
    }

    public UiBoundIntField setMin(int min) {
        this.min = min;
        if (this.max < min) {
            this.max = min;
        }
        normalizeCurrentText();
        return this;
    }

    public UiBoundIntField setMax(int max) {
        this.max = max;
        if (this.min > max) {
            this.min = max;
        }
        normalizeCurrentText();
        return this;
    }

    public UiBoundIntField setDefaultValue(int defaultValue) {
        this.defaultValue = clamp(defaultValue);
        return this;
    }

    public UiBoundIntField setMaxLength(int maxLength) {
        this.widget.setMaxLength(Math.max(1, maxLength));
        return this;
    }

    public UiBoundIntField setEditable(boolean editable) {
        this.widget.setEditable(editable);
        return this;
    }

    public UiBoundIntField setValue(int value) {
        this.widget.setText(Integer.toString(clamp(value)));
        return this;
    }

    public int value() {
        return this.parsedValue != null ? this.parsedValue : this.defaultValue;
    }

    public boolean isValid() {
        return this.parsedValue != null || this.widget.getText().isBlank();
    }

    public String getText() {
        return this.widget.getText();
    }

    public TextFieldWidget widget() {
        return this.widget;
    }

    private void normalizeCurrentText() {
        if (this.widget.getText() == null || this.widget.getText().isBlank()) {
            return;
        }
        Integer value = parse(this.widget.getText());
        if (value == null) {
            return;
        }
        int clamped = clamp(value);
        if (clamped != value) {
            this.widget.setText(Integer.toString(clamped));
        }
    }

    private void handleChanged(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            this.parsedValue = null;
            return;
        }
        Integer parsed = parse(value);
        if (parsed == null) {
            this.parsedValue = null;
            return;
        }
        int clamped = clamp(parsed);
        this.parsedValue = clamped;
        this.saveConsumer.accept(clamped);
    }

    private int clamp(int value) {
        return Math.clamp(value, this.min, this.max);
    }

    private static Integer parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isIntegerCandidate(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if ("-".equals(value)) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i == 0 && c == '-') {
                continue;
            }
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
