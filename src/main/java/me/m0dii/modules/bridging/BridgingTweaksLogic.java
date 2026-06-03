package me.m0dii.modules.bridging;

import me.m0dii.utils.ModConfig;
import net.minecraft.block.AbstractTorchBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.List;

public final class BridgingTweaksLogic {
    private static final double MAXIMUM_PLACE_REACH = 4.5D;
    private static final double TRAPDOOR_HEIGHT = 3.0D / 16.0D;
    private static final double SLAB_HEIGHT = 8.0D / 16.0D;
    private static final double DIRECTION_SIMILARITY_THRESHOLD = 0.1D;

    private BridgingTweaksLogic() {
    }

    public static boolean tryBridgePlacement(MinecraftClient client, ClientPlayerInteractionManager interactionManager) {
        if (!BridgingTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }
        if (client == null || client.player == null || client.world == null || interactionManager == null) {
            return false;
        }
        if (client.crosshairTarget != null && client.crosshairTarget.getType() != HitResult.Type.MISS) {
            return false;
        }
        if (BridgingTweaksModule.INSTANCE.onlyBridgeWhenCrouched() && !client.player.isSneaking()) {
            return false;
        }
        if (interactionManager.isBreakingBlock()) {
            return false;
        }

        BridgingTarget target = BridgingTweaksState.getLastAssistTarget();
        if (target == null) {
            target = findAssistTarget(client, client.player);
        }
        if (target == null) {
            return false;
        }

        Hand hand = getPlacementHand(client.player);
        if (hand == null) {
            return false;
        }

        ItemStack stack = client.player.getStackInHand(hand);
        BlockHitResult hitResult = createPlacementHit(stack, client.world, target);
        ActionResult result = interactionManager.interactBlock(client.player, hand, hitResult);
        if (!result.isAccepted()) {
            return false;
        }

        client.player.swingHand(hand);
        return true;
    }

    public static BridgingTarget findAssistTarget(MinecraftClient client, PlayerEntity player) {
        if (client == null || player == null || client.world == null) {
            return null;
        }
        if (client.crosshairTarget != null && client.crosshairTarget.getType() != HitResult.Type.MISS) {
            return null;
        }
        if (!isHoldingPlaceable(player)) {
            return null;
        }

        BridgingPerspective perspective = BridgingPerspective.resolve(client, player);
        List<BlockPos> path = getViewBlockPath(client, player, perspective);
        List<Direction> sides = getValidAssistSides(perspective.lookVector());

        for (BlockPos placePos : path) {
            if (!isPlacementAllowed(client.world, placePos)) {
                continue;
            }

            if (player.getBoundingBox().intersects(new Box(placePos))) {
                continue;
            }

            for (Direction side : sides) {
                if (canBuildOffSide(player, client.world, placePos, side)) {
                    return new BridgingTarget(placePos, side);
                }
            }
        }

        return null;
    }

    public static List<BlockPos> getViewBlockPath(MinecraftClient client, Entity player, BridgingPerspective perspective) {
        if (client == null || player == null) {
            return List.of();
        }

        double reach = client.player == null ? MAXIMUM_PLACE_REACH : client.player.getBlockInteractionRange();
        Vec3d playerViewEnd = player.getRotationVec(1.0f).multiply(reach).add(player.getEyePos());
        Vec3d cameraOrigin = perspective.position();
        double distance = playerViewEnd.distanceTo(cameraOrigin);

        float minimumDistance = BridgingTweaksModule.wrapPercent(ModConfig.bridgingMinBridgeDistance) / 100.0f;
        Vec3d farVector = perspective.lookVector().multiply(distance);
        Vec3d nearVector = farVector.multiply(minimumDistance);

        BlockPos startPos = BlockPos.ofFloored(cameraOrigin.add(nearVector));
        BlockPos endPos = BlockPos.ofFloored(cameraOrigin.add(farVector));
        return BridgingPath.calculateBresenhamVoxels(startPos, endPos, ModConfig.bridgingAdjacency, ModConfig.bridgingSnapStrength);
    }

