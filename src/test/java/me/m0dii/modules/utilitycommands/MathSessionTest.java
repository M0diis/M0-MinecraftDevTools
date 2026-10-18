package me.m0dii.modules.utilitycommands;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MathSessionTest {
    @Test
    void plainMathStillWorks() {
        MathSession session = new MathSession();
        assertEquals("5 + 5 = 10", session.run("5 + 5", Map.of()));
        assertEquals("ans * 2 = 20", session.run("ans * 2", Map.of()));
    }

    @Test
    void variables() {
        MathSession session = new MathSession();
        assertEquals("x = 320", session.run("x = 5 * 64", Map.of()));
        assertEquals("x / 2 = 160", session.run("x / 2", Map.of()));
        assertEquals(Map.of("x", 320.0), session.variables());
        assertThrows(IllegalArgumentException.class, () -> session.run("stacks = 3", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new MathSession().run("ans + 1", Map.of()));
        session.clear();
        assertTrue(session.variables().isEmpty());
    }

    @Test
    void itemUnits() {
        MathSession session = new MathSession();
        assertEquals("3 stacks + 5 = 197 items (197 items = 3 stacks + 5, 0.114 shulker boxes)", session.run("3 stacks + 5", Map.of()));
        assertEquals("3000 in stacks = 46.875 stacks (3000 items = 46 stacks + 56, 1.736 shulker boxes)", session.run("3000 in stacks", Map.of()));
        assertTrue(session.run("2 dc in shulkers", Map.of()).startsWith("2 dc in shulkers = 4 shulker boxes"));
    }

    @Test
    void timeUnits() {
        MathSession session = new MathSession();
        assertEquals("1200 ticks in s = 60 seconds (1200 ticks, 60 s, 1:00)", session.run("1200 ticks in s", Map.of()));
        assertTrue(session.run("2.5 min", Map.of()).startsWith("2.5 min = 3000 ticks"));
        assertTrue(session.run("90 min to h", Map.of()).startsWith("90 min to h = 1.5 hours"));
    }

    @Test
    void netherAndOverworld() {
        MathSession session = new MathSession();
        assertTrue(session.run("800 nether in overworld", Map.of()).startsWith("800 nether in overworld = 6400 blocks"));
        assertTrue(session.run("px in nether", Map.of("px", 1000.0)).startsWith("px in nether = 125 nether blocks"));
        assertThrows(IllegalArgumentException.class, () -> session.run("5 stacks in s", Map.of()));
    }

    @Test
    void wordsThatAreNotUnitsStayErrors() {
        MathSession session = new MathSession();
        assertThrows(IllegalArgumentException.class, () -> session.run("3 bananas", Map.of()));
        assertEquals("sqrt(16) = 4", session.run("sqrt(16)", Map.of()));
    }
}
