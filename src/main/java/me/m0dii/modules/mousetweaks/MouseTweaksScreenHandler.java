package me.m0dii.modules.mousetweaks;

import net.minecraft.screen.slot.Slot;

import java.util.List;

interface MouseTweaksScreenHandler {
    boolean isMouseTweaksDisabled();

    boolean isWheelTweakDisabled();

    List<Slot> getSlots();

    Slot getSlotUnderMouse(double mouseX, double mouseY);

    void disableRmbDraggingFunctionality();

    void clickSlot(Slot slot, MouseTweaksButton mouseButton, boolean shiftPressed);

    boolean isCraftingOutput(Slot slot);

    boolean isIgnored(Slot slot);
}
