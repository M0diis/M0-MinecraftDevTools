package me.m0dii.modules.getdata;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class GetDataSyncClient {
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1L);
    private static final Map<Long, Consumer<NbtCompound>> BLOCK_CALLBACKS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private GetDataSyncClient() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(GetDataSyncPayloads.BlockNbtResponsePayload.ID, (payload, context) -> {
            Consumer<NbtCompound> callback = BLOCK_CALLBACKS.remove(payload.requestId());
            if (callback == null) {
                return;
            }
            context.client().execute(() -> callback.accept(payload.nbt() == null ? new NbtCompound() : payload.nbt()));
        });
    }

    public static boolean requestBlockNbt(BlockPos pos, Consumer<NbtCompound> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || pos == null || callback == null) {
            return false;
        }

        long requestId = NEXT_REQUEST_ID.getAndIncrement();
        BLOCK_CALLBACKS.put(requestId, callback);

        ClientPlayNetworking.send(new GetDataSyncPayloads.BlockNbtRequestPayload(requestId, pos));
        return true;
    }
}

