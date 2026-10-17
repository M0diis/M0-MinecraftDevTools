package me.m0dii.modules.automation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutomationRegexTest {
    @Test
    void findsAnywhereInTheText() {
        assertTrue(AutomationRegex.match("joined the game", "Alex joined the game", false).matched());
        assertFalse(AutomationRegex.match("^joined", "Alex joined the game", false).matched());
    }

    @Test
    void exposesNumberedAndNamedGroups() {
        AutomationRegex.Result result = AutomationRegex.match("(?<who>\\w+) joined", "Alex joined the game", false);
        assertEquals("Alex joined", result.groups().get("group0"));
        assertEquals("Alex", result.groups().get("group1"));
        assertEquals("Alex", result.groups().get("group.who"));
    }

    @Test
    void invalidPatternReportsAnError() {
        AutomationRegex.Result result = AutomationRegex.match("(unclosed", "anything", false);
        assertFalse(result.matched());
        assertNotNull(result.error());
    }

    @Test
    void pathologicalPatternIsCutOff() {
        long start = System.nanoTime();
        AutomationRegex.Result result = AutomationRegex.match("(.*a){12}x", "a".repeat(40), false);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertFalse(result.matched());
        assertNotNull(result.error());
        assertTrue(elapsedMs < 2_000L, "took " + elapsedMs + " ms");
    }
}
