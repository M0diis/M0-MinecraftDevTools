package me.m0dii.mixin;

import me.m0dii.modules.heldlight.HeldLightModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.BrightnessGetter.class)
public interface WorldRendererHeldLightMixin {

    @Inject(method = "method_68890", at = @At("TAIL"), cancellable = true, require = 0, remap = false)
    private static void injectHeldLight(BlockRenderView world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (world.getBlockState(pos).isSolidBlock(world, pos)) {
            return;
        }
        int heldBlockLight = HeldLightModule.getHeldLightLevel(MinecraftClient.getInstance(), pos);
        if (heldBlockLight <= 0) {
            return;
        }
        int packed = cir.getReturnValue();
        int currentBlock = (packed >> 4) & 0xF;
        if (heldBlockLight <= currentBlock) {
            return;
        }

        int sky = (packed >> 20) & 0xF;
        cir.setReturnValue((heldBlockLight << 4) | (sky << 20));
    }
}

