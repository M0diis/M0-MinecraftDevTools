package me.m0dii.modules.mousetweaks;

import java.util.List;
import net.minecraft.world.inventory.Slot;

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
