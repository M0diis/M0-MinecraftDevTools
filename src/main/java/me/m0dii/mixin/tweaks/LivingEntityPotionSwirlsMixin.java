package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPotionSwirlsMixin {

    @Shadow
    protected abstract void clearPotionSwirls();

    @Inject(method = "updatePotionSwirls", at = @At("HEAD"), cancellable = true)
    private void hideOwnEffectSwirls(CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.hideOwnEffectParticles()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if ((Object) this != client.player) {
            return;
        }

        clearPotionSwirls();
        ci.cancel();
    }
}
