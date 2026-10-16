package me.m0dii.modules.overlays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.m0dii.modules.Module;
import me.m0dii.render.OverlayRenderEvents;
import net.minecraft.client.gui.Font;
import me.m0dii.render.OverlayBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Base class for overlay modules that render text on blocks in the world.
 */
public abstract class BlockTextOverlayModule extends Module {

    protected BlockTextOverlayModule(@NotNull String id, @NotNull String displayName, boolean defaultEnabled) {
        super(id, displayName, defaultEnabled);
    }

    @Override
    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
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
            BlockPos playerPos = getClient().player.blockPosition();

            OverlayBufferSource vcp = context.consumers();
            if (vcp == null) {
                vcp = OverlayBufferSource.current();
            }

            int xzRadius = getXZRadius();
            int yRadius = getYRadius();

            for (int x = -xzRadius; x <= xzRadius; x++) {
                for (int y = -yRadius; y <= yRadius; y++) {
                    for (int z = -xzRadius; z <= xzRadius; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);

                        TextRenderInfo renderInfo = shouldRender(pos, cameraPos);
                        if (renderInfo == null) {
                            continue;
                        }

                        Vec3 renderPos = getRenderPosition(pos, renderInfo);
                        double distSq = cameraPos.distanceToSqr(renderPos);
                        if (distSq > xzRadius * xzRadius) {
                            continue;
                        }

                        renderText(matrices, textRenderer, vcp, cameraPos, renderPos, renderInfo);
                    }
                }
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
                              Vec3 renderPos,
                              TextRenderInfo renderInfo) {
        matrices.pushPose();

        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);

        // Keep text aligned to the top face of the block.
        matrices.rotate(Axis.XP.rotationDegrees(renderInfo.rotationX));
        matrices.rotate(Axis.YP.rotationDegrees(renderInfo.rotationY));
        matrices.rotate(Axis.ZP.rotationDegrees(renderInfo.rotationZ));

        float scale = renderInfo.scale;
        matrices.scale(scale, scale, scale);

        String text = renderInfo.text;
        int textWidth = textRenderer.width(text);
        Matrix4f model = matrices.last().pose();
        int brightness = renderInfo.brightness;
        int textColor = toOpaqueColor(renderInfo.color);

        OverlayBufferSource.drawText(textRenderer, text,
                -textWidth / 2f,
                0,
                textColor,
                true,
                model,
                vcp,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
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
            float rotationZ
    ) {
        public TextRenderInfo(String text, int color) {
            this(text, color, 0x00F000F0, 0.5, 1.005, 0.5, 0.04F, 90f, 0f, 0f);
        }

        public TextRenderInfo(String text, int color, double offsetX, double offsetY, double offsetZ) {
            this(text, color, 0x00F000F0, offsetX, offsetY, offsetZ, 0.04F, 90f, 0f, 0f);
        }

    }

    private static int toOpaqueColor(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }
}

