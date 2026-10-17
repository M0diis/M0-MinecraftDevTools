package me.m0dii.modules.getdata;

import me.m0dii.utils.NbtExtractors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves block NBT to the /getdata client command.
 * <p>
 * Block-entity data (chest contents, command block commands, ...) is privileged information, so requests are only
 * answered for players with permission level 2 (or the owner of a singleplayer world), only for chunks that are
 * already loaded (never forcing a chunk load or generation), and at a bounded rate. Denied requests get an empty tag,
 * which the client treats as "no server data" and falls back to what it already has.
 */
public final class GetDataSyncServer {
    /** Maximum requests per player per window. */
    static final int MAX_REQUESTS_PER_WINDOW = 20;
    static final long WINDOW_MS = 1000L;

    private static final Map<UUID, RequestRateLimiter> LIMITERS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private GetDataSyncServer() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ServerPlayNetworking.registerGlobalReceiver(GetDataSyncPayloads.BlockNbtRequestPayload.ID, (payload, context) -> {
            context.server().execute(() -> handleBlockRequest(context.player(), payload.requestId(), payload.pos()));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LIMITERS.remove(handler.player.getUUID()));
    }

    private static void handleBlockRequest(ServerPlayer player, long requestId, BlockPos pos) {
        if (player == null || pos == null) {
            return;
        }

        RequestRateLimiter limiter = LIMITERS.computeIfAbsent(player.getUUID(),
                ignored -> new RequestRateLimiter(MAX_REQUESTS_PER_WINDOW, WINDOW_MS));
        if (!limiter.tryAcquire(System.currentTimeMillis())) {
            deny(player, requestId, pos, null);
            return;
        }

        ServerLevel world = player.level();
        if (!isAllowed(player, world)) {
            deny(player, requestId, pos, null);
            return;
        }

        if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            deny(player, requestId, pos, "[GetData] That block is in an unloaded chunk.");
            return;
        }

        CompoundTag nbt = NbtExtractors.extractBlockData(world, pos);
        ServerPlayNetworking.send(player, new GetDataSyncPayloads.BlockNbtResponsePayload(requestId, pos, nbt));
    }

    private static boolean isAllowed(ServerPlayer player, ServerLevel world) {
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) {
            return true;
        }
        // Singleplayer without cheats: the only player on an integrated server is the world owner.
        var server = world.getServer();
        return server != null && !server.isDedicatedServer() && server.getPlayerCount() == 1;
    }

    private static void deny(ServerPlayer player, long requestId, BlockPos pos, String message) {
        if (message != null) {
            player.sendSystemMessage(Component.literal(message));
        }
        // Always answer so the client drops its pending callback.
        ServerPlayNetworking.send(player, new GetDataSyncPayloads.BlockNbtResponsePayload(requestId, pos, new CompoundTag()));
    }
}
