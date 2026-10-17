package me.m0dii.modules.scripting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientCommandRunScript {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("runscript")
                            .then(ClientCommands.literal("stop")
                                    .executes(ClientCommandRunScript::stopScripts))
                            .then(ClientCommands.literal("running")
                                    .executes(ClientCommandRunScript::listScripts))
                            .then(ClientCommands.argument("script", StringArgumentType.greedyString())
                                    .executes(ctx -> runScript(ctx, StringArgumentType.getString(ctx, "script")))
                            )
            );
        });
    }

    private static int stopScripts(CommandContext<FabricClientCommandSource> ctx) {
        int stopped = ScriptRunner.cancelAll();
        ctx.getSource().sendFeedback(Component.literal(stopped == 0
                ? "No scripts are running."
                : "Stopping " + stopped + " script(s)."));
        return Command.SINGLE_SUCCESS;
    }

    private static int listScripts(CommandContext<FabricClientCommandSource> ctx) {
        List<ScriptJob> jobs = ScriptRunner.activeJobs();
        if (jobs.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("No scripts are running."));
            return Command.SINGLE_SUCCESS;
        }
        long now = System.currentTimeMillis();
        for (ScriptJob job : jobs) {
            String state = !job.isRunning() ? "queued" : "running " + ((now - job.startedAtMs()) / 1000L) + "s";
            ctx.getSource().sendFeedback(Component.literal("#" + job.id() + " " + job.name()
                    + (job.isAsync() ? " [async] " : " ") + state + (job.isCancelled() ? " (stopping)" : "")));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runScript(CommandContext<FabricClientCommandSource> ctx, String script) {
        String raw = script == null ? "" : script.trim();
        if (raw.isEmpty()) {
            ctx.getSource().sendError(Component.literal("Provide a script file name or a prefixed inline script."));
            return Command.SINGLE_SUCCESS;
        }

        Map<String, Object> context = new LinkedHashMap<>(ScriptTypes.defaultContext());
        context.put("commandSource", ctx.getSource());

        String name;
        String source;
        ScriptManager manager;
        try {
            if (ScriptStorage.exists(raw) && ScriptTypes.isScriptFile(raw)) {
                name = raw;
                source = ScriptStorage.readScript(raw);
                manager = ScriptTypes.managerFor(raw);
            } else if (raw.regionMatches(true, 0, "groovy:", 0, 7)) {
                name = "inline groovy";
                source = raw.substring(raw.indexOf(':') + 1).trim();
                manager = ScriptTypes.groovy();
            } else if (raw.regionMatches(true, 0, "kotlin:", 0, 7) || raw.regionMatches(true, 0, "kts:", 0, 4)) {
                name = "inline kotlin";
                source = raw.substring(raw.indexOf(':') + 1).trim();
                manager = ScriptTypes.kotlin();
            } else if (raw.regionMatches(true, 0, "javascript:", 0, 11) || raw.regionMatches(true, 0, "js:", 0, 3)) {
                name = "inline javascript";
                source = raw.substring(raw.indexOf(':') + 1).trim();
                manager = ScriptTypes.javascript();
            } else if (ScriptTypes.isScriptFile(raw)) {
                ctx.getSource().sendError(Component.literal("Script file not found: " + raw));
                return Command.SINGLE_SUCCESS;
            } else {
                ctx.getSource().sendError(Component.literal("Unknown script. Use a saved .groovy/.kts/.js file or groovy:/kotlin:/js: inline source."));
                return Command.SINGLE_SUCCESS;
            }
        } catch (Exception e) {
            ctx.getSource().sendError(Component.literal("Could not load script: " + e.getMessage()));
            return Command.SINGLE_SUCCESS;
        }

        boolean async = ScriptDirectives.isAsync(source);
        if (async) {
            ctx.getSource().sendFeedback(Component.literal("Started " + name + " in the background. Stop it with /runscript stop."));
        }
        ScriptRunner.run(name, manager, source, context, async, outcome -> {
            if (outcome.success()) {
                ctx.getSource().sendFeedback(Component.literal("Script result: " + outcome.displayText()));
            } else {
                ctx.getSource().sendError(Component.literal(outcome.error()));
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
