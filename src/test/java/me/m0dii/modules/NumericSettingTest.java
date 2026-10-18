package me.m0dii.modules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NumericSettingTest {
    private double stored;

    private NumericSetting setting(double min, double max, double step, int decimals) {
        return NumericSetting.of(3, "Radius", min, max, step, decimals, () -> stored, v -> stored = v, 4, 5);
    }

    @Test
    void snapsToStepAndRange() {
        NumericSetting s = setting(8, 256, 8, 0);
        s.set(50);
        assertEquals(48.0, stored, 1e-9);
        s.set(1000);
        assertEquals(256.0, stored, 1e-9);
        s.set(-3);
        assertEquals(8.0, stored, 1e-9);
        assertEquals(List.of(4, 5), s.hiddenRows());
    }

    @Test
    void decimalStepsStayClean() {
        NumericSetting s = setting(0.5, 5.0, 0.1, 1);
        s.set(1.23456);
        assertEquals(1.2, stored, 1e-9);
        assertEquals("1.2", s.format(stored));
        NumericSetting reach = setting(1, 16, 0.25, 2);
        reach.set(4.6);
        assertEquals(4.5, stored, 1e-9);
        assertEquals("4.50", reach.format(stored));
    }

    @Test
    void trackPositions() {
        NumericSetting s = setting(0, 100, 1, 0);
        stored = 25;
        assertEquals(0.25, s.fraction(), 1e-9);
        assertEquals(50.0, s.valueAt(0.5), 1e-9);
        assertEquals(100.0, s.valueAt(2), 1e-9);
        assertEquals("25", s.format(stored));
    }

    @Test
    void parsesTypedText() {
        assertEquals(1.5, NumericSetting.parse(" 1,5 "), 1e-9);
        assertEquals(-2.0, NumericSetting.parse("-2"), 1e-9);
        assertNull(NumericSetting.parse("abc"));
        assertNull(NumericSetting.parse(""));
        assertNull(NumericSetting.parse("NaN"));
    }
}
