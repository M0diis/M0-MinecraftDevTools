package me.m0dii.modules.scripting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClientCommandRunScript {
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
            String raw = script == null ? "" : script.trim();
            if (raw.isEmpty()) {
                ctx.getSource().sendError(Text.literal("Provide a script file name or a prefixed inline script."));
                return Command.SINGLE_SUCCESS;
            }

            Map<String, Object> context = new LinkedHashMap<>(ScriptTypes.defaultContext());
            context.put("commandSource", ctx.getSource());

            Object result;
            if (ScriptStorage.exists(raw) && ScriptTypes.isScriptFile(raw)) {
                String source = ScriptStorage.readScript(raw);
                result = ScriptTypes.managerFor(raw).runScript(source, context);
            } else if (raw.regionMatches(true, 0, "groovy:", 0, 7)) {
                result = new GroovyScriptManager().runScript(raw.substring(raw.indexOf(':') + 1).trim(), context);
            } else if (raw.regionMatches(true, 0, "kotlin:", 0, 7) || raw.regionMatches(true, 0, "kts:", 0, 4)) {
                result = new KotlinScriptManager().runScript(raw.substring(raw.indexOf(':') + 1).trim(), context);
            } else if (raw.regionMatches(true, 0, "javascript:", 0, 11) || raw.regionMatches(true, 0, "js:", 0, 3)) {
                result = new JavaScriptScriptManager().runScript(raw.substring(raw.indexOf(':') + 1).trim(), context);
            } else if (ScriptTypes.isScriptFile(raw)) {
                ctx.getSource().sendError(Text.literal("Script file not found: " + raw));
                return Command.SINGLE_SUCCESS;
            } else {
                ctx.getSource().sendError(Text.literal("Unknown script. Use a saved .groovy/.kts/.js file or groovy:/kotlin:/js: inline source."));
                return Command.SINGLE_SUCCESS;
            }
            ctx.getSource().sendFeedback(Text.literal("Script result: " + ScriptTypes.formatResult(result)));
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Script error: " + e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
