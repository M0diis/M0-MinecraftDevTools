package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.DrawUtil;
import me.m0dii.render.OverlayRenderEvents;
import net.minecraft.client.Camera;
import me.m0dii.render.OverlayBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;

public class StructureBoundingBoxOverlay extends Module {

    public static final StructureBoundingBoxOverlay INSTANCE = new StructureBoundingBoxOverlay();

    private StructureBoundingBoxOverlay() {
        super("structure_bounding_box_overlay", "Structure Bounding Box Overlay", false);
    }

    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
    }

    private OverlayRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            PoseStack matrices = context.matrices();
            Camera camera = context.gameRenderer().mainCamera();

            if (matrices == null || camera == null) {
                return;
            }

            OverlayBufferSource vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                return;
            }

            Vec3 cameraPos = camera.position();
            BlockPos playerPos = getClient().player.blockPosition();

            int chunkX = playerPos.getX() >> 4;
            int chunkZ = playerPos.getZ() >> 4;
            int radius = 16;

            List<BoundingBox> boxes = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    LevelChunk chunk = getClient().level.getChunk(chunkX + dx, chunkZ + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (StructureStart start : chunk.getAllStarts().values()) {
                        if (start != null && start.isValid()) {
                            BoundingBox box = start.getBoundingBox();
                            if (box != null) {
                                boxes.add(box);
                            }
                        }
                    }
                }
            }

            for (BoundingBox box : boxes) {
                renderBoundingBox(matrices, vertexConsumers, cameraPos, box);
            }
        };
    }

    private static void renderBoundingBox(PoseStack matrices, OverlayBufferSource vertexConsumers, Vec3 cameraPos, BoundingBox box) {
        float r = 1.0f;
        float g = 0.0f;
        float b = 1.0f;
        float a = 0.8f;
        matrices.pushPose();
        var vertexConsumer = vertexConsumers.getBuffer(RenderTypes.LINES);
        double minX = box.minX() - cameraPos.x;
        double minY = box.minY() - cameraPos.y;
        double minZ = box.minZ() - cameraPos.z;
        double maxX = box.maxX() + 1 - cameraPos.x;
        double maxY = box.maxY() + 1 - cameraPos.y;
        double maxZ = box.maxZ() + 1 - cameraPos.z;

        DrawUtil.drawOutlinedBox(vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);
        matrices.popPose();
    }
}
