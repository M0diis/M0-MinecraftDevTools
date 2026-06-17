package me.m0dii.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.automation.AutomationModule;
import me.m0dii.modules.chat.SecondaryChatManager;
import me.m0dii.modules.chat.SecondaryChatOverlay;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.watson.CoreProtectTracker;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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

    @Inject(
            method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderSecondaryChatReplacement(DrawContext context,
                                                  TextRenderer textRenderer,
                                                  int currentTick,
                                                  int mouseX,
                                                  int mouseY,
                                                  boolean focused,
                                                  boolean refresh,
                                                  CallbackInfo ci) {
        if (SecondaryChatOverlay.shouldSuppressVanillaChat()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessageSecondaryChat(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        handleMessage(message, ci);

        MessageHistoryManager.addMessage(message);
        CoreProtectTracker.onChatMessage(message);
        AutomationModule.INSTANCE.onChatMessage(message);
    }

    @Unique
    private void handleMessage(Text message, CallbackInfo ci) {
        try {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            if (settings.enabled && message != null) {
                SecondaryChatManager.RouteResult result = SecondaryChatManager.routeIncoming(message);
                if (settings.renderMode == SecondaryChatSettings.RenderMode.ADDON
                        && result.matchedFilter()
                        && settings.interceptMode == SecondaryChatSettings.InterceptMode.MOVE) {
                    ci.cancel();
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
