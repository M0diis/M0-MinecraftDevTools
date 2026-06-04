package me.m0dii.modules.mobai;

import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.List;

final class MobAiDebugClientState {
    private static final long OVERLAY_TTL_MS = 120_000L;

    private static InspectState inspectState;
    private static PathPreviewState pathPreviewState;

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
}
