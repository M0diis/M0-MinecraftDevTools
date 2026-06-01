package me.m0dii.mixin;

import me.m0dii.modules.performance.DynamicFpsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public class InactivityFpsLimiterMixin {

    @Inject(method = "update", at = @At("RETURN"), cancellable = true)
    private void applyDynamicUnfocusedLimit(CallbackInfoReturnable<Integer> cir) {
        if (!DynamicFpsModule.INSTANCE.isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isWindowFocused()) {
            return;
        }

        int currentLimit = cir.getReturnValue();
        int configuredLimit = DynamicFpsModule.INSTANCE.getUnfocusedFpsLimit();
        cir.setReturnValue(Math.min(currentLimit, configuredLimit));
    }
}
