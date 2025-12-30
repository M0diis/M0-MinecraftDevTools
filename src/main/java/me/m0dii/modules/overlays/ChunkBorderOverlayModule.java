package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ChunkBorderOverlayModule extends Module {

    public static final ChunkBorderOverlayModule INSTANCE = new ChunkBorderOverlayModule();

    protected ChunkBorderOverlayModule() {
        super("chunk_border_overlay", "Chunk Border Overlay", false);
    }

    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
    }

    private WorldRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider vertexConsumers = context.consumers();
            Vec3d cameraPos = context.camera().getPos();

            BlockPos playerPos = client.player.getBlockPos();
            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;

            // Render chunk borders in a radius around player
            int radius = 3; // Show 3x3 chunks around player
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int targetChunkX = chunkX + dx;
                    int targetChunkZ = chunkZ + dz;

                    renderChunkBorder(matrices, vertexConsumers, cameraPos, targetChunkX, targetChunkZ);
                }
            }
        };
    }

    private static void renderChunkBorder(MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers,
                                          Vec3d cameraPos,
                                          int chunkX,
                                          int chunkZ) {
        matrices.push();

        // Chunk boundaries
        double minX = (chunkX << 4) - cameraPos.x;
        double minZ = (chunkZ << 4) - cameraPos.z;
        double maxX = ((chunkX << 4) + 16) - cameraPos.x;
        double maxZ = ((chunkZ << 4) + 16) - cameraPos.z;

        // Height range for lines
        double minY = -64 - cameraPos.y;
        double maxY = 320 - cameraPos.y;

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Draw vertical lines at chunk corners
        drawLine(vertexConsumer, matrix, (float) minX, (float) minY, (float) minZ, (float) minX, (float) maxY, (float) minZ);
        drawLine(vertexConsumer, matrix, (float) maxX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) minZ);
        drawLine(vertexConsumer, matrix, (float) minX, (float) minY, (float) maxZ, (float) minX, (float) maxY, (float) maxZ);
        drawLine(vertexConsumer, matrix, (float) maxX, (float) minY, (float) maxZ, (float) maxX, (float) maxY, (float) maxZ);

        matrices.pop();
    }

    private static void drawLine(VertexConsumer vertexConsumer, Matrix4f matrix,
                                 float x1, float y1, float z1, float x2, float y2, float z2) {
        // Blue color for chunk borders
        vertexConsumer.vertex(matrix, x1, y1, z1).color(0.0f, 0.5f, 1.0f, 0.8f).normal(0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(0.0f, 0.5f, 1.0f, 0.8f).normal(0.0f, 1.0f, 0.0f);
    }
}
