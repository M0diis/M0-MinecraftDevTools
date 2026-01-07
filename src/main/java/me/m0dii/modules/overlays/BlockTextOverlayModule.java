package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Base class for overlay modules that render text on blocks in the world.
 */
public abstract class BlockTextOverlayModule extends Module {

    protected BlockTextOverlayModule(String id, String displayName, boolean defaultEnabled) {
        super(id, displayName, defaultEnabled);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());
    }

    /**
     * Get the rendering event handler.
     */
    private WorldRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }

            TextRenderer textRenderer = getClient().textRenderer;
            Vec3d cameraPos = context.camera().getPos();
            BlockPos playerPos = getClient().player.getBlockPos();

            VertexConsumerProvider vcp = context.consumers();
            if (vcp == null) {
                vcp = getClient().getBufferBuilders().getEntityVertexConsumers();
            }

            int xzRadius = getXZRadius();
            int yRadius = getYRadius();

            for (int x = -xzRadius; x <= xzRadius; x++) {
                for (int y = -yRadius; y <= yRadius; y++) {
                    for (int z = -xzRadius; z <= xzRadius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);

                        TextRenderInfo renderInfo = shouldRender(pos, cameraPos);
                        if (renderInfo == null) {
                            continue;
                        }

                        Vec3d renderPos = getRenderPosition(pos, renderInfo);
                        double distSq = cameraPos.squaredDistanceTo(renderPos);
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
    protected void renderText(MatrixStack matrices,
                              TextRenderer textRenderer,
                              VertexConsumerProvider vcp,
                              Vec3d cameraPos,
                              Vec3d renderPos,
                              TextRenderInfo renderInfo) {
        matrices.push();

        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderInfo.rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderInfo.rotationY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(renderInfo.rotationZ));

        float scale = renderInfo.scale;
        matrices.scale(scale, scale, scale);

        String text = renderInfo.text;
        int textWidth = textRenderer.getWidth(text);
        Matrix4f model = matrices.peek().getPositionMatrix();
        int brightness = renderInfo.brightness;

        textRenderer.draw(text,
                -textWidth / 2f,
                0,
                renderInfo.color,
                true,
                model,
                vcp,
                TextRenderer.TextLayerType.POLYGON_OFFSET,
                0,
                brightness);

        matrices.pop();
    }

    /**
     * Get the render position for the given block position.
     * Override to customize where text appears relative to the block.
     */
    protected Vec3d getRenderPosition(BlockPos pos, TextRenderInfo renderInfo) {
        return new Vec3d(
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
    protected abstract TextRenderInfo shouldRender(BlockPos pos, Vec3d cameraPos);

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
}

