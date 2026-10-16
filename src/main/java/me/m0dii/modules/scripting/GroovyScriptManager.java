package me.m0dii.modules.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import me.m0dii.M0DevTools;
import net.minecraft.client.Minecraft;
import java.util.HashMap;
import java.util.Map;

public class GroovyScriptManager implements ScriptManager {

    @Override
    public Object runScript(String script, Map<String, Object> context) {
        Binding binding = new Binding();
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }
        try {
            GroovyShell shell = new GroovyShell(binding);
            return shell.evaluate(script);
        } catch (Exception | LinkageError | AssertionError e) {
            // Groovy reports compiler failures as GroovyBugError (an AssertionError); never let a script crash the client.
            M0DevTools.LOGGER.error("Error executing Groovy script:", e);
            return null;
        }
    }

    public Object runScript(String script) {
        Map<String, Object> context = new HashMap<>();
        Minecraft client = Minecraft.getInstance();

        context.put("client", client);
        context.put("source", client.player);
        context.put("player", client.player);
        context.put("world", client.level);
        context.put("options", client.options);
        context.put("server", client.getSingleplayerServer());

        return runScript(script, context);
    }
}
