package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientWorldMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void disableBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableBlockBreakingParticles()) {
            ci.cancel();
        }
    }
    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void disableRainParticlesAndSound(CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableRainEffects()) ci.cancel();
    }
}

