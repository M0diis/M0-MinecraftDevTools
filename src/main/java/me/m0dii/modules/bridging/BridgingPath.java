package me.m0dii.modules.bridging;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class BridgingPath {
    static final double NEAR_ZERO = 0.01D;
    private static final Vec3d CUBE_EXTENT = new Vec3d(0.5D, 0.5D, 0.5D);

    private BridgingPath() {
    }

    static List<BlockPos> calculateBresenhamVoxels(BlockPos startPos,
                                                   BlockPos endPos,
                                                   BridgingAdjacency adjacency,
                                                   float extentMultiplier) {
        List<BlockPos> points = new ArrayList<>();
        points.add(startPos);

        BlockPos delta = endPos.subtract(startPos);
        int dx = Math.abs(delta.getX());
        int dy = Math.abs(delta.getY());
        int dz = Math.abs(delta.getZ());
        int xStep = Integer.compare(delta.getX(), 0);
        int yStep = Integer.compare(delta.getY(), 0);
        int zStep = Integer.compare(delta.getZ(), 0);

        Vec3d workingVec = new Vec3d(startPos.getX(), startPos.getY(), startPos.getZ());
        Vec3d targetVec = new Vec3d(endPos.getX(), endPos.getY(), endPos.getZ());

        if (dx >= dy && dx >= dz) {
            int point1 = (2 * dy) - dx;
            int point2 = (2 * dz) - dx;

            while (Math.abs(workingVec.x - targetVec.x) > NEAR_ZERO) {
                workingVec = workingVec.add(xStep, 0, 0);
                if (point1 >= 0) {
                    workingVec = workingVec.add(0, yStep, 0);
                    point1 -= 2 * dx;
                }
                if (point2 >= 0) {
                    workingVec = workingVec.add(0, 0, zStep);
                    point2 -= 2 * dx;
                }
                point1 += 2 * dy;
                point2 += 2 * dz;
                addPoint(points, BlockPos.ofFloored(workingVec), startPos, endPos, adjacency, extentMultiplier);
            }
            return points;
        }

        if (dy >= dx && dy >= dz) {
            int point1 = (2 * dx) - dy;
            int point2 = (2 * dz) - dy;

            while (Math.abs(workingVec.y - targetVec.y) > NEAR_ZERO) {
                workingVec = workingVec.add(0, yStep, 0);
                if (point1 >= 0) {
                    workingVec = workingVec.add(xStep, 0, 0);
                    point1 -= 2 * dy;
                }
                if (point2 >= 0) {
                    workingVec = workingVec.add(0, 0, zStep);
                    point2 -= 2 * dy;
                }
                point1 += 2 * dx;
                point2 += 2 * dz;
                addPoint(points, BlockPos.ofFloored(workingVec), startPos, endPos, adjacency, extentMultiplier);
            }
            return points;
        }

        int point1 = (2 * dy) - dz;
        int point2 = (2 * dx) - dz;

        while (Math.abs(workingVec.z - targetVec.z) > NEAR_ZERO) {
            workingVec = workingVec.add(0, 0, zStep);
            if (point1 >= 0) {
                workingVec = workingVec.add(0, yStep, 0);
                point1 -= 2 * dz;
            }
            if (point2 >= 0) {
                workingVec = workingVec.add(xStep, 0, 0);
                point2 -= 2 * dz;
            }
            point1 += 2 * dy;
            point2 += 2 * dx;
            addPoint(points, BlockPos.ofFloored(workingVec), startPos, endPos, adjacency, extentMultiplier);
        }

        return points;
    }

    private static void addPoint(List<BlockPos> points,
                                 BlockPos newPoint,
                                 BlockPos lineStart,
                                 BlockPos lineEnd,
                                 BridgingAdjacency adjacency,
                                 float extentMultiplier) {
        if (adjacency != BridgingAdjacency.DISABLED) {
            points.addAll(calculateMissedPoints(points, newPoint, lineStart, lineEnd, adjacency, extentMultiplier));
        }
        points.add(newPoint);
    }

    private static List<BlockPos> calculateMissedPoints(List<BlockPos> points,
                                                        BlockPos newPoint,
                                                        BlockPos lineStart,
                                                        BlockPos lineEnd,
                                                        BridgingAdjacency adjacency,
                                                        float extentMultiplier) {
        if (points.isEmpty()) {
            return List.of();
        }

        BlockPos lastPoint = points.get(points.size() - 1);
        BlockPos delta = newPoint.subtract(lastPoint);
        int distance = newPoint.getManhattanDistance(lastPoint);

        if (distance <= 1) {
            return List.of();
        }

        Set<BlockPos> reviewPositions = new LinkedHashSet<>();
        if (distance == 2 && adjacency.supportsFaces()) {
            addAxisPositions(reviewPositions, lastPoint, delta, false);
        }
        if (distance == 3) {
            if (adjacency.supportsFaces()) {
                addAxisPositions(reviewPositions, lastPoint, delta, false);
            }
            if (adjacency.supportsEdges()) {
                addAxisPositions(reviewPositions, lastPoint, delta, true);
            }
        }

        Vec3d extent = CUBE_EXTENT.multiply(extentMultiplier);
        return reviewPositions.stream()
                .filter(pos -> intersectsLineBox(pos, lineStart, lineEnd, extent))
                .toList();
    }

    private static void addAxisPositions(Set<BlockPos> reviewPositions,
                                         BlockPos lastPoint,
                                         BlockPos delta,
                                         boolean pairAxes) {
        BlockPos x = new BlockPos(delta.getX(), 0, 0);
        BlockPos y = new BlockPos(0, delta.getY(), 0);
        BlockPos z = new BlockPos(0, 0, delta.getZ());

        if (!x.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(x));
        }
        if (!y.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(y));
        }
        if (!z.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(z));
        }

        if (!pairAxes) {
            return;
        }

        if (!x.equals(BlockPos.ORIGIN) && !y.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(x).add(y));
        }
        if (!x.equals(BlockPos.ORIGIN) && !z.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(x).add(z));
        }
        if (!y.equals(BlockPos.ORIGIN) && !z.equals(BlockPos.ORIGIN)) {
            reviewPositions.add(lastPoint.add(y).add(z));
        }
    }

    private static boolean intersectsLineBox(BlockPos pos, BlockPos lineStart, BlockPos lineEnd, Vec3d extent) {
        Vec3d transform = Vec3d.of(pos);
        Vec3d lineStartLocal = Vec3d.of(lineStart).subtract(transform);
        Vec3d lineEndLocal = Vec3d.of(lineEnd).subtract(transform);
        Vec3d lineMid = lineStartLocal.add(lineEndLocal).multiply(0.5D);
        Vec3d line = lineStartLocal.subtract(lineMid);
        Vec3d lineExt = new Vec3d(Math.abs(line.x), Math.abs(line.y), Math.abs(line.z));

        if (Math.abs(lineMid.x) > extent.x + lineExt.x) {
            return false;
        }
        if (Math.abs(lineMid.y) > extent.y + lineExt.y) {
            return false;
        }
        if (Math.abs(lineMid.z) > extent.z + lineExt.z) {
            return false;
        }
        if (Math.abs((lineMid.y * line.z) - (lineMid.z * line.y)) > (extent.y * lineExt.z) + (extent.z * lineExt.y)) {
            return false;
        }
        if (Math.abs((lineMid.x * line.z) - (lineMid.z * line.x)) > (extent.x * lineExt.z) + (extent.z * lineExt.x)) {
            return false;
        }
        return Math.abs((lineMid.x * line.y) - (lineMid.y * line.x)) <= (extent.x * lineExt.y) + (extent.y * lineExt.x);
    }
}
