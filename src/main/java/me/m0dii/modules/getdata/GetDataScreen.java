package me.m0dii.modules.getdata;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.gui.local.FormPanel;
import me.m0dii.gui.local.ListPanel;
import me.m0dii.gui.local.UiTheme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GetDataScreen extends Screen {
    private final Screen parent;
    private final String targetName;
    private final String targetToken;
    private final String initialData;
    private TextFieldWidget pathField;

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
    private boolean syncedPayloadApplied = false;

    private GetDataScreen(Screen parent, String targetName, String targetToken, String initialData) {
        super(Text.literal("GetData Editor"));
        this.parent = parent;
        this.targetName = targetName;
        this.targetToken = targetToken;
        this.initialData = prettySnbt(initialData);
    }

    public static Screen create(Screen parent, String targetName, String targetToken, String initialData) {
        return new GetDataScreen(parent, targetName, targetToken, initialData);
    }

    @Override
    protected void init() {
        this.boxX = 12;
        this.boxY = 34;
        this.boxW = this.width - 24;
        this.boxH = this.height - 84;
        this.editorText = this.initialData;
        this.cursor = this.editorText.length();
        this.selectionAnchor = -1;
        validateEditorSnbt();
        rebuildPathSuggestions();

        this.pathField = new TextFieldWidget(this.textRenderer, 12, this.height - 62, this.width - 24, 18, Text.literal("Path"));
        this.pathField.setMaxLength(120);
        this.pathField.setText("");
        this.pathField.setChangedListener(value -> {
            updatePathSuggestionHint();
            validateEditorSnbt();
        });
        this.addDrawableChild(this.pathField);
        updatePathSuggestionHint();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveChanges())
                .dimensions(12, this.height - 38, 60, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> {
                    if (this.client != null) {
                        this.client.keyboard.setClipboard(this.editorText);
                    }
                }).dimensions(76, this.height - 38, 60, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width - 72, this.height - 38, 60, 20)
                .build());
    }

    private void saveChanges() {
        if (this.client == null || this.client.player == null) {
            return;
        }

        String raw = this.editorText;
        try {
            String path = this.pathField == null ? "" : this.pathField.getText().trim();
            if (path.isEmpty()) {
                NbtCompound merged = StringNbtReader.readCompoundAsArgument(new StringReader(raw));

                if (this.targetToken.startsWith("block ")) {
                    applyBlockStateAndNbtMerge(merged);
                    return;
                }

                String command = "data merge " + this.targetToken + " " + merged;
                this.client.player.networkHandler.sendChatCommand(command);
                this.client.player.sendMessage(Text.literal("[GetData] Sent: /" + command), false);
            } else {
                // /data modify ... set value accepts any NBT element
                NbtElement value = StringNbtReader.fromOps(NbtOps.INSTANCE).readAsArgument(new StringReader(raw));
                String command = "data modify " + this.targetToken + " " + path + " set value " + value;
                this.client.player.networkHandler.sendChatCommand(command);
                this.client.player.sendMessage(Text.literal("[GetData] Sent: /" + command), false);
            }
        } catch (CommandSyntaxException e) {
            this.client.player.sendMessage(Text.literal("[GetData] Invalid NBT/SNBT: " + e.getMessage()), false);
        }
    }

    private void applyBlockStateAndNbtMerge(NbtCompound merged) {
        if (this.client == null || this.client.player == null) {
            return;
        }
        String blockCoords = this.targetToken.substring("block ".length()).trim();

        String id = merged.contains("id") ? merged.getString("id").orElse("") : "";
        NbtCompound props = merged.contains("Properties") && merged.get("Properties") instanceof NbtCompound c ? c : null;

        if (!id.isBlank()) {
            String stateSuffix = toBlockStateSuffix(props);
            String setblock = "setblock " + blockCoords + " " + id + stateSuffix;
            this.client.player.networkHandler.sendChatCommand(setblock);
            this.client.player.sendMessage(Text.literal("[GetData] Sent: /" + setblock), false);
        }

        NbtCompound forMerge = merged.copy();
        forMerge.remove("id");
        forMerge.remove("Properties");
        forMerge.remove("x");
        forMerge.remove("y");
        forMerge.remove("z");
        if (!forMerge.isEmpty()) {
            String mergeCmd = "data merge " + this.targetToken + " " + forMerge;
            this.client.player.networkHandler.sendChatCommand(mergeCmd);
            this.client.player.sendMessage(Text.literal("[GetData] Sent: /" + mergeCmd), false);
        }
    }

    private static String toBlockStateSuffix(NbtCompound properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (String key : properties.getKeys()) {
            String value = properties.getString(key).orElse("");
            if (value.isBlank()) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(key).append('=').append(value.replace('"', ' ').trim());
            first = false;
        }
        builder.append(']');
        return first ? "" : builder.toString();
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
        context.drawTextWithShadow(this.textRenderer, "Target: " + this.targetName, 12, 10, UiTheme.TEXT);
        context.drawTextWithShadow(this.textRenderer, "Edit SNBT. Optional path enables /data modify ... set value. Empty path uses /data merge.", 12, 22, UiTheme.TEXT_MUTED);
        int statusColor = this.parseError == null ? 0xFF70D070 : 0xFFFF7070;
        String status = this.parseError == null ? "SNBT valid" : "SNBT error: " + this.parseError;
        if (this.syncedPayloadApplied && this.parseError == null) {
            status = status + " (server sync)";
        }
        context.drawTextWithShadow(this.textRenderer, status, 12, this.height - 76, statusColor);

        FormPanel editorPanel = new FormPanel(this.boxX, this.boxY, this.boxW, this.boxH);
        editorPanel.render(context);

        List<String> suggestionRows = this.pathSuggestions.stream().limit(6).map(s -> "- " + s).toList();
        if (!suggestionRows.isEmpty()) {
            int suggestionW = 208;
            int suggestionX = this.boxX + this.boxW - suggestionW - 4;
            int suggestionY = this.boxY + 6;
            int suggestionH = Math.clamp(this.boxH - 12, 42, 96);
            ListPanel listPanel = new ListPanel(suggestionX, suggestionY, suggestionW, suggestionH);
            listPanel.render(context, this.textRenderer, suggestionRows, -1, 0, 10);
        }

        int borderColor = this.parseError == null ? 0x60FFFFFF : 0x90FF6060;
        context.fill(this.boxX, this.boxY, this.boxX + this.boxW, this.boxY + 1, borderColor);
        context.fill(this.boxX, this.boxY + this.boxH - 1, this.boxX + this.boxW, this.boxY + this.boxH, borderColor);
        context.fill(this.boxX, this.boxY, this.boxX + 1, this.boxY + this.boxH, borderColor);
        context.fill(this.boxX + this.boxW - 1, this.boxY, this.boxX + this.boxW, this.boxY + this.boxH, borderColor);

        drawSelection(context);
        drawTextAndCursor(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawTextAndCursor(DrawContext context) {
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
                cursorX = x + this.textRenderer.getWidth(line.substring(0, Math.max(0, this.cursor - i)));
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

    private void drawSelection(DrawContext context) {
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
                int sx = x + this.textRenderer.getWidth(line.substring(0, overlapStart - lineStart));
                int ex = x + this.textRenderer.getWidth(line.substring(0, overlapEnd - lineStart));
                context.fill(sx, y, ex, y + this.lineHeight - 1, 0x80448CCF);
            }
            i = lineEnd + 1;
            y += this.lineHeight;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0) {
            return false;
        }
        if (click.x() < this.boxX || click.x() > this.boxX + this.boxW || click.y() < this.boxY || click.y() > this.boxY + this.boxH) {
            return false;
        }
        this.cursor = cursorFromMouse((int) click.x(), (int) click.y());
        this.selectionAnchor = -1;
        ensureCursorVisible();
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() != 0) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (click.x() < this.boxX || click.x() > this.boxX + this.boxW || click.y() < this.boxY || click.y() > this.boxY + this.boxH) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        if (this.selectionAnchor < 0) {
            this.selectionAnchor = this.cursor;
        }
        this.cursor = cursorFromMouse((int) click.x(), (int) click.y());
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
    public boolean charTyped(CharInput input) {
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
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();
        if (this.pathField != null && this.pathField.isFocused() && keyCode != GLFW.GLFW_KEY_TAB) {
            return super.keyPressed(input);
        }
        var window = this.client == null ? null : this.client.getWindow();
        boolean ctrl = window != null && (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL));
        boolean shift = window != null && (InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT));

        if (keyCode == GLFW.GLFW_KEY_TAB && this.pathField != null && this.pathField.isFocused()) {
            if (this.activePathSuggestion != null && !this.activePathSuggestion.isBlank()) {
                this.pathField.setText(this.activePathSuggestion);
                this.pathField.setCursor(this.pathField.getText().length(), false);
                updatePathSuggestionHint();
                return true;
            }
            return super.keyPressed(input);
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            this.selectionAnchor = 0;
            this.cursor = this.editorText.length();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            copySelection();
            deleteSelection();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            if (this.client != null) {
                replaceSelectionOrInsert(this.client.keyboard.getClipboard());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            replaceSelectionOrInsert("\n");
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!deleteSelection() && this.cursor > 0) {
                this.editorText = this.editorText.substring(0, this.cursor - 1) + this.editorText.substring(this.cursor);
                this.cursor--;
                onEditorTextChanged();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (!deleteSelection() && this.cursor < this.editorText.length()) {
                this.editorText = this.editorText.substring(0, this.cursor) + this.editorText.substring(this.cursor + 1);
                onEditorTextChanged();
            }
            return true;
        }

        int prev = this.cursor;
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.cursor = Math.max(0, this.cursor - 1);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.cursor = Math.min(this.editorText.length(), this.cursor + 1);
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            this.cursor = lineStart(this.cursor);
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            this.cursor = lineEnd(this.cursor);
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.cursor = moveVertical(this.cursor, -1);
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
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
        if (this.client == null || this.selectionAnchor < 0 || this.selectionAnchor == this.cursor) {
            return;
        }
        int start = Math.min(this.selectionAnchor, this.cursor);
        int end = Math.max(this.selectionAnchor, this.cursor);
        this.client.keyboard.setClipboard(this.editorText.substring(start, end));
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
            if (this.textRenderer.getWidth(line.substring(0, i)) >= targetX) {
                best = i;
                break;
            }
            best = i;
        }
        return Math.min(this.editorText.length(), index + best);
    }

    public boolean matchesTarget(String token) {
        return token != null && token.equals(this.targetToken);
    }

    public void applySyncedPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        this.editorText = prettySnbt(payload);
        this.cursor = Math.min(this.cursor, this.editorText.length());
        this.selectionAnchor = -1;
        this.syncedPayloadApplied = true;
        onEditorTextChanged();
    }

    private void onEditorTextChanged() {
        validateEditorSnbt();
        rebuildPathSuggestions();
        updatePathSuggestionHint();
    }

    private void validateEditorSnbt() {
        String path = this.pathField == null ? "" : this.pathField.getText().trim();
        try {
            if (path.isEmpty()) {
                StringNbtReader.readCompoundAsArgument(new StringReader(this.editorText));
            } else {
                NbtElement value = StringNbtReader.fromOps(NbtOps.INSTANCE).readAsArgument(new StringReader(this.editorText));
                String valueError = validatePathValue(path, value, this.editorText);
                if (valueError != null) {
                    this.parseError = valueError;
                    this.parseErrorIndex = 0;
                    return;
                }
            }
            this.parseError = null;
            this.parseErrorIndex = -1;
        } catch (CommandSyntaxException e) {
            this.parseError = e.getRawMessage().getString();
            this.parseErrorIndex = Math.max(0, e.getCursor());
        }
    }

    private String validatePathValue(String path, NbtElement value, String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        if ("tru".equals(normalized) || "flase".equals(normalized) || "fasle".equals(normalized)) {
            return "Did you mean true/false?";
        }

        NbtCompound initialRoot = parseRootCompound(this.initialData);
        if (initialRoot == null) {
            return null;
        }
        NbtElement expected = resolvePathValue(initialRoot, path);
        if (expected == null) {
            return null;
        }

        if (isNumericNbt(expected)) {
            if ("true".equals(normalized) || "false".equals(normalized)) {
                return "Expected numeric value at path '" + path + "', got boolean";
            }
            if (!isNumericNbt(value)) {
                return "Expected numeric value at path '" + path + "', got " + typeName(value);
            }
            if ((expected instanceof NbtFloat || expected instanceof NbtDouble) && !isFloatingNbt(value)) {
                return "Expected floating-point value at path '" + path + "'";
            }
            return null;
        }

        if (!expected.getClass().equals(value.getClass())) {
            return "Type mismatch at path '" + path + "': expected " + typeName(expected) + ", got " + typeName(value);
        }
        return null;
    }

    private NbtElement resolvePathValue(NbtCompound root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        NbtElement current = root;
        for (String part : parts) {
            if (!(current instanceof NbtCompound compound) || part.isBlank()) {
                return null;
            }
            current = compound.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static boolean isNumericNbt(NbtElement element) {
        return element instanceof AbstractNbtNumber;
    }

    private static boolean isFloatingNbt(NbtElement element) {
        return element instanceof NbtFloat || element instanceof NbtDouble;
    }

    private static String typeName(NbtElement element) {
        return switch (element) {
            case null -> "null";
            case NbtString nbtString -> "string";
            case NbtCompound nbtCompound -> "compound";
            case NbtByte nbtByte -> "byte/boolean";
            case NbtShort nbtShort -> "short";
            case NbtInt nbtInt -> "int";
            case NbtLong nbtLong -> "long";
            case NbtFloat nbtFloat -> "float";
            case NbtDouble nbtDouble -> "double";
            default -> element.getClass().getSimpleName();
        };
    }

    private void rebuildPathSuggestions() {
        this.pathSuggestions.clear();
        NbtCompound root = parseRootCompound(this.editorText);
        if (root == null) {
            return;
        }
        collectCompoundPaths(root, "", 0);
    }

    private NbtCompound parseRootCompound(String raw) {
        try {
            return StringNbtReader.readCompoundAsArgument(new StringReader(raw));
        } catch (CommandSyntaxException ignored) {
            return null;
        }
    }

    private void collectCompoundPaths(NbtCompound compound, String prefix, int depth) {
        if (depth > 4 || this.pathSuggestions.size() >= MAX_SUGGESTIONS) {
            return;
        }
        for (String key : compound.getKeys()) {
            String path = prefix.isEmpty() ? key : (prefix + "." + key);
            this.pathSuggestions.add(path);
            if (this.pathSuggestions.size() >= MAX_SUGGESTIONS) {
                return;
            }
            NbtElement child = compound.get(key);
            if (child instanceof NbtCompound childCompound) {
                collectCompoundPaths(childCompound, path, depth + 1);
            }
        }
    }

    private void updatePathSuggestionHint() {
        if (this.pathField == null) {
            return;
        }

        String typed = this.pathField.getText() == null ? "" : this.pathField.getText().trim();
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

    private void drawHighlightedLine(DrawContext context, String line, int x, int y) {
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

    private int drawToken(DrawContext context, String token, int x, int y, int color) {
        context.drawTextWithShadow(this.textRenderer, token, x, y, color);
        return x + this.textRenderer.getWidth(token);
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
                    appendIndent(out, indent);
                }
                case '}', ']' -> {
                    out.append('\n');
                    indent = Math.max(0, indent - 1);
                    appendIndent(out, indent);
                    out.append(ch);
                }
                case ',' -> {
                    out.append(ch).append('\n');
                    appendIndent(out, indent);
                }
                case ':' -> out.append(": ");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }

    private static void appendIndent(StringBuilder builder, int indent) {
        builder.repeat("  ", Math.max(0, indent));
    }
}
