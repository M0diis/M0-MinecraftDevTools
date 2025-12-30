package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(at = @At("RETURN"), method = "method_7950(Lnet/minecraft/class_1657;Lnet/minecraft/class_1836;)Ljava/util/List;", remap = false, require = 0)
    @SuppressWarnings("target")
    private void getTooltip(PlayerEntity player, TooltipType context, CallbackInfoReturnable<List<Text>> info) {
        MixinLink.modifyTooltip((ItemStack) (Object) this, info.getReturnValue());
    }
}
