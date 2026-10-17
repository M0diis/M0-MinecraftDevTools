package me.m0dii.modules.commandhistory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatHistorySearchTest {
    private static final List<String> HISTORY = List.of(
            "/gamemode creative", "/tp 0 64 0", "/give @s diamond 64", "/gamemode survival", "/time set day");

    @Test
    void substringMatchesComeFirstThenFuzzy() {
        assertEquals(List.of("/gamemode creative", "/gamemode survival"), ChatHistorySearch.find(HISTORY, "gamemode"));
        // "gmc" only matches fuzzily (g..m..c in order)
        assertEquals(List.of("/gamemode creative"), ChatHistorySearch.find(HISTORY, "gmc"));
        assertEquals(HISTORY, ChatHistorySearch.find(HISTORY, ""));
    }

    @Test
    void stepsThroughMatchesAndWraps() {
        ChatHistorySearch search = new ChatHistorySearch();
        String first = search.next(HISTORY, "game");
        assertEquals("/gamemode creative", first);
        String second = search.next(HISTORY, first);
        assertEquals("/gamemode survival", second);
        assertEquals("/gamemode creative", search.next(HISTORY, second));
        assertTrue(search.isShowing("/gamemode creative"));
        assertEquals("/gamemode survival", search.previous(HISTORY, "/gamemode creative"));
    }

    @Test
    void typingStartsANewSearch() {
        ChatHistorySearch search = new ChatHistorySearch();
        search.next(HISTORY, "game");
        assertEquals("/time set day", search.next(HISTORY, "time"));
        assertFalse(search.isShowing("time"));
    }

    @Test
    void noMatchIsReported() {
        ChatHistorySearch search = new ChatHistorySearch();
        assertNull(search.next(HISTORY, "zzz"));
        assertTrue(search.hadNoMatch("zzz"));
        assertTrue(search.status().contains("no match"));
    }
}
