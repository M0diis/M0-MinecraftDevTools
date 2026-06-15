package me.m0dii.mixin;

import me.m0dii.utils.PickBlockHotbarSlotResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MiddleClickMixin {

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void onMiddleClick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || !client.player.getAbilities().creativeMode || client.isInSingleplayer()) {
            return;
        }

        if (client.options.pickItemKey.wasPressed()) {
            HitResult hit = client.crosshairTarget;
            ItemStack stack = ItemStack.EMPTY;

            if (hit instanceof BlockHitResult blockHit && client.world != null) {
                stack = client.world.getBlockState(blockHit.getBlockPos()).getBlock().asItem().getDefaultStack();
            } else if (hit instanceof EntityHitResult entityHit) {
                stack = entityHit.getEntity().getPickBlockStack();
                if (stack == null) {
                    stack = ItemStack.EMPTY;
                }
            }

            if (!stack.isEmpty()) {
                PlayerInventory inventory = client.player.getInventory();
                int selectedSlot = inventory.getSelectedSlot();
                int targetSlot = PickBlockHotbarSlotResolver.resolveTargetSlot(inventory, stack);

                if (!ItemStack.areItemsAndComponentsEqual(inventory.getStack(targetSlot), stack)) {
                    int creativeSlot = targetSlot + 36;
                    client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(creativeSlot, stack));
                    inventory.setStack(targetSlot, stack);
                }

                if (selectedSlot != targetSlot) {
                    inventory.setSelectedSlot(targetSlot);
                    client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
                }
            }
        }
    }
}
