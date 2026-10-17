package me.m0dii.modules.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders helper outlines for hidden utility blocks when enabled in configuration.
 * <p>
 * The block scan runs on the client tick (at most every {@link #RESCAN_INTERVAL_TICKS} ticks, or right away when the
 * player moves to another block or the settings change); each frame only draws the cached boxes.
 */
public final class BlockAttributeOverlayRenderer {
    private static final int SEARCH_RADIUS = 12;
    private static final int COLLISION_RADIUS = 8;
    private static final int RESCAN_INTERVAL_TICKS = 5;

    /** A box in world coordinates with its outline colour. */
    private record OutlineBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                              float r, float g, float b, float a, float width) {
    }

    private static List<OutlineBox> cachedBoxes = List.of();
    private static BlockPos lastCenter;
    private static Object lastLevel;
    private static int lastFlags = -1;
    private static int ticksSinceScan;

    private BlockAttributeOverlayRenderer() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BlockAttributeOverlayRenderer::tickScan);

        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (currentFlags() == 0 || cachedBoxes.isEmpty()) {
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

            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
            for (OutlineBox box : cachedBoxes) {
                DrawUtil.drawOutlinedBox(lines,
                        box.minX() - cameraPos.x, box.minY() - cameraPos.y, box.minZ() - cameraPos.z,
                        box.maxX() - cameraPos.x, box.maxY() - cameraPos.y, box.maxZ() - cameraPos.z,
                        box.r(), box.g(), box.b(), box.a(), box.width());
            }
        });
    }

    private static int currentFlags() {
        return (ModConfig.blockAttributesShowCollisionMesh ? 1 : 0)
                | (ModConfig.blockAttributesShowLightBlocks ? 2 : 0)
                | (ModConfig.blockAttributesShowBarrierBlocks ? 4 : 0);
    }

    private static void tickScan(Minecraft client) {
        int flags = currentFlags();
        if (flags == 0 || client.player == null || client.level == null) {
            if (!cachedBoxes.isEmpty() || lastCenter != null) {
                cachedBoxes = List.of();
                lastCenter = null;
                lastLevel = null;
            }
            return;
        }

        BlockPos center = client.player.blockPosition();
        ticksSinceScan++;
        boolean stale = lastCenter == null
                || !center.equals(lastCenter)
                || client.level != lastLevel
                || flags != lastFlags
                || ticksSinceScan >= RESCAN_INTERVAL_TICKS;
        if (!stale) {
            return;
        }

        cachedBoxes = scan(client, center, flags);
        lastCenter = center;
        lastLevel = client.level;
        lastFlags = flags;
        ticksSinceScan = 0;
    }

    private static List<OutlineBox> scan(Minecraft client, BlockPos center, int flags) {
        List<OutlineBox> boxes = new ArrayList<>();
        int minY = Math.max(client.level.getMinY(), center.getY() - 6);
        int maxY = Math.min(client.level.getMaxY(), center.getY() + 6);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if ((flags & 1) != 0) {
            for (int x = center.getX() - COLLISION_RADIUS; x <= center.getX() + COLLISION_RADIUS; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = center.getZ() - COLLISION_RADIUS; z <= center.getZ() + COLLISION_RADIUS; z++) {
                        pos.set(x, y, z);
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
                            boxes.add(new OutlineBox(
                                    x + box.minX + 0.001, y + box.minY + 0.001, z + box.minZ + 0.001,
                                    x + box.maxX - 0.001, y + box.maxY - 0.001, z + box.maxZ - 0.001,
                                    0.35f, 0.95f, 1.0f, 0.88f, 1.1f));
                        }
                    }
                }
            }
        }

        boolean showLight = (flags & 2) != 0;
        boolean showBarrier = (flags & 4) != 0;
        if (!showLight && !showBarrier) {
            return boxes;
        }

        for (int x = center.getX() - SEARCH_RADIUS; x <= center.getX() + SEARCH_RADIUS; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = center.getZ() - SEARCH_RADIUS; z <= center.getZ() + SEARCH_RADIUS; z++) {
                    pos.set(x, y, z);
                    var state = client.level.getBlockState(pos);

                    boolean isLight = showLight && state.is(Blocks.LIGHT);
                    boolean isBarrier = showBarrier && state.is(Blocks.BARRIER);
                    if (!isLight && !isBarrier) {
                        continue;
                    }

                    float r = isLight ? 1.0f : 0.95f;
                    float g = isLight ? 0.95f : 0.1f;
                    float b = isLight ? 0.2f : 0.1f;
                    boxes.add(new OutlineBox(
                            x + 0.002, y + 0.002, z + 0.002,
                            x + 1.0 - 0.002, y + 1.0 - 0.002, z + 1.0 - 0.002,
                            r, g, b, 0.85f, 1.25f));
                }
            }
        }
        return boxes;
    }
}
