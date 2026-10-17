package me.m0dii.modules.scripting;

import net.minecraft.client.Minecraft;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * JavaScript via GraalJS. Each run gets a fresh engine (scripts don't leak globals into each other); the engine
 * factory lookup is done once. A running script is stopped by closing its polyglot context, which works from any
 * thread, including for scripts running on the client thread.
 */
public class JavaScriptScriptManager implements ScriptManager {
    private volatile ScriptEngineFactory factory;
    private volatile boolean factoryResolved;

    @Override
    public String languageName() {
        return "JavaScript";
    }

    @Override
    public Object runScript(String script, Map<String, Object> context, ScriptJob job) {
        ScriptEngine engine = createEngine();
        if (engine == null) {
            throw new ScriptExecutionException("JavaScript engine not found (GraalJS is missing from this install).");
        }
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                engine.put(entry.getKey(), entry.getValue());
            }
        }
        Object polyglotContext = polyglotContext(engine);
        if (polyglotContext != null) {
            job.onCancel(() -> closeContext(polyglotContext, true));
        }
        try {
            Object result = engine.eval(script == null ? "" : script);
            // JS objects are views into the polyglot context; turn them into text before the context is closed.
            if (result == null || result instanceof String || result instanceof Number || result instanceof Boolean) {
                return result;
            }
            return String.valueOf(result);
        } catch (Exception e) {
            throw ScriptExecutionException.wrap(languageName(), e);
        } finally {
            if (polyglotContext != null) {
                closeContext(polyglotContext, false);
            }
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

    private ScriptEngine createEngine() {
        ScriptEngineFactory resolved = resolveFactory();
        if (resolved == null) {
            return null;
        }
        ScriptEngine scriptEngine = resolved.getScriptEngine();
        Bindings bindings = scriptEngine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
        if (bindings != null) {
            bindings.put("polyglot.js.allowHostAccess", true);
            bindings.put("polyglot.js.allowHostClassLookup", (Predicate<String>) name -> true);
        }
        return scriptEngine;
    }

    private ScriptEngineFactory resolveFactory() {
        if (factoryResolved) {
            return factory;
        }
        synchronized (this) {
            if (!factoryResolved) {
                ScriptEngineManager manager = new ScriptEngineManager(JavaScriptScriptManager.class.getClassLoader());
                ScriptEngine probe = manager.getEngineByExtension("js");
                if (probe == null) {
                    probe = manager.getEngineByName("js");
                }
                if (probe == null) {
                    probe = manager.getEngineByName("JavaScript");
                }
                factory = probe == null ? null : probe.getFactory();
                if (probe != null) {
                    Object context = polyglotContext(probe);
                    if (context != null) {
                        closeContext(context, false);
                    }
                }
                factoryResolved = true;
            }
        }
        return factory;
    }

    /** GraalJSScriptEngine#getPolyglotContext, looked up reflectively so this class loads without GraalJS. */
    private static Object polyglotContext(ScriptEngine engine) {
        try {
            Method method = engine.getClass().getMethod("getPolyglotContext");
            return method.invoke(engine);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static void closeContext(Object polyglotContext, boolean cancelIfExecuting) {
        try {
            Method close = polyglotContext.getClass().getMethod("close", boolean.class);
            close.invoke(polyglotContext, cancelIfExecuting);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Best effort: a context that is already closed, or an engine without one.
        }
    }
}
