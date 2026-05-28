package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererBlockAttributesMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"), require = 0)
    private void includeFluidsInCrosshairRaycast(float tickDelta, CallbackInfo ci) {
        applyFluidCrosshairTarget(tickDelta);
    }

    @Unique
    private void applyFluidCrosshairTarget(float tickDelta) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes || this.client == null || this.client.world == null) {
            return;
        }

        Entity cameraEntity = this.client.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }

        double reachDistance = this.client.player == null ? 5.0D : this.client.player.getBlockInteractionRange();
        HitResult fluidHit = cameraEntity.raycast(reachDistance, tickDelta, true);
        if (!(fluidHit instanceof BlockHitResult blockHit)) {
            return;
        }

        FluidState fluidState = this.client.world.getFluidState(blockHit.getBlockPos());
        if (!fluidState.isEmpty()) {
            this.client.crosshairTarget = fluidHit;
        }
    }
}

