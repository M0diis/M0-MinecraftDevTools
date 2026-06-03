package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import java.util.ArrayList;
import java.util.List;

public class BiomeBorderOverlayModule extends Module {
    public static final BiomeBorderOverlayModule INSTANCE = new BiomeBorderOverlayModule();

    private int radius = 48;
    private static final double WALL_THICKNESS = 0.075;
    private int wallHalfHeight = 64;
    private int cellSize = 4;

    private BiomeBorderOverlayModule() {
        super("biome_border_overlay", "Biome Border Overlay", false);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderBiomeBorders);
        registerPressedKeybind(
                KeybindCatalog.BIOME_BORDER_OVERLAY_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.BIOME_BORDER_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled()
        );
    }

    private void renderBiomeBorders(WorldRenderContext context) {
        if (!isEnabled() || isClientNull() || context.matrices() == null || context.consumers() == null || context.gameRenderer() == null || context.gameRenderer().getCamera() == null) {
            return;
        }

        if (getClient().world == null) {
            return;
        }

        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.LINES);
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

        BlockPos center = getClient().player.getBlockPos();
        int sampleY = MathHelper.clamp(center.getY(), getClient().world.getBottomY(), getClient().world.getTopYInclusive());
        int minY = Math.max(getClient().world.getBottomY(), center.getY() - wallHalfHeight);
        int maxY = Math.min(getClient().world.getTopYInclusive() + 1, center.getY() + wallHalfHeight + 1);

        BlockPos.Mutable a = new BlockPos.Mutable();
        BlockPos.Mutable b = new BlockPos.Mutable();

        int step = Math.max(1, cellSize);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x += step) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += step) {
                a.set(x, sampleY, z);

                b.set(x + step, sampleY, z);
                RegistryEntry<Biome> biomeA = getClient().world.getBiome(a);
                RegistryEntry<Biome> biomeB = getClient().world.getBiome(b);
                if (!sameBiome(biomeA, biomeB)) {
                    float[] color = blendColors(colorForBiome(biomeA), colorForBiome(biomeB));
                    drawVerticalWall(lines, cameraPos, x + step, z, minY, maxY, step, color[0], color[1], color[2]);
                }

                b.set(x, sampleY, z + step);
                biomeB = getClient().world.getBiome(b);
                if (!sameBiome(biomeA, biomeB)) {
                    float[] color = blendColors(colorForBiome(biomeA), colorForBiome(biomeB));
                    drawHorizontalWall(lines, cameraPos, x, z + step, minY, maxY, step, color[0], color[1], color[2]);
                }
            }
        }
    }

    private static boolean sameBiome(RegistryEntry<Biome> a, RegistryEntry<Biome> b) {
        if (a == b) {
            return true;
        }
        if (a.getKey().isPresent() && b.getKey().isPresent()) {
            return a.getKey().get().equals(b.getKey().get());
        }
        return a.value() == b.value();
    }

    private static void drawVerticalWall(VertexConsumer lines,
                                         Vec3d cameraPos,
                                         int worldX,
                                         int worldZ,
                                         int minY,
                                         int maxY,
                                         int cell,
                                         float r,
                                         float g,
                                         float b) {
        double minX = worldX - (WALL_THICKNESS / 2.0);
        double maxX = worldX + (WALL_THICKNESS / 2.0);
        for (int y = minY; y < maxY; y += cell) {
            double drawMinX = minX - cameraPos.x;
            double drawMaxX = maxX - cameraPos.x;
            double drawMinZ = worldZ - cameraPos.z;
            double drawMaxZ = worldZ + cell - cameraPos.z;
            double drawMinY = y - cameraPos.y;
            double drawMaxY = Math.min(maxY, y + cell) - cameraPos.y;
            DrawUtil.drawOutlinedBox(lines, drawMinX, drawMinY, drawMinZ, drawMaxX, drawMaxY, drawMaxZ, r, g, b, 0.9f, 1.0f);
        }
    }

    private static void drawHorizontalWall(VertexConsumer lines,
                                           Vec3d cameraPos,
                                           int worldX,
                                           int worldZ,
                                           int minY,
                                           int maxY,
                                           int cell,
                                           float r,
                                           float g,
                                           float b) {
        double minZ = worldZ - (WALL_THICKNESS / 2.0);
        double maxZ = worldZ + (WALL_THICKNESS / 2.0);
        for (int y = minY; y < maxY; y += cell) {
            double drawMinX = worldX - cameraPos.x;
            double drawMaxX = worldX + cell - cameraPos.x;
            double drawMinZ = minZ - cameraPos.z;
            double drawMaxZ = maxZ - cameraPos.z;
            double drawMinY = y - cameraPos.y;
            double drawMaxY = Math.min(maxY, y + cell) - cameraPos.y;
            DrawUtil.drawOutlinedBox(lines, drawMinX, drawMinY, drawMinZ, drawMaxX, drawMaxY, drawMaxZ, r, g, b, 0.9f, 1.0f);
        }
    }

    private static float[] colorForBiome(RegistryEntry<Biome> biome) {
        int hash = biome.getKey().map(key -> key.getValue().hashCode()).orElse(System.identityHashCode(biome.value()));
        int rgb = 0x404040 | (hash & 0xBFBFBF);
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }

    private static float[] blendColors(float[] a, float[] b) {
        return new float[]{
                (a[0] + b[0]) * 0.5f,
                (a[1] + b[1]) * 0.5f,
                (a[2] + b[2]) * 0.5f
        };
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Radius: " + radius);
        settings.add("Wall Height: " + (wallHalfHeight * 2 + 1));
        settings.add("Cell Size: " + cellSize);
        settings.add("Radius +16");
        settings.add("Radius -16");
        settings.add("Height +8");
        settings.add("Height -8");
        settings.add("Cell +1");
        settings.add("Cell -1");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> radius = Math.min(160, radius + 16);
            case 4 -> radius = Math.max(16, radius - 16);
            case 5 -> wallHalfHeight = Math.min(96, wallHalfHeight + 8);
            case 6 -> wallHalfHeight = Math.max(4, wallHalfHeight - 8);
            case 7 -> cellSize = Math.min(16, cellSize + 1);
            case 8 -> cellSize = Math.max(1, cellSize - 1);
            default -> {
                // no-op
            }
        }
    }
}

