package me.m0dii.modules.scripting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptSnippetsTest {
    @Test
    void everyLanguageHasSnippets() {
        assertFalse(ScriptSnippets.forLanguage("groovy").isEmpty());
        assertFalse(ScriptSnippets.forLanguage("kotlin").isEmpty());
        assertFalse(ScriptSnippets.forLanguage("javascript").isEmpty());
        assertEquals(ScriptSnippets.forLanguage("groovy"), ScriptSnippets.forLanguage(null));
    }

    @Test
    void snippetsAreAlreadyFormatted() {
        for (String language : new String[]{"groovy", "kotlin", "javascript"}) {
            for (ScriptSnippets.Snippet snippet : ScriptSnippets.forLanguage(language)) {
                assertEquals(snippet.body(), ScriptFormatter.reindent(snippet.body(), "    "), language + ": " + snippet.label());
            }
        }
    }

    @Test
    void indentsFollowingLines() {
        assertEquals("a\n    b", ScriptSnippets.indented("a\nb", "    "));
        assertTrue(ScriptSnippets.indented("x", "").equals("x"));
    }
}
