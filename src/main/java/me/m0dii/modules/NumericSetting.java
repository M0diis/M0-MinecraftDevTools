package me.m0dii.modules;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A number setting the Module Browser shows as a slider with a typed-in value instead of separate +/- rows.
 *
 * @param row        index of the "Label: value" row in {@link Module#getSettingsDisplay()} that the slider replaces
 * @param hiddenRows rows (usually the +/- buttons) the Module Browser hides; other UIs still show them
 * @param decimals   digits after the point shown and kept when typing
 */
public record NumericSetting(int row, String label, double min, double max, double step, int decimals,
                             DoubleSupplier getter, DoubleConsumer setter, List<Integer> hiddenRows) {

    /** Modules with number settings implement this to get sliders in the Module Browser. */
    public interface Provider {
        List<NumericSetting> numericSettings();
    }

    public static NumericSetting of(int row, String label, double min, double max, double step, int decimals,
                                    DoubleSupplier getter, DoubleConsumer setter, Integer... hiddenRows) {
        return new NumericSetting(row, label, min, max, step, decimals, getter, setter, List.of(hiddenRows));
    }

    public double value() {
        return getter.getAsDouble();
    }

    /** Snaps to the step grid (anchored at min) and the range, then applies it. */
    public void set(double value) {
        setter.accept(snap(value));
    }

    public double snap(double value) {
        double clamped = Math.clamp(value, min, max);
        if (step > 0) {
            clamped = min + Math.round((clamped - min) / step) * step;
            clamped = Math.clamp(clamped, min, max);
        }
        double scale = Math.pow(10, Math.max(0, decimals));
        return Math.round(clamped * scale) / scale;
    }

    /** Position of the current value on the track, 0..1. */
    public double fraction() {
        return max <= min ? 0.0 : Math.clamp((value() - min) / (max - min), 0.0, 1.0);
    }

    /** Value for a position on the track, 0..1 (snapped). */
    public double valueAt(double fraction) {
        return snap(min + Math.clamp(fraction, 0.0, 1.0) * (max - min));
    }

    public String format(double value) {
        return decimals <= 0 ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** Parses typed text ("12", "0.5", "1,5"); null when it is not a number. */
    public static Double parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.strip().replace(',', '.');
        if (t.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(t);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
