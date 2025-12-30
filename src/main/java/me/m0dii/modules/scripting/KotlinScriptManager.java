package me.m0dii.modules.scripting;

import net.minecraft.client.MinecraftClient;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class KotlinScriptManager implements ScriptManager {
    private final ScriptEngine engine;

    public KotlinScriptManager() {
        this.engine = new ScriptEngineManager().getEngineByExtension("kts");
    }

    @Override
    public Object runScript(String script, Map<String, Object> context) {
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            return engine.eval(script);
        } catch (ScriptException e) {
            return e.getMessage();
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
