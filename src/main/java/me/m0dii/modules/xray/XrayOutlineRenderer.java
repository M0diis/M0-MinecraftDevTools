package me.m0dii.modules.xray;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class XrayOutlineRenderer {
    private static final int SCAN_CHUNK_BUDGET_PER_FRAME = 4;

    private static boolean registered;

    private static final Map<Long, List<XrayHit>> cachedHitsByChunk = new HashMap<>();
    private static final ArrayDeque<ChunkPos> pendingChunkScans = new ArrayDeque<>();
    private static final Set<Long> pendingChunkKeys = new HashSet<>();
    private static final Set<Long> desiredChunkKeys = new HashSet<>();
    private static final Map<Block, XrayManager.XrayBlockConfig> activeConfigsByBlock = new HashMap<>();

    private static ClientLevel cachedWorld;
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

        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!XrayModule.INSTANCE.isEnabled()) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
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

            BlockPos center = client.player.blockPosition();
            int minY = Math.max(client.level.getMinY(), center.getY() - searchRadius);
            int maxY = Math.min(client.level.getMaxY(), center.getY() + searchRadius);
            int scanMinY = Math.max(client.level.getMinY(), Math.floorDiv(minY, 16) * 16);
            int scanMaxY = Math.min(client.level.getMaxY(), Math.floorDiv(maxY, 16) * 16 + 15);

            if (client.level != cachedWorld
                    || searchRadius != cachedRange
                    || scanMinY != cachedScanMinY
                    || scanMaxY != cachedScanMaxY
                    || trackedBlocksChanged) {
                clearCache();
                cachedWorld = client.level;
                cachedRange = searchRadius;
                cachedScanMinY = scanMinY;
                cachedScanMaxY = scanMaxY;
            }

            int centerChunkX = center.getX() >> 4;
            int centerChunkZ = center.getZ() >> 4;
            int chunkRadius = Math.max(1, (int) Math.ceil(searchRadius / 16.0));

            syncDesiredChunks(client.level, centerChunkX, centerChunkZ, chunkRadius);
            processPendingChunkScans(client.level, scanMinY, scanMaxY);

            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }

            VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
            VertexConsumer linesNoDepth = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            Vec3 cameraPos = context.gameRenderer().mainCamera().position();

            renderCachedHits(lines, linesNoDepth, cameraPos, center, searchRadius);
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
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                continue;
            }

            activeConfigsByBlock.put(BuiltInRegistries.BLOCK.getValue(id), config);
        }

        return !previousBlocks.equals(activeConfigsByBlock.keySet());
    }

    private static void syncDesiredChunks(ClientLevel world, int centerChunkX, int centerChunkZ, int chunkRadius) {
        desiredChunkKeys.clear();
        List<ChunkPos> missingChunks = new ArrayList<>();

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                if (world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                    continue;
                }

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                long key = chunkPos.pack();
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
            long key = chunkPos.pack();
            if (!desiredChunkKeys.contains(key) || cachedHitsByChunk.containsKey(key) || !retainedKeys.add(key)) {
                continue;
            }
            retainedQueue.addLast(chunkPos);
        }

        missingChunks.sort(Comparator.comparingLong(pos -> distanceSq(centerChunkX, centerChunkZ, pos.x(), pos.z())));
        for (ChunkPos chunkPos : missingChunks) {
            long key = chunkPos.pack();
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

    private static void processPendingChunkScans(ClientLevel world, int scanMinY, int scanMaxY) {
        int processed = 0;
        while (processed < SCAN_CHUNK_BUDGET_PER_FRAME && !pendingChunkScans.isEmpty()) {
            ChunkPos chunkPos = pendingChunkScans.pollFirst();
            long key = chunkPos.pack();
            pendingChunkKeys.remove(key);

            if (!desiredChunkKeys.contains(key) || cachedHitsByChunk.containsKey(key)) {
                continue;
            }

            cachedHitsByChunk.put(key, scanChunk(world, chunkPos, scanMinY, scanMaxY));
            processed++;
        }
    }

    private static List<XrayHit> scanChunk(ClientLevel world, ChunkPos chunkPos, int scanMinY, int scanMaxY) {
        if (world.getChunkSource().getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false) == null) {
            return List.of();
        }

        List<XrayHit> hits = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int startX = chunkPos.getMinBlockX();
        int endX = chunkPos.getMaxBlockX();
        int startZ = chunkPos.getMinBlockZ();
        int endZ = chunkPos.getMaxBlockZ();

        for (int x = startX; x <= endX; x++) {
            for (int y = scanMinY; y <= scanMaxY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    mutablePos.set(x, y, z);
                    Block block = world.getBlockState(mutablePos).getBlock();
                    if (!activeConfigsByBlock.containsKey(block)) {
                        continue;
                    }

                    hits.add(new XrayHit(mutablePos.immutable(), block));
                }
            }
        }

        return hits;
    }

    private static void renderCachedHits(VertexConsumer lines,
                                         VertexConsumer linesNoDepth,
                                         Vec3 cameraPos,
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
