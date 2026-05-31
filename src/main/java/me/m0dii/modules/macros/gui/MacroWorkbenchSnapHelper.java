package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.hud.MacroHudDataHandler;

import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

final class MacroWorkbenchSnapHelper {

    private MacroWorkbenchSnapHelper() {
    }

    static int[] snapElementToNeighbors(
            MacroHudDataHandler.HudElement moving,
            int screenX,
            int screenY,
            Set<String> excludedIds,
            List<MacroHudDataHandler.HudElement> elements,
            ToIntFunction<MacroHudDataHandler.HudElement> resolveX,
            ToIntFunction<MacroHudDataHandler.HudElement> resolveY,
            int screenWidth,
            int canvasTop,
            int canvasBottom
    ) {
        if (moving == null) {
            return new int[]{screenX, screenY, Integer.MIN_VALUE, Integer.MIN_VALUE};
        }
        final int threshold = 8;
        int bestDx = threshold + 1;
        int bestDy = threshold + 1;
        int snappedX = screenX;
        int snappedY = screenY;

        int movingRight = screenX + moving.width;
        int movingCenterX = screenX + moving.width / 2;
        int movingBottom = screenY + moving.height;
        int movingCenterY = screenY + moving.height / 2;

        for (MacroHudDataHandler.HudElement other : elements) {
            if (other == null || other.id.equals(moving.id) || excludedIds.contains(other.id)) {
                continue;
            }
            int ox = resolveX.applyAsInt(other);
            int oy = resolveY.applyAsInt(other);
            int otherRight = ox + other.width;
            int otherCenterX = ox + other.width / 2;
            int otherBottom = oy + other.height;
            int otherCenterY = oy + other.height / 2;

            int[] xCandidates = {
                    ox,
                    otherRight,
                    otherCenterX,
                    ox - moving.width,
                    otherRight - moving.width,
                    otherCenterX - moving.width / 2
            };
            int[] yCandidates = {
                    oy,
                    otherBottom,
                    otherCenterY,
                    oy - moving.height,
                    otherBottom - moving.height,
                    otherCenterY - moving.height / 2
            };

            for (int candidate : xCandidates) {
                int dxLeft = Math.abs(candidate - snappedX);
                if (dxLeft <= threshold && dxLeft < bestDx) {
                    bestDx = dxLeft;
                    snappedX = candidate;
                }
                int dxRight = Math.abs(candidate - movingRight);
                if (dxRight <= threshold && dxRight < bestDx) {
                    bestDx = dxRight;
                    snappedX = candidate - moving.width;
                }
                int dxCenter = Math.abs(candidate - movingCenterX);
                if (dxCenter <= threshold && dxCenter < bestDx) {
                    bestDx = dxCenter;
                    snappedX = candidate - moving.width / 2;
                }
            }

            for (int candidate : yCandidates) {
                int dyTop = Math.abs(candidate - snappedY);
                if (dyTop <= threshold && dyTop < bestDy) {
                    bestDy = dyTop;
                    snappedY = candidate;
                }
                int dyBottom = Math.abs(candidate - movingBottom);
                if (dyBottom <= threshold && dyBottom < bestDy) {
                    bestDy = dyBottom;
                    snappedY = candidate - moving.height;
                }
                int dyCenter = Math.abs(candidate - movingCenterY);
                if (dyCenter <= threshold && dyCenter < bestDy) {
                    bestDy = dyCenter;
                    snappedY = candidate - moving.height / 2;
                }
            }
        }

        snappedX = Math.clamp(snappedX, 0, Math.max(0, screenWidth - moving.width));
        snappedY = Math.clamp(snappedY, canvasTop, Math.max(canvasTop, canvasBottom - moving.height));
        int guideX = Math.abs(snappedX - screenX) > 0 ? snappedX : Integer.MIN_VALUE;
        int guideY = Math.abs(snappedY - screenY) > 0 ? snappedY : Integer.MIN_VALUE;
        return new int[]{snappedX, snappedY, guideX, guideY};
    }

    static int[] snapResizeToNeighbors(
            MacroHudDataHandler.HudElement resizingElement,
            int baseX,
            int baseY,
            int width,
            int height,
            List<MacroHudDataHandler.HudElement> elements,
            ToIntFunction<MacroHudDataHandler.HudElement> resolveX,
            ToIntFunction<MacroHudDataHandler.HudElement> resolveY,
            int screenWidth,
            int canvasBottom
    ) {
        if (resizingElement == null) {
            return new int[]{width, height, Integer.MIN_VALUE, Integer.MIN_VALUE};
        }
        final int threshold = 8;
        int bestDx = threshold + 1;
        int bestDy = threshold + 1;
        int right = baseX + width;
        int bottom = baseY + height;
        int snappedW = width;
        int snappedH = height;
        int guideX = Integer.MIN_VALUE;
        int guideY = Integer.MIN_VALUE;

        for (MacroHudDataHandler.HudElement other : elements) {
            if (other == null || other.id.equals(resizingElement.id)) {
                continue;
            }
            int ox = resolveX.applyAsInt(other);
            int oy = resolveY.applyAsInt(other);
            int[] xCandidates = {ox, ox + other.width, ox + (other.width / 2)};
            int[] yCandidates = {oy, oy + other.height, oy + (other.height / 2)};

            for (int candidate : xCandidates) {
                int dx = Math.abs(candidate - right);
                if (dx <= threshold && dx < bestDx) {
                    bestDx = dx;
                    snappedW = Math.max(1, candidate - baseX);
                    guideX = candidate;
                }
            }
            for (int candidate : yCandidates) {
                int dy = Math.abs(candidate - bottom);
                if (dy <= threshold && dy < bestDy) {
                    bestDy = dy;
                    snappedH = Math.max(1, candidate - baseY);
                    guideY = candidate;
                }
            }
        }

        int maxW = Math.max(1, screenWidth - baseX);
        int maxH = Math.max(1, canvasBottom - baseY);
        snappedW = Math.clamp(snappedW, 1, Math.min(2000, maxW));
        snappedH = Math.clamp(snappedH, 1, Math.min(1200, maxH));
        return new int[]{snappedW, snappedH, guideX, guideY};
    }
}

