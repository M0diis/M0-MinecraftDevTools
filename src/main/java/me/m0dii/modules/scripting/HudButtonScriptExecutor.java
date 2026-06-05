package me.m0dii.modules.scripting;

import me.m0dii.M0DevTools;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class HudButtonScriptExecutor {
    private static final GroovyScriptManager GROOVY = new GroovyScriptManager();
    private static final KotlinScriptManager KOTLIN = new KotlinScriptManager();
    private static final JavaScriptScriptManager JAVASCRIPT = new JavaScriptScriptManager();

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
        ScriptManager engine = switch (mode) {
            case KOTLIN_SCRIPT -> KOTLIN;
            case JAVASCRIPT_SCRIPT -> JAVASCRIPT;
            default -> GROOVY;
        };

        if (raw.regionMatches(true, 0, "kotlin:", 0, 7) || raw.regionMatches(true, 0, "kts:", 0, 4)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = KOTLIN;
        } else if (raw.regionMatches(true, 0, "groovy:", 0, 7)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = GROOVY;
        } else if (raw.regionMatches(true, 0, "javascript:", 0, 11)
                || raw.regionMatches(true, 0, "js:", 0, 3)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = JAVASCRIPT;
        }

        if (ScriptTypes.isScriptFile(script) && !script.contains("\n") && !script.contains("\r")) {
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

        Map<String, Object> context = new HashMap<>(ScriptTypes.defaultContext());

        if (async) {
            String finalScript = script;
            ScriptManager finalEngine = engine;
            client.execute(() -> {
                try {
                    Object result = finalEngine.runScript(finalScript, context);
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(ScriptTypes.formatResult(result)), false);
                    }
                } catch (Exception e) {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("Script button '" + actionName + "' failed: " + e.getMessage()), true);
                    }
                    M0DevTools.LOGGER.error("Error executing HUD script button '{}':", actionName, e);
                }
            });
            return true;
        }

        try {
            Object result = engine.runScript(script, context);
            client.player.sendMessage(Text.literal(ScriptTypes.formatResult(result)), false);
            return true;
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Script button '" + actionName + "' failed: " + e.getMessage()), true);
            M0DevTools.LOGGER.error("Error executing HUD script button '{}':", actionName, e);
            return false;
        }
    }
}

