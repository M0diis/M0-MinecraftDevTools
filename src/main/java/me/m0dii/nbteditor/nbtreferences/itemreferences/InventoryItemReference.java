package me.m0dii.nbteditor.nbtreferences.itemreferences;

import lombok.Getter;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.item.ItemStack;

public class InventoryItemReference implements ItemReference {

    @Getter
    private final int slot;
    private Runnable parent;

    /**
     * @param slot Format: inv
     */
    public InventoryItemReference(int slot) {
        this.slot = slot;
        this.parent = M0DevToolsClient.CURSOR_MANAGER::showRoot;
    }

    public InventoryItemReference setParent(Runnable parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ItemStack getItem() {
        return MiscUtil.client.player.getInventory().getStack(slot);
    }

    @Override
    public void saveItem(ItemStack toSave, Runnable onFinished) {
        MiscUtil.saveItem(slot, toSave);
        onFinished.run();
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isLockable() {
        return false;
    }

    @Override
    public int getBlockedSlot() {
        return slot;
    }

    @Override
    public void showParent() {
        parent.run();
    }

}