    public static BlockPos findNonBridgingOutlineTarget(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult blockHit)) {
            return null;
        }
        if (!isHoldingPlaceable(client.player)) {
            return null;
        }

        BlockPos placeTarget = blockHit.getBlockPos().offset(blockHit.getSide());
        if (client.player.getBoundingBox().intersects(new Box(placeTarget))) {
            return null;
        }
        return placeTarget;
    }

    private static boolean canBuildOffSide(PlayerEntity player, World world, BlockPos placePos, Direction supportDirection) {
        BridgingAxisMode axisMode = player.isSneaking()
                ? ModConfig.bridgingSupportedAxesWhenCrouched.resolve(ModConfig.bridgingSupportedAxes)
                : ModConfig.bridgingSupportedAxes;
        if (!axisMode.supports(supportDirection)) {
            return false;
        }

        BlockPos supportPos = placePos.offset(supportDirection);
        BlockState supportState = world.getBlockState(supportPos);
        FluidState fluidState = world.getFluidState(supportPos);
        if (supportState.isAir() || !fluidState.isEmpty()) {
            return false;
        }
        return !supportState.isReplaceable();
    }

    private static boolean isPlacementAllowed(World world, BlockPos placePos) {
        BlockState targetState = world.getBlockState(placePos);
        return BridgingTweaksModule.INSTANCE.allowReplaceableBlocks()
                ? targetState.isReplaceable()
                : targetState.isAir();
    }

    private static List<Direction> getValidAssistSides(Vec3d lookVector) {
        List<Direction> directions = new LinkedList<>();
        for (Direction direction : Direction.values()) {
            Vec3d normal = Vec3d.of(direction.getVector());
            if (lookVector.dotProduct(normal) >= DIRECTION_SIMILARITY_THRESHOLD) {
                directions.add(direction.getOpposite());
            }
        }
        return directions;
    }

    private static Hand getPlacementHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof BlockItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof BlockItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private static boolean isHoldingPlaceable(PlayerEntity player) {
        return isStackPlaceable(player.getMainHandStack()) || isStackPlaceable(player.getOffHandStack());
    }

    private static boolean isStackPlaceable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return !(BridgingTweaksModule.INSTANCE.skipTorchBridging() && blockItem.getBlock() instanceof AbstractTorchBlock);
    }

    private static BlockHitResult createPlacementHit(ItemStack heldStack, World world, BridgingTarget target) {
        if (BridgingTweaksModule.INSTANCE.slabAssist() && heldStack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SlabBlock || blockItem.getBlock() instanceof TrapdoorBlock) {
                Direction clickSide = target.clickSide();
                if (clickSide.getAxis().isHorizontal()) {
                    BlockHitResult slabHit = createHorizontalHalfPlacement(target.placePos());
                    if (slabHit != null) {
                        return slabHit;
                    }
                }
                if (clickSide.getAxis().isVertical()) {
                    BlockHitResult slabHit = createVerticalHalfPlacement(heldStack, world, target);
                    if (slabHit != null) {
                        return slabHit;
                    }
                }
            }
        }

        return new BlockHitResult(Vec3d.ofCenter(target.placePos()), target.clickSide(), target.placePos(), true);
    }

    private static BlockHitResult createHorizontalHalfPlacement(BlockPos placePos) {
        boolean lowerHalf = BridgingTweaksState.lastKnownYFraction > TRAPDOOR_HEIGHT - BridgingPath.NEAR_ZERO
                && BridgingTweaksState.lastKnownYFraction < SLAB_HEIGHT + BridgingPath.NEAR_ZERO;

        Vec3d hitPos = lowerHalf
                ? Vec3d.ofBottomCenter(placePos).add(0.0D, 0.1D, 0.0D)
                : Vec3d.ofBottomCenter(placePos).add(0.0D, 0.9D, 0.0D);
        Direction side = lowerHalf ? Direction.UP : Direction.DOWN;
        return new BlockHitResult(hitPos, side, placePos, false);
    }

    private static BlockHitResult createVerticalHalfPlacement(ItemStack heldStack, World world, BridgingTarget target) {
        if (!(heldStack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof SlabBlock)) {
            return null;
        }

        BlockPos supportPos = target.supportPos();
        BlockState supportState = world.getBlockState(supportPos);
        if (!(supportState.getBlock() instanceof SlabBlock)) {
            return null;
        }

        SlabType slabType = supportState.get(SlabBlock.TYPE);
        Direction clickSide = target.clickSide();
        if (slabType == SlabType.DOUBLE) {
            return null;
        }
        if (slabType == SlabType.TOP && clickSide != Direction.DOWN) {
            return null;
        }
        if (slabType == SlabType.BOTTOM && clickSide != Direction.UP) {
            return null;
        }

        return new BlockHitResult(Vec3d.ofCenter(target.placePos()), clickSide, supportPos, false);
    }
}
