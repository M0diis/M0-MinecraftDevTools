package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.network.HungerTweaksSyncHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Entity {
    protected ServerPlayerEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void m0dev$onUpdate(CallbackInfo ci) {
        HungerTweaksSyncHandler.onPlayerUpdate((ServerPlayer) (Object) this);
    }
}
