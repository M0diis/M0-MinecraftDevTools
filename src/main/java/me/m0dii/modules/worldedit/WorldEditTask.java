package me.m0dii.modules.worldedit;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Incremental work with a separately budgeted rollback when any step fails. */
final class WorldEditTask {
    private final Deque<Iterator<Runnable>> phases = new ArrayDeque<>();
    private final Supplier<Iterator<Runnable>> rollback;
    private final Runnable success;
    private final Consumer<RuntimeException> failure;
    private RuntimeException error;
    private boolean done;

    WorldEditTask(Supplier<Iterator<Runnable>> rollback, Runnable success, Consumer<RuntimeException> failure) {
        this.rollback = rollback;
        this.success = success;
        this.failure = failure;
    }

    <T> void add(Iterable<T> input, Consumer<T> action) {
        phases.add(work(input, action));
    }

    static <T> Iterator<Runnable> work(Iterable<T> input, Consumer<T> action) {
        return new Iterator<>() {
            private Iterator<T> iterator;
            private Iterator<T> iterator() {
                if (iterator == null) iterator = input.iterator();
                return iterator;
            }
            public boolean hasNext() { return iterator().hasNext(); }
            public Runnable next() {
                T value = iterator().next();
                return () -> action.accept(value);
            }
        };
    }

    void fail(RuntimeException cause) {
        if (done || error != null) {
            return;
        }
        error = cause;
        phases.clear();
        phases.add(rollback.get());
    }

    boolean tick(int maxSteps, long budgetNanos) {
        long start = System.nanoTime();
        for (int steps = 0; !done && steps < maxSteps && System.nanoTime() - start < budgetNanos; steps++) {
            try {
                while (!phases.isEmpty() && !phases.peek().hasNext()) {
                    phases.remove();
                }
                if (phases.isEmpty()) {
                    if (error == null) {
                        success.run();
                    } else {
                        done = true;
                        failure.accept(error);
                    }
                    done = true;
                } else {
                    phases.peek().next().run();
                }
            } catch (RuntimeException e) {
                if (error == null) {
                    fail(e);
                } else {
                    // Keep restoring the remaining blocks even if an individual restore fails.
                    error.addSuppressed(e);
                }
            }
        }
        return done;
    }
}
