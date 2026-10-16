package me.m0dii.modules.getdata;

import me.m0dii.utils.NbtExtractors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GetDataSyncServer {
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
    }

    private static void handleBlockRequest(ServerPlayer player, long requestId, BlockPos pos) {
        ServerLevel world = player.level();
        CompoundTag nbt = NbtExtractors.extractBlockData(world, pos);
        ServerPlayNetworking.send(player, new GetDataSyncPayloads.BlockNbtResponsePayload(requestId, pos, nbt));
    }
}

