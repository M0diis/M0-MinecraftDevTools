package me.m0dii.modules.macros.gui;



import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.local.*;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class MacroPanelEditorScreen extends Screen implements ThemedScreen {
    private final Screen parent;
    private MacroPanelDataHandler.PanelConfig working;
    private final UiSelectionList buttonList = new UiSelectionList(0, 0, 1, 1);
    private String selectedButtonId;

    private UiBoundTextField titleField;
    private UiBoundTextField labelField;
    private UiBoundTextField macroIdField;
    private UiBoundIntField xField;
    private UiBoundIntField yField;
    private UiBoundIntField widthField;
    private UiBoundIntField heightField;
    private UiToggleButton enabledToggle;
    private Button addButton;
    private Button deleteButton;

    private MacroPanelEditorScreen(Screen parent) {
        super(Component.literal("Macro Panel Editor"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new MacroPanelEditorScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        if (this.working == null) {
            this.working = MacroPanelDataHandler.getConfig();
        }
        if (this.selectedButtonId == null && !this.working.buttons.isEmpty()) {
            this.selectedButtonId = this.working.buttons.getFirst().id;
        }

        int topY = 8;
        int buttonW = 66;
        int doneX = this.width - buttonW - 8;
        int saveX = doneX - buttonW - 4;
        int macrosX = saveX - 96 - 4;

        this.addRenderableWidget(Button.builder(Component.literal("Open Macros"), button -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(MacroWorkbenchScreen.create(this, MacroWorkbenchScreen.Tab.MACROS));
                    }
                }).bounds(macrosX, topY, 96, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveWorking())
                .bounds(saveX, topY, buttonW, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                    saveWorking();
                    onClose();
                }).bounds(doneX, topY, buttonW, 20)
                .build());

        int leftX = 12;
        int leftW = Math.max(220, this.width / 3);
        int rightX = leftX + leftW + 12;
        int rightW = Math.max(220, this.width - rightX - 12);
        int controlsY = 38;

        this.addButton = this.addRenderableWidget(Button.builder(Component.literal("+ Button"), button -> addPanelButton())
                .bounds(leftX, controlsY, 82, 20)
                .build());
        this.deleteButton = this.addRenderableWidget(Button.builder(Component.literal("Delete"), button -> deleteSelectedButton())
                .bounds(leftX + 86, controlsY, 72, 20)
                .build());

        int fieldX = rightX + 12;
        int fieldW = Math.max(180, rightW - 24);
        int halfW = (fieldW - 4) / 2;
        int rowY = 64;

        this.titleField = new UiBoundTextField(this.font, fieldX, rowY, fieldW, 18, "Panel title")
                .setMaxLength(96)
                .setSaveConsumer(value -> this.working.title = value);
        this.addRenderableWidget(this.titleField.widget());

        rowY += 30;
        this.enabledToggle = new UiToggleButton("Panel Enabled", this.working.enabled, fieldX, rowY, fieldW, 20)
                .setLabels("YES", "NO")
                .setSaveConsumer(value -> this.working.enabled = value);
        this.addRenderableWidget(this.enabledToggle.widget());

        rowY += 34;
        this.labelField = new UiBoundTextField(this.font, fieldX, rowY, fieldW, 18, "Button label")
                .setMaxLength(80)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button == null) {
                        return;
                    }
                    button.label = value;
                    refreshButtonRows();
                });
        this.addRenderableWidget(this.labelField.widget());

        rowY += 30;
        this.macroIdField = new UiBoundTextField(this.font, fieldX, rowY, fieldW, 18, "Macro id")
                .setMaxLength(80)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button == null) {
                        return;
                    }
                    button.macroId = value == null ? "" : value.trim();
                    refreshButtonRows();
                });
        this.addRenderableWidget(this.macroIdField.widget());

        rowY += 30;
        this.xField = new UiBoundIntField(this.font, fieldX, rowY, halfW, 18, "X")
                .setMin(0)
                .setMax(6000)
                .setDefaultValue(20)
                .setMaxLength(6)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button != null) {
                        button.x = value;
                    }
                });
        this.yField = new UiBoundIntField(this.font, fieldX + halfW + 4, rowY, halfW, 18, "Y")
                .setMin(0)
                .setMax(6000)
                .setDefaultValue(40)
                .setMaxLength(6)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button != null) {
                        button.y = value;
                    }
                });
        this.addRenderableWidget(this.xField.widget());
        this.addRenderableWidget(this.yField.widget());

        rowY += 30;
        this.widthField = new UiBoundIntField(this.font, fieldX, rowY, halfW, 18, "Width")
                .setMin(50)
                .setMax(240)
                .setDefaultValue(110)
                .setMaxLength(4)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button != null) {
                        button.width = value;
                    }
                });
        this.heightField = new UiBoundIntField(this.font, fieldX + halfW + 4, rowY, halfW, 18, "Height")
                .setMin(18)
                .setMax(40)
                .setDefaultValue(20)
                .setMaxLength(4)
                .setSaveConsumer(value -> {
                    MacroPanelDataHandler.PanelButton button = selectedButton();
                    if (button != null) {
                        button.height = value;
                    }
                });
        this.addRenderableWidget(this.widthField.widget());
        this.addRenderableWidget(this.heightField.widget());

        refreshButtonRows();
        syncFields();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractBackground(context, mouseX, mouseY, delta);

        int leftX = 12;
        int leftW = Math.max(220, this.width / 3);
        int listY = 64;
        int listH = Math.max(120, this.height - listY - 10);
        int rightX = leftX + leftW + 12;
        int rightW = Math.max(220, this.width - rightX - 12);
        int rightY = 64;
        int rightH = Math.max(140, this.height - rightY - 10);
        int titleY = 64;
        int enabledY = titleY + 30;
        int labelY = enabledY + 34;
        int macroIdY = labelY + 30;
        int positionY = macroIdY + 30;
        int sizeY = positionY + 30;

        this.buttonList.setBounds(leftX, listY, leftW, listH);

        context.text(this.font, "Buttons", leftX, 48, 0xFFFFFFFF);
        context.text(this.font, "Panel Settings", rightX, 48, 0xFFFFFFFF);
        this.buttonList.extractRenderState(context, this.font, mouseX, mouseY);
        UiForms.drawPanel(context, rightX, rightY, rightW, rightH);

        int infoX = rightX + 12;
        int infoY = rightY + 8;
        MacroPanelDataHandler.PanelButton selected = selectedButton();
        context.text(this.font, "Panel title", infoX, titleY - 10, 0xFFB8B8B8);
        context.text(this.font, "Runtime macros can be managed in the Macros tab.", infoX, enabledY + 24, 0xFF9FCFCF);
        context.text(this.font, "Button", infoX, labelY - 10, 0xFFB8B8B8);
        context.text(this.font, "Macro Id", infoX, macroIdY - 10, 0xFFB8B8B8);
        context.text(this.font, "Position", infoX, positionY - 10, 0xFFB8B8B8);
        context.text(this.font, "Size", infoX, sizeY - 10, 0xFFB8B8B8);

        if (selected == null) {
            context.text(this.font, "Create or select a button to edit it.", infoX, sizeY + 38, 0xFFB8B8B8);
        } else {
            String macroName = resolveMacroName(selected.macroId);
            context.text(this.font, "Selected: " + selected.label + " [" + selected.id + "]", infoX, sizeY + 38, 0xFFFFFFFF);
            context.text(this.font, "Targets: " + macroName, infoX, sizeY + 52, 0xFFB8B8B8);
        }

        renderMacroHelp(context, infoX, rightY + rightH - 36, rightW - 24);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT && this.buttonList.handleMouseClick(click.x(), click.y())) {
            int index = this.buttonList.selectedIndex();
            this.selectedButtonId = index >= 0 && index < this.working.buttons.size() ? this.working.buttons.get(index).id : null;
            syncFields();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.buttonList.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);
        context.fill(0, 31, this.width, 32, 0x70FFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addPanelButton() {
        MacroPanelDataHandler.PanelButton button = MacroPanelDataHandler.createDefaultButton();
        this.working.buttons.add(button);
        this.selectedButtonId = button.id;
        refreshButtonRows();
        syncFields();
    }

    private void deleteSelectedButton() {
        if (this.selectedButtonId == null) {
            return;
        }
        this.working.buttons.removeIf(button -> this.selectedButtonId.equals(button.id));
        if (this.working.buttons.isEmpty()) {
            this.selectedButtonId = null;
        } else {
            this.selectedButtonId = this.working.buttons.getFirst().id;
        }
        refreshButtonRows();
        syncFields();
    }

    private void saveWorking() {
        MacroPanelDataHandler.setConfig(this.working);
    }

    private void refreshButtonRows() {
        List<String> rows = new ArrayList<>(this.working.buttons.size());
        for (MacroPanelDataHandler.PanelButton button : this.working.buttons) {
            rows.add(StringUtils.safe(button.label).trim() + " [" + button.id + "] -> " + (button.macroId == null || button.macroId.isBlank() ? "(none)" : button.macroId));
        }
        this.buttonList.setRows(rows);

        int selectedIndex = -1;
        if (this.selectedButtonId != null) {
            for (int i = 0; i < this.working.buttons.size(); i++) {
                if (this.selectedButtonId.equals(this.working.buttons.get(i).id)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        this.buttonList.setSelectedIndex(selectedIndex);
    }

    private void syncFields() {
        this.titleField.setText(StringUtils.safe(this.working.title));
        this.enabledToggle.setValue(this.working.enabled);

        MacroPanelDataHandler.PanelButton button = selectedButton();
        boolean active = button != null;
        this.labelField.setEditable(active);
        this.macroIdField.setEditable(active);
        this.xField.setEditable(active);
        this.yField.setEditable(active);
        this.widthField.setEditable(active);
        this.heightField.setEditable(active);
        this.deleteButton.active = active;

        if (!active) {
            this.labelField.setText("");
            this.macroIdField.setText("");
            this.xField.widget().setValue("");
            this.yField.widget().setValue("");
            this.widthField.widget().setValue("");
            this.heightField.widget().setValue("");
            return;
        }

        this.labelField.setText(StringUtils.safe(button.label));
        this.macroIdField.setText(StringUtils.safe(button.macroId));
        this.xField.setValue(button.x);
        this.yField.setValue(button.y);
        this.widthField.setValue(button.width);
        this.heightField.setValue(button.height);
    }

    private MacroPanelDataHandler.PanelButton selectedButton() {
        if (this.selectedButtonId == null) {
            return null;
        }
        for (MacroPanelDataHandler.PanelButton button : this.working.buttons) {
            if (this.selectedButtonId.equals(button.id)) {
                return button;
            }
        }
        return null;
    }

    private void renderMacroHelp(GuiGraphicsExtractor context, int x, int y, int maxWidth) {
        List<String> macroIds = new ArrayList<>(MacroDataHandler.getAllMacros().keySet());
        macroIds.sort(String.CASE_INSENSITIVE_ORDER);
        String joined = macroIds.isEmpty() ? "No macros created yet." : "Known macro ids: " + String.join(", ", macroIds);
        String text = joined;
        while (this.font.width(text) > maxWidth && text.length() > 12) {
            text = text.substring(0, text.length() - 1);
        }
        if (!text.equals(joined)) {
            text += "...";
        }
        context.text(this.font, text, x, y, 0xFF9FCFCF);
    }

    private static String resolveMacroName(String macroId) {
        if (macroId == null || macroId.isBlank()) {
            return "(none)";
        }
        MacroDataHandler.MacroEntry macro = MacroDataHandler.getMacro(macroId.trim());
        if (macro == null) {
            return macroId.trim() + " (missing)";
        }
        String name = StringUtils.safe(macro.name).trim();
        return name.isEmpty() ? macroId.trim() : name;
    }
}
