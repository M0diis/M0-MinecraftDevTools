package me.m0dii.utils;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

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
        return resolveTargetSlot(
                hotbarStacks,
                selectedSlot,
                target,
                ItemStack::isEmpty,
                ItemStack::areItemsAndComponentsEqual
        );
    }

    static <T> int resolveTargetSlot(
            List<T> hotbarStacks,
            int selectedSlot,
            T target,
            Predicate<T> isEmpty,
            BiPredicate<T, T> matches
    ) {
        int fallbackSlot = Math.clamp(selectedSlot, 0, PlayerInventory.HOTBAR_SIZE - 1);
        int firstEmptySlot = -1;
        int limit = Math.min(hotbarStacks.size(), PlayerInventory.HOTBAR_SIZE);

        for (int slot = 0; slot < limit; slot++) {
            T existing = hotbarStacks.get(slot);
            if (matches.test(existing, target)) {
                return slot;
            }
            if (firstEmptySlot < 0 && isEmpty.test(existing)) {
                firstEmptySlot = slot;
            }
        }

        return firstEmptySlot >= 0 ? firstEmptySlot : fallbackSlot;
    }
}
