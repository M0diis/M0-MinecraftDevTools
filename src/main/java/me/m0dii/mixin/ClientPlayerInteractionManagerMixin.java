package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import me.m0dii.modules.instantbreak.InstantBreakModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow
    private int destroyDelay;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void handleInstantBreak(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (InstantBreakModule.INSTANCE.isEnabled()) {
            this.destroyDelay = 0;
        }

        if (CameraUtils.shouldPreventPlayerInputs()) {
            cir.setReturnValue(true);
        }
    }

}
