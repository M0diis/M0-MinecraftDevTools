package me.m0dii.modules.heldlight;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeldLightModule extends Module {
    public static final HeldLightModule INSTANCE = new HeldLightModule();
    private static ChunkPos lastLightChunkPos;
    private static BlockPos lastRefreshBlockPos;
    private static int lastChunkRadius;
    private static boolean lastHadLightSource;
    private static boolean cachedLightActive;
    private static int cachedBrightness;
    private static double cachedFalloffDistance;
    private static double cachedFalloffDistanceSq;

    private static int cachedWhitelistHash;
    private static Set<String> cachedItemWhitelist = Set.of();
    private static List<TagKey<net.minecraft.world.item.Item>> cachedTagWhitelist = List.of();

    private HeldLightModule() {
        super("held_light", "Held Light", false);
    }

    @Override
    public void register() {
        HeldLightConfigDataHandler.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(Minecraft client) {
        if (client == null || client.player == null || client.level == null || client.levelRenderer == null) {
            lastLightChunkPos = null;
            lastRefreshBlockPos = null;
            lastChunkRadius = 0;
            lastHadLightSource = false;
            cachedLightActive = false;
            return;
        }

        HeldLightConfigDataHandler.Config cfg = HeldLightConfigDataHandler.get();
        refreshWhitelistCache(cfg);

        boolean hasLightSource = isLightSource(client.player.getMainHandItem(), cfg)
                || isLightSource(client.player.getOffhandItem(), cfg);
        boolean active = isEnabled() && hasLightSource && cfg.falloffDistance > 0.0 && cfg.brightness > 0;

        if (active) {
            cachedLightActive = true;
            cachedBrightness = cfg.brightness;
            cachedFalloffDistance = cfg.falloffDistance;
            cachedFalloffDistanceSq = cfg.falloffDistance * cfg.falloffDistance;
        } else {
            cachedLightActive = false;
        }

        if (!active) {
            if (lastHadLightSource && lastLightChunkPos != null) {
                scheduleChunkRange(client.levelRenderer, lastLightChunkPos, Math.max(1, lastChunkRadius), client.level.getMinY(), client.level.getMaxY() + 1);
            }
            lastLightChunkPos = null;
            lastRefreshBlockPos = null;
            lastChunkRadius = 0;
            lastHadLightSource = false;
            return;
        }

        int chunkRadius = Math.max(1, (int) Math.ceil(cfg.falloffDistance / 16.0) + 1);
        BlockPos currentBlockPos = client.player.blockPosition();
        ChunkPos currentChunk = ChunkPos.containing(currentBlockPos);
        boolean movedEnough = lastRefreshBlockPos == null
                || Math.abs(currentBlockPos.getX() - lastRefreshBlockPos.getX()) >= 2
                || Math.abs(currentBlockPos.getY() - lastRefreshBlockPos.getY()) >= 2
                || Math.abs(currentBlockPos.getZ() - lastRefreshBlockPos.getZ()) >= 2;

        if (!lastHadLightSource || lastLightChunkPos == null || !lastLightChunkPos.equals(currentChunk) || lastChunkRadius != chunkRadius || movedEnough) {
            int minY = client.level.getMinY();
            int maxY = client.level.getMaxY() + 1;
            if (lastHadLightSource && lastLightChunkPos != null) {
                scheduleChunkRange(client.levelRenderer, lastLightChunkPos, Math.max(1, Math.max(lastChunkRadius, chunkRadius)), minY, maxY);
            }
            scheduleChunkRange(client.levelRenderer, currentChunk, chunkRadius, minY, maxY);
            lastLightChunkPos = currentChunk;
            lastRefreshBlockPos = currentBlockPos;
            lastChunkRadius = chunkRadius;
            lastHadLightSource = true;
        }
    }

    private static void scheduleChunkRange(LevelRenderer renderer, ChunkPos centerChunk, int chunkRadius, int minY, int maxY) {
        for (int cz = centerChunk.z() - chunkRadius; cz <= centerChunk.z() + chunkRadius; cz++) {
            for (int cx = centerChunk.x() - chunkRadius; cx <= centerChunk.x() + chunkRadius; cx++) {
                int minX = cx << 4;
                int minZ = cz << 4;
                Minecraft.getInstance().levelExtractor.setBlocksDirty(minX, minY, minZ, minX + 16, maxY, minZ + 16);
            }
        }
    }

    public static boolean isLightSource(ItemStack stack) {
        return isLightSource(stack, HeldLightConfigDataHandler.get());
    }

    private static boolean isLightSource(ItemStack stack, HeldLightConfigDataHandler.Config cfg) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        refreshWhitelistCache(cfg);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (cachedItemWhitelist.contains(id)) {
            return true;
        }
        for (TagKey<net.minecraft.world.item.Item> tag : cachedTagWhitelist) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static int getHeldLightLevel(Minecraft client, BlockPos pos) {
        if (!INSTANCE.isEnabled() || client == null || client.player == null || pos == null || !cachedLightActive) {
            return 0;
        }

        double playerX = client.player.getX();
        double playerY = client.player.getY() + client.player.getEyeHeight();
        double playerZ = client.player.getZ();

        double dx = pos.getX() + 0.5 - playerX;
        double dy = pos.getY() + 0.5 - playerY;
        double dz = pos.getZ() + 0.5 - playerZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq > cachedFalloffDistanceSq) {
            return 0;
        }

        double distance = Math.sqrt(distanceSq);
        float distanceFactor = (float) Math.clamp(1.0 - (distance / cachedFalloffDistance), 0.0, 1.0);
        return Math.clamp(Math.round(cachedBrightness * distanceFactor), 1, 15);
    }

    private static void refreshWhitelistCache(HeldLightConfigDataHandler.Config cfg) {
        int currentHash = 31 * cfg.itemWhitelist.hashCode() + cfg.tagWhitelist.hashCode();
        if (currentHash == cachedWhitelistHash) {
            return;
        }

        cachedWhitelistHash = currentHash;
        cachedItemWhitelist = new HashSet<>(cfg.itemWhitelist);

        List<TagKey<net.minecraft.world.item.Item>> tags = new ArrayList<>();
        for (String tagId : cfg.tagWhitelist) {
            Identifier parsed = Identifier.tryParse(tagId);
            if (parsed != null) {
                tags.add(TagKey.create(Registries.ITEM, parsed));
            }
        }
        cachedTagWhitelist = List.copyOf(tags);
    }
}

