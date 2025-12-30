package me.m0dii.nbteditor.containers;

import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.tagreferences.TagNames;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class SpawnEggContainerIO implements ItemContainerIO {

    @Override
    public int getMaxItemSize(ItemStack item) {
        if (item == null) {
            return 0;
        }

        NbtCompound nbt = NBTManagers.ITEM.getNbt(item);
        NbtCompound entityTag = (nbt == null ? new NbtCompound() : nbt.getCompound(TagNames.ENTITY_TAG));
        return ContainerIO.getMaxSize(new LocalEntity(MVMisc.getEntityType(item), entityTag));
    }

    @Override
    public boolean isItemReadable(ItemStack item) {
        NbtCompound nbt = NBTManagers.ITEM.getNbt(item);
        NbtCompound entityTag = (nbt == null ? new NbtCompound() : nbt.getCompound(TagNames.ENTITY_TAG));
        return ContainerIO.isContainer(new LocalEntity(MVMisc.getEntityType(item), entityTag));
    }

    @Override
    public ItemStack[] readItem(ItemStack container) {
        NbtCompound nbt = NBTManagers.ITEM.getNbt(container);
        NbtCompound entityTag = (nbt == null ? new NbtCompound() : nbt.getCompound(TagNames.ENTITY_TAG));
        return ContainerIO.read(new LocalEntity(MVMisc.getEntityType(container), entityTag));
    }

    @Override
    public int writeItem(ItemStack container, ItemStack[] contents) {
        LocalEntity entity = new LocalEntity(MVMisc.getEntityType(container),
                container.manager$getOrCreateNbt().getCompound(TagNames.ENTITY_TAG));
        int output = ContainerIO.write(entity, contents);
        container.manager$modifyNbt(nbt -> nbt.put(TagNames.ENTITY_TAG,
                MiscUtil.fillId(entity.getNBT(), entity.getId().toString())));
        return output;
    }

    @Override
    public int getWrittenItemSlotIndex(ItemStack container, ItemStack[] contents, int slot) {
        LocalEntity entity = new LocalEntity(MVMisc.getEntityType(container),
                container.manager$getOrCreateNbt().getCompound(TagNames.ENTITY_TAG));
        return ContainerIO.getWrittenSlotIndex(entity, contents, slot);
    }

}
