package me.m0dii.modules;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Base class for modules that display floating text when targeting specific blocks.
 */
public abstract class BlockTargetTextModule extends Module {

    protected BlockTargetTextModule(String id, String displayName, boolean defaultEnabled) {
        super(id, displayName, defaultEnabled);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::onAfterEntities);
    }

    private void onAfterEntities(WorldRenderContext context) {
        if (!isEnabled() || isClientNull()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();

        if (matrices == null || camera == null) {
            return;
        }

        HitResult hitResult = getClient().crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos();
        BlockState blockState = getWorld().getBlockState(targetPos);

        if (!shouldRenderFor(targetPos, blockState)) {
            return;
        }

        renderText(matrices, camera, targetPos, blockState);
    }

    /**
     * Check if text should be rendered for the given block.
     *
     * @param pos        The block position
     * @param blockState The block state
     * @return true if text should be rendered
     */
    protected abstract boolean shouldRenderFor(@NotNull BlockPos pos, @NotNull BlockState blockState);

    /**
     * Render the text for the targeted block.
     *
     * @param matrices   The matrix stack
     * @param camera     The camera
     * @param pos        The block position
     * @param blockState The block state
     */
    protected abstract void renderText(@NotNull MatrixStack matrices,
                                       @NotNull Camera camera,
                                       @NotNull BlockPos pos,
                                       @NotNull BlockState blockState);

    /**
     * Render floating text at a specific world position.
     *
     * @param matrices  The matrix stack
     * @param worldPos  The world position to render at
     * @param cameraPos The camera position
     * @param text      The text to render
     * @param camera    The camera
     * @param color     The text color (ARGB format)
     * @param scale     The text scale
     */
    protected void renderFloatingText(@NotNull MatrixStack matrices,
                                      @NotNull Vec3d worldPos,
                                      @NotNull Vec3d cameraPos,
                                      @Nullable String text,
                                      @NotNull Camera camera,
                                      int color,
                                      float scale) {
        if (text == null || text.isEmpty()) {
            return;
        }

        matrices.push();

        // Translate to world position
        matrices.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z
        );

        // Face the camera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        // Apply scale (negative to flip text)
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider.Immediate immediate = getClient().getBufferBuilders().getEntityVertexConsumers();

        // Center the text
        float textWidth = getClient().textRenderer.getWidth(text);
        float x = -textWidth / 2f;

        // Full brightness so text is always visible
        int fullBright = 0x00F000F0;

        getClient().textRenderer.draw(
                text,
                x,
                0,
                color,
                false,
                matrix,
                immediate,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                fullBright
        );

        immediate.draw();

        matrices.pop();
    }

    /**
     * Render floating text at a specific world position with default settings.
     *
     * @param matrices  The matrix stack
     * @param worldPos  The world position to render at
     * @param cameraPos The camera position
     * @param text      The text to render
     * @param camera    The camera
     */
    protected void renderFloatingText(@NotNull MatrixStack matrices,
                                      @NotNull Vec3d worldPos,
                                      @NotNull Vec3d cameraPos,
                                      @Nullable String text,
                                      @NotNull Camera camera) {
        renderFloatingText(matrices, worldPos, cameraPos, text, camera, 0xFFFFFF, 0.025f);
    }

    /**
     * Get the default render position above a block.
     *
     * @param pos            The block position
     * @param verticalOffset The vertical offset from block center
     * @return The world position to render at
     */
    protected Vec3d getBlockCenterWithVerticalOffset(BlockPos pos, double verticalOffset) {
        return Vec3d.of(pos).add(0.5, 0.5 + verticalOffset, 0.5);
    }
}

