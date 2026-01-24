package me.m0dii.modules.actionrunner;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ActionRunnerClientCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("actionrunner")
                .then(literal("add")
                        .then(argument("delay", IntegerArgumentType.integer(0))
                                .then(argument("action", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (ActionRunnerModule.Action.Type type : ActionRunnerModule.Action.Type.values()) {
                                                builder.suggest(type.name().toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(argument("param", StringArgumentType.greedyString()).executes(ctx -> {
                                            int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                            String actionType = StringArgumentType.getString(ctx, "action").toUpperCase();
                                            String param = StringArgumentType.getString(ctx, "param");
                                            ActionRunnerModule.Action.Type type = ActionRunnerModule.Action.Type.valueOf(actionType);
                                            // Pass param for all actions that need it, not just COMMAND
                                            String command = (type == ActionRunnerModule.Action.Type.COMMAND
                                                    || type == ActionRunnerModule.Action.Type.SELECT_HOTBAR_SLOT) ? param : null;
                                            ActionRunnerModule.getInstance().addAction(
                                                    new ActionRunnerModule.Action(delay, type, command)
                                            );
                                            ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Added " + actionType + " with delay " + delay + (command != null ? ": " + command : "")));
                                            return 1;
                                        }))
                                        .executes(ctx -> {
                                            int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                            String actionType = StringArgumentType.getString(ctx, "action").toUpperCase();
                                            ActionRunnerModule.Action.Type type = ActionRunnerModule.Action.Type.valueOf(actionType);
                                            ActionRunnerModule.getInstance().addAction(
                                                    new ActionRunnerModule.Action(delay, type, null)
                                            );
                                            ctx.getSource().sendFeedback(Text.literal("[ActionRunner] Added " + actionType + " with delay " + delay));
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
}
