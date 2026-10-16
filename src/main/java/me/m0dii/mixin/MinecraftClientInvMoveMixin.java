package me.m0dii.mixin;

import me.m0dii.modules.bridging.BridgingTweaksLogic;
import me.m0dii.modules.bridging.BridgingTweaksModule;
import me.m0dii.modules.bridging.BridgingTweaksState;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.mojang.blaze3d.platform.InputConstants.*;

@Mixin(Minecraft.class)
public class MinecraftClientInvMoveMixin {

    @Shadow
    private int rightClickDelay;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public MultiPlayerGameMode gameMode;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (FastBlockPlacementModule.INSTANCE.isEnabled()) {
            this.rightClickDelay = 0;
        }

        BridgingTweaksState.tick(client);

        if (!InventoryMoveModule.INSTANCE.isEnabled()) {
            return;
        }

        Screen screen = client.gui.screen();

        if (screen instanceof AbstractContainerScreen) {
            client.options.keyUp.setDown(isKeyDown(KEY_W));
            client.options.keyLeft.setDown(isKeyDown(KEY_A));
            client.options.keyDown.setDown(isKeyDown(KEY_S));
            client.options.keyRight.setDown(isKeyDown(KEY_D));
            client.options.keyJump.setDown(isKeyDown(KEY_SPACE));
            client.options.keySprint.setDown(isKeyDown(KEY_LCONTROL));
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void applyPlacementTweaks(CallbackInfo ci) {
        if (BridgingTweaksLogic.tryBridgePlacement(Minecraft.getInstance(), this.gameMode)) {
            this.rightClickDelay = BridgingTweaksModule.INSTANCE.delayPostBridging();
            ci.cancel();
            return;
        }

        if (!TweaksModule.INSTANCE.angelBlock()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || this.gameMode == null) {
            return;
        }

        if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
            return;
        }

        InteractionHand hand = getBlockPlacingHand(client);
        if (hand == null) {
            return;
        }

        // Looking down at a ledge: attach to a neighbouring block; otherwise place in mid-air ahead.
        BlockHitResult hitResult = client.player.getXRot() >= 35.0f ? findAngelPlacementHit(client) : null;
        if (hitResult == null) {
            hitResult = findMidAirPlacementHit(client);
        }
        if (hitResult == null) {
            return;
        }

        InteractionResult result = this.gameMode.useItemOn(client.player, hand, hitResult);
        // Same cooldown vanilla applies; without it the next tick places again against the new block.
        this.rightClickDelay = 4;
        if (result.consumesAction()) {
            client.player.swing(hand, client.player.getItemInHand(hand).getInteractAnimation(), false);
            ci.cancel();
        }
    }

    @Unique
    private static BlockHitResult findMidAirPlacementHit(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        BlockPos pos = BlockPos.containing(eye.add(client.player.getViewVector(1.0f).scale(2.5)));
        BlockState state = client.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return null;
        }
        // Clicking a replaceable (air) block places the item at that position.
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @Unique
    private static InteractionHand getBlockPlacingHand(Minecraft client) {
        ItemStack mainHand = client.player.getMainHandItem();
        if (mainHand.getItem() instanceof BlockItem) {
            return InteractionHand.MAIN_HAND;
        }

        ItemStack offHand = client.player.getOffhandItem();
        if (offHand.getItem() instanceof BlockItem) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    @Unique
    private static BlockHitResult findAngelPlacementHit(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }

        Vec3 look = client.player.getViewVector(1.0f);
        Set<BlockPos> candidates = new LinkedHashSet<>();

        BlockPos center = BlockPos.containing(client.player.getX(), client.player.getY() - 1.0, client.player.getZ());
        BlockPos forward = BlockPos.containing(client.player.getX() + look.x, client.player.getY() - 1.0, client.player.getZ() + look.z);
        BlockPos forwardFar = BlockPos.containing(client.player.getX() + (look.x * 2.0), client.player.getY() - 1.0, client.player.getZ() + (look.z * 2.0));

        candidates.add(forward);
        candidates.add(forwardFar);
        candidates.add(center);

        addNearbyCandidates(candidates, center);
        addNearbyCandidates(candidates, forward);

        for (BlockPos placePos : candidates) {
            BlockState placeState = client.level.getBlockState(placePos);
            if (!placeState.isAir() && !placeState.canBeReplaced()) {
                continue;
            }

            BlockHitResult hit = findPlacementHit(client, placePos);
            if (hit != null) {
                return hit;
            }
        }

        return null;
    }

    @Unique
    private static void addNearbyCandidates(Set<BlockPos> candidates, BlockPos origin) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                candidates.add(origin.offset(dx, 0, dz));
            }
        }
    }

    @Unique
    private static BlockHitResult findPlacementHit(Minecraft client, BlockPos placePos) {
        Direction[] priority = new Direction[]{
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };

        for (Direction direction : priority) {
            BlockPos supportPos = placePos.relative(direction);
            BlockState supportState = client.level.getBlockState(supportPos);
            if (supportState.isAir()) {
                continue;
            }

            Direction side = direction.getOpposite();
            Vec3 hitPos = Vec3.atCenterOf(supportPos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5));
            return new BlockHitResult(hitPos, side, supportPos, false);
        }

        return null;
    }
}
