package me.m0dii.nbteditor.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {

    private static final Collection<String> EXAMPLES = Arrays.asList("option1", "option2");
    private final Class<T> options;

    private EnumArgumentType(@NotNull Class<T> options) {
        this.options = options;
    }

    public static <T extends Enum<T>> EnumArgumentType<T> options(@NotNull Class<T> options) {
        return new EnumArgumentType<>(options);
    }

    public T parse(@NotNull StringReader stringReader) throws CommandSyntaxException {
        StringBuilder value = new StringBuilder();

        while (stringReader.canRead() && stringReader.peek() != ' ') {
            value.append(stringReader.read());
        }

        return Arrays.stream(options.getEnumConstants())
                .filter(option -> option.name().equalsIgnoreCase(value.toString()))
                .findFirst()
                .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
                        .createWithContext(stringReader));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(options.getEnumConstants()).map(T::name).map(String::toLowerCase), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

}
