package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerBlockAttributesMixin {

    @ModifyVariable(method = "interactBlock", at = @At("HEAD"), argsOnly = true)
    private BlockHitResult adjustPlacementAgainstFluids(BlockHitResult hitResult) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes || hitResult == null) {
            return hitResult;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return hitResult;
        }

        if (client.world.getFluidState(hitResult.getBlockPos()).isEmpty()) {
            return hitResult;
        }

        ItemStack held = client.player.getMainHandStack();
        if (!(held.getItem() instanceof BlockItem)) {
            return hitResult;
        }

        BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
        return new BlockHitResult(hitResult.getPos(), hitResult.getSide(), placePos, false);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void preventContainerInteractionWithBlockItem(ClientPlayerEntity player,
                                                          Hand hand,
                                                          BlockHitResult hitResult,
                                                          CallbackInfoReturnable<ActionResult> cir) {
        if (!ModConfig.blockAttributesPreventInteractions || player == null) {
            return;
        }

        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        if (!(heldStack.getItem() instanceof BlockItem) || player.isSneaking()) {
            return;
        }

        BlockState targetState = world.getBlockState(hitResult.getBlockPos());
        if (isUsuallyInteractable(targetState) || world.getBlockEntity(hitResult.getBlockPos()) != null) {
            cir.setReturnValue(ActionResult.PASS);
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
                || block instanceof TrapdoorBlock
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
                || block instanceof SignBlock
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

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void breakFluidLikeSolid(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.blockAttributesSolidFluidHitboxes) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (client.world.getFluidState(pos).isEmpty()) {
            return;
        }

        // Mature behavior: only perform client-assisted fluid removal in creative to avoid survival desync.
        if (!client.player.getAbilities().creativeMode) {
            cir.setReturnValue(false);
            return;
        }

        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
        client.world.setBlockState(pos, Blocks.AIR.getDefaultState());
        cir.setReturnValue(true);
    }
}

