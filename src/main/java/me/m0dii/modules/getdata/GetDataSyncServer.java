package me.m0dii.modules.getdata;

import me.m0dii.utils.NbtExtractors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

    private static void handleBlockRequest(ServerPlayerEntity player, long requestId, BlockPos pos) {
        ServerWorld world = player.getEntityWorld();
        NbtCompound nbt = NbtExtractors.extractBlockData(world, pos);
        ServerPlayNetworking.send(player, new GetDataSyncPayloads.BlockNbtResponsePayload(requestId, pos, nbt));
    }
}

