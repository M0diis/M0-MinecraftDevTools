package me.m0dii.mixin;

import me.m0dii.utils.PickBlockHotbarSlotResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MiddleClickMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onMiddleClick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || !client.player.getAbilities().instabuild || client.isLocalServer()) {
            return;
        }

        if (client.options.keyPickItem.consumeClick()) {
            HitResult hit = client.hitResult;
            ItemStack stack = ItemStack.EMPTY;

            if (hit instanceof BlockHitResult blockHit && client.level != null) {
                stack = client.level.getBlockState(blockHit.getBlockPos()).getBlock().asItem().getDefaultInstance();
            } else if (hit instanceof EntityHitResult entityHit) {
                stack = entityHit.getEntity().getPickResult();
                if (stack == null) {
                    stack = ItemStack.EMPTY;
                }
            }

            if (!stack.isEmpty()) {
                Inventory inventory = client.player.getInventory();
                int selectedSlot = inventory.getSelectedSlot();
                int targetSlot = PickBlockHotbarSlotResolver.resolveTargetSlot(inventory, stack);

                if (!ItemStack.isSameItemSameComponents(inventory.getItem(targetSlot), stack)) {
                    int creativeSlot = targetSlot + 36;
                    client.player.connection.send(new ServerboundSetCreativeModeSlotPacket(creativeSlot, stack));
                    inventory.setItem(targetSlot, stack);
                }

                if (selectedSlot != targetSlot) {
                    inventory.setSelectedSlot(targetSlot);
                    client.player.connection.send(new ServerboundSetCarriedItemPacket(targetSlot));
                }
            }
        }
    }
}
