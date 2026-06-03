package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksDebugInfoHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V", at = @At("HEAD"))
    private void m0dev$drawText(DrawContext context, List<String> lines, boolean right, CallbackInfo ci) {
        if (!right && HungerTweaksDebugInfoHandler.INSTANCE != null) {
            HungerTweaksDebugInfoHandler.INSTANCE.onTextRender(lines);
        }
    }
}
