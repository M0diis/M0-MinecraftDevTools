package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerBlockAttributesMixin {

    @ModifyVariable(method = "useItemOn", at = @At("HEAD"), argsOnly = true)
    private BlockHitResult adjustPlacementAgainstFluids(BlockHitResult hitResult) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes || hitResult == null) {
            return hitResult;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return hitResult;
        }

        if (client.level.getFluidState(hitResult.getBlockPos()).isEmpty()) {
            return hitResult;
        }

        ItemStack held = client.player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem)) {
            return hitResult;
        }

        BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());
        return new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), placePos, false);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preventContainerInteractionWithBlockItem(LocalPlayer player,
                                                          InteractionHand hand,
                                                          BlockHitResult hitResult,
                                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (!ModConfig.blockAttributesPreventInteractions || player == null) {
            return;
        }

        var world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (!(heldStack.getItem() instanceof BlockItem) || player.isShiftKeyDown()) {
            return;
        }

        BlockState targetState = world.getBlockState(hitResult.getBlockPos());
        if (isUsuallyInteractable(targetState) || world.getBlockEntity(hitResult.getBlockPos()) != null) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Unique
    private static boolean isUsuallyInteractable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof NoteBlock
                || block instanceof BedBlock
                || block instanceof CraftingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof GrindstoneBlock
                || block instanceof SmithingTableBlock
                || block instanceof EnchantingTableBlock
                || block instanceof AnvilBlock
                || block instanceof BeaconBlock
                || block instanceof BellBlock
                || block instanceof LecternBlock
                || block instanceof ComposterBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof JukeboxBlock
                || block instanceof StandingSignBlock
                || block instanceof BarrelBlock
                || block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof FurnaceBlock
                || block instanceof BlastFurnaceBlock
                || block instanceof SmokerBlock
                || block instanceof BrewingStandBlock;
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void breakFluidLikeSolid(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        if (client.level.getFluidState(pos).isEmpty()) {
            return;
        }

        // Mature behavior: only perform client-assisted fluid removal in creative to avoid survival desync.
        if (!client.player.getAbilities().instabuild) {
            cir.setReturnValue(false);
            return;
        }

        client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction));
        client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
        client.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        cir.setReturnValue(true);
    }
}

