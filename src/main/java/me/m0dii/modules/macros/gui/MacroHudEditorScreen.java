package me.m0dii.modules.macros.gui;


import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MacroHudEditorScreen extends Screen implements ThemedScreen {
    /** Left button state; MouseHandler#isLeftPressed only updates while no screen is open (26.x). */
    private boolean leftMouseDown = false;

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            this.leftMouseDown = false;
        }
        return super.mouseReleased(event);
    }


    private static final int TOOLBAR_H = 32;

    private final Screen parent;
    private MacroHudDataHandler.HudConfig working;
    private MacroHudDataHandler.HudElement selected;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private EditBox primaryField;
    private EditBox macroField;

    public MacroHudEditorScreen(Screen parent) {
        super(Component.literal("Macro HUD Editor"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new MacroHudEditorScreen(parent);
    }

    @Override
    protected void init() {
        super.init();
        this.working = MacroHudDataHandler.getConfigCopy();

        this.addRenderableWidget(Button.builder(Component.literal("Add Button"), b -> {
                    MacroHudDataHandler.HudElement element = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.BUTTON);
                    element.x = 20;
                    element.y = 50;
                    element.label = "Macro";
                    this.working.elements.add(element);
                    this.selected = element;
                    syncFieldsFromSelection();
                }).bounds(8, 8, 80, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Add Text"), b -> {
                    MacroHudDataHandler.HudElement element = MacroHudDataHandler.createElement(MacroHudDataHandler.ElementType.TEXT);
                    element.x = 20;
                    element.y = 80;
                    element.text = "Status Text";
                    this.working.elements.add(element);
                    this.selected = element;
                    syncFieldsFromSelection();
                }).bounds(92, 8, 70, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
                    if (selected != null) {
                        this.working.elements.removeIf(e -> e.id.equals(selected.id));
                        this.selected = null;
                        syncFieldsFromSelection();
                    }
                }).bounds(166, 8, 58, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
                    applyFieldEdits();
                    MacroHudDataHandler.setConfig(this.working);
                }).bounds(this.width - 152, 8, 66, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
                    applyFieldEdits();
                    MacroHudDataHandler.setConfig(this.working);
                    onClose();
                }).bounds(this.width - 82, 8, 66, 20)
                .build());

        this.primaryField = new EditBox(this.font, this.width - 220, 44, 200, 18, Component.literal("Label/Text"));
        this.primaryField.setMaxLength(80);
        this.addRenderableWidget(this.primaryField);

        this.macroField = new EditBox(this.font, this.width - 220, 74, 200, 18, Component.literal("Macro Id"));
        this.macroField.setMaxLength(80);
        this.addRenderableWidget(this.macroField);

        syncFieldsFromSelection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        applyFieldEdits();
        updateDragging(mouseX, mouseY);

        context.fill(0, 0, this.width, this.height, 0xD0101010);
        context.fill(0, TOOLBAR_H, this.width, TOOLBAR_H + 1, 0x80FFFFFF);

        context.text(this.font, "Drag elements on canvas. Buttons are clickable when chat is open.", 8, TOOLBAR_H + 6, 0xFFE0E0E0);
        context.text(this.font, "Selection fields", this.width - 220, 30, 0xFFFFFF);

        int canvasRight = this.width - 240;
        context.fill(canvasRight, TOOLBAR_H, canvasRight + 1, this.height, 0x60FFFFFF);

        for (MacroHudDataHandler.HudElement element : this.working.elements) {
            if (!element.visible) {
                continue;
            }
            drawElement(context, element);
        }

        if (selected != null) {
            context.text(this.font, "Type: " + selected.type.name(), this.width - 220, 98, 0xFFB0B0B0);
            context.text(this.font, "Pos: " + selected.x + ", " + selected.y, this.width - 220, 112, 0xFFB0B0B0);
            context.text(this.font, "Size: " + selected.width + " x " + selected.height, this.width - 220, 126, 0xFFB0B0B0);
            context.text(this.font, "Use mouse wheel to resize width", this.width - 220, 144, 0xFF808080);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.leftMouseDown = true;
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        if (mouseY < TOOLBAR_H || mouseX >= this.width - 240) {
            dragging = false;
            return false;
        }

        for (int i = this.working.elements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement element = this.working.elements.get(i);
            if (contains(element, mouseX, mouseY)) {
                selected = element;
                dragOffsetX = (int) mouseX - element.x;
                dragOffsetY = (int) mouseY - element.y;
                dragging = true;
                syncFieldsFromSelection();
                return true;
            }
        }

        selected = null;
        dragging = false;
        syncFieldsFromSelection();
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected != null && mouseX < this.width - 240) {
            int delta = verticalAmount > 0 ? 4 : -4;
            selected.width = Math.clamp(selected.width + delta, 40, 260);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateDragging(int mouseX, int mouseY) {
        if (!dragging || selected == null || this.minecraft == null) {
            return;
        }

        long handle = this.minecraft.getWindow().handle();
        if (!this.leftMouseDown) {
            dragging = false;
            return;
        }

        int maxX = this.width - 240 - selected.width;
        int maxY = this.height - selected.height;
        selected.x = Math.clamp(mouseX - dragOffsetX, 0, Math.max(0, maxX));
        selected.y = Math.clamp(mouseY - dragOffsetY, TOOLBAR_H + 2, Math.max(TOOLBAR_H + 2, maxY));
    }

    private void drawElement(GuiGraphicsExtractor context, MacroHudDataHandler.HudElement element) {
        int x1 = element.x;
        int y1 = element.y;
        int x2 = element.x + element.width;
        int y2 = element.y + element.height;

        if (element.type == MacroHudDataHandler.ElementType.TEXT) {
            context.text(this.font, element.text, element.x, element.y, element.textColor);
        } else {
            context.fill(x1, y1, x2, y2, element.backgroundColor);
            int textX = x1 + (element.width - this.font.width(element.label)) / 2;
            int textY = y1 + (element.height - 8) / 2;
            context.text(this.font, element.label, textX, textY, element.textColor);
        }

        if (selected != null && selected.id.equals(element.id)) {
            context.fill(x1, y1, x2, y1 + 1, 0xFFFFFF00);
            context.fill(x1, y2 - 1, x2, y2, 0xFFFFFF00);
            context.fill(x1, y1, x1 + 1, y2, 0xFFFFFF00);
            context.fill(x2 - 1, y1, x2, y2, 0xFFFFFF00);
        }
    }

    private void syncFieldsFromSelection() {
        if (this.primaryField == null || this.macroField == null) {
            return;
        }

        if (selected == null) {
            this.primaryField.setEditable(false);
            this.macroField.setEditable(false);
            this.primaryField.setValue("");
            this.macroField.setValue("");
            return;
        }

        this.primaryField.setEditable(true);
        this.macroField.setEditable(selected.type == MacroHudDataHandler.ElementType.BUTTON);
        this.primaryField.setValue(selected.type == MacroHudDataHandler.ElementType.BUTTON ? selected.label : selected.text);
        this.macroField.setValue(selected.macroId == null ? "" : selected.macroId);
    }

    private void applyFieldEdits() {
        if (selected == null || this.primaryField == null || this.macroField == null) {
            return;
        }

        String primary = this.primaryField.getValue();
        if (selected.type == MacroHudDataHandler.ElementType.BUTTON) {
            selected.label = primary == null || primary.isBlank() ? "Button" : primary.trim();
            selected.macroId = this.macroField.getValue() == null ? "" : this.macroField.getValue().trim();
        } else {
            selected.text = primary == null || primary.isBlank() ? "Text" : primary.trim();
        }
    }

    private static boolean contains(MacroHudDataHandler.HudElement e, double x, double y) {
        return x >= e.x && x <= e.x + e.width && y >= e.y && y <= e.y + e.height;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

