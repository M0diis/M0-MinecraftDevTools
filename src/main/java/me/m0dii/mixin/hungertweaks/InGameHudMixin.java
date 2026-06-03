package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksHUDOverlayHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderFood", at = @At("HEAD"))
    private void m0dev$renderFoodPre(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onPreRenderFood(context, player, top, right);
        }
    }

    @Inject(method = "renderFood", at = @At("RETURN"))
    private void m0dev$renderFoodPost(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onRenderFood(context, player, top, right);
        }
    }

    @Inject(method = "renderHealthBar", at = @At("RETURN"))
    private void m0dev$renderHealthPost(DrawContext context, PlayerEntity player, int left, int top, int lines,
                                        int regeneratingHeartIndex, float maxHealth, int lastHealth, int health,
                                        int absorption, boolean blinking, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onRenderHealth(context, player, left, top, lines,
                    regeneratingHeartIndex, maxHealth, lastHealth, health, absorption, blinking);
        }
    }
}
