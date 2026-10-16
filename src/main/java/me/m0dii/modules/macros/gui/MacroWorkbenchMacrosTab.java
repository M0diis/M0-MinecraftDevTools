package me.m0dii.modules.macros.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.local.*;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.utils.ModConfig;
import me.m0dii.utils.StringUtils;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedWidgets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.*;

final class MacroWorkbenchMacrosTab {
    private static final int GROUP_H = 30;
    private static final int LABEL_OFFSET = 10;

    /** Positions shared by widget creation and rendering, derived from the workbench content area. */
    private record Layout(int listX, int listY, int listW, int listH, int buttonsY,
                          int cardX, int cardY, int cardW, int cardH,
                          int fieldX, int fieldW, int halfW,
                          int nameY, int keysY, int delayY, int commandsY, int footerY) {
    }

    private final MacroWorkbenchScreen owner;
    private final List<AbstractWidget> widgets;
    private final UiSelectionList macroList = new UiSelectionList(0, 0, 1, 1);
    private final List<MacroDraft> drafts = new ArrayList<>();

    private UiBoundTextField nameField;
    private UiBoundTextField commandsField;
    private UiBoundIntField delayField;
    private UiToggleButton hudOverlayToggle;
    private UiToggleButton showInOverlayToggle;
    private UiKeyCaptureButton keyButton;
    private UiKeyCaptureButton modifierButton;
    private Button createButton;
    private Button deleteButton;
    private Button editCommandsButton;
    private Button placeholdersButton;

    private int selectedIndex = -1;
    private boolean dirty = false;
    private boolean dirtyHudVisibility = false;
    private boolean overlayHudVisible = ModConfig.showMacroKeybindOverlay;

    MacroWorkbenchMacrosTab(MacroWorkbenchScreen owner, List<AbstractWidget> widgets) {
        this.owner = owner;
        this.widgets = widgets;
    }

