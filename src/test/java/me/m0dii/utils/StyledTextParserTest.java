package me.m0dii.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StyledTextParserTest {

    @Test
    void parsesLegacyColorsAndFormatting() {
        List<StyledTextParser.StyledFragment> fragments = StyledTextParser.parseFragments("&cRed &lBold&r Plain");

        assertEquals(3, fragments.size());
        assertEquals("Red ", fragments.get(0).text());
        assertEquals(0xFFFF5555, fragments.get(0).colorArgb());
        assertFalse(fragments.get(0).bold());

        assertEquals("Bold", fragments.get(1).text());
        assertEquals(0xFFFF5555, fragments.get(1).colorArgb());
        assertTrue(fragments.get(1).bold());

        assertEquals(" Plain", fragments.get(2).text());
        assertNull(fragments.get(2).colorArgb());
        assertFalse(fragments.get(2).bold());
    }

    @Test
    void parsesAngleHexAndLoreLines() {
        List<StyledTextParser.StyledFragment> fragments = StyledTextParser.parseFragments("<#00ff00>Green");
        assertEquals(1, fragments.size());
        assertEquals("Green", fragments.getFirst().text());
        assertEquals(0xFF00FF00, fragments.getFirst().colorArgb());

        assertEquals(2, StyledTextParser.parseLines("&7Line 1\n<#ff5555>Danger").size());
    }
}
