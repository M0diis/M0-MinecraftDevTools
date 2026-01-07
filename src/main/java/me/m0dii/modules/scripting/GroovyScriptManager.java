package me.m0dii.modules.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import me.m0dii.M0DevTools;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

public class GroovyScriptManager implements ScriptManager {

    private final GroovyShell shell;
    private final Binding binding;

    public GroovyScriptManager() {
        this.binding = new Binding();
        this.shell = new GroovyShell(binding);
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
            M0DevTools.LOGGER.error("Error executing Groovy script:", e);
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
