package me.m0dii.modules.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.*;

final class WorldEditServerHistory {
    private static final WorldEditHistoryStore<HistoryEntry> HISTORIES = new WorldEditHistoryStore<>(
            15, 64L * 1024 * 1024, HistoryEntry::estimatedBytes);

    private WorldEditServerHistory() { }

    static void clear(MinecraftServer server) {
        HISTORIES.clear(server);
    }

    static void pushUndo(ServerPlayer player, String label, ResourceKey<Level> worldKey, List<WorldEditServerChange> changes) {
        if (!changes.isEmpty()) {
            HISTORIES.push(player.level().getServer(), player.getUUID(), new HistoryEntry(label, worldKey, List.copyOf(changes)));
        }
    }

    static boolean undo(ServerPlayer player) {
        return applyHistory(player, true);
    }

    static boolean redo(ServerPlayer player) {
        return applyHistory(player, false);
    }

    private static boolean applyHistory(ServerPlayer player, boolean undo) {
        MinecraftServer server = player.level().getServer();
        HistoryEntry entry = HISTORIES.peek(server, player.getUUID(), undo);
        if (entry == null) {
            player.sendSystemMessage(Component.literal("[WE] Nothing to " + (undo ? "undo" : "redo") + "."));
            return false;
        }
        ServerLevel world = server.getLevel(entry.worldKey());
        if (world == null) {
            player.sendSystemMessage(Component.literal("[WE] The world for that history entry is no longer available."));
            return false;
        }

        List<WorldEditServerChange> ordered = undo ? entry.changes().reversed() : entry.changes();
        List<WorldEditServerChange> applied = new ArrayList<>();
        long[] rollbackBytes = {0};
        WorldEditTask task = new WorldEditTask(
                () -> WorldEditTask.work(applied.reversed(), change -> {
                    WorldEditBlockSnapshot restore = change.before();
                    if (!restore.apply(world, change.pos()) && !restore.matches(world, change.pos())) {
                        throw new IllegalStateException("Failed to roll back " + change.pos().toShortString());
                    }
                }),
                () -> {
                    HISTORIES.complete(server, player.getUUID(), undo);
                    player.sendSystemMessage(Component.literal("[WE] " + (undo ? "Undid " : "Redid ") + entry.label()
                            + " (" + entry.changes().size() + " block change(s))."));
                },
                error -> WorldEditSyncServer.reportFailure(player, undo ? "Undo" : "Redo", error));
        // Preflight all chunks before modifying anything, with the same tick budget as the edit.
        task.add(ordered, change -> requireLoaded(world, change.pos()));
        task.add(ordered, change -> {
            requireLoaded(world, change.pos());
            WorldEditBlockSnapshot target = undo ? change.before() : change.after();
            WorldEditBlockSnapshot before = WorldEditBlockSnapshot.capture(world, change.pos());
            if (!target.equals(before)) {
                WorldEditServerChange rollback = new WorldEditServerChange(change.pos(), before, target);
                rollbackBytes[0] += rollback.estimatedBytes();
                if (rollbackBytes[0] > WorldEditLimits.MAX_SNAPSHOT_BYTES) {
                    throw new IllegalStateException("Undo/redo exceeds the 16 MiB rollback snapshot limit.");
                }
                applied.add(rollback);
                if (!target.apply(world, change.pos()) && !target.matches(world, change.pos())) {
                    throw new IllegalStateException("Failed to restore " + change.pos().toShortString());
                }
            }
        });
        WorldEditTasks.submit(player, task);
        return true;
    }

    private static void requireLoaded(ServerLevel world, BlockPos pos) {
        if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            throw new IllegalStateException("Load the affected chunks first.");
        }
    }

    private record HistoryEntry(String label, ResourceKey<Level> worldKey, List<WorldEditServerChange> changes) {
        long estimatedBytes() {
            return changes.stream().mapToLong(WorldEditServerChange::estimatedBytes).sum();
        }
    }

    record WorldEditServerChange(BlockPos pos, WorldEditBlockSnapshot before, WorldEditBlockSnapshot after) {
        long estimatedBytes() {
            return 64L + before.estimatedBytes() + after.estimatedBytes();
        }
    }
}
