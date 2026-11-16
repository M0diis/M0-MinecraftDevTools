package me.m0dii.modules.macros;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PendingMacrosTest {
    @AfterEach
    void clear() { CommandMacros.clearPendingRuns(); }

    @Test
    void delayedCommandCannotRunOnAnotherConnection() {
        Object first = new Object();
        List<String> sent = new ArrayList<>();
        CommandMacros.queuePendingRun("test", List.of("/setblock 0 64 0 stone"), 2, first);
        CommandMacros.tickPendingRuns(first, (name, commands) -> sent.addAll(commands));
        CommandMacros.tickPendingRuns(new Object(), (name, commands) -> sent.addAll(commands));
        CommandMacros.tickPendingRuns(first, (name, commands) -> sent.addAll(commands));
        assertTrue(sent.isEmpty());
        assertTrue(CommandMacros.getPendingDisplayLines().isEmpty());
    }

    @Test
    void disconnectDiscardsCountdownButNormalRunsStillFireOnce() {
        Object connection = new Object();
        List<String> sent = new ArrayList<>();
        CommandMacros.queuePendingRun("old", List.of("old"), 1, connection);
        CommandMacros.clearPendingRuns();
        CommandMacros.queuePendingRun("new", List.of("new"), 2, connection);
        CommandMacros.tickPendingRuns(connection, (name, commands) -> sent.addAll(commands));
        assertTrue(sent.isEmpty());
        CommandMacros.tickPendingRuns(connection, (name, commands) -> sent.addAll(commands));
        CommandMacros.tickPendingRuns(connection, (name, commands) -> sent.addAll(commands));
        assertEquals(List.of("new"), sent);
        assertFalse(CommandMacros.queuePendingRun("offline", List.of("no"), 1, null));
    }
}
