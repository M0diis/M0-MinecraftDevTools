package me.m0dii.modules.itemdata;


import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiRect;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.modules.getdata.GetDataScreen;
import me.m0dii.utils.NbtEditorUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.IntConsumer;

public final class ItemDataScreen extends Screen implements ThemedScreen {
    private static final int LIST_ROW_HEIGHT = 12;
    private static final int DETAIL_ROW_HEIGHT = 10;
    private static final int DETAIL_WRAP = 60;

    private final Screen parent;
    private final ItemDataReference reference;
    private CompoundTag itemData;

    private UiRect summaryRect;
    private UiRect currentPanelRect;
    private UiRect currentListRect;
    private UiRect availablePanelRect;
    private UiRect availableListRect;
    private UiRect detailPanelRect;
    private UiRect detailListRect;

    private EditBox availableFilterField;
    private EditBox nameField;
    private EditBox countField;

    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private Button copyButton;
    private Button applyNameButton;
    private Button applyCountButton;

    private String selectedComponentId;
    private int presentScroll;
    private int availableScroll;
    private int detailScroll;
    private String statusMessage = "Choose a component on the left, or add one from the middle catalog.";
    private int statusColor = UiTheme.TEXT_MUTED;

    public ItemDataScreen(Screen parent, ItemDataReference reference, CompoundTag itemData) {
        super(Component.literal("Item Components"));
        this.parent = parent;
        this.reference = reference;
        this.itemData = itemData == null ? new CompoundTag() : itemData.copy();
        this.savedData = this.itemData.copy();
    }

    // ---- undo & review before saving -------------------------------------------------------------------------

    /** The item as last loaded from / saved to the server, for the "review changes" step. */
    private CompoundTag savedData;
    private final java.util.ArrayDeque<CompoundTag> undoStack = new java.util.ArrayDeque<>();
    private static final int UNDO_LIMIT = 50;
    /** Set after the first Save click: the detail panel shows the changes and Save must be pressed again. */
    private boolean reviewingSave;
    private Button saveButton;
    private Button undoButton;

