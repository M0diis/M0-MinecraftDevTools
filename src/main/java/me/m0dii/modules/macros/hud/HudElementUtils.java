package me.m0dii.modules.macros.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudElementUtils {
    private HudElementUtils() {
    }

    public static MacroHudDataHandler.HudElement cloneElement(MacroHudDataHandler.HudElement element) {
        return MacroHudDataHandler.copyElement(element);
    }

    public static List<MacroHudDataHandler.HudElement> sortedByLayer(List<MacroHudDataHandler.HudElement> elements) {
        List<MacroHudDataHandler.HudElement> layered = new ArrayList<>();
        if (elements == null) {
            return layered;
        }
        for (MacroHudDataHandler.HudElement element : elements) {
            if (element != null) {
                layered.add(element);
            }
        }
        layered.sort(Comparator.comparingInt(element -> element.zIndex));
        return layered;
    }
}
