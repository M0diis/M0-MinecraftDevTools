package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin {

    @Inject(method = "onMouseClick", at = @At(value = "HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo info) {
        if (slot != null) {
            if (slot instanceof CreativeInventoryScreen.CreativeSlot creativeSlot) {
                slot = creativeSlot.slot;
            }
        }

        MixinLink.onMouseClick((CreativeInventoryScreen) (Object) this, slot, slotId, button, actionType, info);
    }

    @Inject(method = "onMouseClick", at = @At(value = "RETURN"))
    private void onMouseClickReturn(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo info) {
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        MixinLink.keyPressed((CreativeInventoryScreen) (Object) this, keyCode, scanCode, modifiers, info);
    }

}
