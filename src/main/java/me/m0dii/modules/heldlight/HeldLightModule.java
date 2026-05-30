package me.m0dii.modules.heldlight;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeldLightModule extends Module {
    public static final HeldLightModule INSTANCE = new HeldLightModule();
    private static ChunkPos lastLightChunkPos;
    private static int lastChunkRadius;
    private static boolean lastHadLightSource;
    private static boolean cachedLightActive;
    private static int cachedBrightness;
    private static double cachedFalloffDistance;
    private static double cachedFalloffDistanceSq;
    private static double cachedPlayerX;
    private static double cachedPlayerY;
    private static double cachedPlayerZ;

    private static int cachedWhitelistHash;
    private static Set<String> cachedItemWhitelist = Set.of();
    private static List<TagKey<net.minecraft.item.Item>> cachedTagWhitelist = List.of();

    private HeldLightModule() {
        super("held_light", "Held Light", false);
    }

    @Override
    public void register() {
        HeldLightConfigDataHandler.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.worldRenderer == null) {
            lastLightChunkPos = null;
            lastChunkRadius = 0;
            lastHadLightSource = false;
            cachedLightActive = false;
            return;
        }

        HeldLightConfigDataHandler.Config cfg = HeldLightConfigDataHandler.get();
        refreshWhitelistCache(cfg);

        boolean hasLightSource = isLightSource(client.player.getMainHandStack(), cfg)
                || isLightSource(client.player.getOffHandStack(), cfg);
        boolean active = isEnabled() && hasLightSource && cfg.falloffDistance > 0.0 && cfg.brightness > 0;

        if (active) {
            cachedLightActive = true;
            cachedBrightness = cfg.brightness;
            cachedFalloffDistance = cfg.falloffDistance;
            cachedFalloffDistanceSq = cfg.falloffDistance * cfg.falloffDistance;
            cachedPlayerX = client.player.getX();
            cachedPlayerY = client.player.getY() + client.player.getStandingEyeHeight();
            cachedPlayerZ = client.player.getZ();
        } else {
            cachedLightActive = false;
        }

        if (!active) {
            if (lastHadLightSource && lastLightChunkPos != null) {
                scheduleChunkRange(client.worldRenderer, lastLightChunkPos, Math.max(1, lastChunkRadius), client.world.getBottomY(), client.world.getTopYInclusive() + 1);
            }
            lastLightChunkPos = null;
            lastChunkRadius = 0;
            lastHadLightSource = false;
            return;
        }

        int chunkRadius = Math.max(1, (int) Math.ceil(cfg.falloffDistance / 16.0) + 1);
        ChunkPos currentChunk = new ChunkPos(client.player.getBlockPos());
        if (!lastHadLightSource || lastLightChunkPos == null || !lastLightChunkPos.equals(currentChunk) || lastChunkRadius != chunkRadius) {
            int minY = client.world.getBottomY();
            int maxY = client.world.getTopYInclusive() + 1;
            if (lastHadLightSource && lastLightChunkPos != null) {
                scheduleChunkRange(client.worldRenderer, lastLightChunkPos, Math.max(1, Math.max(lastChunkRadius, chunkRadius)), minY, maxY);
            }
            scheduleChunkRange(client.worldRenderer, currentChunk, chunkRadius, minY, maxY);
            lastLightChunkPos = currentChunk;
            lastChunkRadius = chunkRadius;
            lastHadLightSource = true;
        }
    }

    private static void scheduleChunkRange(WorldRenderer renderer, ChunkPos centerChunk, int chunkRadius, int minY, int maxY) {
        for (int cz = centerChunk.z - chunkRadius; cz <= centerChunk.z + chunkRadius; cz++) {
            for (int cx = centerChunk.x - chunkRadius; cx <= centerChunk.x + chunkRadius; cx++) {
                int minX = cx << 4;
                int minZ = cz << 4;
                renderer.scheduleBlockRenders(minX, minY, minZ, minX + 16, maxY, minZ + 16);
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
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        if (cachedItemWhitelist.contains(id)) {
            return true;
        }
        for (TagKey<net.minecraft.item.Item> tag : cachedTagWhitelist) {
            if (stack.isIn(tag)) {
                return true;
            }
        }
        return false;
    }

    public static int getHeldLightLevel(MinecraftClient client, BlockPos pos) {
        if (!INSTANCE.isEnabled() || client == null || client.player == null || pos == null || !cachedLightActive) {
            return 0;
        }

        double dx = pos.getX() + 0.5 - cachedPlayerX;
        double dy = pos.getY() + 0.5 - cachedPlayerY;
        double dz = pos.getZ() + 0.5 - cachedPlayerZ;
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

        List<TagKey<net.minecraft.item.Item>> tags = new ArrayList<>();
        for (String tagId : cfg.tagWhitelist) {
            Identifier parsed = Identifier.tryParse(tagId);
            if (parsed != null) {
                tags.add(TagKey.of(RegistryKeys.ITEM, parsed));
            }
        }
        cachedTagWhitelist = List.copyOf(tags);
    }
}

