package me.m0dii.modules.overlays;

import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Renders helper outlines for hidden utility blocks when enabled in configuration.
 */
public final class BlockAttributeOverlayRenderer {
    private static final int SEARCH_RADIUS = 12;
    private static final int COLLISION_RADIUS = 8;

    private BlockAttributeOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!ModConfig.blockAttributesShowCollisionMesh
                    && !ModConfig.blockAttributesShowLightBlocks
                    && !ModConfig.blockAttributesShowBarrierBlocks) {
                return;
            }
            if (context.gameRenderer() == null || context.gameRenderer().getCamera() == null) {
                return;
            }

            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
            VertexConsumer lines = consumers.getBuffer(RenderLayers.LINES);

            BlockPos center = client.player.getBlockPos();
            int minY = Math.max(client.world.getBottomY(), center.getY() - 6);
            int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + 6);

            if (ModConfig.blockAttributesShowCollisionMesh) {
                for (int x = center.getX() - COLLISION_RADIUS; x <= center.getX() + COLLISION_RADIUS; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = center.getZ() - COLLISION_RADIUS; z <= center.getZ() + COLLISION_RADIUS; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            var state = client.world.getBlockState(pos);
                            if (state.isAir()) {
                                continue;
                            }

                            VoxelShape collision = state.getCollisionShape(client.world, pos);
                            if (collision.isEmpty()) {
                                continue;
                            }
                            if (!VoxelShapes.matchesAnywhere(collision, VoxelShapes.fullCube(), BooleanBiFunction.NOT_SAME)) {
                                continue;
                            }

                            for (Box box : collision.getBoundingBoxes()) {
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
                        var state = client.world.getBlockState(pos);

                        boolean isLight = ModConfig.blockAttributesShowLightBlocks && state.isOf(Blocks.LIGHT);
                        boolean isBarrier = ModConfig.blockAttributesShowBarrierBlocks && state.isOf(Blocks.BARRIER);
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

