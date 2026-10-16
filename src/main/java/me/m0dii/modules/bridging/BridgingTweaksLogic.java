package me.m0dii.modules.bridging;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedList;
import java.util.List;

public final class BridgingTweaksLogic {
    private static final double MAXIMUM_PLACE_REACH = 4.5D;
    private static final double TRAPDOOR_HEIGHT = 3.0D / 16.0D;
    private static final double SLAB_HEIGHT = 8.0D / 16.0D;
    private static final double DIRECTION_SIMILARITY_THRESHOLD = 0.1D;

    private BridgingTweaksLogic() {
    }

    public static boolean tryBridgePlacement(Minecraft client, MultiPlayerGameMode interactionManager) {
        if (!BridgingTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }
        if (client == null || client.player == null || client.level == null || interactionManager == null) {
            return false;
        }
        if (client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
            return false;
        }
        if (BridgingTweaksModule.INSTANCE.onlyBridgeWhenCrouched() && !client.player.isShiftKeyDown()) {
            return false;
        }
        if (interactionManager.isDestroying()) {
            return false;
        }

        BridgingTarget target = BridgingTweaksState.getLastAssistTarget();
        if (target == null) {
            target = findAssistTarget(client, client.player);
        }
        if (target == null) {
            return false;
        }

        InteractionHand hand = getPlacementHand(client.player);
        if (hand == null) {
            return false;
        }

        ItemStack stack = client.player.getItemInHand(hand);
        BlockHitResult hitResult = createPlacementHit(stack, client.level, target);
        InteractionResult result = interactionManager.useItemOn(client.player, hand, hitResult);
        if (!result.consumesAction()) {
            return false;
        }

        client.player.swing(hand, client.player.getItemInHand(hand).getInteractAnimation(), false);
        return true;
    }

    public static BridgingTarget findAssistTarget(Minecraft client, Player player) {
        if (client == null || player == null || client.level == null) {
            return null;
        }
        if (client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
            return null;
        }
        if (!isHoldingPlaceable(player)) {
            return null;
        }

        BridgingPerspective perspective = BridgingPerspective.resolve(client, player);
        List<BlockPos> path = getViewBlockPath(client, player, perspective);
        List<Direction> sides = getValidAssistSides(perspective.lookVector());

        for (BlockPos placePos : path) {
            if (!isPlacementAllowed(client.level, placePos)) {
                continue;
            }

            if (player.getBoundingBox().intersects(new AABB(placePos))) {
                continue;
            }

            for (Direction side : sides) {
                if (canBuildOffSide(player, client.level, placePos, side)) {
                    return new BridgingTarget(placePos, side);
                }
            }
        }

        return null;
    }

    public static List<BlockPos> getViewBlockPath(Minecraft client, Entity player, BridgingPerspective perspective) {
        if (client == null || player == null) {
            return List.of();
        }

        double reach = client.player == null ? MAXIMUM_PLACE_REACH : client.player.blockInteractionRange();
        Vec3 playerViewEnd = player.getViewVector(1.0f).scale(reach).add(player.getEyePosition());
        Vec3 cameraOrigin = perspective.position();
        double distance = playerViewEnd.distanceTo(cameraOrigin);

        float minimumDistance = BridgingTweaksModule.wrapPercent(ModConfig.bridgingMinBridgeDistance) / 100.0f;
        Vec3 farVector = perspective.lookVector().scale(distance);
        Vec3 nearVector = farVector.scale(minimumDistance);

        BlockPos startPos = BlockPos.containing(cameraOrigin.add(nearVector));
        BlockPos endPos = BlockPos.containing(cameraOrigin.add(farVector));
        return BridgingPath.calculateBresenhamVoxels(startPos, endPos, ModConfig.bridgingAdjacency, ModConfig.bridgingSnapStrength);
    }

