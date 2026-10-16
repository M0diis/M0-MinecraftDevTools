package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherRenderingMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void disableRainRendering(ClientLevel world,
                                      float tickProgress,
                                      Vec3 cameraPos,
                                      WeatherRenderState weatherRenderState,
                                      CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.disableRainEffects()) {
            return;
        }

        weatherRenderState.reset();
        ci.cancel();
    }


}
