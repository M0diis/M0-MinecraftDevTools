package me.m0dii.modules.macros.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.GuiTextEditingUtils;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandhistory.CommandHistoryManager;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholderCatalog;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.ProxyAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.SecondaryAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.StandardAdvancedLayout;
import me.m0dii.modules.macros.hud.HudElementUtils;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.nbthud.NBTInfoHudModule;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.utils.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

public class MacroWorkbenchScreen extends Screen {

    public enum Tab {
        CANVAS,
        KEYBOARD,
        PLACEHOLDERS,
        ENTITY_RADAR,
        COMMAND_HISTORY,
        MESSAGE_HISTORY,
        CONFIGURATION
    }

    private static final int TOP_BAR_H = 54;
    private static final int BOTTOM_BAR_H = 44;
    private static final int CANVAS_CONTENT_TOP = 0;
    private static final int MODAL_W = 620;
    private static final int MODAL_H = 420;
    private static final String EXTERNAL_NBT_INSPECTOR_ID = "__ext_nbt_inspector";
    private static final String EXTERNAL_SECONDARY_CHAT_ID = "__ext_secondary_chat";
    private static final String EXTERNAL_MACRO_KEYBINDS_ID = "__ext_macro_keybinds";
    private static final String EXTERNAL_PICKUP_NOTIFIER_ID = "__ext_pickup_notifier";
    private static boolean GRID_ENABLED_PREF = false;
    private static int GRID_ROWS_PREF = 12;
    private static int GRID_COLS_PREF = 16;
    private static boolean CENTER_LINES_ENABLED_PREF = false;
    private static final int[] STYLE_COLOR_PALETTE = {
            0xAA101010, 0xAA1F1F1F, 0xAA2D1F0E, 0xAA0E2D1F, 0xAA1F0E2D,
            0xCC2A2A2A, 0xCC4A2A2A, 0xCC2A4A2A, 0xCC2A2A4A, 0xCC808080,
            0xFFFFFFFF, 0xFFFFAA00, 0xFFFF5555, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF
    };
    private static final Set<String> SCRIPT_HIGHLIGHT_KEYWORDS = Set.of(
            "def", "class", "if", "else", "for", "while", "return", "import", "new",
            "true", "false", "null", "fun", "val", "var", "object", "when",
            "try", "catch", "finally", "throw", "in", "as", "is"
    );

    private final Screen parent;
    private Tab tab;

    private MacroHudDataHandler.HudConfig working;
    private MacroHudDataHandler.HudElement selected;
    private boolean dragging = false;
    private boolean resizing = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int resizeStartMouseX = 0;
    private int resizeStartMouseY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private String dragPrimaryElementId;
    private final Map<String, int[]> dragStartScreenPositions = new LinkedHashMap<>();
    private List<ElementSnapshot> activeMoveSnapshot;
    private final Deque<List<ElementSnapshot>> moveUndoStack = new ArrayDeque<>();
    private final LinkedHashSet<String> selectedElementIds = new LinkedHashSet<>();
    private final List<MacroHudDataHandler.HudElement> elementClipboard = new ArrayList<>();
    private int clipboardWidth = -1;
    private int clipboardHeight = -1;
    private final MacroWorkbenchCustomWidgetAdvancedClickHandler.Ops customWidgetAdvancedClickOpsBridge =
            new MacroWorkbenchCustomWidgetAdvancedClickOpsBridge(this);

    private TextFieldWidget quickField;
    private TextFieldWidget macroField;
    private TextFieldWidget actionField;
    private ButtonWidget backgroundToggle;
    private ButtonWidget borderToggle;
    private ButtonWidget editButton;
    private ButtonWidget deleteButton;
    private ButtonWidget gridToggleButton;
    private ButtonWidget gridRowsMinusButton;
    private ButtonWidget gridRowsPlusButton;
    private ButtonWidget gridColsMinusButton;
    private ButtonWidget gridColsPlusButton;
    private ButtonWidget centerLinesToggleButton;
    private ButtonWidget presetPrevButton;
    private ButtonWidget presetNextButton;
    private ButtonWidget presetNewButton;
    private ButtonWidget presetRenameButton;
    private ButtonWidget presetDeleteButton;
    private TextFieldWidget presetNameField;

    private final List<ClickableWidget> topBarWidgets = new ArrayList<>();
    private final List<ClickableWidget> canvasWidgets = new ArrayList<>();
    private final List<ClickableWidget> keyboardWidgets = new ArrayList<>();
    private final List<ClickableWidget> entityRadarWidgets = new ArrayList<>();
    private final List<ClickableWidget> cmdHistoryWidgets = new ArrayList<>();
    private final List<ClickableWidget> msgHistoryWidgets = new ArrayList<>();
    private final List<ClickableWidget> configWidgets = new ArrayList<>();
    private MacroWorkbenchConfigurationTab configurationTabController;

    private final List<KeyCell> cells = new ArrayList<>();
    private final Map<Integer, List<String>> macroBindingNames = new HashMap<>();
    private final Map<Integer, List<String>> macroBindingIds = new HashMap<>();
    private final Map<Integer, List<String>> gameBindingNames = new HashMap<>();
    private int selectedKey = -1;
    private String selectedMacroId;
    private boolean showGameKeybinds = false;

    private TextFieldWidget kbNameField;
    private TextFieldWidget kbCommandsField;
    private TextFieldWidget kbDelayField;

    private boolean advancedOpen = false;
    private boolean advancedTextFocused = false;
    private boolean advancedActionFocused = false;
    private boolean advancedBgColorFocused = false;
    private boolean advancedBorderColorFocused = false;
    private boolean advancedVisibilityScreenTypeFocused = false;
    private String advancedText = "";
    private String advancedAction = "";
    private String advancedVisibilityScreenType = "";
    private String advancedBgColor = "";
    private String advancedBorderColor = "";
    private int advancedCursor = 0;
    private int advancedTextScrollLine = 0;
    private boolean advancedTextManualScroll = false;
    private int advancedActionCursor = 0;
    private int advancedActionSuggestionIndex = -1;
    private int advancedActionSuggestionScroll = 0;
    private int advancedSelectionAnchor = -1;
    private int advancedActionSelectionAnchor = -1;
    private int advancedBgCursor = 0;
    private int advancedBorderCursor = 0;
    private int advancedVisibilityScreenTypeCursor = 0;
    private int advancedBgSelectionAnchor = -1;
    private int advancedBorderSelectionAnchor = -1;
    private int advancedVisibilityScreenTypeSelectionAnchor = -1;
    private boolean colorPickerOpen = false;
    private IntConsumer colorPickerApply;
    private String colorPickerTitle = "Pick Color";
    private int colorPickerX = 0;
    private int colorPickerY = 0;
    private boolean colorPickerDragging = false;
    private int colorPickerDragOffsetX = 0;
    private int colorPickerDragOffsetY = 0;
    private ModalDragSelectionField activeDragSelectionField = ModalDragSelectionField.NONE;
    private int modalDragStartMouseX = Integer.MIN_VALUE;
    private int modalDragStartMouseY = Integer.MIN_VALUE;
    private boolean modalDragSelectionStarted = false;
    private int snapGuideX = Integer.MIN_VALUE;
    private int snapGuideY = Integer.MIN_VALUE;

    private static final int[] COLOR_PICKER_PRESETS = {
            0xFFFFFFFF, 0xFF000000, 0xFFFF5555, 0xFF55FF55,
            0xFF5555FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF,
            0xFFB0B0B0, 0xFF808080, 0xFF4B2E1A, 0xFF996633,
            0xFF2C3E50, 0xFF16A085, 0xFF8E44AD, 0xFFFF8800
    };

    private int placeholderScroll = 0;
    private boolean gridEnabled = GRID_ENABLED_PREF;

    private List<String> cmdHistoryItems = new ArrayList<>();
    private int cmdHistoryScroll = 0;
    private List<String> msgHistoryItems = new ArrayList<>();
    private int msgHistoryScroll = 0;
    private List<Entity> entityRadarItems = new ArrayList<>();
    private int entityRadarScroll = 0;
    private static final int HISTORY_LINE_HEIGHT = 20;
    private int gridRows = GRID_ROWS_PREF;
    private int gridCols = GRID_COLS_PREF;
    private boolean centerLinesEnabled = CENTER_LINES_ENABLED_PREF;
    private int gridOverlayTicks = 0;
    private boolean canvasChromeVisible = true;

    private boolean kbCommandsModalOpen = false;
    private boolean kbCommandsFocused = false;
    private String kbCommandsText = "";
    private int kbCommandsCursor = 0;
    private int kbCommandsSelectionAnchor = -1;

    private boolean advancedSecondaryShowWhileGuiOpen;
    private boolean advancedSecondaryFadeEnabled;
    private boolean advancedSecondaryResetTransparencyOnHover;
    private boolean advancedSecondaryNoTransparencyWhenChatOpen;
    private int advancedSecondaryFadeDurationMs;
    private int advancedSecondaryMinAlpha;
    private int advancedSecondaryMaxLines;
    private double advancedSecondaryScale;
    private int advancedSecondaryLineHeight;
    private SecondaryChatSettings.InterceptMode advancedSecondaryInterceptMode;

    private boolean addElementModalOpen = false;
    private boolean advancedModalDragging = false;
    private int advancedModalDragOffsetX = 0;
    private int advancedModalDragOffsetY = 0;
    private Integer advancedModalPosX = null;
    private Integer advancedModalPosY = null;

    private record KeyCell(int keyCode, String label, int x, int y, int w, int h) {
    }

    private record ElementSnapshot(String id, int x, int y, int width, int height) {
    }

    private enum ModalDragSelectionField {
        NONE,
        ADVANCED_TEXT,
        ADVANCED_ACTION,
        ADVANCED_BG,
        ADVANCED_BORDER,
        KB_COMMANDS
    }

    private enum ExternalProxyRenderState {
        ACTIVE,
        MODULE_DISABLED
    }

    public MacroWorkbenchScreen(Screen parent, Tab initialTab) {
        super(Text.literal("Macro Workbench"));
        this.parent = parent;
        this.tab = initialTab == null ? Tab.CANVAS : initialTab;
    }

    public static Screen create(Screen parent, Tab tab) {
        return new MacroWorkbenchScreen(parent, tab);
    }

