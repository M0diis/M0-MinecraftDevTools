package me.m0dii.modules.macros.gui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.GuiTextEditingUtils;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;
import me.m0dii.gui.theme.*;
import me.m0dii.mixin.InGameHudAccessor;
import me.m0dii.mixin.PlayerListHudAccessor;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandhistory.CommandHistoryManager;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import me.m0dii.modules.hungertweaks.HungerTweaksTextureHelper;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholderCatalog;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.ProxyAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.SecondaryAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.StandardAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchExternalProxies.ExternalProxyRenderState;
import me.m0dii.modules.macros.gui.MacroWorkbenchModels.*;
import me.m0dii.modules.macros.gui.MacroWorkbenchScriptHighlighter.ScriptTokenSpan;
import me.m0dii.modules.macros.gui.MacroWorkbenchVanillaHud.VanillaHudDescriptor;
import me.m0dii.modules.macros.gui.MacroWorkbenchVanillaHud.VanillaPlayerListMetrics;
import me.m0dii.modules.macros.gui.MacroWorkbenchVanillaHud.VanillaSidebarEntryMetrics;
import me.m0dii.modules.macros.gui.MacroWorkbenchVanillaHud.VanillaSidebarMetrics;
import me.m0dii.modules.macros.hud.HudElementUtils;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.macros.hud.MacroInventoryWidgetSupport;
import me.m0dii.modules.messagehistory.MessageHistoryManager;
import me.m0dii.modules.nbthud.NBTInfoHudModule;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.utils.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static me.m0dii.modules.macros.gui.MacroWorkbenchScriptHighlighter.tokenizeButtonScriptLine;

public class MacroWorkbenchScreen extends Screen implements ThemedScreen {

    public enum Tab {
        CANVAS("✎", "Canvas", "Canvas", "Arrange HUD elements on screen"),
        KEYBOARD("⌘", "Keyboard", "Keyboard", "Click a key to bind or edit its macro"),
        MACROS("⚡", "Macros", "Macros", "Command macros and their key bindings"),
        AUTOMATION("⚙", "Automation", "Automation", "Rules that react to game events"),
        SCRIPTS("▶", "Scripts", "Scripts", "Groovy, Kotlin and JavaScript scripts: preview, run, bind a key, edit"),
        PLACEHOLDERS("{}", "Placeholders", "Placeholders", "Every {token} you can use; click one to copy it"),
        ENTITY_RADAR("◎", "Radar", "Entity Radar", "Entities currently tracked nearby"),
        COMMAND_HISTORY("/", "Commands", "Command History", "Recently sent commands; click to reuse"),
        MESSAGE_HISTORY("✉", "Messages", "Message History", "Recently received chat messages"),
        CONFIGURATION("☰", "Settings", "Settings", "Modules, overlays and tweaks");

        final String icon;
        /** Short name shown in the sidebar. */
        final String navLabel;
        /** Page title shown in the header. */
        final String label;
        final String subtitle;

