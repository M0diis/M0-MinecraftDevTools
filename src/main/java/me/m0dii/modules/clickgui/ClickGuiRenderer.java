package me.m0dii.modules.clickgui;

import lombok.Getter;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ClickGuiRenderer {
    @Getter
    private boolean visible = false;
    private final ModuleCategory[] categories;
    private int selectedCategoryIndex = 0;
    private int selectedModuleIndex = 0;
    private boolean inModuleList = false;
    private boolean inSettingsView = false;
    private int selectedSettingIndex = 0;
    private Module currentSettingsModule = null;

    private static final int CATEGORY_WIDTH = 120;
    private static final int MODULE_WIDTH = 140;
    private static final int SETTINGS_WIDTH = 180;
    private static final int ITEM_HEIGHT = 18;
    private static final int PADDING = 4;
    private static final int START_X = 10;
    private static final int START_Y = 10;

    private static final int COLOR_BACKGROUND = 0xD0101010;
    private static final int COLOR_SELECTED = 0xFF4A90E2;
    private static final int COLOR_ENABLED = 0xFF50C878;
    private static final int COLOR_DISABLED = 0xFF808080;
    private static final int COLOR_BORDER = 0xFF404040;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean enterPressed = false;

    public ClickGuiRenderer() {
        this.categories = ModuleRegistry.getCategories();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public void toggle() {
        visible = !visible;
    }

    public void hide() {
        visible = false;
    }

    public void show() {
        visible = true;
    }

    private void onClientTick(MinecraftClient client) {
        if (!visible || client.player == null) {
            return;
        }

        long window = client.getWindow().getHandle();

        boolean upNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_UP);
        boolean downNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_DOWN);
        boolean leftNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT);
        boolean rightNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT);
        boolean enterNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_ENTER);

        if (categories.length == 0) {
            return;
        }

        ModuleCategory currentCategory = categories[selectedCategoryIndex];
        List<Module> modules = currentCategory.getModules();

        if (inSettingsView) {
            if (currentSettingsModule != null) {
                List<String> settings = currentSettingsModule.getSettingsDisplay();

                if (upNow && !upPressed) {
                    if (!settings.isEmpty()) {
                        selectedSettingIndex = (selectedSettingIndex - 1 + settings.size()) % settings.size();
                    }
                }
                upPressed = upNow;

                if (downNow && !downPressed) {
                    if (!settings.isEmpty()) {
                        selectedSettingIndex = (selectedSettingIndex + 1) % settings.size();
                    }
                }
                downPressed = downNow;

                if (leftNow && !leftPressed) {
                    inSettingsView = false;
                    currentSettingsModule = null;
                    selectedSettingIndex = 0;
                }
                leftPressed = leftNow;

                rightPressed = rightNow;

                if (enterNow && !enterPressed) {
                    if (!settings.isEmpty() && selectedSettingIndex < settings.size()) {
                        currentSettingsModule.onSettingSelected(selectedSettingIndex);
                    }
                }
                enterPressed = enterNow;
            }
        } else if (inModuleList) {
            if (upNow && !upPressed) {
                if (!modules.isEmpty()) {
                    selectedModuleIndex = (selectedModuleIndex - 1 + modules.size()) % modules.size();
                }
            }
            upPressed = upNow;

            if (downNow && !downPressed) {
                if (!modules.isEmpty()) {
                    selectedModuleIndex = (selectedModuleIndex + 1) % modules.size();
                }
            }
            downPressed = downNow;

            if (leftNow && !leftPressed) {
                inModuleList = false;
            }
            leftPressed = leftNow;

            if (rightNow && !rightPressed) {
                if (!modules.isEmpty() && selectedModuleIndex < modules.size()) {
                    Module module = modules.get(selectedModuleIndex);
                    if (module.hasSettings()) {
                        inSettingsView = true;
                        currentSettingsModule = module;
                        selectedSettingIndex = 0;
                    }
                }
            }
            rightPressed = rightNow;

            if (enterNow && !enterPressed) {
                if (!modules.isEmpty() && selectedModuleIndex < modules.size()) {
                    Module module = modules.get(selectedModuleIndex);
                    module.toggleEnabled();
                }
            }
            enterPressed = enterNow;
        } else {
            if (upNow && !upPressed) {
                selectedCategoryIndex = (selectedCategoryIndex - 1 + categories.length) % categories.length;
                selectedModuleIndex = 0;
            }
            upPressed = upNow;

            if (downNow && !downPressed) {
                selectedCategoryIndex = (selectedCategoryIndex + 1) % categories.length;
                selectedModuleIndex = 0;
            }
            downPressed = downNow;

            leftPressed = leftNow;

            if (rightNow && !rightPressed) {
                inModuleList = true;
                selectedModuleIndex = 0;
            }
            rightPressed = rightNow;

            if (enterNow && !enterPressed) {
                inModuleList = true;
                selectedModuleIndex = 0;
            }
            enterPressed = enterNow;
        }
    }

    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!visible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        int screenHeight = client.getWindow().getScaledHeight();

        renderCategories(context, START_X, START_Y);

        if (categories.length > 0 && selectedCategoryIndex < categories.length) {
            ModuleCategory category = categories[selectedCategoryIndex];
            renderModules(context, START_X + CATEGORY_WIDTH + 10, START_Y, category);

            if (inSettingsView && currentSettingsModule != null) {
                renderSettings(context, START_X + CATEGORY_WIDTH + MODULE_WIDTH + 20, START_Y);
            }
        }

        renderInstructions(context, screenHeight);
    }

    private void renderCategories(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        int currentY = y;

        for (int i = 0; i < categories.length; i++) {
            ModuleCategory category = categories[i];
            boolean selected = (i == selectedCategoryIndex);
            boolean categoryFocused = selected && !inModuleList;

            int bgColor = categoryFocused ? COLOR_SELECTED : (selected ? 0x80404040 : COLOR_BACKGROUND);
            context.fill(x, currentY, x + CATEGORY_WIDTH, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + CATEGORY_WIDTH, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + CATEGORY_WIDTH, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            String count = "(" + category.getModules().size() + ")";
            int countWidth = client.textRenderer.getWidth(count);
            int countX = x + CATEGORY_WIDTH - countWidth - 4;

            int textX = x + PADDING + 2;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = countX - textX - 4;

            String text = category.getName();
            int textWidth = client.textRenderer.getWidth(text);
            if (textWidth > availableWidth) {
                while (textWidth > availableWidth - client.textRenderer.getWidth("...") && !text.isEmpty()) {
                    text = text.substring(0, text.length() - 1);
                    textWidth = client.textRenderer.getWidth(text);
                }
                text = text + "...";
            }

            context.drawText(client.textRenderer, text, textX, textY, COLOR_TEXT, false);
            context.drawText(client.textRenderer, count, countX, textY, COLOR_TEXT, false);

            currentY += ITEM_HEIGHT;
        }
    }

    private void renderModules(DrawContext context, int x, int y, ModuleCategory category) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Module> modules = category.getModules();
        int currentY = y;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean selected = (i == selectedModuleIndex) && inModuleList;
            boolean enabled = module.isEnabled();
            boolean hasSettings = module.hasSettings();

            int bgColor = selected ? COLOR_SELECTED : COLOR_BACKGROUND;
            context.fill(x, currentY, x + MODULE_WIDTH, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + MODULE_WIDTH, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + MODULE_WIDTH, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            int indicatorColor = enabled ? COLOR_ENABLED : COLOR_DISABLED;
            context.fill(x + 2, currentY + 4, x + 6, currentY + ITEM_HEIGHT - 4, indicatorColor);

            String status = enabled ? "ON" : "OFF";
            String settingsIndicator = hasSettings ? " >" : "";
            String rightText = status + settingsIndicator;
            int rightTextWidth = client.textRenderer.getWidth(rightText);
            int statusX = x + MODULE_WIDTH - rightTextWidth - 4;

            int textX = x + 10;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = statusX - textX - 4;

            String text = module.getDisplayName();
            int textWidth = client.textRenderer.getWidth(text);
            if (textWidth > availableWidth) {
                while (textWidth > availableWidth - client.textRenderer.getWidth("...") && !text.isEmpty()) {
                    text = text.substring(0, text.length() - 1);
                    textWidth = client.textRenderer.getWidth(text);
                }
                text = text + "...";
            }

            context.drawText(client.textRenderer, text, textX, textY, COLOR_TEXT, false);

            context.drawText(client.textRenderer, status, statusX, textY,
                    enabled ? COLOR_ENABLED : COLOR_DISABLED, false);

            if (hasSettings) {
                int arrowX = statusX + client.textRenderer.getWidth(status);
                context.drawText(client.textRenderer, settingsIndicator, arrowX, textY, 0xFF888888, false);
            }

            currentY += ITEM_HEIGHT;
        }
    }

    private void renderSettings(DrawContext context, int x, int y) {
        if (currentSettingsModule == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        List<String> settings = currentSettingsModule.getSettingsDisplay();
        int currentY = y;

        context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT, 0xFF303030);
        context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + 1, COLOR_BORDER);
        context.fill(x, currentY + ITEM_HEIGHT, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

        String title = currentSettingsModule.getDisplayName() + " Settings";
        int titleWidth = client.textRenderer.getWidth(title);
        int titleX = x + (SETTINGS_WIDTH - titleWidth) / 2;
        int titleY = currentY + (ITEM_HEIGHT - 8) / 2;
        context.drawText(client.textRenderer, title, titleX, titleY, COLOR_TEXT, false);

        currentY += ITEM_HEIGHT;

        for (int i = 0; i < settings.size(); i++) {
            String setting = settings.get(i);
            boolean selected = (i == selectedSettingIndex);

            int bgColor = selected ? COLOR_SELECTED : COLOR_BACKGROUND;
            context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            int textX = x + PADDING + 2;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = SETTINGS_WIDTH - (PADDING + 4);

            String displayText = setting;
            int textWidth = client.textRenderer.getWidth(displayText);
            if (textWidth > availableWidth) {
                while (textWidth > availableWidth - client.textRenderer.getWidth("...") && !displayText.isEmpty()) {
                    displayText = displayText.substring(0, displayText.length() - 1);
                    textWidth = client.textRenderer.getWidth(displayText);
                }
                displayText = displayText + "...";
            }

            context.drawText(client.textRenderer, displayText, textX, textY, COLOR_TEXT, false);

            currentY += ITEM_HEIGHT;
        }

        if (settings.isEmpty()) {
            context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT, COLOR_BACKGROUND);
            context.fill(x, currentY, x + SETTINGS_WIDTH, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + SETTINGS_WIDTH, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            String noSettings = "No settings available";
            int noSettingsWidth = client.textRenderer.getWidth(noSettings);
            int noSettingsX = x + (SETTINGS_WIDTH - noSettingsWidth) / 2;
            int noSettingsY = currentY + (ITEM_HEIGHT - 8) / 2;
            context.drawText(client.textRenderer, noSettings, noSettingsX, noSettingsY, 0xFF888888, false);
        }
    }

    private void renderInstructions(DrawContext context, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        String[] instructions = {
                "Up/Down: Navigate | Right: Enter Modules | Left: Back to Categories",
                "Enter: Toggle Module | Right ALT: Close"
        };

        int y = screenHeight - 30;
        int x = 10;

        for (String instruction : instructions) {
            context.drawText(client.textRenderer, instruction, x, y, 0xFF808080, false);
            y += 10;
        }
    }
}

