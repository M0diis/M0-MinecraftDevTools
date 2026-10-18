package me.m0dii.modules.getdata;


import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.modules.itemdata.ItemDataDiff;
import me.m0dii.utils.NbtEditorUtils;
import me.m0dii.utils.SnbtText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;

import java.util.*;

public class GetDataScreen extends Screen implements GetDataTargetView, ThemedScreen {
    private static final int BUTTON_BAR_HEIGHT = 34;
    private static final int NO_PATH_BOTTOM_RESERVE = 90;
    private static final int PATH_BOTTOM_RESERVE = 84;

    @FunctionalInterface
    public interface SaveAction {
        void save(GetDataScreen screen, String raw, String path) throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface InputValidator {
        void validate(GetDataScreen screen, String raw, String path) throws CommandSyntaxException;
    }

    @FunctionalInterface
    public interface InlineSuggestionProvider {
        InlineSuggestions suggest(String editorText, int cursor);
    }

    public record InlineSuggestions(int start, int end, List<String> suggestions) {
    }

    private final Screen parent;
    private final Component screenTitle;
    private final String targetName;
    private final String targetToken;
    private final String helperText;
    private final String initialData;
    private final boolean supportsPathField;
    private final boolean requireCompoundRootWhenPathBlank;
    private final boolean closeAfterSave;
    private final SaveAction saveAction;
    private final InputValidator inputValidator;
    private final InlineSuggestionProvider inlineSuggestionProvider;
    private EditBox pathField;

    private String editorText = "{}";
    private int cursor = 0;
    private int selectionAnchor = -1;
    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;
    private final int lineHeight = 10;
    private int scrollLine = 0;
    private static final int MAX_SUGGESTIONS = 96;

    private String parseError = null;
    private int parseErrorIndex = -1;
    private final List<String> pathSuggestions = new ArrayList<>();
    private String activePathSuggestion = null;
    private InlineSuggestions inlineSuggestions = null;
    private int suggestionPanelX = -1;
    private int suggestionPanelY = -1;
    private int suggestionPanelW = 0;
    private int suggestionPanelRowHeight = 10;
    private int suggestionPanelRows = 0;
    private boolean syncedPayloadApplied = false;

    private record Snapshot(String text, int cursor) {
    }

    private static final int UNDO_LIMIT = 100;
    private static final String INDENT = "  ";
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();
    private long lastTypingMs;
    private boolean reviewing;

    /** Guards the initial-content reset so window resizes don't wipe in-progress edits. */
    private boolean appliedInitialContent;
    private List<String> reviewLines = List.of();
    private Button saveButton;
    /** What the target held when opened (or last synced from the server); reviews compare against it. */
    private String baselineData;

    private GetDataScreen(Screen parent,
                          Component screenTitle,
                          String targetName,
                          String targetToken,
                          String initialData,
                          String helperText,
                          boolean supportsPathField,
                          boolean requireCompoundRootWhenPathBlank,
                          boolean closeAfterSave,
                          SaveAction saveAction,
                          InputValidator inputValidator,
                          InlineSuggestionProvider inlineSuggestionProvider) {
        super(screenTitle);
        this.parent = parent;
        this.screenTitle = screenTitle;
        this.targetName = targetName;
        this.targetToken = targetToken;
        this.helperText = helperText;
        this.initialData = NbtEditorUtils.prettySnbt(initialData);
        this.baselineData = this.initialData;
        this.supportsPathField = supportsPathField;
        this.requireCompoundRootWhenPathBlank = requireCompoundRootWhenPathBlank;
        this.closeAfterSave = closeAfterSave;
        this.saveAction = saveAction;
        this.inputValidator = inputValidator;
        this.inlineSuggestionProvider = inlineSuggestionProvider;
    }

    public static Screen create(Screen parent, String targetName, String targetToken, String initialData) {
        return new GetDataScreen(
                parent,
                Component.literal("GetData Editor"),
                targetName,
                targetToken,
                initialData,
                "Edit SNBT. Optional path enables /data modify ... set value. Empty path uses /data merge.",
                true,
                true,
                false,
                GetDataScreen::saveGetDataChanges,
                null,
                null
        );
    }

    public static Screen createStandalone(Screen parent,
                                          Component screenTitle,
                                          String targetName,
                                          String initialData,
                                          String helperText,
                                          boolean requireCompoundRootWhenPathBlank,
                                          boolean closeAfterSave,
                                          SaveAction saveAction) {
        return createStandalone(
                parent,
                screenTitle,
                targetName,
                initialData,
                helperText,
                requireCompoundRootWhenPathBlank,
                closeAfterSave,
                saveAction,
                null,
                null
        );
    }

    public static Screen createStandalone(Screen parent,
                                          Component screenTitle,
                                          String targetName,
                                          String initialData,
                                          String helperText,
                                          boolean requireCompoundRootWhenPathBlank,
                                          boolean closeAfterSave,
                                          SaveAction saveAction,
                                          InlineSuggestionProvider inlineSuggestionProvider) {
        return createStandalone(
                parent,
                screenTitle,
                targetName,
                initialData,
                helperText,
                requireCompoundRootWhenPathBlank,
                closeAfterSave,
                saveAction,
                null,
                inlineSuggestionProvider
        );
    }

