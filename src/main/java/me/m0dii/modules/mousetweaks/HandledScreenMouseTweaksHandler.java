package me.m0dii.modules.mousetweaks;

import me.m0dii.mixin.HandledScreenAccessor;
import me.m0dii.modules.mousetweaks.api.MouseTweaksDisableWheelTweak;
import me.m0dii.modules.mousetweaks.api.MouseTweaksIgnore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.*;

import java.util.List;

class HandledScreenMouseTweaksHandler implements MouseTweaksScreenHandler {
    protected final MinecraftClient client;
    private final HandledScreen<?> screen;
    private final HandledScreenAccessor accessor;

    HandledScreenMouseTweaksHandler(HandledScreen<?> screen) {
        this.client = MinecraftClient.getInstance();
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
        return screen.getScreenHandler().slots;
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
        accessor.m0dev$invokeOnMouseClick(slot, slot.id, mouseButton.getId(), shiftPressed ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
    }

    @Override
    public boolean isCraftingOutput(Slot slot) {
        return slot instanceof CraftingResultSlot
                || slot instanceof FurnaceOutputSlot
                || slot instanceof TradeOutputSlot;
    }

    @Override
    public boolean isIgnored(Slot slot) {
        return false;
    }

    protected boolean isPlayerInventorySlot(Slot slot) {
        if (client.player == null) {
            return false;
        }
        PlayerInventory inventory = client.player.getInventory();
        return slot.inventory == inventory;
    }
}
