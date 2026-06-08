package me.m0dii.modules.getdata;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiRect;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.utils.NbtEditorUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class GetDataBrowserScreen extends Screen implements GetDataTargetView {
    private static final int LIST_ROW_HEIGHT = 12;
    private static final int DETAIL_ROW_HEIGHT = 10;
    private static final int DETAIL_WRAP = 62;

    private final Screen parent;
    private final String targetName;
    private final String targetToken;
    private final List<String> pathSegments = new ArrayList<>();

    private NbtCompound rootData;
    private String selectedSegment;
    private int listScroll;
    private int detailScroll;
    private String statusMessage = "Select a field to inspect it. Double-click compounds and lists to browse into them.";
    private int statusColor = UiTheme.TEXT_MUTED;

    private UiRect summaryRect;
    private UiRect fieldPanelRect;
    private UiRect fieldListRect;
    private UiRect detailPanelRect;
    private UiRect detailListRect;

    private ButtonWidget upButton;
    private ButtonWidget openButton;
    private ButtonWidget editButton;
    private ButtonWidget removeButton;
    private ButtonWidget copyButton;
    private ButtonWidget copyFullButton;

    private GetDataBrowserScreen(Screen parent, String targetName, String targetToken, String payload) {
        super(Text.literal("GetData Browser"));
        this.parent = parent;
        this.targetName = targetName;
        this.targetToken = targetToken;
        this.rootData = parsePayload(payload);
    }

    public static Screen create(Screen parent, String targetName, String targetToken, String payload) {
        return new GetDataBrowserScreen(parent, targetName, targetToken, payload);
    }

    @Override
    protected void init() {
        int bottomButtonsY = this.height - 30;
        int mainTop = 98;
        int mainHeight = Math.max(140, bottomButtonsY - mainTop - 18);
        int totalWidth = this.width - 24;

        this.summaryRect = new UiRect(12, 12, totalWidth, 78);
        this.fieldPanelRect = new UiRect(12, mainTop, Math.max(240, this.width / 3), mainHeight);
        this.detailPanelRect = new UiRect(this.fieldPanelRect.right() + 8, mainTop, Math.max(260, this.width - this.fieldPanelRect.right() - 20), mainHeight);
        this.fieldListRect = this.fieldPanelRect.inset(8, 20, 8, 8);
        this.detailListRect = this.detailPanelRect.inset(8, 44, 8, 8);

        this.upButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Up"), b -> goUp())
                .dimensions(12, bottomButtonsY, 48, 20)
                .build());
        this.openButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Open"), b -> openSelectedEntry())
                .dimensions(64, bottomButtonsY, 56, 20)
                .build());
        this.editButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), b -> editSelectedEntry())
                .dimensions(124, bottomButtonsY, 56, 20)
                .build());
        this.removeButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> removeSelectedEntry())
                .dimensions(184, bottomButtonsY, 68, 20)
                .build());
        this.copyButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> copySelectedValue())
                .dimensions(256, bottomButtonsY, 56, 20)
                .build());
        this.copyFullButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Copy Full"), b -> copyFullPayload())
                .dimensions(316, bottomButtonsY, 72, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Raw Mode"), b -> openRawMode())
                .dimensions(this.width - 248, bottomButtonsY, 82, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveToTarget())
                .dimensions(this.width - 160, bottomButtonsY, 60, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width - 94, bottomButtonsY, 60, 20)
                .build());

        refreshSelection();
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
        renderFieldPanel(context);
        renderDetailPanel(context);
        context.drawTextWithShadow(this.textRenderer, this.statusMessage, 12, this.height - 44, this.statusColor);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0 || !this.fieldListRect.contains(click.x(), click.y())) {
            return false;
        }

        List<NbtEditorUtils.ChildEntry> entries = childEntries();
        int relativeY = (int) click.y() - this.fieldListRect.y() - 4;
        int clickedIndex = this.listScroll + Math.max(0, relativeY / LIST_ROW_HEIGHT);
        if (clickedIndex < 0 || clickedIndex >= entries.size()) {
            return false;
        }

        this.selectedSegment = entries.get(clickedIndex).segment();
        this.detailScroll = 0;
        updateButtons();
        if (doubled) {
            if (selectedEntryNavigable()) {
                openSelectedEntry();
            } else {
                editSelectedEntry();
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (scrollList(mouseX, mouseY, verticalAmount, this.fieldListRect, childEntries().size(), visibleRows(this.fieldListRect, LIST_ROW_HEIGHT), value -> this.listScroll = value, this.listScroll)) {
            return true;
        }
        if (scrollList(mouseX, mouseY, verticalAmount, this.detailListRect, buildDetailLines().size(), visibleRows(this.detailListRect, DETAIL_ROW_HEIGHT), value -> this.detailScroll = value, this.detailScroll)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE && !this.pathSegments.isEmpty()) {
            goUp();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean matchesTarget(String token) {
        return token != null && token.equals(this.targetToken);
    }

    @Override
    public void applySyncedPayload(String payload) {
        this.rootData = parsePayload(payload);
        trimPathToExistingNode();
        refreshSelection();
        updateButtons();
        setStatus("Updated the local draft from the synced target payload.", 0xFF70D070);
    }

    private void renderSummaryPanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.summaryRect);
        panel.render(context);
        context.drawTextWithShadow(this.textRenderer, this.targetName, this.summaryRect.x() + 10, this.summaryRect.y() + 10, UiTheme.TEXT);
        context.drawTextWithShadow(this.textRenderer, "Path: " + NbtEditorUtils.formatPath(this.pathSegments), this.summaryRect.x() + 10, this.summaryRect.y() + 24, UiTheme.TEXT_ACCENT);
        context.drawTextWithShadow(this.textRenderer, "Node: " + NbtEditorUtils.summary(currentNode()), this.summaryRect.x() + 10, this.summaryRect.y() + 38, UiTheme.TEXT_MUTED);
        context.drawTextWithShadow(this.textRenderer, "Mode: Structured", this.summaryRect.right() - 98, this.summaryRect.y() + 10, UiTheme.TEXT_ACCENT);
        context.drawTextWithShadow(this.textRenderer,
                "Double-click compounds and lists to descend. Raw Mode keeps full-payload copy/paste editing available.",
                this.summaryRect.x() + 10,
                this.summaryRect.y() + 56,
                UiTheme.TEXT_MUTED);
    }

    private void renderFieldPanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.fieldPanelRect);
        panel.render(context);
        panel.drawLabel(context, this.textRenderer, "Fields (" + childEntries().size() + ")", 8, 8);
        new ListPanel(this.fieldListRect).render(
                context,
                this.textRenderer,
                fieldRows(),
                selectedIndex(),
                this.listScroll,
                LIST_ROW_HEIGHT
        );
    }

    private void renderDetailPanel(DrawContext context) {
        FormPanel panel = new FormPanel(this.detailPanelRect);
        panel.render(context);
        panel.drawLabel(context, this.textRenderer, "Details", 8, 8);
        new ListPanel(this.detailListRect).render(
                context,
                this.textRenderer,
                buildDetailLines(),
                -1,
                this.detailScroll,
                DETAIL_ROW_HEIGHT
        );
    }

    private void goUp() {
        if (this.pathSegments.isEmpty()) {
            return;
        }
        this.pathSegments.removeLast();
        this.selectedSegment = null;
        refreshSelection();
        updateButtons();
        setStatus("Moved up to " + NbtEditorUtils.formatPath(this.pathSegments) + ".", UiTheme.TEXT_MUTED);
    }

    private void openSelectedEntry() {
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        if (entry == null || !NbtEditorUtils.isContainer(entry.value())) {
            setStatus("Select a compound or list to open it.", 0xFFFF9090);
            return;
        }
        this.pathSegments.add(entry.segment());
        this.selectedSegment = null;
        refreshSelection();
        updateButtons();
        setStatus("Opened " + displayPathFor(entry) + ".", UiTheme.TEXT_MUTED);
    }

    private void editSelectedEntry() {
        if (this.client == null) {
            return;
        }
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        if (entry == null || entry.value() == null) {
            setStatus("Select a field to edit.", 0xFFFF9090);
            return;
        }

        String segment = entry.segment();
        String entryPath = displayPathFor(entry);
        this.client.setScreen(GetDataScreen.createStandalone(
                this,
                Text.literal("Edit NBT Field"),
                entryPath,
                entry.value().toString(),
                "Edit the selected field value. Save updates only this local draft. Use Save on the previous screen to apply the full target payload.",
                false,
                true,
                (screen, raw, path) -> {
                    NbtEditorUtils.setChild(currentNode(), segment, NbtEditorUtils.parseElement(raw));
                    this.selectedSegment = segment;
                    refreshSelection();
                    updateButtons();
                    setStatus("Updated " + entryPath + ".", 0xFF70D070);
                }
        ));
    }

    private void removeSelectedEntry() {
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        if (entry == null) {
            setStatus("Select a field to remove.", 0xFFFF9090);
            return;
        }
        NbtEditorUtils.removeChild(currentNode(), entry.segment());
        this.selectedSegment = null;
        refreshSelection();
        updateButtons();
        setStatus("Removed " + displayPathFor(entry) + ".", 0xFF70D070);
    }

    private void copySelectedValue() {
        if (this.client == null) {
            return;
        }
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        if (entry == null || entry.value() == null) {
            setStatus("Select a field to copy.", 0xFFFF9090);
            return;
        }
        this.client.keyboard.setClipboard(entry.value().toString());
        setStatus("Copied " + displayPathFor(entry) + " to the clipboard.", 0xFF70D070);
    }

    private void copyFullPayload() {
        if (this.client == null) {
            return;
        }
        this.client.keyboard.setClipboard(this.rootData.toString());
        setStatus("Copied the full payload to the clipboard.", 0xFF70D070);
    }

    private void openRawMode() {
        if (this.client == null) {
            return;
        }
        this.client.setScreen(GetDataScreen.createStandalone(
                this,
                Text.literal("GetData Raw Mode"),
                this.targetName,
                this.rootData.toString(),
                "Edit the full SNBT payload directly. Save updates only the local draft; use Save on the previous screen to apply it to the target.",
                true,
                true,
                (screen, raw, path) -> {
                    this.rootData = NbtEditorUtils.parseCompound(raw);
                    trimPathToExistingNode();
                    refreshSelection();
                    updateButtons();
                    setStatus("Updated the local draft from raw mode.", 0xFF70D070);
                }
        ));
    }

    private void saveToTarget() {
        if (this.client == null || this.client.player == null) {
            return;
        }
        try {
            GetDataTargetActions.saveTargetPayload(this.client, this.targetToken, this.rootData.toString());
            setStatus("Sent the updated target payload.", 0xFF70D070);
        } catch (CommandSyntaxException exception) {
            setStatus(exception.getRawMessage().getString(), 0xFFFF9090);
        }
    }

    private List<NbtEditorUtils.ChildEntry> childEntries() {
        return NbtEditorUtils.childEntries(currentNode());
    }

    private List<String> fieldRows() {
        List<NbtEditorUtils.ChildEntry> entries = childEntries();
        if (entries.isEmpty()) {
            return List.of("No fields at this node.");
        }
        return entries.stream().map(NbtEditorUtils::rowLabel).toList();
    }

    private List<String> buildDetailLines() {
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        if (entry == null) {
            return List.of(
                    "No field selected.",
                    "Open compounds and lists to browse deeper.",
                    "Edit changes only the selected field in the local draft.",
                    "Raw Mode lets you copy/paste the complete payload."
            );
        }

        List<String> lines = new ArrayList<>();
        lines.add("Field: " + entry.label());
        lines.add("Path: " + displayPathFor(entry));
        lines.add("Type: " + NbtEditorUtils.typeName(entry.value()));
        lines.add("Summary: " + NbtEditorUtils.summary(entry.value()));
        lines.add(" ");
        if (NbtEditorUtils.isContainer(entry.value())) {
            appendWrapped(lines, "Hint: ", "Open descends into this " + NbtEditorUtils.typeName(entry.value()) + ". Edit opens the raw value editor for this subtree only.");
            lines.add(" ");
        }
        lines.add("Current Value:");
        appendPretty(lines, entry.value() == null ? "{}" : entry.value().toString());
        lines.add(" ");
        appendWrapped(lines, "Copy: ", "Copies only the selected field value. Copy Full copies the entire root payload.");
        return lines;
    }

    private NbtEditorUtils.ChildEntry selectedEntry() {
        if (this.selectedSegment == null) {
            return null;
        }
        for (NbtEditorUtils.ChildEntry entry : childEntries()) {
            if (entry.segment().equals(this.selectedSegment)) {
                return entry;
            }
        }
        return null;
    }

    private boolean selectedEntryNavigable() {
        NbtEditorUtils.ChildEntry entry = selectedEntry();
        return entry != null && NbtEditorUtils.isContainer(entry.value());
    }

    private int selectedIndex() {
        List<NbtEditorUtils.ChildEntry> entries = childEntries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).segment().equals(this.selectedSegment)) {
                return i;
            }
        }
        return -1;
    }

    private NbtElement currentNode() {
        NbtElement current = NbtEditorUtils.resolvePath(this.rootData, this.pathSegments);
        return current == null ? this.rootData : current;
    }

    private void refreshSelection() {
        trimPathToExistingNode();
        List<NbtEditorUtils.ChildEntry> entries = childEntries();
        if (this.selectedSegment == null || entries.stream().noneMatch(entry -> entry.segment().equals(this.selectedSegment))) {
            this.selectedSegment = entries.isEmpty() ? null : entries.getFirst().segment();
            this.detailScroll = 0;
        }

        this.listScroll = clampScroll(this.listScroll, entries.size(), visibleRows(this.fieldListRect, LIST_ROW_HEIGHT));
        this.detailScroll = clampScroll(this.detailScroll, buildDetailLines().size(), visibleRows(this.detailListRect, DETAIL_ROW_HEIGHT));
        ensureVisible(entries, value -> this.listScroll = value, this.listScroll, visibleRows(this.fieldListRect, LIST_ROW_HEIGHT));
    }

    private void trimPathToExistingNode() {
        while (!this.pathSegments.isEmpty() && !NbtEditorUtils.isContainer(NbtEditorUtils.resolvePath(this.rootData, this.pathSegments))) {
            this.pathSegments.removeLast();
        }
    }

    private void updateButtons() {
        boolean hasSelection = selectedEntry() != null;
        if (this.upButton != null) {
            this.upButton.active = !this.pathSegments.isEmpty();
        }
        if (this.openButton != null) {
            this.openButton.active = selectedEntryNavigable();
        }
        if (this.editButton != null) {
            this.editButton.active = hasSelection;
        }
        if (this.removeButton != null) {
            this.removeButton.active = hasSelection;
        }
        if (this.copyButton != null) {
            this.copyButton.active = hasSelection;
        }
        if (this.copyFullButton != null) {
            this.copyFullButton.active = true;
        }
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

    private void ensureVisible(List<NbtEditorUtils.ChildEntry> entries, IntConsumer setter, int currentScroll, int visibleRows) {
        if (this.selectedSegment == null || entries.isEmpty()) {
            return;
        }
        int index = selectedIndex();
        if (index < 0) {
            return;
        }
        int scroll = currentScroll;
        if (index < scroll) {
            scroll = index;
        } else if (index >= scroll + visibleRows) {
            scroll = Math.max(0, index - visibleRows + 1);
        }
        setter.accept(scroll);
    }

    private int visibleRows(UiRect rect, int rowHeight) {
        return Math.max(1, (rect.height() - 8) / rowHeight);
    }

    private int clampScroll(int current, int totalRows, int visibleRows) {
        return Math.clamp(current, 0, Math.max(0, totalRows - visibleRows));
    }

    private String displayPathFor(NbtEditorUtils.ChildEntry entry) {
        List<String> fullPath = new ArrayList<>(this.pathSegments);
        fullPath.add(entry.segment());
        return NbtEditorUtils.formatPath(fullPath);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    private NbtCompound parsePayload(String payload) {
        NbtCompound parsed = NbtEditorUtils.parseCompoundOrNull(payload);
        return parsed == null ? new NbtCompound() : parsed;
    }

    private void appendPretty(List<String> lines, String raw) {
        String[] split = NbtEditorUtils.prettySnbt(raw).split("\\n", -1);
        for (String line : split) {
            lines.add(line.isEmpty() ? " " : line);
        }
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
}
