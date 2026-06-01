package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.gui.Click;

import java.util.List;

public final class MacroWorkbenchCustomWidgetAdvancedClickHandler {
    private MacroWorkbenchCustomWidgetAdvancedClickHandler() {
    }

    public interface Ops {
        boolean contains(Click click, UiRect rect);

        boolean contains(double x, double y, int boxX, int boxY, int boxW, int boxH);

        int stepInt(int base);

        double stepDouble(double base);

        int cycleStyleColor(int current, boolean forward);

        void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta);

        String cyclePreset(String current, String[] presets, boolean forward);

        String[] iconIdSuggestionsForKind(String kind);

        MacroHudDataHandler.HorizontalAlign cycleHorizontalAlign(MacroHudDataHandler.HorizontalAlign current, boolean forward);

        MacroHudDataHandler.VerticalAlign cycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward);

        MacroHudDataHandler.Anchor cycleAnchor(MacroHudDataHandler.Anchor current, boolean forward);

        void cycleBorderSetting(MacroHudDataHandler.HudElement element, boolean forward);

        void ensureVisibleBackground(MacroHudDataHandler.HudElement element);

        int resolveElementX(MacroHudDataHandler.HudElement element);

        int resolveElementY(MacroHudDataHandler.HudElement element);

        void setElementScreenPosition(MacroHudDataHandler.HudElement element, int screenX, int screenY);

        void clampElementToCanvas(MacroHudDataHandler.HudElement element);

        String getAdvancedAction();

        void setAdvancedAction(String value);

        void setAdvancedActionCursor(int value);

        int getAdvancedActionSuggestionScroll();

        void setAdvancedActionSuggestionScroll(int value);

        void setAdvancedActionSuggestionIndex(int value);
    }

    public static boolean handleSuggestionClick(Click click, UiRect suggestionsArea, List<String> suggestions, Ops ops) {
        if (suggestions.isEmpty()) {
            return false;
        }
        int dropX = suggestionsArea.x();
        int dropY = suggestionsArea.y();
        int dropW = suggestionsArea.width();
        int rowH = 10;
        int maxVisible = Math.clamp(Math.max(1, suggestionsArea.height() / rowH), 1, suggestions.size());
        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
        int scroll = Math.clamp(ops.getAdvancedActionSuggestionScroll(), 0, maxScroll);
        ops.setAdvancedActionSuggestionScroll(scroll);
        for (int i = 0; i < maxVisible; i++) {
            int yy = dropY + i * rowH;
            if (ops.contains(click.x(), click.y(), dropX, yy, dropW, rowH)) {
                int idx = scroll + i;
                if (idx >= 0 && idx < suggestions.size() && !MacroWorkbenchActionSuggestions.isHeader(suggestions.get(idx))) {
                    String value = MacroWorkbenchActionSuggestions.value(suggestions.get(idx));
                    ops.setAdvancedAction(value);
                    ops.setAdvancedActionCursor(value.length());
                    ops.setAdvancedActionSuggestionIndex(idx);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean handleGeneralRowClick(Click click,
                                                List<UiRect> generalRow1,
                                                List<UiRect> generalRow2,
                                                boolean forward,
                                                MacroHudDataHandler.HudElement selected,
                                                String[] barValueSourcePresets,
                                                String[] listSourcePresets,
                                                String[] stateSourcePresets,
                                                Ops ops) {
        if (selected == null) {
            return false;
        }
        if (ops.contains(click, slot(generalRow1, 0))) {
            if (forward) {
                selected.backgroundColor = ops.cycleStyleColor(selected.backgroundColor, false);
            } else {
                ops.adjustBackgroundAlpha(selected, -ops.stepInt(8));
            }
            return true;
        }
        if (ops.contains(click, slot(generalRow1, 1))) {
            if (forward) {
                selected.backgroundColor = ops.cycleStyleColor(selected.backgroundColor, true);
            } else {
                ops.adjustBackgroundAlpha(selected, ops.stepInt(8));
            }
            return true;
        }
        if (ops.contains(click, slot(generalRow1, 2))) {
            selected.borderColor = ops.cycleStyleColor(selected.borderColor, false);
            return true;
        }
        if (ops.contains(click, slot(generalRow1, 3))) {
            selected.borderColor = ops.cycleStyleColor(selected.borderColor, true);
            return true;
        }
        if (ops.contains(click, slot(generalRow2, 0))) {
            selected.fontScale = Math.clamp((float) (selected.fontScale - ops.stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (ops.contains(click, slot(generalRow2, 1))) {
            selected.fontScale = Math.clamp((float) (selected.fontScale + ops.stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (ops.contains(click, slot(generalRow2, 2))) {
            String[] presets = switch (selected.type) {
                case LIST -> listSourcePresets;
                case STATE_BADGE -> stateSourcePresets;
                case ICON -> ops.iconIdSuggestionsForKind(selected.iconKind);
                case INVENTORY -> new String[]{"HOTBAR", "INVENTORY", "ARMOR"};
                default -> barValueSourcePresets;
            };
            String value = ops.cyclePreset(ops.getAdvancedAction(), presets, forward);
            ops.setAdvancedAction(value);
            ops.setAdvancedActionCursor(value.length());
            ops.setAdvancedActionSuggestionScroll(0);
            return true;
        }
        return false;
    }

    public static boolean handleBaseRowClick(Click click,
                                             List<UiRect> baseRow,
                                             boolean forward,
                                             MacroHudDataHandler.HudElement selected,
                                             Ops ops) {
        if (selected == null) {
            return false;
        }
        if (ops.contains(click, slot(baseRow, 0))) {
            selected.horizontalAlign = ops.cycleHorizontalAlign(selected.horizontalAlign, forward);
            return true;
        }
        if (ops.contains(click, slot(baseRow, 1))) {
            selected.verticalAlign = ops.cycleVerticalAlign(selected.verticalAlign, forward);
            return true;
        }
        if (ops.contains(click, slot(baseRow, 2))) {
            int oldScreenX = ops.resolveElementX(selected);
            int oldScreenY = ops.resolveElementY(selected);
            selected.anchor = ops.cycleAnchor(selected.anchor, forward);
            ops.setElementScreenPosition(selected, oldScreenX, oldScreenY);
            ops.clampElementToCanvas(selected);
            return true;
        }
        if (ops.contains(click, slot(baseRow, 3))) {
            if (forward) {
                selected.drawBackground = !selected.drawBackground;
                ops.ensureVisibleBackground(selected);
            } else {
                selected.drawBackground = true;
                selected.backgroundOpaque = false;
                ops.adjustBackgroundAlpha(selected, 255 - Math.clamp(selected.backgroundAlpha, 0, 255));
            }
            return true;
        }
        if (ops.contains(click, slot(baseRow, 4))) {
            ops.cycleBorderSetting(selected, forward);
            return true;
        }
        return false;
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }
}
