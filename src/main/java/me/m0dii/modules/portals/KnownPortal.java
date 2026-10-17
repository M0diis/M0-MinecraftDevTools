package me.m0dii.modules.portals;

import java.util.Locale;
import java.util.Objects;

/** A nether portal seen by the player: the box of its portal blocks in one dimension of one world. */
public final class KnownPortal {
    public String world;
    public String dimension;
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    public long lastSeen;

    public KnownPortal() {
    }

    public KnownPortal(String world, String dimension, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world;
        this.dimension = dimension;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.lastSeen = System.currentTimeMillis();
    }

    public double centerX() {
        return (minX + maxX + 1) / 2.0;
    }

    public double centerY() {
        return minY;
    }

    public double centerZ() {
        return (minZ + maxZ + 1) / 2.0;
    }

    public boolean sameWorldAndDimension(String otherWorld, String otherDimension) {
        return Objects.equals(world, otherWorld) && Objects.equals(dimension, otherDimension);
    }

    /** True when the two boxes touch or overlap (same portal seen twice, maybe resized). */
    public boolean touches(KnownPortal other) {
        return sameWorldAndDimension(other.world, other.dimension)
                && minX <= other.maxX + 1 && other.minX <= maxX + 1
                && minY <= other.maxY + 1 && other.minY <= maxY + 1
                && minZ <= other.maxZ + 1 && other.minZ <= maxZ + 1;
    }

    /** Squared distance from a point to the nearest portal block. */
    public double distanceSq(double x, double y, double z) {
        double dx = Math.max(Math.max(minX - x, 0), x - (maxX + 1));
        double dy = Math.max(Math.max(minY - y, 0), y - (maxY + 1));
        double dz = Math.max(Math.max(minZ - z, 0), z - (maxZ + 1));
        return dx * dx + dy * dy + dz * dz;
    }

    public String describe() {
        return String.format(Locale.ROOT, "%d %d %d", (int) Math.floor(centerX()), minY, (int) Math.floor(centerZ()));
    }
}
