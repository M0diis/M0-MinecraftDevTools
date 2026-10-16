package me.m0dii.modules.getdata;


import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.utils.NbtEditorUtils;
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

import java.util.ArrayList;
import java.util.List;

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
        this.editorText = this.initialData;
        this.cursor = this.editorText.length();
        this.selectionAnchor = -1;
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

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveChanges())
                .bounds(12, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Copy"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(this.editorText);
                    }
                }).bounds(76, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width - 72, this.height - BUTTON_BAR_HEIGHT, 60, 20)
                .build());
    }

    private void saveChanges() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        String raw = this.editorText;
        try {
            String path = this.pathField == null ? "" : this.pathField.getValue().trim();
            validateRawInput(raw, path);
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
        drawTextAndCursor(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawTextAndCursor(GuiGraphicsExtractor context) {
        int x = this.boxX + 6;
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
            }
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
        int x = this.boxX + 6;
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
            replaceSelectionOrInsert(new String(Character.toChars(codepoint)));
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();
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
            replaceSelectionOrInsert("\n");
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            if (!deleteSelection() && this.cursor > 0) {
                this.editorText = this.editorText.substring(0, this.cursor - 1) + this.editorText.substring(this.cursor);
                this.cursor--;
                onEditorTextChanged();
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE) {
            if (!deleteSelection() && this.cursor < this.editorText.length()) {
                this.editorText = this.editorText.substring(0, this.cursor) + this.editorText.substring(this.cursor + 1);
                onEditorTextChanged();
            }
            return true;
        }

        int prev = this.cursor;
        if (keyCode == InputConstants.KEY_LEFT) {
            this.cursor = Math.max(0, this.cursor - 1);
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            this.cursor = Math.min(this.editorText.length(), this.cursor + 1);
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

    private void replaceSelectionOrInsert(String s) {
        if (s == null) {
            return;
        }
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
        int targetX = mouseX - (this.boxX + 6);
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
        this.editorText = NbtEditorUtils.prettySnbt(payload);
        this.cursor = Math.min(this.cursor, this.editorText.length());
        this.selectionAnchor = -1;
        this.syncedPayloadApplied = true;
        onEditorTextChanged();
    }

    private void onEditorTextChanged() {
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
        int i = 0;
        while (i < line.length()) {
            char ch = line.charAt(i);
            if (ch == '"') {
                int end = i + 1;
                while (end < line.length()) {
                    char c = line.charAt(end);
                    if (c == '"' && line.charAt(end - 1) != '\\') {
                        end++;
                        break;
                    }
                    end++;
                }
                cursorX = drawToken(context, line.substring(i, Math.min(end, line.length())), cursorX, y, 0xFFFFD37A);
                i = end;
                continue;
            }

            if ("{}[]:,".indexOf(ch) >= 0) {
                cursorX = drawToken(context, String.valueOf(ch), cursorX, y, 0xFF80D8FF);
                i++;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                int end = i + 1;
                while (end < line.length() && Character.isWhitespace(line.charAt(end))) {
                    end++;
                }
                cursorX = drawToken(context, line.substring(i, end), cursorX, y, 0xFFE8E8E8);
                i = end;
                continue;
            }

            int end = i + 1;
            while (end < line.length() && !Character.isWhitespace(line.charAt(end)) && "{}[]:,\"".indexOf(line.charAt(end)) < 0) {
                end++;
            }

            String token = line.substring(i, end);
            int color = classifyTokenColor(line, i, end, token);
            cursorX = drawToken(context, token, cursorX, y, color);
            i = end;
        }
    }

    private int drawToken(GuiGraphicsExtractor context, String token, int x, int y, int color) {
        context.text(this.font, token, x, y, color);
        return x + this.font.width(token);
    }

    private int classifyTokenColor(String line, int tokenStart, int tokenEnd, String token) {
        if (token.matches("-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?[bBsSlLfFdD]?")) {
            return 0xFF86E17E;
        }
        if ("true".equals(token) || "false".equals(token)) {
            return 0xFFB18CFF;
        }

        int cursor = tokenEnd;
        while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
            cursor++;
        }
        if (cursor < line.length() && line.charAt(cursor) == ':') {
            return 0xFF79C8FF;
        }

        return 0xFFE8E8E8;
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
