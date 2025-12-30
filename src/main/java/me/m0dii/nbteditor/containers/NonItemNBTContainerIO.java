package me.m0dii.nbteditor.containers;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

public interface NonItemNBTContainerIO {
    /**
     * @param nbt May be null, which should result in 0 if it is not possible to determine the max size
     */
    int getMaxNBTSize(NbtCompound nbt, SourceContainerType source);

    default boolean isNBTReadable(NbtCompound nbt, SourceContainerType source) {
        return true;
    }

    ItemStack[] readNBT(NbtCompound container, SourceContainerType source);

    int writeNBT(NbtCompound container, ItemStack[] contents, SourceContainerType source);

    default int getWrittenNBTSlotIndex(NbtCompound container, ItemStack[] contents, int slot, SourceContainerType source) {
        return slot;
    }

    default NBTContainerIO withItemSupport(String defaultEntityId) {
        return new NBTContainerIO() {
            @Override
            public int getMaxNBTSize(NbtCompound nbt, SourceContainerType source) {
                return NonItemNBTContainerIO.this.getMaxNBTSize(nbt, source);
            }

            @Override
            public boolean isNBTReadable(NbtCompound nbt, SourceContainerType source) {
                return NonItemNBTContainerIO.this.isNBTReadable(nbt, source);
            }

            @Override
            public ItemStack[] readNBT(NbtCompound container, SourceContainerType source) {
                return NonItemNBTContainerIO.this.readNBT(container, source);
            }

            @Override
            public int writeNBT(NbtCompound container, ItemStack[] contents, SourceContainerType source) {
                return NonItemNBTContainerIO.this.writeNBT(container, contents, source);
            }

            @Override
            public int getWrittenNBTSlotIndex(NbtCompound container, ItemStack[] contents, int slot, SourceContainerType source) {
                return NonItemNBTContainerIO.this.getWrittenNBTSlotIndex(container, contents, slot, source);
            }

            @Override
            public String getDefaultEntityId() {
                return defaultEntityId;
            }
        };
    }

    default NBTContainerIO withItemSupport(BlockEntityType<?> block) {
        return withItemSupport(Objects.requireNonNull(BlockEntityType.getId(block)).toString());
    }

    default NBTContainerIO withItemSupport(EntityType<?> entity) {
        return withItemSupport(EntityType.getId(entity).toString());
    }
}
