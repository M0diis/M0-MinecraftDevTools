package me.m0dii.modules.scripting;

import java.util.Map;

public interface ScriptManager {
    /**
     * Runs a script.
     *
     * @return the script's result value (may be {@code null})
     * @throws ScriptExecutionException when the script fails to compile, throws, times out or is stopped
     */
    Object runScript(String script, Map<String, Object> context, ScriptJob job);

    /** Runs a script outside the {@link ScriptRunner} (no timeout, no {@code m0} helper unless provided). */
    default Object runScript(String script, Map<String, Object> context) {
        return runScript(script, context, ScriptJob.detached());
    }

    /** Whether a running script can be stopped by a timeout or {@code /runscript stop}. */
    default boolean supportsTimeout() {
        return true;
    }

    /** Language name used in messages. */
    String languageName();
}
