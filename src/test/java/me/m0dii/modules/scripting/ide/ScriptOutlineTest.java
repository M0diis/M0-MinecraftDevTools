package me.m0dii.modules.scripting.ide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptOutlineTest {
    private static List<String> labels(List<ScriptOutline.Symbol> symbols) {
        return symbols.stream().map(s -> s.label() + "@" + s.line()).toList();
    }

    @Test
    void kotlin() {
        String src = "val radius = 8\nfun scan(r: Int) {\n    val local = 1\n}\nprivate class Helper\n/* fun hidden() */\n// fun nope()\ndata class Box(val a: Int)";
        assertEquals(List.of("val radius@0", "fun scan@1", "class Helper@4", "class Box@7"), labels(ScriptOutline.symbols(src, "kotlin")));
    }

    @Test
    void groovy() {
        String src = "def count = 0\ndef greet(name) {\n  println name\n}\nString label(int x) {\n}\nclass Foo {}";
        assertEquals(List.of("def count@0", "def fun greet@1", "method label@4", "class Foo@6"), labels(ScriptOutline.symbols(src, "groovy")));
    }

    @Test
    void javascript() {
        String src = "const LIMIT = 5\nfunction tick() {\n  let i = 0\n}\nasync function load() {}\nclass Thing {}";
        assertEquals(List.of("const LIMIT@0", "function tick@1", "function load@4", "class Thing@5"), labels(ScriptOutline.symbols(src, "javascript")));
    }
}
