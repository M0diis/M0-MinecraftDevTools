package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.network.HungerTweaksSyncHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void m0dev$onPlayerConnect(ClientConnection conn, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        HungerTweaksSyncHandler.onPlayerLoggedIn(player);
    }
}
