package me.m0dii.nbteditor.containers;

import me.m0dii.nbteditor.localnbt.LocalBlock;
import net.minecraft.item.ItemStack;

public interface BlockContainerIO {
    /**
     * @param block May be null, which should result in 0 if it is not possible to determine the max size
     */
    int getMaxBlockSize(LocalBlock block);

    default boolean isBlockReadable(LocalBlock block) {
        return true;
    }

    ItemStack[] readBlock(LocalBlock container);

    int writeBlock(LocalBlock container, ItemStack[] contents);

    default int getWrittenBlockSlotIndex(LocalBlock container, ItemStack[] contents, int slot) {
        return slot;
    }
}
