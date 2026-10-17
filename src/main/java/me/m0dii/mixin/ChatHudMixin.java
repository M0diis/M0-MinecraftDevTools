package me.m0dii.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.automation.AutomationModule;
import me.m0dii.modules.chat.SecondaryChatManager;
import me.m0dii.modules.chat.SecondaryChatOverlay;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.watson.CoreProtectTracker;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderSecondaryChatReplacement(GuiGraphicsExtractor context,
                                                  Font textRenderer,
                                                  int currentTick,
                                                  int mouseX,
                                                  int mouseY,
                                                  ChatComponent.DisplayMode focused,
                                                  boolean refresh,
                                                  CallbackInfo ci) {
        if (SecondaryChatOverlay.shouldSuppressVanillaChat()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessageSecondaryChat(Component message, MessageSignature signature, net.minecraft.client.multiplayer.chat.GuiMessageSource source, GuiMessageTag indicator, CallbackInfo ci) {
        handleMessage(message, ci);

        MessageHistoryManager.addMessage(message);
        CoreProtectTracker.onChatMessage(message);
        AutomationModule.INSTANCE.onChatMessage(message);
    }

    /** Chat highlights: recolour matching words before the line is stored and shown. */
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component m0dev$highlightMessage(Component message) {
        return me.m0dii.modules.chathighlight.ChatHighlightModule.apply(message);
    }

    @Unique
    private void handleMessage(Component message, CallbackInfo ci) {
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
            method = {"addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", "addMessageToDisplayQueue", "addRecentChat"},
            at = {@At(value = "CONSTANT", args = {"intValue=100"})})
    public int modifyChatHistorySize(int original) {
        return ModConfig.messageBoxHistoryLimit;
    }
}
