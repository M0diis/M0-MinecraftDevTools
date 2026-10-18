package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public final class MathClientCommand {
    /** Variables and {@code ans} live for the game session. */
    private static final MathSession SESSION = new MathSession();

    private MathClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("math")
                        .executes(MathClientCommand::showHelp)
                        .then(ClientCommands.literal("vars").executes(MathClientCommand::showVariables))
                        .then(ClientCommands.literal("clear").executes(context -> {
                            SESSION.clear();
                            context.getSource().sendFeedback(Component.literal("[Math] Variables cleared."));
                            return 1;
                        }))
                        .then(ClientCommands.argument("expression", StringArgumentType.greedyString())
                                .executes(MathClientCommand::evaluateExpression))));
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("[Math] Usage: /math <expression>"));
        context.getSource().sendFeedback(Component.literal("[Math] Examples: /math 5 + 5, /math sqrt(144), /math sin(90), /math 2^16"));
        context.getSource().sendFeedback(Component.literal("[Math] Variables: /math x = 5*64, then /math x/2 · ans = last result · px py pz = your position"));
        context.getSource().sendFeedback(Component.literal("[Math] Units: /math 3 stacks + 5 · /math 3000 in stacks · /math 1200 ticks in s · /math px in nether"));
        context.getSource().sendFeedback(Component.literal("[Math] (items, stacks, sb, dc · t, rt, s, min, h, days · blocks, nether, chunks) · /math vars, /math clear"));
        return 1;
    }

    private static int showVariables(CommandContext<FabricClientCommandSource> context) {
        Map<String, Double> variables = SESSION.variables();
        if (variables.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("[Math] No variables yet. Set one with /math name = expression"));
            return 1;
        }
        variables.forEach((name, value) -> context.getSource().sendFeedback(
                Component.literal("[Math] " + name + " = " + MathExpressionEvaluator.formatResult(value))));
        return 1;
    }

    private static Map<String, Double> playerValues(FabricClientCommandSource source) {
        Map<String, Double> values = new HashMap<>();
        var player = source.getClient().player;
        if (player != null) {
            values.put("px", (double) player.blockPosition().getX());
            values.put("py", (double) player.blockPosition().getY());
            values.put("pz", (double) player.blockPosition().getZ());
        }
        return values;
    }

    private static int evaluateExpression(CommandContext<FabricClientCommandSource> context) {
        String expression = StringArgumentType.getString(context, "expression").trim();
        if (expression.isEmpty()) {
            return showHelp(context);
        }

        try {
            context.getSource().sendFeedback(Component.literal("[Math] " + SESSION.run(expression, playerValues(context.getSource()))));
            return 1;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Component.literal("[Math] " + exception.getMessage()));
            return 0;
        }
    }
}
