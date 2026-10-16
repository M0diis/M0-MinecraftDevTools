package me.m0dii.mixin;

import me.m0dii.modules.mobdrops.MobDropTrackerServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMobDropTrackerMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void m0dev$captureDropStack(ServerLevel world,
                                        ItemStack stack,
                                        CallbackInfoReturnable<ItemEntity> cir) {
        MobDropTrackerServer.captureDroppedStack(cir.getReturnValue(), stack);
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void m0dev$captureDropStackWithOffset(ServerLevel world,
                                                  ItemStack stack,
                                                  float yOffset,
                                                  CallbackInfoReturnable<ItemEntity> cir) {
        MobDropTrackerServer.captureDroppedStack(cir.getReturnValue(), stack);
    }
}
