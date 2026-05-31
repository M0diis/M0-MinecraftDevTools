package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.gui.Click;

import java.util.function.Consumer;

public final class MacroWorkbenchAdvancedClickSupport {
    private MacroWorkbenchAdvancedClickSupport() {
    }

    public interface Ops {
        boolean contains(Click click, UiRect rect);

        int stepInt(int base);

        int cycleStyleColor(int current, boolean forward);

        void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta);
    }

    public static boolean handleApplyCancel(Click click,
                                            UiRect apply,
                                            UiRect cancel,
                                            Ops ops,
                                            Runnable onApply,
                                            Runnable onCancel) {
        if (ops.contains(click, apply)) {
            onApply.run();
            return true;
        }
        if (ops.contains(click, cancel)) {
            onCancel.run();
            return true;
        }
        return false;
    }

    public static boolean handleBackgroundColorButtons(Click click,
                                                       boolean forward,
                                                       UiRect minus,
                                                       UiRect plus,
                                                       UiRect alphaMinus,
                                                       UiRect alphaPlus,
                                                       MacroHudDataHandler.HudElement element,
                                                       Ops ops,
                                                       Runnable onColorChanged) {
        if (ops.contains(click, minus)) {
            if (forward) {
                element.backgroundColor = ops.cycleStyleColor(element.backgroundColor, false);
                onColorChanged.run();
            } else {
                ops.adjustBackgroundAlpha(element, -ops.stepInt(8));
            }
            return true;
        }
        if (ops.contains(click, plus)) {
            if (forward) {
                element.backgroundColor = ops.cycleStyleColor(element.backgroundColor, true);
                onColorChanged.run();
            } else {
                ops.adjustBackgroundAlpha(element, ops.stepInt(8));
            }
            return true;
        }
        if (ops.contains(click, alphaMinus)) {
            ops.adjustBackgroundAlpha(element, -ops.stepInt(8));
            return true;
        }
        if (ops.contains(click, alphaPlus)) {
            ops.adjustBackgroundAlpha(element, ops.stepInt(8));
            return true;
        }
        return false;
    }

    public static boolean handleColorCycleButtons(Click click,
                                                  UiRect minus,
                                                  UiRect plus,
                                                  int currentColor,
                                                  Ops ops,
                                                  Consumer<Integer> setColor) {
        if (ops.contains(click, minus)) {
            setColor.accept(ops.cycleStyleColor(currentColor, false));
            return true;
        }
        if (ops.contains(click, plus)) {
            setColor.accept(ops.cycleStyleColor(currentColor, true));
            return true;
        }
        return false;
    }
}
