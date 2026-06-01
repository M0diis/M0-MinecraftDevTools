package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {

    @Inject(method = "buildPrecipitationPieces", at = @At("HEAD"), cancellable = true)
    private void disableRainRendering(World world,
                                      int ticks,
                                      float tickProgress,
                                      Vec3d cameraPos,
                                      WeatherRenderState weatherRenderState,
                                      CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.disableRainEffects()) {
            return;
        }

        weatherRenderState.clear();
        ci.cancel();
    }

    @Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void disableRainParticlesAndSound(ClientWorld world,
                                              Camera camera,
                                              int ticks,
                                              ParticlesMode particlesMode,
                                              int radius,
                                              CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableRainEffects()) {
            ci.cancel();
        }
    }
}
