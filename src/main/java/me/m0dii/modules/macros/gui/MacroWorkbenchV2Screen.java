package me.m0dii.modules.macros.gui;

import me.m0dii.gui.GuiSystem;
import me.m0dii.modules.commandhistory.CommandHistoryManager;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
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
import java.util.regex.Pattern;

public class MacroWorkbenchV2Screen extends Screen {

    public enum Tab {
        CANVAS,
        KEYBOARD,
        PLACEHOLDERS,
        ENTITY_RADAR,
        COMMAND_HISTORY,
        MESSAGE_HISTORY
    }

    private static final int TOP_BAR_H = 54;
    private static final int BOTTOM_BAR_H = 44;
    private static final int CANVAS_CONTENT_TOP = 0;
    private static final int MODAL_W = 500;
    private static final int MODAL_H = 320;
    private static final String EXTERNAL_NBT_INSPECTOR_ID = "__ext_nbt_inspector";
    private static final String EXTERNAL_SECONDARY_CHAT_ID = "__ext_secondary_chat";
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
    private static final String[] ICON_ITEM_ID_PRESETS = {
            "minecraft:stone", "minecraft:diamond_sword", "minecraft:ender_pearl", "minecraft:totem_of_undying"
    };
    private static final String[] ICON_BLOCK_ID_PRESETS = {
            "minecraft:stone", "minecraft:chest", "minecraft:crafting_table", "minecraft:redstone_block"
    };
    private static final String[] ICON_ENTITY_ID_PRESETS = {
            "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:enderman"
    };
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
    private String advancedText = "";
    private String advancedAction = "";
    private String advancedBgColor = "";
    private String advancedBorderColor = "";
    private int advancedCursor = 0;
    private int advancedActionCursor = 0;
    private int advancedActionSuggestionIndex = -1;
    private int advancedActionSuggestionScroll = 0;
    private int advancedSelectionAnchor = -1;
    private int advancedActionSelectionAnchor = -1;
    private int advancedBgCursor = 0;
    private int advancedBorderCursor = 0;
    private int advancedBgSelectionAnchor = -1;
    private int advancedBorderSelectionAnchor = -1;
    private ModalDragSelectionField activeDragSelectionField = ModalDragSelectionField.NONE;
    private int snapGuideX = Integer.MIN_VALUE;
    private int snapGuideY = Integer.MIN_VALUE;

    private static List<String> ALL_ITEM_IDS;
    private static List<String> ALL_BLOCK_IDS;
    private static List<String> ALL_ENTITY_IDS;

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
    private ModConfig.ChatInterceptMode advancedSecondaryInterceptMode;

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

