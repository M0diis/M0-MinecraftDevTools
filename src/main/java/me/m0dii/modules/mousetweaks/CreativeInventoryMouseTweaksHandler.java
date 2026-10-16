package me.m0dii.modules.mousetweaks;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;

final class CreativeInventoryMouseTweaksHandler extends HandledScreenMouseTweaksHandler {
    CreativeInventoryMouseTweaksHandler(CreativeModeInventoryScreen screen) {
        super(screen);
    }

    @Override
    public boolean isIgnored(Slot slot) {
        return super.isIgnored(slot) || !isPlayerInventorySlot(slot);
    }
}
