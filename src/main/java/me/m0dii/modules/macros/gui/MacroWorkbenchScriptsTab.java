package me.m0dii.modules.macros.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.local.UiBoundTextField;
import me.m0dii.gui.local.UiKeyCaptureButton;
import me.m0dii.gui.local.UiSelectionList;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedWidgets;
import me.m0dii.modules.automation.AutomationActionType;
import me.m0dii.modules.automation.AutomationModule;
import me.m0dii.modules.automation.AutomationRule;
import me.m0dii.modules.scripting.*;
import me.m0dii.modules.scripting.gui.ScriptEditorScreen;
import me.m0dii.modules.scripting.ide.ScriptInfo;
import me.m0dii.modules.scripting.ide.ScriptNames;
import me.m0dii.modules.scripting.ide.ScriptSyntax;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Workbench "Scripts" tab: every script in {@code config/m0-dev-tools/scripts} with a preview, run/stop, a key
 * binding, the automation rules that use it and the script console. "Edit" opens the full script editor, which
 * comes back here when closed.
 */
final class MacroWorkbenchScriptsTab {
    private static final int ROW_GAP = Theme.GAP_S;
    private static final int CONSOLE_LINES = 7;
    private static final String[] NEW_EXTENSIONS = {ScriptTypes.KOTLIN_EXT, ScriptTypes.GROOVY_EXT, ScriptTypes.JAVASCRIPT_EXT};
    private static final String[] NEW_LABELS = {"Kotlin", "Groovy", "JS"};

    /** Remembered across workbench re-inits (resizes, coming back from the editor). */
    private static String selectedName;
    private static String filter = "";
    private static int newLanguage;

    private record Layout(int listX, int listY, int listW, int listH, int newY,
                          int cardX, int cardY, int cardW, int cardH, int pad,
                          int buttonsY, int infoY, int previewY, int consoleY) {
    }

    private final MacroWorkbenchScreen owner;
    private final List<AbstractWidget> widgets;
    private final UiSelectionList list = new UiSelectionList(0, 0, 1, 1);
    private final List<String> names = new ArrayList<>();

    private UiBoundTextField searchField;
    private Button newButton;
    private Button languageButton;
    private Button runButton;
    private Button editButton;
    private Button stopButton;
    private Button deleteButton;
    private UiKeyCaptureButton keyButton;

    private String cachedName;
    private String cachedText = "";
    private ScriptInfo cachedInfo = ScriptInfo.of("", "");
    private long cachedModified;
    private String rowsSignature = "";
    private long deleteArmedAt;
    private long lastClickAt;
    private int lastClickIndex = -1;
    private String status = "";

    MacroWorkbenchScriptsTab(MacroWorkbenchScreen owner, List<AbstractWidget> widgets) {
        this.owner = owner;
        this.widgets = widgets;
    }

    private <T extends AbstractWidget> T register(T widget) {
        this.widgets.add(widget);
        this.owner.addRenderableWidget(widget);
        return widget;
    }

    private Layout layout() {
        int left = this.owner.contentLeft();
        int top = this.owner.contentTop();
        int right = this.owner.contentRight();
        int bottom = this.owner.contentBottom();
        int listW = Math.clamp(Math.round(this.owner.contentWidth() * 0.32f), 120, 220);
        int listY = top + Theme.CONTROL_H + ROW_GAP;
        int newY = bottom - Theme.CONTROL_H;
        int cardX = left + listW + Theme.GAP_L;
        int cardW = right - cardX;
        int pad = Theme.GAP_M;
        int buttonsY = top + pad + 34;
        int infoY = buttonsY + Theme.CONTROL_H + Theme.GAP_M;
        int consoleH = 14 + CONSOLE_LINES * 10;
        int consoleY = bottom - pad - consoleH;
        int previewY = infoY + 14;
        return new Layout(left, listY, listW, newY - ROW_GAP - listY, newY,
                cardX, top, cardW, bottom - top, pad, buttonsY, infoY, previewY, consoleY);
    }

