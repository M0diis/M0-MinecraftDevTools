package me.m0dii.nbteditor.nbtreferences.itemreferences;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.SlotUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public record HandItemReference(Hand hand) implements ItemReference {

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ItemStack getItem() {
        return MiscUtil.client.player.getStackInHand(hand);
    }

    @Override
    public void saveItem(ItemStack toSave, Runnable onFinished) {
        MiscUtil.saveItem(hand, toSave);
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
        return hand == Hand.MAIN_HAND
                ? SlotUtil.createHotbarInInv(MiscUtil.client.player.getInventory().selectedSlot)
                : SlotUtil.createOffHandInInv();

    }

    @Override
    public void showParent() {
        M0DevToolsClient.CURSOR_MANAGER.closeRoot();
    }

}
