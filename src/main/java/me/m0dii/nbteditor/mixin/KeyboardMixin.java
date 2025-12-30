package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.screens.widgets.FormattedTextFieldWidget;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z"))
    private boolean isActive(NarratorManager manager) {
        if (MiscUtil.client.currentScreen != null) {
            Element focused = MiscUtil.client.currentScreen.getFocused();
            while (focused != null) {
                if (focused instanceof FormattedTextFieldWidget) {
                    return false;
                } else if (focused instanceof ParentElement parent) {
                    focused = parent.getFocused();
                } else {
                    break;
                }
            }
        }
        return manager.isActive();
    }
}