    void initWidgets() {
        Layout l = layout();
        Font font = this.owner.workbenchTextRenderer();
        this.searchField = new UiBoundTextField(font, l.listX(), this.owner.contentTop(), l.listW(), Theme.CONTROL_H, "Search scripts")
                .setMaxLength(64)
                .setSaveConsumer(value -> {
                    filter = value == null ? "" : value;
                    reload();
                });
        this.searchField.setText(filter);
        register(this.searchField.widget());

        int newW = l.listW() - 52 - ROW_GAP;
        this.newButton = register(ThemedWidgets.primary(Button.builder(Component.literal("+ New script"), b -> createScript())
                .bounds(l.listX(), l.newY(), newW, Theme.CONTROL_H).build()));
        this.languageButton = register(Button.builder(Component.literal(NEW_LABELS[newLanguage]), b -> {
                    newLanguage = (newLanguage + 1) % NEW_LABELS.length;
                    b.setMessage(Component.literal(NEW_LABELS[newLanguage]));
                })
                .bounds(l.listX() + newW + ROW_GAP, l.newY(), 52, Theme.CONTROL_H).build());

        int x = l.cardX() + l.pad();
        int y = l.buttonsY();
        this.runButton = register(ThemedWidgets.primary(Button.builder(Component.literal("▶ Run"), b -> runSelected())
                .bounds(x, y, 54, Theme.CONTROL_H).build()));
        this.editButton = register(Button.builder(Component.literal("Edit…"), b -> openEditor(selectedName))
                .bounds(x + 58, y, 50, Theme.CONTROL_H).build());
        this.stopButton = register(Button.builder(Component.literal("Stop all"), b -> {
                    int stopped = ScriptRunner.cancelAll();
                    this.status = stopped == 0 ? "Nothing is running." : "Stopped " + stopped + " script(s).";
                })
                .bounds(x + 112, y, 60, Theme.CONTROL_H).build());
        int keyW = Math.clamp(l.cardW() - l.pad() * 2 - 176 - 62 - 8, 70, 140);
        this.keyButton = new UiKeyCaptureButton("Key", -1, x + 176, y, keyW, Theme.CONTROL_H)
                .setLabelFormatter(code -> code < 0 ? "None" : MacroWorkbenchCanvasUtils.keyLabel(code))
                .setSaveConsumer(code -> {
                    if (selectedName == null) {
                        return;
                    }
                    if (code < 0) {
                        InGameScriptingKeybinds.clearKeybind(selectedName);
                        this.status = "Removed the key for " + selectedName + ".";
                    } else {
                        InGameScriptingKeybinds.registerKeybind(selectedName, code);
                        this.status = selectedName + " now runs on " + MacroWorkbenchCanvasUtils.keyLabel(code) + " (in game, no screen open).";
                    }
                });
        register(this.keyButton.widget());
        this.deleteButton = register(ThemedWidgets.danger(Button.builder(Component.literal("Delete"), b -> deleteSelected())
                .bounds(l.cardX() + l.cardW() - l.pad() - 58, y, 58, Theme.CONTROL_H).build()));

        this.list.setEmptyText("No scripts yet. + New script makes one.");
        reload();
    }

