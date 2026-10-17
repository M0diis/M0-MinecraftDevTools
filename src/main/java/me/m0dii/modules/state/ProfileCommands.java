package me.m0dii.modules.state;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** {@code /m0profile save|load|delete|list|next} */
public final class ProfileCommands {
    private ProfileCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("m0profile")
                        .executes(ProfileCommands::list)
                        .then(ClientCommands.literal("list").executes(ProfileCommands::list))
                        .then(ClientCommands.literal("next").executes(ctx -> reply(ctx, ModuleStateStore.cycleProfile())))
                        .then(ClientCommands.literal("save")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(ProfileCommands::suggestProfiles)
                                        .executes(ctx -> reply(ctx, ModuleStateStore.saveProfile(StringArgumentType.getString(ctx, "name"))))))
                        .then(ClientCommands.literal("load")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(ProfileCommands::suggestProfiles)
                                        .executes(ctx -> reply(ctx, ModuleStateStore.loadProfile(StringArgumentType.getString(ctx, "name"))))))
                        .then(ClientCommands.literal("delete")
                                .then(ClientCommands.argument("name", StringArgumentType.word())
                                        .suggests(ProfileCommands::suggestProfiles)
                                        .executes(ctx -> reply(ctx, ModuleStateStore.deleteProfile(StringArgumentType.getString(ctx, "name"))))))));
    }

    private static int list(CommandContext<FabricClientCommandSource> ctx) {
        List<String> names = ModuleStateStore.profileNames();
        if (names.isEmpty()) {
            return reply(ctx, "No profiles yet. Turn on the modules you want, then /m0profile save <name>.");
        }
        String active = ModuleStateStore.activeProfile();
        StringBuilder text = new StringBuilder("Profiles: ");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(names.get(i));
            if (names.get(i).equals(active)) {
                text.append(" (active)");
            }
        }
        return reply(ctx, text.toString());
    }

    private static CompletableFuture<Suggestions> suggestProfiles(CommandContext<FabricClientCommandSource> ctx,
                                                                  SuggestionsBuilder builder) {
        for (String name : ModuleStateStore.profileNames()) {
            if (name.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static int reply(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[Profiles] " + message));
        return 1;
    }
}
