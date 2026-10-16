package me.m0dii.modules.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.modules.Module;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ChunkBorderOverlayModule extends Module {

    public static final ChunkBorderOverlayModule INSTANCE = new ChunkBorderOverlayModule();

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
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int targetChunkX = chunkX + dx;
                    int targetChunkZ = chunkZ + dz;

                    renderChunkBorder(matrices, vertexConsumers, cameraPos, targetChunkX, targetChunkZ);
                }
            }
        };
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
