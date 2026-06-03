package me.m0dii.modules.mousetweaks;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.screen.slot.Slot;

final class CreativeInventoryMouseTweaksHandler extends HandledScreenMouseTweaksHandler {
    CreativeInventoryMouseTweaksHandler(CreativeInventoryScreen screen) {
        super(screen);
    }

    @Override
    public boolean isIgnored(Slot slot) {
        return super.isIgnored(slot) || !isPlayerInventorySlot(slot);
    }
}
