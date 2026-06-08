package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class MathClientCommand {
    private MathClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("math")
                        .executes(MathClientCommand::showHelp)
                        .then(ClientCommandManager.argument("expression", StringArgumentType.greedyString())
                                .executes(MathClientCommand::evaluateExpression))));
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[Math] Usage: /math <expression>"));
        context.getSource().sendFeedback(Text.literal("[Math] Examples: /math 5 + 5, /math sqrt(144), /math sin(90), /math 2^16"));
        return 1;
    }

    private static int evaluateExpression(CommandContext<FabricClientCommandSource> context) {
        String expression = StringArgumentType.getString(context, "expression").trim();
        if (expression.isEmpty()) {
            return showHelp(context);
        }

        try {
            double result = MathExpressionEvaluator.evaluate(expression);
            context.getSource().sendFeedback(Text.literal("[Math] " + expression + " = " + MathExpressionEvaluator.formatResult(result)));
            return 1;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendError(Text.literal("[Math] " + exception.getMessage()));
            return 0;
        }
    }
}
