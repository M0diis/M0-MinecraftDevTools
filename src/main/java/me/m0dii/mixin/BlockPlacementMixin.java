package me.m0dii.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftClient.class)
public class BlockPlacementMixin {

    @Unique
    private void setBlockStateWithPackets(MinecraftClient client, BlockPos pos, BlockState state, BlockHitResult blockHit) {
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }

        ItemStack itemStack = new ItemStack(state.getBlock().asItem());
        int selectedSlot = client.player.getInventory().selectedSlot;
        client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + selectedSlot, itemStack));

        BlockHitResult newHit = new BlockHitResult(
                blockHit.getPos(),
                blockHit.getSide(),
                pos,
                blockHit.isInsideBlock()
        );

        client.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                newHit,
                0
        ));
    }
}
