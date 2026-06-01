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
    private void disableNetherFog(FogData fogData,
                                  Camera camera,
                                  ClientWorld world,
                                  float viewDistance,
                                  RenderTickCounter tickCounter,
                                  CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.disableNetherFog()) {
            return;
        }

        if (world.getRegistryKey() != World.NETHER) {
            return;
        }

        float far = Math.max(viewDistance, fogData.renderDistanceEnd) + 1024.0f;
        fogData.environmentalStart = far - 64.0f;
        fogData.environmentalEnd = far;
        fogData.skyEnd = far;
        fogData.cloudEnd = far;
    }
}
