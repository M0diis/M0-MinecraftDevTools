package me.m0dii.modules.chat;

import me.m0dii.gui.local.UiForms;
import me.m0dii.gui.local.UiSelectionList;
import me.m0dii.gui.local.UiTheme;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecondaryChatConfigScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int TOP = 32;
    private static final int BOTTOM = 34;
    private static final int GAP = 8;
    private static final int BUTTON_H = 20;
    private static final int FIELD_H = 18;

    private final Screen parent;
    private final UiSelectionList windowList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList tabList = new UiSelectionList(0, 0, 1, 1);
    private final UiSelectionList regexList = new UiSelectionList(0, 0, 1, 1);

    private int selectedWindowIndex = 0;
    private int selectedTabIndex = 0;
    private int selectedRegexIndex = -1;
    private boolean loadingFields = false;

    private TextFieldWidget windowTitleField;
    private TextFieldWidget windowXField;
    private TextFieldWidget windowYField;
    private TextFieldWidget windowWidthField;
    private TextFieldWidget windowHeightField;
    private TextFieldWidget windowScaleField;
    private TextFieldWidget windowLineHeightField;
    private TextFieldWidget windowPaddingField;
    private TextFieldWidget windowBackgroundField;
    private TextFieldWidget windowTextColorField;
    private TextFieldWidget tabNameField;
    private TextFieldWidget regexField;

    private ButtonWidget enabledButton;
    private ButtonWidget addWindowButton;
    private ButtonWidget removeWindowButton;
    private ButtonWidget applyWindowButton;
    private ButtonWidget visibleWindowButton;
    private ButtonWidget drawBackgroundButton;
    private ButtonWidget drawBorderButton;
    private ButtonWidget showTabsButton;
    private ButtonWidget timestampsButton;
    private ButtonWidget compactButton;
    private ButtonWidget addTabButton;
    private ButtonWidget removeTabButton;
    private ButtonWidget applyTabButton;
    private ButtonWidget selectTabButton;
    private ButtonWidget catchAllButton;
    private ButtonWidget alwaysAddButton;
    private ButtonWidget skipOthersButton;
    private ButtonWidget notificationsButton;
    private ButtonWidget priorityDownButton;
    private ButtonWidget priorityUpButton;
    private ButtonWidget addRegexButton;
    private ButtonWidget removeRegexButton;
    private ButtonWidget clearRegexButton;

    public SecondaryChatConfigScreen(Screen parent) {
        super(Text.literal("Secondary Chat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.windowList.setRowHeight(18);
        this.windowList.setEmptyText("No windows");
        this.tabList.setRowHeight(18);
        this.tabList.setEmptyText("No tabs");
        this.regexList.setRowHeight(16);
        this.regexList.setEmptyText("No regex filters");

        this.enabledButton = addButton(MARGIN, TOP, 150, "Enabled", b -> toggleEnabled());

        int bodyTop = TOP + 34;
        int bodyBottom = this.height - BOTTOM;
        int listW = Math.clamp((this.width - MARGIN * 2) / 5, 160, 230);
        int listX = MARGIN;
        int listAreaH = Math.max(220, bodyBottom - bodyTop);
        int windowListY = bodyTop + 22;
        int windowListH = Math.max(76, (listAreaH - 84) / 2);
        int windowButtonsY = windowListY + windowListH + 4;
        int tabTitleY = windowButtonsY + BUTTON_H + 12;
        int tabListY = tabTitleY + 14;
        int tabButtonsY = bodyBottom - BUTTON_H;
        int tabListH = Math.max(68, tabButtonsY - tabListY - 4);
        this.windowList.setBounds(listX, windowListY, listW, windowListH);
        this.tabList.setBounds(listX, tabListY, listW, tabListH);

        int listHalf = (listW - 6) / 2;
        this.addWindowButton = addButton(listX, windowButtonsY, listHalf, "+ Window", b -> addWindow());
        this.removeWindowButton = addButton(listX + listHalf + 6, windowButtonsY, listHalf, "Remove", b -> removeWindow());
        this.addTabButton = addButton(listX, tabButtonsY, listHalf, "+ Tab", b -> addTab());
        this.removeTabButton = addButton(listX + listHalf + 6, tabButtonsY, listHalf, "Delete", b -> removeTab());

        int editorX = listX + listW + GAP;
        int editorW = this.width - editorX - MARGIN;
        int editorH = Math.max(240, bodyBottom - bodyTop);
        int windowPanelH = Math.clamp(editorH / 2, 214, 248);
        if (editorH - windowPanelH - GAP < 190) {
            windowPanelH = Math.max(190, editorH - GAP - 190);
        }
        int windowPanelY = bodyTop;
        int tabPanelY = windowPanelY + windowPanelH + GAP;
        int tabPanelH = bodyBottom - tabPanelY;

        int fieldX = editorX + 10;
        int fieldW = editorW - 20;
        int colGap = 8;
        int col4 = (fieldW - colGap * 3) / 4;
        int col3 = (fieldW - colGap * 2) / 3;
        int titleY = windowPanelY + 34;
        int metricsY = titleY + 32;
        int styleY = metricsY + 32;
        int colorY = styleY + 32;
        int windowButtonsBaseY = colorY + 30;

        this.windowTitleField = addField(fieldX, titleY, fieldW, "Window title", 48);
        this.windowXField = addField(fieldX, metricsY, col4, "X", 8);
        this.windowYField = addField(fieldX + (col4 + colGap), metricsY, col4, "Y", 8);
        this.windowWidthField = addField(fieldX + (col4 + colGap) * 2, metricsY, col4, "Width", 8);
        this.windowHeightField = addField(fieldX + (col4 + colGap) * 3, metricsY, col4, "Height", 8);
        this.windowScaleField = addField(fieldX, styleY, col3, "Scale", 8);
        this.windowLineHeightField = addField(fieldX + (col3 + colGap), styleY, col3, "Line height", 8);
        this.windowPaddingField = addField(fieldX + (col3 + colGap) * 2, styleY, col3, "Padding", 8);
        this.windowBackgroundField = addField(fieldX, colorY, (fieldW - colGap) / 2, "Background", 10);
        this.windowTextColorField = addField(fieldX + (fieldW + colGap) / 2, colorY, (fieldW - colGap) / 2, "Text", 10);

        int windowButtonW = (fieldW - colGap * 2) / 3;
        this.applyWindowButton = addButton(fieldX, windowButtonsBaseY, windowButtonW, "Apply", b -> applyWindowFields());
        this.visibleWindowButton = addButton(fieldX + (windowButtonW + colGap), windowButtonsBaseY, windowButtonW, "Visible", b -> toggleWindowVisible());
        this.drawBackgroundButton = addButton(fieldX + (windowButtonW + colGap) * 2, windowButtonsBaseY, windowButtonW, "Background", b -> toggleDrawBackground());
        this.drawBorderButton = addButton(fieldX, windowButtonsBaseY + 24, windowButtonW, "Border", b -> toggleDrawBorder());
        this.showTabsButton = addButton(fieldX + (windowButtonW + colGap), windowButtonsBaseY + 24, windowButtonW, "Tabs", b -> toggleShowTabs());
        this.timestampsButton = addButton(fieldX + (windowButtonW + colGap) * 2, windowButtonsBaseY + 24, windowButtonW, "Timestamps", b -> toggleTimestamps());
        this.compactButton = addButton(fieldX, windowButtonsBaseY + 48, windowButtonW, "Compact", b -> toggleCompact());

        int tabFieldX = editorX + 10;
        int tabFieldW = editorW - 20;
        int tabSettingsW = Math.max(236, (tabFieldW - GAP) / 2);
        int regexX = tabFieldX + tabSettingsW + GAP;
        int regexW = Math.max(160, tabFieldW - tabSettingsW - GAP);
        int tabNameY = tabPanelY + 34;
        this.tabNameField = addField(tabFieldX, tabNameY, tabSettingsW, "Tab name", 48);

        int tabHalf = (tabSettingsW - colGap) / 2;
        int tabButtonsBaseY = tabNameY + 28;
        this.applyTabButton = addButton(tabFieldX, tabButtonsBaseY, tabHalf, "Apply", b -> applyTabFields());
        this.selectTabButton = addButton(tabFieldX + tabHalf + colGap, tabButtonsBaseY, tabHalf, "Selected", b -> selectTab());
        this.catchAllButton = addButton(tabFieldX, tabButtonsBaseY + 24, tabHalf, "Catch All", b -> toggleCatchAll());
        this.notificationsButton = addButton(tabFieldX + tabHalf + colGap, tabButtonsBaseY + 24, tabHalf, "Notify", b -> toggleNotifications());
        this.alwaysAddButton = addButton(tabFieldX, tabButtonsBaseY + 48, tabHalf, "Always Add", b -> toggleAlwaysAdd());
        this.skipOthersButton = addButton(tabFieldX + tabHalf + colGap, tabButtonsBaseY + 48, tabHalf, "Skip Others", b -> toggleSkipOthers());
        this.priorityDownButton = addButton(tabFieldX, tabButtonsBaseY + 72, tabHalf, "Priority -", b -> adjustPriority(-1));
        this.priorityUpButton = addButton(tabFieldX + tabHalf + colGap, tabButtonsBaseY + 72, tabHalf, "Priority +", b -> adjustPriority(1));

        int regexButtonsY = bodyBottom - BUTTON_H;
        int regexFieldY = regexButtonsY - GAP - FIELD_H;
        int regexLabelY = regexFieldY - 12;
        int regexListY = tabPanelY + 34;
        this.regexList.setBounds(regexX, regexListY, regexW, Math.max(24, regexLabelY - regexListY - 4));
        this.regexField = addField(regexX, regexFieldY, regexW, "Regex", 180);
        int third = Math.max(52, (regexW - 8) / 3);
        this.addRegexButton = addButton(regexX, regexButtonsY, third, "Add", b -> addRegex());
        this.removeRegexButton = addButton(regexX + third + 4, regexButtonsY, third, "Remove", b -> removeRegex());
        this.clearRegexButton = addButton(regexX + (third + 4) * 2, regexButtonsY, Math.max(52, regexW - (third + 4) * 2), "Clear", b -> clearRegexes());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width - 112, this.height - 26, 100, BUTTON_H)
                .build());

        refreshLists();
        loadFieldsFromSelection();
        syncButtons();
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        drawStatic(context);
        this.windowList.render(context, this.textRenderer, mouseX, mouseY);
        this.tabList.render(context, this.textRenderer, mouseX, mouseY);
        this.regexList.render(context, this.textRenderer, mouseX, mouseY);
        syncButtons();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, UiTheme.BG);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() == 0) {
            if (this.windowList.handleMouseClick(click.x(), click.y())) {
                this.selectedWindowIndex = this.windowList.selectedIndex();
                this.selectedTabIndex = 0;
                this.selectedRegexIndex = -1;
                refreshLists();
                loadFieldsFromSelection();
                return true;
            }
            if (this.tabList.handleMouseClick(click.x(), click.y())) {
                this.selectedTabIndex = this.tabList.selectedIndex();
                this.selectedRegexIndex = -1;
                refreshLists();
                loadFieldsFromSelection();
                return true;
            }
            if (this.regexList.handleMouseClick(click.x(), click.y())) {
                this.selectedRegexIndex = this.regexList.selectedIndex();
                SecondaryChatSettings.TabConfig tab = selectedTab();
                if (tab != null && this.selectedRegexIndex >= 0 && this.selectedRegexIndex < tab.regexList.size()) {
                    this.regexField.setText(tab.regexList.get(this.selectedRegexIndex));
                    this.regexField.setCursorToEnd(false);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.windowList.handleMouseScroll(mouseX, mouseY, verticalAmount)
                || this.tabList.handleMouseScroll(mouseX, mouseY, verticalAmount)
                || this.regexList.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        applyFocusedFields();
        super.removed();
    }

    @Override
    public void close() {
        applyFocusedFields();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawStatic(DrawContext context) {
        context.fill(0, 0, this.width, 28, 0x90202020);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 11, UiTheme.TEXT);

        SecondaryChatSettings.WindowConfig window = selectedWindow();
        SecondaryChatSettings.TabConfig tab = selectedTab();
        int bodyTop = TOP + 34;
        int bodyBottom = this.height - BOTTOM;
        int listW = Math.clamp((this.width - MARGIN * 2) / 5, 160, 230);
        int listX = MARGIN;
        int listAreaH = Math.max(220, bodyBottom - bodyTop);
        int windowListY = bodyTop + 22;
        int windowListH = Math.max(76, (listAreaH - 84) / 2);
        int windowButtonsY = windowListY + windowListH + 4;
        int tabTitleY = windowButtonsY + BUTTON_H + 12;

        int editorX = listX + listW + GAP;
        int editorW = this.width - editorX - MARGIN;
        int editorH = Math.max(240, bodyBottom - bodyTop);
        int windowPanelH = Math.clamp(editorH / 2, 214, 248);
        if (editorH - windowPanelH - GAP < 190) {
            windowPanelH = Math.max(190, editorH - GAP - 190);
        }
        int windowPanelY = bodyTop;
        int tabPanelY = windowPanelY + windowPanelH + GAP;
        int tabPanelH = bodyBottom - tabPanelY;

        UiForms.drawPanel(context, listX - 6, bodyTop, listW + 12, bodyBottom - bodyTop + 5);
        UiForms.drawPanel(context, editorX, windowPanelY, editorW, windowPanelH);
        UiForms.drawPanel(context, editorX, tabPanelY, editorW, tabPanelH + 5);

        context.drawTextWithShadow(this.textRenderer, "Windows", listX, bodyTop + 8, UiTheme.TEXT_ACCENT);
        context.drawTextWithShadow(this.textRenderer, "Tabs", listX, tabTitleY, UiTheme.TEXT_ACCENT);

        int fieldX = editorX + 10;
        int fieldW = editorW - 20;
        int colGap = 8;
        int col4 = (fieldW - colGap * 3) / 4;
        int col3 = (fieldW - colGap * 2) / 3;
        int titleY = windowPanelY + 34;
        int metricsY = titleY + 32;
        int styleY = metricsY + 32;
        int colorY = styleY + 32;

        context.drawTextWithShadow(this.textRenderer, "Window", fieldX, windowPanelY + 8, UiTheme.TEXT_ACCENT);
        if (window != null && window.useHudCanvas) {
            context.drawTextWithShadow(this.textRenderer, "HUD canvas", fieldX + 62, windowPanelY + 8, 0xFF8FB9D8);
        }
        drawLabel(context, "Title", fieldX, titleY - 11);
        drawLabel(context, "X", fieldX, metricsY - 11);
        drawLabel(context, "Y", fieldX + (col4 + colGap), metricsY - 11);
        drawLabel(context, "Width", fieldX + (col4 + colGap) * 2, metricsY - 11);
        drawLabel(context, "Height", fieldX + (col4 + colGap) * 3, metricsY - 11);
        drawLabel(context, "Scale", fieldX, styleY - 11);
        drawLabel(context, "Line", fieldX + (col3 + colGap), styleY - 11);
        drawLabel(context, "Padding", fieldX + (col3 + colGap) * 2, styleY - 11);
        drawLabel(context, "Background", fieldX, colorY - 11);
        drawLabel(context, "Text", fieldX + (fieldW + colGap) / 2, colorY - 11);

        int tabFieldX = editorX + 10;
        int tabFieldW = editorW - 20;
        int tabSettingsW = Math.max(236, (tabFieldW - GAP) / 2);
        int regexX = tabFieldX + tabSettingsW + GAP;
        int tabNameY = tabPanelY + 34;
        int tabButtonsBaseY = tabNameY + 28;
        int regexButtonsY = bodyBottom - BUTTON_H;
        int regexFieldY = regexButtonsY - GAP - FIELD_H;

        context.drawTextWithShadow(this.textRenderer, "Tab", tabFieldX, tabPanelY + 8, UiTheme.TEXT_ACCENT);
        if (tab != null) {
            context.drawTextWithShadow(this.textRenderer, "Priority: " + tab.priority,
                    tabFieldX, tabButtonsBaseY + 98, UiTheme.TEXT_MUTED);
        }
        drawLabel(context, "Name", tabFieldX, tabNameY - 11);
        context.drawTextWithShadow(this.textRenderer, "Regex filters", regexX, tabPanelY + 8, UiTheme.TEXT_ACCENT);
        drawLabel(context, "Pattern", regexX, regexFieldY - 11);
    }

    private void drawLabel(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(this.textRenderer, label, x, y, UiTheme.TEXT_MUTED);
    }

    private ButtonWidget addButton(int x, int y, int w, String label, ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, w, BUTTON_H)
                .build());
    }

    private TextFieldWidget addField(int x, int y, int w, String placeholder, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, w, FIELD_H, Text.literal(placeholder));
        field.setPlaceholder(Text.literal(placeholder));
        field.setMaxLength(maxLength);
        addDrawableChild(field);
        return field;
    }

    private void refreshLists() {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        this.selectedWindowIndex = Math.clamp(this.selectedWindowIndex, 0, Math.max(0, settings.windows.size() - 1));

        List<String> windowRows = new ArrayList<>();
        for (SecondaryChatSettings.WindowConfig window : settings.windows) {
            windowRows.add((window.visible ? "" : "[hidden] ") + window.id + "  " + window.title);
        }
        this.windowList.setRows(windowRows);
        this.windowList.setSelectedIndex(this.selectedWindowIndex);

        SecondaryChatSettings.WindowConfig window = selectedWindow();
        List<String> tabRows = new ArrayList<>();
        if (window != null) {
            this.selectedTabIndex = Math.clamp(this.selectedTabIndex, 0, Math.max(0, window.tabs.size() - 1));
            for (SecondaryChatSettings.TabConfig tab : window.tabs) {
                String marker = tab.id.equals(window.selectedTabId) ? "* " : "";
                tabRows.add(marker + tab.id + "  " + tab.name);
            }
        }
        this.tabList.setRows(tabRows);
        this.tabList.setSelectedIndex(this.selectedTabIndex);

        SecondaryChatSettings.TabConfig tab = selectedTab();
        List<String> regexRows = new ArrayList<>();
        if (tab != null) {
            this.selectedRegexIndex = tab.regexList.isEmpty()
                    ? -1
                    : Math.clamp(this.selectedRegexIndex, 0, tab.regexList.size() - 1);
            for (String regex : tab.regexList) {
                regexRows.add(regex);
            }
        }
        this.regexList.setRows(regexRows);
        this.regexList.setSelectedIndex(this.selectedRegexIndex);
    }

    private void loadFieldsFromSelection() {
        this.loadingFields = true;
        try {
            SecondaryChatSettings.WindowConfig window = selectedWindow();
            if (window != null) {
                SecondaryChatWindowLayout.Frame frame = SecondaryChatWindowLayout.frame(window);
                this.windowTitleField.setText(window.title == null ? "" : window.title);
                this.windowXField.setText(String.valueOf(frame.x));
                this.windowYField.setText(String.valueOf(frame.y));
                this.windowWidthField.setText(String.valueOf(frame.width));
                this.windowHeightField.setText(String.valueOf(frame.height));
                this.windowScaleField.setText(formatFloat(frame.scale));
                this.windowLineHeightField.setText(String.valueOf(frame.lineHeight));
                this.windowPaddingField.setText(String.valueOf(frame.padding));
                this.windowBackgroundField.setText(formatColor(frame.backgroundColor));
                this.windowTextColorField.setText(formatColor(frame.textColor));
            }

            SecondaryChatSettings.TabConfig tab = selectedTab();
            this.tabNameField.setText(tab == null ? "" : tab.name);
            this.regexField.setText("");
        } finally {
            this.loadingFields = false;
        }
    }

    private void syncButtons() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        SecondaryChatSettings.TabConfig tab = selectedTab();

        this.enabledButton.setMessage(Text.literal("Secondary Chat: " + onOff(SecondaryChatSettings.get().enabled)));
        this.removeWindowButton.active = window != null && !"main".equals(window.id);
        this.applyWindowButton.active = window != null;
        this.visibleWindowButton.active = window != null;
        this.visibleWindowButton.setMessage(Text.literal("Visible: " + onOff(window != null && window.visible)));
        this.drawBackgroundButton.setMessage(Text.literal("Background: " + onOff(window != null && window.drawBackground)));
        this.drawBorderButton.setMessage(Text.literal("Border: " + onOff(window != null && window.drawBorder)));
        this.showTabsButton.setMessage(Text.literal("Tabs: " + onOff(window != null && window.showTabs)));
        this.timestampsButton.setMessage(Text.literal("Timestamps: " + onOff(window != null && window.showTimestamps)));
        this.compactButton.setMessage(Text.literal("Compact: " + onOff(window != null && window.compactRepeats)));

        this.removeTabButton.active = window != null && window.tabs.size() > 1;
        this.applyTabButton.active = tab != null;
        this.selectTabButton.active = window != null && tab != null;
        this.selectTabButton.setMessage(Text.literal("Selected: " + onOff(window != null && tab != null && tab.id.equals(window.selectedTabId))));
        this.catchAllButton.setMessage(Text.literal("Catch All: " + onOff(tab != null && tab.catchAll)));
        this.alwaysAddButton.setMessage(Text.literal("Always Add: " + onOff(tab != null && tab.alwaysAdd)));
        this.skipOthersButton.setMessage(Text.literal("Skip Others: " + onOff(tab != null && tab.skipOthers)));
        this.notificationsButton.setMessage(Text.literal("Notify: " + onOff(tab != null && tab.showNotifications)));
        this.priorityDownButton.setMessage(Text.literal("Priority -"));
        this.priorityUpButton.setMessage(Text.literal("Priority +"));
        this.removeRegexButton.active = tab != null && this.selectedRegexIndex >= 0 && this.selectedRegexIndex < tab.regexList.size();
        this.clearRegexButton.active = tab != null && !tab.regexList.isEmpty();
    }

    private void toggleEnabled() {
        boolean enabled = !SecondaryChatSettings.get().enabled;
        SecondaryChatModule.INSTANCE.setEnabled(enabled);
        if (enabled) {
            SecondaryChatSettings.updateAndSave(() -> {
                SecondaryChatSettings.get().showOverlay = true;
                SecondaryChatSettings.get().renderMode = SecondaryChatSettings.RenderMode.REPLACE;
                SecondaryChatSettings.get().showWhileGuiOpen = true;
            });
        }
        syncButtons();
    }

    private void addWindow() {
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            int next = settings.windows.size() + 1;
            SecondaryChatSettings.WindowConfig window = new SecondaryChatSettings.WindowConfig();
            window.id = uniqueWindowId("window_" + next, settings.windows);
            window.title = "Window " + next;
            window.useHudCanvas = false;
            window.x = 28 + next * 16;
            window.y = 56 + next * 16;
            SecondaryChatSettings.TabConfig tab = new SecondaryChatSettings.TabConfig();
            tab.id = "all";
            tab.name = "All";
            tab.catchAll = true;
            window.tabs.add(tab);
            window.selectedTabId = tab.id;
            settings.windows.add(window);
            this.selectedWindowIndex = settings.windows.size() - 1;
            this.selectedTabIndex = 0;
            this.selectedRegexIndex = -1;
        });
        refreshLists();
        loadFieldsFromSelection();
    }

    private void removeWindow() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window == null || "main".equals(window.id)) {
            return;
        }
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().windows.removeIf(existing -> existing.id.equals(window.id)));
        this.selectedWindowIndex = Math.max(0, this.selectedWindowIndex - 1);
        this.selectedTabIndex = 0;
        this.selectedRegexIndex = -1;
        refreshLists();
        loadFieldsFromSelection();
    }

    private void applyWindowFields() {
        if (this.loadingFields) {
            return;
        }
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window == null) {
            return;
        }

        int x = parseInt(this.windowXField.getText(), window.x, -10000, 10000);
        int y = parseInt(this.windowYField.getText(), window.y, -10000, 10000);
        int w = parseInt(this.windowWidthField.getText(), window.width, 120, 3000);
        int h = parseInt(this.windowHeightField.getText(), window.height, 60, 3000);
        float scale = parseFloat(this.windowScaleField.getText(), window.fontScale, 0.25f, 5.0f);
        int lineHeight = parseInt(this.windowLineHeightField.getText(), window.lineHeight, 6, 40);
        int padding = parseInt(this.windowPaddingField.getText(), window.padding, 0, 60);
        int bg = parseColor(this.windowBackgroundField.getText(), window.backgroundColor);
        int tx = parseColor(this.windowTextColorField.getText(), window.textColor);
        String title = this.windowTitleField.getText().trim();

        SecondaryChatSettings.updateAndSave(() -> {
            window.title = title.isEmpty() ? window.id : title;
            window.x = x;
            window.y = y;
            window.width = w;
            window.height = h;
            window.fontScale = scale;
            window.lineHeight = lineHeight;
            window.padding = padding;
            window.backgroundColor = bg;
            window.textColor = tx;
        });

        if (window.useHudCanvas || "main".equals(window.id)) {
            HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                    HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                    SecondaryChatOverlay::defaultCanvasElement
            );
            canvas.x = x;
            canvas.y = y;
            canvas.width = w;
            canvas.height = h;
            canvas.fontScale = scale;
            canvas.lineHeight = lineHeight;
            canvas.padding = padding;
            canvas.backgroundColor = bg;
            canvas.textColor = tx;
            HudCanvasDataHandler.save();
        }

        refreshLists();
        loadFieldsFromSelection();
    }

    private void toggleWindowVisible() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window == null) {
            return;
        }
        SecondaryChatSettings.updateAndSave(() -> window.visible = !window.visible);
        refreshLists();
    }

    private void toggleDrawBackground() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window != null) {
            SecondaryChatSettings.updateAndSave(() -> window.drawBackground = !window.drawBackground);
        }
    }

    private void toggleDrawBorder() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window != null) {
            SecondaryChatSettings.updateAndSave(() -> window.drawBorder = !window.drawBorder);
        }
    }

    private void toggleShowTabs() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window != null) {
            SecondaryChatSettings.updateAndSave(() -> window.showTabs = !window.showTabs);
        }
    }

    private void toggleTimestamps() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window != null) {
            SecondaryChatSettings.updateAndSave(() -> window.showTimestamps = !window.showTimestamps);
        }
    }

    private void toggleCompact() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window != null) {
            SecondaryChatSettings.updateAndSave(() -> window.compactRepeats = !window.compactRepeats);
        }
    }

    private void addTab() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window == null) {
            return;
        }
        SecondaryChatSettings.updateAndSave(() -> {
            int next = window.tabs.size() + 1;
            SecondaryChatSettings.TabConfig tab = new SecondaryChatSettings.TabConfig();
            tab.id = uniqueTabId("tab_" + next, window.tabs);
            tab.name = "Tab " + next;
            tab.catchAll = false;
            window.tabs.add(tab);
            window.selectedTabId = tab.id;
            this.selectedTabIndex = window.tabs.size() - 1;
            this.selectedRegexIndex = -1;
        });
        refreshLists();
        loadFieldsFromSelection();
    }

    private void removeTab() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (window == null || tab == null || window.tabs.size() <= 1) {
            return;
        }
        String windowId = window.id;
        String removedTabId = tab.id;
        int removedIndex = this.selectedTabIndex;
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig target = findWindowById(SecondaryChatSettings.get().windows, windowId);
            if (target == null || target.tabs.size() <= 1) {
                return;
            }
            target.tabs.removeIf(existing -> existing.id.equals(removedTabId));
            if (!target.tabs.isEmpty()) {
                int nextIndex = Math.clamp(removedIndex, 0, target.tabs.size() - 1);
                target.selectedTabId = target.tabs.get(nextIndex).id;
            }
        });
        SecondaryChatManager.clear(windowId, removedTabId);
        this.selectedTabIndex = Math.max(0, removedIndex - 1);
        this.selectedRegexIndex = -1;
        refreshLists();
        loadFieldsFromSelection();
    }

    private void applyTabFields() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab == null) {
            return;
        }
        String name = this.tabNameField.getText().trim();
        SecondaryChatSettings.updateAndSave(() -> tab.name = name.isEmpty() ? tab.id : name);
        refreshLists();
        loadFieldsFromSelection();
    }

    private void selectTab() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (window == null || tab == null) {
            return;
        }
        SecondaryChatManager.selectTab(window.id, tab.id);
        refreshLists();
    }

    private void toggleCatchAll() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab != null) {
            SecondaryChatSettings.updateAndSave(() -> tab.catchAll = !tab.catchAll);
        }
    }

    private void toggleAlwaysAdd() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab != null) {
            SecondaryChatSettings.updateAndSave(() -> tab.alwaysAdd = !tab.alwaysAdd);
        }
    }

    private void toggleSkipOthers() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab != null) {
            SecondaryChatSettings.updateAndSave(() -> tab.skipOthers = !tab.skipOthers);
        }
    }

    private void toggleNotifications() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab != null) {
            SecondaryChatSettings.updateAndSave(() -> tab.showNotifications = !tab.showNotifications);
        }
    }

    private void adjustPriority(int direction) {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab != null) {
            SecondaryChatSettings.updateAndSave(() -> tab.priority = Math.clamp(tab.priority + direction, -9999, 9999));
        }
    }

    private void addRegex() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab == null) {
            return;
        }
        String regex = this.regexField.getText().trim();
        if (regex.isEmpty()) {
            return;
        }
        SecondaryChatSettings.updateAndSave(() -> tab.regexList.add(regex));
        this.selectedRegexIndex = tab.regexList.size() - 1;
        this.regexField.setText("");
        refreshLists();
    }

    private void removeRegex() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab == null || this.selectedRegexIndex < 0 || this.selectedRegexIndex >= tab.regexList.size()) {
            return;
        }
        SecondaryChatSettings.updateAndSave(() -> tab.regexList.remove(this.selectedRegexIndex));
        this.selectedRegexIndex = Math.max(-1, this.selectedRegexIndex - 1);
        refreshLists();
    }

    private void clearRegexes() {
        SecondaryChatSettings.TabConfig tab = selectedTab();
        if (tab == null) {
            return;
        }
        SecondaryChatSettings.updateAndSave(tab.regexList::clear);
        this.selectedRegexIndex = -1;
        refreshLists();
    }

    private void applyFocusedFields() {
        applyWindowFields();
        applyTabFields();
    }

    private SecondaryChatSettings.WindowConfig selectedWindow() {
        List<SecondaryChatSettings.WindowConfig> windows = SecondaryChatSettings.get().windows;
        if (windows.isEmpty()) {
            return null;
        }
        this.selectedWindowIndex = Math.clamp(this.selectedWindowIndex, 0, windows.size() - 1);
        return windows.get(this.selectedWindowIndex);
    }

    private SecondaryChatSettings.TabConfig selectedTab() {
        SecondaryChatSettings.WindowConfig window = selectedWindow();
        if (window == null || window.tabs.isEmpty()) {
            return null;
        }
        this.selectedTabIndex = Math.clamp(this.selectedTabIndex, 0, window.tabs.size() - 1);
        return window.tabs.get(this.selectedTabIndex);
    }

    private static String uniqueWindowId(String base, List<SecondaryChatSettings.WindowConfig> windows) {
        String candidate = sanitizeId(base);
        int suffix = 2;
        while (containsWindowId(windows, candidate)) {
            candidate = sanitizeId(base) + "_" + suffix++;
        }
        return candidate;
    }

    private static String uniqueTabId(String base, List<SecondaryChatSettings.TabConfig> tabs) {
        String candidate = sanitizeId(base);
        int suffix = 2;
        while (containsTabId(tabs, candidate)) {
            candidate = sanitizeId(base) + "_" + suffix++;
        }
        return candidate;
    }

    private static boolean containsWindowId(List<SecondaryChatSettings.WindowConfig> windows, String id) {
        for (SecondaryChatSettings.WindowConfig window : windows) {
            if (window.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static SecondaryChatSettings.WindowConfig findWindowById(List<SecondaryChatSettings.WindowConfig> windows,
                                                                     String id) {
        if (windows == null) {
            return null;
        }
        for (SecondaryChatSettings.WindowConfig window : windows) {
            if (window != null && window.id.equals(id)) {
                return window;
            }
        }
        return null;
    }

    private static boolean containsTabId(List<SecondaryChatSettings.TabConfig> tabs, String id) {
        for (SecondaryChatSettings.TabConfig tab : tabs) {
            if (tab.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return value.isEmpty() ? "item" : value;
    }

    private static int parseInt(String raw, int fallback, int min, int max) {
        try {
            return Math.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback, float min, float max) {
        try {
            return Math.clamp(Float.parseFloat(raw.trim()), min, max);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseColor(String raw, int fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            long parsed = Long.parseLong(value, 16);
            if (value.length() <= 6) {
                parsed |= 0xFF000000L;
            }
            return (int) parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
