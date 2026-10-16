package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.zoom.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("HEAD"), cancellable = true)
    private void applyZoom(float partialTicks, CallbackInfoReturnable<Float> callback) {
        if (ZoomModule.INSTANCE.isHeld()) callback.setReturnValue(ZoomModule.INSTANCE.getZoomFov());
        else if (FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController())
            callback.setReturnValue(Minecraft.getInstance().options.fov().get().floatValue());
    }
}
