package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.MacroDataHandler;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacroKeyboardLayoutScreen extends Screen {

    private final Screen parent;
    private final List<KeyCell> cells = new ArrayList<>();
    private final Map<Integer, List<String>> bindings = new HashMap<>();
    private int selectedKey = -1;

    private record KeyCell(int keyCode, String label, int x, int y, int w, int h) {
    }

    public MacroKeyboardLayoutScreen(Screen parent) {
        super(Text.literal("Macro Keyboard Layout"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new MacroKeyboardLayoutScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(this.width - 78, 8, 66, 20)
                .build());

        rebuildBindings();
        rebuildKeyboardGrid();
    }

    private void rebuildBindings() {
        bindings.clear();

        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : MacroDataHandler.getAllMacros().entrySet()) {
            MacroDataHandler.MacroEntry macro = entry.getValue();
            if (macro == null || macro.keyCode < 0) {
                continue;
            }
            String name = (macro.name == null || macro.name.isBlank()) ? entry.getKey() : macro.name;
            bindings.computeIfAbsent(macro.keyCode, k -> new ArrayList<>()).add(name + " [" + entry.getKey() + "]");
        }
    }

    private void rebuildKeyboardGrid() {
        cells.clear();

        int x0 = 14;
        int y0 = 42;
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
    }

    private void addRow(int xStart, int y, int w, int h, int gap, int... keys) {
        int x = xStart;
        for (int key : keys) {
            int keyW = w;
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_CAPS_LOCK
                    || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || key == GLFW.GLFW_KEY_SPACE) {
                keyW = switch (key) {
                    case GLFW.GLFW_KEY_SPACE -> w * 4;
                    case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> w * 2;
                    case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_CAPS_LOCK, GLFW.GLFW_KEY_ENTER -> w + 18;
                    default -> w;
                };
            }
            cells.add(new KeyCell(key, keyLabel(key), x, y, keyW, h));
            x += keyW + gap;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);

        context.drawTextWithShadow(this.textRenderer, "Keyboard Macro Map", 14, 12, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Green keys have one or more macros bound", 14, 24, 0xFFB0FFB0);

        for (KeyCell cell : cells) {
            boolean hasMacro = bindings.containsKey(cell.keyCode);
            boolean selected = cell.keyCode == selectedKey;
            int bg = hasMacro ? 0xAA1E5A1E : 0xAA202020;
            if (selected) {
                bg = 0xAA7A5A1A;
            }

            context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + cell.h, bg);
            context.fill(cell.x, cell.y, cell.x + cell.w, cell.y + 1, 0x60FFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, cell.label, cell.x + (cell.w / 2), cell.y + 5, 0xFFFFFFFF);
        }

        int panelX = this.width - 280;
        context.fill(panelX, 42, this.width - 10, this.height - 10, 0xAA111111);
        context.drawTextWithShadow(this.textRenderer, "Selected key", panelX + 10, 50, 0xFFFFFFFF);

        String keyName = selectedKey < 0 ? "None" : keyLabel(selectedKey);
        context.drawTextWithShadow(this.textRenderer, keyName, panelX + 10, 64, 0xFFFFFF55);

        int y = 84;
        List<String> bound = bindings.getOrDefault(selectedKey, List.of());
        if (bound.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "No macros bound", panelX + 10, y, 0xFFAAAAAA);
        } else {
            context.drawTextWithShadow(this.textRenderer, "Macros:", panelX + 10, y, 0xFFAAFFAA);
            y += 14;
            for (String line : bound) {
                if (y > this.height - 22) {
                    context.drawTextWithShadow(this.textRenderer, "...", panelX + 10, y, 0xFF888888);
                    break;
                }
                context.drawTextWithShadow(this.textRenderer, "- " + line, panelX + 10, y, 0xFFE0E0E0);
                y += 12;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() != 0) {
            return false;
        }

        double x = click.x();
        double y = click.y();
        for (KeyCell cell : cells) {
            if (x >= cell.x && x <= cell.x + cell.w && y >= cell.y && y <= cell.y + cell.h) {
                selectedKey = cell.keyCode;
                return true;
            }
        }
        return false;
    }

    private static String keyLabel(int keyCode) {
        try {
            String s = InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
            return s.length() > 8 ? s.substring(0, 8) : s;
        } catch (Exception ignored) {
            return "?";
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

