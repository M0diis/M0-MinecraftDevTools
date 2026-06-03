package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksHUDOverlayHandler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void m0dev$onTick(CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onClientTick();
        }
    }
}
