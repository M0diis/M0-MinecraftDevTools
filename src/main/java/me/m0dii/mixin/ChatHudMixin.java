package me.m0dii.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.chat.SecondaryChatManager;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
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
public class ChatHudMixin {

    // Hook the simple addMessage(Text) - catches command feedback
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessageSecondaryChat(Text message, CallbackInfo ci) {
        handleMessage(message, ci);
    }

    // Hook the full signature addMessage - catches actual chat messages
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessageFullSecondaryChat(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        handleMessage(message, ci);

        MessageHistoryManager.addMessage(message);
    }

    @Unique
    private void handleMessage(Text message, CallbackInfo ci) {
        try {
            if (ModConfig.secondaryChatEnabled && message != null) {
                if (SecondaryChatManager.matchesFilter(message)) {
                    SecondaryChatManager.push(message);

                    if (ModConfig.secondaryChatInterceptMode == ModConfig.ChatInterceptMode.MOVE) {
                        ci.cancel();
                    }
                }
            }
        } catch (Exception e) {
            // Silently catch errors to avoid breaking chat
        }
    }

    // Modify the chat history size constant
    @ModifyExpressionValue(
            method = {"addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", "addVisibleMessage", "addToMessageHistory"},
            at = {@At(value = "CONSTANT", args = {"intValue=100"})})
    public int modifyChatHistorySize(int original) {
        return ModConfig.messageBoxHistoryLimit;
    }
}
