package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.network.HungerTweaksSyncHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void m0dev$onPlayerConnect(Connection conn, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        HungerTweaksSyncHandler.onPlayerLoggedIn(player);
    }
}
