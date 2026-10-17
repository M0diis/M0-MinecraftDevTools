package me.m0dii.modules.scripting;

/**
 * Thrown by a {@link ScriptManager} when a script fails to compile, throws, times out or is cancelled.
 * The message is meant to be shown to the player as-is.
 */
public class ScriptExecutionException extends RuntimeException {
    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScriptExecutionException(String message) {
        super(message);
    }

    /** Builds a readable one-line message from an engine exception, unwrapping wrapper layers. */
    static ScriptExecutionException wrap(String language, Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root
                && (root.getMessage() == null || root.getMessage().isBlank()
                || root instanceof java.lang.reflect.InvocationTargetException)) {
            root = root.getCause();
        }
        if (root instanceof ScriptExecutionException existing) {
            return existing;
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = root.getClass().getSimpleName();
        }
        if (root instanceof java.util.concurrent.TimeoutException || root instanceof InterruptedException) {
            message = "stopped (" + message + ")";
        }
        return new ScriptExecutionException(language + " error: " + firstLine(message), error);
    }

    private static String firstLine(String text) {
        String trimmed = text.strip();
        int newline = trimmed.indexOf('\n');
        return newline < 0 ? trimmed : trimmed.substring(0, newline).strip();
    }
}
