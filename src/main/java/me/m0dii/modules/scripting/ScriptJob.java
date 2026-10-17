package me.m0dii.modules.scripting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One script execution. Tracks the running thread and lets the runner (timeout, {@code /runscript stop}) cancel it.
 * <p>
 * Cancellation is best effort and depends on the engine: Groovy checks for it inside loops, JavaScript execution is
 * aborted through its polyglot context, Kotlin can only stop at {@code m0.checkCancelled()} / {@code m0.sleep()}.
 */
public final class ScriptJob {
    private static final AtomicLong NEXT_ID = new AtomicLong(1L);
    private static final ThreadLocal<ScriptJob> CURRENT = new ThreadLocal<>();

    private final long id;
    private final String name;
    private final boolean async;
    private final long timeoutMs;
    private final List<Runnable> cancelHooks = new CopyOnWriteArrayList<>();

    private volatile boolean cancelled;
    private volatile String cancelReason;
    private volatile Thread thread;
    private volatile long startedAtMs;
    private volatile boolean stoppable = true;

    ScriptJob(String name, boolean async, long timeoutMs) {
        this.id = NEXT_ID.getAndIncrement();
        this.name = name == null || name.isBlank() ? "inline script" : name;
        this.async = async;
        this.timeoutMs = timeoutMs;
    }

    /** A job that is never cancelled; used when a manager is called directly without the runner. */
    static ScriptJob detached() {
        return new ScriptJob("script", false, 0L);
    }

    /** The job running on the current thread, or {@code null} outside a script. */
    public static ScriptJob current() {
        return CURRENT.get();
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean isAsync() {
        return async;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public long startedAtMs() {
        return startedAtMs;
    }

    /** Whether the engine running this job can be stopped mid-run (false for Kotlin). */
    public boolean isStoppable() {
        return stoppable;
    }

    void setStoppable(boolean stoppable) {
        this.stoppable = stoppable;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public boolean isRunning() {
        return thread != null;
    }

    /** Throws if the job was cancelled. Scripts can call this (via {@code m0.checkCancelled()}) inside long loops. */
    public void checkCancelled() {
        if (cancelled) {
            throw new ScriptExecutionException("Script '" + name + "' " + (cancelReason == null ? "was stopped" : cancelReason) + ".");
        }
    }

    /** Registers an action to run when the job is cancelled (e.g. closing a JavaScript context). */
    public void onCancel(Runnable hook) {
        if (hook == null) {
            return;
        }
        cancelHooks.add(hook);
        if (cancelled) {
            runHook(hook);
        }
    }

    void cancel(String reason) {
        if (cancelled) {
            return;
        }
        cancelReason = reason;
        cancelled = true;
        for (Runnable hook : cancelHooks) {
            runHook(hook);
        }
        Thread running = thread;
        // Never interrupt the client thread; only script worker threads.
        if (async && running != null) {
            running.interrupt();
        }
    }

    ScriptJob begin() {
        ScriptJob previous = CURRENT.get();
        thread = Thread.currentThread();
        startedAtMs = System.currentTimeMillis();
        CURRENT.set(this);
        return previous;
    }

    void end(ScriptJob previous) {
        thread = null;
        cancelHooks.clear();
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    private static void runHook(Runnable hook) {
        try {
            hook.run();
        } catch (RuntimeException ignored) {
            // A failing cancel hook must not prevent the others from running.
        }
    }
}
