package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.zoom.ZoomModule;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "calculateFov", at = @At("HEAD"), cancellable = true)
    private void applyZoom(float partialTicks, CallbackInfoReturnable<Float> callback) {
        if (ZoomModule.INSTANCE.isHeld()) callback.setReturnValue(ZoomModule.INSTANCE.getZoomFov());
        else if (FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController())
            callback.setReturnValue(Minecraft.getInstance().options.fov().get().floatValue());
    }

    /** Freelook: the camera uses the freelook angles instead of the player's view; third person then orbits. */
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float m0dev$freelookYaw(float yRot) {
        return me.m0dii.modules.freelook.FreelookModule.isActive() ? me.m0dii.modules.freelook.FreelookModule.yaw() : yRot;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float m0dev$freelookPitch(float xRot) {
        return me.m0dii.modules.freelook.FreelookModule.isActive() ? me.m0dii.modules.freelook.FreelookModule.pitch() : xRot;
    }
}
