package me.m0dii.mixin;

import me.m0dii.modules.mousetweaks.MouseTweaksRuntime;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMouseTweaksMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaksRuntime.onMouseClicked((HandledScreen<?>) (Object) this, click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaksRuntime.onMouseReleased((HandledScreen<?>) (Object) this, click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseDragged(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaksRuntime.onMouseDragged((HandledScreen<?>) (Object) this, click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (MouseTweaksRuntime.onMouseScrolled((HandledScreen<?>) (Object) this, mouseX, mouseY, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}
