package me.m0dii.modules.scripting;

import me.m0dii.M0DevTools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Central place that runs scripts.
 * <ul>
 *     <li><b>Sync</b> scripts run on the calling (client) thread and get a short time limit.</li>
 *     <li><b>Async</b> scripts ({@code // @async on}, or the HUD button "async" toggle) run one at a time on a
 *     background worker, with a longer time limit. Use {@code m0.sync { ... }} to touch game state from them.</li>
 *     <li>Every run gets the {@code m0} helper ({@link ScriptApi}) in its bindings.</li>
 *     <li>{@link #cancelAll()} stops everything that is running or queued ({@code /runscript stop}).</li>
 * </ul>
 */
public final class ScriptRunner {
    public static final long SYNC_TIMEOUT_MS = 5_000L;
    public static final long ASYNC_TIMEOUT_MS = 120_000L;
    /** How long a cancelled async script may keep running before its worker thread is abandoned. */
    static final long ABANDON_GRACE_MS = 2_000L;

    /** Result of a run, delivered on the client thread. */
    public record Outcome(@NotNull ScriptJob job, boolean success, @Nullable Object result, @Nullable String error) {
        public String displayText() {
            return success ? ScriptTypes.formatResult(result) : error;
        }
    }

    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();
    private static final Set<ScriptJob> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(
            daemonFactory("M0Dev-ScriptWatchdog"));
    private static final Object WORKER_LOCK = new Object();
    private static ExecutorService worker = newWorker();

    // Indirections so the runner can be unit-tested without a running client.
    private static volatile Consumer<Runnable> mainThreadExecutor = task -> net.minecraft.client.Minecraft.getInstance().execute(task);
    private static volatile BooleanSupplier onMainThread = () -> net.minecraft.client.Minecraft.getInstance().isSameThread();

    private ScriptRunner() {
    }

    /**
     * Runs {@code source} with {@code manager}.
     *
     * @param callback receives the outcome on the client thread (directly for sync runs); may be {@code null}
     * @return the job, already finished for sync runs
     */
    public static ScriptJob run(@Nullable String name,
                                @NotNull ScriptManager manager,
                                @NotNull String source,
                                @Nullable Map<String, Object> context,
                                boolean async,
                                @Nullable Consumer<Outcome> callback) {
        ScriptJob job = new ScriptJob(name, async, async ? ASYNC_TIMEOUT_MS : SYNC_TIMEOUT_MS);
        job.setStoppable(manager.supportsTimeout());
        Map<String, Object> bindings = new LinkedHashMap<>();
        if (context != null) {
            bindings.putAll(context);
        }
        bindings.put("m0", new ScriptApi(job));
        ACTIVE.add(job);

        if (!async) {
            Outcome outcome = execute(job, manager, source, bindings);
            deliver(callback, outcome, false);
            return job;
        }

        Runnable task = () -> deliver(callback, execute(job, manager, source, bindings), true);
        synchronized (WORKER_LOCK) {
            worker.execute(new QueuedScript(job, task));
        }
        return job;
    }

    /** Stops all running and queued scripts. Returns how many were cancelled. */
    public static int cancelAll() {
        int count = 0;
        for (ScriptJob job : List.copyOf(ACTIVE)) {
            if (!job.isCancelled()) {
                job.cancel("was stopped");
                count++;
            }
        }
        return count;
    }

    /** Snapshot of scripts that are queued or running. */
    public static List<ScriptJob> activeJobs() {
        List<ScriptJob> jobs = new ArrayList<>(ACTIVE);
        jobs.sort((a, b) -> Long.compare(a.id(), b.id()));
        return jobs;
    }

    static boolean isOnMainThread() {
        return onMainThread.getAsBoolean();
    }

    static void runOnMainThread(Runnable task) {
        mainThreadExecutor.accept(task);
    }

    /** Test hook: replace the client-thread executor and thread check. */
    static void useMainThreadForTests(Consumer<Runnable> executor, BooleanSupplier isMainThread) {
        mainThreadExecutor = executor;
        onMainThread = isMainThread;
    }

    private static Outcome execute(ScriptJob job, ScriptManager manager, String source, Map<String, Object> bindings) {
        if (job.isCancelled()) {
            ACTIVE.remove(job);
            return new Outcome(job, false, null, "Script '" + job.name() + "' " + job.cancelReason() + ".");
        }

        ScriptJob previous = job.begin();
        ScheduledFuture<?> timeout = WATCHDOG.schedule(() -> onTimeout(job), job.timeoutMs(), TimeUnit.MILLISECONDS);
        try {
            Object result = manager.runScript(source, bindings, job);
            if (job.isCancelled()) {
                return new Outcome(job, false, null, "Script '" + job.name() + "' " + job.cancelReason() + ".");
            }
            return new Outcome(job, true, result, null);
        } catch (ScriptExecutionException e) {
            return new Outcome(job, false, null, cancelledOr(job, e.getMessage()));
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable e) {
            // Includes StackOverflowError from runaway recursion and LinkageError from missing engine classes.
            M0DevTools.LOGGER.error("Script '{}' failed", job.name(), e);
            return new Outcome(job, false, null, cancelledOr(job, ScriptExecutionException.wrap("Script", e).getMessage()));
        } finally {
            timeout.cancel(false);
            job.end(previous);
            ACTIVE.remove(job);
            if (job.isAsync()) {
                // Clear a pending interrupt so it does not leak into the next queued script.
                Thread.interrupted();
            }
        }
    }

    private static String cancelledOr(ScriptJob job, String message) {
        return job.isCancelled() ? "Script '" + job.name() + "' " + job.cancelReason() + "." : message;
    }

    private static void onTimeout(ScriptJob job) {
        if (!job.isRunning()) {
            return;
        }
        if (!job.isAsync() && !job.isStoppable()) {
            // e.g. Kotlin on the client thread: it cannot be interrupted, so don't throw away its result either.
            M0DevTools.LOGGER.warn("Script '{}' is taking longer than {} ms on the client thread; mark it '// @async on' if it is meant to run long.",
                    job.name(), job.timeoutMs());
            return;
        }
        job.cancel("timed out after " + (job.timeoutMs() / 1000L) + "s");
        if (job.isAsync()) {
            WATCHDOG.schedule(() -> {
                if (job.isRunning()) {
                    abandonWorker(job);
                }
            }, ABANDON_GRACE_MS, TimeUnit.MILLISECONDS);
        } else {
            M0DevTools.LOGGER.warn("Script '{}' exceeded {} ms on the client thread; mark it '// @async on' if it is meant to run long.",
                    job.name(), job.timeoutMs());
        }
    }

    /**
     * The worker is stuck in a script that ignores cancellation (e.g. a tight Kotlin loop). Leave that daemon thread
     * behind and move the queued scripts to a fresh worker so they are not blocked forever.
     */
    private static void abandonWorker(ScriptJob stuck) {
        List<Runnable> pending;
        synchronized (WORKER_LOCK) {
            pending = worker.shutdownNow();
            worker = newWorker();
            for (Runnable task : pending) {
                worker.execute(task);
            }
        }
        M0DevTools.LOGGER.warn("Script '{}' did not stop after cancellation; its worker thread was abandoned ({} queued script(s) moved).",
                stuck.name(), pending.size());
    }

    private static void deliver(@Nullable Consumer<Outcome> callback, Outcome outcome, boolean fromWorker) {
        if (!outcome.success()) {
            M0DevTools.LOGGER.warn("Script '{}': {}", outcome.job().name(), outcome.error());
        }
        if (callback == null) {
            return;
        }
        Runnable call = () -> {
            try {
                callback.accept(outcome);
            } catch (RuntimeException e) {
                M0DevTools.LOGGER.error("Script result handler failed for '{}'", outcome.job().name(), e);
            }
        };
        if (fromWorker) {
            runOnMainThread(call);
        } else {
            call.run();
        }
    }

    private static ExecutorService newWorker() {
        return Executors.newSingleThreadExecutor(daemonFactory("M0Dev-ScriptWorker-" + WORKER_COUNTER.incrementAndGet()));
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Wrapper so queued (not yet started) scripts can still be reported and cancelled. */
    private record QueuedScript(ScriptJob job, Runnable task) implements Runnable {
        @Override
        public void run() {
            task.run();
        }
    }
}