        ButtonWidget tabCanvas = ButtonWidget.builder(Text.literal("Canvas"), b -> setTab(Tab.CANVAS)).dimensions(8, 8, 70, 20).build();
        ButtonWidget tabKeyboard = ButtonWidget.builder(Text.literal("Keyboard"), b -> setTab(Tab.KEYBOARD)).dimensions(82, 8, 80, 20).build();
        ButtonWidget tabPlaceholders = ButtonWidget.builder(Text.literal("Placeholders"), b -> setTab(Tab.PLACEHOLDERS)).dimensions(166, 8, 102, 20).build();
        ButtonWidget tabEntityRadar = ButtonWidget.builder(Text.literal("Entity Radar"), b -> setTab(Tab.ENTITY_RADAR)).dimensions(272, 8, 92, 20).build();
        ButtonWidget tabCmdHistory = ButtonWidget.builder(Text.literal("Cmd History"), b -> setTab(Tab.COMMAND_HISTORY)).dimensions(368, 8, 90, 20).build();
        ButtonWidget tabMsgHistory = ButtonWidget.builder(Text.literal("Msg History"), b -> setTab(Tab.MESSAGE_HISTORY)).dimensions(462, 8, 90, 20).build();
        ButtonWidget saveButton = ButtonWidget.builder(Text.literal("Save"), b -> saveAll()).dimensions(this.width - 152, 8, 66, 20).build();
        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), b -> {
            saveAll();
            close();
        }).dimensions(this.width - 82, 8, 66, 20).build();
        this.topBarWidgets.add(tabCanvas);
        this.topBarWidgets.add(tabKeyboard);
        this.topBarWidgets.add(tabPlaceholders);
        this.topBarWidgets.add(tabEntityRadar);
        this.topBarWidgets.add(tabCmdHistory);
        this.topBarWidgets.add(tabMsgHistory);
        this.topBarWidgets.add(saveButton);
        this.topBarWidgets.add(doneButton);
        addDrawableChild(tabCanvas);
        addDrawableChild(tabKeyboard);
        addDrawableChild(tabPlaceholders);
        addDrawableChild(tabEntityRadar);
        addDrawableChild(tabCmdHistory);
        addDrawableChild(tabMsgHistory);
        addDrawableChild(saveButton);
        addDrawableChild(doneButton);

        ButtonWidget addElement = ButtonWidget.builder(Text.literal("Add Element"), b -> addElementModalOpen = true)
                .dimensions(8, 32, 92, 18).build();

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
        }).dimensions(104, 32, 74, 18).build();

        this.gridRowsMinusButton = ButtonWidget.builder(Text.literal("R-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.max(2, gridRows - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(182, 32, 24, 18).build();
        this.gridRowsPlusButton = ButtonWidget.builder(Text.literal("R+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.min(80, gridRows + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(210, 32, 24, 18).build();
        this.gridColsMinusButton = ButtonWidget.builder(Text.literal("C-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.max(2, gridCols - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(238, 32, 24, 18).build();
        this.gridColsPlusButton = ButtonWidget.builder(Text.literal("C+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.min(80, gridCols + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(266, 32, 24, 18).build();

        this.centerLinesToggleButton = ButtonWidget.builder(Text.literal("Center: OFF"), b -> {
            centerLinesEnabled = !centerLinesEnabled;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(294, 32, 86, 18).build();

        this.presetPrevButton = ButtonWidget.builder(Text.literal("<"), b -> cyclePreset(false)).dimensions(384, 32, 20, 18).build();
        this.presetNextButton = ButtonWidget.builder(Text.literal(">"), b -> cyclePreset(true)).dimensions(408, 32, 20, 18).build();
        this.presetNameField = new TextFieldWidget(this.textRenderer, 432, 32, 130, 18, Text.literal("Preset"));
        this.presetNameField.setMaxLength(40);
        this.presetNewButton = ButtonWidget.builder(Text.literal("New"), b -> createPresetFromField()).dimensions(566, 32, 38, 18).build();
        this.presetRenameButton = ButtonWidget.builder(Text.literal("Rename"), b -> renamePresetFromField()).dimensions(608, 32, 54, 18).build();
        this.presetDeleteButton = ButtonWidget.builder(Text.literal("Delete"), b -> {
            MacroHudDataHandler.deletePreset(MacroHudDataHandler.getActivePresetId());
            this.working = MacroHudDataHandler.getConfigCopy();
            syncExternalCanvasElementsFromSources();
            syncCanvasFields();
            syncGridButtons();
            syncPresetControls();
        }).dimensions(666, 32, 50, 18).build();

        this.quickField = new TextFieldWidget(this.textRenderer, 8, this.height - 38, 280, 18, Text.literal("Quick Edit"));
        this.quickField.setMaxLength(300);
        this.macroField = new TextFieldWidget(this.textRenderer, 292, this.height - 38, 120, 18, Text.literal("Macro Id"));
        this.macroField.setMaxLength(120);
        this.actionField = new TextFieldWidget(this.textRenderer, 416, this.height - 38, 180, 18, Text.literal("Action"));
        this.actionField.setMaxLength(220);

        this.backgroundToggle = ButtonWidget.builder(Text.literal("BG: OFF"), b -> {
            if (selected != null) {
                selected.drawBackground = !selected.drawBackground;
                ensureVisibleBackground(selected);
                syncStyleButtons();
            }
        }).dimensions(this.width - 316, this.height - 38, 74, 18).build();

        this.borderToggle = ButtonWidget.builder(Text.literal("Border: OFF"), b -> {
            if (selected != null) {
                selected.drawBorder = !selected.drawBorder;
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

        int panelX = this.width - 320;
        ButtonWidget gameToggle = ButtonWidget.builder(Text.literal("Show Game Binds: OFF"), b -> {
            showGameKeybinds = !showGameKeybinds;
            b.setMessage(Text.literal("Show Game Binds: " + (showGameKeybinds ? "ON" : "OFF")));
        }).dimensions(panelX + 10, TOP_BAR_H + 8, 300, 18).build();

        this.kbNameField = new TextFieldWidget(this.textRenderer, panelX + 10, this.height - 94, 300, 18, Text.literal("Macro Name"));
        this.kbCommandsField = new TextFieldWidget(this.textRenderer, panelX + 10, this.height - 72, 300, 18, Text.literal("Commands (; separated)"));
        this.kbDelayField = new TextFieldWidget(this.textRenderer, panelX + 10, this.height - 50, 120, 18, Text.literal("Delay Ticks"));
        this.kbNameField.setMaxLength(80);
        this.kbCommandsField.setMaxLength(500);
        this.kbDelayField.setMaxLength(6);

        ButtonWidget kbSave = ButtonWidget.builder(Text.literal("Save Macro"), b -> saveKeyboardMacro()).dimensions(panelX + 226, this.height - 50, 84, 18).build();
        ButtonWidget kbDelete = ButtonWidget.builder(Text.literal("Delete"), b -> deleteKeyboardMacro()).dimensions(panelX + 124, this.height - 28, 80, 18).build();
        ButtonWidget kbNew = ButtonWidget.builder(Text.literal("+ New on Key"), b -> createKeyboardMacro()).dimensions(panelX + 10, this.height - 28, 110, 18).build();
        ButtonWidget kbEditCommands = ButtonWidget.builder(Text.literal("Edit Cmds"), b -> openKeyboardCommandsModal())
                .dimensions(panelX + 134, this.height - 50, 88, 18).build();
        ButtonWidget kbOpenManager = ButtonWidget.builder(Text.literal("Open Macro Manager"),
                b -> this.client.setScreen(MacroConfigScreen.create(this))).dimensions(panelX + 208, this.height - 28, 102, 18).build();

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
        }
    }

    private void refreshTabWidgetVisibility() {
        boolean canvasTab = this.tab == Tab.CANVAS;
        boolean keyboardTab = this.tab == Tab.KEYBOARD;
        boolean entityRadarTab = this.tab == Tab.ENTITY_RADAR;
        boolean cmdHistoryTab = this.tab == Tab.COMMAND_HISTORY;
        boolean msgHistoryTab = this.tab == Tab.MESSAGE_HISTORY;
        // In canvas, F1 controls visibility; in other tabs controls are always visible.
        boolean showChrome = !canvasTab || canvasChromeVisible;

        setWidgetState(topBarWidgets, showChrome);
        setWidgetState(canvasWidgets, canvasTab && showChrome);
        setWidgetState(keyboardWidgets, keyboardTab && showChrome);
        setWidgetState(entityRadarWidgets, entityRadarTab && showChrome);
        setWidgetState(cmdHistoryWidgets, cmdHistoryTab && showChrome);
        setWidgetState(msgHistoryWidgets, msgHistoryTab && showChrome);
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
            context.drawTextWithShadow(this.textRenderer, "Action (optional)", 416, y - 10, 0xFFAAAAAA);
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
        List<String> docs = MacroPlaceholders.PLACEHOLDER_DOCS;
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


    private int getHistoryVisibleLines() {
        return Math.max(1, (this.height - TOP_BAR_H - 80) / HISTORY_LINE_HEIGHT);
    }

    private void renderCommandHistoryTab(DrawContext context, int mouseX, int mouseY) {
        int top = TOP_BAR_H + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Command History"), width / 2, top, 0xFFFFFF);
        String info = cmdHistoryItems.isEmpty() ? "No commands in history yet." : "Click any command to copy it to clipboard";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), width / 2, top + 14, 0xAAAAAA);

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
                int textColor = hovering ? 0xFFFF00 : 0xFFFFFF;
                int maxWidth = width - 50;
                if (textRenderer.getWidth(displayText) > maxWidth) {
                    while (textRenderer.getWidth(displayText + "...") > maxWidth && displayText.length() > 10)
                        displayText = displayText.substring(0, displayText.length() - 1);
                    displayText += "...";
                }
                context.drawTextWithShadow(this.textRenderer, displayText, 25, y, textColor);
                if (hovering) {
                    String hint = "[Click to copy]";
                    context.drawTextWithShadow(this.textRenderer, Text.literal(hint), boxX2 - textRenderer.getWidth(hint) - 5, y, 0xFFFF00);
                }
            }
            if (cmdHistoryItems.size() > getHistoryVisibleLines()) {
                int total = (cmdHistoryItems.size() + getHistoryVisibleLines() - 1) / getHistoryVisibleLines();
                int cur = (cmdHistoryScroll / getHistoryVisibleLines()) + 1;
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Page " + cur + "/" + total + " (Scroll to navigate)"),
                        width / 2, height - 52, 0x888888);
            }
        }
    }

    private void renderMessageHistoryTab(DrawContext context, int mouseX, int mouseY) {
        int top = TOP_BAR_H + 8;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Message History"), width / 2, top, 0xFFFFFF);
        String info = msgHistoryItems.isEmpty() ? "No messages in history yet." : "Click any message to copy it to clipboard";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), width / 2, top + 14, 0xAAAAAA);

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
                int textColor = hovering ? 0xFFFF00 : 0xFFFFFF;
                int maxWidth = width - 50;
                if (textRenderer.getWidth(displayText) > maxWidth) {
                    while (textRenderer.getWidth(displayText + "...") > maxWidth && displayText.length() > 10)
                        displayText = displayText.substring(0, displayText.length() - 1);
                    displayText += "...";
                }
                context.drawTextWithShadow(this.textRenderer, displayText, 25, y, textColor);
                if (hovering) {
                    String hint = "[Click to copy]";
                    context.drawTextWithShadow(this.textRenderer, Text.literal(hint), boxX2 - textRenderer.getWidth(hint) - 5, y, 0xFFFF00);
                }
            }
            if (msgHistoryItems.size() > getHistoryVisibleLines()) {
                int total = (msgHistoryItems.size() + getHistoryVisibleLines() - 1) / getHistoryVisibleLines();
                int cur = (msgHistoryScroll / getHistoryVisibleLines()) + 1;
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("Page " + cur + "/" + total + " (Scroll to navigate)"),
                        width / 2, height - 52, 0x888888);
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
        int panelX = this.width - 320;
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

        context.fill(panelX, TOP_BAR_H, this.width - 8, this.height - 8, 0xAA111111);
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
                context.fill(panelX + 8, y - 2, panelX + 312, y + 10, id.equals(selectedMacroId) ? 0x905A3A12 : 0x50202020);
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
        if (kbCommandsModalOpen) {
            return onKeyboardCommandsModalClick(click);
        }
        if (advancedOpen) {
            return onAdvancedMouseClick(click);
        }
        if (this.tab == Tab.CANVAS && addElementModalOpen) {
            return onAddElementModalClick(click);
        }

        if (super.mouseClicked(click, doubled)) {
            return true;
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
            if (selected != null && isCustomWidgetType(selected)) {
                int boxX = modalX();
                int boxY = modalY();
                int sourceX = boxX + 12;
                int sourceY = boxY + 72;
                int sourceW = 198;
                List<String> suggestions = advancedActionSuggestions();
                if (!suggestions.isEmpty()) {
                    int rowH = 10;
                    int bottomLimit = boxY + 208;
                    int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, (bottomLimit - (sourceY + 22)) / rowH)));
                    int dropY = sourceY + 22;
                    int dropH = maxVisible * rowH;
                    if (containsBox(mouseX, mouseY, sourceX, dropY, sourceW, dropH)) {
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
                    if (selectedElementIds.isEmpty()) {
                        selectedElementIds.add(e.id);
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
                if (isResizeHandleHit(ex, ey, e, mouseX, mouseY)) {
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
        this.dragging = false;
        this.resizing = false;
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

        int panelX = this.width - 320;
        int lineY = TOP_BAR_H + 76;
        for (String id : macroBindingIds.getOrDefault(selectedKey, List.of())) {
            if (x >= panelX + 8 && x <= panelX + 312 && y >= lineY - 2 && y <= lineY + 10) {
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
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = MODAL_W - 24;
        int textH = 108;
        int actionX = boxX + 12;
        int actionY = boxY + 148;
        int actionW = MODAL_W - 24;
        int actionH = 18;
        int bgFieldX = boxX + 238;
        int bgFieldY = boxY + 218;
        int bgFieldW = 88;
        int bgFieldH = 18;
        int borderFieldX = boxX + 356;
        int borderFieldY = boxY + 218;
        int borderFieldW = 88;
        int borderFieldH = 18;

        if (containsBox(click.x(), click.y(), boxX, boxY, MODAL_W, 20)) {
            advancedModalDragging = true;
            advancedModalDragOffsetX = (int) click.x() - boxX;
            advancedModalDragOffsetY = (int) click.y() - boxY;
            return true;
        }

        if (isSecondaryChatProxy(selected)) {
            int regexX = boxX + 12;
            int regexY = boxY + 112;
            int regexW = MODAL_W - 24;
            int regexH = 86;
            int outgoingX = boxX + 12;
            int outgoingY = boxY + 206;
            int outgoingW = 244;
            int outgoingH = 18;
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 44, 150, 18)) {
                advancedSecondaryShowWhileGuiOpen = !advancedSecondaryShowWhileGuiOpen;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 166, boxY + 44, 150, 18)) {
                advancedSecondaryFadeEnabled = !advancedSecondaryFadeEnabled;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 320, boxY + 44, 136, 18)) {
                advancedSecondaryResetTransparencyOnHover = !advancedSecondaryResetTransparencyOnHover;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 68, 220, 18)) {
                advancedSecondaryNoTransparencyWhenChatOpen = !advancedSecondaryNoTransparencyWhenChatOpen;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 236, boxY + 68, 64, 18)) {
                advancedSecondaryInterceptMode = advancedSecondaryInterceptMode == ModConfig.ChatInterceptMode.COPY
                        ? ModConfig.ChatInterceptMode.MOVE
                        : ModConfig.ChatInterceptMode.COPY;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 304, boxY + 68, 48, 18)) {
                advancedSecondaryScale = Math.max(0.1, advancedSecondaryScale - stepDouble(0.05));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 356, boxY + 68, 48, 18)) {
                advancedSecondaryScale = Math.min(3.0, advancedSecondaryScale + stepDouble(0.05));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 408, boxY + 68, 48, 18)) {
                advancedSecondaryLineHeight = Math.max(1, advancedSecondaryLineHeight - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 408, boxY + 92, 48, 18)) {
                advancedSecondaryLineHeight = Math.min(30, advancedSecondaryLineHeight + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 236, 48, 18)) {
                advancedSecondaryFadeDurationMs = Math.max(1000, advancedSecondaryFadeDurationMs - stepInt(1000));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 64, boxY + 236, 48, 18)) {
                advancedSecondaryFadeDurationMs = Math.min(120000, advancedSecondaryFadeDurationMs + stepInt(1000));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 116, boxY + 236, 48, 18)) {
                advancedSecondaryMinAlpha = Math.max(0, advancedSecondaryMinAlpha - stepInt(5));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 168, boxY + 236, 48, 18)) {
                advancedSecondaryMinAlpha = Math.min(255, advancedSecondaryMinAlpha + stepInt(5));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 220, boxY + 236, 48, 18)) {
                advancedSecondaryMaxLines = Math.max(10, advancedSecondaryMaxLines - stepInt(10));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 272, boxY + 236, 48, 18)) {
                advancedSecondaryMaxLines = Math.min(500, advancedSecondaryMaxLines + stepInt(10));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 324, boxY + 236, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 236, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 324, boxY + 274, 64, 18)) {
                selected.textColor = cycleStyleColor(selected.textColor, false);
                advancedBorderColor = formatColor(selected.textColor);
                advancedBorderCursor = advancedBorderColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 274, 64, 18)) {
                selected.textColor = cycleStyleColor(selected.textColor, true);
                advancedBorderColor = formatColor(selected.textColor);
                advancedBorderCursor = advancedBorderColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18)) {
                applyAdvancedAndClose();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18)) {
                closeAdvancedModal();
                return true;
            }

            advancedBgColorFocused = containsBox(click.x(), click.y(), boxX + 324, boxY + 274, 64, 18);
            if (advancedBgColorFocused) {
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBorderColorFocused = false;
                advancedBgSelectionAnchor = -1;
                int localX = (int) (click.x() - (boxX + 328));
                advancedBgCursor = cursorIndexFromPoint(advancedBgColor, localX, 0, 9);
                return true;
            }

            advancedBorderColorFocused = containsBox(click.x(), click.y(), boxX + 392, boxY + 274, 64, 18);
            if (advancedBorderColorFocused) {
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBgColorFocused = false;
                advancedBorderSelectionAnchor = -1;
                int localX = (int) (click.x() - (boxX + 396));
                advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, localX, 0, 9);
                return true;
            }

            advancedActionFocused = containsBox(click.x(), click.y(), outgoingX, outgoingY, outgoingW, outgoingH);
            if (advancedActionFocused) {
                advancedTextFocused = false;
                advancedBgColorFocused = false;
                advancedBorderColorFocused = false;
                advancedActionSelectionAnchor = -1;
                int localX = (int) (click.x() - (outgoingX + 4));
                advancedActionCursor = cursorIndexFromPoint(advancedAction, localX, 0, 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
                return true;
            }

            advancedTextFocused = containsBox(click.x(), click.y(), regexX, regexY, regexW, regexH);
            if (advancedTextFocused) {
                advancedActionFocused = false;
                advancedBgColorFocused = false;
                advancedBorderColorFocused = false;
                advancedSelectionAnchor = -1;
                advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (regexX + 4)), (int) (click.y() - (regexY + 4)), 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
            }
            return true;
        }

        if (isNbtInspectorProxy(selected)) {
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 52, 64, 18)) {
                selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 80, boxY + 52, 64, 18)) {
                selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 76, 64, 18)) {
                selected.lineHeight = Math.clamp(selected.lineHeight - stepInt(1), 6, 24);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 80, boxY + 76, 64, 18)) {
                selected.lineHeight = Math.clamp(selected.lineHeight + stepInt(1), 6, 24);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 100, 120, 18)) {
                selected.drawBackground = !selected.drawBackground;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 136, boxY + 100, 120, 18)) {
                selected.drawBorder = !selected.drawBorder;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 260, boxY + 100, 196, 18)) {
                selected.visible = !selected.visible;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 132, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 80, boxY + 132, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 148, boxY + 132, 64, 18)) {
                selected.textColor = cycleStyleColor(selected.textColor, false);
                advancedBorderColor = formatColor(selected.textColor);
                advancedBorderCursor = advancedBorderColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 216, boxY + 132, 64, 18)) {
                selected.textColor = cycleStyleColor(selected.textColor, true);
                advancedBorderColor = formatColor(selected.textColor);
                advancedBorderCursor = advancedBorderColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18)) {
                applyAdvancedAndClose();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18)) {
                closeAdvancedModal();
                return true;
            }
            return true;
        }

        if (isCustomWidgetType(selected)) {
            return onCustomWidgetAdvancedClick(click, boxX, boxY);
        }

        if (containsBox(click.x(), click.y(), boxX + 12, boxY + 172, 56, 18)) {
            if (selected != null) {
                selected.drawBackground = !selected.drawBackground;
                ensureVisibleBackground(selected);
                syncStyleButtons();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 72, boxY + 172, 66, 18)) {
            if (selected != null) {
                selected.drawBorder = !selected.drawBorder;
                syncStyleButtons();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 142, boxY + 172, 92, 18)) {
            if (selected != null) {
                selected.horizontalAlign = cycleHorizontalAlign(selected.horizontalAlign, forward);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 238, boxY + 172, 92, 18)) {
            if (selected != null) {
                selected.verticalAlign = cycleVerticalAlign(selected.verticalAlign, forward);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 334, boxY + 172, 122, 18)) {
            if (selected != null) {
                int oldScreenX = resolveElementX(selected);
                int oldScreenY = resolveElementY(selected);
                selected.anchor = cycleAnchor(selected.anchor, forward);
                setElementScreenPosition(selected, oldScreenX, oldScreenY);
                clampElementToCanvas(selected);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 196, boxY + 196, 80, 18)) {
            if (selected != null) {
                selected.visibilityMode = selected.visibilityMode == MacroHudDataHandler.VisibilityMode.ALWAYS
                        ? MacroHudDataHandler.VisibilityMode.CHAT_ONLY
                        : MacroHudDataHandler.VisibilityMode.ALWAYS;
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 12, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.lineHeight = Math.clamp(selected.lineHeight - stepInt(1), 6, 24);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 56, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.lineHeight = Math.clamp(selected.lineHeight + stepInt(1), 6, 24);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 104, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 148, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 280, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                ensureVisibleBackground(selected);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 324, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                ensureVisibleBackground(selected);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 368, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.borderColor = cycleStyleColor(selected.borderColor, false);
                advancedBorderColor = formatColor(selected.borderColor);
                advancedBorderCursor = advancedBorderColor.length();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 412, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.borderColor = cycleStyleColor(selected.borderColor, true);
                advancedBorderColor = formatColor(selected.borderColor);
                advancedBorderCursor = advancedBorderColor.length();
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18)) {
            applyAdvancedAndClose();
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18)) {
            closeAdvancedModal();
            return true;
        }

        advancedBgColorFocused = containsBox(click.x(), click.y(), bgFieldX, bgFieldY, bgFieldW, bgFieldH);
        if (advancedBgColorFocused) {
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBorderColorFocused = false;
            advancedBgSelectionAnchor = -1;
            int localX = (int) (click.x() - (bgFieldX + 4));
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
            return true;
        }

        advancedBorderColorFocused = containsBox(click.x(), click.y(), borderFieldX, borderFieldY, borderFieldW, borderFieldH);
        if (advancedBorderColorFocused) {
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderSelectionAnchor = -1;
            int localX = (int) (click.x() - (borderFieldX + 4));
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
            return true;
        }

        advancedActionFocused = containsBox(click.x(), click.y(), actionX, actionY, actionW, actionH);
        if (advancedActionFocused) {
            advancedTextFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedActionSelectionAnchor = -1;
            int localX = (int) (click.x() - (actionX + 4));
            advancedActionCursor = cursorIndexFromPoint(advancedAction, localX, 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
            return true;
        }

        advancedTextFocused = containsBox(click.x(), click.y(), textX, textY, textW, textH);
        if (advancedTextFocused) {
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedSelectionAnchor = -1;
            advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (textX + 4)), (int) (click.y() - (textY + 4)), 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
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
            selected.width = Math.clamp(resizeStartWidth + (mouseX - resizeStartMouseX), 1, 2000);
            selected.height = Math.clamp(resizeStartHeight + (mouseY - resizeStartMouseY), 1, 1200);

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
        if (moving == null) {
            return new int[]{screenX, screenY};
        }
        final int threshold = 8;
        int bestDx = threshold + 1;
        int bestDy = threshold + 1;
        int snappedX = screenX;
        int snappedY = screenY;

        int movingLeft = screenX;
        int movingRight = screenX + moving.width;
        int movingCenterX = screenX + moving.width / 2;
        int movingTop = screenY;
        int movingBottom = screenY + moving.height;
        int movingCenterY = screenY + moving.height / 2;

        for (MacroHudDataHandler.HudElement other : this.working.elements) {
            if (other == null || other.id.equals(moving.id) || excludedIds.contains(other.id)) {
                continue;
            }
            int ox = resolveElementX(other);
            int oy = resolveElementY(other);
            int otherLeft = ox;
            int otherRight = ox + other.width;
            int otherCenterX = ox + other.width / 2;
            int otherTop = oy;
            int otherBottom = oy + other.height;
            int otherCenterY = oy + other.height / 2;

            int[] xCandidates = {
                    otherLeft,
                    otherRight,
                    otherCenterX,
                    otherLeft - moving.width,
                    otherRight - moving.width,
                    otherCenterX - moving.width / 2
            };
            int[] yCandidates = {
                    otherTop,
                    otherBottom,
                    otherCenterY,
                    otherTop - moving.height,
                    otherBottom - moving.height,
                    otherCenterY - moving.height / 2
            };

            for (int candidate : xCandidates) {
                int dx = Math.abs(candidate - movingLeft);
                if (dx < bestDx && dx <= threshold) {
                    bestDx = dx;
                    snappedX = candidate;
                }
                int dxRight = Math.abs(candidate - movingRight);
                if (dxRight < bestDx && dxRight <= threshold) {
                    bestDx = dxRight;
                    snappedX = candidate - moving.width;
                }
                int dxCenter = Math.abs(candidate - movingCenterX);
                if (dxCenter < bestDx && dxCenter <= threshold) {
                    bestDx = dxCenter;
                    snappedX = candidate - moving.width / 2;
                }
            }

            for (int candidate : yCandidates) {
                int dy = Math.abs(candidate - movingTop);
                if (dy < bestDy && dy <= threshold) {
                    bestDy = dy;
                    snappedY = candidate;
                }
                int dyBottom = Math.abs(candidate - movingBottom);
                if (dyBottom < bestDy && dyBottom <= threshold) {
                    bestDy = dyBottom;
                    snappedY = candidate - moving.height;
                }
                int dyCenter = Math.abs(candidate - movingCenterY);
                if (dyCenter < bestDy && dyCenter <= threshold) {
                    bestDy = dyCenter;
                    snappedY = candidate - moving.height / 2;
                }
            }
        }

        snappedX = Math.clamp(snappedX, 0, Math.max(0, this.width - moving.width));
        snappedY = Math.clamp(snappedY, CANVAS_CONTENT_TOP, Math.max(CANVAS_CONTENT_TOP, this.height - BOTTOM_BAR_H - moving.height));
        snapGuideX = Math.abs(snappedX - screenX) > 0 ? snappedX : Integer.MIN_VALUE;
        snapGuideY = Math.abs(snappedY - screenY) > 0 ? snappedY : Integer.MIN_VALUE;
        return new int[]{snappedX, snappedY};
    }

    private void drawSnapGuides(DrawContext context) {
        if (!dragging || !isShiftDown()) {
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
        } else if (isNbtInspectorProxy(element)) {
            lines = buildExternalProxyPreviewLines(element, "NBT Inspector");
        } else if (element.type == MacroHudDataHandler.ElementType.MACRO_KEYBINDS) {
            lines = buildMacroKeybindPreviewLines(element);
        } else {
            lines = splitLines(expandForCanvas(element.type == MacroHudDataHandler.ElementType.TEXT ? element.text : element.label));
        }

        if (element.drawBackground) {
            context.fill(x1, y1, x2, y2, element.backgroundColor);
        }
        if (element.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, element.borderColor);
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
            context.fill(x1, y1, x2, y2, e.backgroundColor);
        }
        if (e.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
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
            context.fill(x1, y1, x2, y2, e.backgroundColor);
        }
        if (e.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
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
            context.fill(x1, y1, x2, y2, e.backgroundColor);
        }
        if (e.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
        }
        Double valueToken = resolveCanvasNumericToken(e.sourceToken);
        double value = valueToken == null ? 0.0 : valueToken;
        String prefix = safe(e.prefix);
        if (prefix.isBlank()) {
            String label = safe(e.label);
            if (!label.isBlank() && !"Value".equalsIgnoreCase(label)) {
                prefix = label + ": ";
            }
        }
        String text = prefix + formatCanvasValue(value) + safe(e.suffix);
        float scale = Math.max(0.5f, e.fontScale);
        int tw = Math.max(1, Math.round(styledLineWidth(text) * scale));
        int tx = alignedStartX(x1, e, tw, true);
        int ty = alignedStartY(e, Math.max(9, Math.round(9 * scale)), true);
        drawStyledTextLine(context, text, tx, ty, e.textColor, scale);
    }

    private void drawCanvasListPreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, e.backgroundColor);
        }
        if (e.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
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
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
        }
    }

    private void drawCanvasStateBadgePreview(DrawContext context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        boolean on = true;
        int bg = on ? e.colorStart : e.colorEnd;
        context.fill(x1, y1, x2, y2, bg);
        if (e.drawBorder) {
            GuiSystem.drawOutline(context, x1, y1, x2, y2, e.borderColor);
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
            quickField.setText(isSecondaryChatProxy(selected)
                    ? "Secondary Chat (edit style/pos/size)" + (externalState == ExternalProxyRenderState.MODULE_DISABLED ? " [DISABLED]" : "")
                    : "NBT Inspector (edit style/pos/size)" + (externalState == ExternalProxyRenderState.MODULE_DISABLED ? " [DISABLED]" : ""));
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
            selected.buttonAction = actionField.getText() == null ? "" : actionField.getText().trim();
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
        backgroundToggle.setMessage(Text.literal(selected.drawBackground ? "BG: ON" : "BG: OFF"));
        borderToggle.setMessage(Text.literal(selected.drawBorder ? "Border: ON" : "Border: OFF"));
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
        this.advancedActionFocused = false;
        this.advancedBgColorFocused = false;
        this.advancedBorderColorFocused = false;
        this.advancedText = selected.type == MacroHudDataHandler.ElementType.TEXT ? safe(selected.text) : safe(selected.label);
        this.advancedAction = selected.type == MacroHudDataHandler.ElementType.BUTTON ? safe(selected.buttonAction) : safe(selected.sourceToken);
        this.advancedBgColor = formatColor(selected.backgroundColor);
        this.advancedBorderColor = formatColor(selected.borderColor);
        this.advancedCursor = this.advancedText.length();
        this.advancedActionCursor = this.advancedAction.length();
        this.advancedActionSuggestionIndex = -1;
        this.advancedActionSuggestionScroll = 0;
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
        this.advancedSelectionAnchor = -1;
        this.advancedActionSelectionAnchor = -1;
        this.advancedBgSelectionAnchor = -1;
        this.advancedBorderSelectionAnchor = -1;
        this.advancedBgCursor = this.advancedBgColor.length();
        this.advancedBorderCursor = this.advancedBorderColor.length();
        this.advancedActionSuggestionIndex = -1;
        if (isSecondaryChatProxy(selected)) {
            this.advancedText = String.join("\n", ModConfig.secondaryChatRegexList == null ? List.of() : ModConfig.secondaryChatRegexList);
            this.advancedAction = ModConfig.secondaryChatOutgoingRegex == null ? "" : ModConfig.secondaryChatOutgoingRegex;
            this.advancedBgColor = formatColor(selected.backgroundColor);
            this.advancedBorderColor = formatColor(selected.textColor);
            this.advancedBgCursor = this.advancedBgColor.length();
            this.advancedBorderCursor = this.advancedBorderColor.length();
            this.advancedSecondaryShowWhileGuiOpen = ModConfig.secondaryChatShowWhileGuiOpen;
            this.advancedSecondaryFadeEnabled = ModConfig.secondaryChatFadeEnabled;
            this.advancedSecondaryResetTransparencyOnHover = ModConfig.resetTransparencyWhenHovered;
            this.advancedSecondaryNoTransparencyWhenChatOpen = ModConfig.noTransparencyWhenChatOpen;
            this.advancedSecondaryFadeDurationMs = ModConfig.secondaryChatFadeDurationMs;
            this.advancedSecondaryMinAlpha = ModConfig.secondaryChatMinAlpha;
            this.advancedSecondaryMaxLines = ModConfig.secondaryChatMaxLines;
            this.advancedSecondaryScale = Math.max(0.1, ModConfig.secondaryChatScale);
            this.advancedSecondaryLineHeight = Math.max(1, ModConfig.secondaryChatLineHeight);
            this.advancedSecondaryInterceptMode = ModConfig.secondaryChatInterceptMode;
        } else if (isNbtInspectorProxy(selected)) {
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
            }
        }
        ensureVisibleBackground(selected);
    }

    private void renderAdvancedModal(DrawContext context, int mouseX, int mouseY) {
        int boxX = modalX();
        int boxY = modalY();
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = MODAL_W - 24;
        int textH = 108;
        int actionX = boxX + 12;
        int actionY = boxY + 148;
        int actionW = MODAL_W - 24;
        int actionH = 18;
        int bgFieldX = boxX + 238;
        int bgFieldY = boxY + 218;
        int bgFieldW = 88;
        int bgFieldH = 18;
        int borderFieldX = boxX + 356;
        int borderFieldY = boxY + 218;
        int borderFieldW = 88;
        int borderFieldH = 18;

        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, MODAL_W, MODAL_H);

        if (isSecondaryChatProxy(selected)) {
            renderSecondaryChatAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }
        if (isNbtInspectorProxy(selected)) {
            renderNbtInspectorAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }
        if (isCustomWidgetType(selected)) {
            renderCustomWidgetAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }

        context.drawTextWithShadow(this.textRenderer, "Edit", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Multi-line editor (Enter for new line)", boxX + 12, boxY + 22, 0xFFB0B0B0);

        context.fill(textX, textY, textX + textW, textY + textH, advancedTextFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textX, textY, textX + textW, textY + 1, 0x60FFFFFF);
        drawMultilineSelection(context, textX + 4, textY + 4, textY + textH - 12, advancedText, advancedSelectionAnchor, advancedCursor, 9);

        List<String> lines = splitLinesRaw(advancedText);
        int y = textY + 4;
        for (String line : lines) {
            if (y > textY + textH - 12) {
                break;
            }
            context.drawTextWithShadow(this.textRenderer, line, textX + 4, y, 0xFFEAEAEA);
            y += 9;
        }

        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = cursorPixel(textX + 4, textY + 4, advancedText, advancedCursor);
            context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
        }

        context.drawTextWithShadow(this.textRenderer, "Button Action (cmd:/ msg:/ copy:/ etc)", actionX, actionY - 10, 0xFFB8B8B8);
        int actionBg = advancedActionFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(actionX, actionY, actionX + actionW, actionY + actionH, actionBg);
        context.fill(actionX, actionY, actionX + actionW, actionY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, actionX + 4, actionY + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, actionX + 4, actionY + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = actionX + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, actionY + 4, ax + 1, actionY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + 12, boxY + 172, 56, 18, "BG: " + ((selected != null && selected.drawBackground) ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 12, boxY + 172, 56, 18));
        drawModalButton(context, boxX + 72, boxY + 172, 66, 18, "Border: " + ((selected != null && selected.drawBorder) ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 72, boxY + 172, 66, 18));
        drawModalButton(context, boxX + 142, boxY + 172, 92, 18, "H: " + (selected != null ? selected.horizontalAlign.name() : "-"), containsBox(mouseX, mouseY, boxX + 142, boxY + 172, 92, 18));
        drawModalButton(context, boxX + 238, boxY + 172, 92, 18, "V: " + (selected != null ? selected.verticalAlign.name() : "-"), containsBox(mouseX, mouseY, boxX + 238, boxY + 172, 92, 18));
        drawModalButton(context, boxX + 334, boxY + 172, 122, 18, "Anchor: " + (selected != null ? shortAnchor(selected.anchor) : "-"), containsBox(mouseX, mouseY, boxX + 334, boxY + 172, 122, 18));

        drawModalButton(context, boxX + 12, boxY + 196, 40, 18, "LH-", containsBox(mouseX, mouseY, boxX + 12, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 56, boxY + 196, 40, 18, "LH+", containsBox(mouseX, mouseY, boxX + 56, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 104, boxY + 196, 40, 18, "FS-", containsBox(mouseX, mouseY, boxX + 104, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 148, boxY + 196, 40, 18, "FS+", containsBox(mouseX, mouseY, boxX + 148, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 196, boxY + 196, 80, 18, "Vis: " + (selected != null ? shortVisibility(selected.visibilityMode) : "-"), containsBox(mouseX, mouseY, boxX + 196, boxY + 196, 80, 18));
        drawModalButton(context, boxX + 280, boxY + 196, 40, 18, "BG-", containsBox(mouseX, mouseY, boxX + 280, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 324, boxY + 196, 40, 18, "BG+", containsBox(mouseX, mouseY, boxX + 324, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 368, boxY + 196, 40, 18, "BR-", containsBox(mouseX, mouseY, boxX + 368, boxY + 196, 40, 18));
        drawModalButton(context, boxX + 412, boxY + 196, 40, 18, "BR+", containsBox(mouseX, mouseY, boxX + 412, boxY + 196, 40, 18));
        context.drawTextWithShadow(this.textRenderer, "Line: " + (selected != null ? selected.lineHeight : 9), boxX + 12, boxY + 220, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "Scale: " + (selected != null ? String.format("%.1f", selected.fontScale) : "1.0"), boxX + 88, boxY + 220, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "BG", boxX + 212, boxY + 224, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "BR", boxX + 332, boxY + 224, 0xFFEAEAEA);

        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(bgFieldX, bgFieldY, bgFieldX + bgFieldW, bgFieldY + bgFieldH, bgInputBg);
        context.fill(bgFieldX, bgFieldY, bgFieldX + bgFieldW, bgFieldY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, bgFieldX + 4, bgFieldY + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, bgFieldX + 4, bgFieldY + 5, 0xFFEAEAEA);

        int borderInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(borderFieldX, borderFieldY, borderFieldX + borderFieldW, borderFieldY + borderFieldH, borderInputBg);
        context.fill(borderFieldX, borderFieldY, borderFieldX + borderFieldW, borderFieldY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, borderFieldX + 4, borderFieldY + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, borderFieldX + 4, borderFieldY + 5, 0xFFEAEAEA);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = bgFieldX + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, bgFieldY + 4, cx + 1, bgFieldY + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = borderFieldX + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, borderFieldY + 4, cx + 1, borderFieldY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18, "Apply", containsBox(mouseX, mouseY, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18));
        drawModalButton(context, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18, "Cancel", containsBox(mouseX, mouseY, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18));
    }

    private void renderSecondaryChatAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        context.drawTextWithShadow(this.textRenderer, "Secondary Chat Settings", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Canvas controls position/size. This panel edits behavior.", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, boxX + 12, boxY + 44, 150, 18,
                "GUI Open: " + (advancedSecondaryShowWhileGuiOpen ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 12, boxY + 44, 150, 18));
        drawModalButton(context, boxX + 166, boxY + 44, 150, 18,
                "Fade: " + (advancedSecondaryFadeEnabled ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 166, boxY + 44, 150, 18));
        drawModalButton(context, boxX + 320, boxY + 44, 136, 18,
                "Hover Reset: " + (advancedSecondaryResetTransparencyOnHover ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 320, boxY + 44, 136, 18));
        drawModalButton(context, boxX + 12, boxY + 68, 220, 18,
                "No Transparency In Chat: " + (advancedSecondaryNoTransparencyWhenChatOpen ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 12, boxY + 68, 220, 18));
        drawModalButton(context, boxX + 236, boxY + 68, 64, 18,
                "Mode: " + (advancedSecondaryInterceptMode == null ? "COPY" : advancedSecondaryInterceptMode.name()),
                containsBox(mouseX, mouseY, boxX + 236, boxY + 68, 64, 18));
        drawModalButton(context, boxX + 304, boxY + 68, 48, 18, "S-", containsBox(mouseX, mouseY, boxX + 304, boxY + 68, 48, 18));
        drawModalButton(context, boxX + 356, boxY + 68, 48, 18, "S+", containsBox(mouseX, mouseY, boxX + 356, boxY + 68, 48, 18));
        drawModalButton(context, boxX + 408, boxY + 68, 48, 18, "LH-", containsBox(mouseX, mouseY, boxX + 408, boxY + 68, 48, 18));
        drawModalButton(context, boxX + 408, boxY + 92, 48, 18, "LH+", containsBox(mouseX, mouseY, boxX + 408, boxY + 92, 48, 18));
        context.drawTextWithShadow(this.textRenderer,
                "Scale: " + String.format("%.2f", advancedSecondaryScale) + "  Line: " + advancedSecondaryLineHeight,
                boxX + 304, boxY + 96, 0xFFEAEAEA);

        int regexX = boxX + 12;
        int regexY = boxY + 112;
        int regexW = MODAL_W - 24;
        int regexH = 86;
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

        int outgoingX = boxX + 12;
        int outgoingY = boxY + 206;
        int outgoingW = 244;
        int outgoingH = 18;
        context.drawTextWithShadow(this.textRenderer, "Outgoing Regex", outgoingX, outgoingY - 10, 0xFFB8B8B8);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + outgoingH, advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, outgoingX + 4, outgoingY + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, outgoingX + 4, outgoingY + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = outgoingX + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, outgoingY + 4, ax + 1, outgoingY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + 12, boxY + 236, 48, 18, "FD-", containsBox(mouseX, mouseY, boxX + 12, boxY + 236, 48, 18));
        drawModalButton(context, boxX + 64, boxY + 236, 48, 18, "FD+", containsBox(mouseX, mouseY, boxX + 64, boxY + 236, 48, 18));
        drawModalButton(context, boxX + 116, boxY + 236, 48, 18, "A-", containsBox(mouseX, mouseY, boxX + 116, boxY + 236, 48, 18));
        drawModalButton(context, boxX + 168, boxY + 236, 48, 18, "A+", containsBox(mouseX, mouseY, boxX + 168, boxY + 236, 48, 18));
        drawModalButton(context, boxX + 220, boxY + 236, 48, 18, "L-", containsBox(mouseX, mouseY, boxX + 220, boxY + 236, 48, 18));
        drawModalButton(context, boxX + 272, boxY + 236, 48, 18, "L+", containsBox(mouseX, mouseY, boxX + 272, boxY + 236, 48, 18));
        context.drawTextWithShadow(this.textRenderer,
                "Fade: " + advancedSecondaryFadeDurationMs + "ms  Alpha: " + advancedSecondaryMinAlpha + "  Max: " + advancedSecondaryMaxLines,
                boxX + 12, boxY + 260, 0xFFEAEAEA);

        drawModalButton(context, boxX + 324, boxY + 236, 64, 18, "BG-", containsBox(mouseX, mouseY, boxX + 324, boxY + 236, 64, 18));
        drawModalButton(context, boxX + 392, boxY + 236, 64, 18, "BG+", containsBox(mouseX, mouseY, boxX + 392, boxY + 236, 64, 18));
        context.drawTextWithShadow(this.textRenderer, "BG", boxX + 324, boxY + 268, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "TX", boxX + 392, boxY + 268, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(boxX + 324, boxY + 274, boxX + 388, boxY + 292, bgInputBg);
        context.fill(boxX + 324, boxY + 274, boxX + 388, boxY + 275, 0x60FFFFFF);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, boxX + 328, boxY + 279, 0xFFEAEAEA);
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(boxX + 392, boxY + 274, boxX + 456, boxY + 292, txInputBg);
        context.fill(boxX + 392, boxY + 274, boxX + 456, boxY + 275, 0x60FFFFFF);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, boxX + 396, boxY + 279, 0xFFEAEAEA);

        drawModalButton(context, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18, "Apply",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18));
        drawModalButton(context, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18, "Cancel",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18));

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = boxX + 328 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, boxY + 278, cx + 1, boxY + 287, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = boxX + 396 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, boxY + 278, cx + 1, boxY + 287, 0xFFFFFFFF);
        }
    }

    private void renderNbtInspectorAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        context.drawTextWithShadow(this.textRenderer, "NBT Inspector Settings", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Use canvas for positioning, this panel for style", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, boxX + 12, boxY + 52, 64, 18, "S-", containsBox(mouseX, mouseY, boxX + 12, boxY + 52, 64, 18));
        drawModalButton(context, boxX + 80, boxY + 52, 64, 18, "S+", containsBox(mouseX, mouseY, boxX + 80, boxY + 52, 64, 18));
        drawModalButton(context, boxX + 12, boxY + 76, 64, 18, "LH-", containsBox(mouseX, mouseY, boxX + 12, boxY + 76, 64, 18));
        drawModalButton(context, boxX + 80, boxY + 76, 64, 18, "LH+", containsBox(mouseX, mouseY, boxX + 80, boxY + 76, 64, 18));
        context.drawTextWithShadow(this.textRenderer,
                "Scale: " + String.format("%.2f", selected == null ? 1.0f : selected.fontScale) +
                        "  Line: " + (selected == null ? 9 : selected.lineHeight),
                boxX + 150, boxY + 62, 0xFFEAEAEA);

        drawModalButton(context, boxX + 12, boxY + 100, 120, 18,
                "BG: " + ((selected != null && selected.drawBackground) ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 12, boxY + 100, 120, 18));
        drawModalButton(context, boxX + 136, boxY + 100, 120, 18,
                "Border: " + ((selected != null && selected.drawBorder) ? "ON" : "OFF"),
                containsBox(mouseX, mouseY, boxX + 136, boxY + 100, 120, 18));
        drawModalButton(context, boxX + 260, boxY + 100, 196, 18,
                "Visible: " + ((selected != null && selected.visible) ? "YES" : "NO"),
                containsBox(mouseX, mouseY, boxX + 260, boxY + 100, 196, 18));

        drawModalButton(context, boxX + 12, boxY + 132, 64, 18, "BG-", containsBox(mouseX, mouseY, boxX + 12, boxY + 132, 64, 18));
        drawModalButton(context, boxX + 80, boxY + 132, 64, 18, "BG+", containsBox(mouseX, mouseY, boxX + 80, boxY + 132, 64, 18));
        drawModalButton(context, boxX + 148, boxY + 132, 64, 18, "TX-", containsBox(mouseX, mouseY, boxX + 148, boxY + 132, 64, 18));
        drawModalButton(context, boxX + 216, boxY + 132, 64, 18, "TX+", containsBox(mouseX, mouseY, boxX + 216, boxY + 132, 64, 18));

        context.drawTextWithShadow(this.textRenderer, "BG: " + advancedBgColor, boxX + 12, boxY + 158, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "TX: " + advancedBorderColor, boxX + 170, boxY + 158, 0xFFEAEAEA);

        drawModalButton(context, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18, "Apply",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18));
        drawModalButton(context, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18, "Cancel",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18));
    }

    private void renderCustomWidgetAdvancedModal(DrawContext context, int mouseX, int mouseY, int boxX, int boxY) {
        if (selected == null) {
            return;
        }
        int labelX = boxX + 12;
        int labelY = boxY + 44;
        int labelW = 214;
        int labelH = 18;
        int sourceX = boxX + 12;
        int sourceY = boxY + 72;
        int sourceW = 198;
        int sourceH = 18;

        context.drawTextWithShadow(this.textRenderer, selected.type + " Widget", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON
                        ? "Label + icon id + type-specific controls"
                        : "Label + source token + type-specific controls",
                boxX + 12, boxY + 24, 0xFFB0B0B0);

        context.drawTextWithShadow(this.textRenderer, "Label", labelX, labelY - 10, 0xFFB8B8B8);
        context.fill(labelX, labelY, labelX + labelW, labelY + labelH, advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(labelX, labelY, labelX + labelW, labelY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, labelX + 4, labelY + 5, advancedText, advancedSelectionAnchor, advancedCursor);
        context.drawTextWithShadow(this.textRenderer, advancedText, labelX + 4, labelY + 5, 0xFFEAEAEA);

        context.drawTextWithShadow(this.textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON ? "Icon id" : "Source token",
                sourceX, sourceY - 10, 0xFFB8B8B8);
        context.fill(sourceX, sourceY, sourceX + sourceW, sourceY + sourceH, advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(sourceX, sourceY, sourceX + sourceW, sourceY + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, sourceX + 4, sourceY + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(this.textRenderer, advancedAction, sourceX + 4, sourceY + 5, 0xFFEAEAEA);
        List<String> suggestions = advancedActionSuggestions();
        if (!suggestions.isEmpty()) {
            int dropX = sourceX;
            int dropY = sourceY + 22;
            int dropW = sourceW;
            int rowH = 10;
            int bottomLimit = boxY + 208;
            int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, (bottomLimit - dropY) / rowH)));
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
                context.drawTextWithShadow(this.textRenderer, token, dropX + 3, yy + 1, 0xFF8FC8FF);
            }
            if (suggestions.size() > maxVisible) {
                context.drawTextWithShadow(this.textRenderer,
                        "scroll " + (advancedActionSuggestionScroll + 1) + "/" + (maxScroll + 1),
                        dropX + 3, dropY + maxVisible * rowH - 9, 0xFF909090);
            }
        }

        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = labelX + 4 + this.textRenderer.getWidth(advancedText.substring(0, Math.clamp(advancedCursor, 0, advancedText.length())));
            context.fill(cx, labelY + 4, cx + 1, labelY + 13, 0xFFFFFFFF);
        }
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = sourceX + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(cx, sourceY + 4, cx + 1, sourceY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + 232, boxY + 38, 54, 18, "BG-", containsBox(mouseX, mouseY, boxX + 232, boxY + 38, 54, 18));
        drawModalButton(context, boxX + 290, boxY + 38, 54, 18, "BG+", containsBox(mouseX, mouseY, boxX + 290, boxY + 38, 54, 18));
        drawModalButton(context, boxX + 348, boxY + 38, 54, 18, "BR-", containsBox(mouseX, mouseY, boxX + 348, boxY + 38, 54, 18));
        drawModalButton(context, boxX + 406, boxY + 38, 50, 18, "BR+", containsBox(mouseX, mouseY, boxX + 406, boxY + 38, 50, 18));
        drawModalButton(context, boxX + 232, boxY + 64, 54, 18, "FS-", containsBox(mouseX, mouseY, boxX + 232, boxY + 64, 54, 18));
        drawModalButton(context, boxX + 290, boxY + 64, 54, 18, "FS+", containsBox(mouseX, mouseY, boxX + 290, boxY + 64, 54, 18));
        drawModalButton(context, boxX + 348, boxY + 64, 108, 18, "Pick Src", containsBox(mouseX, mouseY, boxX + 348, boxY + 64, 108, 18));
        context.drawTextWithShadow(this.textRenderer, "Scale: " + String.format(Locale.ROOT, "%.2f", selected.fontScale), boxX + 232, boxY + 76, 0xFFEAEAEA);

        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            drawModalButton(context, boxX + 232, boxY + 90, 124, 18, "Kind: " + selected.iconKind, containsBox(mouseX, mouseY, boxX + 232, boxY + 90, 124, 18));
            drawModalButton(context, boxX + 360, boxY + 90, 96, 18, "Pick Id", containsBox(mouseX, mouseY, boxX + 360, boxY + 90, 96, 18));
            if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
                drawModalButton(context, boxX + 232, boxY + 138, 54, 18, "Z-", containsBox(mouseX, mouseY, boxX + 232, boxY + 138, 54, 18));
                drawModalButton(context, boxX + 290, boxY + 138, 54, 18, "Z+", containsBox(mouseX, mouseY, boxX + 290, boxY + 138, 54, 18));
                drawModalButton(context, boxX + 348, boxY + 138, 54, 18, "Y-", containsBox(mouseX, mouseY, boxX + 348, boxY + 138, 54, 18));
                drawModalButton(context, boxX + 406, boxY + 138, 50, 18, "Y+", containsBox(mouseX, mouseY, boxX + 406, boxY + 138, 50, 18));
                drawModalButton(context, boxX + 232, boxY + 162, 54, 18, "P-", containsBox(mouseX, mouseY, boxX + 232, boxY + 162, 54, 18));
                drawModalButton(context, boxX + 290, boxY + 162, 54, 18, "P+", containsBox(mouseX, mouseY, boxX + 290, boxY + 162, 54, 18));
                drawModalButton(context, boxX + 348, boxY + 162, 54, 18, "OX-", containsBox(mouseX, mouseY, boxX + 348, boxY + 162, 54, 18));
                drawModalButton(context, boxX + 406, boxY + 162, 50, 18, "OX+", containsBox(mouseX, mouseY, boxX + 406, boxY + 162, 50, 18));
                drawModalButton(context, boxX + 232, boxY + 186, 54, 18, "OY-", containsBox(mouseX, mouseY, boxX + 232, boxY + 186, 54, 18));
                drawModalButton(context, boxX + 290, boxY + 186, 54, 18, "OY+", containsBox(mouseX, mouseY, boxX + 290, boxY + 186, 54, 18));
                drawModalButton(context, boxX + 348, boxY + 186, 52, 18,
                        "Fit: " + (selected.modelAutoFit ? "ON" : "OFF"),
                        containsBox(mouseX, mouseY, boxX + 348, boxY + 186, 52, 18));
                drawModalButton(context, boxX + 404, boxY + 186, 52, 18,
                        "Look: " + (selected.modelFollowLook ? "ON" : "OFF"),
                        containsBox(mouseX, mouseY, boxX + 404, boxY + 186, 52, 18));
                context.drawTextWithShadow(this.textRenderer, "Id: " + selected.iconId, boxX + 232, boxY + 114, 0xFFEAEAEA);
                context.drawTextWithShadow(this.textRenderer,
                        String.format(Locale.ROOT, "Zoom %.2f  Yaw %.0f  Pitch %.0f", selected.modelZoom, selected.modelYaw, selected.modelPitch),
                        boxX + 232, boxY + 126, 0xFFEAEAEA);
                context.drawTextWithShadow(this.textRenderer,
                        "Offset X: " + selected.modelOffsetX + "  Y: " + selected.modelOffsetY,
                        boxX + 232, boxY + 210, 0xFFEAEAEA);
            } else {
                drawModalButton(context, boxX + 232, boxY + 114, 72, 18, "Count: " + (selected.iconShowCount ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 72, 18));
                drawModalButton(context, boxX + 308, boxY + 114, 72, 18, "Dur: " + (selected.iconShowDurability ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 308, boxY + 114, 72, 18));
                drawModalButton(context, boxX + 384, boxY + 114, 72, 18, "CD: " + (selected.iconShowCooldown ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 384, boxY + 114, 72, 18));
                context.drawTextWithShadow(this.textRenderer, "Id: " + selected.iconId, boxX + 232, boxY + 136, 0xFFEAEAEA);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            drawModalButton(context, boxX + 232, boxY + 90, 224, 18, "Max Src: " + (safe(selected.sourceTokenMax).isBlank() ? "(none)" : selected.sourceTokenMax), containsBox(mouseX, mouseY, boxX + 232, boxY + 90, 224, 18));
            drawModalButton(context, boxX + 232, boxY + 114, 90, 18, "Segmented: " + (selected.segmented ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 90, 18));
            drawModalButton(context, boxX + 326, boxY + 114, 62, 18, "R-", containsBox(mouseX, mouseY, boxX + 326, boxY + 114, 62, 18));
            drawModalButton(context, boxX + 392, boxY + 114, 64, 18, "R+", containsBox(mouseX, mouseY, boxX + 392, boxY + 114, 64, 18));
            drawModalButton(context, boxX + 232, boxY + 138, 54, 18, "MIN-", containsBox(mouseX, mouseY, boxX + 232, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 138, 54, 18, "MIN+", containsBox(mouseX, mouseY, boxX + 290, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 138, 54, 18, "MAX-", containsBox(mouseX, mouseY, boxX + 348, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 138, 50, 18, "MAX+", containsBox(mouseX, mouseY, boxX + 406, boxY + 138, 50, 18));
            drawModalButton(context, boxX + 232, boxY + 162, 54, 18, "C1-", containsBox(mouseX, mouseY, boxX + 232, boxY + 162, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 162, 54, 18, "C1+", containsBox(mouseX, mouseY, boxX + 290, boxY + 162, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 162, 54, 18, "C2-", containsBox(mouseX, mouseY, boxX + 348, boxY + 162, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 162, 50, 18, "C2+", containsBox(mouseX, mouseY, boxX + 406, boxY + 162, 50, 18));
            context.drawTextWithShadow(this.textRenderer, "Range: " + String.format(Locale.ROOT, "%.1f..%.1f", selected.minValue, selected.maxValue) + "  Segments: " + selected.segments, boxX + 232, boxY + 176, 0xFFEAEAEA);

            int rangeX = boxX + 232;
            int rangeY = boxY + 206;
            int rangeW = 124;
            int segX = boxX + 360;
            int segY = boxY + 206;
            int segW = 96;
            context.drawTextWithShadow(this.textRenderer, "Range (min,max)", rangeX, rangeY - 10, 0xFFB8B8B8);
            context.fill(rangeX, rangeY, rangeX + rangeW, rangeY + 18, advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(rangeX, rangeY, rangeX + rangeW, rangeY + 1, 0x60FFFFFF);
            context.drawTextWithShadow(this.textRenderer, advancedBgColor, rangeX + 4, rangeY + 5, 0xFFEAEAEA);

            context.drawTextWithShadow(this.textRenderer, "Segments", segX, segY - 10, 0xFFB8B8B8);
            context.fill(segX, segY, segX + segW, segY + 18, advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(segX, segY, segX + segW, segY + 1, 0x60FFFFFF);
            context.drawTextWithShadow(this.textRenderer, advancedBorderColor, segX + 4, segY + 5, 0xFFEAEAEA);

            if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = rangeX + 4 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
                context.fill(cx, rangeY + 4, cx + 1, rangeY + 13, 0xFFFFFFFF);
            }
            if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = segX + 4 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
                context.fill(cx, segY + 4, cx + 1, segY + 13, 0xFFFFFFFF);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            drawModalButton(context, boxX + 232, boxY + 90, 54, 18, "WRN-", containsBox(mouseX, mouseY, boxX + 232, boxY + 90, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 90, 54, 18, "WRN+", containsBox(mouseX, mouseY, boxX + 290, boxY + 90, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 90, 54, 18, "CRT-", containsBox(mouseX, mouseY, boxX + 348, boxY + 90, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 90, 50, 18, "CRT+", containsBox(mouseX, mouseY, boxX + 406, boxY + 90, 50, 18));
            drawModalButton(context, boxX + 232, boxY + 114, 54, 18, "W-", containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 114, 54, 18, "W+", containsBox(mouseX, mouseY, boxX + 290, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 114, 54, 18, "C-", containsBox(mouseX, mouseY, boxX + 348, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 114, 50, 18, "C+", containsBox(mouseX, mouseY, boxX + 406, boxY + 114, 50, 18));
            drawModalButton(context, boxX + 232, boxY + 138, 108, 18, "Prefix preset", containsBox(mouseX, mouseY, boxX + 232, boxY + 138, 108, 18));
            drawModalButton(context, boxX + 344, boxY + 138, 112, 18, "Suffix preset", containsBox(mouseX, mouseY, boxX + 344, boxY + 138, 112, 18));
            context.drawTextWithShadow(this.textRenderer, "Warn/Crit: " + String.format(Locale.ROOT, "%.1f / %.1f", selected.warnThreshold, selected.critThreshold), boxX + 232, boxY + 162, 0xFFEAEAEA);
            context.drawTextWithShadow(this.textRenderer, "Prefix", boxX + 232, boxY + 186, 0xFFB8B8B8);
            context.drawTextWithShadow(this.textRenderer, "Suffix", boxX + 344, boxY + 186, 0xFFB8B8B8);
            context.fill(boxX + 232, boxY + 196, boxX + 340, boxY + 214, advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(boxX + 232, boxY + 196, boxX + 340, boxY + 197, 0x60FFFFFF);
            context.fill(boxX + 344, boxY + 196, boxX + 456, boxY + 214, advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
            context.fill(boxX + 344, boxY + 196, boxX + 456, boxY + 197, 0x60FFFFFF);
            drawSingleLineSelection(context, boxX + 236, boxY + 201, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
            drawSingleLineSelection(context, boxX + 348, boxY + 201, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
            context.drawTextWithShadow(this.textRenderer, advancedBgColor, boxX + 236, boxY + 201, 0xFFEAEAEA);
            context.drawTextWithShadow(this.textRenderer, advancedBorderColor, boxX + 348, boxY + 201, 0xFFEAEAEA);
            if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = boxX + 236 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
                context.fill(cx, boxY + 200, cx + 1, boxY + 209, 0xFFFFFFFF);
            }
            if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = boxX + 348 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
                context.fill(cx, boxY + 200, cx + 1, boxY + 209, 0xFFFFFFFF);
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            drawModalButton(context, boxX + 232, boxY + 90, 224, 18, "List source preset", containsBox(mouseX, mouseY, boxX + 232, boxY + 90, 224, 18));
            drawModalButton(context, boxX + 232, boxY + 114, 54, 18, "L-", containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 114, 54, 18, "L+", containsBox(mouseX, mouseY, boxX + 290, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 114, 54, 18, "S-", containsBox(mouseX, mouseY, boxX + 348, boxY + 114, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 114, 50, 18, "S+", containsBox(mouseX, mouseY, boxX + 406, boxY + 114, 50, 18));
            context.drawTextWithShadow(this.textRenderer, "Max lines: " + selected.maxLines + "  Scroll: " + selected.listScroll, boxX + 232, boxY + 138, 0xFFEAEAEA);
        } else if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            drawModalButton(context, boxX + 232, boxY + 90, 224, 18, "Type: " + selected.shapeType, containsBox(mouseX, mouseY, boxX + 232, boxY + 90, 224, 18));
            drawModalButton(context, boxX + 232, boxY + 114, 90, 18, "Filled: " + (selected.shapeFilled ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 90, 18));
            drawModalButton(context, boxX + 326, boxY + 114, 62, 18, "R-", containsBox(mouseX, mouseY, boxX + 326, boxY + 114, 62, 18));
            drawModalButton(context, boxX + 392, boxY + 114, 64, 18, "R+", containsBox(mouseX, mouseY, boxX + 392, boxY + 114, 64, 18));
            drawModalButton(context, boxX + 326, boxY + 138, 62, 18, "T-", containsBox(mouseX, mouseY, boxX + 326, boxY + 138, 62, 18));
            drawModalButton(context, boxX + 392, boxY + 138, 64, 18, "T+", containsBox(mouseX, mouseY, boxX + 392, boxY + 138, 64, 18));
            context.drawTextWithShadow(this.textRenderer, "Radius: " + selected.shapeRadius + "  Thickness: " + selected.shapeThickness, boxX + 232, boxY + 162, 0xFFEAEAEA);
        } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            context.drawTextWithShadow(this.textRenderer, "Use Pick Src above for state source", boxX + 232, boxY + 96, 0xFFB8B8B8);
            drawModalButton(context, boxX + 232, boxY + 114, 108, 18, "ON text", containsBox(mouseX, mouseY, boxX + 232, boxY + 114, 108, 18));
            drawModalButton(context, boxX + 344, boxY + 114, 112, 18, "OFF text", containsBox(mouseX, mouseY, boxX + 344, boxY + 114, 112, 18));
            drawModalButton(context, boxX + 232, boxY + 138, 54, 18, "ON-", containsBox(mouseX, mouseY, boxX + 232, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 290, boxY + 138, 54, 18, "ON+", containsBox(mouseX, mouseY, boxX + 290, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 348, boxY + 138, 54, 18, "OFF-", containsBox(mouseX, mouseY, boxX + 348, boxY + 138, 54, 18));
            drawModalButton(context, boxX + 406, boxY + 138, 50, 18, "OFF+", containsBox(mouseX, mouseY, boxX + 406, boxY + 138, 50, 18));
            drawModalButton(context, boxX + 232, boxY + 162, 224, 18, "Show Value: " + (selected.stateShowValue ? "ON" : "OFF"), containsBox(mouseX, mouseY, boxX + 232, boxY + 162, 224, 18));
            context.drawTextWithShadow(this.textRenderer, "ON: " + selected.stateOnText + "  OFF: " + selected.stateOffText, boxX + 232, boxY + 184, 0xFFEAEAEA);
        }

        drawModalButton(context, boxX + 12, boxY + 218, 82, 18, "H: " + selected.horizontalAlign.name(), containsBox(mouseX, mouseY, boxX + 12, boxY + 218, 82, 18));
        drawModalButton(context, boxX + 98, boxY + 218, 82, 18, "V: " + selected.verticalAlign.name(), containsBox(mouseX, mouseY, boxX + 98, boxY + 218, 82, 18));
        drawModalButton(context, boxX + 184, boxY + 218, 110, 18, "Anchor: " + shortAnchor(selected.anchor), containsBox(mouseX, mouseY, boxX + 184, boxY + 218, 110, 18));

        drawModalButton(context, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18, "Apply",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18));
        drawModalButton(context, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18, "Cancel",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18));
    }

    private boolean onCustomWidgetAdvancedClick(Click click, int boxX, int boxY) {
        if (click.button() != 0 && click.button() != 1) {
            return true;
        }
        boolean forward = click.button() != 1;
        if (selected == null) {
            return true;
        }
        int labelX = boxX + 12;
        int labelY = boxY + 44;
        int labelW = 214;
        int labelH = 18;
        int sourceX = boxX + 12;
        int sourceY = boxY + 72;
        int sourceW = 198;
        int sourceH = 18;

        List<String> suggestions = advancedActionSuggestions();
        if (!suggestions.isEmpty()) {
            int dropX = sourceX;
            int dropY = sourceY + 22;
            int dropW = sourceW;
            int rowH = 10;
            int bottomLimit = boxY + 208;
            int maxVisible = Math.max(1, Math.min(suggestions.size(), Math.max(1, (bottomLimit - dropY) / rowH)));
            int maxScroll = Math.max(0, suggestions.size() - maxVisible);
            advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll, 0, maxScroll);
            for (int i = 0; i < maxVisible; i++) {
                int yy = dropY + i * rowH;
                if (containsBox(click.x(), click.y(), dropX, yy, dropW, rowH)) {
                    int idx = advancedActionSuggestionScroll + i;
                    advancedAction = suggestions.get(idx);
                    advancedActionCursor = advancedAction.length();
                    advancedActionSuggestionIndex = idx;
                    return true;
                }
            }
        }

        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18)) {
            applyAdvancedAndClose();
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18)) {
            closeAdvancedModal();
            return true;
        }

        if (containsBox(click.x(), click.y(), boxX + 232, boxY + 38, 54, 18)) {
            selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 290, boxY + 38, 54, 18)) {
            selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 348, boxY + 38, 54, 18)) {
            selected.borderColor = cycleStyleColor(selected.borderColor, false);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 406, boxY + 38, 50, 18)) {
            selected.borderColor = cycleStyleColor(selected.borderColor, true);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 232, boxY + 64, 54, 18)) {
            selected.fontScale = Math.clamp((float) (selected.fontScale - stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 290, boxY + 64, 54, 18)) {
            selected.fontScale = Math.clamp((float) (selected.fontScale + stepDouble(0.1)), 0.5f, 4.0f);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 348, boxY + 64, 108, 18)) {
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

        if (containsBox(click.x(), click.y(), boxX + 12, boxY + 218, 82, 18)) {
            selected.horizontalAlign = cycleHorizontalAlign(selected.horizontalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 98, boxY + 218, 82, 18)) {
            selected.verticalAlign = cycleVerticalAlign(selected.verticalAlign, forward);
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 184, boxY + 218, 110, 18)) {
            int oldScreenX = resolveElementX(selected);
            int oldScreenY = resolveElementY(selected);
            selected.anchor = cycleAnchor(selected.anchor, forward);
            setElementScreenPosition(selected, oldScreenX, oldScreenY);
            clampElementToCanvas(selected);
            return true;
        }

        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 90, 124, 18)) {
                selected.iconKind = cyclePreset(selected.iconKind, ICON_KIND_PRESETS, forward);
                if ("entity_model".equalsIgnoreCase(selected.iconKind)
                        && (safe(selected.iconId).isBlank() || "minecraft:stone".equalsIgnoreCase(safe(selected.iconId)))) {
                    selected.iconId = "minecraft:player";
                }
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 360, boxY + 90, 96, 18)) {
                String[] ids = iconIdSuggestionsForKind(selected.iconKind);
                selected.iconId = cyclePreset(selected.iconId, ids, forward);
                return true;
            }
            if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
                if (containsBox(click.x(), click.y(), boxX + 232, boxY + 138, 54, 18)) {
                    selected.modelZoom = Math.clamp((float) (selected.modelZoom - stepDouble(0.05)), 0.2f, 2.5f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 290, boxY + 138, 54, 18)) {
                    selected.modelZoom = Math.clamp((float) (selected.modelZoom + stepDouble(0.05)), 0.2f, 2.5f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 348, boxY + 138, 54, 18)) {
                    selected.modelYaw = Math.clamp((float) (selected.modelYaw - stepDouble(5.0)), -180.0f, 180.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 406, boxY + 138, 50, 18)) {
                    selected.modelYaw = Math.clamp((float) (selected.modelYaw + stepDouble(5.0)), -180.0f, 180.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 232, boxY + 162, 54, 18)) {
                    selected.modelPitch = Math.clamp((float) (selected.modelPitch - stepDouble(5.0)), -90.0f, 90.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 290, boxY + 162, 54, 18)) {
                    selected.modelPitch = Math.clamp((float) (selected.modelPitch + stepDouble(5.0)), -90.0f, 90.0f);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 348, boxY + 162, 54, 18)) {
                    selected.modelOffsetX = Math.clamp(selected.modelOffsetX - stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 406, boxY + 162, 50, 18)) {
                    selected.modelOffsetX = Math.clamp(selected.modelOffsetX + stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 232, boxY + 186, 54, 18)) {
                    selected.modelOffsetY = Math.clamp(selected.modelOffsetY - stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 290, boxY + 186, 54, 18)) {
                    selected.modelOffsetY = Math.clamp(selected.modelOffsetY + stepInt(1), -200, 200);
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 348, boxY + 186, 52, 18)) {
                    selected.modelAutoFit = !selected.modelAutoFit;
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 404, boxY + 186, 52, 18)) {
                    selected.modelFollowLook = !selected.modelFollowLook;
                    return true;
                }
            } else {
                if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 72, 18)) {
                    selected.iconShowCount = !selected.iconShowCount;
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 308, boxY + 114, 72, 18)) {
                    selected.iconShowDurability = !selected.iconShowDurability;
                    return true;
                }
                if (containsBox(click.x(), click.y(), boxX + 384, boxY + 114, 72, 18)) {
                    selected.iconShowCooldown = !selected.iconShowCooldown;
                    return true;
                }
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 90, 224, 18)) {
                selected.sourceTokenMax = cyclePreset(selected.sourceTokenMax, new String[]{"", "max_hp", "food", "players.count"}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 90, 18)) {
                selected.segmented = !selected.segmented;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 326, boxY + 114, 62, 18)) {
                selected.segments = Math.max(1, selected.segments - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 114, 64, 18)) {
                selected.segments = Math.min(120, selected.segments + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 138, 54, 18)) {
                selected.minValue -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 138, 54, 18)) {
                selected.minValue += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 138, 54, 18)) {
                selected.maxValue = Math.max(selected.minValue + 1.0, selected.maxValue - stepDouble(1.0));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 138, 50, 18)) {
                selected.maxValue = selected.maxValue + stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 162, 54, 18)) {
                selected.colorStart = cycleStyleColor(selected.colorStart, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 162, 54, 18)) {
                selected.colorStart = cycleStyleColor(selected.colorStart, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 162, 54, 18)) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 162, 50, 18)) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, true);
                return true;
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 90, 54, 18)) {
                selected.warnThreshold -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 90, 54, 18)) {
                selected.warnThreshold += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 90, 54, 18)) {
                selected.critThreshold -= stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 90, 50, 18)) {
                selected.critThreshold += stepDouble(1.0);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 54, 18)) {
                selected.colorWarn = cycleStyleColor(selected.colorWarn, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 114, 54, 18)) {
                selected.colorWarn = cycleStyleColor(selected.colorWarn, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 114, 54, 18)) {
                selected.colorCrit = cycleStyleColor(selected.colorCrit, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 114, 50, 18)) {
                selected.colorCrit = cycleStyleColor(selected.colorCrit, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 138, 108, 18)) {
                selected.prefix = cyclePreset(selected.prefix, new String[]{"", "HP: ", "Food: ", "FPS: "}, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 344, boxY + 138, 112, 18)) {
                selected.suffix = cyclePreset(selected.suffix, new String[]{"", "%", " hp", " ms"}, forward);
                return true;
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 90, 224, 18)) {
                advancedAction = cyclePreset(advancedAction, LIST_SOURCE_PRESETS, forward);
                advancedActionCursor = advancedAction.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 54, 18)) {
                selected.maxLines = Math.max(1, selected.maxLines - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 114, 54, 18)) {
                selected.maxLines = Math.min(200, selected.maxLines + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 114, 54, 18)) {
                selected.listScroll = Math.max(0, selected.listScroll - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 114, 50, 18)) {
                selected.listScroll = Math.min(500, selected.listScroll + stepInt(1));
                return true;
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 90, 224, 18)) {
                selected.shapeType = cyclePreset(selected.shapeType, SHAPE_TYPE_PRESETS, forward);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 90, 18)) {
                selected.shapeFilled = !selected.shapeFilled;
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 326, boxY + 114, 62, 18)) {
                selected.shapeRadius = Math.max(0, selected.shapeRadius - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 114, 64, 18)) {
                selected.shapeRadius = Math.min(64, selected.shapeRadius + stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 326, boxY + 138, 62, 18)) {
                selected.shapeThickness = Math.max(1, selected.shapeThickness - stepInt(1));
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 138, 64, 18)) {
                selected.shapeThickness = Math.min(24, selected.shapeThickness + stepInt(1));
                return true;
            }
        } else if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 114, 108, 18)) {
                selected.stateOnText = cyclePreset(selected.stateOnText, new String[]{"ON", "YES", "ENABLED", "ACTIVE"}, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 344, boxY + 114, 112, 18)) {
                selected.stateOffText = cyclePreset(selected.stateOffText, new String[]{"OFF", "NO", "DISABLED", "IDLE"}, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 138, 54, 18)) {
                selected.colorStart = cycleStyleColor(selected.colorStart, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 290, boxY + 138, 54, 18)) {
                selected.colorStart = cycleStyleColor(selected.colorStart, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 348, boxY + 138, 54, 18)) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, false);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 406, boxY + 138, 50, 18)) {
                selected.colorEnd = cycleStyleColor(selected.colorEnd, true);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 162, 224, 18)) {
                selected.stateShowValue = !selected.stateShowValue;
                return true;
            }
        }

        if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            int rangeX = boxX + 232;
            int rangeY = boxY + 206;
            int rangeW = 124;
            int segX = boxX + 360;
            int segY = boxY + 206;
            int segW = 96;
            if (containsBox(click.x(), click.y(), rangeX, rangeY, rangeW, 18)) {
                advancedBgColorFocused = true;
                advancedBorderColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBgCursor = cursorIndexFromPoint(advancedBgColor, (int) (click.x() - (rangeX + 4)), 0, 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
                return true;
            }
            if (containsBox(click.x(), click.y(), segX, segY, segW, 18)) {
                advancedBorderColorFocused = true;
                advancedBgColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, (int) (click.x() - (segX + 4)), 0, 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
                return true;
            }
        }

        if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            if (containsBox(click.x(), click.y(), boxX + 232, boxY + 196, 108, 18)) {
                advancedBgColorFocused = true;
                advancedBorderColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBgSelectionAnchor = -1;
                advancedBgCursor = cursorIndexFromPoint(advancedBgColor, (int) (click.x() - (boxX + 236)), 0, 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BG);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 344, boxY + 196, 112, 18)) {
                advancedBorderColorFocused = true;
                advancedBgColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                advancedBorderSelectionAnchor = -1;
                advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, (int) (click.x() - (boxX + 348)), 0, 9);
                beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_BORDER);
                return true;
            }
        }

        advancedTextFocused = containsBox(click.x(), click.y(), labelX, labelY, labelW, labelH);
        if (advancedTextFocused) {
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedSelectionAnchor = -1;
            advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (labelX + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_TEXT);
            return true;
        }

        advancedActionFocused = containsBox(click.x(), click.y(), sourceX, sourceY, sourceW, sourceH);
        if (advancedActionFocused) {
            advancedTextFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedActionSelectionAnchor = -1;
            advancedActionCursor = cursorIndexFromPoint(advancedAction, (int) (click.x() - (sourceX + 4)), 0, 9);
            beginModalSelectionDrag(ModalDragSelectionField.ADVANCED_ACTION);
            return true;
        }

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

    private double stepDouble(double base) {
        return base * (isShiftDown() ? 10.0 : 1.0);
    }

    private List<String> sourceTokenSuggestions(String prefix) {
        String p = safe(prefix).toLowerCase(Locale.ROOT);
        List<String> starts = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String token : SOURCE_TOKEN_SUGGESTIONS) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (p.isBlank() || lower.startsWith(p)) {
                starts.add(token);
            } else if (lower.contains(p)) {
                contains.add(token);
            }
        }
        starts.addAll(contains);
        return starts;
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
        return switch (current) {
            case LEFT -> forward ? MacroHudDataHandler.HorizontalAlign.CENTER : MacroHudDataHandler.HorizontalAlign.RIGHT;
            case CENTER -> forward ? MacroHudDataHandler.HorizontalAlign.RIGHT : MacroHudDataHandler.HorizontalAlign.LEFT;
            case RIGHT -> forward ? MacroHudDataHandler.HorizontalAlign.LEFT : MacroHudDataHandler.HorizontalAlign.CENTER;
        };
    }

    private static MacroHudDataHandler.VerticalAlign cycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return switch (current) {
            case TOP -> forward ? MacroHudDataHandler.VerticalAlign.CENTER : MacroHudDataHandler.VerticalAlign.BOTTOM;
            case CENTER -> forward ? MacroHudDataHandler.VerticalAlign.BOTTOM : MacroHudDataHandler.VerticalAlign.TOP;
            case BOTTOM -> forward ? MacroHudDataHandler.VerticalAlign.TOP : MacroHudDataHandler.VerticalAlign.CENTER;
        };
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
        if (presets == null || presets.length == 0) {
            return current == null ? "" : current;
        }
        String c = current == null ? "" : current;
        int idx = -1;
        for (int i = 0; i < presets.length; i++) {
            if (Objects.equals(c, presets[i])) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return presets[0];
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0) {
            next = presets.length - 1;
        }
        if (next >= presets.length) {
            next = 0;
        }
        return presets[next];
    }

    private void applyAdvancedAndClose() {
        if (selected != null) {
            boolean valueCustom = selected.type == MacroHudDataHandler.ElementType.VALUE;
            if (!valueCustom) {
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
                final ModConfig.ChatInterceptMode interceptMode = advancedSecondaryInterceptMode == null
                        ? ModConfig.ChatInterceptMode.COPY
                        : advancedSecondaryInterceptMode;
                final List<String> regexList = splitLinesRaw(advancedText).stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                final String outgoingRegex = safe(advancedAction);
                ModConfig.updateAndSave(() -> {
                    ModConfig.secondaryChatShowWhileGuiOpen = showWhileGui;
                    ModConfig.secondaryChatFadeEnabled = fadeEnabled;
                    ModConfig.resetTransparencyWhenHovered = hoverReset;
                    ModConfig.noTransparencyWhenChatOpen = noTransparencyChatOpen;
                    ModConfig.secondaryChatFadeDurationMs = fadeMs;
                    ModConfig.secondaryChatMinAlpha = minAlpha;
                    ModConfig.secondaryChatMaxLines = maxLines;
                    ModConfig.secondaryChatScale = scale;
                    ModConfig.secondaryChatLineHeight = lineHeight;
                    ModConfig.secondaryChatInterceptMode = interceptMode;
                    ModConfig.secondaryChatRegexList = new ArrayList<>(regexList);
                    ModConfig.secondaryChatOutgoingRegex = outgoingRegex;
                });
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
                }
            } else if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
                selected.label = advancedText;
                selected.buttonAction = advancedAction;
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
        this.advancedTextFocused = false;
        this.advancedActionFocused = false;
        this.advancedBgColorFocused = false;
        this.advancedBorderColorFocused = false;
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
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
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
                int start = selectionStart(advancedSelectionAnchor, advancedCursor);
                int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
                advancedText = advancedText.substring(0, start) + advancedText.substring(end);
                advancedCursor = start;
                advancedSelectionAnchor = -1;
                return true;
            }
            if (advancedCursor > 0) {
                advancedText = advancedText.substring(0, advancedCursor - 1) + advancedText.substring(advancedCursor);
                advancedCursor--;
            }
            advancedSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
                int start = selectionStart(advancedSelectionAnchor, advancedCursor);
                int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
                advancedText = advancedText.substring(0, start) + advancedText.substring(end);
                advancedCursor = start;
                advancedSelectionAnchor = -1;
                return true;
            }
            if (advancedCursor < advancedText.length()) {
                advancedText = advancedText.substring(0, advancedCursor) + advancedText.substring(advancedCursor + 1);
            }
            advancedSelectionAnchor = -1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            int old = advancedCursor;
            advancedCursor = Math.max(0, advancedCursor - 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            int old = advancedCursor;
            advancedCursor = Math.min(advancedText.length(), advancedCursor + 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            int old = advancedCursor;
            advancedCursor = lineStart(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            int old = advancedCursor;
            advancedCursor = lineEnd(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, -1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
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
                if (start < 0) {
                    start = dir < 0 ? suggestions.size() : -1;
                }
                int next = start + dir;
                if (next < 0) {
                    next = suggestions.size() - 1;
                }
                if (next >= suggestions.size()) {
                    next = 0;
                }
                advancedActionSuggestionIndex = next;
                advancedAction = suggestions.get(next);
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
                if (start < 0) {
                    start = isShiftDown() ? suggestions.size() : -1;
                }
                int next = start + dir;
                if (next < 0) {
                    next = suggestions.size() - 1;
                }
                if (next >= suggestions.size()) {
                    next = 0;
                }
                advancedActionSuggestionIndex = next;
                advancedAction = suggestions.get(next);
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

    private boolean handleAdvancedBgKey(int keyCode) {
        return handleAdvancedColorKey(keyCode, true);
    }

    private boolean handleAdvancedBorderKey(int keyCode) {
        return handleAdvancedColorKey(keyCode, false);
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
        if (s == null || s.isEmpty()) {
            return;
        }
        if (hasSelection(advancedSelectionAnchor, advancedCursor)) {
            int start = selectionStart(advancedSelectionAnchor, advancedCursor);
            int end = selectionEnd(advancedSelectionAnchor, advancedCursor);
            this.advancedText = advancedText.substring(0, start) + advancedText.substring(end);
            this.advancedCursor = start;
        }
        this.advancedText = advancedText.substring(0, advancedCursor) + s + advancedText.substring(advancedCursor);
        this.advancedCursor += s.length();
        this.advancedSelectionAnchor = -1;
        this.advancedActionSuggestionScroll = 0;
    }

    private void insertAtAdvancedActionCursor(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        if (hasSelection(advancedActionSelectionAnchor, advancedActionCursor)) {
            int start = selectionStart(advancedActionSelectionAnchor, advancedActionCursor);
            int end = selectionEnd(advancedActionSelectionAnchor, advancedActionCursor);
            this.advancedAction = advancedAction.substring(0, start) + advancedAction.substring(end);
            this.advancedActionCursor = start;
        }
        this.advancedAction = advancedAction.substring(0, advancedActionCursor) + s + advancedAction.substring(advancedActionCursor);
        this.advancedActionCursor += s.length();
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

    private void applyAdvancedColorFieldsToSelection() {
        if (selected == null) {
            return;
        }
        Integer parsedBg = parseArgb(advancedBgColor);
        if (parsedBg != null) {
            selected.backgroundColor = parsedBg;
        }
        Integer parsedBorder = parseArgb(advancedBorderColor);
        if (parsedBorder != null) {
            if (isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected)) {
                selected.textColor = parsedBorder;
            } else {
                selected.borderColor = parsedBorder;
            }
        }
        advancedBgColor = formatColor(selected.backgroundColor);
        advancedBorderColor = formatColor((isSecondaryChatProxy(selected) || isNbtInspectorProxy(selected)) ? selected.textColor : selected.borderColor);
        advancedBgCursor = Math.clamp(advancedBgCursor, 0, advancedBgColor.length());
        advancedBorderCursor = Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length());
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

        int panelX = this.width - 320;
        int x0 = 10;
        int y0 = TOP_BAR_H + 14;
        int kw = 28;
        int kh = 18;
        int gap = 4;

        addRow(x0, y0, kw, kh, gap,
                GLFW.GLFW_KEY_ESCAPE,
                GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4,
                GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8,
                GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10, GLFW.GLFW_KEY_F11, GLFW.GLFW_KEY_F12);

        addRow(x0, y0 + 28, kw, kh, gap,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
                GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0,
                GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_BACKSPACE);

        addRow(x0, y0 + 50, kw, kh, gap,
                GLFW.GLFW_KEY_TAB,
                GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T,
                GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
                GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET, GLFW.GLFW_KEY_BACKSLASH);

        addRow(x0, y0 + 72, kw, kh, gap,
                GLFW.GLFW_KEY_CAPS_LOCK,
                GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G,
                GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L,
                GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_ENTER);

        addRow(x0, y0 + 94, kw, kh, gap,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B,
                GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD,
                GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_RIGHT_SHIFT);

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

    private void addRow(int xStart, int y, int w, int h, int gap, int... keys) {
        int x = xStart;
        for (int key : keys) {
            int keyW = w;
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_CAPS_LOCK
                    || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_KP_0) {
                keyW = switch (key) {
                    case GLFW.GLFW_KEY_SPACE -> w * 4;
                    case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> w * 2;
                    case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_ENTER -> w + 18;
                    case GLFW.GLFW_KEY_KP_0 -> w * 2;
                    default -> w;
                };
            }
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
        return isSecondaryChatProxy(element) || isNbtInspectorProxy(element);
    }

    private boolean isSecondaryChatProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_SECONDARY_CHAT_ID.equals(element.id);
    }

    private boolean isNbtInspectorProxy(MacroHudDataHandler.HudElement element) {
        return element != null && EXTERNAL_NBT_INSPECTOR_ID.equals(element.id);
    }

    private HudCanvasDataHandler.HudCanvasElement defaultSecondaryChatCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = ModConfig.secondaryChatX;
        e.y = ModConfig.secondaryChatY;
        e.width = Math.max(50, ModConfig.secondaryChatWidth);
        e.height = Math.max(30, ModConfig.secondaryChatHeight);
        e.fontScale = (float) Math.max(0.1, ModConfig.secondaryChatScale);
        e.lineHeight = Math.max(1, ModConfig.secondaryChatLineHeight);
        e.padding = Math.max(0, ModConfig.secondaryChatPadding);
        e.backgroundColor = ModConfig.secondaryChatBackgroundColor;
        e.textColor = ModConfig.secondaryChatTextColor;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
    }

    private HudCanvasDataHandler.HudCanvasElement defaultNbtInspectorCanvas() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = Math.max(0, ModConfig.overlayInspectorMarginX);
        e.y = Math.max(0, ModConfig.overlayInspectorMarginY);
        e.width = 260;
        e.height = 120;
        e.fontScale = (float) Math.clamp(ModConfig.overlayInspectorTextScale, 0.25, 5.0);
        e.lineHeight = Math.max(6, ModConfig.overlayInspectorLineHeight);
        e.padding = Math.max(0, ModConfig.overlayInspectorPadding);
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
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
    }

    private HudCanvasDataHandler.HudCanvasElement getMutableExternalCanvasElement(MacroHudDataHandler.HudElement proxy) {
        boolean secondaryChat = isSecondaryChatProxy(proxy);
        return HudCanvasDataHandler.getMutableElement(
                secondaryChat ? HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT : HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR,
                secondaryChat ? this::defaultSecondaryChatCanvas : this::defaultNbtInspectorCanvas
        );
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
            boolean active = ModConfig.secondaryChatEnabled && ModConfig.secondaryChatShowOverlay;
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
        }
        if (isNbtInspectorProxy(element)) {
            boolean active = NBTInfoHudOverlayModule.INSTANCE.isEnabled();
            return active ? ExternalProxyRenderState.ACTIVE : ExternalProxyRenderState.MODULE_DISABLED;
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
    }

    private void updateModalDragSelection(int mouseX, int mouseY) {
        if (this.client == null || activeDragSelectionField == ModalDragSelectionField.NONE) {
            return;
        }
        long window = this.client.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            activeDragSelectionField = ModalDragSelectionField.NONE;
            return;
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

    private static boolean containsBox(double x, double y, int boxX, int boxY, int boxW, int boxH) {
        return GuiSystem.contains(x, y, boxX, boxY, boxW, boxH);
    }

    private static String firstLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String normalized = raw.replace("\\n", "\n");
        int idx = normalized.indexOf('\n');
        return idx < 0 ? normalized : normalized.substring(0, idx);
    }

    private static List<String> splitLinesRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of("");
        }
        String normalized = raw.replace("\\n", "\n");
        String[] parts = normalized.split("\\n", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            out.add(p == null ? "" : p);
        }
        return out;
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
            case CHAT_ONLY -> "CHAT";
        };
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
        if (e.height < 14) {
            e.height = 14;
        }
        if ((e.borderColor >>> 24) == 0) {
            e.borderColor = 0xFFFFFFFF;
        }
    }

    private static int cycleStyleColor(int current, boolean forward) {
        for (int i = 0; i < STYLE_COLOR_PALETTE.length; i++) {
            if (STYLE_COLOR_PALETTE[i] == current) {
                int next = forward ? i + 1 : i - 1;
                if (next < 0) {
                    next = STYLE_COLOR_PALETTE.length - 1;
                }
                if (next >= STYLE_COLOR_PALETTE.length) {
                    next = 0;
                }
                return STYLE_COLOR_PALETTE[next];
            }
        }
        return STYLE_COLOR_PALETTE[0];
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

