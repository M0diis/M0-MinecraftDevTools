package me.m0dii.nbteditor.nbtreferences.itemreferences;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.packets.SetSlotC2SPacket;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;

public class ServerItemReference implements ItemReference {

    private final HandledScreen<?> screen;
    private final int slot;

    /**
     * @param screen
     * @param slot   Format: generic container
     */
    public ServerItemReference(HandledScreen<?> screen, int slot) {
        if (screen.getScreenHandler().getSlot(slot).inventory == MiscUtil.client.player.getInventory()) {
            throw new IllegalArgumentException("The slot cannot be in the player's inventory!");
        }

        this.screen = screen;
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    @Override
    public boolean exists() {
        return M0DevToolsClient.CURSOR_MANAGER.getCurrentRoot() == screen &&
                !M0DevToolsClient.CURSOR_MANAGER.isCurrentRootClosed();
    }

    @Override
    public ItemStack getItem() {
        return screen.getScreenHandler().getSlot(slot).getStack();
    }

    @Override
    public void saveItem(ItemStack toSave, Runnable onFinished) {
        screen.getScreenHandler().getSlot(slot).setStackNoCallbacks(toSave);
        if (M0DevToolsClient.SERVER_CONN.isContainerScreen()) {
            ClientNetworking.send(new SetSlotC2SPacket(slot, toSave));
        }
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
        return -1;
    }

    @Override
    public void showParent() {
        M0DevToolsClient.CURSOR_MANAGER.showBranch(screen);
    }

}
