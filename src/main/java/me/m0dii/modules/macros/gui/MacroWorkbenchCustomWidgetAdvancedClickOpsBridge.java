package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.gui.Click;

final class MacroWorkbenchCustomWidgetAdvancedClickOpsBridge implements MacroWorkbenchCustomWidgetAdvancedClickHandler.Ops {
    private final MacroWorkbenchScreen screen;

    MacroWorkbenchCustomWidgetAdvancedClickOpsBridge(MacroWorkbenchScreen screen) {
        this.screen = screen;
    }

    @Override
    public boolean contains(Click click, UiRect rect) {
        return screen.cwContains(click, rect);
    }

    @Override
    public boolean contains(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return screen.cwContains(x, y, boxX, boxY, boxW, boxH);
    }

    @Override
    public int stepInt(int base) {
        return screen.cwStepInt(base);
    }

    @Override
    public double stepDouble(double base) {
        return screen.cwStepDouble(base);
    }

    @Override
    public String cyclePreset(String current, String[] presets, boolean forward) {
        return screen.cwCyclePreset(current, presets, forward);
    }

    @Override
    public int cycleStyleColor(int current, boolean forward) {
        return screen.cwCycleStyleColor(current, forward);
    }

    @Override
    public void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta) {
        screen.cwAdjustBackgroundAlpha(element, delta);
    }

    @Override
    public void adjustZIndex(MacroHudDataHandler.HudElement element, int delta) {
        screen.cwAdjustZIndex(element, delta);
    }

    @Override
    public String[] iconIdSuggestionsForKind(String kind) {
        return screen.cwIconIdSuggestionsForKind(kind);
    }

    @Override
    public String getAdvancedAction() {
        return screen.cwGetAdvancedAction();
    }

    @Override
    public void setAdvancedAction(String value) {
        screen.cwSetAdvancedAction(value);
    }

    @Override
    public void setAdvancedActionCursor(int value) {
        screen.cwSetAdvancedActionCursor(value);
    }

    @Override
    public int getAdvancedActionSuggestionScroll() {
        return screen.cwGetAdvancedActionSuggestionScroll();
    }

    @Override
    public void setAdvancedActionSuggestionScroll(int value) {
        screen.cwSetAdvancedActionSuggestionScroll(value);
    }

    @Override
    public void setAdvancedActionSuggestionIndex(int value) {
        screen.cwSetAdvancedActionSuggestionIndex(value);
    }

    @Override
    public MacroHudDataHandler.HorizontalAlign cycleHorizontalAlign(MacroHudDataHandler.HorizontalAlign current, boolean forward) {
        return screen.cwCycleHorizontalAlign(current, forward);
    }

    @Override
    public MacroHudDataHandler.VerticalAlign cycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return screen.cwCycleVerticalAlign(current, forward);
    }

    @Override
    public MacroHudDataHandler.Anchor cycleAnchor(MacroHudDataHandler.Anchor current, boolean forward) {
        return screen.cwCycleAnchor(current, forward);
    }

    @Override
    public void cycleBorderSetting(MacroHudDataHandler.HudElement element, boolean forward) {
        screen.cwCycleBorderSetting(element, forward);
    }

    @Override
    public void ensureVisibleBackground(MacroHudDataHandler.HudElement element) {
        screen.cwEnsureVisibleBackground(element);
    }

    @Override
    public int resolveElementX(MacroHudDataHandler.HudElement element) {
        return screen.cwResolveElementX(element);
    }

    @Override
    public int resolveElementY(MacroHudDataHandler.HudElement element) {
        return screen.cwResolveElementY(element);
    }

    @Override
    public void setElementScreenPosition(MacroHudDataHandler.HudElement element, int screenX, int screenY) {
        screen.cwSetElementScreenPosition(element, screenX, screenY);
    }

    @Override
    public void clampElementToCanvas(MacroHudDataHandler.HudElement element) {
        screen.cwClampElementToCanvas(element);
    }

}
