package me.m0dii.mixin;

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

@Mixin(ItemStack.class)

public abstract class ItemstackMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    protected void injectEditTooltipMethod(Item.TooltipContext context,
                                           @Nullable PlayerEntity player,
                                           TooltipType type,
                                           CallbackInfoReturnable<ArrayList<Text>> info) {

        if (type.isAdvanced() && NBTTooltipModule.INSTANCE.isEnabled()) {
            ItemStack itemStack = (ItemStack) (Object) this;
            ArrayList<Text> list = info.getReturnValue();

            if (itemStack.manager$hasNbt()) {
                info.setReturnValue(new ArrayList<>(NBTTooltipModule.getNbtTooltipText(itemStack, list)));
            }
        }
    }
}
