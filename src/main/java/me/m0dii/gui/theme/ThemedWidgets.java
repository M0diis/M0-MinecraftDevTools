package me.m0dii.gui.theme;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-widget style variants for themed screens. Unmarked buttons use {@link Variant#DEFAULT}.
 */
public final class ThemedWidgets {

    public enum Variant {
        /** Neutral raised button. */
        DEFAULT,
        /** Filled accent button for the main action of a view. */
        PRIMARY,
        /** Destructive action; neutral until hovered. */
        DANGER,
        /** Borderless button that only shows a background on hover. */
        GHOST,
        /** Selected state of a segmented/choice button. */
        SELECTED
    }

    private static final Map<AbstractWidget, Variant> VARIANTS = Collections.synchronizedMap(new WeakHashMap<>());

    private ThemedWidgets() {
    }

    public static <T extends AbstractWidget> T variant(T widget, Variant variant) {
        if (widget != null) {
            if (variant == Variant.DEFAULT) {
                VARIANTS.remove(widget);
            } else {
                VARIANTS.put(widget, variant);
            }
        }
        return widget;
    }

    public static <T extends AbstractWidget> T primary(T widget) {
        return variant(widget, Variant.PRIMARY);
    }

    public static <T extends AbstractWidget> T danger(T widget) {
        return variant(widget, Variant.DANGER);
    }

    public static <T extends AbstractWidget> T ghost(T widget) {
        return variant(widget, Variant.GHOST);
    }

    public static <T extends AbstractWidget> T selected(T widget, boolean selected) {
        return variant(widget, selected ? Variant.SELECTED : Variant.DEFAULT);
    }

    public static Variant of(AbstractWidget widget) {
        return VARIANTS.getOrDefault(widget, Variant.DEFAULT);
    }
}
