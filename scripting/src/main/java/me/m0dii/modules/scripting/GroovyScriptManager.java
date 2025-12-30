package me.m0dii.modules.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroovyScriptManager implements ScriptManager {
    private static final Logger LOGGER = LogManager.getLogger(GroovyScriptManager.class);

    private final GroovyShell shell;
    private final Binding binding;

    public GroovyScriptManager() {
        this.binding = new Binding();
        this.shell = new GroovyShell(binding);
    }

    /**
     * Execute a saved script by name (reads from storage and runs it).
     */
    public static void executeScript(String scriptName) {
        try {
            String script = ScriptStorage.readScript(scriptName);
            if (script == null) {
                LOGGER.warn("Script '{}' returned null content", scriptName);
                return;
            }
            GroovyScriptManager manager = new GroovyScriptManager();
            manager.runScript(script);
        } catch (IOException e) {
            LOGGER.warn("Failed to read script '{}' from storage: {}", scriptName, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unhandled exception while executing script '{}':", scriptName, e);
        }
    }

    @Override
    public Object runScript(String script, Map<String, Object> context) {
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }
        try {
            return shell.evaluate(script);
        } catch (Exception e) {
            LOGGER.error("Error executing Groovy script:", e);
            return null;
        }
    }

    public Object runScript(String script) {
        Map<String, Object> context = new HashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();

        context.put("client", client);
        context.put("source", client.player);
        context.put("player", client.player);
        context.put("world", client.world);
        context.put("options", client.options);
        context.put("server", client.getServer());

        return runScript(script, context);
    }
}

