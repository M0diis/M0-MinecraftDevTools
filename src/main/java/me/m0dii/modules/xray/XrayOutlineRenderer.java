package me.m0dii.modules.xray;

import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.*;

public final class XrayOutlineRenderer {
    private static final int SCAN_CHUNK_BUDGET_PER_FRAME = 4;

    private static boolean registered;

    private static final Map<Long, List<XrayHit>> cachedHitsByChunk = new HashMap<>();
    private static final ArrayDeque<ChunkPos> pendingChunkScans = new ArrayDeque<>();
    private static final Set<Long> pendingChunkKeys = new HashSet<>();
    private static final Set<Long> desiredChunkKeys = new HashSet<>();
    private static final Map<Block, XrayManager.XrayBlockConfig> activeConfigsByBlock = new HashMap<>();

    private static ClientWorld cachedWorld;
    private static int cachedRange = Integer.MIN_VALUE;
    private static int cachedScanMinY = Integer.MIN_VALUE;
    private static int cachedScanMaxY = Integer.MIN_VALUE;

    private XrayOutlineRenderer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!XrayModule.INSTANCE.isEnabled()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                clearCache();
                return;
            }

            int searchRadius = XrayManager.getDisplayRange();
            if (searchRadius <= 0) {
                clearCache();
                return;
            }

            boolean trackedBlocksChanged = refreshActiveConfigsByBlock();
            if (activeConfigsByBlock.isEmpty()) {
                clearCache();
                return;
            }

            BlockPos center = client.player.getBlockPos();
            int minY = Math.max(client.world.getBottomY(), center.getY() - searchRadius);
            int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + searchRadius);
            int scanMinY = Math.max(client.world.getBottomY(), Math.floorDiv(minY, 16) * 16);
            int scanMaxY = Math.min(client.world.getTopYInclusive(), Math.floorDiv(maxY, 16) * 16 + 15);

            if (client.world != cachedWorld
                    || searchRadius != cachedRange
                    || scanMinY != cachedScanMinY
                    || scanMaxY != cachedScanMaxY
                    || trackedBlocksChanged) {
                clearCache();
                cachedWorld = client.world;
                cachedRange = searchRadius;
                cachedScanMinY = scanMinY;
                cachedScanMaxY = scanMaxY;
            }

            int centerChunkX = center.getX() >> 4;
            int centerChunkZ = center.getZ() >> 4;
            int chunkRadius = Math.max(1, (int) Math.ceil(searchRadius / 16.0));

            syncDesiredChunks(client.world, centerChunkX, centerChunkZ, chunkRadius);
            processPendingChunkScans(client.world, scanMinY, scanMaxY);

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) {
                consumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            VertexConsumer lines = consumers.getBuffer(RenderLayers.LINES);
            VertexConsumer linesNoDepth = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

            renderCachedHits(lines, linesNoDepth, cameraPos, center, searchRadius);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayers.LINES);
                immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
            }
        });
    }

    public static void clearCache() {
        cachedHitsByChunk.clear();
        pendingChunkScans.clear();
        pendingChunkKeys.clear();
        desiredChunkKeys.clear();
        cachedWorld = null;
        cachedRange = Integer.MIN_VALUE;
        cachedScanMinY = Integer.MIN_VALUE;
        cachedScanMaxY = Integer.MIN_VALUE;
    }

    private static boolean refreshActiveConfigsByBlock() {
        Set<Block> previousBlocks = new HashSet<>(activeConfigsByBlock.keySet());
        activeConfigsByBlock.clear();

        for (Map.Entry<String, XrayManager.XrayBlockConfig> entry : XrayManager.getXrayBlocks().entrySet()) {
            XrayManager.XrayBlockConfig config = entry.getValue();
            if (config == null || !config.enabled) {
                continue;
            }

            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || !Registries.BLOCK.containsId(id)) {
                continue;
            }

            activeConfigsByBlock.put(Registries.BLOCK.get(id), config);
        }

        return !previousBlocks.equals(activeConfigsByBlock.keySet());
    }

    private static void syncDesiredChunks(ClientWorld world, int centerChunkX, int centerChunkZ, int chunkRadius) {
        desiredChunkKeys.clear();
        List<ChunkPos> missingChunks = new ArrayList<>();

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                if (world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                    continue;
                }

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                long key = chunkPos.toLong();
                desiredChunkKeys.add(key);

                if (!cachedHitsByChunk.containsKey(key) && !pendingChunkKeys.contains(key)) {
                    missingChunks.add(chunkPos);
                }
            }
        }

        cachedHitsByChunk.keySet().removeIf(key -> !desiredChunkKeys.contains(key));

        ArrayDeque<ChunkPos> retainedQueue = new ArrayDeque<>();
        Set<Long> retainedKeys = new HashSet<>();
        for (ChunkPos chunkPos : pendingChunkScans) {
            long key = chunkPos.toLong();
            if (!desiredChunkKeys.contains(key) || cachedHitsByChunk.containsKey(key) || !retainedKeys.add(key)) {
                continue;
            }
            retainedQueue.addLast(chunkPos);
        }

        missingChunks.sort(Comparator.comparingLong(pos -> distanceSq(centerChunkX, centerChunkZ, pos.x, pos.z)));
        for (ChunkPos chunkPos : missingChunks) {
            long key = chunkPos.toLong();
            if (!retainedKeys.add(key)) {
                continue;
            }
            retainedQueue.addLast(chunkPos);
        }

        pendingChunkScans.clear();
        pendingChunkScans.addAll(retainedQueue);
        pendingChunkKeys.clear();
        pendingChunkKeys.addAll(retainedKeys);
    }

    private static void processPendingChunkScans(ClientWorld world, int scanMinY, int scanMaxY) {
        int processed = 0;
        while (processed < SCAN_CHUNK_BUDGET_PER_FRAME && !pendingChunkScans.isEmpty()) {
            ChunkPos chunkPos = pendingChunkScans.pollFirst();
            long key = chunkPos.toLong();
            pendingChunkKeys.remove(key);

            if (!desiredChunkKeys.contains(key) || cachedHitsByChunk.containsKey(key)) {
                continue;
            }

            cachedHitsByChunk.put(key, scanChunk(world, chunkPos, scanMinY, scanMaxY));
            processed++;
        }
    }

    private static List<XrayHit> scanChunk(ClientWorld world, ChunkPos chunkPos, int scanMinY, int scanMaxY) {
        if (world.getChunkManager().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) == null) {
            return List.of();
        }

        List<XrayHit> hits = new ArrayList<>();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int startX = chunkPos.getStartX();
        int endX = chunkPos.getEndX();
        int startZ = chunkPos.getStartZ();
        int endZ = chunkPos.getEndZ();

        for (int x = startX; x <= endX; x++) {
            for (int y = scanMinY; y <= scanMaxY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    mutablePos.set(x, y, z);
                    Block block = world.getBlockState(mutablePos).getBlock();
                    if (!activeConfigsByBlock.containsKey(block)) {
                        continue;
                    }

                    hits.add(new XrayHit(mutablePos.toImmutable(), block));
                }
            }
        }

        return hits;
    }

    private static void renderCachedHits(VertexConsumer lines,
                                         VertexConsumer linesNoDepth,
                                         Vec3d cameraPos,
                                         BlockPos center,
                                         int searchRadius) {
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        for (List<XrayHit> chunkHits : cachedHitsByChunk.values()) {
            for (XrayHit hit : chunkHits) {
                BlockPos pos = hit.pos();
                if (Math.abs(pos.getX() - centerX) > searchRadius
                        || Math.abs(pos.getY() - centerY) > searchRadius
                        || Math.abs(pos.getZ() - centerZ) > searchRadius) {
                    continue;
                }

                XrayManager.XrayBlockConfig blockConfig = activeConfigsByBlock.get(hit.block());
                if (blockConfig == null) {
                    continue;
                }

                float r = ((blockConfig.color >> 16) & 0xFF) / 255.0f;
                float g = ((blockConfig.color >> 8) & 0xFF) / 255.0f;
                float b = (blockConfig.color & 0xFF) / 255.0f;

                double minX = pos.getX() - cameraX - 0.003;
                double minBlockY = pos.getY() - cameraY - 0.003;
                double minZ = pos.getZ() - cameraZ - 0.003;
                double maxX = minX + 1.006;
                double maxBlockY = minBlockY + 1.006;
                double maxZ = minZ + 1.006;

                DrawUtil.drawOutlinedBoxSafe(lines, minX, minBlockY, minZ, maxX, maxBlockY, maxZ, r, g, b, 1.0f, 1.5f);
                DrawUtil.drawOutlinedBoxSafe(linesNoDepth, minX, minBlockY, minZ, maxX, maxBlockY, maxZ, r, g, b, 0.5f, 1.0f);
            }
        }
    }

    private static long distanceSq(int ax, int az, int bx, int bz) {
        long dx = ax - bx;
        long dz = az - bz;
        return dx * dx + dz * dz;
    }

    private record XrayHit(BlockPos pos, Block block) {
    }
}
