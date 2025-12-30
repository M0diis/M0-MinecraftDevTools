package me.m0dii.nbteditor.screens.containers;

import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.SlotUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ServerInventoryManager {

    private final Inventory serverInv;

    public ServerInventoryManager() {
        Inventory playerInv = MiscUtil.client.player.getInventory();
        serverInv = new SimpleInventory(playerInv.size());
        for (int i = 0; i < serverInv.size(); i++) {
            serverInv.setStack(i, playerInv.getStack(i).copy());
        }
    }

    private ScreenHandler getScreenHandler(int syncId) {
        if (syncId == 0) {
            return MiscUtil.client.player.playerScreenHandler;
        }
        if (syncId == MiscUtil.client.player.currentScreenHandler.syncId) {
            return MiscUtil.client.player.currentScreenHandler;
        }
        return null;
    }

    public void onSetPlayerInventoryPacket(SetPlayerInventoryS2CPacket packet) {
        serverInv.setStack(packet.slot(), packet.contents().copy());
    }

    public void onInventoryPacket(InventoryS2CPacket packet) {
        ScreenHandler handler = getScreenHandler(packet.getSyncId());
        if (handler == null) {
            return;
        }

        for (int i = 0; i < packet.getContents().size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory == MiscUtil.client.player.getInventory()) {
                serverInv.setStack(slot.getIndex(), packet.getContents().get(i).copy());
            }
        }
    }

    public void onScreenHandlerSlotUpdatePacket(ScreenHandlerSlotUpdateS2CPacket packet) {
        if (packet.getSyncId() == -1) {
            return;
        }

        if (packet.getSyncId() == -2) {
            serverInv.setStack(packet.getSlot(), packet.getStack().copy());
            return;
        }

        ScreenHandler handler = getScreenHandler(packet.getSyncId());
        if (handler == null) {
            return;
        }
        Slot slot = handler.getSlot(packet.getSlot());
        if (slot.inventory == MiscUtil.client.player.getInventory()) {
            serverInv.setStack(slot.getIndex(), packet.getStack().copy());
        }
    }

    public void updateServer() {
        Inventory playerInv = MiscUtil.client.player.getInventory();
        for (int i = 0; i < serverInv.size(); i++) {
            ItemStack item = playerInv.getStack(i);
            if (!ItemStack.areEqual(item, serverInv.getStack(i))) {
                MiscUtil.clickCreativeStack(item, SlotUtil.invToContainer(i));
                serverInv.setStack(i, item.copy());
            }
        }
    }

}
