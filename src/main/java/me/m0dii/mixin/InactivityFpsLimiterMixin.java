package me.m0dii.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import me.m0dii.modules.performance.DynamicFpsModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public class InactivityFpsLimiterMixin {

    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void applyDynamicUnfocusedLimit(CallbackInfoReturnable<Integer> cir) {
        if (!DynamicFpsModule.INSTANCE.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.isWindowActive()) {
            return;
        }

        int currentLimit = cir.getReturnValue();
        int configuredLimit = DynamicFpsModule.INSTANCE.getUnfocusedFpsLimit();
        cir.setReturnValue(Math.min(currentLimit, configuredLimit));
    }
}
