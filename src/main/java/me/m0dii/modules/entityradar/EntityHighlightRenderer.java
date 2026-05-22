package me.m0dii.modules.entityradar;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.Chunk;

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
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!enabled) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            List<Entity> entities = EntityRadarModule.INSTANCE.getEntities();

            VertexConsumerProvider vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            VertexConsumer lineBuffer = vertexConsumers.getBuffer(RenderLayers.LINES);
            VertexConsumer occludedLineBuffer = vertexConsumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);

            double cameraX = context.gameRenderer().getCamera().getCameraPos().x;
            double cameraY = context.gameRenderer().getCamera().getCameraPos().y;
            double cameraZ = context.gameRenderer().getCamera().getCameraPos().z;

            for (Entity entity : entities) {
                if (entity == client.player) {
                    continue;
                }

                Box box = entity.getBoundingBox().expand(0.03);
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
                    double centerY = entity.getY() + (entity.getHeight() * 0.5) - cameraY;
                    double centerZ = entity.getZ() - cameraZ;
                    DrawUtil.drawLineSafe(lineBuffer, 0.0, 0.0, 0.0, centerX, centerY, centerZ, 1.0f, 0.8f, 0.2f, 0.9f);
                    DrawUtil.drawLineSafe(occludedLineBuffer, 0.0, 0.0, 0.0, centerX, centerY, centerZ, 1.0f, 0.8f, 0.2f, 0.45f);
                }
            }

            List<Chunk> nearbyChunks = new ArrayList<>();
            int playerChunkX = client.player.getChunkPos().x;
            int playerChunkZ = client.player.getChunkPos().z;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Chunk chunk = client.world.getChunk(playerChunkX + dx, playerChunkZ + dz);
                    nearbyChunks.add(chunk);
                }
            }

            nearbyChunks.forEach(chunk ->
                    chunk.getBlockEntityPositions().forEach(pos -> {
                        drawBlockEntityBoxSafe(lineBuffer, pos, cameraX, cameraY, cameraZ);
                        drawBlockEntityBoxSafe(occludedLineBuffer, pos, cameraX, cameraY, cameraZ);
                    }));

            // Explicitly flush our line buffers so vertices are sent to the GPU in this pass.
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayers.LINES);
                immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
            }
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
