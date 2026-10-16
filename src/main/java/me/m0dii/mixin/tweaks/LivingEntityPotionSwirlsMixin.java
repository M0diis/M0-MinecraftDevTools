package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPotionSwirlsMixin {

    @Shadow
    protected abstract void removeEffectParticles();

    @Inject(method = "updateSynchronizedMobEffectParticles", at = @At("HEAD"), cancellable = true)
    private void hideOwnEffectSwirls(CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.hideOwnEffectParticles()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if ((Object) this != client.player) {
            return;
        }

        removeEffectParticles();
        ci.cancel();
    }
}
