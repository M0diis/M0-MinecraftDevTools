package me.m0dii.modules.macros.gui;

import me.m0dii.gui.GuiSystem;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
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
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class MacroWorkbenchV2Screen extends Screen {

    public enum Tab {
        CANVAS,
        KEYBOARD,
        PLACEHOLDERS
    }

    private static final int TOP_BAR_H = 54;
    private static final int BOTTOM_BAR_H = 44;
    private static final int CANVAS_CONTENT_TOP = 0;
    private static final int MODAL_W = 468;
    private static final int MODAL_H = 268;
    private static final String EXTERNAL_NBT_INSPECTOR_ID = "__ext_nbt_inspector";
    private static final String EXTERNAL_SECONDARY_CHAT_ID = "__ext_secondary_chat";
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static boolean GRID_ENABLED_PREF = false;
    private static int GRID_ROWS_PREF = 12;
    private static int GRID_COLS_PREF = 16;
    private static final int[] STYLE_COLOR_PALETTE = {
            0xAA101010, 0xAA1F1F1F, 0xAA2D1F0E, 0xAA0E2D1F, 0xAA1F0E2D,
            0xCC2A2A2A, 0xCC4A2A2A, 0xCC2A4A2A, 0xCC2A2A4A, 0xCC808080,
            0xFFFFFFFF, 0xFFFFAA00, 0xFFFF5555, 0xFF55FF55, 0xFF55FFFF, 0xFF5555FF
    };

    private final Screen parent;
    private Tab tab;

    private MacroHudDataHandler.HudConfig working;
    private MacroHudDataHandler.HudElement selected;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private TextFieldWidget quickField;
    private TextFieldWidget macroField;
    private TextFieldWidget actionField;
    private ButtonWidget backgroundToggle;
    private ButtonWidget borderToggle;
    private ButtonWidget editButton;
    private ButtonWidget gridToggleButton;
    private ButtonWidget gridRowsMinusButton;
    private ButtonWidget gridRowsPlusButton;
    private ButtonWidget gridColsMinusButton;
    private ButtonWidget gridColsPlusButton;

    private final List<ClickableWidget> canvasWidgets = new ArrayList<>();
    private final List<ClickableWidget> keyboardWidgets = new ArrayList<>();

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
    private int advancedBgCursor = 0;
    private int advancedBorderCursor = 0;

    private int placeholderScroll = 0;
    private boolean gridEnabled = GRID_ENABLED_PREF;
    private int gridRows = GRID_ROWS_PREF;
    private int gridCols = GRID_COLS_PREF;
    private int gridOverlayTicks = 0;
    private boolean canvasChromeVisible = true;

    private boolean kbCommandsModalOpen = false;
    private boolean kbCommandsFocused = false;
    private String kbCommandsText = "";
    private int kbCommandsCursor = 0;

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

    private record KeyCell(int keyCode, String label, int x, int y, int w, int h) {
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

        addDrawableChild(ButtonWidget.builder(Text.literal("Canvas"), b -> setTab(Tab.CANVAS)).dimensions(8, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Keyboard"), b -> setTab(Tab.KEYBOARD)).dimensions(82, 8, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Placeholders"), b -> setTab(Tab.PLACEHOLDERS)).dimensions(166, 8, 102, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAll()).dimensions(this.width - 152, 8, 66, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            saveAll();
            close();
        }).dimensions(this.width - 82, 8, 66, 20).build());

        ButtonWidget addButton = ButtonWidget.builder(Text.literal("Add Button"), b -> {
            MacroHudDataHandler.HudElement e = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.BUTTON);
            e.x = 20;
            e.y = 20;
            e.label = "Macro";
            this.working.elements.add(e);
            this.selected = e;
            syncCanvasFields();
        }).dimensions(8, 32, 82, 18).build();

        ButtonWidget addText = ButtonWidget.builder(Text.literal("Add Text"), b -> {
            MacroHudDataHandler.HudElement e = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.TEXT);
            e.x = 20;
            e.y = 50;
            e.text = "Text";
            this.working.elements.add(e);
            this.selected = e;
            syncCanvasFields();
        }).dimensions(94, 32, 72, 18).build();

        ButtonWidget addMacroList = ButtonWidget.builder(Text.literal("Add Macro List"), b -> {
            MacroHudDataHandler.HudElement e = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.MACRO_KEYBINDS);
            e.x = 20;
            e.y = 80;
            this.working.elements.add(e);
            this.selected = e;
            syncCanvasFields();
        }).dimensions(170, 32, 92, 18).build();

        ButtonWidget delete = ButtonWidget.builder(Text.literal("Delete"), b -> {
            if (selected != null) {
                this.working.elements.removeIf(e -> e.id.equals(selected.id));
                this.selected = null;
                syncCanvasFields();
            }
        }).dimensions(266, 32, 58, 18).build();

        this.gridToggleButton = ButtonWidget.builder(Text.literal("Grid: OFF"), b -> {
            gridEnabled = !gridEnabled;
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(328, 32, 74, 18).build();

        this.gridRowsMinusButton = ButtonWidget.builder(Text.literal("R-"), b -> {
            gridRows = Math.max(2, gridRows - 1);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(406, 32, 24, 18).build();
        this.gridRowsPlusButton = ButtonWidget.builder(Text.literal("R+"), b -> {
            gridRows = Math.min(80, gridRows + 1);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(434, 32, 24, 18).build();
        this.gridColsMinusButton = ButtonWidget.builder(Text.literal("C-"), b -> {
            gridCols = Math.max(2, gridCols - 1);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(462, 32, 24, 18).build();
        this.gridColsPlusButton = ButtonWidget.builder(Text.literal("C+"), b -> {
            gridCols = Math.min(80, gridCols + 1);
            gridOverlayTicks = 120;
            persistGridPrefs();
            syncGridButtons();
        }).dimensions(490, 32, 24, 18).build();

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
        }).dimensions(this.width - 238, this.height - 38, 74, 18).build();

        this.borderToggle = ButtonWidget.builder(Text.literal("Border: OFF"), b -> {
            if (selected != null) {
                selected.drawBorder = !selected.drawBorder;
                syncStyleButtons();
            }
        }).dimensions(this.width - 160, this.height - 38, 78, 18).build();

        this.editButton = ButtonWidget.builder(Text.literal("Edit"), b -> openAdvancedModal())
                .dimensions(this.width - 80, this.height - 38, 72, 18).build();

        this.canvasWidgets.add(addButton);
        this.canvasWidgets.add(addText);
        this.canvasWidgets.add(addMacroList);
        this.canvasWidgets.add(delete);
        this.canvasWidgets.add(gridToggleButton);
        this.canvasWidgets.add(gridRowsMinusButton);
        this.canvasWidgets.add(gridRowsPlusButton);
        this.canvasWidgets.add(gridColsMinusButton);
        this.canvasWidgets.add(gridColsPlusButton);
        this.canvasWidgets.add(quickField);
        this.canvasWidgets.add(macroField);
        this.canvasWidgets.add(actionField);
        this.canvasWidgets.add(backgroundToggle);
        this.canvasWidgets.add(borderToggle);
        this.canvasWidgets.add(editButton);

        addDrawableChild(addButton);
        addDrawableChild(addText);
        addDrawableChild(addMacroList);
        addDrawableChild(delete);
        addDrawableChild(gridToggleButton);
        addDrawableChild(gridRowsMinusButton);
        addDrawableChild(gridRowsPlusButton);
        addDrawableChild(gridColsMinusButton);
        addDrawableChild(gridColsPlusButton);
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

        syncCanvasFields();
        syncGridButtons();
        syncKeyboardFields();
        setTab(this.tab);
    }

    private void setTab(Tab next) {
        this.tab = next;
        boolean canvas = next == Tab.CANVAS;
        boolean keyboard = next == Tab.KEYBOARD;
        boolean placeholders = next == Tab.PLACEHOLDERS;

        for (ClickableWidget w : canvasWidgets) {
            w.visible = canvas;
            w.active = canvas;
        }
        for (ClickableWidget w : keyboardWidgets) {
            w.visible = keyboard;
            w.active = keyboard;
        }

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
        } else {
            renderPlaceholdersTab(context);
        }

        // Draw normal widgets first.
        super.render(context, mouseX, mouseY, delta);

        if (advancedOpen) {
            renderAdvancedModal(context, mouseX, mouseY);
        }
        if (kbCommandsModalOpen) {
            renderKeyboardCommandsModal(context, mouseX, mouseY);
        }
    }

    private void renderCanvasTab(DrawContext context, int mouseX, int mouseY) {
        applyQuickEdit();
        updateDragging(mouseX, mouseY);

        context.drawTextWithShadow(this.textRenderer, "Mouse wheel = width, Shift + wheel = height", 8, TOP_BAR_H + 4, 0xFFE0E0E0);
        context.drawTextWithShadow(this.textRenderer,
                "F1 toggles editor chrome: " + (canvasChromeVisible ? "ON" : "OFF"),
                8, TOP_BAR_H + 16, 0xFF9FCFCF);
        int canvasHeight = Math.max(1, this.height - BOTTOM_BAR_H - CANVAS_CONTENT_TOP);
        int cellW = Math.max(1, Math.round(this.width / (float) Math.max(1, gridCols)));
        int cellH = Math.max(1, Math.round(canvasHeight / (float) Math.max(1, gridRows)));
        context.drawTextWithShadow(this.textRenderer,
                "Grid " + (gridEnabled ? "ON" : "OFF") + "  " + gridCols + "x" + gridRows + "  Cell: " + cellW + "x" + cellH,
                520, 36, 0xFFBFCFCF);

        if (gridEnabled || gridOverlayTicks > 0) {
            drawGridOverlay(context);
            if (gridOverlayTicks > 0) {
                gridOverlayTicks--;
            }
        }

        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (element.visible) {
                drawCanvasElement(context, element);
            }
        }

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
        List<String> docs = placeholderDocs();
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

        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() != 0) {
            return false;
        }

        if (this.tab == Tab.CANVAS) {
            return onCanvasClick(click.x(), click.y());
        }
        if (this.tab == Tab.KEYBOARD) {
            return onKeyboardClick(click.x(), click.y());
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (advancedOpen) {
            return true;
        }

        if (this.tab == Tab.PLACEHOLDERS) {
            placeholderScroll -= verticalAmount > 0 ? 3 : -3;
            return true;
        }

        if (this.tab == Tab.CANVAS && selected != null) {
            int delta = verticalAmount > 0 ? 4 : -4;
            boolean shiftDown = isShiftDown();
            if (shiftDown) {
                selected.height = Math.clamp(selected.height + delta, 12, 128);
            } else {
                selected.width = Math.clamp(selected.width + delta, 40, 380);
            }
            return true;
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
            return false;
        }

        for (int i = this.working.elements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement e = this.working.elements.get(i);
            int ex = resolveElementX(e);
            int ey = resolveElementY(e);
            if (contains(ex, ey, e, mouseX, mouseY)) {
                this.selected = e;
                this.dragOffsetX = (int) mouseX - ex;
                this.dragOffsetY = (int) mouseY - ey;
                this.dragging = true;
                syncCanvasFields();
                return true;
            }
        }

        this.selected = null;
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
        if (click.button() != 0) {
            return true;
        }

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

        if (isSecondaryChatProxy(selected)) {
            int regexX = boxX + 12;
            int regexY = boxY + 92;
            int regexW = MODAL_W - 24;
            int regexH = 86;
            int outgoingX = boxX + 12;
            int outgoingY = boxY + 182;
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
                advancedSecondaryScale = Math.max(0.1, advancedSecondaryScale - 0.05);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 356, boxY + 68, 48, 18)) {
                advancedSecondaryScale = Math.min(3.0, advancedSecondaryScale + 0.05);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 408, boxY + 68, 48, 18)) {
                advancedSecondaryLineHeight = Math.max(1, advancedSecondaryLineHeight - 1);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 408, boxY + 92, 48, 18)) {
                advancedSecondaryLineHeight = Math.min(30, advancedSecondaryLineHeight + 1);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 206, 48, 18)) {
                advancedSecondaryFadeDurationMs = Math.max(1000, advancedSecondaryFadeDurationMs - 1000);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 64, boxY + 206, 48, 18)) {
                advancedSecondaryFadeDurationMs = Math.min(120000, advancedSecondaryFadeDurationMs + 1000);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 116, boxY + 206, 48, 18)) {
                advancedSecondaryMinAlpha = Math.max(0, advancedSecondaryMinAlpha - 5);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 168, boxY + 206, 48, 18)) {
                advancedSecondaryMinAlpha = Math.min(255, advancedSecondaryMinAlpha + 5);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 220, boxY + 206, 48, 18)) {
                advancedSecondaryMaxLines = Math.max(10, advancedSecondaryMaxLines - 10);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 272, boxY + 206, 48, 18)) {
                advancedSecondaryMaxLines = Math.min(500, advancedSecondaryMaxLines + 10);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 324, boxY + 206, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, false);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 206, 64, 18)) {
                selected.backgroundColor = cycleStyleColor(selected.backgroundColor, true);
                advancedBgColor = formatColor(selected.backgroundColor);
                advancedBgCursor = advancedBgColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 324, boxY + 230, 64, 18)) {
                selected.textColor = cycleStyleColor(selected.textColor, false);
                advancedBorderColor = formatColor(selected.textColor);
                advancedBorderCursor = advancedBorderColor.length();
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 392, boxY + 230, 64, 18)) {
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

            advancedBgColorFocused = containsBox(click.x(), click.y(), boxX + 324, boxY + 230, 64, 18);
            if (advancedBgColorFocused) {
                advancedBorderColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                int localX = (int) (click.x() - (boxX + 328));
                advancedBgCursor = cursorIndexFromPoint(advancedBgColor, localX, 0, 9);
                return true;
            }

            advancedBorderColorFocused = containsBox(click.x(), click.y(), boxX + 392, boxY + 230, 64, 18);
            if (advancedBorderColorFocused) {
                advancedBgColorFocused = false;
                advancedTextFocused = false;
                advancedActionFocused = false;
                int localX = (int) (click.x() - (boxX + 396));
                advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, localX, 0, 9);
                return true;
            }

            advancedActionFocused = containsBox(click.x(), click.y(), outgoingX, outgoingY, outgoingW, outgoingH);
            if (advancedActionFocused) {
                advancedTextFocused = false;
                advancedBgColorFocused = false;
                advancedBorderColorFocused = false;
                int localX = (int) (click.x() - (outgoingX + 4));
                advancedActionCursor = cursorIndexFromPoint(advancedAction, localX, 0, 9);
                return true;
            }

            advancedTextFocused = containsBox(click.x(), click.y(), regexX, regexY, regexW, regexH);
            if (advancedTextFocused) {
                advancedActionFocused = false;
                advancedBgColorFocused = false;
                advancedBorderColorFocused = false;
                advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (regexX + 4)), (int) (click.y() - (regexY + 4)), 9);
            }
            return true;
        }

        if (isNbtInspectorProxy(selected)) {
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 52, 64, 18)) {
                selected.fontScale = Math.clamp(selected.fontScale - 0.1f, 0.5f, 4.0f);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 80, boxY + 52, 64, 18)) {
                selected.fontScale = Math.clamp(selected.fontScale + 0.1f, 0.5f, 4.0f);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 12, boxY + 76, 64, 18)) {
                selected.lineHeight = Math.clamp(selected.lineHeight - 1, 6, 24);
                return true;
            }
            if (containsBox(click.x(), click.y(), boxX + 80, boxY + 76, 64, 18)) {
                selected.lineHeight = Math.clamp(selected.lineHeight + 1, 6, 24);
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
                selected.horizontalAlign = switch (selected.horizontalAlign) {
                    case LEFT -> MacroHudDataHandler.HorizontalAlign.CENTER;
                    case CENTER -> MacroHudDataHandler.HorizontalAlign.RIGHT;
                    case RIGHT -> MacroHudDataHandler.HorizontalAlign.LEFT;
                };
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 238, boxY + 172, 92, 18)) {
            if (selected != null) {
                selected.verticalAlign = switch (selected.verticalAlign) {
                    case TOP -> MacroHudDataHandler.VerticalAlign.CENTER;
                    case CENTER -> MacroHudDataHandler.VerticalAlign.BOTTOM;
                    case BOTTOM -> MacroHudDataHandler.VerticalAlign.TOP;
                };
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 334, boxY + 172, 122, 18)) {
            if (selected != null) {
                selected.anchor = switch (selected.anchor) {
                    case TOP_LEFT -> MacroHudDataHandler.Anchor.TOP_RIGHT;
                    case TOP_RIGHT -> MacroHudDataHandler.Anchor.BOTTOM_RIGHT;
                    case BOTTOM_RIGHT -> MacroHudDataHandler.Anchor.BOTTOM_LEFT;
                    case BOTTOM_LEFT -> MacroHudDataHandler.Anchor.CENTER;
                    case CENTER -> MacroHudDataHandler.Anchor.TOP_LEFT;
                };
                setElementScreenPosition(selected, resolveElementX(selected), resolveElementY(selected));
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
                selected.lineHeight = Math.clamp(selected.lineHeight - 1, 6, 24);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 56, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.lineHeight = Math.clamp(selected.lineHeight + 1, 6, 24);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 104, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.fontScale = Math.clamp(selected.fontScale - 0.1f, 0.5f, 4.0f);
            }
            return true;
        }
        if (containsBox(click.x(), click.y(), boxX + 148, boxY + 196, 40, 18)) {
            if (selected != null) {
                selected.fontScale = Math.clamp(selected.fontScale + 0.1f, 0.5f, 4.0f);
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
            int localX = (int) (click.x() - (bgFieldX + 4));
            advancedBgCursor = cursorIndexFromPoint(advancedBgColor, localX, 0, 9);
            return true;
        }

        advancedBorderColorFocused = containsBox(click.x(), click.y(), borderFieldX, borderFieldY, borderFieldW, borderFieldH);
        if (advancedBorderColorFocused) {
            advancedTextFocused = false;
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            int localX = (int) (click.x() - (borderFieldX + 4));
            advancedBorderCursor = cursorIndexFromPoint(advancedBorderColor, localX, 0, 9);
            return true;
        }

        advancedActionFocused = containsBox(click.x(), click.y(), actionX, actionY, actionW, actionH);
        if (advancedActionFocused) {
            advancedTextFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            int localX = (int) (click.x() - (actionX + 4));
            advancedActionCursor = cursorIndexFromPoint(advancedAction, localX, 0, 9);
            return true;
        }

        advancedTextFocused = containsBox(click.x(), click.y(), textX, textY, textW, textH);
        if (advancedTextFocused) {
            advancedActionFocused = false;
            advancedBgColorFocused = false;
            advancedBorderColorFocused = false;
            advancedCursor = cursorIndexFromPoint(advancedText, (int) (click.x() - (textX + 4)), (int) (click.y() - (textY + 4)), 9);
        }
        return true;
    }

    private void updateDragging(int mouseX, int mouseY) {
        if (!dragging || selected == null || this.client == null) {
            return;
        }

        long window = this.client.getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            dragging = false;
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
            selected.width = snapped[2];
            selected.height = snapped[3];
        }

        setElementScreenPosition(selected, screenX, screenY);
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
        int snappedW = Math.clamp((int) Math.round(cellW), 40, 2000);
        int snappedH = Math.clamp((int) Math.round(cellH), 12, 1200);

        snappedX = Math.clamp(snappedX, 0, Math.max(0, canvasW - snappedW));
        snappedY = Math.clamp(snappedY, 0, Math.max(0, canvasH - snappedH));
        return new int[]{snappedX, snappedY + CANVAS_CONTENT_TOP, snappedW, snappedH};
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

    private void drawCanvasElement(DrawContext context, MacroHudDataHandler.HudElement element) {
        List<String> lines;
        if (isSecondaryChatProxy(element)) {
            lines = List.of("Secondary Chat", "Unified HUD element");
        } else if (isNbtInspectorProxy(element)) {
            lines = List.of("NBT Inspector", "Unified HUD element");
        } else if (element.type == MacroHudDataHandler.ElementType.MACRO_KEYBINDS) {
            lines = buildMacroKeybindPreviewLines(element);
        } else {
            lines = splitLines(expandForCanvas(element.type == MacroHudDataHandler.ElementType.BUTTON ? element.label : element.text));
        }

        int x1 = resolveElementX(element);
        int y1 = resolveElementY(element);
        int x2 = x1 + element.width;
        int y2 = y1 + element.height;

        if (element.drawBackground) {
            context.fill(x1, y1, x2, y2, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x1, y1, x2, y2, element.borderColor);
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

        if (selected != null && selected.id.equals(element.id)) {
            context.fill(x1, y1, x2, y1 + 1, 0xFFFFFF00);
            context.fill(x1, y2 - 1, x2, y2, 0xFFFFFF00);
            context.fill(x1, y1, x1 + 1, y2, 0xFFFFFF00);
            context.fill(x2 - 1, y1, x2, y2, 0xFFFFFF00);
        }
    }

    private void syncCanvasFields() {
        if (quickField == null || macroField == null || actionField == null) {
            return;
        }

        if (selected == null) {
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
            quickField.setEditable(false);
            quickField.setText(isSecondaryChatProxy(selected)
                    ? "Secondary Chat (edit style/pos/size)"
                    : "NBT Inspector (edit style/pos/size)");
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
        String raw = selected.type == MacroHudDataHandler.ElementType.BUTTON ? selected.label : selected.text;
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

        String raw = selected.type == MacroHudDataHandler.ElementType.BUTTON ? selected.label : selected.text;
        List<String> lines = splitLinesRaw(raw);
        if (lines.isEmpty()) {
            lines = new ArrayList<>(List.of(""));
        }
        lines.set(0, quickField.getText() == null ? "" : quickField.getText());
        String merged = String.join("\n", lines);

        if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            selected.label = merged;
            selected.macroId = macroField.getText() == null ? "" : macroField.getText().trim();
            selected.buttonAction = actionField.getText() == null ? "" : actionField.getText().trim();
        } else {
            selected.text = merged;
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
                || gridColsMinusButton == null || gridColsPlusButton == null) {
            return;
        }
        gridToggleButton.setMessage(Text.literal(gridEnabled ? "Grid: ON" : "Grid: OFF"));
        gridRowsMinusButton.setMessage(Text.literal("R-"));
        gridRowsPlusButton.setMessage(Text.literal("R+"));
        gridColsMinusButton.setMessage(Text.literal("C-"));
        gridColsPlusButton.setMessage(Text.literal("C+"));
    }

    private void refreshCanvasChromeVisibility() {
        boolean visible = this.tab == Tab.CANVAS && canvasChromeVisible;
        for (ClickableWidget w : canvasWidgets) {
            w.visible = visible;
            w.active = visible;
        }
    }

    private void persistGridPrefs() {
        GRID_ENABLED_PREF = gridEnabled;
        GRID_ROWS_PREF = Math.max(2, gridRows);
        GRID_COLS_PREF = Math.max(2, gridCols);
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
        this.advancedText = selected.type == MacroHudDataHandler.ElementType.BUTTON ? safe(selected.label) : safe(selected.text);
        this.advancedAction = selected.type == MacroHudDataHandler.ElementType.BUTTON ? safe(selected.buttonAction) : "";
        this.advancedBgColor = formatColor(selected.backgroundColor);
        this.advancedBorderColor = formatColor(selected.borderColor);
        this.advancedCursor = this.advancedText.length();
        this.advancedActionCursor = this.advancedAction.length();
        this.advancedBgCursor = this.advancedBgColor.length();
        this.advancedBorderCursor = this.advancedBorderColor.length();
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
        context.fill(boxX, boxY, boxX + MODAL_W, boxY + MODAL_H, 0xEE1A1A1A);
        context.fill(boxX, boxY, boxX + MODAL_W, boxY + 1, 0x80FFFFFF);

        if (isSecondaryChatProxy(selected)) {
            renderSecondaryChatAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }
        if (isNbtInspectorProxy(selected)) {
            renderNbtInspectorAdvancedModal(context, mouseX, mouseY, boxX, boxY);
            return;
        }

        context.drawTextWithShadow(this.textRenderer, "Edit", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Multi-line editor (Enter for new line)", boxX + 12, boxY + 22, 0xFFB0B0B0);

        context.fill(textX, textY, textX + textW, textY + textH, advancedTextFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textX, textY, textX + textW, textY + 1, 0x60FFFFFF);

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
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, bgFieldX + 4, bgFieldY + 5, 0xFFEAEAEA);

        int borderInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(borderFieldX, borderFieldY, borderFieldX + borderFieldW, borderFieldY + borderFieldH, borderInputBg);
        context.fill(borderFieldX, borderFieldY, borderFieldX + borderFieldW, borderFieldY + 1, 0x60FFFFFF);
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
        int regexY = boxY + 92;
        int regexW = MODAL_W - 24;
        int regexH = 86;
        context.drawTextWithShadow(this.textRenderer, "Regex List (one pattern per line)", regexX, regexY - 10, 0xFFB8B8B8);
        context.fill(regexX, regexY, regexX + regexW, regexY + regexH, advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(regexX, regexY, regexX + regexW, regexY + 1, 0x60FFFFFF);
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
        int outgoingY = boxY + 182;
        int outgoingW = 244;
        int outgoingH = 18;
        context.drawTextWithShadow(this.textRenderer, "Outgoing Regex", outgoingX, outgoingY - 10, 0xFFB8B8B8);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + outgoingH, advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + 1, 0x60FFFFFF);
        context.drawTextWithShadow(this.textRenderer, advancedAction, outgoingX + 4, outgoingY + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = outgoingX + 4 + this.textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, outgoingY + 4, ax + 1, outgoingY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, boxX + 12, boxY + 206, 48, 18, "FD-", containsBox(mouseX, mouseY, boxX + 12, boxY + 206, 48, 18));
        drawModalButton(context, boxX + 64, boxY + 206, 48, 18, "FD+", containsBox(mouseX, mouseY, boxX + 64, boxY + 206, 48, 18));
        drawModalButton(context, boxX + 116, boxY + 206, 48, 18, "A-", containsBox(mouseX, mouseY, boxX + 116, boxY + 206, 48, 18));
        drawModalButton(context, boxX + 168, boxY + 206, 48, 18, "A+", containsBox(mouseX, mouseY, boxX + 168, boxY + 206, 48, 18));
        drawModalButton(context, boxX + 220, boxY + 206, 48, 18, "L-", containsBox(mouseX, mouseY, boxX + 220, boxY + 206, 48, 18));
        drawModalButton(context, boxX + 272, boxY + 206, 48, 18, "L+", containsBox(mouseX, mouseY, boxX + 272, boxY + 206, 48, 18));
        context.drawTextWithShadow(this.textRenderer,
                "Fade: " + advancedSecondaryFadeDurationMs + "ms  Alpha: " + advancedSecondaryMinAlpha + "  Max: " + advancedSecondaryMaxLines,
                boxX + 12, boxY + 230, 0xFFEAEAEA);

        drawModalButton(context, boxX + 324, boxY + 206, 64, 18, "BG-", containsBox(mouseX, mouseY, boxX + 324, boxY + 206, 64, 18));
        drawModalButton(context, boxX + 392, boxY + 206, 64, 18, "BG+", containsBox(mouseX, mouseY, boxX + 392, boxY + 206, 64, 18));
        context.drawTextWithShadow(this.textRenderer, "BG", boxX + 324, boxY + 224, 0xFFEAEAEA);
        context.drawTextWithShadow(this.textRenderer, "TX", boxX + 392, boxY + 224, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(boxX + 324, boxY + 230, boxX + 388, boxY + 248, bgInputBg);
        context.fill(boxX + 324, boxY + 230, boxX + 388, boxY + 231, 0x60FFFFFF);
        context.drawTextWithShadow(this.textRenderer, advancedBgColor, boxX + 328, boxY + 235, 0xFFEAEAEA);
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(boxX + 392, boxY + 230, boxX + 456, boxY + 248, txInputBg);
        context.fill(boxX + 392, boxY + 230, boxX + 456, boxY + 231, 0x60FFFFFF);
        context.drawTextWithShadow(this.textRenderer, advancedBorderColor, boxX + 396, boxY + 235, 0xFFEAEAEA);

        drawModalButton(context, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18, "Apply",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 134, boxY + MODAL_H - 24, 60, 18));
        drawModalButton(context, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18, "Cancel",
                containsBox(mouseX, mouseY, boxX + MODAL_W - 70, boxY + MODAL_H - 24, 58, 18));

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = boxX + 328 + this.textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, boxY + 234, cx + 1, boxY + 243, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = boxX + 396 + this.textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, boxY + 234, cx + 1, boxY + 243, 0xFFFFFFFF);
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

    private void applyAdvancedAndClose() {
        if (selected != null) {
            applyAdvancedColorFieldsToSelection();
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
        this.advancedTextFocused = false;
        this.advancedActionFocused = false;
        this.advancedBgColorFocused = false;
        this.advancedBorderColorFocused = false;
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
    }

    private void renderKeyboardCommandsModal(DrawContext context, int mouseX, int mouseY) {
        int boxX = this.width / 2 - (MODAL_W / 2);
        int boxY = this.height / 2 - (MODAL_H / 2);
        int textX = boxX + 12;
        int textY = boxY + 34;
        int textW = MODAL_W - 24;
        int textH = 150;

        context.fill(0, 0, this.width, this.height, 0x88000000);
        context.fill(boxX, boxY, boxX + MODAL_W, boxY + MODAL_H, 0xEE1A1A1A);
        context.fill(boxX, boxY, boxX + MODAL_W, boxY + 1, 0x80FFFFFF);

        context.drawTextWithShadow(this.textRenderer, "Edit Commands", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "One command per line", boxX + 12, boxY + 22, 0xFFB0B0B0);

        context.fill(textX, textY, textX + textW, textY + textH, kbCommandsFocused ? 0xFF0F0F0F : 0xFF141414);
        context.fill(textX, textY, textX + textW, textY + 1, 0x60FFFFFF);

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
        }
        return true;
    }

    private boolean handleKeyboardCommandsKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (kbCommandsCursor > 0) {
                kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor - 1) + kbCommandsText.substring(kbCommandsCursor);
                kbCommandsCursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (kbCommandsCursor < kbCommandsText.length()) {
                kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor) + kbCommandsText.substring(kbCommandsCursor + 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            kbCommandsCursor = Math.max(0, kbCommandsCursor - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            kbCommandsCursor = Math.min(kbCommandsText.length(), kbCommandsCursor + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, -1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            kbCommandsCursor = moveCursorVertical(kbCommandsText, kbCommandsCursor, 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            kbCommandsCursor = lineStart(kbCommandsText, kbCommandsCursor);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            kbCommandsCursor = lineEnd(kbCommandsText, kbCommandsCursor);
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
            this.client.keyboard.setClipboard(kbCommandsText);
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
        kbCommandsText = kbCommandsText.substring(0, kbCommandsCursor) + s + kbCommandsText.substring(kbCommandsCursor);
        kbCommandsCursor += s.length();
    }

    private void applyKeyboardCommandsModal() {
        if (selectedMacroId == null) {
            closeKeyboardCommandsModal();
            return;
        }
        MacroDataHandler.MacroEntry existing = MacroDataHandler.getMacro(selectedMacroId);
        if (existing == null) {
            closeKeyboardCommandsModal();
            return;
        }

        List<String> commands = splitLinesRaw(kbCommandsText).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        MacroDataHandler.updateMacro(
                selectedMacroId,
                safe(kbNameField.getText()),
                commands,
                existing.keyCode,
                existing.modifierKey,
                parseDelayOrDefault(existing.delayTicks),
                existing.showInOverlay
        );

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
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (advancedCursor > 0) {
                advancedText = advancedText.substring(0, advancedCursor - 1) + advancedText.substring(advancedCursor);
                advancedCursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (advancedCursor < advancedText.length()) {
                advancedText = advancedText.substring(0, advancedCursor) + advancedText.substring(advancedCursor + 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            advancedCursor = Math.max(0, advancedCursor - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            advancedCursor = Math.min(advancedText.length(), advancedCursor + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            advancedCursor = lineStart(advancedText, advancedCursor);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            advancedCursor = lineEnd(advancedText, advancedCursor);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, -1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            advancedCursor = moveCursorVertical(advancedText, advancedCursor, 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            insertAtCursor("\n");
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtCursor(this.client.keyboard.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(advancedText);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closeAdvancedModal();
            return true;
        }
        return false;
    }

    private boolean handleAdvancedActionKey(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (advancedActionCursor > 0) {
                advancedAction = advancedAction.substring(0, advancedActionCursor - 1) + advancedAction.substring(advancedActionCursor);
                advancedActionCursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (advancedActionCursor < advancedAction.length()) {
                advancedAction = advancedAction.substring(0, advancedActionCursor) + advancedAction.substring(advancedActionCursor + 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            advancedActionCursor = Math.max(0, advancedActionCursor - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            advancedActionCursor = Math.min(advancedAction.length(), advancedActionCursor + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            advancedActionCursor = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            advancedActionCursor = advancedAction.length();
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
            insertAtAdvancedActionCursor(this.client.keyboard.getClipboard());
            return true;
        }
        if (isCtrlDown() && keyCode == GLFW.GLFW_KEY_C && this.client != null) {
            this.client.keyboard.setClipboard(advancedAction);
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

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
            }
            setAdvancedColorState(background, value, cursor);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursor < value.length()) {
                value = value.substring(0, cursor) + value.substring(cursor + 1);
            }
            setAdvancedColorState(background, value, cursor);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            setAdvancedColorState(background, value, Math.max(0, cursor - 1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            setAdvancedColorState(background, value, Math.min(value.length(), cursor + 1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setAdvancedColorState(background, value, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setAdvancedColorState(background, value, value.length());
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
            this.client.keyboard.setClipboard(value);
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
        this.advancedText = advancedText.substring(0, advancedCursor) + s + advancedText.substring(advancedCursor);
        this.advancedCursor += s.length();
    }

    private void insertAtAdvancedActionCursor(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        this.advancedAction = advancedAction.substring(0, advancedActionCursor) + s + advancedAction.substring(advancedActionCursor);
        this.advancedActionCursor += s.length();
    }

    private void insertAtAdvancedBgCursor(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        this.advancedBgColor = advancedBgColor.substring(0, advancedBgCursor) + s + advancedBgColor.substring(advancedBgCursor);
        this.advancedBgCursor += s.length();
    }

    private void insertAtAdvancedBorderCursor(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        this.advancedBorderColor = advancedBorderColor.substring(0, advancedBorderCursor) + s + advancedBorderColor.substring(advancedBorderCursor);
        this.advancedBorderCursor += s.length();
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

        MacroHudDataHandler.HudConfig next = new MacroHudDataHandler.HudConfig();
        next.enabled = this.working.enabled;
        next.elements = new ArrayList<>();
        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (!isExternalCanvasProxy(element)) {
                next.elements.add(element);
            }
        }
        MacroHudDataHandler.setConfig(next);
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
            String hudId = isSecondaryChatProxy(element)
                    ? HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT
                    : HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR;
            HudCanvasDataHandler.HudCanvasElement external = HudCanvasDataHandler.getMutableElement(hudId,
                    isSecondaryChatProxy(element) ? this::defaultSecondaryChatCanvas : this::defaultNbtInspectorCanvas);
            external.x = element.x;
            external.y = element.y;
            external.width = element.width;
            external.height = element.height;
            external.lineHeight = element.lineHeight;
            external.fontScale = element.fontScale;
            external.backgroundColor = element.backgroundColor;
            external.textColor = element.textColor;
            external.borderColor = element.borderColor;
            external.drawBackground = element.drawBackground;
            external.drawBorder = element.drawBorder;
            external.visible = element.visible;
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

        clampProxyToCanvas(proxy);
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

    private static void drawBorder(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
    }

    private int resolveElementX(MacroHudDataHandler.HudElement e) {
        return switch (e.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> e.x;
            case TOP_RIGHT, BOTTOM_RIGHT -> this.width - e.width - e.x;
            case CENTER -> (this.width - e.width) / 2 + e.x;
        };
    }

    private int resolveElementY(MacroHudDataHandler.HudElement e) {
        return switch (e.anchor) {
            case TOP_LEFT, TOP_RIGHT -> e.y;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> this.height - e.height - e.y;
            case CENTER -> (this.height - e.height) / 2 + e.y;
        };
    }

    private void setElementScreenPosition(MacroHudDataHandler.HudElement e, int screenX, int screenY) {
        e.x = switch (e.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> screenX;
            case TOP_RIGHT, BOTTOM_RIGHT -> this.width - e.width - screenX;
            case CENTER -> screenX - (this.width - e.width) / 2;
        };
        e.y = switch (e.anchor) {
            case TOP_LEFT, TOP_RIGHT -> screenY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> this.height - e.height - screenY;
            case CENTER -> screenY - (this.height - e.height) / 2;
        };
    }

    private static String shortAnchor(MacroHudDataHandler.Anchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "TL";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_RIGHT -> "BR";
            case CENTER -> "C";
        };
    }

    private static String shortVisibility(MacroHudDataHandler.VisibilityMode mode) {
        return switch (mode) {
            case ALWAYS -> "ALL";
            case CHAT_ONLY -> "CHAT";
        };
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

    private static List<String> placeholderDocs() {
        return List.of(
                "[How to use]",
                "Write placeholders as {token}. Example: /msg {player.name} hi",
                "Button Action supports cmd:/ msg:/ say:/ copy:/ bar:/ plain chat text",
                "Button Action and macro commands both support placeholders",
                "",
                "[Player]",
                "{player.name} => your name",
                "{player.uuid} => your UUID",
                "{hp} {max_hp} {food} {saturation} {xp} {level}",
                "{yaw} {pitch} {player.gamemode}",
                "",
                "[Position]",
                "{pos.x} {pos.y} {pos.z} => block coords",
                "{pos.xf} {pos.yf} {pos.zf} => decimal coords",
                "{pos.xyz} => x y z",
                "{pos.biome} {pos.dim} {pos.light} {pos.facing}",
                "",
                "[Look target]",
                "{look.block.xyz} {look.block.x} {look.block.y} {look.block.z}",
                "{look.entity.name} {look.entity.uuid} {look.entity.id}",
                "{look.dir.x} {look.dir.y} {look.dir.z}",
                "",
                "[Selectors and lists]",
                "{sel.self} {sel.nearest} {sel.random} {sel.all} {sel.entities}",
                "{players.count} {players.count.other} {players.nearby.count}",
                "{players.list} {players.list.other} {.nl for newline}",
                "{players.nearby.3} {.r128 .with_distance .unique .sort=name}",
                "{entities.nearby.3} {.r64 .with_distance .unique .sort=distance}",
                "",
                "[Item and misc]",
                "{hand.item} {hand.id} {hand.count}",
                "{hand.damage} {hand.max_damage} {hand.durability}",
                "{dim} => current dimension id",
                "{rand.int(1,10)} => random integer in range",
                "",
                "[Conditionals in macro commands]",
                "if:<left>==<right>::<when_true>:else:<when_false>",
                "Example: if:{player.gamemode}==creative::cmd:/say yes:else:cmd:/say no"
        );
    }

    private int modalX() {
        return this.width / 2 - (MODAL_W / 2);
    }

    private int modalY() {
        return this.height / 2 - (MODAL_H / 2);
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

