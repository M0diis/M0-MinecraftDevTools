package me.m0dii.mixin;

import me.m0dii.modules.hungertweaks.HungerTweaksTooltipOverlayHandler;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ItemstackMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    protected void injectEditTooltipMethod(Item.TooltipContext context,
                                           @Nullable PlayerEntity player,
                                           TooltipType type,
                                           CallbackInfoReturnable<List> info) {
        ItemStack itemStack = (ItemStack) (Object) this;
        List tooltip = info.getReturnValue();

        if (tooltip == null) {
            return;
        }

        if (NBTTooltipModule.INSTANCE.isEnabled()) {
            tooltip = new ArrayList<>(NBTTooltipModule.getNbtTooltipText(itemStack, (List<Text>) tooltip));
        }

        if (HungerTweaksTooltipOverlayHandler.INSTANCE != null) {
            HungerTweaksTooltipOverlayHandler.INSTANCE.onItemTooltip(itemStack, player, context, type, tooltip);
        }

        info.setReturnValue(tooltip);
    }
}
