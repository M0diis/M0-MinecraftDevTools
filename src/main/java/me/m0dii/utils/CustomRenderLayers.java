package me.m0dii.utils;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Custom render layers used for special rendering effects.
 */
public final class CustomRenderLayers {

    private CustomRenderLayers() {
    }

    /**
     * Like {@link net.minecraft.client.renderer.rendertype.RenderTypes#LINES} but with
     * {@link DepthTestFunction#NO_DEPTH_TEST}, so lines are rendered through
     * solid blocks (see-through / occluded pass).
     */
    public static final RenderType LINES_NO_DEPTH;

    static {
        RenderPipeline linesNoDepthPipeline = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                        .withLocation("m0-dev-tools-rendertype_lines_no_depth")
                        .withColorTargetState(ColorTargetState.DEFAULT)
                        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                        .build()
        );

        // No sortOnUpload(): since 26.x only quad draws may be depth-sorted, and sorting LINES throws.
        RenderSetup setup = RenderSetup.builder(linesNoDepthPipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup();

        LINES_NO_DEPTH = RenderType.create("m0-dev-tools-lines_no_depth", setup);
    }
}

