package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.screens.containers.ClientHandledScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo info) {
        if ((HandledScreen<?>) (Object) this instanceof ClientHandledScreen) {
            return;
        }

        MixinLink.onMouseClick((HandledScreen<?>) (Object) this, slot, slotId, button, actionType, info);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        HandledScreen<?> source = (HandledScreen<?>) (Object) this;
        if (source instanceof CreativeInventoryScreen || source instanceof ClientHandledScreen) {
            return;
        }

        MixinLink.keyPressed(source, keyCode, scanCode, modifiers, info);
    }
}
