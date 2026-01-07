package me.m0dii.modules.waypoints;

import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class WaypointRenderer {
    @Getter
    private static boolean enabled = true;
    private static final int MAX_RENDER_DISTANCE = 1000;

    private WaypointRenderer() {
        // Utility class
    }

    public static void setEnabled(boolean enabled) {
        WaypointRenderer.enabled = enabled;
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!enabled) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }

            VertexConsumerProvider vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
            }

            Camera camera = context.camera();
            Vec3d cameraPos = camera.getPos();
            String currentDimension = client.world.getRegistryKey().getValue().toString();

            List<WaypointHandler.Waypoint> waypoints = WaypointHandler.getWaypoints();
            TextRenderer textRenderer = client.textRenderer;

            for (WaypointHandler.Waypoint waypoint : waypoints) {
                if (!waypoint.dimension.equals(currentDimension)) {
                    continue;
                }

                Vec3d waypointPos = waypoint.position;
                double distance = cameraPos.distanceTo(waypointPos);

                if (distance > MAX_RENDER_DISTANCE) {
                    continue;
                }

                matrices.push();

                matrices.translate(
                        waypointPos.x - cameraPos.x,
                        waypointPos.y - cameraPos.y + 2.0,
                        waypointPos.z - cameraPos.z
                );

                // Billboard effect - rotate to face camera
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                // Scale based on distance (but keep it readable)
                float scale = (float) Math.clamp(distance * 0.003, 0.025, 0.05);
                matrices.scale(-scale, -scale, scale);

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int fullBright = 0x00F000F0; // Always visible

                String nameText = waypoint.name;
                int nameWidth = textRenderer.getWidth(nameText);
                int nameColor = 0x00FFFF; // Cyan

                textRenderer.draw(
                        nameText,
                        -nameWidth / 2f,
                        -20, // Move up from center
                        nameColor,
                        false,
                        matrix,
                        vertexConsumers,
                        TextRenderer.TextLayerType.SEE_THROUGH,
                        0,
                        fullBright
                );

                String distText = String.format("%.0fm", distance);
                int distWidth = textRenderer.getWidth(distText);

                int distColor;
                if (distance < 100) {
                    distColor = 0x00FF00; // Green
                } else if (distance < 200) {
                    distColor = 0xFFFF00; // Yellow
                } else {
                    distColor = 0xFF5555; // Red
                }

                textRenderer.draw(
                        distText,
                        -distWidth / 2f,
                        -10, // Just below name
                        distColor,
                        false, // no shadow for SEE_THROUGH
                        matrix,
                        vertexConsumers,
                        TextRenderer.TextLayerType.SEE_THROUGH,
                        0,
                        fullBright
                );

                // Render coordinates below distance (smaller text)
                matrices.push();
                matrices.scale(0.7f, 0.7f, 0.7f);
                Matrix4f coordMatrix = matrices.peek().getPositionMatrix();

                String coordText = String.format("%.0f, %.0f, %.0f", waypointPos.x, waypointPos.y, waypointPos.z);
                int coordWidth = textRenderer.getWidth(coordText);
                int coordColor = 0xAAAAAA;

                textRenderer.draw(
                        coordText,
                        -coordWidth / 2f,
                        0,
                        coordColor,
                        false,
                        coordMatrix,
                        vertexConsumers,
                        TextRenderer.TextLayerType.SEE_THROUGH,
                        0,
                        fullBright
                );

                matrices.pop();
                matrices.pop();
            }
        });
    }
}

