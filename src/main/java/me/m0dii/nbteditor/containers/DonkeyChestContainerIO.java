package me.m0dii.nbteditor.containers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class DonkeyChestContainerIO implements NonItemNBTContainerIO {

    private final boolean llama;
    private final ConstSizeContainerIO chest;

    public DonkeyChestContainerIO(boolean llama) {
        this.llama = llama;
        this.chest = new ConstSizeContainerIO(15);
    }

    @Override
    public int getMaxNBTSize(NbtCompound nbt, SourceContainerType source) {
        return 15;
    }

    @Override
    public ItemStack[] readNBT(NbtCompound container, SourceContainerType source) {
        return chest.readNBT(container, SourceContainerType.ENTITY);
    }

    @Override
    public int writeNBT(NbtCompound container, ItemStack[] contents, SourceContainerType source) {
        int output = chest.writeNBT(container, contents, SourceContainerType.ENTITY);

        for (ItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                container.putBoolean("ChestedHorse", true);
                break;
            }
        }

        if (llama) {
            int columns = 1;
            for (int i = 3; i < contents.length; i++) {
                if (contents[i] != null && !contents[i].isEmpty()) {
                    columns = (i / 3) + 1;
                }
            }
            if (columns != 1 && container.getInt("Strength") < columns) {
                container.putInt("Strength", columns);
            }
        }

        return output;
    }

}
