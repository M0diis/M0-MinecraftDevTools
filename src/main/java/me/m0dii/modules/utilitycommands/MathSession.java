package me.m0dii.modules.utilitycommands;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /math} with memory and Minecraft units:
 * <ul>
 *     <li>{@code x = 5 * 64} stores a variable, {@code ans} is the last result</li>
 *     <li>units multiply: {@code 3 stacks + 5}, {@code 2.5 min}, {@code 300 nether}</li>
 *     <li>{@code ... in <unit>} converts: {@code 2000 in stacks}, {@code 1200 ticks in s}, {@code px in nether}</li>
 * </ul>
 * Pure Java; the command supplies player values (px, py, pz) through {@code extra}.
 */
public final class MathSession {
    public enum Dimension { ITEMS, TIME, DISTANCE }

    public record Unit(String name, Dimension dimension, double factor, String plural) {
    }

    private static final Map<String, Unit> UNITS = new LinkedHashMap<>();
    private static final Pattern ASSIGN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=(?!=)\\s*(.+)$");
    private static final Pattern CONVERT = Pattern.compile("^(.+?)\\s+(?:in|to|as)\\s+([a-zA-Z_]+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    static {
        unit(Dimension.ITEMS, 1, "items", "item", "items", "i");
        unit(Dimension.ITEMS, 16, "stacks of 16", "stack16", "stacks16");
        unit(Dimension.ITEMS, 64, "stacks", "stack", "stacks", "st");
        unit(Dimension.ITEMS, 1728, "shulker boxes", "shulker", "shulkers", "sb", "chest", "chests");
        unit(Dimension.ITEMS, 3456, "double chests", "dc", "doublechest", "doublechests");
        unit(Dimension.TIME, 1, "ticks", "tick", "ticks", "t", "gt");
        unit(Dimension.TIME, 2, "redstone ticks", "rt", "rtick", "rticks");
        unit(Dimension.TIME, 20, "seconds", "s", "sec", "second", "seconds");
        unit(Dimension.TIME, 1200, "minutes", "min", "mins", "minute", "minutes");
        unit(Dimension.TIME, 72000, "hours", "h", "hr", "hour", "hours");
        unit(Dimension.TIME, 24000, "Minecraft days", "day", "days", "mcday", "mcdays");
        unit(Dimension.DISTANCE, 1, "blocks", "block", "blocks", "b", "overworld", "ow", "m");
        unit(Dimension.DISTANCE, 8, "nether blocks", "nether", "n");
        unit(Dimension.DISTANCE, 16, "chunks", "chunk", "chunks", "c");
        unit(Dimension.DISTANCE, 512, "regions", "region", "regions");
    }

    private final Map<String, Double> variables = new LinkedHashMap<>();
    private Double ans;

    private static void unit(Dimension dimension, double factor, String plural, String... names) {
        for (String name : names) {
            UNITS.put(name, new Unit(name, dimension, factor, plural));
        }
    }

    public static Unit unit(String name) {
        return name == null ? null : UNITS.get(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, Double> variables() {
        return Map.copyOf(variables);
    }

    public void clear() {
        variables.clear();
        ans = null;
    }

    /** Evaluates one /math line and returns the text to show. Throws IllegalArgumentException on errors. */
    public String run(String line, Map<String, Double> extra) {
        String text = line == null ? "" : line.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Nothing to calculate");
        }
        Matcher assign = ASSIGN.matcher(text);
        if (assign.matches()) {
            String name = assign.group(1);
            if (unit(name) != null || name.equalsIgnoreCase("ans") || name.equalsIgnoreCase("pi") || name.equalsIgnoreCase("e")
                    || extra != null && extra.containsKey(name)) {
                throw new IllegalArgumentException("'" + name + "' is a built-in name");
            }
            double value = evaluate(assign.group(2), extra);
            variables.put(name, value);
            ans = value;
            return name + " = " + MathExpressionEvaluator.formatResult(value);
        }

        Unit target = null;
        String expression = text;
        Matcher convert = CONVERT.matcher(text);
        if (convert.matches() && unit(convert.group(2)) != null) {
            target = unit(convert.group(2));
            expression = convert.group(1);
        }
        double value = evaluate(expression, extra);
        ans = value;
        Unit first = firstUnit(expression);
        if (target != null) {
            if (first != null && first.dimension() != target.dimension()) {
                throw new IllegalArgumentException("Can't convert " + first.plural() + " to " + target.plural());
            }
            return text + " = " + describe(value / target.factor(), target, value);
        }
        if (first != null) {
            Unit base = baseUnit(first.dimension());
            return text + " = " + describe(value, base, value);
        }
        return text + " = " + MathExpressionEvaluator.formatResult(value);
    }

    private double evaluate(String expression, Map<String, Double> extra) {
        return MathExpressionEvaluator.evaluate(expression, name -> {
            Double variable = variables.get(name);
            if (variable != null) {
                return variable;
            }
            if (name.equalsIgnoreCase("ans")) {
                if (ans == null) {
                    throw new IllegalArgumentException("No previous result for 'ans'");
                }
                return ans;
            }
            if (extra != null && extra.containsKey(name)) {
                return extra.get(name);
            }
            Unit unit = unit(name);
            return unit == null ? null : unit.factor();
        });
    }

    private Unit firstUnit(String expression) {
        Matcher words = WORD.matcher(expression);
        while (words.find()) {
            String word = words.group();
            if (!variables.containsKey(word)) {
                Unit unit = unit(word);
                if (unit != null) {
                    return unit;
                }
            }
        }
        return null;
    }

    private static Unit baseUnit(Dimension dimension) {
        return switch (dimension) {
            case ITEMS -> UNITS.get("items");
            case TIME -> UNITS.get("ticks");
            case DISTANCE -> UNITS.get("blocks");
        };
    }

    /** "46.875 stacks (3000 items = 46 stacks + 56)", "60 seconds (1200 ticks, 1:00)" ... */
    static String describe(double amount, Unit unit, double baseValue) {
        String main = MathExpressionEvaluator.formatResult(round(amount)) + " " + unit.plural();
        return switch (unit.dimension()) {
            case ITEMS -> {
                long items = Math.round(baseValue);
                String split = (items / 64) + " stacks + " + (items % 64);
                yield main + " (" + items + " items = " + split + ", " + fmt(baseValue / 1728.0) + " shulker boxes)";
            }
            case TIME -> {
                double seconds = baseValue / 20.0;
                long whole = (long) Math.floor(seconds);
                String clock = whole >= 3600
                        ? String.format(Locale.ROOT, "%d:%02d:%02d", whole / 3600, (whole / 60) % 60, whole % 60)
                        : String.format(Locale.ROOT, "%d:%02d", whole / 60, whole % 60);
                yield main + " (" + fmt(baseValue) + " ticks, " + fmt(seconds) + " s, " + clock + ")";
            }
            case DISTANCE -> main + " (" + fmt(baseValue) + " overworld blocks = " + fmt(baseValue / 8.0) + " nether, "
                    + fmt(baseValue / 16.0) + " chunks)";
        };
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static String fmt(double value) {
        return MathExpressionEvaluator.formatResult(round(value));
    }
}
