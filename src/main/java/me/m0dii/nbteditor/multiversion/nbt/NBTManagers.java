package me.m0dii.nbteditor.multiversion.nbt;

import me.m0dii.nbteditor.multiversion.nbt.components.ComponentBlockEntityNBTManager;
import me.m0dii.nbteditor.multiversion.nbt.components.ComponentEntityNBTManager;
import me.m0dii.nbteditor.multiversion.nbt.components.ComponentItemNBTManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public class NBTManagers {
    private NBTManagers() {
    }

    public static final DeserializableNBTManager<ItemStack> ITEM = new ComponentItemNBTManager();
    public static final NBTManager<BlockEntity> BLOCK_ENTITY = new ComponentBlockEntityNBTManager();
    public static final NBTManager<Entity> ENTITY = new ComponentEntityNBTManager();
}
