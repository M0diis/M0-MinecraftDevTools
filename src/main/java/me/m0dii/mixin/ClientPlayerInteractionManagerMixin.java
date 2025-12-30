package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraUtils;
import me.m0dii.modules.instabreak.InstaBreakModule;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Shadow
    private int blockBreakingCooldown;

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void handleInstantBreak(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (InstaBreakModule.INSTANCE.isEnabled()) {
            this.blockBreakingCooldown = 0;
        }

        if (CameraUtils.shouldPreventPlayerInputs()) {
            cir.setReturnValue(true);
        }
    }

}
