package me.m0dii.modules.itemdata;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiRect;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.modules.getdata.GetDataScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ItemDataScreen extends Screen {
    private static final int LIST_ROW_HEIGHT = 12;
    private static final int DETAIL_ROW_HEIGHT = 10;
    private static final int DETAIL_WRAP = 60;

    private final Screen parent;
    private final ItemDataReference reference;
    private NbtCompound itemData;

    private UiRect summaryRect;
    private UiRect currentPanelRect;
    private UiRect currentListRect;
    private UiRect availablePanelRect;
    private UiRect availableListRect;
    private UiRect detailPanelRect;
    private UiRect detailListRect;

    private TextFieldWidget availableFilterField;
    private TextFieldWidget nameField;
    private TextFieldWidget countField;

    private ButtonWidget addButton;
    private ButtonWidget editButton;
    private ButtonWidget removeButton;
    private ButtonWidget copyButton;
    private ButtonWidget applyNameButton;
    private ButtonWidget applyCountButton;

    private String selectedComponentId;
    private int presentScroll;
    private int availableScroll;
    private int detailScroll;
    private String statusMessage = "Choose a component on the left, or add one from the middle catalog.";
    private int statusColor = UiTheme.TEXT_MUTED;

    public ItemDataScreen(Screen parent, ItemDataReference reference, NbtCompound itemData) {
        super(Text.literal("Item Components"));
        this.parent = parent;
        this.reference = reference;
        this.itemData = itemData == null ? new NbtCompound() : itemData.copy();
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

        this.availableFilterField = new TextFieldWidget(
                this.textRenderer,
                this.availablePanelRect.x() + 8,
                this.availablePanelRect.y() + 20,
                this.availablePanelRect.width() - 16,
                18,
                Text.literal("Search Components")
        );
        this.availableFilterField.setMaxLength(128);
        this.availableFilterField.setChangedListener(value -> {
            refreshSelection();
            updateAvailableSuggestion();
            updateButtons();
        });
        this.addDrawableChild(this.availableFilterField);

        this.countField = new TextFieldWidget(
                this.textRenderer,
                this.summaryRect.x() + 300,
                this.summaryRect.y() + 32,
                56,
                18,
                Text.literal("Count")
        );
        this.countField.setMaxLength(16);
        this.addDrawableChild(this.countField);

        this.nameField = new TextFieldWidget(
                this.textRenderer,
                this.summaryRect.x() + 460,
                this.summaryRect.y() + 32,
                Math.max(110, this.summaryRect.width() - 620),
                18,
                Text.literal("Display Name")
        );
        this.nameField.setMaxLength(256);
        this.addDrawableChild(this.nameField);

        this.applyCountButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Count"), b -> applyCount())
                .dimensions(this.summaryRect.x() + 364, this.summaryRect.y() + 32, 84, 18)
                .build());
        this.applyNameButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Name"), b -> applyPlainName())
                .dimensions(this.summaryRect.right() - 100, this.summaryRect.y() + 32, 88, 18)
                .build());

        this.addButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> addSelectedComponent())
                .dimensions(12, bottomButtonsY, 58, 20)
                .build());
        this.editButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> editSelectedComponent())
                .dimensions(74, bottomButtonsY, 58, 20)
                .build());
        this.removeButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> removeSelectedComponent())
                .dimensions(136, bottomButtonsY, 68, 20)
                .build());
        this.copyButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> copySelectedValue())
                .dimensions(208, bottomButtonsY, 58, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("JSON Mode"), b -> openFullItemEditor())
                .dimensions(this.width - 248, bottomButtonsY, 82, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveToServer())
                .dimensions(this.width - 160, bottomButtonsY, 60, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width - 94, bottomButtonsY, 60, 20)
                .build());

        refreshFieldsFromData();
        refreshSelection();
        updateAvailableSuggestion();
        updateButtons();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, UiTheme.BG);

        renderSummaryPanel(context);
        renderCurrentPanel(context);
        renderAvailablePanel(context);
        renderDetailPanel(context);
        context.drawTextWithShadow(this.textRenderer, this.statusMessage, 12, this.height - 44, this.statusColor);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0) {
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
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == GLFW.GLFW_KEY_TAB && this.availableFilterField != null && this.availableFilterField.isFocused()) {
            String suggestion = availableSuggestionSuffix();
            if (suggestion != null && !suggestion.isBlank()) {
                this.availableFilterField.setText(this.availableFilterField.getText() + suggestion);
                this.availableFilterField.setCursor(this.availableFilterField.getText().length(), false);
                updateAvailableSuggestion();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    private void renderSummaryPanel(DrawContext context) {
        FormPanel summaryPanel = new FormPanel(this.summaryRect);
        summaryPanel.render(context);

        ItemStack previewStack = previewStack();
        if (!previewStack.isEmpty()) {
            context.drawItem(previewStack, this.summaryRect.x() + 10, this.summaryRect.y() + 18);
        }

        String itemTitle = currentItemTitle();
        String itemId = this.itemData.getString("id").orElse("minecraft:air");
        context.drawTextWithShadow(this.textRenderer, itemTitle, this.summaryRect.x() + 40, this.summaryRect.y() + 10, UiTheme.TEXT);
        context.drawTextWithShadow(this.textRenderer, this.reference.slotLabel() + " | " + itemId, this.summaryRect.x() + 40, this.summaryRect.y() + 22, UiTheme.TEXT_MUTED);
        context.drawTextWithShadow(this.textRenderer, "Mode: Easy", this.summaryRect.right() - 76, this.summaryRect.y() + 10, UiTheme.TEXT_ACCENT);
        context.drawTextWithShadow(this.textRenderer, "Count", this.countField.getX(), this.countField.getY() - 10, UiTheme.TEXT_MUTED);
        context.drawTextWithShadow(this.textRenderer, "Rename (minecraft:custom_name)", this.nameField.getX(), this.nameField.getY() - 10, UiTheme.TEXT_MUTED);
        context.drawTextWithShadow(this.textRenderer,
                "Current components: " + presentComponentIds().size(),
                this.summaryRect.x() + 40,
                this.summaryRect.y() + 56,
                UiTheme.TEXT_MUTED);
    }

    private void renderCurrentPanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.currentPanelRect);
        panel.render(context);
        panel.drawLabel(context, this.textRenderer, "On Item (" + presentComponentIds().size() + ")", 8, 8);

        List<String> rows = presentRows();
        new ListPanel(this.currentListRect).render(
                context,
                this.textRenderer,
                rows,
                selectedIndex(presentComponentIds()),
                this.presentScroll,
                LIST_ROW_HEIGHT
        );
    }

    private void renderAvailablePanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.availablePanelRect);
        panel.render(context);
        panel.drawLabel(context, this.textRenderer, "Available Components", 8, 8);

        List<String> rows = availableRows();
        new ListPanel(this.availableListRect).render(
                context,
                this.textRenderer,
                rows,
                selectedIndex(availableComponentIds()),
                this.availableScroll,
                LIST_ROW_HEIGHT
        );
    }

    private void renderDetailPanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.detailPanelRect);
        panel.render(context);
        panel.drawLabel(context, this.textRenderer, "Component Details", 8, 8);

        if (this.selectedComponentId == null) {
            context.drawTextWithShadow(this.textRenderer, "No component selected.", this.detailPanelRect.x() + 8, this.detailPanelRect.y() + 24, UiTheme.TEXT_MUTED);
        } else {
            boolean present = isComponentPresent(this.selectedComponentId);
            context.drawTextWithShadow(this.textRenderer,
                    ItemComponentMetadata.label(this.selectedComponentId),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 24,
                    UiTheme.TEXT_ACCENT);
            context.drawTextWithShadow(this.textRenderer,
                    this.selectedComponentId,
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 36,
                    UiTheme.TEXT_MUTED);
            context.drawTextWithShadow(this.textRenderer,
                    "Type: " + ItemComponentMetadata.typeHint(this.selectedComponentId),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 48,
                    UiTheme.TEXT_MUTED);
            context.drawTextWithShadow(this.textRenderer,
                    "State: " + (present ? "Present on item" : "Missing from item"),
                    this.detailPanelRect.x() + 8,
                    this.detailPanelRect.y() + 60,
                    present ? 0xFF70D070 : 0xFFFFD37A);
        }

        new ListPanel(this.detailListRect).render(
                context,
                this.textRenderer,
                buildDetailLines(),
                -1,
                this.detailScroll,
                DETAIL_ROW_HEIGHT
        );
    }

    private boolean selectFromList(Click click, UiRect rect, List<String> ids, boolean presentList, boolean doubled) {
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
                               java.util.function.IntConsumer setter,
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
        if (this.client == null || this.selectedComponentId == null) {
            return;
        }
        RegistryWrapper.WrapperLookup registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }

        String componentId = this.selectedComponentId;
        NbtElement currentValue = selectedComponentValue();
        String initialValue = present && currentValue != null
                ? currentValue.toString()
                : ItemDataCodec.defaultTemplateForComponent(componentId, registryLookup);
        String helperText = present
                ? "Edit this component value directly. Save validates it with the actual 1.21.11 component codec."
                : "Add this component using the starter template below. Save validates it with the actual 1.21.11 component codec.";

        this.client.setScreen(GetDataScreen.createStandalone(
                this,
                Text.literal(present ? "Edit Component" : "Add Component"),
                ItemComponentMetadata.label(componentId) + " (" + componentId + ")",
                initialValue,
                helperText,
                false,
                true,
                (screen, raw, path) -> {
                    NbtElement normalized = ItemDataCodec.normalizeComponentValue(componentId, raw, registryLookup);
                    ItemDataCodec.ensureComponents(this.itemData).put(componentId, normalized);
                    this.selectedComponentId = componentId;
                    refreshFieldsFromData();
                    refreshSelection();
                    updateButtons();
                    setStatus((present ? "Updated " : "Added ") + componentId + ".", 0xFF70D070);
                }
        ));
    }

    private void openFullItemEditor() {
        if (this.client == null) {
            return;
        }
        RegistryWrapper.WrapperLookup registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }

        this.client.setScreen(GetDataScreen.createStandalone(
                this,
                Text.literal("Item JSON Mode"),
                currentItemTitle(),
                this.itemData.toString(),
                "Edit the full serialized item root. Component ids under `components` support Tab/click completion.",
                true,
                true,
                (screen, raw, path) -> {
                    this.itemData = ItemDataCodec.normalizeItemData(raw, registryLookup);
                    refreshFieldsFromData();
                    refreshSelection();
                    updateButtons();
                    setStatus("Updated the full item draft from JSON mode.", 0xFF70D070);
                },
                ItemComponentCatalog.jsonSuggestionProvider()
        ));
    }

    private void applyPlainName() {
        RegistryWrapper.WrapperLookup registryLookup = registryLookup();
        if (registryLookup == null) {
            setStatus("Registry lookup is not available on the client right now.", 0xFFFF9090);
            return;
        }
        try {
            ItemDataCodec.applyPlainCustomName(this.itemData, this.nameField == null ? "" : this.nameField.getText(), registryLookup);
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
            ItemDataCodec.applyCount(this.itemData, this.countField == null ? "" : this.countField.getText());
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

        NbtCompound components = ItemDataCodec.getComponents(this.itemData);
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
        if (this.client == null || this.selectedComponentId == null || !isComponentPresent(this.selectedComponentId)) {
            setStatus("Select an existing component to copy.", 0xFFFF9090);
            return;
        }
        NbtElement value = selectedComponentValue();
        if (value == null) {
            setStatus("That component does not have a readable value.", 0xFFFF9090);
            return;
        }
        this.client.keyboard.setClipboard(value.toString());
        setStatus("Copied " + this.selectedComponentId + " to the clipboard.", 0xFF70D070);
    }

    private void saveToServer() {
        boolean sent = ItemDataSyncClient.saveItemData(this.reference.slotIndex(), this.itemData);
        setStatus(
                sent ? "Sent the updated item data to the server for " + this.reference.slotLabel() + "." : "Unable to send the item data to the server.",
                sent ? 0xFF70D070 : 0xFFFF9090
        );
    }

    private void refreshFieldsFromData() {
        if (this.nameField != null) {
            this.nameField.setText(ItemDataCodec.readPlainDisplayName(this.itemData, registryLookup()));
        }
        if (this.countField != null) {
            this.countField.setText(String.valueOf(ItemDataCodec.readCount(this.itemData)));
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
        return ItemDataCodec.getComponents(this.itemData).getKeys().stream()
                .sorted(Comparator
                        .comparingInt(ItemComponentMetadata::priority)
                        .thenComparing(ItemComponentMetadata::label))
                .toList();
    }

    private List<String> availableComponentIds() {
        Set<String> present = presentComponentIdSet();
        return ItemComponentCatalog.filterComponentIds(this.availableFilterField == null ? "" : this.availableFilterField.getText(), present).stream()
                .filter(id -> !present.contains(id))
                .toList();
    }

    private Set<String> presentComponentIdSet() {
        return ItemDataCodec.getComponents(this.itemData).getKeys();
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

        RegistryWrapper.WrapperLookup registryLookup = registryLookup();
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
        String typed = this.availableFilterField.getText() == null ? "" : this.availableFilterField.getText().trim().toLowerCase(Locale.ROOT);
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

    private NbtElement selectedComponentValue() {
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
        RegistryWrapper.WrapperLookup registryLookup = registryLookup();
        if (registryLookup == null) {
            return ItemStack.EMPTY;
        }
        return ItemDataCodec.decode(this.itemData, registryLookup).orElse(ItemStack.EMPTY);
    }

    private RegistryWrapper.WrapperLookup registryLookup() {
        return this.client == null || this.client.world == null ? null : this.client.world.getRegistryManager();
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
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
        if (current.length() > 0) {
            lines.add(current.toString());
        }
    }

    private static String prettySnbt(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }

        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '"' && (i == 0 || raw.charAt(i - 1) != '\\')) {
                inString = !inString;
                out.append(ch);
                continue;
            }
            if (inString) {
                out.append(ch);
                continue;
            }
            switch (ch) {
                case '{', '[' -> {
                    out.append(ch).append('\n');
                    indent++;
                    out.repeat("  ", Math.max(0, indent));
                }
                case '}', ']' -> {
                    out.append('\n');
                    indent = Math.max(0, indent - 1);
                    out.repeat("  ", Math.max(0, indent));
                    out.append(ch);
                }
                case ',' -> {
                    out.append(ch).append('\n');
                    out.repeat("  ", Math.max(0, indent));
                }
                case ':' -> out.append(": ");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }
}
