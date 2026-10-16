package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksDebugInfoHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugHudMixin {
    @Inject(method = "extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V", at = @At("HEAD"))
    private void m0dev$drawText(GuiGraphicsExtractor context, List<String> lines, boolean alignLeft, int scaledWidth, CallbackInfo ci) {
        if (alignLeft && HungerTweaksDebugInfoHandler.INSTANCE != null) {
            HungerTweaksDebugInfoHandler.INSTANCE.onTextRender(lines);
        }
    }
}
