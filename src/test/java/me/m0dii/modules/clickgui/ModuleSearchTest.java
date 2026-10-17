package me.m0dii.modules.clickgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleSearchTest {
    private static int score(String query, String name, String id, String category) {
        return ModuleSearch.score(name, id, category, query.toLowerCase().split("\\s+"));
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertEquals(0, score("", "Light Overlay", "light_overlay", "Overlays"));
    }

    @Test
    void prefixBeatsWordBeatsAnywhere() {
        int prefix = score("light", "Light Overlay", "light_overlay", "Overlays");
        int word = score("overlay", "Light Overlay", "light_overlay", "Overlays");
        int category = score("overlays", "Xray", "xray", "Overlays");
        assertTrue(prefix < word, prefix + " < " + word);
        assertTrue(word < category, word + " < " + category);
    }

    @Test
    void everyWordMustMatch() {
        assertEquals(-1, score("light chat", "Light Overlay", "light_overlay", "Overlays"));
        assertTrue(score("light over", "Light Overlay", "light_overlay", "Overlays") >= 0);
    }

    @Test
    void idsMatchWithSpaces() {
        assertTrue(score("bud", "Redstone Update Order", "redstone_bud_order", "Overlays") >= 0);
    }
}
