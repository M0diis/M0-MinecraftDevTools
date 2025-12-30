package me.m0dii.nbteditor.localnbt;

import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;

public class LocalItemStack extends LocalItem {

    private ItemStack item;

    public LocalItemStack(ItemStack item) {
        this.item = item;
    }

    public static LocalItemStack deserialize(NbtCompound nbt, int defaultDataVersion) {
        return new LocalItemStack(NBTManagers.ITEM.deserialize(
                MiscUtil.updateDynamic(TypeReferences.ITEM_STACK, nbt, defaultDataVersion), true));
    }

    @Override
    public LocalItemStack toStack() {
        return this;
    }

    @Override
    public LocalItemParts toParts() {
        return new LocalItemParts(item);
    }

    @Override
    public ItemStack getEditableItem() {
        return item;
    }

    @Override
    public ItemStack getReadableItem() {
        return item;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public boolean isEmpty(Identifier id) {
        return MVRegistry.ITEM.get(id) == Items.AIR;
    }

    @Override
    public Text getName() {
        return item.getName();
    }

    @Override
    public void setName(Text name) {
        item.manager$setCustomName(name);
    }

    @Override
    public String getDefaultName() {
        return MiscUtil.getBaseItemNameSafely(item).getString();
    }

    @Override
    public Item getItemType() {
        return item.getItem();
    }

    @Override
    public Identifier getId() {
        return MVRegistry.ITEM.getId(item.getItem());
    }

    @Override
    public void setId(Identifier id) {
        item = MiscUtil.setType(MVRegistry.ITEM.get(id), item);
    }

    @Override
    public Set<Identifier> getIdOptions() {
        return MVRegistry.ITEM.getIds();
    }

    @Override
    public int getCount() {
        return item.getCount();
    }

    @Override
    public void setCount(int count) {
        item = MiscUtil.setType(item.getItem(), item, count);
    }

    @Override
    public NbtCompound getNBT() {
        return NBTManagers.ITEM.getNbt(item);
    }

    @Override
    public void setNBT(NbtCompound nbt) {
        NBTManagers.ITEM.setNbt(item, nbt);
    }

    @Override
    public NbtCompound getOrCreateNBT() {
        return NBTManagers.ITEM.getOrCreateNbt(item);
    }

    @Override
    public void renderIcon(MatrixStack matrices, int x, int y, float tickDelta) {
        DrawableHelper.renderItem(matrices, 200.0F, true, item, x, y);
    }

    @Override
    public Optional<ItemStack> toItem() {
        return Optional.of(item.copy());
    }

    @Override
    public NbtCompound serialize() {
        NbtCompound output = NBTManagers.ITEM.serialize(item, true);
        output.putString("type", "item");
        return output;
    }

    @Override
    public Text toHoverableText() {
        return item.toHoverableText();
    }

    @Override
    public LocalItemStack copy() {
        return new LocalItemStack(MiscUtil.copyAirable(item));
    }

}
