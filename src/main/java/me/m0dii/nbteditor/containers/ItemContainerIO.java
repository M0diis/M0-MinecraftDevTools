package me.m0dii.nbteditor.containers;

import net.minecraft.item.ItemStack;

public interface ItemContainerIO {
    static ItemContainerIO forNBTIO(NBTContainerIO io) {
        return new ItemTagContainerIO(null, io);
    }

    /**
     * @param item May be null, which should result in 0 if it is not possible to determine the max size
     */
    int getMaxItemSize(ItemStack item);

    default boolean isItemReadable(ItemStack item) {
        return true;
    }

    ItemStack[] readItem(ItemStack container);

    int writeItem(ItemStack container, ItemStack[] contents);

    default int getWrittenItemSlotIndex(ItemStack container, ItemStack[] contents, int slot) {
        return slot;
    }
}
