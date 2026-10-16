package me.m0dii.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.render.OverlayRenderEvents.OverlayRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        OverlayRenderEvents.AFTER_ENTITIES.register(this::onAfterEntities);
    }

    private void onAfterEntities(OverlayRenderContext context) {
        if (!isEnabled() || isClientNull()) {
            return;
        }

        PoseStack matrices = context.matrices();
        Camera camera = context.gameRenderer().mainCamera();

        if (camera == null) {
            return;
        }
        if (matrices == null) {
            matrices = new PoseStack();
        }

        HitResult hitResult = getClient().hitResult;
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
    protected abstract void renderText(@NotNull PoseStack matrices,
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
    protected void renderFloatingText(@NotNull PoseStack matrices,
                                      @NotNull Vec3 worldPos,
                                      @NotNull Vec3 cameraPos,
                                      @Nullable String text,
                                      @NotNull Camera camera,
                                      int color,
                                      float scale) {
        if (text == null || text.isEmpty()) {
            return;
        }

        matrices.pushPose();

        // Translate to world position
        matrices.translate(
                worldPos.x - cameraPos.x,
                worldPos.y - cameraPos.y,
                worldPos.z - cameraPos.z
        );

        // Face the camera
        matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));

        // Apply scale (negative to flip text)
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix = matrices.last().pose();
        OverlayBufferSource immediate = OverlayBufferSource.current();

        // Center the text
        float textWidth = getClient().font.width(text);
        float x = -textWidth / 2f;

        // Full brightness so text is always visible
        int fullBright = 0x00F000F0;

        OverlayBufferSource.drawText(getClient().font, 
                text,
                x,
                0,
                toOpaqueColor(color),
                false,
                matrix,
                immediate,
                Font.DisplayMode.SEE_THROUGH,
                0,
                fullBright
        );


        matrices.popPose();
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
    protected void renderFloatingText(@NotNull PoseStack matrices,
                                      @NotNull Vec3 worldPos,
                                      @NotNull Vec3 cameraPos,
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
    protected Vec3 getBlockCenterWithVerticalOffset(BlockPos pos, double verticalOffset) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 0.5 + verticalOffset, 0.5);
    }

    private static int toOpaqueColor(int color) {
        if ((color & 0xFF000000) == 0) {
            return color | 0xFF000000;
        }
        return color;
    }
}

