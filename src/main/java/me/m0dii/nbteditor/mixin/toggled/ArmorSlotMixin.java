package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.server.ServerMiscUtil;
import me.m0dii.nbteditor.server.ServerMixinLink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Same as PlayerScreenHandler1Mixin
@Mixin(targets = "net.minecraft.screen.slot.ArmorSlot")
public class ArmorSlotMixin {
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        PlayerEntity owner = ((PlayerInventory) ((Slot) (Object) this).inventory).player;
        if (owner instanceof ServerPlayerEntity) {
            if (ServerMiscUtil.hasPermissionLevel(owner, 2) && ServerMixinLink.NO_SLOT_RESTRICTIONS_PLAYERS.getOrDefault(owner, false)) {
                info.setReturnValue(true);
            }
        } else {
            if (M0DevToolsClient.SERVER_CONN.isEditingAllowed() && ConfigScreen.isNoSlotRestrictions()) {
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void canTakeItems(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (player instanceof ServerPlayerEntity) {
            if (ServerMiscUtil.hasPermissionLevel(player, 2) && ServerMixinLink.NO_SLOT_RESTRICTIONS_PLAYERS.getOrDefault(player, false)) {
                info.setReturnValue(true);
            }
        } else {
            if (M0DevToolsClient.SERVER_CONN.isEditingAllowed() && ConfigScreen.isNoSlotRestrictions()) {
                info.setReturnValue(true);
            }
        }
    }
}
