package me.m0dii.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class PickBlockHotbarSlotResolver {
    private PickBlockHotbarSlotResolver() {
    }

    public static int resolveTargetSlot(PlayerInventory inventory, ItemStack target) {
        int selectedSlot = Math.clamp(inventory.getSelectedSlot(), 0, PlayerInventory.HOTBAR_SIZE - 1);
        int firstEmptySlot = -1;

        for (int slot = 0; slot < PlayerInventory.HOTBAR_SIZE; slot++) {
            ItemStack existing = inventory.getStack(slot);
            if (ItemStack.areItemsAndComponentsEqual(existing, target)) {
                return slot;
            }
            if (firstEmptySlot < 0 && existing.isEmpty()) {
                firstEmptySlot = slot;
            }
        }

        return firstEmptySlot >= 0 ? firstEmptySlot : selectedSlot;
    }

    static int resolveTargetSlot(List<ItemStack> hotbarStacks, int selectedSlot, ItemStack target) {
        int fallbackSlot = Math.clamp(selectedSlot, 0, PlayerInventory.HOTBAR_SIZE - 1);
        int firstEmptySlot = -1;
        int limit = Math.min(hotbarStacks.size(), PlayerInventory.HOTBAR_SIZE);

        for (int slot = 0; slot < limit; slot++) {
            ItemStack existing = hotbarStacks.get(slot);
            if (ItemStack.areItemsAndComponentsEqual(existing, target)) {
                return slot;
            }
            if (firstEmptySlot < 0 && existing.isEmpty()) {
                firstEmptySlot = slot;
            }
        }

        return firstEmptySlot >= 0 ? firstEmptySlot : fallbackSlot;
    }
}