    void initWidgets() {
        Layout l = layout();
        int newW = (l.listW() - Theme.GAP_S) / 2;
        this.createButton = register(ThemedWidgets.primary(Button.builder(Component.literal("+ New"), button -> createMacro())
                .bounds(l.listX(), l.buttonsY(), newW, Theme.CONTROL_H)
                .build()));
        this.deleteButton = register(ThemedWidgets.danger(Button.builder(Component.literal("Delete"), button -> deleteSelectedMacro())
                .bounds(l.listX() + newW + Theme.GAP_S, l.buttonsY(), l.listW() - newW - Theme.GAP_S, Theme.CONTROL_H)
                .build()));

        int footerHalf = l.halfW();
        this.hudOverlayToggle = new UiToggleButton("Keybind HUD", this.overlayHudVisible, l.fieldX(), l.footerY(), footerHalf, Theme.CONTROL_H)
                .setLabels("ON", "OFF")
                .setSaveConsumer(value -> {
                    this.overlayHudVisible = value;
                    this.dirtyHudVisibility = true;
                });
        this.placeholdersButton = register(ThemedWidgets.ghost(Button.builder(Component.literal("Placeholders →"), button -> this.owner.openWorkbenchTab(MacroWorkbenchScreen.Tab.PLACEHOLDERS))
                .bounds(l.fieldX() + l.fieldW() - footerHalf, l.footerY(), footerHalf, Theme.CONTROL_H)
                .build()));
        register(this.hudOverlayToggle.widget());

        int fieldX = l.fieldX();
        int fieldW = l.fieldW();
        int rowY = l.nameY();

        this.nameField = new UiBoundTextField(this.owner.workbenchTextRenderer(), fieldX, rowY, fieldW, Theme.CONTROL_H, "Macro name")
                .setMaxLength(96)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.name = value;
                    this.dirty = true;
                    refreshListRows();
                });
        register(this.nameField.widget());

        rowY = l.keysY();
        int halfW = l.halfW();
        this.keyButton = new UiKeyCaptureButton("Key", -1, fieldX, rowY, halfW, Theme.CONTROL_H)
                .setLabelFormatter(this::formatKeyValue)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.keyCode = value;
                    if (value < 0) {
                        draft.modifierKey = "";
                        this.modifierButton.setValue(-1);
                    }
                    this.dirty = true;
                    refreshListRows();
                    syncSelectionIntoWorkbench();
                });
        this.modifierButton = new UiKeyCaptureButton("Modifier", -1, fieldX + fieldW - halfW, rowY, halfW, Theme.CONTROL_H)
                .setLabelFormatter(this::formatModifierValue)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.modifierKey = value < 0 ? "" : translationKeyForCode(value);
                    this.dirty = true;
                    refreshListRows();
                });
        register(this.keyButton.widget());
        register(this.modifierButton.widget());

        rowY = l.delayY();
        this.delayField = new UiBoundIntField(this.owner.workbenchTextRenderer(), fieldX, rowY, halfW, Theme.CONTROL_H, "Delay ticks")
                .setMin(0)
                .setMax(1000)
                .setDefaultValue(0)
                .setMaxLength(6)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.delayTicks = value;
                    this.dirty = true;
                });
        this.showInOverlayToggle = new UiToggleButton("Show in HUD", false, fieldX + fieldW - halfW, rowY, halfW, Theme.CONTROL_H)
                .setLabels("ON", "OFF")
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.showInOverlay = value;
                    this.dirty = true;
                    refreshListRows();
                });
        register(this.delayField.widget());
        register(this.showInOverlayToggle.widget());

        rowY = l.commandsY();
        int editW = Math.min(60, fieldW / 3);
        this.commandsField = new UiBoundTextField(this.owner.workbenchTextRenderer(), fieldX, rowY, fieldW - editW - Theme.GAP_S, Theme.CONTROL_H, "Commands (; separated)")
                .setMaxLength(32767)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.commands = parseCommands(value);
                    this.dirty = true;
                });
        this.editCommandsButton = register(Button.builder(Component.literal("Edit…"), button -> openCommandsEditor())
                .bounds(fieldX + fieldW - editW, rowY, editW, Theme.CONTROL_H)
                .build());
        register(this.commandsField.widget());

        this.macroList.setEmptyText("No macros yet");
        reloadFromStore(this.owner.currentMacroSelectionId());
    }

    void reloadFromStore(String preferredId) {
        this.drafts.clear();
        List<Map.Entry<String, MacroDataHandler.MacroEntry>> entries = new ArrayList<>(MacroDataHandler.getAllMacros().entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<String, MacroDataHandler.MacroEntry> entry) -> displayName(entry.getKey(), entry.getValue()).toLowerCase(Locale.ROOT))
                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : entries) {
            this.drafts.add(MacroDraft.from(entry.getKey(), entry.getValue()));
        }

        this.overlayHudVisible = ModConfig.showMacroKeybindOverlay;
        this.hudOverlayToggle.setValue(this.overlayHudVisible);
        this.dirty = false;
        this.dirtyHudVisibility = false;

        refreshListRows();
        selectPreferred(preferredId);
        syncFieldsFromSelection();
    }

    private Layout layout() {
        int left = this.owner.contentLeft();
        int top = this.owner.contentTop();
        int right = this.owner.contentRight();
        int bottom = this.owner.contentBottom();
        int listW = Math.clamp(Math.round(this.owner.contentWidth() * 0.34f), 112, 220);
        int listY = top + Theme.CONTROL_H + Theme.GAP_S;
        int cardX = left + listW + Theme.GAP_L;
        int cardW = right - cardX;
        int pad = Theme.GAP_M;
        int fieldX = cardX + pad;
        int fieldW = cardW - pad * 2;
        int nameY = top + 26 + LABEL_OFFSET;
        return new Layout(left, listY, listW, bottom - listY, top,
                cardX, top, cardW, bottom - top,
                fieldX, fieldW, (fieldW - Theme.GAP_S) / 2,
                nameY, nameY + GROUP_H, nameY + GROUP_H * 2, nameY + GROUP_H * 3,
                bottom - pad - Theme.CONTROL_H);
    }

    void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Layout l = layout();
        Font font = this.owner.workbenchTextRenderer();
        this.macroList.setBounds(l.listX(), l.listY(), l.listW(), l.listH());
        this.macroList.extractRenderState(context, font, mouseX, mouseY);

        ThemeDraw.card(context, l.cardX(), l.cardY(), l.cardW(), l.cardH());
        int textX = l.fieldX();
        int textW = l.fieldW();
        int titleY = l.cardY() + Theme.GAP_M;
        ThemeDraw.hDivider(context, l.cardX() + 1, l.footerY() - Theme.GAP_S - 1, l.cardW() - 2);

        MacroDraft draft = selectedDraft();
        if (draft == null) {
            context.text(font, "No macro selected", textX, titleY, Theme.TEXT, false);
            context.text(font, ThemeDraw.fit(font, "Create one with + New, or pick one from the list.", textW), textX, titleY + 11, Theme.TEXT_MUTED, false);
            return;
        }

        context.text(font, ThemeDraw.fit(font, displayName(draft.id, draft), textW), textX, titleY, Theme.TEXT, false);
        context.text(font, ThemeDraw.fit(font, "ID " + draft.id, textW), textX, titleY + 11, Theme.TEXT_DIM, false);
        ThemeDraw.sectionLabel(context, font, "Name", textX, l.nameY() - LABEL_OFFSET, textW);
        ThemeDraw.sectionLabel(context, font, "Key", textX, l.keysY() - LABEL_OFFSET, l.halfW());
        ThemeDraw.sectionLabel(context, font, "Modifier", textX + textW - l.halfW(), l.keysY() - LABEL_OFFSET, l.halfW());
        ThemeDraw.sectionLabel(context, font, "Delay (ticks)", textX, l.delayY() - LABEL_OFFSET, l.halfW());
        ThemeDraw.sectionLabel(context, font, "Commands", textX, l.commandsY() - LABEL_OFFSET, textW);

        int hintY = l.commandsY() + Theme.CONTROL_H + Theme.GAP_S;
        if (hintY + 9 < l.footerY() - Theme.GAP_S) {
            context.text(font, ThemeDraw.fit(font, "Separate commands with ';'. Esc clears a key while capturing.", textW), textX, hintY, Theme.TEXT_DIM, false);
        }
    }

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!this.macroList.handleMouseClick(mouseX, mouseY)) {
            return false;
        }
        this.selectedIndex = this.macroList.selectedIndex();
        syncFieldsFromSelection();
        return true;
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        return this.macroList.handleMouseScroll(mouseX, mouseY, verticalAmount);
    }

    boolean handleKeyPressed(int keyCode) {
        if (this.keyButton != null && this.keyButton.handleKeyPressed(keyCode)) {
            return true;
        }
        if (this.modifierButton != null && this.modifierButton.handleKeyPressed(keyCode)) {
            return true;
        }
        return false;
    }

    void commitAll() {
        commitHudVisibility();
        if (!this.dirty) {
            return;
        }

        Map<String, MacroDataHandler.MacroEntry> next = new LinkedHashMap<>();
        for (MacroDraft draft : this.drafts) {
            String modifier = draft.keyCode < 0 ? "" : StringUtils.safe(draft.modifierKey).trim().toLowerCase(Locale.ROOT);
            MacroDataHandler.MacroEntry entry = MacroDataHandler.MacroEntry.builder()
                    .name(StringUtils.safe(draft.name).trim())
                    .commands(new ArrayList<>(sanitizeCommands(draft.commands)))
                    .keyCode(draft.keyCode)
                    .modifierKey(modifier)
                    .delayTicks(Math.clamp(draft.delayTicks, 0, 1000))
                    .showInOverlay(draft.showInOverlay)
                    .build();
            next.put(draft.id, entry);
        }

        MacroDataHandler.setAllMacros(next);
        CommandMacros.refreshKeybindings();
        this.owner.onMacrosChanged();
        this.dirty = false;
    }

    private void commitHudVisibility() {
        if (!this.dirtyHudVisibility) {
            return;
        }
        boolean next = this.overlayHudVisible;
        ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = next);
        this.dirtyHudVisibility = false;
    }

    private void createMacro() {
        MacroDraft draft = new MacroDraft();
        draft.id = UUID.randomUUID().toString().substring(0, 8);
        draft.name = "New Macro";
        draft.commands = new ArrayList<>(List.of("/"));
        draft.keyCode = -1;
        draft.modifierKey = "";
        draft.delayTicks = 0;
        draft.showInOverlay = false;
        this.drafts.add(draft);
        this.dirty = true;
        refreshListRows();
        this.selectedIndex = this.drafts.size() - 1;
        this.macroList.setSelectedIndex(this.selectedIndex);
        syncFieldsFromSelection();
    }

    private void deleteSelectedMacro() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.drafts.size()) {
            return;
        }
        this.drafts.remove(this.selectedIndex);
        this.dirty = true;
        if (this.drafts.isEmpty()) {
            this.selectedIndex = -1;
        } else {
            this.selectedIndex = Math.min(this.selectedIndex, this.drafts.size() - 1);
        }
        refreshListRows();
        this.macroList.setSelectedIndex(this.selectedIndex);
        syncFieldsFromSelection();
    }

    private void openCommandsEditor() {
        MacroDraft draft = selectedDraft();
        if (draft == null) {
            return;
        }
        this.owner.openCommandListEditor(draft.commands, commands -> {
            draft.commands = new ArrayList<>(sanitizeCommands(commands));
            this.commandsField.setText(joinCommands(draft.commands));
            this.dirty = true;
        });
    }

    private void refreshListRows() {
        List<String> rows = new ArrayList<>(this.drafts.size());
        for (MacroDraft draft : this.drafts) {
            String key = draft.keyCode < 0 ? "None" : MacroWorkbenchCanvasUtils.keyLabel(draft.keyCode);
            String modifier = StringUtils.safe(draft.modifierKey).isBlank()
                    ? ""
                    : MacroWorkbenchCanvasUtils.keyTranslationLabel(draft.modifierKey) + " + ";
            rows.add(displayName(draft.id, draft) + " [" + modifier + key + "]");
        }
        this.macroList.setRows(rows);
        this.macroList.setSelectedIndex(this.selectedIndex);
    }

    private void selectPreferred(String preferredId) {
        if (this.drafts.isEmpty()) {
            this.selectedIndex = -1;
            this.macroList.setSelectedIndex(-1);
            this.owner.updateKeyboardMacroSelection(null, -1);
            return;
        }

        int match = -1;
        if (preferredId != null && !preferredId.isBlank()) {
            for (int i = 0; i < this.drafts.size(); i++) {
                if (preferredId.equals(this.drafts.get(i).id)) {
                    match = i;
                    break;
                }
            }
        }

        this.selectedIndex = Math.max(match, 0);
        this.macroList.setSelectedIndex(this.selectedIndex);
        syncSelectionIntoWorkbench();
    }

    private void syncFieldsFromSelection() {
        MacroDraft draft = selectedDraft();
        this.keyButton.cancelCapture();
        this.modifierButton.cancelCapture();

        boolean active = draft != null;
        this.nameField.setEditable(active);
        this.commandsField.setEditable(active);
        this.delayField.setEditable(active);
        this.keyButton.widget().active = active;
        this.modifierButton.widget().active = active;
        this.showInOverlayToggle.widget().active = active;
        this.editCommandsButton.active = active;
        this.deleteButton.active = active;
        // With nothing selected, show a clean empty state instead of a stack of blank inputs.
        for (AbstractWidget editorWidget : List.of(this.nameField.widget(), this.commandsField.widget(), this.delayField.widget(),
                this.keyButton.widget(), this.modifierButton.widget(), this.showInOverlayToggle.widget(), this.editCommandsButton)) {
            editorWidget.visible = active;
        }

        if (!active) {
            this.nameField.setText("");
            this.commandsField.setText("");
            this.delayField.widget().setValue("");
            this.keyButton.setValue(-1);
            this.modifierButton.setValue(-1);
            this.showInOverlayToggle.setValue(false);
            this.owner.updateKeyboardMacroSelection(null, -1);
            return;
        }

        this.nameField.setText(StringUtils.safe(draft.name));
        this.commandsField.setText(joinCommands(draft.commands));
        this.delayField.setValue(draft.delayTicks);
        this.keyButton.setValue(draft.keyCode);
        this.modifierButton.setValue(translationToCode(draft.modifierKey));
        this.showInOverlayToggle.setValue(draft.showInOverlay);
        syncSelectionIntoWorkbench();
    }

    private void syncSelectionIntoWorkbench() {
        MacroDraft draft = selectedDraft();
        this.owner.updateKeyboardMacroSelection(draft == null ? null : draft.id, draft == null ? -1 : draft.keyCode);
    }

    private MacroDraft selectedDraft() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.drafts.size()) {
            return null;
        }
        return this.drafts.get(this.selectedIndex);
    }

    private String formatKeyValue(int value) {
        return value < 0 ? "None" : MacroWorkbenchCanvasUtils.keyLabel(value);
    }

    private String formatModifierValue(int value) {
        return value < 0 ? "None" : MacroWorkbenchCanvasUtils.keyTranslationLabel(translationKeyForCode(value));
    }

    private static String translationKeyForCode(int keyCode) {
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getName().toLowerCase(Locale.ROOT);
    }

    private static int translationToCode(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return -1;
        }
        InputConstants.Key key = InputConstants.getKey(translationKey.toLowerCase(Locale.ROOT));
        return key == null ? -1 : key.getValue();
    }

    private static String displayName(String id, MacroDataHandler.MacroEntry entry) {
        return entry == null ? id : displayName(id, entry.name);
    }

    private static String displayName(String id, MacroDraft draft) {
        return displayName(id, draft == null ? "" : draft.name);
    }

    private static String displayName(String id, String name) {
        String trimmed = StringUtils.safe(name).trim();
        return trimmed.isEmpty() ? id : trimmed;
    }

    private static List<String> parseCommands(String raw) {
        List<String> commands = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return commands;
        }
        for (String part : raw.split("[;\\n]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
        return commands;
    }

    private static List<String> sanitizeCommands(List<String> commands) {
        List<String> sanitized = new ArrayList<>();
        if (commands == null) {
            return sanitized;
        }
        for (String command : commands) {
            String trimmed = StringUtils.safe(command).trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    private static String joinCommands(List<String> commands) {
        return String.join(";", sanitizeCommands(commands));
    }

    private <T extends AbstractWidget> T register(T widget) {
        this.widgets.add(widget);
        this.owner.addRenderableWidget(widget);
        return widget;
    }

    private static final class MacroDraft {
        private String id;
        private String name;
        private List<String> commands = new ArrayList<>();
        private int keyCode = -1;
        private String modifierKey = "";
        private int delayTicks = 0;
        private boolean showInOverlay = false;

        private static MacroDraft from(String id, MacroDataHandler.MacroEntry entry) {
            MacroDraft draft = new MacroDraft();
            draft.id = id;
            draft.name = entry == null ? "" : StringUtils.safe(entry.name);
            draft.commands = entry == null ? new ArrayList<>() : new ArrayList<>(sanitizeCommands(entry.commands));
            draft.keyCode = entry == null ? -1 : entry.keyCode;
            draft.modifierKey = entry == null ? "" : StringUtils.safe(entry.modifierKey);
            draft.delayTicks = entry == null ? 0 : Math.max(0, entry.delayTicks);
            draft.showInOverlay = entry != null && entry.showInOverlay;
            return draft;
        }
    }
}
