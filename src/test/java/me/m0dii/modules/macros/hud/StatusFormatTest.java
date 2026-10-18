package me.m0dii.modules.macros.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusFormatTest {
    @Test
    void durabilityText() {
        assertEquals("50%", StatusFormat.durability(StatusFormat.TextMode.PERCENT, 100, 50));
        assertEquals("1561", StatusFormat.durability(StatusFormat.TextMode.REMAINING, 1561, 0));
        assertEquals("10 (1%)", StatusFormat.durability(StatusFormat.TextMode.BOTH, 1000, 990));
        assertEquals("", StatusFormat.durability(StatusFormat.TextMode.NONE, 100, 1));
        assertEquals("", StatusFormat.durability(StatusFormat.TextMode.PERCENT, 0, 0));
        assertEquals("0%", StatusFormat.durability(StatusFormat.TextMode.PERCENT, 100, 150));
    }

    @Test
    void lowAndColor() {
        assertTrue(StatusFormat.isLow(100, 90, 10));
        assertFalse(StatusFormat.isLow(100, 89, 10));
        assertFalse(StatusFormat.isLow(0, 0, 100));
        assertEquals(0xFF00FF40, StatusFormat.durabilityColor(1.0));
        assertEquals(0xFFFF0040, StatusFormat.durabilityColor(0.0));
        assertEquals(0xFFFFFF40, StatusFormat.durabilityColor(0.5));
    }

    @Test
    void timeAndLevels() {
        assertEquals("0:00", StatusFormat.duration(0));
        assertEquals("1:30", StatusFormat.duration(1800));
        assertEquals("1:00:05", StatusFormat.duration(72100));
        assertEquals("∞", StatusFormat.duration(-1));
        assertEquals("", StatusFormat.level(0));
        assertEquals("II", StatusFormat.level(1));
        assertEquals("X", StatusFormat.level(9));
        assertEquals("11", StatusFormat.level(10));
    }

    @Test
    void enumsParseAndCycle() {
        assertEquals(StatusFormat.Kind.EFFECTS, StatusFormat.Kind.parse("effects"));
        assertEquals(StatusFormat.Kind.ARMOR, StatusFormat.Kind.parse("bogus"));
        assertEquals(StatusFormat.Kind.ARMOR, StatusFormat.Kind.ALERT.next(true));
        assertEquals(StatusFormat.Kind.ALERT, StatusFormat.Kind.ARMOR.next(false));
        assertEquals(StatusFormat.TextMode.NONE, StatusFormat.TextMode.PERCENT.next(false));
        assertTrue(StatusFormat.blinkVisible(400, 250));
        assertNotEquals(StatusFormat.blinkVisible(100, 0), StatusFormat.blinkVisible(100, 500));
    }
}
