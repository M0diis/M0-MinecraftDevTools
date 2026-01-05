package me.m0dii.modules.entityradar;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class EntityHighlightRenderer implements Toggleable {

    @Getter
    @Setter
    private boolean enabled = false;

    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "lines_no_depth",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            1536,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .build(false)
    );

    public void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!enabled) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            List<Entity> entities = EntityRadarModule.INSTANCE.getEntities();

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                return;
            }

            double cameraX = context.camera().getPos().x;
            double cameraY = context.camera().getPos().y;
            double cameraZ = context.camera().getPos().z;

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(LINES_NO_DEPTH);

            for (Entity entity : entities) {
                if (entity == client.player) {
                    continue;
                }
                matrices.push();
                matrices.translate(entity.getX() - cameraX, entity.getY() - cameraY, entity.getZ() - cameraZ);
                Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
                VertexRendering.drawBox(matrices, vertexConsumer, box, 1.0f, 1.0f, 0.0f, 1.0f);
                matrices.pop();
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
                        matrices.push();
                        matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                        Box localBox = new Box(0, 0, 0, 1, 1, 1);
                        VertexRendering.drawBox(matrices, vertexConsumer, localBox, 0.0f, 1.0f, 1.0f, 1.0f);
                        matrices.pop();
                    }));

        });
    }
}

