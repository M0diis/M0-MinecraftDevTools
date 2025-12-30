package me.m0dii.modules.scripting.gui;

import lombok.Getter;
import me.m0dii.modules.scripting.InGameScriptingKeybinds;
import me.m0dii.modules.scripting.ScriptStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SavedScriptsScreen extends Screen {
    private static final String GROOVY_EXT = ".groovy";
    private static final String KOTLIN_EXT = ".kts";

    private final Screen parent;
    private final List<String> savedScripts = new ArrayList<>();

    private int scriptListScroll = 0;
    @Getter
    private String selectedScript = "";
    private String output = "";
    private boolean renaming = false;
    private String renameInput = "";
    private ButtonWidget setKeybindButton;
    private ButtonWidget clearKeybindButton;
    private String awaitingKeybindScript = null;

    public SavedScriptsScreen(Screen parent) {
        super(Text.literal("Saved Scripts"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        this.clearChildren();
        ButtonWidget backButton = ButtonWidget.builder(Text.literal("Back"), btn -> MinecraftClient.getInstance().setScreen(parent)).dimensions(20, this.height - 30, 60, 20).build();
        ButtonWidget loadButton = ButtonWidget.builder(Text.literal("Load"), btn -> loadSelectedScript()).dimensions(100, this.height - 30, 60, 20).build();
        ButtonWidget deleteButton = ButtonWidget.builder(Text.literal("Delete"), btn -> deleteSelectedScript()).dimensions(170, this.height - 30, 60, 20).build();
        ButtonWidget renameButton = ButtonWidget.builder(Text.literal("Rename"), btn -> startRename()).dimensions(240, this.height - 30, 60, 20).build();
        ButtonWidget copyButton = ButtonWidget.builder(Text.literal("Copy Name"), btn -> copySelectedName()).dimensions(310, this.height - 30, 80, 20).build();
        this.addDrawableChild(backButton);
        this.addDrawableChild(loadButton);
        this.addDrawableChild(deleteButton);
        this.addDrawableChild(renameButton);
        this.addDrawableChild(copyButton);
        // Add a single Set Keybind button below the list
        setKeybindButton = ButtonWidget.builder(Text.literal(getKeybindLabel(selectedScript)), btn -> {
            if (selectedScript != null && !selectedScript.isEmpty()) {
                awaitingKeybindScript = selectedScript;
                output = "Press a key to assign as keybind...";
                setKeybindButton.active = false;
            }
        }).dimensions(40, this.height - 90, 160, 20).build();
        setKeybindButton.active = selectedScript != null && !selectedScript.isEmpty();
        this.addDrawableChild(setKeybindButton);
        // Add Clear Keybind button
        clearKeybindButton = ButtonWidget.builder(Text.literal("Clear Keybind"), btn -> {
            if (selectedScript != null && !selectedScript.isEmpty() && InGameScriptingKeybinds.getKeybindKey(selectedScript) != null) {
                InGameScriptingKeybinds.clearKeybind(selectedScript);
                setKeybindButton.setMessage(Text.literal(getKeybindLabel(selectedScript)));
                clearKeybindButton.active = false;
                output = "Keybind cleared for " + selectedScript;
            }
        }).dimensions(210, this.height - 90, 120, 20).build();
        clearKeybindButton.active = selectedScript != null && InGameScriptingKeybinds.getKeybindKey(selectedScript) != null;
        this.addDrawableChild(clearKeybindButton);
        loadSavedScripts();
    }

    private String getKeybindLabel(String scriptName) {
        if (scriptName == null || scriptName.isEmpty()) {
            return "Set Keybind";
        }
        Integer key = InGameScriptingKeybinds.getKeybindKey(scriptName);
        if (key == null) {
            return "Set Keybind";
        }
        return "Key: " + InputUtil.fromKeyCode(key, 0).getTranslationKey();
    }

    private void loadSavedScripts() {
        savedScripts.clear();
        savedScripts.addAll(ScriptStorage.listScripts());
    }

    private void loadSelectedScript() {
        if (selectedScript.isEmpty()) {
            output = "No script selected.";
            return;
        }
        try {
            String content = ScriptStorage.readScript(selectedScript);
            if (parent instanceof ScriptEditorScreen editor) {
                editor.setScriptBoxText(content);
                editor.setFileNameBoxText(selectedScript);
                editor.setOutputBoxText("Loaded " + selectedScript);
            }
            MinecraftClient.getInstance().setScreen(parent);
        } catch (IOException e) {
            output = "Load error: " + e.getMessage();
        }
    }

    private void deleteSelectedScript() {
        if (selectedScript.isEmpty()) {
            output = "No script selected.";
            return;
        }

        try {
            boolean deleted = ScriptStorage.deleteScript(selectedScript);
            output = deleted ? ("Deleted " + selectedScript) : ("Failed to delete " + selectedScript);
            selectedScript = "";
            loadSavedScripts();
        } catch (IOException e) {
            output = "Delete error: " + e.getMessage();
        }
    }

    private void startRename() {
        if (!renaming && !selectedScript.isEmpty()) {
            renaming = true;
            renameInput = selectedScript;
            output = "Type new name and press Enter.";
        }
    }

    private void finishRename() {
        if (!renaming || selectedScript.isEmpty()) {
            return;
        }

        String newName = renameInput.trim();

        if (newName.isEmpty()) {
            output = "Enter a new name!";
            return;
        }

        try {
            boolean success = ScriptStorage.renameScript(selectedScript, newName);
            if (!success) {
                output = "A script with that name already exists.";
                return;
            }

            output = "File has been renamed to " + newName;
            selectedScript = newName;
            renaming = false;
            loadSavedScripts();
        } catch (IOException e) {
            output = "Rename error: " + e.getMessage();
        }
    }

    private void copySelectedName() {
        if (!selectedScript.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selectedScript), null);
            output = "Copied name to clipboard.";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = 40;
        int listY = 40;
        int listW = this.width - 80;
        // Use the same list height as `render` so hit detection matches the drawn list
        int listH = this.height - 140;
        int rowHeight = 18;

        if (mouseY >= listY && mouseY < listY + listH && mouseX >= listX && mouseX < listX + listW) {
            int visibleRows = Math.max(1, listH / rowHeight);
            int maxScroll = Math.max(0, savedScripts.size() - visibleRows);
            scriptListScroll = MathHelper.clamp(scriptListScroll, 0, maxScroll);
            int idx = (int) ((mouseY - listY) / rowHeight) + scriptListScroll;
            if (idx >= 0 && idx < savedScripts.size()) {
                selectedScript = savedScripts.get(idx);
                output = "Selected: " + selectedScript;
                if (setKeybindButton != null) {
                    setKeybindButton.setMessage(Text.literal(getKeybindLabel(selectedScript)));
                    setKeybindButton.active = true;
                }
                if (clearKeybindButton != null) {
                    clearKeybindButton.active = InGameScriptingKeybinds.getKeybindKey(selectedScript) != null;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listX = 40;
        int listY = 40;
        int listW = this.width - 80;
        // Match the list height used in render()
        int listH = this.height - 140;
        int rowHeight = 18;

        if (mouseY >= listY && mouseY < listY + listH && mouseX >= listX && mouseX < listX + listW) {
            int visibleRows = Math.max(1, listH / rowHeight);
            int maxScroll = Math.max(0, savedScripts.size() - visibleRows);
            // amount is positive when scrolling up; we want to move the list accordingly
            int delta = (int) Math.signum(verticalAmount);
            scriptListScroll = MathHelper.clamp(scriptListScroll - delta, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renaming) {
            if (keyCode == 257 || keyCode == 335) { // Enter
                finishRename();
                return true;
            } else if (keyCode == 259) { // Backspace
                if (!renameInput.isEmpty()) {
                    renameInput = renameInput.substring(0, renameInput.length() - 1);
                }
                return true;
            }
        }
        if (awaitingKeybindScript != null) {
            InGameScriptingKeybinds.registerKeybind(awaitingKeybindScript, keyCode);
            output = "Keybind set for " + awaitingKeybindScript;
            awaitingKeybindScript = null;
            if (setKeybindButton != null) {
                setKeybindButton.setMessage(Text.literal(getKeybindLabel(selectedScript)));
                setKeybindButton.active = selectedScript != null && !selectedScript.isEmpty();
            }
            if (clearKeybindButton != null) {
                clearKeybindButton.active = true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (renaming && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.')) {
            renameInput += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        int listX = 40;
        int listY = 40;
        int listW = this.width - 80;
        int listH = this.height - 140;
        int rowHeight = 18;
        int visibleRows = Math.max(1, listH / rowHeight);
        int startIdx = Math.max(0, scriptListScroll);
        int endIdx = Math.min(savedScripts.size(), startIdx + visibleRows);

        // List background with border
        context.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, 0xFF222222);
        try {
            // drawBorder may not exist on all DrawContext versions — surround with try in case
            context.drawBorder(listX - 2, listY - 2, listW + 4, listH + 4, 0xFF888888);
        } catch (NoSuchMethodError ignored) {
        }

        int selectedIndex = savedScripts.indexOf(selectedScript);

        // Draw scripts with alternating row colors, highlight selected and hovered
        for (int i = startIdx; i < endIdx; i++) {
            int y = listY + (i - startIdx) * rowHeight;
            int bgColor = (i % 2 == 0) ? 0xFF292929 : 0xFF232323;
            boolean isSelected = i == selectedIndex;
            boolean isHovered = mouseY >= y && mouseY < y + rowHeight && mouseX >= listX && mouseX < listX + listW;
            if (isSelected) {
                bgColor = 0xFF3A5A8C;
            } else if (isHovered) {
                bgColor = 0xFF2E3A4C;
            }
            context.fill(listX, y, listX + listW, y + rowHeight, bgColor);
            // Script name with padding
            int textColor;
            if (isSelected) {
                textColor = 0xFFFFFFFF; // White
            } else if (isHovered) {
                textColor = 0xFFEEEEEE; // Light gray
            } else {
                textColor = 0xFFCCCCCC; // Gray
            }

            context.drawText(this.textRenderer, savedScripts.get(i), listX + 8, y + 4, textColor, false);
        }

        // Scroll bar if needed
        if (savedScripts.size() > visibleRows) {
            int barH = Math.max(20, listH * visibleRows / savedScripts.size());
            int barY = listY + (listH - barH) * scriptListScroll / Math.max(1, (savedScripts.size() - visibleRows));
            context.fill(listX + listW - 6, barY, listX + listW, barY + barH, 0xFF888888);
        }

        if (renaming) {
            context.drawText(this.textRenderer, "Rename to: " + renameInput + "_", 40, this.height - 70, 0xFFFFCC66, false);
        }

        context.drawText(this.textRenderer, output, 40, this.height - 50, 0xFFFFFFFF, false);

        if (awaitingKeybindScript != null) {
            context.drawText(this.textRenderer, "Press a key to assign as keybind...", 220, this.height - 90, 0xFFFFCC66, false);
        }
    }
}
