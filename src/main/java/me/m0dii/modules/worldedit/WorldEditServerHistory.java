package me.m0dii.modules.worldedit;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

final class WorldEditServerHistory {
    private static final int MAX_HISTORY = 15;
    private static final Map<UUID, HistoryState> HISTORIES = new HashMap<>();

    private WorldEditServerHistory() {
    }

    static void pushUndo(ServerPlayerEntity player, String label, RegistryKey<World> worldKey, List<WorldEditServerChange> changes) {
        if (changes.isEmpty()) {
            return;
        }

        HistoryState state = HISTORIES.computeIfAbsent(player.getUuid(), ignored -> new HistoryState());
        state.undo.push(new HistoryEntry(label, worldKey, List.copyOf(changes)));
        trim(state.undo);
        state.redo.clear();
    }

    static boolean undo(ServerPlayerEntity player) {
        HistoryState state = HISTORIES.get(player.getUuid());
        if (state == null || state.undo.isEmpty()) {
            player.sendMessage(Text.literal("[WE] Nothing to undo."), false);
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

    static boolean redo(ServerPlayerEntity player) {
        HistoryState state = HISTORIES.get(player.getUuid());
        if (state == null || state.redo.isEmpty()) {
            player.sendMessage(Text.literal("[WE] Nothing to redo."), false);
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

    private static boolean applyHistoryEntry(ServerPlayerEntity player, HistoryEntry entry, boolean undo) {
        ServerWorld world = player.getCommandSource().getServer().getWorld(entry.worldKey());
        if (world == null) {
            player.sendMessage(Text.literal("[WE] The world for that history entry is no longer available."), false);
            return false;
        }

        if (!areChunksLoaded(world, entry.changes())) {
            player.sendMessage(Text.literal("[WE] Load the affected chunks first before " + (undo ? "undo" : "redo") + "."), false);
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
            player.sendMessage(Text.literal("[WE] " + (undo ? "Undo" : "Redo") + " failed: " + e.getMessage()), false);
            return false;
        }

        player.sendMessage(Text.literal("[WE] " + (undo ? "Undid " : "Redid ") + entry.label() + " (" + entry.changes().size() + " block change(s))."), false);
        return true;
    }

    private static void rollbackHistory(ServerWorld world, List<WorldEditServerChange> applied, boolean undo) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            WorldEditServerChange change = applied.get(i);
            if (undo) {
                change.after().apply(world, change.pos());
            } else {
                change.before().apply(world, change.pos());
            }
        }
    }

    private static boolean areChunksLoaded(ServerWorld world, List<WorldEditServerChange> changes) {
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (WorldEditServerChange change : changes) {
            BlockPos pos = change.pos();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            if (seen.add(chunkKey) && !world.isChunkLoaded(chunkX, chunkZ)) {
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

    private record HistoryEntry(String label, RegistryKey<World> worldKey, List<WorldEditServerChange> changes) {
    }

    record WorldEditServerChange(BlockPos pos, WorldEditBlockSnapshot before, WorldEditBlockSnapshot after) {
    }
}
