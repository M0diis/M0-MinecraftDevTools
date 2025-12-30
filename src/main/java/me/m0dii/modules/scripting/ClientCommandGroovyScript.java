package me.m0dii.modules.scripting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.HashMap;

public class ClientCommandGroovyScript {
    private static final GroovyScriptManager groovyScriptManager = new GroovyScriptManager();
    private static final KotlinScriptManager kotlinScriptManager = new KotlinScriptManager();

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("runscript")
                            .then(ClientCommandManager.argument("script", StringArgumentType.greedyString())
                                    .executes(ctx -> runScript(ctx, StringArgumentType.getString(ctx, "script")))
                            )
            );
        });
    }

    private static int runScript(CommandContext<FabricClientCommandSource> ctx, String script) {
        try {
            HashMap<String, Object> context = new HashMap<>();
            context.put("source", ctx.getSource());
            Object result;
            if (script.trim().endsWith(".kts")) {
                result = kotlinScriptManager.runScript(script, context);
            } else if (script.trim().endsWith(".groovy")) {
                result = groovyScriptManager.runScript(script, context);
            } else {
                ctx.getSource().sendError(Text.literal("Unknown script extension. Use .groovy or .kts"));
                return Command.SINGLE_SUCCESS;
            }
            ctx.getSource().sendFeedback(Text.literal("Script result: " + result));
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Script error: " + e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
