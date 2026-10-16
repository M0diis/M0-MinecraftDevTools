package me.m0dii.modules.macros.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.local.UiForms;
import me.m0dii.gui.local.UiSelectionList;
import me.m0dii.modules.automation.*;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemedWidgets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MacroWorkbenchAutomationTab {
    /** Below this content width the rule list and the rule editor are shown one at a time. */
    private static final int MASTER_DETAIL_MIN_W = 520;
    /** Height reserved for the section panels; the detail pane scrolls when the window is shorter. */
    private static final int SECTION_H = 280;
    private static final int SCROLL_STEP = 24;
    private static final int PARKED = -10_000;
    private static final int GAP = 12;
    private static final int PANEL_GAP = 8;
    private static final int PANEL_TITLE_Y = 8;
    private static final int BUTTON_H = 20;
    private static final int FIELD_H = 18;
    private static final int FILTER_LIST_H = 40;
    private static final int CONDITION_LIST_H = 32;
    private static final int ACTION_LIST_H = 88;
    private static final int DIAGNOSTIC_ROWS = 6;
    private static final int FILTER_PICKER_W = 320;
    private static final int FILTER_PICKER_H = 220;
    private static final int CONDITION_PICKER_W = 320;
    private static final int CONDITION_PICKER_H = 220;
    private static final int FIELD_PICKER_W = 380;
    private static final int FIELD_PICKER_H = 244;
    private static final int HOVER_SUPPRESS_MOUSE = -10_000;
    private static final int MODAL_SCRIM_COLOR = 0xC0000000;
    private static final int FILTER_FIELD_Y = 86;
    private static final int FILTER_OPERATOR_Y = 138;
    private static final int FILTER_VALUE_Y = 176;
    private static final int CONDITION_SOURCE_Y = 72;
    private static final int CONDITION_FIELD_Y = 110;
    private static final int CONDITION_OPERATOR_Y = 162;
    private static final int CONDITION_VALUE_Y = 200;

    private final MacroWorkbenchScreen owner;
    private final List<AbstractWidget> widgets;
    private final UiSelectionList ruleList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList filterList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList conditionList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList actionList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList filterPickerList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList conditionPickerList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList fieldPickerList = new UiSelectionList(0, 0, 1, 1);
    private final EditBox fieldPickerSearchField;

    private final List<AutomationRule> rules = new ArrayList<>();
    private List<AutomationFieldCatalog.FieldOption> fieldPickerSourceOptions = List.of();
    private List<AutomationFieldCatalog.FieldOption> fieldPickerOptions = List.of();

    private String selectedRuleId;
    private int selectedFilterIndex = -1;
    private int selectedConditionIndex = -1;
    private int selectedActionIndex = -1;
    private boolean syncing = false;
    private boolean dirty = false;
    private boolean filterPickerOpen = false;
    private boolean conditionPickerOpen = false;
    private boolean fieldPickerOpen = false;
    private FieldPickerMode fieldPickerMode = FieldPickerMode.NONE;
    private EditorSection activeSection = EditorSection.OVERVIEW;
    private String fieldPickerSearchQuery = "";
    private String statusMessage = "Automation rules are stored in config/m0-dev-tools-automation.json";
    private int statusColor = 0xFF9FCFCF;

    private Button newRuleButton;
    private Button duplicateRuleButton;
    private Button deleteRuleButton;
    private Button reloadButton;
    private Button saveButton;
    private Button testButton;
    private Button overviewTabButton;
    private Button filtersTabButton;
    private Button conditionsTabButton;
    private Button actionsTabButton;
    private Button eventTypeButton;
    private Button enabledButton;
    private Button filterPickButton;
    private Button filterAddButton;
    private Button filterDeleteButton;
    private Button filterFieldButton;
    private Button filterOperatorButton;
    private Button filterIgnoreCaseButton;
    private Button conditionPickButton;
    private Button conditionAddButton;
    private Button conditionDeleteButton;
    private Button conditionSourceButton;
    private Button conditionFieldButton;
    private Button conditionFieldModeButton;
    private Button conditionOperatorButton;
    private Button conditionIgnoreCaseButton;
    private Button actionAddButton;
    private Button actionDeleteButton;
    private Button actionUpButton;
    private Button actionDownButton;
    private Button actionTypeButton;
    private Button actionEnabledButton;

    private EditBox ruleIdField;
    private EditBox ruleNameField;
    private EditBox priorityField;
    private EditBox cooldownField;
    private EditBox debounceField;
    private EditBox rateCountField;
    private EditBox rateWindowField;
    private EditBox filterValueField;
    private EditBox conditionFieldManualField;
    private EditBox conditionValueField;
    private EditBox actionTargetField;
    private EditBox actionArgumentField;
    private boolean placeholderConditionManualEntry = false;

    MacroWorkbenchAutomationTab(MacroWorkbenchScreen owner, List<AbstractWidget> widgets) {
        this.owner = owner;
        this.widgets = widgets;
        this.fieldPickerSearchField = textField(0, 0, FIELD_PICKER_W - 20, 128, "Search placeholders");
        this.fieldPickerSearchField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            this.fieldPickerSearchQuery = value == null ? "" : value;
            refreshFieldPickerRows(currentFieldPickerValue());
        });
    }

    private enum WidgetGroup {
        LIST,
        TABS,
        DETAIL
    }

    private record Placed(AbstractWidget widget, WidgetGroup group, int baseX, int baseY) {
    }

    private final List<Placed> placedWidgets = new ArrayList<>();
    private int detailScroll = 0;
    private boolean narrowShowDetail = false;
    private Button backButton;

    void initWidgets() {
        this.detailScroll = 0;
        this.placedWidgets.clear();
        Layout layout = layout();

        this.ruleList.setRowHeight(18);
        this.ruleList.setEmptyText("No automation rules");
        this.filterList.setRowHeight(18);
        this.filterList.setEmptyText("No filters");
        this.conditionList.setRowHeight(18);
        this.conditionList.setEmptyText("No conditions");
        this.actionList.setRowHeight(18);
        this.actionList.setEmptyText("No actions");
        this.filterPickerList.setRowHeight(18);
        this.filterPickerList.setEmptyText("No filters");
        this.conditionPickerList.setRowHeight(18);
        this.conditionPickerList.setEmptyText("No conditions");
        this.fieldPickerList.setRowHeight(18);
        this.fieldPickerList.setEmptyText("No fields");

        int buttonY = layout.topY;
        boolean twoRows = layout.leftW < 220;
        int perRow = twoRows ? 2 : 4;
        int listButtonW = (layout.leftW - Theme.GAP_S * (perRow - 1)) / perRow;
        int[][] slots = new int[4][2];
        for (int i = 0; i < 4; i++) {
            int col = i % perRow;
            int row = i / perRow;
            slots[i][0] = layout.leftX + col * (listButtonW + Theme.GAP_S);
            slots[i][1] = buttonY + row * (BUTTON_H + Theme.GAP_S);
        }
        this.newRuleButton = register(ThemedWidgets.primary(Button.builder(Component.literal("+ New"), b -> createRule())
                .bounds(slots[0][0], slots[0][1], listButtonW, BUTTON_H)
                .build()));
        this.duplicateRuleButton = register(Button.builder(Component.literal("Copy"), b -> duplicateRule())
                .bounds(slots[1][0], slots[1][1], listButtonW, BUTTON_H)
                .build());
        this.deleteRuleButton = register(ThemedWidgets.danger(Button.builder(Component.literal("Delete"), b -> deleteRule())
                .bounds(slots[2][0], slots[2][1], listButtonW, BUTTON_H)
                .build()));
        this.reloadButton = register(Button.builder(Component.literal("Reload"), b -> reloadFromDisk())
                .bounds(slots[3][0], slots[3][1], listButtonW, BUTTON_H)
                .build());

        int tabsX = layout.rightX;
        int tabsW = layout.rightW;
        if (layout.narrow) {
            int backW = 52;
            this.backButton = register(ThemedWidgets.ghost(Button.builder(Component.literal("\u2190 Rules"), b -> this.narrowShowDetail = false)
                    .bounds(tabsX, buttonY, backW, BUTTON_H)
                    .build()));
            tabsX += backW + Theme.GAP_S;
            tabsW -= backW + Theme.GAP_S;
        } else {
            this.backButton = null;
        }
        int tabW = (tabsW - Theme.GAP_XS * 3) / 4;
        this.overviewTabButton = register(Button.builder(Component.literal("Overview"), b -> setActiveSection(EditorSection.OVERVIEW))
                .bounds(tabsX, buttonY, tabW, BUTTON_H)
                .build());
        this.filtersTabButton = register(Button.builder(Component.literal("Filters"), b -> setActiveSection(EditorSection.FILTERS))
                .bounds(tabsX + (tabW + Theme.GAP_XS), buttonY, tabW, BUTTON_H)
                .build());
        this.conditionsTabButton = register(Button.builder(Component.literal("Conditions"), b -> setActiveSection(EditorSection.CONDITIONS))
                .bounds(tabsX + (tabW + Theme.GAP_XS) * 2, buttonY, tabW, BUTTON_H)
                .build());
        this.actionsTabButton = register(Button.builder(Component.literal("Actions"), b -> setActiveSection(EditorSection.ACTIONS))
                .bounds(tabsX + (tabW + Theme.GAP_XS) * 3, buttonY, tabsW - (tabW + Theme.GAP_XS) * 3, BUTTON_H)
                .build());

        this.ruleIdField = register(textField(layout.rightX + 12, layout.headerY + 20, layout.fieldW, 128, "rule id"));
        this.ruleNameField = register(textField(layout.rightX2 + 12, layout.headerY + 20, layout.fieldW, 128, "name"));
        this.eventTypeButton = register(Button.builder(Component.literal("Event"), b -> cycleEventType())
                .bounds(layout.rightX + 12, layout.headerY + 52, layout.fieldW, BUTTON_H)
                .build());
        this.enabledButton = register(Button.builder(Component.literal("Enabled"), b -> toggleRuleEnabled())
                .bounds(layout.rightX2 + 12, layout.headerY + 52, layout.fieldW, BUTTON_H)
                .build());

        this.priorityField = register(signedNumberField(layout.rightX + 12, layout.numberRowY, layout.numericW, 8, "0"));
        this.cooldownField = register(numberField(layout.rightX + 12 + (layout.numericW + PANEL_GAP), layout.numberRowY, layout.numericW, 10, "0"));
        this.debounceField = register(numberField(layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 2), layout.numberRowY, layout.numericW, 10, "0"));
        this.rateCountField = register(numberField(layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 3), layout.numberRowY, layout.numericW, 8, "0"));
        this.rateWindowField = register(numberField(layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 4), layout.numberRowY, layout.numericW, 10, "0"));

        this.filterList.setBounds(layout.rightX, layout.filterPanelY + 24, layout.columnW, FILTER_LIST_H);
        this.conditionList.setBounds(layout.rightX, layout.conditionPanelY + 24, layout.columnW, CONDITION_LIST_H);
        this.actionList.setBounds(layout.rightX2, layout.actionsY + 24, layout.columnW, ACTION_LIST_H);

        this.filterPickButton = register(Button.builder(Component.literal("Pick"), b -> openFilterPicker())
                .bounds(layout.rightX + layout.columnW - 106, layout.filterPanelY + 4, 44, 18)
                .build());
        this.filterAddButton = register(Button.builder(Component.literal("+"), b -> addFilter())
                .bounds(layout.rightX + layout.columnW - 58, layout.filterPanelY + 4, 26, 18)
                .build());
        this.filterDeleteButton = register(Button.builder(Component.literal("-"), b -> deleteFilter())
                .bounds(layout.rightX + layout.columnW - 30, layout.filterPanelY + 4, 26, 18)
                .build());
        this.filterFieldButton = register(Button.builder(Component.literal("Field"), b -> openFilterFieldPicker())
                .bounds(layout.rightX + 8, layout.filterPanelY + FILTER_FIELD_Y, layout.columnW - 16, BUTTON_H)
                .build());
        this.filterOperatorButton = register(Button.builder(Component.literal("Operator"), b -> cycleFilterOperator())
                .bounds(layout.rightX + 8, layout.filterPanelY + FILTER_OPERATOR_Y, Math.max(116, (layout.columnW - 28) / 2), BUTTON_H)
                .build());
        this.filterIgnoreCaseButton = register(Button.builder(Component.literal("Ignore Case"), b -> toggleFilterIgnoreCase())
                .bounds(layout.rightX + 16 + Math.max(116, (layout.columnW - 28) / 2), layout.filterPanelY + FILTER_OPERATOR_Y, Math.max(116, (layout.columnW - 28) / 2), BUTTON_H)
                .build());
        this.filterValueField = register(textField(layout.rightX + 8, layout.filterPanelY + FILTER_VALUE_Y, layout.columnW - 16, 256, "value or regex"));

        this.conditionPickButton = register(Button.builder(Component.literal("Pick"), b -> openConditionPicker())
                .bounds(layout.rightX + layout.columnW - 106, layout.conditionPanelY + 4, 44, 18)
                .build());
        this.conditionAddButton = register(Button.builder(Component.literal("+"), b -> addCondition())
                .bounds(layout.rightX + layout.columnW - 58, layout.conditionPanelY + 4, 26, 18)
                .build());
        this.conditionDeleteButton = register(Button.builder(Component.literal("-"), b -> deleteCondition())
                .bounds(layout.rightX + layout.columnW - 30, layout.conditionPanelY + 4, 26, 18)
                .build());
        this.conditionSourceButton = register(Button.builder(Component.literal("Source"), b -> cycleConditionSource())
                .bounds(layout.rightX + 8, layout.conditionPanelY + CONDITION_SOURCE_Y, layout.columnW - 16, BUTTON_H)
                .build());
        int conditionFieldModeButtonW = 84;
        this.conditionFieldButton = register(Button.builder(Component.literal("Field"), b -> openConditionFieldPicker())
                .bounds(layout.rightX + 8, layout.conditionPanelY + CONDITION_FIELD_Y, layout.columnW - 24 - conditionFieldModeButtonW, BUTTON_H)
                .build());
        this.conditionFieldModeButton = register(Button.builder(Component.literal("Pick"), b -> togglePlaceholderConditionFieldMode())
                .bounds(layout.rightX + layout.columnW - 8 - conditionFieldModeButtonW, layout.conditionPanelY + CONDITION_FIELD_Y, conditionFieldModeButtonW, BUTTON_H)
                .build());
        this.conditionFieldManualField = register(textField(layout.rightX + 8, layout.conditionPanelY + CONDITION_FIELD_Y, layout.columnW - 16, 256, "placeholder token (e.g. player.name)"));
        this.conditionOperatorButton = register(Button.builder(Component.literal("Operator"), b -> cycleConditionOperator())
                .bounds(layout.rightX + 8, layout.conditionPanelY + CONDITION_OPERATOR_Y, Math.max(116, (layout.columnW - 28) / 2), BUTTON_H)
                .build());
        this.conditionIgnoreCaseButton = register(Button.builder(Component.literal("Ignore Case"), b -> toggleConditionIgnoreCase())
                .bounds(layout.rightX + 16 + Math.max(116, (layout.columnW - 28) / 2), layout.conditionPanelY + CONDITION_OPERATOR_Y, Math.max(116, (layout.columnW - 28) / 2), BUTTON_H)
                .build());
        this.conditionValueField = register(textField(layout.rightX + 8, layout.conditionPanelY + CONDITION_VALUE_Y, layout.columnW - 16, 256, "expected value"));

        this.actionAddButton = register(Button.builder(Component.literal("+ Action"), b -> addAction())
                .bounds(layout.rightX2 + layout.columnW - 218, layout.actionsY + 4, 64, 18)
                .build());
        this.actionDeleteButton = register(Button.builder(Component.literal("Delete"), b -> deleteAction())
                .bounds(layout.rightX2 + layout.columnW - 150, layout.actionsY + 4, 60, 18)
                .build());
        this.actionUpButton = register(Button.builder(Component.literal("Up"), b -> moveAction(-1))
                .bounds(layout.rightX2 + layout.columnW - 86, layout.actionsY + 4, 36, 18)
                .build());
        this.actionDownButton = register(Button.builder(Component.literal("Down"), b -> moveAction(1))
                .bounds(layout.rightX2 + layout.columnW - 58, layout.actionsY + 4, 48, 18)
                .build());
        this.actionTypeButton = register(Button.builder(Component.literal("Type"), b -> cycleActionType())
                .bounds(layout.rightX2 + 8, layout.actionsY + 128, layout.columnW - 16, BUTTON_H)
                .build());
        this.actionTargetField = register(textField(layout.rightX2 + 8, layout.actionsY + 160, layout.columnW - 16, 256, "target"));
        this.actionArgumentField = register(textField(layout.rightX2 + 8, layout.actionsY + 208, layout.columnW - 16, 256, "argument"));
        this.actionEnabledButton = register(Button.builder(Component.literal("Enabled"), b -> toggleActionEnabled())
                .bounds(layout.rightX2 + 8, layout.actionsY + 232, 140, BUTTON_H)
                .build());
        this.saveButton = register(ThemedWidgets.primary(Button.builder(Component.literal("Save Rules"), b -> commitAll())
                .bounds(layout.rightX2 + 8, layout.actionsY + layout.actionsH - 24, 90, BUTTON_H)
                .build()));
        this.testButton = register(Button.builder(Component.literal("Fire Test"), b -> testSelectedRule())
                .bounds(layout.rightX2 + 102, layout.actionsY + layout.actionsH - 24, 90, BUTTON_H)
                .build());

        classifyWidgets(layout);
        bindListeners();
        reloadFromEngine(null);
        syncAllFields();
    }

    void reloadFromEngine(String preferredId) {
        this.rules.clear();
        this.rules.addAll(AutomationModule.INSTANCE.engine().getRules());
        this.rules.sort((left, right) -> Integer.compare(right.priority, left.priority));
        this.dirty = false;
        this.selectedRuleId = preferredId == null ? this.selectedRuleId : preferredId;
        if (this.selectedRuleId == null || this.rules.stream().noneMatch(rule -> this.selectedRuleId.equals(rule.id))) {
            this.selectedRuleId = this.rules.isEmpty() ? null : this.rules.getFirst().id;
        }
        this.selectedFilterIndex = 0;
        this.selectedConditionIndex = 0;
        this.selectedActionIndex = 0;
        this.fieldPickerOpen = false;
        this.fieldPickerMode = FieldPickerMode.NONE;
        this.filterPickerOpen = false;
        this.conditionPickerOpen = false;
        refreshRuleRows();
        syncAllFields();
        status("Automation rules reloaded.", 0xFF9FCFCF);
    }

    void commitAll() {
        String preferredId = selectedRule() == null ? this.selectedRuleId : selectedRule().id;
        AutomationModule.INSTANCE.engine().setRules(this.rules);
        reloadFromEngine(preferredId);
        status("Automation rules saved.", 0xFF70D070);
    }

    void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Layout layout = layout();
        int renderMouseX = hasOverlayOpen() ? HOVER_SUPPRESS_MOUSE : mouseX;
        int renderMouseY = hasOverlayOpen() ? HOVER_SUPPRESS_MOUSE : mouseY;

        applyWidgetPlacement(layout);
        boolean showList = !layout.narrow || !this.narrowShowDetail;
        boolean showDetail = !layout.narrow || this.narrowShowDetail;
        Font font = this.owner.workbenchTextRenderer();
        if (showList) {
            int listBottom = layout.bottom - 12;
            this.ruleList.setBounds(layout.leftX, layout.listY, layout.leftW, Math.max(20, listBottom - layout.listY));
            this.ruleList.extractRenderState(context, font, renderMouseX, renderMouseY);
            context.text(font, trimToWidth(this.statusMessage, layout.leftW), layout.leftX, layout.bottom - 9, this.statusColor, false);
        } else {
            this.ruleList.setBounds(PARKED, PARKED, 1, 1);
        }
        if (!showDetail) {
            this.filterList.setBounds(PARKED, PARKED, 1, 1);
            this.conditionList.setBounds(PARKED, PARKED, 1, 1);
            this.actionList.setBounds(PARKED, PARKED, 1, 1);
            return;
        }
        this.filterList.setBounds(layout.rightX, layout.filterPanelY + 24, layout.columnW, FILTER_LIST_H);
        this.conditionList.setBounds(layout.rightX, layout.conditionPanelY + 24, layout.columnW, CONDITION_LIST_H);
        this.actionList.setBounds(layout.rightX2, layout.actionsY + 24, layout.columnW, ACTION_LIST_H);

        context.enableScissor(layout.rightX, layout.detailTop, layout.rightX + layout.rightW, layout.bottom);
        UiForms.drawPanel(context, layout.rightX, layout.headerY, layout.rightW, layout.headerH);
        syncSectionButtons();

        context.text(this.owner.workbenchTextRenderer(), "Id", layout.rightX + 12, layout.headerY + PANEL_TITLE_Y, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Name", layout.rightX2 + 12, layout.headerY + PANEL_TITLE_Y, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Priority", layout.rightX + 12, layout.headerY + 78, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Cooldown", layout.rightX + 12 + (layout.numericW + PANEL_GAP), layout.headerY + 78, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Debounce", layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 2), layout.headerY + 78, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Rate", layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 3), layout.headerY + 78, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Window", layout.rightX + 12 + ((layout.numericW + PANEL_GAP) * 4), layout.headerY + 78, 0xFFB8B8B8);

        AutomationRule rule = selectedRule();
        AutomationEngine.RuleRuntimeSnapshot runtime = rule == null
                ? new AutomationEngine.RuleRuntimeSnapshot(null, false, 0L)
                : AutomationModule.INSTANCE.engine().getRuntimeSnapshot(rule.id);
        renderActiveSection(context, layout, rule, runtime, renderMouseX, renderMouseY);
        context.text(font, trimToWidth(this.dirty ? "Unsaved changes" : "Saved", Math.max(0, layout.columnW - 204)), layout.rightX2 + 204, layout.actionsY + layout.actionsH - 18, this.dirty ? Theme.WARNING : Theme.SUCCESS, false);
        context.disableScissor();

        int maxScroll = maxDetailScroll(layout);
        if (maxScroll > 0) {
            int trackX = layout.rightX + layout.rightW + 3;
            int trackH = layout.bottom - layout.detailTop;
            int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int thumbY = layout.detailTop + (trackH - thumbH) * this.detailScroll / maxScroll;
            context.fill(trackX, layout.detailTop, trackX + 2, layout.bottom, Theme.DIVIDER);
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, Theme.BORDER_STRONG);
        }

    }

    private void reloadFromDisk() {
        AutomationModule.INSTANCE.engine().reload();
        reloadFromEngine(this.selectedRuleId);
        status("Automation rules reloaded from disk.", 0xFF9FCFCF);
    }

    private void setActiveSection(EditorSection section) {
        this.activeSection = section == null ? EditorSection.OVERVIEW : section;
        syncSectionButtons();
        syncSectionVisibility();
    }

    private void syncSectionButtons() {
        if (this.overviewTabButton == null) {
            return;
        }
        ThemedWidgets.selected(this.overviewTabButton, this.activeSection == EditorSection.OVERVIEW);
        ThemedWidgets.selected(this.filtersTabButton, this.activeSection == EditorSection.FILTERS);
        ThemedWidgets.selected(this.conditionsTabButton, this.activeSection == EditorSection.CONDITIONS);
        ThemedWidgets.selected(this.actionsTabButton, this.activeSection == EditorSection.ACTIONS);
    }

    private void renderActiveSection(GuiGraphicsExtractor context,
                                     Layout layout,
                                     AutomationRule rule,
                                     AutomationEngine.RuleRuntimeSnapshot runtime,
                                     int mouseX,
                                     int mouseY) {
        switch (this.activeSection) {
            case OVERVIEW -> renderOverviewSection(context, layout, rule, runtime);
            case FILTERS -> renderFiltersSection(context, layout, rule, mouseX, mouseY);
            case CONDITIONS -> renderConditionsSection(context, layout, rule, mouseX, mouseY);
            case ACTIONS -> renderActionsSection(context, layout, rule, runtime, mouseX, mouseY);
        }
    }

    private void renderOverviewSection(GuiGraphicsExtractor context,
                                       Layout layout,
                                       AutomationRule rule,
                                       AutomationEngine.RuleRuntimeSnapshot runtime) {
        UiForms.drawPanel(context, layout.diagnosticsX, layout.diagnosticsY, layout.columnW, layout.diagnosticsH);
        UiForms.drawPanel(context, layout.actionsX, layout.actionsY, layout.columnW, layout.actionsH);
        context.text(this.owner.workbenchTextRenderer(), "Recent Diagnostics", layout.diagnosticsX + 8, layout.diagnosticsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Summary", layout.actionsX + 8, layout.actionsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        renderDiagnostics(context, layout.diagnosticsX + 8, layout.diagnosticsY + 28, layout.columnW - 16, layout.diagnosticsH - 36);
        renderOverviewSummary(context, layout.actionsX + 8, layout.actionsY + 28, layout.columnW - 16, layout.actionsH - 44, rule, runtime);
    }

    private void renderFiltersSection(GuiGraphicsExtractor context,
                                      Layout layout,
                                      AutomationRule rule,
                                      int mouseX,
                                      int mouseY) {
        UiForms.drawPanel(context, layout.filterPanelX, layout.filterPanelY, layout.columnW, layout.filterPanelH);
        UiForms.drawPanel(context, layout.actionsX, layout.actionsY, layout.columnW, layout.actionsH);
        context.text(this.owner.workbenchTextRenderer(), "Filters", layout.filterPanelX + 8, layout.filterPanelY + PANEL_TITLE_Y, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Available Event Fields", layout.actionsX + 8, layout.actionsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        this.filterList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
        renderFilterEditorLabels(context, layout);
        renderFieldCatalog(context,
                layout.actionsX + 8,
                layout.actionsY + 28,
                layout.columnW - 16,
                layout.actionsH - 64,
                rule == null ? List.of() : AutomationFieldCatalog.eventFilterFields(rule.eventType),
                selectedFilter() == null ? "" : safe(selectedFilter().field));
    }

    private void renderConditionsSection(GuiGraphicsExtractor context,
                                         Layout layout,
                                         AutomationRule rule,
                                         int mouseX,
                                         int mouseY) {
        UiForms.drawPanel(context, layout.conditionPanelX, layout.conditionPanelY, layout.columnW, layout.conditionPanelH);
        UiForms.drawPanel(context, layout.actionsX, layout.actionsY, layout.columnW, layout.actionsH);
        context.text(this.owner.workbenchTextRenderer(), "Conditions", layout.conditionPanelX + 8, layout.conditionPanelY + PANEL_TITLE_Y, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Available Source Fields", layout.actionsX + 8, layout.actionsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        this.conditionList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
        renderConditionEditorLabels(context, layout);
        AutomationRule.Condition condition = selectedCondition();
        AutomationRule.ConditionSource source = condition == null ? AutomationRule.ConditionSource.EVENT : condition.source;
        renderFieldCatalog(context,
                layout.actionsX + 8,
                layout.actionsY + 28,
                layout.columnW - 16,
                layout.actionsH - 64,
                rule == null ? List.of() : AutomationFieldCatalog.conditionFields(source, rule.eventType),
                condition == null ? "" : safe(condition.field));
    }

    private void renderActionsSection(GuiGraphicsExtractor context,
                                      Layout layout,
                                      AutomationRule rule,
                                      AutomationEngine.RuleRuntimeSnapshot runtime,
                                      int mouseX,
                                      int mouseY) {
        UiForms.drawPanel(context, layout.diagnosticsX, layout.diagnosticsY, layout.columnW, layout.diagnosticsH);
        UiForms.drawPanel(context, layout.actionsX, layout.actionsY, layout.columnW, layout.actionsH);
        context.text(this.owner.workbenchTextRenderer(), "Action Notes", layout.diagnosticsX + 8, layout.diagnosticsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Actions", layout.actionsX + 8, layout.actionsY + PANEL_TITLE_Y, 0xFFFFFFFF);
        this.actionList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
        renderActionEditorLabels(context, layout);
        renderActionsHelp(context, layout.diagnosticsX + 8, layout.diagnosticsY + 28, layout.columnW - 16, layout.diagnosticsH - 36, rule, runtime);
    }

    private void renderFilterEditorLabels(GuiGraphicsExtractor context, Layout layout) {
        if (selectedFilter() != null) {
            context.text(this.owner.workbenchTextRenderer(), "Field", this.filterFieldButton.getX(), this.filterFieldButton.getY() - 12, 0xFFB8B8B8);
            context.text(this.owner.workbenchTextRenderer(), "Op / Case", this.filterOperatorButton.getX(), this.filterOperatorButton.getY() - 12, 0xFFB8B8B8);
            context.text(
                    this.owner.workbenchTextRenderer(),
                    trimToWidth(filterFieldHelpText(), layout.columnW - 16),
                    this.filterFieldButton.getX(),
                    this.filterFieldButton.getY() + BUTTON_H + 6,
                    0xFF909090
            );
            context.text(this.owner.workbenchTextRenderer(), "Value", this.filterValueField.getX(), this.filterValueField.getY() - 12, 0xFFB8B8B8);
        } else {
            context.text(this.owner.workbenchTextRenderer(), "Select a filter or click + to add one.", layout.filterPanelX + 8, layout.filterPanelY + 78, 0xFF909090);
            context.text(this.owner.workbenchTextRenderer(), "Field choices are tied to the selected event type.", layout.filterPanelX + 8, layout.filterPanelY + 94, 0xFF909090);
        }
    }

    private void renderConditionEditorLabels(GuiGraphicsExtractor context, Layout layout) {
        if (selectedCondition() != null) {
            context.text(this.owner.workbenchTextRenderer(), "Field", this.conditionFieldButton.getX(), this.conditionFieldButton.getY() - 12, 0xFFB8B8B8);
            context.text(this.owner.workbenchTextRenderer(), "Op / Case", this.conditionOperatorButton.getX(), this.conditionOperatorButton.getY() - 12, 0xFFB8B8B8);
            context.text(
                    this.owner.workbenchTextRenderer(),
                    trimToWidth(conditionFieldHelpText(), layout.columnW - 16),
                    this.conditionFieldButton.getX(),
                    this.conditionFieldButton.getY() + BUTTON_H + 6,
                    0xFF909090
            );
            context.text(this.owner.workbenchTextRenderer(), "Value", this.conditionValueField.getX(), this.conditionValueField.getY() - 12, 0xFFB8B8B8);
        } else {
            context.text(this.owner.workbenchTextRenderer(), "No condition selected. Click + to add one.", layout.conditionPanelX + 8, layout.conditionPanelY + 72, 0xFF909090);
            context.text(this.owner.workbenchTextRenderer(), "Conditions can read event, client, player, or world fields.", layout.conditionPanelX + 8, layout.conditionPanelY + 94, 0xFF909090);
        }
    }

    private void renderActionEditorLabels(GuiGraphicsExtractor context, Layout layout) {
        if (selectedAction() != null) {
            context.text(this.owner.workbenchTextRenderer(), "Target", this.actionTargetField.getX(), this.actionTargetField.getY() - 12, 0xFFB8B8B8);
            context.text(this.owner.workbenchTextRenderer(), actionArgumentLabel(), this.actionArgumentField.getX(), this.actionArgumentField.getY() - 12, 0xFFB8B8B8);
            context.text(this.owner.workbenchTextRenderer(), trimToWidth(actionTargetHint(), layout.columnW - 16), layout.actionsX + 8, layout.actionsY + 264, 0xFF9FCFCF);
            context.text(this.owner.workbenchTextRenderer(), "Target and argument expand {event.*}, {client.*}, {player.*}, {world.*}.", layout.actionsX + 8, layout.actionsY + 278, 0xFF9FCFCF);
        } else {
            context.text(this.owner.workbenchTextRenderer(), "Select an action or click + Action to add one.", layout.actionsX + 8, layout.actionsY + 150, 0xFF909090);
        }
    }

    boolean hasOverlayOpen() {
        return this.fieldPickerOpen || this.filterPickerOpen || this.conditionPickerOpen;
    }

    boolean handleCharTyped(CharacterEvent input) {
        if (!hasOverlayOpen()) {
            return false;
        }
        if (this.fieldPickerOpen && fieldPickerShowsSearch()) {
            if (!this.fieldPickerSearchField.isFocused()) {
                this.fieldPickerSearchField.setFocused(true);
            }
            this.fieldPickerSearchField.charTyped(input);
        }
        return true;
    }

    boolean handleKeyPressed(KeyEvent input) {
        if (!hasOverlayOpen()) {
            return false;
        }
        if (input.input() == InputConstants.KEY_ESCAPE) {
            closeActiveOverlay();
            return true;
        }
        if (this.fieldPickerOpen && fieldPickerShowsSearch()) {
            if (!this.fieldPickerSearchField.isFocused()) {
                this.fieldPickerSearchField.setFocused(true);
            }
            this.fieldPickerSearchField.keyPressed(input);
        }
        return true;
    }

    void renderOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (this.fieldPickerOpen) {
            renderFieldPicker(context, mouseX, mouseY);
        } else if (this.filterPickerOpen) {
            renderFilterPicker(context, mouseX, mouseY);
        } else if (this.conditionPickerOpen) {
            renderConditionPicker(context, mouseX, mouseY);
        }
    }

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (this.fieldPickerOpen) {
            return handleFieldPickerClick(mouseX, mouseY, button);
        }
        if (this.filterPickerOpen) {
            return handleFilterPickerClick(mouseX, mouseY, button);
        }
        if (this.conditionPickerOpen) {
            return handleConditionPickerClick(mouseX, mouseY, button);
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (this.ruleList.handleMouseClick(mouseX, mouseY)) {
            int index = this.ruleList.selectedIndex();
            if (index >= 0 && index < this.rules.size()) {
                this.selectedRuleId = this.rules.get(index).id;
                this.selectedFilterIndex = 0;
                this.selectedConditionIndex = 0;
                this.selectedActionIndex = 0;
                this.fieldPickerOpen = false;
                this.filterPickerOpen = false;
                this.conditionPickerOpen = false;
                this.narrowShowDetail = true;
                syncAllFields();
            }
            return true;
        }
        Layout clickLayout = layout();
        if (mouseY < clickLayout.detailTop || mouseY > clickLayout.bottom) {
            return false;
        }
        if (this.activeSection == EditorSection.FILTERS && this.filterList.handleMouseClick(mouseX, mouseY)) {
            this.selectedFilterIndex = this.filterList.selectedIndex();
            syncFilterFields();
            return true;
        }
        if (this.activeSection == EditorSection.CONDITIONS && this.conditionList.handleMouseClick(mouseX, mouseY)) {
            this.selectedConditionIndex = this.conditionList.selectedIndex();
            syncConditionFields();
            return true;
        }
        if (this.activeSection == EditorSection.ACTIONS && this.actionList.handleMouseClick(mouseX, mouseY)) {
            this.selectedActionIndex = this.actionList.selectedIndex();
            syncActionFields();
            return true;
        }
        return false;
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        if (this.fieldPickerOpen) {
            return this.fieldPickerList.handleMouseScroll(mouseX, mouseY, verticalAmount);
        }
        if (this.filterPickerOpen) {
            return this.filterPickerList.handleMouseScroll(mouseX, mouseY, verticalAmount);
        }
        if (this.conditionPickerOpen) {
            return this.conditionPickerList.handleMouseScroll(mouseX, mouseY, verticalAmount);
        }
        if (this.ruleList.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        boolean listScrolled = switch (this.activeSection) {
            case FILTERS -> this.filterList.handleMouseScroll(mouseX, mouseY, verticalAmount);
            case CONDITIONS -> this.conditionList.handleMouseScroll(mouseX, mouseY, verticalAmount);
            case ACTIONS -> this.actionList.handleMouseScroll(mouseX, mouseY, verticalAmount);
            case OVERVIEW -> false;
        };
        if (listScrolled) {
            return true;
        }
        Layout layout = layout();
        boolean overDetail = mouseX >= layout.rightX && mouseX <= layout.rightX + layout.rightW
                && mouseY >= layout.detailTop && mouseY <= layout.bottom;
        int maxScroll = maxDetailScroll(layout);
        if (!overDetail || maxScroll <= 0) {
            return false;
        }
        this.detailScroll = Math.clamp(this.detailScroll + (verticalAmount > 0 ? -SCROLL_STEP : SCROLL_STEP), 0, maxScroll);
        return true;
    }

    private void bindListeners() {
        this.ruleIdField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule rule = selectedRule();
            if (rule == null) {
                return;
            }
            rule.id = sanitizeRuleIdInput(this.ruleIdField, value);
            this.selectedRuleId = rule.id;
            markDirty();
            refreshRuleRows();
        });
        this.ruleNameField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule rule = selectedRule();
            if (rule == null) {
                return;
            }
            rule.name = sanitizeFieldValue(this.ruleNameField, value);
            markDirty();
            refreshRuleRows();
        });
        this.priorityField.setResponder(value -> updateRuleInt(value, RuleIntField.PRIORITY));
        this.cooldownField.setResponder(value -> updateRuleLong(value, RuleLongField.COOLDOWN));
        this.debounceField.setResponder(value -> updateRuleLong(value, RuleLongField.DEBOUNCE));
        this.rateCountField.setResponder(value -> updateRuleInt(value, RuleIntField.RATE_COUNT));
        this.rateWindowField.setResponder(value -> updateRuleLong(value, RuleLongField.RATE_WINDOW));

        this.filterValueField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule.EventFilter filter = selectedFilter();
            if (filter == null) {
                return;
            }
            filter.value = sanitizeFieldValue(this.filterValueField, value);
            markDirty();
            refreshFilterRows();
        });

        this.conditionValueField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule.Condition condition = selectedCondition();
            if (condition == null) {
                return;
            }
            condition.value = sanitizeFieldValue(this.conditionValueField, value);
            markDirty();
            refreshConditionRows();
        });

        this.conditionFieldManualField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule.Condition condition = selectedCondition();
            if (condition == null || condition.source != AutomationRule.ConditionSource.PLACEHOLDER || !this.placeholderConditionManualEntry) {
                return;
            }
            condition.field = normalizePlaceholderField(sanitizeFieldValue(this.conditionFieldManualField, value));
            markDirty();
            refreshConditionRows();
            syncConditionFields();
        });

        this.actionTargetField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule.Action action = selectedAction();
            if (action == null) {
                return;
            }
            action.target = sanitizeFieldValue(this.actionTargetField, value);
            markDirty();
            refreshActionRows();
        });
        this.actionArgumentField.setResponder(value -> {
            if (this.syncing) {
                return;
            }
            AutomationRule.Action action = selectedAction();
            if (action == null) {
                return;
            }
            action.argument = sanitizeFieldValue(this.actionArgumentField, value);
            markDirty();
            refreshActionRows();
        });
    }

    private void createRule() {
        this.narrowShowDetail = true;
        AutomationRule rule = new AutomationRule();
        rule.id = AutomationRule.newId();
        rule.name = "New Rule";
        rule.enabled = true;
        rule.eventType = AutomationEventType.TICK_INTERVAL;
        rule.priority = 0;
        rule.cooldownMs = 0L;
        rule.debounceMs = 0L;
        rule.rateLimitCount = 0;
        rule.rateLimitWindowMs = 0L;
        rule.eventFilters = new ArrayList<>();
        rule.eventFilters.add(defaultFilterFor(rule.eventType));
        rule.conditions = new ArrayList<>();
        rule.actions = new ArrayList<>();
        rule.actions.add(defaultActionFor(rule.eventType));
        this.rules.add(rule);
        this.rules.sort((left, right) -> Integer.compare(right.priority, left.priority));
        this.selectedRuleId = rule.id;
        this.selectedFilterIndex = 0;
        this.selectedConditionIndex = -1;
        this.selectedActionIndex = 0;
        markDirty();
        refreshRuleRows();
        syncAllFields();
        status("Created rule '" + rule.id + "'.", 0xFF70D070);
    }

    private void duplicateRule() {
        this.narrowShowDetail = true;
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        AutomationRule copy = rule.copy();
        copy.id = AutomationRule.newId();
        copy.name = (rule.name == null || rule.name.isBlank() ? "Rule" : rule.name) + " Copy";
        this.rules.add(copy);
        this.rules.sort((left, right) -> Integer.compare(right.priority, left.priority));
        this.selectedRuleId = copy.id;
        markDirty();
        refreshRuleRows();
        syncAllFields();
        status("Duplicated rule.", 0xFF70D070);
    }

    private void deleteRule() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        int removedIndex = selectedRuleIndex();
        this.rules.remove(removedIndex);
        if (this.rules.isEmpty()) {
            this.selectedRuleId = null;
        } else {
            this.selectedRuleId = this.rules.get(Math.clamp(removedIndex, 0, this.rules.size() - 1)).id;
        }
        this.selectedFilterIndex = 0;
        this.selectedConditionIndex = 0;
        this.selectedActionIndex = 0;
        markDirty();
        refreshRuleRows();
        syncAllFields();
        status("Deleted rule.", 0xFF70D070);
    }

    private void cycleEventType() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        AutomationEventType[] values = AutomationEventType.values();
        AutomationEventType previousType = rule.eventType;
        int next = (rule.eventType.ordinal() + 1) % values.length;
        rule.eventType = values[next];
        if (shouldResetFilters(rule, previousType)) {
            rule.eventFilters = new ArrayList<>();
            rule.eventFilters.add(defaultFilterFor(rule.eventType));
            this.selectedFilterIndex = 0;
        }
        if (shouldResetActions(rule, previousType)) {
            rule.actions = new ArrayList<>();
            rule.actions.add(defaultActionFor(rule.eventType));
            this.selectedActionIndex = 0;
        }
        normalizeEventBoundFields(rule);
        markDirty();
        refreshRuleRows();
        syncAllFields();
        status("Event type set to " + rule.eventType.name() + ".", 0xFF9FCFCF);
    }

    private void toggleRuleEnabled() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        rule.enabled = !rule.enabled;
        markDirty();
        refreshRuleRows();
        syncRuleHeader();
    }

    private void addFilter() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        if (rule.eventFilters == null) {
            rule.eventFilters = new ArrayList<>();
        }
        rule.eventFilters.add(defaultFilterFor(rule.eventType));
        this.selectedFilterIndex = rule.eventFilters.size() - 1;
        markDirty();
        refreshFilterRows();
        syncFilterFields();
        this.fieldPickerOpen = false;
        this.filterPickerOpen = false;
    }

    private void deleteFilter() {
        AutomationRule rule = selectedRule();
        AutomationRule.EventFilter filter = selectedFilter();
        if (rule == null || filter == null || rule.eventFilters == null) {
            return;
        }
        rule.eventFilters.remove(this.selectedFilterIndex);
        this.selectedFilterIndex = clampIndex(this.selectedFilterIndex, rule.eventFilters.size());
        markDirty();
        refreshFilterRows();
        syncFilterFields();
        if (rule.eventFilters.isEmpty()) {
            this.fieldPickerOpen = false;
            this.filterPickerOpen = false;
        }
    }

    private void cycleFilterOperator() {
        AutomationRule.EventFilter filter = selectedFilter();
        if (filter == null) {
            return;
        }
        filter.operator = nextEnum(filter.operator);
        markDirty();
        refreshFilterRows();
        syncFilterFields();
    }

    private void toggleFilterIgnoreCase() {
        AutomationRule.EventFilter filter = selectedFilter();
        if (filter == null) {
            return;
        }
        filter.ignoreCase = !filter.ignoreCase;
        markDirty();
        refreshFilterRows();
        syncFilterFields();
    }

    private void addCondition() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        if (rule.conditions == null) {
            rule.conditions = new ArrayList<>();
        }
        AutomationRule.Condition condition = defaultConditionFor(rule.eventType);
        rule.conditions.add(condition);
        this.selectedConditionIndex = rule.conditions.size() - 1;
        markDirty();
        refreshConditionRows();
        syncConditionFields();
        this.fieldPickerOpen = false;
        this.conditionPickerOpen = false;
    }

    private void deleteCondition() {
        AutomationRule rule = selectedRule();
        AutomationRule.Condition condition = selectedCondition();
        if (rule == null || condition == null || rule.conditions == null) {
            return;
        }
        rule.conditions.remove(this.selectedConditionIndex);
        this.selectedConditionIndex = clampIndex(this.selectedConditionIndex, rule.conditions.size());
        markDirty();
        refreshConditionRows();
        syncConditionFields();
        if (rule.conditions.isEmpty()) {
            this.fieldPickerOpen = false;
            this.conditionPickerOpen = false;
        }
    }

    private void cycleConditionSource() {
        AutomationRule.Condition condition = selectedCondition();
        if (condition == null) {
            return;
        }
        condition.source = nextEnum(condition.source);
        ensureConditionField(condition);
        markDirty();
        refreshConditionRows();
        syncConditionFields();
    }

    private void cycleConditionOperator() {
        AutomationRule.Condition condition = selectedCondition();
        if (condition == null) {
            return;
        }
        condition.operator = nextEnum(condition.operator);
        markDirty();
        refreshConditionRows();
        syncConditionFields();
    }

    private void togglePlaceholderConditionFieldMode() {
        AutomationRule.Condition condition = selectedCondition();
        if (condition == null || condition.source != AutomationRule.ConditionSource.PLACEHOLDER) {
            return;
        }
        this.placeholderConditionManualEntry = !this.placeholderConditionManualEntry;
        this.syncing = true;
        this.conditionFieldManualField.setValue(normalizePlaceholderField(condition.field));
        this.syncing = false;
        syncConditionFields();
    }

    private void toggleConditionIgnoreCase() {
        AutomationRule.Condition condition = selectedCondition();
        if (condition == null) {
            return;
        }
        condition.ignoreCase = !condition.ignoreCase;
        markDirty();
        refreshConditionRows();
        syncConditionFields();
    }

    private void addAction() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        if (rule.actions == null) {
            rule.actions = new ArrayList<>();
        }
        rule.actions.add(defaultActionFor(rule.eventType));
        this.selectedActionIndex = rule.actions.size() - 1;
        markDirty();
        refreshActionRows();
        syncActionFields();
    }

    private void deleteAction() {
        AutomationRule rule = selectedRule();
        AutomationRule.Action action = selectedAction();
        if (rule == null || action == null || rule.actions == null) {
            return;
        }
        rule.actions.remove(this.selectedActionIndex);
        this.selectedActionIndex = clampIndex(this.selectedActionIndex, rule.actions.size());
        markDirty();
        refreshActionRows();
        syncActionFields();
    }

    private void moveAction(int delta) {
        AutomationRule rule = selectedRule();
        if (rule == null || rule.actions == null || this.selectedActionIndex < 0 || this.selectedActionIndex >= rule.actions.size()) {
            return;
        }
        int target = this.selectedActionIndex + delta;
        if (target < 0 || target >= rule.actions.size()) {
            return;
        }
        AutomationRule.Action current = rule.actions.remove(this.selectedActionIndex);
        rule.actions.add(target, current);
        this.selectedActionIndex = target;
        markDirty();
        refreshActionRows();
        syncActionFields();
    }

    private void cycleActionType() {
        AutomationRule.Action action = selectedAction();
        if (action == null) {
            return;
        }
        action.type = nextEnum(action.type);
        if (action.target == null || action.target.isBlank()) {
            action.target = defaultActionFor(selectedRule() == null ? AutomationEventType.TICK_INTERVAL : selectedRule().eventType).target;
        }
        markDirty();
        refreshActionRows();
        syncActionFields();
    }

    private void toggleActionEnabled() {
        AutomationRule.Action action = selectedAction();
        if (action == null) {
            return;
        }
        action.enabled = !action.enabled;
        markDirty();
        refreshActionRows();
        syncActionFields();
    }

    private void testSelectedRule() {
        AutomationRule rule = selectedRule();
        if (rule == null) {
            return;
        }
        previewRules();
        switch (rule.eventType) {
            case TICK_INTERVAL -> AutomationModule.INSTANCE.fireTickTest();
            case PLAYER_MOVE -> AutomationModule.INSTANCE.firePlayerMoveTest();
            case WORLD_JOIN -> AutomationModule.INSTANCE.fireWorldJoinTest();
            case WORLD_LEAVE -> AutomationModule.INSTANCE.fireWorldLeaveTest();
            case DIMENSION_CHANGE -> AutomationModule.INSTANCE.fireDimensionChangeTest("minecraft:the_nether");
            case CHAT_RECEIVED_REGEX -> AutomationModule.INSTANCE.fireChatTest("automation test message");
            case SCREEN_CHANGED -> AutomationModule.INSTANCE.fireScreenChangeTest("InventoryScreen");
            case WEATHER_CHANGED -> AutomationModule.INSTANCE.fireWeatherChangeTest(false, true, false, false);
            case HOTBAR_SLOT_CHANGED -> AutomationModule.INSTANCE.fireHotbarSlotChangeTest(1);
            case HELD_ITEM_CHANGED -> AutomationModule.INSTANCE.fireHeldItemChangeTest("minecraft:diamond_sword");
            case PLAYER_LEVEL_CHANGED -> AutomationModule.INSTANCE.firePlayerLevelChangeTest(10, 11, 0.25F, 0.05F);
            case PLAYER_HEALTH_CHANGED -> AutomationModule.INSTANCE.firePlayerHealthChangeTest(20.0F, 16.0F);
            case PLAYER_FOOD_CHANGED -> AutomationModule.INSTANCE.firePlayerFoodChangeTest(20, 16);
            case PLAYER_DEATH -> AutomationModule.INSTANCE.firePlayerDeathTest(0.0F);
        }
        status("Fired test event for " + rule.eventType.name() + ".", 0xFF70D070);
    }

    private void syncAllFields() {
        refreshRuleRows();
        syncRuleHeader();
        refreshFilterRows();
        syncFilterFields();
        refreshConditionRows();
        syncConditionFields();
        refreshActionRows();
        syncActionFields();
        syncSectionButtons();
        syncSectionVisibility();
    }

    private void syncRuleHeader() {
        this.syncing = true;
        AutomationRule rule = selectedRule();
        boolean active = rule != null;
        this.filterAddButton.active = active;
        this.conditionAddButton.active = active;
        this.actionAddButton.active = active;
        this.filterPickButton.active = active && rule != null && rule.eventFilters != null && !rule.eventFilters.isEmpty();
        this.conditionPickButton.active = active && rule != null && rule.conditions != null && !rule.conditions.isEmpty();

        this.ruleIdField.setEditable(active);
        this.ruleNameField.setEditable(active);
        this.priorityField.setEditable(active);
        this.cooldownField.setEditable(active);
        this.debounceField.setEditable(active);
        this.rateCountField.setEditable(active);
        this.rateWindowField.setEditable(active);
        this.eventTypeButton.active = active;
        this.enabledButton.active = active;
        this.duplicateRuleButton.active = active;
        this.deleteRuleButton.active = active;
        this.saveButton.active = active || this.dirty;
        this.testButton.active = active;

        if (active) {
            this.ruleIdField.setValue(safe(rule.id));
            this.ruleNameField.setValue(safe(rule.name));
            this.priorityField.setValue(Integer.toString(rule.priority));
            this.cooldownField.setValue(Long.toString(Math.max(0L, rule.cooldownMs)));
            this.debounceField.setValue(Long.toString(Math.max(0L, rule.debounceMs)));
            this.rateCountField.setValue(Integer.toString(Math.max(0, rule.rateLimitCount)));
            this.rateWindowField.setValue(Long.toString(Math.max(0L, rule.rateLimitWindowMs)));
            this.eventTypeButton.setMessage(Component.literal("Event: " + rule.eventType.name()));
            this.enabledButton.setMessage(Component.literal(rule.enabled ? "Enabled" : "Disabled"));
        } else {
            this.ruleIdField.setValue("");
            this.ruleNameField.setValue("");
            this.priorityField.setValue("0");
            this.cooldownField.setValue("0");
            this.debounceField.setValue("0");
            this.rateCountField.setValue("0");
            this.rateWindowField.setValue("0");
            this.eventTypeButton.setMessage(Component.literal("Event"));
            this.enabledButton.setMessage(Component.literal("Disabled"));
        }
        this.syncing = false;
    }

    private void refreshRuleRows() {
        List<String> rows = new ArrayList<>();
        for (AutomationRule rule : this.rules) {
            rows.add("[" + (rule.enabled ? "x" : " ") + "] " + displayRuleName(rule) + " -> " + rule.eventType.name());
        }
        this.ruleList.setRows(rows);
        this.ruleList.setSelectedIndex(selectedRuleIndex());
    }

    private void refreshFilterRows() {
        AutomationRule rule = selectedRule();
        List<String> rows = new ArrayList<>();
        if (rule != null && rule.eventFilters != null) {
            for (AutomationRule.EventFilter filter : rule.eventFilters) {
                rows.add(filterSummary(filter));
            }
        }
        this.filterList.setRows(rows);
        this.filterPickerList.setRows(rows);
        this.selectedFilterIndex = clampIndex(this.selectedFilterIndex, rows.size());
        this.filterList.setSelectedIndex(this.selectedFilterIndex);
        this.filterPickerList.setSelectedIndex(this.selectedFilterIndex);
        boolean active = !rows.isEmpty();
        this.filterPickButton.active = active;
        this.filterDeleteButton.active = active;
        this.filterOperatorButton.active = active;
        this.filterIgnoreCaseButton.active = active;
    }

    private void syncFilterFields() {
        this.syncing = true;
        AutomationRule.EventFilter filter = selectedFilter();
        boolean active = filter != null;
        this.filterValueField.setEditable(active);
        setVisible(this.filterFieldButton, active);
        setVisible(this.filterValueField, active);
        setVisible(this.filterOperatorButton, active);
        setVisible(this.filterIgnoreCaseButton, active);
        if (active) {
            this.filterFieldButton.setMessage(Component.literal(filterFieldButtonLabel(filter)));
            this.filterValueField.setValue(safe(filter.value));
            this.filterOperatorButton.setMessage(Component.literal("Op: " + filter.operator.name()));
            this.filterIgnoreCaseButton.setMessage(Component.literal(filter.ignoreCase ? "Ignore Case" : "Case Sensitive"));
        } else {
            this.filterFieldButton.setMessage(Component.literal("Field"));
            this.filterValueField.setValue("");
            this.filterOperatorButton.setMessage(Component.literal("Op"));
            this.filterIgnoreCaseButton.setMessage(Component.literal("Ignore Case"));
        }
        this.syncing = false;
    }

    private void refreshConditionRows() {
        AutomationRule rule = selectedRule();
        List<String> rows = new ArrayList<>();
        if (rule != null && rule.conditions != null) {
            for (AutomationRule.Condition condition : rule.conditions) {
                rows.add(conditionSummary(condition));
            }
        }
        this.conditionList.setRows(rows);
        this.conditionPickerList.setRows(rows);
        this.selectedConditionIndex = clampIndex(this.selectedConditionIndex, rows.size());
        this.conditionList.setSelectedIndex(this.selectedConditionIndex);
        this.conditionPickerList.setSelectedIndex(this.selectedConditionIndex);
        boolean active = !rows.isEmpty();
        this.conditionPickButton.active = active;
        this.conditionDeleteButton.active = active;
        this.conditionSourceButton.active = active;
        this.conditionFieldModeButton.active = active;
        this.conditionOperatorButton.active = active;
        this.conditionIgnoreCaseButton.active = active;
    }

    private void syncConditionFields() {
        this.syncing = true;
        AutomationRule.Condition condition = selectedCondition();
        boolean active = condition != null;
        boolean placeholderSource = active && condition.source == AutomationRule.ConditionSource.PLACEHOLDER;
        boolean manualPlaceholder = placeholderSource && this.placeholderConditionManualEntry;
        this.conditionValueField.setEditable(active);
        this.conditionFieldManualField.setEditable(manualPlaceholder);
        setVisible(this.conditionSourceButton, active);
        setVisible(this.conditionFieldButton, active && (!placeholderSource || !manualPlaceholder));
        setVisible(this.conditionFieldModeButton, placeholderSource);
        setVisible(this.conditionFieldManualField, manualPlaceholder);
        setVisible(this.conditionValueField, active);
        setVisible(this.conditionOperatorButton, active);
        setVisible(this.conditionIgnoreCaseButton, active);
        if (active) {
            this.conditionFieldButton.setMessage(Component.literal(conditionFieldButtonLabel(condition)));
            this.conditionFieldModeButton.setMessage(Component.literal(this.placeholderConditionManualEntry ? "Manual" : "Pick"));
            this.conditionFieldManualField.setValue(normalizePlaceholderField(condition.field));
            this.conditionValueField.setValue(safe(condition.value));
            this.conditionSourceButton.setMessage(Component.literal("Source: " + condition.source.name()));
            this.conditionOperatorButton.setMessage(Component.literal("Op: " + condition.operator.name()));
            this.conditionIgnoreCaseButton.setMessage(Component.literal(condition.ignoreCase ? "Ignore Case" : "Case Sensitive"));
        } else {
            this.conditionFieldButton.setMessage(Component.literal("Field"));
            this.conditionFieldModeButton.setMessage(Component.literal("Pick"));
            this.conditionFieldManualField.setValue("");
            this.conditionValueField.setValue("");
            this.conditionSourceButton.setMessage(Component.literal("Source"));
            this.conditionOperatorButton.setMessage(Component.literal("Op"));
            this.conditionIgnoreCaseButton.setMessage(Component.literal("Ignore Case"));
        }
        this.syncing = false;
    }

    private void refreshActionRows() {
        AutomationRule rule = selectedRule();
        List<String> rows = new ArrayList<>();
        if (rule != null && rule.actions != null) {
            for (AutomationRule.Action action : rule.actions) {
                rows.add(actionSummary(action));
            }
        }
        this.actionList.setRows(rows);
        this.selectedActionIndex = clampIndex(this.selectedActionIndex, rows.size());
        this.actionList.setSelectedIndex(this.selectedActionIndex);
        boolean active = !rows.isEmpty();
        this.actionDeleteButton.active = active;
        this.actionUpButton.active = active && this.selectedActionIndex > 0;
        this.actionDownButton.active = active && this.selectedActionIndex >= 0 && this.selectedActionIndex < rows.size() - 1;
        this.actionTypeButton.active = active;
        this.actionEnabledButton.active = active;
        this.saveButton.active = selectedRule() != null || this.dirty;
    }

    private void syncActionFields() {
        this.syncing = true;
        AutomationRule.Action action = selectedAction();
        boolean active = action != null;
        this.actionTargetField.setEditable(active);
        this.actionArgumentField.setEditable(active);
        setVisible(this.actionTypeButton, active);
        setVisible(this.actionTargetField, active);
        setVisible(this.actionArgumentField, active);
        setVisible(this.actionEnabledButton, active);
        if (active) {
            this.actionTypeButton.setMessage(Component.literal("Type: " + action.type.name()));
            this.actionTargetField.setValue(safe(action.target));
            this.actionArgumentField.setValue(safe(action.argument));
            this.actionEnabledButton.setMessage(Component.literal(action.enabled ? "Enabled" : "Disabled"));
        } else {
            this.actionTypeButton.setMessage(Component.literal("Type"));
            this.actionTargetField.setValue("");
            this.actionArgumentField.setValue("");
            this.actionEnabledButton.setMessage(Component.literal("Enabled"));
        }
        this.syncing = false;
    }

    private void syncSectionVisibility() {
        boolean hasRule = selectedRule() != null;
        boolean showFilters = this.activeSection == EditorSection.FILTERS;
        boolean showConditions = this.activeSection == EditorSection.CONDITIONS;
        boolean showActions = this.activeSection == EditorSection.ACTIONS;
        boolean hasFilter = selectedFilter() != null;
        boolean hasCondition = selectedCondition() != null;
        boolean hasAction = selectedAction() != null;

        setWidgetState(this.filterPickButton, showFilters, showFilters && hasRule && !filterListRows().isEmpty());
        setWidgetState(this.filterAddButton, showFilters, showFilters && hasRule);
        setWidgetState(this.filterDeleteButton, showFilters, showFilters && hasFilter);
        setWidgetState(this.filterFieldButton, showFilters && hasFilter, showFilters && hasFilter);
        setWidgetState(this.filterOperatorButton, showFilters && hasFilter, showFilters && hasFilter);
        setWidgetState(this.filterIgnoreCaseButton, showFilters && hasFilter, showFilters && hasFilter);
        setWidgetState(this.filterValueField, showFilters && hasFilter, showFilters && hasFilter);

        setWidgetState(this.conditionPickButton, showConditions, showConditions && hasRule && !conditionListRows().isEmpty());
        setWidgetState(this.conditionAddButton, showConditions, showConditions && hasRule);
        setWidgetState(this.conditionDeleteButton, showConditions, showConditions && hasCondition);
        setWidgetState(this.conditionSourceButton, showConditions && hasCondition, showConditions && hasCondition);
        boolean placeholderCondition = hasCondition && selectedCondition().source == AutomationRule.ConditionSource.PLACEHOLDER;
        boolean manualPlaceholder = placeholderCondition && this.placeholderConditionManualEntry;
        setWidgetState(this.conditionFieldButton, showConditions && hasCondition && !manualPlaceholder, showConditions && hasCondition && !manualPlaceholder);
        setWidgetState(this.conditionFieldModeButton, showConditions && placeholderCondition, showConditions && placeholderCondition);
        setWidgetState(this.conditionFieldManualField, showConditions && manualPlaceholder, showConditions && manualPlaceholder);
        setWidgetState(this.conditionOperatorButton, showConditions && hasCondition, showConditions && hasCondition);
        setWidgetState(this.conditionIgnoreCaseButton, showConditions && hasCondition, showConditions && hasCondition);
        setWidgetState(this.conditionValueField, showConditions && hasCondition, showConditions && hasCondition);

        setWidgetState(this.actionAddButton, showActions, showActions && hasRule);
        setWidgetState(this.actionDeleteButton, showActions, showActions && hasAction);
        setWidgetState(this.actionUpButton, showActions, showActions && hasAction && this.selectedActionIndex > 0);
        setWidgetState(this.actionDownButton, showActions, showActions && hasAction && ruleHasActionAfterSelection());
        setWidgetState(this.actionTypeButton, showActions && hasAction, showActions && hasAction);
        setWidgetState(this.actionTargetField, showActions && hasAction, showActions && hasAction);
        setWidgetState(this.actionArgumentField, showActions && hasAction, showActions && hasAction);
        setWidgetState(this.actionEnabledButton, showActions && hasAction, showActions && hasAction);

        setWidgetState(this.saveButton, true, hasRule || this.dirty);
        setWidgetState(this.testButton, true, hasRule);
    }

    private boolean ruleHasActionAfterSelection() {
        AutomationRule rule = selectedRule();
        return rule != null
                && rule.actions != null
                && this.selectedActionIndex >= 0
                && this.selectedActionIndex < rule.actions.size() - 1;
    }

    private void renderOverviewSummary(GuiGraphicsExtractor context,
                                       int x,
                                       int y,
                                       int width,
                                       int height,
                                       AutomationRule rule,
                                       AutomationEngine.RuleRuntimeSnapshot runtime) {
        int rowY = y;
        rowY = drawInfoLine(context, x, rowY, width, rule == null ? "Select a rule to edit automation details." : "Event: " + rule.eventType.name(), 0xFFFFFFFF);
        if (rule != null) {
            rowY = drawInfoLine(context, x, rowY, width, "Filters: " + sizeOf(rule.eventFilters) + "  Conditions: " + sizeOf(rule.conditions) + "  Actions: " + sizeOf(rule.actions), 0xFFB8B8B8);
            rowY = drawInfoLine(context, x, rowY, width, "Priority " + rule.priority + "  Cooldown " + rule.cooldownMs + "ms  Debounce " + rule.debounceMs + "ms", 0xFFB8B8B8);
            rowY = drawInfoLine(context, x, rowY, width, actionHint(rule), 0xFF9FCFCF);
        }
        rowY += 6;
        rowY = drawExecutionSummary(context, x, rowY, width, runtime);
        rowY += 6;
        drawInfoLine(context, x, rowY, width, "Use the section tabs to focus on one editor at a time.", 0xFF9FCFCF);
    }

    private void renderActionsHelp(GuiGraphicsExtractor context,
                                   int x,
                                   int y,
                                   int width,
                                   int height,
                                   AutomationRule rule,
                                   AutomationEngine.RuleRuntimeSnapshot runtime) {
        int rowY = y;
        rowY = drawInfoLine(context, x, rowY, width, "Targets and arguments expand placeholders before dispatch.", 0xFFFFFFFF);
        rowY = drawInfoLine(context, x, rowY, width, "{event.*} reads the triggering event.", 0xFFB8B8B8);
        rowY = drawInfoLine(context, x, rowY, width, "{client.*}, {player.*}, {world.*} read live state.", 0xFFB8B8B8);
        rowY = drawInfoLine(context, x, rowY, width, "Regular macro placeholders like {player.name} still work.", 0xFFB8B8B8);
        rowY += 6;
        rowY = drawExecutionSummary(context, x, rowY, width, runtime);
        rowY += 6;
        int diagnosticsY = rowY + 6;
        int diagnosticsH = Math.max(24, height - (diagnosticsY - y));
        if (diagnosticsH > 24) {
            context.text(this.owner.workbenchTextRenderer(), "Recent Diagnostics", x, rowY, 0xFFFFFFFF);
            renderDiagnostics(context, x, diagnosticsY, width, diagnosticsH);
        }
    }

    private int drawExecutionSummary(GuiGraphicsExtractor context,
                                     int x,
                                     int y,
                                     int width,
                                     AutomationEngine.RuleRuntimeSnapshot runtime) {
        if (runtime.lastExecution() == null) {
            return drawInfoLine(context, x, y, width, "Last result: never executed", 0xFFB8B8B8);
        }
        AutomationEngine.ExecutionRecord last = runtime.lastExecution();
        int rowY = drawInfoLine(context, x, y, width, "Last result: " + (last.success() ? "OK" : "FAIL"), last.success() ? 0xFF70D070 : 0xFFFF8080);
        rowY = drawInfoLine(context, x, rowY, width, "Message: " + last.message(), 0xFFB8B8B8);
        rowY = drawInfoLine(context, x, rowY, width, "Event: " + last.eventSummary(), 0xFFB8B8B8);
        if (runtime.pendingDebounce()) {
            rowY = drawInfoLine(context, x, rowY, width, "Debounce pending until " + runtime.pendingDebounceDueAtMs(), 0xFFFFFF99);
        }
        return rowY;
    }

    private void renderFieldCatalog(GuiGraphicsExtractor context,
                                    int x,
                                    int y,
                                    int width,
                                    int height,
                                    List<AutomationFieldCatalog.FieldOption> options,
                                    String selectedKey) {
        if (options == null || options.isEmpty()) {
            context.text(this.owner.workbenchTextRenderer(), "No fields available.", x, y, 0xFF909090);
            return;
        }
        int x1 = Math.max(0, x - 1);
        int y1 = Math.max(0, y - 1);
        int x2 = Math.min(this.owner.width, x + width);
        int y2 = Math.min(this.owner.height, y + height);
        boolean scissor = x2 > x1 && y2 > y1;
        if (scissor) {
            context.enableScissor(x1, y1, x2, y2);
        }
        int rowY = y;
        for (AutomationFieldCatalog.FieldOption option : options) {
            if (rowY > y + height - 24) {
                break;
            }
            boolean selected = safe(selectedKey).equals(option.key());
            int titleColor = selected ? 0xFFFFFFFF : 0xFFB8E8FF;
            int descColor = selected ? 0xFFDFDFDF : 0xFF909090;
            context.text(this.owner.workbenchTextRenderer(),
                    trimToWidth(option.key() + " - " + option.label(), width),
                    x,
                    rowY,
                    titleColor);
            rowY += 12;
            context.text(this.owner.workbenchTextRenderer(),
                    trimToWidth(option.description(), width - 8),
                    x + 6,
                    rowY,
                    descColor);
            rowY += 14;
        }
        if (scissor) {
            context.disableScissor();
        }
    }

    private int drawInfoLine(GuiGraphicsExtractor context, int x, int y, int width, String text, int color) {
        context.text(this.owner.workbenchTextRenderer(), trimToWidth(text, width), x, y, color);
        return y + 14;
    }

    private static int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private void renderDiagnostics(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        List<AutomationEngine.LogEntry> logs = AutomationModule.INSTANCE.engine().getDiagnostics(DIAGNOSTIC_ROWS);
        if (logs.isEmpty()) {
            context.text(this.owner.workbenchTextRenderer(), "No diagnostics yet.", x, y, 0xFF909090);
            return;
        }
        int x1 = Math.max(0, x - 1);
        int y1 = Math.max(0, y - 1);
        int x2 = Math.min(this.owner.width, x + width);
        int y2 = Math.min(this.owner.height, y + height);
        boolean scissor = x2 > x1 && y2 > y1;
        if (scissor) {
            context.enableScissor(x1, y1, x2, y2);
        }
        int rowY = y;
        int lineHeight = 12;
        int visible = Math.max(1, Math.max(0, height - 4) / lineHeight);
        int start = Math.max(0, logs.size() - visible);
        for (int i = start; i < logs.size() && rowY < y + height - lineHeight; i++) {
            AutomationEngine.LogEntry log = logs.get(i);
            int color = switch (log.level()) {
                case "ERROR" -> 0xFFFF8080;
                case "WARN" -> 0xFFFFD17A;
                case "INFO" -> 0xFFB8E8B8;
                default -> 0xFFB8B8B8;
            };
            String prefix = log.ruleId() == null ? "" : "[" + log.ruleId() + "] ";
            String line = log.level() + " " + prefix + log.message();
            context.text(this.owner.workbenchTextRenderer(), trimToWidth(line, width), x, rowY, color);
            rowY += lineHeight;
        }
        if (scissor) {
            context.disableScissor();
        }
    }

    private void updateRuleInt(String raw, RuleIntField field) {
        if (this.syncing) {
            return;
        }
        AutomationRule rule = selectedRule();
        if (rule == null || raw == null || raw.isBlank()) {
            return;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            switch (field) {
                case PRIORITY -> rule.priority = value;
                case RATE_COUNT -> rule.rateLimitCount = Math.max(0, value);
            }
            markDirty();
            if (field == RuleIntField.PRIORITY) {
                this.rules.sort((left, right) -> Integer.compare(right.priority, left.priority));
                refreshRuleRows();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateRuleLong(String raw, RuleLongField field) {
        if (this.syncing) {
            return;
        }
        AutomationRule rule = selectedRule();
        if (rule == null || raw == null || raw.isBlank()) {
            return;
        }
        try {
            long value = Long.parseLong(raw.trim());
            switch (field) {
                case COOLDOWN -> rule.cooldownMs = Math.max(0L, value);
                case DEBOUNCE -> rule.debounceMs = Math.max(0L, value);
                case RATE_WINDOW -> rule.rateLimitWindowMs = Math.max(0L, value);
            }
            markDirty();
        } catch (NumberFormatException ignored) {
        }
    }

    private void markDirty() {
        this.dirty = true;
        previewRules();
        this.saveButton.active = true;
        this.statusMessage = "Unsaved changes are live in memory. Click Save Rules to persist them.";
        this.statusColor = 0xFFFFFF99;
    }

    private void status(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    private int selectedRuleIndex() {
        if (this.selectedRuleId == null) {
            return this.rules.isEmpty() ? -1 : 0;
        }
        for (int i = 0; i < this.rules.size(); i++) {
            if (this.selectedRuleId.equals(this.rules.get(i).id)) {
                return i;
            }
        }
        return this.rules.isEmpty() ? -1 : 0;
    }

    private AutomationRule selectedRule() {
        int index = selectedRuleIndex();
        if (index < 0 || index >= this.rules.size()) {
            return null;
        }
        return this.rules.get(index);
    }

    private AutomationRule.EventFilter selectedFilter() {
        AutomationRule rule = selectedRule();
        if (rule == null || rule.eventFilters == null || this.selectedFilterIndex < 0 || this.selectedFilterIndex >= rule.eventFilters.size()) {
            return null;
        }
        return rule.eventFilters.get(this.selectedFilterIndex);
    }

    private AutomationRule.Condition selectedCondition() {
        AutomationRule rule = selectedRule();
        if (rule == null || rule.conditions == null || this.selectedConditionIndex < 0 || this.selectedConditionIndex >= rule.conditions.size()) {
            return null;
        }
        return rule.conditions.get(this.selectedConditionIndex);
    }

    private AutomationRule.Action selectedAction() {
        AutomationRule rule = selectedRule();
        if (rule == null || rule.actions == null || this.selectedActionIndex < 0 || this.selectedActionIndex >= rule.actions.size()) {
            return null;
        }
        return rule.actions.get(this.selectedActionIndex);
    }

    private static int clampIndex(int current, int size) {
        if (size <= 0) {
            return -1;
        }
        return Math.clamp(Math.max(current, 0), 0, size - 1);
    }

    private static String displayRuleName(AutomationRule rule) {
        String name = safe(rule.name);
        return name.isBlank() ? safe(rule.id) : name;
    }

    private static String filterSummary(AutomationRule.EventFilter filter) {
        if (filter == null) {
            return "(none)";
        }
        return safe(filter.field) + " " + filter.operator.name() + " " + safe(filter.value);
    }

    private static String conditionSummary(AutomationRule.Condition condition) {
        if (condition == null) {
            return "(none)";
        }
        return condition.source.name() + "." + displayConditionField(condition.source, condition.field) + " " + condition.operator.name() + " " + safe(condition.value);
    }

    private static String actionSummary(AutomationRule.Action action) {
        if (action == null) {
            return "(none)";
        }
        String prefix = action.enabled ? "" : "[off] ";
        String target = safe(action.target);
        return prefix + action.type.name() + " -> " + (target.isBlank() ? "(unset)" : target);
    }

    private static String actionHint(AutomationRule rule) {
        if (rule == null) {
            return "Select a rule to edit triggers, conditions, and actions.";
        }
        return switch (rule.eventType) {
            case TICK_INTERVAL -> "Tick rules use an event filter like field=intervalTicks value=20.";
            case PLAYER_MOVE -> "Move rules can filter on distance or destination coordinates.";
            case WORLD_JOIN -> "Join rules usually need no filters.";
            case WORLD_LEAVE -> "Leave rules trigger when unloading/disconnecting from a world.";
            case DIMENSION_CHANGE -> "Dimension rules often use toDimension or fromDimension.";
            case CHAT_RECEIVED_REGEX -> "Chat rules usually use field=message with REGEX.";
            case SCREEN_CHANGED -> "Screen rules can match fromScreen, toScreen, or screen presence changes.";
            case WEATHER_CHANGED -> "Weather rules can detect rain/thunder starts and stops.";
            case HOTBAR_SLOT_CHANGED -> "Hotbar rules can match fromSlot or toSlot.";
            case HELD_ITEM_CHANGED -> "Held-item rules can match item ids or display names.";
            case PLAYER_LEVEL_CHANGED -> "Level rules can react to level-ups or XP progress changes.";
            case PLAYER_HEALTH_CHANGED -> "Health rules can match toHealth or deltaHealth.";
            case PLAYER_FOOD_CHANGED -> "Food rules can match toFood or deltaFood.";
            case PLAYER_DEATH -> "Death rules run once when the client player dies.";
        };
    }

    private static AutomationRule.EventFilter defaultFilterFor(AutomationEventType eventType) {
        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.ignoreCase = true;
        switch (eventType) {
            case TICK_INTERVAL -> {
                filter.field = "intervalTicks";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "20";
            }
            case PLAYER_MOVE -> {
                filter.field = "distance";
                filter.operator = AutomationRule.Operator.GREATER_THAN;
                filter.value = "1.0";
            }
            case WORLD_JOIN -> {
                filter.field = "dimension";
                filter.operator = AutomationRule.Operator.EXISTS;
                filter.value = "";
            }
            case WORLD_LEAVE -> {
                filter.field = "fromDimension";
                filter.operator = AutomationRule.Operator.EXISTS;
                filter.value = "";
            }
            case DIMENSION_CHANGE -> {
                filter.field = "toDimension";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "minecraft:the_nether";
            }
            case CHAT_RECEIVED_REGEX -> {
                filter.field = "message";
                filter.operator = AutomationRule.Operator.REGEX;
                filter.value = ".*";
            }
            case SCREEN_CHANGED -> {
                filter.field = "toScreen";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "InventoryScreen";
            }
            case WEATHER_CHANGED -> {
                filter.field = "startedRaining";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "true";
            }
            case HOTBAR_SLOT_CHANGED -> {
                filter.field = "toSlot";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "1";
            }
            case HELD_ITEM_CHANGED -> {
                filter.field = "toItemId";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "minecraft:diamond_sword";
            }
            case PLAYER_LEVEL_CHANGED -> {
                filter.field = "deltaLevel";
                filter.operator = AutomationRule.Operator.GREATER_THAN;
                filter.value = "0";
            }
            case PLAYER_HEALTH_CHANGED -> {
                filter.field = "deltaHealth";
                filter.operator = AutomationRule.Operator.LESS_THAN;
                filter.value = "0";
            }
            case PLAYER_FOOD_CHANGED -> {
                filter.field = "deltaFood";
                filter.operator = AutomationRule.Operator.LESS_THAN;
                filter.value = "0";
            }
            case PLAYER_DEATH -> {
                filter.field = "health";
                filter.operator = AutomationRule.Operator.LESS_OR_EQUAL;
                filter.value = "0";
            }
        }
        return filter;
    }

    private static AutomationRule.Condition defaultConditionFor(AutomationEventType eventType) {
        AutomationRule.Condition condition = new AutomationRule.Condition();
        condition.source = AutomationRule.ConditionSource.EVENT;
        condition.operator = AutomationRule.Operator.EQUALS;
        condition.ignoreCase = true;
        switch (eventType) {
            case TICK_INTERVAL -> condition.field = "worldLoaded";
            case PLAYER_MOVE -> condition.field = "distance";
            case WORLD_JOIN -> condition.field = "dimension";
            case WORLD_LEAVE -> condition.field = "fromDimension";
            case DIMENSION_CHANGE -> condition.field = "toDimension";
            case CHAT_RECEIVED_REGEX -> {
                condition.field = "message";
                condition.operator = AutomationRule.Operator.CONTAINS;
            }
            case SCREEN_CHANGED -> condition.field = "toScreen";
            case WEATHER_CHANGED -> condition.field = "startedRaining";
            case HOTBAR_SLOT_CHANGED -> condition.field = "toSlot";
            case HELD_ITEM_CHANGED -> condition.field = "toItemId";
            case PLAYER_LEVEL_CHANGED -> condition.field = "toLevel";
            case PLAYER_HEALTH_CHANGED -> condition.field = "toHealth";
            case PLAYER_FOOD_CHANGED -> condition.field = "toFood";
            case PLAYER_DEATH -> condition.field = "health";
        }
        condition.value = "";
        return condition;
    }

    private static AutomationRule.Action defaultActionFor(AutomationEventType eventType) {
        return AutomationRule.Action.script("heartbeat.kts");
    }

    private String filterFieldButtonLabel(AutomationRule.EventFilter filter) {
        AutomationFieldCatalog.FieldOption option = AutomationFieldCatalog.findEventFilterField(selectedRule() == null ? null : selectedRule().eventType, filter.field);
        return option == null ? "Field: " + safe(filter.field) : "Field: " + option.key();
    }

    private String conditionFieldButtonLabel(AutomationRule.Condition condition) {
        AutomationFieldCatalog.FieldOption option = AutomationFieldCatalog.findConditionField(
                condition.source,
                selectedRule() == null ? null : selectedRule().eventType,
                condition.field
        );
        String field = option == null ? safe(condition.field) : option.key();
        return "Field: " + displayConditionField(condition.source, field);
    }

    private String filterFieldHelpText() {
        AutomationRule.EventFilter filter = selectedFilter();
        AutomationRule rule = selectedRule();
        if (filter == null || rule == null) {
            return "";
        }
        AutomationFieldCatalog.FieldOption option = AutomationFieldCatalog.findEventFilterField(rule.eventType, filter.field);
        if (option == null) {
            return "Choose a field from the picker to see what this event exposes.";
        }
        return option.key() + ": " + option.description();
    }

    private String conditionFieldHelpText() {
        AutomationRule.Condition condition = selectedCondition();
        AutomationRule rule = selectedRule();
        if (condition == null || rule == null) {
            return "";
        }
        AutomationFieldCatalog.FieldOption option = AutomationFieldCatalog.findConditionField(condition.source, rule.eventType, condition.field);
        if (option == null) {
            if (condition.source == AutomationRule.ConditionSource.PLACEHOLDER) {
                return "Use Pick for known tokens, or Manual to enter any placeholder token.";
            }
            return "Choose a field from the picker to see what the selected source exposes.";
        }
        return displayConditionField(condition.source, option.key()) + ": " + option.description();
    }

    private void ensureConditionField(AutomationRule.Condition condition) {
        AutomationRule rule = selectedRule();
        if (condition == null || rule == null) {
            return;
        }
        if (AutomationFieldCatalog.findConditionField(condition.source, rule.eventType, condition.field) != null) {
            return;
        }
        if (condition.source == AutomationRule.ConditionSource.PLACEHOLDER) {
            String normalized = normalizePlaceholderField(condition.field);
            if (!normalized.isBlank()) {
                condition.field = normalized;
                return;
            }
        }
        if (condition.source == AutomationRule.ConditionSource.EVENT) {
            condition.field = defaultConditionFor(rule.eventType).field;
            return;
        }
        List<AutomationFieldCatalog.FieldOption> options = AutomationFieldCatalog.conditionFields(condition.source, rule.eventType);
        if (!options.isEmpty()) {
            condition.field = options.getFirst().key();
        }
    }

    private void normalizeEventBoundFields(AutomationRule rule) {
        if (rule == null) {
            return;
        }
        if (rule.eventFilters != null) {
            for (AutomationRule.EventFilter filter : rule.eventFilters) {
                if (filter == null) {
                    continue;
                }
                if (AutomationFieldCatalog.findEventFilterField(rule.eventType, filter.field) == null) {
                    filter.field = defaultFilterFor(rule.eventType).field;
                }
            }
        }
        if (rule.conditions != null) {
            for (AutomationRule.Condition condition : rule.conditions) {
                ensureConditionField(condition);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeFieldValue(EditBox widget, String value) {
        String sanitized = sanitizeInlineText(value);
        if (!safe(value).equals(sanitized)) {
            this.syncing = true;
            widget.setValue(sanitized);
            this.syncing = false;
        }
        return sanitized;
    }

    private String sanitizeRuleIdInput(EditBox widget, String value) {
        String sanitized = sanitizeInlineText(value).trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
        if (!safe(value).equals(sanitized)) {
            this.syncing = true;
            widget.setValue(sanitized);
            this.syncing = false;
        }
        return sanitized;
    }

    private static String sanitizeInlineText(String value) {
        return safe(value).replace('\n', ' ').replace('\r', ' ');
    }

    private static <E extends Enum<E>> E nextEnum(E current) {
        E[] values = current.getDeclaringClass().getEnumConstants();
        return values[(current.ordinal() + 1) % values.length];
    }

    private me.m0dii.gui.local.ValidatedEditBox textField(int x, int y, int width, int maxLength, String placeholder) {
        me.m0dii.gui.local.ValidatedEditBox widget = new me.m0dii.gui.local.ValidatedEditBox(this.owner.workbenchTextRenderer(), x, y, width, FIELD_H, Component.literal(placeholder));
        widget.setMaxLength(maxLength);
        return widget;
    }

    private EditBox numberField(int x, int y, int width, int maxLength, String placeholder) {
        me.m0dii.gui.local.ValidatedEditBox widget = textField(x, y, width, maxLength, placeholder);
        widget.setFilter(value -> value == null || value.isEmpty() || value.chars().allMatch(Character::isDigit));
        return widget;
    }

    private EditBox signedNumberField(int x, int y, int width, int maxLength, String placeholder) {
        me.m0dii.gui.local.ValidatedEditBox widget = textField(x, y, width, maxLength, placeholder);
        widget.setFilter(value -> {
            if (value == null || value.isEmpty() || "-".equals(value)) {
                return true;
            }
            int start = value.startsWith("-") ? 1 : 0;
            return start < value.length() && value.substring(start).chars().allMatch(Character::isDigit);
        });
        return widget;
    }

    private String trimToWidth(String raw, int maxWidth) {
        if (raw == null) {
            return "";
        }
        if (this.owner.workbenchTextRenderer().width(raw) <= maxWidth) {
            return raw;
        }
        String text = raw;
        while (text.length() > 4 && this.owner.workbenchTextRenderer().width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private <T extends AbstractWidget> T register(T widget) {
        this.widgets.add(widget);
        this.owner.addRenderableWidget(widget);
        return widget;
    }

    private void previewRules() {
        AutomationModule.INSTANCE.engine().setRulesInMemory(this.rules);
    }

    private void openFilterFieldPicker() {
        AutomationRule rule = selectedRule();
        AutomationRule.EventFilter filter = selectedFilter();
        if (rule == null || filter == null) {
            return;
        }
        openFieldPicker(FieldPickerMode.FILTER_FIELD, AutomationFieldCatalog.eventFilterFields(rule.eventType), filter.field);
    }

    private void openConditionFieldPicker() {
        AutomationRule rule = selectedRule();
        AutomationRule.Condition condition = selectedCondition();
        if (rule == null || condition == null) {
            return;
        }
        openFieldPicker(FieldPickerMode.CONDITION_FIELD, AutomationFieldCatalog.conditionFields(condition.source, rule.eventType), condition.field);
    }

    private void openFieldPicker(FieldPickerMode mode, List<AutomationFieldCatalog.FieldOption> options, String currentField) {
        this.fieldPickerMode = mode;
        this.fieldPickerOpen = true;
        this.filterPickerOpen = false;
        this.conditionPickerOpen = false;
        this.fieldPickerSourceOptions = withCurrentCustomOption(options, normalizePlaceholderField(currentField));
        this.fieldPickerSearchQuery = "";
        this.syncing = true;
        this.fieldPickerSearchField.setValue("");
        this.syncing = false;
        this.fieldPickerSearchField.setFocused(fieldPickerShowsSearch());
        refreshFieldPickerRows(currentField);
    }

    private void openFilterPicker() {
        if (selectedRule() == null || selectedRule().eventFilters == null || selectedRule().eventFilters.isEmpty()) {
            return;
        }
        closeFieldPicker();
        this.filterPickerOpen = true;
        this.conditionPickerOpen = false;
        this.filterPickerList.setRows(filterListRows());
        this.filterPickerList.setSelectedIndex(this.selectedFilterIndex);
    }

    private void renderFilterPicker(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        FilterPickerLayout layout = filterPickerLayout();
        context.fill(0, 0, this.owner.width, this.owner.height, MODAL_SCRIM_COLOR);
        UiForms.drawPanel(context, layout.x(), layout.y(), layout.width(), layout.height());
        context.text(this.owner.workbenchTextRenderer(), "Select Filter", layout.x() + 10, layout.y() + 8, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Click a filter to edit it.", layout.x() + 10, layout.y() + 22, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Close", layout.closeX(), layout.y() + 8, 0xFFFFFFFF);
        this.filterPickerList.setBounds(layout.listX(), layout.listY(), layout.listW(), layout.listH());
        this.filterPickerList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
    }

    private boolean handleFilterPickerClick(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return true;
        }
        FilterPickerLayout layout = filterPickerLayout();
        if (contains(mouseX, mouseY, layout.closeX(), layout.y() + 6, 38, 12)) {
            this.filterPickerOpen = false;
            return true;
        }
        if (this.filterPickerList.handleMouseClick(mouseX, mouseY)) {
            this.selectedFilterIndex = this.filterPickerList.selectedIndex();
            syncFilterFields();
            this.filterPickerOpen = false;
            return true;
        }
        if (!contains(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height())) {
            this.filterPickerOpen = false;
            return true;
        }
        return true;
    }

    private void closeActiveOverlay() {
        closeFieldPicker();
        this.filterPickerOpen = false;
        this.conditionPickerOpen = false;
    }

    private void openConditionPicker() {
        if (selectedRule() == null || selectedRule().conditions == null || selectedRule().conditions.isEmpty()) {
            return;
        }
        closeFieldPicker();
        this.conditionPickerOpen = true;
        this.filterPickerOpen = false;
        this.conditionPickerList.setRows(conditionListRows());
        this.conditionPickerList.setSelectedIndex(this.selectedConditionIndex);
    }

    private void renderConditionPicker(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        ConditionPickerLayout layout = conditionPickerLayout();
        context.fill(0, 0, this.owner.width, this.owner.height, MODAL_SCRIM_COLOR);
        UiForms.drawPanel(context, layout.x(), layout.y(), layout.width(), layout.height());
        context.text(this.owner.workbenchTextRenderer(), "Select Condition", layout.x() + 10, layout.y() + 8, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), "Click a condition to edit it.", layout.x() + 10, layout.y() + 22, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Close", layout.closeX(), layout.y() + 8, 0xFFFFFFFF);
        this.conditionPickerList.setBounds(layout.listX(), layout.listY(), layout.listW(), layout.listH());
        this.conditionPickerList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
    }

    private boolean handleConditionPickerClick(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return true;
        }
        ConditionPickerLayout layout = conditionPickerLayout();
        if (contains(mouseX, mouseY, layout.closeX(), layout.y() + 6, 38, 12)) {
            this.conditionPickerOpen = false;
            return true;
        }
        if (this.conditionPickerList.handleMouseClick(mouseX, mouseY)) {
            this.selectedConditionIndex = this.conditionPickerList.selectedIndex();
            syncConditionFields();
            this.conditionPickerOpen = false;
            return true;
        }
        if (!contains(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height())) {
            this.conditionPickerOpen = false;
            return true;
        }
        return true;
    }

    private void renderFieldPicker(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        FieldPickerLayout layout = fieldPickerLayout();
        context.fill(0, 0, this.owner.width, this.owner.height, MODAL_SCRIM_COLOR);
        UiForms.drawPanel(context, layout.x(), layout.y(), layout.width(), layout.height());
        context.text(this.owner.workbenchTextRenderer(), fieldPickerTitle(), layout.x() + 10, layout.y() + 8, 0xFFFFFFFF);
        context.text(this.owner.workbenchTextRenderer(), trimToWidth(fieldPickerSubtitle(), layout.width() - 72), layout.x() + 10, layout.y() + 22, 0xFFB8B8B8);
        context.text(this.owner.workbenchTextRenderer(), "Close", layout.closeX(), layout.y() + 8, 0xFFFFFFFF);
        int listY = layout.listY();
        int listH = layout.listH();
        if (fieldPickerShowsSearch()) {
            this.fieldPickerSearchField.setX(layout.listX());
            this.fieldPickerSearchField.setY(layout.listY());
            this.fieldPickerSearchField.setWidth(layout.listW());
            this.fieldPickerSearchField.extractRenderState(context, mouseX, mouseY, 0.0F);
            listY += 24;
            listH -= 24;
        }
        this.fieldPickerList.setBounds(layout.listX(), listY, layout.listW(), listH);
        this.fieldPickerList.extractRenderState(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);

        AutomationFieldCatalog.FieldOption option = selectedFieldPickerOption();
        if (option != null) {
            String key = fieldPickerShowsSearch() ? formatPlaceholderKey(option.key()) : option.key();
            context.text(
                    this.owner.workbenchTextRenderer(),
                    trimToWidth(key + ": " + option.description(), layout.width() - 20),
                    layout.x() + 10,
                    layout.footerY(),
                    0xFF909090
            );
        }
    }

    private boolean handleFieldPickerClick(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return true;
        }
        FieldPickerLayout layout = fieldPickerLayout();
        if (contains(mouseX, mouseY, layout.closeX(), layout.y() + 6, 38, 12)) {
            closeFieldPicker();
            return true;
        }
        if (fieldPickerShowsSearch()) {
            boolean insideSearch = contains(mouseX, mouseY,
                    this.fieldPickerSearchField.getX(),
                    this.fieldPickerSearchField.getY(),
                    this.fieldPickerSearchField.getWidth(),
                    this.fieldPickerSearchField.getHeight());
            this.fieldPickerSearchField.setFocused(insideSearch);
            if (insideSearch) {
                return true;
            }
        }
        if (this.fieldPickerList.handleMouseClick(mouseX, mouseY)) {
            applyFieldPickerSelection(this.fieldPickerList.selectedIndex());
            closeFieldPicker();
            return true;
        }
        if (!contains(mouseX, mouseY, layout.x(), layout.y(), layout.width(), layout.height())) {
            closeFieldPicker();
            return true;
        }
        return true;
    }

    private static void setWidgetState(AbstractWidget widget, boolean visible, boolean active) {
        widget.visible = visible;
        widget.active = visible && active;
    }

    private static void setVisible(AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }

    private List<String> filterListRows() {
        List<String> rows = new ArrayList<>();
        AutomationRule rule = selectedRule();
        if (rule != null && rule.eventFilters != null) {
            for (AutomationRule.EventFilter filter : rule.eventFilters) {
                rows.add(filterSummary(filter));
            }
        }
        return rows;
    }

    private List<String> conditionListRows() {
        List<String> rows = new ArrayList<>();
        AutomationRule rule = selectedRule();
        if (rule != null && rule.conditions != null) {
            for (AutomationRule.Condition condition : rule.conditions) {
                rows.add(conditionSummary(condition));
            }
        }
        return rows;
    }

    private List<String> fieldPickerRows(List<AutomationFieldCatalog.FieldOption> options) {
        List<String> rows = new ArrayList<>();
        for (AutomationFieldCatalog.FieldOption option : options) {
            if (fieldPickerShowsSearch()) {
                rows.add(formatPlaceholderKey(option.key()));
            } else if (option.label().equals(option.key())) {
                rows.add(option.key());
            } else {
                rows.add(option.label() + " [" + option.key() + "]");
            }
        }
        return rows;
    }

    private void refreshFieldPickerRows(String currentField) {
        String normalizedCurrent = normalizePlaceholderField(currentField);
        List<AutomationFieldCatalog.FieldOption> filtered = this.fieldPickerSourceOptions;
        if (fieldPickerShowsSearch()) {
            String query = safe(this.fieldPickerSearchQuery).trim().toLowerCase(Locale.ROOT);
            if (!query.isEmpty()) {
                filtered = this.fieldPickerSourceOptions.stream()
                        .filter(option -> option.key().toLowerCase(Locale.ROOT).contains(query)
                                || option.label().toLowerCase(Locale.ROOT).contains(query)
                                || option.description().toLowerCase(Locale.ROOT).contains(query))
                        .sorted(Comparator.comparing(AutomationFieldCatalog.FieldOption::key))
                        .toList();
            } else {
                filtered = this.fieldPickerSourceOptions.stream()
                        .sorted(Comparator.comparing(AutomationFieldCatalog.FieldOption::key))
                        .toList();
            }
        }
        this.fieldPickerOptions = filtered;
        this.fieldPickerList.setRows(fieldPickerRows(this.fieldPickerOptions));
        this.fieldPickerList.setSelectedIndex(fieldOptionIndex(this.fieldPickerOptions, normalizedCurrent));
    }

    private List<AutomationFieldCatalog.FieldOption> withCurrentCustomOption(List<AutomationFieldCatalog.FieldOption> options, String currentField) {
        if (currentField == null || currentField.isBlank()) {
            return options;
        }
        if (fieldOptionIndex(options, currentField) >= 0) {
            return options;
        }
        ArrayList<AutomationFieldCatalog.FieldOption> copy = new ArrayList<>(options.size() + 1);
        copy.add(new AutomationFieldCatalog.FieldOption(currentField, "Custom / Loaded Field", "This rule uses a field that is not in the current preset list."));
        copy.addAll(options);
        return List.copyOf(copy);
    }

    private int fieldOptionIndex(List<AutomationFieldCatalog.FieldOption> options, String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        for (int i = 0; i < options.size(); i++) {
            if (key.equals(options.get(i).key())) {
                return i;
            }
        }
        return 0;
    }

    private AutomationFieldCatalog.FieldOption selectedFieldPickerOption() {
        int index = this.fieldPickerList.selectedIndex();
        if (index < 0 || index >= this.fieldPickerOptions.size()) {
            return null;
        }
        return this.fieldPickerOptions.get(index);
    }

    private String currentFieldPickerValue() {
        return switch (this.fieldPickerMode) {
            case FILTER_FIELD -> {
                AutomationRule.EventFilter filter = selectedFilter();
                yield filter == null ? "" : filter.field;
            }
            case CONDITION_FIELD -> {
                AutomationRule.Condition condition = selectedCondition();
                yield condition == null ? "" : condition.field;
            }
            case NONE -> "";
        };
    }

    private boolean fieldPickerShowsSearch() {
        AutomationRule.Condition condition = selectedCondition();
        return this.fieldPickerMode == FieldPickerMode.CONDITION_FIELD
                && condition != null
                && condition.source == AutomationRule.ConditionSource.PLACEHOLDER;
    }

    private void closeFieldPicker() {
        this.fieldPickerOpen = false;
        this.fieldPickerMode = FieldPickerMode.NONE;
        this.fieldPickerSearchField.setFocused(false);
    }

    private static String normalizePlaceholderField(String field) {
        String value = safe(field).trim();
        if (value.startsWith("{") && value.endsWith("}") && value.length() > 2) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static String displayConditionField(AutomationRule.ConditionSource source, String field) {
        String normalized = normalizePlaceholderField(field);
        if (source == AutomationRule.ConditionSource.PLACEHOLDER) {
            return formatPlaceholderKey(normalized);
        }
        return normalized;
    }

    private static String formatPlaceholderKey(String key) {
        String normalized = normalizePlaceholderField(key);
        return normalized.isBlank() ? "{}" : "{" + normalized + "}";
    }

    private void applyFieldPickerSelection(int index) {
        if (index < 0 || index >= this.fieldPickerOptions.size()) {
            return;
        }
        AutomationFieldCatalog.FieldOption option = this.fieldPickerOptions.get(index);
        switch (this.fieldPickerMode) {
            case FILTER_FIELD -> {
                AutomationRule.EventFilter filter = selectedFilter();
                if (filter == null) {
                    return;
                }
                filter.field = option.key();
                markDirty();
                refreshFilterRows();
                syncFilterFields();
            }
            case CONDITION_FIELD -> {
                AutomationRule.Condition condition = selectedCondition();
                if (condition == null) {
                    return;
                }
                condition.field = option.key();
                markDirty();
                refreshConditionRows();
                syncConditionFields();
            }
            case NONE -> {
            }
        }
    }

    private String fieldPickerTitle() {
        return switch (this.fieldPickerMode) {
            case FILTER_FIELD -> "Select Filter Field";
            case CONDITION_FIELD -> "Select Condition Field";
            case NONE -> "Select Field";
        };
    }

    private String fieldPickerSubtitle() {
        AutomationRule rule = selectedRule();
        AutomationRule.Condition condition = selectedCondition();
        return switch (this.fieldPickerMode) {
            case FILTER_FIELD -> rule == null
                    ? "Pick one of the fields exposed by the selected event."
                    : "Event " + rule.eventType.name() + " exposes these filterable fields.";
            case CONDITION_FIELD -> rule == null || condition == null
                    ? "Pick one of the fields exposed by the selected source."
                    : condition.source == AutomationRule.ConditionSource.PLACEHOLDER
                    ? "Search and select a macro placeholder token for this condition."
                    : condition.source.name() + " exposes these fields for " + rule.eventType.name() + ".";
            case NONE -> "Pick a field.";
        };
    }

    private FilterPickerLayout filterPickerLayout() {
        int width = Math.min(FILTER_PICKER_W, this.owner.width - 40);
        int height = Math.min(FILTER_PICKER_H, this.owner.height - 40);
        int x = (this.owner.width - width) / 2;
        int y = (this.owner.height - height) / 2;
        return new FilterPickerLayout(x, y, width, height, x + 10, y + 40, width - 20, height - 50, x + width - 48);
    }

    private ConditionPickerLayout conditionPickerLayout() {
        int width = Math.min(CONDITION_PICKER_W, this.owner.width - 40);
        int height = Math.min(CONDITION_PICKER_H, this.owner.height - 40);
        int x = (this.owner.width - width) / 2;
        int y = (this.owner.height - height) / 2;
        return new ConditionPickerLayout(x, y, width, height, x + 10, y + 40, width - 20, height - 50, x + width - 48);
    }

    private FieldPickerLayout fieldPickerLayout() {
        int width = Math.min(FIELD_PICKER_W, this.owner.width - 40);
        int height = Math.min(FIELD_PICKER_H, this.owner.height - 40);
        int x = (this.owner.width - width) / 2;
        int y = (this.owner.height - height) / 2;
        return new FieldPickerLayout(x, y, width, height, x + 10, y + 40, width - 20, height - 64, y + height - 16, x + width - 48);
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static boolean shouldResetFilters(AutomationRule rule, AutomationEventType previousType) {
        if (rule.eventFilters == null || rule.eventFilters.isEmpty()) {
            return true;
        }
        if (rule.eventFilters.size() != 1) {
            return false;
        }
        return sameFilter(rule.eventFilters.getFirst(), defaultFilterFor(previousType));
    }

    private static boolean shouldResetActions(AutomationRule rule, AutomationEventType previousType) {
        if (rule.actions == null || rule.actions.isEmpty()) {
            return true;
        }
        if (rule.actions.size() != 1) {
            return false;
        }
        return sameAction(rule.actions.getFirst(), defaultActionFor(previousType));
    }

    private static boolean sameFilter(AutomationRule.EventFilter left, AutomationRule.EventFilter right) {
        return left != null
                && right != null
                && safe(left.field).equals(safe(right.field))
                && left.operator == right.operator
                && safe(left.value).equals(safe(right.value))
                && left.ignoreCase == right.ignoreCase;
    }

    private static boolean sameAction(AutomationRule.Action left, AutomationRule.Action right) {
        return left != null
                && right != null
                && left.type == right.type
                && safe(left.target).equals(safe(right.target))
                && safe(left.argument).equals(safe(right.argument))
                && left.enabled == right.enabled;
    }

    private String actionArgumentLabel() {
        AutomationRule.Action action = selectedAction();
        if (action == null) {
            return "Argument";
        }
        return switch (action.type) {
            case TOGGLE_MODULE -> "State (on/off, optional)";
            case RUN_SCRIPT -> "Argument (optional)";
            case RUN_MACRO -> "Modifier / Notes (optional)";
            case SEND_CLIENT_COMMAND -> "Unused / Notes (optional)";
        };
    }

    private String actionTargetHint() {
        AutomationRule.Action action = selectedAction();
        if (action == null) {
            return "Select an action to edit its target.";
        }
        return switch (action.type) {
            case RUN_MACRO -> "Target is a macro id from the Macros tab. Placeholders can build the id.";
            case RUN_SCRIPT -> "Target is a file in config/m0-dev-tools/scripts. Placeholders can change the file name.";
            case SEND_CLIENT_COMMAND -> "Target is a client command or chat message. Placeholders expand before sending.";
            case TOGGLE_MODULE -> "Target is a module id; argument can force on/off. Both can use placeholders.";
        };
    }

    private Layout layout() {
        int left = this.owner.contentLeft();
        int top = this.owner.contentTop();
        int right = this.owner.contentRight();
        int bottom = this.owner.contentBottom();
        boolean narrow = this.owner.contentWidth() < MASTER_DETAIL_MIN_W;

        int leftW = narrow ? this.owner.contentWidth() : Math.clamp(Math.round(this.owner.contentWidth() * 0.27f), 140, 220);
        int listButtonRows = leftW < 220 ? 2 : 1;
        int listY = top + listButtonRows * (BUTTON_H + Theme.GAP_S);
        int rightX = narrow ? left : left + leftW + GAP;
        int rightW = right - rightX - (narrow ? 0 : 6);
        int detailTop = top + BUTTON_H + Theme.GAP_S;

        int headerY = detailTop - this.detailScroll;
        int headerH = 118;
        int columnW = (rightW - GAP) / 2;
        int rightX2 = rightX + columnW + GAP;
        int fieldW = Math.max(20, columnW - 20);
        int numericW = Math.max(20, (rightW - 24 - (PANEL_GAP * 4)) / 5);
        int numberRowY = headerY + 86;
        int contentY = headerY + headerH + PANEL_GAP;
        int contentH = SECTION_H;
        return new Layout(narrow, left, top, leftW, listY, rightX, rightW, detailTop, bottom,
                headerY, headerH, columnW, rightX2, fieldW,
                numericW, numberRowY,
                rightX, contentY, contentH,
                rightX, contentY, contentH,
                rightX, contentY, contentH,
                rightX2, contentY, contentH);
    }

    private int maxDetailScroll(Layout layout) {
        int unscrolledBottom = layout.actionsY() + this.detailScroll + layout.actionsH();
        return Math.max(0, unscrolledBottom - layout.bottom);
    }

    /** Records each widget's group and unscrolled position; called once after all widgets are created. */
    private void classifyWidgets(Layout layout) {
        for (AbstractWidget widget : this.widgets) {
            WidgetGroup group;
            if (widget == this.newRuleButton || widget == this.duplicateRuleButton
                    || widget == this.deleteRuleButton || widget == this.reloadButton) {
                group = WidgetGroup.LIST;
            } else if (widget.getY() < layout.detailTop) {
                group = WidgetGroup.TABS;
            } else {
                group = WidgetGroup.DETAIL;
            }
            this.placedWidgets.add(new Placed(widget, group, widget.getX(), widget.getY()));
        }
    }

    /**
     * Applies scrolling and the narrow master/detail switch by moving widgets; widgets that are out of view are
     * parked off-screen so their visibility flags (owned by the section logic) stay untouched.
     */
    private void applyWidgetPlacement(Layout layout) {
        this.detailScroll = Math.clamp(this.detailScroll, 0, maxDetailScroll(layout));
        boolean showList = !layout.narrow || !this.narrowShowDetail;
        boolean showDetail = !layout.narrow || this.narrowShowDetail;
        for (Placed placed : this.placedWidgets) {
            AbstractWidget widget = placed.widget();
            boolean shown = placed.group() == WidgetGroup.LIST ? showList : showDetail;
            int y = placed.group() == WidgetGroup.DETAIL ? placed.baseY() - this.detailScroll : placed.baseY();
            boolean inView = placed.group() != WidgetGroup.DETAIL
                    || (y >= layout.detailTop && y + widget.getHeight() <= layout.bottom);
            if (shown && inView) {
                widget.setX(placed.baseX());
                widget.setY(y);
            } else {
                widget.setX(PARKED);
                widget.setY(PARKED);
            }
        }
    }

    private record Layout(boolean narrow,
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

    private record FilterPickerLayout(int x,
                                      int y,
                                      int width,
                                      int height,
                                      int listX,
                                      int listY,
                                      int listW,
                                      int listH,
                                      int closeX) {
    }

    private record ConditionPickerLayout(int x,
                                         int y,
                                         int width,
                                         int height,
                                         int listX,
                                         int listY,
                                         int listW,
                                         int listH,
                                         int closeX) {
    }

    private record FieldPickerLayout(int x,
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

    private enum RuleIntField {
        PRIORITY,
        RATE_COUNT
    }

    private enum RuleLongField {
        COOLDOWN,
        DEBOUNCE,
        RATE_WINDOW
    }

    private enum EditorSection {
        OVERVIEW,
        FILTERS,
        CONDITIONS,
        ACTIONS
    }

    private enum FieldPickerMode {
        NONE,
        FILTER_FIELD,
        CONDITION_FIELD
    }
}
