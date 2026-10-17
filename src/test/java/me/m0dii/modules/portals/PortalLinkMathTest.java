package me.m0dii.modules.portals;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortalLinkMathTest {
    private static final String W = "sp:test";

    private static KnownPortal portal(String dim, int x, int y, int z) {
        return new KnownPortal(W, dim, x, y, z, x + 1, y + 2, z);
    }

    @Test
    void overworldPortalTargetsScaledNetherPoint() {
        KnownPortal overworld = portal(PortalLinkMath.OVERWORLD, 800, 70, -160);
        PortalLinkMath.Target target = PortalLinkMath.targetOf(overworld);
        assertEquals(PortalLinkMath.NETHER, target.dimension());
        assertEquals(801.0 / 8.0, target.x(), 1e-9);
        assertEquals(-159.5 / 8.0, target.z(), 1e-9);
    }

    @Test
    void picksClosestPortalInsideSearchSquare() {
        KnownPortal overworld = portal(PortalLinkMath.OVERWORLD, 800, 70, 0);
        KnownPortal near = portal(PortalLinkMath.NETHER, 102, 60, 0);
        KnownPortal far = portal(PortalLinkMath.NETHER, 113, 60, 0);
        KnownPortal outside = portal(PortalLinkMath.NETHER, 140, 60, 0);
        PortalLinkMath.Target target = PortalLinkMath.targetOf(overworld);
        assertSame(near, PortalLinkMath.pick(List.of(far, outside, near), W, target));
        assertNull(PortalLinkMath.pick(List.of(outside), W, target));
    }

    @Test
    void otherWorldsAreIgnored() {
        KnownPortal overworld = portal(PortalLinkMath.OVERWORLD, 0, 70, 0);
        KnownPortal elsewhere = new KnownPortal("mp:other", PortalLinkMath.NETHER, 0, 60, 0, 1, 62, 0);
        assertNull(PortalLinkMath.pick(List.of(elsewhere), W, PortalLinkMath.targetOf(overworld)));
    }

    @Test
    void touchingBoxesAreTheSamePortal() {
        KnownPortal a = portal(PortalLinkMath.OVERWORLD, 0, 70, 0);
        KnownPortal b = portal(PortalLinkMath.OVERWORLD, 2, 70, 0);
        KnownPortal c = portal(PortalLinkMath.OVERWORLD, 10, 70, 0);
        assertTrue(a.touches(b));
        assertTrue(!a.touches(c));
    }
}
