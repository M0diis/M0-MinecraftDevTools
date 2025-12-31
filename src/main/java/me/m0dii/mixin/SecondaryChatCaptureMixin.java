package me.m0dii.mixin;

import me.m0dii.modules.chat.SecondaryChatManager;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class SecondaryChatCaptureMixin {

    // Hook the simple addMessage(Text) - catches command feedback
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void m0devtools$onAddMessage(Text message, CallbackInfo ci) {
        handleMessage(message, ci);
    }

    // Hook the full signature addMessage - catches actual chat messages
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void m0devtools$onAddMessageFull(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        handleMessage(message, ci);
    }

    @Unique
    private void handleMessage(Text message, CallbackInfo ci) {
        try {
            if (!ModConfig.secondaryChatEnabled || message == null) {
                return;
            }

            if (!SecondaryChatManager.matchesFilter(message)) {
                return;
            }

            SecondaryChatManager.push(message);

            if (ModConfig.secondaryChatInterceptMode == ModConfig.ChatInterceptMode.MOVE) {
                ci.cancel();
            }
        } catch (Exception e) {
            // Silently catch errors to avoid breaking chat
        }
    }
}
