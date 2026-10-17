package me.m0dii.modules.ranges;

import java.util.ArrayList;
import java.util.List;

/** Numbers behind the range overlays, kept free of game classes so they can be tested. */
public final class RangeMath {
    /** Mobs never spawn closer than this to a player. */
    public static final int SPAWN_MIN_DISTANCE = 24;
    /** Beyond this, mobs that can despawn may do so at random. */
    public static final int RANDOM_DESPAWN_DISTANCE = 32;
    /** Beyond this, mobs that can despawn vanish at once; also the random-tick / spawning reach. */
    public static final int INSTANT_DESPAWN_DISTANCE = 128;
    public static final int REGION_SIZE = 512;

    @FunctionalInterface
    public interface OffsetTest {
        boolean test(int dx, int dy, int dz);
    }

    private RangeMath() {
    }

    /** Beacon effect reach for a pyramid of {@code levels} (0-4); 0 means inactive. */
    public static int beaconRange(int levels) {
        return levels <= 0 ? 0 : levels * 10 + 10;
    }

    /**
     * Pyramid levels under a beacon: layer {@code n} (1-4) is the square of radius {@code n} at {@code dy = -n},
     * and every block of it must be a beacon base block. Stops at the first incomplete layer.
     */
    public static int beaconLevels(OffsetTest isBase) {
        int levels = 0;
        for (int layer = 1; layer <= 4; layer++) {
            for (int dx = -layer; dx <= layer; dx++) {
                for (int dz = -layer; dz <= layer; dz++) {
                    if (!isBase.test(dx, -layer, dz)) {
                        return levels;
                    }
                }
            }
            levels = layer;
        }
        return levels;
    }

    /** The 42 offsets around a conduit where frame blocks count (the vanilla ring pattern). */
    public static List<int[]> conduitFrameOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    int ai = Math.abs(i);
                    int aj = Math.abs(j);
                    int ak = Math.abs(k);
                    if ((ai > 1 || aj > 1 || ak > 1)
                            && (i == 0 && (aj == 2 || ak == 2) || j == 0 && (ai == 2 || ak == 2) || k == 0 && (ai == 2 || aj == 2))) {
                        offsets.add(new int[]{i, j, k});
                    }
                }
            }
        }
        return offsets;
    }

    /** Conduit power reach for {@code frames} frame blocks; 0 when it isn't active (fewer than 16). */
    public static int conduitRange(int frames) {
        return frames < 16 ? 0 : frames / 7 * 16;
    }

    /** Region file index ({@code r.X.Z.mca}) for a block coordinate. */
    public static int region(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, REGION_SIZE);
    }

    public static String regionFileName(int blockX, int blockZ) {
        return "r." + region(blockX) + "." + region(blockZ) + ".mca";
    }

    /** Points on a horizontal circle (closed: the first point repeats at the end). */
    public static List<double[]> circle(double cx, double cz, double radius, int segments) {
        List<double[]> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            points.add(new double[]{cx + Math.cos(angle) * radius, cz + Math.sin(angle) * radius});
        }
        return points;
    }

    /** Segments for a smooth circle of {@code radius} without drawing thousands of lines. */
    public static int segmentsFor(double radius) {
        return (int) Math.clamp(Math.round(radius * 1.5), 24, 240);
    }
}