    public static Screen createStandalone(Screen parent,
                                          Component screenTitle,
                                          String targetName,
                                          String initialData,
                                          String helperText,
                                          boolean requireCompoundRootWhenPathBlank,
                                          boolean closeAfterSave,
                                          SaveAction saveAction,
                                          InputValidator inputValidator,
                                          InlineSuggestionProvider inlineSuggestionProvider) {
        return new GetDataScreen(
                parent,
                screenTitle,
                targetName,
                null,
                initialData,
                helperText,
                false,
                requireCompoundRootWhenPathBlank,
                closeAfterSave,
                saveAction,
                inputValidator,
                inlineSuggestionProvider
        );
    }

    @Override
    protected void init() {
        this.boxX = 12;
        this.boxY = 34;
        this.boxW = this.width - 24;
        this.boxH = this.height - (this.supportsPathField ? PATH_BOTTOM_RESERVE : NO_PATH_BOTTOM_RESERVE);
        if (!this.appliedInitialContent) {
            this.editorText = this.initialData;
            this.cursor = this.editorText.length();
            this.selectionAnchor = -1;
            this.appliedInitialContent = true;
        }
        validateEditorSnbt();
        rebuildPathSuggestions();
        rebuildInlineSuggestions();

        this.pathField = null;
        if (this.supportsPathField) {
            this.pathField = new EditBox(this.font, 12, this.height - 98, this.width - 24, 18, Component.literal("Path"));
            this.pathField.setMaxLength(120);
            this.pathField.setValue("");
            this.pathField.setResponder(value -> {
                updatePathSuggestionHint();
                validateEditorSnbt();
            });
            this.addRenderableWidget(this.pathField);
            updatePathSuggestionHint();
        }

        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveChanges())
                .bounds(12, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Copy"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(this.editorText);
                    }
                }).bounds(76, this.height - BUTTON_BAR_HEIGHT, 50, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Format"), b -> formatEditor(false))
                .bounds(130, this.height - BUTTON_BAR_HEIGHT, 54, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Compact"), b -> formatEditor(true))
                .bounds(188, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Undo"), b -> undo())
                .bounds(252, this.height - BUTTON_BAR_HEIGHT, 44, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Redo"), b -> redo())
                .bounds(300, this.height - BUTTON_BAR_HEIGHT, 44, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 72, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
        syncEditorTextInput();
    }

    /**
     * 26.3 delivers CharacterEvents only while SDL text input is started. EditBox does this on focus,
     * but the custom SNBT editor is not an EditBox, so it must enable text input itself.
     */
    private void syncEditorTextInput() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        if (this.pathField != null && this.pathField.isFocused()) {
            return;
        }
        client.textInputManager().setTextInputArea(this.boxX, this.boxY, this.boxX + this.boxW, this.boxY + this.boxH);
        client.onTextInputFocusChange(this, true);
    }

    @Override
    protected void setInitialFocus() {
        // The custom SNBT editor is the text target; the path field must not steal typing on open.
        if (this.pathField != null) {
            this.pathField.setFocused(false);
        }
        this.setFocused(null);
    }

    private void saveChanges() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        String raw = this.editorText;
        try {
            String path = this.pathField == null ? "" : this.pathField.getValue().trim();
            validateRawInput(raw, path);
            if (!this.reviewing && this.supportsPathField && (path == null || path.isBlank())) {
                List<String> changes = describeChanges();
                if (changes != null) {
                    if (changes.isEmpty()) {
                        this.minecraft.player.sendSystemMessage(Component.literal("[GetData] Nothing changed."));
                        return;
                    }
                    this.reviewing = true;
                    this.reviewLines = changes;
                    this.saveButton.setMessage(Component.literal("Confirm"));
                    return;
                }
            }
            endReview();
            this.saveAction.save(this, raw, path);
            if (this.closeAfterSave) {
                onClose();
            }
        } catch (CommandSyntaxException e) {
            this.minecraft.player.sendSystemMessage(Component.literal("[GetData] Invalid NBT/SNBT: " + e.getMessage()));
        }
    }

    private static void saveGetDataChanges(GetDataScreen screen, String raw, String path) throws CommandSyntaxException {
        if (screen.minecraft == null || screen.minecraft.player == null || screen.targetToken == null) {
            return;
        }

        if (path == null || path.isBlank()) {
            GetDataTargetActions.saveTargetPayload(screen.minecraft, screen.targetToken, raw);
            return;
        }

        GetDataTargetActions.saveTargetPathValue(screen.minecraft, screen.targetToken, raw, path);
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
        context.text(this.font, "Target: " + this.targetName, 12, 10, UiTheme.TEXT);
        context.text(this.font, this.helperText, 12, 22, UiTheme.TEXT_MUTED);
        int statusColor = this.parseError == null ? 0xFF70D070 : 0xFFFF7070;
        String statusPrefix = this.inputValidator == null ? "SNBT" : "Input";
        String status = this.parseError == null ? statusPrefix + " valid" : statusPrefix + " error: " + this.parseError;
        if (this.parseError != null && this.parseErrorIndex >= 0) {
            SnbtText.LineCol at = SnbtText.lineCol(this.editorText, this.parseErrorIndex);
            status = status + "  (line " + (at.line() + 1) + ", col " + (at.column() + 1) + " · Ctrl+E jumps there)";
        }
        if (this.syncedPayloadApplied && this.parseError == null) {
            status = status + " (server sync)";
        }
        context.text(this.font, status, 12, this.supportsPathField ? this.height - 76 : this.height - 50, statusColor);

        FormPanel editorPanel = new FormPanel(this.boxX, this.boxY, this.boxW, this.boxH);
        editorPanel.extractRenderState(context);

        List<String> suggestionRows = this.pathSuggestions.stream().limit(6).map(s -> "- " + s).toList();
        if (!this.supportsPathField && this.inlineSuggestions != null && this.inlineSuggestions.suggestions() != null) {
            suggestionRows = this.inlineSuggestions.suggestions().stream().limit(6).map(s -> "- " + s).toList();
        }
        if (((this.supportsPathField && !suggestionRows.isEmpty()) || (!this.supportsPathField && !suggestionRows.isEmpty()))) {
            int suggestionW = 208;
            int suggestionX = this.boxX + this.boxW - suggestionW - 4;
            int suggestionY = this.boxY + 6;
            int suggestionH = Math.clamp(this.boxH - 12, 42, 96);
            this.suggestionPanelX = suggestionX;
            this.suggestionPanelY = suggestionY;
            this.suggestionPanelW = suggestionW;
            this.suggestionPanelRows = Math.min(6, suggestionRows.size());
            ListPanel listPanel = new ListPanel(suggestionX, suggestionY, suggestionW, suggestionH);
            listPanel.extractRenderState(context, this.font, suggestionRows, -1, 0, 10);
            if (!this.supportsPathField) {
                context.text(this.font, "Tab or click to insert", suggestionX + 6, suggestionY + suggestionH - 12, UiTheme.TEXT_MUTED);
            }
        } else {
            this.suggestionPanelX = -1;
            this.suggestionPanelY = -1;
            this.suggestionPanelW = 0;
            this.suggestionPanelRows = 0;
        }

        int borderColor = this.parseError == null ? 0x60FFFFFF : 0x90FF6060;
        context.fill(this.boxX, this.boxY, this.boxX + this.boxW, this.boxY + 1, borderColor);
        context.fill(this.boxX, this.boxY + this.boxH - 1, this.boxX + this.boxW, this.boxY + this.boxH, borderColor);
        context.fill(this.boxX, this.boxY, this.boxX + 1, this.boxY + this.boxH, borderColor);
        context.fill(this.boxX + this.boxW - 1, this.boxY, this.boxX + this.boxW, this.boxY + this.boxH, borderColor);

        drawSelection(context);
        drawBracketMatch(context);
        drawTextAndCursor(context);
        drawReview(context);
        String keys = "Ctrl+Z/Y undo/redo · Ctrl+Shift+F format · Ctrl+E go to error";
        int keysW = this.font.width(keys);
        if (12 + this.font.width("Target: " + this.targetName) + keysW + 24 < this.width) {
            context.text(this.font, keys, this.width - 12 - keysW, 10, UiTheme.TEXT_MUTED);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        syncEditorTextInput();
    }

    private int gutterWidth() {
        int lines = 1;
        for (int i = 0; i < this.editorText.length(); i++) {
            if (this.editorText.charAt(i) == '\n') {
                lines++;
            }
        }
        return this.font.width(Integer.toString(lines)) + 8;
    }

    private int textX() {
        return this.boxX + 6 + gutterWidth();
    }

    private void drawBracketMatch(GuiGraphicsExtractor context) {
        int at = -1;
        if (this.cursor < this.editorText.length() && "{}[]".indexOf(this.editorText.charAt(this.cursor)) >= 0) {
            at = this.cursor;
        } else if (this.cursor > 0 && "{}[]".indexOf(this.editorText.charAt(this.cursor - 1)) >= 0) {
            at = this.cursor - 1;
        }
        if (at < 0 || SnbtText.insideString(this.editorText, at)) {
            return;
        }
        int partner = SnbtText.matchingBracket(this.editorText, at);
        int color = partner < 0 ? 0x80FF5050 : 0x6050A0FF;
        highlightChar(context, at, color);
        if (partner >= 0) {
            highlightChar(context, partner, color);
        }
    }

    private void highlightChar(GuiGraphicsExtractor context, int index, int color) {
        SnbtText.LineCol pos = SnbtText.lineCol(this.editorText, index);
        int row = pos.line() - this.scrollLine;
        if (row < 0 || row >= visibleLineCapacity()) {
            return;
        }
        int lineStart = index - pos.column();
        int x = textX() + this.font.width(this.editorText.substring(lineStart, index));
        int y = this.boxY + 6 + row * this.lineHeight;
        int w = this.font.width(String.valueOf(this.editorText.charAt(index)));
        context.fill(x - 1, y - 1, x + w + 1, y + this.lineHeight - 1, color);
    }

    private void drawReview(GuiGraphicsExtractor context) {
        if (!this.reviewing) {
            return;
        }
        int w = Math.min(this.boxW - 20, 420);
        int x = this.boxX + this.boxW - w - 8;
        int y = this.boxY + 8;
        int rows = Math.min(this.reviewLines.size(), Math.max(1, (this.boxH - 50) / 10));
        int h = 30 + rows * 10;
        context.fill(x, y, x + w, y + h, 0xF0181820);
        context.fill(x, y, x + w, y + 1, 0xFFFFD37A);
        context.text(this.font, "Review: " + this.reviewLines.size() + " change(s). Press Confirm to save, Esc to keep editing.",
                x + 6, y + 6, 0xFFFFD37A);
        for (int i = 0; i < rows; i++) {
            String line = this.reviewLines.get(i);
            int color = line.startsWith("+") ? 0xFF7CE07C : line.startsWith("-") ? 0xFFFF8080 : 0xFFFFE08A;
            context.text(this.font, this.font.plainSubstrByWidth(line, w - 12), x + 6, y + 20 + i * 10, color);
        }
        if (this.reviewLines.size() > rows) {
            context.text(this.font, "… " + (this.reviewLines.size() - rows) + " more", x + w - 60, y + 6 + 10, UiTheme.TEXT_MUTED);
        }
    }

    /** Changes between the opened and current data, or null when either side isn't a compound. */
    private List<String> describeChanges() {
        CompoundTag before = NbtEditorUtils.parseCompoundOrNull(this.baselineData);
        CompoundTag after = NbtEditorUtils.parseCompoundOrNull(this.editorText);
        if (before == null || after == null) {
            return null;
        }
        return ItemDataDiff.diff(flatten(before), flatten(after));
    }

    private static Map<String, String> flatten(CompoundTag root) {
        Map<String, String> out = new LinkedHashMap<>();
        flattenInto(out, root, "", 0);
        return out;
    }

    private static void flattenInto(Map<String, String> out, CompoundTag compound, String prefix, int depth) {
        for (String key : compound.keySet()) {
            Tag value = compound.get(key);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof CompoundTag child && depth < 4 && !child.isEmpty()) {
                flattenInto(out, child, path, depth + 1);
            } else {
                out.put(path, String.valueOf(value));
            }
        }
    }

    private void endReview() {
        if (this.reviewing) {
            this.reviewing = false;
            this.reviewLines = List.of();
            if (this.saveButton != null) {
                this.saveButton.setMessage(Component.literal("Save"));
            }
        }
    }

    private void formatEditor(boolean compactOnly) {
        String formatted = compactOnly ? SnbtText.compact(this.editorText) : SnbtText.format(this.editorText, INDENT, 60);
        if (formatted.equals(this.editorText)) {
            return;
        }
        pushUndo(false);
        int line = SnbtText.lineCol(this.editorText, this.cursor).line();
        this.editorText = formatted;
        this.cursor = Math.min(indexAtLineStart(line), this.editorText.length());
        this.selectionAnchor = -1;
        onEditorTextChanged();
        ensureCursorVisible();
    }

    /** Remembers the current text before an edit. Consecutive typing within a second is one undo step. */
    private void pushUndo(boolean typing) {
        long now = System.currentTimeMillis();
        if (typing && now - this.lastTypingMs < 1000 && !this.undoStack.isEmpty()) {
            this.lastTypingMs = now;
            return;
        }
        this.lastTypingMs = typing ? now : 0;
        Snapshot top = this.undoStack.peek();
        if (top == null || !top.text().equals(this.editorText)) {
            this.undoStack.push(new Snapshot(this.editorText, this.cursor));
            while (this.undoStack.size() > UNDO_LIMIT) {
                this.undoStack.removeLast();
            }
        }
        this.redoStack.clear();
    }

    private void undo() {
        restore(this.undoStack, this.redoStack);
    }

    private void redo() {
        restore(this.redoStack, this.undoStack);
    }

    private void restore(Deque<Snapshot> from, Deque<Snapshot> to) {
        Snapshot snapshot = from.poll();
        if (snapshot == null) {
            return;
        }
        to.push(new Snapshot(this.editorText, this.cursor));
        this.editorText = snapshot.text();
        this.cursor = Math.min(snapshot.cursor(), this.editorText.length());
        this.selectionAnchor = -1;
        this.lastTypingMs = 0;
        onEditorTextChanged();
        ensureCursorVisible();
    }

    private void drawTextAndCursor(GuiGraphicsExtractor context) {
        int x = textX();
        int y = this.boxY + 6;
        int maxY = this.boxY + this.boxH - 12;
        int cursorX = x;
        int cursorY = y;
        int errorLine = lineFromIndex(this.parseErrorIndex);
        String[] lines = this.editorText.split("\\n", -1);
        clampScrollLine(lines.length);
        int startLine = Math.clamp(this.scrollLine, 0, Math.max(0, lines.length - 1));
        int i = indexAtLineStart(startLine);
        for (int lineIndex = startLine; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (y > maxY) {
                break;
            }
            if (lineIndex == errorLine) {
                context.fill(this.boxX + 2, y - 1, this.boxX + this.boxW - 2, y + this.lineHeight - 1, 0x40202070);
                int column = Math.clamp(this.parseErrorIndex - i, 0, line.length());
                int ex = x + this.font.width(line.substring(0, column));
                context.fill(ex, y + this.lineHeight - 2, ex + 6, y + this.lineHeight - 1, 0xFFFF5050);
            }
            String number = Integer.toString(lineIndex + 1);
            context.text(this.font, number, x - 6 - this.font.width(number), y, lineIndex == lineFromIndex(this.cursor) ? 0xFFBBBBBB : 0xFF666666);
            drawHighlightedLine(context, line, x, y);
            int end = i + line.length();
            if (this.cursor >= i && this.cursor <= end) {
                cursorX = x + this.font.width(line.substring(0, Math.max(0, this.cursor - i)));
                cursorY = y;
            }
            i = end + 1;
            y += this.lineHeight;
        }

        ensureCursorVisible();
        int cursorLine = lineFromIndex(this.cursor);
        if (cursorLine < startLine || cursorLine >= startLine + visibleLineCapacity()) {
            return;
        }

        context.fill(cursorX, cursorY, cursorX + 1, cursorY + (this.lineHeight - 1), 0xFFFFFFFF);
    }

    private void drawSelection(GuiGraphicsExtractor context) {
        if (this.selectionAnchor < 0 || this.selectionAnchor == this.cursor) {
            return;
        }
        int selStart = Math.min(this.selectionAnchor, this.cursor);
        int selEnd = Math.max(this.selectionAnchor, this.cursor);
        int x = textX();
        int y = this.boxY + 6;
        int maxY = this.boxY + this.boxH - 12;
        String[] lines = this.editorText.split("\\n", -1);
        clampScrollLine(lines.length);
        int startLine = Math.clamp(this.scrollLine, 0, Math.max(0, lines.length - 1));
        int i = indexAtLineStart(startLine);
        for (int lineIndex = startLine; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (y > maxY) {
                break;
            }
            int lineStart = i;
            int lineEnd = i + line.length();
            int overlapStart = Math.max(selStart, lineStart);
            int overlapEnd = Math.min(selEnd, lineEnd);
            if (overlapStart < overlapEnd) {
                int sx = x + this.font.width(line.substring(0, overlapStart - lineStart));
                int ex = x + this.font.width(line.substring(0, overlapEnd - lineStart));
                context.fill(sx, y, ex, y + this.lineHeight - 1, 0x80448CCF);
            }
            i = lineEnd + 1;
            y += this.lineHeight;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (tryApplyInlineSuggestionClick(click)) {
            return true;
        }
        if (click.x() < this.boxX || click.x() > this.boxX + this.boxW || click.y() < this.boxY || click.y() > this.boxY + this.boxH) {
            return false;
        }
        if (this.pathField != null && this.pathField.isFocused()) {
            this.pathField.setFocused(false);
            this.setFocused(null);
        }
        syncEditorTextInput();
        this.cursor = cursorFromMouse((int) click.x(), (int) click.y());
        this.selectionAnchor = -1;
        rebuildInlineSuggestions();
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (click.x() < this.boxX || click.x() > this.boxX + this.boxW || click.y() < this.boxY || click.y() > this.boxY + this.boxH) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (this.selectionAnchor < 0) {
            this.selectionAnchor = this.cursor;
        }
        this.cursor = cursorFromMouse((int) click.x(), (int) click.y());
        rebuildInlineSuggestions();
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= this.boxX && mouseX <= this.boxX + this.boxW && mouseY >= this.boxY && mouseY <= this.boxY + this.boxH) {
            int delta = verticalAmount > 0 ? -3 : 3;
            this.scrollLine += delta;
            clampScrollLine(this.editorText.split("\\n", -1).length);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (this.pathField != null && this.pathField.isFocused()) {
            return super.charTyped(input);
        }
        int codepoint = input.codepoint();
        if (codepoint >= 32 && codepoint != 127) {
            if (typeWithPairs((char) codepoint)) {
                return true;
            }
            replaceSelectionOrInsert(new String(Character.toChars(codepoint)));
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();
        if (this.reviewing && keyCode == InputConstants.KEY_ESCAPE) {
            endReview();
            return true;
        }
        if (this.pathField != null && this.pathField.isFocused() && keyCode != InputConstants.KEY_TAB) {
            return super.keyPressed(input);
        }
        var window = this.minecraft == null ? null : this.minecraft.getWindow();
        boolean ctrl = window != null && (InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL));
        boolean shift = window != null && (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));

        if (keyCode == InputConstants.KEY_TAB && this.pathField != null && this.pathField.isFocused()) {
            if (this.activePathSuggestion != null && !this.activePathSuggestion.isBlank()) {
                this.pathField.setValue(this.activePathSuggestion);
                this.pathField.moveCursorTo(this.pathField.getValue().length(), false);
                updatePathSuggestionHint();
                return true;
            }
            return super.keyPressed(input);
        }
        if (keyCode == InputConstants.KEY_TAB && applyInlineSuggestion(0)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB && !shift) {
            replaceSelectionOrInsert(INDENT);
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_Z) {
            if (shift) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_Y) {
            redo();
            return true;
        }
        if (ctrl && shift && keyCode == InputConstants.KEY_F) {
            formatEditor(false);
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_E && this.parseErrorIndex >= 0) {
            this.cursor = Math.min(this.parseErrorIndex, this.editorText.length());
            this.selectionAnchor = -1;
            ensureCursorVisible();
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_BACKSPACE) {
            if (!deleteSelection()) {
                int start = SnbtText.wordLeft(this.editorText, this.cursor);
                if (start < this.cursor) {
                    pushUndo(false);
                    this.editorText = this.editorText.substring(0, start) + this.editorText.substring(this.cursor);
                    this.cursor = start;
                    onEditorTextChanged();
                }
            }
            return true;
        }

        if (ctrl && keyCode == InputConstants.KEY_A) {
            this.selectionAnchor = 0;
            this.cursor = this.editorText.length();
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_C) {
            copySelection();
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_X) {
            copySelection();
            deleteSelection();
            return true;
        }
        if (ctrl && keyCode == InputConstants.KEY_V) {
            if (this.minecraft != null) {
                replaceSelectionOrInsert(this.minecraft.keyboardHandler.getClipboard());
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            deleteSelection();
            String insert = SnbtText.newlineInsert(this.editorText, this.cursor, INDENT);
            int caretOffset = insert.indexOf('\n', 1) > 0 ? insert.indexOf('\n', 1) : insert.length();
            replaceSelectionOrInsert(insert);
            this.cursor -= insert.length() - caretOffset;
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            if (!deleteSelection() && this.cursor > 0) {
                pushUndo(true);
                this.editorText = this.editorText.substring(0, this.cursor - 1) + this.editorText.substring(this.cursor);
                this.cursor--;
                onEditorTextChanged();
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE) {
            if (!deleteSelection() && this.cursor < this.editorText.length()) {
                pushUndo(true);
                this.editorText = this.editorText.substring(0, this.cursor) + this.editorText.substring(this.cursor + 1);
                onEditorTextChanged();
            }
            return true;
        }

        int prev = this.cursor;
        if (keyCode == InputConstants.KEY_LEFT) {
            this.cursor = ctrl ? SnbtText.wordLeft(this.editorText, this.cursor) : Math.max(0, this.cursor - 1);
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            this.cursor = ctrl ? SnbtText.wordRight(this.editorText, this.cursor) : Math.min(this.editorText.length(), this.cursor + 1);
        }
        if (keyCode == InputConstants.KEY_HOME) {
            this.cursor = lineStart(this.cursor);
        }
        if (keyCode == InputConstants.KEY_END) {
            this.cursor = lineEnd(this.cursor);
        }
        if (keyCode == InputConstants.KEY_UP) {
            this.cursor = moveVertical(this.cursor, -1);
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            this.cursor = moveVertical(this.cursor, 1);
        }

        if (this.cursor != prev) {
            if (shift) {
                if (this.selectionAnchor < 0) {
                    this.selectionAnchor = prev;
                }
            } else {
                this.selectionAnchor = -1;
            }
            rebuildInlineSuggestions();
            ensureCursorVisible();
            return true;
        }

        return super.keyPressed(input);
    }

    /** Closes brackets and quotes as you type them and steps over a closer that is already there. */
    private boolean typeWithPairs(char ch) {
        boolean hasSelection = this.selectionAnchor >= 0 && this.selectionAnchor != this.cursor;
        char next = this.cursor < this.editorText.length() ? this.editorText.charAt(this.cursor) : 0;
        boolean inString = SnbtText.insideString(this.editorText, this.cursor);
        if ((ch == '}' || ch == ']' || ch == '"') && next == ch && !hasSelection
                && (ch != '"' || inString)) {
            this.cursor++;
            return true;
        }
        if (inString || hasSelection) {
            return false;
        }
        boolean freeAfter = next == 0 || Character.isWhitespace(next) || ",}]".indexOf(next) >= 0;
        String pair = switch (ch) {
            case '{' -> "{}";
            case '[' -> "[]";
            case '"' -> "\"\"";
            default -> null;
        };
        if (pair == null || !freeAfter) {
            return false;
        }
        replaceSelectionOrInsert(pair);
        this.cursor--;
        return true;
    }

    private void replaceSelectionOrInsert(String s) {
        if (s == null) {
            return;
        }
        pushUndo(s.length() == 1);
        endReview();
        if (deleteSelection()) {
            // deleted selection, cursor already at start
        }
        this.editorText = this.editorText.substring(0, this.cursor) + s + this.editorText.substring(this.cursor);
        this.cursor += s.length();
        onEditorTextChanged();
        ensureCursorVisible();
    }

    private boolean deleteSelection() {
        if (this.selectionAnchor < 0 || this.selectionAnchor == this.cursor) {
            return false;
        }
        int start = Math.min(this.selectionAnchor, this.cursor);
        int end = Math.max(this.selectionAnchor, this.cursor);
        pushUndo(false);
        this.editorText = this.editorText.substring(0, start) + this.editorText.substring(end);
        this.cursor = start;
        this.selectionAnchor = -1;
        onEditorTextChanged();
        ensureCursorVisible();
        return true;
    }

    private void copySelection() {
        if (this.minecraft == null || this.selectionAnchor < 0 || this.selectionAnchor == this.cursor) {
            return;
        }
        int start = Math.min(this.selectionAnchor, this.cursor);
        int end = Math.max(this.selectionAnchor, this.cursor);
        this.minecraft.keyboardHandler.setClipboard(this.editorText.substring(start, end));
    }

    private int cursorFromMouse(int mouseX, int mouseY) {
        int row = this.scrollLine + Math.max(0, (mouseY - (this.boxY + 6)) / this.lineHeight);
        String[] lines = this.editorText.split("\\n", -1);
        row = Math.min(row, lines.length - 1);
        int index = 0;
        for (int i = 0; i < row; i++) {
            index += lines[i].length() + 1;
        }
        String line = lines[row];
        int targetX = mouseX - textX();
        int best = 0;
        for (int i = 0; i <= line.length(); i++) {
            if (this.font.width(line.substring(0, i)) >= targetX) {
                best = i;
                break;
            }
            best = i;
        }
        return Math.min(this.editorText.length(), index + best);
    }

    @Override
    public boolean matchesTarget(String token) {
        return token != null && token.equals(this.targetToken);
    }

    @Override
    public void applySyncedPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        pushUndo(false);
        this.editorText = NbtEditorUtils.prettySnbt(payload);
        this.baselineData = this.editorText;
        this.cursor = Math.min(this.cursor, this.editorText.length());
        this.selectionAnchor = -1;
        this.syncedPayloadApplied = true;
        onEditorTextChanged();
    }

    private void onEditorTextChanged() {
        endReview();
        validateEditorSnbt();
        if (this.supportsPathField) {
            rebuildPathSuggestions();
            updatePathSuggestionHint();
        }
        rebuildInlineSuggestions();
    }

    private void validateEditorSnbt() {
        String path = this.pathField == null ? "" : this.pathField.getValue().trim();
        try {
            validateRawInput(this.editorText, path);
            this.parseError = null;
            this.parseErrorIndex = -1;
        } catch (CommandSyntaxException e) {
            this.parseError = e.getRawMessage().getString();
            this.parseErrorIndex = Math.max(0, e.getCursor());
        }
    }

    private void validateRawInput(String raw, String path) throws CommandSyntaxException {
        if (this.inputValidator != null) {
            this.inputValidator.validate(this, raw, path);
            return;
        }

        if (path == null || path.isBlank()) {
            if (this.requireCompoundRootWhenPathBlank) {
                TagParser.parseCompoundAsArgument(new StringReader(raw));
                return;
            }
            TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(raw));
            return;
        }

        Tag value = TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(raw));
        String valueError = validatePathValue(path, value, raw);
        if (valueError != null) {
            throw new SimpleCommandExceptionType(Component.literal(valueError)).create();
        }
    }

    private String validatePathValue(String path, Tag value, String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        if ("tru".equals(normalized) || "flase".equals(normalized) || "fasle".equals(normalized)) {
            return "Did you mean true/false?";
        }

        CompoundTag initialRoot = parseRootCompound(this.initialData);
        if (initialRoot == null) {
            return null;
        }
        Tag expected = resolvePathValue(initialRoot, path);
        if (expected == null) {
            return null;
        }

        if (NbtEditorUtils.isNumeric(expected)) {
            if ("true".equals(normalized) || "false".equals(normalized)) {
                return "Expected numeric value at path '" + path + "', got boolean";
            }
            if (!NbtEditorUtils.isNumeric(value)) {
                return "Expected numeric value at path '" + path + "', got " + NbtEditorUtils.typeName(value);
            }
            if (NbtEditorUtils.isFloating(expected) && !NbtEditorUtils.isFloating(value)) {
                return "Expected floating-point value at path '" + path + "'";
            }
            return null;
        }

        if (!expected.getClass().equals(value.getClass())) {
            return "Type mismatch at path '" + path + "': expected " + NbtEditorUtils.typeName(expected) + ", got " + NbtEditorUtils.typeName(value);
        }
        return null;
    }

    private Tag resolvePathValue(CompoundTag root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Tag current = root;
        for (String part : parts) {
            if (!(current instanceof CompoundTag compound) || part.isBlank()) {
                return null;
            }
            current = compound.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void rebuildPathSuggestions() {
        this.pathSuggestions.clear();
        if (!this.supportsPathField) {
            return;
        }
        CompoundTag root = parseRootCompound(this.editorText);
        if (root == null) {
            return;
        }
        collectCompoundPaths(root, "", 0);
    }

    private CompoundTag parseRootCompound(String raw) {
        return NbtEditorUtils.parseCompoundOrNull(raw);
    }

    private void collectCompoundPaths(CompoundTag compound, String prefix, int depth) {
        if (depth > 4 || this.pathSuggestions.size() >= MAX_SUGGESTIONS) {
            return;
        }
        for (String key : compound.keySet()) {
            String path = prefix.isEmpty() ? key : (prefix + "." + key);
            this.pathSuggestions.add(path);
            if (this.pathSuggestions.size() >= MAX_SUGGESTIONS) {
                return;
            }
            Tag child = compound.get(key);
            if (child instanceof CompoundTag childCompound) {
                collectCompoundPaths(childCompound, path, depth + 1);
            }
        }
    }

    private void updatePathSuggestionHint() {
        if (this.pathField == null) {
            return;
        }

        String typed = this.pathField.getValue() == null ? "" : this.pathField.getValue().trim();
        this.activePathSuggestion = null;
        String hint = null;

        for (String suggestion : this.pathSuggestions) {
            if (typed.isEmpty()) {
                this.activePathSuggestion = suggestion;
                hint = suggestion;
                break;
            }
            if (suggestion.equals(typed)) {
                this.activePathSuggestion = suggestion;
                hint = null;
                break;
            }
            if (suggestion.startsWith(typed)) {
                this.activePathSuggestion = suggestion;
                hint = suggestion.substring(typed.length());
                break;
            }
        }

        this.pathField.setSuggestion(hint);
    }

    private void rebuildInlineSuggestions() {
        if (this.inlineSuggestionProvider == null) {
            this.inlineSuggestions = null;
            return;
        }
        this.inlineSuggestions = this.inlineSuggestionProvider.suggest(this.editorText, this.cursor);
        if (this.inlineSuggestions != null && (this.inlineSuggestions.suggestions() == null || this.inlineSuggestions.suggestions().isEmpty())) {
            this.inlineSuggestions = null;
        }
    }

    private boolean applyInlineSuggestion(int index) {
        if (this.inlineSuggestions == null || this.inlineSuggestions.suggestions() == null) {
            return false;
        }
        if (index < 0 || index >= this.inlineSuggestions.suggestions().size()) {
            return false;
        }
        String suggestion = this.inlineSuggestions.suggestions().get(index);
        pushUndo(false);
        int start = Math.clamp(this.inlineSuggestions.start(), 0, this.editorText.length());
        int end = Math.clamp(this.inlineSuggestions.end(), start, this.editorText.length());
        this.editorText = this.editorText.substring(0, start) + suggestion + this.editorText.substring(end);
        this.cursor = start + suggestion.length();
        this.selectionAnchor = -1;
        onEditorTextChanged();
        ensureCursorVisible();
        return true;
    }

    private boolean tryApplyInlineSuggestionClick(MouseButtonEvent click) {
        if (this.supportsPathField || this.inlineSuggestions == null || this.suggestionPanelX < 0 || click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (click.x() < this.suggestionPanelX || click.x() > this.suggestionPanelX + this.suggestionPanelW) {
            return false;
        }
        if (click.y() < this.suggestionPanelY || click.y() > this.suggestionPanelY + 6 + (this.suggestionPanelRows * this.suggestionPanelRowHeight)) {
            return false;
        }
        int row = Math.max(0, ((int) click.y() - this.suggestionPanelY - 4) / this.suggestionPanelRowHeight);
        return applyInlineSuggestion(row);
    }

    private String buildSuggestionPreview() {
        if (this.pathSuggestions.isEmpty()) {
            return "";
        }
        int count = Math.min(4, this.pathSuggestions.size());
        StringBuilder builder = new StringBuilder("Path suggestions: ");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(this.pathSuggestions.get(i));
        }
        if (this.pathSuggestions.size() > count) {
            builder.append(" ...");
        }
        return builder.toString();
    }

    private int lineFromIndex(int index) {
        if (index < 0) {
            return -1;
        }
        int line = 0;
        for (int i = 0; i < Math.min(index, this.editorText.length()); i++) {
            if (this.editorText.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private void drawHighlightedLine(GuiGraphicsExtractor context, String line, int x, int y) {
        int cursorX = x;
        for (SnbtText.Token token : SnbtText.tokenize(line)) {
            String text = line.substring(token.start(), token.end());
            cursorX = drawToken(context, text, cursorX, y, colorFor(token.kind()));
        }
    }

    private int drawToken(GuiGraphicsExtractor context, String token, int x, int y, int color) {
        context.text(this.font, token, x, y, color);
        return x + this.font.width(token);
    }

    private static int colorFor(SnbtText.Kind kind) {
        return switch (kind) {
            case STRING -> 0xFFFFD37A;
            case KEY -> 0xFF79C8FF;
            case NUMBER -> 0xFF86E17E;
            case BOOLEAN -> 0xFFB18CFF;
            case PUNCTUATION -> 0xFF80D8FF;
            default -> 0xFFE8E8E8;
        };
    }

    private int lineStart(int pos) {
        int p = Math.clamp(pos, 0, this.editorText.length());
        while (p > 0 && this.editorText.charAt(p - 1) != '\n') {
            p--;
        }
        return p;
    }

    private int lineEnd(int pos) {
        int p = Math.clamp(pos, 0, this.editorText.length());
        while (p < this.editorText.length() && this.editorText.charAt(p) != '\n') {
            p++;
        }
        return p;
    }

    private int moveVertical(int pos, int deltaLine) {
        int start = lineStart(pos);
        int col = pos - start;
        int targetStart = start;
        if (deltaLine < 0) {
            if (start == 0) {
                return pos;
            }
            targetStart = lineStart(start - 1);
        } else {
            int end = lineEnd(pos);
            if (end >= this.editorText.length()) {
                return pos;
            }
            targetStart = end + 1;
        }
        int targetEnd = lineEnd(targetStart);
        return Math.min(targetEnd, targetStart + col);
    }

    private int indexAtLineStart(int lineIndex) {
        if (lineIndex <= 0) {
            return 0;
        }
        int line = 0;
        for (int i = 0; i < this.editorText.length(); i++) {
            if (line >= lineIndex) {
                return i;
            }
            if (this.editorText.charAt(i) == '\n') {
                line++;
            }
        }
        return this.editorText.length();
    }

    private int visibleLineCapacity() {
        return Math.max(1, (this.boxH - 12) / this.lineHeight);
    }

    private void clampScrollLine(int totalLines) {
        int maxScroll = Math.max(0, totalLines - visibleLineCapacity());
        this.scrollLine = Math.clamp(this.scrollLine, 0, maxScroll);
    }

    private void ensureCursorVisible() {
        int totalLines = this.editorText.split("\\n", -1).length;
        int line = lineFromIndex(this.cursor);
        int visible = visibleLineCapacity();
        if (line < this.scrollLine) {
            this.scrollLine = line;
        } else if (line >= this.scrollLine + visible) {
            this.scrollLine = line - visible + 1;
        }
        clampScrollLine(totalLines);
    }

}
