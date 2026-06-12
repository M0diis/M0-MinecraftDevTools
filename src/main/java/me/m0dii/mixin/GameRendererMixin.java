package me.m0dii.mixin;


import me.m0dii.modules.camera.CameraPathManager;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.tweaks.TweaksModule;
import me.m0dii.modules.zoom.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 1001)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "updateCamera", at = @At("HEAD"))
    private void applyCameraPathRenderPose(RenderTickCounter tickCounter, CallbackInfo ci) {
        CameraPathManager.applyRenderPose(tickCounter.getTickProgress(true));
    }

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void applyZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (ZoomModule.INSTANCE.isHeld()) {
            cir.setReturnValue(ZoomModule.INSTANCE.getZoomFov());
        } else if (FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController()) {
            cir.setReturnValue((float) this.client.options.getFov().getValue());
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void removeHandRendering(CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController()) {
            ci.cancel();
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void disableHurtTilt(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableHurtCamera()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void disableViewBobbing(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableViewBobbing()) {
            ci.cancel();
        }
    }

}
