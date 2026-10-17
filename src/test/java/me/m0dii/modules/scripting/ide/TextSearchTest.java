package me.m0dii.modules.scripting.ide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSearchTest {
    @Test
    void findsCaseInsensitiveAndWholeWords() {
        String text = "val player = Player.of(players)\nplayer.go()";
        List<TextSearch.Match> all = TextSearch.findAll(text, "player", false, false);
        assertEquals(4, all.size());
        List<TextSearch.Match> words = TextSearch.findAll(text, "player", true, true);
        assertEquals(2, words.size());
        assertEquals(4, words.get(0).start());
        assertEquals(0, TextSearch.findAll(text, "", false, false).size());
    }

    @Test
    void nextAndPreviousWrap() {
        List<TextSearch.Match> m = TextSearch.findAll("ab ab ab", "ab", false, false);
        assertEquals(1, TextSearch.nextIndex(m, 1, false));
        assertEquals(0, TextSearch.nextIndex(m, 7, false));
        assertEquals(0, TextSearch.nextIndex(m, 3, true));
        assertEquals(2, TextSearch.nextIndex(m, 0, true));
        assertEquals(-1, TextSearch.nextIndex(List.of(), 0, false));
        assertEquals(1, TextSearch.indexAt(m, 3, 5));
    }

    @Test
    void replacesAll() {
        TextSearch.Replaced r = TextSearch.replaceAll("x = x + xx", "x", "y", true, true);
        assertEquals("y = y + xx", r.text());
        assertEquals(2, r.count());
        assertEquals(0, TextSearch.replaceAll("abc", "z", "q", true, false).count());
    }
}
