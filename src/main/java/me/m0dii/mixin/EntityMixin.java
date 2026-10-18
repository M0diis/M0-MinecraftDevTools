package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Unique
    private double forcedPitch;
    @Unique
    private double forcedYaw;

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void overrideYaw(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) {
            if (me.m0dii.modules.freelook.FreelookModule.isActive()) {
                me.m0dii.modules.freelook.FreelookModule.turn(cursorDeltaX, cursorDeltaY);
                ci.cancel();
                return;
            }
            if (CameraUtils.shouldPreventPlayerMovement()) {
                CameraUtils.updateCameraRotations((float) cursorDeltaX, (float) cursorDeltaY);
            }

            if (CameraUtils.shouldPreventPlayerMovement()) {
                ci.cancel();
                return;
            }

            // Update the internal rotations while no locking features are enabled
            // They will then be used as the forced rotations when some of the locking features are activated.
            this.forcedYaw = this.yRot;
            this.forcedPitch = this.xRot;
        }
    }
}
