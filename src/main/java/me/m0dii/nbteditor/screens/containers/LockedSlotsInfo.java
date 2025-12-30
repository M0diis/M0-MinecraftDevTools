package me.m0dii.nbteditor.screens.containers;

import lombok.Getter;
import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.SlotUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class LockedSlotsInfo {

    public static final LockedSlotsInfo NONE = new LockedSlotsInfo(false, false, true);
    public static final LockedSlotsInfo ITEMS_LOCKED = new LockedSlotsInfo(true, false, true);

    private final boolean itemsLocked;
    private final boolean airLocked;
    @Getter
    private final boolean copyLockedItem;
    private final List<Integer> playerLockedSlots;
    private final List<Integer> playerLockedHotbarSlots;
    private final List<Integer> containerLockedSlots;

    public LockedSlotsInfo(boolean itemsLocked, boolean airLocked, boolean copyLockedItem) {
        this.itemsLocked = itemsLocked;
        this.airLocked = airLocked;
        this.copyLockedItem = copyLockedItem;
        this.playerLockedSlots = new ArrayList<>();
        this.playerLockedHotbarSlots = new ArrayList<>();
        this.containerLockedSlots = new ArrayList<>();
    }

    private LockedSlotsInfo(LockedSlotsInfo info) {
        this.itemsLocked = info.itemsLocked;
        this.airLocked = info.airLocked;
        this.copyLockedItem = info.copyLockedItem;
        this.playerLockedSlots = new ArrayList<>(info.playerLockedSlots);
        this.playerLockedHotbarSlots = new ArrayList<>(info.playerLockedHotbarSlots);
        this.containerLockedSlots = new ArrayList<>(info.containerLockedSlots);
    }

    /**
     * @param slot Format: inv
     */
    public LockedSlotsInfo addPlayerSlot(int slot) {
        playerLockedSlots.add(slot);
        if (SlotUtil.isHotbarFromInv(slot) || SlotUtil.isOffHandFromInv(slot)) {
            playerLockedHotbarSlots.add(slot);
        }
        return this;
    }

    public LockedSlotsInfo addPlayerSlot(ItemReference itemRef) {
        int slot = itemRef.getBlockedSlot();
        if (slot != -1) {
            addPlayerSlot(slot);
        }
        return this;
    }

    public LockedSlotsInfo addContainerSlot(int slot) {
        containerLockedSlots.add(slot);
        return this;
    }

    public boolean isBlocked(Slot slot, int button, SlotActionType actionType, boolean explicitly) {
        if (actionType == SlotActionType.SWAP && playerLockedHotbarSlots.contains(button)) {
            return true;
        }

        if (slot.inventory == MiscUtil.client.player.getInventory()) {
            return playerLockedSlots.contains(slot.getIndex());
        } else {
            if (containerLockedSlots.contains(slot.getIndex())) {
                return true;
            }

            if (!explicitly) {
                ItemStack item = slot.getStack();
                if (item == null || item.isEmpty()) {
                    return airLocked;
                } else {
                    return itemsLocked;
                }
            }
        }

        return false;
    }

    public boolean isBlocked(Slot slot, boolean explicitly) {
        return isBlocked(slot, 0, SlotActionType.PICKUP, explicitly);
    }

    public void renderLockedHighlights(MatrixStack matrices, ScreenHandler handler, boolean explicitly, boolean player, boolean container) {
        for (Slot slot : handler.slots) {
            if ((slot.inventory == MiscUtil.client.player.getInventory() ? player : container) && isBlocked(slot, explicitly)) {
                DrawableHelper.drawSlotHighlight(matrices, slot.x, slot.y, 0x60FF0000);
            }
        }
    }

    public LockedSlotsInfo copy() {
        return new LockedSlotsInfo(this);
    }

}
