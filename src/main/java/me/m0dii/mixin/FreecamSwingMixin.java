package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class FreecamSwingMixin {
    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void preventHandSwing(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this == Minecraft.getInstance().player && CameraUtils.shouldPreventPlayerInputs()) callback.setReturnValue(false);
    }
}
