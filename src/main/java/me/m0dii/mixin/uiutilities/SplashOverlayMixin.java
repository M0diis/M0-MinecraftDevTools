package me.m0dii.mixin.uiutilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
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
    @Final
    private ResourceReload reload;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideResourcePackLoadingOverlay(net.minecraft.client.gui.DrawContext context,
                                                int mouseX,
                                                int mouseY,
                                                float delta,
                                                CallbackInfo ci) {
        // Let vanilla SplashOverlay render until resources are fully ready.
        if (!this.reload.isComplete()) {
            return;
        }

        if (this.client.currentScreen != null) {
            this.client.currentScreen.renderWithTooltip(context, mouseX, mouseY, delta);
        }
        this.client.setOverlay(null);
        ci.cancel();
    }

    @Inject(method = "pausesGame", at = @At("HEAD"), cancellable = true)
    private void allowBackgroundResourceReload(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearOverlayWhenReloadFinishes(CallbackInfo ci) {
        if (this.reload.isComplete()) {
            this.client.setOverlay(null);
        }
    }
}
