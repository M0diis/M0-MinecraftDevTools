package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.screens.containers.ClientHandledScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Redirect(method = "internalOnSlotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/ItemEntity;"))
    private ItemEntity dropItem(PlayerEntity player, ItemStack stack, boolean retainOwnership) {
        if (!(MiscUtil.client.currentScreen instanceof ClientHandledScreen)) {
            return player.dropItem(stack, retainOwnership);
        }

        MiscUtil.dropCreativeStack(stack);
        return null;
    }

}
