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
import org.joml.Matrix4f;

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
            if (entities.isEmpty()) {
                return;
            }

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

                drawBox(matrices, vertexConsumer, box, 1.0f, 1.0f, 0.0f, 1.0f);

                matrices.pop();
            }
        });
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer vertexConsumer, Box box, float red, float green, float blue, float alpha) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face
        line(vertexConsumer, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        // Top face
        line(vertexConsumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        // Vertical edges
        line(vertexConsumer, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void line(VertexConsumer vertexConsumer,
                             Matrix4f matrix,
                             float x1,
                             float y1,
                             float z1,
                             float x2,
                             float y2,
                             float z2,
                             float red,
                             float green,
                             float blue,
                             float alpha) {
        // Normal vector
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        vertexConsumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).normal(dx, dy, dz);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).normal(dx, dy, dz);
    }
}

