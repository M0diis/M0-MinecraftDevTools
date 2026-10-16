package me.m0dii.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMoveMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void allowMovementKeys(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.input();

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (InventoryMoveModule.INSTANCE.isEnabled()) {
                if (keyCode == InputConstants.KEY_W) {
                    client.options.keyUp.setDown(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputConstants.KEY_A) {
                    client.options.keyLeft.setDown(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputConstants.KEY_S) {
                    client.options.keyDown.setDown(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputConstants.KEY_D) {
                    client.options.keyRight.setDown(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputConstants.KEY_SPACE) {
                    client.options.keyJump.setDown(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputConstants.KEY_LCONTROL) {
                    client.options.keySprint.setDown(true);
                    cir.setReturnValue(false);
                }
            }
        }
    }

}
