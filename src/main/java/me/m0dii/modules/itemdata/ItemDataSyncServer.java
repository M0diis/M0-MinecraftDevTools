package me.m0dii.modules.itemdata;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ItemDataSyncServer {
    private static boolean registered = false;

    private ItemDataSyncServer() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ServerPlayNetworking.registerGlobalReceiver(ItemDataPayloads.ItemStackRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> handleRequest(context.player(), payload.requestId(), payload.slotIndex())));
        ServerPlayNetworking.registerGlobalReceiver(ItemDataPayloads.ItemStackSavePayload.ID, (payload, context) ->
                context.server().execute(() -> handleSave(context.player(), payload)));
    }

    private static void handleRequest(ServerPlayer player, long requestId, int slotIndex) {
        if (!isValidInventorySlot(slotIndex)) {
            ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, false, new CompoundTag()));
            return;
        }

        ItemStack stack = player.getInventory().getItem(slotIndex);
        if (stack == null || stack.isEmpty()) {
            ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, false, new CompoundTag()));
            return;
        }

        CompoundTag itemData = ItemDataCodec.encodeOrEmpty(stack, player.level().registryAccess());
        ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, true, itemData));
    }

    private static void handleSave(ServerPlayer player, ItemDataPayloads.ItemStackSavePayload payload) {
        int slotIndex = payload.slotIndex();
        CompoundTag itemData = payload.itemData();
        if (!isValidInventorySlot(slotIndex)) {
            saveResult(player, payload, false, "Invalid inventory slot.", new CompoundTag());
            return;
        }
        if (itemData == null || itemData.isEmpty()) {
            saveResult(player, payload, false, "Item data cannot be empty.", new CompoundTag());
            return;
        }

        ItemStack current = player.getInventory().getItem(slotIndex);
        if (current == null || current.isEmpty()) {
            saveResult(player, payload, false, "The target slot is empty. Reopen the editor.", new CompoundTag());
            return;
        }

        CompoundTag currentData = ItemDataCodec.encodeOrEmpty(current, player.level().registryAccess());
        if (!matchesExpected(payload.expectedData(), currentData)) {
            saveResult(player, payload, false, "The item changed while editing. Reopen the editor before saving.", currentData);
            return;
        }

        ItemStack decoded = ItemDataCodec.decode(itemData, player.level().registryAccess()).orElse(ItemStack.EMPTY);
        if (decoded.isEmpty()) {
            saveResult(player, payload, false, "Invalid item data.", currentData);
            return;
        }

        player.getInventory().setItem(slotIndex, decoded);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
        saveResult(player, payload, true, "Saved item data for " + describeSlot(slotIndex) + ".",
                ItemDataCodec.encodeOrEmpty(decoded, player.level().registryAccess()));
    }

    static boolean matchesExpected(CompoundTag expected, CompoundTag current) {
        return expected != null && !expected.isEmpty() && expected.equals(current);
    }

    private static void saveResult(ServerPlayer player, ItemDataPayloads.ItemStackSavePayload payload,
                                   boolean success, String message, CompoundTag data) {
        ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackSaveResultPayload(payload.requestId(), success, message, data));
    }

    private static boolean isValidInventorySlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex <= Inventory.SLOT_OFFHAND;
    }

    private static String describeSlot(int slotIndex) {
        if (slotIndex == Inventory.SLOT_OFFHAND) {
            return "offhand";
        }
        if (slotIndex < Inventory.SELECTION_SIZE) {
            return "hotbar slot " + (slotIndex + 1);
        }
        if (slotIndex < Inventory.INVENTORY_SIZE) {
            return "inventory slot " + (slotIndex - Inventory.SELECTION_SIZE + 1);
        }
        return "slot " + slotIndex;
    }
}
