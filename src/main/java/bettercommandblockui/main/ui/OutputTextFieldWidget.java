package bettercommandblockui.main.ui;

import bettercommandblockui.mixin.TextFieldWidgetAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class OutputTextFieldWidget extends EditBox {

    private TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor) this;

    public OutputTextFieldWidget(Font textRenderer, int width, int height, Component text) {
        super(textRenderer, width, height, text);
    }

    public OutputTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x, y, width, height, text);
    }

    public OutputTextFieldWidget(Font textRenderer, int x, int y, int width, int height, @Nullable EditBox copyFrom, Component text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        super.onClick(click, doubled);
    }

    @Override
    public void setFocused(boolean focused) {
        if (accessor.getCanLoseFocus() || focused) {
            super.setFocused(focused);
            if (focused) {
                accessor.setLastSwitchFocusTime(Util.getMillis());
                accessor.setSelectionStart(0);
                accessor.setSelectionEnd(accessor.getValue().length());
            }
        }
        if (!this.isFocused()/* || !this.isNarratable()*/) {
            accessor.setSelectionStart(0);
            accessor.setSelectionEnd(0);
        }
    }
}
