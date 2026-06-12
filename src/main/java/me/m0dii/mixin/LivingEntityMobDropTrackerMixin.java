package me.m0dii.mixin;

import me.m0dii.modules.mobdrops.MobDropTrackerServer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMobDropTrackerMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void m0dev$beginMobDropCapture(DamageSource damageSource, CallbackInfo ci) {
        MobDropTrackerServer.beginDeathCapture((LivingEntity) (Object) this, damageSource);
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void m0dev$endMobDropCapture(DamageSource damageSource, CallbackInfo ci) {
        MobDropTrackerServer.endDeathCapture();
    }
}
