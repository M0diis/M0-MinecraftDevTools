package me.m0dii.nbteditor.mixin;

import me.m0dii.M0DevToolsClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "closeScreen", at = @At("HEAD"))
    private void closeScreen(CallbackInfo info) {
        M0DevToolsClient.SERVER_CONN.closeContainerScreen();
    }
}
