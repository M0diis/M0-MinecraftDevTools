package me.m0dii.nbteditor.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SummonableEntityArgumentType implements ArgumentType<EntityType<?>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");

    public static SummonableEntityArgumentType summonableEntity() {
        return new SummonableEntityArgumentType();
    }

    public EntityType<?> parse(@NotNull StringReader stringReader) throws CommandSyntaxException {
        return MVRegistry.ENTITY_TYPE.getOrEmpty(Identifier.fromCommandInput(stringReader))
                .filter(EntityType::isSummonable)
                .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(stringReader));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(MVRegistry.ENTITY_TYPE.getEntrySet().stream()
                .filter(entry -> entry.getValue().isSummonable())
                .map(Map.Entry::getKey), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}