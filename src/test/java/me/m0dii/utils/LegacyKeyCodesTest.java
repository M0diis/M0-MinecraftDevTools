package me.m0dii.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyKeyCodesTest {
    @Test
    void convertsLettersDigitsAndFunctionKeys() {
        assertEquals(InputConstants.KEY_K, LegacyKeyCodes.fromGlfw(75));
        assertEquals(InputConstants.KEY_A, LegacyKeyCodes.fromGlfw(65));
        assertEquals(InputConstants.KEY_Z, LegacyKeyCodes.fromGlfw(90));
        assertEquals(InputConstants.KEY_0, LegacyKeyCodes.fromGlfw(48));
        assertEquals(InputConstants.KEY_1, LegacyKeyCodes.fromGlfw(49));
        assertEquals(InputConstants.KEY_9, LegacyKeyCodes.fromGlfw(57));
        assertEquals(InputConstants.KEY_F1, LegacyKeyCodes.fromGlfw(290));
        assertEquals(InputConstants.KEY_F12, LegacyKeyCodes.fromGlfw(301));
        assertEquals(InputConstants.KEY_F13, LegacyKeyCodes.fromGlfw(302));
    }

    @Test
    void convertsNavigationModifiersAndKeypad() {
        assertEquals(InputConstants.KEY_ESCAPE, LegacyKeyCodes.fromGlfw(256));
        assertEquals(InputConstants.KEY_RETURN, LegacyKeyCodes.fromGlfw(257));
        assertEquals(InputConstants.KEY_UP, LegacyKeyCodes.fromGlfw(265));
        assertEquals(InputConstants.KEY_PAGEUP, LegacyKeyCodes.fromGlfw(266));
        assertEquals(InputConstants.KEY_LSHIFT, LegacyKeyCodes.fromGlfw(340));
        assertEquals(InputConstants.KEY_RCONTROL, LegacyKeyCodes.fromGlfw(345));
        assertEquals(InputConstants.KEY_NUMPAD0, LegacyKeyCodes.fromGlfw(320));
        assertEquals(InputConstants.KEY_NUMPAD9, LegacyKeyCodes.fromGlfw(329));
        assertEquals(InputConstants.KEY_NUMPADENTER, LegacyKeyCodes.fromGlfw(335));
    }

    @Test
    void keepsUnboundAndDropsUnknownKeys() {
        assertEquals(-1, LegacyKeyCodes.fromGlfw(-1));
        assertEquals(-1, LegacyKeyCodes.fromGlfw(9999));
    }
}
