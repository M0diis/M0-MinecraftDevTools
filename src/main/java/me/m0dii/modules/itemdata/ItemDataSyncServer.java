package me.m0dii.modules.itemdata;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
                context.server().execute(() -> handleSave(context.player(), payload.slotIndex(), payload.itemData())));
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

    private static void handleSave(ServerPlayer player, int slotIndex, CompoundTag itemData) {
        if (!isValidInventorySlot(slotIndex)) {
            player.sendSystemMessage(Component.literal("[ItemData] Invalid inventory slot."));
            return;
        }
        if (itemData == null || itemData.isEmpty()) {
            player.sendSystemMessage(Component.literal("[ItemData] Item data cannot be empty."));
            return;
        }

        ItemStack current = player.getInventory().getItem(slotIndex);
        if (current == null || current.isEmpty()) {
            player.sendSystemMessage(Component.literal("[ItemData] The target slot is empty."));
            return;
        }

        ItemStack decoded = ItemDataCodec.decode(itemData, player.level().registryAccess()).orElse(ItemStack.EMPTY);
        if (decoded.isEmpty()) {
            player.sendSystemMessage(Component.literal("[ItemData] Invalid item data."));
            return;
        }

        player.getInventory().setItem(slotIndex, decoded);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
        player.sendSystemMessage(Component.literal("[ItemData] Saved item data for " + describeSlot(slotIndex) + "."));
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
