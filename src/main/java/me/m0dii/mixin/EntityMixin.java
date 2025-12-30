package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    @Shadow
    public float prevYaw;
    @Shadow
    public float prevPitch;

    private double forcedPitch;
    private double forcedYaw;

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void overrideYaw(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity) {
            if (CameraUtils.shouldPreventPlayerMovement()) {
                CameraUtils.updateCameraRotations((float) cursorDeltaX, (float) cursorDeltaY);
            }

            if (CameraUtils.shouldPreventPlayerMovement()) {
                ci.cancel();
                return;
            }

            // Update the internal rotations while no locking features are enabled
            // They will then be used as the forced rotations when some of the locking features are activated.
            this.forcedYaw = this.yaw;
            this.forcedPitch = this.pitch;
        }
    }
}
