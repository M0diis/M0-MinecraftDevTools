package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StructureBoundingBoxOverlay extends Module {

    public static final StructureBoundingBoxOverlay INSTANCE = new StructureBoundingBoxOverlay();

    private StructureBoundingBoxOverlay() {
        super("structure_bounding_box_overlay", "Structure Bounding Box Overlay", false);
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
            int radius = 16;

            List<BlockBox> boxes = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    WorldChunk chunk = client.world.getChunk(chunkX + dx, chunkZ + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (StructureStart start : chunk.getStructureStarts().values()) {
                        if (start != null && start.hasChildren()) {
                            BlockBox box = start.getBoundingBox();
                            if (box != null) {
                                boxes.add(box);
                            }
                        }
                    }
                }
            }

            for (BlockBox box : boxes) {
                renderBoundingBox(matrices, vertexConsumers, cameraPos, box);
            }
        };
    }

    private static void renderBoundingBox(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Vec3d cameraPos, BlockBox box) {
        float r = 1.0f;
        float g = 0.0f;
        float b = 1.0f;
        float a = 0.8f;
        matrices.push();
        var vertexConsumer = vertexConsumers.getBuffer(net.minecraft.client.render.RenderLayer.getLines());
        double minX = box.getMinX() - cameraPos.x;
        double minY = box.getMinY() - cameraPos.y;
        double minZ = box.getMinZ() - cameraPos.z;
        double maxX = box.getMaxX() + 1 - cameraPos.x;
        double maxY = box.getMaxY() + 1 - cameraPos.y;
        double maxZ = box.getMaxZ() + 1 - cameraPos.z;

        // Draw 12 edges of the box
        drawLine(vertexConsumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(vertexConsumer, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(vertexConsumer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(vertexConsumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(vertexConsumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(vertexConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        matrices.pop();
    }

    private static void drawLine(VertexConsumer vertexConsumer,
                                 double x1,
                                 double y1,
                                 double z1,
                                 double x2,
                                 double y2,
                                 double z2,
                                 float r,
                                 float g,
                                 float b,
                                 float a) {
        vertexConsumer.vertex((float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
        vertexConsumer.vertex((float) x2, (float) y2, (float) z2).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f);
    }
}
