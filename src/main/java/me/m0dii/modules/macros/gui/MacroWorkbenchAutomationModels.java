package me.m0dii.modules.macros.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Layout records and small enums of the automation tab: panel geometry, picker popups and widget groups.
 */
public final class MacroWorkbenchAutomationModels {
    private MacroWorkbenchAutomationModels() {
    }

    public enum WidgetGroup {
        LIST,
        TABS,
        DETAIL
    }

    public record Placed(AbstractWidget widget, WidgetGroup group, int baseX, int baseY) {
    }

    public record Layout(boolean narrow,
                         int leftX,
                         int topY,
                         int leftW,
                         int listY,
                         int rightX,
                         int rightW,
                         int detailTop,
                         int bottom,
                         int headerY,
                         int headerH,
                         int columnW,
                         int rightX2,
                         int fieldW,
                         int numericW,
                         int numberRowY,
                         int filterPanelX,
                         int filterPanelY,
                         int filterPanelH,
                         int conditionPanelX,
                         int conditionPanelY,
                         int conditionPanelH,
                         int diagnosticsX,
                         int diagnosticsY,
                         int diagnosticsH,
                         int actionsX,
                         int actionsY,
                         int actionsH) {
    }

    public record FilterPickerLayout(int x,
                                     int y,
                                     int width,
                                     int height,
                                     int listX,
                                     int listY,
                                     int listW,
                                     int listH,
                                     int closeX) {
    }

    public record ConditionPickerLayout(int x,
                                        int y,
                                        int width,
                                        int height,
                                        int listX,
                                        int listY,
                                        int listW,
                                        int listH,
                                        int closeX) {
    }

    public record FieldPickerLayout(int x,
                                    int y,
                                    int width,
                                    int height,
                                    int listX,
                                    int listY,
                                    int listW,
                                    int listH,
                                    int footerY,
                                    int closeX) {
    }

    public enum RuleIntField {
        PRIORITY,
        RATE_COUNT
    }

    public enum RuleLongField {
        COOLDOWN,
        DEBOUNCE,
        RATE_WINDOW
    }

    public enum EditorSection {
        OVERVIEW,
        FILTERS,
        CONDITIONS,
        ACTIONS
    }

    public enum FieldPickerMode {
        NONE,
        FILTER_FIELD,
        CONDITION_FIELD
    }
}
