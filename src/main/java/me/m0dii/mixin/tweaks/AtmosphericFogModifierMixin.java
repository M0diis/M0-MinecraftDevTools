package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogModifier.class)
public class AtmosphericFogModifierMixin {

    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void applyFogTweaks(FogData fogData,
                                Camera camera,
                                ClientWorld world,
                                float viewDistance,
                                RenderTickCounter tickCounter,
                                CallbackInfo ci) {
        boolean disableRenderDistanceFog = TweaksModule.INSTANCE.disableRenderDistanceFog();
        boolean disableNetherFog = TweaksModule.INSTANCE.disableNetherFog() && world.getRegistryKey() == World.NETHER;
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
