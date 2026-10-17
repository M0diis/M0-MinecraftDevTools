package me.m0dii.modules.placement;

import me.m0dii.modules.placement.PlacementRestriction.Mode;
import me.m0dii.modules.placement.PlacementRestriction.Pos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacementRestrictionTest {
    @Test
    void planeKeepsTheFirstLayer() {
        PlacementRestriction r = new PlacementRestriction();
        assertTrue(r.allow(Mode.PLANE, new Pos(0, 64, 0), "up", 'y'));
        assertTrue(r.allow(Mode.PLANE, new Pos(5, 64, -3), "north", 'z'));
        assertFalse(r.allow(Mode.PLANE, new Pos(1, 65, 0), "up", 'y'));
        assertEquals("plane y=64", r.describe(Mode.PLANE));
    }

    @Test
    void faceAlsoNeedsTheSameClickedFace() {
        PlacementRestriction r = new PlacementRestriction();
        r.allow(Mode.FACE, new Pos(0, 10, 0), "east", 'x');
        assertTrue(r.allow(Mode.FACE, new Pos(0, 11, 4), "east", 'x'));
        assertFalse(r.allow(Mode.FACE, new Pos(0, 11, 5), "up", 'y'));
        assertFalse(r.allow(Mode.FACE, new Pos(1, 11, 5), "east", 'x'));
    }

    @Test
    void columnGoesStraightOut() {
        PlacementRestriction r = new PlacementRestriction();
        r.allow(Mode.COLUMN, new Pos(3, 70, 3), "up", 'y');
        assertTrue(r.allow(Mode.COLUMN, new Pos(3, 75, 3), "up", 'y'));
        assertFalse(r.allow(Mode.COLUMN, new Pos(4, 71, 3), "up", 'y'));
    }

    @Test
    void lineLocksDirectionOnSecondBlock() {
        PlacementRestriction r = new PlacementRestriction();
        r.allow(Mode.LINE, new Pos(0, 64, 0), "up", 'y');
        assertEquals("line (place a 2nd block)", r.describe(Mode.LINE));
        assertFalse(r.allow(Mode.LINE, new Pos(1, 64, 1), "up", 'y'));
        assertTrue(r.allow(Mode.LINE, new Pos(0, 64, 2), "up", 'y'));
        assertEquals("line along z", r.describe(Mode.LINE));
        assertTrue(r.allow(Mode.LINE, new Pos(0, 64, -4), "up", 'y'));
        assertFalse(r.allow(Mode.LINE, new Pos(3, 64, 0), "up", 'y'));
        assertFalse(r.allow(Mode.LINE, new Pos(0, 65, 0), "up", 'y'));
    }

    @Test
    void resetStartsANewStreamAndOffAllowsAll() {
        PlacementRestriction r = new PlacementRestriction();
        r.allow(Mode.PLANE, new Pos(0, 64, 0), "up", 'y');
        r.reset();
        assertTrue(r.allow(Mode.PLANE, new Pos(0, 80, 0), "up", 'y'));
        assertTrue(r.allow(Mode.OFF, new Pos(9, 9, 9), "up", 'y'));
        assertEquals(Mode.PLANE, Mode.OFF.next());
        assertEquals(Mode.OFF, Mode.LINE.next());
        assertEquals(Mode.COLUMN, Mode.parse(" Column "));
    }
}
