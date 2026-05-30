package me.m0dii.mixin.macros;

import me.m0dii.modules.macros.hud.MacroHudRuntime;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMacroHudMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void m0dev$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (MacroHudRuntime.handleClick(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
        }
    }
}

