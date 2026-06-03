package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksTooltipOverlayHandler;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TooltipComponent.class)
public interface TooltipComponentMixin extends TooltipComponent {
    @Inject(method = "of(Lnet/minecraft/text/OrderedText;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void m0dev$ofOrderedText(OrderedText text, CallbackInfoReturnable<TooltipComponent> cir) {
        if (text instanceof HungerTweaksTooltipOverlayHandler.FoodOverlayTextComponent overlayText) {
            cir.setReturnValue(overlayText.foodOverlay);
        }
    }

    @Inject(method = "of(Lnet/minecraft/item/tooltip/TooltipData;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void m0dev$ofTooltipData(TooltipData data, CallbackInfoReturnable<TooltipComponent> cir) {
        if (data instanceof HungerTweaksTooltipOverlayHandler.FoodOverlay overlay) {
            cir.setReturnValue(overlay);
        }
    }
}
