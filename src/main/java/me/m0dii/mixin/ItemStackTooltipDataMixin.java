package me.m0dii.mixin;

import me.m0dii.modules.nbttooltip.ShulkerPreviewTooltipData;
import me.m0dii.modules.nbttooltip.ShulkerTooltipModule;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipDataMixin {

    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void injectShulkerPreviewData(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!ShulkerTooltipModule.INSTANCE.isEnabled()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!ShulkerTooltipModule.canPreview(stack)) {
            return;
        }

        cir.setReturnValue(Optional.of(new ShulkerPreviewTooltipData(ShulkerTooltipModule.readContainerStacks(stack))));
    }
}

