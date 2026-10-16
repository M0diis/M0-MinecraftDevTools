package me.m0dii.modules.watson;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import me.m0dii.render.OverlayRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import me.m0dii.render.OverlayBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.*;

/**
 * Draws CoreProtect hits as block boxes + optional tracers.
 */
public final class CoreProtectRenderer {
    private static boolean registered;

    @Getter
    @Setter
    private static boolean tracersEnabled = true;

    @Getter
    @Setter
    private static boolean vectorsEnabled = true;

    @Getter
    @Setter
    private static boolean labelsEnabled = true;

    @Getter
    @Setter
    private static float outlineLineWidth = 2.5f;

    @Getter
    @Setter
    private static float vectorLineWidth = 2.0f;

    @Getter
    private static int outlineColorPresetIndex;

    @Getter
    private static int vectorColorPresetIndex;

    private static final ColorPreset[] OUTLINE_COLOR_PRESETS = new ColorPreset[]{
            new ColorPreset("Lime", 0.35f, 1.00f, 0.35f),
            new ColorPreset("Cyan", 0.25f, 0.95f, 1.00f),
            new ColorPreset("Gold", 1.00f, 0.85f, 0.25f),
            new ColorPreset("Red", 1.00f, 0.35f, 0.35f),
            new ColorPreset("Pink", 1.00f, 0.50f, 0.75f),
            new ColorPreset("Purple", 0.70f, 0.35f, 0.95f),
            new ColorPreset("White", 0.95f, 0.95f, 0.95f),
            new ColorPreset("Olive", 0.65f, 0.70f, 0.25f)
    };

    private static final ColorPreset[] VECTOR_COLOR_PRESETS = new ColorPreset[]{
            new ColorPreset("Magenta", 0.85f, 0.35f, 1.00f),
            new ColorPreset("Blue", 0.35f, 0.55f, 1.00f),
            new ColorPreset("Cyan", 0.20f, 0.95f, 0.95f),
            new ColorPreset("Orange", 1.00f, 0.55f, 0.20f),
            new ColorPreset("Teal", 0.20f, 0.80f, 0.70f),
            new ColorPreset("Rose", 1.00f, 0.40f, 0.60f),
            new ColorPreset("Lavender", 0.75f, 0.60f, 1.00f),
            new ColorPreset("Slate", 0.45f, 0.55f, 0.65f)
    };

