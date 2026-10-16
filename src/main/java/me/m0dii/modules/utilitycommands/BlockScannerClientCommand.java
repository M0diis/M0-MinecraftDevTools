package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

public final class BlockScannerClientCommand {
    private static final int MAX_RADIUS = 96;
    private static final int MAX_LINES = 12;

    private BlockScannerClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerLiteral(dispatcher, "scanblocks");
            registerLiteral(dispatcher, "scanblock");
        });
    }

    private static void registerLiteral(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher, String literal) {
        dispatcher.register(ClientCommands.literal(literal)
                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                        .executes(context -> scan(context, null))
                        .then(ClientCommands.argument("block", StringArgumentType.greedyString())
                                .executes(context -> scan(context, StringArgumentType.getString(context, "block"))))));
    }

    private static int scan(CommandContext<FabricClientCommandSource> context, String blockFilterRaw) {
        Minecraft client = context.getSource().getClient();
        if (client.player == null || client.level == null) {
            return 0;
        }

        int radius = IntegerArgumentType.getInteger(context, "radius");
        Block filter = null;
        if (blockFilterRaw != null && !blockFilterRaw.isBlank()) {
            filter = resolveBlock(blockFilterRaw);
            if (filter == null) {
                context.getSource().sendError(Component.literal("[ScanBlocks] Unknown block '" + blockFilterRaw + "'"));
                return 0;
            }
        }

        Map<Block, Integer> counts = new HashMap<>();
        int totalCounted = 0;
        int skippedUnloaded = 0;

        BlockPos center = client.player.blockPosition();
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((dx * dx) + (dy * dy) + (dz * dz) > radiusSq) {
                        continue;
                    }

                    int x = center.getX() + dx;
                    int y = center.getY() + dy;
                    int z = center.getZ() + dz;
                    if (!isChunkLoaded(client, x >> 4, z >> 4)) {
                        skippedUnloaded++;
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (filter != null && state.getBlock() != filter) {
                        continue;
                    }

                    counts.merge(state.getBlock(), 1, Integer::sum);
                    totalCounted++;
                }
            }
        }

        if (filter != null) {
            int count = counts.getOrDefault(filter, 0);
            String blockName = filter.getName().getString();
            double percent = totalCounted <= 0 ? 0.0 : (count * 100.0) / totalCounted;
            context.getSource().sendFeedback(Component.literal("[ScanBlocks] " + blockName + " x " + count
                    + " (" + formatPercent(percent) + "% of matched non-air blocks)"));
            if (skippedUnloaded > 0) {
                context.getSource().sendFeedback(Component.literal("[ScanBlocks] Skipped " + skippedUnloaded + " unloaded positions."));
            }
            return 1;
        }

        if (counts.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("[ScanBlocks] No non-air blocks found in radius " + radius + "."));
            return 1;
        }

        List<Map.Entry<Block, Integer>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<Block, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().getName().getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        context.getSource().sendFeedback(Component.literal("[ScanBlocks] Radius " + radius + " found "
                + totalCounted + " non-air blocks across " + counts.size() + " block types."));

        int lines = Math.min(MAX_LINES, sorted.size());
        for (int i = 0; i < lines; i++) {
            Map.Entry<Block, Integer> entry = sorted.get(i);
            double percent = (entry.getValue() * 100.0) / totalCounted;
            context.getSource().sendFeedback(Component.literal(" - " + entry.getKey().getName().getString()
                    + " x " + entry.getValue() + " (" + formatPercent(percent) + "%)"));
        }
        if (sorted.size() > lines) {
            context.getSource().sendFeedback(Component.literal("[ScanBlocks] " + (sorted.size() - lines) + " more block types omitted."));
        }
        if (skippedUnloaded > 0) {
            context.getSource().sendFeedback(Component.literal("[ScanBlocks] Skipped " + skippedUnloaded + " unloaded positions."));
        }
        return 1;
    }

    private static Block resolveBlock(String raw) {
        String query = normalize(raw);
        if (query.isEmpty()) {
            return null;
        }

        Identifier direct = query.contains(":") ? Identifier.tryParse(query) : Identifier.tryParse("minecraft:" + query);
        if (direct != null && BuiltInRegistries.BLOCK.containsKey(direct)) {
            return BuiltInRegistries.BLOCK.getValue(direct);
        }

        List<Block> matches = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.BLOCK.keySet()) {
            String full = id.toString().toLowerCase(Locale.ROOT);
            String path = id.getPath().toLowerCase(Locale.ROOT);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            String display = normalize(block.getName().getString());

            if (path.equals(query) || full.equals(query) || display.equals(query)) {
                return block;
            }
            if (path.contains(query) || display.contains(query)) {
                matches.add(block);
            }
        }

        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private static boolean isChunkLoaded(Minecraft client, int chunkX, int chunkZ) {
        return client.level != null && client.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
