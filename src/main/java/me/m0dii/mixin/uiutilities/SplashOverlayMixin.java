package me.m0dii.mixin.uiutilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadingOverlay.class)
public class SplashOverlayMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ReloadInstance reload;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideResourcePackLoadingOverlay(net.minecraft.client.gui.GuiGraphicsExtractor context,
                                                int mouseX,
                                                int mouseY,
                                                float delta,
                                                CallbackInfo ci) {
        // Let vanilla SplashOverlay render until resources are fully ready.
        if (!this.reload.isDone()) {
            return;
        }

        if (this.minecraft.gui.screen() != null) {
            this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(context, mouseX, mouseY, delta);
        }
        this.minecraft.gui.setOverlay(null);
        ci.cancel();
    }

    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void allowBackgroundResourceReload(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clearOverlayWhenReloadFinishes(CallbackInfo ci) {
        if (this.reload.isDone()) {
            this.minecraft.gui.setOverlay(null);
        }
    }
}
