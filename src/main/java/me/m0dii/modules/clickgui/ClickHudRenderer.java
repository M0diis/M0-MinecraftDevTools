package me.m0dii.modules.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.Setter;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.modules.Module;
import me.m0dii.modules.Toggleable;
import me.m0dii.modules.state.ModuleStateStore;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Keyboard-driven module menu drawn over the HUD (default key: `), so you can keep playing while it is open.
 * Three columns: categories, modules, settings. Arrows move, Right opens, Left goes back, Enter toggles or applies.
 */
public class ClickHudRenderer implements Toggleable {

    private static final ModuleCategory[] BASE_CATEGORIES = ModuleRegistry.getCategories();
    static final String FAVORITES_NAME = "★ Favorites";

    /** Categories as shown: a Favorites category first when any module is starred. */
    private static ModuleCategory[] visibleCategories() {
        List<Module> favorites = ModuleStateStore.favoriteModules();
        if (favorites.isEmpty()) {
            return BASE_CATEGORIES;
        }
        ModuleCategory favoriteCategory = new ModuleCategory(FAVORITES_NAME);
        favorites.forEach(favoriteCategory::addModule);
        ModuleCategory[] result = new ModuleCategory[BASE_CATEGORIES.length + 1];
        result[0] = favoriteCategory;
        System.arraycopy(BASE_CATEGORIES, 0, result, 1, BASE_CATEGORIES.length);
        return result;
    }

    private static final int CATEGORY_WIDTH = 122;
    private static final int MODULE_WIDTH = 170;
    private static final int SETTINGS_WIDTH = 210;
    private static final int ROW_H = 16;
    private static final int HEADER_H = 18;
    private static final int GAP = 6;
    private static final int START_X = 8;
    private static final int START_Y = 8;
    /** Room kept free at the bottom of the screen for the hotbar and chat. */
    private static final int BOTTOM_CLEARANCE = 60;
    private static final int PANEL_BG = 0xE6171B22;
    private static final int SWITCH_W = 16;
    private static final int SWITCH_H = 8;

    /** Held arrow keys repeat after this many ticks, then every {@link #REPEAT_EVERY} ticks. */
    private static final int REPEAT_DELAY = 7;
    private static final int REPEAT_EVERY = 2;
    private static final long FLASH_MS = 350;

    @Getter
    @Setter
    private boolean enabled = false;
    private int selectedCategoryIndex = 0;
    private int selectedModuleIndex = 0;
    private boolean inModuleList = false;
    private boolean inSettingsView = false;
    private int selectedSettingIndex = 0;
    private Module currentSettingsModule = null;

    private int categoryScroll;
    private int moduleScroll;
    private int settingScroll;
    /** Rows that fit in a column; updated while rendering, used while handling keys. */
    private int visibleRows = 12;
    private final Map<String, Integer> lastModuleIndex = new HashMap<>();

    private long flashUntil;
    private int flashRow = -1;

    private final KeyRepeat up = new KeyRepeat();
    private final KeyRepeat down = new KeyRepeat();
    private final KeyRepeat pageUp = new KeyRepeat();
    private final KeyRepeat pageDown = new KeyRepeat();
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean enterPressed = false;
    private boolean favoritePressed = false;
    private boolean homePressed = false;
    private boolean endPressed = false;

    @Getter
    @Setter
    private boolean wasdNavigation = false;

    public ClickHudRenderer() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    /** Edge-triggered key with auto-repeat while held. */
    private static final class KeyRepeat {
        private int heldTicks;

        boolean fire(boolean down) {
            if (!down) {
                heldTicks = 0;
                return false;
            }
            heldTicks++;
            return heldTicks == 1 || (heldTicks > REPEAT_DELAY && (heldTicks - REPEAT_DELAY) % REPEAT_EVERY == 0);
        }
    }

    private void onClientTick(Minecraft client) {
        if (!enabled || client.player == null) {
            return;
        }
        // A screen (chat, inventory, ...) owns the keyboard; don't navigate behind it.
        if (client.gui.screen() != null) {
            return;
        }

        boolean upNow = InputConstants.isKeyDown(InputConstants.KEY_UP);
        boolean downNow = InputConstants.isKeyDown(InputConstants.KEY_DOWN);
        boolean leftNow = InputConstants.isKeyDown(InputConstants.KEY_LEFT);
        boolean rightNow = InputConstants.isKeyDown(InputConstants.KEY_RIGHT);
        boolean enterNow = InputConstants.isKeyDown(InputConstants.KEY_RETURN) || InputConstants.isKeyDown(InputConstants.KEY_NUMPADENTER);
        if (wasdNavigation) {
            upNow |= InputConstants.isKeyDown(InputConstants.KEY_W);
            downNow |= InputConstants.isKeyDown(InputConstants.KEY_S);
            leftNow |= InputConstants.isKeyDown(InputConstants.KEY_A);
            rightNow |= InputConstants.isKeyDown(InputConstants.KEY_D);
        }

        boolean moveUp = up.fire(upNow);
        boolean moveDown = down.fire(downNow);
        boolean movePageUp = pageUp.fire(InputConstants.isKeyDown(InputConstants.KEY_PAGEUP));
        boolean movePageDown = pageDown.fire(InputConstants.isKeyDown(InputConstants.KEY_PAGEDOWN));
        boolean homeNow = InputConstants.isKeyDown(InputConstants.KEY_HOME);
        boolean endNow = InputConstants.isKeyDown(InputConstants.KEY_END);
        boolean goHome = homeNow && !homePressed;
        boolean goEnd = endNow && !endPressed;
        boolean goLeft = leftNow && !leftPressed;
        boolean goRight = rightNow && !rightPressed;
        boolean doEnter = enterNow && !enterPressed;
        boolean favoriteNow = InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
        boolean doFavorite = favoriteNow && !favoritePressed;
        homePressed = homeNow;
        endPressed = endNow;
        leftPressed = leftNow;
        rightPressed = rightNow;
        enterPressed = enterNow;
        favoritePressed = favoriteNow;

        int step = moveDown ? 1 : moveUp ? -1 : movePageDown ? visibleRows : movePageUp ? -visibleRows : 0;

        ModuleCategory[] categories = visibleCategories();
        if (categories.length == 0) {
            return;
        }
        selectedCategoryIndex = Math.clamp(selectedCategoryIndex, 0, categories.length - 1);
        ModuleCategory currentCategory = categories[selectedCategoryIndex];
        List<Module> modules = currentCategory.getModules();

        if (inSettingsView && currentSettingsModule != null) {
            List<String> settings = currentSettingsModule.getSettingsDisplay();
            int last = Math.max(0, settings.size() - 1);
            selectedSettingIndex = move(selectedSettingIndex, step, last, moveUp || moveDown, goHome, goEnd);
            if (goLeft) {
                inSettingsView = false;
                currentSettingsModule = null;
                selectedSettingIndex = 0;
                settingScroll = 0;
            } else if (doEnter && !settings.isEmpty()) {
                currentSettingsModule.onSettingSelected(selectedSettingIndex);
                ModuleStateStore.onSettingsChanged(currentSettingsModule);
                flash(selectedSettingIndex);
            }
        } else if (inModuleList) {
            if (!modules.isEmpty()) {
                selectedModuleIndex = Math.clamp(selectedModuleIndex, 0, modules.size() - 1);
            }
            if (doFavorite && !modules.isEmpty()) {
                Module module = modules.get(selectedModuleIndex);
                boolean starred = ModuleStateStore.toggleFavorite(module);
                client.player.sendOverlayMessage(Component.literal(module.getDisplayName()
                        + (starred ? " added to Favorites" : " removed from Favorites")));
            }
            selectedModuleIndex = move(selectedModuleIndex, step, Math.max(0, modules.size() - 1), moveUp || moveDown, goHome, goEnd);
            lastModuleIndex.put(currentCategory.getName(), selectedModuleIndex);

            if (goLeft) {
                inModuleList = false;
            } else if (goRight && !modules.isEmpty()) {
                Module module = modules.get(selectedModuleIndex);
                if (module.hasSettings()) {
                    inSettingsView = true;
                    currentSettingsModule = module;
                    selectedSettingIndex = 0;
                    settingScroll = 0;
                }
            } else if (doEnter && !modules.isEmpty()) {
                modules.get(selectedModuleIndex).toggleEnabled();
                flash(selectedModuleIndex);
            }
        } else {
            int before = selectedCategoryIndex;
            selectedCategoryIndex = move(selectedCategoryIndex, step, categories.length - 1, moveUp || moveDown, goHome, goEnd);
            if (before != selectedCategoryIndex) {
                selectedModuleIndex = lastModuleIndex.getOrDefault(categories[selectedCategoryIndex].getName(), 0);
                moduleScroll = 0;
            }
            if (goRight || doEnter) {
                inModuleList = true;
            }
        }
    }

    /** The module highlighted in the module list or open in the settings column, if any. */
    public Module focusedModule() {
        if (!enabled) {
            return null;
        }
        if (inSettingsView && currentSettingsModule != null) {
            return currentSettingsModule;
        }
        ModuleCategory[] categories = visibleCategories();
        if (!inModuleList || selectedCategoryIndex >= categories.length) {
            return null;
        }
        List<Module> modules = categories[selectedCategoryIndex].getModules();
        return selectedModuleIndex < modules.size() ? modules.get(selectedModuleIndex) : null;
    }

    /** Moves a selection; single steps wrap around, page and Home/End jumps stop at the ends. */
    private static int move(int index, int step, int last, boolean wrap, boolean home, boolean end) {
        if (home) {
            return 0;
        }
        if (end) {
            return last;
        }
        if (step == 0 || last < 0) {
            return Math.clamp(index, 0, Math.max(0, last));
        }
        int next = index + step;
        if (wrap) {
            return next < 0 ? last : next > last ? 0 : next;
        }
        return Math.clamp(next, 0, last);
    }

    private void flash(int row) {
        flashRow = row;
        flashUntil = System.currentTimeMillis() + FLASH_MS;
    }

    private boolean flashing(int row) {
        return row == flashRow && System.currentTimeMillis() < flashUntil;
    }

    public void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (!enabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        float scale = (float) ModConfig.clickGuiTextScale;
        if (Float.isNaN(scale) || scale <= 0f) {
            scale = 0.85f;
        }
        scale = Math.clamp(scale, 0.5f, 3.0f);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        int x0 = Math.round(START_X / scale);
        int y0 = Math.round(START_Y / scale);
        int screenW = Math.round(client.getWindow().getGuiScaledWidth() / scale);
        int screenH = Math.round(client.getWindow().getGuiScaledHeight() / scale);

        boolean showSettings = inSettingsView && currentSettingsModule != null;
        int categoryWidth = CATEGORY_WIDTH;
        int moduleWidth = MODULE_WIDTH;
        int settingsWidth = SETTINGS_WIDTH;
        int needed = categoryWidth + GAP + moduleWidth + (showSettings ? GAP + settingsWidth : 0);
        int available = Math.max(240, screenW - x0 - 8);
        if (needed > available) {
            int deficit = needed - available;
            if (showSettings) {
                int reduce = Math.min(deficit, settingsWidth - 140);
                settingsWidth -= reduce;
                deficit -= reduce;
            }
            int reduce = Math.min(deficit, moduleWidth - 120);
            moduleWidth -= reduce;
            deficit -= reduce;
            categoryWidth -= Math.min(deficit, categoryWidth - 84);
        }

        visibleRows = Math.max(3, (screenH - y0 - HEADER_H - BOTTOM_CLEARANCE) / ROW_H);

        Font font = client.font;
        ModuleCategory[] categories = visibleCategories();
        int tallest = renderCategories(context, font, x0, y0, categoryWidth, categories);

        if (categories.length > 0 && selectedCategoryIndex < categories.length) {
            ModuleCategory category = categories[selectedCategoryIndex];
            int modulesX = x0 + categoryWidth + GAP;
            tallest = Math.max(tallest, renderModules(context, font, modulesX, y0, moduleWidth, category));
            if (showSettings) {
                int settingsX = modulesX + moduleWidth + GAP;
                tallest = Math.max(tallest, renderSettings(context, font, settingsX, y0, settingsWidth));
            }
        }

        renderHints(context, font, x0, y0 + tallest + 4, screenW - x0 - 8);
        context.pose().popMatrix();
    }

    // ---- panels -------------------------------------------------------------------------------------------------

    /** Panel background and header; returns the y where rows start. */
    private static int panel(GuiGraphicsExtractor g, Font font, int x, int y, int w, int rows, String title, String right, boolean focused) {
        int h = HEADER_H + Math.max(1, rows) * ROW_H + 2;
        ThemeDraw.box(g, x, y, w, h, PANEL_BG);
        ThemeDraw.outline(g, x, y, w, h, focused ? Theme.BORDER_STRONG : Theme.BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + HEADER_H, Theme.SURFACE_RAISED);
        g.fill(x + 1, y + HEADER_H - 1, x + w - 1, y + HEADER_H, focused ? Theme.ACCENT : Theme.DIVIDER);
        int rightW = right == null ? 0 : font.width(right) + 6;
        g.text(font, ThemeDraw.fit(font, title, w - 12 - rightW), x + 6, y + 5, focused ? Theme.TEXT : Theme.TEXT_MUTED, false);
        if (right != null) {
            g.text(font, right, x + w - rightW, y + 5, Theme.TEXT_DIM, false);
        }
        return y + HEADER_H + 1;
    }

    private static int panelHeight(int rows) {
        return HEADER_H + Math.max(1, rows) * ROW_H + 2;
    }

    /** Row highlight: soft fill plus an accent bar on the left when the column has focus. */
    private static void rowHighlight(GuiGraphicsExtractor g, int x, int y, int w, boolean selected, boolean focused, boolean flash) {
        if (flash) {
            g.fill(x + 1, y, x + w - 1, y + ROW_H, Theme.ACCENT_SOFT);
        }
        if (!selected) {
            return;
        }
        g.fill(x + 1, y, x + w - 1, y + ROW_H, focused ? Theme.SURFACE_PRESSED : Theme.SURFACE_HOVER);
        g.fill(x + 1, y + 2, x + 3, y + ROW_H - 2, focused ? Theme.ACCENT : Theme.BORDER_STRONG);
    }

    private static int scrollFor(int selected, int scroll, int total, int rows) {
        if (selected < scroll) {
            scroll = selected;
        } else if (selected >= scroll + rows) {
            scroll = selected - rows + 1;
        }
        return Math.clamp(scroll, 0, Math.max(0, total - rows));
    }

    private static void scrollbar(GuiGraphicsExtractor g, int x, int y, int rows, int total, int scroll) {
        if (total <= rows) {
            return;
        }
        int trackH = rows * ROW_H;
        int thumbH = Math.max(8, trackH * rows / total);
        int thumbY = y + (trackH - thumbH) * scroll / Math.max(1, total - rows);
        g.fill(x, y, x + 2, y + trackH, Theme.DIVIDER);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, Theme.BORDER_STRONG);
    }

    private int renderCategories(GuiGraphicsExtractor g, Font font, int x, int y, int width, ModuleCategory[] categories) {
        int rows = Math.min(categories.length, visibleRows);
        categoryScroll = scrollFor(selectedCategoryIndex, categoryScroll, categories.length, rows);
        boolean focused = !inModuleList && !inSettingsView;
        int rowY = panel(g, font, x, y, width, rows, "M0 DevTools", null, focused);

        for (int i = 0; i < rows; i++) {
            int index = categoryScroll + i;
            ModuleCategory category = categories[index];
            int ry = rowY + i * ROW_H;
            boolean selected = index == selectedCategoryIndex;
            rowHighlight(g, x, ry, width, selected, focused, false);

            long on = category.getModules().stream().filter(Objects::nonNull).filter(Module::isEnabled).count();
            String count = on + "/" + category.getModules().size();
            int countW = font.width(count);
            g.text(font, count, x + width - countW - 7, ry + 4, on > 0 ? Theme.ACCENT_TEXT : Theme.TEXT_DIM, false);
            g.text(font, ThemeDraw.fit(font, category.getName(), width - countW - 20), x + 8, ry + 4,
                    selected ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }
        scrollbar(g, x + width - 3, rowY, rows, categories.length, categoryScroll);
        return panelHeight(rows);
    }

    private int renderModules(GuiGraphicsExtractor g, Font font, int x, int y, int width, ModuleCategory category) {
        List<Module> modules = category.getModules().stream().filter(Objects::nonNull).toList();
        int rows = Math.min(Math.max(1, modules.size()), visibleRows);
        moduleScroll = scrollFor(selectedModuleIndex, moduleScroll, modules.size(), rows);
        boolean focused = inModuleList && !inSettingsView;
        int rowY = panel(g, font, x, y, width, rows, category.getName(), modules.size() + "", focused);

        if (modules.isEmpty()) {
            g.text(font, "No modules", x + 8, rowY + 4, Theme.TEXT_DIM, false);
        }
        for (int i = 0; i < rows && moduleScroll + i < modules.size(); i++) {
            int index = moduleScroll + i;
            Module module = modules.get(index);
            int ry = rowY + i * ROW_H;
            boolean selected = inModuleList && index == selectedModuleIndex;
            rowHighlight(g, x, ry, width, selected, focused, flashing(index) && !inSettingsView);

            boolean on = module.isEnabled();
            int right = x + width - 7;
            if (module.hasSettings()) {
                g.text(font, "›", right - font.width("›"), ry + 4, selected ? Theme.TEXT_MUTED : Theme.TEXT_DIM, false);
                right -= font.width("›") + 5;
            }
            ThemeDraw.toggle(g, right - SWITCH_W, ry + (ROW_H - SWITCH_H) / 2, SWITCH_W, SWITCH_H, on, true);
            right -= SWITCH_W + 6;

            int textX = x + 8;
            if (ModuleStateStore.isFavorite(module)) {
                g.text(font, "★", textX, ry + 4, Theme.WARNING, false);
                textX += font.width("★") + 3;
            }
            int nameColor = on ? Theme.TEXT : (selected ? Theme.TEXT_MUTED : 0xFF7A8396);
            g.text(font, ThemeDraw.fit(font, module.getDisplayName(), right - textX), textX, ry + 4, nameColor, false);
        }
        scrollbar(g, x + width - 3, rowY, rows, modules.size(), moduleScroll);
        return panelHeight(rows);
    }

    private int renderSettings(GuiGraphicsExtractor g, Font font, int x, int y, int width) {
        Module module = currentSettingsModule;
        List<String> settings = module.getSettingsDisplay();
        int rows = Math.min(Math.max(1, settings.size()), visibleRows);
        selectedSettingIndex = Math.clamp(selectedSettingIndex, 0, Math.max(0, settings.size() - 1));
        settingScroll = scrollFor(selectedSettingIndex, settingScroll, settings.size(), rows);
        int rowY = panel(g, font, x, y, width, rows, module.getDisplayName(), module.isEnabled() ? "ON" : "OFF", true);

        if (settings.isEmpty()) {
            g.text(font, "No settings", x + 8, rowY + 4, Theme.TEXT_DIM, false);
        }
        for (int i = 0; i < rows && settingScroll + i < settings.size(); i++) {
            int index = settingScroll + i;
            SettingLine line = SettingLine.parse(settings.get(index));
            int ry = rowY + i * ROW_H;
            boolean selected = index == selectedSettingIndex;
            rowHighlight(g, x, ry, width, selected, true, flashing(index));

            int right = x + width - 7;
            if (line.kind() == SettingLine.Kind.ACTION) {
                g.text(font, "↵", right - font.width("↵"), ry + 4, selected ? Theme.ACCENT_TEXT : Theme.TEXT_DIM, false);
                g.text(font, ThemeDraw.fit(font, line.label(), width - 28), x + 8, ry + 4, selected ? Theme.TEXT : Theme.TEXT_MUTED, false);
                continue;
            }
            int valueRoom = Math.max(30, (width - 16) / 2);
            if (line.isToggle()) {
                ThemeDraw.toggle(g, right - SWITCH_W, ry + (ROW_H - SWITCH_H) / 2, SWITCH_W, SWITCH_H, line.kind() == SettingLine.Kind.ON, true);
                valueRoom = SWITCH_W;
            } else {
                String value = ThemeDraw.fit(font, line.value(), valueRoom);
                valueRoom = font.width(value);
                g.text(font, value, right - valueRoom, ry + 4, line.valueColor(), false);
            }
            g.text(font, ThemeDraw.fit(font, line.label(), right - valueRoom - 8 - (x + 8)), x + 8, ry + 4,
                    selected ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }
        scrollbar(g, x + width - 3, rowY, rows, settings.size(), settingScroll);
        return panelHeight(rows);
    }

    /** Key hints as keycap chips under the panels. */
    private void renderHints(GuiGraphicsExtractor g, Font font, int x, int y, int maxWidth) {
        String[][] hints;
        if (inSettingsView) {
            hints = new String[][]{{"↑↓", "Select"}, {"Enter", "Apply"}, {"←", "Back"}};
        } else if (inModuleList) {
            hints = new String[][]{{"↑↓", "Select"}, {"Enter", "Toggle"}, {"→", "Settings"}, {"←", "Back"}, {"RShift", "Favorite"}, {ModuleBrowserScreen.openKeyName(), "Mouse view"}};
        } else {
            hints = new String[][]{{"↑↓", "Select"}, {"→", "Open"}, {"PgUp/PgDn", "Jump"}, {ModuleBrowserScreen.openKeyName(), "Search"}};
        }
        int cx = x + 4;
        int shown = 0;
        int[] widths = new int[hints.length];
        for (int i = 0; i < hints.length; i++) {
            widths[i] = font.width(hints[i][0]) + 6 + 3 + font.width(hints[i][1]) + 8;
            if (cx + widths[i] - x > maxWidth) {
                break;
            }
            cx += widths[i];
            shown++;
        }
        if (shown == 0) {
            return;
        }
        // Solid strip so the hints stay readable over chat and the world.
        ThemeDraw.box(g, x, y, cx - x - 4, 16, PANEL_BG);
        ThemeDraw.outline(g, x, y, cx - x - 4, 16, Theme.BORDER);
        cx = x + 4;
        for (int i = 0; i < shown; i++) {
            String key = hints[i][0];
            int keyW = font.width(key) + 6;
            ThemeDraw.box(g, cx, y + 2, keyW, 12, Theme.SURFACE_PRESSED);
            g.text(font, key, cx + 3, y + 4, Theme.TEXT, false);
            g.text(font, hints[i][1], cx + keyW + 3, y + 4, Theme.TEXT_MUTED, false);
            cx += widths[i];
        }
    }
}
