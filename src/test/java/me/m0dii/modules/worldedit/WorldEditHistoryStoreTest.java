package me.m0dii.modules.worldedit;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WorldEditHistoryStoreTest {
    @Test
    void samePlayerAndDimensionCannotLeakHistoryBetweenServerInstances() {
        WorldEditHistoryStore<String> store = new WorldEditHistoryStore<>(15, 100, String::length);
        Object first = new String("server");
        Object second = new String("server");
        UUID owner = UUID.randomUUID();
        store.push(first, owner, "world A overworld edit");
        assertNull(store.peek(second, owner, true));
        store.push(second, owner, "world B overworld edit");
        store.clear(first);
        assertNull(store.peek(first, owner, true));
        assertEquals("world B overworld edit", store.peek(second, owner, true));
    }

    @Test
    void undoRedoAndNewEditPreserveStackSemantics() {
        WorldEditHistoryStore<String> store = new WorldEditHistoryStore<>(2, 100, String::length);
        Object server = new Object();
        UUID owner = UUID.randomUUID();
        store.push(server, owner, "one");
        store.push(server, owner, "two");
        store.push(server, owner, "three");
        store.complete(server, owner, true);
        assertEquals("two", store.peek(server, owner, true));
        assertEquals("three", store.peek(server, owner, false));
        store.complete(server, owner, false);
        assertEquals("three", store.peek(server, owner, true));
        store.complete(server, owner, true);
        store.push(server, owner, "four");
        assertNull(store.peek(server, owner, false));
        store.complete(server, owner, true);
        store.complete(server, owner, true);
        assertNull(store.peek(server, owner, true));
    }

    @Test
    void totalByteBudgetIncludesRedoAndAllPlayers() {
        WorldEditHistoryStore<String> store = new WorldEditHistoryStore<>(15, 6, String::length);
        Object server = new Object();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        store.push(server, a, "old");
        store.complete(server, a, true);
        store.push(server, b, "new!");
        assertNull(store.peek(server, a, false));
        assertEquals("new!", store.peek(server, b, true));
    }
}
