package me.m0dii.modules.worldedit;

import me.m0dii.modules.debugdraw.DebugDrawManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record WorldEditSelection(BlockPos min, BlockPos max) {
    public WorldEditSelection {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        min = new BlockPos(minX, minY, minZ);
        max = new BlockPos(maxX, maxY, maxZ);
    }

    public static WorldEditSelection fromDebugDrawSelection() {
        DebugDrawManager.SelectionBounds bounds = DebugDrawManager.getSelectionBounds();
        if (bounds == null) {
            return null;
        }
        return new WorldEditSelection(bounds.min(), bounds.max());
    }

    public int sizeX() {
        return this.max.getX() - this.min.getX() + 1;
    }

    public int sizeY() {
        return this.max.getY() - this.min.getY() + 1;
    }

    public int sizeZ() {
        return this.max.getZ() - this.min.getZ() + 1;
    }

    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    public WorldEditSelection shift(int dx, int dy, int dz) {
        return new WorldEditSelection(this.min.add(dx, dy, dz), this.max.add(dx, dy, dz));
    }

    public WorldEditSelection shift(Direction direction, int amount) {
        return shift(
                direction.getOffsetX() * amount,
                direction.getOffsetY() * amount,
                direction.getOffsetZ() * amount
        );
    }

    public WorldEditSelection expand(Direction direction, int amount) {
        return expand(direction, amount, 0);
    }

    public WorldEditSelection expand(Direction direction, int amount, int reverseAmount) {
        return moveSide(direction, amount).moveSide(direction.getOpposite(), reverseAmount);
    }

    public WorldEditSelection contract(Direction direction, int amount) {
        return contract(direction, amount, 0);
    }

    public WorldEditSelection contract(Direction direction, int amount, int reverseAmount) {
        return moveSide(direction, -amount).moveSide(direction.getOpposite(), -reverseAmount);
    }

    public WorldEditSelection outset(int amount, boolean onlyHorizontal, boolean onlyVertical) {
        WorldEditSelection selection = this;
        if (!onlyHorizontal) {
            selection = selection.moveSide(Direction.UP, amount).moveSide(Direction.DOWN, amount);
        }
        if (!onlyVertical) {
            selection = selection.moveSide(Direction.EAST, amount)
                    .moveSide(Direction.WEST, amount)
                    .moveSide(Direction.SOUTH, amount)
                    .moveSide(Direction.NORTH, amount);
        }
        return selection;
    }

    public WorldEditSelection inset(int amount, boolean onlyHorizontal, boolean onlyVertical) {
        WorldEditSelection selection = this;
        if (!onlyHorizontal) {
            selection = selection.moveSide(Direction.UP, -amount).moveSide(Direction.DOWN, -amount);
        }
        if (!onlyVertical) {
            selection = selection.moveSide(Direction.EAST, -amount)
                    .moveSide(Direction.WEST, -amount)
                    .moveSide(Direction.SOUTH, -amount)
                    .moveSide(Direction.NORTH, -amount);
        }
        return selection;
    }

    public WorldEditSelection verticalColumn(int minY, int maxY) {
        return checkedSelection(this.min.getX(), minY, this.min.getZ(), this.max.getX(), maxY, this.max.getZ());
    }

    public WorldEditSelection intersection(WorldEditSelection other) {
        if (other == null) {
            return null;
        }

        int minX = Math.max(this.min.getX(), other.min.getX());
        int minY = Math.max(this.min.getY(), other.min.getY());
        int minZ = Math.max(this.min.getZ(), other.min.getZ());
        int maxX = Math.min(this.max.getX(), other.max.getX());
        int maxY = Math.min(this.max.getY(), other.max.getY());
        int maxZ = Math.min(this.max.getZ(), other.max.getZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return null;
        }

        return new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    public List<WorldEditSelection> subtract(WorldEditSelection cutout) {
        WorldEditSelection overlap = intersection(cutout);
        if (overlap == null) {
            return List.of(this);
        }
        if (overlap.min.equals(this.min) && overlap.max.equals(this.max)) {
            return List.of();
        }

        List<WorldEditSelection> out = new ArrayList<>();

        addIfValid(out,
                this.min.getX(), this.min.getY(), this.min.getZ(),
                overlap.min.getX() - 1, this.max.getY(), this.max.getZ());
        addIfValid(out,
                overlap.max.getX() + 1, this.min.getY(), this.min.getZ(),
                this.max.getX(), this.max.getY(), this.max.getZ());

        int innerMinX = Math.max(this.min.getX(), overlap.min.getX());
        int innerMaxX = Math.min(this.max.getX(), overlap.max.getX());

        addIfValid(out,
                innerMinX, this.min.getY(), this.min.getZ(),
                innerMaxX, overlap.min.getY() - 1, this.max.getZ());
        addIfValid(out,
                innerMinX, overlap.max.getY() + 1, this.min.getZ(),
                innerMaxX, this.max.getY(), this.max.getZ());

        int innerMinY = Math.max(this.min.getY(), overlap.min.getY());
        int innerMaxY = Math.min(this.max.getY(), overlap.max.getY());

        addIfValid(out,
                innerMinX, innerMinY, this.min.getZ(),
                innerMaxX, innerMaxY, overlap.min.getZ() - 1);
        addIfValid(out,
                innerMinX, innerMinY, overlap.max.getZ() + 1,
                innerMaxX, innerMaxY, this.max.getZ());

        return out;
    }

    public List<WorldEditSelection> partition(int maxVolume) {
        List<WorldEditSelection> out = new ArrayList<>();
        partitionRecursive(this, maxVolume, out);
        return out;
    }

    public List<WorldEditSelection> partitionOrdered(Direction.Axis primaryAxis, boolean descending, int maxVolume) {
        List<WorldEditSelection> out = partition(maxVolume);
        Comparator<WorldEditSelection> comparator = comparatorForAxis(primaryAxis, descending);
        out.sort(comparator);
        return out;
    }

    private static void partitionRecursive(WorldEditSelection selection, int maxVolume, List<WorldEditSelection> out) {
        if (selection.volume() <= maxVolume) {
            out.add(selection);
            return;
        }

        Direction.Axis splitAxis = largestAxis(selection);
        if (splitAxis == null) {
            out.add(selection);
            return;
        }

        int minX = selection.min.getX();
        int minY = selection.min.getY();
        int minZ = selection.min.getZ();
        int maxX = selection.max.getX();
        int maxY = selection.max.getY();
        int maxZ = selection.max.getZ();

        switch (splitAxis) {
            case X -> {
                int mid = minX + ((maxX - minX) / 2);
                partitionRecursive(new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(mid, maxY, maxZ)), maxVolume, out);
                partitionRecursive(new WorldEditSelection(new BlockPos(mid + 1, minY, minZ), new BlockPos(maxX, maxY, maxZ)), maxVolume, out);
            }
            case Y -> {
                int mid = minY + ((maxY - minY) / 2);
                partitionRecursive(new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, mid, maxZ)), maxVolume, out);
                partitionRecursive(new WorldEditSelection(new BlockPos(minX, mid + 1, minZ), new BlockPos(maxX, maxY, maxZ)), maxVolume, out);
            }
            case Z -> {
                int mid = minZ + ((maxZ - minZ) / 2);
                partitionRecursive(new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, mid)), maxVolume, out);
                partitionRecursive(new WorldEditSelection(new BlockPos(minX, minY, mid + 1), new BlockPos(maxX, maxY, maxZ)), maxVolume, out);
            }
        }
    }

    private static Direction.Axis largestAxis(WorldEditSelection selection) {
        int sizeX = selection.sizeX();
        int sizeY = selection.sizeY();
        int sizeZ = selection.sizeZ();

        Direction.Axis axis = null;
        int best = 1;
        if (sizeX > best) {
            axis = Direction.Axis.X;
            best = sizeX;
        }
        if (sizeY > best) {
            axis = Direction.Axis.Y;
            best = sizeY;
        }
        if (sizeZ > best) {
            axis = Direction.Axis.Z;
        }
        return axis;
    }

    private static Comparator<WorldEditSelection> comparatorForAxis(Direction.Axis axis, boolean descending) {
        Comparator<WorldEditSelection> comparator = switch (axis) {
            case X -> Comparator.comparingInt(selection -> descending ? selection.max.getX() : selection.min.getX());
            case Y -> Comparator.comparingInt(selection -> descending ? selection.max.getY() : selection.min.getY());
            case Z -> Comparator.comparingInt(selection -> descending ? selection.max.getZ() : selection.min.getZ());
        };
        return descending ? comparator.reversed() : comparator;
    }

    private WorldEditSelection moveSide(Direction direction, int amount) {
        int minX = this.min.getX();
        int minY = this.min.getY();
        int minZ = this.min.getZ();
        int maxX = this.max.getX();
        int maxY = this.max.getY();
        int maxZ = this.max.getZ();

        switch (direction) {
            case EAST -> maxX += amount;
            case WEST -> minX -= amount;
            case UP -> maxY += amount;
            case DOWN -> minY -= amount;
            case SOUTH -> maxZ += amount;
            case NORTH -> minZ -= amount;
        }

        return checkedSelection(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static WorldEditSelection checkedSelection(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (minX > maxX) {
            throw new IllegalArgumentException("Selection would be reduced past zero on the X axis.");
        }
        if (minY > maxY) {
            throw new IllegalArgumentException("Selection would be reduced past zero on the Y axis.");
        }
        if (minZ > maxZ) {
            throw new IllegalArgumentException("Selection would be reduced past zero on the Z axis.");
        }
        return new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    private static void addIfValid(List<WorldEditSelection> out,
                                   int minX,
                                   int minY,
                                   int minZ,
                                   int maxX,
                                   int maxY,
                                   int maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }
        out.add(new WorldEditSelection(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)));
    }
}
