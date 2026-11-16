package me.m0dii.modules.worldedit;

import java.util.*;
import java.util.function.ToLongFunction;

/** Identity-scoped, byte-bounded undo storage. Moving entries between stacks does not duplicate them. */
final class WorldEditHistoryStore<T> {
    private record Entry<T>(long sequence, long bytes, T value) { }
    private static final class State<T> {
        final Deque<Entry<T>> undo = new ArrayDeque<>();
        final Deque<Entry<T>> redo = new ArrayDeque<>();
    }
    private final Map<Object, Map<UUID, State<T>>> scopes = new IdentityHashMap<>();
    private final int maxEntries;
    private final long maxBytes;
    private final ToLongFunction<T> size;
    private long sequence;

    WorldEditHistoryStore(int maxEntries, long maxBytes, ToLongFunction<T> size) {
        this.maxEntries = maxEntries;
        this.maxBytes = maxBytes;
        this.size = size;
    }

    void push(Object scope, UUID owner, T value) {
        Map<UUID, State<T>> histories = scopes.computeIfAbsent(scope, ignored -> new HashMap<>());
        State<T> state = histories.computeIfAbsent(owner, ignored -> new State<>());
        state.redo.clear();
        state.undo.push(new Entry<>(++sequence, size.applyAsLong(value), value));
        while (state.undo.size() > maxEntries) {
            state.undo.removeLast();
        }
        while (bytes(histories) > maxBytes) {
            Deque<Entry<T>> oldest = null;
            Entry<T> oldestEntry = null;
            for (State<T> candidate : histories.values()) {
                for (Deque<Entry<T>> stack : List.of(candidate.undo, candidate.redo)) {
                    for (Entry<T> entry : stack) {
                        if (oldestEntry == null || entry.sequence() < oldestEntry.sequence()) {
                            oldest = stack;
                            oldestEntry = entry;
                        }
                    }
                }
            }
            if (oldest == null) break;
            oldest.remove(oldestEntry);
        }
        histories.values().removeIf(history -> history.undo.isEmpty() && history.redo.isEmpty());
    }

    T peek(Object scope, UUID owner, boolean undo) {
        State<T> state = scopes.getOrDefault(scope, Map.of()).get(owner);
        if (state == null) return null;
        Entry<T> entry = (undo ? state.undo : state.redo).peek();
        return entry == null ? null : entry.value();
    }

    void complete(Object scope, UUID owner, boolean undo) {
        State<T> state = scopes.get(scope).get(owner);
        (undo ? state.redo : state.undo).push((undo ? state.undo : state.redo).pop());
    }

    void clear(Object scope) {
        scopes.remove(scope);
    }

    private long bytes(Map<UUID, State<T>> histories) {
        long bytes = 0;
        for (State<T> state : histories.values()) {
            for (Entry<T> entry : state.undo) bytes += entry.bytes();
            for (Entry<T> entry : state.redo) bytes += entry.bytes();
        }
        return bytes;
    }
}
