package me.m0dii.mixin;

import me.m0dii.modules.bridging.BridgingTweaksLogic;
import me.m0dii.modules.bridging.BridgingTweaksModule;
import me.m0dii.modules.bridging.BridgingTweaksState;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

import static net.minecraft.client.util.InputUtil.*;

@Mixin(MinecraftClient.class)
public class MinecraftClientInvMoveMixin {

    @Shadow
    private int itemUseCooldown;

    @Shadow
    public HitResult crosshairTarget;

    @Shadow
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (FastBlockPlacementModule.INSTANCE.isEnabled()) {
            this.itemUseCooldown = 0;
        }

        BridgingTweaksState.tick(client);

        if (!InventoryMoveModule.INSTANCE.isEnabled()) {
            return;
        }

        Screen screen = client.currentScreen;

        if (screen instanceof HandledScreen) {
            client.options.forwardKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_W));
            client.options.leftKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_A));
            client.options.backKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_S));
            client.options.rightKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_D));
            client.options.jumpKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_SPACE));
            client.options.sprintKey.setPressed(isKeyPressed(client.getWindow(), GLFW_KEY_LEFT_CONTROL));
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void applyPlacementTweaks(CallbackInfo ci) {
        if (BridgingTweaksLogic.tryBridgePlacement(MinecraftClient.getInstance(), this.interactionManager)) {
            this.itemUseCooldown = BridgingTweaksModule.INSTANCE.delayPostBridging();
            ci.cancel();
            return;
        }

        if (!TweaksModule.INSTANCE.angelBlock()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || this.interactionManager == null) {
            return;
        }

        if (this.crosshairTarget != null && this.crosshairTarget.getType() != HitResult.Type.MISS) {
            return;
        }

        if (client.player.getPitch() < 35.0f) {
            return;
        }

        Hand hand = getBlockPlacingHand(client);
        if (hand == null) {
            return;
        }

        BlockHitResult hitResult = findAngelPlacementHit(client);
        if (hitResult == null) {
            return;
        }

        ActionResult result = this.interactionManager.interactBlock(client.player, hand, hitResult);
        if (result.isAccepted()) {
            client.player.swingHand(hand);
            ci.cancel();
        }
    }

    @Unique
    private static Hand getBlockPlacingHand(MinecraftClient client) {
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() instanceof BlockItem) {
            return Hand.MAIN_HAND;
        }

        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() instanceof BlockItem) {
            return Hand.OFF_HAND;
        }

        return null;
    }

    @Unique
    private static BlockHitResult findAngelPlacementHit(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }

        Vec3d look = client.player.getRotationVec(1.0f);
        Set<BlockPos> candidates = new LinkedHashSet<>();

        BlockPos center = BlockPos.ofFloored(client.player.getX(), client.player.getY() - 1.0, client.player.getZ());
        BlockPos forward = BlockPos.ofFloored(client.player.getX() + look.x, client.player.getY() - 1.0, client.player.getZ() + look.z);
        BlockPos forwardFar = BlockPos.ofFloored(client.player.getX() + (look.x * 2.0), client.player.getY() - 1.0, client.player.getZ() + (look.z * 2.0));

        candidates.add(forward);
        candidates.add(forwardFar);
        candidates.add(center);

        addNearbyCandidates(candidates, center);
        addNearbyCandidates(candidates, forward);

        for (BlockPos placePos : candidates) {
            BlockState placeState = client.world.getBlockState(placePos);
            if (!placeState.isAir() && !placeState.isReplaceable()) {
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
                candidates.add(origin.add(dx, 0, dz));
            }
        }
    }

    @Unique
    private static BlockHitResult findPlacementHit(MinecraftClient client, BlockPos placePos) {
        Direction[] priority = new Direction[]{
                Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };

        for (Direction direction : priority) {
            BlockPos supportPos = placePos.offset(direction);
            BlockState supportState = client.world.getBlockState(supportPos);
            if (supportState.isAir()) {
                continue;
            }

            Direction side = direction.getOpposite();
            Vec3d hitPos = Vec3d.ofCenter(supportPos).add(Vec3d.of(side.getVector()).multiply(0.5));
            return new BlockHitResult(hitPos, side, supportPos, false);
        }

        return null;
    }
}
