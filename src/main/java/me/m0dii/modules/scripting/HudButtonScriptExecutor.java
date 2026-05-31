package me.m0dii.modules.scripting;

import me.m0dii.M0DevTools;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HudButtonScriptExecutor {
    private static final GroovyScriptManager GROOVY = new GroovyScriptManager();
    private static final KotlinScriptManager KOTLIN = new KotlinScriptManager();
    private static final ExecutorService SCRIPT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "m0dev-hud-script");
        t.setDaemon(true);
        return t;
    });

    private HudButtonScriptExecutor() {
    }

    public static boolean runScript(String actionName, String scriptAction, MacroHudDataHandler.ButtonExecutionMode mode, boolean async) {
        String raw = scriptAction == null ? "" : scriptAction.trim();
        if (raw.isEmpty()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        String script = raw;
        ScriptManager engine = mode == MacroHudDataHandler.ButtonExecutionMode.KOTLIN_SCRIPT ? KOTLIN : GROOVY;

        if (raw.regionMatches(true, 0, "kotlin:", 0, 7) || raw.regionMatches(true, 0, "kts:", 0, 4)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = KOTLIN;
        } else if (raw.regionMatches(true, 0, "groovy:", 0, 7)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = GROOVY;
        }

        if ((script.endsWith(".groovy") || script.endsWith(".kts")) && !script.contains("\n") && !script.contains("\r")) {
            try {
                script = ScriptStorage.readScript(script);
            } catch (Exception e) {
                client.player.sendMessage(Text.literal("Script button '" + actionName + "' failed to load script file."), true);
                M0DevTools.LOGGER.error("Failed to load script for HUD button '{}': {}", actionName, e.getMessage());
                return false;
            }
        }

        if (script.isBlank()) {
            return false;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("client", client);
        context.put("player", client.player);
        context.put("source", client.player);
        context.put("world", client.world);
        context.put("options", client.options);
        context.put("server", client.getServer());

        if (async) {
            String finalScript = script;
            ScriptManager finalEngine = engine;
            SCRIPT_EXECUTOR.execute(() -> {
                try {
                    Object result = finalEngine.runScript(finalScript, context);
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal(String.valueOf(result)), false);
                        }
                    });
                } catch (Exception e) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("Script button '" + actionName + "' failed: " + e.getMessage()), true);
                        }
                    });
                    M0DevTools.LOGGER.error("Error executing HUD script button '{}':", actionName, e);
                }
            });
            return true;
        }

        try {
            Object result = engine.runScript(script, context);
            client.player.sendMessage(Text.literal(String.valueOf(result)), false);
            return true;
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Script button '" + actionName + "' failed: " + e.getMessage()), true);
            M0DevTools.LOGGER.error("Error executing HUD script button '{}':", actionName, e);
            return false;
        }
    }
}

