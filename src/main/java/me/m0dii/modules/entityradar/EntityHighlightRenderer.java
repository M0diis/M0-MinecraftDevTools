package me.m0dii.modules.entityradar;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class EntityHighlightRenderer implements Toggleable {

    @Getter
    @Setter
    private boolean enabled = false;

    @Getter
    @Setter
    private boolean tracersEnabled = true;

    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!enabled) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }

            List<Entity> entities = EntityRadarModule.INSTANCE.getEntities();

            OverlayBufferSource vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                vertexConsumers = OverlayBufferSource.current();
            }

            VertexConsumer lineBuffer = vertexConsumers.getBuffer(RenderTypes.LINES);
            VertexConsumer occludedLineBuffer = vertexConsumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);

            double cameraX = context.gameRenderer().mainCamera().position().x;
            double cameraY = context.gameRenderer().mainCamera().position().y;
            double cameraZ = context.gameRenderer().mainCamera().position().z;
            var viewDir = context.gameRenderer().mainCamera().forwardVector();
            double traceStartX = viewDir.x() * 0.35;
            double traceStartY = 0.0;
            double traceStartZ = viewDir.z() * 0.35;

            for (Entity entity : entities) {
                if (entity == client.player) {
                    continue;
                }

                AABB box = entity.getBoundingBox().inflate(0.03);
                DrawUtil.drawOutlinedBoxSafe(
                        lineBuffer,
                        box.minX - cameraX,
                        box.minY - cameraY,
                        box.minZ - cameraZ,
                        box.maxX - cameraX,
                        box.maxY - cameraY,
                        box.maxZ - cameraZ,
                        1.0f, 0.25f, 0.25f, 0.9f
                );
                DrawUtil.drawOutlinedBoxSafe(
                        occludedLineBuffer,
                        box.minX - cameraX,
                        box.minY - cameraY,
                        box.minZ - cameraZ,
                        box.maxX - cameraX,
                        box.maxY - cameraY,
                        box.maxZ - cameraZ,
                        1.0f, 0.25f, 0.25f, 0.45f
                );

                if (tracersEnabled) {
                    double centerX = entity.getX() - cameraX;
                    double centerY = entity.getEyeY() - cameraY;
                    double centerZ = entity.getZ() - cameraZ;
                    DrawUtil.drawLineSafe(lineBuffer, traceStartX, traceStartY, traceStartZ, centerX, centerY, centerZ, 1.0f, 0.8f, 0.2f, 0.9f, 1.25f);
                    DrawUtil.drawLineSafe(occludedLineBuffer, traceStartX, traceStartY, traceStartZ, centerX, centerY, centerZ, 1.0f, 0.8f, 0.2f, 0.5f, 1.25f);
                }
            }

            List<ChunkAccess> nearbyChunks = new ArrayList<>();
            int playerChunkX = client.player.chunkPosition().x();
            int playerChunkZ = client.player.chunkPosition().z();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkAccess chunk = client.level.getChunk(playerChunkX + dx, playerChunkZ + dz);
                    nearbyChunks.add(chunk);
                }
            }

            nearbyChunks.forEach(chunk ->
                    chunk.getBlockEntitiesPos().forEach(pos -> {
                        drawBlockEntityBoxSafe(lineBuffer, pos, cameraX, cameraY, cameraZ);
                        drawBlockEntityBoxSafe(occludedLineBuffer, pos, cameraX, cameraY, cameraZ);
                    }));

            // Explicitly flush our line buffers so vertices are sent to the GPU in this pass.
        });
    }

    private static void drawBlockEntityBox(VertexConsumer lineBuffer, BlockPos pos, double cameraX, double cameraY, double cameraZ) {
        double minX = pos.getX() - cameraX;
        double minY = pos.getY() - cameraY;
        double minZ = pos.getZ() - cameraZ;
        double maxX = minX + 1.0;
        double maxY = minY + 1.0;
        double maxZ = minZ + 1.0;

        DrawUtil.drawOutlinedBox(lineBuffer, minX, minY, minZ, maxX, maxY, maxZ, 0.25f, 0.8f, 1.0f, 0.9f);
    }

    private static void drawBlockEntityBoxSafe(VertexConsumer lineBuffer, BlockPos pos, double cameraX, double cameraY, double cameraZ) {
        try {
            drawBlockEntityBox(lineBuffer, pos, cameraX, cameraY, cameraZ);
        } catch (IllegalStateException ignored) {
            // Optional pass: skip if this buffer is not building in the current phase.
        }
    }

}
