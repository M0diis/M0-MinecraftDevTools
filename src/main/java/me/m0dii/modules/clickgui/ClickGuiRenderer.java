package me.m0dii.modules.clickgui;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Module;
import me.m0dii.modules.Toggleable;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

public class ClickGuiRenderer implements Toggleable {

    private static final ModuleCategory[] categories = ModuleRegistry.getCategories();

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

    @Getter
    @Setter
    private boolean enabled = false;
    private int selectedCategoryIndex = 0;
    private int selectedModuleIndex = 0;
    private boolean inModuleList = false;
    private boolean inSettingsView = false;
    private int selectedSettingIndex = 0;
    private Module currentSettingsModule = null;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean enterPressed = false;

    @Getter
    @Setter
    private boolean wasdNavigation = false;

    public ClickGuiRenderer() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (!enabled || client.player == null) {
            return;
        }

        var window = client.getWindow();

        boolean upNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_UP);
        boolean downNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_DOWN);
        boolean leftNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT);
        boolean rightNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT);
        boolean enterNow = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_ENTER);

        if (wasdNavigation) {
            upNow = upNow || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_W);
            downNow = downNow || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_S);
            leftNow = leftNow || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_A);
            rightNow = rightNow || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_D);
        }

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
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Apply text scaling
        float scale = (float) ModConfig.clickGuiTextScale;
        if (Float.isNaN(scale) || scale <= 0f) {
            scale = 0.85f;
        }
        scale = Math.clamp(scale, 0.5f, 3.0f);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        // Convert screen coordinates to scaled coordinates
        int scaledStartX = Math.round(START_X / scale);
        int scaledStartY = Math.round(START_Y / scale);
        int scaledScreenWidth = Math.round(screenWidth / scale);
        int scaledScreenHeight = Math.round(screenHeight / scale);

        int categoryWidth = CATEGORY_WIDTH;
        int moduleWidth = MODULE_WIDTH;
        int settingsWidth = SETTINGS_WIDTH;

        int neededWidth = categoryWidth + MODULE_WIDTH + 10;
        if (inSettingsView && currentSettingsModule != null) {
            neededWidth += settingsWidth + 10;
        }
        int availableWidth = Math.max(220, scaledScreenWidth - scaledStartX - 8);

        if (neededWidth > availableWidth) {
            int deficit = neededWidth - availableWidth;

            if (inSettingsView && currentSettingsModule != null) {
                int minSettings = 140;
                int reduce = Math.min(deficit, settingsWidth - minSettings);
                settingsWidth -= reduce;
                deficit -= reduce;
            }

            int minModule = 110;
            int reduceModule = Math.min(deficit, moduleWidth - minModule);
            moduleWidth -= reduceModule;
            deficit -= reduceModule;

            int minCategory = 80;
            int reduceCategory = Math.min(deficit, categoryWidth - minCategory);
            categoryWidth -= reduceCategory;
        }

        renderCategories(context, scaledStartX, scaledStartY, categoryWidth);

        if (categories.length > 0 && selectedCategoryIndex < categories.length) {
            ModuleCategory category = categories[selectedCategoryIndex];
            int modulesX = scaledStartX + categoryWidth + 10;
            renderModules(context, modulesX, scaledStartY, moduleWidth, category);

            if (inSettingsView && currentSettingsModule != null) {
                int settingsX = modulesX + moduleWidth + 10;
                renderSettings(context, settingsX, scaledStartY, settingsWidth);
            }
        }

        renderInstructions(context, scaledStartX, scaledScreenHeight, scaledScreenWidth);

        context.getMatrices().popMatrix();
    }

    private void renderCategories(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        int currentY = y;

        for (int i = 0; i < categories.length; i++) {
            ModuleCategory category = categories[i];
            boolean selected = (i == selectedCategoryIndex);
            boolean categoryFocused = selected && !inModuleList;

            int bgColor = categoryFocused ? COLOR_SELECTED : (selected ? 0x80404040 : COLOR_BACKGROUND);
            context.fill(x, currentY, x + width, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + width, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + width, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            String count = "(" + category.getModules().size() + ")";
            int countWidth = client.textRenderer.getWidth(count);
            int countX = x + width - countWidth - 4;

            int textX = x + PADDING + 2;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = countX - textX - 4;

            String text = fitText(client.textRenderer, category.getName(), availableWidth);

            context.drawText(client.textRenderer, text, textX, textY, COLOR_TEXT, false);
            context.drawText(client.textRenderer, count, countX, textY, COLOR_TEXT, false);

            currentY += ITEM_HEIGHT;
        }
    }

    private void renderModules(DrawContext context, int x, int y, int width, ModuleCategory category) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Module> modules = category.getModules()
                .stream()
                .filter(Objects::nonNull)
                .toList();

        int currentY = y;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean selected = (i == selectedModuleIndex) && inModuleList;
            boolean enabled = module.isEnabled();
            boolean hasSettings = module.hasSettings();

            int bgColor = selected ? COLOR_SELECTED : COLOR_BACKGROUND;
            context.fill(x, currentY, x + width, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + width, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + width, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            int indicatorColor = enabled ? COLOR_ENABLED : COLOR_DISABLED;
            context.fill(x + 2, currentY + 4, x + 6, currentY + ITEM_HEIGHT - 4, indicatorColor);

            String status = enabled ? "ON" : "OFF";
            String settingsIndicator = hasSettings ? " >" : "";
            String rightText = status + settingsIndicator;
            int rightTextWidth = client.textRenderer.getWidth(rightText);
            int statusX = x + width - rightTextWidth - 4;

            int textX = x + 10;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = statusX - textX - 4;

            String text = fitText(client.textRenderer, module.getDisplayName(), availableWidth);

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

    private void renderSettings(DrawContext context, int x, int y, int width) {
        if (currentSettingsModule == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        List<String> settings = currentSettingsModule.getSettingsDisplay();
        int currentY = y;

        context.fill(x, currentY, x + width, currentY + ITEM_HEIGHT, 0xFF303030);
        context.fill(x, currentY, x + width, currentY + 1, COLOR_BORDER);
        context.fill(x, currentY + ITEM_HEIGHT, x + width, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

        String title = fitText(client.textRenderer, currentSettingsModule.getDisplayName() + " Settings", width - 8);
        int titleWidth = client.textRenderer.getWidth(title);
        int titleX = x + (width - titleWidth) / 2;
        int titleY = currentY + (ITEM_HEIGHT - 8) / 2;
        context.drawText(client.textRenderer, title, titleX, titleY, COLOR_TEXT, false);

        currentY += ITEM_HEIGHT;

        for (int i = 0; i < settings.size(); i++) {
            String setting = settings.get(i);
            boolean selected = (i == selectedSettingIndex);

            int bgColor = selected ? COLOR_SELECTED : COLOR_BACKGROUND;
            context.fill(x, currentY, x + width, currentY + ITEM_HEIGHT, bgColor);
            context.fill(x, currentY, x + width, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + width, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            int textX = x + PADDING + 2;
            int textY = currentY + (ITEM_HEIGHT - 8) / 2;
            int availableWidth = width - (PADDING + 6);

            String displayText = fitText(client.textRenderer, setting, availableWidth);

            context.drawText(client.textRenderer, displayText, textX, textY, COLOR_TEXT, false);

            currentY += ITEM_HEIGHT;
        }

        if (settings.isEmpty()) {
            context.fill(x, currentY, x + width, currentY + ITEM_HEIGHT, COLOR_BACKGROUND);
            context.fill(x, currentY, x + width, currentY + 1, COLOR_BORDER);
            context.fill(x, currentY + ITEM_HEIGHT, x + width, currentY + ITEM_HEIGHT + 1, COLOR_BORDER);

            String noSettings = fitText(client.textRenderer, "No settings available", width - 8);
            int noSettingsWidth = client.textRenderer.getWidth(noSettings);
            int noSettingsX = x + (width - noSettingsWidth) / 2;
            int noSettingsY = currentY + (ITEM_HEIGHT - 8) / 2;
            context.drawText(client.textRenderer, noSettings, noSettingsX, noSettingsY, 0xFF888888, false);
        }
    }

    private void renderInstructions(DrawContext context, int x, int screenHeight, int screenWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        String[] instructions = {
                "Up/Down: Navigate | Right: Enter Modules | Left: Back to Categories",
                "Enter: Toggle Module | Right ALT: Close"
        };

        int y = screenHeight - 30;
        int maxWidth = Math.max(100, screenWidth - x - 8);

        for (String instruction : instructions) {
            context.drawText(client.textRenderer, fitText(client.textRenderer, instruction, maxWidth), x, y, 0xFF808080, false);
            y += 10;
        }
    }

    private static String fitText(TextRenderer textRenderer, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        final String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        String result = text;
        while (!result.isEmpty() && textRenderer.getWidth(result) + ellipsisWidth > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? "" : result + ellipsis;
    }
}

