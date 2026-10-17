package me.m0dii.modules.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.ThreadInterrupt;
import groovy.transform.TimedInterrupt;
import net.minecraft.client.Minecraft;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Groovy scripts. Compiled classes are cached per source text, so scripts run repeatedly (keybinds, automation
 * ticks) compile once instead of generating a new class every run. Loops are instrumented with
 * {@link TimedInterrupt} (time limit) and {@link ThreadInterrupt} (stop for async scripts).
 */
public class GroovyScriptManager implements ScriptManager {
    private static final int CACHE_LIMIT = 64;
    private static final AtomicLong CLASS_COUNTER = new AtomicLong();

    private final Compiler syncCompiler = new Compiler(ScriptRunner.SYNC_TIMEOUT_MS);
    private final Compiler asyncCompiler = new Compiler(ScriptRunner.ASYNC_TIMEOUT_MS);

    @Override
    public String languageName() {
        return "Groovy";
    }

    @Override
    public Object runScript(String script, Map<String, Object> context, ScriptJob job) {
        Binding binding = new Binding();
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }
        try {
            Class<? extends Script> scriptClass = (job.isAsync() ? asyncCompiler : syncCompiler).compile(script == null ? "" : script);
            return InvokerHelper.createScript(scriptClass, binding).run();
        } catch (CompilationFailedException e) {
            throw new ScriptExecutionException("Groovy compile error: " + compileMessage(e), e);
        } catch (Exception | LinkageError | AssertionError e) {
            // Groovy reports some compiler failures as GroovyBugError (an AssertionError).
            throw ScriptExecutionException.wrap(languageName(), e);
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

    private static String compileMessage(CompilationFailedException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().strip();
        // "startup failed:\nScript1.groovy: 3: unexpected token ..." -> keep the useful line.
        for (String line : message.split("\\R")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("startup failed:")) {
                return trimmed;
            }
        }
        return "unknown error";
    }

    private static final class Compiler {
        private final long timeoutMs;
        private final Map<String, Class<? extends Script>> cache = new LinkedHashMap<>(16, 0.75f, true);
        private GroovyClassLoader loader;

        Compiler(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        synchronized Class<? extends Script> compile(String source) {
            Class<? extends Script> cached = cache.get(source);
            if (cached != null) {
                return cached;
            }
            if (loader == null || cache.size() >= CACHE_LIMIT) {
                // Drop the loader with the cache so old script classes can be unloaded.
                cache.clear();
                loader = newLoader();
            }
            @SuppressWarnings("unchecked")
            Class<? extends Script> compiled = (Class<? extends Script>) loader.parseClass(source,
                    "M0Script" + CLASS_COUNTER.incrementAndGet() + ".groovy");
            cache.put(source, compiled);
            return compiled;
        }

        private GroovyClassLoader newLoader() {
            CompilerConfiguration config = new CompilerConfiguration();
            long seconds = Math.max(1L, timeoutMs / 1000L);
            config.addCompilationCustomizers(
                    new ASTTransformationCustomizer(Map.of("value", seconds), TimedInterrupt.class),
                    new ASTTransformationCustomizer(ThreadInterrupt.class)
            );
            return new GroovyClassLoader(GroovyScriptManager.class.getClassLoader(), config);
        }
    }
}
