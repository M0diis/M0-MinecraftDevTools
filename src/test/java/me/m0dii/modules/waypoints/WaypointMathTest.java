package me.m0dii.modules.waypoints;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaypointMathTest {
    @Test
    void scalesBetweenOverworldAndNether() {
        assertEquals(1.0, WaypointMath.scale(WaypointMath.OVERWORLD, WaypointMath.OVERWORLD));
        assertEquals(0.125, WaypointMath.scale(WaypointMath.OVERWORLD, WaypointMath.NETHER));
        assertEquals(8.0, WaypointMath.scale(WaypointMath.NETHER, WaypointMath.OVERWORLD));
        assertEquals(0.0, WaypointMath.scale("minecraft:the_end", WaypointMath.OVERWORLD));
    }

    @Test
    void arrowPointsRelativeToView() {
        // Facing south (yaw 0): a target to the south is ahead, to the east (+X) is on the left.
        assertEquals("↑", WaypointMath.arrowFor(0, 10, 0));
        assertEquals("←", WaypointMath.arrowFor(10, 0, 0));
        assertEquals("→", WaypointMath.arrowFor(-10, 0, 0));
        assertEquals("↓", WaypointMath.arrowFor(0, -10, 0));
        // Facing east (yaw -90): east is ahead.
        assertEquals("↑", WaypointMath.arrowFor(10, 0, -90));
        // Wrap-around: yaw 350 is almost south.
        assertEquals("↑", WaypointMath.arrowFor(0, 10, 350));
    }

    @Test
    void wrapsDegrees() {
        assertEquals(-170.0, WaypointMath.wrapDegrees(190.0), 1e-9);
        assertEquals(170.0, WaypointMath.wrapDegrees(-190.0), 1e-9);
        assertEquals(0.0, WaypointMath.wrapDegrees(720.0), 1e-9);
    }
}
