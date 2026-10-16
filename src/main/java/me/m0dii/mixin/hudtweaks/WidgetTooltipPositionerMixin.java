package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MenuTooltipPositioner.class)
public abstract class WidgetTooltipPositionerMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private static ScreenRectangle m0devHudTweaksWidgetTooltipRect(ScreenRectangle original) {
        float scale = Math.max(0.01f, HudTweaksSettings.getElement(HudTweaksSettings.ElementType.TOOLTIP).scale);
        return new ScreenRectangle(
                Math.round(original.left() / scale),
                Math.round(original.top() / scale),
                Math.round(original.width() / scale),
                Math.round(original.height() / scale)
        );
    }
}
