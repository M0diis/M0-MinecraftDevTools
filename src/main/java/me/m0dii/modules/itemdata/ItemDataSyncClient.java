package me.m0dii.modules.itemdata;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ItemDataSyncClient {
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1L);
    private static final Map<Long, Consumer<ItemDataPayloads.ItemStackResponsePayload>> CALLBACKS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private ItemDataSyncClient() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(ItemDataPayloads.ItemStackResponsePayload.ID, (payload, context) -> {
            Consumer<ItemDataPayloads.ItemStackResponsePayload> callback = CALLBACKS.remove(payload.requestId());
            if (callback == null) {
                return;
            }
            context.client().execute(() -> callback.accept(payload));
        });
    }

    public static boolean requestItemData(int slotIndex, Consumer<ItemDataPayloads.ItemStackResponsePayload> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || callback == null) {
            return false;
        }

        long requestId = NEXT_REQUEST_ID.getAndIncrement();
        CALLBACKS.put(requestId, callback);
        ClientPlayNetworking.send(new ItemDataPayloads.ItemStackRequestPayload(requestId, slotIndex));
        return true;
    }

    public static boolean saveItemData(int slotIndex, NbtCompound itemData) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || itemData == null) {
            return false;
        }

        ClientPlayNetworking.send(new ItemDataPayloads.ItemStackSavePayload(slotIndex, itemData.copy()));
        return true;
    }
}
