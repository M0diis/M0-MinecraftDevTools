package me.m0dii.nbteditor.containers;

import me.m0dii.nbteditor.localnbt.LocalEntity;
import net.minecraft.item.ItemStack;

public interface EntityContainerIO {
    static EntityContainerIO forNBTIO(NonItemNBTContainerIO io) {
        return new EntityTagContainerIO(io.withItemSupport((String) null));
    }

    /**
     * @param entity May be null, which should result in 0 if it is not possible to determine the max size
     */
    int getMaxEntitySize(LocalEntity entity);

    default boolean isEntityReadable(LocalEntity entity) {
        return true;
    }

    ItemStack[] readEntity(LocalEntity container);

    int writeEntity(LocalEntity container, ItemStack[] contents);

    default int getWrittenEntitySlotIndex(LocalEntity container, ItemStack[] contents, int slot) {
        return slot;
    }
}
