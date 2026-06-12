package me.m0dii.modules.worldedit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Consumer;

final class WorldEditRegionMask {
    private final WorldEditSelection bounds;
    private final WorldEditRegionShape shape;
    private final Direction.Axis circleNormalAxis;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radiusX;
    private final double radiusY;
    private final double radiusZ;

    private WorldEditRegionMask(WorldEditSelection bounds,
                                WorldEditRegionShape shape,
                                Direction.Axis circleNormalAxis,
                                double centerX,
                                double centerY,
                                double centerZ,
                                double radiusX,
                                double radiusY,
                                double radiusZ) {
        this.bounds = bounds;
        this.shape = shape;
        this.circleNormalAxis = circleNormalAxis;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
    }

    static WorldEditRegionMask fromSelection(WorldEditSelection selection, WorldEditRegionShape shape) {
        double centerX = center(selection.min().getX(), selection.max().getX());
        double centerY = center(selection.min().getY(), selection.max().getY());
        double centerZ = center(selection.min().getZ(), selection.max().getZ());
        double radiusX = axisRadius(selection.sizeX());
        double radiusY = axisRadius(selection.sizeY());
        double radiusZ = axisRadius(selection.sizeZ());
        Direction.Axis normalAxis = shape == WorldEditRegionShape.CIRCLE ? smallestAxis(selection) : null;
        return new WorldEditRegionMask(selection, shape, normalAxis, centerX, centerY, centerZ, radiusX, radiusY, radiusZ);
    }

    static WorldEditRegionMask cylinder(BlockPos anchor, double radiusX, double radiusZ, int height) {
        double centerX = anchor.getX() + 0.5;
        double centerZ = anchor.getZ() + 0.5;
        double clampedRadiusX = Math.max(0.5, radiusX);
        double clampedRadiusZ = Math.max(0.5, radiusZ);
        int minX = (int) Math.floor(centerX - clampedRadiusX);
        int maxX = (int) Math.ceil(centerX + clampedRadiusX) - 1;
        int minZ = (int) Math.floor(centerZ - clampedRadiusZ);
        int maxZ = (int) Math.ceil(centerZ + clampedRadiusZ) - 1;
        int minY = anchor.getY();
        int maxY = anchor.getY() + Math.max(1, height) - 1;
        WorldEditSelection bounds = new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        return new WorldEditRegionMask(bounds, WorldEditRegionShape.CYLINDER, null, centerX, center(bounds.min().getY(), bounds.max().getY()), centerZ,
                clampedRadiusX, axisRadius(bounds.sizeY()), clampedRadiusZ);
    }

    static WorldEditRegionMask sphere(BlockPos centerPos, double radiusX, double radiusY, double radiusZ) {
        double centerX = centerPos.getX() + 0.5;
        double centerY = centerPos.getY() + 0.5;
        double centerZ = centerPos.getZ() + 0.5;
        double clampedRadiusX = Math.max(0.5, radiusX);
        double clampedRadiusY = Math.max(0.5, radiusY);
        double clampedRadiusZ = Math.max(0.5, radiusZ);
        int minX = (int) Math.floor(centerX - clampedRadiusX);
        int maxX = (int) Math.ceil(centerX + clampedRadiusX) - 1;
        int minY = (int) Math.floor(centerY - clampedRadiusY);
        int maxY = (int) Math.ceil(centerY + clampedRadiusY) - 1;
        int minZ = (int) Math.floor(centerZ - clampedRadiusZ);
        int maxZ = (int) Math.ceil(centerZ + clampedRadiusZ) - 1;
        WorldEditSelection bounds = new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        return new WorldEditRegionMask(bounds, WorldEditRegionShape.SPHERE, null, centerX, centerY, centerZ,
                clampedRadiusX, clampedRadiusY, clampedRadiusZ);
    }

    WorldEditSelection bounds() {
        return this.bounds;
    }

    WorldEditRegionShape shape() {
        return this.shape;
    }

    WorldEditRegionMask shifted(int dx, int dy, int dz) {
        return new WorldEditRegionMask(
                this.bounds.shift(dx, dy, dz),
                this.shape,
                this.circleNormalAxis,
                this.centerX + dx,
                this.centerY + dy,
                this.centerZ + dz,
                this.radiusX,
                this.radiusY,
                this.radiusZ
        );
    }

