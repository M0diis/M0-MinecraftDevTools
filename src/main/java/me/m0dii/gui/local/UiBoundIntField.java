package me.m0dii.gui.local;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public final class UiBoundIntField {
    private final ValidatedEditBox widget;
    private IntConsumer saveConsumer = ignored -> {
    };
    private int min = Integer.MIN_VALUE;
    private int max = Integer.MAX_VALUE;
    private int defaultValue = 0;
    private Integer parsedValue = null;

    public UiBoundIntField(Font textRenderer,
                           int x,
                           int y,
                           int width,
                           int height,
                           String placeholder) {
        this.widget = new ValidatedEditBox(textRenderer, x, y, width, height, Component.literal(placeholder == null ? "" : placeholder));
        this.widget.setFilter(UiBoundIntField::isIntegerCandidate);
        this.widget.setResponder(this::handleChanged);
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
        this.widget.setValue(Integer.toString(clamp(value)));
        return this;
    }

    public int value() {
        return this.parsedValue != null ? this.parsedValue : this.defaultValue;
    }

    public boolean isValid() {
        return this.parsedValue != null || this.widget.getValue().isBlank();
    }

    public String getText() {
        return this.widget.getValue();
    }

    public EditBox widget() {
        return this.widget;
    }

    private void normalizeCurrentText() {
        if (this.widget.getValue() == null || this.widget.getValue().isBlank()) {
            return;
        }
        Integer value = parse(this.widget.getValue());
        if (value == null) {
            return;
        }
        int clamped = clamp(value);
        if (clamped != value) {
            this.widget.setValue(Integer.toString(clamped));
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
