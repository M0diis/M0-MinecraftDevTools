package me.m0dii.modules.macros.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandhistory.CommandHistoryManager;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.ProxyAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.SecondaryAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.StandardAdvancedLayout;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.modules.scripting.ScriptStorage;
import me.m0dii.utils.ModConfig;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

public class MacroWorkbenchV2Screen extends Screen {

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
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static boolean GRID_ENABLED_PREF = false;
    private static int GRID_ROWS_PREF = 12;
    private static int GRID_COLS_PREF = 16;
    private static boolean CENTER_LINES_ENABLED_PREF = false;
    private static final int[] STYLE_COLOR_PALETTE = {
            0xAA101010, 0xAA1F1F1F, 0xAA2D1F0E, 0xAA0E2D1F, 0xAA1F0E2D,
            0xCC2A2A2A, 0xCC4A2A2A, 0xCC2A4A2A, 0xCC2A2A4A, 0xCC808080,
            0xFFFFFFFF, 0xFFFFAA00, 0xFFFF5555, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF
    };
    private static final String[] BAR_VALUE_SOURCE_PRESETS = {
            "hp", "max_hp", "food", "saturation", "xp", "level", "client.fps", "players.count", "players.nearby.count"
    };
    private static final String[] LIST_SOURCE_PRESETS = {
            "players.nearby.5.with_distance",
            "players.nearby.8.with_distance.with_direction.nl",
            "players.nearby.8.with_distance.with_direction_arrow.nl",
            "players.nearby.16.r96.with_distance.sort=distance.nl",
            "players.list.other",
            "players.list.other.nl",
            "entities.nearby.6.with_distance",
            "entities.nearby.10.with_distance.with_direction_arrow.nl",
            "entities.nearby.20.r96.unique.with_distance.with_direction.sort=name",
            "players.nearby.10.nl"
    };
    private static final String[] STATE_SOURCE_PRESETS = {
            "player.sprinting", "player.sneaking", "player.swimming", "player.on_ground", "world.is_day", "client.server.singleplayer"
    };
    private static final String[] ICON_KIND_PRESETS = {"item", "block", "entity", "entity_model"};
    private static final String[] SHAPE_TYPE_PRESETS = {"rounded_rect", "rect", "circle", "line", "triangle", "cross", "diamond"};
    private static final String[] SOURCE_TOKEN_SUGGESTIONS = {
            "hp", "max_hp", "food", "saturation", "xp", "level", "client.fps", "player.speed",
            "players.count", "players.nearby.count", "players.nearby.5.with_distance",
            "entities.nearby.6.with_distance", "world.time.clock", "world.is_day", "world.is_night",
            "player.sprinting", "player.sneaking", "player.swimming", "player.on_ground",
            "client.screen.width", "client.screen.height",
            "key.pressed.w", "key.held.w", "key.pressed.space", "key.held.space"
    };

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

    private static List<String> ALL_ITEM_IDS;
    private static List<String> ALL_BLOCK_IDS;
    private static List<String> ALL_ENTITY_IDS;
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

    public MacroWorkbenchV2Screen(Screen parent, Tab initialTab) {
        super(Text.literal("Macro Workbench"));
        this.parent = parent;
        this.tab = initialTab == null ? Tab.CANVAS : initialTab;
    }

