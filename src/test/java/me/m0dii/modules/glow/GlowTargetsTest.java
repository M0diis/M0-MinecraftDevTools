package me.m0dii.modules.glow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlowTargetsTest {
    @Test
    void toggleAndColors() {
        GlowTargets targets = new GlowTargets();
        assertTrue(targets.toggle("zombie", null));
        assertEquals(Integer.valueOf(0xFFFFFF), targets.colorFor("minecraft:zombie", "hostile"));
        assertTrue(targets.toggle("zombie", 0xFF5555));
        assertEquals(Integer.valueOf(0xFF5555), targets.colorFor("minecraft:zombie"));
        assertFalse(targets.toggle("minecraft:zombie", null));
        assertNull(targets.colorFor("minecraft:zombie", "hostile"));
    }

    @Test
    void groupsAndPriority() {
        GlowTargets targets = new GlowTargets();
        targets.toggle("hostile", null);
        targets.toggle("creeper", 0x55FF55);
        assertEquals(Integer.valueOf(0xFF5555), targets.colorFor("minecraft:zombie", "hostile"));
        assertEquals(Integer.valueOf(0x55FF55), targets.colorFor("minecraft:creeper", "hostile"));
        assertNull(targets.colorFor("minecraft:cow", "passive"));
        targets.toggle("all", 0xAAAAAA);
        assertEquals(Integer.valueOf(0xAAAAAA), targets.colorFor("minecraft:cow", "passive"));
        targets.clear();
        assertTrue(targets.isEmpty());
    }

    @Test
    void parsing() {
        assertEquals(Integer.valueOf(0xFF5555), GlowTargets.parseColor("Red"));
        assertEquals(Integer.valueOf(0x12AB34), GlowTargets.parseColor("#12ab34"));
        assertNull(GlowTargets.parseColor("nope"));
        assertEquals("hostile", GlowTargets.normalize("Hostile"));
        assertEquals("minecraft:pig", GlowTargets.normalize("pig"));
        assertEquals("mymod:thing", GlowTargets.normalize("mymod:thing"));
    }
}