    @Override
    protected void init() {
        super.init();

        if (this.working == null) {
            this.working = MacroHudDataHandler.getConfigCopy();
        }
        syncExternalCanvasElementsFromSources();

        rebuildBindingMaps();
        rebuildKeyboardGrid();

        int topSaveW = 66;
        int topDoneW = 66;
        int saveX = this.width - topSaveW - (topDoneW + 12);
        int saveY = 8;
        int doneX = this.width - topDoneW - 8;
        int doneY = 8;
        ButtonWidget saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveAll())
                .dimensions(saveX, saveY, topSaveW, 20)
                .build();
        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), b -> {
            saveAll();
            close();
        }).dimensions(doneX, doneY, topDoneW, 20).build();
        List<Tab> tabs = List.of(
                Tab.CANVAS,
                Tab.KEYBOARD,
                Tab.PLACEHOLDERS,
                Tab.ENTITY_RADAR,
                Tab.COMMAND_HISTORY,
                Tab.MESSAGE_HISTORY,
                Tab.CONFIGURATION
        );
        List<String> labels = List.of("Canvas", "Keyboard", "Placeholders", "Entity Radar", "Cmd Hist", "Msg Hist", "Configuration");
        List<String> compactLabels = List.of("Canvas", "Keys", "Place", "Radar", "Cmd", "Msg", "Config");
        List<String> tinyLabels = List.of("CV", "KB", "PH", "ER", "CH", "MH", "CF");
        int tabGap = 4;
        int tabsStartX = 8;
        int tabsEndX = saveX - 8;
        int tabWidth = Math.max(16, (tabsEndX - tabsStartX - (tabGap * (tabs.size() - 1))) / tabs.size());
        int tabY = 8;
        int tabX = tabsStartX;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tabDef = tabs.get(i);
            String tabLabel = tabWidth < 36 ? tinyLabels.get(i) : (tabWidth < 72 ? compactLabels.get(i) : labels.get(i));
            ButtonWidget tabButton = ButtonComponent.builder(Text.literal(tabLabel), b -> setTab(tabDef))
                    .dimensions(tabX, tabY, tabWidth, 20)
                    .build();
            this.topBarWidgets.add(tabButton);
            addDrawableChild(tabButton);
            tabX += tabWidth + tabGap;
        }
        this.topBarWidgets.add(saveButton);
        this.topBarWidgets.add(doneButton);
        addDrawableChild(saveButton);
        addDrawableChild(doneButton);

        int canvasControlY = 30;
        int canvasControlH = 20;
        UiRect canvasControlRow = FormPanels.panel(8, canvasControlY, Math.max(320, this.width - 16), canvasControlH);
        List<UiRect> canvasControlSlots = FormPanels.row(canvasControlRow, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.flex(90, 1), // Add Element
                UiFlexLayout.Item.flex(70, 1),  // Grid
                UiFlexLayout.Item.flex(26, 1),  // R-
                UiFlexLayout.Item.flex(26, 1),  // R+
                UiFlexLayout.Item.flex(26, 1),  // C-
                UiFlexLayout.Item.flex(26, 1),  // C+
                UiFlexLayout.Item.flex(90, 1),  // Center
                UiFlexLayout.Item.flex(22, 1),  // <
                UiFlexLayout.Item.flex(22, 1),  // >
                UiFlexLayout.Item.flex(120, 1), // Preset Name
                UiFlexLayout.Item.flex(42, 1),  // New
                UiFlexLayout.Item.flex(60, 1),  // Rename
                UiFlexLayout.Item.flex(54, 1)   // Delete
        );
        ButtonWidget addElement = ButtonWidget.builder(Text.literal("Add Element"), b -> addElementModalOpen = true)
                .dimensions(canvasControlSlots.get(0).x(), canvasControlSlots.get(0).y(), canvasControlSlots.get(0).width(), canvasControlSlots.get(0).height()).build();

        this.deleteButton = ButtonWidget.builder(Text.literal("Delete"), b -> {
            if (selected != null) {
                Set<String> ids = selectedElementIds.isEmpty() ? Set.of(selected.id) : new HashSet<>(selectedElementIds);
                this.working.elements.removeIf(e -> ids.contains(e.id));
                selectedElementIds.clear();
                this.selected = null;
                syncCanvasFields();
            }
        }).dimensions(this.width - 160, this.height - 38, 76, 18).build();

        this.gridToggleButton = ButtonWidget.builder(Text.literal("Grid: OFF"), b -> {
            gridEnabled = !gridEnabled;
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(1).x(), canvasControlSlots.get(1).y(), canvasControlSlots.get(1).width(), canvasControlSlots.get(1).height()).build();

        this.gridRowsMinusButton = ButtonWidget.builder(Text.literal("R-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.max(2, gridRows - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(2).x(), canvasControlSlots.get(2).y(), canvasControlSlots.get(2).width(), canvasControlSlots.get(2).height()).build();
        this.gridRowsPlusButton = ButtonWidget.builder(Text.literal("R+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.min(80, gridRows + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(3).x(), canvasControlSlots.get(3).y(), canvasControlSlots.get(3).width(), canvasControlSlots.get(3).height()).build();
        this.gridColsMinusButton = ButtonWidget.builder(Text.literal("C-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.max(2, gridCols - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(4).x(), canvasControlSlots.get(4).y(), canvasControlSlots.get(4).width(), canvasControlSlots.get(4).height()).build();
        this.gridColsPlusButton = ButtonWidget.builder(Text.literal("C+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.min(80, gridCols + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(5).x(), canvasControlSlots.get(5).y(), canvasControlSlots.get(5).width(), canvasControlSlots.get(5).height()).build();

        this.centerLinesToggleButton = ButtonWidget.builder(Text.literal("Center: OFF"), b -> {
            centerLinesEnabled = !centerLinesEnabled;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(canvasControlSlots.get(6).x(), canvasControlSlots.get(6).y(), canvasControlSlots.get(6).width(), canvasControlSlots.get(6).height()).build();

        this.presetPrevButton = ButtonWidget.builder(Text.literal("<"), b -> cyclePreset(false)).dimensions(canvasControlSlots.get(7).x(), canvasControlSlots.get(7).y(), canvasControlSlots.get(7).width(), canvasControlSlots.get(7).height()).build();
        this.presetNextButton = ButtonWidget.builder(Text.literal(">"), b -> cyclePreset(true)).dimensions(canvasControlSlots.get(8).x(), canvasControlSlots.get(8).y(), canvasControlSlots.get(8).width(), canvasControlSlots.get(8).height()).build();
        this.presetNameField = new TextFieldWidget(this.textRenderer, canvasControlSlots.get(9).x(), canvasControlSlots.get(9).y(), canvasControlSlots.get(9).width(), canvasControlSlots.get(9).height(), Text.literal("Preset"));
        this.presetNameField.setMaxLength(40);
        this.presetNewButton = ButtonWidget.builder(Text.literal("New"), b -> createPresetFromField()).dimensions(canvasControlSlots.get(10).x(), canvasControlSlots.get(10).y(), canvasControlSlots.get(10).width(), canvasControlSlots.get(10).height()).build();
        this.presetRenameButton = ButtonWidget.builder(Text.literal("Rename"), b -> renamePresetFromField()).dimensions(canvasControlSlots.get(11).x(), canvasControlSlots.get(11).y(), canvasControlSlots.get(11).width(), canvasControlSlots.get(11).height()).build();
        this.presetDeleteButton = ButtonWidget.builder(Text.literal("Delete"), b -> {
            MacroHudDataHandler.deletePreset(MacroHudDataHandler.getActivePresetId());
            this.working = MacroHudDataHandler.getConfigCopy();
            syncExternalCanvasElementsFromSources();
            syncCanvasFields();
            syncGridButtons();
            syncPresetControls();
        }).dimensions(canvasControlSlots.get(12).x(), canvasControlSlots.get(12).y(), canvasControlSlots.get(12).width(), canvasControlSlots.get(12).height()).build();

        this.quickField = new TextFieldWidget(this.textRenderer, 8, this.height - 38, 280, 18, Text.literal("Quick Edit"));
        this.quickField.setMaxLength(300);
        this.macroField = new TextFieldWidget(this.textRenderer, 292, this.height - 38, 120, 18, Text.literal("Macro Id"));
        this.macroField.setMaxLength(120);
        this.actionField = new TextFieldWidget(this.textRenderer, 416, this.height - 38, 180, 18, Text.literal("Action"));
        this.actionField.setMaxLength(32767);

        this.backgroundToggle = ButtonWidget.builder(Text.literal("BG: OFF"), b -> {
            if (selected != null) {
                selected.drawBackground = !selected.drawBackground;
                ensureVisibleBackground(selected);
                syncStyleButtons();
            }
        }).dimensions(this.width - 316, this.height - 38, 74, 18).build();

        this.borderToggle = ButtonWidget.builder(Text.literal("Border: OFF"), b -> {
            if (selected != null) {
                cycleBorderSetting(selected, true);
                syncStyleButtons();
            }
        }).dimensions(this.width - 238, this.height - 38, 78, 18).build();

        this.editButton = ButtonWidget.builder(Text.literal("Edit"), b -> openAdvancedModal())
                .dimensions(this.width - 80, this.height - 38, 72, 18).build();

        this.canvasWidgets.add(addElement);
        this.canvasWidgets.add(deleteButton);
        this.canvasWidgets.add(gridToggleButton);
        this.canvasWidgets.add(gridRowsMinusButton);
        this.canvasWidgets.add(gridRowsPlusButton);
        this.canvasWidgets.add(gridColsMinusButton);
        this.canvasWidgets.add(gridColsPlusButton);
        this.canvasWidgets.add(centerLinesToggleButton);
        this.canvasWidgets.add(presetPrevButton);
        this.canvasWidgets.add(presetNextButton);
        this.canvasWidgets.add(presetNameField);
        this.canvasWidgets.add(presetNewButton);
        this.canvasWidgets.add(presetRenameButton);
        this.canvasWidgets.add(presetDeleteButton);
        this.canvasWidgets.add(quickField);
        this.canvasWidgets.add(macroField);
        this.canvasWidgets.add(actionField);
        this.canvasWidgets.add(backgroundToggle);
        this.canvasWidgets.add(borderToggle);
        this.canvasWidgets.add(editButton);
        addDrawableChild(addElement);
        addDrawableChild(deleteButton);
        addDrawableChild(gridToggleButton);
        addDrawableChild(gridRowsMinusButton);
        addDrawableChild(gridRowsPlusButton);
        addDrawableChild(gridColsMinusButton);
        addDrawableChild(gridColsPlusButton);
        addDrawableChild(centerLinesToggleButton);
        addDrawableChild(presetPrevButton);
        addDrawableChild(presetNextButton);
        addDrawableChild(presetNameField);
        addDrawableChild(presetNewButton);
        addDrawableChild(presetRenameButton);
        addDrawableChild(presetDeleteButton);
        addDrawableChild(quickField);
        addDrawableChild(macroField);
        addDrawableChild(actionField);
        addDrawableChild(backgroundToggle);
        addDrawableChild(borderToggle);
        addDrawableChild(editButton);

        int panelW = keyboardPanelWidth();
        int panelX = keyboardPanelX();
        int panelInnerW = panelW - 20;
        int keyboardRowY1 = this.height - 50;
        int keyboardRowY2 = this.height - 28;

        ButtonWidget gameToggle = ButtonWidget.builder(Text.literal("Show Game Binds: OFF"), b -> {
            showGameKeybinds = !showGameKeybinds;
            b.setMessage(Text.literal("Show Game Binds: " + (showGameKeybinds ? "ON" : "OFF")));
        }).dimensions(panelX + 10, TOP_BAR_H + 8, panelInnerW, 18).build();

        this.kbNameField = new TextFieldWidget(this.textRenderer, panelX + 10, this.height - 94, panelInnerW, 18, Text.literal("Macro Name"));
        this.kbCommandsField = new TextFieldWidget(this.textRenderer, panelX + 10, this.height - 72, panelInnerW, 18, Text.literal("Commands (; separated)"));

        int delayW = Math.clamp(panelInnerW / 3, 72, 120);
        int row1RightW = panelInnerW - delayW - 4;
        int editW = Math.max(70, row1RightW / 2 - 2);
        int kbSaveW = Math.max(70, row1RightW - editW - 4);
        int newW = Math.max(76, (panelInnerW - 8) / 3);
        int deleteW = Math.max(66, (panelInnerW - 8 - newW) / 2);
        int openW = Math.max(80, panelInnerW - newW - deleteW - 8);

        this.kbDelayField = new TextFieldWidget(this.textRenderer, panelX + 10, keyboardRowY1, delayW, 18, Text.literal("Delay Ticks"));
        this.kbNameField.setMaxLength(80);
        this.kbCommandsField.setMaxLength(500);
        this.kbDelayField.setMaxLength(6);

        ButtonWidget kbEditCommands = ButtonWidget.builder(Text.literal("Edit Cmds"), b -> openKeyboardCommandsModal())
                .dimensions(panelX + 10 + delayW + 4, keyboardRowY1, editW, 18).build();
        ButtonWidget kbSave = ButtonWidget.builder(Text.literal("Save Macro"), b -> saveKeyboardMacro())
                .dimensions(panelX + 10 + delayW + 4 + editW + 4, keyboardRowY1, kbSaveW, 18).build();
        ButtonWidget kbNew = ButtonWidget.builder(Text.literal("+ New"), b -> createKeyboardMacro()).dimensions(panelX + 10, keyboardRowY2, newW, 18).build();
        ButtonWidget kbDelete = ButtonWidget.builder(Text.literal("Delete"), b -> deleteKeyboardMacro())
                .dimensions(panelX + 10 + newW + 4, keyboardRowY2, deleteW, 18).build();
        ButtonWidget kbOpenManager = ButtonWidget.builder(Text.literal("Open Macro Manager"),
                b -> this.client.setScreen(MacroConfigScreen.create(this))).dimensions(panelX + 10 + newW + 4 + deleteW + 4, keyboardRowY2, openW, 18).build();

        this.keyboardWidgets.add(gameToggle);
        this.keyboardWidgets.add(kbNameField);
        this.keyboardWidgets.add(kbCommandsField);
        this.keyboardWidgets.add(kbDelayField);
        this.keyboardWidgets.add(kbSave);
        this.keyboardWidgets.add(kbDelete);
        this.keyboardWidgets.add(kbEditCommands);
        this.keyboardWidgets.add(kbNew);
        this.keyboardWidgets.add(kbOpenManager);
        addDrawableChild(gameToggle);
        addDrawableChild(kbNameField);
        addDrawableChild(kbCommandsField);
        addDrawableChild(kbDelayField);
        addDrawableChild(kbSave);
        addDrawableChild(kbDelete);
        addDrawableChild(kbEditCommands);
        addDrawableChild(kbNew);
        addDrawableChild(kbOpenManager);

        ButtonWidget clearCmdHistoryButton = ButtonWidget.builder(
                        Text.literal("Clear History"),
                        b -> {
                            CommandHistoryManager.clearHistory();
                            cmdHistoryItems.clear();
                        })
                .dimensions(this.width / 2 + 5, this.height - 30, 110, 20).build();
        ButtonWidget clearMsgHistoryButton = ButtonWidget.builder(
                        Text.literal("Clear History"),
                        b -> {
                            MessageHistoryManager.clearHistory();
                            msgHistoryItems.clear();
                        })
                .dimensions(this.width / 2 + 5, this.height - 30, 110, 20).build();
        this.cmdHistoryWidgets.add(clearCmdHistoryButton);
        this.msgHistoryWidgets.add(clearMsgHistoryButton);
        addDrawableChild(clearCmdHistoryButton);
        addDrawableChild(clearMsgHistoryButton);

        initConfigurationWidgets();

        syncCanvasFields();
        syncGridButtons();
        syncPresetControls();
        syncKeyboardFields();
        setTab(this.tab);
    }

    private void setTab(Tab next) {
        this.tab = next;
        boolean canvas = next == Tab.CANVAS;
        boolean keyboard = next == Tab.KEYBOARD;
        boolean placeholders = next == Tab.PLACEHOLDERS;

        refreshTabWidgetVisibility();

        if (canvas) {
            refreshCanvasChromeVisibility();
            syncCanvasFields();
        } else if (keyboard) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            rebuildBindingMaps();
            rebuildKeyboardGrid();
            syncKeyboardFields();
        } else if (placeholders) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
        } else if (next == Tab.ENTITY_RADAR) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            entityRadarItems = new ArrayList<>(EntityRadarModule.INSTANCE.getEntities());
            entityRadarScroll = 0;
        } else if (next == Tab.COMMAND_HISTORY) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            cmdHistoryItems = new ArrayList<>(CommandHistoryManager.getHistory());
            cmdHistoryScroll = 0;
        } else if (next == Tab.MESSAGE_HISTORY) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            msgHistoryItems = new ArrayList<>(MessageHistoryManager.getHistory());
            msgHistoryScroll = 0;
        } else if (next == Tab.CONFIGURATION) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            syncConfigurationControls();
        }
    }

    private void refreshTabWidgetVisibility() {
        boolean canvasTab = this.tab == Tab.CANVAS;
        boolean keyboardTab = this.tab == Tab.KEYBOARD;
        boolean entityRadarTab = this.tab == Tab.ENTITY_RADAR;
        boolean cmdHistoryTab = this.tab == Tab.COMMAND_HISTORY;
        boolean msgHistoryTab = this.tab == Tab.MESSAGE_HISTORY;
        boolean configTab = this.tab == Tab.CONFIGURATION;
        // In canvas, F1 controls visibility; in other tabs controls are always visible.
        boolean showChrome = !canvasTab || canvasChromeVisible;

        setWidgetState(topBarWidgets, showChrome);
        setWidgetState(canvasWidgets, canvasTab && showChrome);
        setWidgetState(keyboardWidgets, keyboardTab);
        setWidgetState(entityRadarWidgets, entityRadarTab);
        setWidgetState(cmdHistoryWidgets, cmdHistoryTab);
        setWidgetState(msgHistoryWidgets, msgHistoryTab);
        setWidgetState(configWidgets, configTab);
    }

    private static void setWidgetState(List<ClickableWidget> widgets, boolean visible) {
        for (ClickableWidget widget : widgets) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);
        if (canvasChromeVisible) {
            context.fill(0, TOP_BAR_H - 1, this.width, TOP_BAR_H, 0x70FFFFFF);
        }

        if (this.tab == Tab.CANVAS) {
            renderCanvasTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.KEYBOARD) {
            renderKeyboardTab(context);
        } else if (this.tab == Tab.ENTITY_RADAR) {
            renderEntityRadarTab(context);
        } else if (this.tab == Tab.COMMAND_HISTORY) {
            renderCommandHistoryTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.MESSAGE_HISTORY) {
            renderMessageHistoryTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.CONFIGURATION) {
            renderConfigurationTab(context);
        } else {
            renderPlaceholdersTab(context);
        }

        // Draw normal widgets first.
        super.render(context, mouseX, mouseY, delta);

        if (advancedOpen && advancedModalDragging && this.client != null) {
            long window = this.client.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                this.advancedModalPosX = mouseX - advancedModalDragOffsetX;
                this.advancedModalPosY = mouseY - advancedModalDragOffsetY;
            } else {
                this.advancedModalDragging = false;
            }
        }
        if (colorPickerOpen && colorPickerDragging && this.client != null) {
            long window = this.client.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                int pickerW = 140;
                int pickerH = 86;
                this.colorPickerX = Math.clamp(mouseX - colorPickerDragOffsetX, 6, Math.max(6, this.width - pickerW - 6));
                this.colorPickerY = Math.clamp(mouseY - colorPickerDragOffsetY, 26, Math.max(26, this.height - pickerH - 6));
            } else {
                this.colorPickerDragging = false;
            }
        }

        if (advancedOpen) {
            renderAdvancedModal(context, mouseX, mouseY);
            updateModalDragSelection(mouseX, mouseY);
        }
        if (kbCommandsModalOpen) {
            renderKeyboardCommandsModal(context, mouseX, mouseY);
            updateModalDragSelection(mouseX, mouseY);
        }
        if (this.tab == Tab.CANVAS && addElementModalOpen && !advancedOpen && !kbCommandsModalOpen) {
            renderAddElementModal(context, mouseX, mouseY);
        }
        if (advancedOpen && colorPickerOpen) {
            renderColorPickerPopup(context, mouseX, mouseY);
        }
    }

    private void renderCanvasTab(DrawContext context, int mouseX, int mouseY) {
        applyQuickEdit();
        updateDragging(mouseX, mouseY);

        context.drawTextWithShadow(this.textRenderer,
                "Press F1 to toggle placement mode: " + (canvasChromeVisible ? "OFF" : "ON"),
                8, TOP_BAR_H + 4, 0xFF9FCFCF);
        context.drawTextWithShadow(this.textRenderer,
                "Ctrl+Click multi-select, Ctrl+C/V copy/paste element, Ctrl+Shift+C/V dimensions, Ctrl+Z undo move",
                8, TOP_BAR_H + 16, 0xFF9FBFBF);
        int canvasHeight = canvasHeightBound();
        int cellW = Math.max(1, Math.round(this.width / (float) Math.max(1, gridCols)));
        int cellH = Math.max(1, Math.round(canvasHeight / (float) Math.max(1, gridRows)));
        context.drawTextWithShadow(this.textRenderer,
                "Grid " + (gridEnabled ? "ON" : "OFF") + "  " + gridCols + "x" + gridRows + "  Cell: " + cellW + "x" + cellH,
                8, TOP_BAR_H + 28, 0xFFBFCFCF);

        if (gridEnabled || gridOverlayTicks > 0) {
            drawGridOverlay(context);
            if (gridOverlayTicks > 0) {
                gridOverlayTicks--;
            }
        }
        if (centerLinesEnabled) {
            drawCenterLinesOverlay(context);
        }

        for (MacroHudDataHandler.HudElement element : HudElementUtils.sortedByLayer(this.working.elements)) {
            if (element.visible) {
                drawCanvasElement(context, element);
            }
        }
        drawSnapGuides(context);

        int y = this.height - BOTTOM_BAR_H;
            if (this.macroField.visible) {
            context.drawTextWithShadow(this.textRenderer, "Quick Edit (first line)", 8, y - 10, 0xFFAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Macro Id (optional)", 292, y - 10, 0xFFAAAAAA);
            String actionLabel = "Action (optional)";
            if (selected != null && selected.type == MacroHudDataHandler.ElementType.BUTTON
                    && selected.buttonExecutionMode != MacroHudDataHandler.ButtonExecutionMode.COMMAND) {
                actionLabel = "Script (inline or file name)";
            }
            context.drawTextWithShadow(this.textRenderer, actionLabel, 416, y - 10, 0xFFAAAAAA);
        }
        if (selected != null) {
            context.drawTextWithShadow(this.textRenderer,
                    "Selected: " + selected.type + " Pos: " + selected.x + "," + selected.y + " Size: " + selected.width + "x" + selected.height,
                    8, this.height - 18, 0xFFCCCCCC);
        }
    }

    private void renderPlaceholdersTab(DrawContext context) {
        int left = 12;
        int top = TOP_BAR_H + 8;
        int bottom = this.height - 14;
        int lineHeight = 11;
        int maxLines = Math.max(1, (bottom - top) / lineHeight);
        List<String> docs = MacroPlaceholders.getPlaceholderDocs();
        int maxScroll = Math.max(0, docs.size() - maxLines);
        placeholderScroll = Math.clamp(placeholderScroll, 0, maxScroll);

        context.drawTextWithShadow(this.textRenderer, "Placeholder reference and return values", left, TOP_BAR_H - 16, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Mouse wheel scrolls this page", this.width - 170, TOP_BAR_H - 16, 0xFF9F9F9F);

        int y = top;
        for (int i = placeholderScroll; i < docs.size() && y <= bottom; i++) {
            String line = docs.get(i);
            int color = line.startsWith("[") ? 0xFF55FFFF : 0xFFD8D8D8;
            context.drawTextWithShadow(this.textRenderer, line, left, y, color);
            y += lineHeight;
        }
    }

    private void initConfigurationWidgets() {
        this.configurationTabController = new MacroWorkbenchConfigurationTab(this, this.configWidgets, this::isShiftDown);
        this.configurationTabController.initWidgets();
    }


    private void syncConfigurationControls() {
        if (this.configurationTabController != null) {
            this.configurationTabController.syncControls();
        }
    }

    private void renderConfigurationTab(DrawContext context) {
        if (this.configurationTabController != null) {
            this.configurationTabController.render(context);
        }
    }


    private int getHistoryVisibleLines() {
        return Math.max(1, (this.height - TOP_BAR_H - 80) / HISTORY_LINE_HEIGHT);
    }

    private void renderCommandHistoryTab(DrawContext context, int mouseX, int mouseY) {
        int top = TOP_BAR_H + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Command History"), width / 2, top, 0xFFFFFFFF);
        String info = cmdHistoryItems.isEmpty() ? "No commands in history yet." : "Click any command to copy it to clipboard";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), width / 2, top + 14, 0xFFAAAAAA);

        if (!cmdHistoryItems.isEmpty()) {
            int listY = top + 32;
            int maxVisible = Math.min(getHistoryVisibleLines(), cmdHistoryItems.size() - cmdHistoryScroll);
            for (int i = 0; i < maxVisible; i++) {
                int index = i + cmdHistoryScroll;
                if (index >= cmdHistoryItems.size()) {
                    break;
                }
                String command = cmdHistoryItems.get(index);
                int y = listY + (i * HISTORY_LINE_HEIGHT);
                int boxX1 = 20, boxX2 = width - 20;
                int boxY1 = y - 2, boxY2 = y + HISTORY_LINE_HEIGHT - 2;
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2 && mouseY >= boxY1 && mouseY <= boxY2;
                if (hovering) {
                    context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
                }
                String displayText = (index + 1) + ". " + command;
                int textColor = hovering ? 0xFFFFFF00 : 0xFFFFFFFF;
                int maxWidth = width - 50;
                if (textRenderer.getWidth(displayText) > maxWidth) {
                    while (textRenderer.getWidth(displayText + "...") > maxWidth && displayText.length() > 10)
                        displayText = displayText.substring(0, displayText.length() - 1);
                    displayText += "...";
                }
                context.drawTextWithShadow(this.textRenderer, displayText, 25, y, textColor);
                if (hovering) {
                    String hint = "[Click to copy]";
                    context.drawTextWithShadow(this.textRenderer, Text.literal(hint), boxX2 - textRenderer.getWidth(hint) - 5, y, 0xFFFFFF00);
                }
            }
            if (cmdHistoryItems.size() > getHistoryVisibleLines()) {
                int total = (cmdHistoryItems.size() + getHistoryVisibleLines() - 1) / getHistoryVisibleLines();
                int cur = (cmdHistoryScroll / getHistoryVisibleLines()) + 1;
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Page " + cur + "/" + total + " (Scroll to navigate)"),
                        width / 2, height - 52, 0xFF888888);
            }
        }
    }

    private void renderMessageHistoryTab(DrawContext context, int mouseX, int mouseY) {
        int top = TOP_BAR_H + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Message History"), width / 2, top, 0xFFFFFFFF);
        String info = msgHistoryItems.isEmpty() ? "No messages in history yet." : "Click any message to copy it to clipboard";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), width / 2, top + 14, 0xFFAAAAAA);

        if (!msgHistoryItems.isEmpty()) {
            int listY = top + 32;
            int maxVisible = Math.min(getHistoryVisibleLines(), msgHistoryItems.size() - msgHistoryScroll);
            for (int i = 0; i < maxVisible; i++) {
                int index = i + msgHistoryScroll;
                if (index >= msgHistoryItems.size()) {
                    break;
                }
                String message = msgHistoryItems.get(index);
                int y = listY + (i * HISTORY_LINE_HEIGHT);
                int boxX1 = 20, boxX2 = width - 20;
                int boxY1 = y - 2, boxY2 = y + HISTORY_LINE_HEIGHT - 2;
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2 && mouseY >= boxY1 && mouseY <= boxY2;
                if (hovering) {
                    context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
                }
                String displayText = (index + 1) + ". " + message;
                int textColor = hovering ? 0xFFFFFF00 : 0xFFFFFFFF;
                int maxWidth = width - 50;
                if (textRenderer.getWidth(displayText) > maxWidth) {
                    while (textRenderer.getWidth(displayText + "...") > maxWidth && displayText.length() > 10)
                        displayText = displayText.substring(0, displayText.length() - 1);
                    displayText += "...";
                }
                context.drawTextWithShadow(this.textRenderer, displayText, 25, y, textColor);
                if (hovering) {
                    String hint = "[Click to copy]";
                    context.drawTextWithShadow(this.textRenderer, Text.literal(hint), boxX2 - textRenderer.getWidth(hint) - 5, y, 0xFFFFFF00);
                }
            }
            if (msgHistoryItems.size() > getHistoryVisibleLines()) {
                int total = (msgHistoryItems.size() + getHistoryVisibleLines() - 1) / getHistoryVisibleLines();
                int cur = (msgHistoryScroll / getHistoryVisibleLines()) + 1;
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Page " + cur + "/" + total + " (Scroll to navigate)"),
                        width / 2, height - 52, 0xFF888888);
            }
        }
    }

    private void renderEntityRadarTab(DrawContext context) {
        int top = TOP_BAR_H + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Entity Radar"), width / 2, top, 0xFFFFFFFF);

        if (this.client != null && this.client.world != null && this.client.player != null) {
            entityRadarItems = new ArrayList<>(EntityRadarModule.INSTANCE.getEntities());
            entityRadarScroll = Math.clamp(entityRadarScroll, 0, Math.max(0, entityRadarItems.size() - getHistoryVisibleLines()));
        } else {
            entityRadarItems = List.of();
            entityRadarScroll = 0;
        }

        int passive = 0;
        int hostile = 0;
        int neutral = 0;
        for (Entity entity : entityRadarItems) {
            if (entity instanceof HostileEntity) {
                hostile++;
            } else if (entity instanceof PassiveEntity) {
                passive++;
            } else {
                neutral++;
            }
        }

        String info = entityRadarItems.isEmpty()
                ? "No nearby entities."
                : "Total: " + entityRadarItems.size() + " | Passive: " + passive + " | Neutral: " + neutral + " | Hostile: " + hostile;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), width / 2, top + 14, 0xFFAAAAAA);

        if (!entityRadarItems.isEmpty()) {
            int listY = top + 32;
            int maxVisible = Math.min(getHistoryVisibleLines(), entityRadarItems.size() - entityRadarScroll);
            for (int i = 0; i < maxVisible; i++) {
                int index = i + entityRadarScroll;
                if (index >= entityRadarItems.size()) {
                    break;
                }

                Entity entity = entityRadarItems.get(index);
                String name = entity.getName().getString();
                double distance = this.client == null || this.client.player == null
                        ? 0.0
                        : entity.distanceTo(this.client.player);
                String line = String.format(Locale.ROOT, "%d. %s - %.1fm", index + 1, name, distance);

                int y = listY + (i * HISTORY_LINE_HEIGHT);
                int color = 0xFFFFFFFF;
                if (entity instanceof HostileEntity) {
                    color = 0xFFFF6666;
                } else if (entity instanceof PassiveEntity) {
                    color = 0xFF66FF66;
                }

                int maxWidth = width - 50;
                if (textRenderer.getWidth(line) > maxWidth) {
                    while (textRenderer.getWidth(line + "...") > maxWidth && line.length() > 10) {
                        line = line.substring(0, line.length() - 1);
                    }
                    line += "...";
                }
                context.drawTextWithShadow(this.textRenderer, line, 25, y, color);
            }
        }
    }

    private boolean onCommandHistoryClick(double mouseX, double mouseY) {
        if (cmdHistoryItems.isEmpty()) {
            return false;
        }
        int listY = TOP_BAR_H + 8 + 32;
        int maxVisible = Math.min(getHistoryVisibleLines(), cmdHistoryItems.size() - cmdHistoryScroll);
        for (int i = 0; i < maxVisible; i++) {
            int index = i + cmdHistoryScroll;
            if (index >= cmdHistoryItems.size()) {
                break;
            }
            String command = cmdHistoryItems.get(index);
            int y = listY + (i * HISTORY_LINE_HEIGHT);
            if (mouseX >= 20 && mouseX <= width - 20 && mouseY >= y - 2 && mouseY <= y + HISTORY_LINE_HEIGHT - 2) {
                if (client != null) {
                    client.keyboard.setClipboard(command);
                }
                return true;
            }
        }
        return false;
    }

    private boolean onMessageHistoryClick(double mouseX, double mouseY) {
        if (msgHistoryItems.isEmpty()) {
            return false;
        }
        int listY = TOP_BAR_H + 8 + 32;
        int maxVisible = Math.min(getHistoryVisibleLines(), msgHistoryItems.size() - msgHistoryScroll);
        for (int i = 0; i < maxVisible; i++) {
            int index = i + msgHistoryScroll;
            if (index >= msgHistoryItems.size()) {
                break;
            }
            String message = msgHistoryItems.get(index);
            int y = listY + (i * HISTORY_LINE_HEIGHT);
            if (mouseX >= 20 && mouseX <= width - 20 && mouseY >= y - 2 && mouseY <= y + HISTORY_LINE_HEIGHT - 2) {
                if (client != null) {
                    client.keyboard.setClipboard(message);
                }
                return true;
            }
        }
        return false;
    }

    private void renderKeyboardTab(DrawContext context) {
        int panelW = keyboardPanelWidth();
        int panelX = keyboardPanelX();
        int panelRight = panelX + panelW;
        context.drawTextWithShadow(this.textRenderer, "Macros = Green, Game binds = Blue", 8, TOP_BAR_H + 4, 0xFFB0FFB0);

        for (KeyCell cell : cells) {
            if (cell.x + cell.w > panelX - 6) {
                continue;
            }
            boolean macroBound = macroBindingIds.containsKey(cell.keyCode);
            boolean gameBound = showGameKeybinds && gameBindingNames.containsKey(cell.keyCode);
            boolean selectedCell = cell.keyCode == selectedKey;

            int bg = 0xAA202020;
            if (gameBound) {
                bg = 0xAA1C355A;
            }
            if (macroBound) {
                bg = 0xAA1E5A1E;
            }
            if (selectedCell) {
                bg = 0xAA7A5A1A;
            }

            context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, bg);
            context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + 1, 0x60FFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, cell.label, cell.x + (cell.w / 2), cell.y + 5, 0xFFFFFFFF);
        }

        context.fill(panelX, TOP_BAR_H, panelRight, this.height - 8, 0xAA111111);
        context.drawTextWithShadow(this.textRenderer, "Selected key", panelX + 10, TOP_BAR_H + 30, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, selectedKey < 0 ? "None" : MacroWorkbenchCanvasUtils.keyLabel(selectedKey), panelX + 10, TOP_BAR_H + 42, 0xFFFFFF55);

        int y = TOP_BAR_H + 62;
        List<String> ids = macroBindingIds.getOrDefault(selectedKey, List.of());
        List<String> names = macroBindingNames.getOrDefault(selectedKey, List.of());
        if (ids.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "No macros bound", panelX + 10, y, 0xFFAAAAAA);
        } else {
            context.drawTextWithShadow(this.textRenderer, "Macros (click):", panelX + 10, y, 0xFFAAFFAA);
            y += 14;
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                String name = i < names.size() ? names.get(i) : id;
                context.fill(panelX + 8, y - 2, panelRight - 8, y + 10, id.equals(selectedMacroId) ? 0x905A3A12 : 0x50202020);
                context.drawTextWithShadow(this.textRenderer, name + " [" + id + "]", panelX + 12, y, 0xFFE0E0E0);
                y += 13;
                if (y > this.height - 178) {
                    break;
                }
            }
        }

        if (showGameKeybinds) {
            int gameY = Math.max(y + 8, this.height - 168);
            context.drawTextWithShadow(this.textRenderer, "Game keybinds:", panelX + 10, gameY, 0xFF9AC2FF);
            gameY += 14;
            for (String bind : gameBindingNames.getOrDefault(selectedKey, List.of())) {
                if (gameY > this.height - 124) {
                    break;
                }
                context.drawTextWithShadow(this.textRenderer, "- " + bind, panelX + 10, gameY, 0xFFC8DFFF);
                gameY += 12;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            this.activeDragSelectionField = ModalDragSelectionField.NONE;
            this.modalDragStartMouseX = Integer.MIN_VALUE;
            this.modalDragStartMouseY = Integer.MIN_VALUE;
            this.modalDragSelectionStarted = false;
        }
        if (kbCommandsModalOpen) {
            return onKeyboardCommandsModalClick(click);
        }
        if (advancedOpen) {
            return onAdvancedMouseClick(click);
        }
        if (this.tab == Tab.CANVAS && addElementModalOpen) {
            return onAddElementModalClick(click);
        }

        // Configuration tab has right-click decrement controls.
        if (this.tab == Tab.CONFIGURATION && this.configurationTabController != null && click.button() != 0) {
            return this.configurationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }

        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (this.tab == Tab.CONFIGURATION && this.configurationTabController != null) {
            return this.configurationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }

        if (click.button() != 0) {
            return false;
        }

        if (this.tab == Tab.CANVAS) {
            boolean hit = onCanvasClick(click.x(), click.y());
            if (hit && doubled && selected != null) {
                openAdvancedModal();
                return true;
            }
            return hit;
        }
        if (this.tab == Tab.KEYBOARD) {
            return onKeyboardClick(click.x(), click.y());
        }
        if (this.tab == Tab.ENTITY_RADAR) {
            return false;
        }
        if (this.tab == Tab.COMMAND_HISTORY) {
            return onCommandHistoryClick(click.x(), click.y());
        }
        if (this.tab == Tab.MESSAGE_HISTORY) {
            return onMessageHistoryClick(click.x(), click.y());
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (advancedOpen) {
            if (selected != null && !isSecondaryChatProxy(selected) && !isNbtInspectorProxy(selected)
                    && !isMacroKeybindProxy(selected) && !isPickupNotifierProxy(selected)
                    && !isCustomWidgetType(selected)) {
                StandardAdvancedLayout layout = standardAdvancedLayout(modalX(), modalY());
                if (layout.textArea().contains(mouseX, mouseY)) {
                    int delta = verticalAmount > 0 ? -1 : 1;
                    int maxVisible = Math.max(1, (layout.textArea().height() - 8) / 9);
                    int maxScroll = Math.max(0, splitLinesRaw(advancedText).size() - maxVisible);
                    advancedTextScrollLine = Math.clamp(advancedTextScrollLine + delta, 0, maxScroll);
                    advancedTextManualScroll = true;
                    return true;
                }
            }
            if (selected != null && isCustomWidgetType(selected)) {
                CustomWidgetAdvancedLayout layout = customWidgetAdvancedLayout(modalX(), modalY());
                UiRect suggestionsArea = layout.suggestionArea();
                List<String> suggestions = advancedActionSuggestions();
                if (!suggestions.isEmpty()) {
                    int rowH = 10;
                    int maxVisible = Math.clamp(Math.max(1, suggestionsArea.height() / rowH), 1, suggestions.size());
                    int dropY = suggestionsArea.y();
                    int dropH = maxVisible * rowH;
                    if (containsBox(mouseX, mouseY, suggestionsArea.x(), dropY, suggestionsArea.width(), dropH)) {
                        int delta = verticalAmount > 0 ? -1 : 1;
                        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
                        advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll + delta, 0, maxScroll);
                    }
                }
            }
            return true;
        }

        if (this.tab == Tab.PLACEHOLDERS) {
            placeholderScroll -= verticalAmount > 0 ? 3 : -3;
            return true;
        }

        if (this.tab == Tab.COMMAND_HISTORY) {
            int maxScroll = Math.max(0, cmdHistoryItems.size() - getHistoryVisibleLines());
            cmdHistoryScroll = Math.clamp(cmdHistoryScroll + (verticalAmount > 0 ? -1 : 1), 0, maxScroll);
            return true;
        }

        if (this.tab == Tab.ENTITY_RADAR) {
            int maxScroll = Math.max(0, entityRadarItems.size() - getHistoryVisibleLines());
            entityRadarScroll = Math.clamp(entityRadarScroll + (verticalAmount > 0 ? -1 : 1), 0, maxScroll);
            return true;
        }

        if (this.tab == Tab.MESSAGE_HISTORY) {
            int maxScroll = Math.max(0, msgHistoryItems.size() - getHistoryVisibleLines());
            msgHistoryScroll = Math.clamp(msgHistoryScroll + (verticalAmount > 0 ? -1 : 1), 0, maxScroll);
            return true;
        }

        if (this.tab == Tab.CONFIGURATION && this.configurationTabController != null) {
            if (this.configurationTabController.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }

        if (this.tab == Tab.CANVAS && selected != null) {
            if (selected.type == MacroHudDataHandler.ElementType.LIST) {
                int sx = resolveElementX(selected);
                int sy = resolveElementY(selected);
                if (containsBox(mouseX, mouseY, sx, sy, selected.width, selected.height)) {
                    int delta = verticalAmount > 0 ? -1 : 1;
                    var window = this.client == null ? null : this.client.getWindow();
                    boolean shiftDown = window != null && (
                            InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT)
                    );
                    int step = shiftDown ? 5 : 1;
                    selected.listScroll = Math.max(0, selected.listScroll + (delta * step));
                    return true;
                }
            }
            return false;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (kbCommandsModalOpen) {
            int codepoint = input.codepoint();
            if (kbCommandsFocused && codepoint >= 32 && codepoint != 127) {
                insertAtKeyboardCommandsCursor(new String(Character.toChars(codepoint)));
            }
            return true;
        }
        if (advancedOpen) {
            int codepoint = input.codepoint();
            if (advancedTextFocused && codepoint >= 32 && codepoint != 127) {
                insertAtCursor(new String(Character.toChars(codepoint)));
            } else if (advancedActionFocused && codepoint >= 32 && codepoint != 127) {
                insertAtAdvancedActionCursor(new String(Character.toChars(codepoint)));
            } else if (advancedBgColorFocused && codepoint >= 32 && codepoint != 127) {
                insertAtAdvancedBgCursor(new String(Character.toChars(codepoint)));
            } else if (advancedBorderColorFocused && codepoint >= 32 && codepoint != 127) {
                insertAtAdvancedBorderCursor(new String(Character.toChars(codepoint)));
            } else if (advancedVisibilityScreenTypeFocused && codepoint >= 32 && codepoint != 127) {
                insertAtAdvancedVisibilityScreenTypeCursor(new String(Character.toChars(codepoint)));
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && keyCode == GLFW.GLFW_KEY_F1) {
            canvasChromeVisible = !canvasChromeVisible;
            refreshCanvasChromeVisibility();
            return true;
        }

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && keyCode == GLFW.GLFW_KEY_DELETE && selected != null) {
            Set<String> ids = selectedElementIds.isEmpty() ? Set.of(selected.id) : new HashSet<>(selectedElementIds);
            this.working.elements.removeIf(e -> ids.contains(e.id));
            selectedElementIds.clear();
            this.selected = null;
            syncCanvasFields();
            return true;
        }

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && !isAnyCanvasTextFieldFocused() && isCtrlDown()) {
            if (keyCode == GLFW.GLFW_KEY_Z) {
                undoLastMove();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                if (isShiftDown()) {
                    copySelectedDimensions();
                } else {
                    copySelectedElements();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                if (isShiftDown()) {
                    pasteDimensionsToSelection();
                } else {
                    pasteElementsFromClipboard();
                }
                return true;
            }
        }

        if (kbCommandsModalOpen) {
            if (kbCommandsFocused) {
                handleKeyboardCommandsKey(keyCode);
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeKeyboardCommandsModal();
            }
            return true;
        }
        if (advancedOpen) {
            if (advancedTextFocused) {
                handleAdvancedTextKey(keyCode);
            } else if (advancedActionFocused) {
                handleAdvancedActionKey(keyCode);
            } else if (advancedBgColorFocused) {
                handleAdvancedBgKey(keyCode);
            } else if (advancedBorderColorFocused) {
                handleAdvancedBorderKey(keyCode);
            } else if (advancedVisibilityScreenTypeFocused) {
                handleAdvancedVisibilityScreenTypeKey(keyCode);
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeAdvancedModal();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private boolean onCanvasClick(double mouseX, double mouseY) {
        if (mouseY < canvasTopBound()) {
            dragging = false;
            resizing = false;
            return false;
        }

        boolean ctrl = isCtrlDown();

        List<MacroHudDataHandler.HudElement> layeredElements = HudElementUtils.sortedByLayer(this.working.elements);
        for (int i = layeredElements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement e = layeredElements.get(i);
            int ex = resolveElementX(e);
            int ey = resolveElementY(e);
            if (contains(ex, ey, e, mouseX, mouseY)) {
                if (ctrl) {
                    this.selected = e;
                    if (!selectedElementIds.add(e.id)) {
                        selectedElementIds.remove(e.id);
                    }
                    dragging = false;
                    resizing = false;
                    syncCanvasFields();
                    return true;
                }

                this.selected = e;
                if (!selectedElementIds.contains(e.id)) {
                    selectedElementIds.clear();
                    selectedElementIds.add(e.id);
                }

                if (canResize(e, mouseX, mouseY)) {
                    this.resizing = true;
                    this.dragging = false;
                    this.resizeStartMouseX = (int) mouseX;
                    this.resizeStartMouseY = (int) mouseY;
                    this.resizeStartWidth = e.width;
                    this.resizeStartHeight = e.height;
                    this.activeMoveSnapshot = captureSnapshots(List.of(e));
                } else {
                    this.activeMoveSnapshot = captureSnapshots(getSelectedElements());
                    this.dragStartScreenPositions.clear();
                    for (MacroHudDataHandler.HudElement selectedElement : getSelectedElements()) {
                        this.dragStartScreenPositions.put(selectedElement.id, new int[]{resolveElementX(selectedElement), resolveElementY(selectedElement)});
                    }
                    this.dragPrimaryElementId = e.id;
                    this.dragOffsetX = (int) mouseX - ex;
                    this.dragOffsetY = (int) mouseY - ey;
                    this.dragging = true;
                    this.resizing = false;
                }
                syncCanvasFields();
                return true;
            }
        }

        this.selected = null;
        selectedElementIds.clear();
        this.resizing = false;
        this.dragging = false;
        syncCanvasFields();
        return false;
    }

    private boolean onKeyboardClick(double x, double y) {
        for (KeyCell cell : cells) {
            if (x >= cell.x && x <= cell.x + cell.w && y >= cell.y && y <= cell.y + cell.h) {
                selectedKey = cell.keyCode;
                selectedMacroId = null;
                syncKeyboardFields();
                return true;
            }
        }

        int panelX = keyboardPanelX();
        int panelRight = panelX + keyboardPanelWidth();
        int lineY = TOP_BAR_H + 76;
        for (String id : macroBindingIds.getOrDefault(selectedKey, List.of())) {
            if (x >= panelX + 8 && x <= panelRight - 8 && y >= lineY - 2 && y <= lineY + 10) {
                selectedMacroId = id;
                syncKeyboardFields();
                return true;
            }
            lineY += 13;
            if (lineY > this.height - 178) {
                break;
            }
        }

        return false;
    }

    private boolean onAdvancedMouseClick(Click click) {
        if (click.button() != 0 && click.button() != 1) {
            return true;
        }

        boolean forward = click.button() != 1;
        MacroWorkbenchAdvancedClickSupport.Ops clickOps = advancedClickSupportOps();
        int boxX = modalX();
        int boxY = modalY();

        if (colorPickerOpen && handleColorPickerClick(click)) {
            return true;
        }

        if (containsBox(click.x(), click.y(), boxX, boxY, MODAL_W, 20)) {
            advancedModalDragging = true;
            advancedModalDragOffsetX = (int) click.x() - boxX;
            advancedModalDragOffsetY = (int) click.y() - boxY;
            return true;
        }

        if (selected == null) {
            return true;
        }

        if (isCustomWidgetType(selected)) {
            return onCustomWidgetAdvancedClick(click, boxX, boxY);
        }

        if (isSecondaryChatProxy(selected)) {
            return onSecondaryAdvancedMouseClick(click, forward, boxX, boxY);
        }
        if (isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) {
            return onProxyAdvancedMouseClick(click, forward, boxX, boxY);
        }

        StandardAdvancedLayout layout = standardAdvancedLayout(boxX, boxY);

        if (containsBox(click.x(), click.y(), layout.bgToggle())) {
            selected.drawBackground = !selected.drawBackground;
            ensureVisibleBackground(selected);
            syncStyleButtons();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.bgOpaque())) {
            selected.drawBackground = true;
            selected.backgroundOpaque = false;
            adjustBackgroundAlpha(selected, 255 - Math.clamp(selected.backgroundAlpha, 0, 255));
            syncStyleButtons();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.borderToggle())) {
            cycleBorderSetting(selected, forward);
            syncStyleButtons();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.hAlign())) {
            selected.horizontalAlign = cycleHorizontalAlign(selected.horizontalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.vAlign())) {
            selected.verticalAlign = cycleVerticalAlign(selected.verticalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.anchor())) {
            int oldScreenX = resolveElementX(selected);
            int oldScreenY = resolveElementY(selected);
            selected.anchor = cycleAnchor(selected.anchor, forward);
            setElementScreenPosition(selected, oldScreenX, oldScreenY);
            clampElementToCanvas(selected);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.visibility())) {
            selected.visibilityMode = cycleVisibilityMode(selected.visibilityMode, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.execution())) {
            if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
                selected.buttonExecutionMode = cycleButtonExecutionMode(selected.buttonExecutionMode, true);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.asyncToggle())) {
            if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
                selected.runScriptsAsync = !selected.runScriptsAsync;
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.lineMinus())) {
            selected.lineHeight = Math.clamp(selected.lineHeight - stepInt(1), 6, 24);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.linePlus())) {
            selected.lineHeight = Math.clamp(selected.lineHeight + stepInt(1), 6, 24);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.fontMinus())) {
            selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.fontPlus())) {
            selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zMinus())) {
            selected.zIndex = Math.clamp(selected.zIndex - Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zPlus())) {
            selected.zIndex = Math.clamp(selected.zIndex + Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleBackgroundColorButtons(
                click,
                forward,
                layout.bgMinus(),
                layout.bgPlus(),
                layout.alphaMinus(),
                layout.alphaPlus(),
                selected,
                clickOps,
                () -> {
                    ensureVisibleBackground(selected);
                    advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
                    advancedBgCursor = advancedBgColor.length();
                })) {
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleColorCycleButtons(
                click,
                layout.borderMinus(),
                layout.borderPlus(),
                selected.borderColor,
                clickOps,
                color -> {
                    selected.borderColor = color;
                    advancedBorderColor = ColorUtils.formatColor(selected.borderColor);
                    advancedBorderCursor = advancedBorderColor.length();
                })) {
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleApplyCancel(
                click,
                layout.apply(),
                layout.cancel(),
                clickOps,
                this::applyAdvancedAndClose,
                this::closeAdvancedModal)) {
            return true;
        }

        advancedBgColorFocused = containsBox(click.x(), click.y(), layout.bgHex());
        if (advancedBgColorFocused) {
            if (!forward) {
                openColorPicker(true, layout.bgHex().right() + 8, layout.bgHex().y() - 6);
                return true;
            }
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBorderColorFocused = false;
            advancedVisibilityScreenTypeFocused = false;
            advancedBgSelectionAnchor = -1;
            int localX = (int) (click.x() - (layout.bgHex().x() + 4));
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
            return true;
        }

        advancedBorderColorFocused = containsBox(click.x(), click.y(), layout.borderHex());
        if (advancedBorderColorFocused) {
            if (!forward) {
                openColorPicker(false, layout.borderHex().right() + 8, layout.borderHex().y() - 6);
                return true;
            }
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedVisibilityScreenTypeFocused = false;
            advancedBorderSelectionAnchor = -1;
            int localX = (int) (click.x() - (layout.borderHex().x() + 4));
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
            return true;
        }

        advancedVisibilityScreenTypeFocused = selected.visibilityMode == MacroHudDataHandler.VisibilityMode.SCREEN
                && containsBox(click.x(), click.y(), layout.visibilityType());
        if (advancedVisibilityScreenTypeFocused) {
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedVisibilityScreenTypeSelectionAnchor = -1;
            int localX = (int) (click.x() - (layout.visibilityType().x() + 4));
            advancedVisibilityScreenTypeCursor = cursorIndexFromPoint(advancedVisibilityScreenType, localX, 0, 9);
            return true;
        }

        advancedActionFocused = containsBox(click.x(), click.y(), layout.actionField());
        if (advancedActionFocused) {
            advancedTextFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedVisibilityScreenTypeFocused = false;
            advancedActionSelectionAnchor = -1;
            int localX = (int) (click.x() - (layout.actionField().x() + 4));
            advancedActionCursor = cursorIndexFromPoint(advancedAction, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
            return true;
        }

        advancedTextFocused = containsBox(click.x(), click.y(), layout.textArea());
        if (advancedTextFocused) {
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedVisibilityScreenTypeFocused = false;
            advancedSelectionAnchor = -1;
            advancedCursor = cursorIndexFromPoint(
                    advancedText,
                    (int) (click.x() - (layout.textArea().x() + 4)),
                    (int) (click.y() - (layout.textArea().y() + 4)) + (advancedTextScrollLine * 9),
                    9
            );
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
        }
        return true;
    }

    private boolean onSecondaryAdvancedMouseClick(Click click, boolean forward, int boxX, int boxY) {
        SecondaryAdvancedLayout layout = secondaryAdvancedLayout(boxX, boxY);
        MacroWorkbenchAdvancedClickSupport.Ops clickOps = advancedClickSupportOps();
        if (containsBox(click.x(), click.y(), layout.guiOpen())) {
            advancedSecondaryShowWhileGuiOpen = !advancedSecondaryShowWhileGuiOpen;
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.fadeToggle())) {
            advancedSecondaryFadeEnabled = !advancedSecondaryFadeEnabled;
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.hoverReset())) {
            advancedSecondaryResetTransparencyOnHover = !advancedSecondaryResetTransparencyOnHover;
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.noTransparency())) {
            advancedSecondaryNoTransparencyWhenChatOpen = !advancedSecondaryNoTransparencyWhenChatOpen;
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.mode())) {
            advancedSecondaryInterceptMode = advancedSecondaryInterceptMode == SecondaryChatSettings.InterceptMode.COPY
                    ? SecondaryChatSettings.InterceptMode.MOVE
                    : SecondaryChatSettings.InterceptMode.COPY;
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.scaleMinus())) {
            advancedSecondaryScale = Math.max(0.1, advancedSecondaryScale - stepDouble(0.05));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.scalePlus())) {
            advancedSecondaryScale = Math.min(3.0, advancedSecondaryScale + stepDouble(0.05));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.lineMinus())) {
            advancedSecondaryLineHeight = Math.max(1, advancedSecondaryLineHeight - stepInt(1));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.linePlus())) {
            advancedSecondaryLineHeight = Math.min(30, advancedSecondaryLineHeight + stepInt(1));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zMinus())) {
            selected.zIndex = Math.clamp(selected.zIndex - Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zPlus())) {
            selected.zIndex = Math.clamp(selected.zIndex + Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.fadeMinus())) {
            advancedSecondaryFadeDurationMs = Math.max(1000, advancedSecondaryFadeDurationMs - stepInt(1000));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.fadePlus())) {
            advancedSecondaryFadeDurationMs = Math.min(120000, advancedSecondaryFadeDurationMs + stepInt(1000));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alphaMinus())) {
            advancedSecondaryMinAlpha = Math.max(0, advancedSecondaryMinAlpha - stepInt(5));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alphaPlus())) {
            advancedSecondaryMinAlpha = Math.min(255, advancedSecondaryMinAlpha + stepInt(5));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.linesMinus())) {
            advancedSecondaryMaxLines = Math.max(10, advancedSecondaryMaxLines - stepInt(10));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.linesPlus())) {
            advancedSecondaryMaxLines = Math.min(500, advancedSecondaryMaxLines + stepInt(10));
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleBackgroundColorButtons(
                click,
                forward,
                layout.bgMinus(),
                layout.bgPlus(),
                layout.bgAlphaMinus(),
                layout.bgAlphaPlus(),
                selected,
                clickOps,
                () -> {
                    advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
                    advancedBgCursor = advancedBgColor.length();
                })) {
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleApplyCancel(
                click,
                layout.apply(),
                layout.cancel(),
                clickOps,
                this::applyAdvancedAndClose,
                this::closeAdvancedModal)) {
            return true;
        }

        advancedBgColorFocused = containsBox(click.x(), click.y(), layout.bgHex());
        if (advancedBgColorFocused) {
            if (!forward) {
                openColorPicker(true, layout.bgHex().right() + 8, layout.bgHex().y() - 6);
                return true;
            }
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBorderColorFocused = false;
            advancedBgSelectionAnchor = -1;
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, (int) (click.x() - (layout.bgHex().x() + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
            return true;
        }

        advancedBorderColorFocused = containsBox(click.x(), click.y(), layout.txHex());
        if (advancedBorderColorFocused) {
            if (!forward) {
                openColorPicker(false, layout.txHex().right() + 8, layout.txHex().y() - 6);
                return true;
            }
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderSelectionAnchor = -1;
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, (int) (click.x() - (layout.txHex().x() + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
            return true;
        }

        advancedActionFocused = containsBox(click.x(), click.y(), layout.outgoingInput());
        if (advancedActionFocused) {
            advancedTextFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedActionSelectionAnchor = -1;
            advancedActionCursor = cursorIndexFromPoint(advancedAction, (int) (click.x() - (layout.outgoingInput().x() + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
            return true;
        }

        advancedTextFocused = containsBox(click.x(), click.y(), layout.regexInput());
        if (advancedTextFocused) {
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedSelectionAnchor = -1;
            advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (layout.regexInput().x() + 4)), (int) (click.y() - (layout.regexInput().y() + 4)), 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
        }
        return true;
    }

    private boolean onProxyAdvancedMouseClick(Click click, boolean forward, int boxX, int boxY) {
        ProxyAdvancedLayout layout = proxyAdvancedLayout(boxX, boxY, isPickupNotifierProxy(selected));
        MacroWorkbenchAdvancedClickSupport.Ops clickOps = advancedClickSupportOps();
        if (containsBox(click.x(), click.y(), layout.scaleMinus())) {
            selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.scalePlus())) {
            selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.lineMinus())) {
            selected.lineHeight = Math.clamp(selected.lineHeight - stepInt(1), 6, 24);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.linePlus())) {
            selected.lineHeight = Math.clamp(selected.lineHeight + stepInt(1), 6, 24);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.toggleBg())) {
            if (forward) {
                selected.drawBackground = !selected.drawBackground;
            } else {
                selected.drawBackground = true;
                selected.backgroundOpaque = false;
                adjustBackgroundAlpha(selected, 255 - Math.clamp(selected.backgroundAlpha, 0, 255));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.toggleBorder())) {
            cycleBorderSetting(selected, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.toggleVisible())) {
            selected.visible = !selected.visible;
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleBackgroundColorButtons(
                click,
                forward,
                layout.colorBgMinus(),
                layout.colorBgPlus(),
                layout.colorAlphaMinus(),
                layout.colorAlphaPlus(),
                selected,
                clickOps,
                () -> {
                    advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
                    advancedBgCursor = advancedBgColor.length();
                })) {
            return true;
        }
        if (MacroWorkbenchAdvancedClickSupport.handleColorCycleButtons(
                click,
                layout.colorTxMinus(),
                layout.colorTxPlus(),
                selected.textColor,
                clickOps,
                color -> {
                    selected.textColor = color;
                    advancedBorderColor = ColorUtils.formatColor(selected.textColor);
                    advancedBorderCursor = advancedBorderColor.length();
                })) {
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alignH())) {
            selected.horizontalAlign = cycleHorizontalAlign(selected.horizontalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alignV())) {
            selected.verticalAlign = cycleVerticalAlign(selected.verticalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.anchor())) {
            int oldScreenX = resolveElementX(selected);
            int oldScreenY = resolveElementY(selected);
            selected.anchor = cycleAnchor(selected.anchor, forward);
            setElementScreenPosition(selected, oldScreenX, oldScreenY);
            clampElementToCanvas(selected);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zMinus())) {
            selected.zIndex = Math.clamp(selected.zIndex - Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.zPlus())) {
            selected.zIndex = Math.clamp(selected.zIndex + Math.max(1, stepInt(1)), -9999, 9999);
            return true;
        }
        if (isPickupNotifierProxy(selected)) {
            int direction = forward ? 1 : -1;
            if (containsBox(click.x(), click.y(), layout.pickupDuration())) {
                PickupFeedSettings.updateAndSave(() -> PickupFeedSettings.get().durationMs += direction * (isShiftDown() ? 250 : 500));
                return true;
            }
            if (containsBox(click.x(), click.y(), layout.pickupLines())) {
                PickupFeedSettings.updateAndSave(() -> PickupFeedSettings.get().maxLines += direction * (isShiftDown() ? 1 : 2));
                return true;
            }
            if (containsBox(click.x(), click.y(), layout.pickupIcon())) {
                PickupFeedSettings.updateAndSave(() -> PickupFeedSettings.get().iconScale += direction * (isShiftDown() ? 0.05f : 0.1f));
                return true;
            }
            if (containsBox(click.x(), click.y(), layout.pickupDirection())) {
                PickupFeedSettings.updateAndSave(() -> {
                    PickupFeedSettings.Direction current = PickupFeedSettings.get().direction;
                    PickupFeedSettings.get().direction = (current == PickupFeedSettings.Direction.UP)
                            ? PickupFeedSettings.Direction.DOWN
                            : PickupFeedSettings.Direction.UP;
                });
                return true;
            }
        }
        if (containsBox(click.x(), click.y(), layout.bgInput())) {
            if (!forward) {
                openColorPicker(true, layout.bgInput().right() + 8, layout.bgInput().y() - 6);
                return true;
            }
            advancedBgColorFocused = true;
            advancedBorderColorFocused = false;
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgSelectionAnchor = -1;
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, (int) (click.x() - (layout.bgInput().x() + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.txInput())) {
            if (!forward) {
                openColorPicker(false, layout.txInput().right() + 8, layout.txInput().y() - 6);
                return true;
            }
            advancedBgColorFocused = false;
            advancedBorderColorFocused = true;
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBorderSelectionAnchor = -1;
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, (int) (click.x() - (layout.txInput().x() + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
            return true;
        }
        MacroWorkbenchAdvancedClickSupport.handleApplyCancel(
                click,
                layout.apply(),
                layout.cancel(),
                clickOps,
                this::applyAdvancedAndClose,
                this::closeAdvancedModal);
        return true;
    }

    private void updateDragging(int mouseX, int mouseY) {
        if ((!dragging && !resizing) || selected == null || this.client == null) {
            return;
        }

        long window = this.client.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            if (activeMoveSnapshot != null && hasSnapshotChanges(activeMoveSnapshot)) {
                moveUndoStack.push(activeMoveSnapshot);
                while (moveUndoStack.size() > 25) {
                    moveUndoStack.removeLast();
                }
            }
            activeMoveSnapshot = null;
            dragStartScreenPositions.clear();
            dragPrimaryElementId = null;
            dragging = false;
            resizing = false;
            snapGuideX = Integer.MIN_VALUE;
            snapGuideY = Integer.MIN_VALUE;
            return;
        }

        if (resizing) {
            int nextWidth = Math.clamp(resizeStartWidth + (mouseX - resizeStartMouseX), 1, 2000);
            int nextHeight = Math.clamp(resizeStartHeight + (mouseY - resizeStartMouseY), 1, 1200);
            int baseX = resolveElementX(selected);
            int baseY = resolveElementY(selected);
            if (isShiftDown()) {
                int[] snapped = snapResizeToNeighbors(selected, baseX, baseY, nextWidth, nextHeight);
                nextWidth = snapped[0];
                nextHeight = snapped[1];
                snapGuideX = snapped[2];
                snapGuideY = snapped[3];
            } else {
                snapGuideX = Integer.MIN_VALUE;
                snapGuideY = Integer.MIN_VALUE;
            }
            selected.width = nextWidth;
            selected.height = nextHeight;

            int clampedX = Math.clamp(resolveElementX(selected), 0, Math.max(0, this.width - selected.width));
            int clampedY = Math.clamp(resolveElementY(selected), canvasTopBound(),
                    Math.max(canvasTopBound(), canvasBottomBound() - selected.height));
            setElementScreenPosition(selected, clampedX, clampedY);
            return;
        }

        int maxX = this.width - selected.width;
        int maxY = canvasBottomBound() - selected.height;
        int screenX = Math.clamp(mouseX - dragOffsetX, 0, Math.max(0, maxX));
        int screenY = Math.clamp(mouseY - dragOffsetY, canvasTopBound(), Math.max(canvasTopBound(), maxY));

        if (gridEnabled) {
            int[] snapped = snapToGrid(screenX, screenY);
            screenX = snapped[0];
            screenY = snapped[1];
            if (selectedElementIds.size() <= 1) {
                selected.width = snapped[2];
                selected.height = snapped[3];
            }
        }

        if (isShiftDown()) {
            MacroHudDataHandler.HudElement snapTarget = dragPrimaryElementId == null ? selected : findElementById(dragPrimaryElementId);
            if (snapTarget == null) {
                snapTarget = selected;
            }
            Set<String> excludedIds = selectedElementIds.size() > 1 ? new HashSet<>(selectedElementIds) : Set.of();
            int[] snappedToElements = snapElementToNeighbors(snapTarget, screenX, screenY, excludedIds);
            screenX = snappedToElements[0];
            screenY = snappedToElements[1];
        } else {
            snapGuideX = Integer.MIN_VALUE;
            snapGuideY = Integer.MIN_VALUE;
        }

        if (selectedElementIds.size() <= 1 || dragPrimaryElementId == null || !dragStartScreenPositions.containsKey(dragPrimaryElementId)) {
            setElementScreenPosition(selected, screenX, screenY);
            return;
        }

        int[] primaryStart = dragStartScreenPositions.get(dragPrimaryElementId);
        int dx = screenX - primaryStart[0];
        int dy = screenY - primaryStart[1];

        int boundedDx = dx;
        int boundedDy = dy;
        int canvasBottom = canvasBottomBound();
        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            int[] start = dragStartScreenPositions.get(e.id);
            if (start == null) {
                continue;
            }
            int minDx = -start[0];
            int maxDx = this.width - e.width - start[0];
            int minDy = canvasTopBound() - start[1];
            int maxDy = canvasBottom - e.height - start[1];
            boundedDx = Math.clamp(boundedDx, minDx, maxDx);
            boundedDy = Math.clamp(boundedDy, minDy, maxDy);
        }

        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            int[] start = dragStartScreenPositions.get(e.id);
            if (start == null) {
                continue;
            }
            setElementScreenPosition(e, start[0] + boundedDx, start[1] + boundedDy);
        }
    }

    private int[] snapToGrid(int x, int y) {
        int canvasW = this.width;
        int canvasH = canvasHeightBound();
        int cols = Math.max(1, gridCols);
        int rows = Math.max(1, gridRows);
        double cellW = canvasW / (double) cols;
        double cellH = canvasH / (double) rows;

        int col = Math.clamp((int) Math.round(x / cellW), 0, cols - 1);
        int row = Math.clamp((int) Math.round(y / cellH), 0, rows - 1);

        int snappedX = Math.clamp((int) Math.round(col * cellW), 0, Math.max(0, canvasW - 1));
        int snappedY = Math.clamp((int) Math.round(row * cellH), 0, Math.max(0, canvasH - 1));
        int snappedW = Math.clamp((int) Math.round(cellW), 1, 2000);
        int snappedH = Math.clamp((int) Math.round(cellH), 1, 1200);

        snappedX = Math.clamp(snappedX, 0, Math.max(0, canvasW - snappedW));
        snappedY = Math.clamp(snappedY, 0, Math.max(0, canvasH - snappedH));
        return new int[]{snappedX, snappedY + canvasTopBound(), snappedW, snappedH};
    }

    private int[] snapElementToNeighbors(MacroHudDataHandler.HudElement moving, int screenX, int screenY, Set<String> excludedIds) {
        int[] snapped = MacroWorkbenchSnapHelper.snapElementToNeighbors(
                moving,
                screenX,
                screenY,
                excludedIds,
                this.working.elements,
                this::resolveElementX,
                this::resolveElementY,
                this.width,
                canvasTopBound(),
                canvasBottomBound()
        );
        snapGuideX = snapped[2];
        snapGuideY = snapped[3];
        return new int[]{snapped[0], snapped[1]};
    }

    private int[] snapResizeToNeighbors(MacroHudDataHandler.HudElement resizingElement, int baseX, int baseY, int width, int height) {
        return MacroWorkbenchSnapHelper.snapResizeToNeighbors(
                resizingElement,
                baseX,
                baseY,
                width,
                height,
                this.working.elements,
                this::resolveElementX,
                this::resolveElementY,
                this.width,
                canvasBottomBound()
        );
    }

    private void drawSnapGuides(DrawContext context) {
        if (!(dragging && isShiftDown()) && !(resizing && isShiftDown())) {
            return;
        }
        int canvasBottom = Math.max(canvasTopBound() + 1, canvasBottomBound());
        if (snapGuideX != Integer.MIN_VALUE) {
            context.fill(snapGuideX, canvasTopBound(), snapGuideX + 1, canvasBottom, 0x90FFD75A);
        }
        if (snapGuideY != Integer.MIN_VALUE) {
            context.fill(0, snapGuideY, this.width, snapGuideY + 1, 0x90FFD75A);
        }
    }

    private void drawGridOverlay(DrawContext context) {
        int canvasTop = canvasTopBound();
        int canvasBottom = Math.max(canvasTop + 1, canvasBottomBound());
        int canvasRight = this.width;

        for (int c = 1; c < Math.max(1, gridCols); c++) {
            int x = (int) Math.round((c * canvasRight) / (double) Math.max(1, gridCols));
            context.fill(x, canvasTop, x + 1, canvasBottom, 0x45FFFFFF);
        }
        for (int r = 1; r < Math.max(1, gridRows); r++) {
            int y = canvasTop + (int) Math.round((r * (canvasBottom - canvasTop)) / (double) Math.max(1, gridRows));
            context.fill(0, y, canvasRight, y + 1, 0x35FFFFFF);
        }
    }

    private void drawCenterLinesOverlay(DrawContext context) {
        int canvasTop = canvasTopBound();
        int canvasBottom = Math.max(canvasTop + 1, canvasBottomBound());
        int centerX = this.width / 2;
        int centerY = canvasTop + (canvasBottom - canvasTop) / 2;
        context.fill(centerX, canvasTop, centerX + 1, canvasBottom, 0x80FFCC66);
        context.fill(0, centerY, this.width, centerY + 1, 0x80FFCC66);
    }

    private void drawCanvasElement(DrawContext context, MacroHudDataHandler.HudElement element) {
        int x1 = resolveElementX(element);
        int y1 = resolveElementY(element);
        int x2 = x1 + element.width;
        int y2 = y1 + element.height;

        boolean customWidget = element.type == MacroHudDataHandler.ElementType.ICON
                || element.type == MacroHudDataHandler.ElementType.BAR
                || element.type == MacroHudDataHandler.ElementType.VALUE
                || element.type == MacroHudDataHandler.ElementType.LIST
                || element.type == MacroHudDataHandler.ElementType.INVENTORY
                || element.type == MacroHudDataHandler.ElementType.SHAPE
                || element.type == MacroHudDataHandler.ElementType.STATE_BADGE;

        if (customWidget) {
            drawCanvasSpecialWidget(context, element, x1, y1, x2, y2);
            drawSelectedOutline(context, x1, y1, x2, y2, selectedElementIds.contains(element.id));
            return;
        }

        List<String> lines;
        if (isSecondaryChatProxy(element)) {
            lines = buildExternalProxyPreviewLines(element, "Secondary Chat");
        } else if (isMacroKeybindProxy(element)) {
            lines = buildExternalProxyPreviewLines(element, "Macro Keybinds");
        } else if (isPickupNotifierProxy(element)) {
            lines = buildExternalProxyPreviewLines(element, "Pick-up Notifier");
        } else if (isNbtInspectorProxy(element)) {
            lines = buildExternalProxyPreviewLines(element, "NBT Inspector");
        } else if (element.type == MacroHudDataHandler.ElementType.MACRO_KEYBINDS) {
            lines = buildMacroKeybindPreviewLines(element);
        } else {
            lines = splitLines(expandForCanvas(element.type == MacroHudDataHandler.ElementType.TEXT ? element.text : element.label));
        }

        if (element.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(element));
        }
        if (element.drawBorder) {
            drawCanvasElementBorder(context, element, x1, y1, x2, y2);
        }

        float scale = Math.clamp(element.fontScale, 0.5f, 4.0f);
        int lineHeight = Math.max(6, Math.round(Math.max(6, element.lineHeight) * scale));
        int totalHeight = Math.max(8, lines.size() * lineHeight);
        int startY = alignedStartY(element, totalHeight, element.drawBackground);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = Math.max(1, Math.round(styledLineWidth(line) * scale));
            int x = alignedStartX(x1, element, textWidth, element.drawBackground);
            int y = startY + i * lineHeight;
            drawStyledTextLine(context, line, x, y, element.textColor, scale);
        }

        drawSelectedOutline(context, x1, y1, x2, y2, selectedElementIds.contains(element.id));
    }

    private void drawCanvasSpecialWidget(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        switch (e.type) {
            case ICON -> drawCanvasIconPreview(context, e, x1, y1, x2, y2);
            case BAR -> drawCanvasBarPreview(context, e, x1, y1, x2, y2);
            case VALUE -> drawCanvasValuePreview(context, e, x1, y1, x2, y2);
            case LIST -> drawCanvasListPreview(context, e, x1, y1, x2, y2);
            case INVENTORY -> drawCanvasInventoryPreview(context, e, x1, y1, x2, y2);
            case SHAPE -> drawCanvasShapePreview(context, e, x1, y1, x2, y2);
            case STATE_BADGE -> drawCanvasStateBadgePreview(context, e, x1, y1, x2, y2);
            default -> {
                // no-op
            }
        }
    }

    private void drawCanvasIconPreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        if ("entity_model".equalsIgnoreCase(StringUtils.safe(e.iconKind))) {
            MacroWorkbenchEntityModelRenderer.draw(context, this.client, e, x1, y1, e.width, e.height);
        } else {
            ItemStack stack = resolvePreviewIconStack(e.iconKind, e.iconId);
            int ix = x1 + Math.max(0, (e.width - 16) / 2);
            int iy = y1 + Math.max(0, (e.height - 16) / 2);
            context.drawItem(stack, ix, iy);
        }
        String label = StringUtils.safe(e.label);
        if (!label.isBlank()) {
            float scale = Math.max(0.5f, e.fontScale);
            int textW = Math.max(1, Math.round(styledLineWidth(label) * scale));
            int tx = alignedStartX(x1, e, textW, true);
            int ty = alignedStartY(e, Math.max(9, Math.round(9 * scale)), true);
            drawStyledTextLine(context, label, tx, ty, e.textColor, scale);
        }
    }

    private void drawCanvasBarPreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        double min = e.minValue;
        double max = e.maxValue;
        Double maxToken = resolveCanvasNumericToken(e.sourceTokenMax);
        if (maxToken != null) {
            max = maxToken;
        }
        Double valueToken = resolveCanvasNumericToken(e.sourceToken);
        double value = valueToken == null ? min : valueToken;
        if (max <= min) {
            max = min + 1.0;
        }
        float progress = (float) Math.clamp((value - min) / (max - min), 0.0, 1.0);
        int innerX = x1 + 1;
        int innerY = y1 + 1;
        int innerW = Math.max(1, e.width - 2);
        int innerH = Math.max(1, e.height - 2);
        int fillW = Math.round(innerW * progress);
        for (int px = 0; px < fillW; px++) {
            float t = innerW <= 1 ? progress : (px / (float) (innerW - 1));
            int c = blendColor(e.colorStart, e.colorEnd, t);
            context.fill(innerX + px, innerY, innerX + px + 1, innerY + innerH, c);
        }
    }

    private void drawCanvasValuePreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        Double valueToken = resolveCanvasNumericToken(e.sourceToken);
        double value = valueToken == null ? 0.0 : valueToken;
        String prefix = StringUtils.preserve(e.prefix);
        if (prefix.isBlank()) {
            String label = StringUtils.safe(e.label);
            if (!label.isBlank() && !"Value".equalsIgnoreCase(label)) {
                prefix = label + ": ";
            }
        }
        String text = prefix + MacroWorkbenchCanvasUtils.formatValue(value) + StringUtils.preserve(e.suffix);
        float scale = Math.max(0.5f, e.fontScale);
        int tw = Math.max(1, Math.round(styledLineWidth(text) * scale));
        int tx = alignedStartX(x1, e, tw, true);
        int ty = alignedStartY(e, Math.max(9, Math.round(9 * scale)), true);
        drawStyledTextLine(context, text, tx, ty, e.textColor, scale);
    }

    private void drawCanvasListPreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        String src = MacroPlaceholders.expandForCanvas(this.client, "{" + StringUtils.safe(e.sourceToken) + "}");
        List<String> lines = MacroWorkbenchCanvasUtils.splitListSource(src);
        if (lines.isEmpty()) {
            lines = List.of("(none)");
        }
        int maxLines = Math.max(1, e.maxLines);
        int scroll = Math.clamp(e.listScroll, 0, Math.max(0, lines.size() - 1));
        int end = Math.min(lines.size(), scroll + maxLines);
        List<String> visible = lines.subList(scroll, end);
        int startY = y1 + 3;
        int lineHeight = Math.max(8, Math.round(Math.max(6, e.lineHeight) * Math.max(0.5f, e.fontScale)));
        for (int i = 0; i < visible.size(); i++) {
            int yy = startY + i * lineHeight;
            if (yy > y2 - 10) {
                break;
            }
            drawStyledTextLine(context, visible.get(i), x1 + 4, yy, e.textColor, Math.max(0.5f, e.fontScale));
        }
    }

    private void drawCanvasInventoryPreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        if (this.client == null || this.client.player == null) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>();
        int cols = 9;
        int rows = 1;
        int selectedSlot = -1;
        MacroHudDataHandler.InventoryDisplayMode mode = e.inventoryDisplayMode == null
                ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR
                : e.inventoryDisplayMode;
        switch (mode) {
            case HOTBAR -> {
                cols = 9;
                rows = 1;
                for (int i = 0; i < 9; i++) {
                    stacks.add(this.client.player.getInventory().getStack(i));
                }
                selectedSlot = this.client.player.getInventory().getSelectedSlot();
            }
            case INVENTORY -> {
                cols = 9;
                rows = 3;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int index = 9 + row * 9 + col;
                        stacks.add(this.client.player.getInventory().getStack(index));
                    }
                }
            }
            case ARMOR -> {
                cols = 1;
                rows = 5;
                stacks.add(this.client.player.getEquippedStack(EquipmentSlot.HEAD));
                stacks.add(this.client.player.getEquippedStack(EquipmentSlot.CHEST));
                stacks.add(this.client.player.getEquippedStack(EquipmentSlot.LEGS));
                stacks.add(this.client.player.getEquippedStack(EquipmentSlot.FEET));
                stacks.add(this.client.player.getOffHandStack());
            }
        }

        int padding = 2;
        int gap = 2;
        int contentW = Math.max(1, e.width - padding * 2);
        int contentH = Math.max(1, e.height - padding * 2);
        int cellW = Math.max(1, (contentW - gap * Math.max(0, cols - 1)) / Math.max(1, cols));
        int cellH = Math.max(1, (contentH - gap * Math.max(0, rows - 1)) / Math.max(1, rows));
        int cell = Math.max(1, Math.min(cellW, cellH));
        int gridW = cols * cell + gap * Math.max(0, cols - 1);
        int gridH = rows * cell + gap * Math.max(0, rows - 1);
        int startX = x1 + padding + Math.max(0, (contentW - gridW) / 2);
        int startY = y1 + padding + Math.max(0, (contentH - gridH) / 2);
        boolean showCount = inventoryCountVisible(e);

        for (int i = 0; i < stacks.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int slotX = startX + col * (cell + gap);
            int slotY = startY + row * (cell + gap);
            boolean selectedSlotCell = mode == MacroHudDataHandler.InventoryDisplayMode.HOTBAR && i == selectedSlot;
            drawCanvasInventorySlot(context, slotX, slotY, cell, stacks.get(i), selectedSlotCell, showCount);
        }
    }

    private boolean inventoryCountVisible(MacroHudDataHandler.HudElement element) {
        return element.inventoryShowCount == null || element.inventoryShowCount;
    }

    private void drawCanvasInventorySlot(DrawContext context,
                                         int x,
                                         int y,
                                         int size,
                                         ItemStack stack,
                                         boolean selectedSlot,
                                         boolean showCount) {
        int bg = selectedSlot ? 0xA0785A20 : 0xAA1A1A1A;
        int edge = Math.max(1, Math.round(size / 18.0f));
        context.fill(x, y, x + size, y + size, bg);
        context.fill(x, y, x + size, y + edge, 0x50FFFFFF);
        context.fill(x, y + size - edge, x + size, y + size, 0x50303030);
        context.fill(x, y, x + edge, y + size, 0x50FFFFFF);
        context.fill(x + size - edge, y, x + size, y + size, 0x50303030);

        if (stack.isEmpty()) {
            return;
        }

        int inner = Math.max(1, size - edge * 2);
        float scale = inner / 16.0f;
        int ix = x + Math.max(0, (size - inner) / 2);
        int iy = y + Math.max(0, (size - inner) / 2);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(ix, iy);
        context.getMatrices().scale(scale, scale);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();

        if (showCount && stack.getCount() > 1) {
            drawScaledInventoryCount(context, stack.getCount(), x, y, size, edge);
        }
    }

    private void drawScaledInventoryCount(DrawContext context, int count, int x, int y, int size, int padding) {
        String text = Integer.toString(count);
        float scale = Math.clamp(size / 18.0f, 0.35f, 4.0f);
        int textWidth = Math.max(1, Math.round(this.textRenderer.getWidth(text) * scale));
        int textHeight = Math.max(1, Math.round(9 * scale));
        int screenX = x + Math.max(padding, size - textWidth - padding);
        int screenY = y + Math.max(padding, size - textHeight - padding);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawTextWithShadow(this.textRenderer, text, Math.round(screenX / scale), Math.round(screenY / scale), 0xFFFFFFFF);
        context.getMatrices().popMatrix();
    }

    private void drawCanvasShapePreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        String type = StringUtils.safe(e.shapeType).toLowerCase(Locale.ROOT);
        int w = Math.max(1, x2 - x1);
        int h = Math.max(1, y2 - y1);
        switch (type) {
            case "triangle" -> {
                drawCanvasTriangleShape(context, x1, y1, w, h,
                        e.backgroundColor, e.borderColor,
                        e.shapeFilled || e.drawBackground,
                        e.drawBorder,
                        Math.max(1, e.shapeThickness));
                return;
            }
            case "diamond" -> {
                drawCanvasDiamondShape(context, x1, y1, w, h,
                        e.backgroundColor, e.borderColor,
                        e.shapeFilled || e.drawBackground,
                        e.drawBorder,
                        Math.max(1, e.shapeThickness));
                return;
            }
            case "line", "cross" -> {
                if (!e.drawBorder && !e.shapeFilled) {
                    return;
                }
                int color = e.drawBorder ? e.borderColor : e.backgroundColor;
                if ("line".equals(type)) {
                    for (int i = 0; i < Math.max(1, e.shapeThickness); i++) {
                        int yy = y2 - 1 - i;
                        int xx = x1 + i;
                        context.fill(xx, yy, xx + w - i * 2, yy + 1, color);
                    }
                } else {
                    int cx = x1 + w / 2;
                    int cy = y1 + h / 2;
                    int t = Math.max(1, e.shapeThickness);
                    context.fill(cx - t / 2, y1, cx - t / 2 + t, y2, color);
                    context.fill(x1, cy - t / 2, x2, cy - t / 2 + t, color);
                }
                return;
            }
        }
        if (e.drawBackground || e.shapeFilled) {
            if ("triangle".equals(type)) {
                for (int row = 0; row < h; row++) {
                    float t = h <= 1 ? 1.0f : (row / (float) (h - 1));
                    int halfW = Math.max(0, Math.round((w / 2f) * t));
                    int cx = x1 + w / 2;
                    int yy = y1 + row;
                    context.fill(cx - halfW, yy, cx + halfW + 1, yy + 1, e.backgroundColor);
                }
            } else if ("diamond".equals(type)) {
                int cy = y1 + h / 2;
                int ry = Math.max(1, h / 2);
                int cx = x1 + w / 2;
                for (int row = -ry; row <= ry; row++) {
                    float t = 1.0f - (Math.abs(row) / (float) ry);
                    int halfW = Math.max(0, Math.round((w / 2f) * t));
                    int yy = cy + row;
                    context.fill(cx - halfW, yy, cx + halfW + 1, yy + 1, e.backgroundColor);
                }
            } else {
                context.fill(x1, y1, x2, y2, e.backgroundColor);
            }
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
    }

    private void drawCanvasStateBadgePreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        boolean on = true;
        int bg = on ? e.colorStart : e.colorEnd;
        context.fill(x1, y1, x2, y2, bg);
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        String baseLabel = StringUtils.safe(e.label).isBlank() ? "State" : e.label;
        String label = e.stateShowValue ? (baseLabel + ": " + (on ? StringUtils.safe(e.stateOnText) : StringUtils.safe(e.stateOffText))) : baseLabel;
        float scale = Math.clamp(e.fontScale, 0.5f, 4.0f);
        int textWidth = Math.max(1, Math.round(styledLineWidth(label) * scale));
        int tx = alignedStartX(x1, e, textWidth, true);
        int ty = alignedStartY(e, Math.max(9, Math.round(Math.max(6, e.lineHeight) * scale)), true);
        drawStyledTextLine(context, label, tx, ty, e.textColor, scale);
    }

    private void drawSelectedOutline(DrawContext context, int x1, int y1, int x2, int y2, boolean isSelected) {
        if (!isSelected) {
            return;
        }
        context.fill(x1, y1, x2, y1 + 1, 0xFFFFFF00);
        context.fill(x1, y2 - 1, x2, y2, 0xFFFFFF00);
        context.fill(x1, y1, x1 + 1, y2, 0xFFFFFF00);
        context.fill(x2 - 1, y1, x2, y2, 0xFFFFFF00);
        int handleSize = 6;
        int hx = x2 - handleSize;
        int hy = y2 - handleSize;
        int handleColor = resizing ? 0xFFFFC36B : 0xFFC0C0C0;
        context.fill(hx, hy, x2, y2, handleColor);
    }

    private void syncCanvasFields() {
        if (quickField == null || macroField == null || actionField == null) {
            return;
        }
        if (deleteButton != null) {
            deleteButton.active = selected != null;
        }

        if (selected == null) {
            selectedElementIds.clear();
            quickField.setEditable(false);
            quickField.setText("");
            macroField.setEditable(false);
            macroField.setText("");
            macroField.visible = false;
            macroField.active = false;
            actionField.setEditable(false);
            actionField.setText("");
            actionField.visible = false;
            actionField.active = false;
            syncStyleButtons();
            return;
        }

        if (isExternalCanvasProxy(selected)) {
            ExternalProxyRenderState externalState = getExternalProxyRenderState(selected);
            quickField.setEditable(false);
            String proxyName = isSecondaryChatProxy(selected)
                    ? "Secondary Chat"
                    : (isMacroKeybindProxy(selected)
                       ? "Macro Keybinds"
                       : (isPickupNotifierProxy(selected) ? "Pick-up Notifier" : "NBT Inspector"));
            quickField.setText(proxyName + " (edit style/pos/size)" + (externalState == ExternalProxyRenderState.MODULE_DISABLED ? " [DISABLED]" : ""));
            macroField.setEditable(false);
            macroField.setText("");
            macroField.visible = false;
            macroField.active = false;
            actionField.setEditable(false);
            actionField.setText("");
            actionField.visible = false;
            actionField.active = false;
            syncStyleButtons();
            return;
        }

        quickField.setEditable(true);
        String raw = selected.type == MacroHudDataHandler.ElementType.TEXT ? selected.text : selected.label;
        quickField.setText(firstLine(raw));

        boolean button = selected.type == MacroHudDataHandler.ElementType.BUTTON;
        macroField.setEditable(button);
        macroField.visible = button;
        macroField.active = button;
        macroField.setText(selected.macroId == null ? "" : selected.macroId);
        actionField.setEditable(button);
        actionField.visible = button;
        actionField.active = button;
        actionField.setText(selected.buttonAction == null ? "" : selected.buttonAction);

        syncStyleButtons();
    }

    private void applyQuickEdit() {
        if (selected == null || quickField == null || macroField == null || actionField == null) {
            return;
        }
        if (isExternalCanvasProxy(selected)) {
            return;
        }

        String raw = selected.type == MacroHudDataHandler.ElementType.TEXT ? selected.text : selected.label;
        List<String> lines = new ArrayList<>(splitLinesRaw(raw));
        if (lines.isEmpty()) {
            lines = new ArrayList<>(List.of(""));
        }
        lines.set(0, quickField.getText() == null ? "" : quickField.getText());
        String merged = String.join("\n", lines);

        if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            selected.label = merged;
            selected.macroId = macroField.getText() == null ? "" : macroField.getText().trim();
            String compactAction = actionField.getText() == null ? "" : actionField.getText();
            String currentAction = selected.buttonAction == null ? "" : selected.buttonAction;
            boolean multilineAction = currentAction.indexOf('\n') >= 0 || currentAction.indexOf('\r') >= 0;
            // Do not clobber multiline script bodies from the single-line quick action field.
            if (!multilineAction || actionField.isFocused()) {
                selected.buttonAction = compactAction;
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.TEXT) {
            selected.text = merged;
        } else {
            selected.label = merged;
        }
    }

    private void syncStyleButtons() {
        if (backgroundToggle == null || borderToggle == null) {
            return;
        }

        if (selected == null) {
            backgroundToggle.active = false;
            borderToggle.active = false;
            backgroundToggle.setMessage(Text.literal("BG: OFF"));
            borderToggle.setMessage(Text.literal("Border: OFF"));
            return;
        }

        backgroundToggle.active = true;
        borderToggle.active = true;
        backgroundToggle.setMessage(Text.literal(backgroundLabel(selected)));
        borderToggle.setMessage(Text.literal(borderModeLabel(selected)));
    }

    private void syncGridButtons() {
        if (gridToggleButton == null || gridRowsMinusButton == null || gridRowsPlusButton == null
                || gridColsMinusButton == null || gridColsPlusButton == null || centerLinesToggleButton == null) {
            return;
        }
        gridToggleButton.setMessage(Text.literal(gridEnabled ? "Grid: ON" : "Grid: OFF"));
        gridRowsMinusButton.setMessage(Text.literal("R-"));
        gridRowsPlusButton.setMessage(Text.literal("R+"));
        gridColsMinusButton.setMessage(Text.literal("C-"));
        gridColsPlusButton.setMessage(Text.literal("C+"));
        centerLinesToggleButton.setMessage(Text.literal(centerLinesEnabled ? "Center: ON" : "Center: OFF"));
    }

    private void syncPresetControls() {
        if (presetNameField == null || presetDeleteButton == null || presetPrevButton == null || presetNextButton == null) {
            return;
        }
        String active = this.working == null ? "default" : StringUtils.safe(this.working.activePresetId);
        presetNameField.setText(active);
        List<String> ids = MacroHudDataHandler.listPresetIds();
        int idx = ids.indexOf(active);
        if (idx < 0) {
            idx = 0;
        }
        presetDeleteButton.active = ids.size() > 1;
        presetPrevButton.active = ids.size() > 1;
        presetNextButton.active = ids.size() > 1;
    }

    private void cyclePreset(boolean forward) {
        if (this.working == null) {
            return;
        }
        saveAll();
        List<String> ids = MacroHudDataHandler.listPresetIds();
        if (ids.isEmpty()) {
            return;
        }
        String active = MacroHudDataHandler.getActivePresetId();
        int idx = ids.indexOf(active);
        if (idx < 0) {
            idx = 0;
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0) {
            next = ids.size() - 1;
        }
        if (next >= ids.size()) {
            next = 0;
        }
        MacroHudDataHandler.setActivePresetId(ids.get(next));
        this.working = MacroHudDataHandler.getConfigCopy();
        syncExternalCanvasElementsFromSources();
        this.selected = null;
        syncCanvasFields();
        syncPresetControls();
    }

    private void createPresetFromField() {
        String name = StringUtils.safe(presetNameField == null ? null : presetNameField.getText());
        if (name.isBlank()) {
            name = "preset_" + (MacroHudDataHandler.listPresetIds().size() + 1);
        }
        name = uniquePresetName(name);
        saveAll();
        MacroHudDataHandler.createPreset(name, true);
        this.working = MacroHudDataHandler.getConfigCopy();
        syncExternalCanvasElementsFromSources();
        this.selected = null;
        syncCanvasFields();
        syncPresetControls();
    }

    private void renamePresetFromField() {
        String target = StringUtils.safe(presetNameField == null ? null : presetNameField.getText());
        if (target.isBlank()) {
            return;
        }
        String current = MacroHudDataHandler.getActivePresetId();
        MacroHudDataHandler.renamePreset(current, target);
        this.working = MacroHudDataHandler.getConfigCopy();
        syncExternalCanvasElementsFromSources();
        this.selected = null;
        syncCanvasFields();
        syncPresetControls();
    }

    private String uniquePresetName(String base) {
        String candidate = base;
        Set<String> existing = new HashSet<>(MacroHudDataHandler.listPresetIds());
        int i = 2;
        while (existing.contains(candidate)) {
            candidate = base + "_" + i;
            i++;
        }
        return candidate;
    }

    private void refreshCanvasChromeVisibility() {
        refreshTabWidgetVisibility();
    }

    private void persistGridPrefs() {
        GRID_ENABLED_PREF = gridEnabled;
        GRID_ROWS_PREF = Math.max(2, gridRows);
        GRID_COLS_PREF = Math.max(2, gridCols);
        CENTER_LINES_ENABLED_PREF = centerLinesEnabled;
    }

    private void openAdvancedModal() {
        if (selected == null) {
            return;
        }

        applyQuickEdit();
        this.advancedOpen = true;
        this.advancedTextFocused = true;
        this.advancedTextScrollLine = 0;
        this.advancedTextManualScroll = false;
        this.advancedActionFocused = false;
        this.advancedBgColorFocused = false;
        this.advancedBorderColorFocused = false;
        this.advancedVisibilityScreenTypeFocused = false;
        this.advancedText = selected.type == MacroHudDataHandler.ElementType.BUTTON
                ? StringUtils.safe(selected.buttonAction)
                : (selected.type == MacroHudDataHandler.ElementType.TEXT ? StringUtils.safe(selected.text) : StringUtils.safe(selected.label));
        this.advancedAction = selected.type == MacroHudDataHandler.ElementType.BUTTON ? StringUtils.safe(selected.label) : StringUtils.safe(selected.sourceToken);
        this.advancedVisibilityScreenType = StringUtils.safe(selected.visibilityScreenType);
        this.advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
        this.advancedBorderColor = ColorUtils.formatColor(selected.borderColor);
        this.advancedCursor = this.advancedText.length();
        this.advancedActionCursor = this.advancedAction.length();
        this.advancedVisibilityScreenTypeCursor = this.advancedVisibilityScreenType.length();
        this.advancedActionSuggestionIndex = -1;
        this.advancedActionSuggestionScroll = 0;
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
        this.advancedSelectionAnchor = -1;
        this.advancedActionSelectionAnchor = -1;
        this.advancedBgSelectionAnchor = -1;
        this.advancedBorderSelectionAnchor = -1;
        this.advancedVisibilityScreenTypeSelectionAnchor = -1;
        this.advancedBgCursor = this.advancedBgColor.length();
        this.advancedBorderCursor = this.advancedBorderColor.length();
        this.advancedActionSuggestionIndex = -1;
        if (isSecondaryChatProxy(selected)) {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            this.advancedText = String.join("\n", settings.regexList == null ? List.of() : settings.regexList);
            this.advancedAction = settings.outgoingRegex == null ? "" : settings.outgoingRegex;
            this.advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
            this.advancedBorderColor = ColorUtils.formatColor(selected.textColor);
            this.advancedBgCursor = this.advancedBgColor.length();
            this.advancedBorderCursor = this.advancedBorderColor.length();
            this.advancedSecondaryShowWhileGuiOpen = settings.showWhileGuiOpen;
            this.advancedSecondaryFadeEnabled = settings.fadeEnabled;
            this.advancedSecondaryResetTransparencyOnHover = settings.resetTransparencyWhenHovered;
            this.advancedSecondaryNoTransparencyWhenChatOpen = settings.noTransparencyWhenChatOpen;
            this.advancedSecondaryFadeDurationMs = settings.fadeDurationMs;
            this.advancedSecondaryMinAlpha = settings.minAlpha;
            this.advancedSecondaryMaxLines = settings.maxLines;
            this.advancedSecondaryScale = Math.max(0.1, selected.fontScale);
            this.advancedSecondaryLineHeight = Math.max(1, selected.lineHeight);
            this.advancedSecondaryInterceptMode = settings.interceptMode;
        } else if (isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) {
            this.advancedText = "";
            this.advancedAction = "";
            this.advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
            this.advancedBorderColor = ColorUtils.formatColor(selected.textColor);
            this.advancedBgCursor = this.advancedBgColor.length();
            this.advancedBorderCursor = this.advancedBorderColor.length();
        } else if (isCustomWidgetType(selected)) {
            this.advancedText = StringUtils.safe(selected.label);
            this.advancedAction = switch (selected.type) {
                case ICON -> StringUtils.safe(selected.iconId);
                case INVENTORY -> (selected.inventoryDisplayMode == null
                        ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR
                        : selected.inventoryDisplayMode).name();
                default -> StringUtils.safe(selected.sourceToken);
            };
            this.advancedCursor = this.advancedText.length();
            this.advancedActionCursor = this.advancedAction.length();
            if (selected.type == MacroHudDataHandler.ElementType.BAR) {
                this.advancedBgColor = String.format(Locale.ROOT, "%.2f,%.2f", selected.minValue, selected.maxValue);
                this.advancedBorderColor = Integer.toString(selected.segments);
                this.advancedBgCursor = this.advancedBgColor.length();
                this.advancedBorderCursor = this.advancedBorderColor.length();
            } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
                this.advancedBgColor = selected.prefix == null ? "" : selected.prefix;
                this.advancedBorderColor = selected.suffix == null ? "" : selected.suffix;
                this.advancedBgCursor = this.advancedBgColor.length();
                this.advancedBorderCursor = this.advancedBorderColor.length();
            } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
                this.advancedBgColor = selected.stateTrueValues == null ? "" : selected.stateTrueValues;
                this.advancedBorderColor = selected.stateFalseValues == null ? "" : selected.stateFalseValues;
                this.advancedBgCursor = this.advancedBgColor.length();
                this.advancedBorderCursor = this.advancedBorderColor.length();
            }
        }
        normalizeAdvancedModalInputState();
        ensureVisibleBackground(selected);
    }

    private void renderAdvancedModal(DrawContext context, int mouseX, int mouseY) {
        int boxX = modalX();
        int boxY = modalY();
        StandardAdvancedLayout layout = standardAdvancedLayout(boxX, boxY);
        UiRect textArea = layout.textArea();
        UiRect actionField = layout.actionField();

        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, MODAL_W, MODAL_H);

        if (isSecondaryChatProxy(selected)) {
            renderSecondaryChatAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }
        if (isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) {
            renderNbtInspectorAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }
        if (isCustomWidgetType(selected)) {
            renderCustomWidgetAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }

        context.drawTextWithShadow(this.textRenderer, "Edit", boxX + 12, boxY + 12, 0xFFFFFFFF);

        context.fill(textArea.x(), textArea.y(), textArea.right(), textArea.bottom(), advancedTextFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textArea.x(), textArea.y(), textArea.right(), textArea.y() + 1, 0x60FFFFFF);

        List<String> lines = splitLinesRaw(advancedText);
        int maxVisible = Math.max(1, (textArea.height() - 8) / 9);
        int maxScroll = Math.max(0, lines.size() - maxVisible);
        advancedTextScrollLine = Math.clamp(advancedTextScrollLine, 0, maxScroll);
        if (advancedTextFocused && !advancedTextManualScroll) {
            int cursorLine = cursorLineIndex(advancedText, advancedCursor);
            if (cursorLine < advancedTextScrollLine) {
                advancedTextScrollLine = cursorLine;
            } else if (cursorLine >= advancedTextScrollLine + maxVisible) {
                advancedTextScrollLine = cursorLine - maxVisible + 1;
            }
        }

        drawMultilineSelectionWithScroll(
                context,
                textArea.x() + 4,
                textArea.y() + 4,
                textArea.bottom() - 12,
                advancedText,
                advancedSelectionAnchor,
                advancedCursor,
                9,
                advancedTextScrollLine
        );

        int y = textArea.y() + 4;
        for (int i = advancedTextScrollLine; i < lines.size(); i++) {
            if (y > textArea.bottom() - 12) {
                break;
            }
            String line = lines.get(i);
            if (shouldHighlightButtonScript()) {
                drawButtonScriptHighlightedLine(context, line, textArea.x() + 4, y);
            } else {
                context.drawTextWithShadow(this.textRenderer, line, textArea.x() + 4, y, 0xFFEAEAEA);
            }
            y += 9;
        }

        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = cursorPixelWithScroll(textArea.x() + 4, textArea.y() + 4, advancedText, advancedCursor, advancedTextScrollLine);
            if (cursor[1] >= textArea.y() + 4 && cursor[1] <= textArea.bottom() - 12) {
                context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
            }
        }

        if (selected != null && selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            context.drawTextWithShadow(this.textRenderer, "Command / Script (multi-line)", textArea.x(), textArea.y() - 10, 0xFFB8B8B8);
            context.drawTextWithShadow(this.textRenderer, "Label (single-line)", actionField.x(), actionField.y() - 10, 0xFFB8B8B8);
        } else {
            context.drawTextWithShadow(this.textRenderer, "Button Action (cmd:/ msg:/ copy:/ etc)", actionField.x(), actionField.y() - 10, 0xFFB8B8B8);
        }
        int actionBg = advancedActionFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(actionField.x(), actionField.y(), actionField.right(), actionField.bottom(), actionBg);
        context.fill(actionField.x(), actionField.y(), actionField.right(), actionField.y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, actionField.x() + 4, actionField.y() + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, actionField.x() + 4, actionField.y() + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = actionField.x() + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, actionField.y() + 4, ax + 1, actionField.y() + 13, 0xFFFFFFFF);
        }
        drawModalButton(context, layout.bgToggle().x(), layout.bgToggle().y(), layout.bgToggle().width(), layout.bgToggle().height(), backgroundLabel(selected), layout.bgToggle().contains(mouseX, mouseY));
        drawModalButton(context, layout.bgOpaque().x(), layout.bgOpaque().y(), layout.bgOpaque().width(), layout.bgOpaque().height(),
                "Opaque: " + (selected != null && selected.backgroundOpaque ? "ON" : "OFF"), layout.bgOpaque().contains(mouseX, mouseY));
        drawModalButton(context, layout.borderToggle().x(), layout.borderToggle().y(), layout.borderToggle().width(), layout.borderToggle().height(), borderModeLabel(selected), layout.borderToggle().contains(mouseX, mouseY));
        drawModalButton(context, layout.hAlign().x(), layout.hAlign().y(), layout.hAlign().width(), layout.hAlign().height(), "H: " + (selected != null ? selected.horizontalAlign.name() : "-"), layout.hAlign().contains(mouseX, mouseY));
        drawModalButton(context, layout.vAlign().x(), layout.vAlign().y(), layout.vAlign().width(), layout.vAlign().height(), "V: " + (selected != null ? selected.verticalAlign.name() : "-"), layout.vAlign().contains(mouseX, mouseY));
        drawModalButton(context, layout.anchor().x(), layout.anchor().y(), layout.anchor().width(), layout.anchor().height(), "Anchor: " + (selected != null ? MacroWorkbenchCanvasUtils.shortAnchor(selected.anchor) : "-"), layout.anchor().contains(mouseX, mouseY));

        drawModalButton(context, layout.lineMinus().x(), layout.lineMinus().y(), layout.lineMinus().width(), layout.lineMinus().height(), "LH-", layout.lineMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.linePlus().x(), layout.linePlus().y(), layout.linePlus().width(), layout.linePlus().height(), "LH+", layout.linePlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.fontMinus().x(), layout.fontMinus().y(), layout.fontMinus().width(), layout.fontMinus().height(), "FS-", layout.fontMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.fontPlus().x(), layout.fontPlus().y(), layout.fontPlus().width(), layout.fontPlus().height(), "FS+", layout.fontPlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.visibility().x(), layout.visibility().y(), layout.visibility().width(), layout.visibility().height(), "Visibility: " + (selected != null ? MacroWorkbenchCanvasUtils.shortVisibility(selected.visibilityMode) : "-"), layout.visibility().contains(mouseX, mouseY));
        if (selected != null && selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            drawModalButton(context, layout.execution().x(), layout.execution().y(), layout.execution().width(), layout.execution().height(),
                    "Exec: " + MacroWorkbenchCanvasUtils.shortExecutionMode(selected.buttonExecutionMode),
                    layout.execution().contains(mouseX, mouseY));
            drawModalButton(context, layout.asyncToggle().x(), layout.asyncToggle().y(), layout.asyncToggle().width(), layout.asyncToggle().height(),
                    "Async: " + (selected.runScriptsAsync ? "ON" : "OFF"),
                    layout.asyncToggle().contains(mouseX, mouseY));
        }
        drawModalButton(context, layout.bgMinus().x(), layout.bgMinus().y(), layout.bgMinus().width(), layout.bgMinus().height(), "BG-", layout.bgMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.bgPlus().x(), layout.bgPlus().y(), layout.bgPlus().width(), layout.bgPlus().height(), "BG+", layout.bgPlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.alphaMinus().x(), layout.alphaMinus().y(), layout.alphaMinus().width(), layout.alphaMinus().height(), "Opacity-", layout.alphaMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.alphaPlus().x(), layout.alphaPlus().y(), layout.alphaPlus().width(), layout.alphaPlus().height(), "Opacity+", layout.alphaPlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.borderMinus().x(), layout.borderMinus().y(), layout.borderMinus().width(), layout.borderMinus().height(), "BR-", layout.borderMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.borderPlus().x(), layout.borderPlus().y(), layout.borderPlus().width(), layout.borderPlus().height(), "BR+", layout.borderPlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.zMinus().x(), layout.zMinus().y(), layout.zMinus().width(), layout.zMinus().height(), "Z-", layout.zMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.zPlus().x(), layout.zPlus().y(), layout.zPlus().width(), layout.zPlus().height(), "Z+", layout.zPlus().contains(mouseX, mouseY));
        int bgPct = selected == null ? 0 : Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f);
        context.drawTextWithShadow(this.textRenderer,
                "Line: " + (selected != null ? selected.lineHeight : 9) + "   Scale: " + (selected != null ? String.format("%.1f", selected.fontScale) : "1.0") + "   BG: " + bgPct + "%   Z: " + (selected != null ? selected.zIndex : 0),
                layout.bgHex().x(), layout.bgHex().y() - 10, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "BG", layout.bgHex().x() - 26, layout.bgHex().y() + 4, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "BR", layout.borderHex().x() - 26, layout.borderHex().y() + 4, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "Use Opacity +/- for transparency. Right-click BR hex to open picker.", layout.bgHex().x(), layout.bgHex().bottom() + 2, 0xFF9A9A9A);

        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(layout.bgHex().x(), layout.bgHex().y(), layout.bgHex().right(), layout.bgHex().bottom(), bgInputBg);
        context.fill(layout.bgHex().x(), layout.bgHex().y(), layout.bgHex().right(), layout.bgHex().y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, layout.bgHex().x() + 4, layout.bgHex().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, layout.bgHex().x() + 4, layout.bgHex().y() + 5, 0xFFEAEAEA);

        int borderInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(layout.borderHex().x(), layout.borderHex().y(), layout.borderHex().right(), layout.borderHex().bottom(), borderInputBg);
        context.fill(layout.borderHex().x(), layout.borderHex().y(), layout.borderHex().right(), layout.borderHex().y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, layout.borderHex().x() + 4, layout.borderHex().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, layout.borderHex().x() + 4, layout.borderHex().y() + 5, 0xFFEAEAEA);

        if (selected != null && selected.visibilityMode == MacroHudDataHandler.VisibilityMode.SCREEN) {
            int visTypeX = layout.visibilityType().x();
            int visTypeY = layout.visibilityType().y();
            int visTypeW = layout.visibilityType().width();
            context.drawTextWithShadow(this.textRenderer, "Visibility Screen Type", visTypeX, visTypeY - 10, 0xFFB8B8B8);
            int visTypeBg = advancedVisibilityScreenTypeFocused ? 0xFF0F0F0F : 0xFF161616;
            context.fill(visTypeX, visTypeY, visTypeX + visTypeW, visTypeY + 18, visTypeBg);
            context.fill(visTypeX, visTypeY, visTypeX + visTypeW, visTypeY + 1, 0x60FFFFFF);
            drawSingleLineSelection(context, visTypeX + 4, visTypeY + 5, advancedVisibilityScreenType,
                    advancedVisibilityScreenTypeSelectionAnchor, advancedVisibilityScreenTypeCursor);
            context.drawTextWithShadow(this.textRenderer, advancedVisibilityScreenType, visTypeX + 4, visTypeY + 5, 0xFFEAEAEA);
            if (advancedVisibilityScreenTypeFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = visTypeX + 4 + this.textRenderer.getWidth(advancedVisibilityScreenType.substring(0,
                        Math.clamp(advancedVisibilityScreenTypeCursor, 0, advancedVisibilityScreenType.length())));
                context.fill(cx, visTypeY + 4, cx + 1, visTypeY + 13, 0xFFFFFFFF);
            }
        }

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.bgHex().x() + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, layout.bgHex().y() + 4, cx + 1, layout.bgHex().y() + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.borderHex().x() + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, layout.borderHex().y() + 4, cx + 1, layout.borderHex().y() + 13, 0xFFFFFFFF);
        }


        drawModalButton(context, layout.apply().x(), layout.apply().y(), layout.apply().width(), layout.apply().height(), "Apply", layout.apply().contains(mouseX, mouseY));
        drawModalButton(context, layout.cancel().x(), layout.cancel().y(), layout.cancel().width(), layout.cancel().height(), "Cancel", layout.cancel().contains(mouseX, mouseY));
    }

    private StandardAdvancedLayout standardAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.standard(boxX, boxY, MODAL_W, MODAL_H);
    }

    private int[] cursorPixelWithScroll(int baseX, int baseY, String text, int cursorIndex, int scrollLine) {
        return GuiTextEditingUtils.cursorPixelWithScroll(this.textRenderer, baseX, baseY, text, cursorIndex, scrollLine);
    }

    private static int cursorLineIndex(String text, int cursorIndex) {
        return GuiTextEditingUtils.cursorLineIndex(text, cursorIndex);
    }

    private void renderSecondaryChatAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        SecondaryAdvancedLayout layout = secondaryAdvancedLayout(boxX, boxY);
        MacroWorkbenchProxyAdvancedModalRenderer.renderSecondary(
                context,
                this.textRenderer,
                selected,
                layout,
                boxX,
                boxY,
                mouseX,
                mouseY,
                advancedSecondaryShowWhileGuiOpen,
                advancedSecondaryFadeEnabled,
                advancedSecondaryResetTransparencyOnHover,
                advancedSecondaryNoTransparencyWhenChatOpen,
                advancedSecondaryInterceptMode,
                advancedSecondaryScale,
                advancedSecondaryLineHeight,
                advancedText,
                advancedTextFocused,
                advancedSelectionAnchor,
                advancedCursor,
                advancedAction,
                advancedActionFocused,
                advancedActionSelectionAnchor,
                advancedActionCursor,
                advancedSecondaryFadeDurationMs,
                advancedSecondaryMinAlpha,
                advancedSecondaryMaxLines,
                advancedBgColor,
                advancedBgColorFocused,
                advancedBgSelectionAnchor,
                advancedBgCursor,
                advancedBorderColor,
                advancedBorderColorFocused,
                advancedBorderSelectionAnchor,
                advancedBorderCursor
        );
    }

    private void renderNbtInspectorAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        String title = isMacroKeybindProxy(selected)
                ? "Macro Keybinds Settings"
                : (isPickupNotifierProxy(selected) ? "Pick-up Notifier Settings" : "NBT Inspector Settings");
        ProxyAdvancedLayout layout = proxyAdvancedLayout(boxX, boxY, isPickupNotifierProxy(selected));
        MacroWorkbenchProxyAdvancedModalRenderer.renderProxy(
                context,
                this.textRenderer,
                selected,
                layout,
                boxX,
                boxY,
                mouseX,
                mouseY,
                title,
                backgroundLabel(selected),
                borderModeLabel(selected),
                advancedBgColor,
                advancedBgColorFocused,
                advancedBgSelectionAnchor,
                advancedBgCursor,
                advancedBorderColor,
                advancedBorderColorFocused,
                advancedBorderSelectionAnchor,
                advancedBorderCursor,
                isPickupNotifierProxy(selected)
        );
    }

    private ProxyAdvancedLayout proxyAdvancedLayout(int boxX, int boxY, boolean pickup) {
        return MacroWorkbenchAdvancedLayouts.proxy(boxX, boxY, MODAL_W, MODAL_H, pickup);
    }

    private SecondaryAdvancedLayout secondaryAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.secondary(boxX, boxY, MODAL_W, MODAL_H);
    }

    private CustomWidgetAdvancedLayout customWidgetAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.custom(boxX, boxY, MODAL_W, MODAL_H);
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }


    private void renderCustomWidgetAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        if (selected == null) {
            return;
        }
        CustomWidgetAdvancedLayout layout = customWidgetAdvancedLayout(boxX, boxY);
        List<String> suggestions = advancedActionSuggestions();
        int rowH = 10;
        int maxVisible = Math.clamp(Math.max(1, layout.suggestionArea().height() / rowH), 1, Math.max(1, suggestions.size()));
        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
        advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll, 0, maxScroll);
        MacroWorkbenchCustomWidgetAdvancedModalRenderer.render(
                context,
                this.textRenderer,
                selected,
                layout,
                mouseX,
                mouseY,
                boxX,
                boxY,
                advancedText,
                advancedTextFocused,
                advancedSelectionAnchor,
                advancedCursor,
                advancedAction,
                advancedActionFocused,
                advancedActionSelectionAnchor,
                advancedActionCursor,
                suggestions,
                advancedActionSuggestionScroll,
                advancedActionSuggestionIndex,
                advancedBgColor,
                advancedBgSelectionAnchor,
                advancedBgCursor,
                advancedBgColorFocused,
                advancedBorderColor,
                advancedBorderSelectionAnchor,
                advancedBorderCursor,
                advancedBorderColorFocused,
                backgroundLabel(selected),
                borderModeLabel(selected)
        );
    }

    private boolean onCustomWidgetAdvancedClick(Click click, int boxX, int boxY) {
        if (click.button() != 0 && click.button() != 1) {
            return true;
        }
        boolean forward = click.button() != 1;
        if (selected == null) {
            return true;
        }
        CustomWidgetAdvancedLayout layout = customWidgetAdvancedLayout(boxX, boxY);
        UiRect labelInput = layout.labelInput();
        UiRect sourceInput = layout.sourceInput();
        UiRect suggestionsArea = layout.suggestionArea();
        List<UiRect> baseRow = layout.baseRow();
        List<UiRect> generalRow1 = layout.generalRow1();
        List<UiRect> generalRow2 = layout.generalRow2();

        List<String> suggestions = advancedActionSuggestions();
        if (handleAdvancedSuggestionClick(click, suggestionsArea, suggestions)) {
            return true;
        }

        if (containsBox(click.x(), click.y(), layout.apply())) {
            applyAdvancedAndClose();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.cancel())) {
            closeAdvancedModal();
            return true;
        }

        if (handleCustomWidgetGeneralRowClick(click, generalRow1, generalRow2, forward)) {
            return true;
        }

        if (handleCustomWidgetBaseRowClick(click, baseRow, forward)) {
            return true;
        }

        if (handleCustomWidgetTypeClick(click, layout, forward)) {
            return true;
        }

        if (handleCustomWidgetTypeInputFocus(click, layout)) {
            return true;
        }

        if (focusAdvancedLabelInput(click, labelInput)) {
            return true;
        }

        if (focusAdvancedSourceInput(click, sourceInput)) {
            return true;
        }

        return true;
    }

    private boolean handleAdvancedSuggestionClick(Click click, UiRect suggestionsArea, List<String> suggestions) {
        return MacroWorkbenchCustomWidgetAdvancedClickHandler.handleSuggestionClick(click, suggestionsArea, suggestions, customWidgetAdvancedClickOpsBridge);
    }

    private boolean handleCustomWidgetGeneralRowClick(Click click, List<UiRect> generalRow1, List<UiRect> generalRow2, boolean forward) {
        return MacroWorkbenchCustomWidgetAdvancedClickHandler.handleGeneralRowClick(
                click,
                generalRow1,
                generalRow2,
                forward,
                selected,
                MacroPlaceholderCatalog.BAR_VALUE_SOURCE_PRESETS,
                MacroPlaceholderCatalog.LIST_SOURCE_PRESETS,
                MacroPlaceholderCatalog.STATE_SOURCE_PRESETS,
                customWidgetAdvancedClickOpsBridge
        );
    }

    private boolean handleCustomWidgetBaseRowClick(Click click, List<UiRect> baseRow, boolean forward) {
        return MacroWorkbenchCustomWidgetAdvancedClickHandler.handleBaseRowClick(
                click,
                baseRow,
                forward,
                selected,
                customWidgetAdvancedClickOpsBridge
        );
    }

    private MacroWorkbenchAdvancedClickSupport.Ops advancedClickSupportOps() {
        return new MacroWorkbenchAdvancedClickSupport.Ops() {
            @Override
            public boolean contains(Click click, UiRect rect) {
                return containsBox(click.x(), click.y(), rect);
            }

            @Override
            public int stepInt(int base) {
                return MacroWorkbenchScreen.this.stepInt(base);
            }

            @Override
            public int cycleStyleColor(int current, boolean forward) {
                return MacroWorkbenchScreen.cycleStyleColor(current, forward);
            }

            @Override
            public void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta) {
                MacroWorkbenchScreen.adjustBackgroundAlpha(element, delta);
            }
        };
    }

    boolean cwContains(Click click, UiRect rect) {
        return containsBox(click.x(), click.y(), rect);
    }

    boolean cwContains(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return containsBox(x, y, boxX, boxY, boxW, boxH);
    }

    int cwStepInt(int base) {
        return this.stepInt(base);
    }

    double cwStepDouble(double base) {
        return this.stepDouble(base);
    }

    int cwCycleStyleColor(int current, boolean forward) {
        return cycleStyleColor(current, forward);
    }

    void cwAdjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta) {
        adjustBackgroundAlpha(element, delta);
    }

    void cwAdjustZIndex(MacroHudDataHandler.HudElement element, int delta) {
        element.zIndex = Math.clamp(element.zIndex + delta, -9999, 9999);
    }

    String cwCyclePreset(String current, String[] presets, boolean forward) {
        return cyclePreset(current, presets, forward);
    }

    String[] cwIconIdSuggestionsForKind(String kind) {
        return iconIdSuggestionsForKind(kind);
    }

    MacroHudDataHandler.HorizontalAlign cwCycleHorizontalAlign(MacroHudDataHandler.HorizontalAlign current, boolean forward) {
        return cycleHorizontalAlign(current, forward);
    }

    MacroHudDataHandler.VerticalAlign cwCycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return cycleVerticalAlign(current, forward);
    }

    MacroHudDataHandler.Anchor cwCycleAnchor(MacroHudDataHandler.Anchor current, boolean forward) {
        return cycleAnchor(current, forward);
    }

    void cwCycleBorderSetting(MacroHudDataHandler.HudElement element, boolean forward) {
        cycleBorderSetting(element, forward);
    }

    void cwEnsureVisibleBackground(MacroHudDataHandler.HudElement element) {
        ensureVisibleBackground(element);
    }

    int cwResolveElementX(MacroHudDataHandler.HudElement element) {
        return resolveElementX(element);
    }

    int cwResolveElementY(MacroHudDataHandler.HudElement element) {
        return resolveElementY(element);
    }

    void cwSetElementScreenPosition(MacroHudDataHandler.HudElement element, int screenX, int screenY) {
        setElementScreenPosition(element, screenX, screenY);
    }

    void cwClampElementToCanvas(MacroHudDataHandler.HudElement element) {
        clampElementToCanvas(element);
    }

    String cwGetAdvancedAction() {
        return advancedAction;
    }

    void cwSetAdvancedAction(String value) {
        advancedAction = value;
    }

    void cwSetAdvancedActionCursor(int value) {
        advancedActionCursor = value;
    }

    int cwGetAdvancedActionSuggestionScroll() {
        return advancedActionSuggestionScroll;
    }

    void cwSetAdvancedActionSuggestionScroll(int value) {
        advancedActionSuggestionScroll = value;
    }

    void cwSetAdvancedActionSuggestionIndex(int value) {
        advancedActionSuggestionIndex = value;
    }

    void cwOpenColorPicker(java.util.function.IntConsumer onPick, String title, int x, int y) {
        openColorPicker(onPick, title, x, y);
    }

    private boolean focusAdvancedLabelInput(Click click, UiRect labelInput) {
        if (!containsBox(click.x(), click.y(), labelInput)) {
            advancedTextFocused = false;
            return false;
        }
        advancedTextFocused = true;
        advancedActionFocused = false;
        advancedBgColorFocused = false;
        advancedBorderColorFocused = false;
        advancedSelectionAnchor = -1;
        advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (labelInput.x() + 4)), 0, 9);
        beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
        return true;
    }

    private boolean focusAdvancedSourceInput(Click click, UiRect sourceInput) {
        if (!containsBox(click.x(), click.y(), sourceInput)) {
            advancedActionFocused = false;
            return false;
        }
        advancedActionFocused = true;
        advancedTextFocused = false;
        advancedBgColorFocused = false;
        advancedBorderColorFocused = false;
        advancedActionSelectionAnchor = -1;
        advancedActionCursor = cursorIndexFromPoint(advancedAction, (int) (click.x() - (sourceInput.x() + 4)), 0, 9);
        beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
        return true;
    }

    private boolean handleCustomWidgetTypeClick(Click click, CustomWidgetAdvancedLayout layout, boolean forward) {
        if (selected == null) {
            return false;
        }
        return MacroWorkbenchCustomWidgetTypeClickHandler.handleTypeClick(
                click,
                layout,
                forward,
                selected,
                MacroPlaceholderCatalog.ICON_KIND_PRESETS,
                MacroPlaceholderCatalog.LIST_SOURCE_PRESETS,
                MacroPlaceholderCatalog.STATE_SOURCE_PRESETS,
                MacroPlaceholderCatalog.SHAPE_TYPE_PRESETS,
                new MacroWorkbenchCustomWidgetTypeClickHandler.Ops() {
                    @Override
                    public boolean contains(Click valueClick, UiRect rect) {
                        return containsBox(valueClick.x(), valueClick.y(), rect);
                    }

                    @Override
                    public int stepInt(int base) {
                        return MacroWorkbenchScreen.this.stepInt(base);
                    }

                    @Override
                    public double stepDouble(double base) {
                        return MacroWorkbenchScreen.this.stepDouble(base);
                    }

                    @Override
                    public String cyclePreset(String current, String[] presets, boolean valueForward) {
                        return MacroWorkbenchScreen.cyclePreset(current, presets, valueForward);
                    }

                    @Override
                    public int cycleStyleColor(int current, boolean valueForward) {
                        return MacroWorkbenchScreen.cycleStyleColor(current, valueForward);
                    }

                    @Override
                    public String[] iconIdSuggestionsForKind(String kind) {
                        return MacroWorkbenchScreen.this.iconIdSuggestionsForKind(kind);
                    }

                    @Override
                    public String getAdvancedAction() {
                        return advancedAction;
                    }

                    @Override
                    public void setAdvancedAction(String value) {
                        advancedAction = value;
                    }

                    @Override
                    public void setAdvancedActionCursor(int value) {
                        advancedActionCursor = value;
                    }

                    @Override
                    public void setAdvancedActionSuggestionScroll(int value) {
                        advancedActionSuggestionScroll = value;
                    }

                    @Override
                    public void openColorPicker(java.util.function.IntConsumer onPick, String title, int x, int y) {
                        MacroWorkbenchScreen.this.openColorPicker(onPick, title, x, y);
                    }
                }
        );
    }

    private boolean handleCustomWidgetTypeInputFocus(Click click, CustomWidgetAdvancedLayout layout) {
        if (selected == null) {
            return false;
        }
        if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            UiRect rangeInput = layout.typeInputLeft();
            UiRect segInput = layout.typeInputRight();
            if (focusAdvancedTypeInputField(click, rangeInput, ModalDragSelectionField.ADVANCED_BG, false)) {
                return true;
            }
            return focusAdvancedTypeInputField(click, segInput, ModalDragSelectionField.ADVANCED_BORDER, false);
        }
        if (selected.type == MacroHudDataHandler.ElementType.VALUE || selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            if (focusAdvancedTypeInputField(click, layout.typeInputLeft(), ModalDragSelectionField.ADVANCED_BG, true)) {
                return true;
            }
            return focusAdvancedTypeInputField(click, layout.typeInputRight(), ModalDragSelectionField.ADVANCED_BORDER, true);
        }
        return false;
    }

    private boolean focusAdvancedTypeInputField(Click click, UiRect input, ModalDragSelectionField field, boolean resetSelectionAnchor) {
        if (!containsBox(click.x(), click.y(), input)) {
            return false;
        }
        boolean bgField = field == ModalDragSelectionField.ADVANCED_BG;
        advancedBgColorFocused = bgField;
        advancedBorderColorFocused = !bgField;
        advancedTextFocused = false;
        advancedActionFocused = false;
        if (bgField) {
            if (resetSelectionAnchor) {
                advancedBgSelectionAnchor = -1;
            }
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, (int) (click.x() - (input.x() + 4)), 0, 9);
        } else {
            if (resetSelectionAnchor) {
                advancedBorderSelectionAnchor = -1;
            }
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, (int) (click.x() - (input.x() + 4)), 0, 9);
        }
        beginModalSelectionDrag(field);
        return true;
    }

    private static boolean isCustomWidgetType(MacroHudDataHandler.HudElement element) {
        if (element == null) {
            return false;
        }
        return element.type == MacroHudDataHandler.ElementType.ICON
                || element.type == MacroHudDataHandler.ElementType.BAR
                || element.type == MacroHudDataHandler.ElementType.VALUE
                || element.type == MacroHudDataHandler.ElementType.LIST
                || element.type == MacroHudDataHandler.ElementType.INVENTORY
                || element.type == MacroHudDataHandler.ElementType.SHAPE
                || element.type == MacroHudDataHandler.ElementType.STATE_BADGE;
    }

    private int stepInt(int base) {
        return base * (isShiftDown() ? 10 : 1);
    }

    private boolean isControlPressed() {
        if (this.client == null) {
            return false;
        }
        var window = this.client.getWindow();
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private double stepDouble(double base) {
        return base * (isShiftDown() ? 10.0 : 1.0);
    }


    private List<String> advancedActionSuggestions() {
        return MacroWorkbenchActionSuggestions.forAction(selected, advancedAction);
    }

    private static String[] iconIdSuggestionsForKind(String kind) {
        return MacroWorkbenchActionSuggestions.iconIdSuggestionsForKind(kind);
    }

    private static MacroHudDataHandler.HorizontalAlign cycleHorizontalAlign(MacroHudDataHandler.HorizontalAlign current, boolean forward) {
        return MacroWorkbenchUiOps.cycleHorizontalAlign(current, forward);
    }

    private static MacroHudDataHandler.VerticalAlign cycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return MacroWorkbenchUiOps.cycleVerticalAlign(current, forward);
    }

    private static void cycleBorderSetting(MacroHudDataHandler.HudElement element, boolean forward) {
        MacroWorkbenchUiOps.cycleBorderSetting(element, forward);
    }

    private static String borderModeLabel(MacroHudDataHandler.HudElement element) {
        return MacroWorkbenchUiOps.borderModeLabel(element);
    }

    private static String backgroundLabel(MacroHudDataHandler.HudElement element) {
        return MacroWorkbenchUiOps.backgroundLabel(element);
    }

    private static void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta) {
        MacroWorkbenchUiOps.adjustBackgroundAlpha(element, delta);
    }

    private static MacroHudDataHandler.Anchor cycleAnchor(MacroHudDataHandler.Anchor current, boolean forward) {
        return MacroWorkbenchUiOps.cycleAnchor(current, forward);
    }

    private static String cyclePreset(String current, String[] presets, boolean forward) {
        return MacroWorkbenchUiOps.cyclePreset(current, presets, forward);
    }

    private static MacroHudDataHandler.InventoryDisplayMode parseInventoryDisplayMode(
            String raw,
            MacroHudDataHandler.InventoryDisplayMode fallback
    ) {
        String value = StringUtils.safe(raw).trim();
        if (value.isEmpty()) {
            return fallback == null ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR : fallback;
        }
        try {
            return MacroHudDataHandler.InventoryDisplayMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback == null ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR : fallback;
        }
    }

    private void applyAdvancedAndClose() {
        if (selected != null) {
            boolean colorHexFields = !isCustomWidgetType(selected);
            if (colorHexFields) {
                applyAdvancedColorFieldsToSelection();
            }
            if (isSecondaryChatProxy(selected)) {
                final boolean showWhileGui = advancedSecondaryShowWhileGuiOpen;
                final boolean fadeEnabled = advancedSecondaryFadeEnabled;
                final boolean hoverReset = advancedSecondaryResetTransparencyOnHover;
                final boolean noTransparencyChatOpen = advancedSecondaryNoTransparencyWhenChatOpen;
                final int fadeMs = Math.max(1000, advancedSecondaryFadeDurationMs);
                final int minAlpha = Math.clamp(advancedSecondaryMinAlpha, 0, 255);
                final int maxLines = Math.max(10, advancedSecondaryMaxLines);
                final double scale = Math.max(0.1, advancedSecondaryScale);
                final int lineHeight = Math.max(1, advancedSecondaryLineHeight);
                final SecondaryChatSettings.InterceptMode interceptMode = advancedSecondaryInterceptMode == null
                        ? SecondaryChatSettings.InterceptMode.COPY
                        : advancedSecondaryInterceptMode;
                final List<String> regexList = splitLinesRaw(advancedText).stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                final String outgoingRegex = StringUtils.safe(advancedAction);
                SecondaryChatSettings.updateAndSave(() -> {
                    SecondaryChatSettings.get().showWhileGuiOpen = showWhileGui;
                    SecondaryChatSettings.get().fadeEnabled = fadeEnabled;
                    SecondaryChatSettings.get().resetTransparencyWhenHovered = hoverReset;
                    SecondaryChatSettings.get().noTransparencyWhenChatOpen = noTransparencyChatOpen;
                    SecondaryChatSettings.get().fadeDurationMs = fadeMs;
                    SecondaryChatSettings.get().minAlpha = minAlpha;
                    SecondaryChatSettings.get().maxLines = maxLines;
                    SecondaryChatSettings.get().interceptMode = interceptMode;
                    SecondaryChatSettings.get().regexList = new ArrayList<>(regexList);
                    SecondaryChatSettings.get().outgoingRegex = outgoingRegex;
                });
                selected.fontScale = (float) Math.max(0.1, scale);
                selected.lineHeight = Math.max(1, lineHeight);
                HudCanvasDataHandler.save();
            } else if (isExternalCanvasProxy(selected)) {
                // External canvas elements only use size/position/style edits.
            } else if (isCustomWidgetType(selected)) {
                selected.label = StringUtils.safe(advancedText);
                if (selected.type == MacroHudDataHandler.ElementType.ICON) {
                    selected.iconId = StringUtils.safe(advancedAction);
                    selected.sourceToken = "";
                } else if (selected.type == MacroHudDataHandler.ElementType.INVENTORY) {
                    selected.inventoryDisplayMode = parseInventoryDisplayMode(advancedAction, selected.inventoryDisplayMode);
                    selected.sourceToken = "";
                } else {
                    selected.sourceToken = StringUtils.safe(advancedAction);
                }
                if (selected.type == MacroHudDataHandler.ElementType.BAR) {
                    try {
                        String[] parts = StringUtils.safe(advancedBgColor).split(",");
                        if (parts.length >= 2) {
                            selected.minValue = Double.parseDouble(parts[0].trim());
                            selected.maxValue = Double.parseDouble(parts[1].trim());
                        }
                    } catch (Exception ignored) {
                        // keep previous values when parse fails
                    }
                    try {
                        selected.segments = Math.clamp(Integer.parseInt(StringUtils.safe(advancedBorderColor).trim()), 1, 120);
                    } catch (Exception ignored) {
                        // keep previous segment value when parse fails
                    }
                } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
                    selected.prefix = advancedBgColor == null ? "" : advancedBgColor;
                    selected.suffix = advancedBorderColor == null ? "" : advancedBorderColor;
                } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
                    selected.stateTrueValues = StringUtils.safe(advancedBgColor);
                    selected.stateFalseValues = StringUtils.safe(advancedBorderColor);
                }
            } else if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
                selected.buttonAction = advancedText;
                selected.label = advancedAction;
                selected.visibilityScreenType = StringUtils.safe(advancedVisibilityScreenType);
            } else {
                selected.text = advancedText;
            }
        }
        syncCanvasFields();
        closeAdvancedModal();
    }

    private void closeAdvancedModal() {
        this.advancedOpen = false;
        this.advancedModalDragging = false;
        this.colorPickerDragging = false;
        this.advancedTextFocused = false;
        this.advancedActionFocused = false;
        this.advancedBgColorFocused = false;
        this.advancedBorderColorFocused = false;
        this.advancedVisibilityScreenTypeFocused = false;
        this.advancedTextScrollLine = 0;
        this.advancedTextManualScroll = false;
        this.colorPickerOpen = false;
        this.colorPickerApply = null;
        this.colorPickerTitle = "Pick Color";
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
        this.modalDragStartMouseX = Integer.MIN_VALUE;
        this.modalDragStartMouseY = Integer.MIN_VALUE;
        this.modalDragSelectionStarted = false;
    }

    private void openKeyboardCommandsModal() {
        if (selectedMacroId == null) {
            return;
        }
        MacroDataHandler.MacroEntry macro = MacroDataHandler.getMacro(selectedMacroId);
        if (macro == null) {
            return;
        }

        this.kbCommandsModalOpen = true;
        this.kbCommandsFocused = true;
        this.kbCommandsText = macro.commands == null ? "" : String.join("\n", macro.commands);
        this.kbCommandsCursor = this.kbCommandsText.length();
    }

    private void closeKeyboardCommandsModal() {
        this.kbCommandsModalOpen = false;
        this.kbCommandsFocused = false;
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
        this.modalDragStartMouseX = Integer.MIN_VALUE;
        this.modalDragStartMouseY = Integer.MIN_VALUE;
        this.modalDragSelectionStarted = false;
    }

    private void renderKeyboardCommandsModal(DrawContext context, int mouseX, int mouseY) {
        int boxX = this.width / 2 - (MODAL_W / 2);
        int boxY = this.height / 2 - (MODAL_H / 2);
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = MODAL_W - 24;
        int textH = 150;

        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, MODAL_W, MODAL_H);

        context.drawTextWithShadow(this.textRenderer, "Edit Commands", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "One command per line", boxX + 12, boxY + 22, 0xFFB0B0B0);

        context.fill(textX, textY, textX + textW, textY + textH, kbCommandsFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textX, textY, textX + textW, textY + 1, 0x60FFFFFF);
        drawMultilineSelection(context, textX + 4, textY + 4, textY + textH - 12, kbCommandsText, kbCommandsSelectionAnchor, kbCommandsCursor, 9);

        List<String> lines = splitLinesRaw(kbCommandsText);
        int yy = textY + 4;
        for (String line : lines) {
            if (yy > textY + textH - 12) {
                break;
            }
            context.drawTextWithShadow(this.textRenderer, line, textX + 4, yy, 0xFFEAEAEA);
            yy += 9;
        }

        if (kbCommandsFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = cursorPixel(textX + 4, textY + 4, kbCommandsText, kbCommandsCursor);
            context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + MODAL_W - 122, boxY + MODAL_H - 22, 54, 18, "Apply", containsBox(mouseX, mouseY, boxX + MODAL_W - 122, boxY + MODAL_H - 22, 54, 18));
        drawModalButton(context, boxX + MODAL_W - 64, boxY + MODAL_H - 22, 54, 18, "Cancel", containsBox(mouseX, mouseY, boxX + MODAL_W - 64, boxY + MODAL_H - 22, 54, 18));
    }

    private boolean onKeyboardCommandsModalClick(Click click) {
        if (click.button() != 0) {
            return true;
        }

        this.activeDragSelectionField = ModalDragSelectionField.NONE;

        int boxX = this.width / 2 - (MODAL_W / 2);
        int boxY = this.height / 2 - (MODAL_H / 2);
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = MODAL_W - 24;
        int textH = 150;

        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 122, boxY + MODAL_H - 22, 54, 18)) {
            applyKeyboardCommandsModal();
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 64, boxY + MODAL_H - 22, 54, 18)) {
            closeKeyboardCommandsModal();
            return true;
        }

        kbCommandsFocused = containsBox(click.x(), click.y(), textX, textY, textW, textH);
        if (kbCommandsFocused) {
            kbCommandsCursor = cursorIndexFromPoint(kbCommandsText, (int) (click.x() - (textX + 4)), (int) (click.y() - (textY + 4)), 9);
            kbCommandsSelectionAnchor = -1;
            beginModalSelectionDrag(ModalDragSelectionField.KB_COMMANDS);
        }
        return true;
    }

    private void renderAddElementModal(DrawContext context, int mouseX, int mouseY) {
        int boxX = this.width / 2 - 160;
        int boxY = this.height / 2 - 110;
        int boxW = 320;
        int boxH = 220;
        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, boxW, boxH);
        context.drawTextWithShadow(this.textRenderer, "Add Element", boxX + 12, boxY + 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Pick element type", boxX + 12, boxY + 22, 0xFFB0B0B0);

        String[] labels = {"Button", "Text", "Macro List", "Icon", "Bar", "Value", "List", "Inventory", "Shape", "State Badge"};
        for (int i = 0; i < labels.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int bx = boxX + 12 + col * 100;
            int by = boxY + 42 + row * 26;
            drawModalButton(context, bx, by, 92, 20, labels[i], containsBox(mouseX, mouseY, bx, by, 92, 20));
        }
        drawModalButton(context, boxX + boxW - 70, boxY + boxH - 24, 58, 18, "Close", containsBox(mouseX, mouseY, boxX + boxW - 70, boxY + boxH - 24, 58, 18));
    }

    private boolean onAddElementModalClick(Click click) {
        if (click.button() != 0) {
            return true;
        }
        int boxX = this.width / 2 - 160;
        int boxY = this.height / 2 - 110;
        int boxW = 320;
        int boxH = 220;
        if (containsBox(click.x(), click.y(), boxX + boxW - 70, boxY + boxH - 24, 58, 18)) {
            addElementModalOpen = false;
            return true;
        }

        MacroHudDataHandler.ElementType[] types = {
                MacroHudDataHandler.ElementType.BUTTON,
                MacroHudDataHandler.ElementType.TEXT,
                MacroHudDataHandler.ElementType.MACRO_KEYBINDS,
                MacroHudDataHandler.ElementType.ICON,
                MacroHudDataHandler.ElementType.BAR,
                MacroHudDataHandler.ElementType.VALUE,
                MacroHudDataHandler.ElementType.LIST,
                MacroHudDataHandler.ElementType.INVENTORY,
                MacroHudDataHandler.ElementType.SHAPE,
                MacroHudDataHandler.ElementType.STATE_BADGE
        };

        for (int i = 0; i < types.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int bx = boxX + 12 + col * 100;
            int by = boxY + 42 + row * 26;
            if (containsBox(click.x(), click.y(), bx, by, 92, 20)) {
                createCanvasElement(types[i]);
                addElementModalOpen = false;
                return true;
            }
        }
        return true;
    }

    private void createCanvasElement(MacroHudDataHandler.ElementType type) {
        MacroHudDataHandler.HudElement e = MacroHudDataHandler.createElement(type);
        e.zIndex = nextAvailableZIndex();
        int centerX = Math.max(0, (this.width - e.width) / 2);
        int centerY = canvasTopBound() + Math.max(0, (canvasHeightBound() - e.height) / 2);
        setElementScreenPosition(e, centerX, centerY);
        if (type == MacroHudDataHandler.ElementType.BUTTON) {
            e.label = "Macro";
        }
        if (type == MacroHudDataHandler.ElementType.TEXT) {
            e.text = "Text";
        }
        this.working.elements.add(e);
        this.selected = e;
        this.selectedElementIds.clear();
        this.selectedElementIds.add(e.id);
        clampElementToCanvas(e);
        syncCanvasFields();
    }

    private int nextAvailableZIndex() {
        return this.working.elements.stream()
                .filter(Objects::nonNull)
                .mapToInt(element -> element.zIndex)
                .max()
                .orElse(-1) + 1;
    }

    private boolean handleKeyboardCommandsKey(int keyCode) {
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_A) {
            kbCommandsSelectionAnchor = 0;
            kbCommandsCursor = kbCommandsText.length();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)) {
                int start = selectionStart(kbCommandsSelectionAnchor, kbCommandsCursor);
                int end = selectionEnd(kbCommandsSelectionAnchor, kbCommandsCursor);
                kbCommandsText = kbCommandsText.substring(0, start) + kbCommandsText.substring(end);
                kbCommandsCursor = start;
                kbCommandsSelectionAnchor = -1;
                return true;
            }
            if (kbCommandsCursor > 0) {
                kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor - 1) + kbCommandsText.substring(kbCommandsCursor);
                kbCommandsCursor--;
            }
            kbCommandsSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)) {
                int start = selectionStart(kbCommandsSelectionAnchor, kbCommandsCursor);
                int end = selectionEnd(kbCommandsSelectionAnchor, kbCommandsCursor);
                kbCommandsText = kbCommandsText.substring(0, start) + kbCommandsText.substring(end);
                kbCommandsCursor = start;
                kbCommandsSelectionAnchor = -1;
                return true;
            }
            if (kbCommandsCursor < kbCommandsText.length()) {
                kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor) + kbCommandsText.substring(kbCommandsCursor + 1);
            }
            kbCommandsSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = kbCommandsCursor;
            kbCommandsCursor = Math.max(0, kbCommandsCursor - 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = kbCommandsCursor;
            kbCommandsCursor = Math.min(kbCommandsText.length(), kbCommandsCursor + 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int old = kbCommandsCursor;
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, -1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int old = kbCommandsCursor;
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = kbCommandsCursor;
            kbCommandsCursor = lineStart(kbCommandsText, kbCommandsCursor);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = kbCommandsCursor;
            kbCommandsCursor = lineEnd(kbCommandsText, kbCommandsCursor);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            insertAtKeyboardCommandsCursor("\n");
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtKeyboardCommandsCursor(this.client.keyboard.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)
                    ? selectedText(kbCommandsText, kbCommandsSelectionAnchor, kbCommandsCursor)
                    : kbCommandsText);
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_X && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)
                    ? selectedText(kbCommandsText, kbCommandsSelectionAnchor, kbCommandsCursor)
                    : kbCommandsText);
            if (hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)) {
                int start = selectionStart(kbCommandsSelectionAnchor, kbCommandsCursor);
                int end = selectionEnd(kbCommandsSelectionAnchor, kbCommandsCursor);
                kbCommandsText = kbCommandsText.substring(0, start) + kbCommandsText.substring(end);
                kbCommandsCursor = start;
            } else {
                kbCommandsText = "";
                kbCommandsCursor = 0;
            }
            kbCommandsSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeKeyboardCommandsModal();
            return true;
        }
        return false;
    }

    private void insertAtKeyboardCommandsCursor(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        if (hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)) {
            int start = selectionStart(kbCommandsSelectionAnchor, kbCommandsCursor);
            int end = selectionEnd(kbCommandsSelectionAnchor, kbCommandsCursor);
            kbCommandsText = kbCommandsText.substring(0, start) + kbCommandsText.substring(end);
            kbCommandsCursor = start;
        }
        kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor) + s + kbCommandsText.substring(kbCommandsCursor);
        kbCommandsCursor += s.length();
        kbCommandsSelectionAnchor = -1;
    }

    private void applyKeyboardCommandsModal() {
        if (selectedMacroId == null) {
            closeKeyboardCommandsModal();
            return;
        }
        MacroDataHandler.MacroEntry existing = MacroDataHandler.getMacro(selectedMacroId);
        if (existing == null) {
            return;
        }

        List<String> commands = new ArrayList<>();
        String raw = kbCommandsText == null ? "" : kbCommandsText.trim();
        if (!raw.isBlank()) {
            for (String part : raw.split("[;\\n]+")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    commands.add(t);
                }
            }
        }

        MacroDataHandler.updateMacro(
                selectedMacroId,
                StringUtils.safe(kbNameField.getText()),
                commands,
                existing.keyCode,
                existing.modifierKey,
                parseDelayOrDefault(existing.delayTicks),
                existing.showInOverlay
        );

        kbCommandsText = String.join("\n", commands);
        kbCommandsField.setText(String.join(";", commands));
        CommandMacros.refreshKeybindings();
        rebuildBindingMaps();
        closeKeyboardCommandsModal();
    }

    private int parseDelayOrDefault(int fallback) {
        try {
            if (kbDelayField.getText() != null && !kbDelayField.getText().isBlank()) {
                return Math.max(0, Integer.parseInt(kbDelayField.getText().trim()));
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private boolean handleAdvancedTextKey(int keyCode) {
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_A) {
            advancedSelectionAnchor = 0;
            advancedCursor = advancedText.length();
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
                int start = selectionStart(advancedSelectionAnchor, advancedCursor);
                int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
                advancedText = advancedText.substring(0, start) + advancedText.substring(end);
                advancedCursor = start;
                advancedSelectionAnchor = -1;
                advancedTextManualScroll = false;
                return true;
            }
            if (advancedCursor > 0) {
                advancedText = advancedText.substring(0, advancedCursor - 1) + advancedText.substring(advancedCursor);
                advancedCursor--;
            }
            advancedSelectionAnchor = -1;
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
                int start = selectionStart(advancedSelectionAnchor, advancedCursor);
                int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
                advancedText = advancedText.substring(0, start) + advancedText.substring(end);
                advancedCursor = start;
                advancedSelectionAnchor = -1;
                advancedTextManualScroll = false;
                return true;
            }
            if (advancedCursor < advancedText.length()) {
                advancedText = advancedText.substring(0, advancedCursor) + advancedText.substring(advancedCursor + 1);
            }
            advancedSelectionAnchor = -1;
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = advancedCursor;
            advancedCursor = Math.max(0, advancedCursor - 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = advancedCursor;
            advancedCursor = Math.min(advancedText.length(), advancedCursor + 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = advancedCursor;
            advancedCursor = lineStart(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = advancedCursor;
            advancedCursor = lineEnd(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, -1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            insertAtCursor("\n");
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtCursor(this.client.keyboard.getClipboard());
            advancedActionSuggestionIndex = -1;
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(advancedSelectionAnchor, advancedCursor)
                    ? selectedText(advancedText, advancedSelectionAnchor, advancedCursor)
                    : advancedText);
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_X && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(advancedSelectionAnchor, advancedCursor)
                    ? selectedText(advancedText, advancedSelectionAnchor, advancedCursor)
                    : advancedText);
            if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
                int start = selectionStart(advancedSelectionAnchor, advancedCursor);
                int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
                advancedText = advancedText.substring(0, start) + advancedText.substring(end);
                advancedCursor = start;
            } else {
                advancedText = "";
                advancedCursor = 0;
            }
            advancedSelectionAnchor = -1;
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private boolean handleAdvancedActionKey(int keyCode) {
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_A) {
            advancedActionSelectionAnchor = 0;
            advancedActionCursor = advancedAction.length();
            return true;
        }
        if (!isShiftDown() && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
            List<String> suggestions = advancedActionSuggestions();
            if (!suggestions.isEmpty()) {
                int dir = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
                int start = advancedActionSuggestionIndex < 0
                        ? suggestions.indexOf(advancedAction)
                        : advancedActionSuggestionIndex;
                int next = nextSelectableSuggestionIndex(suggestions, start, dir);
                if (next < 0) {
                    return true;
                }
                advancedActionSuggestionIndex = next;
                advancedAction = MacroWorkbenchActionSuggestions.value(suggestions.get(next));
                advancedActionCursor = advancedAction.length();
                ensureSuggestionVisible(suggestions.size(), next, modalY() + 94);
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(advancedActionSelectionAnchor, advancedActionCursor)) {
                int start = selectionStart(advancedActionSelectionAnchor, advancedActionCursor);
                int end = selectionEnd(advancedActionSelectionAnchor, advancedActionCursor);
                advancedAction = advancedAction.substring(0, start) + advancedAction.substring(end);
                advancedActionCursor = start;
                advancedActionSelectionAnchor = -1;
                advancedActionSuggestionIndex = -1;
                return true;
            }
            if (advancedActionCursor > 0) {
                advancedAction = advancedAction.substring(0, advancedActionCursor - 1) + advancedAction.substring(advancedActionCursor);
                advancedActionCursor--;
                advancedActionSuggestionIndex = -1;
            }
            advancedActionSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(advancedActionSelectionAnchor, advancedActionCursor)) {
                int start = selectionStart(advancedActionSelectionAnchor, advancedActionCursor);
                int end = selectionEnd(advancedActionSelectionAnchor, advancedActionCursor);
                advancedAction = advancedAction.substring(0, start) + advancedAction.substring(end);
                advancedActionCursor = start;
                advancedActionSelectionAnchor = -1;
                advancedActionSuggestionIndex = -1;
                return true;
            }
            if (advancedActionCursor < advancedAction.length()) {
                advancedAction = advancedAction.substring(0, advancedActionCursor) + advancedAction.substring(advancedActionCursor + 1);
                advancedActionSuggestionIndex = -1;
            }
            advancedActionSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = advancedActionCursor;
            advancedActionCursor = Math.max(0, advancedActionCursor - 1);
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = advancedActionCursor;
            advancedActionCursor = Math.min(advancedAction.length(), advancedActionCursor + 1);
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = advancedActionCursor;
            advancedActionCursor = 0;
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = advancedActionCursor;
            advancedActionCursor = advancedAction.length();
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            List<String> suggestions = advancedActionSuggestions();
            if (!suggestions.isEmpty()) {
                int dir = isShiftDown() ? -1 : 1;
                int start = advancedActionSuggestionIndex < 0
                        ? suggestions.indexOf(advancedAction)
                        : advancedActionSuggestionIndex;
                int next = nextSelectableSuggestionIndex(suggestions, start, dir);
                if (next < 0) {
                    return true;
                }
                advancedActionSuggestionIndex = next;
                advancedAction = MacroWorkbenchActionSuggestions.value(suggestions.get(next));
                advancedActionCursor = advancedAction.length();
                advancedActionSelectionAnchor = -1;
                ensureSuggestionVisible(suggestions.size(), next, modalY() + 94);
            }
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtAdvancedActionCursor(this.client.keyboard.getClipboard());
            advancedActionSuggestionIndex = -1;
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(advancedActionSelectionAnchor, advancedActionCursor)
                    ? selectedText(advancedAction, advancedActionSelectionAnchor, advancedActionCursor)
                    : advancedAction);
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_X && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(advancedActionSelectionAnchor, advancedActionCursor)
                    ? selectedText(advancedAction, advancedActionSelectionAnchor, advancedActionCursor)
                    : advancedAction);
            if (hasSelection(advancedActionSelectionAnchor, advancedActionCursor)) {
                int start = selectionStart(advancedActionSelectionAnchor, advancedActionCursor);
                int end = selectionEnd(advancedActionSelectionAnchor, advancedActionCursor);
                advancedAction = advancedAction.substring(0, start) + advancedAction.substring(end);
                advancedActionCursor = start;
            } else {
                advancedAction = "";
                advancedActionCursor = 0;
            }
            advancedActionSelectionAnchor = -1;
            advancedActionSuggestionIndex = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private static int nextSelectableSuggestionIndex(List<String> suggestions, int start, int direction) {
        return MacroWorkbenchActionSuggestions.nextSelectableIndex(suggestions, start, direction);
    }

    private boolean handleAdvancedBgKey(int keyCode) {
        return handleAdvancedColorKey(keyCode, true);
    }

    private boolean handleAdvancedBorderKey(int keyCode) {
        return handleAdvancedColorKey(keyCode, false);
    }

    private boolean handleAdvancedVisibilityScreenTypeKey(int keyCode) {
        String value = advancedVisibilityScreenType;
        int cursor = advancedVisibilityScreenTypeCursor;
        int selectionAnchor = advancedVisibilityScreenTypeSelectionAnchor;

        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_A) {
            advancedVisibilityScreenTypeSelectionAnchor = 0;
            advancedVisibilityScreenTypeCursor = value.length();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                advancedVisibilityScreenType = value.substring(0, start) + value.substring(end);
                advancedVisibilityScreenTypeCursor = start;
            } else if (cursor > 0) {
                advancedVisibilityScreenType = value.substring(0, cursor - 1) + value.substring(cursor);
                advancedVisibilityScreenTypeCursor = cursor - 1;
            }
            advancedVisibilityScreenTypeSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                advancedVisibilityScreenType = value.substring(0, start) + value.substring(end);
                advancedVisibilityScreenTypeCursor = start;
            } else if (cursor < value.length()) {
                advancedVisibilityScreenType = value.substring(0, cursor) + value.substring(cursor + 1);
            }
            advancedVisibilityScreenTypeSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = cursor;
            int next = Math.max(0, cursor - 1);
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = cursor;
            int next = Math.min(value.length(), cursor + 1);
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = cursor;
            advancedVisibilityScreenTypeCursor = 0;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, 0, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = cursor;
            int next = value.length();
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtAdvancedVisibilityScreenTypeCursor(this.client.keyboard.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_X && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                advancedVisibilityScreenType = value.substring(0, start) + value.substring(end);
                advancedVisibilityScreenTypeCursor = start;
            } else {
                advancedVisibilityScreenType = "";
                advancedVisibilityScreenTypeCursor = 0;
            }
            advancedVisibilityScreenTypeSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private boolean handleAdvancedColorKey(int keyCode, boolean background) {
        String value = background ? advancedBgColor : advancedBorderColor;
        int cursor = background ? advancedBgCursor : advancedBorderCursor;
        int selectionAnchor = background ? advancedBgSelectionAnchor : advancedBorderSelectionAnchor;

        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_A) {
            if (background) {
                advancedBgSelectionAnchor = 0;
                advancedBgCursor = value.length();
            } else {
                advancedBorderSelectionAnchor = 0;
                advancedBorderCursor = value.length();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                value = value.substring(0, start) + value.substring(end);
                setAdvancedColorState(background, value, start);
                if (background) {
                    advancedBgSelectionAnchor = -1;
                } else {
                    advancedBorderSelectionAnchor = -1;
                }
                return true;
            }
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
            }
            setAdvancedColorState(background, value, cursor);
            if (background) {
                advancedBgSelectionAnchor = -1;
            } else {
                advancedBorderSelectionAnchor = -1;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                value = value.substring(0, start) + value.substring(end);
                setAdvancedColorState(background, value, start);
                if (background) {
                    advancedBgSelectionAnchor = -1;
                } else {
                    advancedBorderSelectionAnchor = -1;
                }
                return true;
            }
            if (cursor < value.length()) {
                value = value.substring(0, cursor) + value.substring(cursor + 1);
            }
            setAdvancedColorState(background, value, cursor);
            if (background) {
                advancedBgSelectionAnchor = -1;
            } else {
                advancedBorderSelectionAnchor = -1;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = cursor;
            int next = Math.max(0, cursor - 1);
            setAdvancedColorState(background, value, next);
            if (background) {
                advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            } else {
                advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = cursor;
            int next = Math.min(value.length(), cursor + 1);
            setAdvancedColorState(background, value, next);
            if (background) {
                advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            } else {
                advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = cursor;
            setAdvancedColorState(background, value, 0);
            if (background) {
                advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, 0, isShiftDown());
            } else {
                advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, 0, isShiftDown());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = cursor;
            int next = value.length();
            setAdvancedColorState(background, value, next);
            if (background) {
                advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            } else {
                advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
            }
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            if (background) {
                insertAtAdvancedBgCursor(this.client.keyboard.getClipboard());
            } else {
                insertAtAdvancedBorderCursor(this.client.keyboard.getClipboard());
            }
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_X && this.client != null) {
            this.client.keyboard.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                value = value.substring(0, start) + value.substring(end);
                setAdvancedColorState(background, value, start);
            } else {
                setAdvancedColorState(background, "", 0);
            }
            if (background) {
                advancedBgSelectionAnchor = -1;
            } else {
                advancedBorderSelectionAnchor = -1;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private void setAdvancedColorState(boolean background, String value, int cursor) {
        if (background) {
            advancedBgColor = value;
            advancedBgCursor = Math.clamp(cursor, 0, advancedBgColor.length());
        } else {
            advancedBorderColor = value;
            advancedBorderCursor = Math.clamp(cursor, 0, advancedBorderColor.length());
        }
    }

    private void insertAtCursor(String s) {
        normalizeAdvancedModalInputState();
        String normalizedInput = normalizeMultilineInput(s);
        if (normalizedInput.isEmpty()) {
            return;
        }
        if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
            int start = selectionStart(advancedSelectionAnchor, advancedCursor);
            int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
            this.advancedText = advancedText.substring(0, start) + advancedText.substring(end);
            this.advancedCursor = start;
        }
        this.advancedText = advancedText.substring(0, advancedCursor) + normalizedInput + advancedText.substring(advancedCursor);
        this.advancedCursor += normalizedInput.length();
        this.advancedSelectionAnchor = -1;
        this.advancedActionSuggestionScroll = 0;
        this.advancedTextManualScroll = false;
    }

    private void insertAtAdvancedActionCursor(String s) {
        normalizeAdvancedModalInputState();
        String normalizedInput = normalizeSingleLineInput(s);
        if (normalizedInput.isEmpty()) {
            return;
        }
        if (hasSelection(advancedActionSelectionAnchor, advancedActionCursor)) {
            int start = selectionStart(advancedActionSelectionAnchor, advancedActionCursor);
            int end = selectionEnd(advancedActionSelectionAnchor, advancedActionCursor);
            this.advancedAction = advancedAction.substring(0, start) + advancedAction.substring(end);
            this.advancedActionCursor = start;
        }
        this.advancedAction = advancedAction.substring(0, advancedActionCursor) + normalizedInput + advancedAction.substring(advancedActionCursor);
        this.advancedActionCursor += normalizedInput.length();
        this.advancedActionSelectionAnchor = -1;
        this.advancedActionSuggestionIndex = -1;
        this.advancedActionSuggestionScroll = 0;
    }

    private void insertAtAdvancedBgCursor(String s) {
        normalizeAdvancedModalInputState();
        if (s == null || s.isEmpty()) {
            return;
        }
        if (hasSelection(advancedBgSelectionAnchor, advancedBgCursor)) {
            int start = selectionStart(advancedBgSelectionAnchor, advancedBgCursor);
            int end = selectionEnd(advancedBgSelectionAnchor, advancedBgCursor);
            this.advancedBgColor = advancedBgColor.substring(0, start) + advancedBgColor.substring(end);
            this.advancedBgCursor = start;
        }
        this.advancedBgColor = advancedBgColor.substring(0, advancedBgCursor) + s + advancedBgColor.substring(advancedBgCursor);
        this.advancedBgCursor += s.length();
        this.advancedBgSelectionAnchor = -1;
    }

    private void insertAtAdvancedBorderCursor(String s) {
        normalizeAdvancedModalInputState();
        if (s == null || s.isEmpty()) {
            return;
        }
        if (hasSelection(advancedBorderSelectionAnchor, advancedBorderCursor)) {
            int start = selectionStart(advancedBorderSelectionAnchor, advancedBorderCursor);
            int end = selectionEnd(advancedBorderSelectionAnchor, advancedBorderCursor);
            this.advancedBorderColor = advancedBorderColor.substring(0, start) + advancedBorderColor.substring(end);
            this.advancedBorderCursor = start;
        }
        this.advancedBorderColor = advancedBorderColor.substring(0, advancedBorderCursor) + s + advancedBorderColor.substring(advancedBorderCursor);
        this.advancedBorderCursor += s.length();
        this.advancedBorderSelectionAnchor = -1;
    }

    private void insertAtAdvancedVisibilityScreenTypeCursor(String s) {
        normalizeAdvancedModalInputState();
        String normalizedInput = normalizeSingleLineInput(s);
        if (normalizedInput.isEmpty()) {
            return;
        }
        if (hasSelection(advancedVisibilityScreenTypeSelectionAnchor, advancedVisibilityScreenTypeCursor)) {
            int start = selectionStart(advancedVisibilityScreenTypeSelectionAnchor, advancedVisibilityScreenTypeCursor);
            int end = selectionEnd(advancedVisibilityScreenTypeSelectionAnchor, advancedVisibilityScreenTypeCursor);
            this.advancedVisibilityScreenType = advancedVisibilityScreenType.substring(0, start) + advancedVisibilityScreenType.substring(end);
            this.advancedVisibilityScreenTypeCursor = start;
        }
        this.advancedVisibilityScreenType = advancedVisibilityScreenType.substring(0, advancedVisibilityScreenTypeCursor)
                + normalizedInput
                + advancedVisibilityScreenType.substring(advancedVisibilityScreenTypeCursor);
        this.advancedVisibilityScreenTypeCursor += normalizedInput.length();
        this.advancedVisibilityScreenTypeSelectionAnchor = -1;
    }

    private void normalizeAdvancedModalInputState() {
        this.advancedText = StringUtils.safe(this.advancedText);
        this.advancedAction = StringUtils.safe(this.advancedAction);
        this.advancedBgColor = StringUtils.safe(this.advancedBgColor);
        this.advancedBorderColor = StringUtils.safe(this.advancedBorderColor);
        this.advancedVisibilityScreenType = StringUtils.safe(this.advancedVisibilityScreenType);

        this.advancedCursor = clampTextIndex(this.advancedText, this.advancedCursor);
        this.advancedActionCursor = clampTextIndex(this.advancedAction, this.advancedActionCursor);
        this.advancedBgCursor = clampTextIndex(this.advancedBgColor, this.advancedBgCursor);
        this.advancedBorderCursor = clampTextIndex(this.advancedBorderColor, this.advancedBorderCursor);
        this.advancedVisibilityScreenTypeCursor = clampTextIndex(this.advancedVisibilityScreenType, this.advancedVisibilityScreenTypeCursor);

        this.advancedSelectionAnchor = clampSelectionAnchor(this.advancedText, this.advancedSelectionAnchor);
        this.advancedActionSelectionAnchor = clampSelectionAnchor(this.advancedAction, this.advancedActionSelectionAnchor);
        this.advancedBgSelectionAnchor = clampSelectionAnchor(this.advancedBgColor, this.advancedBgSelectionAnchor);
        this.advancedBorderSelectionAnchor = clampSelectionAnchor(this.advancedBorderColor, this.advancedBorderSelectionAnchor);
        this.advancedVisibilityScreenTypeSelectionAnchor = clampSelectionAnchor(this.advancedVisibilityScreenType, this.advancedVisibilityScreenTypeSelectionAnchor);
    }

    private void applyAdvancedColorFieldsToSelection() {
        if (selected == null) {
            return;
        }
        Integer parsedBg = parseArgb(advancedBgColor);
        if (parsedBg != null) {
            selected.backgroundColor = parsedBg;
            selected.backgroundAlpha = (parsedBg >>> 24) & 0xFF;
        }
        Integer parsedBorder = parseArgb(advancedBorderColor);
        if (parsedBorder != null) {
            if (isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) {
                selected.textColor = parsedBorder;
            } else {
                selected.borderColor = parsedBorder;
            }
        }
        advancedBgColor = ColorUtils.formatColor(selected.backgroundColor);
        advancedBorderColor = ColorUtils.formatColor((isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) ? selected.textColor : selected.borderColor);
        advancedBgCursor = Math.clamp(advancedBgCursor, 0, advancedBgColor.length());
        advancedBorderCursor = Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length());
    }

    private void openColorPicker(boolean forBackground, int anchorX, int anchorY) {
        IntConsumer apply = forBackground
                ? color -> {
            advancedBgColor = ColorUtils.formatColor(color);
            advancedBgCursor = advancedBgColor.length();
            if (selected != null) {
                selected.backgroundColor = color;
                selected.backgroundAlpha = (color >>> 24) & 0xFF;
            }
        }
                : color -> {
            advancedBorderColor = ColorUtils.formatColor(color);
            advancedBorderCursor = advancedBorderColor.length();
            if (selected != null) {
                if (isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) {
                    selected.textColor = color;
                } else {
                    selected.borderColor = color;
                }
            }
        };
        openColorPicker(apply, forBackground ? "Pick BG" : "Pick Color", anchorX, anchorY);
    }

    private void openColorPicker(IntConsumer apply, String title, int anchorX, int anchorY) {
        colorPickerApply = apply;
        colorPickerTitle = StringUtils.safe(title).isBlank() ? "Pick Color" : StringUtils.safe(title);
        int pickerW = 140;
        int pickerH = 86;
        colorPickerX = Math.clamp(anchorX, 6, Math.max(6, this.width - pickerW - 6));
        colorPickerY = Math.clamp(anchorY, 26, Math.max(26, this.height - pickerH - 6));
        colorPickerDragging = false;
        colorPickerOpen = true;
    }

    private boolean handleColorPickerClick(Click click) {
        int pickerW = 140;
        int pickerH = 86;
        int headerH = 18;
        if (!containsBox(click.x(), click.y(), colorPickerX, colorPickerY, pickerW, pickerH)) {
            colorPickerDragging = false;
            colorPickerOpen = false;
            return true;
        }
        int closeX = colorPickerX + pickerW - 16;
        int closeY = colorPickerY + 4;
        if (containsBox(click.x(), click.y(), closeX, closeY, 12, 12)) {
            colorPickerDragging = false;
            colorPickerOpen = false;
            return true;
        }
        if (containsBox(click.x(), click.y(), colorPickerX, colorPickerY, pickerW, headerH)) {
            colorPickerDragging = true;
            colorPickerDragOffsetX = (int) click.x() - colorPickerX;
            colorPickerDragOffsetY = (int) click.y() - colorPickerY;
            return true;
        }
        int swX = colorPickerX + 8;
        int swY = colorPickerY + 20;
        int sw = 12;
        int gap = 4;
        for (int i = 0; i < COLOR_PICKER_PRESETS.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int x = swX + col * (sw + gap);
            int y = swY + row * (sw + gap);
            if (containsBox(click.x(), click.y(), x, y, sw, sw)) {
                applyColorPickerSelection(COLOR_PICKER_PRESETS[i]);
                return true;
            }
        }
        return true;
    }

    private void applyColorPickerSelection(int color) {
        if (colorPickerApply != null) {
            colorPickerApply.accept(color);
        }
    }

    private void renderColorPickerPopup(DrawContext context, int mouseX, int mouseY) {
        if (!colorPickerOpen) {
            return;
        }
        int pickerW = 140;
        int pickerH = 60;
        GuiSystem.drawPanel(context, colorPickerX, colorPickerY, pickerW, pickerH);
        context.drawTextWithShadow(this.textRenderer, colorPickerTitle, colorPickerX + 8, colorPickerY + 6, 0xFFFFFFFF);
        drawModalButton(context, colorPickerX + pickerW - 16, colorPickerY + 4, 12, 12, "x",
                containsBox(mouseX, mouseY, colorPickerX + pickerW - 16, colorPickerY + 4, 12, 12));

        int swX = colorPickerX + 8;
        int swY = colorPickerY + 20;
        int sw = 12;
        int gap = 4;
        for (int i = 0; i < COLOR_PICKER_PRESETS.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int x = swX + col * (sw + gap);
            int y = swY + row * (sw + gap);
            int color = COLOR_PICKER_PRESETS[i];
            context.fill(x, y, x + sw, y + sw, color);
            context.fill(x, y, x + sw, y + 1, 0x90FFFFFF);
            context.fill(x, y + sw - 1, x + sw, y + sw, 0x90000000);
            if (containsBox(mouseX, mouseY, x, y, sw, sw)) {
                context.fill(x - 1, y - 1, x + sw + 1, y, 0xFFFFFFFF);
                context.fill(x - 1, y + sw, x + sw + 1, y + sw + 1, 0xFFFFFFFF);
                context.fill(x - 1, y, x, y + sw, 0xFFFFFFFF);
                context.fill(x + sw, y, x + sw + 1, y + sw, 0xFFFFFFFF);
            }
        }
    }

    private static Integer parseArgb(String raw) {
        return ColorUtils.parseArgb(raw);
    }

    private int[] cursorPixel(int x, int y, String text, int cursorIndex) {
        return GuiTextEditingUtils.cursorPixel(this.textRenderer, x, y, text, cursorIndex);
    }

    private void rebuildBindingMaps() {
        macroBindingNames.clear();
        macroBindingIds.clear();
        gameBindingNames.clear();

        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : MacroDataHandler.getAllMacros().entrySet()) {
            String id = entry.getKey();
            MacroDataHandler.MacroEntry m = entry.getValue();
            if (m == null || m.keyCode < 0) {
                continue;
            }
            String name = m.name == null || m.name.isBlank() ? id : m.name;
            macroBindingIds.computeIfAbsent(m.keyCode, k -> new ArrayList<>()).add(id);
            macroBindingNames.computeIfAbsent(m.keyCode, k -> new ArrayList<>()).add(name);
        }

        if (this.client != null && this.client.options != null) {
            for (KeyBinding kb : this.client.options.allKeys) {
                if (kb == null) {
                    continue;
                }
                String translation = kb.getBoundKeyTranslationKey();
                var key = InputUtil.fromTranslationKey(translation);
                if (key == null) {
                    continue;
                }
                int code = key.getCode();
                if (code < 0) {
                    continue;
                }
                gameBindingNames.computeIfAbsent(code, k -> new ArrayList<>())
                        .add(ReflectionUtils.getGameKeybindDisplayName(kb, this.client.options));
            }
        }
    }

    private void rebuildKeyboardGrid() {
        cells.clear();

        int panelX = keyboardPanelX();
        int availableW = Math.max(170, panelX - 16);
        int x0 = 10;
        int y0 = TOP_BAR_H + 14;
        int kw = 28;
        int kh = 18;
        int gap = 4;

        int[] rowFn = {
                GLFW.GLFW_KEY_ESCAPE,
                GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4,
                GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8,
                GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12
        };
        int[] rowDigits = {
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
                GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0,
                GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_BACKSPACE
        };
        int[] rowTab = {
                GLFW.GLFW_KEY_TAB,
                GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T,
                GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
                GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH
        };
        int[] rowCaps = {
                GLFW.GLFW_KEY_CAPS_LOCK,
                GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G,
                GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
                GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_ENTER
        };
        int[] rowShift = {
                GLFW.GLFW_KEY_LEFT_SHIFT,
                GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B,
                GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD,
                GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_RIGHT_SHIFT
        };

        while (kw > 14 && rowPixelWidth(kw, gap, rowTab) > availableW) {
            kw--;
        }
        while (gap > 1 && rowPixelWidth(kw, gap, rowTab) > availableW) {
            gap--;
        }

        addRow(x0, y0, kw, kh, gap, rowFn);

        addRow(x0, y0 + 28, kw, kh, gap, rowDigits);

        addRow(x0, y0 + 50, kw, kh, gap, rowTab);

        addRow(x0, y0 + 72, kw, kh, gap, rowCaps);

        addRow(x0, y0 + 94, kw, kh, gap, rowShift);

        addRow(x0, y0 + 116, kw, kh, gap,
                GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_SPACE,
                GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_CONTROL,
                GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT);

        addRow(x0, y0 + 138, kw, kh, gap,
                GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,
                GLFW.GLFW_KEY_INSERT, GLFW.GLFW_KEY_DELETE);

        addRow(x0 + 7 * (kw + gap), y0 + 138, kw, kh, gap,
                GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_MOUSE_BUTTON_4, GLFW.GLFW_MOUSE_BUTTON_5);

        boolean placeNumpadRight = panelX >= 700;
        int numpadX = placeNumpadRight ? Math.max(360, panelX - 170) : x0;
        int numpadY = placeNumpadRight ? (y0 + 50) : (y0 + 160);
        addRow(numpadX, numpadY, kw, kh, gap, GLFW.GLFW_KEY_KP_DIVIDE, GLFW.GLFW_KEY_KP_MULTIPLY, GLFW.GLFW_KEY_KP_SUBTRACT);
        addRow(numpadX, numpadY + 22, kw, kh, gap, GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9, GLFW.GLFW_KEY_KP_ADD);
        addRow(numpadX, numpadY + 44, kw, kh, gap, GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6);
        addRow(numpadX, numpadY + 66, kw, kh, gap, GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_ENTER);
        addRow(numpadX, numpadY + 88, kw, kh, gap, GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_DECIMAL);
    }

    private int keyboardPanelWidth() {
        return Math.clamp(this.width - 16, 220, 320);
    }

    private int keyboardPanelX() {
        return Math.max(8, this.width - keyboardPanelWidth() - 8);
    }

    private int rowPixelWidth(int baseWidth, int gap, int... keys) {
        int width = 0;
        for (int i = 0; i < keys.length; i++) {
            width += keyCellWidth(baseWidth, keys[i]);
            if (i < keys.length - 1) {
                width += gap;
            }
        }
        return width;
    }

    private int keyCellWidth(int baseWidth, int key) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            return baseWidth * 4;
        }
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_KP_0) {
            return baseWidth * 2;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_CAPS_LOCK || key == GLFW.GLFW_KEY_ENTER) {
            return baseWidth + 18;
        }
        return baseWidth;
    }

    private void addRow(int xStart, int y, int w, int h, int gap, int... keys) {
        int x = xStart;
        for (int key : keys) {
            int keyW = keyCellWidth(w, key);
            cells.add(new KeyCell(key, MacroWorkbenchCanvasUtils.keyLabel(key), x, y, keyW, h));
            x += keyW + gap;
        }
    }

    private void syncKeyboardFields() {
        if (kbNameField == null || kbCommandsField == null || kbDelayField == null) {
            return;
        }
        MacroDataHandler.MacroEntry macro = selectedMacroId == null ? null : MacroDataHandler.getMacro(selectedMacroId);
        boolean active = macro != null;
        kbNameField.setEditable(active);
        kbCommandsField.setEditable(active);
        kbDelayField.setEditable(active);
        if (!active) {
            kbNameField.setText("");
            kbCommandsField.setText("");
            kbDelayField.setText("");
            return;
        }
        kbNameField.setText(StringUtils.safe(macro.name));
        kbCommandsField.setText(macro.commands == null ? "" : String.join(";", macro.commands));
        kbDelayField.setText(Integer.toString(Math.max(0, macro.delayTicks)));
    }

    private void saveKeyboardMacro() {
        if (selectedMacroId == null) {
            return;
        }
        MacroDataHandler.MacroEntry existing = MacroDataHandler.getMacro(selectedMacroId);
        if (existing == null) {
            return;
        }

        int delay = existing.delayTicks;
        try {
            if (kbDelayField.getText() != null && !kbDelayField.getText().isBlank()) {
                delay = Math.max(0, Integer.parseInt(kbDelayField.getText().trim()));
            }
        } catch (Exception ignored) {
            // keep previous
        }

        List<String> commands = new ArrayList<>();
        String raw = kbCommandsField.getText() == null ? "" : kbCommandsField.getText().trim();
        if (!raw.isBlank()) {
            for (String part : raw.split("[;\\n]+")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    commands.add(t);
                }
            }
        }

        MacroDataHandler.updateMacro(
                selectedMacroId,
                StringUtils.safe(kbNameField.getText()),
                commands,
                existing.keyCode,
                existing.modifierKey,
                delay,
                existing.showInOverlay
        );
        CommandMacros.refreshKeybindings();
        rebuildBindingMaps();
    }

    private void deleteKeyboardMacro() {
        if (selectedMacroId == null) {
            return;
        }
        MacroDataHandler.removeMacro(selectedMacroId);
        selectedMacroId = null;
        CommandMacros.refreshKeybindings();
        rebuildBindingMaps();
        syncKeyboardFields();
    }

    private void createKeyboardMacro() {
        if (selectedKey < 0) {
            return;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        MacroDataHandler.addMacro(id, "New Macro", List.of("/"), selectedKey, "", 0, false);
        selectedMacroId = id;
        CommandMacros.refreshKeybindings();
        rebuildBindingMaps();
        syncKeyboardFields();
    }

    private void saveAll() {
        applyQuickEdit();
        if (advancedOpen) {
            applyAdvancedAndClose();
        }
        saveKeyboardMacro();

        persistExternalCanvasElements();

        MacroHudDataHandler.HudConfig next = MacroHudDataHandler.getConfigCopy();
        next.enabled = this.working.enabled;
        next.activePresetId = StringUtils.safe(this.working.activePresetId).isBlank() ? MacroHudDataHandler.getActivePresetId() : StringUtils.safe(this.working.activePresetId);
        if (next.presetElements == null) {
            next.presetElements = new LinkedHashMap<>();
        }

        List<MacroHudDataHandler.HudElement> filtered = new ArrayList<>();
        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (!isExternalCanvasProxy(element)) {
                filtered.add(element);
            }
        }
        next.presetElements.put(next.activePresetId, new ArrayList<>(filtered));
        next.elements = new ArrayList<>(filtered);
        MacroHudDataHandler.setConfig(next);
        this.working = MacroHudDataHandler.getConfigCopy();
        syncExternalCanvasElementsFromSources();
        syncPresetControls();
    }

    private void syncExternalCanvasElementsFromSources() {
        syncExternalProxy(
                EXTERNAL_SECONDARY_CHAT_ID,
                HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                "Secondary Chat",
                this::defaultSecondaryChatCanvas
        );
        syncExternalProxy(
                EXTERNAL_NBT_INSPECTOR_ID,
                HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR,
                "NBT Inspector",
                this::defaultNbtInspectorCanvas
        );
        syncExternalProxy(
                EXTERNAL_MACRO_KEYBINDS_ID,
                HudCanvasDataHandler.ELEMENT_MACRO_KEYBINDS,
                "Macro Keybinds",
                this::defaultMacroKeybindsCanvas
        );
        syncExternalProxy(
                EXTERNAL_PICKUP_NOTIFIER_ID,
                HudCanvasDataHandler.ELEMENT_PICKUP_NOTIFIER,
                "Pick-up Notifier",
                this::defaultPickupNotifierCanvas
        );
    }

    private void persistExternalCanvasElements() {
        boolean changed = false;
        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (!isExternalCanvasProxy(element)) {
                continue;
            }
            HudCanvasDataHandler.HudCanvasElement external = getMutableExternalCanvasElement(element);
            applyProxyToExternalCanvas(element, external);
            changed = true;
        }
        if (changed) {
            HudCanvasDataHandler.save();
        }
    }

    private boolean isExternalCanvasProxy(MacroHudDataHandler.HudElement element) {
        return isSecondaryChatProxy(element) || isNbtInspectorProxy(element) || isMacroKeybindProxy(element) || isPickupNotifierProxy(element);
    }

    private boolean isSecondaryChatProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_SECONDARY_CHAT_ID.equals(element.id);
    }

    private boolean isNbtInspectorProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_NBT_INSPECTOR_ID.equals(element.id);
    }

    private boolean isMacroKeybindProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_MACRO_KEYBINDS_ID.equals(element.id);
    }

    private boolean isPickupNotifierProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_PICKUP_NOTIFIER_ID.equals(element.id);
    }

    private HudCanvasDataHandler.HudCanvasElement defaultSecondaryChatCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 40;
        e.width = 260;
        e.height = 120;
        e.fontScale = 0.85f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
    }

    private HudCanvasDataHandler.HudCanvasElement defaultNbtInspectorCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 8;
        e.width = 260;
        e.height = 120;
        e.fontScale = 1.0f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_LEFT;
        return e;
    }

    private HudCanvasDataHandler.HudCanvasElement defaultMacroKeybindsCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 12;
        e.y = 12;
        e.width = 240;
        e.height = 120;
        e.fontScale = 0.85f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_RIGHT;
        return e;
    }

    private HudCanvasDataHandler.HudCanvasElement defaultPickupNotifierCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 12;
        e.y = 12;
        e.width = 220;
        e.height = 110;
        e.fontScale = 1.0f;
        e.lineHeight = 18;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFFFFFFF;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_RIGHT;
        e.horizontalAlign = HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        e.verticalAlign = HudCanvasDataHandler.HudCanvasElement.VerticalAlign.BOTTOM;
        return e;
    }

    private void syncExternalProxy(String proxyId,
                                   String hudCanvasId,
                                   String title,
                                   java.util.function.Supplier<HudCanvasDataHandler.HudCanvasElement> defaults) {
        MacroHudDataHandler.HudElement proxy = this.working.elements.stream()
                .filter(e -> e != null && proxyId.equals(e.id))
                .findFirst()
                .orElse(null);
        if (proxy == null) {
            proxy = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.TEXT);
            proxy.id = proxyId;
            proxy.type = MacroHudDataHandler.ElementType.TEXT;
            proxy.zIndex = nextAvailableZIndex();
            this.working.elements.add(proxy);
        }

        HudCanvasDataHandler.HudCanvasElement external = HudCanvasDataHandler.getMutableElement(hudCanvasId, defaults);
        proxy.type = MacroHudDataHandler.ElementType.TEXT;
        proxy.text = title;
        proxy.label = title;
        proxy.anchor = MacroHudDataHandler.Anchor.TOP_LEFT;
        proxy.horizontalAlign = MacroHudDataHandler.HorizontalAlign.LEFT;
        proxy.verticalAlign = MacroHudDataHandler.VerticalAlign.TOP;
        proxy.visibilityMode = MacroHudDataHandler.VisibilityMode.ALWAYS;
        applyExternalCanvasToProxy(external, proxy);

        clampProxyToCanvas(proxy);
    }

    private void applyExternalCanvasToProxy(HudCanvasDataHandler.HudCanvasElement external, MacroHudDataHandler.HudElement proxy) {
        proxy.visible = external.visible;
        proxy.x = external.x;
        proxy.y = external.y;
        proxy.width = external.width;
        proxy.height = external.height;
        proxy.lineHeight = external.lineHeight;
        proxy.fontScale = external.fontScale;
        proxy.zIndex = external.zIndex;
        proxy.backgroundColor = external.backgroundColor;
        proxy.textColor = external.textColor;
        proxy.borderColor = external.borderColor;
        proxy.drawBackground = external.drawBackground;
        proxy.drawBorder = external.drawBorder;
        proxy.anchor = switch (external.anchor) {
            case TOP_CENTER -> MacroHudDataHandler.Anchor.TOP_CENTER;
            case TOP_RIGHT -> MacroHudDataHandler.Anchor.TOP_RIGHT;
            case MIDDLE_LEFT -> MacroHudDataHandler.Anchor.MIDDLE_LEFT;
            case MIDDLE_CENTER -> MacroHudDataHandler.Anchor.MIDDLE_CENTER;
            case MIDDLE_RIGHT -> MacroHudDataHandler.Anchor.MIDDLE_RIGHT;
            case BOTTOM_LEFT -> MacroHudDataHandler.Anchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> MacroHudDataHandler.Anchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> MacroHudDataHandler.Anchor.BOTTOM_RIGHT;
            default -> MacroHudDataHandler.Anchor.TOP_LEFT;
        };
        proxy.horizontalAlign = switch (external.horizontalAlign) {
            case CENTER -> MacroHudDataHandler.HorizontalAlign.CENTER;
            case RIGHT -> MacroHudDataHandler.HorizontalAlign.RIGHT;
            default -> MacroHudDataHandler.HorizontalAlign.LEFT;
        };
        proxy.verticalAlign = switch (external.verticalAlign) {
            case CENTER -> MacroHudDataHandler.VerticalAlign.CENTER;
            case BOTTOM -> MacroHudDataHandler.VerticalAlign.BOTTOM;
            default -> MacroHudDataHandler.VerticalAlign.TOP;
        };
    }

    private void applyProxyToExternalCanvas(MacroHudDataHandler.HudElement proxy, HudCanvasDataHandler.HudCanvasElement external) {
        external.x = proxy.x;
        external.y = proxy.y;
        external.width = proxy.width;
        external.height = proxy.height;
        external.lineHeight = proxy.lineHeight;
        external.fontScale = proxy.fontScale;
        external.zIndex = proxy.zIndex;
        external.backgroundColor = proxy.backgroundColor;
        external.textColor = proxy.textColor;
        external.borderColor = proxy.borderColor;
        external.drawBackground = proxy.drawBackground;
        external.drawBorder = proxy.drawBorder;
        external.visible = proxy.visible;
        external.anchor = switch (proxy.anchor) {
            case TOP_CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_CENTER;
            case TOP_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_RIGHT;
            case MIDDLE_LEFT -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_LEFT;
            case MIDDLE_CENTER, CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_CENTER;
            case MIDDLE_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.MIDDLE_RIGHT;
            case BOTTOM_LEFT -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_LEFT;
            case BOTTOM_CENTER -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_CENTER;
            case BOTTOM_RIGHT -> HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_RIGHT;
            default -> HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_LEFT;
        };
        external.horizontalAlign = switch (proxy.horizontalAlign) {
            case CENTER -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.CENTER;
            case RIGHT -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.RIGHT;
            default -> HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        };
        external.verticalAlign = switch (proxy.verticalAlign) {
            case CENTER -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.CENTER;
            case BOTTOM -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.BOTTOM;
            default -> HudCanvasDataHandler.HudCanvasElement.VerticalAlign.TOP;
        };
    }

    private HudCanvasDataHandler.HudCanvasElement getMutableExternalCanvasElement(MacroHudDataHandler.HudElement proxy) {
        if (isSecondaryChatProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT, this::defaultSecondaryChatCanvas);
        }
        if (isMacroKeybindProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_MACRO_KEYBINDS, this::defaultMacroKeybindsCanvas);
        }
        if (isPickupNotifierProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_PICKUP_NOTIFIER, this::defaultPickupNotifierCanvas);
        }
        return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR, this::defaultNbtInspectorCanvas);
    }

    private List<String> buildExternalProxyPreviewLines(MacroHudDataHandler.HudElement element, String title) {
        ExternalProxyRenderState state = getExternalProxyRenderState(element);
        if (state == ExternalProxyRenderState.MODULE_DISABLED) {
            return List.of(title, "&cUnified HUD element [DISABLED]");
        }
        return List.of(title, "&aUnified HUD element [ACTIVE]");
    }

    private ExternalProxyRenderState getExternalProxyRenderState(MacroHudDataHandler.HudElement element) {
        if (isSecondaryChatProxy(element)) {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            boolean active = settings.enabled && settings.showOverlay;
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
        }
        if (isNbtInspectorProxy(element)) {
            boolean active = NBTInfoHudModule.INSTANCE.isEnabled();
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
        }
        if (isMacroKeybindProxy(element)) {
            boolean active = MacroKeybindHudModule.INSTANCE.isEnabled() && ModConfig.showMacroKeybindOverlay;
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
        }
        if (isPickupNotifierProxy(element)) {
            return ItemPickupNotifierModule.INSTANCE.isEnabled()
                    ? ExternalProxyRenderState.ACTIVE
                    : ExternalProxyRenderState.MODULE_DISABLED;
        }
        return ExternalProxyRenderState.ACTIVE;
    }

    private void clampProxyToCanvas(MacroHudDataHandler.HudElement proxy) {
        if (proxy == null) {
            return;
        }
        proxy.width = Math.clamp(proxy.width, 40, Math.max(40, this.width));
        proxy.height = Math.clamp(proxy.height, 20, Math.max(20, canvasHeightBound()));
        proxy.x = Math.clamp(proxy.x, 0, Math.max(0, this.width - proxy.width));
        proxy.y = Math.clamp(proxy.y, canvasTopBound(), Math.max(canvasTopBound(), canvasBottomBound() - proxy.height));
    }

    private void clampElementToCanvas(MacroHudDataHandler.HudElement element) {
        if (element == null) {
            return;
        }
        element.width = Math.clamp(element.width, 1, Math.max(1, this.width));
        element.height = Math.clamp(element.height, 1, Math.max(1, canvasHeightBound()));
        int screenX = Math.clamp(resolveElementX(element), 0, Math.max(0, this.width - element.width));
        int screenY = Math.clamp(resolveElementY(element), canvasTopBound(),
                Math.max(canvasTopBound(), canvasBottomBound() - element.height));
        setElementScreenPosition(element, screenX, screenY);
    }

    private boolean isAnyCanvasTextFieldFocused() {
        return (quickField != null && quickField.isFocused())
                || (macroField != null && macroField.isFocused())
                || (actionField != null && actionField.isFocused())
                || (presetNameField != null && presetNameField.isFocused());
    }

    private List<MacroHudDataHandler.HudElement> getSelectedElements() {
        if (selectedElementIds.isEmpty() && selected != null) {
            selectedElementIds.add(selected.id);
        }
        List<MacroHudDataHandler.HudElement> out = new ArrayList<>();
        for (MacroHudDataHandler.HudElement e : this.working.elements) {
            if (e != null && selectedElementIds.contains(e.id)) {
                out.add(e);
            }
        }
        return out;
    }

    private MacroHudDataHandler.HudElement findElementById(String id) {
        if (id == null || this.working == null) {
            return null;
        }
        for (MacroHudDataHandler.HudElement e : this.working.elements) {
            if (e != null && id.equals(e.id)) {
                return e;
            }
        }
        return null;
    }

    private List<ElementSnapshot> captureSnapshots(List<MacroHudDataHandler.HudElement> elements) {
        List<ElementSnapshot> snapshots = new ArrayList<>();
        for (MacroHudDataHandler.HudElement e : elements) {
            snapshots.add(new ElementSnapshot(e.id, e.x, e.y, e.width, e.height));
        }
        return snapshots;
    }

    private boolean hasSnapshotChanges(List<ElementSnapshot> snapshots) {
        for (ElementSnapshot snapshot : snapshots) {
            MacroHudDataHandler.HudElement e = findElementById(snapshot.id());
            if (e == null) {
                continue;
            }
            if (e.x != snapshot.x() || e.y != snapshot.y() || e.width != snapshot.width() || e.height != snapshot.height()) {
                return true;
            }
        }
        return false;
    }

    private void undoLastMove() {
        if (moveUndoStack.isEmpty()) {
            return;
        }
        List<ElementSnapshot> snapshots = moveUndoStack.pop();
        for (ElementSnapshot snapshot : snapshots) {
            MacroHudDataHandler.HudElement e = findElementById(snapshot.id());
            if (e == null) {
                continue;
            }
            e.x = snapshot.x();
            e.y = snapshot.y();
            e.width = snapshot.width();
            e.height = snapshot.height();
            clampElementToCanvas(e);
        }
        syncCanvasFields();
    }

    private void copySelectedElements() {
        elementClipboard.clear();
        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            elementClipboard.add(HudElementUtils.cloneElement(e));
        }
    }

    private void pasteElementsFromClipboard() {
        if (elementClipboard.isEmpty()) {
            return;
        }
        List<MacroHudDataHandler.HudElement> pasted = new ArrayList<>();
        for (int i = 0; i < elementClipboard.size(); i++) {
            MacroHudDataHandler.HudElement clone = HudElementUtils.cloneElement(elementClipboard.get(i));
            clone.id = UUID.randomUUID().toString().substring(0, 8);
            int offset = 12 + i * 4;
            clone.x += offset;
            clone.y += offset;
            clampElementToCanvas(clone);
            this.working.elements.add(clone);
            pasted.add(clone);
        }
        selectedElementIds.clear();
        for (MacroHudDataHandler.HudElement e : pasted) {
            selectedElementIds.add(e.id);
        }
        selected = pasted.getLast();
        syncCanvasFields();
    }

    private void copySelectedDimensions() {
        if (selected == null) {
            return;
        }
        clipboardWidth = Math.max(1, selected.width);
        clipboardHeight = Math.max(1, selected.height);
    }

    private void pasteDimensionsToSelection() {
        if (clipboardWidth <= 0 || clipboardHeight <= 0) {
            return;
        }
        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            e.width = clipboardWidth;
            e.height = clipboardHeight;
            clampElementToCanvas(e);
        }
        syncCanvasFields();
    }

    private boolean isShiftDown() {
        if (this.client == null) {
            return false;
        }
        var window = this.client.getWindow();
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isCtrlDown() {
        if (this.client == null) {
            return false;
        }
        var window = this.client.getWindow();
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private int alignedStartX(int x, MacroHudDataHandler.HudElement e, int textWidth, boolean insideBox) {
        int pad = insideBox ? 4 : 0;
        return switch (e.horizontalAlign) {
            case LEFT -> x + pad;
            case RIGHT -> x + Math.max(pad, e.width - textWidth - pad);
            case CENTER -> x + (e.width - textWidth) / 2;
        };
    }

    private int alignedStartY(MacroHudDataHandler.HudElement e, int totalTextHeight, boolean insideBox) {
        int pad = insideBox ? 2 : 0;
        int y = resolveElementY(e);
        return switch (e.verticalAlign) {
            case TOP -> y + pad;
            case BOTTOM -> y + Math.max(pad, e.height - totalTextHeight - pad);
            case CENTER -> y + Math.max(0, (e.height - totalTextHeight) / 2);
        };
    }

    private String expandForCanvas(String raw) {
        try {
            String out = MacroPlaceholders.expandForCanvas(this.client, raw);
            return out == null ? "" : out;
        } catch (Exception ignored) {
            return raw == null ? "" : raw;
        }
    }

    private static boolean contains(int x, int y, MacroHudDataHandler.HudElement e, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + e.width && mouseY >= y && mouseY <= y + e.height;
    }

    private static boolean isResizeHandleHit(int x, int y, MacroHudDataHandler.HudElement e, double mouseX, double mouseY) {
        int handleSize = 8;
        int hx = x + e.width - handleSize;
        int hy = y + e.height - handleSize;
        return containsBox(mouseX, mouseY, hx, hy, handleSize, handleSize);
    }

    private boolean canResize(MacroHudDataHandler.HudElement e, double mouseX, double mouseY) {
        int ex = resolveElementX(e);
        int ey = resolveElementY(e);
        return isResizeHandleHit(ex, ey, e, mouseX, mouseY);
    }


    private int resolveElementX(MacroHudDataHandler.HudElement e) {
        return switch (e.anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> e.x;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> this.width - e.width - e.x;
            case TOP_CENTER, BOTTOM_CENTER, MIDDLE_CENTER, CENTER -> (this.width - e.width) / 2 + e.x;
        };
    }

    private int resolveElementY(MacroHudDataHandler.HudElement e) {
        int canvasBottom = canvasBottomBound();
        int canvasHeight = canvasHeightBound();
        return switch (e.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> e.y;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, CENTER ->
                    canvasTopBound() + (canvasHeight - e.height) / 2 + e.y;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> canvasBottom - e.height - e.y;
        };
    }

    private void setElementScreenPosition(MacroHudDataHandler.HudElement e, int screenX, int screenY) {
        int canvasBottom = canvasBottomBound();
        int canvasHeight = canvasHeightBound();
        e.x = switch (e.anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> screenX;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> this.width - e.width - screenX;
            case TOP_CENTER, BOTTOM_CENTER, MIDDLE_CENTER, CENTER -> screenX - (this.width - e.width) / 2;
        };
        e.y = switch (e.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> screenY;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, CENTER ->
                    screenY - (canvasTopBound() + (canvasHeight - e.height) / 2);
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> canvasBottom - e.height - screenY;
        };
    }

    private int canvasTopBound() {
        return CANVAS_CONTENT_TOP;
    }

    private int canvasBottomBound() {
        return this.height;
    }

    private int canvasHeightBound() {
        return Math.max(1, canvasBottomBound() - canvasTopBound());
    }

    private static boolean hasSelection(int anchor, int cursor) {
        return TextSelectionUtils.hasSelection(anchor, cursor);
    }

    private static int selectionStart(int anchor, int cursor) {
        return TextSelectionUtils.selectionStart(anchor, cursor);
    }

    private static int selectionEnd(int anchor, int cursor) {
        return TextSelectionUtils.selectionEnd(anchor, cursor);
    }

    private static int clampTextIndex(String text, int index) {
        return TextSelectionUtils.clampTextIndex(text, index);
    }

    private static int clampSelectionAnchor(String text, int anchor) {
        return anchor < 0 ? -1 : clampTextIndex(text, anchor);
    }

    private static int[] clampedSelectionRange(String text, int anchor, int cursor) {
        return TextSelectionUtils.clampedSelectionRange(text, anchor, cursor);
    }

    private static String selectedText(String text, int anchor, int cursor) {
        return TextSelectionUtils.selectedText(text, anchor, cursor);
    }

    private static int updateSelectionAnchor(int currentAnchor, int oldCursor, int newCursor, boolean shiftDown) {
        return TextSelectionUtils.updateSelectionAnchor(currentAnchor, oldCursor, newCursor, shiftDown);
    }

    private void beginModalSelectionDrag(ModalDragSelectionField field) {
        this.activeDragSelectionField = field == null ? ModalDragSelectionField.NONE : field;
        this.modalDragStartMouseX = Integer.MIN_VALUE;
        this.modalDragStartMouseY = Integer.MIN_VALUE;
        this.modalDragSelectionStarted = false;
    }

    private void updateModalDragSelection(int mouseX, int mouseY) {
        if (this.client == null || activeDragSelectionField == ModalDragSelectionField.NONE) {
            return;
        }

        if ((activeDragSelectionField == ModalDragSelectionField.ADVANCED_TEXT && !advancedTextFocused)
                || (activeDragSelectionField == ModalDragSelectionField.ADVANCED_ACTION && !advancedActionFocused)
                || (activeDragSelectionField == ModalDragSelectionField.ADVANCED_BG && !advancedBgColorFocused)
                || (activeDragSelectionField == ModalDragSelectionField.ADVANCED_BORDER && !advancedBorderColorFocused)
                || (activeDragSelectionField == ModalDragSelectionField.KB_COMMANDS && !kbCommandsFocused)) {
            activeDragSelectionField = ModalDragSelectionField.NONE;
            return;
        }

        long window = this.client.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            activeDragSelectionField = ModalDragSelectionField.NONE;
            modalDragStartMouseX = Integer.MIN_VALUE;
            modalDragStartMouseY = Integer.MIN_VALUE;
            modalDragSelectionStarted = false;
            return;
        }

        if (modalDragStartMouseX == Integer.MIN_VALUE || modalDragStartMouseY == Integer.MIN_VALUE) {
            modalDragStartMouseX = mouseX;
            modalDragStartMouseY = mouseY;
            return;
        }

        if (!modalDragSelectionStarted) {
            int dx = Math.abs(mouseX - modalDragStartMouseX);
            int dy = Math.abs(mouseY - modalDragStartMouseY);
            if (dx < 2 && dy < 2) {
                return;
            }
            modalDragSelectionStarted = true;
        }

        int boxX = modalX();
        int boxY = modalY();
        switch (activeDragSelectionField) {
            case ADVANCED_TEXT -> {
                int textLeft = boxX + 16;
                int textTop = boxY + 38;
                if (isCustomWidgetType(selected)) {
                    textTop = boxY + 49;
                } else if (isSecondaryChatProxy(selected)) {
                    textTop = boxY + 116;
                }
                int cursor = cursorIndexFromPoint(advancedText, mouseX - textLeft, mouseY - textTop, 9);
                if (advancedSelectionAnchor < 0) {
                    advancedSelectionAnchor = advancedCursor;
                }
                advancedCursor = cursor;
            }
            case ADVANCED_ACTION -> {
                int actionLeft = boxX + 16;
                if (isSecondaryChatProxy(selected)) {
                    actionLeft = boxX + 16;
                }
                int cursor = cursorIndexFromPoint(advancedAction, mouseX - actionLeft, 0, 9);
                if (advancedActionSelectionAnchor < 0) {
                    advancedActionSelectionAnchor = advancedActionCursor;
                }
                advancedActionCursor = Math.clamp(cursor, 0, advancedAction.length());
            }
            case ADVANCED_BG -> {
                int cursor = cursorIndexFromPoint(advancedBgColor, mouseX - (boxX + 242), 0, 9);
                if (advancedBgSelectionAnchor < 0) {
                    advancedBgSelectionAnchor = advancedBgCursor;
                }
                advancedBgCursor = Math.clamp(cursor, 0, advancedBgColor.length());
            }
            case ADVANCED_BORDER -> {
                int cursor = cursorIndexFromPoint(advancedBorderColor, mouseX - (boxX + 360), 0, 9);
                if (advancedBorderSelectionAnchor < 0) {
                    advancedBorderSelectionAnchor = advancedBorderCursor;
                }
                advancedBorderCursor = Math.clamp(cursor, 0, advancedBorderColor.length());
            }
            case KB_COMMANDS -> {
                int cursor = cursorIndexFromPoint(kbCommandsText, mouseX - (boxX + 16), mouseY - (boxY + 38), 9);
                if (kbCommandsSelectionAnchor < 0) {
                    kbCommandsSelectionAnchor = kbCommandsCursor;
                }
                kbCommandsCursor = cursor;
            }
            case NONE -> {
                // no-op
            }
        }
    }

    private void ensureSuggestionVisible(int totalSuggestions, int selectedIndex, int dropY) {
        int rowH = 10;
        int bottomLimit = modalY() + 208;
        int visible = Math.clamp(Math.max(1, (bottomLimit - dropY) / rowH), 1, Math.max(1, totalSuggestions));
        if (selectedIndex < advancedActionSuggestionScroll) {
            advancedActionSuggestionScroll = selectedIndex;
            return;
        }
        int lastVisible = advancedActionSuggestionScroll + visible - 1;
        if (selectedIndex > lastVisible) {
            advancedActionSuggestionScroll = selectedIndex - visible + 1;
        }
    }

    private void drawSingleLineSelection(DrawContext context, int textX, int textY, String text, int anchor, int cursor) {
        GuiTextEditingUtils.drawSingleLineSelection(context, this.textRenderer, textX, textY, text, anchor, cursor);
    }

    private void drawMultilineSelection(DrawContext context, int textX, int textY, int maxY, String text, int anchor, int cursor, int lineHeight) {
        GuiTextEditingUtils.drawMultilineSelection(context, this.textRenderer, textX, textY, maxY, text, anchor, cursor, lineHeight);
    }

    private void drawMultilineSelectionWithScroll(DrawContext context, int textX, int textY, int maxY,
                                                  String text, int anchor, int cursor, int lineHeight, int scrollLines) {
        GuiTextEditingUtils.drawMultilineSelectionWithScroll(context, this.textRenderer, textX, textY, maxY, text, anchor, cursor, lineHeight, scrollLines);
    }

    private static boolean containsBox(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return GuiSystem.contains(x, y, boxX, boxY, boxW, boxH);
    }

    private static boolean containsBox(double x, double y, UiRect rect) {
        return GuiSystem.contains(x, y, rect);
    }

    private static String firstLine(String raw) {
        return StringUtils.firstLine(raw);
    }

    private static List<String> splitLinesRaw(String raw) {
        return StringUtils.splitLinesRaw(raw);
    }

    private static String normalizeMultilineInput(String raw) {
        return StringUtils.normalizeMultilineInput(raw);
    }

    private static String normalizeSingleLineInput(String raw) {
        return StringUtils.normalizeSingleLineInput(raw);
    }

    private static List<String> splitLines(String raw) {
        return splitLinesRaw(raw);
    }

    private int styledLineWidth(String raw) {
        int width = 0;
        for (MacroWorkbenchTextStyling.TextRun run : MacroWorkbenchTextStyling.parseColorRuns(raw, 0xFFFFFFFF)) {
            width += this.textRenderer.getWidth(run.text());
        }
        return width;
    }

    private void drawStyledTextLine(DrawContext context, String raw, int x, int y, int defaultColor, float scale) {
        List<MacroWorkbenchTextStyling.TextRun> runs = MacroWorkbenchTextStyling.parseColorRuns(raw, defaultColor);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int logicalX = Math.round(x / scale);
        int logicalY = Math.round(y / scale);
        int cursor = logicalX;
        for (MacroWorkbenchTextStyling.TextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, run.text(), cursor, logicalY, run.color());
            cursor += this.textRenderer.getWidth(run.text());
        }

        context.getMatrices().popMatrix();
    }

    private boolean shouldHighlightButtonScript() {
        return selected != null
                && selected.type == MacroHudDataHandler.ElementType.BUTTON
                && selected.buttonExecutionMode != MacroHudDataHandler.ButtonExecutionMode.COMMAND;
    }

    private void drawButtonScriptHighlightedLine(DrawContext context, String line, int x, int y) {
        List<ScriptTokenSpan> spans = tokenizeButtonScriptLine(line);
        if (spans.isEmpty()) {
            return;
        }
        int drawX = x;
        for (ScriptTokenSpan span : spans) {
            if (span.end() <= span.start() || span.start() < 0 || span.end() > line.length()) {
                continue;
            }
            String part = line.substring(span.start(), span.end());
            context.drawText(this.textRenderer, part, drawX, y, span.color(), false);
            drawX += this.textRenderer.getWidth(part);
        }
    }

    private static List<ScriptTokenSpan> tokenizeButtonScriptLine(String line) {
        List<ScriptTokenSpan> out = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return out;
        }
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (i + 1 < line.length() && c == '/' && line.charAt(i + 1) == '/') {
                out.add(new ScriptTokenSpan(i, line.length(), 0xFF6FA86F));
                break;
            }
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < line.length()) {
                    if (line.charAt(j) == c && line.charAt(j - 1) != '\\') {
                        j++;
                        break;
                    }
                    j++;
                }
                out.add(new ScriptTokenSpan(i, Math.min(j, line.length()), 0xFFE6C07B));
                i = j;
                continue;
            }
            if (c == '{') {
                int close = line.indexOf('}', i + 1);
                if (close > i + 1 && isScriptPlaceholderToken(line.substring(i + 1, close))) {
                    out.add(new ScriptTokenSpan(i, close + 1, 0xFF56B6C2));
                    i = close + 1;
                    continue;
                }
            }
            if (Character.isDigit(c)) {
                int j = i + 1;
                while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '.')) {
                    j++;
                }
                out.add(new ScriptTokenSpan(i, j, 0xFFD19A66));
                i = j;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < line.length() && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '_')) {
                    j++;
                }
                String token = line.substring(i, j);
                int color = SCRIPT_HIGHLIGHT_KEYWORDS.contains(token) ? 0xFF61AFEF : 0xFFDCDCAA;
                out.add(new ScriptTokenSpan(i, j, color));
                i = j;
                continue;
            }
            out.add(new ScriptTokenSpan(i, i + 1, 0xFFEAEAEA));
            i++;
        }
        return out;
    }

    private static boolean isScriptPlaceholderToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '-')) {
                return false;
            }
        }
        return true;
    }

    private record ScriptTokenSpan(int start, int end, int color) {
    }

    private int cursorIndexFromPoint(String text, int localX, int localY, int lineHeight) {
        return GuiTextEditingUtils.cursorIndexFromPoint(this.textRenderer, text, localX, localY, lineHeight);
    }

    private static int canvasBackgroundColor(MacroHudDataHandler.HudElement element) {
        if (element == null) {
            return 0x00000000;
        }
        int alpha = Math.clamp(element.backgroundAlpha, 0, 255);
        int color = (alpha << 24) | (element.backgroundColor & 0x00FFFFFF);
        if (element.backgroundOpaque) {
            return 0xFF000000 | (color & 0x00FFFFFF);
        }
        return color;
    }

    private static void drawCanvasElementBorder(DrawContext context, MacroHudDataHandler.HudElement element,
                                                int x1, int y1, int x2, int y2) {
        MacroHudDataHandler.BorderMode mode = element == null || element.borderMode == null
                ? MacroHudDataHandler.BorderMode.FULL
                : element.borderMode;
        int color = element == null ? 0xFFFFFFFF : element.borderColor;
        if (mode == MacroHudDataHandler.BorderMode.FULL || mode == MacroHudDataHandler.BorderMode.TOP) {
            context.fill(x1, y1, x2, y1 + 1, color);
        }
        if (mode == MacroHudDataHandler.BorderMode.FULL || mode == MacroHudDataHandler.BorderMode.BOTTOM) {
            context.fill(x1, y2 - 1, x2, y2, color);
        }
        if (mode == MacroHudDataHandler.BorderMode.FULL || mode == MacroHudDataHandler.BorderMode.LEFT) {
            context.fill(x1, y1, x1 + 1, y2, color);
        }
        if (mode == MacroHudDataHandler.BorderMode.FULL || mode == MacroHudDataHandler.BorderMode.RIGHT) {
            context.fill(x2 - 1, y1, x2, y2, color);
        }
    }

    TextRenderer workbenchTextRenderer() {
        return this.textRenderer;
    }

    private List<String> buildMacroKeybindPreviewLines(MacroHudDataHandler.HudElement element) {
        List<String> out = new ArrayList<>();
        String title = expandForCanvas(element.text);
        out.add(title == null || title.isBlank() ? "Macro Keybinds" : title);
        for (MacroDataHandler.MacroEntry macro : MacroDataHandler.getAllMacros().values()) {
            if (macro == null || macro.keyCode < 0) {
                continue;
            }
            String name = macro.name == null || macro.name.isBlank() ? "Unnamed" : macro.name;
            out.add(name + " - [" + MacroWorkbenchCanvasUtils.keyLabel(macro.keyCode) + "]");
        }
        if (out.size() == 1) {
            out.add("(none)");
        }
        return out;
    }

    private ItemStack resolvePreviewIconStack(String kind, String iconId) {
        Identifier id = Identifier.tryParse(StringUtils.safe(iconId));
        if (id == null) {
            return new ItemStack(Items.STONE);
        }
        if ("block".equalsIgnoreCase(kind) && Registries.BLOCK.containsId(id)) {
            return new ItemStack(Registries.BLOCK.get(id).asItem());
        }
        if ("entity".equalsIgnoreCase(kind)) {
            if (Registries.ITEM.containsId(id)) {
                return new ItemStack(Registries.ITEM.get(id));
            }
            Identifier eggId = Identifier.tryParse(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
            if (eggId != null && Registries.ITEM.containsId(eggId)) {
                return new ItemStack(Registries.ITEM.get(eggId));
            }
            return new ItemStack(Items.BARRIER);
        }
        if (Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id));
        }
        return new ItemStack(Items.STONE);
    }

    private void drawCanvasTriangleShape(DrawContext context, int x, int y, int w, int h,
                                         int fillColor, int borderColor, boolean filled,
                                         boolean drawBorder, int thickness) {
        int apexX = x + w / 2;
        int apexY = y;
        int leftX = x;
        int rightX = x + w - 1;
        int baseY = y + h - 1;
        for (int row = 0; row < h; row++) {
            float t = h <= 1 ? 1.0f : (row / (float) (h - 1));
            int rowHalfW = Math.max(0, Math.round((w / 2f) * t));
            int yy = apexY + row;
            int rowLeft = apexX - rowHalfW;
            int rowRight = apexX + rowHalfW;
            if (filled) {
                context.fill(rowLeft, yy, rowRight + 1, yy + 1, fillColor);
            }
            if (drawBorder) {
                context.fill(rowLeft, yy, rowLeft + 1, yy + 1, borderColor);
                context.fill(rowRight, yy, rowRight + 1, yy + 1, borderColor);
            }
        }
        if (drawBorder) {
            for (int i = 0; i < thickness; i++) {
                context.fill(leftX + i, baseY - i, rightX - i + 1, baseY - i + 1, borderColor);
            }
        }
    }

    private void drawCanvasDiamondShape(DrawContext context, int x, int y, int w, int h,
                                        int fillColor, int borderColor, boolean filled,
                                        boolean drawBorder, int thickness) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int ry = Math.max(1, h / 2);
        for (int row = -ry; row <= ry; row++) {
            float t = 1.0f - (Math.abs(row) / (float) ry);
            int halfW = Math.max(0, Math.round((w / 2f) * t));
            int yy = cy + row;
            int left = cx - halfW;
            int right = cx + halfW;
            if (filled) {
                context.fill(left, yy, right + 1, yy + 1, fillColor);
            }
            if (drawBorder) {
                context.fill(left, yy, left + 1, yy + 1, borderColor);
                context.fill(right, yy, right + 1, yy + 1, borderColor);
            }
        }
        if (drawBorder) {
            for (int i = 1; i < Math.max(1, thickness); i++) {
                context.fill(cx - i, y + i, cx + i + 1, y + i + 1, borderColor);
                context.fill(cx - i, y + h - i - 1, cx + i + 1, y + h - i, borderColor);
            }
        }
    }

    private Double resolveCanvasNumericToken(String token) {
        if (this.client == null || token == null || token.isBlank()) {
            return null;
        }
        String expanded = MacroPlaceholders.expandForCanvas(this.client, "{" + token + "}");
        return MacroWorkbenchCanvasUtils.parseFirstDouble(expanded);
    }

    private static int blendColor(int c1, int c2, float t) {
        return ColorUtils.blendColor(c1, c2, t);
    }

    private static MacroHudDataHandler.VisibilityMode cycleVisibilityMode(MacroHudDataHandler.VisibilityMode mode, boolean forward) {
        return MacroWorkbenchUiOps.cycleVisibilityMode(mode, forward);
    }

    private static MacroHudDataHandler.ButtonExecutionMode cycleButtonExecutionMode(MacroHudDataHandler.ButtonExecutionMode mode, boolean forward) {
        return MacroWorkbenchUiOps.cycleButtonExecutionMode(mode, forward);
    }

    private static void ensureVisibleBackground(MacroHudDataHandler.HudElement e) {
        MacroWorkbenchUiOps.ensureVisibleBackground(e);
    }

    private static int cycleStyleColor(int current, boolean forward) {
        return MacroWorkbenchUiOps.cycleStyleColor(current, forward, STYLE_COLOR_PALETTE);
    }


    private int modalX() {
        int defaultX = this.width / 2 - (MODAL_W / 2);
        int x = advancedModalPosX == null ? defaultX : advancedModalPosX;
        return Math.clamp(x, 0, Math.max(0, this.width - MODAL_W));
    }

    private int modalY() {
        int defaultY = this.height / 2 - (MODAL_H / 2);
        int y = advancedModalPosY == null ? defaultY : advancedModalPosY;
        return Math.clamp(y, 0, Math.max(0, this.height - MODAL_H));
    }

    private void drawModalButton(DrawContext context, int x, int y, int w, int h, String label, boolean hovered) {
        GuiSystem.drawButton(context, this.textRenderer, x, y, w, h, label, hovered, true);
    }

    private void drawModalButton(DrawContext context, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        drawModalButton(context, x, y, w, h, label, containsBox(mouseX, mouseY, x, y, w, h));
    }

    private static int lineStart(String text, int index) {
        return StringUtils.lineStart(text, index);
    }

    private static int lineEnd(String text, int index) {
        return StringUtils.lineEnd(text, index);
    }

    private static int moveCursorVertical(String text, int index, int dir) {
        return StringUtils.moveCursorVertical(text, index, dir);
    }

    @Override
    public void close() {
        closeAdvancedModal();
        persistGridPrefs();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}






