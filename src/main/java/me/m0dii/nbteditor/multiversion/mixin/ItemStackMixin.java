package me.m0dii.nbteditor.multiversion.mixin;

import me.m0dii.nbteditor.multiversion.MVComponentType;
import me.m0dii.nbteditor.multiversion.nbt.IntegratedNBTManager;
import me.m0dii.nbteditor.multiversion.nbt.MVItemStackParent;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public class ItemStackMixin implements IntegratedNBTManager, MVItemStackParent {

    @Override
    public NbtCompound manager$serialize(boolean requireSuccess) {
        return NBTManagers.ITEM.serialize((ItemStack) (Object) this, requireSuccess);
    }

    @Override
    public boolean manager$hasNbt() {
        return NBTManagers.ITEM.hasNbt((ItemStack) (Object) this);
    }

    @Override
    public NbtCompound manager$getNbt() {
        return NBTManagers.ITEM.getNbt((ItemStack) (Object) this);
    }

    @Override
    public NbtCompound manager$getOrCreateNbt() {
        return NBTManagers.ITEM.getOrCreateNbt((ItemStack) (Object) this);
    }

    @Override
    public void manager$setNbt(NbtCompound nbt) {
        NBTManagers.ITEM.setNbt((ItemStack) (Object) this, nbt);
    }

    @Override
    public boolean manager$hasCustomName() {
        return ((ItemStack) (Object) this).contains(MVComponentType.CUSTOM_NAME);
    }

    @Override
    public ItemStack manager$setCustomName(Text name) {
        ((ItemStack) (Object) this).set(MVComponentType.CUSTOM_NAME, name);

        return (ItemStack) (Object) this;
    }
}
