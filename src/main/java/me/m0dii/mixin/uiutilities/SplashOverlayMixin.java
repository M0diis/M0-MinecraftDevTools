package me.m0dii.mixin.uiutilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private long reloadCompleteTime;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideResourcePackLoadingOverlay(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "pausesGame", at = @At("HEAD"), cancellable = true)
    private void allowBackgroundResourceReload(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearOverlayWhenReloadFinishes(CallbackInfo ci) {
        if (this.reloadCompleteTime != -1L) {
            this.client.setOverlay(null);
        }
    }
}
