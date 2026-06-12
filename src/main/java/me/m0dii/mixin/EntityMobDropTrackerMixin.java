package me.m0dii.mixin;

import me.m0dii.modules.mobdrops.MobDropTrackerServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMobDropTrackerMixin {
    @Inject(method = "dropStack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/ItemEntity;",
            at = @At("RETURN"))
    private void m0dev$captureDropStack(ServerWorld world,
                                        ItemStack stack,
                                        CallbackInfoReturnable<ItemEntity> cir) {
        MobDropTrackerServer.captureDroppedStack(cir.getReturnValue(), stack);
    }

    @Inject(method = "dropStack(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;",
            at = @At("RETURN"))
    private void m0dev$captureDropStackWithOffset(ServerWorld world,
                                                  ItemStack stack,
                                                  float yOffset,
                                                  CallbackInfoReturnable<ItemEntity> cir) {
        MobDropTrackerServer.captureDroppedStack(cir.getReturnValue(), stack);
    }
}
