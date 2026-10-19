package me.m0dii.modules.scripting.gui;

import com.mojang.blaze3d.platform.InputConstants;
import groovy.lang.GroovyShell;
import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.scripting.*;
import me.m0dii.modules.scripting.ide.*;
import me.m0dii.modules.scripting.ide.ScriptNames.*;
import me.m0dii.modules.scripting.ide.ScriptValidation.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.*;

import static me.m0dii.modules.scripting.ide.ScriptCompletions.*;
import static me.m0dii.modules.scripting.ide.ScriptText.*;

public class ScriptEditorScreen extends Screen implements ThemedScreen {
    /** Editor-only colours derived from the theme accent (selection and the current line). */
    private static final int SELECTION = 0x664C8BF5;
    private static final int CURRENT_LINE = 0x144C8BF5;
    /** Recently opened scripts for the Ctrl+E switcher. */
    private static final me.m0dii.modules.scripting.ide.RecentFiles RECENT =
            new me.m0dii.modules.scripting.ide.RecentFiles(java.nio.file.Paths.get("config/m0-dev-tools/script-recent.txt"));

    private static final int PAD = 10;
    private static final int TAB_H = 22;
    private static final int TAB_W = 96;
    private static final int CODE_LINE_H = 10;
    private static final int SCRIPT_ROW_H = 14;
    private static final int SCRIPT_HEADER_H = 18;
    private static final int SCRIPT_SEARCH_TOP = 16;
    private static final int SCRIPT_FIELD_H = 16;
    private static final int SCRIPT_RENAME_TOP = 36;
    private static final int SCRIPT_HELP_TOP = 56;
    private static final int SCRIPT_BODY_TOP = 72;
    private static final int DIAG_ROW_H = 11;
    private static final int HISTORY_LIMIT = 180;
    private static final int SUGGESTION_ROW_H = 11;
    private enum UiTab {MAIN, DOCS, SCRIPTS}

    private enum SortMode {NAME_ASC, NAME_DESC, LANGUAGE}

    private enum FilterMode {ALL, GROOVY, KOTLIN, JAVASCRIPT}

    private record Rect(int x, int y, int w, int h) {
        private boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record Snapshot(String text, int cursor, int anchor, int scrollLine, int viewLeft) {
    }

    private static final class EditorState {
        private String text = "";
        private int cursor = 0;
        private int selectionAnchor = -1;
        private int scrollLine = 0;
        private int viewLeft = 0;
        private int preferredColumn = -1;
        private boolean focused = false;
        private boolean dragging = false;
        private int dragStartCursor = 0;
        private int clickCount = 0;
        private long lastClickAt = 0L;
        private int lastClickCursor = -1;
    }

    /** One open file: its own text, cursor, scroll and undo history. */
    private static final class Buffer {
        private String name;
        private final EditorState state = new EditorState();
        private final Deque<Snapshot> undo = new ArrayDeque<>();
        private final Deque<Snapshot> redo = new ArrayDeque<>();
        /** Text as last saved/loaded; null for a file that was never saved. */
        private String savedText;

        private Buffer(String name, String text, String savedText) {
            this.name = name;
            this.state.text = text;
            this.savedText = savedText;
        }

        private boolean dirty() {
            return savedText == null ? !state.text.isEmpty() : !savedText.equals(state.text);
        }
    }

    private enum FindMode {NONE, FIND, REPLACE, GOTO}

    private static final int FILE_TAB_H = 14;
    private static final int STATUS_H = 11;

    /** Open files survive closing the editor (until the game quits), like an IDE session. */
    private static final List<Buffer> SESSION = new ArrayList<>();
    private static Buffer sessionActive;
    private static boolean sessionCaseSensitive;
    private static boolean sessionWholeWord;

    private final Screen parent;
    private final String initialScript;
    private Buffer active;
    private Buffer pendingClose;
    private long closeWarnedAtEdit = Long.MIN_VALUE;
    private long globalEdits;

    private FindMode findMode = FindMode.NONE;
    private String findQuery = "";
    private String replaceText = "";
    private String gotoText = "";
    private boolean replaceFocused;
    private boolean findFocused;
    private List<TextSearch.Match> findMatches = List.of();
    private int findCurrent = -1;
    private long findMatchesVersion = -1;
    private Object findMatchesBuffer;

    private boolean outlineVisible;
    private String outlineQuery = "";
    private int outlineSelected;
    private List<ScriptOutline.Symbol> outlineItems = List.of();
    /** The outline popup lists recent files (Ctrl+E) instead of symbols (Ctrl+O). */
    private boolean recentMode;

    private List<ScriptSyntax.State> cachedStates = List.of();
    private long cachedStatesVersion = -1;
    private Object cachedStatesBuffer;

    private UiTab tab = UiTab.SCRIPTS;
    private String currentScriptName = ScriptNames.DEFAULT_SCRIPT_NAME;
    private EditorState editor = new EditorState();

    private final List<String> docsLines = new ArrayList<>();
    private final List<String> allScripts = new ArrayList<>();
    private final List<String> visibleScripts = new ArrayList<>();
    private int selectedScriptIndex = -1;
    private int scriptsScroll = 0;
    private String scriptSearch = "";
    private boolean scriptsSearchFocused = false;
    private String renameDraft = "";
    private boolean renameFieldFocused = false;
    private SortMode sortMode = SortMode.NAME_ASC;
    private FilterMode filterMode = FilterMode.ALL;

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int diagnosticsScroll = 0;
    /** Bottom panel shows the Console tab instead of Problems. */
    private boolean consoleTab = false;
    private int consoleScroll = 0;
    private long consoleSeenVersion = -1;
    private int docsScroll = 0;
    private String outputText = "";
    private long lastEditAt = 0L;
    private long lastValidationAt = 0L;
    private long editVersion = 0L;
    private long lastAutoValidatedEditVersion = -1L;
    private long lastStrictValidatedEditVersion = -1L;

    private final List<Suggestion> suggestions = new ArrayList<>();
    private final List<DisplaySuggestion> visibleSuggestions = new ArrayList<>();
    private final Map<String, Boolean> expandedMethodGroups = new LinkedHashMap<>();
    private final boolean placeholderSuggestionsInScripts = false;
    private boolean suggestionsVisible = false;
    private int suggestionSelected = 0;
    private int suggestionScroll = 0;
    private int suggestionReplaceStart = -1;
    private int suggestionReplaceEnd = -1;

    private Deque<Snapshot> undo = new ArrayDeque<>();
    private Deque<Snapshot> redo = new ArrayDeque<>();

    public ScriptEditorScreen() {
        this(null, null);
    }

    /** Opens the editor on {@code scriptName} (may be null) and returns to {@code parent} when closed. */
    public ScriptEditorScreen(Screen parent, String scriptName) {
        super(Component.literal("Scripting"));
        this.parent = parent;
        this.initialScript = scriptName;
    }

    @Override
    protected void init() {
        rebuildDocs();
        refreshScripts();
        if (active != null) {
            // Window resized: keep the open file as it is.
            return;
        }
        restoreSession();
        if (initialScript != null && ScriptStorage.exists(initialScript)) {
            openScriptByName(initialScript);
            tab = UiTab.MAIN;
            editor.focused = true;
        } else if (active == null && !visibleScripts.isEmpty()) {
            openScriptByVisibleIndex(Math.max(0, selectedScriptIndex), true);
        }
        if (active == null) {
            activate(new Buffer(DEFAULT_SCRIPT_NAME, "", null));
        }
    }

    // ---- open files ----------------------------------------------------------------------------------------

    private void restoreSession() {
        // Drop saved files that were deleted outside the editor; keep unsaved work.
        SESSION.removeIf(buffer -> !buffer.dirty() && buffer.savedText != null && !ScriptStorage.exists(buffer.name));
        if (sessionActive != null && SESSION.contains(sessionActive)) {
            activate(sessionActive);
        } else if (!SESSION.isEmpty()) {
            activate(SESSION.getFirst());
        }
    }

    private void activate(Buffer buffer) {
        if (!SESSION.contains(buffer)) {
            SESSION.add(buffer);
        }
        active = buffer;
        sessionActive = buffer;
        if (buffer.savedText != null) {
            RECENT.touch(buffer.name);
        }
        editor = buffer.state;
        undo = buffer.undo;
        redo = buffer.redo;
        currentScriptName = buffer.name;
        renameDraft = buffer.name;
        diagnostics.clear();
        hideSuggestions();
        outlineVisible = false;
        int idx = visibleScripts.indexOf(buffer.name);
        if (idx >= 0) {
            selectedScriptIndex = idx;
        }
        clampEditor();
    }

    private Buffer findBuffer(String name) {
        for (Buffer buffer : SESSION) {
            if (buffer.name.equals(name)) {
                return buffer;
            }
        }
        return null;
    }

    /** Switches to the open file, or loads it from disk into a new tab. */
    private void openScriptByName(String name) {
        Buffer existing = findBuffer(name);
        if (existing != null) {
            activate(existing);
            return;
        }
        try {
            String text = normalize(ScriptStorage.readScript(name));
            Buffer buffer = new Buffer(name, text, text);
            activate(buffer);
            outputText = "Opened " + name;
        } catch (IOException e) {
            outputText = "Load error: " + e.getMessage();
        }
    }

    /** Closes a file tab. Unsaved changes need a second click (or Ctrl+W) to discard. */
    private void closeBuffer(Buffer buffer) {
        if (buffer.dirty() && pendingClose != buffer) {
            pendingClose = buffer;
            outputText = buffer.name + " has unsaved changes. Close again to discard them, or Ctrl+S to save.";
            return;
        }
        pendingClose = null;
        int index = SESSION.indexOf(buffer);
        SESSION.remove(buffer);
        if (buffer == active) {
            active = null;
            if (!SESSION.isEmpty()) {
                activate(SESSION.get(Math.clamp(index, 0, SESSION.size() - 1)));
            } else {
                activate(new Buffer(DEFAULT_SCRIPT_NAME, "", null));
            }
        }
        outputText = "Closed " + buffer.name;
    }

    private void cycleBuffer(int direction) {
        if (SESSION.size() < 2 || active == null) {
            return;
        }
        int index = SESSION.indexOf(active);
        activate(SESSION.get(Math.floorMod(index + direction, SESSION.size())));
    }

