package me.m0dii.modules.itemdata;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ItemDataSyncClient {
    private static final AtomicLong NEXT_REQUEST_ID = new AtomicLong(1L);
    private static final Map<Long, Consumer<ItemDataPayloads.ItemStackResponsePayload>> CALLBACKS = new ConcurrentHashMap<>();
    private record PendingSave(long deadline, Consumer<ItemDataPayloads.ItemStackSaveResultPayload> callback) { }
    private static final Map<Long, PendingSave> SAVES = new ConcurrentHashMap<>();
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
            var connection = context.client().getConnection();
            context.client().execute(() -> {
                if (context.client().getConnection() == connection) callback.accept(payload);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ItemDataPayloads.ItemStackSaveResultPayload.ID, (payload, context) -> {
            PendingSave pending = SAVES.remove(payload.requestId());
            if (pending != null) {
                var connection = context.client().getConnection();
                context.client().execute(() -> {
                    if (context.client().getConnection() == connection) {
                        pending.callback().accept(payload);
                    }
                });
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CALLBACKS.clear();
            SAVES.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> SAVES.forEach((id, pending) -> {
            if (System.nanoTime() >= pending.deadline() && SAVES.remove(id, pending)) {
                pending.callback().accept(new ItemDataPayloads.ItemStackSaveResultPayload(id, false,
                        "Save confirmation timed out. Reopen the editor to check the item.", new CompoundTag()));
            }
        }));
    }

    public static boolean requestItemData(int slotIndex, Consumer<ItemDataPayloads.ItemStackResponsePayload> callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || callback == null || !ClientPlayNetworking.canSend(ItemDataPayloads.ItemStackRequestPayload.ID)) {
            return false;
        }

        long requestId = NEXT_REQUEST_ID.getAndIncrement();
        CALLBACKS.put(requestId, callback);
        ClientPlayNetworking.send(new ItemDataPayloads.ItemStackRequestPayload(requestId, slotIndex));
        return true;
    }

    public static boolean saveItemData(int slotIndex, CompoundTag expectedData, CompoundTag itemData,
                                      Consumer<ItemDataPayloads.ItemStackSaveResultPayload> callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || expectedData == null || itemData == null || callback == null
                || !ClientPlayNetworking.canSend(ItemDataPayloads.ItemStackSavePayload.ID)) {
            return false;
        }

        long requestId = NEXT_REQUEST_ID.getAndIncrement();
        SAVES.put(requestId, new PendingSave(System.nanoTime() + 10_000_000_000L, callback));
        ClientPlayNetworking.send(new ItemDataPayloads.ItemStackSavePayload(requestId, slotIndex, expectedData.copy(), itemData.copy()));
        return true;
    }
}
