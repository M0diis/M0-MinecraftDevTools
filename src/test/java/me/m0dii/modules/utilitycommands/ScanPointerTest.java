package me.m0dii.modules.utilitycommands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanPointerTest {
    @Test
    void arrowsFollowMinecraftYaw() {
        // Facing south (yaw 0): a target to the south is ahead, west (-x) is on the right.
        assertEquals("↑", ScanPointer.arrow(0, 0, 10));
        assertEquals("→", ScanPointer.arrow(0, -10, 0));
        assertEquals("←", ScanPointer.arrow(0, 10, 0));
        assertEquals("↓", ScanPointer.arrow(0, 0, -10));
        // Facing west (yaw 90): north (-z) is on the right.
        assertEquals("→", ScanPointer.arrow(90, 0, -10));
        assertEquals("↗", ScanPointer.arrow(0, -10, 10));
        assertEquals("•", ScanPointer.arrow(0, 0, 0));
        assertEquals("↑", ScanPointer.arrow(720, 0, 5));
    }

    @Test
    void relativeYawIsWrapped() {
        assertEquals(90.0, ScanPointer.relativeYaw(0, -1, 0), 1e-9);
        assertEquals(-90.0, ScanPointer.relativeYaw(-360, 1, 0), 1e-9);
    }

    @Test
    void distances() {
        assertEquals("5 m", ScanPointer.distance(3, 0, 4));
        assertEquals("1.5 km", ScanPointer.distance(1500, 0, 0));
        assertEquals("10 m ▼10", ScanPointer.distance(0, -10, 0));
    }
}
