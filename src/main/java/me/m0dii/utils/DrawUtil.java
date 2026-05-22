package me.m0dii.utils;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public final class DrawUtil {
    private DrawUtil() {
    }

    public static void drawLine(VertexConsumer buffer,
                                double x1,
                                double y1,
                                double z1,
                                double x2,
                                double y2,
                                double z2,
                                float r,
                                float g,
                                float b,
                                float a) {
        drawLine(buffer, x1, y1, z1, x2, y2, z2, r, g, b, a, 1.0f);
    }

    public static void drawLine(VertexConsumer buffer,
                                double x1,
                                double y1,
                                double z1,
                                double x2,
                                double y2,
                                double z2,
                                float r,
                                float g,
                                float b,
                                float a,
                                float width) {
        buffer.vertex((float) x1, (float) y1, (float) z1).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f).lineWidth(width);
        buffer.vertex((float) x2, (float) y2, (float) z2).color(r, g, b, a).normal(0.0f, 1.0f, 0.0f).lineWidth(width);
    }

    public static void drawLine(VertexConsumer buffer,
                                Matrix4f matrix,
                                float x1,
                                float y1,
                                float z1,
                                float x2,
                                float y2,
                                float z2,
                                float r,
                                float g,
                                float b,
                                float a,
                                float nx,
                                float ny,
                                float nz,
                                float width) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz).lineWidth(width);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(nx, ny, nz).lineWidth(width);
    }

    public static void drawLineSafe(VertexConsumer buffer,
                                    double x1,
                                    double y1,
                                    double z1,
                                    double x2,
                                    double y2,
                                    double z2,
                                    float r,
                                    float g,
                                    float b,
                                    float a,
                                    float width) {
        try {
            drawLine(buffer, x1, y1, z1, x2, y2, z2, r, g, b, a, width);
        } catch (IllegalStateException ignored) {
            // Optional pass: skip if this buffer is not building in this phase.
        }
    }

    public static void drawLineSafe(VertexConsumer buffer,
                                    double x1,
                                    double y1,
                                    double z1,
                                    double x2,
                                    double y2,
                                    double z2,
                                    float r,
                                    float g,
                                    float b,
                                    float a) {
        drawLineSafe(buffer, x1, y1, z1, x2, y2, z2, r, g, b, a, 1.0f);
    }

    public static void drawOutlinedBox(VertexConsumer buffer,
                                       double minX,
                                       double minY,
                                       double minZ,
                                       double maxX,
                                       double maxY,
                                       double maxZ,
                                       float r,
                                       float g,
                                       float b,
                                       float a,
                                       float width) {
        drawLine(buffer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, width);
        drawLine(buffer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, width);
        drawLine(buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, width);
        drawLine(buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, width);

        drawLine(buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, width);
        drawLine(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, width);
        drawLine(buffer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, width);
        drawLine(buffer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, width);

        drawLine(buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, width);
        drawLine(buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, width);
        drawLine(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, width);
        drawLine(buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, width);
    }

    public static void drawOutlinedBox(VertexConsumer buffer,
                                       double minX,
                                       double minY,
                                       double minZ,
                                       double maxX,
                                       double maxY,
                                       double maxZ,
                                       float r,
                                       float g,
                                       float b,
                                       float a) {
        drawOutlinedBox(buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, 1.0f);
    }

    public static void drawOutlinedBoxSafe(VertexConsumer buffer,
                                           double minX,
                                           double minY,
                                           double minZ,
                                           double maxX,
                                           double maxY,
                                           double maxZ,
                                           float r,
                                           float g,
                                           float b,
                                           float a,
                                           float width) {
        try {
            drawOutlinedBox(buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, width);
        } catch (IllegalStateException ignored) {
            // Optional pass: skip if this buffer is not building in this phase.
        }
    }

    public static void drawOutlinedBoxSafe(VertexConsumer buffer,
                                           double minX,
                                           double minY,
                                           double minZ,
                                           double maxX,
                                           double maxY,
                                           double maxZ,
                                           float r,
                                           float g,
                                           float b,
                                           float a) {
        drawOutlinedBoxSafe(buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a, 1.0f);
    }
}

