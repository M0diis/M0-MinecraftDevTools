package me.m0dii.modules.itemdata;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

    private static void handleRequest(ServerPlayerEntity player, long requestId, int slotIndex) {
        if (!isValidInventorySlot(slotIndex)) {
            ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, false, new NbtCompound()));
            return;
        }

        ItemStack stack = player.getInventory().getStack(slotIndex);
        if (stack == null || stack.isEmpty()) {
            ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, false, new NbtCompound()));
            return;
        }

        NbtCompound itemData = ItemDataCodec.encodeOrEmpty(stack, player.getEntityWorld().getRegistryManager());
        ServerPlayNetworking.send(player, new ItemDataPayloads.ItemStackResponsePayload(requestId, slotIndex, true, itemData));
    }

    private static void handleSave(ServerPlayerEntity player, int slotIndex, NbtCompound itemData) {
        if (!isValidInventorySlot(slotIndex)) {
            player.sendMessage(Text.literal("[ItemData] Invalid inventory slot."), false);
            return;
        }
        if (itemData == null || itemData.isEmpty()) {
            player.sendMessage(Text.literal("[ItemData] Item data cannot be empty."), false);
            return;
        }

        ItemStack current = player.getInventory().getStack(slotIndex);
        if (current == null || current.isEmpty()) {
            player.sendMessage(Text.literal("[ItemData] The target slot is empty."), false);
            return;
        }

        ItemStack decoded = ItemDataCodec.decode(itemData, player.getEntityWorld().getRegistryManager()).orElse(ItemStack.EMPTY);
        if (decoded.isEmpty()) {
            player.sendMessage(Text.literal("[ItemData] Invalid item data."), false);
            return;
        }

        player.getInventory().setStack(slotIndex, decoded);
        player.playerScreenHandler.sendContentUpdates();
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.currentScreenHandler.sendContentUpdates();
        }
        player.sendMessage(Text.literal("[ItemData] Saved item data for " + describeSlot(slotIndex) + "."), false);
    }

    private static boolean isValidInventorySlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex <= PlayerInventory.OFF_HAND_SLOT;
    }

    private static String describeSlot(int slotIndex) {
        if (slotIndex == PlayerInventory.OFF_HAND_SLOT) {
            return "offhand";
        }
        if (slotIndex < PlayerInventory.HOTBAR_SIZE) {
            return "hotbar slot " + (slotIndex + 1);
        }
        if (slotIndex < PlayerInventory.MAIN_SIZE) {
            return "inventory slot " + (slotIndex - PlayerInventory.HOTBAR_SIZE + 1);
        }
        return "slot " + slotIndex;
    }
}
