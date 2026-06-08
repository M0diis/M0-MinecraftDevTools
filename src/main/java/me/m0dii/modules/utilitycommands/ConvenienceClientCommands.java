package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ConvenienceClientCommands {
    private static final int MAX_UP_BLOCKS = 512;
    private static final double JUMPTO_DISTANCE = 512.0;
    private static final double THRU_DISTANCE = 128.0;
    private static final double STEP_DISTANCE = 0.5;
    private static final int SAFE_SEARCH_VERTICAL = 4;

    private ConvenienceClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("top")
                .executes(context -> teleportTop(context.getSource())));
        dispatcher.register(ClientCommandManager.literal("asc")
                .executes(context -> teleportAsc(context.getSource())));
        dispatcher.register(ClientCommandManager.literal("desc")
                .executes(context -> teleportDesc(context.getSource())));
        dispatcher.register(ClientCommandManager.literal("thru")
                .executes(context -> teleportThru(context.getSource())));
        dispatcher.register(ClientCommandManager.literal("jumpto")
                .executes(context -> teleportJumpTo(context.getSource())));
        dispatcher.register(ClientCommandManager.literal("up")
                .executes(context -> teleportUp(context.getSource(), 1))
                .then(ClientCommandManager.argument("blocks", IntegerArgumentType.integer(1, MAX_UP_BLOCKS))
                        .executes(context -> teleportUp(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "blocks")
                        ))));
    }

    private static int teleportTop(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        ClientPlayerEntity player = client.player;
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int topY = client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
        if (topY <= player.getBlockY()) {
            source.sendError(Text.literal("No higher block found above you."));
            return 0;
        }

        for (int y = topY; y > player.getBlockY(); y--) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockPos feet = floor.up();
            if (isSafeFeetPosition(player, feet)) {
                teleportTo(player, toFeetPosition(feet));
                source.sendFeedback(Text.literal("Teleported to the highest safe block above you."));
                return 1;
            }
        }

        source.sendError(Text.literal("Couldn't find a safe position above you."));
        return 0;
    }

    private static int teleportAsc(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        BlockPos feet = findNearestSafeFeetAbove(client.player);
        if (feet == null) {
            source.sendError(Text.literal("No empty space found above you."));
            return 0;
        }

        teleportTo(client.player, toFeetPosition(feet));
        source.sendFeedback(Text.literal("Ascended to the nearest safe space above."));
        return 1;
    }

    private static int teleportDesc(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        BlockPos feet = findNearestSafeFeetBelow(client.player);
        if (feet == null) {
            source.sendError(Text.literal("No empty space found below you."));
            return 0;
        }

        teleportTo(client.player, toFeetPosition(feet));
        source.sendFeedback(Text.literal("Descended to the nearest safe space below."));
        return 1;
    }

    private static int teleportThru(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        ClientPlayerEntity player = client.player;
        Vec3d eyePos = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f).normalize();
        boolean crossedSolid = false;

        for (double distance = STEP_DISTANCE; distance <= THRU_DISTANCE; distance += STEP_DISTANCE) {
            Vec3d sample = eyePos.add(direction.multiply(distance));
            BlockPos sampleBlock = BlockPos.ofFloored(sample);
            boolean solid = !client.world.getBlockState(sampleBlock).getCollisionShape(client.world, sampleBlock).isEmpty();
            if (solid) {
                crossedSolid = true;
                continue;
            }
            if (!crossedSolid) {
                continue;
            }

            double feetY = sample.y - player.getEyeHeight(player.getPose());
            BlockPos nearFeet = BlockPos.ofFloored(sample.x, feetY, sample.z);
            BlockPos safeFeet = findSafeFeetNearby(player, nearFeet);
            if (safeFeet != null) {
                teleportTo(player, toFeetPosition(safeFeet));
                source.sendFeedback(Text.literal("Teleported through the obstacle."));
                return 1;
            }
        }

        source.sendError(Text.literal(crossedSolid
                ? "Couldn't find safe space beyond the obstacle."
                : "No blocking obstacle found in front of you."));
        return 0;
    }

    private static int teleportUp(FabricClientCommandSource source, int blocks) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        ClientPlayerEntity player = client.player;
        teleportTo(player, new Vec3d(player.getX(), player.getY() + blocks, player.getZ()));
        source.sendFeedback(Text.literal("Teleported up " + blocks + " block" + (blocks == 1 ? "" : "s") + "."));
        return 1;
    }

    private static int teleportJumpTo(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        ClientPlayerEntity player = client.player;
        BlockHitResult hit = raycastBlock(player, JUMPTO_DISTANCE);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            source.sendError(Text.literal("No block found in sight."));
            return 0;
        }

        for (BlockPos candidate : jumpCandidates(hit)) {
            if (isSafeFeetPosition(player, candidate)) {
                teleportTo(player, toFeetPosition(candidate));
                source.sendFeedback(Text.literal("Teleported to the targeted block."));
                return 1;
            }
        }

        source.sendError(Text.literal("Couldn't find safe space near the targeted block."));
        return 0;
    }

    private static BlockHitResult raycastBlock(ClientPlayerEntity player, double distance) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(distance));
        return player.getEntityWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));
    }

    private static List<BlockPos> jumpCandidates(BlockHitResult hit) {
        BlockPos target = hit.getBlockPos();
        Direction side = hit.getSide();
        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();
        candidates.add(target.offset(side));
        candidates.add(target.up());
        candidates.add(target.offset(side).up());
        if (side != Direction.UP) {
            candidates.add(target.offset(side).up(2));
        }
        for (int i = 2; i <= 6; i++) {
            candidates.add(target.up(i));
        }
        return new ArrayList<>(candidates);
    }

    private static BlockPos findSafeFeetNearby(ClientPlayerEntity player, BlockPos around) {
        for (int dy = 0; dy <= SAFE_SEARCH_VERTICAL; dy++) {
            BlockPos up = around.up(dy);
            if (isSafeFeetPosition(player, up)) {
                return up;
            }
            if (dy == 0) {
                continue;
            }
            BlockPos down = around.down(dy);
            if (isSafeFeetPosition(player, down)) {
                return down;
            }
        }
        return null;
    }

    private static BlockPos findNearestSafeFeetAbove(ClientPlayerEntity player) {
        for (int y = player.getBlockY() + 1; y <= player.getEntityWorld().getTopYInclusive(); y++) {
            BlockPos feet = new BlockPos(player.getBlockX(), y, player.getBlockZ());
            if (isSafeFeetPosition(player, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static BlockPos findNearestSafeFeetBelow(ClientPlayerEntity player) {
        for (int y = player.getBlockY() - 1; y > player.getEntityWorld().getBottomY(); y--) {
            BlockPos feet = new BlockPos(player.getBlockX(), y, player.getBlockZ());
            if (isSafeFeetPosition(player, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isSafeFeetPosition(ClientPlayerEntity player, BlockPos feetPos) {
        if (player == null || player.getEntityWorld() == null) {
            return false;
        }

        BlockPos floor = feetPos.down();
        if (player.getEntityWorld().getBlockState(floor).getCollisionShape(player.getEntityWorld(), floor).isEmpty()) {
            return false;
        }

        Vec3d destination = toFeetPosition(feetPos);
        Box box = player.getBoundingBox().offset(
                destination.x - player.getX(),
                destination.y - player.getY(),
                destination.z - player.getZ()
        );
        return player.getEntityWorld().isSpaceEmpty(player, box);
    }

    private static Vec3d toFeetPosition(BlockPos feetPos) {
        return new Vec3d(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
    }

    private static void teleportTo(ClientPlayerEntity player, Vec3d position) {
        player.setPosition(position.x, position.y, position.z);
        player.setVelocity(Vec3d.ZERO);
    }

    private static boolean ensureReady(MinecraftClient client, FabricClientCommandSource source) {
        if (client.player == null || client.world == null) {
            source.sendError(Text.literal("You must be in a world to use this command."));
            return false;
        }
        return true;
    }
}
