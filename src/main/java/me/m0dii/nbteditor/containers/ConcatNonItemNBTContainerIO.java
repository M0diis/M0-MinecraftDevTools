package me.m0dii.nbteditor.containers;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.List;

public class ConcatNonItemNBTContainerIO implements NonItemNBTContainerIO {

    private final NonItemNBTContainerIO[] nbtIOs;

    public ConcatNonItemNBTContainerIO(NonItemNBTContainerIO... nbtIOs) {
        this.nbtIOs = nbtIOs;
    }

    @Override
    public int getMaxNBTSize(NbtCompound nbt, SourceContainerType source) {
        int total = 0;
        for (NonItemNBTContainerIO nbtIO : nbtIOs) {
            int size = nbtIO.getMaxNBTSize(nbt, source);
            if (size == 0) {
                return 0;
            }
            total += size;
        }
        return total;
    }

    @Override
    public boolean isNBTReadable(NbtCompound nbt, SourceContainerType source) {
        return Arrays.stream(nbtIOs).allMatch(nbtIO -> nbtIO.isNBTReadable(nbt, source));
    }

    @Override
    public ItemStack[] readNBT(NbtCompound container, SourceContainerType source) {
        List<ItemStack> output = Arrays.stream(nbtIOs)
                .flatMap(nbtIO -> Arrays.stream(nbtIO.readNBT(container, source)))
                .toList();
        return output.toArray(ItemStack[]::new);
    }

    @Override
    public int writeNBT(NbtCompound container, ItemStack[] contents, SourceContainerType source) {
        int total = 0;
        for (NonItemNBTContainerIO nbtIO : nbtIOs) {
            int numWritten = nbtIO.writeNBT(container, contents, source);
            if (numWritten >= contents.length) {
                contents = new ItemStack[0];
            } else {
                ItemStack[] temp = new ItemStack[contents.length - numWritten];
                System.arraycopy(contents, numWritten, temp, 0, temp.length);
                contents = temp;
            }
            total += numWritten;
        }
        return total;
    }

    @Override
    public int getWrittenNBTSlotIndex(NbtCompound container, ItemStack[] contents, int slot, SourceContainerType source) {
        NbtCompound tempContainer = container.copy();

        int total = 0;
        for (NonItemNBTContainerIO nbtIO : nbtIOs) {
            int numWritten = nbtIO.writeNBT(tempContainer, contents, source);
            if (slot < total + numWritten) {
                return nbtIO.getWrittenNBTSlotIndex(container, contents, slot - total, source) + total;
            }
            if (numWritten >= contents.length) {
                contents = new ItemStack[0];
            } else {
                ItemStack[] temp = new ItemStack[contents.length - numWritten];
                System.arraycopy(contents, numWritten, temp, 0, temp.length);
                contents = temp;
            }
            total += numWritten;
        }
        throw new IllegalArgumentException("Slot is never written: " + slot);
    }

}
