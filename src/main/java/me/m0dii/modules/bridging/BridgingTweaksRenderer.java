package me.m0dii.modules.bridging;

import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

final class BridgingTweaksRenderer {
    private static final int[] OUTLINE_COLORS = new int[]{
            0x66000000,
            0x6699E2FF,
            0x66FFCC33,
            0x66FF6B6B,
            0x66FFFFFF,
            0x6691FF8A
    };

    private boolean registered;

    void register() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        WorldRenderEvents.AFTER_ENTITIES.register(this::onAfterEntities);
    }

    void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }
        if (!BridgingTweaksModule.INSTANCE.isEnabled() || !BridgingTweaksModule.INSTANCE.showCrosshair()) {
            return;
        }
        if (BridgingTweaksModule.INSTANCE.onlyBridgeWhenCrouched() && !client.player.isSneaking()) {
            return;
        }

        BridgingTarget target = BridgingTweaksState.getLastAssistTarget();
        if (target == null) {
            return;
        }

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;
        int color = 0xFFE6F0FF;
        drawCrosshairIndicator(context, centerX, centerY, target.supportDirection(), color);
    }

    private void onAfterEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        boolean debugHud = isDebugHudVisible(client);
        boolean bridgingActive = BridgingTweaksModule.INSTANCE.isEnabled()
                && (!BridgingTweaksModule.INSTANCE.onlyBridgeWhenCrouched() || client.player.isSneaking());
        boolean renderBridgeOutline = bridgingActive && (debugHud ? BridgingTweaksModule.INSTANCE.debugHighlight() : BridgingTweaksModule.INSTANCE.showOutline());
        boolean renderNonBridgeOutline = (debugHud ? BridgingTweaksModule.INSTANCE.debugNonBridgingHighlight() : BridgingTweaksModule.INSTANCE.showOutlineWhenNotBridging())
                && (bridgingActive || !BridgingTweaksModule.INSTANCE.nonBridgeOutlineRespectsCrouchRules());
        boolean renderTrace = debugHud && BridgingTweaksModule.INSTANCE.debugTrace();

        if (!renderBridgeOutline && !renderNonBridgeOutline && !renderTrace) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            consumers = client.getBufferBuilders().getEntityVertexConsumers();
        }

        VertexConsumer visible = consumers.getBuffer(RenderLayers.LINES);
        VertexConsumer occluded = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

        if (renderTrace) {
            List<BlockPos> path = BridgingTweaksLogic.getViewBlockPath(client, client.player, BridgingPerspective.resolve(client, client.player));
            for (BlockPos pos : path) {
                drawBox(visible, pos, cameraPos, 0.20f, 0.20f, 0.20f, 0.16f);
            }
        }

        if (renderBridgeOutline) {
            BridgingTarget target = BridgingTweaksState.getLastAssistTarget();
            if (target != null) {
                drawOutline(visible, occluded, target.placePos(), cameraPos);
            }
        }

        if (renderNonBridgeOutline) {
            BlockPos nonBridgeTarget = BridgingTweaksLogic.findNonBridgingOutlineTarget(client);
            if (nonBridgeTarget != null) {
                drawOutline(visible, occluded, nonBridgeTarget, cameraPos);
            }
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayers.LINES);
            immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
        }
    }

    static int nextOutlineColor(int current, boolean forward) {
        int index = 0;
        for (int i = 0; i < OUTLINE_COLORS.length; i++) {
            if (OUTLINE_COLORS[i] == current) {
                index = i;
                break;
            }
        }
        int next = forward ? index + 1 : index - 1;
        if (next < 0) {
            next = OUTLINE_COLORS.length - 1;
        }
        if (next >= OUTLINE_COLORS.length) {
            next = 0;
        }
        return OUTLINE_COLORS[next];
    }

    private static boolean isDebugHudVisible(MinecraftClient client) {
        try {
            return client.getDebugHud().shouldShowDebugHud();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void drawOutline(VertexConsumer visible, VertexConsumer occluded, BlockPos pos, Vec3d cameraPos) {
        int argb = ModConfig.bridgingOutlineColor;
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        drawBox(visible, pos, cameraPos, r, g, b, Math.max(a, 0.55f));
        drawBox(occluded, pos, cameraPos, r, g, b, Math.max(a * 0.6f, 0.22f));
    }

    private static void drawBox(VertexConsumer buffer, BlockPos pos, Vec3d cameraPos, float r, float g, float b, float a) {
        double minX = pos.getX() - cameraPos.x;
        double minY = pos.getY() - cameraPos.y;
        double minZ = pos.getZ() - cameraPos.z;
        DrawUtil.drawOutlinedBoxSafe(buffer, minX, minY, minZ, minX + 1.0D, minY + 1.0D, minZ + 1.0D, r, g, b, a, 1.4f);
    }

    private static void drawCrosshairIndicator(DrawContext context, int x, int y, Direction supportDirection, int color) {
        switch (supportDirection) {
            case DOWN -> drawUpMarker(context, x, y, color);
            case UP -> drawDownMarker(context, x, y, color);
            default -> drawHorizontalMarker(context, x, y, color);
        }
    }

    private static void drawUpMarker(DrawContext context, int x, int y, int color) {
        context.fill(x - 1, y - 10, x + 1, y - 4, color);
        context.fill(x - 4, y - 7, x + 4, y - 5, color);
    }

    private static void drawDownMarker(DrawContext context, int x, int y, int color) {
        context.fill(x - 1, y + 4, x + 1, y + 10, color);
        context.fill(x - 4, y + 5, x + 4, y + 7, color);
    }

    private static void drawHorizontalMarker(DrawContext context, int x, int y, int color) {
        context.fill(x - 10, y - 1, x - 4, y + 1, color);
        context.fill(x + 4, y - 1, x + 10, y + 1, color);
    }
}
