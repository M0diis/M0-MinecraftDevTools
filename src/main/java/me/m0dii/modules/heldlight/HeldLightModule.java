package me.m0dii.modules.heldlight;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HeldLightModule extends Module {
    public static final HeldLightModule INSTANCE = new HeldLightModule();
    private static BlockPos lastRefreshBlockPos;
    private static SectionBox lastLitBox;
    private static double lastFalloff;

    // Read from chunk-meshing threads, so published as volatiles; the position is sampled once per tick.
    private static volatile boolean cachedLightActive;
    private static volatile int cachedBrightness;
    private static volatile double cachedFalloffDistance;
    private static volatile double cachedFalloffDistanceSq;
    private static volatile double cachedLightX;
    private static volatile double cachedLightY;
    private static volatile double cachedLightZ;

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
        if (client == null || client.player == null || client.level == null) {
            lastRefreshBlockPos = null;
            lastLitBox = null;
            cachedLightActive = false;
            return;
        }

        HeldLightConfigDataHandler.Config cfg = HeldLightConfigDataHandler.get();
        refreshWhitelistCache(cfg);

        boolean hasLightSource = isLightSource(client.player.getMainHandItem(), cfg)
                || isLightSource(client.player.getOffhandItem(), cfg);
        boolean active = isEnabled() && hasLightSource && cfg.falloffDistance > 0.0 && cfg.brightness > 0;

        if (!active) {
            cachedLightActive = false;
            if (lastLitBox != null) {
                // Rebuild once more so the light disappears.
                markSectionsDirty(client, lastLitBox);
            }
            lastLitBox = null;
            lastRefreshBlockPos = null;
            return;
        }

        cachedBrightness = cfg.brightness;
        cachedFalloffDistance = cfg.falloffDistance;
        cachedFalloffDistanceSq = cfg.falloffDistance * cfg.falloffDistance;
        cachedLightX = client.player.getX();
        cachedLightY = client.player.getY() + client.player.getEyeHeight();
        cachedLightZ = client.player.getZ();
        cachedLightActive = true;

        BlockPos currentBlockPos = client.player.blockPosition();
        if (currentBlockPos.equals(lastRefreshBlockPos) && cfg.falloffDistance == lastFalloff) {
            return;
        }

        // Only the sections the light can reach (plus one block for smooth lighting of neighbouring faces).
        SectionBox litBox = SectionBox.around(cachedLightX, cachedLightY, cachedLightZ, cfg.falloffDistance + 1.0);
        if (lastLitBox != null && !lastLitBox.equals(litBox)) {
            markSectionsDirty(client, lastLitBox);
        }
        markSectionsDirty(client, litBox);
        lastLitBox = litBox;
        lastRefreshBlockPos = currentBlockPos;
        lastFalloff = cfg.falloffDistance;
    }

    /** Inclusive range of chunk sections. */
    private record SectionBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static SectionBox around(double x, double y, double z, double radius) {
            return new SectionBox(
                    SectionPos.blockToSectionCoord(Mth.floor(x - radius)),
                    SectionPos.blockToSectionCoord(Mth.floor(y - radius)),
                    SectionPos.blockToSectionCoord(Mth.floor(z - radius)),
                    SectionPos.blockToSectionCoord(Mth.floor(x + radius)),
                    SectionPos.blockToSectionCoord(Mth.floor(y + radius)),
                    SectionPos.blockToSectionCoord(Mth.floor(z + radius)));
        }
    }

    private static void markSectionsDirty(Minecraft client, SectionBox box) {
        // Section-level call: LevelExtractor.setBlocksDirty visits every block, which is what made this lag.
        client.levelExtractor.setSectionRangeDirty(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
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

    /** Called for every vertex while chunks are meshed, so it must stay cheap. */
    public static int getHeldLightLevel(BlockPos pos) {
        if (!cachedLightActive || pos == null) {
            return 0;
        }

        double dx = pos.getX() + 0.5 - cachedLightX;
        double dy = pos.getY() + 0.5 - cachedLightY;
        double dz = pos.getZ() + 0.5 - cachedLightZ;
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

