package me.m0dii.modules.scripting.ide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LineOpsTest {
    @Test
    void togglesCommentsOnBlocks() {
        String text = "fun a() {\n    one()\n    two()\n}";
        int start = text.indexOf("one");
        int end = text.indexOf("two") + 2;
        LineOps.Edit on = LineOps.toggleComment(text, end, start, "//");
        assertEquals("fun a() {\n    // one()\n    // two()\n}", on.text());
        LineOps.Edit off = LineOps.toggleComment(on.text(), on.cursor(), on.anchor(), "//");
        assertEquals(text, off.text());
        LineOps.Edit single = LineOps.toggleComment("x()", 1, -1, "//");
        assertEquals("// x()", single.text());
        assertEquals(4, single.cursor());
    }

    @Test
    void duplicatesLineOrSelection() {
        LineOps.Edit line = LineOps.duplicate("a\nbc\nd", 3, -1);
        assertEquals("a\nbc\nbc\nd", line.text());
        assertEquals(6, line.cursor());
        LineOps.Edit sel = LineOps.duplicate("hello", 3, 1);
        assertEquals("helello", sel.text());
    }

    @Test
    void movesLines() {
        LineOps.Edit up = LineOps.moveLines("a\nb\nc", 2, -1, -1);
        assertEquals("b\na\nc", up.text());
        assertEquals(0, up.cursor());
        LineOps.Edit down = LineOps.moveLines("a\nb\nc", 2, -1, 1);
        assertEquals("a\nc\nb", down.text());
        assertEquals(4, down.cursor());
        assertEquals("a\nb", LineOps.moveLines("a\nb", 0, -1, -1).text());
        assertEquals("a\nb", LineOps.moveLines("a\nb", 2, -1, 1).text());
    }

    @Test
    void deletesLines() {
        assertEquals("a\nc", LineOps.deleteLines("a\nb\nc", 2, -1).text());
        assertEquals("a\nb", LineOps.deleteLines("a\nb\nc", 4, -1).text());
        assertEquals("", LineOps.deleteLines("only", 2, -1).text());
    }

    @Test
    void homeAndWords() {
        String text = "    val x = 1";
        assertEquals(4, LineOps.smartHome(text, 9));
        assertEquals(0, LineOps.smartHome(text, 4));
        assertEquals(8, LineOps.wordLeft(text, 9));
        assertEquals(4, LineOps.wordLeft(text, 8));
        assertEquals(8, LineOps.wordRight(text, 4));
    }

    @Test
    void pairs() {
        assertEquals(')', LineOps.autoClose("foo", 3, '('));
        assertEquals(0, LineOps.autoClose("foo", 1, '('));
        assertEquals('"', LineOps.autoClose("x = ", 4, '"'));
        assertEquals(0, LineOps.autoClose("don", 3, '\''));
        assertTrue(LineOps.skipsClosing("a()", 2, ')'));
        assertFalse(LineOps.skipsClosing("a()", 1, ')'));
        assertTrue(LineOps.betweenEmptyPair("f()", 2));
        assertFalse(LineOps.betweenEmptyPair("f(x)", 2));
    }
}
