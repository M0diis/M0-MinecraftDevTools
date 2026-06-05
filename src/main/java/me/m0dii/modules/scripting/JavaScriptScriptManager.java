package me.m0dii.modules.scripting;

import net.minecraft.client.MinecraftClient;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class JavaScriptScriptManager implements ScriptManager {

    @Override
    public Object runScript(String script, Map<String, Object> context) {
        ScriptEngine engine = createEngine();
        if (engine == null) {
            return "JavaScript engine not found.";
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
        MinecraftClient client = MinecraftClient.getInstance();
        context.put("client", client);
        context.put("source", client.player);
        context.put("player", client.player);
        context.put("world", client.world);
        context.put("options", client.options);
        context.put("server", client.getServer());
        return runScript(script, context);
    }

    private static ScriptEngine createEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine scriptEngine = manager.getEngineByExtension("js");
        if (scriptEngine == null) {
            scriptEngine = manager.getEngineByName("js");
        }
        if (scriptEngine == null) {
            scriptEngine = manager.getEngineByName("JavaScript");
        }
        if (scriptEngine == null) {
            return null;
        }

        Bindings bindings = scriptEngine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
        if (bindings != null) {
            bindings.put("polyglot.js.allowHostAccess", true);
            bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) name -> true);
        }
        return scriptEngine;
    }
}
