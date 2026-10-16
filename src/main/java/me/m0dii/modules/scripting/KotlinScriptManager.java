package me.m0dii.modules.scripting;

import net.minecraft.client.Minecraft;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public class KotlinScriptManager implements ScriptManager {

    @Override
    public Object runScript(String script, Map<String, Object> context) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("kts");
        if (engine == null) {
            return "Kotlin script engine not found.";
        }
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
