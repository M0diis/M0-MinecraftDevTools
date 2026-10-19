package me.m0dii.modules.chathighlight;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HighlightRulesTest {
    private static HighlightRules.Rule rule(String pattern, String color, boolean regex, boolean sound) {
        HighlightRules.Rule rule = new HighlightRules.Rule();
        rule.pattern = pattern;
        rule.color = color;
        rule.regex = regex;
        rule.sound = sound;
        return rule;
    }

    @Test
    void plainWordsMatchWholeWordsCaseInsensitively() {
        HighlightRules.Result result = HighlightRules.match(List.of(rule("diamond", "aqua", false, false)),
                "Found DIAMOND and diamonds", "Steve");
        assertEquals(1, result.spans().size());
        assertEquals(6, result.spans().getFirst().start());
        assertEquals(13, result.spans().getFirst().end());
    }

    @Test
    void meMatchesPlayerNameButNotOwnLines() {
        List<HighlightRules.Rule> rules = List.of(rule("{me}", "gold", false, true));
        HighlightRules.Result mention = HighlightRules.match(rules, "<Alex> hi steve!", "Steve");
        assertTrue(mention.matched());
        assertTrue(mention.sound());
        assertFalse(HighlightRules.match(rules, "<Steve> hello Steve", "Steve").matched());
        assertFalse(HighlightRules.match(rules, "<Alex> Steven", "Steve").matched());
    }

    @Test
    void regexRulesAndEarlierRulesWin() {
        List<HighlightRules.Rule> rules = List.of(
                rule("error", "red", false, false),
                rule("err\\w*", "gold", true, false));
        HighlightRules.Result result = HighlightRules.match(rules, "error errors", null);
        assertEquals(2, result.spans().size());
        assertEquals(0xFF5555, result.spans().get(0).rgb());
        assertEquals(0xFFAA00, result.spans().get(1).rgb());
    }

    @Test
    void parsesColours() {
        assertEquals(0xFFAA00, HighlightRules.parseColor("gold"));
        assertEquals(0x12AB34, HighlightRules.parseColor("#12ab34"));
        assertEquals(-1, HighlightRules.parseColor("nope"));
        assertNull(HighlightRules.compile(rule("(", "red", true, false), "x"));
    }

    @Test
    void splitKeepsStylesAndCutsAtSpans() {
        List<Map.Entry<String, String>> parts = List.of(
                new AbstractMap.SimpleEntry<>("A", "hello wo"),
                new AbstractMap.SimpleEntry<>("B", "rld!"));
        List<HighlightRules.Span> spans = List.of(new HighlightRules.Span(6, 11, 0x123456));
        List<HighlightRules.Segment<String>> segments = HighlightRules.split(parts, spans);
        assertEquals(List.of(
                new HighlightRules.Segment<>("A", "hello ", null),
                new HighlightRules.Segment<>("A", "wo", 0x123456),
                new HighlightRules.Segment<>("B", "rld", 0x123456),
                new HighlightRules.Segment<>("B", "!", null)), segments);
    }

    @Test
    void catastrophicRegexIsAbortedInsteadOfFreezing() {
        List<HighlightRules.Rule> rules = List.of(
                rule("(.*a){20}", "red", true, false),
                rule("steve", "gold", false, false));
        String text = "a".repeat(60) + "!";
        HighlightRules.Result result = assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> HighlightRules.match(rules, text, "Alex"));
        assertNotNull(result);
        // The slow rule is abandoned; the message still comes back (nothing to highlight here).
        assertFalse(result.matched());
    }
}
