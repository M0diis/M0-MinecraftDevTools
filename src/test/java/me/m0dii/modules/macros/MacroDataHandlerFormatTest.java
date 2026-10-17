package me.m0dii.modules.macros;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MacroDataHandlerFormatTest {
    private static final String LEGACY = """
            {
              "creative": {"name": "Creative", "commands": ["gamemode creative"], "keyCode": 75,
                           "modifierKey": "key.keyboard.left.shift", "delayTicks": 2, "showInOverlay": true},
              "unbound": {"name": "Unbound", "commands": [], "keyCode": -1, "delayTicks": 0, "showInOverlay": false}
            }
            """;

    @Test
    void convertsLegacyFileAndKeepsOtherFields() {
        MacroDataHandler.LoadResult result = MacroDataHandler.parse(LEGACY);
        assertTrue(result.convertedLegacyKeyCodes());

        MacroDataHandler.MacroEntry creative = result.macros().get("creative");
        assertEquals(InputConstants.KEY_K, creative.keyCode);
        assertEquals("Creative", creative.name);
        assertEquals(List.of("gamemode creative"), creative.commands);
        assertEquals("key.keyboard.left.shift", creative.modifierKey);
        assertEquals(2, creative.delayTicks);
        assertTrue(creative.showInOverlay);
        assertEquals(-1, result.macros().get("unbound").keyCode);
    }

    @Test
    void savedFormatRoundTripsWithoutReconversion() {
        Map<String, MacroDataHandler.MacroEntry> converted = MacroDataHandler.parse(LEGACY).macros();
        MacroDataHandler.LoadResult reloaded = MacroDataHandler.parse(MacroDataHandler.serialize(converted));
        assertFalse(reloaded.convertedLegacyKeyCodes());
        assertEquals(InputConstants.KEY_K, reloaded.macros().get("creative").keyCode);
        assertEquals(List.of("creative", "unbound"), List.copyOf(reloaded.macros().keySet()));
    }

    @Test
    void nonObjectFileIsAnErrorNotAnEmptyMacroList() {
        // An unreadable file must fail loudly so it gets backed up instead of silently overwritten.
        assertThrows(RuntimeException.class, () -> MacroDataHandler.parse("[1, 2, 3]"));
        assertThrows(RuntimeException.class, () -> MacroDataHandler.parse("{ \"formatVersion\": 2, \"macros\": "));
    }

    @Test
    void emptyFileLoadsAsNoMacros() {
        assertTrue(MacroDataHandler.parse("").macros().isEmpty());
        assertTrue(MacroDataHandler.parse("   ").macros().isEmpty());
    }

    @Test
    void nullEntriesAreSkipped() {
        String json = """
                {"formatVersion": 2, "macros": {"a": null, "b": {"name": "B", "commands": [], "keyCode": -1}}}
                """;
        Map<String, MacroDataHandler.MacroEntry> macros = MacroDataHandler.parse(json).macros();
        assertEquals(List.of("b"), List.copyOf(macros.keySet()));
    }
}
