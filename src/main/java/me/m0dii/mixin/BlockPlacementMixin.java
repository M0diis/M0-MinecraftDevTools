package me.m0dii.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Minecraft.class)
public class BlockPlacementMixin {

    @Unique
    private void setBlockStateWithPackets(Minecraft client, BlockPos pos, BlockState state, BlockHitResult blockHit) {
        if (client.player == null || client.player.connection == null) {
            return;
        }

        ItemStack itemStack = new ItemStack(state.getBlock().asItem());
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        client.player.connection.send(new ServerboundSetCreativeModeSlotPacket(36 + selectedSlot, itemStack));

        BlockHitResult newHit = new BlockHitResult(
                blockHit.getLocation(),
                blockHit.getDirection(),
                pos,
                blockHit.isInside()
        );

        client.player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                newHit,
                0
        ));
    }
}