    private int dirtyCount() {
        int count = 0;
        for (Buffer buffer : SESSION) {
            if (buffer.dirty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void tick() {
        if (!editor.focused) {
            return;
        }
        long now = System.currentTimeMillis();
        if (editVersion != lastAutoValidatedEditVersion
                && editVersion != lastStrictValidatedEditVersion
                && now - lastEditAt >= 350L
                && now - lastValidationAt >= 300L) {
            diagnostics.clear();
            diagnostics.addAll(validateSyntax(editor.text, inferLanguage(currentScriptName), false));
            lastValidationAt = now;
            lastAutoValidatedEditVersion = editVersion;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_MIDDLE && tab != UiTab.DOCS) {
            for (FileTab fileTab : fileTabs()) {
                if (fileTab.rect().contains(click.x(), click.y())) {
                    closeBuffer(fileTab.buffer());
                    return true;
                }
            }
        }
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        editor.dragging = false;

        if (tab != UiTab.DOCS) {
            if (outlineVisible && handleOutlineClick(click.x(), click.y())) {
                return true;
            }
            if (findMode != FindMode.NONE && handleFindBarClick(click.x(), click.y())) {
                return true;
            }
            for (FileTab fileTab : fileTabs()) {
                if (fileTab.close().contains(click.x(), click.y())) {
                    closeBuffer(fileTab.buffer());
                    return true;
                }
                if (fileTab.rect().contains(click.x(), click.y())) {
                    activate(fileTab.buffer());
                    editor.focused = true;
                    return true;
                }
            }
            Rect diag = diagnosticsRect();
            String clear = "Clear";
            int clearX = diag.x + diag.w - font.width(clear) - 6;
            if (consoleTab && click.y() >= diag.y && click.y() < diag.y + 14 && click.x() >= clearX && click.x() < diag.x + diag.w) {
                ScriptConsole.clear();
                outputText = "Console cleared.";
                return true;
            }
            findFocused = false;
        }

        if (tabsArea().contains(click.x(), click.y())) {
            int idx = Math.clamp((int) ((click.x() - tabsArea().x) / TAB_W), 0, 2);
            tab = switch (idx) {
                case 0 -> UiTab.MAIN;
                case 1 -> UiTab.DOCS;
                default -> UiTab.SCRIPTS;
            };
            scriptsSearchFocused = false;
            renameFieldFocused = false;
            hideSuggestions();
            return true;
        }

        if (tab == UiTab.DOCS) {
            editor.focused = false;
            scriptsSearchFocused = false;
            renameFieldFocused = false;
            return false;
        }

        if (tab == UiTab.SCRIPTS) {
            if (scriptsSearchRect().contains(click.x(), click.y())) {
                scriptsSearchFocused = true;
                renameFieldFocused = false;
                editor.focused = false;
                hideSuggestions();
                return true;
            }
            if (scriptsRenameRect().contains(click.x(), click.y())) {
                renameFieldFocused = true;
                scriptsSearchFocused = false;
                editor.focused = false;
                hideSuggestions();
                return true;
            }
            scriptsSearchFocused = false;
            renameFieldFocused = false;
            if (scriptsBodyRect().contains(click.x(), click.y())) {
                int idx = visibleScriptIndexAt(click.y());
                if (idx >= 0 && idx < visibleScripts.size()) {
                    editor.focused = false;
                    openScriptByVisibleIndex(idx, false);
                    return true;
                }
            }
            if (scriptsBtnCreate().contains(click.x(), click.y())) {
                return createScript();
            }
            if (scriptsBtnRename().contains(click.x(), click.y())) {
                return renameScript();
            }
            if (scriptsBtnDuplicate().contains(click.x(), click.y())) {
                return duplicateScript();
            }
            if (scriptsBtnDelete().contains(click.x(), click.y())) {
                return deleteScript();
            }
            if (scriptsBtnSort().contains(click.x(), click.y())) {
                return cycleSort();
            }
            if (scriptsBtnFilter().contains(click.x(), click.y())) {
                return cycleFilter();
            }
            if (scriptsBtnSave().contains(click.x(), click.y())) {
                return saveScript();
            }
            if (scriptsBtnRun().contains(click.x(), click.y())) {
                return runScript();
            }
            if (scriptsBtnAsync().contains(click.x(), click.y())) {
                return toggleScriptAsync();
            }
        } else {
            scriptsSearchFocused = false;
            renameFieldFocused = false;
            if (mainBtnSave().contains(click.x(), click.y())) {
                return saveScript();
            }
            if (mainBtnRun().contains(click.x(), click.y())) {
                return runScript();
            }
            if (mainBtnValidate().contains(click.x(), click.y())) {
                return validateNow();
            }
            if (mainBtnAsync().contains(click.x(), click.y())) {
                return toggleScriptAsync();
            }
        }

        if (diagnosticsRect().contains(click.x(), click.y()) && click.y() < diagnosticsRect().y + 14) {
            // Header: "Problems (n)" and "Console (n)" act as tabs.
            consoleTab = click.x() >= diagnosticsRect().x + 6 + font.width(problemsLabel()) + 6;
            return true;
        }
        if (consoleTab && diagnosticsRect().contains(click.x(), click.y())) {
            List<ScriptConsole.Line> lines = ScriptConsole.lines();
            int idx = consoleScroll + (int) ((click.y() - (diagnosticsRect().y + 16)) / DIAG_ROW_H);
            if (idx >= 0 && idx < lines.size() && minecraft != null) {
                minecraft.keyboardHandler.setClipboard(lines.get(idx).text());
                outputText = "Copied console line.";
            }
            return true;
        }
        if (diagnosticsRect().contains(click.x(), click.y())) {
            int idx = diagnosticIndexAt(click.y());
            if (idx >= 0 && idx < diagnostics.size()) {
                Diagnostic diagnostic = diagnostics.get(idx);
                jumpToDiagnostic(diagnostic);
                copyDiagnosticToClipboard(diagnostic);
                return true;
            }
        }

        if (suggestionsVisible && suggestionsRect().contains(click.x(), click.y())) {
            int idx = suggestionIndexAt(click.y());
            if (idx >= 0 && idx < visibleSuggestions.size()) {
                suggestionSelected = idx;
                DisplaySuggestion ds = visibleSuggestions.get(idx);
                if (ds.groupHeader() && ds.hiddenOverloads() > 0) {
                    toggleMethodGroup(ds.groupKey());
                } else {
                    acceptSuggestion();
                }
                return true;
            }
        }

        Rect code = codeTextRect();
        Rect gutter = gutterRect(editorRect());
        if (gutter.contains(click.x(), click.y())) {
            editor.focused = true;
            scriptsSearchFocused = false;
            hideSuggestions();
            return true;
        }
        if (code.contains(click.x(), click.y())) {
            editor.focused = true;
            scriptsSearchFocused = false;
            int idx = indexFromMouse((int) click.x(), (int) click.y(), code);
            updateClickCount(idx);
            if (editor.clickCount >= 3) {
                selectLineAt(idx);
                editor.dragging = false;
            } else if (editor.clickCount == 2 || doubled) {
                selectWordAt(idx);
                editor.dragging = false;
            } else {
                editor.cursor = idx;
                editor.selectionAnchor = -1;
                editor.dragging = true;
                editor.dragStartCursor = idx;
            }
            clampEditor();
            refreshSuggestions();
            return true;
        }

        editor.focused = false;
        scriptsSearchFocused = false;
        hideSuggestions();
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT || !editor.focused || !editor.dragging) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        Rect code = codeTextRect();
        int next = indexFromMouse((int) click.x(), (int) click.y(), code);
        if (editor.selectionAnchor < 0) {
            editor.selectionAnchor = editor.dragStartCursor;
        }
        editor.cursor = next;
        clampEditor();
        refreshSuggestions();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == UiTab.DOCS) {
            docsScroll = Math.max(0, docsScroll + (verticalAmount > 0 ? -2 : 2));
            return true;
        }
        if (tab == UiTab.SCRIPTS && scriptsBodyRect().contains(mouseX, mouseY)) {
            scriptsScroll = Math.max(0, scriptsScroll + (verticalAmount > 0 ? -1 : 1));
            return true;
        }
        if (codeTextRect().contains(mouseX, mouseY) || gutterRect(editorRect()).contains(mouseX, mouseY)) {
            editor.scrollLine = Math.max(0, editor.scrollLine + (verticalAmount > 0 ? -2 : 2));
            clampEditor();
            return true;
        }
        if (diagnosticsRect().contains(mouseX, mouseY)) {
            if (consoleTab) {
                consoleScroll = Math.max(0, consoleScroll + (verticalAmount > 0 ? -1 : 1));
            } else {
                diagnosticsScroll = Math.max(0, diagnosticsScroll + (verticalAmount > 0 ? -1 : 1));
            }
            return true;
        }
        if (suggestionsVisible && suggestionsRect().contains(mouseX, mouseY)) {
            suggestionScroll = Math.max(0, suggestionScroll + (verticalAmount > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        int codepoint = input.codepoint();
        if (codepoint >= 32 && codepoint != 127) {
            String typed = new String(Character.toChars(codepoint));
            if (outlineVisible) {
                outlineQuery += typed;
                refreshOutline();
                return true;
            }
            if (findMode != FindMode.NONE && findFocused) {
                if (findMode == FindMode.GOTO) {
                    if (Character.isDigit(codepoint) || codepoint == ':') {
                        gotoText += typed;
                    }
                } else if (replaceFocused) {
                    replaceText += typed;
                } else {
                    findQuery += typed;
                    findMatchesVersion = -1;
                    jumpToFirstMatchFrom(Math.min(editor.cursor, editor.selectionAnchor < 0 ? editor.cursor : editor.selectionAnchor));
                }
                return true;
            }
        }
        if (tab == UiTab.SCRIPTS && scriptsSearchFocused && (codepoint >= 32 && codepoint != 127)) {
            scriptSearch = scriptSearch + new String(Character.toChars(codepoint));
            refreshScripts();
            return true;
        }
        if (tab == UiTab.SCRIPTS && renameFieldFocused && (codepoint >= 32 && codepoint != 127)) {
            renameDraft = renameDraft + new String(Character.toChars(codepoint));
            return true;
        }

        if (!editor.focused || tab == UiTab.DOCS) {
            return false;
        }
        if (codepoint >= 32 && codepoint != 127) {
            if (codepoint < 0x10000 && typeWithPairs((char) codepoint)) {
                refreshSuggestions();
                return true;
            }
            replaceSelectionOrInsert(new String(Character.toChars(codepoint)), true);
            refreshSuggestions();
            return true;
        }
        return false;
    }

    /** Closing brackets/quotes: wraps a selection, steps over an existing closer, or inserts the pair. */
    private boolean typeWithPairs(char ch) {
        if (hasSelection() && "([{\"'".indexOf(ch) >= 0) {
            char close = switch (ch) {
                case '(' -> ')';
                case '[' -> ']';
                case '{' -> '}';
                default -> ch;
            };
            int start = Math.min(editor.selectionAnchor, editor.cursor);
            int end = Math.max(editor.selectionAnchor, editor.cursor);
            saveUndo();
            editor.text = editor.text.substring(0, start) + ch + editor.text.substring(start, end) + close + editor.text.substring(end);
            editor.selectionAnchor = start + 1;
            editor.cursor = end + 1;
            markEdited();
            clampEditor();
            return true;
        }
        if (hasSelection()) {
            return false;
        }
        if (LineOps.skipsClosing(editor.text, editor.cursor, ch)) {
            editor.cursor++;
            clampEditor();
            return true;
        }
        char close = LineOps.autoClose(editor.text, editor.cursor, ch);
        if (close == 0) {
            return false;
        }
        replaceSelectionOrInsert("" + ch + close, true);
        editor.cursor--;
        clampEditor();
        return true;
    }

    private void applyLineEdit(LineOps.Edit edit) {
        if (edit.text().equals(editor.text) && edit.cursor() == editor.cursor) {
            return;
        }
        saveUndo();
        applyEditWithoutUndo(edit);
    }

    private void applyEditWithoutUndo(LineOps.Edit edit) {
        editor.text = edit.text();
        editor.cursor = Math.clamp(edit.cursor(), 0, editor.text.length());
        editor.selectionAnchor = edit.anchor() < 0 ? -1 : Math.clamp(edit.anchor(), 0, editor.text.length());
        editor.preferredColumn = -1;
        markEdited();
        clampEditor();
        hideSuggestions();
    }

    // ---- find, replace, go to line ------------------------------------------------------------------------

    private void openFind(FindMode mode) {
        findMode = mode;
        findFocused = true;
        replaceFocused = false;
        outlineVisible = false;
        hideSuggestions();
        if (mode == FindMode.GOTO) {
            gotoText = "";
            outputText = "Go to line: type a number (line or line:column), Enter to jump, Esc to cancel.";
            return;
        }
        if (hasSelection()) {
            String selected = selectedTextOrAll();
            if (!selected.contains("\n") && !selected.isBlank()) {
                findQuery = selected;
            }
        }
        findMatchesVersion = -1;
        refreshFindMatches();
        outputText = "Enter/Shift+Enter: next/previous · Tab: replace field · Alt+C case · Alt+W whole word · Esc closes";
    }

    private void closeFind() {
        findMode = FindMode.NONE;
        findFocused = false;
        replaceFocused = false;
        editor.focused = true;
    }

    private void refreshFindMatches() {
        if (findMatchesVersion == editVersion && findMatchesBuffer == active) {
            return;
        }
        findMatches = TextSearch.findAll(editor.text, findQuery, sessionCaseSensitive, sessionWholeWord);
        findMatchesVersion = editVersion;
        findMatchesBuffer = active;
        if (findCurrent >= findMatches.size()) {
            findCurrent = findMatches.isEmpty() ? -1 : 0;
        }
    }

    private void jumpToFirstMatchFrom(int from) {
        refreshFindMatches();
        int index = TextSearch.nextIndex(findMatches, from, false);
        selectMatch(index);
    }

    private void selectMatch(int index) {
        findCurrent = index;
        if (index < 0 || index >= findMatches.size()) {
            return;
        }
        TextSearch.Match match = findMatches.get(index);
        editor.selectionAnchor = match.start();
        editor.cursor = match.end();
        editor.preferredColumn = -1;
        clampEditor();
    }

    private void findStep(boolean backwards) {
        refreshFindMatches();
        if (findMatches.isEmpty()) {
            return;
        }
        int from = backwards ? Math.min(editor.cursor, editor.selectionAnchor < 0 ? editor.cursor : editor.selectionAnchor)
                : editor.cursor;
        selectMatch(TextSearch.nextIndex(findMatches, from, backwards));
    }

    private void replaceCurrent() {
        refreshFindMatches();
        int start = Math.min(editor.cursor, editor.selectionAnchor < 0 ? editor.cursor : editor.selectionAnchor);
        int end = Math.max(editor.cursor, editor.selectionAnchor);
        if (TextSearch.indexAt(findMatches, start, end) < 0) {
            findStep(false);
            return;
        }
        saveUndo();
        editor.text = editor.text.substring(0, start) + replaceText + editor.text.substring(end);
        editor.cursor = start + replaceText.length();
        editor.selectionAnchor = -1;
        markEdited();
        clampEditor();
        refreshFindMatches();
        findStep(false);
    }

    private void replaceAllMatches() {
        TextSearch.Replaced result = TextSearch.replaceAll(editor.text, findQuery, replaceText, sessionCaseSensitive, sessionWholeWord);
        if (result.count() == 0) {
            outputText = "No matches to replace.";
            return;
        }
        saveUndo();
        editor.text = result.text();
        editor.selectionAnchor = -1;
        markEdited();
        clampEditor();
        refreshFindMatches();
        outputText = "Replaced " + result.count() + " match(es). Ctrl+Z undoes.";
    }

    private void goToLine() {
        String[] parts = gotoText.split(":");
        try {
            int line = Integer.parseInt(parts[0].trim());
            int col = parts.length > 1 && !parts[1].isBlank() ? Integer.parseInt(parts[1].trim()) : 1;
            jumpToDiagnostic(new Diagnostic("INFO", currentScriptName, line, col, ""));
            closeFind();
        } catch (NumberFormatException e) {
            outputText = "Type a line number.";
        }
    }

    private boolean handleFindKey(int key, boolean ctrl, boolean shift, boolean alt) {
        if (key == InputConstants.KEY_ESCAPE) {
            closeFind();
            return true;
        }
        if (findMode == FindMode.GOTO) {
            if (key == InputConstants.KEY_BACKSPACE && !gotoText.isEmpty()) {
                gotoText = gotoText.substring(0, gotoText.length() - 1);
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                goToLine();
                return true;
            }
            return key == InputConstants.KEY_BACKSPACE;
        }
        if (alt && key == InputConstants.KEY_C) {
            sessionCaseSensitive = !sessionCaseSensitive;
            findMatchesVersion = -1;
            refreshFindMatches();
            return true;
        }
        if (alt && key == InputConstants.KEY_W) {
            sessionWholeWord = !sessionWholeWord;
            findMatchesVersion = -1;
            refreshFindMatches();
            return true;
        }
        if (key == InputConstants.KEY_TAB) {
            if (findMode == FindMode.FIND) {
                findMode = FindMode.REPLACE;
            }
            replaceFocused = !replaceFocused;
            return true;
        }
        if (key == InputConstants.KEY_BACKSPACE) {
            if (replaceFocused) {
                replaceText = replaceText.isEmpty() ? "" : replaceText.substring(0, replaceText.length() - 1);
            } else if (!findQuery.isEmpty()) {
                findQuery = findQuery.substring(0, findQuery.length() - 1);
                findMatchesVersion = -1;
                refreshFindMatches();
            }
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            if (replaceFocused) {
                if (ctrl) {
                    replaceAllMatches();
                } else {
                    replaceCurrent();
                }
            } else {
                findStep(shift);
            }
            return true;
        }
        if (ctrl && key == InputConstants.KEY_V && minecraft != null) {
            String clip = minecraft.keyboardHandler.getClipboard().replace("\n", " ");
            if (replaceFocused) {
                replaceText += clip;
            } else {
                findQuery += clip;
                findMatchesVersion = -1;
                jumpToFirstMatchFrom(editor.cursor);
            }
            return true;
        }
        if (key == InputConstants.KEY_F3) {
            findStep(shift);
            return true;
        }
        return false;
    }

    // ---- outline (Ctrl+O) ------------------------------------------------------------------------------------

    /** Ctrl+E: recently opened scripts, newest first; type to filter, Enter to open. */
    private void openRecent() {
        outlineVisible = true;
        recentMode = true;
        outlineQuery = "";
        outlineSelected = 0;
        findFocused = false;
        hideSuggestions();
        refreshOutline();
        // The current file is first; preselect the previous one so Ctrl+E, Enter flips between two files.
        if (outlineItems.size() > 1 && outlineItems.getFirst().name().equals(currentScriptName)) {
            outlineSelected = 1;
        }
        if (outlineItems.isEmpty()) {
            outputText = "No recent scripts yet.";
        }
    }

    private void openOutline() {
        recentMode = false;
        outlineVisible = true;
        outlineQuery = "";
        outlineSelected = 0;
        findFocused = false;
        hideSuggestions();
        refreshOutline();
        if (outlineItems.isEmpty()) {
            outputText = "No functions, classes or top-level values found.";
        }
    }

    private void refreshOutline() {
        if (recentMode) {
            String q = outlineQuery.toLowerCase(Locale.ROOT);
            List<ScriptOutline.Symbol> files = new ArrayList<>();
            for (String name : RECENT.list(name -> ScriptStorage.exists(name) || findBuffer(name) != null)) {
                if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                    Buffer open = findBuffer(name);
                    files.add(new ScriptOutline.Symbol(name, open != null && open.dirty() ? "●" : open != null ? "open" : "", -1, 0));
                }
            }
            outlineItems = files;
            outlineSelected = Math.clamp(outlineSelected, 0, Math.max(0, files.size() - 1));
            return;
        }
        String language = inferLanguage(currentScriptName).name();
        List<ScriptOutline.Symbol> all = ScriptOutline.symbols(editor.text, language);
        String q = outlineQuery.toLowerCase(Locale.ROOT);
        List<ScriptOutline.Symbol> filtered = new ArrayList<>();
        for (ScriptOutline.Symbol symbol : all) {
            if (q.isEmpty() || symbol.name().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(symbol);
            }
        }
        outlineItems = filtered;
        outlineSelected = Math.clamp(outlineSelected, 0, Math.max(0, filtered.size() - 1));
    }

    private boolean handleOutlineKey(int key) {
        switch (key) {
            case InputConstants.KEY_ESCAPE -> outlineVisible = false;
            case InputConstants.KEY_UP -> outlineSelected = Math.max(0, outlineSelected - 1);
            case InputConstants.KEY_DOWN -> outlineSelected = Math.min(Math.max(0, outlineItems.size() - 1), outlineSelected + 1);
            case InputConstants.KEY_BACKSPACE -> {
                if (!outlineQuery.isEmpty()) {
                    outlineQuery = outlineQuery.substring(0, outlineQuery.length() - 1);
                    refreshOutline();
                }
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> jumpToOutlineItem(outlineSelected);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void jumpToOutlineItem(int index) {
        if (index < 0 || index >= outlineItems.size()) {
            outlineVisible = false;
            return;
        }
        ScriptOutline.Symbol symbol = outlineItems.get(index);
        outlineVisible = false;
        if (recentMode) {
            openScriptByName(symbol.name());
            return;
        }
        jumpToDiagnostic(new Diagnostic("INFO", currentScriptName, symbol.line() + 1, symbol.column() + 1, ""));
        // Put the symbol near the top of the view.
        editor.scrollLine = Math.max(0, symbol.line() - 3);
        clampEditor();
    }

    private boolean isAltDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        boolean ctrl = isCtrlDown();
        boolean shift = isShiftDown();
        boolean alt = isAltDown();

        if (outlineVisible && handleOutlineKey(key)) {
            return true;
        }
        if (findMode != FindMode.NONE && findFocused && handleFindKey(key, ctrl, shift, alt)) {
            return true;
        }
        if (tab != UiTab.DOCS) {
            if (key == InputConstants.KEY_F5) {
                if (shift) {
                    int stopped = ScriptRunner.cancelAll();
                    outputText = stopped == 0 ? "Nothing is running." : "Stopped " + stopped + " script(s).";
                    return true;
                }
                return runScript();
            }
            if (ctrl && !shift && key == InputConstants.KEY_F) {
                openFind(FindMode.FIND);
                return true;
            }
            if (ctrl && key == InputConstants.KEY_H) {
                openFind(FindMode.REPLACE);
                return true;
            }
            if (ctrl && key == InputConstants.KEY_G) {
                openFind(FindMode.GOTO);
                return true;
            }
            if (ctrl && key == InputConstants.KEY_O) {
                openOutline();
                return true;
            }
            if (ctrl && key == InputConstants.KEY_E) {
                openRecent();
                return true;
            }
            if (ctrl && key == InputConstants.KEY_TAB) {
                cycleBuffer(shift ? -1 : 1);
                return true;
            }
            if (ctrl && key == InputConstants.KEY_W && active != null) {
                closeBuffer(active);
                return true;
            }
            if (ctrl && shift && key == InputConstants.KEY_S) {
                return saveAll();
            }
            if (ctrl && key == InputConstants.KEY_N) {
                return createScript();
            }
        }

        if (tab == UiTab.SCRIPTS && scriptsSearchFocused && key == InputConstants.KEY_BACKSPACE && !scriptSearch.isEmpty()) {
            scriptSearch = scriptSearch.substring(0, scriptSearch.length() - 1);
            refreshScripts();
            return true;
        }
        if (tab == UiTab.SCRIPTS && renameFieldFocused) {
            if (key == InputConstants.KEY_BACKSPACE && !renameDraft.isEmpty()) {
                renameDraft = renameDraft.substring(0, renameDraft.length() - 1);
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                return renameScript();
            }
        }

        if (key == InputConstants.KEY_ESCAPE) {
            if (suggestionsVisible) {
                hideSuggestions();
                return true;
            }
            if (hasSelection() && editor.focused) {
                editor.selectionAnchor = -1;
                return true;
            }
            onClose();
            return true;
        }

        if (!editor.focused || tab == UiTab.DOCS) {
            return false;
        }

        if (ctrl && key == InputConstants.KEY_Z && shift) {
            return redoEdit();
        }
        if (ctrl && key == InputConstants.KEY_Y) {
            return redoEdit();
        }
        if (ctrl && key == InputConstants.KEY_Z) {
            return undoEdit();
        }

        if (suggestionsVisible) {
            if (key == InputConstants.KEY_UP) {
                suggestionSelected = Math.max(0, suggestionSelected - 1);
                ensureSuggestionVisible();
                return true;
            }
            if (key == InputConstants.KEY_DOWN) {
                suggestionSelected = Math.min(visibleSuggestions.size() - 1, suggestionSelected + 1);
                ensureSuggestionVisible();
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_TAB) {
                acceptSuggestion();
                return true;
            }
        }

        if (ctrl && key == InputConstants.KEY_A) {
            editor.selectionAnchor = 0;
            editor.cursor = editor.text.length();
            return true;
        }
        if (ctrl && key == InputConstants.KEY_C && minecraft != null) {
            minecraft.keyboardHandler.setClipboard(selectedTextOrAll());
            return true;
        }
        if (ctrl && key == InputConstants.KEY_X && minecraft != null) {
            if (hasSelection()) {
                minecraft.keyboardHandler.setClipboard(selectedTextOrAll());
                deleteSelection(true);
            } else {
                // No selection: cut the current line (like most editors), never the whole file.
                int start = lineStart(editor.cursor);
                int end = lineEnd(editor.cursor);
                minecraft.keyboardHandler.setClipboard(editor.text.substring(start, end) + "\n");
                saveUndo();
                applyEditWithoutUndo(LineOps.deleteLines(editor.text, editor.cursor, -1));
            }
            refreshSuggestions();
            return true;
        }
        if (ctrl && key == InputConstants.KEY_V && minecraft != null) {
            replaceSelectionOrInsert(minecraft.keyboardHandler.getClipboard(), true);
            refreshSuggestions();
            return true;
        }
        if (ctrl && key == InputConstants.KEY_S) {
            return saveScript();
        }
        if (ctrl && shift && key == InputConstants.KEY_R) {
            return runSelection();
        }
        if (ctrl && shift && key == InputConstants.KEY_F) {
            return formatCode();
        }
        if (ctrl && key == InputConstants.KEY_J) {
            return showSnippets();
        }
        if (ctrl && key == InputConstants.KEY_R) {
            return runScript();
        }

        if (ctrl && key == InputConstants.KEY_SLASH) {
            applyLineEdit(LineOps.toggleComment(editor.text, editor.cursor, editor.selectionAnchor,
                    LineOps.commentPrefix(currentScriptName)));
            return true;
        }
        if (ctrl && key == InputConstants.KEY_D) {
            applyLineEdit(LineOps.duplicate(editor.text, editor.cursor, editor.selectionAnchor));
            return true;
        }
        if (ctrl && shift && key == InputConstants.KEY_K) {
            applyLineEdit(LineOps.deleteLines(editor.text, editor.cursor, editor.selectionAnchor));
            return true;
        }
        if (alt && (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN)) {
            applyLineEdit(LineOps.moveLines(editor.text, editor.cursor, editor.selectionAnchor,
                    key == InputConstants.KEY_UP ? -1 : 1));
            return true;
        }
        if (ctrl && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
            int target = key == InputConstants.KEY_LEFT
                    ? LineOps.wordLeft(editor.text, editor.cursor)
                    : LineOps.wordRight(editor.text, editor.cursor);
            editor.preferredColumn = -1;
            moveCursor(target, shift);
            hideSuggestions();
            return true;
        }
        if (ctrl && key == InputConstants.KEY_BACKSPACE && !hasSelection()) {
            int start = LineOps.wordLeft(editor.text, editor.cursor);
            if (start < editor.cursor) {
                saveUndo();
                editor.text = editor.text.substring(0, start) + editor.text.substring(editor.cursor);
                editor.cursor = start;
                markEdited();
                clampEditor();
            }
            refreshSuggestions();
            return true;
        }
        if (ctrl && key == InputConstants.KEY_DELETE && !hasSelection()) {
            int end = LineOps.wordRight(editor.text, editor.cursor);
            if (end > editor.cursor) {
                saveUndo();
                editor.text = editor.text.substring(0, editor.cursor) + editor.text.substring(end);
                markEdited();
                clampEditor();
            }
            refreshSuggestions();
            return true;
        }

        if (key == InputConstants.KEY_TAB) {
            if (shift) {
                unindentSelectionOrLine();
            } else {
                indentSelectionOrInsert();
            }
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            insertNewlineWithAutoIndent();
            refreshSuggestions();
            return true;
        }

        if (key == InputConstants.KEY_BACKSPACE) {
            if (hasSelection()) {
                deleteSelection(true);
            } else if (editor.cursor > 0) {
                saveUndo();
                int after = LineOps.betweenEmptyPair(editor.text, editor.cursor) ? editor.cursor + 1 : editor.cursor;
                editor.text = editor.text.substring(0, editor.cursor - 1) + editor.text.substring(after);
                editor.cursor--;
                markEdited();
            }
            editor.selectionAnchor = -1;
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection(true);
            } else if (editor.cursor < editor.text.length()) {
                saveUndo();
                editor.text = editor.text.substring(0, editor.cursor) + editor.text.substring(editor.cursor + 1);
                markEdited();
            }
            editor.selectionAnchor = -1;
            refreshSuggestions();
            return true;
        }

        if (key == InputConstants.KEY_LEFT) {
            moveCursor(editor.cursor - 1, shift);
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            moveCursor(editor.cursor + 1, shift);
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            moveCursor(ctrl ? 0 : LineOps.smartHome(editor.text, editor.cursor), shift);
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_END) {
            moveCursor(ctrl ? editor.text.length() : lineEnd(editor.cursor), shift);
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            moveCursor(verticalMove(editor.cursor, -1), shift);
            refreshSuggestions();
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            moveCursor(verticalMove(editor.cursor, 1), shift);
            refreshSuggestions();
            return true;
        }

        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, Theme.SCRIM);
        drawTabs(context, mouseX, mouseY);

        if (tab == UiTab.DOCS) {
            renderDocs(context);
        } else {
            if (tab == UiTab.SCRIPTS) {
                renderScriptsPane(context, mouseX, mouseY);
            } else {
                renderMainHeader(context, mouseX, mouseY);
            }
            renderFileTabs(context, mouseX, mouseY);
            renderEditor(context, mouseX, mouseY);
            renderStatusBar(context);
            renderDiagnostics(context, mouseX, mouseY);
            if (findMode != FindMode.NONE) {
                renderFindBar(context, mouseX, mouseY);
            }
            if (outlineVisible) {
                renderOutline(context, mouseX, mouseY);
            }
        }

        if (suggestionsVisible && !suggestions.isEmpty() && editor.focused) {
            renderSuggestions(context, mouseX, mouseY);
        }
    }

    // ---- file tabs, status bar, find bar and outline rendering -----------------------------------------------

    private record FileTab(Buffer buffer, Rect rect, Rect close) {
    }

    private List<FileTab> fileTabs() {
        Rect panel = editorRect();
        List<FileTab> tabs = new ArrayList<>();
        int x = panel.x;
        int y = panel.y - FILE_TAB_H;
        int maxX = panel.x + panel.w;
        for (Buffer buffer : SESSION) {
            String label = buffer.name + (buffer.dirty() ? " ●" : "");
            int w = font.width(label) + 20;
            if (x + w > maxX) {
                break;
            }
            Rect rect = new Rect(x, y, w, FILE_TAB_H - 1);
            tabs.add(new FileTab(buffer, rect, new Rect(x + w - 11, y + 2, 9, FILE_TAB_H - 5)));
            x += w + 1;
        }
        return tabs;
    }

    private void renderFileTabs(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int shown = 0;
        for (FileTab fileTab : fileTabs()) {
            Rect r = fileTab.rect();
            boolean isActive = fileTab.buffer() == active;
            boolean hover = r.contains(mouseX, mouseY);
            context.fill(r.x, r.y, r.x + r.w, r.y + r.h, isActive ? Theme.SURFACE_PRESSED : hover ? Theme.SURFACE_HOVER : Theme.SURFACE);
            if (isActive) {
                context.fill(r.x, r.y + r.h - 1, r.x + r.w, r.y + r.h, Theme.ACCENT);
            }
            String label = fileTab.buffer().name + (fileTab.buffer().dirty() ? " ●" : "");
            context.text(font, label, r.x + 5, r.y + 3, isActive ? Theme.TEXT : Theme.TEXT_MUTED, false);
            boolean closeHover = fileTab.close().contains(mouseX, mouseY);
            context.text(font, "×", fileTab.close().x + 1, r.y + 3, closeHover ? Theme.DANGER : Theme.TEXT_DIM, false);
            shown++;
        }
        if (shown < SESSION.size()) {
            Rect panel = editorRect();
            String more = "+" + (SESSION.size() - shown) + " (Ctrl+Tab)";
            context.text(font, more, panel.x + panel.w - font.width(more) - 2, panel.y - FILE_TAB_H + 3, Theme.TEXT_DIM, false);
        }
    }

    private void renderStatusBar(GuiGraphicsExtractor context) {
        Rect panel = editorRect();
        int y = panel.y + panel.h + 2;
        int lineNo = lineNumberOfCursor() + 1;
        int col = editor.cursor - lineStart(editor.cursor) + 1;
        StringBuilder left = new StringBuilder("Ln " + lineNo + ", Col " + col);
        if (hasSelection()) {
            left.append(" (").append(Math.abs(editor.cursor - editor.selectionAnchor)).append(" selected)");
        }
        left.append("  ·  ").append(inferLanguage(currentScriptName).name().charAt(0))
                .append(inferLanguage(currentScriptName).name().substring(1).toLowerCase(Locale.ROOT));
        left.append("  ·  ").append(splitLines(editor.text).size()).append(" lines");
        left.append("  ·  ").append(active != null && active.dirty() ? "modified" : "saved");
        left.append("  ·  async ").append(scriptRunsAsync(editor.text) ? "on" : "off");
        context.text(font, left.toString(), panel.x + 2, y, Theme.TEXT_MUTED, false);
        int running = ScriptRunner.activeJobs().size();
        String right = (running > 0 ? "● " + running + " running (Shift+F5 stops)   " : "")
                + "F5 run · Ctrl+F find · Ctrl+O outline · Ctrl+E recent · Ctrl+/ comment";
        context.text(font, trimToWidth(right, Math.max(40, panel.w / 2)), panel.x + panel.w - Math.min(font.width(right), panel.w / 2) - 2, y,
                running > 0 ? Theme.SUCCESS : Theme.TEXT_DIM, false);
    }

    private Rect findBarRect() {
        Rect code = codeTextRect();
        int w = Math.min(360, code.w - 8);
        int h = findMode == FindMode.REPLACE ? 30 : 16;
        return new Rect(code.x + code.w - w - 4, code.y + 2, w, h);
    }

    private Rect findButton(int index, int row) {
        Rect bar = findBarRect();
        int bw = 18;
        return new Rect(bar.x + bar.w - (index + 1) * (bw + 2) - 2, bar.y + 2 + row * 14, bw, 12);
    }

    private void renderFindBar(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect bar = findBarRect();
        context.fill(bar.x - 1, bar.y - 1, bar.x + bar.w + 1, bar.y + bar.h + 1, Theme.BORDER_STRONG);
        context.fill(bar.x, bar.y, bar.x + bar.w, bar.y + bar.h, Theme.SURFACE_RAISED);
        if (findMode == FindMode.GOTO) {
            String text = "Go to line: " + gotoText + (blinkOn() ? "_" : "");
            context.text(font, text, bar.x + 4, bar.y + 4, Theme.TEXT, false);
            return;
        }
        refreshFindMatches();
        String count = findQuery.isEmpty() ? "" : findMatches.isEmpty() ? "No results"
                : (findCurrent >= 0 ? (findCurrent + 1) + "/" : "") + findMatches.size();
        int fieldRight = findButton(4, 0).x - 4;
        String query = "Find: " + findQuery + (findFocused && !replaceFocused && blinkOn() ? "_" : "");
        context.fill(bar.x + 2, bar.y + 2, fieldRight, bar.y + 14, !replaceFocused && findFocused ? Theme.SURFACE_PRESSED : Theme.INPUT);
        context.text(font, trimToWidth(query, fieldRight - bar.x - 50), bar.x + 4, bar.y + 4, Theme.TEXT, false);
        context.text(font, count, fieldRight - font.width(count) - 2, bar.y + 4, findMatches.isEmpty() && !findQuery.isEmpty() ? Theme.DANGER : Theme.TEXT_MUTED, false);
        drawFindToggle(context, findButton(4, 0), "Aa", sessionCaseSensitive, mouseX, mouseY);
        drawFindToggle(context, findButton(3, 0), "W", sessionWholeWord, mouseX, mouseY);
        drawFindToggle(context, findButton(2, 0), "↑", false, mouseX, mouseY);
        drawFindToggle(context, findButton(1, 0), "↓", false, mouseX, mouseY);
        drawFindToggle(context, findButton(0, 0), "×", false, mouseX, mouseY);
        if (findMode == FindMode.REPLACE) {
            int replaceRight = findButton(1, 1).x - 4;
            String replace = "Replace: " + replaceText + (findFocused && replaceFocused && blinkOn() ? "_" : "");
            context.fill(bar.x + 2, bar.y + 16, replaceRight, bar.y + 28, replaceFocused ? Theme.SURFACE_PRESSED : Theme.INPUT);
            context.text(font, trimToWidth(replace, replaceRight - bar.x - 6), bar.x + 4, bar.y + 18, Theme.TEXT, false);
            drawFindToggle(context, findButton(1, 1), "1", false, mouseX, mouseY);
            drawFindToggle(context, findButton(0, 1), "All", false, mouseX, mouseY);
        }
    }

    private void drawFindToggle(GuiGraphicsExtractor context, Rect r, String label, boolean on, int mouseX, int mouseY) {
        boolean hover = r.contains(mouseX, mouseY);
        context.fill(r.x, r.y, r.x + r.w, r.y + r.h, on ? Theme.ACCENT_PRESSED : hover ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED);
        context.text(font, label, r.x + (r.w - font.width(label)) / 2, r.y + 2, Theme.TEXT, false);
    }

    private boolean handleFindBarClick(double mx, double my) {
        Rect bar = findBarRect();
        if (!bar.contains(mx, my)) {
            return false;
        }
        findFocused = true;
        if (findMode == FindMode.GOTO) {
            return true;
        }
        if (findButton(4, 0).contains(mx, my)) {
            sessionCaseSensitive = !sessionCaseSensitive;
            findMatchesVersion = -1;
        } else if (findButton(3, 0).contains(mx, my)) {
            sessionWholeWord = !sessionWholeWord;
            findMatchesVersion = -1;
        } else if (findButton(2, 0).contains(mx, my)) {
            findStep(true);
        } else if (findButton(1, 0).contains(mx, my)) {
            findStep(false);
        } else if (findButton(0, 0).contains(mx, my)) {
            closeFind();
        } else if (findMode == FindMode.REPLACE && findButton(1, 1).contains(mx, my)) {
            replaceCurrent();
        } else if (findMode == FindMode.REPLACE && findButton(0, 1).contains(mx, my)) {
            replaceAllMatches();
        } else {
            replaceFocused = findMode == FindMode.REPLACE && my >= bar.y + 15;
        }
        return true;
    }

    private Rect outlineRect() {
        Rect code = codeTextRect();
        int w = Math.min(300, code.w - 20);
        int rows = Math.clamp(outlineItems.size(), 1, 12);
        return new Rect(code.x + (code.w - w) / 2, code.y + 6, w, 16 + rows * SUGGESTION_ROW_H + 4);
    }

    private void renderOutline(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect r = outlineRect();
        context.fill(r.x - 1, r.y - 1, r.x + r.w + 1, r.y + r.h + 1, Theme.BORDER_STRONG);
        context.fill(r.x, r.y, r.x + r.w, r.y + r.h, Theme.SURFACE_RAISED);
        context.text(font, (recentMode ? "Recent files: " : "Go to symbol: ") + outlineQuery + (blinkOn() ? "_" : ""), r.x + 4, r.y + 4, Theme.TEXT, false);
        if (outlineItems.isEmpty()) {
            context.text(font, "Nothing found", r.x + 4, r.y + 18, Theme.TEXT_DIM, false);
            return;
        }
        int rows = Math.clamp(outlineItems.size(), 1, 12);
        int first = Math.clamp(outlineSelected - rows + 1, 0, Math.max(0, outlineItems.size() - rows));
        for (int i = 0; i < rows && first + i < outlineItems.size(); i++) {
            ScriptOutline.Symbol symbol = outlineItems.get(first + i);
            int y = r.y + 16 + i * SUGGESTION_ROW_H;
            boolean selected = first + i == outlineSelected;
            boolean hover = mouseX >= r.x && mouseX < r.x + r.w && mouseY >= y && mouseY < y + SUGGESTION_ROW_H;
            if (selected || hover) {
                context.fill(r.x + 1, y, r.x + r.w - 1, y + SUGGESTION_ROW_H, selected ? SELECTION : Theme.SURFACE_HOVER);
            }
            if (recentMode) {
                context.text(font, symbol.name(), r.x + 4, y + 1, symbol.name().equals(currentScriptName) ? Theme.ACCENT_TEXT : Theme.TEXT, false);
                context.text(font, symbol.kind(), r.x + r.w - font.width(symbol.kind()) - 4, y + 1, Theme.TEXT_DIM, false);
                continue;
            }
            context.text(font, symbol.kind(), r.x + 4, y + 1, ScriptSyntax.color(ScriptSyntax.Kind.KEYWORD), false);
            context.text(font, symbol.name(), r.x + 8 + font.width("const val"), y + 1, ScriptSyntax.color(ScriptSyntax.Kind.FUNCTION), false);
            String line = ":" + (symbol.line() + 1);
            context.text(font, line, r.x + r.w - font.width(line) - 4, y + 1, Theme.TEXT_DIM, false);
        }
    }

    private boolean handleOutlineClick(double mx, double my) {
        Rect r = outlineRect();
        if (!r.contains(mx, my)) {
            outlineVisible = false;
            return false;
        }
        int rows = Math.clamp(outlineItems.size(), 1, 12);
        int first = Math.clamp(outlineSelected - rows + 1, 0, Math.max(0, outlineItems.size() - rows));
        int row = (int) ((my - (r.y + 16)) / SUGGESTION_ROW_H);
        if (row >= 0 && row < rows) {
            jumpToOutlineItem(first + row);
        }
        return true;
    }

    private void drawTabs(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect tabs = tabsArea();
        context.fill(tabs.x, tabs.y, tabs.x + tabs.w, tabs.y + tabs.h, Theme.SIDEBAR);
        context.fill(tabs.x, tabs.y + tabs.h - 1, tabs.x + tabs.w, tabs.y + tabs.h, Theme.DIVIDER);
        UiTab[] values = UiTab.values();
        for (int i = 0; i < values.length; i++) {
            int x = tabs.x + i * TAB_W;
            boolean active = values[i] == tab;
            boolean hover = mouseX >= x && mouseX < x + TAB_W && mouseY >= tabs.y && mouseY < tabs.y + tabs.h;
            int bg = active ? Theme.SURFACE_PRESSED : (hover ? Theme.SURFACE_HOVER : Theme.SIDEBAR);
            context.fill(x + 1, tabs.y + 1, x + TAB_W - 1, tabs.y + tabs.h - 1, bg);
            if (active) {
                context.fill(x + 1, tabs.y + tabs.h - 2, x + TAB_W - 1, tabs.y + tabs.h, Theme.ACCENT);
            }
            context.centeredText(font, Component.literal(values[i] == UiTab.MAIN ? "Main" : values[i] == UiTab.DOCS ? "Docs" : "Scripts"),
                    x + TAB_W / 2, tabs.y + 7, active ? Theme.TEXT : (hover ? Theme.TEXT : Theme.TEXT_MUTED));
        }
    }

    private void renderMainHeader(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect header = mainHeaderRect();
        context.fill(header.x, header.y, header.x + header.w, header.y + header.h, Theme.SURFACE);
        context.text(font,
                "File: " + currentScriptName + (active != null && active.dirty() ? " ●" : "") + " [" + inferLanguage(currentScriptName).name().toLowerCase(Locale.ROOT) + "]  Undo: " + undo.size() + "  Redo: " + redo.size(),
                header.x + 8, header.y + 7, Theme.TEXT);
        drawButton(context, mainBtnSave(), "Save", mouseX, mouseY);
        drawPrimaryButton(context, mainBtnRun(), "▶ Run", mouseX, mouseY);
        drawButton(context, mainBtnValidate(), "Validate", mouseX, mouseY);
        drawButton(context, mainBtnAsync(), "Async:" + (scriptRunsAsync(editor.text) ? "ON" : "OFF"), mouseX, mouseY);
    }

    private void renderScriptsPane(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect pane = scriptsPaneRect();
        context.fill(pane.x, pane.y, pane.x + pane.w, pane.y + pane.h, Theme.SURFACE);
        context.text(font, "Scripts", pane.x + 6, pane.y + 4, Theme.TEXT);

        Rect search = scriptsSearchRect();
        context.fill(search.x, search.y, search.x + search.w, search.y + search.h, scriptsSearchFocused ? Theme.SURFACE_PRESSED : Theme.INPUT);
        String searchLine = "Search: " + scriptSearch;
        context.text(font, searchLine, search.x + 4, search.y + 5, Theme.TEXT, false);
        if (scriptsSearchFocused && blinkOn()) {
            int cursorX = search.x + 4 + font.width(searchLine);
            if (cursorX < search.x + search.w - 2) {
                context.fill(cursorX, search.y + 4, cursorX + 1, search.y + 13, Theme.TEXT);
            }
        }

        Rect rename = scriptsRenameRect();
        context.fill(rename.x, rename.y, rename.x + rename.w, rename.y + rename.h, renameFieldFocused ? Theme.SURFACE_PRESSED : Theme.INPUT);
        String renameValue = renameDraft.isEmpty() ? "<new script name>" : renameDraft;
        String renameLine = "Name: " + renameValue;
        int renameColor = renameDraft.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT;
        context.text(font, renameLine, rename.x + 4, rename.y + 5, renameColor, false);
        if (renameFieldFocused && blinkOn()) {
            int cursorX = rename.x + 4 + font.width("Name: " + renameDraft);
            if (cursorX < rename.x + rename.w - 2) {
                context.fill(cursorX, rename.y + 4, cursorX + 1, rename.y + 13, Theme.TEXT);
            }
        }
        context.text(font, "Rename selected script. Press Enter or click Rename.", pane.x + 6, rename.y + rename.h + 4, Theme.TEXT_MUTED, false);

        Rect body = scriptsBodyRect();
        context.fill(body.x, body.y, body.x + body.w, body.y + body.h, Theme.INPUT);
        int visible = Math.max(1, body.h / SCRIPT_ROW_H);
        int maxScroll = Math.max(0, visibleScripts.size() - visible);
        scriptsScroll = Math.clamp(scriptsScroll, 0, maxScroll);

        for (int i = 0; i < visible; i++) {
            int idx = scriptsScroll + i;
            if (idx >= visibleScripts.size()) {
                break;
            }
            int y = body.y + i * SCRIPT_ROW_H;
            String name = visibleScripts.get(idx);
            boolean selected = idx == selectedScriptIndex;
            boolean hover = body.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + SCRIPT_ROW_H;
            if (selected) {
                context.fill(body.x + 1, y, body.x + body.w - 1, y + SCRIPT_ROW_H, Theme.SURFACE_PRESSED);
            } else if (hover) {
                context.fill(body.x + 1, y, body.x + body.w - 1, y + SCRIPT_ROW_H, Theme.SURFACE_HOVER);
            }
            Buffer open = findBuffer(name);
            String marker = open == null ? "" : open.dirty() ? " ●" : " ○";
            context.text(font, name + marker, body.x + 6, y + 3, selected ? Theme.TEXT : open != null ? Theme.ACCENT_TEXT : Theme.TEXT, false);
        }

        drawButton(context, scriptsBtnCreate(), "Create", mouseX, mouseY);
        drawButton(context, scriptsBtnRename(), "Rename", mouseX, mouseY);
        drawButton(context, scriptsBtnDuplicate(), "Duplicate", mouseX, mouseY);
        drawButton(context, scriptsBtnDelete(), "Delete", mouseX, mouseY);
        drawButton(context, scriptsBtnSort(), "Sort:" + sortMode.name(), mouseX, mouseY);
        drawButton(context, scriptsBtnFilter(), "Filter:" + filterMode.name(), mouseX, mouseY);
        drawButton(context, scriptsBtnSave(), "Save", mouseX, mouseY);
        drawButton(context, scriptsBtnRun(), "Run", mouseX, mouseY);
        drawButton(context, scriptsBtnAsync(), "Async:" + (scriptRunsAsync(editor.text) ? "ON" : "OFF"), mouseX, mouseY);
    }

    private void renderEditor(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect panel = editorRect();
        Rect gutter = gutterRect(panel);
        Rect code = codeTextRect(panel, gutter);
        context.fill(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h, editor.focused ? Theme.INPUT : Theme.SIDEBAR);
        context.fill(gutter.x, gutter.y, gutter.x + gutter.w, gutter.y + gutter.h, Theme.SIDEBAR);
        context.fill(gutter.x + gutter.w, panel.y, gutter.x + gutter.w + 1, panel.y + panel.h, Theme.BORDER);

        List<String> lines = splitLines(editor.text);
        int visible = Math.max(1, (code.h - 6) / CODE_LINE_H);
        int maxScroll = Math.max(0, lines.size() - visible);
        editor.scrollLine = Math.clamp(editor.scrollLine, 0, maxScroll);

        drawFindMatches(context, code, lines);
        drawSelection(context, code, lines);
        drawCurrentLineHighlight(context, code);

        List<ScriptSyntax.State> states = syntaxStates(lines);
        Set<Integer> errorLines = new HashSet<>();
        for (Diagnostic d : diagnostics) {
            errorLines.add(Math.max(1, d.line()) - 1);
        }
        int cursorLine = lineNumberOfCursor();
        String language = inferLanguage(currentScriptName).name();
        String hoverMessage = null;
        int y = code.y + 3;
        for (int lineIdx = editor.scrollLine; lineIdx < lines.size(); lineIdx++) {
            if (y > code.y + code.h - CODE_LINE_H) {
                break;
            }
            String line = lines.get(lineIdx);
            String n = Integer.toString(lineIdx + 1);
            boolean error = errorLines.contains(lineIdx);
            if (error) {
                context.fill(gutter.x + 1, y, gutter.x + 3, y + 9, Theme.DANGER);
                if (mouseX >= gutter.x && mouseX < gutter.x + gutter.w && mouseY >= y && mouseY < y + CODE_LINE_H) {
                    hoverMessage = diagnosticMessageForLine(lineIdx);
                }
            }
            context.text(font, n, gutter.x + gutter.w - 4 - font.width(n), y,
                    error ? Theme.DANGER : lineIdx == cursorLine ? Theme.TEXT : Theme.TEXT_DIM, false);
            ScriptSyntax.State start = lineIdx < states.size() ? states.get(lineIdx) : ScriptSyntax.State.CODE;
            drawSyntaxLine(context, line, code.x + 4 - editor.viewLeft, y, ScriptSyntax.line(line, language, start));
            y += CODE_LINE_H;
        }
        drawIndentGuides(context, code, lines);

        highlightErrorSpans(context, code);
        highlightMatchingBracket(context, code);

        if (editor.focused && blinkOn()) {
            int[] p = cursorPixel(code, editor.cursor);
            context.fill(p[0], p[1], p[0] + 1, p[1] + 9, Theme.TEXT);
        }
        if (hoverMessage != null) {
            int w = Math.min(font.width(hoverMessage) + 8, width - mouseX - 12);
            context.fill(mouseX + 7, mouseY + 3, mouseX + 9 + w, mouseY + 19, Theme.DANGER);
            context.fill(mouseX + 8, mouseY + 4, mouseX + 8 + w, mouseY + 18, Theme.SURFACE_RAISED);
            context.text(font, trimToWidth(hoverMessage, w - 6), mouseX + 11, mouseY + 7, 0xFFFFB0B0, false);
        }
    }

    private String diagnosticMessageForLine(int zeroBasedLine) {
        for (Diagnostic d : diagnostics) {
            if (Math.max(1, d.line()) - 1 == zeroBasedLine) {
                return d.message();
            }
        }
        return null;
    }

    /** Block-comment / multi-line string state at the start of each line, recomputed only after edits. */
    private List<ScriptSyntax.State> syntaxStates(List<String> lines) {
        if (cachedStatesVersion != editVersion || cachedStatesBuffer != active || cachedStates.size() != lines.size()) {
            cachedStates = ScriptSyntax.startStates(lines, inferLanguage(currentScriptName).name());
            cachedStatesVersion = editVersion;
            cachedStatesBuffer = active;
        }
        return cachedStates;
    }

    private void drawSyntaxLine(GuiGraphicsExtractor context, String line, int x, int y, ScriptSyntax.LineResult result) {
        int drawX = x;
        int pos = 0;
        for (ScriptSyntax.Span span : result.spans()) {
            if (span.start() > pos) {
                String gap = line.substring(pos, span.start());
                context.text(font, gap, drawX, y, ScriptSyntax.color(ScriptSyntax.Kind.PLAIN), false);
                drawX += font.width(gap);
            }
            String part = line.substring(span.start(), Math.min(span.end(), line.length()));
            context.text(font, part, drawX, y, ScriptSyntax.color(span.kind()), false);
            drawX += font.width(part);
            pos = Math.min(span.end(), line.length());
        }
        if (pos < line.length()) {
            context.text(font, line.substring(pos), drawX, y, ScriptSyntax.color(ScriptSyntax.Kind.PLAIN), false);
        }
    }

    /** Faint vertical lines at each 4-space indent level of the visible lines. */
    private void drawIndentGuides(GuiGraphicsExtractor context, Rect code, List<String> lines) {
        int spaceW = Math.max(1, font.width("    "));
        int y = code.y + 3;
        for (int lineIdx = editor.scrollLine; lineIdx < lines.size(); lineIdx++) {
            if (y > code.y + code.h - CODE_LINE_H) {
                break;
            }
            String line = lines.get(lineIdx);
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            if (line.isBlank()) {
                indent = 0;
            }
            for (int level = 1; level * 4 <= indent; level++) {
                int gx = code.x + 4 + (level - 1) * spaceW - editor.viewLeft;
                if (gx > code.x) {
                    context.fill(gx, y, gx + 1, y + CODE_LINE_H, 0x18FFFFFF);
                }
            }
            y += CODE_LINE_H;
        }
    }

    private void drawFindMatches(GuiGraphicsExtractor context, Rect code, List<String> lines) {
        if (findMode == FindMode.NONE || findMode == FindMode.GOTO || findQuery.isEmpty()) {
            return;
        }
        refreshFindMatches();
        if (findMatches.isEmpty()) {
            return;
        }
        int firstVisible = editor.scrollLine;
        int lastVisible = firstVisible + Math.max(1, (code.h - 6) / CODE_LINE_H);
        int offset = 0;
        int lineIdx = 0;
        int matchIdx = 0;
        while (lineIdx < lines.size() && lineIdx <= lastVisible && matchIdx < findMatches.size()) {
            String line = lines.get(lineIdx);
            int lineEndOffset = offset + line.length();
            while (matchIdx < findMatches.size() && findMatches.get(matchIdx).start() <= lineEndOffset) {
                TextSearch.Match match = findMatches.get(matchIdx);
                if (match.start() >= offset && lineIdx >= firstVisible) {
                    int s = match.start() - offset;
                    int e = Math.min(match.end() - offset, line.length());
                    int y = code.y + 3 + (lineIdx - editor.scrollLine) * CODE_LINE_H;
                    int sx = code.x + 4 + font.width(line.substring(0, s)) - editor.viewLeft;
                    int ex = code.x + 4 + font.width(line.substring(0, e)) - editor.viewLeft;
                    context.fill(sx, y, Math.max(sx + 2, ex), y + 9, matchIdx == findCurrent ? 0x90E5A00D : 0x50E5C07B);
                }
                matchIdx++;
            }
            offset = lineEndOffset + 1;
            lineIdx++;
        }
    }

    private void ensureSuggestionVisible() {
        int visible = Math.clamp(visibleSuggestions.size(), 1, 8);
        if (suggestionSelected < suggestionScroll) {
            suggestionScroll = suggestionSelected;
        } else if (suggestionSelected >= suggestionScroll + visible) {
            suggestionScroll = suggestionSelected - visible + 1;
        }
        int maxScroll = Math.max(0, visibleSuggestions.size() - visible);
        suggestionScroll = Math.clamp(suggestionScroll, 0, maxScroll);
    }

    private String problemsLabel() {
        return "Problems (" + diagnostics.size() + ")";
    }

    private String consoleLabel() {
        return "Console (" + ScriptConsole.size() + ")";
    }

    private void renderConsole(GuiGraphicsExtractor context, Rect panel) {
        List<ScriptConsole.Line> lines = ScriptConsole.lines();
        int rows = Math.max(1, (panel.h - 18) / DIAG_ROW_H);
        int maxScroll = Math.max(0, lines.size() - rows);
        long version = ScriptConsole.version();
        if (version != consoleSeenVersion) {
            consoleSeenVersion = version;
            consoleScroll = maxScroll; // follow new output
        }
        consoleScroll = Math.clamp(consoleScroll, 0, maxScroll);
        if (lines.isEmpty()) {
            context.text(font, "m0.print(...) output, run results and script errors appear here. Ctrl+Shift+R runs the selection.",
                    panel.x + 6, panel.y + 18, Theme.TEXT_DIM, false);
            return;
        }
        java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("HH:mm:ss");
        for (int i = 0; i < rows && consoleScroll + i < lines.size(); i++) {
            ScriptConsole.Line line = lines.get(consoleScroll + i);
            int color = switch (line.level()) {
                case ERROR -> 0xFFFF9D9D;
                case RESULT -> Theme.SUCCESS;
                case INFO -> Theme.TEXT;
            };
            String text = time.format(new java.util.Date(line.timeMs())) + "  " + line.text();
            context.text(font, trimToWidth(text, Math.max(40, panel.w - 12)), panel.x + 6, panel.y + 16 + i * DIAG_ROW_H + 1, color, false);
        }
    }

    private void renderDiagnostics(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect panel = diagnosticsRect();
        context.fill(panel.x, panel.y, panel.x + panel.w, panel.y + panel.h, Theme.SURFACE);
        String problems = problemsLabel();
        String console = consoleLabel();
        int consoleX = panel.x + 6 + font.width(problems) + 12;
        context.text(font, problems, panel.x + 6, panel.y + 4, consoleTab ? Theme.TEXT_DIM : Theme.TEXT);
        context.text(font, console, consoleX, panel.y + 4, consoleTab ? Theme.TEXT : Theme.TEXT_DIM);
        context.fill(consoleTab ? consoleX : panel.x + 6, panel.y + 13,
                (consoleTab ? consoleX + font.width(console) : panel.x + 6 + font.width(problems)), panel.y + 14, Theme.ACCENT);
        int outputX = consoleX + font.width(console) + 14;
        if (!outputText.isBlank()) {
            context.text(font, trimToWidth(outputText, Math.max(40, panel.x + panel.w - outputX - 6)), outputX, panel.y + 4, Theme.TEXT_MUTED, false);
        }
        if (consoleTab) {
            String clear = "Clear";
            int clearX = panel.x + panel.w - font.width(clear) - 6;
            boolean hoverClear = mouseX >= clearX && mouseY >= panel.y && mouseY < panel.y + 14;
            context.text(font, clear, clearX, panel.y + 4, hoverClear ? Theme.TEXT : Theme.TEXT_MUTED, false);
            renderConsole(context, panel);
            return;
        }

        int rows = Math.max(1, (panel.h - 18) / DIAG_ROW_H);
        int maxScroll = Math.max(0, diagnostics.size() - rows);
        diagnosticsScroll = Math.clamp(diagnosticsScroll, 0, maxScroll);
        for (int i = 0; i < rows; i++) {
            int idx = diagnosticsScroll + i;
            if (idx >= diagnostics.size()) {
                break;
            }
            int y = panel.y + 16 + i * DIAG_ROW_H;
            Diagnostic diagnostic = diagnostics.get(idx);
            boolean hover = panel.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + DIAG_ROW_H;
            if (hover) {
                context.fill(panel.x + 1, y, panel.x + panel.w - 1, y + DIAG_ROW_H, Theme.ACCENT_SOFT);
            }
            int color = "ERROR".equals(diagnostic.severity()) ? 0xFFFF9D9D : 0xFFE8D49E;
            context.text(font, trimToWidth(diagnostic.rowText(), Math.max(40, panel.w - 12)), panel.x + 6, y + 1, color, false);
        }
    }

    private void renderDocs(GuiGraphicsExtractor context) {
        Rect body = docsRect();
        context.fill(body.x, body.y, body.x + body.w, body.y + body.h, Theme.SURFACE);
        int rows = Math.max(1, (body.h - 12) / CODE_LINE_H);
        int maxScroll = Math.max(0, docsLines.size() - rows);
        docsScroll = Math.clamp(docsScroll, 0, maxScroll);

        int y = body.y + 4;
        for (int i = docsScroll; i < docsLines.size(); i++) {
            if (y > body.y + body.h - CODE_LINE_H) {
                break;
            }
            String line = docsLines.get(i);
            int color = line.startsWith("[") ? Theme.ACCENT_TEXT : Theme.TEXT;
            context.text(font, line, body.x + 8, y, color, false);
            y += CODE_LINE_H;
        }
    }

    private void renderSuggestions(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Rect drop = suggestionsRect();
        int breadcrumbH = 11;
        int visible = Math.clamp(visibleSuggestions.size(), 1, 8);
        int maxScroll = Math.max(0, visibleSuggestions.size() - visible);
        suggestionScroll = Math.clamp(suggestionScroll, 0, maxScroll);
        context.fill(drop.x, drop.y, drop.x + drop.w, drop.y + breadcrumbH + visible * SUGGESTION_ROW_H, Theme.SURFACE_RAISED);
        String breadcrumb = suggestionBreadcrumb();
        context.text(font, breadcrumb, drop.x + 4, drop.y + 1, Theme.ACCENT_TEXT, false);

        for (int i = 0; i < visible; i++) {
            int idx = suggestionScroll + i;
            if (idx >= visibleSuggestions.size()) {
                break;
            }
            int y = drop.y + breadcrumbH + i * SUGGESTION_ROW_H;
            if (idx == suggestionSelected) {
                context.fill(drop.x + 1, y, drop.x + drop.w - 1, y + SUGGESTION_ROW_H, SELECTION);
            }
            boolean hover = mouseX >= drop.x && mouseX < drop.x + drop.w && mouseY >= y && mouseY < y + SUGGESTION_ROW_H;
            DisplaySuggestion ds = visibleSuggestions.get(idx);
            Suggestion suggestion = ds.suggestion();
            String marker = ds.groupHeader() && ds.hiddenOverloads() > 0
                    ? (Boolean.TRUE.equals(expandedMethodGroups.get(ds.groupKey())) ? "[-] " : "[+] ")
                    : "";
            String label = marker + suggestionKindTag(suggestion.kind()) + " " + suggestion.token();
            if (ds.groupHeader() && ds.hiddenOverloads() > 0) {
                label += "  (+" + ds.hiddenOverloads() + " overloads)";
            }
            if (suggestion.detail() != null && !suggestion.detail().isBlank()) {
                label += "  : " + suggestion.detail();
            }
            int color = hover ? Theme.TEXT : suggestionKindColor(suggestion.kind());
            context.text(font, label, drop.x + 4, y + 1, color, false);
        }
    }

    private boolean createScript() {
        String base = "new_script";
        String name = base + KOTLIN_EXT;
        int i = 1;
        while (allScripts.contains(name) || findBuffer(name) != null) {
            name = base + "_" + i + KOTLIN_EXT;
            i++;
        }
        activate(new Buffer(name, newScriptTemplate(name), null));
        editor.cursor = editor.text.length();
        editor.focused = true;
        outputText = "Created " + name + " (not saved yet; rename it, then Ctrl+S)";
        return true;
    }

    private boolean renameScript() {
        String current = selectedScriptName();
        if (current == null) {
            outputText = "Select a script first.";
            return true;
        }
        String requestedName = renameDraft == null ? "" : renameDraft.trim();
        String renamed = requestedName.isBlank()
                ? nextName(current, "_renamed", allScripts)
                : normalizeScriptNameForRename(requestedName, current);
        if (renamed.indexOf('/') >= 0 || renamed.indexOf('\\') >= 0 || renamed.indexOf(':') >= 0) {
            outputText = "Invalid script name.";
            return true;
        }
        if (renamed.equals(current)) {
            outputText = "Rename target is unchanged.";
            return true;
        }
        try {
            if (!ScriptStorage.renameScript(current, renamed)) {
                outputText = "Rename failed: target already exists.";
                return true;
            }
            RECENT.rename(current, renamed);
            Buffer open = findBuffer(current);
            if (open != null) {
                open.name = renamed;
            }
            currentScriptName = renamed;
            renameDraft = renamed;
            renameFieldFocused = false;
            refreshScripts();
            selectByName(renamed, true);
            outputText = "Renamed to " + renamed;
        } catch (IOException e) {
            outputText = "Rename error: " + e.getMessage();
        }
        return true;
    }

    private boolean duplicateScript() {
        String current = selectedScriptName();
        if (current == null) {
            outputText = "Select a script first.";
            return true;
        }
        String duplicate = nextName(current, "_copy", allScripts);
        try {
            ScriptStorage.writeScript(duplicate, editor.text);
            refreshScripts();
            selectByName(duplicate, false);
            outputText = "Duplicated to " + duplicate;
        } catch (IOException e) {
            outputText = "Duplicate error: " + e.getMessage();
        }
        return true;
    }

    private boolean deleteScript() {
        String current = selectedScriptName();
        if (current == null) {
            outputText = "Select a script first.";
            return true;
        }
        try {
            ScriptStorage.deleteScript(current);
            RECENT.remove(current);
            Buffer open = findBuffer(current);
            if (open != null) {
                pendingClose = open; // deleting is the confirmation
                closeBuffer(open);
            }
            outputText = "Deleted " + current;
            refreshScripts();
            if (!visibleScripts.isEmpty() && (active == null || active.savedText == null && active.state.text.isEmpty())) {
                openScriptByVisibleIndex(Math.clamp(selectedScriptIndex, 0, visibleScripts.size() - 1), true);
            }
        } catch (IOException e) {
            outputText = "Delete error: " + e.getMessage();
        }
        return true;
    }

    private boolean cycleSort() {
        sortMode = switch (sortMode) {
            case NAME_ASC -> SortMode.NAME_DESC;
            case NAME_DESC -> SortMode.LANGUAGE;
            case LANGUAGE -> SortMode.NAME_ASC;
        };
        refreshScripts();
        return true;
    }

    private boolean cycleFilter() {
        filterMode = switch (filterMode) {
            case ALL -> FilterMode.GROOVY;
            case GROOVY -> FilterMode.KOTLIN;
            case KOTLIN -> FilterMode.JAVASCRIPT;
            case JAVASCRIPT -> FilterMode.ALL;
        };
        refreshScripts();
        return true;
    }

    private boolean saveScript() {
        currentScriptName = normalizeScriptName(currentScriptName);
        diagnostics.clear();
        diagnostics.addAll(validateSyntax(editor.text, inferLanguage(currentScriptName), true));
        markStrictValidationComplete();
        try {
            String text = normalize(editor.text);
            ScriptStorage.writeScript(currentScriptName, text);
            if (active != null) {
                active.name = currentScriptName;
                active.savedText = text;
            }
            pendingClose = null;
            outputText = "Saved " + currentScriptName + (diagnostics.isEmpty() ? "" : " (" + diagnostics.size() + " problem(s))");
            refreshScripts();
            int idx = visibleScripts.indexOf(currentScriptName);
            if (idx >= 0) {
                selectedScriptIndex = idx;
            }
        } catch (IOException e) {
            outputText = "Save error: " + e.getMessage();
        }
        return true;
    }

    /** Ctrl+Shift+S: saves every open file with changes. */
    private boolean saveAll() {
        int saved = 0;
        for (Buffer buffer : List.copyOf(SESSION)) {
            if (!buffer.dirty()) {
                continue;
            }
            try {
                buffer.name = normalizeScriptName(buffer.name);
                String text = normalize(buffer.state.text);
                ScriptStorage.writeScript(buffer.name, text);
                buffer.savedText = text;
                saved++;
            } catch (IOException e) {
                outputText = "Save error (" + buffer.name + "): " + e.getMessage();
                return true;
            }
        }
        if (active != null) {
            currentScriptName = active.name;
        }
        refreshScripts();
        outputText = saved == 0 ? "Nothing to save." : "Saved " + saved + " file(s).";
        return true;
    }

    private boolean runScript() {
        diagnostics.clear();
        Language language = inferLanguage(currentScriptName);
        diagnostics.addAll(validateSyntax(editor.text, language, true));
        markStrictValidationComplete();
        if (!diagnostics.isEmpty()) {
            outputText = "Execution cancelled: syntax errors present.";
            return true;
        }

        String scriptSnapshot = editor.text;
        Map<String, Object> context = ScriptTypes.defaultContext();
        ScriptManager manager = ScriptTypes.managerFor(currentScriptName);
        boolean async = scriptRunsAsync(scriptSnapshot);
        return startRun(currentScriptName, manager, scriptSnapshot, context, async);
    }

    private boolean startRun(String name, ScriptManager manager, String source, Map<String, Object> context, boolean async) {
        consoleTab = true;
        outputText = async ? "Running in the background... (/runscript stop to cancel)" : "Running...";
        ScriptConsole.add(ScriptConsole.Level.INFO, "▶ " + name + (async ? " (async)" : ""));
        ScriptRunner.run(name, manager, source, context, async, outcome -> {
            outputText = outcome.success() ? outcome.displayText() : "Run error: " + outcome.error();
            if (outcome.success()) {
                ScriptConsole.add(ScriptConsole.Level.RESULT, "✔ " + name + " → " + outcome.displayText());
            }
        });
        return true;
    }

    /** Ctrl+J: lists ready-made snippets for the current language; Enter/Tab inserts at the cursor. */
    private boolean showSnippets() {
        String language = inferLanguage(currentScriptName).name().toLowerCase(Locale.ROOT);
        String indent = currentLineIndent();
        suggestions.clear();
        int score = 0;
        for (me.m0dii.modules.scripting.ScriptSnippets.Snippet snippet : me.m0dii.modules.scripting.ScriptSnippets.forLanguage(language)) {
            suggestions.add(new Suggestion("snippet", snippet.label(),
                    me.m0dii.modules.scripting.ScriptSnippets.indented(snippet.body(), indent),
                    SuggestionKind.SNIPPET, snippet.detail(), score++));
        }
        rebuildVisibleSuggestions();
        suggestionReplaceStart = editor.cursor;
        suggestionReplaceEnd = editor.cursor;
        suggestionsVisible = !suggestions.isEmpty();
        suggestionSelected = 0;
        suggestionScroll = 0;
        outputText = "Snippets: Up/Down to choose, Enter to insert, Esc to close.";
        return true;
    }

    private String currentLineIndent() {
        int lineStart = editor.text.lastIndexOf('\n', Math.max(0, editor.cursor - 1)) + 1;
        if (editor.cursor == 0) {
            lineStart = 0;
        }
        int i = lineStart;
        while (i < editor.text.length() && (editor.text.charAt(i) == ' ' || editor.text.charAt(i) == '\t')) {
            i++;
        }
        return editor.text.substring(lineStart, i);
    }

    /** Ctrl+Shift+F: re-indents the whole script from its brackets, keeping the cursor on the same line. */
    private boolean formatCode() {
        String before = editor.text;
        String formatted = me.m0dii.modules.scripting.ScriptFormatter.reindent(before, "    ");
        if (formatted.equals(before)) {
            outputText = "Already formatted.";
            return true;
        }
        int line = countLines(before.substring(0, Math.clamp(editor.cursor, 0, before.length())));
        saveUndo();
        editor.text = formatted;
        int offset = 0;
        for (int i = 0; i < line && offset >= 0; i++) {
            offset = formatted.indexOf('\n', offset) + 1;
            if (offset == 0) {
                offset = formatted.length();
                break;
            }
        }
        int lineEnd = formatted.indexOf('\n', offset);
        editor.cursor = lineEnd < 0 ? formatted.length() : lineEnd;
        editor.selectionAnchor = -1;
        markEdited();
        clampEditor();
        outputText = "Formatted (Ctrl+Z to undo).";
        return true;
    }

    /** Ctrl+Shift+R: runs only the selected code (handy for trying a line or two). */
    private boolean runSelection() {
        if (!hasSelection()) {
            outputText = "Select some code first (Ctrl+Shift+R runs the selection).";
            return true;
        }
        String selection = selectedTextOrAll();
        ScriptManager manager = ScriptTypes.managerFor(currentScriptName);
        return startRun(currentScriptName + " (selection)", manager, selection, ScriptTypes.defaultContext(),
                scriptRunsAsync(editor.text));
    }

    private boolean toggleScriptAsync() {
        saveUndo();
        boolean next = !scriptRunsAsync(editor.text);
        editor.text = withAsyncDirective(editor.text, next);
        editor.cursor = Math.clamp(editor.cursor, 0, editor.text.length());
        editor.selectionAnchor = -1;
        markEdited();
        outputText = "Per-script async: " + (next ? "ON" : "OFF");
        return true;
    }

    private boolean validateNow() {
        diagnostics.clear();
        diagnostics.addAll(validateSyntax(editor.text, inferLanguage(currentScriptName), true));
        markStrictValidationComplete();
        outputText = diagnostics.isEmpty() ? "No syntax issues found." : "Found " + diagnostics.size() + " problem(s).";
        return true;
    }

    private List<Diagnostic> validateSyntax(String source, Language language, boolean strict) {
        List<Diagnostic> out = new ArrayList<>(lightweightValidation(source, language));
        if (!strict) {
            return dedupeDiagnostics(out);
        }
        if (language == Language.GROOVY) {
            try {
                new GroovyShell().parse(source == null ? "" : source);
            } catch (Exception e) {
                int line = parseIntAfter("line", e.getMessage(), 1);
                out.add(new Diagnostic("ERROR", currentScriptName, line, 1, safeMessage(e.getMessage(), "Groovy parse error")));
            }
        } else if (language == Language.KOTLIN) {
            out.addAll(validateKotlinScript(source));
        } else {
            out.addAll(validateJavaScriptScript(source));
        }
        return dedupeDiagnostics(out);
    }

    private List<Diagnostic> validateKotlinScript(String source) {
        List<Diagnostic> out = new ArrayList<>();
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("kts");
            if (engine == null) {
                out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, "Kotlin script engine not found."));
                return out;
            }
            if (!(engine instanceof Compilable compilable)) {
                out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, "Kotlin script engine does not support compilation diagnostics."));
                return out;
            }
            KotlinValidationSource wrapped = wrapKotlinValidationSource(source);
            compilable.compile(wrapped.source());
        } catch (ScriptException e) {
            int line = e.getLineNumber() > 0 ? e.getLineNumber() : parseIntAfter("line", e.getMessage(), 1);
            int col = e.getColumnNumber() > 0 ? e.getColumnNumber() : parseIntAfter("column", e.getMessage(), 1);
            line = unwrapKotlinValidationLine(source, line);
            out.add(new Diagnostic("ERROR", currentScriptName, line, col, cleanupCompilerMessage(safeMessage(e.getMessage(), "Kotlin parse error"))));
        } catch (Exception e) {
            out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, safeMessage(e.getMessage(), "Kotlin validation failed")));
        }
        return out;
    }

    private List<Diagnostic> validateJavaScriptScript(String source) {
        List<Diagnostic> out = new ArrayList<>();
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
            if (engine == null) {
                engine = new ScriptEngineManager().getEngineByName("js");
            }
            if (engine == null) {
                out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, "JavaScript engine not found."));
                return out;
            }
            if (!(engine instanceof Compilable compilable)) {
                out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, "JavaScript engine does not support compilation diagnostics."));
                return out;
            }
            compilable.compile(source == null ? "" : source);
        } catch (ScriptException e) {
            int line = e.getLineNumber() > 0 ? e.getLineNumber() : parseIntAfter("line", e.getMessage(), 1);
            int col = e.getColumnNumber() > 0 ? e.getColumnNumber() : parseIntAfter("column", e.getMessage(), 1);
            out.add(new Diagnostic("ERROR", currentScriptName, line, col, cleanupCompilerMessage(safeMessage(e.getMessage(), "JavaScript parse error"))));
        } catch (Exception e) {
            out.add(new Diagnostic("ERROR", currentScriptName, 1, 1, safeMessage(e.getMessage(), "JavaScript validation failed")));
        }
        return out;
    }

    private void jumpToDiagnostic(Diagnostic d) {
        int line = Math.max(1, d.line());
        int col = Math.max(1, d.column());
        List<String> lines = splitLines(editor.text);
        int targetLine = Math.clamp(line - 1L, 0, Math.max(0, lines.size() - 1));
        int index = 0;
        for (int i = 0; i < targetLine; i++) index += lines.get(i).length() + 1;
        int inLine = Math.clamp(col - 1L, 0, lines.get(targetLine).length());
        editor.cursor = index + inLine;
        editor.selectionAnchor = -1;
        editor.focused = true;
        clampEditor();
    }

    private void copyDiagnosticToClipboard(Diagnostic diagnostic) {
        if (minecraft == null || diagnostic == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(diagnostic.rowText());
        outputText = diagnostic.rowText();
    }

    private void refreshScripts() {
        String keepName = selectedScriptName();
        allScripts.clear();
        allScripts.addAll(ScriptStorage.listScripts());
        visibleScripts.clear();
        for (String script : allScripts) {
            if (!matchesFilter(script)) {
                continue;
            }
            if (!scriptSearch.isBlank() && !script.toLowerCase(Locale.ROOT).contains(scriptSearch.toLowerCase(Locale.ROOT))) {
                continue;
            }
            visibleScripts.add(script);
        }
        applySort();
        if (keepName != null) {
            selectedScriptIndex = visibleScripts.indexOf(keepName);
        }
        if (selectedScriptIndex < 0 && !visibleScripts.isEmpty()) {
            selectedScriptIndex = 0;
        }
        if (keepName == null && selectedScriptIndex >= 0 && selectedScriptIndex < visibleScripts.size()) {
            renameDraft = visibleScripts.get(selectedScriptIndex);
        }
    }

    private boolean matchesFilter(String script) {
        if (filterMode == FilterMode.ALL) {
            return true;
        }
        if (filterMode == FilterMode.GROOVY) {
            return script.toLowerCase(Locale.ROOT).endsWith(GROOVY_EXT);
        }
        if (filterMode == FilterMode.KOTLIN) {
            return script.toLowerCase(Locale.ROOT).endsWith(KOTLIN_EXT);
        }
        return script.toLowerCase(Locale.ROOT).endsWith(JAVASCRIPT_EXT);
    }

    private void applySort() {
        Comparator<String> comp = switch (sortMode) {
            case NAME_ASC -> Comparator.naturalOrder();
            case NAME_DESC -> Comparator.<String>naturalOrder().reversed();
            case LANGUAGE ->
                    Comparator.comparing((String s) -> inferLanguage(s).name()).thenComparing(Comparator.naturalOrder());
        };
        visibleScripts.sort(comp);
    }

    private int visibleScriptIndexAt(double mouseY) {
        Rect body = scriptsBodyRect();
        if (mouseY < body.y || mouseY >= body.y + body.h) {
            return -1;
        }
        int row = (int) ((mouseY - body.y) / SCRIPT_ROW_H);
        int idx = scriptsScroll + row;
        return idx < visibleScripts.size() ? idx : -1;
    }

    private int diagnosticIndexAt(double mouseY) {
        Rect panel = diagnosticsRect();
        int row = (int) ((mouseY - (panel.y + 16)) / DIAG_ROW_H);
        int idx = diagnosticsScroll + row;
        return idx < diagnostics.size() ? idx : -1;
    }

    private int suggestionIndexAt(double mouseY) {
        Rect drop = suggestionsRect();
        int row = (int) ((mouseY - (drop.y + 11)) / SUGGESTION_ROW_H);
        int idx = suggestionScroll + row;
        return idx >= 0 && idx < visibleSuggestions.size() ? idx : -1;
    }

    private void openScriptByVisibleIndex(int idx, boolean keepOutput) {
        if (idx < 0 || idx >= visibleScripts.size()) {
            return;
        }
        selectedScriptIndex = idx;
        String previousOutput = outputText;
        openScriptByName(visibleScripts.get(idx));
        if (keepOutput) {
            outputText = previousOutput;
        }
    }

    private void selectByName(String name, boolean keepOutput) {
        int idx = visibleScripts.indexOf(name);
        if (idx >= 0) {
            openScriptByVisibleIndex(idx, keepOutput);
        }
    }

    private String selectedScriptName() {
        if (selectedScriptIndex < 0 || selectedScriptIndex >= visibleScripts.size()) {
            return null;
        }
        return visibleScripts.get(selectedScriptIndex);
    }

    private void rebuildDocs() {
        docsLines.clear();
        docsLines.add("[Files]");
        docsLines.add("- Files open in tabs above the editor; ● = unsaved. Click × or middle-click to close, Ctrl+W closes,");
        docsLines.add("  Ctrl+Tab / Ctrl+Shift+Tab switch. Open files and unsaved edits stay until you quit the game.");
        docsLines.add("- Ctrl+N new script, Ctrl+S save, Ctrl+Shift+S save all");
        docsLines.add("");
        docsLines.add("[Run]");
        docsLines.add("- F5 or Ctrl+R run, Shift+F5 stop all scripts, Ctrl+Shift+R run the selection only");
        docsLines.add("- First line '// @async on' runs the script in the background (stop: Shift+F5 or /runscript stop)");
        docsLines.add("- The bottom panel has Problems and Console tabs; click a console line to copy it, 'Clear' empties it");
        docsLines.add("");
        docsLines.add("[Navigate]");
        docsLines.add("- Ctrl+F find, Ctrl+H replace, Enter/Shift+Enter (or F3/Shift+F3) next/previous,");
        docsLines.add("  Alt+C match case, Alt+W whole word, Tab switches to the replace field, Ctrl+Enter replaces all");
        docsLines.add("- Ctrl+G go to line (line or line:column), Ctrl+O go to a function/class/value");
        docsLines.add("- Ctrl+E recent files (Ctrl+E, Enter switches back to the previous file)");
        docsLines.add("- Ctrl+Left/Right move by word, Home jumps to the first character, Ctrl+Home/End to the file start/end");
        docsLines.add("- Hover a red line number to see its problem");
        docsLines.add("");
        docsLines.add("[Edit]");
        docsLines.add("- Ctrl+/ comment or uncomment lines, Ctrl+D duplicate, Alt+Up/Down move lines, Ctrl+Shift+K delete line");
        docsLines.add("- Ctrl+X with nothing selected cuts the line, Ctrl+Backspace/Delete removes a word");
        docsLines.add("- Brackets and quotes close themselves; typing one with a selection wraps it");
        docsLines.add("- Ctrl+J snippet palette, Ctrl+Shift+F re-indent the script");
        docsLines.add("");
        docsLines.add("[m0 helper]");
        docsLines.add("- m0.print(x)            line in the Console tab");
        docsLines.add("- m0.message(x)          chat line only you see; m0.actionbar(x) for the action bar");
        docsLines.add("- m0.chat(x), m0.command(\"time set day\")   send to the server");
        docsLines.add("- m0.placeholders(\"{player.x}\"), m0.macro(\"id\")");
        docsLines.add("- m0.sleep(ms)           async scripts only");
        docsLines.add("- m0.sync { ... } / m0.call { ... }   run on the game thread from async scripts");
        docsLines.add("- m0.checkCancelled()    call in long loops so timeouts and stop work");
        docsLines.add("");
        docsLines.add("- Ctrl+Z undo, Ctrl+Y / Ctrl+Shift+Z redo (per file)");
        docsLines.add("- Tab/Shift+Tab indent and unindent");
        docsLines.add("- Placeholder suggestions trigger after '{'");
        docsLines.add("- Supported extensions: .groovy, .kts, .js");
        docsLines.add("");
        docsLines.add("[Variables]");
        docsLines.add("client, source, player, world, options, server");
        docsLines.add("");
        docsLines.addAll(MacroPlaceholders.getPlaceholderDocs());
    }

    private void updateClickCount(int cursorIdx) {
        long now = System.currentTimeMillis();
        if (cursorIdx == editor.lastClickCursor && (now - editor.lastClickAt) < 320L) {
            editor.clickCount++;
            if (editor.clickCount > 3) {
                editor.clickCount = 1;
            }
        } else {
            editor.clickCount = 1;
        }
        editor.lastClickCursor = cursorIdx;
        editor.lastClickAt = now;
    }

    private void selectWordAt(int index) {
        int c = Math.clamp(index, 0, editor.text.length());
        int start = c;
        int end = c;
        while (start > 0 && isWord(editor.text.charAt(start - 1))) start--;
        while (end < editor.text.length() && isWord(editor.text.charAt(end))) end++;
        editor.selectionAnchor = start;
        editor.cursor = end;
    }

    private void selectLineAt(int index) {
        int start = lineStart(index);
        int end = lineEnd(index);
        editor.selectionAnchor = start;
        editor.cursor = end;
    }

    private boolean hasSelection() {
        return editor.selectionAnchor >= 0 && editor.selectionAnchor != editor.cursor;
    }

    private String selectedTextOrAll() {
        if (!hasSelection()) {
            return editor.text;
        }
        int start = Math.min(editor.selectionAnchor, editor.cursor);
        int end = Math.max(editor.selectionAnchor, editor.cursor);
        return editor.text.substring(start, end);
    }

    private void replaceSelectionOrInsert(String insert, boolean saveHistory) {
        if (insert == null || insert.isEmpty()) {
            return;
        }
        if (saveHistory) {
            saveUndo();
        }
        String value = normalize(insert);
        if (hasSelection()) {
            int start = Math.min(editor.selectionAnchor, editor.cursor);
            int end = Math.max(editor.selectionAnchor, editor.cursor);
            editor.text = editor.text.substring(0, start) + value + editor.text.substring(end);
            editor.cursor = start + value.length();
            editor.selectionAnchor = -1;
        } else {
            editor.text = editor.text.substring(0, editor.cursor) + value + editor.text.substring(editor.cursor);
            editor.cursor += value.length();
        }
        markEdited();
        clampEditor();
    }

    private void deleteSelection(boolean saveHistory) {
        if (!hasSelection()) {
            return;
        }
        if (saveHistory) {
            saveUndo();
        }
        int start = Math.min(editor.selectionAnchor, editor.cursor);
        int end = Math.max(editor.selectionAnchor, editor.cursor);
        editor.text = editor.text.substring(0, start) + editor.text.substring(end);
        editor.cursor = start;
        editor.selectionAnchor = -1;
        markEdited();
        clampEditor();
    }

    private void insertNewlineWithAutoIndent() {
        saveUndo();
        int startOfLine = lineStart(editor.cursor);
        int endOfLine = lineEnd(editor.cursor);
        String line = editor.text.substring(startOfLine, endOfLine);
        String indent = leadingIndent(line);
        String beforeCursor = editor.text.substring(startOfLine, editor.cursor).trim();
        boolean opens = beforeCursor.endsWith("{") || beforeCursor.endsWith("(") || beforeCursor.endsWith("[");
        char next = editor.cursor < editor.text.length() ? editor.text.charAt(editor.cursor) : 0;
        if (opens && (next == '}' || next == ')' || next == ']') && !hasSelection()) {
            // Between a bracket pair: put the closer on its own line and the cursor on an indented line between.
            String inner = "\n" + indent + "    ";
            replaceSelectionOrInsert(inner + "\n" + indent, false);
            editor.cursor -= indent.length() + 1;
            clampEditor();
            return;
        }
        if (opens) {
            indent += "    ";
        }
        replaceSelectionOrInsert("\n" + indent, false);
    }

    private void indentSelectionOrInsert() {
        if (!hasSelection()) {
            replaceSelectionOrInsert("    ", true);
            return;
        }
        saveUndo();
        int start = Math.min(editor.selectionAnchor, editor.cursor);
        int end = Math.max(editor.selectionAnchor, editor.cursor);
        int lineStart = lineStart(start);
        int lineEnd = lineEnd(end);
        String block = editor.text.substring(lineStart, lineEnd);
        String indented = "    " + block.replace("\n", "\n    ");
        editor.text = editor.text.substring(0, lineStart) + indented + editor.text.substring(lineEnd);
        editor.selectionAnchor = lineStart;
        editor.cursor = lineStart + indented.length();
        markEdited();
        clampEditor();
    }

    private void unindentSelectionOrLine() {
        saveUndo();
        int start = hasSelection() ? Math.min(editor.selectionAnchor, editor.cursor) : lineStart(editor.cursor);
        int end = hasSelection() ? Math.max(editor.selectionAnchor, editor.cursor) : lineEnd(editor.cursor);
        int blockStart = lineStart(start);
        int blockEnd = lineEnd(end);
        String block = editor.text.substring(blockStart, blockEnd);
        String[] lines = block.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("    ")) {
                lines[i] = lines[i].substring(4);
            } else if (lines[i].startsWith("\t")) {
                lines[i] = lines[i].substring(1);
            }
        }
        String joined = String.join("\n", lines);
        editor.text = editor.text.substring(0, blockStart) + joined + editor.text.substring(blockEnd);
        editor.selectionAnchor = hasSelection() ? blockStart : -1;
        editor.cursor = hasSelection() ? blockStart + joined.length() : Math.max(blockStart, editor.cursor - 4);
        markEdited();
        clampEditor();
    }

    private static String leadingIndent(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
        return line.substring(0, i);
    }

    private void moveCursor(int next, boolean shift) {
        int old = editor.cursor;
        editor.cursor = Math.clamp(next, 0, editor.text.length());
        if (shift) {
            editor.selectionAnchor = editor.selectionAnchor < 0 ? old : editor.selectionAnchor;
        } else {
            editor.selectionAnchor = -1;
        }
        clampEditor();
    }

    private int lineStart(int index) {
        int i = Math.clamp(index, 0, editor.text.length());
        while (i > 0 && editor.text.charAt(i - 1) != '\n') i--;
        return i;
    }

    private int lineEnd(int index) {
        int i = Math.clamp(index, 0, editor.text.length());
        while (i < editor.text.length() && editor.text.charAt(i) != '\n') i++;
        return i;
    }

    private int verticalMove(int index, int direction) {
        List<String> lines = splitLines(editor.text);
        int remain = Math.clamp(index, 0, editor.text.length());
        int line = 0;
        int col = 0;
        for (int i = 0; i < lines.size(); i++) {
            int len = lines.get(i).length();
            if (remain <= len) {
                line = i;
                col = remain;
                break;
            }
            remain -= len + 1;
            line = i + 1;
        }
        if (editor.preferredColumn < 0) {
            editor.preferredColumn = col;
        }
        int targetLine = Math.clamp(line + direction, 0, lines.size() - 1);
        int targetCol = Math.min(lines.get(targetLine).length(), editor.preferredColumn);
        int out = 0;
        for (int i = 0; i < targetLine; i++) out += lines.get(i).length() + 1;
        return out + targetCol;
    }

    private void clampEditor() {
        editor.cursor = Math.clamp(editor.cursor, 0, editor.text.length());
        if (editor.selectionAnchor > editor.text.length()) {
            editor.selectionAnchor = editor.text.length();
        }
        if (editor.selectionAnchor < -1) {
            editor.selectionAnchor = -1;
        }
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        Rect code = codeTextRect();
        int[] pixel = cursorPixel(code, editor.cursor);
        int top = code.y + 2;
        int bottom = code.y + code.h - CODE_LINE_H;
        if (pixel[1] < top) {
            editor.scrollLine = Math.max(0, editor.scrollLine - Math.max(1, (top - pixel[1]) / CODE_LINE_H));
        } else if (pixel[1] > bottom) {
            editor.scrollLine += Math.max(1, (pixel[1] - bottom) / CODE_LINE_H);
        }
        int left = code.x + 4;
        int right = code.x + code.w - 6;
        if (pixel[0] < left) {
            editor.viewLeft = Math.max(0, editor.viewLeft - (left - pixel[0]));
        } else if (pixel[0] > right) {
            editor.viewLeft += (pixel[0] - right);
        }
    }

    private int indexFromMouse(int mouseX, int mouseY, Rect code) {
        List<String> lines = splitLines(editor.text);
        int line = Math.clamp(((mouseY - (code.y + 3)) / CODE_LINE_H) + editor.scrollLine, 0, lines.size() - 1);
        String textLine = lines.get(line);
        int localX = Math.max(0, mouseX - (code.x + 4) + editor.viewLeft);
        int col = textLine.length();
        for (int i = 0; i <= textLine.length(); i++) {
            if (font.width(textLine.substring(0, i)) >= localX) {
                col = i;
                break;
            }
        }
        int idx = 0;
        for (int i = 0; i < line; i++) idx += lines.get(i).length() + 1;
        return Math.clamp(idx + col, 0, editor.text.length());
    }

    private int[] cursorPixel(Rect code, int index) {
        List<String> lines = splitLines(editor.text);
        int remaining = Math.clamp(index, 0, editor.text.length());
        int line = 0;
        for (int i = 0; i < lines.size(); i++) {
            int len = lines.get(i).length();
            if (remaining <= len) {
                line = i;
                break;
            }
            remaining -= len + 1;
            line = i + 1;
        }
        String current = lines.get(Math.clamp(line, 0, lines.size() - 1));
        int x = code.x + 4 + font.width(current.substring(0, Math.min(remaining, current.length()))) - editor.viewLeft;
        int y = code.y + 3 + (line - editor.scrollLine) * CODE_LINE_H;
        return new int[]{x, y};
    }

    private void drawSelection(GuiGraphicsExtractor context, Rect code, List<String> lines) {
        if (!hasSelection()) {
            return;
        }
        int start = Math.min(editor.selectionAnchor, editor.cursor);
        int end = Math.max(editor.selectionAnchor, editor.cursor);
        int y = code.y + 3;
        int offset = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineStart = offset;
            int lineEnd = lineStart + line.length();
            if (i >= editor.scrollLine) {
                if (y > code.y + code.h - CODE_LINE_H) {
                    break;
                }
                int s = Math.max(start, lineStart);
                int e = Math.min(end, lineEnd);
                if (e > s) {
                    int sx = code.x + 4 + font.width(line.substring(0, s - lineStart)) - editor.viewLeft;
                    int ex = code.x + 4 + font.width(line.substring(0, e - lineStart)) - editor.viewLeft;
                    context.fill(sx, y, ex, y + 9, SELECTION);
                }
                y += CODE_LINE_H;
            }
            offset = lineEnd + 1;
        }
    }

    private void drawCurrentLineHighlight(GuiGraphicsExtractor context, Rect code) {
        int line = lineNumberOfCursor();
        int y = code.y + 3 + (line - editor.scrollLine) * CODE_LINE_H;
        if (y >= code.y && y <= code.y + code.h - CODE_LINE_H) {
            context.fill(code.x + 1, y, code.x + code.w - 1, y + CODE_LINE_H, CURRENT_LINE);
        }
    }

    private int lineNumberOfCursor() {
        int line = 0;
        for (int i = 0; i < Math.min(editor.cursor, editor.text.length()); i++) {
            if (editor.text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private void highlightErrorSpans(GuiGraphicsExtractor context, Rect code) {
        for (Diagnostic d : diagnostics) {
            int line = Math.max(1, d.line()) - 1;
            int col = Math.max(1, d.column()) - 1;
            int y = code.y + 3 + (line - editor.scrollLine) * CODE_LINE_H;
            if (y < code.y || y > code.y + code.h - CODE_LINE_H) {
                continue;
            }
            context.fill(code.x + 1, y + 8, code.x + code.w - 1, y + 9, 0x90FF5555);
            String lineText = lineTextAt(line);
            int x1 = code.x + 4 + font.width(lineText.substring(0, Math.min(col, lineText.length()))) - editor.viewLeft;
            int x2 = x1 + Math.max(2, font.width(lineText.substring(Math.min(col, lineText.length()), Math.min(col + 1, lineText.length()))));
            context.fill(x1, y + 8, x2, y + 9, 0xFFFF7070);
        }
    }

    private String lineTextAt(int zeroBased) {
        List<String> lines = splitLines(editor.text);
        if (zeroBased < 0 || zeroBased >= lines.size()) {
            return "";
        }
        return lines.get(zeroBased);
    }

    private void highlightMatchingBracket(GuiGraphicsExtractor context, Rect code) {
        int leftIdx = editor.cursor > 0 ? editor.cursor - 1 : -1;
        if (leftIdx < 0 || leftIdx >= editor.text.length()) {
            return;
        }
        char c = editor.text.charAt(leftIdx);
        int match = findMatchingBracket(leftIdx, c);
        if (match < 0) {
            return;
        }
        drawBracketMarker(context, code, leftIdx, 0xFF8FD7FF);
        drawBracketMarker(context, code, match, 0xFF8FD7FF);
    }

    private int findMatchingBracket(int pos, char c) {
        char open;
        char close;
        int dir;
        if (c == '(') {
            open = '(';
            close = ')';
            dir = 1;
        } else if (c == '{') {
            open = '{';
            close = '}';
            dir = 1;
        } else if (c == '[') {
            open = '[';
            close = ']';
            dir = 1;
        } else if (c == ')') {
            open = '(';
            close = ')';
            dir = -1;
        } else if (c == '}') {
            open = '{';
            close = '}';
            dir = -1;
        } else if (c == ']') {
            open = '[';
            close = ']';
            dir = -1;
        } else {
            return -1;
        }

        int depth = 0;
        for (int i = pos; i >= 0 && i < editor.text.length(); i += dir) {
            char v = editor.text.charAt(i);
            if (v == open) {
                depth += dir > 0 ? 1 : -1;
            }
            if (v == close) {
                depth += dir > 0 ? -1 : 1;
            }
            if (depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private void drawBracketMarker(GuiGraphicsExtractor context, Rect code, int idx, int color) {
        int[] p = cursorPixel(code, idx);
        if (p[1] < code.y || p[1] > code.y + code.h - CODE_LINE_H) {
            return;
        }
        int charW = font.width("(");
        context.fill(p[0], p[1], p[0] + Math.max(2, charW), p[1] + 1, color);
        context.fill(p[0], p[1] + 8, p[0] + Math.max(2, charW), p[1] + 9, color);
    }

    private void refreshSuggestions() {
        CodeCompletionContext codeContext = codeCompletionContext(editor.text, editor.cursor);
        if (codeContext != null) {
            List<Suggestion> codeSuggestions = resolveCodeSuggestions(codeContext);
            if (!codeSuggestions.isEmpty()) {
                suggestions.clear();
                suggestions.addAll(codeSuggestions);
                rebuildVisibleSuggestions();
                suggestionReplaceStart = codeContext.replaceStart();
                suggestionReplaceEnd = codeContext.replaceEnd();
                suggestionsVisible = true;
                suggestionSelected = 0;
                suggestionScroll = 0;
                return;
            }
        }

        if (!placeholderSuggestionsInScripts) {
            hideSuggestions();
            return;
        }

        PlaceholderCtx ctx = placeholderContext(editor.text, editor.cursor);
        if (ctx == null) {
            hideSuggestions();
            return;
        }
        suggestions.clear();
        suggestionReplaceStart = ctx.replaceStart();
        suggestionReplaceEnd = editor.cursor;
        suggestions.addAll(matchSuggestions(ctx.prefix()));
        rebuildVisibleSuggestions();
        suggestionsVisible = !suggestions.isEmpty();
        suggestionSelected = 0;
        suggestionScroll = 0;
    }

    private void hideSuggestions() {
        suggestionsVisible = false;
        suggestions.clear();
        visibleSuggestions.clear();
        suggestionReplaceStart = -1;
        suggestionReplaceEnd = -1;
    }

    private void acceptSuggestion() {
        if (!suggestionsVisible || suggestionSelected < 0 || suggestionSelected >= visibleSuggestions.size()) {
            return;
        }
        DisplaySuggestion ds = visibleSuggestions.get(suggestionSelected);
        Suggestion s = ds.suggestion();
        if (ds.groupHeader() && ds.hiddenOverloads() > 0) {
            toggleMethodGroup(ds.groupKey());
            return;
        }
        int start = Math.max(0, suggestionReplaceStart);
        int end = Math.clamp(suggestionReplaceEnd, start, editor.text.length());
        saveUndo();
        String before = editor.text.substring(0, start);
        String after = editor.text.substring(end);
        String insert = s.insertText() == null ? s.token() : s.insertText();
        if (placeholderSuggestionsInScripts && !after.isEmpty() && !after.startsWith("}")
                && insert.equals(s.token()) && !insert.endsWith("}")) {
            insert = insert + "}";
        }
        editor.text = before + insert + after;
        editor.cursor = before.length() + insert.length();
        editor.selectionAnchor = -1;
        markEdited();
        hideSuggestions();
    }

    private void rebuildVisibleSuggestions() {
        visibleSuggestions.clear();
        LinkedHashMap<String, List<Suggestion>> groupedMethods = new LinkedHashMap<>();

        for (Suggestion suggestion : suggestions) {
            if (suggestion.kind() == SuggestionKind.METHOD) {
                String groupKey = methodGroupKey(suggestion);
                groupedMethods.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(suggestion);
            } else {
                visibleSuggestions.add(new DisplaySuggestion(suggestion, false, "", 0));
            }
        }

        for (Map.Entry<String, List<Suggestion>> entry : groupedMethods.entrySet()) {
            String key = entry.getKey();
            List<Suggestion> methods = entry.getValue();
            methods.sort(Comparator.comparingInt(Suggestion::score).thenComparing(Suggestion::token));
            Suggestion best = methods.getFirst();
            int hidden = Math.max(0, methods.size() - 1);
            boolean expanded = Boolean.TRUE.equals(expandedMethodGroups.get(key));
            visibleSuggestions.add(new DisplaySuggestion(best, true, key, hidden));
            if (expanded && methods.size() > 1) {
                for (int i = 1; i < methods.size(); i++) {
                    visibleSuggestions.add(new DisplaySuggestion(methods.get(i), false, key, 0));
                }
            }
        }

        visibleSuggestions.sort(Comparator
                .comparingInt((DisplaySuggestion d) -> d.suggestion().score())
                .thenComparing(d -> d.suggestion().token().toLowerCase(Locale.ROOT)));
    }

    private void toggleMethodGroup(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        boolean next = !Boolean.TRUE.equals(expandedMethodGroups.get(key));
        expandedMethodGroups.put(key, next);
        rebuildVisibleSuggestions();
        suggestionSelected = Math.clamp(suggestionSelected, 0, Math.max(0, visibleSuggestions.size() - 1));
        ensureSuggestionVisible();
    }

    private String suggestionBreadcrumb() {
        CodeCompletionContext ctx = codeCompletionContext(editor.text, editor.cursor);
        if (ctx == null) {
            return "scope: global";
        }
        String q = ctx.qualifier();
        if (q == null || q.isBlank()) {
            return "scope: global";
        }
        String typeKey = resolveQualifierType(q);
        if (typeKey == null) {
            return q + " => ?";
        }
        int lastDot = typeKey.lastIndexOf('.');
        String shortType = lastDot < 0 ? typeKey : typeKey.substring(lastDot + 1);
        return q + " => " + shortType;
    }

    private void saveUndo() {
        undo.push(snapshot());
        redo.clear();
        while (undo.size() > historyLimit()) {
            undo.removeLast();
        }
    }

    private boolean undoEdit() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.push(snapshot());
        restore(undo.pop());
        markEdited();
        return true;
    }

    private boolean redoEdit() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.push(snapshot());
        restore(redo.pop());
        markEdited();
        return true;
    }

    private int historyLimit() {
        return editor.text.length() > 250_000 ? 60 : HISTORY_LIMIT;
    }

    private Snapshot snapshot() {
        return new Snapshot(editor.text, editor.cursor, editor.selectionAnchor, editor.scrollLine, editor.viewLeft);
    }

    private void restore(Snapshot snapshot) {
        editor.text = snapshot.text;
        editor.cursor = snapshot.cursor;
        editor.selectionAnchor = snapshot.anchor;
        editor.scrollLine = snapshot.scrollLine;
        editor.viewLeft = snapshot.viewLeft;
        clampEditor();
    }

    private void markEdited() {
        lastEditAt = System.currentTimeMillis();
        editVersion++;
        globalEdits++;
        pendingClose = null;
    }

    private void markStrictValidationComplete() {
        lastValidationAt = System.currentTimeMillis();
        lastStrictValidatedEditVersion = editVersion;
        lastAutoValidatedEditVersion = editVersion;
    }

    private String trimToWidth(String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 0 || font.width(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        int keep = text.length();
        while (keep > 0 && font.width(text.substring(0, keep) + ellipsis) > maxWidth) {
            keep--;
        }
        return keep <= 0 ? ellipsis : text.substring(0, keep) + ellipsis;
    }

    private boolean blinkOn() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    private boolean isCtrlDown() {
        if (minecraft == null) {
            return false;
        }
        var w = minecraft.getWindow();
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
    }

    private boolean isShiftDown() {
        if (minecraft == null) {
            return false;
        }
        var w = minecraft.getWindow();
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    private Rect tabsArea() {
        return new Rect(PAD, PAD, TAB_W * 3, TAB_H);
    }

    private Rect mainHeaderRect() {
        return new Rect(PAD, PAD + TAB_H + 4, width - PAD * 2, 22);
    }

    private Rect docsRect() {
        return new Rect(PAD, PAD + TAB_H + 4, width - PAD * 2, height - (PAD * 2 + TAB_H + 4));
    }

    private Rect scriptsPaneRect() {
        return new Rect(PAD, PAD + TAB_H + 4, Math.max(220, width / 3), height - (PAD + TAB_H + 4) - PAD);
    }

    private Rect scriptsSearchRect() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 4, p.y + SCRIPT_SEARCH_TOP, p.w - 8, SCRIPT_FIELD_H);
    }

    private Rect scriptsRenameRect() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 4, p.y + SCRIPT_RENAME_TOP, p.w - 8, SCRIPT_FIELD_H);
    }

    private Rect scriptsBodyRect() {
        Rect p = scriptsPaneRect();
        int top = p.y + SCRIPT_BODY_TOP;
        int h = p.h - (SCRIPT_BODY_TOP + 68);
        return new Rect(p.x + 4, top, p.w - 8, Math.max(40, h));
    }

    private Rect editorRect() {
        int top = PAD + TAB_H + 4;
        int bottom = diagnosticsRect().y - 6;
        int left = editorColumnLeft();
        int right = editorColumnRight();
        if (tab == UiTab.MAIN) {
            top += 24;
        }
        top += FILE_TAB_H;
        bottom -= STATUS_H;
        return new Rect(left, top, Math.max(120, right - left), Math.max(90, bottom - top));
    }

    private Rect diagnosticsRect() {
        int left = editorColumnLeft();
        int right = editorColumnRight();
        return new Rect(left, height - 116, Math.max(120, right - left), 106);
    }

    private int editorColumnLeft() {
        return tab == UiTab.SCRIPTS ? scriptsPaneRect().x + scriptsPaneRect().w + 8 : PAD;
    }

    private int editorColumnRight() {
        return width - PAD;
    }

    private Rect gutterRect(Rect code) {
        int lines = Math.max(1, splitLines(editor.text).size());
        int digits = Integer.toString(lines).length();
        int gutterW = 8 + digits * 6;
        return new Rect(code.x, code.y, gutterW, code.h);
    }

    private Rect codeTextRect() {
        Rect panel = editorRect();
        return codeTextRect(panel, gutterRect(panel));
    }

    private Rect codeTextRect(Rect panel, Rect gutter) {
        int left = gutter.x + gutter.w + 5;
        int textW = Math.max(40, panel.x + panel.w - left - 2);
        return new Rect(left, panel.y, textW, panel.h);
    }

    private Rect scriptsBtnCreate() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 4, p.y + p.h - 78, 62, 18);
    }

    private Rect scriptsBtnRename() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 70, p.y + p.h - 78, 62, 18);
    }

    private Rect scriptsBtnDuplicate() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 136, p.y + p.h - 78, 76, 18);
    }

    private Rect scriptsBtnDelete() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 4, p.y + p.h - 56, 62, 18);
    }

    private Rect scriptsBtnSort() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 70, p.y + p.h - 56, 102, 18);
    }

    private Rect scriptsBtnFilter() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 176, p.y + p.h - 56, 102, 18);
    }

    private Rect scriptsBtnSave() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 4, p.y + p.h - 34, 62, 18);
    }

    private Rect scriptsBtnRun() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 78, p.y + p.h - 34, 70, 18);
    }

    private Rect scriptsBtnAsync() {
        Rect p = scriptsPaneRect();
        return new Rect(p.x + 152, p.y + p.h - 34, 120, 18);
    }

    private Rect mainBtnSave() {
        Rect h = mainHeaderRect();
        return new Rect(h.x + h.w - 274, h.y + 2, 58, 18);
    }

    private Rect mainBtnRun() {
        Rect h = mainHeaderRect();
        return new Rect(h.x + h.w - 212, h.y + 2, 58, 18);
    }

    private Rect mainBtnValidate() {
        Rect h = mainHeaderRect();
        return new Rect(h.x + h.w - 148, h.y + 2, 58, 18);
    }

    private Rect mainBtnAsync() {
        Rect h = mainHeaderRect();
        return new Rect(h.x + h.w - 86, h.y + 2, 82, 18);
    }

    private Rect suggestionsRect() {
        int[] p = cursorPixel(codeTextRect(), editor.cursor);
        int x = Math.clamp(p[0], PAD, width - 360);
        int y = Math.min(height - 138, p[1] + 12);
        return new Rect(x, y, 350, 110);
    }

    private void drawPrimaryButton(GuiGraphicsExtractor context, Rect rect, String label, int mouseX, int mouseY) {
        boolean hot = rect.contains(mouseX, mouseY);
        me.m0dii.gui.theme.ThemeDraw.box(context, rect.x, rect.y, rect.w, rect.h, hot ? Theme.ACCENT_HOVER : Theme.ACCENT);
        me.m0dii.gui.theme.ThemeDraw.textCentered(context, font, label, rect.x, rect.y, rect.w, rect.h, Theme.TEXT);
    }

    private void drawButton(GuiGraphicsExtractor context, Rect rect, String label, int mouseX, int mouseY) {
        GuiSystem.drawButton(context, font, rect.x, rect.y, rect.w, rect.h, label, rect.contains(mouseX, mouseY), true);
    }

    @Override
    public void onClose() {
        int unsaved = dirtyCount();
        if (unsaved > 0 && closeWarnedAtEdit != globalEdits) {
            // Unsaved files stay open for this game session; say so once before leaving.
            closeWarnedAtEdit = globalEdits;
            outputText = unsaved + " file(s) have unsaved changes. They stay open until you quit the game."
                    + " Ctrl+Shift+S saves all; Esc again closes.";
            return;
        }
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}