    WorldEditRegionMask shifted(Direction direction, int amount) {
        return shifted(direction.getOffsetX() * amount, direction.getOffsetY() * amount, direction.getOffsetZ() * amount);
    }

    void forEach(Consumer<BlockPos> consumer) {
        for (int y = this.bounds.min().getY(); y <= this.bounds.max().getY(); y++) {
            for (int z = this.bounds.min().getZ(); z <= this.bounds.max().getZ(); z++) {
                for (int x = this.bounds.min().getX(); x <= this.bounds.max().getX(); x++) {
                    if (contains(x, y, z)) {
                        consumer.accept(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    boolean matchesSurface(BlockPos pos, WorldEditRegionSurface surface) {
        return matchesSurface(pos.getX(), pos.getY(), pos.getZ(), surface);
    }

    boolean matchesSurface(int x, int y, int z, WorldEditRegionSurface surface) {
        if (!contains(x, y, z)) {
            return false;
        }
        return switch (surface) {
            case SOLID -> true;
            case FLOOR -> !contains(x, y - 1, z);
            case ROOF -> !contains(x, y + 1, z);
            case WALLS -> !contains(x - 1, y, z)
                    || !contains(x + 1, y, z)
                    || !contains(x, y, z - 1)
                    || !contains(x, y, z + 1);
            case SHELL -> !contains(x - 1, y, z)
                    || !contains(x + 1, y, z)
                    || !contains(x, y - 1, z)
                    || !contains(x, y + 1, z)
                    || !contains(x, y, z - 1)
                    || !contains(x, y, z + 1);
        };
    }

    boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    boolean contains(int x, int y, int z) {
        if (x < this.bounds.min().getX() || x > this.bounds.max().getX()
                || y < this.bounds.min().getY() || y > this.bounds.max().getY()
                || z < this.bounds.min().getZ() || z > this.bounds.max().getZ()) {
            return false;
        }

        double sampleX = x + 0.5;
        double sampleY = y + 0.5;
        double sampleZ = z + 0.5;

        return switch (this.shape) {
            case BOX -> true;
            case CYLINDER -> insideEllipse(sampleX, sampleZ, this.centerX, this.centerZ, this.radiusX, this.radiusZ);
            case SPHERE -> insideEllipsoid(sampleX, sampleY, sampleZ, this.centerX, this.centerY, this.centerZ, this.radiusX, this.radiusY, this.radiusZ);
            case CIRCLE -> switch (this.circleNormalAxis) {
                case X -> insideEllipse(sampleY, sampleZ, this.centerY, this.centerZ, this.radiusY, this.radiusZ);
                case Y -> insideEllipse(sampleX, sampleZ, this.centerX, this.centerZ, this.radiusX, this.radiusZ);
                case Z -> insideEllipse(sampleX, sampleY, this.centerX, this.centerY, this.radiusX, this.radiusY);
            };
        };
    }

    private static boolean insideEllipse(double sampleA,
                                         double sampleB,
                                         double centerA,
                                         double centerB,
                                         double radiusA,
                                         double radiusB) {
        double dA = (sampleA - centerA) / radiusA;
        double dB = (sampleB - centerB) / radiusB;
        return (dA * dA) + (dB * dB) <= 1.0;
    }

    private static boolean insideEllipsoid(double sampleX,
                                           double sampleY,
                                           double sampleZ,
                                           double centerX,
                                           double centerY,
                                           double centerZ,
                                           double radiusX,
                                           double radiusY,
                                           double radiusZ) {
        double dx = (sampleX - centerX) / radiusX;
        double dy = (sampleY - centerY) / radiusY;
        double dz = (sampleZ - centerZ) / radiusZ;
        return (dx * dx) + (dy * dy) + (dz * dz) <= 1.0;
    }

    private static double center(int min, int max) {
        return (min + max + 1.0) * 0.5;
    }

    private static double axisRadius(int size) {
        return Math.max(0.5, size * 0.5);
    }

    private static Direction.Axis smallestAxis(WorldEditSelection selection) {
        int x = selection.sizeX();
        int y = selection.sizeY();
        int z = selection.sizeZ();
        if (x <= y && x <= z) {
            return Direction.Axis.X;
        }
        if (y <= x && y <= z) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }
}
