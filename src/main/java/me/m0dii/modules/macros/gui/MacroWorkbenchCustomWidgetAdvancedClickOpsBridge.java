package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.UiRect;
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
    public void adjustBackgroundAlpha(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element, int delta) {
        screen.cwAdjustBackgroundAlpha(element, delta);
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
    public me.m0dii.modules.macros.hud.MacroHudDataHandler.HorizontalAlign cycleHorizontalAlign(me.m0dii.modules.macros.hud.MacroHudDataHandler.HorizontalAlign current, boolean forward) {
        return screen.cwCycleHorizontalAlign(current, forward);
    }

    @Override
    public me.m0dii.modules.macros.hud.MacroHudDataHandler.VerticalAlign cycleVerticalAlign(me.m0dii.modules.macros.hud.MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return screen.cwCycleVerticalAlign(current, forward);
    }

    @Override
    public me.m0dii.modules.macros.hud.MacroHudDataHandler.Anchor cycleAnchor(me.m0dii.modules.macros.hud.MacroHudDataHandler.Anchor current, boolean forward) {
        return screen.cwCycleAnchor(current, forward);
    }

    @Override
    public void cycleBorderSetting(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element, boolean forward) {
        screen.cwCycleBorderSetting(element, forward);
    }

    @Override
    public void ensureVisibleBackground(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element) {
        screen.cwEnsureVisibleBackground(element);
    }

    @Override
    public int resolveElementX(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element) {
        return screen.cwResolveElementX(element);
    }

    @Override
    public int resolveElementY(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element) {
        return screen.cwResolveElementY(element);
    }

    @Override
    public void setElementScreenPosition(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element, int screenX, int screenY) {
        screen.cwSetElementScreenPosition(element, screenX, screenY);
    }

    @Override
    public void clampElementToCanvas(me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement element) {
        screen.cwClampElementToCanvas(element);
    }

}
