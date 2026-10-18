package me.m0dii.mixin.nbtcomplete;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.nbtcomplete.NbtCommandSuggestions;
import net.minecraft.commands.arguments.NbtPathArgument;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

/** Adds key suggestions to NBT paths ({@code /data get entity @s equipment.mainhand}). */
@Mixin(NbtPathArgument.class)
public abstract class NbtPathArgumentMixin {
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return NbtCommandSuggestions.path(builder);
    }
}
