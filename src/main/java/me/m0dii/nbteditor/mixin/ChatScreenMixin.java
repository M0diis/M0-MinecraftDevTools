package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.DrawableHelper;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setMaxLength(I)V"), index = 0)
    private int setMaxLength(int length) {
        if (ConfigScreen.isChatLimitExtended()) {
            return Integer.MAX_VALUE;
        }
        return length;
    }

    @Inject(method = "render", at = @At("HEAD"))
    @Group(name = "render", min = 1)
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        MixinLink.renderChatLimitWarning((ChatScreen) (Object) this, DrawableHelper.getMatrices(context));
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        if (!(MiscUtil.client.currentScreen instanceof ChatScreen)) {
            info.setReturnValue(true);
            info.cancel();
        }
    }

}
