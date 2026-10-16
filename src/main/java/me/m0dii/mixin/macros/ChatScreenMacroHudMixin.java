package me.m0dii.mixin.macros;

import me.m0dii.modules.macros.hud.MacroHudRuntime;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMacroHudMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (MacroHudRuntime.handleClick(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
        }
    }
}

