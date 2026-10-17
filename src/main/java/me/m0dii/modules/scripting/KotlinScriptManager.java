package me.m0dii.modules.scripting;

import net.minecraft.client.Minecraft;

import javax.script.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kotlin scripts (.kts). Starting the Kotlin compiler is the expensive part, so each thread (client thread and
 * the async script worker) keeps one engine and reuses it. The engine is a REPL that remembers every snippet, so it
 * is replaced after {@link #EVALS_PER_ENGINE} runs to keep memory bounded.
 * <p>
 * Kotlin code cannot be interrupted from outside; long-running Kotlin scripts should be {@code @async} and call
 * {@code m0.checkCancelled()} (or {@code m0.sleep(...)}) inside loops so timeouts and {@code /runscript stop} work.
 */
public class KotlinScriptManager implements ScriptManager {
    static final int EVALS_PER_ENGINE = 32;

    private volatile ScriptEngineFactory factory;
    private volatile boolean factoryResolved;
    private final ThreadLocal<EngineSlot> engines = new ThreadLocal<>();

    private static final class EngineSlot {
        final ScriptEngine engine;
        final Set<String> boundKeys = new HashSet<>();
        int evals;

        EngineSlot(ScriptEngine engine) {
            this.engine = engine;
        }
    }

    @Override
    public String languageName() {
        return "Kotlin";
    }

    @Override
    public boolean supportsTimeout() {
        return false;
    }

    @Override
    public Object runScript(String script, Map<String, Object> context, ScriptJob job) {
        EngineSlot slot = acquireEngine();
        Bindings bindings = slot.engine.getBindings(ScriptContext.ENGINE_SCOPE);
        // Drop variables from the previous run that this run does not define.
        for (String key : slot.boundKeys) {
            if (context == null || !context.containsKey(key)) {
                bindings.remove(key);
            }
        }
        slot.boundKeys.clear();
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                slot.engine.put(entry.getKey(), entry.getValue());
                slot.boundKeys.add(entry.getKey());
            }
        }
        try {
            return slot.engine.eval(script == null ? "" : script);
        } catch (Exception | LinkageError e) {
            // A failed REPL snippet can leave the engine in a bad state; start fresh next time.
            engines.remove();
            throw ScriptExecutionException.wrap(languageName(), e);
        } finally {
            slot.evals++;
            if (slot.evals >= EVALS_PER_ENGINE) {
                engines.remove();
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

    private EngineSlot acquireEngine() {
        EngineSlot slot = engines.get();
        if (slot != null) {
            return slot;
        }
        ScriptEngine engine;
        synchronized (this) {
            if (!factoryResolved) {
                // The lookup creates an engine anyway; keep it as this thread's first engine.
                engine = new ScriptEngineManager(KotlinScriptManager.class.getClassLoader()).getEngineByExtension("kts");
                factory = engine == null ? null : engine.getFactory();
                factoryResolved = true;
            } else {
                engine = factory == null ? null : factory.getScriptEngine();
            }
        }
        if (engine == null) {
            throw new ScriptExecutionException("Kotlin script engine not found (Kotlin scripting is missing from this install).");
        }
        slot = new EngineSlot(engine);
        engines.set(slot);
        return slot;
    }
}