    private CoreProtectRenderer() {
        // Utility class
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!WatsonCoreProtectModule.INSTANCE.isEnabled()) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) {
                return;
            }

            List<CoreProtectEntry> entries = CoreProtectTracker.snapshot();
            List<BlockPos> positions = CoreProtectTracker.getOrderedPositions();
            if (positions.isEmpty()) {
                return;
            }

            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }

            VertexConsumer visible = consumers.getBuffer(RenderTypes.LINES);
            VertexConsumer occluded = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);

            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            double cameraX = cameraPos.x;
            double cameraY = cameraPos.y;
            double cameraZ = cameraPos.z;
            Camera camera = context.gameRenderer().mainCamera();

            Map<BlockPos, Integer> positionNumbers = labelsEnabled
                    ? CoreProtectTracker.getPositionIndexMap()
                    : Map.of();

            Map<BlockPos, CoreProtectEntry> latestByPos = buildLatestByPosition(entries);
            List<LabelEntry> labelsToRender = labelsEnabled ? new ArrayList<>() : List.of();

            for (BlockPos pos : positions) {
                CoreProtectEntry entry = latestByPos.get(pos);
                if (entry == null) {
                    entry = new CoreProtectEntry(pos, "unknown", CoreProtectEntry.Action.UNKNOWN, 0L, "");
                }
                Integer labelIndex = labelsEnabled ? positionNumbers.get(entry.pos()) : null;
                if (labelsEnabled && labelIndex != null) {
                    labelsToRender.add(new LabelEntry(entry.pos(), labelIndex));
                }
                renderEntry(
                        visible,
                        occluded,
                        entry,
                        cameraX,
                        cameraY,
                        cameraZ
                );
            }

            if (vectorsEnabled && positions.size() > 1) {
                renderHistoryVectors(visible, occluded, entries, cameraX, cameraY, cameraZ);
            }

            if (labelsEnabled && !labelsToRender.isEmpty()) {
                OverlayBufferSource labelConsumers = OverlayBufferSource.current();
                for (LabelEntry label : labelsToRender) {
                    renderLabel(
                            context.matrices(),
                            labelConsumers,
                            client.font,
                            camera,
                            label.pos(),
                            label.index(),
                            cameraX,
                            cameraY,
                            cameraZ
                    );
                }
            }

            // Explicitly flush line buffers to ensure they are rendered in this pass.
        });
    }

    public static String cycleOutlineColorPreset() {
        outlineColorPresetIndex = (outlineColorPresetIndex + 1) % OUTLINE_COLOR_PRESETS.length;
        return getOutlineColorPresetName();
    }

    public static String cycleVectorColorPreset() {
        vectorColorPresetIndex = (vectorColorPresetIndex + 1) % VECTOR_COLOR_PRESETS.length;
        return getVectorColorPresetName();
    }

    public static String getOutlineColorPresetName() {
        return OUTLINE_COLOR_PRESETS[outlineColorPresetIndex].name();
    }

    public static String getVectorColorPresetName() {
        return VECTOR_COLOR_PRESETS[vectorColorPresetIndex].name();
    }

    private static float[] getOutlineColor() {
        return OUTLINE_COLOR_PRESETS[outlineColorPresetIndex].rgb();
    }

    private static float[] getVectorColor() {
        return VECTOR_COLOR_PRESETS[vectorColorPresetIndex].rgb();
    }

    private static Map<BlockPos, CoreProtectEntry> buildLatestByPosition(List<CoreProtectEntry> entries) {
        Map<BlockPos, CoreProtectEntry> latest = new java.util.LinkedHashMap<>();
        for (CoreProtectEntry entry : entries) {
            CoreProtectEntry existing = latest.get(entry.pos());
            if (existing == null || entry.observedAt() > existing.observedAt()) {
                latest.put(entry.pos(), entry);
            }
        }
        return latest;
    }

    private static void renderHistoryVectors(VertexConsumer visible,
                                             VertexConsumer occluded,
                                             List<CoreProtectEntry> entries,
                                             double cameraX,
                                             double cameraY,
                                             double cameraZ) {
        float[] color = getVectorColor();
        List<CoreProtectEntry> orderedEntries = new ArrayList<>(entries);
        orderedEntries.sort(Comparator.comparingLong(CoreProtectEntry::observedAt));

        LinkedHashSet<BlockPos> uniquePos = new LinkedHashSet<>();
        for (CoreProtectEntry entry : orderedEntries) {
            uniquePos.add(entry.pos());
        }

        if (uniquePos.size() <= 1) {
            return;
        }

        List<BlockPos> ordered = new ArrayList<>(uniquePos);

        BlockPos prev = ordered.getFirst();
        for (int i = 1; i < ordered.size(); i++) {
            BlockPos next = ordered.get(i);

            Vec3 a = Vec3.atCenterOf(prev);
            Vec3 b = Vec3.atCenterOf(next);

            DrawUtil.drawLineSafe(
                    visible,
                    a.x - cameraX,
                    a.y - cameraY,
                    a.z - cameraZ,
                    b.x - cameraX,
                    b.y - cameraY,
                    b.z - cameraZ,
                    color[0],
                    color[1],
                    color[2],
                    1.00f,
                    vectorLineWidth
            );
            DrawUtil.drawLineSafe(
                    occluded,
                    a.x - cameraX,
                    a.y - cameraY,
                    a.z - cameraZ,
                    b.x - cameraX,
                    b.y - cameraY,
                    b.z - cameraZ,
                    color[0],
                    color[1],
                    color[2],
                    0.85f,
                    vectorLineWidth + 0.5f
            );

            prev = next;
        }
    }

    private static void renderEntry(VertexConsumer visible,
                                    VertexConsumer occluded,
                                    CoreProtectEntry entry,
                                    double cameraX,
                                    double cameraY,
                                    double cameraZ) {
        BlockPos pos = entry.pos();

        double minX = pos.getX() - cameraX - 0.003;
        double minY = pos.getY() - cameraY - 0.003;
        double minZ = pos.getZ() - cameraZ - 0.003;
        double maxX = minX + 1.006;
        double maxY = minY + 1.006;
        double maxZ = minZ + 1.006;

        float[] color = getOutlineColor();
        // Draw both passes strongly so outlines remain visible even through terrain.
        DrawUtil.drawOutlinedBoxSafe(occluded, minX, minY, minZ, maxX, maxY, maxZ, color[0], color[1], color[2], 1.00f, outlineLineWidth + 0.5f);
        DrawUtil.drawOutlinedBoxSafe(visible, minX, minY, minZ, maxX, maxY, maxZ, color[0], color[1], color[2], 1.00f, outlineLineWidth);

        if (!tracersEnabled) {
            return;
        }

        double cx = minX + 0.5;
        double cy = minY + 0.5;
        double cz = minZ + 0.5;

        DrawUtil.drawLineSafe(visible, 0.0, 0.0, 0.0, cx, cy, cz, color[0], color[1], color[2], 1.00f, vectorLineWidth);
        DrawUtil.drawLineSafe(occluded, 0.0, 0.0, 0.0, cx, cy, cz, color[0], color[1], color[2], 0.85f, vectorLineWidth + 0.5f);
    }

    private static void renderLabel(PoseStack matrices,
                                    OverlayBufferSource consumers,
                                    Font textRenderer,
                                    Camera camera,
                                    BlockPos pos,
                                    int number,
                                    double cameraX,
                                    double cameraY,
                                    double cameraZ) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = center.x - cameraX;
        double dy = center.y - cameraY;
        double dz = center.z - cameraZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        matrices.pushPose();
        matrices.translate(dx, dy, dz);
        matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));

        float scale = (float) Math.clamp(distance * 0.003f, 0.018f, 0.045f);
        matrices.scale(-scale, -scale, scale);

        String text = "[" + number + "]";
        int width = textRenderer.width(text);
        Matrix4f matrix = matrices.last().pose();
        OverlayBufferSource.drawText(textRenderer, 
                text,
                -width / 2f,
                -4,
                0xFFFFFFFF,
                false,
                matrix,
                consumers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                0x00F000F0
        );

        matrices.popPose();
    }


    private record ColorPreset(String name, float r, float g, float b) {
        private float[] rgb() {
            return new float[]{r, g, b};
        }
    }

    private record LabelEntry(BlockPos pos, int index) {
    }
}

