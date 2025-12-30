package me.m0dii.mixin;

import me.m0dii.modules.inventorymove.InventoryMoveModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMoveMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void allowMovementKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (InventoryMoveModule.INSTANCE.isEnabled()) {
                if (keyCode == InputUtil.GLFW_KEY_W) {
                    client.options.forwardKey.setPressed(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputUtil.GLFW_KEY_A) {
                    client.options.leftKey.setPressed(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputUtil.GLFW_KEY_S) {
                    client.options.backKey.setPressed(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputUtil.GLFW_KEY_D) {
                    client.options.rightKey.setPressed(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputUtil.GLFW_KEY_SPACE) {
                    client.options.jumpKey.setPressed(true);
                    cir.setReturnValue(false);
                } else if (keyCode == InputUtil.GLFW_KEY_LEFT_CONTROL) {
                    client.options.sprintKey.setPressed(true);
                    cir.setReturnValue(false);
                }
            }
        }
    }

}
