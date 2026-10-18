package me.m0dii.modules.scripting;

import net.minecraft.client.Minecraft;
import org.graalvm.polyglot.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JavaScript via the GraalVM Polyglot API.
 * <p>
 * Every run gets its own {@link Context} (scripts don't leak globals into each other), all built from one shared
 * {@link Engine} with exactly the same options. Building every context the same way is what keeps GraalJS from
 * failing with "Found different host access configuration for a context with a shared engine", which the old
 * JSR-223 setup (magic {@code polyglot.js.*} bindings plus a probe engine) ran into on the second script. The
 * shared engine also keeps parsed code cached between runs.
 * <p>
 * A running script is stopped by closing its context, which works from any thread.
 */
public class JavaScriptScriptManager implements ScriptManager {
    private static final String LANGUAGE = "js";
    private volatile Engine engine;
    private volatile String unavailableReason;

    @Override
    public String languageName() {
        return "JavaScript";
    }

    @Override
    public Object runScript(String script, Map<String, Object> context, ScriptJob job) {
        Context jsContext = newContext();
        job.onCancel(() -> close(jsContext, true));
        try {
            Value bindings = jsContext.getBindings(LANGUAGE);
            if (context != null) {
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    bindings.putMember(entry.getKey(), entry.getValue());
                }
            }
            Value result = jsContext.eval(LANGUAGE, script == null ? "" : script);
            // Values are views into the context; turn them into plain Java values before it is closed.
            return toJava(result);
        } catch (PolyglotException e) {
            if (e.isCancelled() || job.isCancelled()) {
                throw new ScriptExecutionException("JavaScript script " + (job.cancelReason() == null ? "was stopped" : job.cancelReason()), e);
            }
            throw new ScriptExecutionException(describe(e), e);
        } catch (RuntimeException e) {
            throw ScriptExecutionException.wrap(languageName(), e);
        } finally {
            close(jsContext, false);
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

    private Context newContext() {
        try {
            return Context.newBuilder(LANGUAGE)
                    .engine(sharedEngine())
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .build();
        } catch (LinkageError | IllegalArgumentException | IllegalStateException e) {
            unavailableReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw new ScriptExecutionException("JavaScript engine not available (" + unavailableReason + ").", e);
        }
    }

    private Engine sharedEngine() {
        Engine current = engine;
        if (current == null) {
            synchronized (this) {
                current = engine;
                if (current == null) {
                    current = Engine.newBuilder(LANGUAGE)
                            // Runs fine without the optimizing compiler; don't print a warning about it every launch.
                            .option("engine.WarnInterpreterOnly", "false")
                            .build();
                    engine = current;
                }
            }
        }
        return current;
    }

    static Object toJava(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.fitsInLong() ? (Object) value.asLong() : (Object) value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isHostObject()) {
            Object host = value.asHostObject();
            return host instanceof String || host instanceof Number || host instanceof Boolean ? host : String.valueOf(host);
        }
        return value.toString();
    }

    /** "JavaScript error: ReferenceError: foo is not defined (line 3)". */
    private static String describe(PolyglotException e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().strip();
        int newline = message.indexOf('\n');
        if (newline >= 0) {
            message = message.substring(0, newline).strip();
        }
        String where = "";
        try {
            if (e.getSourceLocation() != null) {
                where = " (line " + e.getSourceLocation().getStartLine() + ")";
            }
        } catch (RuntimeException ignored) {
            // no location
        }
        return "JavaScript error: " + message + where;
    }

    private static void close(Context context, boolean cancelIfExecuting) {
        try {
            context.close(cancelIfExecuting);
        } catch (RuntimeException ignored) {
            // Already closed, or closing a context that is still running on another thread without cancel.
        }
    }
}
