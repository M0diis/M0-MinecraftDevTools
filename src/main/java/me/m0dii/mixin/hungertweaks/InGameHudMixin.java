package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksHUDOverlayHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {
    @Inject(method = "extractFood", at = @At("HEAD"))
    private void m0dev$renderFoodPre(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onPreRenderFood(context, player, top, right);
        }
    }

    @Inject(method = "extractFood", at = @At("RETURN"))
    private void m0dev$renderFoodPost(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onRenderFood(context, player, top, right);
        }
    }

    @Inject(method = "extractHearts", at = @At("RETURN"))
    private void m0dev$renderHealthPost(GuiGraphicsExtractor context, Player player, int left, int top, int lines,
                                        int regeneratingHeartIndex, float maxHealth, int lastHealth, int health,
                                        int absorption, boolean blinking, CallbackInfo ci) {
        if (HungerTweaksHUDOverlayHandler.INSTANCE != null) {
            HungerTweaksHUDOverlayHandler.INSTANCE.onRenderHealth(context, player, left, top, lines,
                    regeneratingHeartIndex, maxHealth, lastHealth, health, absorption, blinking);
        }
    }
}
