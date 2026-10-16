package me.m0dii.mixin.hungertweaks;

import me.m0dii.modules.hungertweaks.HungerTweaksTooltipOverlayHandler;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientTooltipComponent.class)
public interface TooltipComponentMixin extends ClientTooltipComponent {
    @Inject(method = "create(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void m0dev$ofOrderedText(FormattedCharSequence text, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (text instanceof HungerTweaksTooltipOverlayHandler.FoodOverlayTextComponent overlayText) {
            cir.setReturnValue(overlayText.foodOverlay);
        }
    }

    @Inject(method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;", at = @At("HEAD"), cancellable = true)
    private static void m0dev$ofTooltipData(TooltipComponent data, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if (data instanceof HungerTweaksTooltipOverlayHandler.FoodOverlay overlay) {
            cir.setReturnValue(overlay);
        }
    }
}
