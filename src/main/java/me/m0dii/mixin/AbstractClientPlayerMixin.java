package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void overrideIsSpectator(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(CameraUtils.getFreeCameraSpectator());
        }
    }

}
