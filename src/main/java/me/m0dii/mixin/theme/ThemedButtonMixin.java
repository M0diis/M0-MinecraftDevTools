package me.m0dii.mixin.theme;

import me.m0dii.gui.theme.ThemedButtonRenderer;
import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.Plain.class)
public abstract class ThemedButtonMixin {
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void m0devThemedContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ThemedScreen.isActive()) {
            ThemedButtonRenderer.render((Button) (Object) this, graphics);
            ci.cancel();
        }
    }
}
