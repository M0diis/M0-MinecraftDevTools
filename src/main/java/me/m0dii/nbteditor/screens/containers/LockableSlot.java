package me.m0dii.nbteditor.screens.containers;

import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LockableSlot extends Slot {

    private static final Set<Thread> UNLOCKED_THREADS = Collections.synchronizedSet(new HashSet<>());

    public LockableSlot(Slot slot) {
        super(slot.inventory, slot.getIndex(), slot.x, slot.y);
        this.id = slot.id;
    }

    public static void unlockDuring(Runnable callback) {
        UNLOCKED_THREADS.add(Thread.currentThread());
        try {
            callback.run();
        } finally {
            UNLOCKED_THREADS.remove(Thread.currentThread());
        }
    }

    private boolean isBlocked() {
        if (UNLOCKED_THREADS.contains(Thread.currentThread())) {
            return false;
        }

        if (MiscUtil.client.currentScreen instanceof ClientHandledScreen clientHandledScreen) {
            return clientHandledScreen.getLockedSlotsInfo().isBlocked(this, false);
        }
        return false;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (isBlocked()) {
            return false;
        }
        return super.canInsert(stack);
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        if (isBlocked()) {
            return false;
        }
        return super.canTakeItems(playerEntity);
    }

    // Prevent quick moving identical items into slot
    @Override
    public int getMaxItemCount() {
        if (isBlocked()) {
            return getStack().getCount();
        }
        return super.getMaxItemCount();
    }

}
