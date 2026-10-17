package me.m0dii.modules.overlays;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.ranges.RangeMath;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ChunkBorderOverlayModule extends Module implements PersistentSettings {

    public static final ChunkBorderOverlayModule INSTANCE = new ChunkBorderOverlayModule();

    private boolean showChunks = true;
    private boolean showRegions = true;

    protected ChunkBorderOverlayModule() {
        super("chunk_border_overlay", "Chunk Border Overlay", false);
    }

    @Override
    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
    }

    private OverlayRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            PoseStack matrices = context.matrices();
            if (matrices == null) {
                return;
            }

            OverlayBufferSource vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                return;
            }

            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            BlockPos playerPos = getClient().player.blockPosition();

            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;

            int radius = 3;
            if (showChunks) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int targetChunkX = chunkX + dx;
                        int targetChunkZ = chunkZ + dz;

                        renderChunkBorder(matrices, vertexConsumers, cameraPos, targetChunkX, targetChunkZ);
                    }
                }
            }
            if (showRegions) {
                renderRegionBorder(vertexConsumers, cameraPos, playerPos);
            }
        };
    }

    /** The edges of the region file (512×512 blocks, r.X.Z.mca) you stand in, at your height and as corner posts. */
    private void renderRegionBorder(@NotNull OverlayBufferSource vertexConsumers, @NotNull Vec3 cameraPos, @NotNull BlockPos playerPos) {
        int regionX = RangeMath.region(playerPos.getX());
        int regionZ = RangeMath.region(playerPos.getZ());
        double minX = regionX * (double) RangeMath.REGION_SIZE - cameraPos.x;
        double minZ = regionZ * (double) RangeMath.REGION_SIZE - cameraPos.z;
        double maxX = minX + RangeMath.REGION_SIZE;
        double maxZ = minZ + RangeMath.REGION_SIZE;
        double y = playerPos.getY() + 0.05 - cameraPos.y;
        double bottom = getClient().level == null ? -64 - cameraPos.y : getClient().level.getMinY() - cameraPos.y;
        double top = getClient().level == null ? 320 - cameraPos.y : getClient().level.getMaxY() + 1 - cameraPos.y;
        VertexConsumer lines = vertexConsumers.getBuffer(RenderTypes.LINES);
        float r = 1.0f;
        float g = 0.35f;
        float b = 0.35f;
        DrawUtil.drawLineSafe(lines, minX, y, minZ, maxX, y, minZ, r, g, b, 0.9f, 2.5f);
        DrawUtil.drawLineSafe(lines, maxX, y, minZ, maxX, y, maxZ, r, g, b, 0.9f, 2.5f);
        DrawUtil.drawLineSafe(lines, maxX, y, maxZ, minX, y, maxZ, r, g, b, 0.9f, 2.5f);
        DrawUtil.drawLineSafe(lines, minX, y, maxZ, minX, y, minZ, r, g, b, 0.9f, 2.5f);
        double[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        for (double[] corner : corners) {
            DrawUtil.drawLineSafe(lines, corner[0], bottom, corner[1], corner[0], top, corner[1], r, g, b, 0.9f, 2.5f);
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Chunk borders: " + (showChunks ? "ON" : "OFF"));
        settings.add("Region file borders: " + (showRegions ? "ON" : "OFF"));
        if (getClient().player != null) {
            BlockPos pos = getClient().player.blockPosition();
            settings.add("You are in " + RangeMath.regionFileName(pos.getX(), pos.getZ()));
        }
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> showChunks = !showChunks;
            case 2 -> showRegions = !showRegions;
            default -> {
                // info line
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("showChunks", showChunks);
        json.addProperty("showRegions", showRegions);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        showChunks = PersistentSettings.readBoolean(settings, "showChunks", showChunks);
        showRegions = PersistentSettings.readBoolean(settings, "showRegions", showRegions);
    }

    private static void renderChunkBorder(@NotNull PoseStack matrices,
                                          @NotNull OverlayBufferSource vertexConsumers,
                                          @NotNull Vec3 cameraPos,
                                          int chunkX,
                                          int chunkZ) {
        matrices.pushPose();

        // Chunk boundaries
        double minX = (chunkX << 4) - cameraPos.x;
        double minZ = (chunkZ << 4) - cameraPos.z;
        double maxX = ((chunkX << 4) + 16) - cameraPos.x;
        double maxZ = ((chunkZ << 4) + 16) - cameraPos.z;

        double minY = -64 - cameraPos.y;
        double maxY = 320 - cameraPos.y;

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderTypes.LINES);
        Matrix4f matrix = matrices.last().pose();

        // Draw vertical lines at chunk corners
        DrawUtil.drawLine(vertexConsumer, matrix, (float) minX, (float) minY, (float) minZ, (float) minX, (float) maxY, (float) minZ,
                0.0f, 0.5f, 1.0f, 0.8f, 0.0f, 1.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, (float) maxX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) minZ,
                0.0f, 0.5f, 1.0f, 0.8f, 0.0f, 1.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, (float) minX, (float) minY, (float) maxZ, (float) minX, (float) maxY, (float) maxZ,
                0.0f, 0.5f, 1.0f, 0.8f, 0.0f, 1.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, (float) maxX, (float) minY, (float) maxZ, (float) maxX, (float) maxY, (float) maxZ,
                0.0f, 0.5f, 1.0f, 0.8f, 0.0f, 1.0f, 0.0f, 1.0f);

        matrices.popPose();
    }
}