    /** Re-reads the scripts folder (on tab open, after edits, when the filter changes). */
    void reload() {
        this.names.clear();
        String q = filter.toLowerCase(Locale.ROOT).trim();
        for (String name : ScriptStorage.listScripts()) {
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                this.names.add(name);
            }
        }
        this.rowsSignature = "";
        refreshRows();
        int index = selectedName == null ? -1 : this.names.indexOf(selectedName);
        if (index < 0 && !this.names.isEmpty()) {
            index = 0;
        }
        this.list.setSelectedIndex(index);
        select(index);
        this.cachedName = null;
    }

    private void refreshRows() {
        List<String> running = new ArrayList<>();
        for (ScriptJob job : ScriptRunner.activeJobs()) {
            running.add(job.name());
        }
        String signature = this.names + "|" + running;
        if (signature.equals(this.rowsSignature)) {
            return;
        }
        this.rowsSignature = signature;
        List<String> rows = new ArrayList<>();
        for (String name : this.names) {
            String key = keyLabelFor(name);
            rows.add((running.contains(name) ? "● " : "") + name + (key.isEmpty() ? "" : "  [" + key + "]"));
        }
        int selected = this.list.selectedIndex();
        this.list.setRows(rows);
        this.list.setSelectedIndex(selected);
    }

    private static String keyLabelFor(String name) {
        Integer key = InGameScriptingKeybinds.getKeybindKey(name);
        return key == null || key < 0 ? "" : MacroWorkbenchCanvasUtils.keyLabel(key);
    }

    private void select(int index) {
        selectedName = index >= 0 && index < this.names.size() ? this.names.get(index) : null;
        Integer key = selectedName == null ? null : InGameScriptingKeybinds.getKeybindKey(selectedName);
        if (this.keyButton != null) {
            this.keyButton.cancelCapture();
            this.keyButton.setValue(key == null ? -1 : key);
        }
        this.deleteArmedAt = 0;
        if (this.deleteButton != null) {
            this.deleteButton.setMessage(Component.literal("Delete"));
        }
        boolean has = selectedName != null;
        for (Button button : new Button[]{this.runButton, this.editButton, this.deleteButton}) {
            if (button != null) {
                button.active = has && button.visible;
            }
        }
        if (this.keyButton != null) {
            this.keyButton.widget().active = has && this.keyButton.widget().visible;
        }
    }

    private void loadSelectedText() {
        if (selectedName == null) {
            this.cachedName = null;
            this.cachedText = "";
            this.cachedInfo = ScriptInfo.of("", "");
            return;
        }
        long modified = ScriptStorage.lastModified(selectedName);
        if (selectedName.equals(this.cachedName) && modified == this.cachedModified) {
            return;
        }
        try {
            this.cachedText = ScriptStorage.readScript(selectedName);
        } catch (IOException e) {
            this.cachedText = "";
        }
        this.cachedName = selectedName;
        this.cachedModified = modified;
        this.cachedInfo = ScriptInfo.of(selectedName, this.cachedText);
    }

    // ---- actions -----------------------------------------------------------------------------------------

    private void createScript() {
        String base = "new_script";
        String ext = NEW_EXTENSIONS[newLanguage];
        String name = base + ext;
        int i = 1;
        while (ScriptStorage.exists(name)) {
            name = base + "_" + i++ + ext;
        }
        try {
            ScriptStorage.writeScript(name, ScriptNames.newScriptTemplate(name));
        } catch (IOException e) {
            this.status = "Could not create the script: " + e.getMessage();
            return;
        }
        selectedName = name;
        filter = "";
        openEditor(name);
    }

    private void openEditor(String name) {
        Minecraft.getInstance().gui.setScreen(new ScriptEditorScreen(this.owner, name));
    }

    private void runSelected() {
        String name = selectedName;
        if (name == null) {
            return;
        }
        String source;
        try {
            source = ScriptStorage.readScript(name);
        } catch (IOException e) {
            this.status = "Could not read " + name + ": " + e.getMessage();
            return;
        }
        boolean async = ScriptDirectives.isAsync(source);
        ScriptConsole.add(ScriptConsole.Level.INFO, "▶ " + name + (async ? " (async)" : ""));
        this.status = async ? "Running " + name + " in the background…" : "Running " + name + "…";
        ScriptRunner.run(name, ScriptTypes.managerFor(name), source, ScriptTypes.defaultContext(), async, outcome -> {
            if (outcome.success()) {
                ScriptConsole.add(ScriptConsole.Level.RESULT, "✔ " + name + " → " + outcome.displayText());
                this.status = "✔ " + name + " finished.";
            } else {
                this.status = "✘ " + name + ": " + outcome.error();
            }
        });
    }

    private void deleteSelected() {
        if (selectedName == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.deleteArmedAt > 3000) {
            this.deleteArmedAt = now;
            this.deleteButton.setMessage(Component.literal("Sure?"));
            this.status = "Click Delete again to delete " + selectedName + ".";
            return;
        }
        String name = selectedName;
        try {
            ScriptStorage.deleteScript(name);
            InGameScriptingKeybinds.clearKeybind(name);
            this.status = "Deleted " + name + ".";
        } catch (IOException e) {
            this.status = "Delete failed: " + e.getMessage();
        }
        selectedName = null;
        reload();
    }

    // ---- input -------------------------------------------------------------------------------------------

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!this.list.handleMouseClick(mouseX, mouseY)) {
            return false;
        }
        int index = this.list.selectedIndex();
        long now = System.currentTimeMillis();
        boolean doubleClick = index == this.lastClickIndex && now - this.lastClickAt < 350;
        this.lastClickAt = now;
        this.lastClickIndex = index;
        select(index);
        if (doubleClick && selectedName != null) {
            openEditor(selectedName);
        }
        return true;
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        return this.list.handleMouseScroll(mouseX, mouseY, verticalAmount);
    }

    boolean handleKeyPressed(int keyCode) {
        if (this.keyButton != null && this.keyButton.handleKeyPressed(keyCode)) {
            return true;
        }
        if (this.searchField != null && this.searchField.widget().isFocused()) {
            return false;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (selectedName != null) {
                openEditor(selectedName);
                return true;
            }
        }
        if (keyCode == InputConstants.KEY_F5) {
            runSelected();
            return true;
        }
        if (keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN) {
            int next = this.list.selectedIndex() + (keyCode == InputConstants.KEY_UP ? -1 : 1);
            if (next >= 0 && next < this.names.size()) {
                this.list.setSelectedIndex(next);
                select(next);
            }
            return true;
        }
        return false;
    }

    // ---- rendering ---------------------------------------------------------------------------------------

    void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        Layout l = layout();
        Font font = this.owner.workbenchTextRenderer();
        refreshRows();
        this.list.setBounds(l.listX(), l.listY(), l.listW(), l.listH());
        this.list.extractRenderState(context, font, mouseX, mouseY);

        ThemeDraw.card(context, l.cardX(), l.cardY(), l.cardW(), l.cardH());
        int textX = l.cardX() + l.pad();
        int textW = l.cardW() - l.pad() * 2;
        int titleY = l.cardY() + l.pad();
        if (selectedName == null) {
            context.text(font, "No script selected", textX, titleY, Theme.TEXT, false);
            context.text(font, ThemeDraw.fit(font, "Scripts live in config/m0-dev-tools/scripts (.kts, .groovy, .js).", textW),
                    textX, titleY + 11, Theme.TEXT_MUTED, false);
            renderConsole(context, font, l, textX, textW);
            return;
        }
        loadSelectedText();
        ScriptInfo info = this.cachedInfo;
        context.text(font, ThemeDraw.fit(font, selectedName, textW), textX, titleY, Theme.TEXT, false);
        String meta = info.language() + " · " + info.lines() + " lines · " + (info.async() ? "async" : "game thread")
                + (this.cachedModified > 0 ? " · saved " + new SimpleDateFormat("MMM d HH:mm", Locale.ROOT).format(new Date(this.cachedModified)) : "");
        context.text(font, ThemeDraw.fit(font, meta, textW), textX, titleY + 11, Theme.TEXT_DIM, false);
        if (!info.description().isEmpty()) {
            context.text(font, ThemeDraw.fit(font, info.description(), textW), textX, titleY + 22, Theme.TEXT_MUTED, false);
        }

        String usedBy = automationUsers(selectedName);
        String infoLine = usedBy.isEmpty() ? "Double-click or Enter opens the editor · F5 runs" : "Used by automation: " + usedBy;
        context.text(font, ThemeDraw.fit(font, infoLine, textW), textX, l.infoY(), usedBy.isEmpty() ? Theme.TEXT_DIM : Theme.ACCENT_TEXT, false);

        // Preview with syntax colours.
        int previewBottom = l.consoleY() - Theme.GAP_M;
        int rows = Math.max(0, (previewBottom - l.previewY()) / 10);
        if (rows > 0) {
            ThemeDraw.box(context, textX - 2, l.previewY() - 2, textW + 4, rows * 10 + 4, Theme.INPUT);
            List<String> preview = ScriptInfo.preview(this.cachedText, rows);
            List<ScriptSyntax.State> states = ScriptSyntax.startStates(preview, info.language());
            for (int i = 0; i < preview.size(); i++) {
                drawCodeLine(context, font, preview.get(i), info.language(), states.get(i), textX, l.previewY() + i * 10, textW);
            }
            if (preview.isEmpty()) {
                context.text(font, "(empty file)", textX, l.previewY(), Theme.TEXT_DIM, false);
            }
        }
        renderConsole(context, font, l, textX, textW);
    }

    private void drawCodeLine(GuiGraphicsExtractor context, Font font, String line, String language,
                              ScriptSyntax.State state, int x, int y, int maxWidth) {
        int drawX = x;
        int limit = x + maxWidth;
        for (ScriptSyntax.Span span : ScriptSyntax.line(line, language, state).spans()) {
            if (drawX >= limit) {
                break;
            }
            String part = line.substring(span.start(), Math.min(span.end(), line.length()));
            String fitted = ThemeDraw.fit(font, part, limit - drawX);
            context.text(font, fitted, drawX, y, ScriptSyntax.color(span.kind()), false);
            drawX += font.width(fitted);
        }
    }

    private void renderConsole(GuiGraphicsExtractor context, Font font, Layout l, int textX, int textW) {
        int y = l.consoleY();
        ThemeDraw.hDivider(context, l.cardX() + 1, y - 4, l.cardW() - 2);
        List<ScriptJob> jobs = ScriptRunner.activeJobs();
        String header = "Console" + (jobs.isEmpty() ? "" : "  ·  " + jobs.size() + " running");
        ThemeDraw.sectionLabel(context, font, header, textX, y, textW);
        int lineY = y + 12;
        int shown = 0;
        long now = System.currentTimeMillis();
        for (ScriptJob job : jobs) {
            if (shown >= CONSOLE_LINES) {
                break;
            }
            long seconds = job.startedAtMs() <= 0 ? 0 : (now - job.startedAtMs()) / 1000;
            context.text(font, ThemeDraw.fit(font, "● " + job.name() + (job.isAsync() ? " (async)" : "") + " · " + seconds + "s", textW),
                    textX, lineY, Theme.SUCCESS, false);
            lineY += 10;
            shown++;
        }
        List<ScriptConsole.Line> lines = ScriptConsole.lines();
        int start = Math.max(0, lines.size() - (CONSOLE_LINES - shown));
        for (int i = start; i < lines.size(); i++) {
            ScriptConsole.Line line = lines.get(i);
            int color = switch (line.level()) {
                case ERROR -> 0xFFFF9D9D;
                case RESULT -> 0xFF9DFFB0;
                case INFO -> Theme.TEXT_MUTED;
            };
            context.text(font, ThemeDraw.fit(font, line.text(), textW), textX, lineY, color, false);
            lineY += 10;
        }
        if (!this.status.isEmpty()) {
            String fitted = ThemeDraw.fit(font, this.status, textW);
            context.text(font, fitted, textX + textW - font.width(fitted), y, Theme.ACCENT_TEXT, false);
        }
    }

    private static String automationUsers(String scriptName) {
        try {
            List<String> users = new ArrayList<>();
            for (AutomationRule rule : AutomationModule.INSTANCE.engine().getRules()) {
                for (AutomationRule.Action action : rule.actions) {
                    if (action.type == AutomationActionType.RUN_SCRIPT && scriptName.equals(action.target)) {
                        users.add(rule.name == null || rule.name.isBlank() ? rule.id : rule.name);
                        break;
                    }
                }
            }
            return String.join(", ", users);
        } catch (RuntimeException e) {
            return "";
        }
    }
}
