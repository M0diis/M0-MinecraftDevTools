package me.m0dii.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Records CPU geometry; no GPU buffers or live world objects cross render phases. */
public final class OverlayBufferSource {
    static final ThreadLocal<OverlayBufferSource> CURRENT = new ThreadLocal<>();
    private final Map<RenderType, Vertices> geometry = new LinkedHashMap<>();
    private final List<Consumer<SubmitNodeCollector>> text = new ArrayList<>();
    private List<Consumer<SubmitNodeCollector>> submissions;

    public static OverlayBufferSource current() {
        return java.util.Objects.requireNonNull(CURRENT.get(), "Outside overlay extraction");
    }

    public VertexConsumer getBuffer(RenderType type) {
        if (submissions != null) throw new IllegalStateException("Overlay frame already frozen");
        return geometry.computeIfAbsent(type, ignored -> new Vertices());
    }

    void freeze() {
        List<Consumer<SubmitNodeCollector>> result = new ArrayList<>(text);
        geometry.forEach((type, vertices) -> {
            List<Vertex> snapshot = vertices.snapshot();
            if (!snapshot.isEmpty()) result.add(collector -> collector.submitCustomGeometry(new PoseStack(), type,
                    (pose, target) -> snapshot.forEach(vertex -> vertex.write(target))));
        });
        submissions = List.copyOf(result);
        geometry.clear();
        text.clear();
    }

    void submit(SubmitNodeCollector collector) {
        for (Consumer<SubmitNodeCollector> submission : submissions) submission.accept(collector);
    }

    public static void drawText(Font font, String text, float x, float y, int color, boolean shadow,
                                Matrix4f matrix, OverlayBufferSource source, Font.DisplayMode mode, int background, int light) {
        drawText(font, Component.literal(text).getVisualOrderText(), x, y, color, shadow, matrix, source, mode, background, light);
    }

    public static void drawText(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow,
                                Matrix4f matrix, OverlayBufferSource source, Font.DisplayMode mode, int background, int light) {
        Matrix4f snapshot = new Matrix4f(matrix);
        source.text.add(collector -> {
            PoseStack pose = new PoseStack();
            pose.mulPose(snapshot);
            collector.submitText(pose, x, y, text, shadow, mode, light, color, background, 0);
        });
    }

    private record Vertex(float x, float y, float z, int color, float nx, float ny, float nz, float width) {
        void write(VertexConsumer target) {
            target.addVertex(x, y, z).setColor(color).setNormal(nx, ny, nz).setLineWidth(width);
        }
    }

    private static final class Vertices implements VertexConsumer {
        private final List<Vertex> vertices = new ArrayList<>();
        private float x, y, z, nx, ny = 1, nz, width = 1;
        private int color = -1;
        private boolean pending;
        private void finish() {
            if (pending) vertices.add(new Vertex(x, y, z, color, nx, ny, nz, width));
            pending = false;
        }
        List<Vertex> snapshot() { finish(); return List.copyOf(vertices); }
        public VertexConsumer addVertex(float x, float y, float z) {
            finish(); this.x = x; this.y = y; this.z = z; pending = true; return this;
        }
        public VertexConsumer setColor(int r, int g, int b, int a) { return setColor(a << 24 | r << 16 | g << 8 | b); }
        public VertexConsumer setColor(int color) { this.color = color; return this; }
        public VertexConsumer setNormal(float x, float y, float z) { nx = x; ny = y; nz = z; return this; }
        public VertexConsumer setLineWidth(float width) { this.width = width; return this; }
        public VertexConsumer setUv(float u, float v) { return this; }
        public VertexConsumer setUv1(int u, int v) { return this; }
        public VertexConsumer setUv2(int u, int v) { return this; }
        public VertexConsumer setUv3(float u, float v) { return this; }
    }
}
