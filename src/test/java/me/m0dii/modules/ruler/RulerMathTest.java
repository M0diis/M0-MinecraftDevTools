package me.m0dii.modules.ruler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RulerMathTest {
    @Test
    void countsBothEndBlocks() {
        RulerMath m = RulerMath.between(0, 64, 0, 9, 66, 4);
        assertEquals(10, m.sizeX());
        assertEquals(3, m.sizeY());
        assertEquals(5, m.sizeZ());
        assertEquals(150, m.volume());
        assertEquals(50, m.area());
        assertEquals(15, m.manhattan());
    }

    @Test
    void sameBlockIsOneByOne() {
        RulerMath m = RulerMath.between(5, 5, 5, 5, 5, 5);
        assertEquals(1, m.volume());
        assertEquals(0.0, m.distance());
    }

    @Test
    void orderDoesNotMatter() {
        assertEquals(RulerMath.between(0, 0, 0, -3, 4, 0), RulerMath.between(-3, 4, 0, 0, 0, 0));
        assertEquals(5.0, RulerMath.between(0, 0, 0, 3, 4, 0).distance(), 1e-9);
    }
}
