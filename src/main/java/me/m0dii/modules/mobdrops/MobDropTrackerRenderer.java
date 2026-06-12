package me.m0dii.modules.mobdrops;

import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public final class MobDropTrackerRenderer {
    private static boolean registered;

    private MobDropTrackerRenderer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            List<MobDropTrackerClientState.TrackerEntry> trackers = MobDropTrackerClientState.visibleTrackers(
                    client.world.getRegistryKey().getValue().toString());
            if (trackers.isEmpty()) {
                return;
            }

            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) {
                consumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            VertexConsumer visible = consumers.getBuffer(RenderLayers.LINES);
            VertexConsumer through = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

            for (MobDropTrackerClientState.TrackerEntry tracker : trackers) {
                drawTrackerBounds(visible, through, cameraPos, tracker);
            }

            renderLabels(context.matrices(), consumers, context.gameRenderer().getCamera(), cameraPos, client.textRenderer, trackers);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayers.LINES);
                immediate.draw(CustomRenderLayers.LINES_NO_DEPTH);
            }
        });
    }

    private static void drawTrackerBounds(VertexConsumer visible,
                                          VertexConsumer through,
                                          Vec3d cameraPos,
                                          MobDropTrackerClientState.TrackerEntry tracker) {
        boolean container = "container".equalsIgnoreCase(tracker.kind());
        float[] visibleColor = trackerColor(tracker.name(), container ? 0.95f : 0.92f);
        float[] throughColor = trackerColor(tracker.name(), container ? 0.36f : 0.32f);
        float lineWidth = container ? 2.8f : 2.35f;

        DrawUtil.drawOutlinedBoxSafe(visible,
                tracker.min().getX() - cameraPos.x,
                tracker.min().getY() - cameraPos.y,
                tracker.min().getZ() - cameraPos.z,
                tracker.max().getX() + 1.0 - cameraPos.x,
                tracker.max().getY() + 1.0 - cameraPos.y,
                tracker.max().getZ() + 1.0 - cameraPos.z,
                visibleColor[0], visibleColor[1], visibleColor[2], visibleColor[3], lineWidth);
        DrawUtil.drawOutlinedBoxSafe(through,
                tracker.min().getX() - cameraPos.x,
                tracker.min().getY() - cameraPos.y,
                tracker.min().getZ() - cameraPos.z,
                tracker.max().getX() + 1.0 - cameraPos.x,
                tracker.max().getY() + 1.0 - cameraPos.y,
                tracker.max().getZ() + 1.0 - cameraPos.z,
                throughColor[0], throughColor[1], throughColor[2], throughColor[3], lineWidth);

        DrawUtil.drawOutlinedBoxSafe(visible,
                tracker.anchor().getX() - cameraPos.x,
                tracker.anchor().getY() - cameraPos.y,
                tracker.anchor().getZ() - cameraPos.z,
                tracker.anchor().getX() + 1.0 - cameraPos.x,
                tracker.anchor().getY() + 1.0 - cameraPos.y,
                tracker.anchor().getZ() + 1.0 - cameraPos.z,
                1.0f, 1.0f, 1.0f, 0.98f, container ? 3.1f : 2.7f);
        DrawUtil.drawOutlinedBoxSafe(through,
                tracker.anchor().getX() - cameraPos.x,
                tracker.anchor().getY() - cameraPos.y,
                tracker.anchor().getZ() - cameraPos.z,
                tracker.anchor().getX() + 1.0 - cameraPos.x,
                tracker.anchor().getY() + 1.0 - cameraPos.y,
                tracker.anchor().getZ() + 1.0 - cameraPos.z,
                1.0f, 1.0f, 1.0f, 0.34f, container ? 3.1f : 2.7f);
    }

    private static void renderLabels(MatrixStack matrices,
                                     VertexConsumerProvider consumers,
                                     net.minecraft.client.render.Camera camera,
                                     Vec3d cameraPos,
                                     TextRenderer textRenderer,
                                     List<MobDropTrackerClientState.TrackerEntry> trackers) {
        if (matrices == null) {
            return;
        }

        for (MobDropTrackerClientState.TrackerEntry tracker : trackers) {
            Vec3d labelPos = new Vec3d(
                    tracker.anchor().getX() + 0.5,
                    tracker.anchor().getY() + 1.25,
                    tracker.anchor().getZ() + 0.5
            );
            double distance = cameraPos.distanceTo(labelPos);
            if (distance > 192.0) {
                continue;
            }

            matrices.push();
            matrices.translate(labelPos.x - cameraPos.x, labelPos.y - cameraPos.y, labelPos.z - cameraPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            float scale = (float) Math.clamp(distance * 0.0025, 0.018, 0.046);
            matrices.scale(-scale, -scale, scale);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            String line1 = tracker.name() + " [" + tracker.kind() + "]";
            String line2 = "container".equalsIgnoreCase(tracker.kind())
                    ? "items " + tracker.totalItems() + " | dpm " + tracker.dropsPerMinute()
                    : "kills " + tracker.killCount() + " | items " + tracker.totalItems() + " | dpm " + tracker.dropsPerMinute();
            String line3 = tracker.itemCounts().isEmpty()
                    ? "no drops yet"
                    : MobDropTrackerClientState.topSummary(tracker.name());

            drawLine(textRenderer, consumers, matrix, line1, 0, 0xFFFFFFFF);
            drawLine(textRenderer, consumers, matrix, line2, 10, 0xFFD2F7FF);
            drawLine(textRenderer, consumers, matrix, line3, 20, 0xFFFFE38A);
            matrices.pop();
        }
    }

    private static void drawLine(TextRenderer textRenderer,
                                 VertexConsumerProvider consumers,
                                 Matrix4f matrix,
                                 String text,
                                 int y,
                                 int color) {
        int width = textRenderer.getWidth(text);
        textRenderer.draw(text,
                -width / 2f,
                y,
                color,
                false,
                matrix,
                consumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                0x00F000F0);
    }

    private static float[] trackerColor(String name, float alpha) {
        float hue = Math.floorMod(name == null ? 0 : name.hashCode(), 360) / 360.0f;
        int rgb = Color.HSBtoRGB(hue, 0.55f, 1.0f);
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f,
                alpha
        };
    }
}
