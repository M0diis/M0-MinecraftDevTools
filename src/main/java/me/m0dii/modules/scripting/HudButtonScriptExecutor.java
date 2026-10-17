package me.m0dii.modules.scripting;

import me.m0dii.M0DevTools;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public final class HudButtonScriptExecutor {
    private HudButtonScriptExecutor() {
    }

    public static boolean runScript(String actionName, String scriptAction, MacroHudDataHandler.ButtonExecutionMode mode, boolean async) {
        String raw = scriptAction == null ? "" : scriptAction.trim();
        if (raw.isEmpty()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }

        String script = raw;
        String name = actionName;
        ScriptManager engine = switch (mode) {
            case KOTLIN_SCRIPT -> ScriptTypes.kotlin();
            case JAVASCRIPT_SCRIPT -> ScriptTypes.javascript();
            default -> ScriptTypes.groovy();
        };

        if (raw.regionMatches(true, 0, "kotlin:", 0, 7) || raw.regionMatches(true, 0, "kts:", 0, 4)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = ScriptTypes.kotlin();
        } else if (raw.regionMatches(true, 0, "groovy:", 0, 7)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = ScriptTypes.groovy();
        } else if (raw.regionMatches(true, 0, "javascript:", 0, 11)
                || raw.regionMatches(true, 0, "js:", 0, 3)) {
            script = raw.substring(raw.indexOf(':') + 1).trim();
            engine = ScriptTypes.javascript();
        }

        if (ScriptTypes.isScriptFile(script) && !script.contains("\n") && !script.contains("\r")) {
            try {
                name = script;
                engine = ScriptTypes.managerFor(script);
                script = ScriptStorage.readScript(script);
            } catch (Exception e) {
                client.player.sendOverlayMessage(Component.literal("Script button '" + actionName + "' failed to load script file."));
                M0DevTools.LOGGER.error("Failed to load script for HUD button '{}': {}", actionName, e.getMessage());
                return false;
            }
        }

        if (script.isBlank()) {
            return false;
        }

        boolean runAsync = async || ScriptDirectives.isAsync(script);
        Map<String, Object> context = new HashMap<>(ScriptTypes.defaultContext());
        ScriptRunner.run(name, engine, script, context, runAsync, outcome -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (outcome.success()) {
                player.sendSystemMessage(Component.literal(outcome.displayText()));
            } else {
                player.sendOverlayMessage(Component.literal("Script button '" + actionName + "' failed: " + outcome.error()));
            }
        });
        return true;
    }
}
