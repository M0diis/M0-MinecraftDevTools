package me.m0dii.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.GameRenderer;
import java.util.ArrayList;
import java.util.List;

/** Extracts overlays on the game thread and submits their snapshots on the render thread. */
public final class OverlayRenderEvents {
    private static final RenderStateDataKey<OverlayBufferSource> FRAME = RenderStateDataKey.create(() -> "m0dev overlays");
    public static final Listeners AFTER_ENTITIES = new Listeners();

    private OverlayRenderEvents() {}

    static {
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            OverlayBufferSource buffers = new OverlayBufferSource();
            OverlayRenderContext overlay = new OverlayRenderContext(new PoseStack(), buffers, context.gameRenderer());
            OverlayBufferSource.CURRENT.set(buffers);
            try {
                for (AfterEntities listener : AFTER_ENTITIES.listeners) listener.afterEntities(overlay);
                buffers.freeze();
                context.levelState().setData(FRAME, buffers);
            } finally {
                OverlayBufferSource.CURRENT.remove();
            }
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            OverlayBufferSource buffers = context.levelState().getData(FRAME);
            if (buffers != null) buffers.submit(context.submitNodeCollector());
        });
    }

    public record OverlayRenderContext(PoseStack matrices, OverlayBufferSource consumers, GameRenderer gameRenderer) {}

    @FunctionalInterface
    public interface AfterEntities {
        void afterEntities(OverlayRenderContext context);
    }

    public static final class Listeners {
        private final List<AfterEntities> listeners = new ArrayList<>();
        public void register(AfterEntities listener) { listeners.add(listener); }
    }
}