    /** Call before every edit so it can be undone. */
    private void rememberForUndo() {
        undoStack.push(this.itemData.copy());
        while (undoStack.size() > UNDO_LIMIT) {
            undoStack.removeLast();
        }
        reviewingSave = false;
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            setStatus("Nothing to undo.", 0xFFFFD37A);
            return;
        }
        this.itemData = undoStack.pop();
        reviewingSave = false;
        refreshFieldsFromData();
        refreshSelection();
        updateButtons();
        setStatus("Undid the last change.", 0xFF70D070);
    }

    private List<String> pendingChanges() {
        return ItemDataDiff.diff(flatten(this.savedData), flatten(this.itemData));
    }

    private static java.util.Map<String, String> flatten(CompoundTag root) {
        java.util.Map<String, String> out = new java.util.TreeMap<>();
        if (root == null) {
            return out;
        }
        for (String key : root.keySet()) {
            if (!"components".equals(key)) {
                out.put(key, String.valueOf(root.get(key)));
            }
        }
        CompoundTag components = ItemDataCodec.getComponents(root);
        for (String key : components.keySet()) {
            out.put("components/" + key, String.valueOf(components.get(key)));
        }
        return out;
    }

    @Override
    protected void init() {
        int bottomButtonsY = this.height - 30;
        int mainTop = 98;
        int mainHeight = Math.max(140, bottomButtonsY - mainTop - 18);
        int totalWidth = this.width - 24;

        this.summaryRect = new UiRect(12, 12, totalWidth, 78);
        this.currentPanelRect = new UiRect(12, mainTop, 196, mainHeight);
        this.availablePanelRect = new UiRect(216, mainTop, 236, mainHeight);
        this.detailPanelRect = new UiRect(460, mainTop, Math.max(220, this.width - 472), mainHeight);
        this.currentListRect = this.currentPanelRect.inset(8, 20, 8, 8);
        this.availableListRect = this.availablePanelRect.inset(8, 44, 8, 8);
        this.detailListRect = this.detailPanelRect.inset(8, 78, 8, 8);

        this.availableFilterField = new EditBox(
                this.font,
                this.availablePanelRect.x() + 8,
                this.availablePanelRect.y() + 20,
                this.availablePanelRect.width() - 16,
                18,
                Component.literal("Search Components")
        );
        this.availableFilterField.setMaxLength(128);
        this.availableFilterField.setResponder(value -> {
            refreshSelection();
            updateAvailableSuggestion();
            updateButtons();
        });
        this.addRenderableWidget(this.availableFilterField);

        this.countField = new EditBox(
                this.font,
                this.summaryRect.x() + 300,
                this.summaryRect.y() + 32,
                56,
                18,
                Component.literal("Count")
        );
        this.countField.setMaxLength(16);
        this.addRenderableWidget(this.countField);

        this.nameField = new EditBox(
                this.font,
                this.summaryRect.x() + 460,
                this.summaryRect.y() + 32,
                Math.max(110, this.summaryRect.width() - 620),
                18,
                Component.literal("Display Name")
        );
        this.nameField.setMaxLength(256);
        this.addRenderableWidget(this.nameField);

        this.applyCountButton = this.addRenderableWidget(Button.builder(Component.literal("Apply Count"), b -> applyCount())
                .bounds(this.summaryRect.x() + 364, this.summaryRect.y() + 32, 84, 18)
                .build());
        this.applyNameButton = this.addRenderableWidget(Button.builder(Component.literal("Apply Name"), b -> applyPlainName())
                .bounds(this.summaryRect.right() - 100, this.summaryRect.y() + 32, 88, 18)
                .build());

        this.addButton = this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> addSelectedComponent())
                .bounds(12, bottomButtonsY, 58, 20)
                .build());
        this.editButton = this.addRenderableWidget(Button.builder(Component.literal("Edit"), b -> editSelectedComponent())
                .bounds(74, bottomButtonsY, 58, 20)
                .build());
        this.removeButton = this.addRenderableWidget(Button.builder(Component.literal("Remove"), b -> removeSelectedComponent())
                .bounds(136, bottomButtonsY, 68, 20)
                .build());
        this.copyButton = this.addRenderableWidget(Button.builder(Component.literal("Copy"), b -> copySelectedValue())
                .bounds(208, bottomButtonsY, 58, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("JSON Mode"), b -> openFullItemEditor())
                .bounds(this.width - 248, bottomButtonsY, 82, 20)
                .build());
        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveToServer())
                .bounds(this.width - 160, bottomButtonsY, 60, 20)
                .build());
        this.undoButton = this.addRenderableWidget(Button.builder(Component.literal("Undo"), b -> undo())
                .bounds(270, bottomButtonsY, 52, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 94, bottomButtonsY, 60, 20)
                .build());

        refreshFieldsFromData();
        refreshSelection();
        updateAvailableSuggestion();
        updateButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, UiTheme.BG);

        renderSummaryPanel(context);
        renderCurrentPanel(context);
        renderAvailablePanel(context);
        renderDetailPanel(context);
        context.text(this.font, this.statusMessage, 12, this.height - 44, this.statusColor);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (selectFromList(click, this.currentListRect, presentComponentIds(), true, doubled)) {
            return true;
        }
        return selectFromList(click, this.availableListRect, availableComponentIds(), false, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollList(mouseX, mouseY, verticalAmount, this.currentListRect, presentComponentIds().size(), visibleRows(this.currentListRect, LIST_ROW_HEIGHT), value -> this.presentScroll = value, this.presentScroll)) {
            return true;
        }
        if (scrollList(mouseX, mouseY, verticalAmount, this.availableListRect, availableComponentIds().size(), visibleRows(this.availableListRect, LIST_ROW_HEIGHT), value -> this.availableScroll = value, this.availableScroll)) {
            return true;
        }
        if (scrollList(mouseX, mouseY, verticalAmount, this.detailListRect, buildDetailLines().size(), visibleRows(this.detailListRect, DETAIL_ROW_HEIGHT), value -> this.detailScroll = value, this.detailScroll)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        boolean ctrl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
        boolean typing = (this.availableFilterField != null && this.availableFilterField.isFocused())
                || (this.nameField != null && this.nameField.isFocused())
                || (this.countField != null && this.countField.isFocused());
        if (ctrl && !typing && input.input() == InputConstants.KEY_Z) {
            undo();
            return true;
        }
        if (input.input() == InputConstants.KEY_TAB && this.availableFilterField != null && this.availableFilterField.isFocused()) {
            String suggestion = availableSuggestionSuffix();
            if (suggestion != null && !suggestion.isBlank()) {
                this.availableFilterField.setValue(this.availableFilterField.getValue() + suggestion);
                this.availableFilterField.moveCursorTo(this.availableFilterField.getValue().length(), false);
                updateAvailableSuggestion();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private void renderSummaryPanel(GuiGraphicsExtractor context) {
        FormPanel summaryPanel = new FormPanel(this.summaryRect);
        summaryPanel.extractRenderState(context);

        ItemStack previewStack = previewStack();
        if (!previewStack.isEmpty()) {
            context.item(previewStack, this.summaryRect.x() + 10, this.summaryRect.y() + 18);
        }

        String itemTitle = currentItemTitle();
        String itemId = this.itemData.getString("id").orElse("minecraft:air");
        context.text(this.font, itemTitle, this.summaryRect.x() + 40, this.summaryRect.y() + 10, UiTheme.TEXT);
        context.text(this.font, this.reference.slotLabel() + " | " + itemId, this.summaryRect.x() + 40, this.summaryRect.y() + 22, UiTheme.TEXT_MUTED);
        context.text(this.font, "Mode: Easy", this.summaryRect.right() - 76, this.summaryRect.y() + 10, UiTheme.TEXT_ACCENT);
        context.text(this.font, "Count", this.countField.getX(), this.countField.getY() - 10, UiTheme.TEXT_MUTED);
        context.text(this.font, "Rename (minecraft:custom_name, supports & codes and <#hex>)", this.nameField.getX(), this.nameField.getY() - 10, UiTheme.TEXT_MUTED);
        context.text(this.font,
                "Current components: " + presentComponentIds().size(),
                this.summaryRect.x() + 40,
                this.summaryRect.y() + 56,
                UiTheme.TEXT_MUTED);
    }

    private void renderCurrentPanel(GuiGraphicsExtractor context) {
        FormPanel panel = new FormPanel(this.currentPanelRect);
        panel.extractRenderState(context);
        panel.drawLabel(context, this.font, "On Item (" + presentComponentIds().size() + ")", 8, 8);

        List<String> rows = presentRows();
        new ListPanel(this.currentListRect).extractRenderState(
                context,
                this.font,
                rows,
                selectedIndex(presentComponentIds()),
                this.presentScroll,
                LIST_ROW_HEIGHT
        );
    }

    private void renderAvailablePanel(GuiGraphicsExtractor context) {
        FormPanel panel = new FormPanel(this.availablePanelRect);
        panel.extractRenderState(context);
        panel.drawLabel(context, this.font, "Available Components", 8, 8);

        List<String> rows = availableRows();
        new ListPanel(this.availableListRect).extractRenderState(
                context,
                this.font,
                rows,
                selectedIndex(availableComponentIds()),
                this.availableScroll,
                LIST_ROW_HEIGHT
        );
    }

    private void renderDetailPanel(GuiGraphicsExtractor context) {
        FormPanel panel = new FormPanel(this.detailPanelRect);
        panel.extractRenderState(context);
        panel.drawLabel(context, this.font, "Component Details", 8, 8);

        if (this.selectedComponentId == null) {
            context.text(this.font, "No component selected.", this.detailPanelRect.x() + 8, this.detailPanelRect.y() + 24, UiTheme.TEXT_MUTED);
        } else {
            boolean present = isComponentPresent(this.selectedComponentId);
            context.text(this.font,
                    ItemComponentMetadata.label(this.selectedComponentId),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 24,
                    UiTheme.TEXT_ACCENT);
            context.text(this.font,
                    this.selectedComponentId,
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 36,
                    UiTheme.TEXT_MUTED);
            context.text(this.font,
                    "Type: " + ItemComponentMetadata.typeHint(this.selectedComponentId),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 48,
                    UiTheme.TEXT_MUTED);
            context.text(this.font,
                    "State: " + (present ? "Present on item" : "Missing from item"),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 60,
                    present ? 0xFF70D070 : 0xFFFFD37A);
        }

        new ListPanel(this.detailListRect).extractRenderState(
                context,
                this.font,
                buildDetailLines(),
                -1,
                this.detailScroll,
                DETAIL_ROW_HEIGHT
        );
    }

    private boolean selectFromList(MouseButtonEvent click, UiRect rect, List<String> ids, boolean presentList, boolean doubled) {
        if (!rect.contains(click.x(), click.y())) {
            return false;
        }
        int scroll = presentList ? this.presentScroll : this.availableScroll;
        int relativeY = (int) click.y() - rect.y() - 4;
        int clickedIndex = scroll + Math.max(0, relativeY / LIST_ROW_HEIGHT);
        if (clickedIndex < 0 || clickedIndex >= ids.size()) {
            return false;
        }

        this.selectedComponentId = ids.get(clickedIndex);
        this.detailScroll = 0;
        setStatus((presentList ? "Selected existing " : "Selected available ") + this.selectedComponentId + ".", UiTheme.TEXT_MUTED);
        updateButtons();

        if (doubled) {
            if (presentList) {
                editSelectedComponent();
            } else {
                addSelectedComponent();
            }
        }
        return true;
    }

    private boolean scrollList(double mouseX,
                               double mouseY,
                               double verticalAmount,
                               UiRect rect,
                               int totalRows,
                               int visibleRows,
                               IntConsumer setter,
                               int currentValue) {
        if (!rect.contains(mouseX, mouseY)) {
            return false;
        }
        int delta = verticalAmount > 0 ? -1 : 1;
        int maxScroll = Math.max(0, totalRows - visibleRows);
        setter.accept(Math.clamp(currentValue + delta, 0, maxScroll));
        return true;
    }

    private void addSelectedComponent() {
        if (this.selectedComponentId == null) {
            setStatus("Select a component to add.", 0xFFFF9090);
            return;
        }
        if (isComponentPresent(this.selectedComponentId)) {
            setStatus("That component is already on the item. Use Edit instead.", 0xFFFFD37A);
            return;
        }
        openSelectedComponentEditor(false);
    }

    private void editSelectedComponent() {
        if (this.selectedComponentId == null) {
            setStatus("Select a component to edit.", 0xFFFF9090);
            return;
        }
        if (!isComponentPresent(this.selectedComponentId)) {
            setStatus("That component is not on the item yet. Use Add instead.", 0xFFFFD37A);
            return;
        }
        openSelectedComponentEditor(true);
    }

    private void openSelectedComponentEditor(boolean present) {
        if (this.minecraft == null || this.selectedComponentId == null) {
            return;
        }
        HolderLookup.Provider registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }

        String componentId = this.selectedComponentId;
        Tag currentValue = selectedComponentValue();
        String initialValue = present && currentValue != null
                ? currentValue.toString()
                : ItemDataCodec.defaultTemplateForComponent(componentId, registryLookup);
        String helperText = buildComponentEditorHelperText(componentId, present);

        this.minecraft.gui.setScreen(GetDataScreen.createStandalone(
                this,
                Component.literal(present ? "Edit Component" : "Add Component"),
                ItemComponentMetadata.label(componentId) + " (" + componentId + ")",
                initialValue,
                helperText,
                false,
                true,
                (screen, raw, path) -> {
                    Tag normalized = ItemDataCodec.normalizeComponentValue(componentId, raw, registryLookup);
                    rememberForUndo();
                    ItemDataCodec.ensureComponents(this.itemData).put(componentId, normalized);
                    this.selectedComponentId = componentId;
                    refreshFieldsFromData();
                    refreshSelection();
                    updateButtons();
                    setStatus((present ? "Updated " : "Added ") + componentId + ".", 0xFF70D070);
                },
                (screen, raw, path) -> ItemDataCodec.normalizeComponentValue(componentId, raw, registryLookup),
                null
        ));
    }

    private void openFullItemEditor() {
        if (this.minecraft == null) {
            return;
        }
        HolderLookup.Provider registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }

        this.minecraft.gui.setScreen(GetDataScreen.createStandalone(
                this,
                Component.literal("Item JSON Mode"),
                currentItemTitle(),
                this.itemData.toString(),
                "Edit the full serialized item root. Component ids under `components` support Tab/click completion.",
                true,
                true,
                (screen, raw, path) -> {
                    CompoundTag replacement = ItemDataCodec.normalizeItemData(raw, registryLookup);
                    rememberForUndo();
                    this.itemData = replacement;
                    refreshFieldsFromData();
                    refreshSelection();
                    updateButtons();
                    setStatus("Updated the full item draft from JSON mode.", 0xFF70D070);
                },
                ItemComponentCatalog.jsonSuggestionProvider()
        ));
    }

    private void applyPlainName() {
        HolderLookup.Provider registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }
        try {
            CompoundTag before = this.itemData.copy();
            ItemDataCodec.applyPlainCustomName(this.itemData, this.nameField == null ? "" : this.nameField.getValue(), registryLookup);
            pushUndoIfChanged(before);
            refreshFieldsFromData();
            refreshSelection();
            updateButtons();
            setStatus("Updated minecraft:custom_name.", 0xFF70D070);
        } catch (CommandSyntaxException exception) {
            setStatus(exception.getRawMessage().getString(), 0xFFFF9090);
        }
    }

    private void applyCount() {
        try {
            CompoundTag before = this.itemData.copy();
            ItemDataCodec.applyCount(this.itemData, this.countField == null ? "" : this.countField.getValue());
            pushUndoIfChanged(before);
            refreshFieldsFromData();
            setStatus("Updated item count.", 0xFF70D070);
        } catch (CommandSyntaxException exception) {
            setStatus(exception.getRawMessage().getString(), 0xFFFF9090);
        }
    }

    private void removeSelectedComponent() {
        if (this.selectedComponentId == null) {
            setStatus("Select a component to remove.", 0xFFFF9090);
            return;
        }
        if (!isComponentPresent(this.selectedComponentId)) {
            setStatus("That component is not on the item.", 0xFFFF9090);
            return;
        }

        rememberForUndo();
        CompoundTag components = ItemDataCodec.getComponents(this.itemData);
        components.remove(this.selectedComponentId);
        if (components.isEmpty()) {
            this.itemData.remove("components");
        }
        setStatus("Removed " + this.selectedComponentId + ".", 0xFF70D070);
        refreshFieldsFromData();
        refreshSelection();
        updateButtons();
    }

    private void copySelectedValue() {
        if (this.minecraft == null || this.selectedComponentId == null || !isComponentPresent(this.selectedComponentId)) {
            setStatus("Select an existing component to copy.", 0xFFFF9090);
            return;
        }
        Tag value = selectedComponentValue();
        if (value == null) {
            setStatus("That component does not have a readable value.", 0xFFFF9090);
            return;
        }
        this.minecraft.keyboardHandler.setClipboard(value.toString());
        setStatus("Copied " + this.selectedComponentId + " to the clipboard.", 0xFF70D070);
    }

    private void pushUndoIfChanged(CompoundTag before) {
        if (!before.equals(this.itemData)) {
            undoStack.push(before);
            while (undoStack.size() > UNDO_LIMIT) {
                undoStack.removeLast();
            }
            reviewingSave = false;
        }
    }

    /** First click shows what will change; the second click sends it. */
    private void saveToServer() {
        List<String> changes = pendingChanges();
        if (changes.isEmpty()) {
            reviewingSave = false;
            setStatus("Nothing changed since the last save.", 0xFFFFD37A);
            return;
        }
        if (!reviewingSave) {
            reviewingSave = true;
            this.detailScroll = 0;
            setStatus(changes.size() + " change(s) listed on the right. Press Save again to send, or edit/Undo to cancel.", 0xFFFFD37A);
            updateButtons();
            return;
        }
        reviewingSave = false;
        boolean sent = ItemDataSyncClient.saveItemData(this.reference.slotIndex(), this.itemData);
        if (sent) {
            this.savedData = this.itemData.copy();
        }
        updateButtons();
        setStatus(
                sent ? "Sent the updated item data to the server for " + this.reference.slotLabel() + "." : "Unable to send the item data to the server.",
                sent ? 0xFF70D070 : 0xFFFF9090
        );
    }

    private void refreshFieldsFromData() {
        if (this.nameField != null) {
            this.nameField.setValue(ItemDataCodec.readPlainDisplayName(this.itemData, registryLookup()));
        }
        if (this.countField != null) {
            this.countField.setValue(String.valueOf(ItemDataCodec.readCount(this.itemData)));
        }
    }

    private void refreshSelection() {
        List<String> present = presentComponentIds();
        List<String> available = availableComponentIds();
        if (this.selectedComponentId == null || (!present.contains(this.selectedComponentId) && !available.contains(this.selectedComponentId))) {
            if (!present.isEmpty()) {
                this.selectedComponentId = present.getFirst();
            } else if (!available.isEmpty()) {
                this.selectedComponentId = available.getFirst();
            } else {
                this.selectedComponentId = null;
            }
            this.detailScroll = 0;
        }

        this.presentScroll = clampScroll(this.presentScroll, present.size(), visibleRows(this.currentListRect, LIST_ROW_HEIGHT));
        this.availableScroll = clampScroll(this.availableScroll, available.size(), visibleRows(this.availableListRect, LIST_ROW_HEIGHT));
        this.detailScroll = clampScroll(this.detailScroll, buildDetailLines().size(), visibleRows(this.detailListRect, DETAIL_ROW_HEIGHT));
        ensureVisible(present, true);
        ensureVisible(available, false);
    }

    private void ensureVisible(List<String> ids, boolean presentList) {
        if (this.selectedComponentId == null || ids.isEmpty()) {
            return;
        }
        int index = ids.indexOf(this.selectedComponentId);
        if (index < 0) {
            return;
        }
        int visibleRows = visibleRows(presentList ? this.currentListRect : this.availableListRect, LIST_ROW_HEIGHT);
        int scroll = presentList ? this.presentScroll : this.availableScroll;
        if (index < scroll) {
            scroll = index;
        } else if (index >= scroll + visibleRows) {
            scroll = Math.max(0, index - visibleRows + 1);
        }
        if (presentList) {
            this.presentScroll = scroll;
        } else {
            this.availableScroll = scroll;
        }
    }

    private List<String> presentComponentIds() {
        return ItemDataCodec.getComponents(this.itemData).keySet().stream()
                .sorted(Comparator
                        .comparingInt(ItemComponentMetadata::priority)
                        .thenComparing(ItemComponentMetadata::label))
                .toList();
    }

    private List<String> availableComponentIds() {
        Set<String> present = presentComponentIdSet();
        return ItemComponentCatalog.filterComponentIds(this.availableFilterField == null ? "" : this.availableFilterField.getValue(), present).stream()
                .filter(id -> !present.contains(id))
                .toList();
    }

    private Set<String> presentComponentIdSet() {
        return ItemDataCodec.getComponents(this.itemData).keySet();
    }

    private List<String> presentRows() {
        List<String> ids = presentComponentIds();
        if (ids.isEmpty()) {
            return List.of("No components on this item.");
        }
        List<String> rows = new ArrayList<>(ids.size());
        for (String id : ids) {
            rows.add(ItemComponentMetadata.label(id));
        }
        return rows;
    }

    private List<String> availableRows() {
        List<String> ids = availableComponentIds();
        if (ids.isEmpty()) {
            return List.of("No more components match this search.");
        }
        List<String> rows = new ArrayList<>(ids.size());
        for (String id : ids) {
            rows.add("+ " + ItemComponentMetadata.label(id));
        }
        return rows;
    }

    private List<String> buildDetailLines() {
        if (reviewingSave) {
            List<String> review = new ArrayList<>();
            review.add("Changes to send (+ added, - removed, ~ changed):");
            review.addAll(pendingChanges());
            return review;
        }
        return buildComponentDetailLines();
    }

    private List<String> buildComponentDetailLines() {
        if (this.selectedComponentId == null) {
            return List.of(
                    "Pick a component from the left if it already exists on the item.",
                    "Pick a component from the middle to add a missing one.",
                    "Double-click shortcuts:",
                    "- current component -> Edit",
                    "- available component -> Add",
                    "JSON mode remains available for full raw edits."
            );
        }

        HolderLookup.Provider registryLookup = registryLookup();
        boolean present = isComponentPresent(this.selectedComponentId);
        List<String> lines = new ArrayList<>();
        appendWrapped(lines, "Desc: ", ItemComponentMetadata.description(this.selectedComponentId));
        lines.add(" ");

        if (present) {
            lines.add("Current Value:");
            appendPretty(lines, String.valueOf(selectedComponentValue()));
            lines.add(" ");
        }

        lines.add("Starter Template:");
        appendPretty(lines, ItemDataCodec.defaultTemplateForComponent(this.selectedComponentId, registryLookup));
        lines.add(" ");
        lines.add("Example:");
        appendPretty(lines, ItemDataCodec.exampleForComponent(this.selectedComponentId, registryLookup));
        lines.add(" ");
            appendWrapped(lines, "Hint: ", present
                ? "Edit opens a focused SNBT editor for this component. Copy puts the raw value on your clipboard."
                : "Add opens the same editor with the starter template prefilled, then validates it against the real component codec.");
        return lines;
    }

    private void appendPretty(List<String> lines, String raw) {
        String[] split = prettySnbt(raw).split("\\n", -1);
        for (String line : split) {
            lines.add(line.isEmpty() ? " " : line);
        }
    }

    private void updateButtons() {
        boolean hasSelection = this.selectedComponentId != null;
        boolean present = hasSelection && isComponentPresent(this.selectedComponentId);
        if (this.addButton != null) {
            this.addButton.active = hasSelection && !present;
        }
        if (this.editButton != null) {
            this.editButton.active = present;
        }
        if (this.removeButton != null) {
            this.removeButton.active = present;
        }
        if (this.copyButton != null) {
            this.copyButton.active = present;
        }
        if (this.applyNameButton != null) {
            this.applyNameButton.active = registryLookup() != null;
        }
        if (this.applyCountButton != null) {
            this.applyCountButton.active = true;
        }
        if (this.undoButton != null) {
            this.undoButton.active = !undoStack.isEmpty();
        }
        if (this.saveButton != null) {
            this.saveButton.setMessage(Component.literal(reviewingSave ? "Confirm" : "Save"));
        }
    }

    private void updateAvailableSuggestion() {
        if (this.availableFilterField == null) {
            return;
        }

        this.availableFilterField.setSuggestion(availableSuggestionSuffix());
    }

    private String availableSuggestionSuffix() {
        if (this.availableFilterField == null) {
            return null;
        }
        String typed = this.availableFilterField.getValue() == null ? "" : this.availableFilterField.getValue().trim().toLowerCase(Locale.ROOT);
        String hint = null;
        for (String componentId : availableComponentIds()) {
            String candidate = componentId.toLowerCase(Locale.ROOT);
            if (typed.isEmpty()) {
                hint = componentId;
                break;
            }
            if (candidate.equals(typed)) {
                hint = null;
                break;
            }
            if (candidate.startsWith(typed)) {
                hint = componentId.substring(typed.length());
                break;
            }
        }
        return hint;
    }

    private boolean isComponentPresent(String componentId) {
        return componentId != null && ItemDataCodec.getComponents(this.itemData).contains(componentId);
    }

    private Tag selectedComponentValue() {
        if (this.selectedComponentId == null) {
            return null;
        }
        return ItemDataCodec.getComponents(this.itemData).get(this.selectedComponentId);
    }

    private int selectedIndex(List<String> ids) {
        if (this.selectedComponentId == null) {
            return -1;
        }
        return ids.indexOf(this.selectedComponentId);
    }

    private int visibleRows(UiRect rect, int rowHeight) {
        return Math.max(1, (rect.height() - 8) / rowHeight);
    }

    private int clampScroll(int current, int totalRows, int visibleRows) {
        return Math.clamp(current, 0, Math.max(0, totalRows - visibleRows));
    }

    private String currentItemTitle() {
        String plainName = ItemDataCodec.readPlainDisplayName(this.itemData, registryLookup());
        return plainName.isBlank() ? this.reference.displayName() : plainName;
    }

    private ItemStack previewStack() {
        HolderLookup.Provider registryLookup = registryLookup();
        if (registryLookup == null) {
            return ItemStack.EMPTY;
        }
        return ItemDataCodec.decode(this.itemData, registryLookup).orElse(ItemStack.EMPTY);
    }

    private HolderLookup.Provider registryLookup() {
        return this.minecraft == null || this.minecraft.level == null ? null : this.minecraft.level.registryAccess();
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    private static String buildComponentEditorHelperText(String componentId, boolean present) {
        String prefix = present
                ? "Edit this component value directly. Live validation uses the actual 1.21.11 component codec."
                : "Add this component using the starter template below. Live validation uses the actual 1.21.11 component codec.";
        if ("minecraft:custom_name".equals(componentId) || "minecraft:item_name".equals(componentId)) {
            return prefix + " Friendly text input supports legacy color/format codes like &c, &l, &r and hex tags like <#ff0000>.";
        }
        if ("minecraft:lore".equals(componentId)) {
            return prefix + " You can still use raw SNBT, but plain multi-line lore input is also accepted. Use one line per lore row, with & color codes or <#rrggbb> hex tags.";
        }
        return prefix;
    }

    private static void appendWrapped(List<String> lines, String prefix, String text) {
        if (text == null || text.isBlank()) {
            lines.add(prefix.trim());
            return;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder(prefix);
        for (String word : words) {
            if (current.length() > prefix.length() && current.length() + 1 + word.length() > DETAIL_WRAP) {
                lines.add(current.toString());
                current = new StringBuilder("    ");
            }
            if (current.length() > prefix.length() && current.charAt(current.length() - 1) != ' ') {
                current.append(' ');
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
    }

    private static String prettySnbt(String raw) {
        return NbtEditorUtils.prettySnbt(raw);
    }
}
