package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class GameRendererBlockAttributesMixin {



    @Inject(method = "pick", at = @At("TAIL"))
    private void includeFluidsInCrosshairRaycast(float tickDelta, CallbackInfo ci) {
        applyFluidCrosshairTarget(tickDelta);
    }

    @Unique
    private void applyFluidCrosshairTarget(float tickDelta) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes || ((Minecraft) (Object) this) == null || ((Minecraft) (Object) this).level == null) {
            return;
        }

        Entity cameraEntity = ((Minecraft) (Object) this).getCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        double reachDistance = ((Minecraft) (Object) this).player == null ? 5.0D : ((Minecraft) (Object) this).player.blockInteractionRange();
        HitResult fluidHit = cameraEntity.pick(reachDistance, tickDelta, true);
        if (!(fluidHit instanceof BlockHitResult blockHit)) {
            return;
        }

        FluidState fluidState = ((Minecraft) (Object) this).level.getFluidState(blockHit.getBlockPos());
        if (!fluidState.isEmpty()) {
            ((Minecraft) (Object) this).hitResult = fluidHit;
        }
    }
}

