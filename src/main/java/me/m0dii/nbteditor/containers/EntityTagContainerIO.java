package me.m0dii.nbteditor.containers;

import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.tagreferences.TagNames;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class EntityTagContainerIO extends ItemTagContainerIO implements EntityContainerIO {

    private final NonItemNBTContainerIO entityNbtIO;

    public EntityTagContainerIO(NBTContainerIO itemNbtIO, NonItemNBTContainerIO entityNbtIO) {
        super(TagNames.ENTITY_TAG, itemNbtIO);
        this.entityNbtIO = entityNbtIO;
    }

    public EntityTagContainerIO(NBTContainerIO nbtIO) {
        this(nbtIO, nbtIO);
    }

    @Override
    public int getMaxEntitySize(LocalEntity entity) {
        return entityNbtIO.getMaxNBTSize(getNBT(entity), SourceContainerType.ENTITY);
    }

    @Override
    public boolean isEntityReadable(LocalEntity entity) {
        return entityNbtIO.isNBTReadable(getNBT(entity), SourceContainerType.ENTITY);
    }

    @Override
    public ItemStack[] readEntity(LocalEntity container) {
        return entityNbtIO.readNBT(getNBT(container), SourceContainerType.ENTITY);
    }

    @Override
    public int writeEntity(LocalEntity container, ItemStack[] contents) {
        NbtCompound nbt = getNBT(container);
        int output = entityNbtIO.writeNBT(nbt, contents, SourceContainerType.ENTITY);
        container.setNBT(nbt);
        return output;
    }

    @Override
    public int getWrittenEntitySlotIndex(LocalEntity container, ItemStack[] contents, int slot) {
        return entityNbtIO.getWrittenNBTSlotIndex(getNBT(container), contents, slot, SourceContainerType.ENTITY);
    }

}
