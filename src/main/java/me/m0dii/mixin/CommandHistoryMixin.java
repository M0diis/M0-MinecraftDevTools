package me.m0dii.mixin;

import me.m0dii.modules.commandhistory.CommandHistoryManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class CommandHistoryMixin {

    @Inject(method = "sendCommand", at = @At("HEAD"), require = 0)
    private void onSendCommand(String command, CallbackInfo ci) {
        recordCommand(command);
    }

    @Inject(method = "sendChat", at = @At("HEAD"), require = 0)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message != null && !message.trim().isEmpty() && message.startsWith("/")) {
            CommandHistoryManager.addCommand(message);
        }
    }

    @Unique
    private static void recordCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            CommandHistoryManager.addCommand("/" + command);
        }
    }
}

