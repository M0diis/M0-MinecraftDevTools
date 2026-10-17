package me.m0dii.modules.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.m0dii.modules.Module;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for overlay modules that render text on blocks in the world.
 */
public abstract class BlockTextOverlayModule extends Module {
    /**
     * Scanning every block in range is expensive (~10k block lookups at the default radius), so the scan runs on
     * the client tick at most this often, or immediately when the player moves to another block. Rendering only
     * walks the cached results.
     */
    protected static final int RESCAN_INTERVAL_TICKS = 5;

    private record CachedText(Vec3 renderPos, TextRenderInfo info) {
    }

    private List<CachedText> cachedTexts = List.of();
    private BlockPos lastScanCenter;
    private Object lastScanLevel;
    private int lastScanXZ = -1;
    private int lastScanY = -1;
    private int ticksSinceScan;

    protected BlockTextOverlayModule(@NotNull String id, @NotNull String displayName, boolean defaultEnabled) {
        super(id, displayName, defaultEnabled);
    }

    @Override
    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
        ClientTickEvents.END_CLIENT_TICK.register(this::tickScan);
    }

    private void tickScan(Minecraft client) {
        if (!isEnabled() || isClientNull()) {
            if (!cachedTexts.isEmpty() || lastScanCenter != null) {
                cachedTexts = List.of();
                lastScanCenter = null;
                lastScanLevel = null;
            }
            return;
        }

        BlockPos center = client.player.blockPosition();
        int xzRadius = getXZRadius();
        int yRadius = getYRadius();
        ticksSinceScan++;
        boolean stale = lastScanCenter == null
                || !center.equals(lastScanCenter)
                || client.level != lastScanLevel
                || xzRadius != lastScanXZ
                || yRadius != lastScanY
                || ticksSinceScan >= RESCAN_INTERVAL_TICKS;
        if (!stale) {
            return;
        }

        // Text placement does not depend on the camera; the eye position stands in for it during the scan.
        Vec3 cameraPos = client.player.getEyePosition();
        List<CachedText> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -xzRadius; x <= xzRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xzRadius; z <= xzRadius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    TextRenderInfo renderInfo = shouldRender(cursor, cameraPos);
                    if (renderInfo == null) {
                        continue;
                    }
                    BlockPos pos = cursor.immutable();
                    found.add(new CachedText(getRenderPosition(pos, renderInfo), renderInfo));
                }
            }
        }

        cachedTexts = found;
        lastScanCenter = center;
        lastScanLevel = client.level;
        lastScanXZ = xzRadius;
        lastScanY = yRadius;
        ticksSinceScan = 0;
    }

    /**
     * Get the rendering event handler.
     */
    private OverlayRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            PoseStack matrices = context.matrices();
            if (matrices == null) {
                matrices = new PoseStack();
            }

            Font textRenderer = getClient().font;
            Vec3 cameraPos = context.gameRenderer().mainCamera().position();
            Quaternionf cameraRotation = new Quaternionf(context.gameRenderer().mainCamera().rotation());

            OverlayBufferSource vcp = context.consumers();
            if (vcp == null) {
                vcp = OverlayBufferSource.current();
            }

            int xzRadius = getXZRadius();
            double maxDistSq = (double) xzRadius * xzRadius;

            for (CachedText cached : cachedTexts) {
                if (cameraPos.distanceToSqr(cached.renderPos()) > maxDistSq) {
                    continue;
                }
                renderText(matrices, textRenderer, vcp, cameraPos, cameraRotation, cached.renderPos(), cached.info());
            }
        };
    }

    /**
     * Render text at the specified position.
     */
    protected void renderText(PoseStack matrices,
                              Font textRenderer,
                              OverlayBufferSource vcp,
                              Vec3 cameraPos,
                              Quaternionf cameraRotation,
                              Vec3 renderPos,
                              TextRenderInfo renderInfo) {
        matrices.pushPose();

        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);

        float scale = renderInfo.scale;
        if (renderInfo.billboard) {
            // Name-tag style: always faces the camera, so it reads from any angle and is never hidden in the block.
            matrices.rotate(cameraRotation);
            matrices.scale(scale, -scale, scale);
        } else {
            // Lie flat on the top face of the block.
            matrices.rotate(Axis.XP.rotationDegrees(renderInfo.rotationX));
            matrices.rotate(Axis.YP.rotationDegrees(renderInfo.rotationY));
            matrices.rotate(Axis.ZP.rotationDegrees(renderInfo.rotationZ));
            matrices.scale(scale, scale, scale);
        }

        String text = renderInfo.text;
        int textWidth = textRenderer.width(text);
        Matrix4f model = matrices.last().pose();
        int brightness = renderInfo.brightness;
        int textColor = toOpaqueColor(renderInfo.color);

        OverlayBufferSource.drawText(textRenderer, text,
                -textWidth / 2f,
                renderInfo.billboard ? -4 : 0,
                textColor,
                !renderInfo.billboard,
                model,
                vcp,
                renderInfo.billboard ? Font.DisplayMode.NORMAL : Font.DisplayMode.POLYGON_OFFSET,
                renderInfo.background,
                brightness);

        matrices.popPose();
    }

    /**
     * Get the render position for the given block position.
     * Override to customize where text appears relative to the block.
     */
    protected Vec3 getRenderPosition(BlockPos pos, TextRenderInfo renderInfo) {
        return new Vec3(
                pos.getX() + renderInfo.offsetX,
                pos.getY() + renderInfo.offsetY,
                pos.getZ() + renderInfo.offsetZ
        );
    }

    /**
     * Check if the block at the given position should be rendered and return the render info.
     * Return null if the block should not be rendered.
     *
     * @param pos       The block position to check
     * @param cameraPos The camera position
     * @return TextRenderInfo if should render, null otherwise
     */
    @Nullable
    protected abstract TextRenderInfo shouldRender(BlockPos pos, Vec3 cameraPos);

    /**
     * Get the XZ radius for scanning blocks around the player.
     */
    protected abstract int getXZRadius();

    /**
     * Get the Y radius for scanning blocks around the player.
     */
    protected abstract int getYRadius();

    /**
     * Container for text rendering information.
     */
    public record TextRenderInfo(
            String text,
            int color,
            int brightness,
            double offsetX,
            double offsetY,
            double offsetZ,
            float scale,
            float rotationX,
            float rotationY,
            float rotationZ,
            boolean billboard,
            int background
    ) {
        public TextRenderInfo(String text, int color) {
            this(text, color, 0x00F000F0, 0.5, 1.005, 0.5, 0.04F, 90f, 0f, 0f, false, 0);
        }

        public TextRenderInfo(String text, int color, double offsetX, double offsetY, double offsetZ) {
            this(text, color, 0x00F000F0, offsetX, offsetY, offsetZ, 0.04F, 90f, 0f, 0f, false, 0);
        }

        /** A label that floats at the given offset and turns to face the camera, on a dark backing. */
        public static TextRenderInfo floating(String text, int color, double offsetX, double offsetY, double offsetZ) {
            return new TextRenderInfo(text, color, 0x00F000F0, offsetX, offsetY, offsetZ, 0.025F, 0f, 0f, 0f, true, 0x90000000);
        }
    }

    private static int toOpaqueColor(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }
}

