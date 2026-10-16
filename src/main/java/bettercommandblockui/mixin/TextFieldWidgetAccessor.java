package bettercommandblockui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;

@Mixin(EditBox.class)
public interface TextFieldWidgetAccessor {

    @Invoker("getMaxLength")
    int invokeGetMaxLength();

    @Invoker
    int invokeGetCursorPos(int characterOffset);

    @Accessor
    boolean getBordered();

    @Accessor
    boolean getIsEditable();

    @Accessor
    boolean getCanLoseFocus();

    @Accessor
    int getTextColor();

    @Accessor
    int getTextColorUneditable();

    @Accessor
    int getCursorPos();

    @Accessor("cursorPos")
    void setSelectionStart(int index);

    @Accessor
    int getHighlightPos();

    @Accessor("highlightPos")
    void setSelectionEnd(int index);

    @Accessor
    int getDisplayPos();

    @Accessor
    long getFocusedTime();

    @Accessor("focusedTime")
    void setLastSwitchFocusTime(long value);

    @Accessor
    String getValue();

    @Accessor("value")
    void setTextVariable(String text);

    @Accessor
    String getSuggestion();

    @Accessor
    Font getFont();

    @Accessor
    Consumer<String> getResponder();

}
