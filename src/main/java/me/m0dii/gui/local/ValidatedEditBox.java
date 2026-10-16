package me.m0dii.gui.local;

import bettercommandblockui.mixin.TextFieldWidgetAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import java.util.function.Predicate;

/** Restores candidate validation for numeric fields after vanilla removed EditBox filters. */
public final class ValidatedEditBox extends EditBox {
    private Predicate<String> filter = value -> true;

    public ValidatedEditBox(Font font, int x, int y, int width, int height, Component title) {
        super(font, x, y, width, height, title);
    }

    public void setFilter(Predicate<String> filter) { this.filter = java.util.Objects.requireNonNull(filter); }

    @Override
    public void setValue(String value) {
        if (filter == null || filter.test(value)) super.setValue(value);
    }

    @Override
    public void insertText(String input) {
        TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor) (Object) this;
        int start = Math.min(getCursorPosition(), accessor.getHighlightPos());
        int end = Math.max(getCursorPosition(), accessor.getHighlightPos());
        String insertion = StringUtil.filterText(input);
        int remaining = accessor.invokeGetMaxLength() - getValue().length() + end - start;
        if (remaining <= 0) return;
        if (insertion.length() > remaining) insertion = insertion.substring(0, remaining);
        String candidate = new StringBuilder(getValue()).replace(start, end, insertion).toString();
        if (filter.test(candidate)) super.insertText(input);
    }
}
