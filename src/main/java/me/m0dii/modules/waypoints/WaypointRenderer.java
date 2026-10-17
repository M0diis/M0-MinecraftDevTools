package me.m0dii.modules.waypoints;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.Getter;
import lombok.Setter;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class WaypointRenderer {

    @Getter
    @Setter
    private static boolean enabled = true;

    private static final int MAX_RENDER_DISTANCE = 1000;

    /** Vertical beam at each waypoint so it can be found from far away. */
    @Getter
    @Setter
    private static boolean beams = true;

    /** Show overworld waypoints in the nether (coordinates ÷8) and the other way round (×8). */
    @Getter
    @Setter
    private static boolean showOtherDimension = true;

    private WaypointRenderer() {
        // Utility class
    }

    public static void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!enabled) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }

            PoseStack matrices = context.matrices();
            if (matrices == null) {
                return;
            }

            OverlayBufferSource vertexConsumers = context.consumers();
            if (vertexConsumers == null) {
                vertexConsumers = OverlayBufferSource.current();
            }

            Camera camera = context.gameRenderer().mainCamera();
            Vec3 cameraPos = camera.position();
            String currentDimension = client.level.dimension().identifier().toString();

            List<WaypointHandler.Waypoint> waypoints = WaypointHandler.getWaypoints();
            Font textRenderer = client.font;

            for (WaypointHandler.Waypoint waypoint : waypoints) {
                boolean sameDimension = waypoint.dimension.equals(currentDimension);
                if (!sameDimension && !showOtherDimension) {
                    continue;
                }
                Vec3 waypointPos = WaypointHandler.positionIn(waypoint, currentDimension);
                if (waypointPos == null) {
                    continue;
                }

                double distance = cameraPos.distanceTo(waypointPos);

                if (distance > MAX_RENDER_DISTANCE) {
                    continue;
                }

                if (beams) {
                    drawBeam(vertexConsumers, client, cameraPos, waypointPos, WaypointMath.colorFor(waypoint.id), sameDimension);
                }

                matrices.pushPose();

                matrices.translate(
                        waypointPos.x - cameraPos.x,
                        waypointPos.y - cameraPos.y + 2.0,
                        waypointPos.z - cameraPos.z
                );

                // Billboard effect - rotate to face camera
                matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
                matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));

                // Scale based on distance (but keep it readable)
                float scale = (float) Math.clamp(distance * 0.003, 0.025, 0.05);
                matrices.scale(-scale, -scale, scale);

                Matrix4f matrix = matrices.last().pose();
                int fullBright = 0x00F000F0; // Always visible

                String nameText = sameDimension ? waypoint.name
                        : waypoint.name + (WaypointMath.NETHER.equals(currentDimension) ? " (nether ÷8)" : " (overworld ×8)");
                int nameWidth = textRenderer.width(nameText);
                int nameColor = 0x00FFFF; // Cyan

                OverlayBufferSource.drawText(textRenderer, 
                        nameText,
                        -nameWidth / 2f,
                        -20, // Move up from center
                        nameColor,
                        false,
                        matrix,
                        vertexConsumers,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        fullBright
                );

                String distText = String.format("%.0fm", distance);
                int distWidth = textRenderer.width(distText);

                int distColor;
                if (distance < 100) {
                    distColor = 0x00FF00; // Green
                } else if (distance < 200) {
                    distColor = 0xFFFF00; // Yellow
                } else {
                    distColor = 0xFF5555; // Red
                }

                OverlayBufferSource.drawText(textRenderer, 
                        distText,
                        -distWidth / 2f,
                        -10, // Just below name
                        distColor,
                        false, // no shadow for SEE_THROUGH
                        matrix,
                        vertexConsumers,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        fullBright
                );

                // Render coordinates below distance (smaller text)
                matrices.pushPose();
                matrices.scale(0.7f, 0.7f, 0.7f);
                Matrix4f coordMatrix = matrices.last().pose();

                String coordText = String.format("%.0f, %.0f, %.0f", waypointPos.x, waypointPos.y, waypointPos.z);
                int coordWidth = textRenderer.width(coordText);
                int coordColor = 0xAAAAAA;

                OverlayBufferSource.drawText(textRenderer, 
                        coordText,
                        -coordWidth / 2f,
                        0,
                        coordColor,
                        false,
                        coordMatrix,
                        vertexConsumers,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        fullBright
                );

                matrices.popPose();
                matrices.popPose();
            }
        });
    }

    private static void drawBeam(OverlayBufferSource consumers, Minecraft client, Vec3 cameraPos, Vec3 pos, int rgb, boolean solid) {
        if (consumers == null) {
            return;
        }
        VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        float a = solid ? 0.9f : 0.45f;
        double half = 0.15;
        double minY = client.level.getMinY();
        double maxY = client.level.getMaxY();
        DrawUtil.drawOutlinedBoxSafe(lines,
                pos.x - half - cameraPos.x, minY - cameraPos.y, pos.z - half - cameraPos.z,
                pos.x + half - cameraPos.x, maxY - cameraPos.y, pos.z + half - cameraPos.z,
                r, g, b, a, 2.0f);
        // Ground marker so the exact spot is visible up close.
        DrawUtil.drawOutlinedBoxSafe(lines,
                Math.floor(pos.x) - cameraPos.x, Math.floor(pos.y) - cameraPos.y, Math.floor(pos.z) - cameraPos.z,
                Math.floor(pos.x) + 1 - cameraPos.x, Math.floor(pos.y) + 0.05 - cameraPos.y, Math.floor(pos.z) + 1 - cameraPos.z,
                r, g, b, a, 2.0f);
    }
}
