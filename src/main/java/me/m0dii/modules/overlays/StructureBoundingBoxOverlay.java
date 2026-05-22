package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
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
            if (!isEnabled() || isClientNull()) {
                return;
            }

            MatrixStack matrices = context.matrices();
            Camera camera = context.gameRenderer().getCamera();

            if (matrices == null || camera == null) {
                return;
            }

            VertexConsumerProvider vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                return;
            }

            Vec3d cameraPos = camera.getCameraPos();
            BlockPos playerPos = getClient().player.getBlockPos();

            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;
            int radius = 16;

            List<BlockBox> boxes = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    WorldChunk chunk = getClient().world.getChunk(chunkX + dx, chunkZ + dz);
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
        var vertexConsumer = vertexConsumers.getBuffer(RenderLayers.LINES);
        double minX = box.getMinX() - cameraPos.x;
        double minY = box.getMinY() - cameraPos.y;
        double minZ = box.getMinZ() - cameraPos.z;
        double maxX = box.getMaxX() + 1 - cameraPos.x;
        double maxY = box.getMaxY() + 1 - cameraPos.y;
        double maxZ = box.getMaxZ() + 1 - cameraPos.z;

        DrawUtil.drawOutlinedBox(vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        matrices.pop();
    }
}
