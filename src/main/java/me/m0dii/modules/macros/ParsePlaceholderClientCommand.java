package me.m0dii.modules.macros;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ParsePlaceholderClientCommand {
    private ParsePlaceholderClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("parseplaceholder")
                        .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                                .suggests(ParsePlaceholderClientCommand::suggestPlaceholders)
                                .executes(ParsePlaceholderClientCommand::execute))));
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }
        String input = StringArgumentType.getString(context, "input");
        String expanded = MacroPlaceholders.expand(client, input);
        client.player.sendMessage(Text.literal("[Placeholder] " + expanded), false);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestPlaceholders(CommandContext<FabricClientCommandSource> context,
                                                                      SuggestionsBuilder builder) {
        String lower = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String token : MacroPlaceholders.getKnownPlaceholderTokens()) {
            String wrapped = "{" + token + "}";
            if (wrapped.toLowerCase(Locale.ROOT).contains(lower) || lower.isBlank()) {
                builder.suggest(wrapped);
            }
        }
        return builder.buildFuture();
    }
}

