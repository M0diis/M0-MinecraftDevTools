package me.m0dii.modules.actionrunner;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ActionRunnerClientCommand {
    private static final String[] ACTION_NAMES = Arrays.stream(ActionRunnerModule.Action.Type.values())
            .map(type -> type.name().toLowerCase(Locale.ROOT))
            .toArray(String[]::new);

    private static final String[] HOTBAR_SLOT_SUGGESTIONS = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8"
    };

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("actionrunner")
                .then(literal("add")
                        .then(argument("delay", IntegerArgumentType.integer(0))
                                .then(argument("action", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(ACTION_NAMES, builder))
                                        .then(argument("param", StringArgumentType.greedyString())
                                                .suggests(ActionRunnerClientCommand::suggestParamForAction)
                                                .executes(ctx -> {
                                                    int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                                    String actionType = StringArgumentType.getString(ctx, "action");
                                                    ActionRunnerModule.Action.Type type = parseActionType(actionType);
                                                    if (type == null) {
                                                        ctx.getSource().sendError(Text.literal("[ActionRunner] Unknown action: " + actionType));
                                                        return 0;
                                                    }

                                                    String param = StringArgumentType.getString(ctx, "param");
                                                    String command = (type == ActionRunnerModule.Action.Type.COMMAND
                                                            || type == ActionRunnerModule.Action.Type.SELECT_HOTBAR_SLOT) ? param : null;
                                                    ActionRunnerModule.getInstance().addAction(
                                                            new ActionRunnerModule.Action(delay, type, command)
                                                    );
                                                    ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Added " + type.name() + " with delay " + delay + (command != null ? ": " + command : "")));
                                                    return 1;
                                                }))
                                        .executes(ctx -> {
                                            int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                            String actionType = StringArgumentType.getString(ctx, "action");
                                            ActionRunnerModule.Action.Type type = parseActionType(actionType);
                                            if (type == null) {
                                                ctx.getSource().sendError(Text.literal("[ActionRunner] Unknown action: " + actionType));
                                                return 0;
                                            }
                                            if (type == ActionRunnerModule.Action.Type.COMMAND || type == ActionRunnerModule.Action.Type.SELECT_HOTBAR_SLOT) {
                                                ctx.getSource().sendError(Text.literal("[ActionRunner] " + type.name() + " requires a parameter."));
                                                return 0;
                                            }

                                            ActionRunnerModule.getInstance().addAction(
                                                    new ActionRunnerModule.Action(delay, type, null)
                                            );
                                            ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Added " + type.name() + " with delay " + delay));
                                            return 1;
                                        })
                                )
                        )
                )
                .then(literal("run")
                        .executes(ctx -> {
                            ActionRunnerModule.getInstance().runClient(ctx.getSource());
                            return 1;
                        })
                )
                .then(literal("clear")
                        .executes(ctx -> {
                            ActionRunnerModule.getInstance().clear();
                            ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Cleared all actions"));
                            return 1;
                        })
                )
                .then(literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Listing actions:"));
                            int index = 1;
                            for (ActionRunnerModule.Action action : ActionRunnerModule.getInstance().getActions()) {
                                ctx.getSource().sendFeedback(Text.literal(index + ". " + action.getActionInfo()));
                                index++;
                            }
                            return 1;
                        })
                )
        );
    }

    private static ActionRunnerModule.Action.Type parseActionType(String rawType) {
        try {
            return ActionRunnerModule.Action.Type.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static CompletableFuture<Suggestions> suggestParamForAction(
            com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        ActionRunnerModule.Action.Type type = parseActionType(StringArgumentType.getString(ctx, "action"));
        if (type == null) {
            return Suggestions.empty();
        }

        return switch (type) {
            case SELECT_HOTBAR_SLOT -> CommandSource.suggestMatching(HOTBAR_SLOT_SUGGESTIONS, builder);
            case COMMAND -> {
                if (builder.getRemaining().isEmpty()) {
                    builder.suggest("/");
                }
                yield builder.buildFuture();
            }
            default -> Suggestions.empty();
        };
    }
}
