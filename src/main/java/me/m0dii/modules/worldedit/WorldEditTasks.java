package me.m0dii.modules.worldedit;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Map;

/** Serializes edits per server so an undo cannot interleave with an unfinished edit. */
final class WorldEditTasks {
    private record Active(ServerPlayer player, WorldEditTask task) { }
    private static final Map<MinecraftServer, Active> ACTIVE = new IdentityHashMap<>();

    private WorldEditTasks() { }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Active active = ACTIVE.get(server);
            if (active != null) {
                if (server.getPlayerList().getPlayer(active.player().getUUID()) != active.player()) {
                    active.task().fail(new IllegalStateException("Player disconnected; edit cancelled."));
                }
                if (active.task().tick(512, 5_000_000L)) {
                    ACTIVE.remove(server);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Active active = ACTIVE.remove(server);
            if (active != null) {
                active.task().fail(new IllegalStateException("Server stopping; edit cancelled."));
                // Restore before the server saves its worlds.
                while (!active.task().tick(4096, Long.MAX_VALUE)) { }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE.remove(server);
            WorldEditServerHistory.clear(server);
        });
    }

    static boolean isBusy(ServerPlayer player) {
        if (ACTIVE.containsKey(player.level().getServer())) {
            player.sendSystemMessage(Component.literal("[WE] Another edit is still running. Wait for it to finish."));
            return true;
        }
        return false;
    }

    static void submit(ServerPlayer player, WorldEditTask task) {
        ACTIVE.put(player.level().getServer(), new Active(player, task));
    }
}
