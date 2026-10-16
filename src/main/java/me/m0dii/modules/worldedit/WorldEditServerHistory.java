package me.m0dii.modules.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.*;

final class WorldEditServerHistory {
    private static final int MAX_HISTORY = 15;
    private static final Map<UUID, HistoryState> HISTORIES = new HashMap<>();

    private WorldEditServerHistory() {
    }

    static void pushUndo(ServerPlayer player, String label, ResourceKey<Level> worldKey, List<WorldEditServerChange> changes) {
        if (changes.isEmpty()) {
            return;
        }

        HistoryState state = HISTORIES.computeIfAbsent(player.getUUID(), ignored -> new HistoryState());
        state.undo.push(new HistoryEntry(label, worldKey, List.copyOf(changes)));
        trim(state.undo);
        state.redo.clear();
    }

    static boolean undo(ServerPlayer player) {
        HistoryState state = HISTORIES.get(player.getUUID());
        if (state == null || state.undo.isEmpty()) {
            player.sendSystemMessage(Component.literal("[WE] Nothing to undo."));
            return false;
        }

        HistoryEntry entry = state.undo.pop();
        if (!applyHistoryEntry(player, entry, true)) {
            state.undo.push(entry);
            return false;
        }

        state.redo.push(entry);
        trim(state.redo);
        return true;
    }

    static boolean redo(ServerPlayer player) {
        HistoryState state = HISTORIES.get(player.getUUID());
        if (state == null || state.redo.isEmpty()) {
            player.sendSystemMessage(Component.literal("[WE] Nothing to redo."));
            return false;
        }

        HistoryEntry entry = state.redo.pop();
        if (!applyHistoryEntry(player, entry, false)) {
            state.redo.push(entry);
            return false;
        }

        state.undo.push(entry);
        trim(state.undo);
        return true;
    }

    private static boolean applyHistoryEntry(ServerPlayer player, HistoryEntry entry, boolean undo) {
        ServerLevel world = player.createCommandSourceStack().getServer().getLevel(entry.worldKey());
        if (world == null) {
            player.sendSystemMessage(Component.literal("[WE] The world for that history entry is no longer available."));
            return false;
        }

        if (!areChunksLoaded(world, entry.changes())) {
            player.sendSystemMessage(Component.literal("[WE] Load the affected chunks first before " + (undo ? "undo" : "redo") + "."));
            return false;
        }

        List<WorldEditServerChange> applied = new ArrayList<>();
        try {
            if (undo) {
                for (int i = entry.changes().size() - 1; i >= 0; i--) {
                    WorldEditServerChange change = entry.changes().get(i);
                    if (change.before().matches(world, change.pos())) {
                        continue;
                    }
                    if (!change.before().apply(world, change.pos()) && !change.before().matches(world, change.pos())) {
                        throw new IllegalStateException("Failed to restore " + change.pos().toShortString());
                    }
                    applied.add(change);
                }
            } else {
                for (WorldEditServerChange change : entry.changes()) {
                    if (change.after().matches(world, change.pos())) {
                        continue;
                    }
                    if (!change.after().apply(world, change.pos()) && !change.after().matches(world, change.pos())) {
                        throw new IllegalStateException("Failed to reapply " + change.pos().toShortString());
                    }
                    applied.add(change);
                }
            }
        } catch (RuntimeException e) {
            rollbackHistory(world, applied, undo);
            player.sendSystemMessage(Component.literal("[WE] " + (undo ? "Undo" : "Redo") + " failed: " + e.getMessage()));
            return false;
        }

        player.sendSystemMessage(Component.literal("[WE] " + (undo ? "Undid " : "Redid ") + entry.label() + " (" + entry.changes().size() + " block change(s))."));
        return true;
    }

    private static void rollbackHistory(ServerLevel world, List<WorldEditServerChange> applied, boolean undo) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            WorldEditServerChange change = applied.get(i);
            if (undo) {
                change.after().apply(world, change.pos());
            } else {
                change.before().apply(world, change.pos());
            }
        }
    }

    private static boolean areChunksLoaded(ServerLevel world, List<WorldEditServerChange> changes) {
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (WorldEditServerChange change : changes) {
            BlockPos pos = change.pos();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            if (seen.add(chunkKey) && !world.hasChunk(chunkX, chunkZ)) {
                return false;
            }
        }
        return true;
    }

    private static void trim(Deque<HistoryEntry> stack) {
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    private record HistoryState(Deque<HistoryEntry> undo, Deque<HistoryEntry> redo) {
        private HistoryState() {
            this(new ArrayDeque<>(), new ArrayDeque<>());
        }
    }

    private record HistoryEntry(String label, ResourceKey<Level> worldKey, List<WorldEditServerChange> changes) {
    }

    record WorldEditServerChange(BlockPos pos, WorldEditBlockSnapshot before, WorldEditBlockSnapshot after) {
    }
}