    public static BlockPos findNonBridgingOutlineTarget(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return null;
        }
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            return null;
        }
        if (!isHoldingPlaceable(client.player)) {
            return null;
        }

        BlockPos placeTarget = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (client.player.getBoundingBox().intersects(new AABB(placeTarget))) {
            return null;
        }
        return placeTarget;
    }

    private static boolean canBuildOffSide(Player player, Level world, BlockPos placePos, Direction supportDirection) {
        BridgingAxisMode axisMode = player.isShiftKeyDown()
                ? ModConfig.bridgingSupportedAxesWhenCrouched.resolve(ModConfig.bridgingSupportedAxes)
                : ModConfig.bridgingSupportedAxes;
        if (!axisMode.supports(supportDirection)) {
            return false;
        }

        BlockPos supportPos = placePos.relative(supportDirection);
        BlockState supportState = world.getBlockState(supportPos);
        FluidState fluidState = world.getFluidState(supportPos);
        if (supportState.isAir() || !fluidState.isEmpty()) {
            return false;
        }
        return !supportState.canBeReplaced();
    }

    private static boolean isPlacementAllowed(Level world, BlockPos placePos) {
        BlockState targetState = world.getBlockState(placePos);
        return BridgingTweaksModule.INSTANCE.allowReplaceableBlocks()
                ? targetState.canBeReplaced()
                : targetState.isAir();
    }

    private static List<Direction> getValidAssistSides(Vec3 lookVector) {
        List<Direction> directions = new LinkedList<>();
        for (Direction direction : Direction.values()) {
            Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
            if (lookVector.dot(normal) >= DIRECTION_SIMILARITY_THRESHOLD) {
                directions.add(direction.getOpposite());
            }
        }
        return directions;
    }

    private static InteractionHand getPlacementHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof BlockItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof BlockItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isHoldingPlaceable(Player player) {
        return isStackPlaceable(player.getMainHandItem()) || isStackPlaceable(player.getOffhandItem());
    }

    private static boolean isStackPlaceable(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return !(BridgingTweaksModule.INSTANCE.skipTorchBridging() && blockItem.getBlock() instanceof BaseTorchBlock);
    }

    private static BlockHitResult createPlacementHit(ItemStack heldStack, Level world, BridgingTarget target) {
        if (BridgingTweaksModule.INSTANCE.slabAssist() && heldStack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof SlabBlock || blockItem.getBlock() instanceof TrapDoorBlock) {
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

        return new BlockHitResult(Vec3.atCenterOf(target.placePos()), target.clickSide(), target.placePos(), true);
    }

    private static BlockHitResult createHorizontalHalfPlacement(BlockPos placePos) {
        boolean lowerHalf = BridgingTweaksState.lastKnownYFraction > TRAPDOOR_HEIGHT - BridgingPath.NEAR_ZERO
                && BridgingTweaksState.lastKnownYFraction < SLAB_HEIGHT + BridgingPath.NEAR_ZERO;

        Vec3 hitPos = lowerHalf
                ? Vec3.atBottomCenterOf(placePos).add(0.0D, 0.1D, 0.0D)
                : Vec3.atBottomCenterOf(placePos).add(0.0D, 0.9D, 0.0D);
        Direction side = lowerHalf ? Direction.UP : Direction.DOWN;
        return new BlockHitResult(hitPos, side, placePos, false);
    }

    private static BlockHitResult createVerticalHalfPlacement(ItemStack heldStack, Level world, BridgingTarget target) {
        if (!(heldStack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof SlabBlock)) {
            return null;
        }

        BlockPos supportPos = target.supportPos();
        BlockState supportState = world.getBlockState(supportPos);
        if (!(supportState.getBlock() instanceof SlabBlock)) {
            return null;
        }

        SlabType slabType = supportState.getValue(SlabBlock.TYPE);
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

        return new BlockHitResult(Vec3.atCenterOf(target.placePos()), clickSide, supportPos, false);
    }
}
