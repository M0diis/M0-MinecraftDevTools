package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogModifierMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void applyFogTweaks(FogData fogData,
                                Camera camera,
                                ClientLevel world,
                                float viewDistance,
                                DeltaTracker tickCounter,
                                CallbackInfo ci) {
        boolean disableRenderDistanceFog = TweaksModule.INSTANCE.disableRenderDistanceFog();
        boolean disableNetherFog = TweaksModule.INSTANCE.disableNetherFog() && world.dimension() == Level.NETHER;
        if (!disableRenderDistanceFog && !disableNetherFog) {
            return;
        }

        float far = Math.max(viewDistance, fogData.renderDistanceEnd) + 1024.0f;
        if (disableRenderDistanceFog) {
            fogData.renderDistanceStart = far - 64.0f;
            fogData.renderDistanceEnd = far;
        }
        if (disableNetherFog) {
            fogData.environmentalStart = far - 64.0f;
            fogData.environmentalEnd = far;
            fogData.skyEnd = far;
            fogData.cloudEnd = far;
        }
    }
}
