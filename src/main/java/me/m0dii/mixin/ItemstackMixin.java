package me.m0dii.mixin;

import me.m0dii.modules.hungertweaks.HungerTweaksTooltipOverlayHandler;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    protected void injectEditTooltipMethod(Item.TooltipContext context,
                                           @Nullable Player player,
                                           TooltipFlag type,
                                           CallbackInfoReturnable<List> info) {
        ItemStack itemStack = (ItemStack) (Object) this;
        List tooltip = info.getReturnValue();

        if (tooltip == null) {
            return;
        }

        if (NBTTooltipModule.INSTANCE.isEnabled()) {
            tooltip = new ArrayList<>(NBTTooltipModule.getNbtTooltipText(itemStack, (List<Component>) tooltip));
        }

        if (HungerTweaksTooltipOverlayHandler.INSTANCE != null) {
            HungerTweaksTooltipOverlayHandler.INSTANCE.onItemTooltip(itemStack, player, context, type, tooltip);
        }

        info.setReturnValue(tooltip);
    }
}
