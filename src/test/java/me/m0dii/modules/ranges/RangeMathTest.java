package me.m0dii.modules.ranges;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeMathTest {
    @Test
    void beaconLevelsStopAtFirstIncompleteLayer() {
        assertEquals(4, RangeMath.beaconLevels((dx, dy, dz) -> true));
        assertEquals(0, RangeMath.beaconLevels((dx, dy, dz) -> false));
        // Layer 3 missing one corner.
        assertEquals(2, RangeMath.beaconLevels((dx, dy, dz) -> !(dy == -3 && dx == 3 && dz == -3)));
        assertEquals(50, RangeMath.beaconRange(4));
        assertEquals(20, RangeMath.beaconRange(1));
        assertEquals(0, RangeMath.beaconRange(0));
    }

    @Test
    void conduitFrameHas42UniqueSpots() {
        List<int[]> offsets = RangeMath.conduitFrameOffsets();
        assertEquals(42, offsets.size());
        Set<String> unique = new HashSet<>();
        for (int[] o : offsets) {
            unique.add(o[0] + "," + o[1] + "," + o[2]);
            assertTrue(Math.abs(o[0]) <= 2 && Math.abs(o[1]) <= 2 && Math.abs(o[2]) <= 2);
        }
        assertEquals(42, unique.size());
        assertEquals(96, RangeMath.conduitRange(42));
        assertEquals(32, RangeMath.conduitRange(16));
        assertEquals(0, RangeMath.conduitRange(15));
    }

    @Test
    void regionFiles() {
        assertEquals(0, RangeMath.region(0));
        assertEquals(0, RangeMath.region(511));
        assertEquals(1, RangeMath.region(512));
        assertEquals(-1, RangeMath.region(-1));
        assertEquals(-2, RangeMath.region(-513));
        assertEquals("r.-1.2.mca", RangeMath.regionFileName(-10, 1100));
    }

    @Test
    void circleIsClosed() {
        List<double[]> points = RangeMath.circle(10, -5, 24, 48);
        assertEquals(49, points.size());
        assertEquals(points.get(0)[0], points.get(48)[0], 1e-9);
        for (double[] p : points) {
            assertEquals(24.0, Math.hypot(p[0] - 10, p[1] + 5), 1e-9);
        }
        assertEquals(24, RangeMath.segmentsFor(4));
        assertEquals(192, RangeMath.segmentsFor(128));
    }
}
