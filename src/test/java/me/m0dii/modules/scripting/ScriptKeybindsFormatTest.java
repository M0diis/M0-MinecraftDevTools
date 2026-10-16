package me.m0dii.modules.scripting;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptKeybindsFormatTest {
    @Test
    void convertsLegacyBareMap() {
        Map<String, Integer> out = new HashMap<>();
        assertTrue(ScriptKeybindsFormat.parse("{\"heal.groovy\": 75.0, \"broken.js\": 9999}", out));
        assertEquals(Map.of("heal.groovy", InputConstants.KEY_K), out);
    }

    @Test
    void readsVersionedFormatUnchanged() {
        Map<String, Integer> out = new HashMap<>();
        String json = "{\"formatVersion\": 2, \"keybinds\": {\"heal.groovy\": " + InputConstants.KEY_K + "}}";
        assertFalse(ScriptKeybindsFormat.parse(json, out));
        assertEquals(Map.of("heal.groovy", InputConstants.KEY_K), out);
    }
}
