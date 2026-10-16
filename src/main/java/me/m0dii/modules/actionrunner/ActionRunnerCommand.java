package me.m0dii.modules.actionrunner;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ActionRunnerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                                            String command = type == ActionRunnerModule.Action.Type.COMMAND ? param : null;
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            ActionRunnerModule.getInstance().addAction(
                                                    new ActionRunnerModule.Action(delay, type, command)
                                            );
                                            player.sendSystemMessage(Component.literal("[ActionRunner] Added " + actionType + " with delay " + delay + (command != null ? ": /" + command : "")));
                                            return 1;
                                        }))
                                        .executes(ctx -> {
                                            int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                            String actionType = StringArgumentType.getString(ctx, "action").toUpperCase();
                                            ActionRunnerModule.Action.Type type = ActionRunnerModule.Action.Type.valueOf(actionType);
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            ActionRunnerModule.getInstance().addAction(
                                                    new ActionRunnerModule.Action(delay, type, null)
                                            );
                                            player.sendSystemMessage(Component.literal("[ActionRunner] Added " + actionType + " with delay " + delay));
                                            return 1;
                                        })
                                )
                        )
                )
                .then(literal("run")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            ActionRunnerModule.getInstance().run(player);
                            return 1;
                        })
                )
                .then(literal("clear")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            ActionRunnerModule.getInstance().clear();
                            player.sendSystemMessage(Component.literal("[ActionRunner] Cleared all actions"));
                            return 1;
                        })
                )
                .then(literal("list")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            player.sendSystemMessage(Component.literal("[ActionRunner] Listing actions:"));
                            int index = 1;
                            for (ActionRunnerModule.Action action : ActionRunnerModule.getInstance().getActions()) {
                                player.sendSystemMessage(Component.literal(index + ". " + action.getActionInfo()));
                                index++;
                            }
                            return 1;
                        })
                )
        );
    }
}
