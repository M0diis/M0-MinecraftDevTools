package me.m0dii.mixin;

import me.m0dii.modules.mobdrops.MobDropTrackerServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMobDropTrackerMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void m0dev$beginMobDropCapture(DamageSource damageSource, CallbackInfo ci) {
        MobDropTrackerServer.beginDeathCapture((LivingEntity) (Object) this, damageSource);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void m0dev$endMobDropCapture(DamageSource damageSource, CallbackInfo ci) {
        MobDropTrackerServer.endDeathCapture();
    }
}
