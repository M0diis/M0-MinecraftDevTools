package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.network.HungerTweaksSyncHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends Entity {
    protected ServerPlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void m0dev$onUpdate(CallbackInfo ci) {
        HungerTweaksSyncHandler.onPlayerUpdate((ServerPlayerEntity) (Object) this);
    }
}
