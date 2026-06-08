package me.m0dii.modules.utilitycommands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MathExpressionEvaluator {
    private final String input;
    private int index;

    private MathExpressionEvaluator(String input) {
        this.input = input == null ? "" : input;
    }

    public static double evaluate(String expression) {
        MathExpressionEvaluator evaluator = new MathExpressionEvaluator(expression);
        double value = evaluator.parseExpression();
        evaluator.skipWhitespace();
        if (!evaluator.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected token at position " + (evaluator.index + 1));
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Expression did not evaluate to a finite number");
        }
        return value;
    }

    public static String formatResult(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Result is not finite");
        }
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipWhitespace();
            if (consume('+')) {
                value += parseTerm();
            } else if (consume('-')) {
                value -= parseTerm();
            } else {
                return value;
            }
        }
    }

    private double parseTerm() {
        double value = parsePower();
        while (true) {
            skipWhitespace();
            if (consume('*')) {
                value *= parsePower();
            } else if (consume('/')) {
                double divisor = parsePower();
                if (Math.abs(divisor) < 1.0E-12) {
                    throw new IllegalArgumentException("Division by zero");
                }
                value /= divisor;
            } else {
                return value;
            }
        }
    }

    private double parsePower() {
        double base = parseUnary();
        skipWhitespace();
        if (consume('^')) {
            double exponent = parsePower();
            return Math.pow(base, exponent);
        }
        return base;
    }

    private double parseUnary() {
        skipWhitespace();
        if (consume('+')) {
            return parseUnary();
        }
        if (consume('-')) {
            return -parseUnary();
        }
        return parsePostfix();
    }

    private double parsePostfix() {
        double value = parsePrimary();
        while (true) {
            skipWhitespace();
            if (consume('%')) {
                value /= 100.0;
            } else {
                return value;
            }
        }
    }

    private double parsePrimary() {
        skipWhitespace();
        if (consume('(')) {
            double value = parseExpression();
            expect(')');
            return value;
        }

        if (peekLetter()) {
            String identifier = parseIdentifier();
            skipWhitespace();
            if (consume('(')) {
                List<Double> args = new ArrayList<>();
                skipWhitespace();
                if (!consume(')')) {
                    do {
                        args.add(parseExpression());
                        skipWhitespace();
                    } while (consume(','));
                    expect(')');
                }
                return applyFunction(identifier, args);
            }
            return resolveConstant(identifier);
        }

        return parseNumber();
    }

    private double parseNumber() {
        skipWhitespace();
        int start = index;
        boolean sawDigit = false;
        while (!isAtEnd() && Character.isDigit(currentChar())) {
            sawDigit = true;
            index++;
        }
        if (!isAtEnd() && currentChar() == '.') {
            index++;
            while (!isAtEnd() && Character.isDigit(currentChar())) {
                sawDigit = true;
                index++;
            }
        }
        if (!sawDigit) {
            throw new IllegalArgumentException("Expected a number at position " + (index + 1));
        }
        return Double.parseDouble(input.substring(start, index));
    }

    private double resolveConstant(String identifier) {
        return switch (identifier.toLowerCase(Locale.ROOT)) {
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            default -> throw new IllegalArgumentException("Unknown constant '" + identifier + "'");
        };
    }

    private double applyFunction(String identifier, List<Double> args) {
        String key = identifier.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "sqrt" -> unary(identifier, args, value -> {
                if (value < 0.0) {
                    throw new IllegalArgumentException("sqrt() requires a non-negative value");
                }
                return Math.sqrt(value);
            });
            case "sin" -> unary(identifier, args, value -> Math.sin(Math.toRadians(value)));
            case "cos" -> unary(identifier, args, value -> Math.cos(Math.toRadians(value)));
            case "tan" -> unary(identifier, args, value -> Math.tan(Math.toRadians(value)));
            case "asin" -> unary(identifier, args, value -> Math.toDegrees(Math.asin(value)));
            case "acos" -> unary(identifier, args, value -> Math.toDegrees(Math.acos(value)));
            case "atan" -> unary(identifier, args, value -> Math.toDegrees(Math.atan(value)));
            case "abs" -> unary(identifier, args, Math::abs);
            case "floor" -> unary(identifier, args, Math::floor);
            case "ceil", "ceiling" -> unary(identifier, args, Math::ceil);
            case "round" -> unary(identifier, args, value -> (double) Math.round(value));
            case "ln" -> unary(identifier, args, value -> {
                if (value <= 0.0) {
                    throw new IllegalArgumentException("ln() requires a positive value");
                }
                return Math.log(value);
            });
            case "log" -> unary(identifier, args, value -> {
                if (value <= 0.0) {
                    throw new IllegalArgumentException("log() requires a positive value");
                }
                return Math.log10(value);
            });
            case "pow" -> binary(identifier, args, Math::pow);
            case "root" -> {
                ensureArgCount(identifier, args, 2);
                double value = args.get(0);
                double degree = args.get(1);
                if (Math.abs(degree) < 1.0E-12) {
                    throw new IllegalArgumentException("root() degree must not be zero");
                }
                yield Math.pow(value, 1.0 / degree);
            }
            case "min" -> atLeast(identifier, args, 2, list -> list.stream().min(Double::compareTo).orElseThrow());
            case "max" -> atLeast(identifier, args, 2, list -> list.stream().max(Double::compareTo).orElseThrow());
            case "clamp" -> {
                ensureArgCount(identifier, args, 3);
                yield Math.clamp(args.get(0), args.get(1), args.get(2));
            }
            case "mod" -> {
                ensureArgCount(identifier, args, 2);
                if (Math.abs(args.get(1)) < 1.0E-12) {
                    throw new IllegalArgumentException("mod() divisor must not be zero");
                }
                yield args.get(0) % args.get(1);
            }
            default -> throw new IllegalArgumentException("Unknown function '" + identifier + "'");
        };
    }

    private static double unary(String identifier, List<Double> args, DoubleUnary op) {
        ensureArgCount(identifier, args, 1);
        return op.apply(args.getFirst());
    }

    private static double binary(String identifier, List<Double> args, DoubleBinary op) {
        ensureArgCount(identifier, args, 2);
        return op.apply(args.get(0), args.get(1));
    }

    private static double atLeast(String identifier, List<Double> args, int minArgs, DoubleList op) {
        if (args.size() < minArgs) {
            throw new IllegalArgumentException(identifier + "() expects at least " + minArgs + " arguments");
        }
        return op.apply(args);
    }

    private static void ensureArgCount(String identifier, List<Double> args, int expected) {
        if (args.size() != expected) {
            throw new IllegalArgumentException(identifier + "() expects " + expected + " argument(s)");
        }
    }

    private String parseIdentifier() {
        int start = index;
        while (!isAtEnd() && (Character.isLetterOrDigit(currentChar()) || currentChar() == '_')) {
            index++;
        }
        return input.substring(start, index);
    }

    private void expect(char expected) {
        skipWhitespace();
        if (!consume(expected)) {
            throw new IllegalArgumentException("Expected '" + expected + "' at position " + (index + 1));
        }
    }

    private boolean peekLetter() {
        return !isAtEnd() && Character.isLetter(currentChar());
    }

    private boolean consume(char expected) {
        if (!isAtEnd() && currentChar() == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(currentChar())) {
            index++;
        }
    }

    private char currentChar() {
        return input.charAt(index);
    }

    private boolean isAtEnd() {
        return index >= input.length();
    }

    @FunctionalInterface
    private interface DoubleUnary {
        double apply(double value);
    }

    @FunctionalInterface
    private interface DoubleBinary {
        double apply(double left, double right);
    }

    @FunctionalInterface
    private interface DoubleList {
        double apply(List<Double> values);
    }
}
