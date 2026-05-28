package me.m0dii.modules.xray;

import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public final class XrayOutlineRenderer {
    private static boolean registered;

    private XrayOutlineRenderer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!XrayModule.INSTANCE.isEnabled()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            int searchRadius = XrayManager.getDisplayRange();

            Map<String, XrayManager.XrayBlockConfig> active = new HashMap<>();
            for (Map.Entry<String, XrayManager.XrayBlockConfig> entry : XrayManager.getXrayBlocks().entrySet()) {
                XrayManager.XrayBlockConfig cfg = entry.getValue();
                if (cfg != null && cfg.enabled) {
                    active.put(entry.getKey(), cfg);
                }
            }
            if (active.isEmpty()) {
                return;
            }

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) {
                consumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            VertexConsumer lines = consumers.getBuffer(RenderLayers.LINES);
            VertexConsumer linesNoDepth = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
            double cameraX = cameraPos.x;
            double cameraY = cameraPos.y;
            double cameraZ = cameraPos.z;
            BlockPos center = client.player.getBlockPos();

            int minY = Math.max(client.world.getBottomY(), center.getY() - searchRadius);
            int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + searchRadius);

            for (int x = center.getX() - searchRadius; x <= center.getX() + searchRadius; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = center.getZ() - searchRadius; z <= center.getZ() + searchRadius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        String id = String.valueOf(Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()));
                        XrayManager.XrayBlockConfig blockConfig = active.get(id);
                        if (blockConfig == null) {
                            continue;
                        }

                        float r = ((blockConfig.color >> 16) & 0xFF) / 255.0f;
                        float g = ((blockConfig.color >> 8) & 0xFF) / 255.0f;
                        float b = (blockConfig.color & 0xFF) / 255.0f;

                        double minX = pos.getX() - cameraX - 0.003;
                        double minBlockY = pos.getY() - cameraY - 0.003;
                        double minZ = pos.getZ() - cameraZ - 0.003;
                        double maxX = minX + 1.006;
                        double maxBlockY = minBlockY + 1.006;
                        double maxZ = minZ + 1.006;

                        DrawUtil.drawOutlinedBoxSafe(lines, minX, minBlockY, minZ, maxX, maxBlockY, maxZ, r, g, b, 1.0f, 1.5f);
                        DrawUtil.drawOutlinedBoxSafe(linesNoDepth, minX, minBlockY, minZ, maxX, maxBlockY, maxZ, r, g, b, 0.5f, 1.0f);
                    }
                }
            }

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayers.LINES);
                immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
            }
        });
    }
}
