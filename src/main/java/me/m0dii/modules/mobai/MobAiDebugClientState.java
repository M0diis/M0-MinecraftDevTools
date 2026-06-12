package me.m0dii.modules.mobai;

import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MobAiDebugClientState {
    private static final long OVERLAY_TTL_MS = 120_000L;

    private static final TrackerConfig DEFAULT_TRACKER_CONFIG = new TrackerConfig(Set.of(), true, 144, 48, "all");

    private static InspectState inspectState;
    private static PathPreviewState pathPreviewState;
    private static TrackerConfig trackerConfig = DEFAULT_TRACKER_CONFIG;

    private MobAiDebugClientState() {
    }

    static synchronized void setInspect(int entityId, List<MobAiDebugPayloads.DebugLine> lines) {
        inspectState = new InspectState(entityId, List.copyOf(lines), Util.getMeasuringTimeMs() + OVERLAY_TTL_MS);
    }

    static synchronized void setPathPreview(MobAiDebugPayloads.PathPreviewPayload payload) {
        pathPreviewState = new PathPreviewState(
                payload.entityId(),
                payload.target(),
                payload.pathFound(),
                payload.reachesTarget(),
                payload.currentNodeIndex(),
                payload.manhattanDistance(),
                List.copyOf(payload.nodes()),
                List.copyOf(payload.openNodes()),
                List.copyOf(payload.closedNodes()),
                List.copyOf(payload.lines()),
                Util.getMeasuringTimeMs() + OVERLAY_TTL_MS
        );
    }

    static synchronized void clear(boolean clearInspect, boolean clearPathPreview) {
        if (clearInspect) {
            inspectState = null;
        }
        if (clearPathPreview) {
            pathPreviewState = null;
        }
    }

    static synchronized void setTrackerConfig(List<String> enabledDisplays,
                                              boolean showBoxes,
                                              int alpha,
                                              int radius,
                                              String hostileFocus) {
        trackerConfig = new TrackerConfig(new LinkedHashSet<>(enabledDisplays),
                showBoxes,
                Math.clamp(alpha, 0, 255),
                Math.clamp(radius, 8, 256),
                hostileFocus == null || hostileFocus.isBlank() ? "all" : hostileFocus);
    }

    static synchronized boolean hasActiveData() {
        pruneExpired();
        return inspectState != null || pathPreviewState != null;
    }

    static synchronized InspectState getInspectState() {
        pruneExpired();
        return inspectState;
    }

    static synchronized PathPreviewState getPathPreviewState() {
        pruneExpired();
        return pathPreviewState;
    }

    static synchronized TrackerConfig getTrackerConfig() {
        return trackerConfig;
    }

    static synchronized boolean requiresServerDebugSubscriptions() {
        return trackerConfig != null && trackerConfig.isActive();
    }

    private static void pruneExpired() {
        long now = Util.getMeasuringTimeMs();
        if (inspectState != null && inspectState.expiresAtMs() < now) {
            inspectState = null;
        }
        if (pathPreviewState != null && pathPreviewState.expiresAtMs() < now) {
            pathPreviewState = null;
        }
    }

    record InspectState(int entityId, List<MobAiDebugPayloads.DebugLine> lines, long expiresAtMs) {
    }

    record PathPreviewState(int entityId,
                            BlockPos target,
                            boolean pathFound,
                            boolean reachesTarget,
                            int currentNodeIndex,
                            int manhattanDistance,
                            List<BlockPos> nodes,
                            List<BlockPos> openNodes,
                            List<BlockPos> closedNodes,
                            List<MobAiDebugPayloads.DebugLine> lines,
                            long expiresAtMs) {
    }

    record TrackerConfig(Set<String> enabledDisplays,
                         boolean showBoxes,
                         int alpha,
                         int radius,
                         String hostileFocus) {
        TrackerConfig {
            enabledDisplays = Set.copyOf(enabledDisplays);
            hostileFocus = hostileFocus == null || hostileFocus.isBlank() ? "all" : hostileFocus;
        }

        boolean isActive() {
            return !enabledDisplays.isEmpty();
        }

        boolean hasDisplay(String key) {
            return key != null && enabledDisplays.contains(key);
        }
    }
}
