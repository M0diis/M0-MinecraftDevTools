package me.m0dii.mixin;

import me.m0dii.modules.heldlight.HeldLightModule;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightCoordsUtil.BrightnessGetter.class)
public interface WorldRendererHeldLightMixin {

    @Inject(method = "lambda$static$0", at = @At("TAIL"), cancellable = true)
    private static void injectHeldLight(BlockAndLightGetter world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // Cheapest checks first: this runs for every vertex of every chunk being meshed.
        int heldBlockLight = HeldLightModule.getHeldLightLevel(pos);
        if (heldBlockLight <= 0) {
            return;
        }
        int packed = cir.getReturnValue();
        int currentBlock = (packed >> 4) & 0xF;
        if (heldBlockLight <= currentBlock) {
            return;
        }
        if (world.getBlockState(pos).isRedstoneConductor(world, pos)) {
            return;
        }

        int sky = (packed >> 20) & 0xF;
        cir.setReturnValue((heldBlockLight << 4) | (sky << 20));
    }
}

