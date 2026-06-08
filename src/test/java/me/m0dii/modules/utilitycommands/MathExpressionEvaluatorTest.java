package me.m0dii.modules.utilitycommands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MathExpressionEvaluatorTest {

    @Test
    void evaluatesBasicArithmetic() {
        assertEquals(10.0, MathExpressionEvaluator.evaluate("5 + 5"), 1.0E-9);
        assertEquals(25.0, MathExpressionEvaluator.evaluate("(10 * 5) / 2"), 1.0E-9);
        assertEquals(65536.0, MathExpressionEvaluator.evaluate("2^16"), 1.0E-9);
    }

    @Test
    void evaluatesFunctionsAndConstants() {
        assertEquals(12.0, MathExpressionEvaluator.evaluate("sqrt(144)"), 1.0E-9);
        assertEquals(1.0, MathExpressionEvaluator.evaluate("sin(90)"), 1.0E-9);
        assertEquals(Math.PI * 2.0, MathExpressionEvaluator.evaluate("pi * 2"), 1.0E-9);
        assertEquals(5.0, MathExpressionEvaluator.evaluate("root(125, 3)"), 1.0E-9);
    }

    @Test
    void supportsPostfixPercent() {
        assertEquals(0.15, MathExpressionEvaluator.evaluate("15%"), 1.0E-9);
        assertEquals(30.0, MathExpressionEvaluator.evaluate("200 * 15%"), 1.0E-9);
    }

    @Test
    void rejectsInvalidExpressions() {
        assertThrows(IllegalArgumentException.class, () -> MathExpressionEvaluator.evaluate("5 +"));
        assertThrows(IllegalArgumentException.class, () -> MathExpressionEvaluator.evaluate("sqrt(-1)"));
        assertThrows(IllegalArgumentException.class, () -> MathExpressionEvaluator.evaluate("1 / 0"));
    }
}
