package me.m0dii.modules.scripting.ide;

import me.m0dii.modules.scripting.ide.ScriptSyntax.Kind;
import me.m0dii.modules.scripting.ide.ScriptSyntax.State;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptSyntaxTest {
    private static Kind kindAt(ScriptSyntax.LineResult result, int index) {
        for (ScriptSyntax.Span span : result.spans()) {
            if (index >= span.start() && index < span.end()) {
                return span.kind();
            }
        }
        return null;
    }

    @Test
    void kotlinLine() {
        String line = "val name = m0.message(\"hi\") // done";
        ScriptSyntax.LineResult r = ScriptSyntax.line(line, "kotlin", State.CODE);
        assertEquals(Kind.KEYWORD, kindAt(r, 0));
        assertEquals(Kind.PLAIN, kindAt(r, 4));
        assertEquals(Kind.BUILTIN, kindAt(r, 11));
        assertEquals(Kind.FUNCTION, kindAt(r, 14));
        assertEquals(Kind.STRING, kindAt(r, 22));
        assertEquals(Kind.COMMENT, kindAt(r, line.indexOf("//") + 3));
        assertEquals(State.CODE, r.endState());
    }

    @Test
    void blockCommentsSpanLines() {
        List<String> lines = List.of("a /* start", "still comment", "end */ val x = 1", "val y");
        List<State> states = ScriptSyntax.startStates(lines, "kotlin");
        assertEquals(List.of(State.CODE, State.BLOCK_COMMENT, State.BLOCK_COMMENT, State.CODE), states);
        ScriptSyntax.LineResult third = ScriptSyntax.line(lines.get(2), "kotlin", State.BLOCK_COMMENT);
        assertEquals(Kind.COMMENT, kindAt(third, 0));
        assertEquals(Kind.KEYWORD, kindAt(third, 7));
        assertEquals(Kind.NUMBER, kindAt(third, lines.get(2).length() - 1));
    }

    @Test
    void tripleQuotesAndTemplates() {
        assertEquals(State.TRIPLE_STRING, ScriptSyntax.line("val s = \"\"\"multi", "kotlin", State.CODE).endState());
        ScriptSyntax.LineResult closing = ScriptSyntax.line("line\"\"\" + 1", "groovy", State.TRIPLE_STRING);
        assertEquals(Kind.STRING, kindAt(closing, 2));
        assertEquals(Kind.NUMBER, kindAt(closing, 10));
        assertEquals(State.TEMPLATE_STRING, ScriptSyntax.line("let t = `a ${b}", "javascript", State.CODE).endState());
        assertEquals(State.CODE, ScriptSyntax.line("`;", "javascript", State.TEMPLATE_STRING).endState());
    }

    @Test
    void escapesNumbersAnnotationsAndTypes() {
        ScriptSyntax.LineResult r = ScriptSyntax.line("@Suppress(\"a\\\"b\") val n = 0x1F + 1.5e3f + Foo.bar", "kotlin", State.CODE);
        assertEquals(Kind.ANNOTATION, kindAt(r, 1));
        assertEquals(Kind.STRING, kindAt(r, 14));
        String line = "@Suppress(\"a\\\"b\") val n = 0x1F + 1.5e3f + Foo.bar";
        assertEquals(Kind.NUMBER, kindAt(r, line.indexOf("0x1F") + 3));
        assertEquals(Kind.NUMBER, kindAt(r, line.indexOf("1.5e3f") + 5));
        assertEquals(Kind.TYPE, kindAt(r, line.indexOf("Foo")));
        assertEquals(Kind.KEYWORD, kindAt(ScriptSyntax.line("def x", "groovy", State.CODE), 0));
        assertEquals(Kind.PLAIN, kindAt(ScriptSyntax.line("def x", "kotlin", State.CODE), 0));
        assertEquals(Kind.KEYWORD, kindAt(ScriptSyntax.line("let x", "js", State.CODE), 0));
    }
}
