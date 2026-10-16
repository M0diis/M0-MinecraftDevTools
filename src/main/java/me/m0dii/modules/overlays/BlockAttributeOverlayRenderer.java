package me.m0dii.modules.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Renders helper outlines for hidden utility blocks when enabled in configuration.
 */
public final class BlockAttributeOverlayRenderer {
    private static final int SEARCH_RADIUS = 12;
    private static final int COLLISION_RADIUS = 8;

    private BlockAttributeOverlayRenderer() {
    }

    public static void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!ModConfig.blockAttributesShowCollisionMesh
                    && !ModConfig.blockAttributesShowLightBlocks
                    && !ModConfig.blockAttributesShowBarrierBlocks) {
                return;
            }
            if (context.gameRenderer() == null || context.gameRenderer().mainCamera() == null) {
                return;
            }

            PoseStack matrices = context.matrices();
            OverlayBufferSource consumers = context.consumers();
            if (matrices == null || consumers == null) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }

            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);

            BlockPos center = client.player.blockPosition();
            int minY = Math.max(client.level.getMinY(), center.getY() - 6);
            int maxY = Math.min(client.level.getMaxY(), center.getY() + 6);

            if (ModConfig.blockAttributesShowCollisionMesh) {
                for (int x = center.getX() - COLLISION_RADIUS; x <= center.getX() + COLLISION_RADIUS; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = center.getZ() - COLLISION_RADIUS; z <= center.getZ() + COLLISION_RADIUS; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            var state = client.level.getBlockState(pos);
                            if (state.isAir()) {
                                continue;
                            }

                            VoxelShape collision = state.getCollisionShape(client.level, pos);
                            if (collision.isEmpty()) {
                                continue;
                            }
                            if (!Shapes.joinIsNotEmpty(collision, Shapes.block(), BooleanOp.NOT_SAME)) {
                                continue;
                            }

                            for (AABB box : collision.toAabbs()) {
                                double minX = pos.getX() + box.minX - cameraPos.x + 0.001;
                                double minBlockY = pos.getY() + box.minY - cameraPos.y + 0.001;
                                double minZ = pos.getZ() + box.minZ - cameraPos.z + 0.001;
                                double maxX = pos.getX() + box.maxX - cameraPos.x - 0.001;
                                double maxBlockY = pos.getY() + box.maxY - cameraPos.y - 0.001;
                                double maxZ = pos.getZ() + box.maxZ - cameraPos.z - 0.001;
                                DrawUtil.drawOutlinedBox(lines, minX, minBlockY, minZ, maxX, maxBlockY, maxZ,
                                        0.35f, 0.95f, 1.0f, 0.88f, 1.1f);
                            }
                        }
                    }
                }
            }

            if (!ModConfig.blockAttributesShowLightBlocks && !ModConfig.blockAttributesShowBarrierBlocks) {
                return;
            }

            for (int x = center.getX() - SEARCH_RADIUS; x <= center.getX() + SEARCH_RADIUS; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = center.getZ() - SEARCH_RADIUS; z <= center.getZ() + SEARCH_RADIUS; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = client.level.getBlockState(pos);

                        boolean isLight = ModConfig.blockAttributesShowLightBlocks && state.is(Blocks.LIGHT);
                        boolean isBarrier = ModConfig.blockAttributesShowBarrierBlocks && state.is(Blocks.BARRIER);
                        if (!isLight && !isBarrier) {
                            continue;
                        }

                        float r = isLight ? 1.0f : 0.95f;
                        float g = isLight ? 0.95f : 0.1f;
                        float b = isLight ? 0.2f : 0.1f;
                        float a = 0.85f;

                        double minX = pos.getX() - cameraPos.x + 0.002;
                        double minBlockY = pos.getY() - cameraPos.y + 0.002;
                        double minZ = pos.getZ() - cameraPos.z + 0.002;
                        double maxX = pos.getX() + 1.0 - cameraPos.x - 0.002;
                        double maxBlockY = pos.getY() + 1.0 - cameraPos.y - 0.002;
                        double maxZ = pos.getZ() + 1.0 - cameraPos.z - 0.002;

                        DrawUtil.drawOutlinedBox(lines, minX, minBlockY, minZ, maxX, maxBlockY, maxZ, r, g, b, a, 1.25f);
                    }
                }
            }
        });
    }
}

