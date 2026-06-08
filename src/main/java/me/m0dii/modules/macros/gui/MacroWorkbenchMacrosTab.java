package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.*;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.utils.ModConfig;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.*;

final class MacroWorkbenchMacrosTab {
    private static final int TOP_BAR_H = 54;

    private final MacroWorkbenchScreen owner;
    private final List<ClickableWidget> widgets;
    private final UiSelectionList macroList = new UiSelectionList(0, 0, 1, 1);
    private final List<MacroDraft> drafts = new ArrayList<>();

    private UiBoundTextField nameField;
    private UiBoundTextField commandsField;
    private UiBoundIntField delayField;
    private UiToggleButton hudOverlayToggle;
    private UiToggleButton showInOverlayToggle;
    private UiKeyCaptureButton keyButton;
    private UiKeyCaptureButton modifierButton;
    private ButtonWidget createButton;
    private ButtonWidget deleteButton;
    private ButtonWidget saveButton;
    private ButtonWidget editCommandsButton;
    private ButtonWidget placeholdersButton;

    private int selectedIndex = -1;
    private boolean dirty = false;
    private boolean dirtyHudVisibility = false;
    private boolean overlayHudVisible = ModConfig.showMacroKeybindOverlay;

    MacroWorkbenchMacrosTab(MacroWorkbenchScreen owner, List<ClickableWidget> widgets) {
        this.owner = owner;
        this.widgets = widgets;
    }

    void initWidgets() {
        int leftX = 12;
        int topY = TOP_BAR_H + 18;
        int leftW = Math.max(220, this.owner.width / 3);
        int rightX = leftX + leftW + 12;
        int rightW = Math.max(220, this.owner.width - rightX - 12);
        int buttonY = topY;

        this.createButton = register(ButtonWidget.builder(Text.literal("+ New"), button -> createMacro())
                .dimensions(leftX, buttonY, 72, 20)
                .build());
        this.deleteButton = register(ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelectedMacro())
                .dimensions(leftX + 76, buttonY, 72, 20)
                .build());

        int rightTopY = topY;
        this.hudOverlayToggle = new UiToggleButton("HUD Overlay", this.overlayHudVisible, rightX, rightTopY, 140, 20)
                .setLabels("ON", "OFF")
                .setSaveConsumer(value -> {
                    this.overlayHudVisible = value;
                    this.dirtyHudVisibility = true;
                });
        this.placeholdersButton = register(ButtonWidget.builder(Text.literal("Placeholders"), button -> this.owner.openWorkbenchTab(MacroWorkbenchScreen.Tab.PLACEHOLDERS))
                .dimensions(rightX + 144, rightTopY, 96, 20)
                .build());
        this.saveButton = register(ButtonWidget.builder(Text.literal("Save Macros"), button -> commitAll())
                .dimensions(rightX + Math.max(0, rightW - 96), rightTopY, 96, 20)
                .build());
        register(this.hudOverlayToggle.widget());

        int fieldX = rightX + 12;
        int fieldW = Math.max(160, rightW - 24);
        int rowY = topY + 92;

        this.nameField = new UiBoundTextField(this.owner.workbenchTextRenderer(), fieldX, rowY, fieldW, 18, "Macro name")
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

        rowY += 32;
        int halfW = (fieldW - 4) / 2;
        this.keyButton = new UiKeyCaptureButton("Key", -1, fieldX, rowY, halfW, 20)
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
        this.modifierButton = new UiKeyCaptureButton("Modifier", -1, fieldX + halfW + 4, rowY, halfW, 20)
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

        rowY += 32;
        this.delayField = new UiBoundIntField(this.owner.workbenchTextRenderer(), fieldX, rowY, halfW, 18, "Delay ticks")
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
        this.showInOverlayToggle = new UiToggleButton("Show In HUD", false, fieldX + halfW + 4, rowY, halfW, 20)
                .setLabels("YES", "NO")
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

        rowY += 32;
        this.commandsField = new UiBoundTextField(this.owner.workbenchTextRenderer(), fieldX, rowY, Math.max(80, fieldW - 88), 18, "Commands (; separated)")
                .setMaxLength(32767)
                .setSaveConsumer(value -> {
                    MacroDraft draft = selectedDraft();
                    if (draft == null) {
                        return;
                    }
                    draft.commands = parseCommands(value);
                    this.dirty = true;
                });
        this.editCommandsButton = register(ButtonWidget.builder(Text.literal("Edit Cmds"), button -> openCommandsEditor())
                .dimensions(fieldX + Math.max(80, fieldW - 84), rowY, 84, 18)
                .build());
        register(this.commandsField.widget());

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

    void render(DrawContext context, int mouseX, int mouseY) {
        int leftX = 12;
        int topY = TOP_BAR_H + 18;
        int leftW = Math.max(220, this.owner.width / 3);
        int listY = topY + 26;
        int listH = Math.max(120, this.owner.height - listY - 10);
        int rightX = leftX + leftW + 12;
        int rightW = Math.max(220, this.owner.width - rightX - 12);
        int rightY = topY + 26;
        int rightH = Math.max(140, this.owner.height - rightY - 10);
        int nameY = topY + 92;
        int keysY = nameY + 32;
        int delayY = keysY + 32;
        int commandsY = delayY + 32;

        this.macroList.setBounds(leftX, listY, leftW, listH);

        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Macros", leftX, TOP_BAR_H + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Manager", rightX, TOP_BAR_H + 6, 0xFFFFFFFF);

        this.macroList.render(context, this.owner.workbenchTextRenderer(), mouseX, mouseY);
        UiForms.drawPanel(context, rightX, rightY, rightW, rightH);

        MacroDraft draft = selectedDraft();
        int infoX = rightX + 12;
        int infoY = rightY + 8;
        if (draft == null) {
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Use Esc/Backspace/Delete while capturing to clear a key.", infoX, infoY, 0xFF9FCFCF);
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Create or select a macro to edit it.", infoX, infoY + 18, 0xFFB8B8B8);
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "The full placeholder reference is available in the Placeholders tab.", infoX, infoY + 32, 0xFF9FCFCF);
            return;
        }

        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Use Esc/Backspace/Delete while capturing to clear a key.", infoX, infoY, 0xFF9FCFCF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Selected: " + displayName(draft.id, draft), infoX, infoY + 18, 0xFFFFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "ID: " + draft.id, infoX, infoY + 30, 0xFFB8B8B8);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Name", infoX, nameY - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Key / Modifier", infoX, keysY - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Delay / Overlay", infoX, delayY - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Commands", infoX, commandsY - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Use ';' between commands or open the multiline editor.", infoX, commandsY + 22, 0xFF9FCFCF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Empty key means the macro is only callable from other UI surfaces.", infoX, commandsY + 34, 0xFF9FCFCF);
    }

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
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

        if (!active) {
            this.nameField.setText("");
            this.commandsField.setText("");
            this.delayField.widget().setText("");
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
        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getTranslationKey().toLowerCase(Locale.ROOT);
    }

    private static int translationToCode(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return -1;
        }
        InputUtil.Key key = InputUtil.fromTranslationKey(translationKey.toLowerCase(Locale.ROOT));
        return key == null ? -1 : key.getCode();
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

    private <T extends ClickableWidget> T register(T widget) {
        this.widgets.add(widget);
        this.owner.addDrawableChild(widget);
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
