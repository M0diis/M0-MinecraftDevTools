package me.m0dii.nbteditor.multiversion.mixin;

import me.m0dii.nbteditor.multiversion.MVScreen;
import me.m0dii.nbteditor.multiversion.MVScreenParent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Screen.class)
public class ScreenMixin implements MVScreenParent {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"), require = 0)
    private void renderBackground(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        if (!((Object) this instanceof MVScreen)) {
            screen.renderBackground(context, mouseX, mouseY, delta);
        }
        // Removes added renderBackground in 1.20.2+
    }
}
