package bettercommandblockui.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractCommandBlockEditScreen.class)
public interface AbstractCommandBlockScreenAccessor {
    @Accessor
    CommandSuggestions getCommandSuggestions();

    @Accessor("commandSuggestions")
    void setCommandSuggestor(CommandSuggestions suggestor);

    @Invoker("onEdited")
    void invokeOnCommandChanged(String text);
}
