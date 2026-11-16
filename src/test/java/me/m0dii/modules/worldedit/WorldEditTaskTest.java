package me.m0dii.modules.worldedit;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class WorldEditTaskTest {
    @Test
    void phasesAreLazyAndWorkIsSplitAcrossTicks() {
        List<Integer> source = new ArrayList<>();
        List<Integer> output = new ArrayList<>();
        AtomicInteger completions = new AtomicInteger();
        WorldEditTask task = new WorldEditTask(Collections::emptyIterator, completions::incrementAndGet, e -> fail(e));
        task.add(List.of(1, 2, 3), source::add);
        task.add(source, output::add);
        assertFalse(task.tick(2, Long.MAX_VALUE));
        assertEquals(List.of(1, 2), source);
        assertTrue(output.isEmpty());
        assertEquals(0, completions.get());
        while (!task.tick(2, Long.MAX_VALUE)) { }
        assertEquals(List.of(1, 2, 3), output);
        assertEquals(1, completions.get());
        assertTrue(task.tick(2, Long.MAX_VALUE));
        assertEquals(1, completions.get());
    }

    @Test
    void failedEditRollsBackIncrementallyAndNeverCommits() {
        List<Integer> changed = new ArrayList<>();
        List<Integer> restored = new ArrayList<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        WorldEditTask task = new WorldEditTask(() -> WorldEditTask.work(changed.reversed(), restored::add),
                () -> fail("Failed edit committed"), error::set);
        task.add(List.of(1, 2, 3), value -> {
            changed.add(value);
            if (value == 3) throw new IllegalStateException("placement failed");
        });
        assertFalse(task.tick(3, Long.MAX_VALUE));
        assertTrue(restored.isEmpty());
        assertFalse(task.tick(1, Long.MAX_VALUE));
        assertEquals(List.of(3), restored);
        while (!task.tick(1, Long.MAX_VALUE)) { }
        assertEquals(List.of(3, 2, 1), restored);
        assertEquals("placement failed", error.get().getMessage());
    }

    @Test
    void cancellationAndRollbackErrorsDoNotSkipRemainingRestores() {
        List<Integer> restored = new ArrayList<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        WorldEditTask task = new WorldEditTask(() -> WorldEditTask.work(List.of(1, 2), value -> {
            if (value == 1) throw new IllegalStateException("restore failed");
            restored.add(value);
        }), () -> fail("Cancelled task committed"), error::set);
        task.add(List.of(0), ignored -> fail("Cancelled action executed"));
        task.fail(new IllegalStateException("disconnected"));
        while (!task.tick(1, Long.MAX_VALUE)) { }
        assertEquals(List.of(2), restored);
        assertEquals(1, error.get().getSuppressed().length);
    }
}
