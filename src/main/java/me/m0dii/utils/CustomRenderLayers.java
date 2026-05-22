package me.m0dii.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

/**
 * Custom render layers used for special rendering effects.
 */
public final class CustomRenderLayers {

    private CustomRenderLayers() {
    }

    /**
     * Like {@link net.minecraft.client.render.RenderLayers#LINES} but with
     * {@link DepthTestFunction#NO_DEPTH_TEST}, so lines are rendered through
     * solid blocks (see-through / occluded pass).
     */
    public static final RenderLayer LINES_NO_DEPTH;

    static {
        RenderPipeline linesNoDepthPipeline = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                        .withLocation("m0-dev-tools-rendertype_lines_no_depth")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        RenderSetup setup = RenderSetup.builder(linesNoDepthPipeline)
                .translucent()
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .expectedBufferSize(1536)
                .build();

        LINES_NO_DEPTH = RenderLayer.of("m0-dev-tools-lines_no_depth", setup);
    }
}

