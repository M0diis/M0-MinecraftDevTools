package me.m0dii.nbteditor.multiversion;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A wrapper for MutableText, since it is changed from an interface (1.18) to a class (1.19)
 */
public class EditableText implements Text {

    private final MutableText value;

    public EditableText(MutableText value) {
        this.value = value;
    }

    public MutableText getInternalValue() {
        return value;
    }

    // Text
    @Override
    public OrderedText asOrderedText() {
        return value.asOrderedText();
    }

    @Override
    public TextContent getContent() {
        return value.getContent();
    }

    @Override
    public List<Text> getSiblings() {
        return value.getSiblings();
    }

    @Override
    public Style getStyle() {
        return value.getStyle();
    }

    // Mutable Text
    public EditableText setStyle(Style style) {
        return value.setStyle(style) == value ? this : new EditableText(value.setStyle(style));
    }

    public EditableText append(String text) {
        return value.append(text) == value ? this : new EditableText(value.append(text));
    }

    public EditableText append(Text text) {
        return value.append(text) == value ? this : new EditableText(value.append(text));
    }

    public EditableText styled(UnaryOperator<Style> styleUpdater) {
        return value.styled(styleUpdater) == value ? this : new EditableText(value.styled(styleUpdater));
    }

    public EditableText fillStyle(Style styleOverride) {
        return value.fillStyle(styleOverride) == value ? this : new EditableText(value);
    }

    public EditableText formatted(Formatting... formattings) {
        return value.formatted(formattings) == value ? this : new EditableText(value.formatted(formattings));
    }

    // Other
    @Override
    public boolean equals(Object obj) {
        try {
            return (boolean) Object.class.getMethod("equals", Object.class).invoke(value, obj);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking equals method", e);
        }
    }

}