        Tab(String icon, String navLabel, String label, String subtitle) {
            this.icon = icon;
            this.navLabel = navLabel;
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    /** Height of the page header (title, subtitle and Save/Done) on non-canvas tabs. */
    static final int HEADER_H = 34;

    private static final int CANVAS_CONTENT_TOP = 0;
    private static final int MODAL_W = 620;
    private static final int MODAL_H = 420;
    private static final Identifier HUD_HOTBAR_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HUD_HOTBAR_SELECTION_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HUD_CROSSHAIR_TEXTURE = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Identifier HUD_ARMOR_EMPTY_TEXTURE = Identifier.withDefaultNamespace("hud/armor_empty");
    private static final Identifier HUD_ARMOR_HALF_TEXTURE = Identifier.withDefaultNamespace("hud/armor_half");
    private static final Identifier HUD_ARMOR_FULL_TEXTURE = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier HUD_EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier HUD_EXPERIENCE_BAR_PROGRESS_TEXTURE = Identifier.withDefaultNamespace("hud/experience_bar_progress");
    private static final Identifier HUD_EFFECT_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("hud/effect_background");
    private static final Identifier HUD_EFFECT_BACKGROUND_AMBIENT_TEXTURE = Identifier.withDefaultNamespace("hud/effect_background_ambient");
    private static final Identifier TOAST_SYSTEM_TEXTURE = Identifier.withDefaultNamespace("toast/system");
    private static final Identifier BOSS_BAR_PINK_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("boss_bar/pink_background");
    private static final Identifier BOSS_BAR_PINK_PROGRESS_TEXTURE = Identifier.withDefaultNamespace("boss_bar/pink_progress");
    private static boolean GRID_ENABLED_PREF = false;
    private static int GRID_ROWS_PREF = 12;
    private static int GRID_COLS_PREF = 16;
    private static boolean CENTER_LINES_ENABLED_PREF = false;
    private static final int[] STYLE_COLOR_PALETTE = {
            0xAA101010, 0xAA1F1F1F, 0xAA2D1F0E, 0xAA0E2D1F, 0xAA1F0E2D,
            0xCC2A2A2A, 0xCC4A2A2A, 0xCC2A4A2A, 0xCC2A2A4A, 0xCC808080,
            0xFFFFFFFF, 0xFFFFAA00, 0xFFFF5555, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF
    };

    private final Screen parent;
    private Tab tab;
    private CanvasMode canvasMode = CanvasMode.CUSTOM;
    private VanillaPreviewMode vanillaPreviewMode = VanillaPreviewMode.VIRTUAL;

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
    private final List<MacroHudDataHandler.HudElement> vanillaCanvasElements = new ArrayList<>();
    private final EnumMap<HudTweaksSettings.ElementType, HudTweaksSettings.ElementConfig> vanillaCanvasConfigs =
            new EnumMap<>(HudTweaksSettings.ElementType.class);
    private int clipboardWidth = -1;
    private int clipboardHeight = -1;
    private final MacroWorkbenchCustomWidgetAdvancedClickHandler.Ops customWidgetAdvancedClickOpsBridge =
            new MacroWorkbenchCustomWidgetAdvancedClickOpsBridge(this);

    private EditBox quickField;
    private EditBox macroField;
    private EditBox actionField;
    private Button backgroundToggle;
    private Button borderToggle;
    private Button editButton;
    private Button deleteButton;
    private Button gridToggleButton;
    private Button gridRowsMinusButton;
    private Button gridRowsPlusButton;
    private Button gridColsMinusButton;
    private Button gridColsPlusButton;
    private Button centerLinesToggleButton;
    private Button canvasModeToggleButton;
    private Button presetPrevButton;
    private Button presetNextButton;
    private Button presetNewButton;
    private Button presetRenameButton;
    private Button presetDeleteButton;
    private EditBox presetNameField;

    private final List<AbstractWidget> topBarWidgets = new ArrayList<>();
    private final List<AbstractWidget> navWidgets = new ArrayList<>();
    private Button headerSaveButton;
    private Button canvasAddButton;
    private Button headerDoneButton;
    private Button canvasMenuButton;
    private boolean canvasNavOpen = false;
    /** Left button state; MouseHandler#isLeftPressed only updates while no screen is open (26.x). */
    private boolean leftMouseDown = false;
    /** Stepper value labels ("16 cols") positioned by the canvas toolbar layout. */
    private final List<ToolbarLabel> toolbarLabels = new ArrayList<>();
    private final List<UiRect> canvasBarRects = new ArrayList<>();
    private final List<AbstractWidget> canvasWidgets = new ArrayList<>();
    private final List<AbstractWidget> keyboardWidgets = new ArrayList<>();
    private final List<AbstractWidget> macroManagerWidgets = new ArrayList<>();
    private final List<AbstractWidget> automationWidgets = new ArrayList<>();
    private final List<AbstractWidget> placeholderWidgets = new ArrayList<>();
    private final List<AbstractWidget> entityRadarWidgets = new ArrayList<>();
    private final List<AbstractWidget> cmdHistoryWidgets = new ArrayList<>();
    private final List<AbstractWidget> msgHistoryWidgets = new ArrayList<>();
    private final List<AbstractWidget> configWidgets = new ArrayList<>();
    private final List<AbstractWidget> scriptsWidgets = new ArrayList<>();
    private MacroWorkbenchScriptsTab scriptsTabController;
    private MacroWorkbenchMacrosTab macrosTabController;
    private MacroWorkbenchAutomationTab automationTabController;
    private MacroWorkbenchConfigurationTab configurationTabController;

    private final List<KeyCell> cells = new ArrayList<>();
    private final Map<Integer, List<String>> macroBindingNames = new HashMap<>();
    private final Map<Integer, List<String>> macroBindingIds = new HashMap<>();
    private final Map<Integer, List<String>> gameBindingNames = new HashMap<>();
    private int selectedKey = -1;
    private String selectedMacroId;
    private boolean showGameKeybinds = false;

    private EditBox kbNameField;
    private EditBox kbCommandsField;
    private EditBox kbDelayField;
    private Button keyboardGameToggleButton;
    private Button kbSaveButton;
    private Button kbDeleteMacroButton;
    private Button kbEditCommandsButton;
    private Button kbNewButton;
    private Button kbOpenManagerButton;

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
    private String placeholderSearchQuery = "";
    private List<String> filteredPlaceholderDocs = List.of();
    private EditBox placeholderSearchField;
    private Button placeholderSearchButton;
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
    private Consumer<List<String>> commandListApplyConsumer;

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

    public MacroWorkbenchScreen(Screen parent, Tab initialTab) {
        super(Component.literal("Macro Workbench"));
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
        syncVanillaCanvasElementsFromSettings();

        rebuildBindingMaps();
        rebuildKeyboardGrid();

        int headerButtonW = 56;
        int doneX = this.width - Theme.GAP_L - headerButtonW;
        int saveX = doneX - Theme.GAP_S - headerButtonW;
        int headerButtonY = (HEADER_H - Theme.CONTROL_H) / 2;
        Button saveButton = Button.builder(Component.literal("Save"), b -> saveAll())
                .bounds(saveX, headerButtonY, headerButtonW, Theme.CONTROL_H)
                .build();
        this.headerSaveButton = saveButton;
        Button doneButton = ThemedWidgets.primary(Button.builder(Component.literal("Done"), b -> {
            saveAll();
            onClose();
        }).bounds(doneX, headerButtonY, headerButtonW, Theme.CONTROL_H).build());
        this.headerDoneButton = doneButton;

        int navX = Theme.GAP_M;
        int navW = sidebarWidth() - Theme.GAP_M * 2;
        int navH = navItemHeight();
        int navY = SIDEBAR_NAV_TOP;
        for (Tab tabDef : Tab.values()) {
            SidebarNavButton navButton = new SidebarNavButton(navX, navY, navW, navH, tabDef.icon, tabDef.navLabel,
                    () -> this.tab == tabDef, () -> setTab(tabDef));
            this.navWidgets.add(navButton);
            addRenderableWidget(navButton);
            navY += navH + 1;
        }
        this.topBarWidgets.add(saveButton);
        this.topBarWidgets.add(doneButton);
        addRenderableWidget(saveButton);
        addRenderableWidget(doneButton);

        int canvasControlY = 30;
        int canvasControlH = 20;
        UiRect canvasControlRow = FormPanels.panel(8, canvasControlY, Math.max(320, this.width - 16), canvasControlH);
        List<UiRect> canvasControlSlots = FormPanels.row(canvasControlRow, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.flex(112, 1), // Canvas Mode
                UiFlexLayout.Item.flex(90, 1),  // Add Element
                UiFlexLayout.Item.flex(62, 1),  // Grid
                UiFlexLayout.Item.flex(24, 1),  // R-
                UiFlexLayout.Item.flex(24, 1),  // R+
                UiFlexLayout.Item.flex(24, 1),  // C-
                UiFlexLayout.Item.flex(24, 1),  // C+
                UiFlexLayout.Item.flex(78, 1),  // Center
                UiFlexLayout.Item.flex(20, 1),  // <
                UiFlexLayout.Item.flex(20, 1),  // >
                UiFlexLayout.Item.flex(104, 1), // Preset Name
                UiFlexLayout.Item.flex(38, 1),  // New
                UiFlexLayout.Item.flex(54, 1),  // Rename
                UiFlexLayout.Item.flex(52, 1)   // Delete
        );
        this.canvasMenuButton = Button.builder(Component.literal("\u2630"), b -> this.canvasNavOpen = !this.canvasNavOpen)
                .bounds(0, 0, Theme.CONTROL_H, Theme.CONTROL_H).build();
        this.canvasModeToggleButton = Button.builder(Component.literal("Custom HUD"), b -> toggleCanvasMode())
                .bounds(canvasControlSlots.get(0).x(), canvasControlSlots.get(0).y(), canvasControlSlots.get(0).width(), canvasControlSlots.get(0).height()).build();
        Button addElement = ThemedWidgets.primary(Button.builder(Component.literal("+ Add"), b -> {
            if (!isVanillaCanvasMode()) {
                addElementModalOpen = true;
            }
        }).bounds(canvasControlSlots.get(1).x(), canvasControlSlots.get(1).y(), canvasControlSlots.get(1).width(), canvasControlSlots.get(1).height()).build());
        this.canvasAddButton = addElement;

        this.deleteButton = Button.builder(Component.literal("Delete"), b -> {
            if (isVanillaCanvasMode()) {
                if (selected != null && isVanillaHudProxy(selected)) {
                    resetVanillaProxy(selected);
                } else {
                    resetAllVanillaProxies();
                }
                syncCanvasFields();
                return;
            }
            if (!isVanillaCanvasMode() && selected != null) {
                Set<String> ids = selectedElementIds.isEmpty() ? Set.of(selected.id) : new HashSet<>(selectedElementIds);
                this.working.elements.removeIf(e -> ids.contains(e.id));
                selectedElementIds.clear();
                this.selected = null;
                syncCanvasFields();
            }
        }).bounds(this.width - 160, this.height - 38, 76, 18).build();

        this.gridToggleButton = Button.builder(Component.literal("Grid: OFF"), b -> {
            gridEnabled = !gridEnabled;
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(2).x(), canvasControlSlots.get(2).y(), canvasControlSlots.get(2).width(), canvasControlSlots.get(2).height()).build();

        this.gridRowsMinusButton = Button.builder(Component.literal("R-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.max(2, gridRows - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(3).x(), canvasControlSlots.get(3).y(), canvasControlSlots.get(3).width(), canvasControlSlots.get(3).height()).build();
        this.gridRowsPlusButton = Button.builder(Component.literal("R+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridRows = Math.min(80, gridRows + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(4).x(), canvasControlSlots.get(4).y(), canvasControlSlots.get(4).width(), canvasControlSlots.get(4).height()).build();
        this.gridColsMinusButton = Button.builder(Component.literal("C-"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.max(2, gridCols - step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(5).x(), canvasControlSlots.get(5).y(), canvasControlSlots.get(5).width(), canvasControlSlots.get(5).height()).build();
        this.gridColsPlusButton = Button.builder(Component.literal("C+"), b -> {
            int step = isShiftDown() ? 5 : 1;
            gridCols = Math.min(80, gridCols + step);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(6).x(), canvasControlSlots.get(6).y(), canvasControlSlots.get(6).width(), canvasControlSlots.get(6).height()).build();

        this.centerLinesToggleButton = Button.builder(Component.literal("Center: OFF"), b -> {
            centerLinesEnabled = !centerLinesEnabled;
            persistGridPrefs();
            syncGridButtons();
        }).bounds(canvasControlSlots.get(7).x(), canvasControlSlots.get(7).y(), canvasControlSlots.get(7).width(), canvasControlSlots.get(7).height()).build();

        this.presetPrevButton = Button.builder(Component.literal("<"), b -> cyclePreset(false)).bounds(canvasControlSlots.get(8).x(), canvasControlSlots.get(8).y(), canvasControlSlots.get(8).width(), canvasControlSlots.get(8).height()).build();
        this.presetNextButton = Button.builder(Component.literal(">"), b -> cyclePreset(true)).bounds(canvasControlSlots.get(9).x(), canvasControlSlots.get(9).y(), canvasControlSlots.get(9).width(), canvasControlSlots.get(9).height()).build();
        this.presetNameField = new EditBox(this.font, canvasControlSlots.get(10).x(), canvasControlSlots.get(10).y(), canvasControlSlots.get(10).width(), canvasControlSlots.get(10).height(), Component.literal("Preset"));
        this.presetNameField.setMaxLength(40);
        this.presetNewButton = Button.builder(Component.literal("New"), b -> createPresetFromField()).bounds(canvasControlSlots.get(11).x(), canvasControlSlots.get(11).y(), canvasControlSlots.get(11).width(), canvasControlSlots.get(11).height()).build();
        this.presetRenameButton = Button.builder(Component.literal("Rename"), b -> renamePresetFromField()).bounds(canvasControlSlots.get(12).x(), canvasControlSlots.get(12).y(), canvasControlSlots.get(12).width(), canvasControlSlots.get(12).height()).build();
        this.presetDeleteButton = Button.builder(Component.literal("Delete"), b -> {
            MacroHudDataHandler.deletePreset(MacroHudDataHandler.getActivePresetId());
            this.working = MacroHudDataHandler.getConfigCopy();
            syncExternalCanvasElementsFromSources();
            syncCanvasFields();
            syncGridButtons();
            syncPresetControls();
        }).bounds(canvasControlSlots.get(13).x(), canvasControlSlots.get(13).y(), canvasControlSlots.get(13).width(), canvasControlSlots.get(13).height()).build();

        this.quickField = new EditBox(this.font, 8, this.height - 38, 280, 18, Component.literal("Quick Edit"));
        this.quickField.setHint(Component.literal("Text (first line)"));
        this.quickField.setMaxLength(300);
        this.macroField = new EditBox(this.font, 292, this.height - 38, 120, 18, Component.literal("Macro Id"));
        this.macroField.setHint(Component.literal("Macro id"));
        this.macroField.setMaxLength(120);
        this.actionField = new EditBox(this.font, 416, this.height - 38, 180, 18, Component.literal("Action"));
        this.actionField.setHint(Component.literal("Action / script"));
        this.actionField.setMaxLength(32767);

        this.backgroundToggle = Button.builder(Component.literal("BG: OFF"), b -> {
            if (isVanillaCanvasMode()) {
                toggleVanillaPreviewMode();
                return;
            }
            if (selected != null) {
                selected.drawBackground = !selected.drawBackground;
                ensureVisibleBackground(selected);
                syncStyleButtons();
            }
        }).bounds(this.width - 316, this.height - 38, 74, 18).build();

        this.borderToggle = Button.builder(Component.literal("Border: OFF"), b -> {
            if (isVanillaCanvasMode()) {
                if (selected != null && isVanillaHudProxy(selected)) {
                    toggleVanillaProxyDisplay(selected);
                    syncCanvasFields();
                }
                return;
            }
            if (selected != null) {
                cycleBorderSetting(selected, true);
                syncStyleButtons();
            }
        }).bounds(this.width - 238, this.height - 38, 78, 18).build();

        this.editButton = Button.builder(Component.literal("Edit"), b -> {
                    if (!isVanillaCanvasMode()) {
                        openAdvancedModal();
                    }
                })
                .bounds(this.width - 80, this.height - 38, 72, 18).build();

        this.canvasWidgets.add(canvasMenuButton);
        this.canvasWidgets.add(canvasModeToggleButton);
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
        addRenderableWidget(canvasMenuButton);
        addRenderableWidget(addElement);
        addRenderableWidget(canvasModeToggleButton);
        addRenderableWidget(deleteButton);
        addRenderableWidget(gridToggleButton);
        addRenderableWidget(gridRowsMinusButton);
        addRenderableWidget(gridRowsPlusButton);
        addRenderableWidget(gridColsMinusButton);
        addRenderableWidget(gridColsPlusButton);
        addRenderableWidget(centerLinesToggleButton);
        addRenderableWidget(presetPrevButton);
        addRenderableWidget(presetNextButton);
        addRenderableWidget(presetNameField);
        addRenderableWidget(presetNewButton);
        addRenderableWidget(presetRenameButton);
        addRenderableWidget(presetDeleteButton);
        addRenderableWidget(quickField);
        addRenderableWidget(macroField);
        addRenderableWidget(actionField);
        addRenderableWidget(backgroundToggle);
        addRenderableWidget(borderToggle);
        addRenderableWidget(editButton);

        KbLayout kb = keyboardLayout();
        Button gameToggle = Button.builder(Component.literal("Game binds: " + (showGameKeybinds ? "ON" : "OFF")), b -> {
            showGameKeybinds = !showGameKeybinds;
            b.setMessage(Component.literal("Game binds: " + (showGameKeybinds ? "ON" : "OFF")));
        }).bounds(kb.game().x(), kb.game().y(), kb.game().width(), kb.game().height()).build();

        this.kbNameField = new EditBox(this.font, kb.name().x(), kb.name().y(), kb.name().width(), kb.name().height(), Component.literal("Macro Name"));
        this.kbNameField.setHint(Component.literal("Name"));
        this.kbCommandsField = new EditBox(this.font, kb.commands().x(), kb.commands().y(), kb.commands().width(), kb.commands().height(), Component.literal("Commands (; separated)"));
        this.kbCommandsField.setHint(Component.literal("Commands, ; separated"));
        this.kbDelayField = new EditBox(this.font, kb.delay().x(), kb.delay().y(), kb.delay().width(), kb.delay().height(), Component.literal("Delay Ticks"));
        this.kbDelayField.setHint(Component.literal("Delay"));
        this.kbNameField.setMaxLength(80);
        this.kbCommandsField.setMaxLength(500);
        this.kbDelayField.setMaxLength(6);

        Button kbEditCommands = Button.builder(Component.literal("Edit\u2026"), b -> openKeyboardCommandsModal())
                .bounds(kb.edit().x(), kb.edit().y(), kb.edit().width(), kb.edit().height()).build();
        Button kbSave = ThemedWidgets.primary(Button.builder(Component.literal("Save"), b -> saveKeyboardMacro())
                .bounds(kb.save().x(), kb.save().y(), kb.save().width(), kb.save().height()).build());
        Button kbNew = Button.builder(Component.literal("+ New"), b -> createKeyboardMacro())
                .bounds(kb.add().x(), kb.add().y(), kb.add().width(), kb.add().height()).build();
        Button kbDelete = ThemedWidgets.danger(Button.builder(Component.literal("Delete"), b -> deleteKeyboardMacro())
                .bounds(kb.delete().x(), kb.delete().y(), kb.delete().width(), kb.delete().height()).build());

        this.keyboardWidgets.add(gameToggle);
        this.keyboardWidgets.add(kbNameField);
        this.keyboardWidgets.add(kbCommandsField);
        this.keyboardWidgets.add(kbDelayField);
        this.keyboardWidgets.add(kbSave);
        this.keyboardWidgets.add(kbDelete);
        this.keyboardWidgets.add(kbEditCommands);
        this.keyboardWidgets.add(kbNew);
        addRenderableWidget(gameToggle);
        addRenderableWidget(kbNameField);
        addRenderableWidget(kbCommandsField);
        addRenderableWidget(kbDelayField);
        addRenderableWidget(kbSave);
        addRenderableWidget(kbDelete);
        addRenderableWidget(kbEditCommands);
        addRenderableWidget(kbNew);

        initPlaceholderWidgets();

        Button clearCmdHistoryButton = Button.builder(
                        Component.literal("Clear History"),
                        b -> {
                            CommandHistoryManager.clearHistory();
                            cmdHistoryItems.clear();
                        })
                .bounds(contentRight() - 88, contentTop(), 88, Theme.CONTROL_H).build();
        Button clearMsgHistoryButton = Button.builder(
                        Component.literal("Clear History"),
                        b -> {
                            MessageHistoryManager.clearHistory();
                            msgHistoryItems.clear();
                        })
                .bounds(contentRight() - 88, contentTop(), 88, Theme.CONTROL_H).build();
        ThemedWidgets.danger(clearCmdHistoryButton);
        ThemedWidgets.danger(this.deleteButton);
        ThemedWidgets.danger(this.presetDeleteButton);
        ThemedWidgets.danger(clearMsgHistoryButton);
        this.cmdHistoryWidgets.add(clearCmdHistoryButton);
        this.msgHistoryWidgets.add(clearMsgHistoryButton);
        addRenderableWidget(clearCmdHistoryButton);
        addRenderableWidget(clearMsgHistoryButton);

        initMacrosWidgets();
        initAutomationWidgets();
        initScriptsWidgets();
        initConfigurationWidgets();

        syncCanvasFields();
        syncGridButtons();
        syncPresetControls();
        syncKeyboardFields();
        setTab(this.tab);
    }

    private static final int SIDEBAR_NAV_TOP = 30;

    /** Width of the left navigation sidebar (hidden on the full-screen canvas). */
    int sidebarWidth() {
        return Math.clamp(Math.round(this.width * 0.2f), 116, 140);
    }

    private int navItemHeight() {
        int available = this.height - SIDEBAR_NAV_TOP - Theme.GAP_M;
        return Math.clamp(available / Tab.values().length - 1, 14, 20);
    }

    /** Content area of non-canvas tabs, right of the sidebar and below the page header. */
    int contentLeft() {
        return sidebarWidth() + Theme.GAP_L;
    }

    int contentTop() {
        return HEADER_H + Theme.GAP_M;
    }

    int contentRight() {
        return this.width - Theme.GAP_L;
    }

    int contentBottom() {
        return this.height - Theme.GAP_M;
    }

    int contentWidth() {
        return contentRight() - contentLeft();
    }

    int contentHeight() {
        return contentBottom() - contentTop();
    }

    private void placeHeaderButtons() {
        int headerButtonW = 56;
        int doneX = this.width - Theme.GAP_L - headerButtonW;
        int saveX = doneX - Theme.GAP_S - headerButtonW;
        int y = (HEADER_H - Theme.CONTROL_H) / 2;
        this.headerSaveButton.setX(saveX);
        this.headerSaveButton.setY(y);
        this.headerSaveButton.setWidth(headerButtonW);
        this.headerDoneButton.setX(doneX);
        this.headerDoneButton.setY(y);
        this.headerDoneButton.setWidth(headerButtonW);
    }

    private void renderShell(GuiGraphicsExtractor context) {
        if (this.tab == Tab.CANVAS) {
            return;
        }
        placeHeaderButtons();
        int sidebarW = sidebarWidth();
        context.fill(0, 0, sidebarW, this.height, Theme.SIDEBAR);
        ThemeDraw.vDivider(context, sidebarW, 0, this.height);
        context.text(this.font, "M0", Theme.GAP_M + 8, 11, Theme.ACCENT, false);
        context.text(this.font, "DevTools", Theme.GAP_M + 8 + this.font.width("M0 "), 11, Theme.TEXT, false);

        int left = contentLeft();
        int titleRight = this.width - Theme.GAP_L - 56 * 2 - Theme.GAP_S - Theme.GAP_M;
        context.text(this.font, ThemeDraw.fit(this.font, this.tab.label, titleRight - left), left, 7, Theme.TEXT, false);
        context.text(this.font, ThemeDraw.fit(this.font, this.tab.subtitle, titleRight - left), left, 19, Theme.TEXT_MUTED, false);
        ThemeDraw.hDivider(context, sidebarW + 1, HEADER_H, this.width - sidebarW - 1);
    }

    private void setTab(Tab next) {
        if (this.tab == Tab.CANVAS && next != Tab.CANVAS) {
            persistVanillaCanvasElements();
        }
        if (this.tab == Tab.KEYBOARD && next != Tab.KEYBOARD) {
            saveKeyboardMacro();
        }
        if (this.tab == Tab.MACROS && next != Tab.MACROS && this.macrosTabController != null) {
            this.macrosTabController.commitAll();
        }
        if (this.tab == Tab.AUTOMATION && next != Tab.AUTOMATION && this.automationTabController != null) {
            this.automationTabController.commitAll();
        }

        this.tab = next;
        this.canvasNavOpen = false;
        boolean canvas = next == Tab.CANVAS;
        boolean keyboard = next == Tab.KEYBOARD;
        boolean macros = next == Tab.MACROS;
        boolean automation = next == Tab.AUTOMATION;
        boolean placeholders = next == Tab.PLACEHOLDERS;

        refreshTabWidgetVisibility();

        if (canvas) {
            if (isVanillaCanvasMode()) {
                syncVanillaCanvasElementsFromSettings();
            }
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
        } else if (macros) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            if (this.macrosTabController != null) {
                this.macrosTabController.reloadFromStore(this.selectedMacroId);
            }
        } else if (automation) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            if (this.automationTabController != null) {
                this.automationTabController.reloadFromEngine(null);
            }
        } else if (placeholders) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            syncPlaceholderSearchButton();
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
        } else if (next == Tab.SCRIPTS) {
            applyQuickEdit();
            closeAdvancedModal();
            closeKeyboardCommandsModal();
            dragging = false;
            if (this.scriptsTabController != null) {
                this.scriptsTabController.reload();
            }
        }
    }

    private void refreshTabWidgetVisibility() {
        boolean canvasTab = this.tab == Tab.CANVAS;
        boolean keyboardTab = this.tab == Tab.KEYBOARD;
        boolean macrosTab = this.tab == Tab.MACROS;
        boolean automationTab = this.tab == Tab.AUTOMATION;
        boolean placeholdersTab = this.tab == Tab.PLACEHOLDERS;
        boolean entityRadarTab = this.tab == Tab.ENTITY_RADAR;
        boolean cmdHistoryTab = this.tab == Tab.COMMAND_HISTORY;
        boolean msgHistoryTab = this.tab == Tab.MESSAGE_HISTORY;
        boolean configTab = this.tab == Tab.CONFIGURATION;
        // In canvas, F1 controls visibility; in other tabs controls are always visible.
        boolean showChrome = !canvasTab || canvasChromeVisible;

        setWidgetState(topBarWidgets, showChrome);
        setWidgetState(navWidgets, !canvasTab);
        setWidgetState(canvasWidgets, canvasTab && showChrome);
        setWidgetState(keyboardWidgets, keyboardTab);
        setWidgetState(macroManagerWidgets, macrosTab);
        setWidgetState(automationWidgets, automationTab);
        setWidgetState(placeholderWidgets, placeholdersTab);
        setWidgetState(entityRadarWidgets, entityRadarTab);
        setWidgetState(cmdHistoryWidgets, cmdHistoryTab);
        setWidgetState(msgHistoryWidgets, msgHistoryTab);
        setWidgetState(configWidgets, configTab);
        setWidgetState(scriptsWidgets, this.tab == Tab.SCRIPTS);
    }

    private static void setWidgetState(List<AbstractWidget> widgets, boolean visible) {
        for (AbstractWidget widget : widgets) {
            widget.visible = visible;
            widget.active = visible;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean automationOverlayOpen = this.tab == Tab.AUTOMATION
                && this.automationTabController != null
                && this.automationTabController.hasOverlayOpen();
        int effectiveMouseX = automationOverlayOpen ? -10_000 : mouseX;
        int effectiveMouseY = automationOverlayOpen ? -10_000 : mouseY;
        context.fill(0, 0, this.width, this.height, this.tab == Tab.CANVAS ? 0xB00B0D11 : Theme.SCRIM);
        renderShell(context);

        if (this.tab == Tab.CANVAS) {
            renderCanvasTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.KEYBOARD) {
            renderKeyboardTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.MACROS) {
            renderMacrosTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.AUTOMATION) {
            renderAutomationTab(context, effectiveMouseX, effectiveMouseY);
        } else if (this.tab == Tab.ENTITY_RADAR) {
            renderEntityRadarTab(context);
        } else if (this.tab == Tab.COMMAND_HISTORY) {
            renderCommandHistoryTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.MESSAGE_HISTORY) {
            renderMessageHistoryTab(context, mouseX, mouseY);
        } else if (this.tab == Tab.CONFIGURATION) {
            renderConfigurationTab(context);
        } else if (this.tab == Tab.SCRIPTS) {
            if (this.scriptsTabController != null) {
                this.scriptsTabController.extractRenderState(context, mouseX, mouseY);
            }
        } else {
            renderPlaceholdersTab(context, mouseX, mouseY);
        }

        // Draw normal widgets first.
        super.extractRenderState(context, effectiveMouseX, effectiveMouseY, delta);

        if (this.tab == Tab.AUTOMATION && this.automationTabController != null) {
            this.automationTabController.renderOverlay(context, mouseX, mouseY);
        }

        float modalScale = modalSpaceScale();
        int modalMouseX = Math.round(mouseX / modalScale);
        int modalMouseY = Math.round(mouseY / modalScale);
        if (modalScale < 1f) {
            context.pose().pushMatrix();
            context.pose().scale(modalScale, modalScale);
        }
        inModalSpace(() -> {
            renderModals(context, modalMouseX, modalMouseY);
            return null;
        });
        if (modalScale < 1f) {
            context.pose().popMatrix();
        }
        if (this.tab == Tab.CANVAS && this.canvasNavOpen && canvasChromeVisible) {
            // Drawn after the widgets so the menu sits above the toolbar.
            renderCanvasNavMenu(context, mouseX, mouseY);
        }
        if (this.tab == Tab.CANVAS && addElementModalOpen && !advancedOpen && !kbCommandsModalOpen) {
            renderAddElementModal(context, mouseX, mouseY);
        }
    }

    private void renderCanvasTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        applyQuickEdit();
        updateDragging(mouseX, mouseY);

        if (gridEnabled || gridOverlayTicks > 0) {
            drawGridOverlay(context);
            if (gridOverlayTicks > 0) {
                gridOverlayTicks--;
            }
        }
        if (centerLinesEnabled) {
            drawCenterLinesOverlay(context);
        }

        for (MacroHudDataHandler.HudElement element : HudElementUtils.sortedByLayer(activeCanvasElements())) {
            if (element.visible) {
                drawCanvasElement(context, element);
            }
        }
        drawSnapGuides(context);

        renderCanvasChrome(context, mouseX, mouseY);
    }

    private static final int CANVAS_MARGIN = 6;
    private static final int CANVAS_BAR_PAD = 4;

    private int preferredWidth(AbstractWidget widget) {
        String message = widget.getMessage().getString();
        if (message.matches(".*:\\s*(ON|OFF)$")) {
            return this.font.width(message.replaceAll(":\\s*(ON|OFF)$", "")) + 34;
        }
        return Math.max(Theme.CONTROL_H, this.font.width(message) + 14);
    }

    /**
     * Lays out the floating canvas toolbar (top) and inspector bar (bottom) for the current window size and widget
     * labels. Groups wrap onto extra toolbar rows when they do not fit.
     */
    private void layoutCanvasChrome() {
        this.toolbarLabels.clear();
        this.canvasBarRects.clear();
        if (!canvasChromeVisible) {
            return;
        }
        int gap = Theme.GAP_S;
        int h = Theme.CONTROL_H;
        int barX = CANVAS_MARGIN;
        int barW = this.width - CANVAS_MARGIN * 2;
        int innerX = barX + CANVAS_BAR_PAD;
        int innerW = barW - CANVAS_BAR_PAD * 2;

        List<List<ToolbarItem>> groups = new ArrayList<>();
        groups.add(List.of(ToolbarItem.of(canvasMenuButton, h), ToolbarItem.of(canvasModeToggleButton, preferredWidth(canvasModeToggleButton)),
                ToolbarItem.of(canvasAddButton, preferredWidth(canvasAddButton))));
        groups.add(List.of(ToolbarItem.of(gridToggleButton, preferredWidth(gridToggleButton)),
                ToolbarItem.of(gridColsMinusButton, 16), ToolbarItem.text(gridCols + " cols", this.font.width("88 cols") + 4), ToolbarItem.of(gridColsPlusButton, 16),
                ToolbarItem.of(gridRowsMinusButton, 16), ToolbarItem.text(gridRows + " rows", this.font.width("88 rows") + 4), ToolbarItem.of(gridRowsPlusButton, 16),
                ToolbarItem.of(centerLinesToggleButton, preferredWidth(centerLinesToggleButton))));
        groups.add(List.of(ToolbarItem.text("Preset", this.font.width("Preset") + 8), ToolbarItem.of(presetPrevButton, 16),
                ToolbarItem.of(presetNameField, 84), ToolbarItem.of(presetNextButton, 16),
                ToolbarItem.of(presetNewButton, preferredWidth(presetNewButton)), ToolbarItem.of(presetRenameButton, preferredWidth(presetRenameButton)),
                ToolbarItem.of(presetDeleteButton, preferredWidth(presetDeleteButton))));
        List<ToolbarItem> right = List.of(ToolbarItem.of(headerSaveButton, 48), ToolbarItem.of(headerDoneButton, 48));
        int rightW = groupWidth(right, gap);

        int rowY = CANVAS_MARGIN + CANVAS_BAR_PAD;
        int cursor = innerX;
        int rowLimit = innerX + innerW - rightW - Theme.GAP_L;
        int rows = 1;
        placeGroup(right, innerX + innerW - rightW, rowY, gap);
        for (List<ToolbarItem> group : groups) {
            int groupW = groupWidth(group, gap);
            int start = cursor == innerX ? cursor : cursor + Theme.GAP_L;
            if (start + groupW > rowLimit && cursor != innerX) {
                rows++;
                rowY += h + gap;
                start = innerX;
                rowLimit = innerX + innerW;
            }
            cursor = placeGroup(group, start, rowY, gap);
        }
        int toolbarH = rows * h + (rows - 1) * gap + CANVAS_BAR_PAD * 2;
        this.canvasBarRects.add(new UiRect(barX, CANVAS_MARGIN, barW, toolbarH));

        // Bottom inspector bar: flexible text fields followed by fixed-width buttons.
        List<EditBox> fields = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        if (quickField.visible) {
            fields.add(quickField);
            weights.add(3);
        }
        if (macroField.visible) {
            fields.add(macroField);
            weights.add(2);
        }
        if (actionField.visible) {
            fields.add(actionField);
            weights.add(3);
        }
        List<ToolbarItem> buttons = new ArrayList<>();
        for (Button button : List.of(backgroundToggle, borderToggle, deleteButton, editButton)) {
            if (button.visible) {
                buttons.add(ToolbarItem.of(button, preferredWidth(button)));
            }
        }
        int buttonsW = groupWidth(buttons, gap);
        int minFieldsW = fields.size() * 56 + Math.max(0, fields.size() - 1) * gap;
        boolean oneRow = minFieldsW + buttonsW + gap <= innerW;
        int bottomRows = oneRow || fields.isEmpty() || buttons.isEmpty() ? 1 : 2;
        int bottomH = bottomRows * h + (bottomRows - 1) * gap + CANVAS_BAR_PAD * 2;
        int bottomY = this.height - CANVAS_MARGIN - bottomH;
        int fieldsRowY = bottomY + CANVAS_BAR_PAD;
        int buttonsRowY = bottomRows == 2 ? fieldsRowY + h + gap : fieldsRowY;
        int fieldsW = oneRow ? innerW - buttonsW - (buttons.isEmpty() ? 0 : gap) : innerW;
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int fx = innerX;
        int available = fieldsW - Math.max(0, fields.size() - 1) * gap;
        for (int i = 0; i < fields.size(); i++) {
            int w = i == fields.size() - 1 ? innerX + fieldsW - fx : available * weights.get(i) / Math.max(1, totalWeight);
            resizeChromeWidget(fields.get(i), fx, fieldsRowY, Math.max(20, w));
            fx += w + gap;
        }
        placeGroup(buttons, innerX + innerW - buttonsW, buttonsRowY, gap);
        this.canvasBarRects.add(new UiRect(barX, bottomY, barW, bottomH));
    }

    private static void resizeChromeWidget(AbstractWidget widget, int x, int y, int width) {
        boolean resized = widget.getWidth() != width;
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
        // A text field first created narrower keeps a stale scroll offset (showing nothing); reset it.
        if (resized && widget instanceof EditBox field && !field.isFocused()) {
            field.moveCursorToStart(false);
        }
    }

    private static int groupWidth(List<ToolbarItem> group, int gap) {
        int width = 0;
        for (ToolbarItem item : group) {
            width += item.width();
        }
        return width + Math.max(0, group.size() - 1) * gap;
    }

    /** @return the x just past the group's last item. */
    private int placeGroup(List<ToolbarItem> group, int x, int y, int gap) {
        int cursor = x;
        for (ToolbarItem item : group) {
            if (item.widget() != null) {
                resizeChromeWidget(item.widget(), cursor, y, item.width());
            } else {
                this.toolbarLabels.add(new ToolbarLabel(item.label(), cursor, y, item.width()));
            }
            cursor += item.width() + gap;
        }
        return cursor - gap;
    }

    private void renderCanvasChrome(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        layoutCanvasChrome();
        if (!canvasChromeVisible) {
            return;
        }
        for (UiRect bar : this.canvasBarRects) {
            ThemeDraw.box(context, bar.x(), bar.y(), bar.width(), bar.height(), 0xF0171B22);
            ThemeDraw.outline(context, bar.x(), bar.y(), bar.width(), bar.height(), Theme.BORDER);
        }
        for (ToolbarLabel label : this.toolbarLabels) {
            ThemeDraw.textCentered(context, this.font, label.text(), label.x(), label.y(), label.width(), Theme.CONTROL_H, Theme.TEXT_MUTED);
        }

        if (this.canvasBarRects.size() >= 2) {
            UiRect bottom = this.canvasBarRects.get(1);
            String summary;
            if (selected != null) {
                summary = isVanillaHudProxy(selected)
                        ? vanillaSelectionSummary(selected)
                        : selected.type + "  \u00b7  " + selected.x + ", " + selected.y + "  \u00b7  " + selected.width + " \u00d7 " + selected.height;
            } else if (isVanillaCanvasMode()) {
                summary = "Drag a vanilla HUD element to move it; drag its corner to scale.  F1 hides the toolbar.";
            } else {
                summary = "Click an element to select it  \u00b7  Ctrl+click multi-select  \u00b7  Ctrl+C/V copy  \u00b7  Ctrl+Z undo  \u00b7  F1 hides the toolbar";
            }
            String shown = ThemeDraw.fit(this.font, summary, bottom.width() - Theme.GAP_M * 2);
            int textW = this.font.width(shown);
            int chipY = bottom.y() - 14;
            ThemeDraw.box(context, bottom.x(), chipY, textW + Theme.GAP_M * 2, 12, 0xE0171B22);
            context.text(this.font, shown, bottom.x() + Theme.GAP_M, chipY + 2, selected != null ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }

    }

    private static final int NAV_MENU_ROW_H = 16;
    private static final int NAV_MENU_W = 128;

    private int canvasNavMenuY() {
        return this.canvasMenuButton.getY() + this.canvasMenuButton.getHeight() + Theme.GAP_S;
    }

    private void renderCanvasNavMenu(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int x = this.canvasMenuButton.getX();
        int y = canvasNavMenuY();
        int h = Tab.values().length * NAV_MENU_ROW_H + Theme.GAP_S * 2;
        ThemeDraw.box(context, x, y, NAV_MENU_W, h, Theme.SIDEBAR);
        ThemeDraw.outline(context, x, y, NAV_MENU_W, h, Theme.BORDER_STRONG);
        int rowY = y + Theme.GAP_S;
        for (Tab tabDef : Tab.values()) {
            boolean current = tabDef == this.tab;
            boolean hovered = containsBox(mouseX, mouseY, x + 2, rowY, NAV_MENU_W - 4, NAV_MENU_ROW_H);
            if (current) {
                ThemeDraw.box(context, x + 2, rowY, NAV_MENU_W - 4, NAV_MENU_ROW_H, Theme.ACCENT_SOFT);
            } else if (hovered) {
                ThemeDraw.box(context, x + 2, rowY, NAV_MENU_W - 4, NAV_MENU_ROW_H, Theme.SURFACE_HOVER);
            }
            int textY = rowY + (NAV_MENU_ROW_H - 8) / 2;
            context.text(this.font, tabDef.icon, x + 8, textY, current ? Theme.ACCENT_TEXT : Theme.TEXT_DIM, false);
            context.text(this.font, tabDef.label, x + 22, textY, current || hovered ? Theme.TEXT : Theme.TEXT_MUTED, false);
            rowY += NAV_MENU_ROW_H;
        }
    }

    /** @return true when the click was consumed by the open navigation menu. */
    private boolean onCanvasNavMenuClick(double mouseX, double mouseY) {
        int x = this.canvasMenuButton.getX();
        int rowY = canvasNavMenuY() + Theme.GAP_S;
        this.canvasNavOpen = false;
        for (Tab tabDef : Tab.values()) {
            if (mouseX >= x && mouseX <= x + NAV_MENU_W && mouseY >= rowY && mouseY < rowY + NAV_MENU_ROW_H) {
                setTab(tabDef);
                return true;
            }
            rowY += NAV_MENU_ROW_H;
        }
        // Clicking the menu button itself just closes the menu.
        return containsBox(mouseX, mouseY, x, this.canvasMenuButton.getY(), this.canvasMenuButton.getWidth(), this.canvasMenuButton.getHeight());
    }

    private void renderPlaceholdersTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<String> docs = placeholderDocs();
        this.filteredPlaceholderDocs = docs;
        UiRect list = listRect();
        int visible = listVisibleRows();
        placeholderScroll = Math.clamp(placeholderScroll, 0, Math.max(0, docs.size() - visible));
        drawListCard(context, list, docs.size(), placeholderScroll);
        if (docs.isEmpty()) {
            drawListEmpty(context, list, "No placeholders match \"" + StringUtils.safe(this.placeholderSearchQuery).trim() + "\"");
            return;
        }
        for (int row = 0; row < visible && placeholderScroll + row < docs.size(); row++) {
            String line = docs.get(placeholderScroll + row);
            int y = listRowY(row);
            if (line.startsWith("[")) {
                context.text(this.font, ThemeDraw.fit(this.font, line.replace("[", "").replace("]", "").toUpperCase(Locale.ROOT), list.width() - 16),
                        list.x() + 8, y + (LIST_ROW_H - 8) / 2 + 1, Theme.ACCENT_TEXT, false);
                continue;
            }
            boolean hovered = listRowHovered(mouseX, mouseY, row);
            drawListRow(context, list, row, hovered, line, Theme.TEXT, hovered ? "Copy" : null);
        }
    }

    private void initPlaceholderWidgets() {
        this.placeholderSearchField = new EditBox(this.font, contentLeft(), contentTop(), contentWidth(), Theme.CONTROL_H, Component.literal("Search placeholders"));
        this.placeholderSearchField.setHint(Component.literal("Search placeholders\u2026"));
        this.placeholderSearchField.setMaxLength(96);
        this.placeholderSearchField.setValue(this.placeholderSearchQuery);
        this.placeholderSearchField.setResponder(value -> {
            this.placeholderSearchQuery = value == null ? "" : value;
            this.placeholderScroll = 0;
        });
        this.placeholderWidgets.add(this.placeholderSearchField);
        addRenderableWidget(this.placeholderSearchField);
    }

    private void applyPlaceholderSearch() {
        this.placeholderSearchQuery = this.placeholderSearchField == null ? "" : StringUtils.safe(this.placeholderSearchField.getValue());
        this.placeholderScroll = 0;
        syncPlaceholderSearchButton();
    }

    private void syncPlaceholderSearchButton() {
        if (this.placeholderSearchButton == null) {
            return;
        }
        String query = StringUtils.safe(this.placeholderSearchQuery).trim();
        this.placeholderSearchButton.setMessage(Component.literal("Search"));
    }

    private List<String> placeholderDocs() {
        String query = StringUtils.safe(this.placeholderSearchQuery).trim().toLowerCase(Locale.ROOT);
        List<String> docs = MacroPlaceholders.getPlaceholderDocs();
        if (query.isEmpty()) {
            return docs;
        }
        List<String> filtered = new ArrayList<>();
        for (String line : docs) {
            if (line != null && line.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    private void initMacrosWidgets() {
        this.macrosTabController = new MacroWorkbenchMacrosTab(this, this.macroManagerWidgets);
        this.macrosTabController.initWidgets();
    }

    private void initAutomationWidgets() {
        this.automationTabController = new MacroWorkbenchAutomationTab(this, this.automationWidgets);
        this.automationTabController.initWidgets();
    }

    private void initScriptsWidgets() {
        this.scriptsWidgets.clear();
        this.scriptsTabController = new MacroWorkbenchScriptsTab(this, this.scriptsWidgets);
        this.scriptsTabController.initWidgets();
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

    private void renderConfigurationTab(GuiGraphicsExtractor context) {
        if (this.configurationTabController != null) {
            this.configurationTabController.extractRenderState(context);
        }
    }

    private void renderMacrosTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (this.macrosTabController != null) {
            this.macrosTabController.extractRenderState(context, mouseX, mouseY);
        }
    }

    private void renderAutomationTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (this.automationTabController != null) {
            this.automationTabController.extractRenderState(context, mouseX, mouseY);
        }
    }

    private int getHistoryVisibleLines() {
        return listVisibleRows();
    }

    private static final int LIST_ROW_H = 14;

    /** List card shared by the placeholder, history and radar tabs (below a one-row toolbar). */
    private UiRect listRect() {
        int y = contentTop() + Theme.CONTROL_H + Theme.GAP_M;
        return new UiRect(contentLeft(), y, contentWidth(), Math.max(LIST_ROW_H + 8, contentBottom() - y));
    }

    private int listVisibleRows() {
        return Math.max(1, (listRect().height() - 8) / LIST_ROW_H);
    }

    private int listRowY(int row) {
        return listRect().y() + 4 + row * LIST_ROW_H;
    }

    /** @return the visible row under the mouse, or -1. */
    private int listRowAt(double mouseX, double mouseY) {
        UiRect list = listRect();
        if (mouseX < list.x() || mouseX > list.right() - 6 || mouseY < list.y() + 4) {
            return -1;
        }
        int row = (int) ((mouseY - list.y() - 4) / LIST_ROW_H);
        return row < listVisibleRows() ? row : -1;
    }

    private boolean listRowHovered(int mouseX, int mouseY, int row) {
        return listRowAt(mouseX, mouseY) == row;
    }

    private void drawListCard(GuiGraphicsExtractor context, UiRect list, int total, int scroll) {
        ThemeDraw.card(context, list.x(), list.y(), list.width(), list.height());
        int visible = listVisibleRows();
        if (total > visible) {
            int trackX = list.right() - 4;
            int trackY = list.y() + 4;
            int trackH = list.height() - 8;
            int thumbH = Math.max(10, trackH * visible / total);
            int thumbY = trackY + (trackH - thumbH) * scroll / Math.max(1, total - visible);
            context.fill(trackX, trackY, trackX + 2, trackY + trackH, Theme.DIVIDER);
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, Theme.BORDER_STRONG);
        }
    }

    private void drawListEmpty(GuiGraphicsExtractor context, UiRect list, String message) {
        ThemeDraw.textCentered(context, this.font, message, list.x(), list.y(), list.width(), Math.min(list.height(), 48), Theme.TEXT_DIM);
    }

    private void drawListRow(GuiGraphicsExtractor context, UiRect list, int row, boolean hovered, String text, int color, String hint) {
        int y = listRowY(row);
        int rowX = list.x() + 4;
        int rowW = list.width() - 12;
        if (hovered) {
            ThemeDraw.box(context, rowX, y, rowW, LIST_ROW_H - 1, Theme.SURFACE_HOVER);
        }
        int hintW = hint == null ? 0 : this.font.width(hint) + Theme.GAP_M;
        context.text(this.font, ThemeDraw.fit(this.font, text, rowW - 8 - hintW), rowX + 4, y + (LIST_ROW_H - 8) / 2, hovered ? Theme.TEXT : color, false);
        if (hint != null) {
            context.text(this.font, hint, rowX + rowW - 4 - this.font.width(hint), y + (LIST_ROW_H - 8) / 2, Theme.ACCENT_TEXT, false);
        }
    }

    private void drawListToolbarText(GuiGraphicsExtractor context, String text, int reservedRight) {
        context.text(this.font, ThemeDraw.fit(this.font, text, contentWidth() - reservedRight), contentLeft(),
                contentTop() + (Theme.CONTROL_H - 8) / 2, Theme.TEXT_MUTED, false);
    }

    private void renderCommandHistoryTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        renderHistoryList(context, mouseX, mouseY, cmdHistoryItems, cmdHistoryScroll, "command", "No commands sent yet.");
    }

    private void renderMessageHistoryTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        renderHistoryList(context, mouseX, mouseY, msgHistoryItems, msgHistoryScroll, "message", "No messages received yet.");
    }

    private void renderHistoryList(GuiGraphicsExtractor context, int mouseX, int mouseY, List<String> items, int scroll,
                                   String noun, String emptyText) {
        drawListToolbarText(context, items.size() + " " + noun + (items.size() == 1 ? "" : "s") + "  \u00b7  click a row to copy it", 96);
        UiRect list = listRect();
        drawListCard(context, list, items.size(), scroll);
        if (items.isEmpty()) {
            drawListEmpty(context, list, emptyText);
            return;
        }
        int visible = listVisibleRows();
        for (int row = 0; row < visible && scroll + row < items.size(); row++) {
            boolean hovered = listRowHovered(mouseX, mouseY, row);
            drawListRow(context, list, row, hovered, items.get(scroll + row), Theme.TEXT_MUTED, hovered ? "Copy" : null);
        }
    }

    private void renderEntityRadarTab(GuiGraphicsExtractor context) {
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
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
            if (entity instanceof Monster) {
                hostile++;
            } else if (entity instanceof AgeableMob) {
                passive++;
            } else {
                neutral++;
            }
        }

        int chipX = contentLeft();
        int chipY = contentTop() + (Theme.CONTROL_H - 8) / 2;
        chipX = drawLegendChip(context, chipX, chipY, Theme.TEXT_MUTED, entityRadarItems.size() + " total");
        chipX = drawLegendChip(context, chipX, chipY, RADAR_HOSTILE, hostile + " hostile");
        chipX = drawLegendChip(context, chipX, chipY, RADAR_PASSIVE, passive + " passive");
        drawLegendChip(context, chipX, chipY, RADAR_NEUTRAL, neutral + " other");

        UiRect list = listRect();
        drawListCard(context, list, entityRadarItems.size(), entityRadarScroll);
        if (entityRadarItems.isEmpty()) {
            drawListEmpty(context, list, "No entities nearby.");
            return;
        }
        int visible = listVisibleRows();
        for (int row = 0; row < visible && entityRadarScroll + row < entityRadarItems.size(); row++) {
            Entity entity = entityRadarItems.get(entityRadarScroll + row);
            int color = entity instanceof Monster ? RADAR_HOSTILE : entity instanceof AgeableMob ? RADAR_PASSIVE : RADAR_NEUTRAL;
            double distance = this.minecraft == null || this.minecraft.player == null ? 0.0 : entity.distanceTo(this.minecraft.player);
            int y = listRowY(row) + (LIST_ROW_H - 8) / 2;
            int rowX = list.x() + 8;
            ThemeDraw.box(context, rowX, y + 1, 6, 6, color);
            String dist = String.format(Locale.ROOT, "%.1f m", distance);
            int distX = list.right() - 12 - this.font.width(dist);
            context.text(this.font, ThemeDraw.fit(this.font, entity.getName().getString(), distX - rowX - 18), rowX + 12, y, Theme.TEXT, false);
            context.text(this.font, dist, distX, y, Theme.TEXT_MUTED, false);
        }
    }

    private static final int RADAR_HOSTILE = 0xFFE5534B;
    private static final int RADAR_PASSIVE = 0xFF3FB950;
    private static final int RADAR_NEUTRAL = 0xFF98A1B3;

    private boolean onCommandHistoryClick(double mouseX, double mouseY) {
        return copyListRow(mouseX, mouseY, cmdHistoryItems, cmdHistoryScroll);
    }

    private boolean onMessageHistoryClick(double mouseX, double mouseY) {
        return copyListRow(mouseX, mouseY, msgHistoryItems, msgHistoryScroll);
    }

    private boolean onPlaceholderClick(double mouseX, double mouseY) {
        List<String> docs = placeholderDocs();
        int row = listRowAt(mouseX, mouseY);
        if (row < 0 || placeholderScroll + row >= docs.size() || docs.get(placeholderScroll + row).startsWith("[")) {
            return false;
        }
        return copyListRow(mouseX, mouseY, docs, placeholderScroll);
    }

    private boolean copyListRow(double mouseX, double mouseY, List<String> items, int scroll) {
        int row = listRowAt(mouseX, mouseY);
        if (row < 0 || scroll + row >= items.size()) {
            return false;
        }
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(items.get(scroll + row));
        }
        return true;
    }

    private void renderKeyboardTab(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        KbLayout kb = keyboardLayout();

        // Legend
        int legendY = kb.kbY();
        int legendX = kb.kbX();
        legendX = drawLegendChip(context, legendX, legendY, KEY_MACRO_BORDER, "Macro");
        if (showGameKeybinds) {
            legendX = drawLegendChip(context, legendX, legendY, KEY_GAME_BORDER, "Game bind");
        }
        drawLegendChip(context, legendX, legendY, Theme.ACCENT, "Selected");

        for (KeyCell cell : cells) {
            boolean macroBound = macroBindingIds.containsKey(cell.keyCode());
            boolean gameBound = showGameKeybinds && gameBindingNames.containsKey(cell.keyCode());
            boolean selectedCell = cell.keyCode() == selectedKey;
            boolean hovered = containsBox(mouseX, mouseY, cell.x(), cell.y(), cell.w(), cell.h());

            int bg = hovered ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED;
            int border = Theme.BORDER;
            int text = Theme.TEXT_MUTED;
            if (gameBound) {
                bg = hovered ? 0xFF22325A : KEY_GAME_BG;
                border = KEY_GAME_BORDER;
                text = Theme.TEXT;
            }
            if (macroBound) {
                bg = hovered ? 0xFF234433 : KEY_MACRO_BG;
                border = KEY_MACRO_BORDER;
                text = Theme.TEXT;
            }
            if (selectedCell) {
                bg = Theme.ACCENT_SOFT;
                border = Theme.ACCENT;
                text = Theme.TEXT;
            }
            ThemeDraw.box(context, cell.x(), cell.y(), cell.w(), cell.h(), bg);
            ThemeDraw.outline(context, cell.x(), cell.y(), cell.w(), cell.h(), border);
            ThemeDraw.textCenteredScaled(context, this.font, cell.label(), cell.x(), cell.y(), cell.w(), cell.h(), text);
        }

        ThemeDraw.card(context, kb.cardX(), kb.cardY(), kb.cardW(), kb.cardH());
        int pad = Theme.GAP_M;
        int textX = kb.cardX() + pad;
        List<String> ids = macroBindingIds.getOrDefault(selectedKey, List.of());
        List<String> names = macroBindingNames.getOrDefault(selectedKey, List.of());
        String keyName = selectedKey < 0 ? "No key selected" : "Key " + MacroWorkbenchCanvasUtils.keyLabel(selectedKey);
        int titleW = kb.game().x() - Theme.GAP_S - textX;

        if (!kb.wide()) {
            String macroName = "";
            if (selectedMacroId != null) {
                int index = ids.indexOf(selectedMacroId);
                String name = index >= 0 && index < names.size() ? names.get(index) : selectedMacroId;
                macroName = "  \u00b7  " + name + (ids.size() > 1 ? "  (" + (index + 1) + "/" + ids.size() + ", click key for next)" : "");
            } else if (selectedKey >= 0) {
                macroName = ids.isEmpty() ? "  \u00b7  no macro, + New to add one" : "";
            }
            int titleY = kb.game().y() + (kb.game().height() - 8) / 2;
            context.text(this.font, ThemeDraw.fit(this.font, keyName + macroName, titleW), textX, titleY, Theme.TEXT, false);
            return;
        }

        int y = kb.cardY() + pad;
        context.text(this.font, ThemeDraw.fit(this.font, keyName, titleW), textX, y + 4, Theme.TEXT, false);
        y = kb.listTop();
        ThemeDraw.sectionLabel(context, this.font, "Macros on this key", textX, y - 11, kb.cardW() - pad * 2);
        int rowW = kb.cardW() - pad * 2;
        if (ids.isEmpty()) {
            context.text(this.font, ThemeDraw.fit(this.font, selectedKey < 0 ? "Click a key to see its bindings." : "None yet \u2014 + New binds one.", rowW), textX, y + 2, Theme.TEXT_DIM, false);
        } else {
            for (int i = 0; i < ids.size() && y + KB_ROW_H <= kb.listBottom(); i++) {
                String id = ids.get(i);
                String name = i < names.size() ? names.get(i) : id;
                boolean isSelected = id.equals(selectedMacroId);
                boolean hovered = containsBox(mouseX, mouseY, textX, y, rowW, KB_ROW_H - 1);
                if (isSelected) {
                    ThemeDraw.box(context, textX, y, rowW, KB_ROW_H - 1, Theme.ACCENT_SOFT);
                    context.fill(textX, y + 2, textX + 2, y + KB_ROW_H - 3, Theme.ACCENT);
                } else if (hovered) {
                    ThemeDraw.box(context, textX, y, rowW, KB_ROW_H - 1, Theme.SURFACE_HOVER);
                }
                context.text(this.font, ThemeDraw.fit(this.font, name, rowW - 12), textX + 6, y + (KB_ROW_H - 9) / 2, isSelected ? Theme.TEXT : Theme.TEXT_MUTED, false);
                y += KB_ROW_H;
            }
        }

        if (showGameKeybinds) {
            List<String> binds = gameBindingNames.getOrDefault(selectedKey, List.of());
            int gameY = Math.max(y + Theme.GAP_M, kb.listTop());
            if (!binds.isEmpty() && gameY + 20 <= kb.listBottom()) {
                ThemeDraw.sectionLabel(context, this.font, "Game binds", textX, gameY, rowW);
                gameY += 11;
                for (String bind : binds) {
                    if (gameY + 9 > kb.listBottom()) {
                        break;
                    }
                    context.text(this.font, ThemeDraw.fit(this.font, bind, rowW), textX, gameY, KEY_GAME_TEXT, false);
                    gameY += 10;
                }
            }
        }

        ThemeDraw.hDivider(context, kb.cardX() + 1, kb.listBottom() + Theme.GAP_S, kb.cardW() - 2);
        ThemeDraw.sectionLabel(context, this.font, "Name", textX, kb.name().y() - 10, rowW);
        ThemeDraw.sectionLabel(context, this.font, "Commands", textX, kb.commands().y() - 10, rowW);
        ThemeDraw.sectionLabel(context, this.font, "Delay (ticks)", textX, kb.delay().y() - 10, kb.delay().width());
    }

    private int drawLegendChip(GuiGraphicsExtractor context, int x, int y, int color, String label) {
        ThemeDraw.box(context, x, y + 1, 7, 7, color);
        context.text(this.font, label, x + 10, y, Theme.TEXT_MUTED, false);
        return x + 10 + this.font.width(label) + Theme.GAP_L;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.leftMouseDown = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.leftMouseDown = true;
        }
        float scale = modalSpaceScale();
        if (scale < 1f) {
            MouseButtonEvent scaled = new MouseButtonEvent(click.x() / scale, click.y() / scale, click.buttonInfo());
            return inModalSpace(() -> handleMouseClicked(scaled, doubled));
        }
        return handleMouseClicked(click, doubled);
    }

    private boolean handleMouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.activeDragSelectionField = ModalDragSelectionField.NONE;
            this.modalDragStartMouseX = Integer.MIN_VALUE;
            this.modalDragStartMouseY = Integer.MIN_VALUE;
            this.modalDragSelectionStarted = false;
        }
        if (this.tab == Tab.CANVAS && this.canvasNavOpen) {
            if (onCanvasNavMenuClick(click.x(), click.y())) {
                return true;
            }
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

        if (this.tab == Tab.AUTOMATION && this.automationTabController != null && this.automationTabController.hasOverlayOpen()) {
            return this.automationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }

        if (this.tab == Tab.CONFIGURATION
                && this.configurationTabController != null
                && this.configurationTabController.handleMouseClickBeforeWidgets(click.x(), click.y(), click.button())) {
            return true;
        }

        // Configuration tab has right-click decrement controls.
        if (this.tab == Tab.CONFIGURATION && this.configurationTabController != null && click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return this.configurationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }

        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (this.tab == Tab.CONFIGURATION && this.configurationTabController != null) {
            return this.configurationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }
        if (this.tab == Tab.MACROS && this.macrosTabController != null) {
            return this.macrosTabController.handleMouseClick(click.x(), click.y(), click.button());
        }
        if (this.tab == Tab.AUTOMATION && this.automationTabController != null) {
            return this.automationTabController.handleMouseClick(click.x(), click.y(), click.button());
        }
        if (this.tab == Tab.SCRIPTS && this.scriptsTabController != null) {
            return this.scriptsTabController.handleMouseClick(click.x(), click.y(), click.button());
        }

        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (this.tab == Tab.CANVAS) {
            for (UiRect bar : this.canvasBarRects) {
                if (containsBox(click.x(), click.y(), bar.x(), bar.y(), bar.width(), bar.height())) {
                    // Clicks on toolbar padding must not select or deselect HUD elements underneath.
                    return true;
                }
            }
            boolean hit = onCanvasClick(click.x(), click.y());
            if (hit && doubled && selected != null && !isVanillaCanvasMode()) {
                openAdvancedModal();
                return true;
            }
            return hit;
        }
        if (this.tab == Tab.KEYBOARD) {
            return onKeyboardClick(click.x(), click.y());
        }
        if (this.tab == Tab.PLACEHOLDERS) {
            return onPlaceholderClick(click.x(), click.y());
        }
        if (this.tab == Tab.MACROS) {
            return false;
        }
        if (this.tab == Tab.AUTOMATION) {
            return false;
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
        float scale = modalSpaceScale();
        if (scale < 1f) {
            return inModalSpace(() -> handleMouseScrolled(mouseX / scale, mouseY / scale, horizontalAmount, verticalAmount));
        }
        return handleMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean handleMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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

        if (this.tab == Tab.MACROS && this.macrosTabController != null) {
            if (this.macrosTabController.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }

        if (this.tab == Tab.AUTOMATION && this.automationTabController != null && this.automationTabController.hasOverlayOpen()) {
            if (this.automationTabController.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }

        if (this.tab == Tab.AUTOMATION && this.automationTabController != null) {
            if (this.automationTabController.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
                return true;
            }
        }

        if (this.tab == Tab.SCRIPTS && this.scriptsTabController != null
                && this.scriptsTabController.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
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
                    var window = this.minecraft == null ? null : this.minecraft.getWindow();
                    boolean shiftDown = window != null && (
                            InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                                    || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
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
    public boolean charTyped(CharacterEvent input) {
        return inModalSpace(() -> handleCharTyped(input));
    }

    private boolean handleCharTyped(CharacterEvent input) {
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
        if (this.tab == Tab.AUTOMATION
                && this.automationTabController != null
                && this.automationTabController.hasOverlayOpen()
                && this.automationTabController.handleCharTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        return inModalSpace(() -> handleKeyPressed(input));
    }

    private boolean handleKeyPressed(KeyEvent input) {
        int keyCode = input.input();

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && keyCode == InputConstants.KEY_F1) {
            canvasChromeVisible = !canvasChromeVisible;
            refreshCanvasChromeVisibility();
            return true;
        }

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && keyCode == InputConstants.KEY_DELETE
                && selected != null && !isVanillaCanvasMode()) {
            Set<String> ids = selectedElementIds.isEmpty() ? Set.of(selected.id) : new HashSet<>(selectedElementIds);
            this.working.elements.removeIf(e -> ids.contains(e.id));
            selectedElementIds.clear();
            this.selected = null;
            syncCanvasFields();
            return true;
        }

        if (this.tab == Tab.CANVAS && !advancedOpen && !kbCommandsModalOpen && !isAnyCanvasTextFieldFocused() && isCtrlDown()) {
            if (keyCode == InputConstants.KEY_Z) {
                undoLastMove();
                return true;
            }
            if (isVanillaCanvasMode()) {
                return false;
            }
            if (keyCode == InputConstants.KEY_C) {
                if (isShiftDown()) {
                    copySelectedDimensions();
                } else {
                    copySelectedElements();
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_V) {
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
            } else if (keyCode == InputConstants.KEY_ESCAPE) {
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
            } else if (keyCode == InputConstants.KEY_ESCAPE) {
                closeAdvancedModal();
            }
            return true;
        }
        if (this.tab == Tab.PLACEHOLDERS
                && this.placeholderSearchField != null
                && this.placeholderSearchField.isFocused()
                && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
            applyPlaceholderSearch();
            return true;
        }
        if (this.tab == Tab.AUTOMATION
                && this.automationTabController != null
                && this.automationTabController.hasOverlayOpen()
                && this.automationTabController.handleKeyPressed(input)) {
            return true;
        }
        if (this.tab == Tab.MACROS && this.macrosTabController != null && this.macrosTabController.handleKeyPressed(keyCode)) {
            return true;
        }
        if (this.tab == Tab.SCRIPTS && this.scriptsTabController != null && this.scriptsTabController.handleKeyPressed(keyCode)) {
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

        List<MacroHudDataHandler.HudElement> layeredElements = HudElementUtils.sortedByLayer(activeCanvasElements());
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
            if (x >= cell.x() && x <= cell.x() + cell.w() && y >= cell.y() && y <= cell.y() + cell.h()) {
                List<String> ids = macroBindingIds.getOrDefault(cell.keyCode(), List.of());
                if (cell.keyCode() == selectedKey && !ids.isEmpty()) {
                    // Clicking the selected key again cycles through its macros.
                    int next = (ids.indexOf(selectedMacroId) + 1) % ids.size();
                    selectedMacroId = ids.get(next);
                } else {
                    selectedKey = cell.keyCode();
                    selectedMacroId = ids.isEmpty() ? null : ids.get(0);
                }
                syncKeyboardFields();
                return true;
            }
        }

        KbLayout kb = keyboardLayout();
        if (!kb.wide()) {
            return false;
        }
        int rowX = kb.cardX() + Theme.GAP_M;
        int rowW = kb.cardW() - Theme.GAP_M * 2;
        int lineY = kb.listTop();
        for (String id : macroBindingIds.getOrDefault(selectedKey, List.of())) {
            if (lineY + KB_ROW_H > kb.listBottom()) {
                break;
            }
            if (x >= rowX && x <= rowX + rowW && y >= lineY && y < lineY + KB_ROW_H) {
                selectedMacroId = id;
                syncKeyboardFields();
                return true;
            }
            lineY += KB_ROW_H;
        }
        return false;
    }

    private boolean onAdvancedMouseClick(MouseButtonEvent click) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT && click.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return true;
        }

        boolean forward = click.button() != InputConstants.MOUSE_BUTTON_RIGHT;
        MacroWorkbenchAdvancedClickSupport.Ops clickOps = advancedClickSupportOps();
        int boxX = modalX();
        int boxY = modalY();

        if (colorPickerOpen && handleColorPickerClick(click)) {
            return true;
        }

        if (containsBox(click.x(), click.y(), boxX, boxY, modalWidth(), 20)) {
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

    private boolean onSecondaryAdvancedMouseClick(MouseButtonEvent click, boolean forward, int boxX, int boxY) {
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

    private boolean onProxyAdvancedMouseClick(MouseButtonEvent click, boolean forward, int boxX, int boxY) {
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
        if ((!dragging && !resizing) || selected == null || this.minecraft == null) {
            return;
        }

        long window = this.minecraft.getWindow().handle();
        if (!this.leftMouseDown) {
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
            if (isVanillaHudProxy(selected)) {
                snapGuideX = Integer.MIN_VALUE;
                snapGuideY = Integer.MIN_VALUE;
                applyVanillaProxyResize(selected, nextWidth, nextHeight);
                clampElementToCanvas(selected);
                return;
            }
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
            syncVanillaProxyConfigFromProxy(selected);
            refreshVanillaProxyPreview(selected);
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
            syncVanillaProxyConfigFromProxy(e);
            refreshVanillaProxyPreview(e);
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
                activeCanvasElements(),
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
                activeCanvasElements(),
                this::resolveElementX,
                this::resolveElementY,
                this.width,
                canvasBottomBound()
        );
    }

    private void drawSnapGuides(GuiGraphicsExtractor context) {
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

    private void drawGridOverlay(GuiGraphicsExtractor context) {
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

    private void drawCenterLinesOverlay(GuiGraphicsExtractor context) {
        int canvasTop = canvasTopBound();
        int canvasBottom = Math.max(canvasTop + 1, canvasBottomBound());
        int centerX = this.width / 2;
        int centerY = canvasTop + (canvasBottom - canvasTop) / 2;
        context.fill(centerX, canvasTop, centerX + 1, canvasBottom, 0x80FFCC66);
        context.fill(0, centerY, this.width, centerY + 1, 0x80FFCC66);
    }

    private void drawCanvasElement(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement element) {
        if (isVanillaHudProxy(element)) {
            refreshVanillaProxyPreview(element);
        }

        int x1 = resolveElementX(element);
        int y1 = resolveElementY(element);
        int x2 = x1 + element.width;
        int y2 = y1 + element.height;

        if (isVanillaHudProxy(element) && vanillaPreviewMode == VanillaPreviewMode.ACTUAL) {
            drawVanillaHudActualPreview(context, element, x1, y1, x2, y2);
            drawSelectedOutline(context, x1, y1, x2, y2, selectedElementIds.contains(element.id));
            return;
        }

        boolean customWidget = element.type == MacroHudDataHandler.ElementType.ICON
                || element.type == MacroHudDataHandler.ElementType.BAR
                || element.type == MacroHudDataHandler.ElementType.VALUE
                || element.type == MacroHudDataHandler.ElementType.LIST
                || element.type == MacroHudDataHandler.ElementType.INVENTORY
                || element.type == MacroHudDataHandler.ElementType.SHAPE
                || element.type == MacroHudDataHandler.ElementType.STATE_BADGE
                || element.type == MacroHudDataHandler.ElementType.REDSTONE_TIMELINE
                || element.type == MacroHudDataHandler.ElementType.STATUS;

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

    private void drawCanvasSpecialWidget(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        switch (e.type) {
            case ICON -> drawCanvasIconPreview(context, e, x1, y1, x2, y2);
            case BAR -> drawCanvasBarPreview(context, e, x1, y1, x2, y2);
            case VALUE -> drawCanvasValuePreview(context, e, x1, y1, x2, y2);
            case LIST -> drawCanvasListPreview(context, e, x1, y1, x2, y2);
            case INVENTORY -> drawCanvasInventoryPreview(context, e, x1, y1, x2, y2);
            case SHAPE -> drawCanvasShapePreview(context, e, x1, y1, x2, y2);
            case STATE_BADGE -> drawCanvasStateBadgePreview(context, e, x1, y1, x2, y2);
            case REDSTONE_TIMELINE -> drawCanvasRedstoneTimelinePreview(context, e, x1, y1, x2, y2);
            case STATUS -> drawCanvasStatusPreview(context, e, x1, y1, x2, y2);
            default -> {
                // no-op
            }
        }
    }

    private void drawCanvasIconPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        if ("entity_model".equalsIgnoreCase(StringUtils.safe(e.iconKind))) {
            MacroWorkbenchEntityModelRenderer.draw(context, this.minecraft, e, x1, y1, e.width, e.height);
        } else {
            ItemStack stack = resolvePreviewIconStack(e.iconKind, e.iconId);
            int ix = x1 + Math.max(0, (e.width - 16) / 2);
            int iy = y1 + Math.max(0, (e.height - 16) / 2);
            context.item(stack, ix, iy);
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

    private void drawCanvasBarPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
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
            float t = innerW == 1 ? progress : (px / (float) (innerW - 1));
            int c = blendColor(e.colorStart, e.colorEnd, t);
            context.fill(innerX + px, innerY, innerX + px + 1, innerY + innerH, c);
        }
    }

    private void drawCanvasValuePreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
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

    private void drawCanvasListPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        String src = MacroPlaceholders.expandForCanvas(this.minecraft, "{" + StringUtils.safe(e.sourceToken) + "}");
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

    private void drawCanvasInventoryPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        MacroInventoryWidgetSupport.extractRenderState(
                context,
                this.font,
                e,
                this.minecraft.player,
                x1,
                y1,
                e.width,
                e.height
        );
    }

    private void drawCanvasShapePreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
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
            case "circle" -> {
                int d = Math.min(w, h);
                me.m0dii.modules.macros.hud.MacroHudRuntime.drawRoundedShape(context, x1 + (w - d) / 2, y1 + (h - d) / 2, d, d, d / 2,
                        e.backgroundColor, e.borderColor, e.shapeFilled || e.drawBackground, e.drawBorder, Math.max(1, e.shapeThickness));
                return;
            }
            case "", "rounded_rect" -> {
                me.m0dii.modules.macros.hud.MacroHudRuntime.drawRoundedShape(context, x1, y1, w, h, Math.max(0, e.shapeRadius),
                        e.backgroundColor, e.borderColor, e.shapeFilled || e.drawBackground, e.drawBorder, Math.max(1, e.shapeThickness));
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

    private void drawCanvasStatusPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        me.m0dii.modules.macros.hud.StatusHudWidget.render(context, this.font, x1, y1, x2 - x1, y2 - y1, e, true);
    }

    private void drawCanvasRedstoneTimelinePreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
        if (e.drawBackground) {
            context.fill(x1, y1, x2, y2, canvasBackgroundColor(e));
        }
        if (e.drawBorder) {
            drawCanvasElementBorder(context, e, x1, y1, x2, y2);
        }
        me.m0dii.modules.redstonetimeline.RedstoneTimelineWidget.render(context, this.font, x1, y1, x2 - x1, y2 - y1,
                StringUtils.safe(e.label).isBlank() ? "Redstone" : e.label, Math.max(1, e.maxLines),
                e.timelineWindowTicks, e.timelineShowLabels, e.colorStart, e.textColor, true);
    }

    private void drawCanvasStateBadgePreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement e, int x1, int y1, int x2, int y2) {
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

    private void drawVanillaHudActualPreview(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement element, int x1, int y1, int x2, int y2) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(element);
        if (type == null) {
            return;
        }
        float scale = vanillaScaleForProxy(type, element);
        HudTweaksSettings.ElementConfig config = currentVanillaHudConfig(type, scale, element);
        int baseWidth = vanillaBaseWidth(type);
        int baseHeight = vanillaBaseHeight(type);
        VanillaHudDescriptor descriptor = vanillaHudDescriptor(type);
        float sx = Math.max(0.01f, element.width / (float) Math.max(1, baseWidth));
        float sy = Math.max(0.01f, element.height / (float) Math.max(1, baseHeight));

        context.pose().pushMatrix();
        context.pose().translate(x1, y1);
        context.pose().scale(sx, sy);
        switch (type) {
            case ACTION_BAR -> drawActualActionBarPreview(context, descriptor.baseWidth(), descriptor.baseHeight());
            case BOSS_BAR -> drawActualBossBarPreview(context);
            case CROSSHAIR -> drawActualCrosshairPreview(context);
            case DEBUG_SCREEN -> drawActualDebugScreenPreview(context);
            case HOTBAR_GROUP -> drawActualHotbarGroupPreview(context);
            case PLAYER_LIST -> drawActualPlayerListPreview(context, baseWidth, baseHeight);
            case SCOREBOARD -> drawActualScoreboardPreview(context, baseWidth, baseHeight);
            case SCREEN_TITLE -> drawActualScreenTitlePreview(context, descriptor.baseWidth());
            case STATUS_EFFECT -> drawActualStatusEffectPreview(context, descriptor.baseWidth());
            case SUBTITLES -> drawActualSubtitlesPreview(context, descriptor.baseWidth(), descriptor.baseHeight());
            case TOAST -> drawActualToastPreview(context);
            case TOOLTIP -> drawActualTooltipPreview(context, descriptor.baseWidth(), descriptor.baseHeight());
        }
        context.pose().popMatrix();

        drawVanillaPreviewOverlay(context, type, config, x1, y1, x2, y2);
    }

    private void drawActualActionBarPreview(GuiGraphicsExtractor context, int width, int height) {
        String text = "Sample Action Bar";
        int tw = this.font.width(text);
        int tx = Math.max(0, (width - tw) / 2);
        int ty = Math.max(0, (height - 9) / 2);
        context.textWithBackdrop(this.font, Component.literal(text), tx, ty, tw, 0xFFFFFFFF);
    }

    private void drawActualBossBarPreview(GuiGraphicsExtractor context) {
        String title = "Boss Name";
        int tw = this.font.width(title);
        context.text(this.font, title, Math.max(0, (182 - tw) / 2), 0, 0xFFFFFFFF);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, BOSS_BAR_PINK_BACKGROUND_TEXTURE, 182, 5, 0, 0, 0, 12, 182, 5);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, BOSS_BAR_PINK_PROGRESS_TEXTURE, 182, 5, 0, 0, 0, 12, 118, 5);
    }

    private void drawActualCrosshairPreview(GuiGraphicsExtractor context) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_CROSSHAIR_TEXTURE, 0, 0, 15, 15);
    }

    private void drawActualDebugScreenPreview(GuiGraphicsExtractor context) {
        String[] lines = {
                "Minecraft 1.21.11/Fabric",
                "XYZ: 123.45 / 64.00 / -78.90",
                "Block: 123 64 -79",
                "Facing: north",
                "Biome: minecraft:plains",
                "FPS: 144"
        };
        for (int i = 0; i < lines.length; i++) {
            context.text(this.font, lines[i], 2, 2 + i * 10, 0xFFE0E0E0);
        }
    }

    private void drawActualHotbarGroupPreview(GuiGraphicsExtractor context) {
        Player player = this.minecraft == null ? null : this.minecraft.player;
        int health = player == null ? 18 : Math.clamp(Math.round(player.getHealth()), 1, 20);
        int food = player == null ? 18 : Math.clamp(player.getFoodData().getFoodLevel(), 0, 20);
        int selectedSlot = player == null ? 0 : Math.clamp(player.getInventory().getSelectedSlot(), 0, 8);
        int experience = player == null ? 12 : Math.max(1, player.experienceLevel);

        for (int i = 0; i < 10; i++) {
            int heartX = i * 8;
            int foodX = 182 - 9 - i * 8;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getHeartTexture(false, HungerTweaksTextureHelper.HeartType.CONTAINER),
                    heartX, 0, 9, 9);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getFoodTexture(false, HungerTweaksTextureHelper.FoodType.EMPTY),
                    foodX, 0, 9, 9);

            int heartValue = i * 2 + 2;
            if (health >= heartValue) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getHeartTexture(false, HungerTweaksTextureHelper.HeartType.FULL),
                        heartX, 0, 9, 9);
            } else if (health == heartValue - 1) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getHeartTexture(false, HungerTweaksTextureHelper.HeartType.HALF),
                        heartX, 0, 9, 9);
            }

