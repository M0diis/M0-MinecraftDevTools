package me.m0dii.nbteditor.fancytext;

import me.m0dii.nbteditor.util.StyleUtil;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public record FancyTextFormattingNode(Formatting formatting) implements FancyTextNode {

    @Override
    public Style modifyStyle(Style style) {
        if (formatting == Formatting.RESET) {
            return StyleUtil.RESET_STYLE.withParent(style);
        }

        return style.withFormatting(formatting);
    }

    @Override
    public int getNumberOfTextNodes() {
        return 0;
    }

}
