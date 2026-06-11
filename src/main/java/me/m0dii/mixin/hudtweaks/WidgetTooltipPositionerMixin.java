package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tooltip.WidgetTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WidgetTooltipPositioner.class)
public abstract class WidgetTooltipPositionerMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private static ScreenRect m0devHudTweaksWidgetTooltipRect(ScreenRect original) {
        float scale = Math.max(0.01f, HudTweaksSettings.getElement(HudTweaksSettings.ElementType.TOOLTIP).scale);
        return new ScreenRect(
                Math.round(original.getLeft() / scale),
                Math.round(original.getTop() / scale),
                Math.round(original.width() / scale),
                Math.round(original.height() / scale)
        );
    }
}
