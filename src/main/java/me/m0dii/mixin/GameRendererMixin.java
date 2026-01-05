package me.m0dii.mixin;


import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.zoom.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class, priority = 1001)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void applyZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (ZoomModule.INSTANCE.isHeld()) {
            cir.setReturnValue(ZoomModule.INSTANCE.getZoomFov());
        } else if (FreecamModule.INSTANCE.isEnabled()) {
            cir.setReturnValue((float) this.client.options.getFov().getValue());
        }
    }

    @Redirect(method = "updateCrosshairTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    private Entity overrideCameraEntityForRayTrace(MinecraftClient mc) {
        if (FreecamModule.INSTANCE.isEnabled() && true && mc.player != null) {
            return mc.player;
        }

        return mc.getCameraEntity();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void removeHandRendering(CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }

}
