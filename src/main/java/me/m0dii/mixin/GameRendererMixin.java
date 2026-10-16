package me.m0dii.mixin;


import me.m0dii.modules.camera.CameraPathManager;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 1001)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "update", at = @At("HEAD"))
    private void applyCameraPathRenderPose(DeltaTracker tickCounter, CallbackInfo ci) {
        CameraPathManager.applyRenderPose(tickCounter.getGameTimeDeltaPartialTick(true));
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void removeHandRendering(CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void disableHurtTilt(CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableHurtCamera()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void disableViewBobbing(CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableViewBobbing()) {
            ci.cancel();
        }
    }

}
