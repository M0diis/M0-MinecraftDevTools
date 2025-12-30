package me.m0dii.nbteditor.nbtreferences.itemreferences;

import lombok.Getter;
import me.m0dii.nbteditor.containers.ContainerIO;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.screens.containers.ContainerScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.SaveQueue;
import net.minecraft.item.ItemStack;

import java.util.concurrent.atomic.AtomicBoolean;

public class ContainerItemReference<L extends LocalNBT> implements ItemReference {

    @Getter
    private final NBTReference<L> container;
    @Getter
    private final int slot;
    private final SaveQueue<ItemStack> save;

    public ContainerItemReference(NBTReference<L> container, int slot) {
        this.container = container;
        this.slot = slot;

        this.save = new SaveQueue<>("Container", toSave -> {
            L containerValue = LocalNBT.copy(container.getLocalNBT());
            ItemStack[] contents = ContainerIO.read(containerValue);
            contents[slot] = toSave;
            ContainerIO.write(containerValue, contents);

            if (MiscUtil.client.currentScreen instanceof ContainerScreen screen && screen.getReference() == container) {
                screen.getScreenHandler().getSlot(slot).setStackNoCallbacks(toSave);
            }

            AtomicBoolean done = new AtomicBoolean();
            Object lock = new Object();
            container.saveLocalNBT(containerValue, () -> {
                done.set(true);
                synchronized (lock) {
                    lock.notifyAll();
                }
            });
            synchronized (lock) {
                while (!done.get()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, true);
    }

    @Override
    public boolean exists() {
        if (!container.exists()) {
            return false;
        }

        L containerValue = container.getLocalNBT();
        return ContainerIO.isContainer(containerValue) && slot < ContainerIO.getMaxSize(containerValue);
    }

    @Override
    public ItemStack getItem() {
        L containerValue = container.getLocalNBT();
        ItemStack[] contents = ContainerIO.read(containerValue);
        if (slot >= contents.length && slot < ContainerIO.getMaxSize(containerValue)) {
            return ItemStack.EMPTY;
        }
        return contents[slot];
    }

    @Override
    public void saveItem(ItemStack toSave, Runnable onFinished) {
        save.save(onFinished, toSave.copy());
    }

    @Override
    public boolean isLocked() {
        return container instanceof ItemReference item && item.isLocked();
    }

    @Override
    public boolean isLockable() {
        return container instanceof ItemReference item && item.isLockable();
    }

    @Override
    public int getBlockedSlot() {
        return container instanceof ItemReference item ? item.getBlockedSlot() : -1;
    }

    @Override
    public void showParent() {
        ContainerScreen.show(container);
    }

    @Override
    public void escapeParent() {
        container.escapeParent();
    }

}
