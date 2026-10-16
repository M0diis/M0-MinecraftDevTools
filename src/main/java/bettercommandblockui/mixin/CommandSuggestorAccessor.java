package bettercommandblockui.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestorAccessor {
    @Invoker("formatChat")
    FormattedCharSequence invokeProvideRenderText(String original, int firstCharacterIndex);

    @Accessor
    CommandSuggestions.SuggestionsList getSuggestions();

    @Accessor
    List<FormattedCharSequence> getCommandUsage();

    @Accessor
    static List<Style> getARGUMENT_STYLES() {
        throw new AssertionError();
    }

    @Accessor
    static Style getLITERAL_STYLE() {
        throw new AssertionError();
    }

    @Accessor
    static Style getUNPARSED_STYLE() {
        throw new AssertionError();
    }

    @Accessor
    Screen getScreen();

    @Accessor
    Font getFont();

    @Accessor
    EditBox getInput();

    @Accessor
    ParseResults<SharedSuggestionProvider> getCurrentParse();

    @Accessor
    boolean getAnchorToBottom();

    @Accessor
    boolean getKeepSuggestions();

    @Accessor
    int getFillColor();

    @Accessor
    int getCommandUsageWidth();
}
