package me.m0dii.modules.mousetweaks;

import me.m0dii.mixin.HandledScreenAccessor;
import me.m0dii.modules.mousetweaks.api.MouseTweaksDisableWheelTweak;
import me.m0dii.modules.mousetweaks.api.MouseTweaksIgnore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;

import java.util.List;

class HandledScreenMouseTweaksHandler implements MouseTweaksScreenHandler {
    protected final Minecraft client;
    private final AbstractContainerScreen<?> screen;
    private final HandledScreenAccessor accessor;

    HandledScreenMouseTweaksHandler(AbstractContainerScreen<?> screen) {
        this.client = Minecraft.getInstance();
        this.screen = screen;
        this.accessor = (HandledScreenAccessor) screen;
    }

    @Override
    public boolean isMouseTweaksDisabled() {
        return screen.getClass().isAnnotationPresent(MouseTweaksIgnore.class);
    }

    @Override
    public boolean isWheelTweakDisabled() {
        return screen.getClass().isAnnotationPresent(MouseTweaksDisableWheelTweak.class);
    }

    @Override
    public List<Slot> getSlots() {
        return screen.getMenu().slots;
    }

    @Override
    public Slot getSlotUnderMouse(double mouseX, double mouseY) {
        return accessor.m0dev$invokeGetSlotAt(mouseX, mouseY);
    }

    @Override
    public void disableRmbDraggingFunctionality() {
        accessor.m0dev$setCancelNextRelease(true);
        if (accessor.m0dev$isCursorDragging() && accessor.m0dev$getHeldButtonType() == MouseTweaksButton.RIGHT.getId()) {
            accessor.m0dev$setCursorDragging(false);
            accessor.m0dev$getCursorDragSlots().clear();
        }
    }

    @Override
    public void clickSlot(Slot slot, MouseTweaksButton mouseButton, boolean shiftPressed) {
        accessor.m0dev$invokeOnMouseClick(slot, slot.index, mouseButton.getId(), shiftPressed ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
    }

    @Override
    public boolean isCraftingOutput(Slot slot) {
        return slot instanceof ResultSlot
                || slot instanceof FurnaceResultSlot
                || slot instanceof MerchantResultSlot;
    }

    @Override
    public boolean isIgnored(Slot slot) {
        return false;
    }

    protected boolean isPlayerInventorySlot(Slot slot) {
        if (client.player == null) {
            return false;
        }
        Inventory inventory = client.player.getInventory();
        return slot.container == inventory;
    }
}