            int foodValue = i * 2 + 2;
            if (food >= foodValue) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getFoodTexture(false, HungerTweaksTextureHelper.FoodType.FULL),
                        foodX, 0, 9, 9);
            } else if (food == foodValue - 1) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.getFoodTexture(false, HungerTweaksTextureHelper.FoodType.HALF),
                        foodX, 0, 9, 9);
            }
        }

        for (int i = 0; i < 10; i++) {
            int armorValue = player == null ? 14 : Math.clamp(player.getArmorValue(), 0, 20);
            int armorX = i * 8;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_ARMOR_EMPTY_TEXTURE, armorX, 8, 9, 9);
            int armorTarget = i * 2 + 2;
            if (armorValue >= armorTarget) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_ARMOR_FULL_TEXTURE, armorX, 8, 9, 9);
            } else if (armorValue == armorTarget - 1) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_ARMOR_HALF_TEXTURE, armorX, 8, 9, 9);
            }
        }

        context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_EXPERIENCE_BAR_BACKGROUND_TEXTURE, 182, 5, 0, 0, 0, 12, 182, 5);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_EXPERIENCE_BAR_PROGRESS_TEXTURE, 182, 5, 0, 0, 0, 12, 124, 5);
        String level = Integer.toString(experience);
        context.text(this.font, level, Math.max(0, (182 - this.font.width(level)) / 2), 8, 0xFF80FF20);

        context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_HOTBAR_TEXTURE, 182, 22, 0, 0, 0, 17, 182, 22);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HUD_HOTBAR_SELECTION_TEXTURE, selectedSlot * 20 - 1, 16, 24, 24);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = previewHotbarStack(player, slot);
            if (!stack.isEmpty()) {
                context.item(stack, 3 + slot * 20, 20);
                context.itemDecorations(this.font, stack, 3 + slot * 20, 20);
            }
        }
    }

    private void drawActualPlayerListPreview(GuiGraphicsExtractor context, int width, int height) {
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.gui != null) {
            Scoreboard scoreboard = this.minecraft.level.getScoreboard();
            Objective objective = scoreboard == null ? null : scoreboard.getDisplayObjective(DisplaySlot.LIST);
            if (scoreboard != null) {
                VanillaPlayerListMetrics metrics = currentPlayerListMetrics();
                context.pose().pushMatrix();
                context.pose().translate(-metrics.left(), -metrics.top());
                this.minecraft.gui.hud.getTabList().extractRenderState(context, metrics.renderWidth(), scoreboard, objective);
                context.pose().popMatrix();
                return;
            }
        }
        int panelColor = 0xA0202020;
        context.fill(20, 0, width - 20, height, panelColor);
        String[] rows = {
                playerPreviewName(),
                "Alex",
                "Builder42",
                "Spectator",
                "NetherRunner"
        };
        int y = 8;
        for (int i = 0; i < rows.length; i++) {
            context.fill(24, y - 2, width - 24, y + 9, (i & 1) == 0 ? 0x402F2F2F : 0x30202020);
            context.text(this.font, rows[i], 30, y, 0xFFFFFFFF);
            context.text(this.font, "||||", width - 48, y, 0xFF7CFF7C);
            y += 12;
        }
    }

    private void drawActualScoreboardPreview(GuiGraphicsExtractor context, int width, int height) {
        if (this.minecraft != null && this.minecraft.gui != null) {
            Objective objective = effectiveSidebarObjective();
            if (objective != null) {
                VanillaSidebarMetrics metrics = currentSidebarMetrics();
                context.pose().pushMatrix();
                context.pose().translate(-metrics.left(), -metrics.top());
                ((InGameHudAccessor) this.minecraft.gui).m0dev$renderScoreboardSidebar(context, objective);
                context.pose().popMatrix();
                return;
            }
        }
        context.fill(0, 0, width, height, 0x70101010);
    }

    private void drawActualScreenTitlePreview(GuiGraphicsExtractor context, int width) {
        String title = "Title";
        String subtitle = "Subtitle text";
        int titleWidth = this.font.width(title);
        context.pose().pushMatrix();
        context.pose().translate(Math.max(0, (width - titleWidth * 2) / 2), 6);
        context.pose().scale(2.0f, 2.0f);
        context.text(this.font, title, 0, 0, 0xFFFFFFFF);
        context.pose().popMatrix();
        int subtitleWidth = this.font.width(subtitle);
        context.text(this.font, subtitle, Math.max(0, (width - subtitleWidth) / 2), 44, 0xFFE0E0E0);
    }

    private void drawActualStatusEffectPreview(GuiGraphicsExtractor context, int width) {
        ItemStack[] icons = {
                new ItemStack(Items.POTION),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(Items.FEATHER),
                new ItemStack(Items.TURTLE_HELMET)
        };
        for (int i = 0; i < icons.length; i++) {
            int row = i / 2;
            int col = i % 2;
            int x = width - 25 - col * 25;
            int y = 1 + row * 25;
            context.blitSprite(RenderPipelines.GUI_TEXTURED,
                    i == 0 ? HUD_EFFECT_BACKGROUND_AMBIENT_TEXTURE : HUD_EFFECT_BACKGROUND_TEXTURE,
                    x, y, 24, 24);
            context.item(icons[i], x + 4, y + 4);
        }
    }

    private void drawActualSubtitlesPreview(GuiGraphicsExtractor context, int width, int height) {
        String[] lines = {"< Footsteps", "> Zombie groans"};
        for (int i = 0; i < lines.length; i++) {
            int yy = height - 20 - i * 10;
            int lineWidth = this.font.width(lines[i]);
            int x = Math.max(0, width - lineWidth - 8);
            context.fill(x - 2, yy - 1, width, yy + 9, 0x80000000);
            context.text(this.font, lines[i], x, yy, 0xFFFFFFFF);
        }
    }

    private void drawActualToastPreview(GuiGraphicsExtractor context) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, TOAST_SYSTEM_TEXTURE, 160, 32, 0, 0, 0, 0, 160, 32);
        context.item(new ItemStack(Items.DIAMOND), 8, 8);
        context.text(this.font, "Advancement Made!", 30, 7, 0xFFFFFF00);
        context.text(this.font, "Shiny Preview", 30, 18, 0xFFFFFFFF);
    }

    private void drawActualTooltipPreview(GuiGraphicsExtractor context, int width, int height) {
        context.fill(0, 0, width, height, 0xF0100010);
        context.fill(0, 0, width, 1, 0x50A0A0FF);
        context.fill(0, height - 1, width, height, 0x505050AA);
        context.fill(0, 0, 1, height, 0x50A0A0FF);
        context.fill(width - 1, 0, width, height, 0x505050AA);
        context.text(this.font, "Diamond Sword", 4, 4, 0xFF55FFFF);
        context.text(this.font, "+7 Attack Damage", 4, 16, 0xFFAAAAFF);
    }

    private void drawVanillaPreviewOverlay(GuiGraphicsExtractor context,
                                           HudTweaksSettings.ElementType type,
                                           HudTweaksSettings.ElementConfig config,
                                           int x1,
                                           int y1,
                                           int x2,
                                           int y2) {
        float opacity = Math.clamp(config.opacity, 0.0f, 1.0f);
        if (opacity < 0.99f) {
            int alpha = Math.clamp(Math.round((1.0f - opacity) * 180.0f), 0, 220);
            context.fill(x1, y1, x2, y2, alpha << 24);
        }
        if (!config.display) {
            context.fill(x1, y1, x2, y2, 0x88000000);
            String hidden = vanillaPreviewLabel(type) + " [Hidden]";
            context.text(this.font, hidden, x1 + 4, y1 + 4, 0xFFFFA0A0);
        }
    }

    private ItemStack previewHotbarStack(Player player, int slot) {
        if (player != null && slot >= 0 && slot < 9) {
            return player.getInventory().getItem(slot);
        }
        return switch (slot) {
            case 0 -> new ItemStack(Items.DIAMOND_SWORD);
            case 1 -> new ItemStack(Items.BOW);
            case 2 -> new ItemStack(Items.COOKED_BEEF, 16);
            case 3 -> new ItemStack(Items.COBBLESTONE, 64);
            case 4 -> new ItemStack(Items.TORCH, 32);
            case 5 -> new ItemStack(Items.WATER_BUCKET);
            case 6 -> new ItemStack(Items.GOLDEN_APPLE, 3);
            case 7 -> new ItemStack(Items.ENDER_PEARL, 8);
            case 8 -> new ItemStack(Items.CLOCK);
            default -> ItemStack.EMPTY;
        };
    }

    private String playerPreviewName() {
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getName().getString();
        }
        return "Player";
    }

    private VanillaPlayerListMetrics currentPlayerListMetrics() {
        return currentPlayerListMetrics(1.0f);
    }

    private VanillaPlayerListMetrics currentPlayerListMetrics(float scale) {
        float clampedScale = Math.clamp(scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE);
        int renderWidth = Math.max(1, Math.round(this.width / clampedScale));
        int top = 9;
        if (this.minecraft == null || this.minecraft.gui == null || this.minecraft.player == null || this.minecraft.player.connection == null) {
            int fallbackWidth = 240;
            return new VanillaPlayerListMetrics(
                    Math.round(((renderWidth - fallbackWidth) / 2.0f) * clampedScale),
                    Math.round(top * clampedScale),
                    Math.round(fallbackWidth * clampedScale),
                    Math.round(140 * clampedScale),
                    renderWidth
            );
        }

        var playerListHud = this.minecraft.gui.hud.getTabList();
        List<PlayerInfo> entries = this.minecraft.player.connection.getListedOnlinePlayers().stream().limit(80).toList();
        int nameWidth = 0;
        int scoreWidth = 0;
        Scoreboard scoreboard = this.minecraft.level == null ? null : this.minecraft.level.getScoreboard();
        Objective objective = scoreboard == null ? null : scoreboard.getDisplayObjective(DisplaySlot.LIST);
        boolean scoreHearts = objective != null && objective.getRenderType() == net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.HEARTS;
        int colonWidth = this.font.width(":");

        for (PlayerInfo entry : entries) {
            nameWidth = Math.max(nameWidth, this.font.width(playerListHud.getNameForDisplay(entry)));
            if (objective != null && scoreboard != null && !scoreHearts) {
                ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(ScoreHolder.fromGameProfile(entry.getProfile()), objective);
                if (score != null) {
                    scoreWidth = Math.max(scoreWidth, colonWidth + this.font.width(score.formatValue(objective.numberFormatOrDefault(net.minecraft.network.chat.numbers.StyledFormat.PLAYER_LIST_DEFAULT))));
                }
            }
        }

        int size = entries.size();
        int rows = size;
        int columns = 1;
        while (rows > 20) {
            columns++;
            rows = (size + columns - 1) / columns;
        }

        boolean showSkins = true;
        int scoreAreaWidth = objective == null ? 0 : (scoreHearts ? 90 : scoreWidth);
        int columnWidth = columns == 0 ? 0 : Math.min(columns * ((showSkins ? 9 : 0) + nameWidth + scoreAreaWidth + 13), renderWidth - 50) / Math.max(1, columns);
        int tableWidth = columnWidth * Math.max(1, columns) + Math.max(0, columns - 1) * 5;
        int fullWidth = tableWidth;

        Component header = ((PlayerListHudAccessor) playerListHud).m0dev$getHeader();
        Component footer = ((PlayerListHudAccessor) playerListHud).m0dev$getFooter();
        int currentY = 10;
        if (header != null) {
            List<?> wrappedHeader = this.font.split(header, renderWidth - 50);
            for (Object line : wrappedHeader) {
                fullWidth = Math.max(fullWidth, this.font.width((net.minecraft.util.FormattedCharSequence) line));
            }
            currentY += wrappedHeader.size() * 9 + 1;
        }
        currentY += rows * 9;
        if (footer != null) {
            currentY += 1;
            List<?> wrappedFooter = this.font.split(footer, renderWidth - 50);
            for (Object line : wrappedFooter) {
                fullWidth = Math.max(fullWidth, this.font.width((net.minecraft.util.FormattedCharSequence) line));
            }
            currentY += wrappedFooter.size() * 9;
        }

        int left = renderWidth / 2 - fullWidth / 2 - 1;
        int height = Math.max(20, currentY - top);
        int width = Math.max(20, fullWidth + 2);
        return new VanillaPlayerListMetrics(
                Math.round(left * clampedScale),
                Math.round(top * clampedScale),
                Math.round(width * clampedScale),
                Math.round(height * clampedScale),
                renderWidth
        );
    }

    private VanillaSidebarMetrics currentSidebarMetrics() {
        return currentSidebarMetrics(1.0f);
    }

    private VanillaSidebarMetrics currentSidebarMetrics(float scale) {
        float clampedScale = Math.clamp(scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE);
        Objective objective = effectiveSidebarObjective();
        if (objective == null) {
            int fallbackWidth = 144;
            int fallbackHeight = 100;
            int logicalHeight = Math.max(1, Math.round(this.height / clampedScale));
            int logicalTop = Math.max(0, logicalHeight / 2 - (fallbackHeight * 2 / 3));
            int logicalLeft = Math.max(0, Math.round(this.width / clampedScale) - fallbackWidth - 5);
            return new VanillaSidebarMetrics(
                    Math.round(logicalLeft * clampedScale),
                    Math.round(logicalTop * clampedScale),
                    Math.round(fallbackWidth * clampedScale),
                    Math.round(fallbackHeight * clampedScale)
            );
        }

        Scoreboard scoreboard = objective.getScoreboard();
        int width = this.font.width(objective.getDisplayName());
        int colonWidth = this.font.width(":");
        List<VanillaSidebarEntryMetrics> entries = scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner))
                .limit(15)
                .map(entry -> measureSidebarEntry(scoreboard, objective, entry))
                .toList();
        for (VanillaSidebarEntryMetrics entry : entries) {
            width = Math.max(width, this.font.width(entry.name()) + (entry.scoreWidth() > 0 ? colonWidth + entry.scoreWidth() : 0));
        }

        int listHeight = entries.size() * 9;
        int logicalHeight = Math.max(1, Math.round(this.height / clampedScale));
        int logicalWidth = Math.max(1, Math.round(this.width / clampedScale));
        int bottom = logicalHeight / 2 + listHeight / 3;
        int top = bottom - listHeight - 10;
        int scaledWidth = Math.round((width + 4) * clampedScale);
        int scaledHeight = Math.round((listHeight + 10) * clampedScale);
        int scaledRight = Math.round((logicalWidth - 1) * clampedScale);
        int scaledBottom = Math.round(bottom * clampedScale);
        return new VanillaSidebarMetrics(
                scaledRight - scaledWidth,
                scaledBottom - scaledHeight,
                scaledWidth,
                scaledHeight
        );
    }

    private VanillaSidebarEntryMetrics measureSidebarEntry(Scoreboard scoreboard,
                                                           Objective objective,
                                                           PlayerScoreEntry entry) {
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        Component decoratedName = PlayerTeam.formatNameForTeam(team, entry.ownerName());
        Component scoreText = entry.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT));
        return new VanillaSidebarEntryMetrics(decoratedName, this.font.width(scoreText));
    }

    private Objective effectiveSidebarObjective() {
        Scoreboard scoreboard = this.minecraft == null || this.minecraft.level == null ? null : this.minecraft.level.getScoreboard();
        Objective live = scoreboard == null ? null : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (live != null) {
            return live;
        }
        return previewSidebarObjective();
    }

    private Objective previewSidebarObjective() {
        Scoreboard scoreboard = new Scoreboard();
        Objective objective = scoreboard.addObjective(
                "m0dev_preview_sidebar",
                ObjectiveCriteria.DUMMY,
                Component.literal("Objectives"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                StyledFormat.SIDEBAR_DEFAULT
        );
        addPreviewSidebarScore(scoreboard, objective, "Kills", 12);
        addPreviewSidebarScore(scoreboard, objective, "Beds", 3);
        addPreviewSidebarScore(scoreboard, objective, "Ping", 42);
        addPreviewSidebarScore(scoreboard, objective, "Coins", 256);
        return objective;
    }

    private void addPreviewSidebarScore(Scoreboard scoreboard, Objective objective, String name, int value) {
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective).set(value);
    }

    private void drawSelectedOutline(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, boolean isSelected) {
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
            deleteButton.active = isVanillaCanvasMode()
                    ? !this.vanillaCanvasElements.isEmpty()
                    : selected != null;
        }

        if (selected == null) {
            selectedElementIds.clear();
            quickField.setEditable(false);
            quickField.setValue("");
            macroField.setEditable(false);
            macroField.setValue("");
            macroField.visible = false;
            macroField.active = false;
            actionField.setEditable(false);
            actionField.setValue("");
            actionField.visible = false;
            actionField.active = false;
            syncStyleButtons();
            syncCanvasModeControls();
            return;
        }

        if (isVanillaHudProxy(selected)) {
            quickField.setEditable(false);
            quickField.setValue(vanillaQuickFieldText(selected));
            macroField.setEditable(false);
            macroField.setValue("");
            macroField.visible = false;
            macroField.active = false;
            actionField.setEditable(false);
            actionField.setValue("");
            actionField.visible = false;
            actionField.active = false;
            syncStyleButtons();
            syncCanvasModeControls();
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
            quickField.setValue(proxyName + " (edit style/pos/size)" + (externalState == ExternalProxyRenderState.MODULE_DISABLED ? " [DISABLED]" : ""));
            macroField.setEditable(false);
            macroField.setValue("");
            macroField.visible = false;
            macroField.active = false;
            actionField.setEditable(false);
            actionField.setValue("");
            actionField.visible = false;
            actionField.active = false;
            syncStyleButtons();
            syncCanvasModeControls();
            return;
        }

        quickField.setEditable(true);
        String raw = selected.type == MacroHudDataHandler.ElementType.TEXT ? selected.text : selected.label;
        quickField.setValue(firstLine(raw));

        boolean button = selected.type == MacroHudDataHandler.ElementType.BUTTON;
        macroField.setEditable(button);
        macroField.visible = button;
        macroField.active = button;
        macroField.setValue(selected.macroId == null ? "" : selected.macroId);
        actionField.setEditable(button);
        actionField.visible = button;
        actionField.active = button;
        actionField.setValue(selected.buttonAction == null ? "" : selected.buttonAction);

        syncStyleButtons();
        syncCanvasModeControls();
    }

    private void applyQuickEdit() {
        if (selected == null || quickField == null || macroField == null || actionField == null) {
            return;
        }
        if (isExternalCanvasProxy(selected) || isVanillaHudProxy(selected)) {
            return;
        }

        String raw = selected.type == MacroHudDataHandler.ElementType.TEXT ? selected.text : selected.label;
        List<String> lines = new ArrayList<>(splitLinesRaw(raw));
        if (lines.isEmpty()) {
            lines = new ArrayList<>(List.of(""));
        }
        lines.set(0, quickField.getValue() == null ? "" : quickField.getValue());
        String merged = String.join("\n", lines);

        if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            selected.label = merged;
            selected.macroId = macroField.getValue() == null ? "" : macroField.getValue().trim();
            String compactAction = actionField.getValue() == null ? "" : actionField.getValue();
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
            if (isVanillaCanvasMode()) {
                backgroundToggle.active = true;
                borderToggle.active = false;
                if (editButton != null) {
                    editButton.active = false;
                }
                backgroundToggle.setMessage(Component.literal(vanillaPreviewMode == VanillaPreviewMode.ACTUAL ? "Preview: Actual" : "Preview: Virtual"));
                borderToggle.setMessage(Component.literal("Visible"));
                return;
            }
            backgroundToggle.active = false;
            borderToggle.active = false;
            if (editButton != null) {
                editButton.active = !isVanillaCanvasMode();
            }
            backgroundToggle.setMessage(Component.literal("BG: OFF"));
            borderToggle.setMessage(Component.literal("Border: OFF"));
            return;
        }

        if (isVanillaHudProxy(selected)) {
            backgroundToggle.active = true;
            borderToggle.active = true;
            if (editButton != null) {
                editButton.active = false;
            }
            backgroundToggle.setMessage(Component.literal(vanillaPreviewMode == VanillaPreviewMode.ACTUAL ? "Preview: Actual" : "Preview: Virtual"));
            HudTweaksSettings.ElementType type = vanillaHudElementType(selected);
            HudTweaksSettings.ElementConfig config = type == null ? null : this.vanillaCanvasConfigs.get(type);
            borderToggle.setMessage(Component.literal("Visible: " + ((config == null || config.display) ? "ON" : "OFF")));
            return;
        }

        backgroundToggle.active = true;
        borderToggle.active = true;
        if (editButton != null) {
            editButton.active = true;
        }
        backgroundToggle.setMessage(Component.literal(backgroundLabel(selected)));
        borderToggle.setMessage(Component.literal(borderModeLabel(selected)));
    }

    private void syncGridButtons() {
        if (gridToggleButton == null || gridRowsMinusButton == null || gridRowsPlusButton == null
                || gridColsMinusButton == null || gridColsPlusButton == null || centerLinesToggleButton == null) {
            return;
        }
        gridToggleButton.setMessage(Component.literal(gridEnabled ? "Grid: ON" : "Grid: OFF"));
        gridRowsMinusButton.setMessage(Component.literal("\u2212"));
        gridRowsPlusButton.setMessage(Component.literal("+"));
        gridColsMinusButton.setMessage(Component.literal("\u2212"));
        gridColsPlusButton.setMessage(Component.literal("+"));
        centerLinesToggleButton.setMessage(Component.literal(centerLinesEnabled ? "Center: ON" : "Center: OFF"));
    }

    private void syncPresetControls() {
        if (presetNameField == null || presetDeleteButton == null || presetPrevButton == null || presetNextButton == null) {
            return;
        }
        if (isVanillaCanvasMode()) {
            presetNameField.setValue("vanilla");
            presetNameField.setEditable(false);
            presetPrevButton.active = false;
            presetNextButton.active = false;
            presetDeleteButton.active = false;
            if (presetNewButton != null) {
                presetNewButton.active = false;
            }
            if (presetRenameButton != null) {
                presetRenameButton.active = false;
            }
            return;
        }
        String active = this.working == null ? "default" : StringUtils.safe(this.working.activePresetId);
        presetNameField.setValue(active);
        presetNameField.setEditable(true);
        List<String> ids = MacroHudDataHandler.listPresetIds();
        int idx = activePresetIndex(ids, active);
        presetDeleteButton.active = ids.size() > 1;
        presetPrevButton.active = idx > 0;
        presetNextButton.active = idx >= 0 && idx < ids.size() - 1;
        if (presetNewButton != null) {
            presetNewButton.active = true;
        }
        if (presetRenameButton != null) {
            presetRenameButton.active = true;
        }
    }

    private void syncCanvasModeControls() {
        if (this.canvasModeToggleButton != null) {
            this.canvasModeToggleButton.setMessage(Component.literal(
                    isVanillaCanvasMode() ? "Vanilla HUD" : "Custom HUD"
            ));
        }
        if (this.deleteButton != null) {
            this.deleteButton.setMessage(Component.literal(
                    isVanillaCanvasMode()
                            ? (selected != null && isVanillaHudProxy(selected) ? "Reset Selected" : "Reset All")
                            : "Delete"
            ));
        }
    }

    private void cyclePreset(boolean forward) {
        if (isVanillaCanvasMode()) {
            return;
        }
        if (this.working == null) {
            return;
        }
        saveAll();
        List<String> ids = MacroHudDataHandler.listPresetIds();
        if (ids.isEmpty()) {
            return;
        }
        String active = MacroHudDataHandler.getActivePresetId();
        int idx = activePresetIndex(ids, active);
        if (idx < 0) {
            return;
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0 || next >= ids.size()) {
            syncPresetControls();
            return;
        }
        MacroHudDataHandler.setActivePresetId(ids.get(next));
        this.working = MacroHudDataHandler.getConfigCopy();
        syncExternalCanvasElementsFromSources();
        this.selected = null;
        syncCanvasFields();
        syncPresetControls();
    }

    private int activePresetIndex(List<String> ids, String active) {
        if (ids.isEmpty()) {
            return -1;
        }
        int idx = ids.indexOf(active);
        return Math.max(idx, 0);
    }

    private void createPresetFromField() {
        if (isVanillaCanvasMode()) {
            return;
        }
        String name = StringUtils.safe(presetNameField == null ? null : presetNameField.getValue());
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
        if (isVanillaCanvasMode()) {
            return;
        }
        String target = StringUtils.safe(presetNameField == null ? null : presetNameField.getValue());
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

    private void toggleCanvasMode() {
        applyQuickEdit();
        if (isVanillaCanvasMode()) {
            persistVanillaCanvasElements();
            this.canvasMode = CanvasMode.CUSTOM;
        } else {
            this.canvasMode = CanvasMode.VANILLA;
            syncVanillaCanvasElementsFromSettings();
        }
        this.selected = null;
        this.selectedElementIds.clear();
        this.dragging = false;
        this.resizing = false;
        closeAdvancedModal();
        syncCanvasFields();
        syncPresetControls();
    }

    private void toggleVanillaPreviewMode() {
        if (!isVanillaCanvasMode()) {
            return;
        }
        this.vanillaPreviewMode = this.vanillaPreviewMode == VanillaPreviewMode.ACTUAL
                ? VanillaPreviewMode.VIRTUAL
                : VanillaPreviewMode.ACTUAL;
        syncCanvasFields();
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
        syncCanvasModeControls();
    }

    private void persistGridPrefs() {
        GRID_ENABLED_PREF = gridEnabled;
        GRID_ROWS_PREF = Math.max(2, gridRows);
        GRID_COLS_PREF = Math.max(2, gridCols);
        CENTER_LINES_ENABLED_PREF = centerLinesEnabled;
    }

    private void openAdvancedModal() {
        if (selected == null || isVanillaHudProxy(selected)) {
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
                ? StringUtils.preserve(selected.buttonAction)
                : (selected.type == MacroHudDataHandler.ElementType.TEXT ? StringUtils.preserve(selected.text) : StringUtils.preserve(selected.label));
        this.advancedAction = selected.type == MacroHudDataHandler.ElementType.BUTTON ? StringUtils.preserve(selected.label) : StringUtils.preserve(selected.sourceToken);
        this.advancedVisibilityScreenType = StringUtils.preserve(selected.visibilityScreenType);
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

    private void renderAdvancedModal(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int boxX = modalX();
        int boxY = modalY();
        StandardAdvancedLayout layout = standardAdvancedLayout(boxX, boxY);
        UiRect textArea = layout.textArea();
        UiRect actionField = layout.actionField();

        int modalW = modalWidth();
        int modalH = modalHeight();
        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, modalW, modalH);

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

        context.text(this.font, "Edit", boxX + 12, boxY + 12, 0xFFFFFFFF);

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
                context.text(this.font, line, textArea.x() + 4, y, 0xFFEAEAEA);
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
            context.text(this.font, "Command / Script (multi-line)", textArea.x(), textArea.y() - 10, 0xFFB8B8B8);
            context.text(this.font, "Label (single-line)", actionField.x(), actionField.y() - 10, 0xFFB8B8B8);
        } else {
            context.text(this.font, "Button Action (cmd:/ msg:/ copy:/ etc)", actionField.x(), actionField.y() - 10, 0xFFB8B8B8);
        }
        int actionBg = advancedActionFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(actionField.x(), actionField.y(), actionField.right(), actionField.bottom(), actionBg);
        context.fill(actionField.x(), actionField.y(), actionField.right(), actionField.y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, actionField.x() + 4, actionField.y() + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.text(this.font, advancedAction, actionField.x() + 4, actionField.y() + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = actionField.x() + 4 + this.font.width(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
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
        context.text(this.font,
                "Line: " + (selected != null ? selected.lineHeight : 9) + "   Scale: " + (selected != null ? String.format("%.1f", selected.fontScale) : "1.0") + "   BG: " + bgPct + "%   Z: " + (selected != null ? selected.zIndex : 0),
                layout.bgHex().x(), layout.bgHex().y() - 10, 0xFFEAEAEA);
        int labelLeftBound = boxX + 12;
        context.text(this.font, "BG",
                Math.max(labelLeftBound, layout.bgHex().x() - this.font.width("BG") - 8),
                layout.bgHex().y() + 4, 0xFFEAEAEA);
        context.text(this.font, "BR",
                Math.max(labelLeftBound, layout.borderHex().x() - this.font.width("BR") - 8),
                layout.borderHex().y() + 4, 0xFFEAEAEA);
        context.text(this.font, ThemeDraw.fit(this.font, "Use Opacity +/- for transparency. Right-click BR hex to open picker.", layout.apply().x() - layout.bgHex().x() - 8),
                layout.bgHex().x(), layout.bgHex().bottom() + 2, Theme.TEXT_DIM, false);

        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(layout.bgHex().x(), layout.bgHex().y(), layout.bgHex().right(), layout.bgHex().bottom(), bgInputBg);
        context.fill(layout.bgHex().x(), layout.bgHex().y(), layout.bgHex().right(), layout.bgHex().y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, layout.bgHex().x() + 4, layout.bgHex().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        context.text(this.font, advancedBgColor, layout.bgHex().x() + 4, layout.bgHex().y() + 5, 0xFFEAEAEA);

        int borderInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(layout.borderHex().x(), layout.borderHex().y(), layout.borderHex().right(), layout.borderHex().bottom(), borderInputBg);
        context.fill(layout.borderHex().x(), layout.borderHex().y(), layout.borderHex().right(), layout.borderHex().y() + 1, 0x60FFFFFF);
        drawSingleLineSelection(context, layout.borderHex().x() + 4, layout.borderHex().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.text(this.font, advancedBorderColor, layout.borderHex().x() + 4, layout.borderHex().y() + 5, 0xFFEAEAEA);

        if (selected != null && selected.visibilityMode == MacroHudDataHandler.VisibilityMode.SCREEN) {
            int visTypeX = layout.visibilityType().x();
            int visTypeY = layout.visibilityType().y();
            int visTypeW = layout.visibilityType().width();
            context.text(this.font, "Visibility Screen Type", visTypeX, visTypeY - 10, 0xFFB8B8B8);
            int visTypeBg = advancedVisibilityScreenTypeFocused ? 0xFF0F0F0F : 0xFF161616;
            context.fill(visTypeX, visTypeY, visTypeX + visTypeW, visTypeY + 18, visTypeBg);
            context.fill(visTypeX, visTypeY, visTypeX + visTypeW, visTypeY + 1, 0x60FFFFFF);
            drawSingleLineSelection(context, visTypeX + 4, visTypeY + 5, advancedVisibilityScreenType,
                    advancedVisibilityScreenTypeSelectionAnchor, advancedVisibilityScreenTypeCursor);
            context.text(this.font, advancedVisibilityScreenType, visTypeX + 4, visTypeY + 5, 0xFFEAEAEA);
            if (advancedVisibilityScreenTypeFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                int cx = visTypeX + 4 + this.font.width(advancedVisibilityScreenType.substring(0,
                        Math.clamp(advancedVisibilityScreenTypeCursor, 0, advancedVisibilityScreenType.length())));
                context.fill(cx, visTypeY + 4, cx + 1, visTypeY + 13, 0xFFFFFFFF);
            }
        }

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.bgHex().x() + 4 + this.font.width(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, layout.bgHex().y() + 4, cx + 1, layout.bgHex().y() + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.borderHex().x() + 4 + this.font.width(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, layout.borderHex().y() + 4, cx + 1, layout.borderHex().y() + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, layout.apply().x(), layout.apply().y(), layout.apply().width(), layout.apply().height(), "Apply", layout.apply().contains(mouseX, mouseY));
        drawModalButton(context, layout.cancel().x(), layout.cancel().y(), layout.cancel().width(), layout.cancel().height(), "Cancel", layout.cancel().contains(mouseX, mouseY));
    }

    private StandardAdvancedLayout standardAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.standard(boxX, boxY, modalWidth(), modalHeight());
    }

    private int[] cursorPixelWithScroll(int baseX, int baseY, String text, int cursorIndex, int scrollLine) {
        return GuiTextEditingUtils.cursorPixelWithScroll(this.font, baseX, baseY, text, cursorIndex, scrollLine);
    }

    private static int cursorLineIndex(String text, int cursorIndex) {
        return GuiTextEditingUtils.cursorLineIndex(text, cursorIndex);
    }

    private void renderSecondaryChatAdvancedModal(GuiGraphicsExtractor context, int mouseX, int mouseY, int boxX, int boxY) {
        SecondaryAdvancedLayout layout = secondaryAdvancedLayout(boxX, boxY);
        MacroWorkbenchProxyAdvancedModalRenderer.renderSecondary(
                context,
                this.font,
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

    private void renderNbtInspectorAdvancedModal(GuiGraphicsExtractor context, int mouseX, int mouseY, int boxX, int boxY) {
        String title = isMacroKeybindProxy(selected)
                ? "Macro Keybinds Settings"
                : (isPickupNotifierProxy(selected) ? "Pick-up Notifier Settings" : "NBT Inspector Settings");
        ProxyAdvancedLayout layout = proxyAdvancedLayout(boxX, boxY, isPickupNotifierProxy(selected));
        MacroWorkbenchProxyAdvancedModalRenderer.renderProxy(
                context,
                this.font,
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
        return MacroWorkbenchAdvancedLayouts.proxy(boxX, boxY, modalWidth(), modalHeight(), pickup);
    }

    private SecondaryAdvancedLayout secondaryAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.secondary(boxX, boxY, modalWidth(), modalHeight());
    }

    private CustomWidgetAdvancedLayout customWidgetAdvancedLayout(int boxX, int boxY) {
        return MacroWorkbenchAdvancedLayouts.custom(boxX, boxY, modalWidth(), modalHeight());
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }

    private void renderCustomWidgetAdvancedModal(GuiGraphicsExtractor context, int mouseX, int mouseY, int boxX, int boxY) {
        if (selected == null) {
            return;
        }
        CustomWidgetAdvancedLayout layout = customWidgetAdvancedLayout(boxX, boxY);
        List<String> suggestions = advancedActionSuggestions();
        int rowH = 10;
        int maxVisible = Math.clamp(Math.max(1, layout.suggestionArea().height() / rowH), 1, Math.max(1, suggestions.size()));
        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
        advancedActionSuggestionScroll = Math.clamp(advancedActionSuggestionScroll, 0, maxScroll);
        MacroWorkbenchCustomWidgetAdvancedModalRenderer.extractRenderState(
                context,
                this.font,
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

    private boolean onCustomWidgetAdvancedClick(MouseButtonEvent click, int boxX, int boxY) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT && click.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return true;
        }
        boolean forward = click.button() != InputConstants.MOUSE_BUTTON_RIGHT;
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

    private boolean handleAdvancedSuggestionClick(MouseButtonEvent click, UiRect suggestionsArea, List<String> suggestions) {
        return MacroWorkbenchCustomWidgetAdvancedClickHandler.handleSuggestionClick(click, suggestionsArea, suggestions, customWidgetAdvancedClickOpsBridge);
    }

    private boolean handleCustomWidgetGeneralRowClick(MouseButtonEvent click, List<UiRect> generalRow1, List<UiRect> generalRow2, boolean forward) {
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

    private boolean handleCustomWidgetBaseRowClick(MouseButtonEvent click, List<UiRect> baseRow, boolean forward) {
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
            public boolean contains(MouseButtonEvent click, UiRect rect) {
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

    boolean cwContains(MouseButtonEvent click, UiRect rect) {
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

    private boolean focusAdvancedLabelInput(MouseButtonEvent click, UiRect labelInput) {
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

    private boolean focusAdvancedSourceInput(MouseButtonEvent click, UiRect sourceInput) {
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

    private boolean handleCustomWidgetTypeClick(MouseButtonEvent click, CustomWidgetAdvancedLayout layout, boolean forward) {
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
                    public boolean contains(MouseButtonEvent valueClick, UiRect rect) {
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
                        return MacroWorkbenchScreen.iconIdSuggestionsForKind(kind);
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

    private boolean handleCustomWidgetTypeInputFocus(MouseButtonEvent click, CustomWidgetAdvancedLayout layout) {
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

    private boolean focusAdvancedTypeInputField(MouseButtonEvent click, UiRect input, ModalDragSelectionField field, boolean resetSelectionAnchor) {
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
                || element.type == MacroHudDataHandler.ElementType.STATE_BADGE
                || element.type == MacroHudDataHandler.ElementType.REDSTONE_TIMELINE
                || element.type == MacroHudDataHandler.ElementType.STATUS;
    }

    private int stepInt(int base) {
        return base * (isShiftDown() ? 10 : 1);
    }

    private boolean isControlPressed() {
        if (this.minecraft == null) {
            return false;
        }
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
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
        openCommandListEditor(macro.commands, commands -> {
            if (selectedMacroId == null) {
                return;
            }
            MacroDataHandler.MacroEntry existing = MacroDataHandler.getMacro(selectedMacroId);
            if (existing == null) {
                return;
            }

            MacroDataHandler.updateMacro(
                    selectedMacroId,
                    StringUtils.safe(kbNameField.getValue()),
                    commands,
                    existing.keyCode,
                    existing.modifierKey,
                    parseDelayOrDefault(existing.delayTicks),
                    existing.showInOverlay
            );

            kbCommandsField.setValue(String.join(";", commands));
            CommandMacros.refreshKeybindings();
            rebuildBindingMaps();
        });
    }

    private void closeKeyboardCommandsModal() {
        this.kbCommandsModalOpen = false;
        this.kbCommandsFocused = false;
        this.commandListApplyConsumer = null;
        this.activeDragSelectionField = ModalDragSelectionField.NONE;
        this.modalDragStartMouseX = Integer.MIN_VALUE;
        this.modalDragStartMouseY = Integer.MIN_VALUE;
        this.modalDragSelectionStarted = false;
    }

    void openCommandListEditor(List<String> initialCommands, Consumer<List<String>> applyConsumer) {
        this.kbCommandsModalOpen = true;
        this.kbCommandsFocused = true;
        this.commandListApplyConsumer = applyConsumer;
        this.kbCommandsText = initialCommands == null ? "" : String.join("\n", initialCommands);
        this.kbCommandsCursor = this.kbCommandsText.length();
        this.kbCommandsSelectionAnchor = -1;
    }

    private void renderKeyboardCommandsModal(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int modalW = modalWidth();
        int modalH = modalHeight();
        int boxX = this.width / 2 - (modalW / 2);
        int boxY = this.height / 2 - (modalH / 2);
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = modalW - 24;
        int textH = Math.max(96, modalH - 68);

        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, modalW, modalH);

        context.text(this.font, "Edit Commands", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.text(this.font, "One command per line", boxX + 12, boxY + 22, 0xFFB0B0B0);

        context.fill(textX, textY, textX + textW, textY + textH, kbCommandsFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textX, textY, textX + textW, textY + 1, 0x60FFFFFF);
        drawMultilineSelection(context, textX + 4, textY + 4, textY + textH - 12, kbCommandsText, kbCommandsSelectionAnchor, kbCommandsCursor, 9);

        List<String> lines = splitLinesRaw(kbCommandsText);
        int yy = textY + 4;
        for (String line : lines) {
            if (yy > textY + textH - 12) {
                break;
            }
            context.text(this.font, line, textX + 4, yy, 0xFFEAEAEA);
            yy += 9;
        }

        if (kbCommandsFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = cursorPixel(textX + 4, textY + 4, kbCommandsText, kbCommandsCursor);
            context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + modalW - 122, boxY + modalH - 22, 54, 18, "Apply", containsBox(mouseX, mouseY, boxX + modalW - 122, boxY + modalH - 22, 54, 18));
        drawModalButton(context, boxX + modalW - 64, boxY + modalH - 22, 54, 18, "Cancel", containsBox(mouseX, mouseY, boxX + modalW - 64, boxY + modalH - 22, 54, 18));
    }

    private boolean onKeyboardCommandsModalClick(MouseButtonEvent click) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return true;
        }

        this.activeDragSelectionField = ModalDragSelectionField.NONE;

        int modalW = modalWidth();
        int modalH = modalHeight();
        int boxX = this.width / 2 - (modalW / 2);
        int boxY = this.height / 2 - (modalH / 2);
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = modalW - 24;
        int textH = Math.max(96, modalH - 68);

        if (containsBox(click.x(), click.y(), boxX + modalW - 122, boxY + modalH - 22, 54, 18)) {
            applyKeyboardCommandsModal();
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + modalW - 64, boxY + modalH - 22, 54, 18)) {
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

    private void renderAddElementModal(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int boxX = this.width / 2 - 160;
        int boxY = this.height / 2 - 110;
        int boxW = 320;
        int boxH = 220;
        context.fill(0, 0, this.width, this.height, 0x88000000);
        GuiSystem.drawPanel(context, boxX, boxY, boxW, boxH);
        context.text(this.font, "Add Element", boxX + 12, boxY + 10, 0xFFFFFFFF);
        context.text(this.font, "Pick element type", boxX + 12, boxY + 22, 0xFFB0B0B0);

        String[] labels = {"Button", "Text", "Macro List", "Icon", "Bar", "Value", "List", "Inventory", "Shape", "State Badge", "Redstone Timeline", "Status"};
        for (int i = 0; i < labels.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int bx = boxX + 12 + col * 100;
            int by = boxY + 42 + row * 26;
            drawModalButton(context, bx, by, 92, 20, labels[i], containsBox(mouseX, mouseY, bx, by, 92, 20));
        }
        drawModalButton(context, boxX + boxW - 70, boxY + boxH - 24, 58, 18, "Close", containsBox(mouseX, mouseY, boxX + boxW - 70, boxY + boxH - 24, 58, 18));
    }

    private boolean onAddElementModalClick(MouseButtonEvent click) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
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
                MacroHudDataHandler.ElementType.STATE_BADGE,
                MacroHudDataHandler.ElementType.REDSTONE_TIMELINE,
                MacroHudDataHandler.ElementType.STATUS
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
        if (isCtrlDown() && keyCode == InputConstants.KEY_A) {
            kbCommandsSelectionAnchor = 0;
            kbCommandsCursor = kbCommandsText.length();
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
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
        if (keyCode == InputConstants.KEY_DELETE) {
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
        if (keyCode == InputConstants.KEY_LEFT) {
            int old = kbCommandsCursor;
            kbCommandsCursor = Math.max(0, kbCommandsCursor - 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            int old = kbCommandsCursor;
            kbCommandsCursor = Math.min(kbCommandsText.length(), kbCommandsCursor + 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            int old = kbCommandsCursor;
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, -1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            int old = kbCommandsCursor;
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, 1);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            int old = kbCommandsCursor;
            kbCommandsCursor = lineStart(kbCommandsText, kbCommandsCursor);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            int old = kbCommandsCursor;
            kbCommandsCursor = lineEnd(kbCommandsText, kbCommandsCursor);
            kbCommandsSelectionAnchor = updateSelectionAnchor(kbCommandsSelectionAnchor, old, kbCommandsCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            insertAtKeyboardCommandsCursor("\n");
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_V && this.minecraft != null) {
            insertAtKeyboardCommandsCursor(this.minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_C && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)
                    ? selectedText(kbCommandsText, kbCommandsSelectionAnchor, kbCommandsCursor)
                    : kbCommandsText);
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_X && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(kbCommandsSelectionAnchor, kbCommandsCursor)
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
        if (keyCode == InputConstants.KEY_ESCAPE) {
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
        if (this.commandListApplyConsumer != null) {
            this.commandListApplyConsumer.accept(commands);
        }
        closeKeyboardCommandsModal();
    }

    private int parseDelayOrDefault(int fallback) {
        try {
            if (kbDelayField.getValue() != null && !kbDelayField.getValue().isBlank()) {
                return Math.max(0, Integer.parseInt(kbDelayField.getValue().trim()));
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private boolean handleAdvancedTextKey(int keyCode) {
        if (isCtrlDown() && keyCode == InputConstants.KEY_A) {
            advancedSelectionAnchor = 0;
            advancedCursor = advancedText.length();
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
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
        if (keyCode == InputConstants.KEY_DELETE) {
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
        if (keyCode == InputConstants.KEY_LEFT) {
            int old = advancedCursor;
            advancedCursor = Math.max(0, advancedCursor - 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            int old = advancedCursor;
            advancedCursor = Math.min(advancedText.length(), advancedCursor + 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            int old = advancedCursor;
            advancedCursor = lineStart(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            int old = advancedCursor;
            advancedCursor = lineEnd(advancedText, advancedCursor);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, -1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            int old = advancedCursor;
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, 1);
            advancedSelectionAnchor = updateSelectionAnchor(advancedSelectionAnchor, old, advancedCursor, isShiftDown());
            advancedTextManualScroll = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            insertAtCursor("\n");
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_V && this.minecraft != null) {
            insertAtCursor(this.minecraft.keyboardHandler.getClipboard());
            advancedActionSuggestionIndex = -1;
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_C && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(advancedSelectionAnchor, advancedCursor)
                    ? selectedText(advancedText, advancedSelectionAnchor, advancedCursor)
                    : advancedText);
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_X && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(advancedSelectionAnchor, advancedCursor)
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
        if (keyCode == InputConstants.KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private boolean handleAdvancedActionKey(int keyCode) {
        if (isCtrlDown() && keyCode == InputConstants.KEY_A) {
            advancedActionSelectionAnchor = 0;
            advancedActionCursor = advancedAction.length();
            return true;
        }
        if (!isShiftDown() && (keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN)) {
            List<String> suggestions = advancedActionSuggestions();
            if (!suggestions.isEmpty()) {
                int dir = keyCode == InputConstants.KEY_UP ? -1 : 1;
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
                ensureSuggestionVisible(suggestions.size(), next);
                return true;
            }
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
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
        if (keyCode == InputConstants.KEY_DELETE) {
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
        if (keyCode == InputConstants.KEY_LEFT) {
            int old = advancedActionCursor;
            advancedActionCursor = Math.max(0, advancedActionCursor - 1);
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            int old = advancedActionCursor;
            advancedActionCursor = Math.min(advancedAction.length(), advancedActionCursor + 1);
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            int old = advancedActionCursor;
            advancedActionCursor = 0;
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            int old = advancedActionCursor;
            advancedActionCursor = advancedAction.length();
            advancedActionSelectionAnchor = updateSelectionAnchor(advancedActionSelectionAnchor, old, advancedActionCursor, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB) {
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
                ensureSuggestionVisible(suggestions.size(), next);
            }
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_V && this.minecraft != null) {
            insertAtAdvancedActionCursor(this.minecraft.keyboardHandler.getClipboard());
            advancedActionSuggestionIndex = -1;
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_C && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(advancedActionSelectionAnchor, advancedActionCursor)
                    ? selectedText(advancedAction, advancedActionSelectionAnchor, advancedActionCursor)
                    : advancedAction);
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_X && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(advancedActionSelectionAnchor, advancedActionCursor)
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
        if (keyCode == InputConstants.KEY_ESCAPE) {
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

        if (isCtrlDown() && keyCode == InputConstants.KEY_A) {
            advancedVisibilityScreenTypeSelectionAnchor = 0;
            advancedVisibilityScreenTypeCursor = value.length();
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
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
        if (keyCode == InputConstants.KEY_DELETE) {
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
        if (keyCode == InputConstants.KEY_LEFT) {
            int old = cursor;
            int next = Math.max(0, cursor - 1);
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            int old = cursor;
            int next = Math.min(value.length(), cursor + 1);
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            int old = cursor;
            advancedVisibilityScreenTypeCursor = 0;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, 0, isShiftDown());
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            int old = cursor;
            int next = value.length();
            advancedVisibilityScreenTypeCursor = next;
            advancedVisibilityScreenTypeSelectionAnchor = updateSelectionAnchor(selectionAnchor, old, next, isShiftDown());
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_V && this.minecraft != null) {
            insertAtAdvancedVisibilityScreenTypeCursor(this.minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_C && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_X && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(selectionAnchor, cursor)
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
        if (keyCode == InputConstants.KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private boolean handleAdvancedColorKey(int keyCode, boolean background) {
        String value = background ? advancedBgColor : advancedBorderColor;
        int cursor = background ? advancedBgCursor : advancedBorderCursor;
        int selectionAnchor = background ? advancedBgSelectionAnchor : advancedBorderSelectionAnchor;

        if (isCtrlDown() && keyCode == InputConstants.KEY_A) {
            if (background) {
                advancedBgSelectionAnchor = 0;
                advancedBgCursor = value.length();
            } else {
                advancedBorderSelectionAnchor = 0;
                advancedBorderCursor = value.length();
            }
            return true;
        }

        if (keyCode == InputConstants.KEY_BACKSPACE) {
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
        if (keyCode == InputConstants.KEY_DELETE) {
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
        if (keyCode == InputConstants.KEY_LEFT) {
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
        if (keyCode == InputConstants.KEY_RIGHT) {
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
        if (keyCode == InputConstants.KEY_HOME) {
            int old = cursor;
            setAdvancedColorState(background, value, 0);
            if (background) {
                advancedBgSelectionAnchor = updateSelectionAnchor(advancedBgSelectionAnchor, old, 0, isShiftDown());
            } else {
                advancedBorderSelectionAnchor = updateSelectionAnchor(advancedBorderSelectionAnchor, old, 0, isShiftDown());
            }
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
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
        if (isCtrlDown() && keyCode == InputConstants.KEY_V && this.minecraft != null) {
            if (background) {
                insertAtAdvancedBgCursor(this.minecraft.keyboardHandler.getClipboard());
            } else {
                insertAtAdvancedBorderCursor(this.minecraft.keyboardHandler.getClipboard());
            }
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_C && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(selectionAnchor, cursor)
                    ? selectedText(value, selectionAnchor, cursor)
                    : value);
            return true;
        }
        if (isCtrlDown() && keyCode == InputConstants.KEY_X && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(hasSelection(selectionAnchor, cursor)
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
        if (keyCode == InputConstants.KEY_ESCAPE) {
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
        this.advancedText = StringUtils.preserve(this.advancedText);
        this.advancedAction = StringUtils.preserve(this.advancedAction);
        this.advancedBgColor = StringUtils.preserve(this.advancedBgColor);
        this.advancedBorderColor = StringUtils.preserve(this.advancedBorderColor);
        this.advancedVisibilityScreenType = StringUtils.preserve(this.advancedVisibilityScreenType);

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

    private boolean handleColorPickerClick(MouseButtonEvent click) {
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

    private void renderColorPickerPopup(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!colorPickerOpen) {
            return;
        }
        int pickerW = 140;
        int pickerH = 60;
        GuiSystem.drawPanel(context, colorPickerX, colorPickerY, pickerW, pickerH);
        context.text(this.font, colorPickerTitle, colorPickerX + 8, colorPickerY + 6, 0xFFFFFFFF);
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
        return GuiTextEditingUtils.cursorPixel(this.font, x, y, text, cursorIndex);
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

        if (this.minecraft != null && this.minecraft.options != null) {
            for (KeyMapping kb : this.minecraft.options.keyMappings) {
                if (kb == null) {
                    continue;
                }
                String translation = kb.saveString();
                var key = InputConstants.getKey(translation);
                if (key == null) {
                    continue;
                }
                int code = key.getValue();
                if (code < 0) {
                    continue;
                }
                if (key.getType() == InputConstants.Type.MOUSE) {
                    code = MacroWorkbenchCanvasUtils.mouseCell(code);
                }
                gameBindingNames.computeIfAbsent(code, k -> new ArrayList<>())
                        .add(ReflectionUtils.getGameKeybindDisplayName(kb, this.minecraft.options));
            }
        }
    }

    private void rebuildKeyboardGrid() {
        cells.clear();
        buildKeyboardGrid();
        if (cells.isEmpty()) {
            return;
        }
        KbLayout kb = keyboardLayout();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (KeyCell cell : cells) {
            minX = Math.min(minX, cell.x());
            maxX = Math.max(maxX, cell.x() + cell.w());
            maxY = Math.max(maxY, cell.y() + cell.h());
        }
        int dx = Math.max(0, (kb.kbX() + kb.kbW() - maxX - (minX - kb.kbX())) / 2);
        int areaBottom = kb.kbY() + kb.kbH();
        int dy = Math.max(0, (areaBottom - maxY) / 2);
        List<KeyCell> centered = new ArrayList<>(cells.size());
        for (KeyCell cell : cells) {
            centered.add(new KeyCell(cell.keyCode(), cell.label(), cell.x() + dx, cell.y() + dy, cell.w(), cell.h()));
        }
        cells.clear();
        cells.addAll(centered);
    }

    private void buildKeyboardGrid() {

        KbLayout kb = keyboardLayout();
        int availableW = kb.kbW();
        int x0 = kb.kbX();
        int y0 = kb.kbY() + KB_LEGEND_H;
        int availableH = kb.kbH() - KB_LEGEND_H;
        int gap = Math.clamp(availableW / 180, 2, 5);

        int[] rowFn = {
                InputConstants.KEY_ESCAPE,
                InputConstants.KEY_F1, InputConstants.KEY_F2, InputConstants.KEY_F3, InputConstants.KEY_F4,
                InputConstants.KEY_F5, InputConstants.KEY_F6, InputConstants.KEY_F7, InputConstants.KEY_F8,
                InputConstants.KEY_F9, InputConstants.KEY_F10, InputConstants.KEY_F11, InputConstants.KEY_F12
        };
        int[] rowDigits = {
                InputConstants.KEY_GRAVE,
                InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3, InputConstants.KEY_4, InputConstants.KEY_5,
                InputConstants.KEY_6, InputConstants.KEY_7, InputConstants.KEY_8, InputConstants.KEY_9, InputConstants.KEY_0,
                InputConstants.KEY_MINUS, InputConstants.KEY_EQUALS, InputConstants.KEY_BACKSPACE
        };
        int[] rowTab = {
                InputConstants.KEY_TAB,
                InputConstants.KEY_Q, InputConstants.KEY_W, InputConstants.KEY_E, InputConstants.KEY_R, InputConstants.KEY_T,
                InputConstants.KEY_Y, InputConstants.KEY_U, InputConstants.KEY_I, InputConstants.KEY_O, InputConstants.KEY_P,
                InputConstants.KEY_LBRACKET, InputConstants.KEY_RBRACKET, InputConstants.KEY_BACKSLASH
        };
        int[] rowCaps = {
                InputConstants.KEY_CAPSLOCK,
                InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_G,
                InputConstants.KEY_H, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L,
                InputConstants.KEY_SEMICOLON, InputConstants.KEY_APOSTROPHE, InputConstants.KEY_RETURN
        };
        int[] rowShift = {
                InputConstants.KEY_LSHIFT,
                InputConstants.KEY_Z, InputConstants.KEY_X, InputConstants.KEY_C, InputConstants.KEY_V, InputConstants.KEY_B,
                InputConstants.KEY_N, InputConstants.KEY_M, InputConstants.KEY_COMMA, InputConstants.KEY_PERIOD,
                InputConstants.KEY_SLASH, InputConstants.KEY_RSHIFT
        };

        int kw = 52;
        while (kw > 18 && keyboardMainWidth(kw, gap, rowDigits, rowTab, rowCaps, rowShift) > availableW) {
            kw--;
        }
        while (gap > 2 && keyboardMainWidth(kw, gap, rowDigits, rowTab, rowCaps, rowShift) > availableW) {
            gap--;
        }
        int kh = Math.clamp(Math.round(kw * 0.9f), 12, 30);
        int rowGap = Math.max(2, gap);
        // Seven rows must fit the available height.
        while (kh > 11 && (kh + rowGap) * 7 - rowGap > availableH) {
            kh--;
        }
        int rowStep = kh + rowGap;

        addRow(x0, y0, kw, kh, gap, rowFn);
        addRow(x0, y0 + rowStep, kw, kh, gap, rowDigits);
        addRow(x0, y0 + rowStep * 2, kw, kh, gap, rowTab);
        addRow(x0, y0 + rowStep * 3, kw, kh, gap, rowCaps);
        addRow(x0, y0 + rowStep * 4, kw, kh, gap, rowShift);
        addRow(x0, y0 + rowStep * 5, kw, kh, gap,
                InputConstants.KEY_LCONTROL, InputConstants.KEY_LALT, InputConstants.KEY_SPACE,
                InputConstants.KEY_RALT, InputConstants.KEY_RCONTROL,
                InputConstants.KEY_UP, InputConstants.KEY_LEFT, InputConstants.KEY_DOWN, InputConstants.KEY_RIGHT);
        addRow(x0, y0 + rowStep * 6, kw, kh, gap,
                InputConstants.KEY_HOME, InputConstants.KEY_END, InputConstants.KEY_PAGEUP, InputConstants.KEY_PAGEDOWN,
                InputConstants.KEY_INSERT, InputConstants.KEY_DELETE);
        addRow(x0 + 7 * (kw + gap), y0 + rowStep * 6, kw, kh, gap,
                MacroWorkbenchCanvasUtils.mouseCell(InputConstants.MOUSE_BUTTON_LEFT),
                MacroWorkbenchCanvasUtils.mouseCell(InputConstants.MOUSE_BUTTON_RIGHT),
                MacroWorkbenchCanvasUtils.mouseCell(InputConstants.MOUSE_BUTTON_MIDDLE),
                MacroWorkbenchCanvasUtils.mouseCell(InputConstants.MOUSE_BUTTON_4),
                MacroWorkbenchCanvasUtils.mouseCell(InputConstants.MOUSE_BUTTON_5));

        int numpadWidth = rowPixelWidth(kw, gap, InputConstants.KEY_NUMPAD7, InputConstants.KEY_NUMPAD8, InputConstants.KEY_NUMPAD9, InputConstants.KEY_ADD);
        int mainWidth = keyboardMainWidth(kw, gap, rowDigits, rowTab, rowCaps, rowShift);
        boolean placeNumpadRight = availableW >= mainWidth + (gap * 6) + numpadWidth;
        boolean placeNumpadBelow = !placeNumpadRight && rowStep * 12 + 8 <= availableH;
        if (!placeNumpadRight && !placeNumpadBelow) {
            return;
        }
        int numpadX = placeNumpadRight
                ? x0 + availableW - numpadWidth
                : x0 + Math.max(0, (mainWidth - numpadWidth) / 2);
        int numpadY = placeNumpadRight ? (y0 + rowStep * 2) : (y0 + rowStep * 7 + 8);
        addRow(numpadX, numpadY, kw, kh, gap, org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_DIVIDE, InputConstants.KEY_MULTIPLY, org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS);
        addRow(numpadX, numpadY + rowStep, kw, kh, gap, InputConstants.KEY_NUMPAD7, InputConstants.KEY_NUMPAD8, InputConstants.KEY_NUMPAD9, InputConstants.KEY_ADD);
        addRow(numpadX, numpadY + rowStep * 2, kw, kh, gap, InputConstants.KEY_NUMPAD4, InputConstants.KEY_NUMPAD5, InputConstants.KEY_NUMPAD6);
        addRow(numpadX, numpadY + rowStep * 3, kw, kh, gap, InputConstants.KEY_NUMPAD1, InputConstants.KEY_NUMPAD2, InputConstants.KEY_NUMPAD3, InputConstants.KEY_NUMPADENTER);
        addRow(numpadX, numpadY + rowStep * 4, kw, kh, gap, InputConstants.KEY_NUMPAD0, org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_PERIOD);
    }

    private static final int KB_LEGEND_H = 14;
    private static final int KB_ROW_H = 14;
    private static final int KEY_MACRO_BG = 0xFF1B3326;
    private static final int KEY_MACRO_BORDER = 0xFF3FB950;
    private static final int KEY_GAME_BG = 0xFF1A2742;
    private static final int KEY_GAME_BORDER = 0xFF4C7BD9;
    private static final int KEY_GAME_TEXT = 0xFF9CB8F0;

    private KbLayout keyboardLayout() {
        int left = contentLeft();
        int top = contentTop();
        int right = contentRight();
        int bottom = contentBottom();
        int pad = Theme.GAP_M;
        int h = Theme.CONTROL_H;
        int gap = Theme.GAP_S;
        boolean wide = contentWidth() >= 480;

        if (wide) {
            int cardW = Math.clamp(Math.round(contentWidth() * 0.34f), 190, 280);
            int cardX = right - cardW;
            int innerX = cardX + pad;
            int innerW = cardW - pad * 2;
            int buttonsY = bottom - pad - h;
            int delayY = buttonsY - gap - h - 10;
            int commandsY = delayY - 10 - h - gap;
            int nameY = commandsY - 10 - h - gap;
            int third = (innerW - gap * 2) / 3;
            int delayW = Math.max(48, innerW / 3);
            int gameW = Math.min(96, innerW / 2);
            return new KbLayout(true, left, top, cardX - Theme.GAP_L - left, bottom - top,
                    cardX, top, cardW, bottom - top, top + pad + 18 + 14, nameY - 10 - gap * 2,
                    new UiRect(cardX + cardW - pad - gameW, top + pad, gameW, h),
                    new UiRect(innerX, nameY, innerW, h),
                    new UiRect(innerX, commandsY, innerW, h),
                    new UiRect(innerX + delayW + gap, delayY, innerW - delayW - gap, h),
                    new UiRect(innerX, delayY, delayW, h),
                    new UiRect(innerX + (third + gap) * 2, buttonsY, innerW - (third + gap) * 2, h),
                    new UiRect(innerX, buttonsY, third, h),
                    new UiRect(innerX + third + gap, buttonsY, third, h));
        }

        int cardH = pad + h + gap + h + gap + h + pad - 2;
        int cardY = bottom - cardH;
        int innerX = left + pad;
        int innerW = contentWidth() - pad * 2;
        int row1 = cardY + pad - 1;
        int row2 = row1 + h + gap;
        int row3 = row2 + h + gap;
        int gameW = Math.min(90, innerW / 3);
        int nameW = Math.round(innerW * 0.34f);
        int editW = Math.min(44, innerW / 6);
        int delayW = Math.max(40, innerW / 6);
        int buttonW = (innerW - delayW - gap * 3) / 3;
        return new KbLayout(false, left, top, contentWidth(), cardY - Theme.GAP_M - top,
                left, cardY, contentWidth(), cardH, 0, 0,
                new UiRect(innerX + innerW - gameW, row1, gameW, h),
                new UiRect(innerX, row2, nameW, h),
                new UiRect(innerX + nameW + gap, row2, innerW - nameW - editW - gap * 2, h),
                new UiRect(innerX + innerW - editW, row2, editW, h),
                new UiRect(innerX, row3, delayW, h),
                new UiRect(innerX + delayW + gap + (buttonW + gap) * 2, row3, innerW - delayW - gap - (buttonW + gap) * 2, h),
                new UiRect(innerX + delayW + gap, row3, buttonW, h),
                new UiRect(innerX + delayW + gap + buttonW + gap, row3, buttonW, h));
    }

    private int keyboardMainWidth(int baseWidth, int gap, int[]... rows) {
        int max = 0;
        for (int[] row : rows) {
            max = Math.max(max, rowPixelWidth(baseWidth, gap, row));
        }
        return max;
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
        if (key == InputConstants.KEY_SPACE) {
            return baseWidth * 4;
        }
        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT || key == InputConstants.KEY_NUMPAD0) {
            return baseWidth * 2;
        }
        if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_TAB || key == InputConstants.KEY_CAPSLOCK || key == InputConstants.KEY_RETURN) {
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
            kbNameField.setValue("");
            kbCommandsField.setValue("");
            kbDelayField.setValue("");
            return;
        }
        kbNameField.setValue(StringUtils.safe(macro.name));
        kbCommandsField.setValue(macro.commands == null ? "" : String.join(";", macro.commands));
        kbDelayField.setValue(Integer.toString(Math.max(0, macro.delayTicks)));
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
            if (kbDelayField.getValue() != null && !kbDelayField.getValue().isBlank()) {
                delay = Math.max(0, Integer.parseInt(kbDelayField.getValue().trim()));
            }
        } catch (Exception ignored) {
            // keep previous
        }

        List<String> commands = new ArrayList<>();
        String raw = kbCommandsField.getValue() == null ? "" : kbCommandsField.getValue().trim();
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
                StringUtils.safe(kbNameField.getValue()),
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
        // Macros are keyboard-only; mouse cells only display game bindings.
        if (selectedKey < 0 || MacroWorkbenchCanvasUtils.isMouseCell(selectedKey)) {
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
        persistVanillaCanvasElements();
        if (this.tab == Tab.KEYBOARD) {
            saveKeyboardMacro();
        }
        if (this.tab == Tab.MACROS && this.macrosTabController != null) {
            this.macrosTabController.commitAll();
        }
        if (this.tab == Tab.AUTOMATION && this.automationTabController != null) {
            this.automationTabController.commitAll();
        }

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
        syncVanillaCanvasElementsFromSettings();
        syncCanvasFields();
        syncPresetControls();
    }

    private void syncExternalCanvasElementsFromSources() {
        syncExternalProxy(
                EXTERNAL_SECONDARY_CHAT_ID,
                HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                "Secondary Chat",
                MacroWorkbenchExternalProxies::defaultSecondaryChatCanvas
        );
        syncExternalProxy(
                EXTERNAL_NBT_INSPECTOR_ID,
                HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR,
                "NBT Inspector",
                MacroWorkbenchExternalProxies::defaultNbtInspectorCanvas
        );
        syncExternalProxy(
                EXTERNAL_MACRO_KEYBINDS_ID,
                HudCanvasDataHandler.ELEMENT_MACRO_KEYBINDS,
                "Macro Keybinds",
                MacroWorkbenchExternalProxies::defaultMacroKeybindsCanvas
        );
        syncExternalProxy(
                EXTERNAL_PICKUP_NOTIFIER_ID,
                HudCanvasDataHandler.ELEMENT_PICKUP_NOTIFIER,
                "Pick-up Notifier",
                MacroWorkbenchExternalProxies::defaultPickupNotifierCanvas
        );
    }

    private void syncVanillaCanvasElementsFromSettings() {
        HudTweaksSettings.ElementType selectedType = vanillaHudElementType(this.selected);
        this.vanillaCanvasElements.clear();
        this.vanillaCanvasConfigs.clear();
        for (HudTweaksSettings.ElementType type : HudTweaksSettings.ElementType.values()) {
            HudTweaksSettings.ElementConfig copy = copyHudTweaksConfig(HudTweaksSettings.getElement(type));
            this.vanillaCanvasConfigs.put(type, copy);
            this.vanillaCanvasElements.add(buildVanillaHudProxy(type, copy));
        }
        if (selectedType != null) {
            this.selected = findVanillaHudProxy(selectedType);
            this.selectedElementIds.clear();
            if (this.selected != null) {
                this.selectedElementIds.add(this.selected.id);
            }
        }
    }

    private void persistVanillaCanvasElements() {
        if (this.vanillaCanvasElements.isEmpty()) {
            return;
        }

        EnumMap<HudTweaksSettings.ElementType, HudTweaksSettings.ElementConfig> updated =
                new EnumMap<>(HudTweaksSettings.ElementType.class);
        for (HudTweaksSettings.ElementType type : HudTweaksSettings.ElementType.values()) {
            HudTweaksSettings.ElementConfig baseConfig = copyHudTweaksConfig(this.vanillaCanvasConfigs.get(type));
            MacroHudDataHandler.HudElement proxy = findVanillaHudProxy(type);
            if (proxy != null) {
                float scale = vanillaScaleForProxy(type, proxy);
                baseConfig.scale = scale;
                baseConfig.offsetX = Math.clamp(resolveElementX(proxy) - defaultVanillaElementX(type, scale), -500, 500);
                baseConfig.offsetY = Math.clamp(resolveElementY(proxy) - defaultVanillaElementY(type, scale), -500, 500);
            }
            updated.put(type, baseConfig);
        }

        HudTweaksSettings.updateAndSave(() -> {
            for (Map.Entry<HudTweaksSettings.ElementType, HudTweaksSettings.ElementConfig> entry : updated.entrySet()) {
                HudTweaksSettings.get().elements.put(entry.getKey(), copyHudTweaksConfig(entry.getValue()));
            }
        });

        this.vanillaCanvasConfigs.clear();
        this.vanillaCanvasConfigs.putAll(updated);
    }

    private List<MacroHudDataHandler.HudElement> activeCanvasElements() {
        return isVanillaCanvasMode() ? this.vanillaCanvasElements : this.working.elements;
    }

    private boolean isVanillaCanvasMode() {
        return this.canvasMode == CanvasMode.VANILLA;
    }

    private boolean isVanillaHudProxy(MacroHudDataHandler.HudElement element) {
        return vanillaHudElementType(element) != null;
    }

    private MacroHudDataHandler.HudElement findVanillaHudProxy(HudTweaksSettings.ElementType type) {
        String id = vanillaHudProxyId(type);
        for (MacroHudDataHandler.HudElement element : this.vanillaCanvasElements) {
            if (element != null && id.equals(element.id)) {
                return element;
            }
        }
        return null;
    }

    private MacroHudDataHandler.HudElement buildVanillaHudProxy(HudTweaksSettings.ElementType type,
                                                                HudTweaksSettings.ElementConfig config) {
        MacroHudDataHandler.HudElement proxy = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.TEXT);
        proxy.id = vanillaHudProxyId(type);
        proxy.anchor = MacroHudDataHandler.Anchor.TOP_LEFT;
        proxy.horizontalAlign = MacroHudDataHandler.HorizontalAlign.LEFT;
        proxy.verticalAlign = MacroHudDataHandler.VerticalAlign.TOP;
        proxy.drawBackground = true;
        proxy.drawBorder = true;
        applyVanillaProxyStyle(proxy, config);
        proxy.fontScale = 0.9f;
        proxy.lineHeight = 9;
        proxy.zIndex = vanillaHudDescriptor(type).baseZ();
        proxy.width = vanillaScaledWidth(type, config.scale);
        proxy.height = vanillaScaledHeight(type, config.scale);
        proxy.x = defaultVanillaElementX(type, config.scale) + config.offsetX;
        proxy.y = defaultVanillaElementY(type, config.scale) + config.offsetY;
        proxy.label = vanillaPreviewLabel(type);
        proxy.text = vanillaPreviewText(type, config);
        normalizeVanillaProxyForCanvas(type, proxy, config);
        return proxy;
    }

    private void normalizeVanillaProxyForCanvas(HudTweaksSettings.ElementType type,
                                                MacroHudDataHandler.HudElement proxy,
                                                HudTweaksSettings.ElementConfig config) {
        if (type == null || proxy == null || config == null) {
            return;
        }

        int canvasWidth = Math.max(40, this.width);
        int canvasHeight = Math.max(40, canvasHeightBound() - canvasTopBound());
        int maxWidth = Math.max(40, canvasWidth - 16);
        int maxHeight = Math.max(40, canvasHeight - 16);
        int baseWidth = Math.max(1, vanillaBaseWidth(type));
        int baseHeight = Math.max(1, vanillaBaseHeight(type));
        float fitScale = Math.clamp(
                Math.min(maxWidth / (float) baseWidth, maxHeight / (float) baseHeight),
                HudTweaksSettings.MIN_SCALE,
                HudTweaksSettings.MAX_SCALE
        );

        if (proxy.width > maxWidth || proxy.height > maxHeight) {
            float recoveredScale = Math.min(
                    Math.clamp(config.scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE),
                    fitScale
            );
            if (recoveredScale < config.scale) {
                config.scale = recoveredScale;
                proxy.width = vanillaScaledWidth(type, recoveredScale);
                proxy.height = vanillaScaledHeight(type, recoveredScale);
            }
        }

        int screenX = resolveElementX(proxy);
        int screenY = resolveElementY(proxy);
        int clampedX = Math.clamp(screenX, 0, Math.max(0, this.width - proxy.width));
        int clampedY = Math.clamp(screenY, canvasTopBound(), Math.max(canvasTopBound(), canvasBottomBound() - proxy.height));
        if (screenX != clampedX || screenY != clampedY) {
            setElementScreenPosition(proxy, clampedX, clampedY);
            config.offsetX = Math.clamp(resolveElementX(proxy) - defaultVanillaElementX(type, config.scale), -500, 500);
            config.offsetY = Math.clamp(resolveElementY(proxy) - defaultVanillaElementY(type, config.scale), -500, 500);
        }

        proxy.text = vanillaPreviewText(type, config);
    }

    private String vanillaPreviewLabel(HudTweaksSettings.ElementType type) {
        return vanillaHudDescriptor(type).label();
    }

    private String vanillaPreviewText(HudTweaksSettings.ElementType type, HudTweaksSettings.ElementConfig config) {
        return vanillaPreviewLabel(type)
                + "\nScale " + String.format(Locale.ROOT, "%.2f", Math.clamp(config.scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE))
                + "  Opacity " + String.format(Locale.ROOT, "%.2f", Math.clamp(config.opacity, 0.0f, 1.0f))
                + (config.display ? "" : "  Hidden");
    }

    private int vanillaBaseWidth(HudTweaksSettings.ElementType type) {
        return switch (type) {
            case PLAYER_LIST -> currentPlayerListMetrics().width();
            case SCOREBOARD -> currentSidebarMetrics().width();
            default -> vanillaHudDescriptor(type).baseWidth();
        };
    }

    private int vanillaBaseHeight(HudTweaksSettings.ElementType type) {
        return switch (type) {
            case PLAYER_LIST -> currentPlayerListMetrics().height();
            case SCOREBOARD -> currentSidebarMetrics().height();
            default -> vanillaHudDescriptor(type).baseHeight();
        };
    }

    private int vanillaScaledWidth(HudTweaksSettings.ElementType type, float scale) {
        return Math.max(12, Math.round(vanillaBaseWidth(type) * Math.clamp(scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE)));
    }

    private int vanillaScaledHeight(HudTweaksSettings.ElementType type, float scale) {
        return Math.max(12, Math.round(vanillaBaseHeight(type) * Math.clamp(scale, HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE)));
    }

    private int defaultVanillaElementX(HudTweaksSettings.ElementType type, float scale) {
        int width = vanillaScaledWidth(type, scale);
        return switch (type) {
            case ACTION_BAR, BOSS_BAR, CROSSHAIR, HOTBAR_GROUP, PLAYER_LIST, SCREEN_TITLE, TOOLTIP ->
                    type == HudTweaksSettings.ElementType.PLAYER_LIST
                            ? currentPlayerListMetrics(scale).left()
                            : Math.round((this.width - width) / 2.0f);
            case DEBUG_SCREEN -> vanillaScaledOffset(2, scale);
            case SCOREBOARD -> currentSidebarMetrics(scale).left();
            case STATUS_EFFECT -> this.width - width - vanillaScaledOffset(1, scale);
            case SUBTITLES -> this.width - width - vanillaScaledOffset(8, scale);
            case TOAST -> this.width - width;
        };
    }

    private int defaultVanillaElementY(HudTweaksSettings.ElementType type, float scale) {
        int height = vanillaScaledHeight(type, scale);
        int canvasHeight = canvasHeightBound();
        int canvasBottom = canvasBottomBound();
        int canvasCenter = canvasTopBound() + canvasHeight / 2;
        return switch (type) {
            case ACTION_BAR -> canvasBottom - vanillaScaledOffset(72, scale);
            case BOSS_BAR -> vanillaScaledOffset(3, scale);
            case CROSSHAIR -> canvasTopBound() + Math.round((canvasHeight - height) / 2.0f);
            case DEBUG_SCREEN -> vanillaScaledOffset(2, scale);
            case HOTBAR_GROUP -> canvasBottom - height;
            case PLAYER_LIST -> currentPlayerListMetrics(scale).top();
            case SCOREBOARD -> currentSidebarMetrics(scale).top();
            case SCREEN_TITLE -> canvasCenter - vanillaScaledOffset(40, scale);
            case STATUS_EFFECT -> vanillaScaledOffset(1, scale);
            case SUBTITLES -> canvasBottom - height - vanillaScaledOffset(30, scale);
            case TOAST -> vanillaScaledOffset(16, scale);
            case TOOLTIP -> canvasTopBound() + Math.round((canvasHeight - height) / 2.0f) + vanillaScaledOffset(20, scale);
        };
    }

    private float vanillaScaleForProxy(HudTweaksSettings.ElementType type, MacroHudDataHandler.HudElement proxy) {
        float widthScale = Math.max(0.01f, proxy.width / (float) vanillaBaseWidth(type));
        float heightScale = Math.max(0.01f, proxy.height / (float) vanillaBaseHeight(type));
        return Math.clamp(Math.min(widthScale, heightScale), HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE);
    }

    private void applyVanillaProxyResize(MacroHudDataHandler.HudElement proxy, int nextWidth, int nextHeight) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return;
        }
        float widthScale = Math.max(0.01f, nextWidth / (float) vanillaBaseWidth(type));
        float heightScale = Math.max(0.01f, nextHeight / (float) vanillaBaseHeight(type));
        float scale = Math.clamp(Math.min(widthScale, heightScale), HudTweaksSettings.MIN_SCALE, HudTweaksSettings.MAX_SCALE);
        proxy.width = vanillaScaledWidth(type, scale);
        proxy.height = vanillaScaledHeight(type, scale);
        syncVanillaProxyConfigFromProxy(proxy);
        refreshVanillaProxyPreview(proxy);
    }

    private HudTweaksSettings.ElementConfig currentVanillaHudConfig(HudTweaksSettings.ElementType type,
                                                                    float scale,
                                                                    MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementConfig config = copyHudTweaksConfig(this.vanillaCanvasConfigs.get(type));
        config.scale = scale;
        config.offsetX = Math.clamp(resolveElementX(proxy) - defaultVanillaElementX(type, scale), -500, 500);
        config.offsetY = Math.clamp(resolveElementY(proxy) - defaultVanillaElementY(type, scale), -500, 500);
        return config;
    }

    private void syncVanillaProxyConfigFromProxy(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return;
        }
        float scale = vanillaScaleForProxy(type, proxy);
        this.vanillaCanvasConfigs.put(type, currentVanillaHudConfig(type, scale, proxy));
    }

    private void syncVanillaProxyLayoutFromConfig(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return;
        }

        HudTweaksSettings.ElementConfig config = copyHudTweaksConfig(this.vanillaCanvasConfigs.get(type));
        proxy.width = vanillaScaledWidth(type, config.scale);
        proxy.height = vanillaScaledHeight(type, config.scale);

        int targetX = defaultVanillaElementX(type, config.scale) + config.offsetX;
        int targetY = defaultVanillaElementY(type, config.scale) + config.offsetY;
        int clampedX = Math.clamp(targetX, 0, Math.max(0, this.width - proxy.width));
        int clampedY = Math.clamp(targetY, canvasTopBound(), Math.max(canvasTopBound(), canvasBottomBound() - proxy.height));
        setElementScreenPosition(proxy, clampedX, clampedY);

        if (targetX != clampedX || targetY != clampedY) {
            config.offsetX = Math.clamp(clampedX - defaultVanillaElementX(type, config.scale), -500, 500);
            config.offsetY = Math.clamp(clampedY - defaultVanillaElementY(type, config.scale), -500, 500);
            this.vanillaCanvasConfigs.put(type, config);
        }

        proxy.text = vanillaPreviewText(type, config);
    }

    private String vanillaQuickFieldText(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return "";
        }
        HudTweaksSettings.ElementConfig config = currentVanillaHudConfig(type, vanillaScaleForProxy(type, proxy), proxy);
        return vanillaPreviewLabel(type) + "  scale " + String.format(Locale.ROOT, "%.2f", config.scale)
                + "  offset " + config.offsetX + "," + config.offsetY;
    }

    private String vanillaSelectionSummary(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return "Selected: Vanilla HUD";
        }
        HudTweaksSettings.ElementConfig config = currentVanillaHudConfig(type, vanillaScaleForProxy(type, proxy), proxy);
        return "Selected: " + vanillaPreviewLabel(type)
                + "  Scale: " + String.format(Locale.ROOT, "%.2f", config.scale)
                + "  Opacity: " + String.format(Locale.ROOT, "%.2f", config.opacity)
                + "  Offset: " + config.offsetX + "," + config.offsetY;
    }

    private void refreshVanillaProxyPreview(MacroHudDataHandler.HudElement proxy) {
        syncVanillaProxyLayoutFromConfig(proxy);
    }

    private void resetVanillaProxy(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return;
        }
        HudTweaksSettings.ElementConfig config = copyHudTweaksConfig(this.vanillaCanvasConfigs.get(type));
        config.scale = 1.0f;
        config.offsetX = 0;
        config.offsetY = 0;
        this.vanillaCanvasConfigs.put(type, config);
        proxy.width = vanillaScaledWidth(type, config.scale);
        proxy.height = vanillaScaledHeight(type, config.scale);
        proxy.x = defaultVanillaElementX(type, config.scale);
        proxy.y = defaultVanillaElementY(type, config.scale);
        proxy.text = vanillaPreviewText(type, config);
        clampElementToCanvas(proxy);
    }

    private void resetAllVanillaProxies() {
        for (MacroHudDataHandler.HudElement proxy : this.vanillaCanvasElements) {
            resetVanillaProxy(proxy);
        }
    }

    private void toggleVanillaProxyDisplay(MacroHudDataHandler.HudElement proxy) {
        HudTweaksSettings.ElementType type = vanillaHudElementType(proxy);
        if (type == null) {
            return;
        }
        HudTweaksSettings.ElementConfig config = copyHudTweaksConfig(this.vanillaCanvasConfigs.get(type));
        config.display = !config.display;
        this.vanillaCanvasConfigs.put(type, config);
        applyVanillaProxyStyle(proxy, config);
        refreshVanillaProxyPreview(proxy);
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

    private HudCanvasDataHandler.HudCanvasElement getMutableExternalCanvasElement(MacroHudDataHandler.HudElement proxy) {
        if (isSecondaryChatProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT, MacroWorkbenchExternalProxies::defaultSecondaryChatCanvas);
        }
        if (isMacroKeybindProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_MACRO_KEYBINDS, MacroWorkbenchExternalProxies::defaultMacroKeybindsCanvas);
        }
        if (isPickupNotifierProxy(proxy)) {
            return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_PICKUP_NOTIFIER, MacroWorkbenchExternalProxies::defaultPickupNotifierCanvas);
        }
        return HudCanvasDataHandler.getMutableElement(HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR, MacroWorkbenchExternalProxies::defaultNbtInspectorCanvas);
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
        if (isVanillaHudProxy(element)) {
            HudTweaksSettings.ElementType type = vanillaHudElementType(element);
            if (type == null) {
                return;
            }
            float scale = vanillaScaleForProxy(type, element);
            element.width = vanillaScaledWidth(type, scale);
            element.height = vanillaScaledHeight(type, scale);
            int screenX = Math.clamp(resolveElementX(element), 0, Math.max(0, this.width - element.width));
            int screenY = Math.clamp(resolveElementY(element), canvasTopBound(),
                    Math.max(canvasTopBound(), canvasBottomBound() - element.height));
            setElementScreenPosition(element, screenX, screenY);
            syncVanillaProxyConfigFromProxy(element);
            refreshVanillaProxyPreview(element);
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
                || (!isVanillaCanvasMode() && presetNameField != null && presetNameField.isFocused());
    }

    private List<MacroHudDataHandler.HudElement> getSelectedElements() {
        if (selectedElementIds.isEmpty() && selected != null) {
            selectedElementIds.add(selected.id);
        }
        List<MacroHudDataHandler.HudElement> out = new ArrayList<>();
        for (MacroHudDataHandler.HudElement e : activeCanvasElements()) {
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
        for (MacroHudDataHandler.HudElement e : activeCanvasElements()) {
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
            refreshVanillaProxyPreview(e);
        }
        syncCanvasFields();
    }

    private void copySelectedElements() {
        if (isVanillaCanvasMode()) {
            return;
        }
        elementClipboard.clear();
        for (MacroHudDataHandler.HudElement e : getSelectedElements()) {
            elementClipboard.add(HudElementUtils.cloneElement(e));
        }
    }

    private void pasteElementsFromClipboard() {
        if (isVanillaCanvasMode()) {
            return;
        }
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
        if (selected == null || isVanillaCanvasMode()) {
            return;
        }
        clipboardWidth = Math.max(1, selected.width);
        clipboardHeight = Math.max(1, selected.height);
    }

    private void pasteDimensionsToSelection() {
        if (clipboardWidth <= 0 || clipboardHeight <= 0 || isVanillaCanvasMode()) {
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
        if (this.minecraft == null) {
            return false;
        }
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    private boolean isCtrlDown() {
        if (this.minecraft == null) {
            return false;
        }
        var window = this.minecraft.getWindow();
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
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
            String out = MacroPlaceholders.expandForCanvas(this.minecraft, raw);
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
        if (this.minecraft == null || activeDragSelectionField == ModalDragSelectionField.NONE) {
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

        long window = this.minecraft.getWindow().handle();
        if (!this.leftMouseDown) {
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

    private void ensureSuggestionVisible(int totalSuggestions, int selectedIndex) {
        CustomWidgetAdvancedLayout layout = customWidgetAdvancedLayout(modalX(), modalY());
        UiRect suggestionArea = layout.suggestionArea();
        int rowH = 10;
        int visible = Math.clamp(Math.max(1, suggestionArea.height() / rowH), 1, Math.max(1, totalSuggestions));
        if (selectedIndex < advancedActionSuggestionScroll) {
            advancedActionSuggestionScroll = selectedIndex;
            return;
        }
        int lastVisible = advancedActionSuggestionScroll + visible - 1;
        if (selectedIndex > lastVisible) {
            advancedActionSuggestionScroll = selectedIndex - visible + 1;
        }
    }

    private void drawSingleLineSelection(GuiGraphicsExtractor context, int textX, int textY, String text, int anchor, int cursor) {
        GuiTextEditingUtils.drawSingleLineSelection(context, this.font, textX, textY, text, anchor, cursor);
    }

    private void drawMultilineSelection(GuiGraphicsExtractor context, int textX, int textY, int maxY, String text, int anchor, int cursor, int lineHeight) {
        GuiTextEditingUtils.drawMultilineSelection(context, this.font, textX, textY, maxY, text, anchor, cursor, lineHeight);
    }

    private void drawMultilineSelectionWithScroll(GuiGraphicsExtractor context, int textX, int textY, int maxY,
                                                  String text, int anchor, int cursor, int lineHeight, int scrollLines) {
        GuiTextEditingUtils.drawMultilineSelectionWithScroll(context, this.font, textX, textY, maxY, text, anchor, cursor, lineHeight, scrollLines);
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
            width += this.font.width(run.text());
        }
        return width;
    }

    private void drawStyledTextLine(GuiGraphicsExtractor context, String raw, int x, int y, int defaultColor, float scale) {
        List<MacroWorkbenchTextStyling.TextRun> runs = MacroWorkbenchTextStyling.parseColorRuns(raw, defaultColor);
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        int logicalX = Math.round(x / scale);
        int logicalY = Math.round(y / scale);
        int cursor = logicalX;
        for (MacroWorkbenchTextStyling.TextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            context.text(this.font, run.text(), cursor, logicalY, run.color());
            cursor += this.font.width(run.text());
        }

        context.pose().popMatrix();
    }

    private boolean shouldHighlightButtonScript() {
        return selected != null
                && selected.type == MacroHudDataHandler.ElementType.BUTTON
                && selected.buttonExecutionMode != MacroHudDataHandler.ButtonExecutionMode.COMMAND;
    }

    private void drawButtonScriptHighlightedLine(GuiGraphicsExtractor context, String line, int x, int y) {
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
            context.text(this.font, part, drawX, y, span.color(), false);
            drawX += this.font.width(part);
        }
    }

    private int cursorIndexFromPoint(String text, int localX, int localY, int lineHeight) {
        return GuiTextEditingUtils.cursorIndexFromPoint(this.font, text, localX, localY, lineHeight);
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

    private static void drawCanvasElementBorder(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement element,
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

    Font workbenchTextRenderer() {
        return this.font;
    }

    void openWorkbenchTab(Tab next) {
        setTab(next);
    }

    String currentMacroSelectionId() {
        return this.selectedMacroId;
    }

    void updateKeyboardMacroSelection(String macroId, int keyCode) {
        this.selectedMacroId = macroId;
        this.selectedKey = keyCode;
        syncKeyboardFields();
    }

    void onMacrosChanged() {
        rebuildBindingMaps();
        if (this.selectedMacroId != null && MacroDataHandler.getMacro(this.selectedMacroId) == null) {
            this.selectedMacroId = null;
        }
        syncKeyboardFields();
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
        if ("block".equalsIgnoreCase(kind) && BuiltInRegistries.BLOCK.containsKey(id)) {
            return new ItemStack(BuiltInRegistries.BLOCK.getValue(id).asItem());
        }
        if ("entity".equalsIgnoreCase(kind)) {
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
            }
            Identifier eggId = Identifier.tryParse(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
            if (eggId != null && BuiltInRegistries.ITEM.containsKey(eggId)) {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(eggId));
            }
            return new ItemStack(Items.BARRIER);
        }
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        }
        return new ItemStack(Items.STONE);
    }

    private void drawCanvasTriangleShape(GuiGraphicsExtractor context, int x, int y, int w, int h,
                                         int fillColor, int borderColor, boolean filled,
                                         boolean drawBorder, int thickness) {
        int apexX = x + w / 2;
        int apexY = y;
        int leftX = x;
        int rightX = x + w - 1;
        int baseY = y + h - 1;
        for (int row = 0; row < h; row++) {
            float t = h == 1 ? 1.0f : (row / (float) (h - 1));
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

    private void drawCanvasDiamondShape(GuiGraphicsExtractor context, int x, int y, int w, int h,
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
        if (this.minecraft == null || token == null || token.isBlank()) {
            return null;
        }
        String expanded = MacroPlaceholders.expandForCanvas(this.minecraft, "{" + token + "}");
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
        int modalW = modalWidth();
        int defaultX = this.width / 2 - (modalW / 2);
        int x = advancedModalPosX == null ? defaultX : advancedModalPosX;
        return Math.clamp(x, 0, Math.max(0, this.width - modalW));
    }

    private int modalY() {
        int modalH = modalHeight();
        int defaultY = this.height / 2 - (modalH / 2);
        int y = advancedModalPosY == null ? defaultY : advancedModalPosY;
        return Math.clamp(y, 0, Math.max(0, this.height - modalH));
    }

    /** Size the editor modals are designed for; smaller windows render them scaled down to this size. */
    private static final int MODAL_MIN_W = 440;
    private static final int MODAL_MIN_H = 316;

    /** Scale applied to the editor modals so they keep their designed layout on small windows. */
    private float modalSpaceScale() {
        if (!(advancedOpen || kbCommandsModalOpen)) {
            return 1f;
        }
        float scale = Math.min((this.width - 16) / (float) MODAL_MIN_W, (this.height - 16) / (float) MODAL_MIN_H);
        return Math.clamp(scale, 0.5f, 1f);
    }

    /**
     * Runs {@code action} with this screen's width/height set to the scaled "modal space", so the modal layout
     * code (which reads width/height) computes the same geometry used for rendering and hit-testing.
     */
    private <T> T inModalSpace(java.util.function.Supplier<T> action) {
        float scale = modalSpaceScale();
        if (scale >= 1f) {
            return action.get();
        }
        int realWidth = this.width;
        int realHeight = this.height;
        this.width = Math.round(realWidth / scale);
        this.height = Math.round(realHeight / scale);
        try {
            return action.get();
        } finally {
            this.width = realWidth;
            this.height = realHeight;
        }
    }

    private void renderModals(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (advancedOpen && advancedModalDragging && this.minecraft != null) {
            long window = this.minecraft.getWindow().handle();
            if (this.leftMouseDown) {
                this.advancedModalPosX = mouseX - advancedModalDragOffsetX;
                this.advancedModalPosY = mouseY - advancedModalDragOffsetY;
            } else {
                this.advancedModalDragging = false;
            }
        }
        if (colorPickerOpen && colorPickerDragging && this.minecraft != null) {
            long window = this.minecraft.getWindow().handle();
            if (this.leftMouseDown) {
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
        if (advancedOpen && colorPickerOpen) {
            renderColorPickerPopup(context, mouseX, mouseY);
        }
    }

    private float modalScale() {
        int maxWidth = Math.max(1, this.width - 24);
        int maxHeight = Math.max(1, this.height - 24);
        float widthScale = maxWidth / (float) MODAL_W;
        float heightScale = maxHeight / (float) MODAL_H;
        return Math.clamp(Math.min(1.0f, Math.min(widthScale, heightScale)), 0.65f, 1.0f);
    }

    private int modalWidth() {
        int maxWidth = Math.max(1, this.width - 24);
        int minWidth = Math.min(420, maxWidth);
        return Math.clamp(Math.round(MODAL_W * modalScale()), minWidth, maxWidth);
    }

    private int modalHeight() {
        int maxHeight = Math.max(1, this.height - 24);
        int minHeight = Math.min(300, maxHeight);
        return Math.clamp(Math.round(MODAL_H * modalScale()), minHeight, maxHeight);
    }

    private void drawModalButton(GuiGraphicsExtractor context, int x, int y, int w, int h, String label, boolean hovered) {
        GuiSystem.drawButton(context, this.font, x, y, w, h, label, hovered, true);
    }

    private void drawModalButton(GuiGraphicsExtractor context, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
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
    public void onClose() {
        closeAdvancedModal();
        persistGridPrefs();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

