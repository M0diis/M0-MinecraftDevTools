package me.m0dii.modules.scripting.ide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScriptInfoTest {
    @Test
    void readsHeader() {
        ScriptInfo info = ScriptInfo.of("farm.kts", "// @async on\n// Farms bonemeal every 5 seconds\n\nm0.print(1)\n");
        assertEquals("Kotlin", info.language());
        assertEquals(4, info.lines());
        assertTrue(info.async());
        assertEquals("Farms bonemeal every 5 seconds", info.description());
    }

    @Test
    void noHeader() {
        ScriptInfo info = ScriptInfo.of("x.groovy", "println 'hi'\n// @async on");
        assertEquals("Groovy", info.language());
        assertFalse(info.async());
        assertEquals("", info.description());
        assertEquals("JavaScript", ScriptInfo.of("a.js", "").language());
        assertEquals(0, ScriptInfo.of("a.js", "").lines());
        assertFalse(ScriptInfo.of("a.js", "/* @async off */\nx()").async());
        assertEquals("Doc text", ScriptInfo.of("a.js", "/** Doc text */\nx()").description());
    }

    @Test
    void preview() {
        assertEquals(List.of("a", "    b"), ScriptInfo.preview("a\n\tb\nc", 2));
    }
}
