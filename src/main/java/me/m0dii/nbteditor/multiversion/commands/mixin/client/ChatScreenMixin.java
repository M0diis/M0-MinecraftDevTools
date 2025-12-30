package me.m0dii.nbteditor.multiversion.commands.mixin.client;

import me.m0dii.nbteditor.multiversion.commands.ClientCommandInternals;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;Z)V"), cancellable = true)
    @Group(name = "keyPressed", min = 1)
    private void enterPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAA");
        String text = StringUtils.normalizeSpace(chatField.getText().trim());
        if (text.length() <= 256) {
            System.out.println("THIS IS NORMAL LENGTH");
            return;
        }
        System.out.println("THIS IS GETTING PRINTDD");
        if (text.charAt(0) == '/' && ClientCommandInternals.executeCommand(text.substring(1))) {
            MiscUtil.client.inGameHud.getChatHud().addToMessageHistory(text);
            if (MiscUtil.client.currentScreen instanceof ChatScreen) {
                MiscUtil.client.setScreen(null);
            }
            info.setReturnValue(true);
        } else {
            chatField.text = (text.length() <= 256 ? text : text.substring(0, 256));
        }
    }

}
