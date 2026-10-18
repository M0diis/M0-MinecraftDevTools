package me.m0dii.mixin.nbtcomplete;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.nbtcomplete.NbtCommandSuggestions;
import net.minecraft.commands.arguments.CompoundTagArgument;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

/**
 * Vanilla has no suggestions for SNBT arguments ({@code /summon ... {}}, {@code /data merge ...}). Server
 * commands without a custom suggestion provider are completed on the client by the argument type itself, so
 * adding {@code listSuggestions} here gives key and value completion for them.
 */
@Mixin(CompoundTagArgument.class)
public abstract class CompoundTagArgumentMixin {
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return NbtCommandSuggestions.compound(builder);
    }
}
