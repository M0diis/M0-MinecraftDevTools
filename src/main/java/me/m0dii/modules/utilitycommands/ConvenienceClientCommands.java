package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ConvenienceClientCommands {
    private static final int MAX_UP_BLOCKS = 512;
    private static final double JUMPTO_DISTANCE = 512.0;
    private static final double THRU_DISTANCE = 128.0;
    private static final double STEP_DISTANCE = 0.5;
    private static final int SAFE_SEARCH_VERTICAL = 4;
    private static final String DEFAULT_UP_BLOCK = "minecraft:glass";

    private ConvenienceClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("top")
                .executes(context -> teleportTop(context.getSource())));
        dispatcher.register(ClientCommands.literal("asc")
                .executes(context -> teleportAsc(context.getSource())));
        dispatcher.register(ClientCommands.literal("desc")
                .executes(context -> teleportDesc(context.getSource())));
        dispatcher.register(ClientCommands.literal("thru")
                .executes(context -> teleportThru(context.getSource())));
        dispatcher.register(ClientCommands.literal("jumpto")
                .executes(context -> teleportJumpTo(context.getSource())));
        dispatcher.register(
                ClientCommands.literal("up")
                        .executes(context -> teleportUp(context.getSource(), 1, null))
                        .then(ClientCommands.argument("blocks", IntegerArgumentType.integer(1, MAX_UP_BLOCKS))
                                .executes(context -> teleportUp(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "blocks"),
                                        null
                                ))
                                .then(ClientCommands.literal("-b")
                                        .executes(context -> teleportUp(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "blocks"),
                                                DEFAULT_UP_BLOCK
                                        ))
                                        .then(ClientCommands.argument("block", StringArgumentType.word())
                                                .suggests(ConvenienceClientCommands::suggestBlockArgument)
                                                .executes(context -> teleportUp(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "blocks"),
                                                        StringArgumentType.getString(context, "block")
                                                )))))
        );
    }

    private static int teleportTop(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        LocalPlayer player = client.player;
        int x = player.getBlockX();
        int z = player.getBlockZ();
        int topY = client.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (topY <= player.getBlockY()) {
            source.sendError(Component.literal("No higher block found above you."));
            return 0;
        }

        for (int y = topY; y > player.getBlockY(); y--) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockPos feet = floor.above();
            if (isSafeFeetPosition(player, feet)) {
                teleportTo(player, toFeetPosition(feet));
                source.sendFeedback(Component.literal("Teleported to the highest safe block above you."));
                return 1;
            }
        }

        source.sendError(Component.literal("Couldn't find a safe position above you."));
        return 0;
    }

    private static int teleportAsc(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        BlockPos feet = findNearestSafeFeetAbove(client.player);
        if (feet == null) {
            source.sendError(Component.literal("No empty space found above you."));
            return 0;
        }

        teleportTo(client.player, toFeetPosition(feet));
        source.sendFeedback(Component.literal("Ascended to the nearest safe space above."));
        return 1;
    }

    private static int teleportDesc(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        BlockPos feet = findNearestSafeFeetBelow(client.player);
        if (feet == null) {
            source.sendError(Component.literal("No empty space found below you."));
            return 0;
        }

        teleportTo(client.player, toFeetPosition(feet));
        source.sendFeedback(Component.literal("Descended to the nearest safe space below."));
        return 1;
    }

    private static int teleportThru(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        LocalPlayer player = client.player;
        Vec3 eyePos = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0f).normalize();
        boolean crossedSolid = false;

        for (double distance = STEP_DISTANCE; distance <= THRU_DISTANCE; distance += STEP_DISTANCE) {
            Vec3 sample = eyePos.add(direction.scale(distance));
            BlockPos sampleBlock = BlockPos.containing(sample);
            boolean solid = !client.level.getBlockState(sampleBlock).getCollisionShape(client.level, sampleBlock).isEmpty();
            if (solid) {
                crossedSolid = true;
                continue;
            }
            if (!crossedSolid) {
                continue;
            }

            double feetY = sample.y - player.getEyeHeight(player.getPose());
            BlockPos nearFeet = BlockPos.containing(sample.x, feetY, sample.z);
            BlockPos safeFeet = findSafeFeetNearby(player, nearFeet);
            if (safeFeet != null) {
                teleportTo(player, toFeetPosition(safeFeet));
                source.sendFeedback(Component.literal("Teleported through the obstacle."));
                return 1;
            }
        }

        source.sendError(Component.literal(crossedSolid
                ? "Couldn't find safe space beyond the obstacle."
                : "No blocking obstacle found in front of you."));
        return 0;
    }

    private static int teleportUp(FabricClientCommandSource source, int blocks, String supportBlockRaw) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        LocalPlayer player = client.player;
        double centeredX = Math.floor(player.getX()) + 0.5;
        double centeredZ = Math.floor(player.getZ()) + 0.5;
        double targetY = Math.floor(player.getY()) + blocks;

        String supportBlockId = resolveBlockId(supportBlockRaw);
        if (supportBlockRaw != null && supportBlockId == null) {
            source.sendError(Component.literal("Unknown block '" + supportBlockRaw + "'."));
            return 0;
        }

        if (supportBlockId != null && client.getConnection() != null) {
            BlockPos supportPos = BlockPos.containing(centeredX, targetY - 1.0, centeredZ);
            client.getConnection().sendCommand("setblock "
                    + supportPos.getX() + " "
                    + supportPos.getY() + " "
                    + supportPos.getZ() + " "
                    + supportBlockId);
        }

        teleportTo(player, new Vec3(centeredX, targetY, centeredZ));
        source.sendFeedback(Component.literal(supportBlockId == null
                ? "Teleported up " + blocks + " block" + (blocks == 1 ? "" : "s") + "."
                : "Teleported up " + blocks + " block" + (blocks == 1 ? "" : "s") + " and requested " + supportBlockId + " below you."));
        return 1;
    }

    private static int teleportJumpTo(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (!ensureReady(client, source)) {
            return 0;
        }

        LocalPlayer player = client.player;
        BlockHitResult hit = raycastBlock(player, JUMPTO_DISTANCE);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            source.sendError(Component.literal("No block found in sight."));
            return 0;
        }

        for (BlockPos candidate : jumpCandidates(hit)) {
            if (isSafeFeetPosition(player, candidate)) {
                teleportTo(player, toFeetPosition(candidate));
                source.sendFeedback(Component.literal("Teleported to the targeted block."));
                return 1;
            }
        }

        source.sendError(Component.literal("Couldn't find safe space near the targeted block."));
        return 0;
    }

    private static BlockHitResult raycastBlock(LocalPlayer player, double distance) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0f).scale(distance));
        return player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    private static List<BlockPos> jumpCandidates(BlockHitResult hit) {
        BlockPos target = hit.getBlockPos();
        Direction side = hit.getDirection();
        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();
        candidates.add(target.relative(side));
        candidates.add(target.above());
        candidates.add(target.relative(side).above());
        if (side != Direction.UP) {
            candidates.add(target.relative(side).above(2));
        }
        for (int i = 2; i <= 6; i++) {
            candidates.add(target.above(i));
        }
        return new ArrayList<>(candidates);
    }

    private static BlockPos findSafeFeetNearby(LocalPlayer player, BlockPos around) {
        for (int dy = 0; dy <= SAFE_SEARCH_VERTICAL; dy++) {
            BlockPos up = around.above(dy);
            if (isSafeFeetPosition(player, up)) {
                return up;
            }
            if (dy == 0) {
                continue;
            }
            BlockPos down = around.below(dy);
            if (isSafeFeetPosition(player, down)) {
                return down;
            }
        }
        return null;
    }

    private static BlockPos findNearestSafeFeetAbove(LocalPlayer player) {
        for (int y = player.getBlockY() + 1; y <= player.level().getMaxY(); y++) {
            BlockPos feet = new BlockPos(player.getBlockX(), y, player.getBlockZ());
            if (isSafeFeetPosition(player, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static BlockPos findNearestSafeFeetBelow(LocalPlayer player) {
        for (int y = player.getBlockY() - 1; y > player.level().getMinY(); y--) {
            BlockPos feet = new BlockPos(player.getBlockX(), y, player.getBlockZ());
            if (isSafeFeetPosition(player, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isSafeFeetPosition(LocalPlayer player, BlockPos feetPos) {
        if (player == null || player.level() == null) {
            return false;
        }

        BlockPos floor = feetPos.below();
        if (player.level().getBlockState(floor).getCollisionShape(player.level(), floor).isEmpty()) {
            return false;
        }

        Vec3 destination = toFeetPosition(feetPos);
        AABB box = player.getBoundingBox().move(
                destination.x - player.getX(),
                destination.y - player.getY(),
                destination.z - player.getZ()
        );
        return player.level().noCollision(player, box);
    }

    private static Vec3 toFeetPosition(BlockPos feetPos) {
        return new Vec3(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
    }

    private static void teleportTo(LocalPlayer player, Vec3 position) {
        player.setPos(position.x, position.y, position.z);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean ensureReady(Minecraft client, FabricClientCommandSource source) {
        if (client.player == null || client.level == null) {
            source.sendError(Component.literal("You must be in a world to use this command."));
            return false;
        }
        return true;
    }

    private static CompletableFuture<Suggestions> suggestBlockArgument(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            String full = id.toString();
            String path = id.getPath();
            if (prefix.contains(":")) {
                if (full.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    builder.suggest(full);
                }
            } else if (path.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(path);
            } else if (full.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(full);
            }
        }
        return builder.buildFuture();
    }

    private static String resolveBlockId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String query = raw.trim().toLowerCase(Locale.ROOT);
        Identifier direct = query.contains(":") ? Identifier.tryParse(query) : Identifier.tryParse("minecraft:" + query);
        if (direct != null && BuiltInRegistries.BLOCK.containsKey(direct)) {
            return direct.toString();
        }

        Block match = null;
        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            String full = id.toString().toLowerCase(Locale.ROOT);
            String path = id.getPath().toLowerCase(Locale.ROOT);
            if (path.equals(query) || full.equals(query)) {
                return id.toString();
            }
            if (path.startsWith(query) || full.startsWith(query)) {
                if (match != null) {
                    return null;
                }
                match = BuiltInRegistries.BLOCK.getValue(id);
            }
        }
        if (match == null) {
            return null;
        }
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(match));
    }
}