    public static Screen create(Screen parent, Tab tab) {
        return new MacroWorkbenchV2Screen(parent, tab);
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

        int delayW = Math.min(120, Math.max(72, panelInnerW / 3));
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
                b -> { CommandHistoryManager.clearHistory(); cmdHistoryItems.clear(); })
                .dimensions(this.width / 2 + 5, this.height - 30, 110, 20).build();
        ButtonWidget clearMsgHistoryButton = ButtonWidget.builder(
                Text.literal("Clear History"),
                b -> { MessageHistoryManager.clearHistory(); msgHistoryItems.clear(); })
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
        context.fill(0, TOP_BAR_H - 1, this.width, TOP_BAR_H, 0x70FFFFFF);

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
                "Press F1 to turn on/off placement mode: " + (canvasChromeVisible ? "OFF" : "ON"),
                8, TOP_BAR_H + 4, 0xFF9FCFCF);
        context.drawTextWithShadow(this.textRenderer,
                "Ctrl+Click multi-select, Ctrl+C/V copy/paste element, Ctrl+Shift+C/V dimensions, Ctrl+Z undo move",
                8, TOP_BAR_H + 16, 0xFF9FBFBF);
        int canvasHeight = Math.max(1, this.height - BOTTOM_BAR_H - CANVAS_CONTENT_TOP);
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

        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (element.visible) {
                drawCanvasElement(context, element);
            }
        }
        drawSnapGuides(context);

        int y = this.height - BOTTOM_BAR_H;
        context.fill(0, y, this.width, this.height, 0xA0000000);
        context.drawTextWithShadow(this.textRenderer, "Quick Edit (first line)", 8, y - 10, 0xFFAAAAAA);
        if (this.macroField.visible) {
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
        this.configurationTabController = new MacroWorkbenchConfigurationTab(this, this.configWidgets, this::isShiftDown, this::isControlPressed);
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
                if (index >= cmdHistoryItems.size()) break;
                String command = cmdHistoryItems.get(index);
                int y = listY + (i * HISTORY_LINE_HEIGHT);
                int boxX1 = 20, boxX2 = width - 20;
                int boxY1 = y - 2, boxY2 = y + HISTORY_LINE_HEIGHT - 2;
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2 && mouseY >= boxY1 && mouseY <= boxY2;
                if (hovering) context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
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
                if (index >= msgHistoryItems.size()) break;
                String message = msgHistoryItems.get(index);
                int y = listY + (i * HISTORY_LINE_HEIGHT);
                int boxX1 = 20, boxX2 = width - 20;
                int boxY1 = y - 2, boxY2 = y + HISTORY_LINE_HEIGHT - 2;
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2 && mouseY >= boxY1 && mouseY <= boxY2;
                if (hovering) context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
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
        if (cmdHistoryItems.isEmpty()) return false;
        int listY = TOP_BAR_H + 8 + 32;
        int maxVisible = Math.min(getHistoryVisibleLines(), cmdHistoryItems.size() - cmdHistoryScroll);
        for (int i = 0; i < maxVisible; i++) {
            int index = i + cmdHistoryScroll;
            if (index >= cmdHistoryItems.size()) break;
            String command = cmdHistoryItems.get(index);
            int y = listY + (i * HISTORY_LINE_HEIGHT);
            if (mouseX >= 20 && mouseX <= width - 20 && mouseY >= y - 2 && mouseY <= y + HISTORY_LINE_HEIGHT - 2) {
                if (client != null) client.keyboard.setClipboard(command);
                return true;
            }
        }
        return false;
    }

    private boolean onMessageHistoryClick(double mouseX, double mouseY) {
        if (msgHistoryItems.isEmpty()) return false;
        int listY = TOP_BAR_H + 8 + 32;
        int maxVisible = Math.min(getHistoryVisibleLines(), msgHistoryItems.size() - msgHistoryScroll);
        for (int i = 0; i < maxVisible; i++) {
            int index = i + msgHistoryScroll;
            if (index >= msgHistoryItems.size()) break;
            String message = msgHistoryItems.get(index);
            int y = listY + (i * HISTORY_LINE_HEIGHT);
            if (mouseX >= 20 && mouseX <= width - 20 && mouseY >= y - 2 && mouseY <= y + HISTORY_LINE_HEIGHT - 2) {
                if (client != null) client.keyboard.setClipboard(message);
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
        context.drawTextWithShadow(this.textRenderer, selectedKey < 0 ? "None" : keyLabel(selectedKey), panelX + 10, TOP_BAR_H + 42, 0xFFFFFF55);

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
                    int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, suggestionsArea.height() / rowH)));
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
            cmdHistoryScroll = Math.clamp((int) (cmdHistoryScroll + (verticalAmount > 0 ? -1 : 1)), 0, maxScroll);
            return true;
        }

        if (this.tab == Tab.ENTITY_RADAR) {
            int maxScroll = Math.max(0, entityRadarItems.size() - getHistoryVisibleLines());
            entityRadarScroll = Math.clamp((int) (entityRadarScroll + (verticalAmount > 0 ? -1 : 1)), 0, maxScroll);
            return true;
        }

        if (this.tab == Tab.MESSAGE_HISTORY) {
            int maxScroll = Math.max(0, msgHistoryItems.size() - getHistoryVisibleLines());
            msgHistoryScroll = Math.clamp((int) (msgHistoryScroll + (verticalAmount > 0 ? -1 : 1)), 0, maxScroll);
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
        if (mouseY > this.height - BOTTOM_BAR_H || mouseY < CANVAS_CONTENT_TOP) {
            dragging = false;
            resizing = false;
            return false;
        }

        boolean ctrl = isCtrlDown();

        for (int i = this.working.elements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement e = this.working.elements.get(i);
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
        if (containsBox(click.x(), click.y(), layout.bgMinus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                ensureVisibleBackground(selected);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, -stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.bgPlus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                ensureVisibleBackground(selected);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alphaMinus())) {
            adjustBackgroundAlpha(selected, -stepInt(8));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.alphaPlus())) {
            adjustBackgroundAlpha(selected, stepInt(8));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.borderMinus())) {
            selected.borderColor = cycleStyleColor(selected.borderColor, false);
            advancedBorderColor = formatColor(selected.borderColor);
            advancedBorderCursor = advancedBorderColor.length();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.borderPlus())) {
            selected.borderColor = cycleStyleColor(selected.borderColor, true);
            advancedBorderColor = formatColor(selected.borderColor);
            advancedBorderCursor = advancedBorderColor.length();
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
        if (containsBox(click.x(), click.y(), layout.bgMinus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, -stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.bgPlus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.bgAlphaMinus())) {
            adjustBackgroundAlpha(selected, -stepInt(8));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.bgAlphaPlus())) {
            adjustBackgroundAlpha(selected, stepInt(8));
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
        if (containsBox(click.x(), click.y(), layout.colorBgMinus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, -stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.colorBgPlus())) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            } else {
                adjustBackgroundAlpha(selected, stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.colorTxMinus())) {
            selected.textColor = cycleStyleColor(selected.textColor, false);
            advancedBorderColor = formatColor(selected.textColor);
            advancedBorderCursor = advancedBorderColor.length();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.colorTxPlus())) {
            selected.textColor = cycleStyleColor(selected.textColor, true);
            advancedBorderColor = formatColor(selected.textColor);
            advancedBorderCursor = advancedBorderColor.length();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.colorAlphaMinus())) {
            adjustBackgroundAlpha(selected, -stepInt(8));
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.colorAlphaPlus())) {
            adjustBackgroundAlpha(selected, stepInt(8));
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
        if (containsBox(click.x(), click.y(), layout.apply())) {
            applyAdvancedAndClose();
            return true;
        }
        if (containsBox(click.x(), click.y(), layout.cancel())) {
            closeAdvancedModal();
            return true;
        }
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
            int clampedY = Math.clamp(resolveElementY(selected), CANVAS_CONTENT_TOP,
                    Math.max(CANVAS_CONTENT_TOP, this.height - BOTTOM_BAR_H - selected.height));
            setElementScreenPosition(selected, clampedX, clampedY);
            return;
        }

        int maxX = this.width - selected.width;
        int maxY = this.height - BOTTOM_BAR_H - selected.height;
        int screenX = Math.clamp(mouseX - dragOffsetX, 0, Math.max(0, maxX));
        int screenY = Math.clamp(mouseY - dragOffsetY, CANVAS_CONTENT_TOP, Math.max(CANVAS_CONTENT_TOP, maxY));

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
        int canvasBottom = this.height - BOTTOM_BAR_H;
        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            int[] start = dragStartScreenPositions.get(e.id);
            if (start == null) {
                continue;
            }
            int minDx = -start[0];
            int maxDx = this.width - e.width - start[0];
            int minDy = CANVAS_CONTENT_TOP - start[1];
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
        int canvasH = Math.max(1, this.height - BOTTOM_BAR_H - CANVAS_CONTENT_TOP);
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
        return new int[]{snappedX, snappedY + CANVAS_CONTENT_TOP, snappedW, snappedH};
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
                CANVAS_CONTENT_TOP,
                this.height - BOTTOM_BAR_H
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
                this.height - BOTTOM_BAR_H
        );
    }

    private void drawSnapGuides(DrawContext context) {
        if (!(dragging && isShiftDown()) && !(resizing && isCtrlDown())) {
            return;
        }
        int canvasBottom = Math.max(CANVAS_CONTENT_TOP + 1, this.height - BOTTOM_BAR_H);
        if (snapGuideX != Integer.MIN_VALUE) {
            context.fill(snapGuideX, CANVAS_CONTENT_TOP, snapGuideX + 1, canvasBottom, 0x90FFD75A);
        }
        if (snapGuideY != Integer.MIN_VALUE) {
            context.fill(0, snapGuideY, this.width, snapGuideY + 1, 0x90FFD75A);
        }
    }

    private void drawGridOverlay(DrawContext context) {
        int canvasTop = CANVAS_CONTENT_TOP;
        int canvasBottom = Math.max(canvasTop + 1, this.height - BOTTOM_BAR_H);
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
        int canvasTop = CANVAS_CONTENT_TOP;
        int canvasBottom = Math.max(canvasTop + 1, this.height - BOTTOM_BAR_H);
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
        if ("entity_model".equalsIgnoreCase(safe(e.iconKind))) {
            drawCanvasPlayerModelPreview(context, e, x1, y1, e.width, e.height);
        } else {
            ItemStack stack = resolvePreviewIconStack(e.iconKind, e.iconId);
            int ix = x1 + Math.max(0, (e.width - 16) / 2);
            int iy = y1 + Math.max(0, (e.height - 16) / 2);
            context.drawItem(stack, ix, iy);
        }
        String label = safe(e.label);
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
        String prefix = preserve(e.prefix);
        if (prefix.isBlank()) {
            String label = safe(e.label);
            if (!label.isBlank() && !"Value".equalsIgnoreCase(label)) {
                prefix = label + ": ";
            }
        }
        String text = prefix + formatCanvasValue(value) + preserve(e.suffix);
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
        String src = MacroPlaceholders.expandForCanvas(this.client, "{" + safe(e.sourceToken) + "}");
        List<String> lines = splitListSourceForCanvas(src);
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

    private void drawCanvasShapePreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        String type = safe(e.shapeType).toLowerCase(Locale.ROOT);
        int w = Math.max(1, x2 - x1);
        int h = Math.max(1, y2 - y1);
        if ("triangle".equals(type)) {
            drawCanvasTriangleShape(context, x1, y1, w, h,
                    e.backgroundColor, e.borderColor,
                    e.shapeFilled || e.drawBackground,
                    e.drawBorder,
                    Math.max(1, e.shapeThickness));
            return;
        }
        if ("diamond".equals(type)) {
            drawCanvasDiamondShape(context, x1, y1, w, h,
                    e.backgroundColor, e.borderColor,
                    e.shapeFilled || e.drawBackground,
                    e.drawBorder,
                    Math.max(1, e.shapeThickness));
            return;
        }
        if ("line".equals(type) || "cross".equals(type)) {
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
        String baseLabel = safe(e.label).isBlank() ? "State" : e.label;
        String label = e.stateShowValue ? (baseLabel + ": " + (on ? safe(e.stateOnText) : safe(e.stateOffText))) : baseLabel;
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
        String active = this.working == null ? "default" : safe(this.working.activePresetId);
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
        String name = safe(presetNameField == null ? null : presetNameField.getText());
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
        String target = safe(presetNameField == null ? null : presetNameField.getText());
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
                ? safe(selected.buttonAction)
                : (selected.type == MacroHudDataHandler.ElementType.TEXT ? safe(selected.text) : safe(selected.label));
        this.advancedAction = selected.type == MacroHudDataHandler.ElementType.BUTTON ? safe(selected.label) : safe(selected.sourceToken);
        this.advancedVisibilityScreenType = safe(selected.visibilityScreenType);
        this.advancedBgColor = formatColor(selected.backgroundColor);
        this.advancedBorderColor = formatColor(selected.borderColor);
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
            this.advancedBgColor = formatColor(selected.backgroundColor);
            this.advancedBorderColor = formatColor(selected.textColor);
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
            this.advancedBgColor = formatColor(selected.backgroundColor);
            this.advancedBorderColor = formatColor(selected.textColor);
            this.advancedBgCursor = this.advancedBgColor.length();
            this.advancedBorderCursor = this.advancedBorderColor.length();
        } else if (isCustomWidgetType(selected)) {
            this.advancedText = safe(selected.label);
            this.advancedAction = selected.type == MacroHudDataHandler.ElementType.ICON
                    ? safe(selected.iconId)
                    : safe(selected.sourceToken);
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
            context.drawTextWithShadow(this.textRenderer, line, textArea.x() + 4, y, 0xFFEAEAEA);
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
        drawModalButton(context, layout.anchor().x(), layout.anchor().y(), layout.anchor().width(), layout.anchor().height(), "Anchor: " + (selected != null ? shortAnchor(selected.anchor) : "-"), layout.anchor().contains(mouseX, mouseY));

        drawModalButton(context, layout.lineMinus().x(), layout.lineMinus().y(), layout.lineMinus().width(), layout.lineMinus().height(), "LH-", layout.lineMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.linePlus().x(), layout.linePlus().y(), layout.linePlus().width(), layout.linePlus().height(), "LH+", layout.linePlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.fontMinus().x(), layout.fontMinus().y(), layout.fontMinus().width(), layout.fontMinus().height(), "FS-", layout.fontMinus().contains(mouseX, mouseY));
        drawModalButton(context, layout.fontPlus().x(), layout.fontPlus().y(), layout.fontPlus().width(), layout.fontPlus().height(), "FS+", layout.fontPlus().contains(mouseX, mouseY));
        drawModalButton(context, layout.visibility().x(), layout.visibility().y(), layout.visibility().width(), layout.visibility().height(), "Visibility: " + (selected != null ? shortVisibility(selected.visibilityMode) : "-"), layout.visibility().contains(mouseX, mouseY));
        if (selected != null && selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            drawModalButton(context, layout.execution().x(), layout.execution().y(), layout.execution().width(), layout.execution().height(),
                    "Exec: " + shortExecutionMode(selected.buttonExecutionMode),
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
        int bgPct = selected == null ? 0 : Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f);
        context.drawTextWithShadow(this.textRenderer,
                "Line: " + (selected != null ? selected.lineHeight : 9) + "   Scale: " + (selected != null ? String.format("%.1f", selected.fontScale) : "1.0") + "   BG: " + bgPct + "%",
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
        int cursor = Math.clamp(cursorIndex, 0, text.length());
        int line = cursorLineIndex(text, cursor);
        int lineStart = lineStart(text, cursor);
        String before = text.substring(lineStart, cursor);
        int x = baseX + this.textRenderer.getWidth(before);
        int y = baseY + Math.max(0, line - Math.max(0, scrollLine)) * 9;
        return new int[]{x, y};
    }

    private static int cursorLineIndex(String text, int cursorIndex) {
        int cursor = Math.clamp(cursorIndex, 0, text.length());
        int line = 0;
        for (int i = 0; i < cursor; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private void renderSecondaryChatAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        SecondaryAdvancedLayout layout = secondaryAdvancedLayout(boxX, boxY);
        context.drawTextWithShadow(this.textRenderer, "Secondary Chat Settings", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Canvas controls position/size. This panel edits behavior.", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, layout.guiOpen().x(), layout.guiOpen().y(), layout.guiOpen().width(), layout.guiOpen().height(),
                "GUI Open: " + (advancedSecondaryShowWhileGuiOpen ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, layout.fadeToggle().x(), layout.fadeToggle().y(), layout.fadeToggle().width(), layout.fadeToggle().height(),
                "Fade: " + (advancedSecondaryFadeEnabled ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, layout.hoverReset().x(), layout.hoverReset().y(), layout.hoverReset().width(), layout.hoverReset().height(),
                "Hover Reset: " + (advancedSecondaryResetTransparencyOnHover ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, layout.noTransparency().x(), layout.noTransparency().y(), layout.noTransparency().width(), layout.noTransparency().height(),
                "No Transparency In Chat: " + (advancedSecondaryNoTransparencyWhenChatOpen ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, layout.mode().x(), layout.mode().y(), layout.mode().width(), layout.mode().height(),
                "Mode: " + (advancedSecondaryInterceptMode == null ? "COPY" : advancedSecondaryInterceptMode.name()), mouseX, mouseY);
        drawModalButton(context, layout.scaleMinus().x(), layout.scaleMinus().y(), layout.scaleMinus().width(), layout.scaleMinus().height(), "S-", mouseX, mouseY);
        drawModalButton(context, layout.scalePlus().x(), layout.scalePlus().y(), layout.scalePlus().width(), layout.scalePlus().height(), "S+", mouseX, mouseY);
        drawModalButton(context, layout.lineMinus().x(), layout.lineMinus().y(), layout.lineMinus().width(), layout.lineMinus().height(), "LH-", mouseX, mouseY);
        drawModalButton(context, layout.linePlus().x(), layout.linePlus().y(), layout.linePlus().width(), layout.linePlus().height(), "LH+", mouseX, mouseY);
        context.drawTextWithShadow(this.textRenderer,
                "Scale: " + String.format("%.2f", advancedSecondaryScale) + "  Line: " + advancedSecondaryLineHeight,
                layout.metricsText().x(), layout.metricsText().y(), 0xFFEAEAEA);

        int regexX = layout.regexInput().x();
        int regexY = layout.regexInput().y();
        int regexW = layout.regexInput().width();
        int regexH = layout.regexInput().height();
        context.drawTextWithShadow(this.textRenderer, "Regex List (one pattern per line)", regexX, regexY - 10, 0xFFB8B8B8);
        context.fill(regexX, regexY, regexX + regexW, regexY + regexH, advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(regexX, regexY, regexX + regexW, regexY + 1, 0x60FFFFFF);
        drawMultilineSelection(context, regexX + 4, regexY + 4, regexY + regexH - 12, advancedText, advancedSelectionAnchor, advancedCursor, 9);
        List<String> regexLines = splitLinesRaw(advancedText);
        int ry = regexY + 4;
        for (String line : regexLines) {
            if (ry > regexY + regexH - 12) {
                break;
            }
            context.drawTextWithShadow(this.textRenderer, line, regexX + 4, ry, 0xFFEAEAEA);
            ry += 9;
        }
        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = cursorPixel(regexX + 4, regexY + 4, advancedText, advancedCursor);
            context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
        }

        int outgoingX = layout.outgoingInput().x();
        int outgoingY = layout.outgoingInput().y();
        int outgoingW = layout.outgoingInput().width();
        int outgoingH = layout.outgoingInput().height();
        context.drawTextWithShadow(this.textRenderer, "Outgoing Regex", outgoingX, outgoingY - 10, 0xFFB8B8B8);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + outgoingH, advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, outgoingX + 4, outgoingY + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, outgoingX + 4, outgoingY + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = outgoingX + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, outgoingY + 4, ax + 1, outgoingY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, layout.fadeMinus().x(), layout.fadeMinus().y(), layout.fadeMinus().width(), layout.fadeMinus().height(), "FD-", mouseX, mouseY);
        drawModalButton(context, layout.fadePlus().x(), layout.fadePlus().y(), layout.fadePlus().width(), layout.fadePlus().height(), "FD+", mouseX, mouseY);
        drawModalButton(context, layout.alphaMinus().x(), layout.alphaMinus().y(), layout.alphaMinus().width(), layout.alphaMinus().height(), "A-", mouseX, mouseY);
        drawModalButton(context, layout.alphaPlus().x(), layout.alphaPlus().y(), layout.alphaPlus().width(), layout.alphaPlus().height(), "A+", mouseX, mouseY);
        drawModalButton(context, layout.linesMinus().x(), layout.linesMinus().y(), layout.linesMinus().width(), layout.linesMinus().height(), "L-", mouseX, mouseY);
        drawModalButton(context, layout.linesPlus().x(), layout.linesPlus().y(), layout.linesPlus().width(), layout.linesPlus().height(), "L+", mouseX, mouseY);
        context.drawTextWithShadow(this.textRenderer,
                "Fade: " + advancedSecondaryFadeDurationMs + "ms  Alpha: " + advancedSecondaryMinAlpha + "  Max: " + advancedSecondaryMaxLines
                        + "  BG: " + Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f) + "%",
                layout.statsText().x(), layout.statsText().y(), 0xFFEAEAEA);

        int secondaryBgHexX = layout.bgHex().x();
        int secondaryBgHexY = layout.bgHex().y();
        int secondaryTxHexX = layout.txHex().x();
        int secondaryTxHexY = layout.txHex().y();
        drawModalButton(context, layout.bgMinus().x(), layout.bgMinus().y(), layout.bgMinus().width(), layout.bgMinus().height(), "BG-", mouseX, mouseY);
        drawModalButton(context, layout.bgPlus().x(), layout.bgPlus().y(), layout.bgPlus().width(), layout.bgPlus().height(), "BG+", mouseX, mouseY);
        drawModalButton(context, layout.bgAlphaMinus().x(), layout.bgAlphaMinus().y(), layout.bgAlphaMinus().width(), layout.bgAlphaMinus().height(), "Opacity-", mouseX, mouseY);
        drawModalButton(context, layout.bgAlphaPlus().x(), layout.bgAlphaPlus().y(), layout.bgAlphaPlus().width(), layout.bgAlphaPlus().height(), "Opacity+", mouseX, mouseY);
        context.drawTextWithShadow(this.textRenderer, "BG", secondaryBgHexX, secondaryBgHexY - 10, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "TX", secondaryTxHexX, secondaryTxHexY - 10, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(secondaryBgHexX, secondaryBgHexY, secondaryBgHexX + 64, secondaryBgHexY + 18, bgInputBg);
        context.fill(secondaryBgHexX, secondaryBgHexY, secondaryBgHexX + 64, secondaryBgHexY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, secondaryBgHexX + 4, secondaryBgHexY + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, secondaryBgHexX + 4, secondaryBgHexY + 5, 0xFFEAEAEA);
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(secondaryTxHexX, secondaryTxHexY, secondaryTxHexX + 64, secondaryTxHexY + 18, txInputBg);
        context.fill(secondaryTxHexX, secondaryTxHexY, secondaryTxHexX + 64, secondaryTxHexY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, secondaryTxHexX + 4, secondaryTxHexY + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, secondaryTxHexX + 4, secondaryTxHexY + 5, 0xFFEAEAEA);

        drawModalButton(context, layout.apply().x(), layout.apply().y(), layout.apply().width(), layout.apply().height(), "Apply", mouseX, mouseY);
        drawModalButton(context, layout.cancel().x(), layout.cancel().y(), layout.cancel().width(), layout.cancel().height(), "Cancel", mouseX, mouseY);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = secondaryBgHexX + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, secondaryBgHexY + 4, cx + 1, secondaryBgHexY + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = secondaryTxHexX + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, secondaryTxHexY + 4, cx + 1, secondaryTxHexY + 13, 0xFFFFFFFF);
        }
    }

    private void renderNbtInspectorAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        String title = isMacroKeybindProxy(selected)
                ? "Macro Keybinds Settings"
                : (isPickupNotifierProxy(selected) ? "Pick-up Notifier Settings" : "NBT Inspector Settings");
        ProxyAdvancedLayout layout = proxyAdvancedLayout(boxX, boxY, isPickupNotifierProxy(selected));
        context.drawTextWithShadow(this.textRenderer, title, boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Use canvas for positioning, this panel for style", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, layout.scaleMinus().x(), layout.scaleMinus().y(), layout.scaleMinus().width(), layout.scaleMinus().height(), "S-", mouseX, mouseY);
        drawModalButton(context, layout.scalePlus().x(), layout.scalePlus().y(), layout.scalePlus().width(), layout.scalePlus().height(), "S+", mouseX, mouseY);
        drawModalButton(context, layout.lineMinus().x(), layout.lineMinus().y(), layout.lineMinus().width(), layout.lineMinus().height(), "LH-", mouseX, mouseY);
        drawModalButton(context, layout.linePlus().x(), layout.linePlus().y(), layout.linePlus().width(), layout.linePlus().height(), "LH+", mouseX, mouseY);
        context.drawTextWithShadow(this.textRenderer,
                "Scale: " + String.format("%.2f", selected == null ? 1.0f : selected.fontScale) +
                        "  Line: " + (selected == null ? 9 : selected.lineHeight)
                        + "  BG: " + (selected == null ? 0 : Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f)) + "%",
                layout.metrics().x(), layout.metrics().y() + 4, 0xFFEAEAEA);

        drawModalButton(context, layout.toggleBg().x(), layout.toggleBg().y(), layout.toggleBg().width(), layout.toggleBg().height(),
                backgroundLabel(selected),
                mouseX, mouseY);
        drawModalButton(context, layout.toggleBorder().x(), layout.toggleBorder().y(), layout.toggleBorder().width(), layout.toggleBorder().height(),
                borderModeLabel(selected),
                mouseX, mouseY);
        drawModalButton(context, layout.toggleVisible().x(), layout.toggleVisible().y(), layout.toggleVisible().width(), layout.toggleVisible().height(),
                "Visible: " + ((selected != null && selected.visible) ? "YES" : "NO"),
                mouseX, mouseY);

        drawModalButton(context, layout.colorBgMinus().x(), layout.colorBgMinus().y(), layout.colorBgMinus().width(), layout.colorBgMinus().height(), "BG-", mouseX, mouseY);
        drawModalButton(context, layout.colorBgPlus().x(), layout.colorBgPlus().y(), layout.colorBgPlus().width(), layout.colorBgPlus().height(), "BG+", mouseX, mouseY);
        drawModalButton(context, layout.colorTxMinus().x(), layout.colorTxMinus().y(), layout.colorTxMinus().width(), layout.colorTxMinus().height(), "TX-", mouseX, mouseY);
        drawModalButton(context, layout.colorTxPlus().x(), layout.colorTxPlus().y(), layout.colorTxPlus().width(), layout.colorTxPlus().height(), "TX+", mouseX, mouseY);
        drawModalButton(context, layout.colorAlphaMinus().x(), layout.colorAlphaMinus().y(), layout.colorAlphaMinus().width(), layout.colorAlphaMinus().height(), "Opacity-", mouseX, mouseY);
        drawModalButton(context, layout.colorAlphaPlus().x(), layout.colorAlphaPlus().y(), layout.colorAlphaPlus().width(), layout.colorAlphaPlus().height(), "Opacity+", mouseX, mouseY);

        drawModalButton(context, layout.alignH().x(), layout.alignH().y(), layout.alignH().width(), layout.alignH().height(),
                "H: " + (selected == null ? "-" : selected.horizontalAlign.name()),
                mouseX, mouseY);
        drawModalButton(context, layout.alignV().x(), layout.alignV().y(), layout.alignV().width(), layout.alignV().height(),
                "V: " + (selected == null ? "-" : selected.verticalAlign.name()),
                mouseX, mouseY);
        drawModalButton(context, layout.anchor().x(), layout.anchor().y(), layout.anchor().width(), layout.anchor().height(),
                "Anchor: " + (selected == null ? "-" : shortAnchor(selected.anchor)),
                mouseX, mouseY);

        int bgInputX = layout.bgInput().x();
        int bgInputY = layout.bgInput().y();
        int bgInputW = layout.bgInput().width();
        int txInputX = layout.txInput().x();
        int txInputY = layout.txInput().y();
        int txInputW = layout.txInput().width();
        context.drawTextWithShadow(this.textRenderer, "BG", bgInputX, bgInputY - 10, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "TX", txInputX, txInputY - 10, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(bgInputX, bgInputY, bgInputX + bgInputW, bgInputY + 18, bgInputBg);
        context.fill(bgInputX, bgInputY, bgInputX + bgInputW, bgInputY + 1, 0x60FFFFFF);
        context.fill(txInputX, txInputY, txInputX + txInputW, txInputY + 18, txInputBg);
        context.fill(txInputX, txInputY, txInputX + txInputW, txInputY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, bgInputX + 4, bgInputY + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        drawSingleLineSelection(context, txInputX + 4, txInputY + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, bgInputX + 4, bgInputY + 5, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, txInputX + 4, txInputY + 5, 0xFFEAEAEA);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = bgInputX + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, bgInputY + 4, cx + 1, bgInputY + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = txInputX + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, txInputY + 4, cx + 1, txInputY + 13, 0xFFFFFFFF);
        }

        if (isPickupNotifierProxy(selected)) {
            PickupFeedSettings.Data pickup = PickupFeedSettings.get();
            drawModalButton(context, layout.pickupDuration().x(), layout.pickupDuration().y(), layout.pickupDuration().width(), layout.pickupDuration().height(), "Duration", mouseX, mouseY);
            drawModalButton(context, layout.pickupLines().x(), layout.pickupLines().y(), layout.pickupLines().width(), layout.pickupLines().height(), "Lines", mouseX, mouseY);
            drawModalButton(context, layout.pickupIcon().x(), layout.pickupIcon().y(), layout.pickupIcon().width(), layout.pickupIcon().height(), "Icon", mouseX, mouseY);
            drawModalButton(context, layout.pickupDirection().x(), layout.pickupDirection().y(), layout.pickupDirection().width(), layout.pickupDirection().height(), "Direction", mouseX, mouseY);
            context.drawTextWithShadow(this.textRenderer,
                    "Dur: " + pickup.durationMs + "ms  Max: " + pickup.maxLines + "  Scale: "
                            + String.format(Locale.ROOT, "%.2f", pickup.iconScale) + "  Dir: " + pickup.direction.name(),
                    layout.pickupInfo().x(), layout.pickupInfo().y(), 0xFFEAEAEA);
        }

        drawModalButton(context, layout.apply().x(), layout.apply().y(), layout.apply().width(), layout.apply().height(), "Apply", mouseX, mouseY);
        drawModalButton(context, layout.cancel().x(), layout.cancel().y(), layout.cancel().width(), layout.cancel().height(), "Cancel", mouseX, mouseY);
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
        UiRect labelInput = layout.labelInput();
        UiRect sourceInput = layout.sourceInput();
        UiRect suggestionsArea = layout.suggestionArea();
        List<UiRect> baseRow = layout.baseRow();
        List<UiRect> generalRow1 = layout.generalRow1();
        List<UiRect> generalRow2 = layout.generalRow2();

        context.drawTextWithShadow(this.textRenderer, selected.type + " Widget", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON
                        ? "Label + icon id + type-specific controls"
                        : "Label + source token + type-specific controls",
                boxX + 12, boxY + 24, 0xFFB0B0B0);

        context.drawTextWithShadow(this.textRenderer, "Label", labelInput.x(), labelInput.y() - 10, 0xFFB8B8B8);
        context.fill(labelInput.x(), labelInput.y(), labelInput.right(), labelInput.bottom(), advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(labelInput.x(), labelInput.y(), labelInput.right(), labelInput.y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, labelInput.x() + 4, labelInput.y() + 5, advancedText, advancedSelectionAnchor, advancedCursor);
        context.drawTextWithShadow(this.textRenderer, advancedText, labelInput.x() + 4, labelInput.y() + 5, 0xFFEAEAEA);

        context.drawTextWithShadow(this.textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON ? "Icon id" : "Source token",
                sourceInput.x(), sourceInput.y() - 10, 0xFFB8B8B8);
        context.fill(sourceInput.x(), sourceInput.y(), sourceInput.right(), sourceInput.bottom(), advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(sourceInput.x(), sourceInput.y(), sourceInput.right(), sourceInput.y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, sourceInput.x() + 4, sourceInput.y() + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, sourceInput.x() + 4, sourceInput.y() + 5, 0xFFEAEAEA);
        List<String> suggestions = advancedActionSuggestions();
        if (!suggestions.isEmpty()) {
            int dropX = suggestionsArea.x();
            int dropY = suggestionsArea.y();
            int dropW = suggestionsArea.width();
            int rowH = 10;
            int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, suggestionsArea.height() / rowH)));
            int maxScroll = Math.max(0, suggestions.size() - maxVisible);
            advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll, 0, maxScroll);
            context.fill(dropX, dropY, dropX + dropW, dropY + maxVisible * rowH, 0xC0101010);
            for (int i = 0; i < maxVisible; i++) {
                int idx = advancedActionSuggestionScroll + i;
                String token = suggestions.get(idx);
                int yy = dropY + i * rowH;
                boolean selectedSuggestion = (advancedActionSuggestionIndex == idx);
                if (selectedSuggestion) {
                    context.fill(dropX, yy, dropX + dropW, yy + rowH, 0x503777AA);
                }
                int color = isSuggestionHeader(token) ? 0xFFB8D8FF : 0xFF8FC8FF;
                context.drawTextWithShadow(this.textRenderer, token, dropX + 3, yy + 1, color);
            }
            if (suggestions.size() > maxVisible) {
                context.drawTextWithShadow(this.textRenderer,
                            "scroll " + (advancedActionSuggestionScroll + 1) + "/" + (maxScroll + 1),
                        dropX + 3, dropY + maxVisible * rowH + 2, 0xFF909090);
            }
        }

        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = labelInput.x() + 4 + this.textRenderer.getWidth(advancedText.substring(0, Math.clamp(advancedCursor, 0, advancedText.length())));
            context.fill(cx, labelInput.y() + 4, cx + 1, labelInput.y() + 13, 0xFFFFFFFF);
        }
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = sourceInput.x() + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(cx, sourceInput.y() + 4, cx + 1, sourceInput.y() + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, slot(generalRow1, 0).x(), slot(generalRow1, 0).y(), slot(generalRow1, 0).width(), slot(generalRow1, 0).height(), "BG-", slot(generalRow1, 0).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow1, 1).x(), slot(generalRow1, 1).y(), slot(generalRow1, 1).width(), slot(generalRow1, 1).height(), "BG+", slot(generalRow1, 1).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow1, 2).x(), slot(generalRow1, 2).y(), slot(generalRow1, 2).width(), slot(generalRow1, 2).height(), "BR-", slot(generalRow1, 2).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow1, 3).x(), slot(generalRow1, 3).y(), slot(generalRow1, 3).width(), slot(generalRow1, 3).height(), "BR+", slot(generalRow1, 3).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow2, 0).x(), slot(generalRow2, 0).y(), slot(generalRow2, 0).width(), slot(generalRow2, 0).height(), "FS-", slot(generalRow2, 0).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow2, 1).x(), slot(generalRow2, 1).y(), slot(generalRow2, 1).width(), slot(generalRow2, 1).height(), "FS+", slot(generalRow2, 1).contains(mouseX, mouseY));
        drawModalButton(context, slot(generalRow2, 2).x(), slot(generalRow2, 2).y(), slot(generalRow2, 2).width(), slot(generalRow2, 2).height(), "Pick Src", slot(generalRow2, 2).contains(mouseX, mouseY));
        int bgPct = Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f);
        context.drawTextWithShadow(this.textRenderer,
                "Scale: " + String.format(Locale.ROOT, "%.2f", selected.fontScale) + "  BG Alpha: " + bgPct + "%",
                layout.metricsText().x(), layout.metricsText().y(), 0xFFEAEAEA);

        UiRect typeHint = layout.typeHintText();
        context.drawTextWithShadow(this.textRenderer, "Type options", typeHint.x(), typeHint.y(), 0xFFB8B8B8);

        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            List<UiRect> topButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH, 
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(60, 1)
            );
            drawModalButton(context, topButtons.get(0).x(), topButtons.get(0).y(), topButtons.get(0).width(), topButtons.get(0).height(), "Kind: " + selected.iconKind, topButtons.get(0).contains(mouseX, mouseY));
            drawModalButton(context, topButtons.get(1).x(), topButtons.get(1).y(), topButtons.get(1).width(), topButtons.get(1).height(), "Pick Id", topButtons.get(1).contains(mouseX, mouseY));
            if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
                List<UiRect> row1 = layout.typeRow1();
                List<UiRect> row2 = layout.typeRow2();
                List<UiRect> row3 = layout.typeRow3();
                drawModalButton(context, row1.get(0).x(), row1.get(0).y(), row1.get(0).width(), row1.get(0).height(), "Z-", row1.get(0).contains(mouseX, mouseY));
                drawModalButton(context, row1.get(1).x(), row1.get(1).y(), row1.get(1).width(), row1.get(1).height(), "Z+", row1.get(1).contains(mouseX, mouseY));
                drawModalButton(context, row1.get(2).x(), row1.get(2).y(), row1.get(2).width(), row1.get(2).height(), "Y-", row1.get(2).contains(mouseX, mouseY));
                drawModalButton(context, row1.get(3).x(), row1.get(3).y(), row1.get(3).width(), row1.get(3).height(), "Y+", row1.get(3).contains(mouseX, mouseY));
                drawModalButton(context, row2.get(0).x(), row2.get(0).y(), row2.get(0).width(), row2.get(0).height(), "P-", row2.get(0).contains(mouseX, mouseY));
                drawModalButton(context, row2.get(1).x(), row2.get(1).y(), row2.get(1).width(), row2.get(1).height(), "P+", row2.get(1).contains(mouseX, mouseY));
                drawModalButton(context, row2.get(2).x(), row2.get(2).y(), row2.get(2).width(), row2.get(2).height(), "OX-", row2.get(2).contains(mouseX, mouseY));
                drawModalButton(context, row2.get(3).x(), row2.get(3).y(), row2.get(3).width(), row2.get(3).height(), "OX+", row2.get(3).contains(mouseX, mouseY));
                drawModalButton(context, row3.get(0).x(), row3.get(0).y(), row3.get(0).width(), row3.get(0).height(), "OY-", row3.get(0).contains(mouseX, mouseY));
                drawModalButton(context, row3.get(1).x(), row3.get(1).y(), row3.get(1).width(), row3.get(1).height(), "OY+", row3.get(1).contains(mouseX, mouseY));
                drawModalButton(context, row3.get(2).x(), row3.get(2).y(), row3.get(2).width(), row3.get(2).height(), "Fit: " + (selected.modelAutoFit ? "ON" : "OFF"), row3.get(2).contains(mouseX, mouseY));
                drawModalButton(context, row3.get(3).x(), row3.get(3).y(), row3.get(3).width(), row3.get(3).height(), "Look: " + (selected.modelFollowLook ? "ON" : "OFF"), row3.get(3).contains(mouseX, mouseY));
                context.drawTextWithShadow(this.textRenderer, "Id: " + selected.iconId, layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFEAEAEA);
                context.drawTextWithShadow(this.textRenderer,
                        String.format(Locale.ROOT, "Zoom %.2f  Yaw %.0f  Pitch %.0f", selected.modelZoom, selected.modelYaw, selected.modelPitch),
                        layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
                context.drawTextWithShadow(this.textRenderer,
                        "Offset X: " + selected.modelOffsetX + "  Y: " + selected.modelOffsetY,
                        layout.typeInfo2().x(), layout.typeInfo2().y(), 0xFFEAEAEA);
            } else {
                List<UiRect> row1 = layout.typeRow1();
                drawModalButton(context, row1.get(0).x(), row1.get(0).y(), row1.get(0).width(), row1.get(0).height(), "Count: " + (selected.iconShowCount ? "ON" : "OFF"), row1.get(0).contains(mouseX, mouseY));
                drawModalButton(context, row1.get(1).x(), row1.get(1).y(), row1.get(1).width(), row1.get(1).height(), "Dur: " + (selected.iconShowDurability ? "ON" : "OFF"), row1.get(1).contains(mouseX, mouseY));
                drawModalButton(context, row1.get(2).x(), row1.get(2).y(), row1.get(2).width(), row1.get(2).height(), "CD: " + (selected.iconShowCooldown ? "ON" : "OFF"), row1.get(2).contains(mouseX, mouseY));
                context.drawTextWithShadow(this.textRenderer, "Id: " + selected.iconId, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            List<UiRect> top = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH, 
                    UiFlexLayout.Item.flex(110, 2), UiFlexLayout.Item.flex(60, 1)
            );
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            drawModalButton(context, top.get(0).x(), top.get(0).y(), top.get(0).width(), top.get(0).height(), "Max Src: " + (safe(selected.sourceTokenMax).isBlank() ? "(none)" : selected.sourceTokenMax), top.get(0).contains(mouseX, mouseY));
            drawModalButton(context, top.get(1).x(), top.get(1).y(), top.get(1).width(), top.get(1).height(), "Segmented: " + (selected.segmented ? "ON" : "OFF"), top.get(1).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(0).x(), row1.get(0).y(), row1.get(0).width(), row1.get(0).height(), "R-", row1.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(1).x(), row1.get(1).y(), row1.get(1).width(), row1.get(1).height(), "R+", row1.get(1).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(2).x(), row1.get(2).y(), row1.get(2).width(), row1.get(2).height(), "MIN-", row1.get(2).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(3).x(), row1.get(3).y(), row1.get(3).width(), row1.get(3).height(), "MIN+", row1.get(3).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(0).x(), row2.get(0).y(), row2.get(0).width(), row2.get(0).height(), "MAX-", row2.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(1).x(), row2.get(1).y(), row2.get(1).width(), row2.get(1).height(), "MAX+", row2.get(1).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(2).x(), row2.get(2).y(), row2.get(2).width(), row2.get(2).height(), "C1-", row2.get(2).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(3).x(), row2.get(3).y(), row2.get(3).width(), row2.get(3).height(), "C1+", row2.get(3).contains(mouseX, mouseY));
            drawModalButton(context, row3.get(0).x(), row3.get(0).y(), row3.get(0).width(), row3.get(0).height(), "C2-", row3.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row3.get(1).x(), row3.get(1).y(), row3.get(1).width(), row3.get(1).height(), "C2+", row3.get(1).contains(mouseX, mouseY));
            context.drawTextWithShadow(this.textRenderer, "Range: " + String.format(Locale.ROOT, "%.1f..%.1f", selected.minValue, selected.maxValue) + "  Segments: " + selected.segments, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);

            UiRect rangeInput = layout.typeInputLeft();
            UiRect segInput = layout.typeInputRight();
            context.drawTextWithShadow(this.textRenderer, "Range (min,max)", rangeInput.x(), rangeInput.y() - 10, 0xFFB8B8B8);
            context.fill(rangeInput.x(), rangeInput.y(), rangeInput.right(), rangeInput.bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(rangeInput.x(), rangeInput.y(), rangeInput.right(), rangeInput.y() + 1, 0x60FFFFFF);
            context.drawTextWithShadow(this.textRenderer, advancedBgColor, rangeInput.x() + 4, rangeInput.y() + 5, 0xFFEAEAEA);

            context.drawTextWithShadow(this.textRenderer, "Segments", segInput.x(), segInput.y() - 10, 0xFFB8B8B8);
            context.fill(segInput.x(), segInput.y(), segInput.right(), segInput.bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(segInput.x(), segInput.y(), segInput.right(), segInput.y() + 1, 0x60FFFFFF);
            context.drawTextWithShadow(this.textRenderer, advancedBorderColor, segInput.x() + 4, segInput.y() + 5, 0xFFEAEAEA);

            if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = rangeInput.x() + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
                context.fill(cx, rangeInput.y() + 4, cx + 1, rangeInput.y() + 13, 0xFFFFFFFF);
            }
            if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = segInput.x() + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
                context.fill(cx, segInput.y() + 4, cx + 1, segInput.y() + 13, 0xFFFFFFFF);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            List<UiRect> presets = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH, 
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
            );
            drawModalButton(context, slot(row1, 0).x(), slot(row1, 0).y(), slot(row1, 0).width(), slot(row1, 0).height(), "WRN-", slot(row1, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 1).x(), slot(row1, 1).y(), slot(row1, 1).width(), slot(row1, 1).height(), "WRN+", slot(row1, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 2).x(), slot(row1, 2).y(), slot(row1, 2).width(), slot(row1, 2).height(), "CRT-", slot(row1, 2).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 3).x(), slot(row1, 3).y(), slot(row1, 3).width(), slot(row1, 3).height(), "CRT+", slot(row1, 3).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 0).x(), slot(row2, 0).y(), slot(row2, 0).width(), slot(row2, 0).height(), "WarnClr-", slot(row2, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 1).x(), slot(row2, 1).y(), slot(row2, 1).width(), slot(row2, 1).height(), "WarnClr+", slot(row2, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 2).x(), slot(row2, 2).y(), slot(row2, 2).width(), slot(row2, 2).height(), "CritClr-", slot(row2, 2).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 3).x(), slot(row2, 3).y(), slot(row2, 3).width(), slot(row2, 3).height(), "CritClr+", slot(row2, 3).contains(mouseX, mouseY));
            drawModalButton(context, slot(row3, 0).x(), slot(row3, 0).y(), slot(row3, 0).width(), slot(row3, 0).height(), "BG-", slot(row3, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(row3, 1).x(), slot(row3, 1).y(), slot(row3, 1).width(), slot(row3, 1).height(), "BG+", slot(row3, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row3, 2).x(), slot(row3, 2).y(), slot(row3, 2).width(), slot(row3, 2).height(), "TX-", slot(row3, 2).contains(mouseX, mouseY));
            drawModalButton(context, slot(row3, 3).x(), slot(row3, 3).y(), slot(row3, 3).width(), slot(row3, 3).height(), "TX+", slot(row3, 3).contains(mouseX, mouseY));
            drawModalButton(context, slot(presets, 0).x(), slot(presets, 0).y(), slot(presets, 0).width(), slot(presets, 0).height(), "Prefix preset", slot(presets, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(presets, 1).x(), slot(presets, 1).y(), slot(presets, 1).width(), slot(presets, 1).height(), "Suffix preset", slot(presets, 1).contains(mouseX, mouseY));
            context.drawTextWithShadow(this.textRenderer, "Warn/Crit: " + String.format(Locale.ROOT, "%.1f / %.1f", selected.warnThreshold, selected.critThreshold), layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
            context.drawTextWithShadow(this.textRenderer, "Tip: right-click Warn/Crit/BG/TX color buttons to open picker", layout.typeInfo2().x(), layout.typeInfo2().y(), 0xFF98B8D8);
            context.drawTextWithShadow(this.textRenderer, "Prefix", layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFB8B8B8);
            context.drawTextWithShadow(this.textRenderer, "Suffix", layout.typeInputRight().x(), layout.typeInputRight().y() - 10, 0xFFB8B8B8);
            context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().y() + 1, 0x60FFFFFF);
            context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().y() + 1, 0x60FFFFFF);
            drawSingleLineSelection(context, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
            drawSingleLineSelection(context, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
            context.drawTextWithShadow(this.textRenderer, advancedBgColor, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, 0xFFEAEAEA);
            context.drawTextWithShadow(this.textRenderer, advancedBorderColor, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, 0xFFEAEAEA);
            if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = layout.typeInputLeft().x() + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
                context.fill(cx, layout.typeInputLeft().y() + 4, cx + 1, layout.typeInputLeft().y() + 13, 0xFFFFFFFF);
            }
            if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = layout.typeInputRight().x() + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
                context.fill(cx, layout.typeInputRight().y() + 4, cx + 1, layout.typeInputRight().y() + 13, 0xFFFFFFFF);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            drawModalButton(context, layout.typeWideTop().x(), layout.typeWideTop().y(), layout.typeWideTop().width(), layout.typeWideTop().height(), "List source preset", layout.typeWideTop().contains(mouseX, mouseY));
            List<UiRect> row1 = layout.typeRow1();
            drawModalButton(context, row1.get(0).x(), row1.get(0).y(), row1.get(0).width(), row1.get(0).height(), "L-", row1.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(1).x(), row1.get(1).y(), row1.get(1).width(), row1.get(1).height(), "L+", row1.get(1).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(2).x(), row1.get(2).y(), row1.get(2).width(), row1.get(2).height(), "S-", row1.get(2).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(3).x(), row1.get(3).y(), row1.get(3).width(), row1.get(3).height(), "S+", row1.get(3).contains(mouseX, mouseY));
            context.drawTextWithShadow(this.textRenderer, "Max lines: " + selected.maxLines + "  Scroll: " + selected.listScroll, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
        } else if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            drawModalButton(context, layout.typeWideTop().x(), layout.typeWideTop().y(), layout.typeWideTop().width(), layout.typeWideTop().height(), "Type: " + selected.shapeType, layout.typeWideTop().contains(mouseX, mouseY));
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            drawModalButton(context, row1.get(0).x(), row1.get(0).y(), row1.get(0).width(), row1.get(0).height(), "Filled: " + (selected.shapeFilled ? "ON" : "OFF"), row1.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(1).x(), row1.get(1).y(), row1.get(1).width(), row1.get(1).height(), "R-", row1.get(1).contains(mouseX, mouseY));
            drawModalButton(context, row1.get(2).x(), row1.get(2).y(), row1.get(2).width(), row1.get(2).height(), "R+", row1.get(2).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(0).x(), row2.get(0).y(), row2.get(0).width(), row2.get(0).height(), "T-", row2.get(0).contains(mouseX, mouseY));
            drawModalButton(context, row2.get(1).x(), row2.get(1).y(), row2.get(1).width(), row2.get(1).height(), "T+", row2.get(1).contains(mouseX, mouseY));
            context.drawTextWithShadow(this.textRenderer, "Radius: " + selected.shapeRadius + "  Thickness: " + selected.shapeThickness, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
        } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            List<UiRect> stateText = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH, 
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
            );
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            drawModalButton(context, slot(stateText, 0).x(), slot(stateText, 0).y(), slot(stateText, 0).width(), slot(stateText, 0).height(), "ON: " + selected.stateOnText, slot(stateText, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(stateText, 1).x(), slot(stateText, 1).y(), slot(stateText, 1).width(), slot(stateText, 1).height(), "OFF: " + selected.stateOffText, slot(stateText, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 0).x(), slot(row1, 0).y(), slot(row1, 0).width(), slot(row1, 0).height(), "ON-", slot(row1, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 1).x(), slot(row1, 1).y(), slot(row1, 1).width(), slot(row1, 1).height(), "ON+", slot(row1, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 2).x(), slot(row1, 2).y(), slot(row1, 2).width(), slot(row1, 2).height(), "OFF-", slot(row1, 2).contains(mouseX, mouseY));
            drawModalButton(context, slot(row1, 3).x(), slot(row1, 3).y(), slot(row1, 3).width(), slot(row1, 3).height(), "OFF+", slot(row1, 3).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 0).x(), slot(row2, 0).y(), slot(row2, 0).width(), slot(row2, 0).height(), "Show Value: " + (selected.stateShowValue ? "ON" : "OFF"), slot(row2, 0).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 1).x(), slot(row2, 1).y(), slot(row2, 1).width(), slot(row2, 1).height(), "TX-", slot(row2, 1).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 2).x(), slot(row2, 2).y(), slot(row2, 2).width(), slot(row2, 2).height(), "TX+", slot(row2, 2).contains(mouseX, mouseY));
            drawModalButton(context, slot(row2, 3).x(), slot(row2, 3).y(), slot(row2, 3).width(), slot(row2, 3).height(), "Src preset", slot(row2, 3).contains(mouseX, mouseY));
            context.drawTextWithShadow(this.textRenderer, "True tokens (csv)", layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFB8B8B8);
            context.drawTextWithShadow(this.textRenderer, "False tokens (csv)", layout.typeInputRight().x(), layout.typeInputRight().y() - 10, 0xFFB8B8B8);
            context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().y() + 1, 0x60FFFFFF);
            context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().y() + 1, 0x60FFFFFF);
            drawSingleLineSelection(context, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
            drawSingleLineSelection(context, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
            context.drawTextWithShadow(this.textRenderer, advancedBgColor, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, 0xFFEAEAEA);
            context.drawTextWithShadow(this.textRenderer, advancedBorderColor, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, 0xFFEAEAEA);
            if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = layout.typeInputLeft().x() + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
                context.fill(cx, layout.typeInputLeft().y() + 4, cx + 1, layout.typeInputLeft().y() + 13, 0xFFFFFFFF);
            }
            if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = layout.typeInputRight().x() + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
                context.fill(cx, layout.typeInputRight().y() + 4, cx + 1, layout.typeInputRight().y() + 13, 0xFFFFFFFF);
            }
        }

        drawModalButton(context, slot(baseRow, 0).x(), slot(baseRow, 0).y(), slot(baseRow, 0).width(), slot(baseRow, 0).height(), "H: " + selected.horizontalAlign.name(), slot(baseRow, 0).contains(mouseX, mouseY));
        drawModalButton(context, slot(baseRow, 1).x(), slot(baseRow, 1).y(), slot(baseRow, 1).width(), slot(baseRow, 1).height(), "V: " + selected.verticalAlign.name(), slot(baseRow, 1).contains(mouseX, mouseY));
        drawModalButton(context, slot(baseRow, 2).x(), slot(baseRow, 2).y(), slot(baseRow, 2).width(), slot(baseRow, 2).height(), "Anchor: " + shortAnchor(selected.anchor), slot(baseRow, 2).contains(mouseX, mouseY));
        drawModalButton(context, slot(baseRow, 3).x(), slot(baseRow, 3).y(), slot(baseRow, 3).width(), slot(baseRow, 3).height(), backgroundLabel(selected), slot(baseRow, 3).contains(mouseX, mouseY));
        drawModalButton(context, slot(baseRow, 4).x(), slot(baseRow, 4).y(), slot(baseRow, 4).width(), slot(baseRow, 4).height(), borderModeLabel(selected), slot(baseRow, 4).contains(mouseX, mouseY));

        drawModalButton(context, layout.apply().x(), layout.apply().y(), layout.apply().width(), layout.apply().height(), "Apply", layout.apply().contains(mouseX, mouseY));
        drawModalButton(context, layout.cancel().x(), layout.cancel().y(), layout.cancel().width(), layout.cancel().height(), "Cancel", layout.cancel().contains(mouseX, mouseY));
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
        if (suggestions.isEmpty()) {
            return false;
        }
        int dropX = suggestionsArea.x();
        int dropY = suggestionsArea.y();
        int dropW = suggestionsArea.width();
        int rowH = 10;
        int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, suggestionsArea.height() / rowH)));
        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
        advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll, 0, maxScroll);
        for (int i = 0; i < maxVisible; i++) {
            int yy = dropY + i * rowH;
            if (containsBox(click.x(), click.y(), dropX, yy, dropW, rowH)) {
                int idx = advancedActionSuggestionScroll + i;
                if (idx >= 0 && idx < suggestions.size() && !isSuggestionHeader(suggestions.get(idx))) {
                    advancedAction = suggestionValue(suggestions.get(idx));
                    advancedActionCursor = advancedAction.length();
                    advancedActionSuggestionIndex = idx;
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleCustomWidgetGeneralRowClick(Click click, List<UiRect> generalRow1, List<UiRect> generalRow2, boolean forward) {
        if (containsBox(click.x(), click.y(), slot(generalRow1, 0))) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
            } else {
                adjustBackgroundAlpha(selected, -stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow1, 1))) {
            if (forward) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
            } else {
                adjustBackgroundAlpha(selected, stepInt(8));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow1, 2))) {
            selected.borderColor = cycleStyleColor(selected.borderColor, false);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow1, 3))) {
            selected.borderColor = cycleStyleColor(selected.borderColor, true);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow2, 0))) {
            selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow2, 1))) {
            selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(generalRow2, 2))) {
            String[] presets = switch (selected.type) {
                case LIST -> LIST_SOURCE_PRESETS;
                case STATE_BADGE -> STATE_SOURCE_PRESETS;
                case ICON -> iconIdSuggestionsForKind(selected.iconKind);
                default -> BAR_VALUE_SOURCE_PRESETS;
            };
            advancedAction = cyclePreset(advancedAction, presets, forward);
            advancedActionCursor = advancedAction.length();
            advancedActionSuggestionScroll = 0;
            return true;
        }
        return false;
    }

    private boolean handleCustomWidgetBaseRowClick(Click click, List<UiRect> baseRow, boolean forward) {
        if (containsBox(click.x(), click.y(), slot(baseRow, 0))) {
            selected.horizontalAlign = cycleHorizontalAlign(selected.horizontalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(baseRow, 1))) {
            selected.verticalAlign = cycleVerticalAlign(selected.verticalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(baseRow, 2))) {
            int oldScreenX = resolveElementX(selected);
            int oldScreenY = resolveElementY(selected);
            selected.anchor = cycleAnchor(selected.anchor, forward);
            setElementScreenPosition(selected, oldScreenX, oldScreenY);
            clampElementToCanvas(selected);
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(baseRow, 3))) {
            if (forward) {
                selected.drawBackground = !selected.drawBackground;
                ensureVisibleBackground(selected);
            } else {
                selected.drawBackground = true;
                selected.backgroundOpaque = false;
                adjustBackgroundAlpha(selected, 255 - Math.clamp(selected.backgroundAlpha, 0, 255));
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), slot(baseRow, 4))) {
            cycleBorderSetting(selected, forward);
            return true;
        }
        return false;
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

        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            List<UiRect> topButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(60, 1)
            );
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            if (containsBox(click.x(), click.y(), topButtons.get(0))) {
                selected.iconKind = cyclePreset(selected.iconKind, ICON_KIND_PRESETS, forward);
                if ("entity_model".equalsIgnoreCase(selected.iconKind)
                        && (safe(selected.iconId).isBlank() || "minecraft:stone".equalsIgnoreCase(safe(selected.iconId)))) {
                    selected.iconId = "minecraft:player";
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), topButtons.get(1))) {
                String[] ids = iconIdSuggestionsForKind(selected.iconKind);
                selected.iconId = cyclePreset(selected.iconId, ids, forward);
                return true;
            }
            if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
                if (containsBox(click.x(), click.y(), row1.get(0))) {
                    selected.modelZoom = Math.clamp((float) (selected.modelZoom - stepDouble(0.05)), 0.2f, 2.5f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row1.get(1))) {
                    selected.modelZoom = Math.clamp((float) (selected.modelZoom + stepDouble(0.05)), 0.2f, 2.5f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row1.get(2))) {
                    selected.modelYaw = Math.clamp((float) (selected.modelYaw - stepDouble(5.0)), -180.0f, 180.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row1.get(3))) {
                    selected.modelYaw = Math.clamp((float) (selected.modelYaw + stepDouble(5.0)), -180.0f, 180.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row2.get(0))) {
                    selected.modelPitch = Math.clamp((float) (selected.modelPitch - stepDouble(5.0)), -90.0f, 90.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row2.get(1))) {
                    selected.modelPitch = Math.clamp((float) (selected.modelPitch + stepDouble(5.0)), -90.0f, 90.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row2.get(2))) {
                    selected.modelOffsetX = Math.clamp(selected.modelOffsetX - stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row2.get(3))) {
                    selected.modelOffsetX = Math.clamp(selected.modelOffsetX + stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row3.get(0))) {
                    selected.modelOffsetY = Math.clamp(selected.modelOffsetY - stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row3.get(1))) {
                    selected.modelOffsetY = Math.clamp(selected.modelOffsetY + stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), row3.get(2))) {
                    selected.modelAutoFit = !selected.modelAutoFit;
                    return true;
                }
                if (containsBox(click.x(), click.y(), row3.get(3))) {
                    selected.modelFollowLook = !selected.modelFollowLook;
                    return true;
                }
            } else {
                if (containsBox(click.x(), click.y(), row1.get(0))) {
                    selected.iconShowCount = !selected.iconShowCount;
                    return true;
                }
                if (containsBox(click.x(), click.y(), row1.get(1))) {
                    selected.iconShowDurability = !selected.iconShowDurability;
                    return true;
                }
                if (containsBox(click.x(), click.y(), row1.get(2))) {
                    selected.iconShowCooldown = !selected.iconShowCooldown;
                    return true;
                }
            }
            return false;
        }

        if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            List<UiRect> top = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                    UiFlexLayout.Item.flex(110, 2), UiFlexLayout.Item.flex(60, 1)
            );
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            if (containsBox(click.x(), click.y(), top.get(0))) {
                selected.sourceTokenMax = cyclePreset(selected.sourceTokenMax, new String[]{"", "max_hp", "food", "players.count"}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), top.get(1))) {
                selected.segmented = !selected.segmented;
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(0))) {
                selected.segments = Math.max(1, selected.segments - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(1))) {
                selected.segments = Math.min(120, selected.segments + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(2))) {
                selected.minValue -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(3))) {
                selected.minValue += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(0))) {
                selected.maxValue = Math.max(selected.minValue + 1.0, selected.maxValue - stepDouble(1.0));
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(1))) {
                selected.maxValue = selected.maxValue + stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(2))) {
                selected.colorStart = cycleStyleColor(selected.colorStart, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(3))) {
                selected.colorStart = cycleStyleColor(selected.colorStart, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), row3.get(0))) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), row3.get(1))) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, true);
                return true;
            }
            return false;
        }

        if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            List<UiRect> presets = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
            );
            if (containsBox(click.x(), click.y(), slot(row1, 0))) {
                selected.warnThreshold -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 1))) {
                selected.warnThreshold += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 2))) {
                selected.critThreshold -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 3))) {
                selected.critThreshold += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 0))) {
                if (forward) {
                    selected.colorWarn = cycleStyleColor(selected.colorWarn, false);
                } else {
                    openColorPicker(color -> selected.colorWarn = color, "Pick Warn Color", slot(row2, 0).right() + 8, slot(row2, 0).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 1))) {
                if (forward) {
                    selected.colorWarn = cycleStyleColor(selected.colorWarn, true);
                } else {
                    openColorPicker(color -> selected.colorWarn = color, "Pick Warn Color", slot(row2, 1).right() + 8, slot(row2, 1).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 2))) {
                if (forward) {
                    selected.colorCrit = cycleStyleColor(selected.colorCrit, false);
                } else {
                    openColorPicker(color -> selected.colorCrit = color, "Pick Crit Color", slot(row2, 2).right() + 8, slot(row2, 2).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 3))) {
                if (forward) {
                    selected.colorCrit = cycleStyleColor(selected.colorCrit, true);
                } else {
                    openColorPicker(color -> selected.colorCrit = color, "Pick Crit Color", slot(row2, 3).right() + 8, slot(row2, 3).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row3, 0))) {
                if (forward) {
                    selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                } else {
                    openColorPicker(color -> {
                        selected.backgroundColor = color;
                        selected.backgroundAlpha = (color >>> 24) & 0xFF;
                    }, "Pick BG", slot(row3, 0).right() + 8, slot(row3, 0).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row3, 1))) {
                if (forward) {
                    selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                } else {
                    openColorPicker(color -> {
                        selected.backgroundColor = color;
                        selected.backgroundAlpha = (color >>> 24) & 0xFF;
                    }, "Pick BG", slot(row3, 1).right() + 8, slot(row3, 1).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row3, 2))) {
                if (forward) {
                    selected.textColor = cycleStyleColor(selected.textColor, false);
                } else {
                    openColorPicker(color -> selected.textColor = color, "Pick Text Color", slot(row3, 2).right() + 8, slot(row3, 2).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row3, 3))) {
                if (forward) {
                    selected.textColor = cycleStyleColor(selected.textColor, true);
                } else {
                    openColorPicker(color -> selected.textColor = color, "Pick Text Color", slot(row3, 3).right() + 8, slot(row3, 3).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(presets, 0))) {
                selected.prefix = cyclePreset(selected.prefix, new String[]{"", "HP: ", "Food: ", "FPS: "}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(presets, 1))) {
                selected.suffix = cyclePreset(selected.suffix, new String[]{"", "%", " hp", " ms"}, forward);
                return true;
            }
            return false;
        }

        if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            List<UiRect> row1 = layout.typeRow1();
            if (containsBox(click.x(), click.y(), layout.typeWideTop())) {
                advancedAction = cyclePreset(advancedAction, LIST_SOURCE_PRESETS, forward);
                advancedActionCursor = advancedAction.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(0))) {
                selected.maxLines = Math.max(1, selected.maxLines - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(1))) {
                selected.maxLines = Math.min(200, selected.maxLines + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(2))) {
                selected.listScroll = Math.max(0, selected.listScroll - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(3))) {
                selected.listScroll = Math.min(500, selected.listScroll + stepInt(1));
                return true;
            }
            return false;
        }

        if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            if (containsBox(click.x(), click.y(), layout.typeWideTop())) {
                selected.shapeType = cyclePreset(selected.shapeType, SHAPE_TYPE_PRESETS, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(0))) {
                selected.shapeFilled = !selected.shapeFilled;
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(1))) {
                selected.shapeRadius = Math.max(0, selected.shapeRadius - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row1.get(2))) {
                selected.shapeRadius = Math.min(64, selected.shapeRadius + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(0))) {
                selected.shapeThickness = Math.max(1, selected.shapeThickness - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), row2.get(1))) {
                selected.shapeThickness = Math.min(24, selected.shapeThickness + stepInt(1));
                return true;
            }
            return false;
        }

        if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            List<UiRect> textButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                    UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
            );
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            if (containsBox(click.x(), click.y(), slot(textButtons, 0))) {
                selected.stateOnText = cyclePreset(selected.stateOnText, new String[]{"ON", "YES", "ENABLED", "ACTIVE"}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(textButtons, 1))) {
                selected.stateOffText = cyclePreset(selected.stateOffText, new String[]{"OFF", "NO", "DISABLED", "IDLE"}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 0))) {
                if (forward) {
                    selected.colorStart = cycleStyleColor(selected.colorStart, false);
                } else {
                    openColorPicker(color -> selected.colorStart = color, "Pick ON Color", slot(row1, 0).right() + 8, slot(row1, 0).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 1))) {
                if (forward) {
                    selected.colorStart = cycleStyleColor(selected.colorStart, true);
                } else {
                    openColorPicker(color -> selected.colorStart = color, "Pick ON Color", slot(row1, 1).right() + 8, slot(row1, 1).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 2))) {
                if (forward) {
                    selected.colorEnd = cycleStyleColor(selected.colorEnd, false);
                } else {
                    openColorPicker(color -> selected.colorEnd = color, "Pick OFF Color", slot(row1, 2).right() + 8, slot(row1, 2).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row1, 3))) {
                if (forward) {
                    selected.colorEnd = cycleStyleColor(selected.colorEnd, true);
                } else {
                    openColorPicker(color -> selected.colorEnd = color, "Pick OFF Color", slot(row1, 3).right() + 8, slot(row1, 3).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 0))) {
                selected.stateShowValue = !selected.stateShowValue;
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 1))) {
                if (forward) {
                    selected.textColor = cycleStyleColor(selected.textColor, false);
                } else {
                    openColorPicker(color -> selected.textColor = color, "Pick Text Color", slot(row2, 1).right() + 8, slot(row2, 1).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 2))) {
                if (forward) {
                    selected.textColor = cycleStyleColor(selected.textColor, true);
                } else {
                    openColorPicker(color -> selected.textColor = color, "Pick Text Color", slot(row2, 2).right() + 8, slot(row2, 2).y() - 6);
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), slot(row2, 3))) {
                advancedAction = cyclePreset(advancedAction, STATE_SOURCE_PRESETS, forward);
                advancedActionCursor = advancedAction.length();
                return true;
            }
            return false;
        }

        return false;
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
            if (focusAdvancedTypeInputField(click, segInput, ModalDragSelectionField.ADVANCED_BORDER, false)) {
                return true;
            }
            return false;
        }
        if (selected.type == MacroHudDataHandler.ElementType.VALUE || selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            if (focusAdvancedTypeInputField(click, layout.typeInputLeft(), ModalDragSelectionField.ADVANCED_BG, true)) {
                return true;
            }
            if (focusAdvancedTypeInputField(click, layout.typeInputRight(), ModalDragSelectionField.ADVANCED_BORDER, true)) {
                return true;
            }
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


    private List<String> sourceTokenSuggestions(String prefix) {
        String p = safe(prefix).toLowerCase(Locale.ROOT);
        LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("Player", new ArrayList<>());
        grouped.put("Inventory", new ArrayList<>());
        grouped.put("Armor", new ArrayList<>());
        grouped.put("Container", new ArrayList<>());
        grouped.put("World", new ArrayList<>());
        grouped.put("Target", new ArrayList<>());
        grouped.put("Macro", new ArrayList<>());
        grouped.put("Script", new ArrayList<>());
        grouped.put("Variables", new ArrayList<>());
        grouped.put("Commands", new ArrayList<>());

        for (String token : MacroPlaceholders.getKnownPlaceholderTokens()) {
            String category = categorizeSuggestion(token);
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(token);
        }

        grouped.get("Commands").addAll(List.of(
                "cmd:/",
                "msg:",
                "say:",
                "copy:",
                "bar:",
                "if:{left}=={right}::cmd:/say yes:else:cmd:/say no"
        ));

        grouped.get("Variables").addAll(List.of(
                "{player.name}",
                "{player.uuid}",
                "{pos.x} {pos.y} {pos.z}",
                "{client.fps}",
                "{world.time.clock}"
        ));

        grouped.get("Script").addAll(List.of(
                "groovy:player.sendMessage(net.minecraft.text.Text.literal('Hi from Groovy'), false)",
                "kotlin:player.sendMessage(net.minecraft.text.Text.literal(\"Hi from Kotlin\"), false)",
                "example.groovy",
                "example.kts"
        ));
        grouped.get("Script").addAll(ScriptStorage.listScripts());

        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> values = entry.getValue().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            List<String> filtered = new ArrayList<>();
            for (String value : values) {
                String lower = value.toLowerCase(Locale.ROOT);
                if (p.isBlank() || lower.startsWith(p) || lower.contains(p)) {
                    filtered.add(value);
                }
            }
            if (filtered.isEmpty()) {
                continue;
            }
            out.add("[" + entry.getKey() + "]");
            for (String value : filtered) {
                out.add(entry.getKey() + " :: " + value);
            }
        }
        return out;
    }

    private static String categorizeSuggestion(String token) {
        String t = safe(token).toLowerCase(Locale.ROOT);
        if (t.startsWith("inventory.") || t.startsWith("hand.") || t.startsWith("offhand.")) return "Inventory";
        if (t.startsWith("armor.")) return "Armor";
        if (t.startsWith("container.")) return "Container";
        if (t.startsWith("world.") || t.startsWith("dim") || t.startsWith("pos.")) return "World";
        if (t.startsWith("look.") || t.startsWith("sel.") || t.startsWith("entities.")) return "Target";
        if (t.startsWith("players.") || t.startsWith("player.") || t.equals("hp") || t.equals("food") || t.equals("xp") || t.equals("level")) return "Player";
        if (t.startsWith("key.") || t.startsWith("cps.")) return "Macro";
        return "Variables";
    }

    private static boolean isSuggestionHeader(String suggestion) {
        return suggestion != null && suggestion.startsWith("[") && suggestion.endsWith("]");
    }

    private static String suggestionValue(String suggestion) {
        if (suggestion == null) {
            return "";
        }
        int idx = suggestion.indexOf(" :: ");
        if (idx < 0) {
            return suggestion;
        }
        return suggestion.substring(idx + 4);
    }

    private List<String> advancedActionSuggestions() {
        if (selected != null && selected.type == MacroHudDataHandler.ElementType.ICON) {
            String prefix = safe(advancedAction).toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String id : iconIdSuggestionsForKind(selected.iconKind)) {
                String candidate = safe(id);
                String lower = candidate.toLowerCase(Locale.ROOT);
                if (prefix.isBlank() || lower.startsWith(prefix) || lower.contains(prefix)) {
                    matches.add(candidate);
                }
            }
            return matches;
        }
        return sourceTokenSuggestions(advancedAction);
    }

    private static String[] iconIdSuggestionsForKind(String kind) {
        ensureIconSuggestionCaches();
        if ("block".equalsIgnoreCase(kind)) {
            return ALL_BLOCK_IDS.toArray(String[]::new);
        }
        if ("entity".equalsIgnoreCase(kind)) {
            return ALL_ENTITY_IDS.toArray(String[]::new);
        }
        if ("entity_model".equalsIgnoreCase(kind)) {
            return new String[]{"player", "minecraft:player"};
        }
        return ALL_ITEM_IDS.toArray(String[]::new);
    }

    private static void ensureIconSuggestionCaches() {
        if (ALL_ITEM_IDS != null && ALL_BLOCK_IDS != null && ALL_ENTITY_IDS != null) {
            return;
        }
        ALL_ITEM_IDS = Registries.ITEM.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
        ALL_BLOCK_IDS = Registries.BLOCK.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
        ALL_ENTITY_IDS = Registries.ENTITY_TYPE.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
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
        MacroHudDataHandler.Anchor[] order = {
                MacroHudDataHandler.Anchor.TOP_LEFT,
                MacroHudDataHandler.Anchor.TOP_CENTER,
                MacroHudDataHandler.Anchor.TOP_RIGHT,
                MacroHudDataHandler.Anchor.MIDDLE_RIGHT,
                MacroHudDataHandler.Anchor.MIDDLE_CENTER,
                MacroHudDataHandler.Anchor.MIDDLE_LEFT,
                MacroHudDataHandler.Anchor.BOTTOM_RIGHT,
                MacroHudDataHandler.Anchor.BOTTOM_CENTER,
                MacroHudDataHandler.Anchor.BOTTOM_LEFT,
        };
        MacroHudDataHandler.Anchor base = current == MacroHudDataHandler.Anchor.CENTER ? MacroHudDataHandler.Anchor.MIDDLE_CENTER : current;
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == base) {
                idx = i;
                break;
            }
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0) {
            next = order.length - 1;
        }
        if (next >= order.length) {
            next = 0;
        }
        return order[next];
    }

    private static String cyclePreset(String current, String[] presets, boolean forward) {
        return MacroWorkbenchUiOps.cyclePreset(current, presets, forward);
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
                final String outgoingRegex = safe(advancedAction);
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
                selected.label = safe(advancedText);
                if (selected.type == MacroHudDataHandler.ElementType.ICON) {
                    selected.iconId = safe(advancedAction);
                    selected.sourceToken = "";
                } else {
                    selected.sourceToken = safe(advancedAction);
                }
                if (selected.type == MacroHudDataHandler.ElementType.BAR) {
                    try {
                        String[] parts = safe(advancedBgColor).split(",");
                        if (parts.length >= 2) {
                            selected.minValue = Double.parseDouble(parts[0].trim());
                            selected.maxValue = Double.parseDouble(parts[1].trim());
                        }
                    } catch (Exception ignored) {
                        // keep previous values when parse fails
                    }
                    try {
                        selected.segments = Math.clamp(Integer.parseInt(safe(advancedBorderColor).trim()), 1, 120);
                    } catch (Exception ignored) {
                        // keep previous segment value when parse fails
                    }
                } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
                    selected.prefix = advancedBgColor == null ? "" : advancedBgColor;
                    selected.suffix = advancedBorderColor == null ? "" : advancedBorderColor;
                } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
                    selected.stateTrueValues = safe(advancedBgColor);
                    selected.stateFalseValues = safe(advancedBorderColor);
                }
            } else if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
                selected.buttonAction = advancedText;
                selected.label = advancedAction;
                selected.visibilityScreenType = safe(advancedVisibilityScreenType);
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

        String[] labels = {"Button", "Text", "Macro List", "Icon", "Bar", "Value", "List", "Shape", "State Badge"};
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
        int centerX = Math.max(0, (this.width - e.width) / 2);
        int centerY = Math.max(CANVAS_CONTENT_TOP, ((this.height - BOTTOM_BAR_H) - e.height) / 2);
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
                safe(kbNameField.getText()),
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
                advancedAction = suggestionValue(suggestions.get(next));
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
                advancedAction = suggestionValue(suggestions.get(next));
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
        if (suggestions == null || suggestions.isEmpty()) {
            return -1;
        }
        int dir = direction < 0 ? -1 : 1;
        int index = start;
        for (int i = 0; i < suggestions.size(); i++) {
            index += dir;
            if (index < 0) {
                index = suggestions.size() - 1;
            }
            if (index >= suggestions.size()) {
                index = 0;
            }
            if (!isSuggestionHeader(suggestions.get(index))) {
                return index;
            }
        }
        return -1;
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
                if (background) advancedBgSelectionAnchor = -1; else advancedBorderSelectionAnchor = -1;
                return true;
            }
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
            }
            setAdvancedColorState(background, value, cursor);
            if (background) advancedBgSelectionAnchor = -1; else advancedBorderSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(selectionAnchor, cursor)) {
                int start = selectionStart(selectionAnchor, cursor);
                int end = selectionEnd(selectionAnchor, cursor);
                value = value.substring(0, start) + value.substring(end);
                setAdvancedColorState(background, value, start);
                if (background) advancedBgSelectionAnchor = -1; else advancedBorderSelectionAnchor = -1;
                return true;
            }
            if (cursor < value.length()) {
                value = value.substring(0, cursor) + value.substring(cursor + 1);
            }
            setAdvancedColorState(background, value, cursor);
            if (background) advancedBgSelectionAnchor = -1; else advancedBorderSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = cursor;
            int next = Math.max(0, cursor - 1);
            setAdvancedColorState(background, value, next);
            if (background) advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            else advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = cursor;
            int next = Math.min(value.length(), cursor + 1);
            setAdvancedColorState(background, value, next);
            if (background) advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            else advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = cursor;
            setAdvancedColorState(background, value, 0);
            if (background) advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, 0, isShiftDown());
            else advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, 0, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = cursor;
            int next = value.length();
            setAdvancedColorState(background, value, next);
            if (background) advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, next, isShiftDown());
            else advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, next, isShiftDown());
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
            if (background) advancedBgSelectionAnchor = -1; else advancedBorderSelectionAnchor = -1;
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
        advancedBgColor = formatColor(selected.backgroundColor);
        advancedBorderColor = formatColor((isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected) || isMacroKeybindProxy(selected) || isPickupNotifierProxy(selected)) ? selected.textColor : selected.borderColor);
        advancedBgCursor = Math.clamp(advancedBgCursor, 0, advancedBgColor.length());
        advancedBorderCursor = Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length());
    }

    private void openColorPicker(boolean forBackground, int anchorX, int anchorY) {
        IntConsumer apply = forBackground
                ? color -> {
            advancedBgColor = formatColor(color);
            advancedBgCursor = advancedBgColor.length();
            if (selected != null) {
                selected.backgroundColor = color;
                selected.backgroundAlpha = (color >>> 24) & 0xFF;
            }
        }
                : color -> {
            advancedBorderColor = formatColor(color);
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
        colorPickerTitle = safe(title).isBlank() ? "Pick Color" : safe(title);
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
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int[] cursorPixel(int x, int y, String text, int cursorIndex) {
        List<String> lines = splitLinesRaw(text.substring(0, Math.clamp(cursorIndex, 0, text.length())));
        int row = Math.max(0, lines.size() - 1);
        String last = lines.isEmpty() ? "" : lines.get(lines.size() - 1);
        return new int[]{x + this.textRenderer.getWidth(last), y + row * 9};
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
                        .add(getGameKeybindDisplayName(kb, this.client.options));
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
        return Math.min(320, Math.max(220, this.width - 16));
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
            cells.add(new KeyCell(key, keyLabel(key), x, y, keyW, h));
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
        kbNameField.setText(safe(macro.name));
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
                safe(kbNameField.getText()),
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
        next.activePresetId = safe(this.working.activePresetId).isBlank() ? MacroHudDataHandler.getActivePresetId() : safe(this.working.activePresetId);
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
            boolean active = NBTInfoHudOverlayModule.INSTANCE.isEnabled();
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
        }
        if (isMacroKeybindProxy(element)) {
            boolean active = MacroKeybindOverlayModule.INSTANCE.isEnabled() && ModConfig.showMacroKeybindOverlay;
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
        proxy.height = Math.clamp(proxy.height, 20, Math.max(20, this.height - BOTTOM_BAR_H - CANVAS_CONTENT_TOP));
        proxy.x = Math.clamp(proxy.x, 0, Math.max(0, this.width - proxy.width));
        proxy.y = Math.clamp(proxy.y, CANVAS_CONTENT_TOP, Math.max(CANVAS_CONTENT_TOP, (this.height - BOTTOM_BAR_H) - proxy.height));
    }

    private void clampElementToCanvas(MacroHudDataHandler.HudElement element) {
        if (element == null) {
            return;
        }
        element.width = Math.clamp(element.width, 1, Math.max(1, this.width));
        element.height = Math.clamp(element.height, 1, Math.max(1, this.height - BOTTOM_BAR_H - CANVAS_CONTENT_TOP));
        int screenX = Math.clamp(resolveElementX(element), 0, Math.max(0, this.width - element.width));
        int screenY = Math.clamp(resolveElementY(element), CANVAS_CONTENT_TOP,
                Math.max(CANVAS_CONTENT_TOP, (this.height - BOTTOM_BAR_H) - element.height));
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
            elementClipboard.add(cloneElement(e));
        }
    }

    private void pasteElementsFromClipboard() {
        if (elementClipboard.isEmpty()) {
            return;
        }
        List<MacroHudDataHandler.HudElement> pasted = new ArrayList<>();
        for (int i = 0; i < elementClipboard.size(); i++) {
            MacroHudDataHandler.HudElement clone = cloneElement(elementClipboard.get(i));
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
        selected = pasted.get(pasted.size() - 1);
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

    private static MacroHudDataHandler.HudElement cloneElement(MacroHudDataHandler.HudElement e) {
        MacroHudDataHandler.HudElement cloned = new MacroHudDataHandler.HudElement();
        cloned.id = e.id;
        cloned.type = e.type;
        cloned.label = e.label;
        cloned.text = e.text;
        cloned.macroId = e.macroId;
        cloned.buttonAction = e.buttonAction;
        cloned.buttonExecutionMode = e.buttonExecutionMode;
        cloned.x = e.x;
        cloned.y = e.y;
        cloned.anchor = e.anchor;
        cloned.width = e.width;
        cloned.height = e.height;
        cloned.lineHeight = e.lineHeight;
        cloned.fontScale = e.fontScale;
        cloned.backgroundColor = e.backgroundColor;
        cloned.borderColor = e.borderColor;
        cloned.textColor = e.textColor;
        cloned.drawBackground = e.drawBackground;
        cloned.drawBorder = e.drawBorder;
        cloned.horizontalAlign = e.horizontalAlign;
        cloned.verticalAlign = e.verticalAlign;
        cloned.visibilityMode = e.visibilityMode;
        cloned.visibilityScreenType = e.visibilityScreenType;
        cloned.visible = e.visible;
        cloned.sourceToken = e.sourceToken;
        cloned.sourceTokenMax = e.sourceTokenMax;
        cloned.prefix = e.prefix;
        cloned.suffix = e.suffix;
        cloned.minValue = e.minValue;
        cloned.maxValue = e.maxValue;
        cloned.colorStart = e.colorStart;
        cloned.colorEnd = e.colorEnd;
        cloned.colorWarn = e.colorWarn;
        cloned.colorCrit = e.colorCrit;
        cloned.warnThreshold = e.warnThreshold;
        cloned.critThreshold = e.critThreshold;
        cloned.segmented = e.segmented;
        cloned.segments = e.segments;
        cloned.maxLines = e.maxLines;
        cloned.listScroll = e.listScroll;
        cloned.iconKind = e.iconKind;
        cloned.iconId = e.iconId;
        cloned.iconShowCount = e.iconShowCount;
        cloned.iconShowDurability = e.iconShowDurability;
        cloned.iconShowCooldown = e.iconShowCooldown;
        cloned.modelZoom = e.modelZoom;
        cloned.modelYaw = e.modelYaw;
        cloned.modelPitch = e.modelPitch;
        cloned.modelOffsetX = e.modelOffsetX;
        cloned.modelOffsetY = e.modelOffsetY;
        cloned.modelAutoFit = e.modelAutoFit;
        cloned.modelFollowLook = e.modelFollowLook;
        cloned.shapeType = e.shapeType;
        cloned.shapeFilled = e.shapeFilled;
        cloned.shapeRadius = e.shapeRadius;
        cloned.shapeThickness = e.shapeThickness;
        cloned.stateOnText = e.stateOnText;
        cloned.stateOffText = e.stateOffText;
        cloned.stateShowValue = e.stateShowValue;
        return cloned;
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
        int canvasBottom = this.height - BOTTOM_BAR_H;
        int canvasHeight = Math.max(1, canvasBottom - CANVAS_CONTENT_TOP);
        return switch (e.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> e.y;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, CENTER -> CANVAS_CONTENT_TOP + (canvasHeight - e.height) / 2 + e.y;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> canvasBottom - e.height - e.y;
        };
    }

    private void setElementScreenPosition(MacroHudDataHandler.HudElement e, int screenX, int screenY) {
        int canvasBottom = this.height - BOTTOM_BAR_H;
        int canvasHeight = Math.max(1, canvasBottom - CANVAS_CONTENT_TOP);
        e.x = switch (e.anchor) {
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> screenX;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> this.width - e.width - screenX;
            case TOP_CENTER, BOTTOM_CENTER, MIDDLE_CENTER, CENTER -> screenX - (this.width - e.width) / 2;
        };
        e.y = switch (e.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> screenY;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, CENTER -> screenY - (CANVAS_CONTENT_TOP + (canvasHeight - e.height) / 2);
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> canvasBottom - e.height - screenY;
        };
    }

    private static boolean hasSelection(int anchor, int cursor) { return anchor >= 0 && anchor != cursor; }
    private static int selectionStart(int anchor, int cursor) { return Math.min(anchor, cursor); }
    private static int selectionEnd(int anchor, int cursor) { return Math.max(anchor, cursor); }
    private static int clampTextIndex(String text, int index) {
        int len = text == null ? 0 : text.length();
        return Math.clamp(index, 0, len);
    }
    private static int[] clampedSelectionRange(String text, int anchor, int cursor) {
        int start = clampTextIndex(text, selectionStart(anchor, cursor));
        int end = clampTextIndex(text, selectionEnd(anchor, cursor));
        if (end < start) {
            end = start;
        }
        return new int[]{start, end};
    }
    private static String selectedText(String text, int anchor, int cursor) {
        if (!hasSelection(anchor, cursor)) return "";
        String safeText = text == null ? "" : text;
        int[] range = clampedSelectionRange(safeText, anchor, cursor);
        return safeText.substring(range[0], range[1]);
    }
    private static int updateSelectionAnchor(int currentAnchor, int oldCursor, int newCursor, boolean shiftDown) {
        if (!shiftDown) return -1;
        if (oldCursor == newCursor) return currentAnchor;
        return currentAnchor < 0 ? oldCursor : currentAnchor;
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
        int visible = Math.max(1, Math.min(totalSuggestions, Math.max(1, (bottomLimit - dropY) / rowH)));
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
        if (!hasSelection(anchor, cursor)) return;
        String safeText = text == null ? "" : text;
        int[] range = clampedSelectionRange(safeText, anchor, cursor);
        int sx = textX + this.textRenderer.getWidth(safeText.substring(0, range[0]));
        int ex = textX + this.textRenderer.getWidth(safeText.substring(0, range[1]));
        if (ex > sx) context.fill(sx, textY, ex, textY + 9, 0x704A7CC7);
    }
    private void drawMultilineSelection(DrawContext context, int textX, int textY, int maxY, String text, int anchor, int cursor, int lineHeight) {
        if (!hasSelection(anchor, cursor)) return;
        String safeText = text == null ? "" : text;
        int[] range = clampedSelectionRange(safeText, anchor, cursor);
        int start = range[0];
        int end = range[1];
        List<String> lines = splitLinesRaw(safeText);
        int y = textY;
        int index = 0;
        for (String line : lines) {
            if (y > maxY) break;
            int lineStart = index;
            int lineEnd = lineStart + line.length();
            int selStart = Math.max(start, lineStart);
            int selEnd = Math.min(end, lineEnd);
            if (selEnd > selStart) {
                int sx = textX + this.textRenderer.getWidth(line.substring(0, selStart - lineStart));
                int ex = textX + this.textRenderer.getWidth(line.substring(0, selEnd - lineStart));
                context.fill(sx, y, ex, y + 9, 0x704A7CC7);
            }
            y += lineHeight;
            index = lineEnd + 1;
        }
    }

    private void drawMultilineSelectionWithScroll(DrawContext context, int textX, int textY, int maxY,
                                                  String text, int anchor, int cursor, int lineHeight, int scrollLines) {
        if (!hasSelection(anchor, cursor)) {
            return;
        }
        String safeText = text == null ? "" : text;
        int[] range = clampedSelectionRange(safeText, anchor, cursor);
        int start = range[0];
        int end = range[1];
        List<String> lines = splitLinesRaw(safeText);
        int y = textY;
        int index = 0;
        int firstLine = Math.max(0, scrollLines);
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            int lineStart = index;
            int lineEnd = lineStart + line.length();
            if (lineIdx >= firstLine) {
                if (y > maxY) {
                    break;
                }
                int selStart = Math.max(start, lineStart);
                int selEnd = Math.min(end, lineEnd);
                if (selEnd > selStart) {
                    int sx = textX + this.textRenderer.getWidth(line.substring(0, selStart - lineStart));
                    int ex = textX + this.textRenderer.getWidth(line.substring(0, selEnd - lineStart));
                    context.fill(sx, y, ex, y + 9, 0x704A7CC7);
                }
                y += lineHeight;
            }
            index = lineEnd + 1;
        }
    }

    private static boolean containsBox(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return GuiSystem.contains(x, y, boxX, boxY, boxW, boxH);
    }

    private static boolean containsBox(double x, double y, UiRect rect) {
        return GuiSystem.contains(x, y, rect);
    }

    private static String firstLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String normalized = normalizeMultilineInput(raw);
        int idx = normalized.indexOf('\n');
        return idx < 0 ? normalized : normalized.substring(0, idx);
    }

    private static List<String> splitLinesRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of("");
        }
        String normalized = normalizeMultilineInput(raw);
        String[] parts = normalized.split("\\n", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            out.add(p == null ? "" : p);
        }
        return out;
    }

    private static String normalizeMultilineInput(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static String normalizeSingleLineInput(String raw) {
        String normalized = normalizeMultilineInput(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replace('\n', ' ');
    }

    private static List<String> splitLines(String raw) {
        return splitLinesRaw(raw);
    }

    private record TextRun(String text, int color) {
    }

    private int styledLineWidth(String raw) {
        int width = 0;
        for (TextRun run : parseColorRuns(raw, 0xFFFFFFFF)) {
            width += this.textRenderer.getWidth(run.text());
        }
        return width;
    }

    private void drawStyledTextLine(DrawContext context, String raw, int x, int y, int defaultColor, float scale) {
        List<TextRun> runs = parseColorRuns(raw, defaultColor);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int logicalX = Math.round(x / scale);
        int logicalY = Math.round(y / scale);
        int cursor = logicalX;
        for (TextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, run.text(), cursor, logicalY, run.color());
            cursor += this.textRenderer.getWidth(run.text());
        }

        context.getMatrices().popMatrix();
    }

    private List<TextRun> parseColorRuns(String raw, int defaultColor) {
        List<TextRun> runs = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            runs.add(new TextRun("", defaultColor));
            return runs;
        }

        StringBuilder chunk = new StringBuilder();
        int color = defaultColor;
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length()) {
                int mapped = mapLegacyColor(raw.charAt(i + 1));
                if (mapped != Integer.MIN_VALUE) {
                    if (!chunk.isEmpty()) {
                        runs.add(new TextRun(chunk.toString(), color));
                        chunk.setLength(0);
                    }
                    color = mapped;
                    i += 2;
                    continue;
                }
            }

            if (c == '#') {
                int end = Math.min(raw.length(), i + 7);
                String token = raw.substring(i, end);
                if (token.length() == 7 && HEX_COLOR.matcher(token).matches()) {
                    if (!chunk.isEmpty()) {
                        runs.add(new TextRun(chunk.toString(), color));
                        chunk.setLength(0);
                    }
                    color = 0xFF000000 | Integer.parseInt(token.substring(1), 16);
                    i += 7;
                    continue;
                }
            }

            chunk.append(c);
            i++;
        }

        if (!chunk.isEmpty() || runs.isEmpty()) {
            runs.add(new TextRun(chunk.toString(), color));
        }
        return runs;
    }

    private static int mapLegacyColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            default -> Integer.MIN_VALUE;
        };
    }

    private int cursorIndexFromPoint(String text, int localX, int localY, int lineHeight) {
        String safeText = text == null ? "" : text;
        List<String> lines = splitLinesRaw(safeText);
        int row = Math.clamp(localY / Math.max(1, lineHeight), 0, Math.max(0, lines.size() - 1));

        int base = 0;
        for (int i = 0; i < row; i++) {
            base += lines.get(i).length() + 1;
        }

        String line = lines.get(row);
        int x = Math.max(0, localX);
        int bestCol = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int col = 0; col <= line.length(); col++) {
            int w = this.textRenderer.getWidth(line.substring(0, col));
            int d = Math.abs(w - x);
            if (d < bestDist) {
                bestDist = d;
                bestCol = col;
            }
        }
        return Math.clamp(base + bestCol, 0, safeText.length());
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String preserve(String s) {
        return s == null ? "" : s;
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

    net.minecraft.client.font.TextRenderer workbenchTextRenderer() {
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
            out.add(name + " - [" + keyLabel(macro.keyCode) + "]");
        }
        if (out.size() == 1) {
            out.add("(none)");
        }
        return out;
    }

    private ItemStack resolvePreviewIconStack(String kind, String iconId) {
        Identifier id = Identifier.tryParse(safe(iconId));
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

    private void drawCanvasPlayerModelPreview(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y, int w, int h) {
        if (this.client == null) {
            return;
        }
        net.minecraft.entity.Entity target = resolveCanvasModelTargetEntity(element);
        if (target == null) {
            return;
        }
        int safeW = Math.max(1, w);
        int safeH = Math.max(1, h);
        int innerPad = element.drawBorder ? 2 : 1;
        int boxW = Math.max(1, safeW - (innerPad * 2));
        int boxH = Math.max(1, safeH - (innerPad * 2));
        int baseSize = element.modelAutoFit
                ? Math.max(8, Math.round(Math.min(boxW, boxH) * 0.48f))
                : Math.max(8, Math.min(safeW, safeH) - 4);
        int size = Math.max(8, Math.round(baseSize * Math.clamp(element.modelZoom, 0.2f, 2.5f)));
        int left = x + innerPad + element.modelOffsetX;
        int top = y + innerPad + element.modelOffsetY;
        int right = x + safeW - innerPad + element.modelOffsetX;
        int bottom = y + safeH - innerPad + element.modelOffsetY;
        if (right <= left) {
            right = left + 1;
        }
        if (bottom <= top) {
            bottom = top + 1;
        }
        drawEntityModelReflective(context, target, left, top, right, bottom, size,
                element.modelYaw, element.modelPitch, element.modelFollowLook);
    }

    private net.minecraft.entity.Entity resolveCanvasModelTargetEntity(MacroHudDataHandler.HudElement element) {
        if (this.client == null) {
            return null;
        }
        String id = safe(element == null ? null : element.iconId).toLowerCase(Locale.ROOT);
        if (id.isBlank() || "player".equals(id) || "minecraft:player".equals(id)) {
            return this.client.player;
        }
        return this.client.player != null ? this.client.player : this.client.getCameraEntity();
    }

    private void drawEntityModelReflective(DrawContext context, net.minecraft.entity.Entity entity,
                                           int left, int top, int right, int bottom,
                                           int size, float yaw, float pitch, boolean followLook) {
        if (entity == null || size < 1) {
            return;
        }
        try {
            Class<?> inventoryScreen = Class.forName("net.minecraft.client.gui.screen.ingame.InventoryScreen");
            Class<?> drawContextClass = Class.forName("net.minecraft.client.gui.DrawContext");
            Class<?> entityClass = Class.forName("net.minecraft.entity.Entity");
            Class<?> vectorClass = Class.forName("org.joml.Vector3f");
            Class<?> quaternionClass = Class.forName("org.joml.Quaternionf");
            Class<?> livingEntityClass = Class.forName("net.minecraft.entity.LivingEntity");

            float[] resolvedAngles = resolveModelAngles(entity, yaw, pitch, followLook);
            float resolvedYaw = resolvedAngles[0];
            float resolvedPitch = resolvedAngles[1];

            Object vecZero = vectorClass.getConstructor(float.class, float.class, float.class).newInstance(0.0f, 0.0f, 0.0f);
            Object modelQuat = buildModelQuaternion(quaternionClass, resolvedYaw, resolvedPitch);
            Object identityQuat = quaternionClass.getConstructor().newInstance();

            if (invokePreferredEntityDrawPreview(inventoryScreen, context, entity,
                    left, top, right, bottom, size,
                    vecZero, modelQuat, identityQuat,
                    drawContextClass, vectorClass, quaternionClass, entityClass, livingEntityClass,
                    resolvedYaw, resolvedPitch)) {
                return;
            }

            List<Class<?>> owners = List.of(inventoryScreen, drawContextClass);
            for (Class<?> owner : owners) {
                for (Method method : owner.getMethods()) {
                    if (!"drawEntity".equals(method.getName())) {
                        continue;
                    }
                    boolean staticMethod = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                    if (!staticMethod && !drawContextClass.isAssignableFrom(owner)) {
                        continue;
                    }

                    Class<?>[] paramTypes = method.getParameterTypes();
                    int intParams = 0;
                    for (Class<?> type : paramTypes) {
                        if (type == int.class || type == Integer.class) {
                            intParams++;
                        }
                    }
                    if (intParams < 3) {
                        continue;
                    }

                    int[] intArgValues = buildEntityDrawIntArgs(left, top, right, bottom, size, intParams);
                    if (intArgValues == null) {
                        continue;
                    }

                    float fallbackCx = (intArgValues.length >= 2)
                            ? (intArgValues[0] + (intArgValues.length >= 4 ? intArgValues[2] : intArgValues[0])) / 2.0f
                            : 0.0f;
                    float fallbackCy = (intArgValues.length >= 2)
                            ? (intArgValues[1] + (intArgValues.length >= 4 ? intArgValues[3] : intArgValues[1])) / 2.0f
                            : 0.0f;
                    float mouseX = fallbackCx - 40.0f * (float) Math.tan(Math.clamp(resolvedYaw, -30f, 30f) / 20.0f);
                    float mouseY = fallbackCy + 40.0f * (float) Math.tan(Math.clamp(resolvedPitch, -30f, 30f) / 20.0f);

                    Object[] args = new Object[paramTypes.length];
                    int intArg = 0;
                    int quatArg = 0;
                    int floatArg = 0;
                    boolean accepted = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> type = paramTypes[i];
                        if (drawContextClass.isAssignableFrom(type)) {
                            args[i] = context;
                        } else if (type == int.class || type == Integer.class) {
                            args[i] = intArgValues[Math.min(intArg++, intArgValues.length - 1)];
                        } else if (type == float.class || type == Float.class) {
                            switch (floatArg++) {
                                case 0 -> args[i] = 0.0625f;
                                case 1 -> args[i] = mouseX;
                                case 2 -> args[i] = mouseY;
                                default -> args[i] = 0.0f;
                            }
                        } else if (type == double.class || type == Double.class) {
                            args[i] = 0.0d;
                        } else if (type == boolean.class || type == Boolean.class) {
                            args[i] = false;
                        } else if (type.getName().equals("org.joml.Vector3f")) {
                            args[i] = vecZero;
                        } else if (type.getName().equals("org.joml.Quaternionf")) {
                            args[i] = (quatArg++ == 0) ? modelQuat : identityQuat;
                        } else if (entityClass.isAssignableFrom(type)) {
                            args[i] = entity;
                        } else {
                            accepted = false;
                            break;
                        }
                    }
                    if (!accepted) {
                        continue;
                    }
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(staticMethod ? null : context, args);
                        return;
                    } catch (Throwable ignoredInvokeFailure) {
                        // Keep trying compatible signatures.
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }
            }
        } catch (Exception ignored) {
            // Graceful fallback when mapped signatures differ across versions.
        }
    }

    private boolean invokePreferredEntityDrawPreview(Class<?> inventoryScreen,
                                                     DrawContext context,
                                                     net.minecraft.entity.Entity entity,
                                                     int left, int top, int right, int bottom, int size,
                                                     Object vecZero, Object modelQuat, Object identityQuat,
                                                     Class<?> drawContextClass, Class<?> vectorClass, Class<?> quaternionClass,
                                                     Class<?> entityClass, Class<?> livingEntityClass,
                                                     float resolvedYaw, float resolvedPitch) {
        int cx = left + ((right - left) / 2);
        int cy = bottom;

        // Collect all static drawEntity methods so we can prioritise.
        java.util.List<Method> candidates = new java.util.ArrayList<>();
        for (Method m : inventoryScreen.getMethods()) {
            if ("drawEntity".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                candidates.add(m);
            }
        }

        // ── Priority 1: MC 1.21.x low-level 8-param overload ───────────────────────
        // drawEntity(DrawContext, float x, float y, float size,
        //            Vector3f, Quaternionf, @Nullable Quaternionf, LivingEntity)
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesFloatXYSizeVecQuatSignature(p, drawContextClass, vectorClass, quaternionClass, livingEntityClass)) {
                continue;
            }
            if (!p[7].isInstance(entity)) {
                continue;
            }
            try {
                float entityScale = (entity instanceof LivingEntity le) ? le.getScale() : 1.0f;
                float q = (float) size / entityScale;
                float mcYaw = 180.0f + resolvedYaw;
                float pitchRad = (float) (-resolvedPitch * Math.PI / 180.0);
                Object uiQuat = quaternionClass.getConstructor().newInstance();
                Method rotZ = quaternionClass.getMethod("rotateZ", float.class);
                Method rotX = quaternionClass.getMethod("rotateX", float.class);
                rotZ.invoke(uiQuat, (float) Math.PI);
                rotX.invoke(uiQuat, pitchRad);
                Object lightQuat = quaternionClass.getConstructor().newInstance();
                rotX.invoke(lightQuat, pitchRad);
                Object vec = vectorClass.getConstructor(float.class, float.class, float.class)
                        .newInstance(0.0f, entity.getHeight() / 2.0f + 0.0625f * entityScale, 0.0f);
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                applyEntityOrientationForScreen(entity, mcYaw, resolvedPitch);
                try {
                    method.invoke(null, context, fcx, fcy, q, vec, uiQuat, lightQuat, entity);
                    return true;
                } finally {
                    restoreEntityOrientation(entity, snapshot);
                }
            } catch (Throwable ignored) {
            }
        }

        // ── Priority 2: MC 1.21.x primary 10-param overload ────────────────────────
        // drawEntity(DrawContext, int x1, int y1, int x2, int y2, int size,
        //            float f, float mouseX, float mouseY, LivingEntity)
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesRectMouseFloat3Signature(p, drawContextClass, livingEntityClass)) {
                continue;
            }
            if (!p[9].isInstance(entity)) {
                continue;
            }
            try {
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                float mx = fcx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                float my = fcy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                method.invoke(null, context, left, top, right, bottom, size, 0.0625f, mx, my, entity);
                return true;
            } catch (Throwable ignored) {
            }
        }

        // ── Priority 3: legacy quaternion-based APIs (pre-1.20.5) ───────────────────
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            try {
                if (matchesRectQuatSignature(p, drawContextClass, vectorClass, quaternionClass)
                        && p[9].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, left, top, right, bottom, size, vecZero, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                if (matchesCenterQuatSignature(p, drawContextClass, quaternionClass, entityClass)
                        && p[6].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, cx, cy, size, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                if (matchesMouseFloatSignature(p, drawContextClass, entityClass)
                        && p[6].isInstance(entity)) {
                    float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                    float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                    float mx = cx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                    float my = cy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                    method.invoke(null, context, cx, cy, size, mx, my, entity);
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    // ── MC 1.21.x primary overload ──────────────────────────────────────────────
    private boolean matchesRectMouseFloat3Signature(Class<?>[] p, Class<?> dcClass, Class<?> livingEntityClass) {
        return p.length == 10
                && dcClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == int.class && p[5] == int.class
                && p[6] == float.class && p[7] == float.class && p[8] == float.class
                && livingEntityClass.isAssignableFrom(p[9]);
    }

    // ── MC 1.21.x low-level 8-param overload ────────────────────────────────────
    private boolean matchesFloatXYSizeVecQuatSignature(Class<?>[] p, Class<?> dcClass, Class<?> vectorClass,
                                                        Class<?> quaternionClass, Class<?> livingEntityClass) {
        return p.length == 8
                && dcClass.isAssignableFrom(p[0])
                && p[1] == float.class && p[2] == float.class && p[3] == float.class
                && vectorClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && (quaternionClass.getName().equals(p[6].getName()) || !p[6].isPrimitive())
                && livingEntityClass.isAssignableFrom(p[7]);
    }

    private boolean matchesRectQuatSignature(Class<?>[] p,
                                             Class<?> drawContextClass,
                                             Class<?> vectorClass,
                                             Class<?> quaternionClass) {
        return p.length == 10
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class && p[5] == int.class
                && vectorClass.getName().equals(p[6].getName())
                && quaternionClass.getName().equals(p[7].getName())
                && quaternionClass.getName().equals(p[8].getName());
    }

    private boolean matchesCenterQuatSignature(Class<?>[] p,
                                               Class<?> drawContextClass,
                                               Class<?> quaternionClass,
                                               Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && quaternionClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && entityClass.isAssignableFrom(p[6]);
    }

    private boolean matchesMouseFloatSignature(Class<?>[] p,
                                               Class<?> drawContextClass,
                                               Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == float.class && p[5] == float.class
                && entityClass.isAssignableFrom(p[6]);
    }

    private EntityOrientationSnapshot captureEntityOrientation(net.minecraft.entity.Entity entity) {
        if (entity == null) {
            return new EntityOrientationSnapshot(0.0f, 0.0f, null, null, null);
        }
        Float bodyYaw = null;
        Float headYaw = null;
        Float prevHeadYaw = null;
        if (entity instanceof LivingEntity living) {
            bodyYaw = living.getBodyYaw();
            headYaw = living.getHeadYaw();
            prevHeadYaw = getFieldFloat(LivingEntity.class, living, "prevHeadYaw");
        }
        return new EntityOrientationSnapshot(entity.getYaw(), entity.getPitch(), bodyYaw, headYaw, prevHeadYaw);
    }

    private void applyEntityOrientation(net.minecraft.entity.Entity entity, float yaw, float pitch) {
        if (entity == null) return;
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        if (entity instanceof LivingEntity living) {
            living.setBodyYaw(yaw);
            living.setHeadYaw(yaw);
            setFieldFloat(LivingEntity.class, living, "prevHeadYaw", yaw);
        }
    }

    private void applyEntityOrientationForScreen(net.minecraft.entity.Entity entity, float displayYaw, float pitch) {
        if (entity == null) return;
        entity.setYaw(displayYaw);
        entity.setPitch(pitch);
        if (entity instanceof LivingEntity living) {
            living.setBodyYaw(displayYaw);
            living.setHeadYaw(displayYaw);
            setFieldFloat(LivingEntity.class, living, "prevHeadYaw", displayYaw);
        }
    }

    private void restoreEntityOrientation(net.minecraft.entity.Entity entity, EntityOrientationSnapshot snapshot) {
        if (entity == null || snapshot == null) {
            return;
        }
        entity.setYaw(snapshot.yaw());
        entity.setPitch(snapshot.pitch());
        if (entity instanceof LivingEntity living) {
            if (snapshot.bodyYaw() != null) {
                living.setBodyYaw(snapshot.bodyYaw());
            }
            if (snapshot.headYaw() != null) {
                living.setHeadYaw(snapshot.headYaw());
            }
            if (snapshot.prevHeadYaw() != null) {
                setFieldFloat(LivingEntity.class, living, "prevHeadYaw", snapshot.prevHeadYaw());
            }
        }
    }

    private Float getFieldFloat(Class<?> clazz, Object obj, String fieldName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getFloat(obj);
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
        return null;
    }

    private void setFieldFloat(Class<?> clazz, Object obj, String fieldName, float value) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.setFloat(obj, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
    }

    private record EntityOrientationSnapshot(float yaw, float pitch, Float bodyYaw, Float headYaw, Float prevHeadYaw) {
    }

    private float[] resolveModelAngles(net.minecraft.entity.Entity entity, float yawOffset, float pitchOffset, boolean followLook) {
        float baseYaw = 0.0f;
        float basePitch = 0.0f;
        if (followLook && entity != null) {
            baseYaw = entity instanceof LivingEntity living ? living.getHeadYaw() : entity.getYaw();
            basePitch = entity.getPitch();
        }
        float resolvedYaw = wrapDegrees(baseYaw + yawOffset);
        float resolvedPitch = Math.clamp(basePitch + pitchOffset, -90.0f, 90.0f);
        return new float[]{resolvedYaw, resolvedPitch};
    }


    private float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private Object buildModelQuaternion(Class<?> quaternionClass, float yaw, float pitch) {
        try {
            Object quat = quaternionClass.getConstructor().newInstance();
            try {
                Method rotateZ = quaternionClass.getMethod("rotateZ", float.class);
                rotateZ.invoke(quat, (float) Math.PI);
            } catch (Exception ignored) {
            }
            float pitchRad = (float) (-pitch * Math.PI / 180.0);
            try {
                Method rotateX = quaternionClass.getMethod("rotateX", float.class);
                rotateX.invoke(quat, pitchRad);
            } catch (Exception ignored) {
            }
            return quat;
        } catch (Exception ignored) {
            try {
                return quaternionClass.getConstructor().newInstance();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private int[] buildEntityDrawIntArgs(int left, int top, int right, int bottom, int size, int intParams) {
        int safeSize = Math.max(8, size);
        int safeLeft = left;
        int safeTop = top;
        int safeRight = Math.max(right, safeLeft + 1);
        int safeBottom = Math.max(bottom, safeTop + 1);
        if (intParams == 3) {
            int cx = safeLeft + ((safeRight - safeLeft) / 2);
            int cy = safeBottom;
            return new int[]{cx, cy, safeSize};
        }
        if (intParams >= 5) {
            int[] args = new int[intParams];
            args[0] = safeLeft;
            args[1] = safeTop;
            args[2] = safeRight;
            args[3] = safeBottom;
            args[4] = safeSize;
            for (int i = 5; i < intParams; i++) {
                args[i] = safeSize;
            }
            return args;
        }
        // Skip ambiguous 4-int signatures instead of risking invalid x2/y2 bounds.
        return null;
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
        return parseCanvasFirstDouble(expanded);
    }

    private static Double parseCanvasFirstDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim().replace(',', '.');
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            StringBuilder num = new StringBuilder();
            boolean dotSeen = false;
            for (int i = 0; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if ((c >= '0' && c <= '9') || (num.isEmpty() && (c == '-' || c == '+'))) {
                    num.append(c);
                } else if (c == '.' && !dotSeen) {
                    dotSeen = true;
                    num.append(c);
                } else if (!num.isEmpty()) {
                    break;
                }
            }
            if (!num.isEmpty()) {
                try {
                    return Double.parseDouble(num.toString());
                } catch (Exception ignored2) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String formatCanvasValue(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<String> splitListSourceForCanvas(String src) {
        if (src == null || src.isBlank()) {
            return new ArrayList<>();
        }
        String normalized = src.replace("\\n", "\n").replace("\r", "");
        List<String> out = new ArrayList<>();
        if (normalized.contains("\n")) {
            for (String line : normalized.split("\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        for (String part : normalized.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static int blendColor(int c1, int c2, float t) {
        float tt = Math.clamp(t, 0.0f, 1.0f);
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * tt);
        int r = Math.round(r1 + (r2 - r1) * tt);
        int g = Math.round(g1 + (g2 - g1) * tt);
        int b = Math.round(b1 + (b2 - b1) * tt);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String shortAnchor(MacroHudDataHandler.Anchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "TL";
            case TOP_CENTER -> "TC";
            case TOP_RIGHT -> "TR";
            case MIDDLE_LEFT -> "ML";
            case MIDDLE_RIGHT -> "MR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_CENTER -> "BC";
            case BOTTOM_RIGHT -> "BR";
            case MIDDLE_CENTER -> "MC";
            case CENTER -> "C*";
        };
    }

    private static String shortVisibility(MacroHudDataHandler.VisibilityMode mode) {
        return switch (mode) {
            case ALWAYS -> "ALL";
            case CHAT -> "CHAT";
            case INVENTORY -> "INV";
            case CONTAINER -> "CONT";
            case CHEST -> "CHEST";
            case SCREEN -> "SCR";
        };
    }

    private static String shortExecutionMode(MacroHudDataHandler.ButtonExecutionMode mode) {
        return switch (mode == null ? MacroHudDataHandler.ButtonExecutionMode.COMMAND : mode) {
            case COMMAND -> "Command";
            case GROOVY_SCRIPT -> "Groovy Script";
            case KOTLIN_SCRIPT -> "Kotlin Script";
        };
    }

    private static MacroHudDataHandler.VisibilityMode cycleVisibilityMode(MacroHudDataHandler.VisibilityMode mode, boolean forward) {
        MacroHudDataHandler.VisibilityMode[] values = MacroHudDataHandler.VisibilityMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mode) {
                index = i;
                break;
            }
        }
        index += forward ? 1 : -1;
        if (index < 0) {
            index = values.length - 1;
        }
        if (index >= values.length) {
            index = 0;
        }
        return values[index];
    }

    private static MacroHudDataHandler.ButtonExecutionMode cycleButtonExecutionMode(MacroHudDataHandler.ButtonExecutionMode mode, boolean forward) {
        MacroHudDataHandler.ButtonExecutionMode[] values = MacroHudDataHandler.ButtonExecutionMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mode) {
                index = i;
                break;
            }
        }
        index += forward ? 1 : -1;
        if (index < 0) {
            index = values.length - 1;
        }
        if (index >= values.length) {
            index = 0;
        }
        return values[index];
    }


    private static String keyLabel(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L.C";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R.C";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L.A";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R.A";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L.S";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R.S";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Dn";
            case GLFW.GLFW_KEY_LEFT -> "Lt";
            case GLFW.GLFW_KEY_RIGHT -> "Rt";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_KP_ADD -> "Num+";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "Num-";
            case GLFW.GLFW_KEY_KP_MULTIPLY -> "Num*";
            case GLFW.GLFW_KEY_KP_DIVIDE -> "Num/";
            case GLFW.GLFW_KEY_KP_ENTER -> "NumE";
            case GLFW.GLFW_KEY_KP_DECIMAL -> "Num.";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "M1";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "M2";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "M3";
            case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
            case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
            default -> {
                try {
                    String s = InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
                    yield s.length() > 7 ? s.substring(0, 7) : s;
                } catch (Exception ignored) {
                    yield "?";
                }
            }
        };
    }

    private static void ensureVisibleBackground(MacroHudDataHandler.HudElement e) {
        if (e == null || !e.drawBackground) {
            return;
        }
        if ((e.backgroundColor >>> 24) == 0) {
            e.backgroundColor = 0xAA101010;
        }
        if (e.backgroundAlpha <= 0) {
            e.backgroundAlpha = (e.backgroundColor >>> 24) & 0xFF;
            if (e.backgroundAlpha <= 0) {
                e.backgroundAlpha = 0xAA;
            }
        }
        if (e.height < 14) {
            e.height = 14;
        }
        if ((e.borderColor >>> 24) == 0) {
            e.borderColor = 0xFFFFFFFF;
        }
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

    private static String formatColor(int argb) {
        return String.format("#%08X", argb);
    }

    private static int lineStart(String text, int index) {
        int cursor = Math.clamp(index, 0, text.length());
        int prevNewline = text.lastIndexOf('\n', Math.max(0, cursor - 1));
        return prevNewline < 0 ? 0 : prevNewline + 1;
    }

    private static int lineEnd(String text, int index) {
        int cursor = Math.clamp(index, 0, text.length());
        int nextNewline = text.indexOf('\n', cursor);
        return nextNewline < 0 ? text.length() : nextNewline;
    }

    private static int moveCursorVertical(String text, int index, int dir) {
        int cursor = Math.clamp(index, 0, text.length());
        int start = lineStart(text, cursor);
        int col = cursor - start;

        if (dir < 0) {
            if (start == 0) {
                return cursor;
            }
            int prevEnd = start - 1;
            int prevStart = lineStart(text, prevEnd);
            return Math.min(prevStart + col, prevEnd);
        }

        int end = lineEnd(text, cursor);
        if (end >= text.length()) {
            return cursor;
        }
        int nextStart = end + 1;
        int nextEnd = lineEnd(text, nextStart);
        return Math.min(nextStart + col, nextEnd);
    }

    private static String getGameKeybindDisplayName(KeyBinding kb, Object options) {
        String action = reflectActionName(kb);
        if ((action == null || action.isBlank()) && options != null) {
            action = deriveActionFromOptionsField(kb, options);
        }
        if (action == null || action.isBlank()) {
            return kb.getBoundKeyLocalizedText().getString();
        }
        return action;
    }

    private static String deriveActionFromOptionsField(KeyBinding kb, Object options) {
        Class<?> c = options.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (!KeyBinding.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(options);
                    if (v == kb) {
                        return humanizeFieldName(f.getName());
                    }
                } catch (Exception ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static String humanizeFieldName(String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        String raw = field.endsWith("Key") ? field.substring(0, field.length() - 3) : field;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                out.append(' ');
            }
            out.append(ch);
        }
        if (out.isEmpty()) {
            return null;
        }
        out.setCharAt(0, Character.toUpperCase(out.charAt(0)));
        return out.toString();
    }

    private static String reflectActionName(KeyBinding kb) {
        String action = extractActionKeyFromMethods(kb);
        if (action != null) {
            return humanizeTranslation(action);
        }

        action = extractActionKeyFromFields(kb);
        if (action != null) {
            return humanizeTranslation(action);
        }

        // Last-ditch fallback: parse class string if it exposes key identifiers.
        String fallback = kb.toString();
        if (fallback != null) {
            int idx = fallback.indexOf("key.");
            if (idx >= 0) {
                String candidate = fallback.substring(idx).split("[^a-zA-Z0-9_.]", 2)[0];
                if (isActionTranslationKey(candidate)) {
                    return humanizeTranslation(candidate);
                }
            }
        }

        return null;
    }

    private static String extractActionKeyFromMethods(KeyBinding kb) {
        try {
            Method m = kb.getClass().getMethod("getTranslationKey");
            Object v = m.invoke(kb);
            if (v instanceof String s && isActionTranslationKey(s)) {
                return s;
            }
        } catch (Exception ignored) {
        }

        for (Method m : kb.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != String.class) {
                continue;
            }
            try {
                Object v = m.invoke(kb);
                if (v instanceof String s && isActionTranslationKey(s)) {
                    return s;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static String extractActionKeyFromFields(KeyBinding kb) {
        Class<?> c = kb.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(kb);
                    if (v instanceof String s && isActionTranslationKey(s)) {
                        return s;
                    }
                } catch (Exception ignored) {
                }
            }
            c = c.getSuperclass();
        }

        return null;
    }

    private static boolean isActionTranslationKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (!key.startsWith("key.")) {
            return false;
        }
        if (key.startsWith("key.keyboard.") || key.startsWith("key.mouse.")) {
            return false;
        }
        return true;
    }

    private static String humanizeTranslation(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.split("\\.");
        String tail = parts.length == 0 ? key : parts[parts.length - 1];
        String spaced = tail.replace('_', ' ');
        if (spaced.isBlank()) {
            return key;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
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




