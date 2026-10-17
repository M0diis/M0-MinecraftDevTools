package me.m0dii.modules.scripting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScriptFormatterTest {
    private static String fmt(String source) {
        return ScriptFormatter.reindent(source, "    ");
    }

    @Test
    void indentsByBrackets() {
        String messy = "if (x) {\nfoo()\n  if (y) {\n bar()\n }\n}";
        assertEquals("if (x) {\n    foo()\n    if (y) {\n        bar()\n    }\n}", fmt(messy));
    }

    @Test
    void handlesMultiLineCallArguments() {
        String messy = "call(a,\nb)\nnext()";
        assertEquals("call(a,\n    b)\nnext()", fmt(messy));
    }

    @Test
    void ignoresBracketsInStringsAndComments() {
        String messy = "def s = \"{ not a block\"\n// } neither\nx()";
        assertEquals("def s = \"{ not a block\"\n// } neither\nx()", fmt(messy));
    }

    @Test
    void keepsMultiLineStringsAndBlockCommentsUntouched() {
        String source = "def t = \"\"\"\n   keep {\n      this\n\"\"\"\n/*\n  { comment\n*/\nend()";
        String out = fmt(source);
        assertEquals(source, out);
    }

    @Test
    void neverGoesNegativeAndClearsBlankLines() {
        assertEquals("}\nx()\n\ny()", fmt("   }\n  x()\n   \ny()"));
        assertFalse(fmt("{\n{\n}").isEmpty());
    }
}
