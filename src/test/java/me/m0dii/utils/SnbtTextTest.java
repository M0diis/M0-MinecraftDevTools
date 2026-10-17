package me.m0dii.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnbtTextTest {
    @Test
    void formatsNestedDataAndKeepsShortListsInline() {
        String out = SnbtText.format("{a:1b,b:{c:\"x, y{\"},list:[1,2,3],empty:{},arr:[I;1,2]}");
        assertEquals("""
                {
                  a: 1b,
                  b: {
                    c: "x, y{"
                  },
                  list: [1, 2, 3],
                  empty: {},
                  arr: [I; 1, 2]
                }""", out);
    }

    @Test
    void formatIsStableAndCompactUndoesIt() {
        String raw = "{name:'it''s',tags:[{id:\"a\"},{id:\"b\"}]}";
        String once = SnbtText.format(raw);
        assertEquals(once, SnbtText.format(once));
        assertEquals(SnbtText.compact(raw), SnbtText.compact(once));
    }

    @Test
    void escapedQuotesStayInsideStrings() {
        String raw = "{a:\"say \\\"hi\\\" {\",b:'x\\'y'}";
        assertEquals(raw, SnbtText.compact(SnbtText.format(raw)));
        List<SnbtText.Token> tokens = SnbtText.tokenize("\"q\\\"x\": 'v'");
        assertEquals(SnbtText.Kind.KEY, tokens.get(0).kind());
        assertEquals(6, tokens.get(0).end());
        assertEquals(SnbtText.Kind.STRING, tokens.get(tokens.size() - 1).kind());
    }

    @Test
    void classifiesWords() {
        List<SnbtText.Token> tokens = SnbtText.tokenize("Count: 3b, ok: true, f: -1.5e3f, id: minecraft:stone");
        assertEquals(SnbtText.Kind.KEY, tokens.get(0).kind());
        assertTrue(tokens.stream().anyMatch(t -> t.kind() == SnbtText.Kind.BOOLEAN));
        long numbers = tokens.stream().filter(t -> t.kind() == SnbtText.Kind.NUMBER).count();
        assertEquals(2L, numbers);
    }

    @Test
    void matchesBracketsOutsideStrings() {
        String text = "{a:[\"]\",{b:1}]}";
        assertEquals(text.length() - 1, SnbtText.matchingBracket(text, 0));
        assertEquals(13, SnbtText.matchingBracket(text, 3));
        assertEquals(3, SnbtText.matchingBracket(text, 13));
        assertEquals(-1, SnbtText.matchingBracket("{a:[1}", 3));
        assertTrue(SnbtText.insideString(text, 5));
        assertFalse(SnbtText.insideString(text, 8));
    }

    @Test
    void lineColAndNewlineIndent() {
        String text = "{\n  a: {\n";
        assertEquals(new SnbtText.LineCol(1, 4), SnbtText.lineCol(text, 6));
        assertEquals("\n    ", SnbtText.newlineInsert("{\n  a: {", 8, "  "));
        assertEquals("\n  ", SnbtText.newlineInsert("{\n  a: 1,", 9, "  "));
        assertEquals("\n  \n", SnbtText.newlineInsert("{}", 1, "  "));
    }

    @Test
    void wordMoves() {
        String text = "{display: minecraft:stone}";
        assertEquals(1, SnbtText.wordLeft(text, 8));
        assertEquals(19, SnbtText.wordRight(text, 9));
    }
}
