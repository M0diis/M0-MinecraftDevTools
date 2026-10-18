package me.m0dii.modules.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.modules.Module;
import me.m0dii.modules.NumericSetting;
import me.m0dii.modules.state.ModuleStateStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Searchable list of every module with a mouse-friendly settings panel.
 * Type to filter; click the switch (or press Enter) to toggle, click the star to favourite, click a row to show its
 * settings on the right and click a setting to change it. Favourites are listed first.
 */
public class ModuleBrowserScreen extends Screen implements ThemedScreen {
    private static final int ROW_H = 18;
    private static final int TOP = 48;
    private static final int STAR_W = 16;
    private static final int SWITCH_W = 20;
    private static final int SWITCH_H = 10;
    /** Below this width the settings panel is hidden and the list uses the whole screen. */
    private static final int SPLIT_MIN_WIDTH = 520;

    /** The "open module browser" key, so other UI can show what it is bound to. */
    public static net.minecraft.client.KeyMapping openKey;

    private final Screen parent;
    private final Map<Module, String> categoryOf = new HashMap<>();
    private EditBox searchBox;
    private List<Module> shown = List.of();
    private String lastQuery = null;
    private int selected = 0;
    private int scroll = 0;
    private int settingsScroll = 0;
    private @Nullable Module initialModule;
    private long flashUntil;
    private int flashSetting = -1;
    /** Slider being dragged, and the number setting whose value is being typed. */
    private @Nullable NumericSetting dragging;
    private @Nullable NumericSetting editing;
    private String editText = "";
    private static final int VALUE_BOX_W = 46;

    /** One visible settings row: its index in getSettingsDisplay() and, for number settings, the slider. */
    private record SettingRow(int index, SettingLine line, @Nullable NumericSetting numeric) {
    }

    public ModuleBrowserScreen(Screen parent) {
        this(parent, null);
    }

    /** @param focus module to select first (for example the one highlighted in ClickHUD) */
    public ModuleBrowserScreen(Screen parent, @Nullable Module focus) {
        super(Component.literal("Modules"));
        this.parent = parent;
        this.initialModule = focus;
        for (ModuleCategory category : ModuleRegistry.getCategories()) {
            for (Module module : category.getModules()) {
                if (module != null) {
                    categoryOf.putIfAbsent(module, category.getName());
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(this.font, listLeft(), 22, listRight() - listLeft(), 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search modules (name, id or category)"));
        if (lastQuery != null) {
            searchBox.setValue(lastQuery);
        }
        this.addRenderableWidget(searchBox);
        this.setFocused(searchBox);
        refresh(true);
        if (initialModule != null) {
            int index = shown.indexOf(initialModule);
            if (index >= 0) {
                selected = index;
                scroll = Math.max(0, index - visibleRows() / 2);
            }
            initialModule = null;
        }
    }

    private void refresh(boolean force) {
        String query = searchBox == null ? "" : searchBox.getValue();
        if (!force && query.equals(lastQuery)) {
            return;
        }
        Module previous = selectedModule();
        lastQuery = query;
        shown = ModuleSearch.rank(ModuleRegistry.allModules(), query,
                module -> ModuleStateStore.isFavorite(module),
                module -> categoryOf.getOrDefault(module, ""));
        int keep = previous == null ? -1 : shown.indexOf(previous);
        selected = keep >= 0 ? keep : 0;
        scroll = 0;
        settingsScroll = 0;
    }

    private @Nullable Module selectedModule() {
        return selected >= 0 && selected < shown.size() ? shown.get(selected) : null;
    }

    private boolean split() {
        return this.width >= SPLIT_MIN_WIDTH;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - TOP - 28) / ROW_H);
    }

    private int listLeft() {
        return split() ? 12 : Math.max(10, this.width / 2 - 210);
    }

    private int listRight() {
        return split() ? Math.min(this.width / 2 + 40, 12 + 360) : Math.min(this.width - 10, this.width / 2 + 210);
    }

    private int settingsLeft() {
        return listRight() + 10;
    }

    private int settingsRight() {
        return this.width - 12;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        refresh(false);
        this.extractBackground(context, mouseX, mouseY, delta);
        context.text(this.font, "Modules", listLeft(), 8, Theme.TEXT, false);
        long enabled = shown.stream().filter(Module::isEnabled).count();
        String count = enabled + " on · " + shown.size() + " shown";
        context.text(this.font, count, listRight() - this.font.width(count), 8, Theme.TEXT_DIM, false);

        renderList(context, mouseX, mouseY);
        if (split()) {
            renderSettings(context, mouseX, mouseY);
        }

        String help = split()
                ? "Type to search · Enter / switch: toggle · ★: favourite · click a setting · drag a slider or click its value to type · Esc: close"
                : "Type to search · Enter / switch: toggle · ★: favourite · widen the window for settings · Esc: close";
        context.text(this.font, ThemeDraw.fit(this.font, help, this.width - 24), 12, this.height - 14, Theme.TEXT_DIM, false);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderList(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int left = listLeft();
        int right = listRight();
        int rows = visibleRows();
        ThemeDraw.card(context, left - 2, TOP - 2, right - left + 4, rows * ROW_H + 4);
        if (shown.isEmpty()) {
            context.text(this.font, "No modules match.", left + 8, TOP + 6, Theme.TEXT_MUTED, false);
            return;
        }
        for (int i = 0; i < rows && scroll + i < shown.size(); i++) {
            int index = scroll + i;
            Module module = shown.get(index);
            int y = TOP + i * ROW_H;
            boolean hover = mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + ROW_H;
            if (index == selected) {
                context.fill(left, y, right, y + ROW_H, Theme.SURFACE_PRESSED);
                context.fill(left, y + 3, left + 2, y + ROW_H - 3, Theme.ACCENT);
            } else if (hover) {
                context.fill(left, y, right, y + ROW_H, Theme.SURFACE_HOVER);
            }

            boolean favorite = ModuleStateStore.isFavorite(module);
            boolean starHover = hover && mouseX < left + STAR_W + 2;
            context.text(this.font, favorite ? "★" : "☆", left + 5, y + 5,
                    favorite ? Theme.WARNING : (starHover ? Theme.TEXT_MUTED : Theme.TEXT_DIM), false);

            int switchX = right - SWITCH_W - 6;
            ThemeDraw.toggle(context, switchX, y + (ROW_H - SWITCH_H) / 2, SWITCH_W, SWITCH_H, module.isEnabled(), true);

            int textX = left + STAR_W + 6;
            String name = module.getDisplayName();
            String category = categoryOf.getOrDefault(module, "");
            String detail = category + (module.requiresServerSideOptIn() ? " · server opt-in" : "");
            int room = switchX - 8 - textX;
            String shownName = ThemeDraw.fit(this.font, name, room);
            context.text(this.font, shownName, textX, y + 5, module.isEnabled() ? Theme.TEXT : Theme.TEXT_MUTED, false);
            int detailX = textX + this.font.width(shownName) + 8;
            if (detailX < switchX - 20) {
                context.text(this.font, ThemeDraw.fit(this.font, detail, switchX - 8 - detailX), detailX, y + 5, Theme.TEXT_DIM, false);
            }
        }
        if (shown.size() > rows) {
            int trackH = rows * ROW_H;
            int thumbH = Math.max(10, trackH * rows / shown.size());
            int thumbY = TOP + (trackH - thumbH) * scroll / Math.max(1, shown.size() - rows);
            context.fill(right + 1, thumbY, right + 2, thumbY + thumbH, Theme.BORDER_STRONG);
        }
    }

    private void renderSettings(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int left = settingsLeft();
        int right = settingsRight();
        int rows = visibleRows();
        ThemeDraw.card(context, left - 2, TOP - 2, right - left + 4, rows * ROW_H + 4);
        Module module = selectedModule();
        if (module == null) {
            return;
        }
        context.fill(left, TOP, right, TOP + ROW_H, Theme.SURFACE_RAISED);
        context.text(this.font, ThemeDraw.fit(this.font, module.getDisplayName(), right - left - 50), left + 6, TOP + 5, Theme.TEXT, false);
        String state = module.isEnabled() ? "ON" : "OFF";
        context.text(this.font, state, right - 6 - this.font.width(state), TOP + 5, module.isEnabled() ? Theme.SUCCESS : Theme.TEXT_DIM, false);

        List<SettingRow> settings = settingRows(module);
        if (settings.isEmpty()) {
            context.text(this.font, "No settings.", left + 6, TOP + ROW_H + 5, Theme.TEXT_DIM, false);
            return;
        }
        int settingRows = rows - 1;
        settingsScroll = Math.clamp(settingsScroll, 0, Math.max(0, settings.size() - settingRows));
        for (int i = 0; i < settingRows && settingsScroll + i < settings.size(); i++) {
            SettingRow row = settings.get(settingsScroll + i);
            int index = row.index();
            int y = TOP + (i + 1) * ROW_H;
            SettingLine line = row.line();
            boolean hover = mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + ROW_H;
            if (index == flashSetting && System.currentTimeMillis() < flashUntil) {
                context.fill(left, y, right, y + ROW_H, Theme.ACCENT_SOFT);
            } else if (hover) {
                context.fill(left, y, right, y + ROW_H, Theme.SURFACE_HOVER);
            }
            if (row.numeric() != null) {
                renderSlider(context, row.numeric(), left, right, y, mouseX, mouseY, hover);
                continue;
            }
            int valueRight = right - 6;
            int valueW;
            if (line.kind() == SettingLine.Kind.ACTION) {
                valueW = this.font.width("Run");
                ThemeDraw.box(context, valueRight - valueW - 6, y + 3, valueW + 6, ROW_H - 6, hover ? Theme.ACCENT : Theme.SURFACE_PRESSED);
                context.text(this.font, "Run", valueRight - valueW - 3, y + 5, Theme.TEXT, false);
                valueW += 6;
            } else if (line.isToggle()) {
                valueW = SWITCH_W;
                ThemeDraw.toggle(context, valueRight - SWITCH_W, y + (ROW_H - SWITCH_H) / 2, SWITCH_W, SWITCH_H,
                        line.kind() == SettingLine.Kind.ON, true);
            } else {
                String value = ThemeDraw.fit(this.font, line.value(), (right - left) / 2);
                valueW = this.font.width(value);
                context.text(this.font, value, valueRight - valueW, y + 5, line.valueColor(), false);
            }
            context.text(this.font, ThemeDraw.fit(this.font, line.label(), valueRight - valueW - 8 - (left + 6)), left + 6, y + 5,
                    hover ? Theme.TEXT : Theme.TEXT_MUTED, false);
        }
    }

    /** Settings rows to show: number settings become sliders and their +/- rows are hidden. */
    private List<SettingRow> settingRows(Module module) {
        List<String> lines = module.hasSettings() ? module.getSettingsDisplay() : List.of();
        Map<Integer, NumericSetting> numeric = new HashMap<>();
        Set<Integer> hidden = new HashSet<>();
        if (module instanceof NumericSetting.Provider provider) {
            for (NumericSetting setting : provider.numericSettings()) {
                numeric.put(setting.row(), setting);
                hidden.addAll(setting.hiddenRows());
            }
        }
        List<SettingRow> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!hidden.contains(i)) {
                rows.add(new SettingRow(i, SettingLine.parse(lines.get(i)), numeric.get(i)));
            }
        }
        return rows;
    }

    private int trackLeft(int left, int right) {
        return left + (right - left) * 2 / 5;
    }

    private int trackRight(int right) {
        return right - 6 - VALUE_BOX_W - 8;
    }

    private void renderSlider(GuiGraphicsExtractor context, NumericSetting setting, int left, int right, int y,
                              int mouseX, int mouseY, boolean hover) {
        int trackL = trackLeft(left, right);
        int trackR = trackRight(right);
        context.text(this.font, ThemeDraw.fit(this.font, setting.label(), trackL - 8 - (left + 6)), left + 6, y + 5,
                hover ? Theme.TEXT : Theme.TEXT_MUTED, false);
        int mid = y + ROW_H / 2;
        if (trackR > trackL + 10) {
            int knob = trackL + (int) Math.round((trackR - trackL) * setting.fraction());
            context.fill(trackL, mid - 1, trackR, mid + 1, Theme.BORDER_STRONG);
            context.fill(trackL, mid - 1, knob, mid + 1, Theme.ACCENT);
            boolean active = same(dragging, setting) || hover && mouseX >= trackL - 3 && mouseX <= trackR + 3;
            context.fill(knob - 2, mid - 5, knob + 2, mid + 5, active ? Theme.ACCENT_TEXT : Theme.TEXT);
        }
        int boxX = right - 6 - VALUE_BOX_W;
        boolean typing = same(editing, setting);
        ThemeDraw.box(context, boxX, y + 2, VALUE_BOX_W, ROW_H - 4, typing ? Theme.INPUT : Theme.SURFACE_PRESSED);
        String text = typing ? editText + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "") : setting.format(setting.value());
        context.text(this.font, ThemeDraw.fit(this.font, text, VALUE_BOX_W - 6), boxX + 4, y + 5,
                typing ? Theme.TEXT : Theme.ACCENT_TEXT, false);
    }

    /** Providers build new setting objects each call, so rows are compared by position. */
    private static boolean same(@Nullable NumericSetting a, @Nullable NumericSetting b) {
        return a != null && b != null && a.row() == b.row();
    }

    private void applyNumeric(Module module, NumericSetting setting, double value) {
        setting.set(value);
        ModuleStateStore.onSettingsChanged(module);
    }

    private void commitEdit() {
        NumericSetting setting = editing;
        Module module = selectedModule();
        editing = null;
        if (setting == null || module == null) {
            return;
        }
        Double value = NumericSetting.parse(editText);
        if (value != null) {
            applyNumeric(module, setting, value);
        }
    }

    /** The settings row under a mouse y position, or null. */
    private @Nullable SettingRow settingRowAt(double y) {
        Module module = selectedModule();
        int row = (int) ((y - TOP) / ROW_H);
        if (module == null || y < TOP || row < 1 || row >= visibleRows()) {
            return null;
        }
        List<SettingRow> rows = settingRows(module);
        int index = settingsScroll + row - 1;
        return index < rows.size() ? rows.get(index) : null;
    }

    /** Display name of the key that opens this screen, e.g. "N". */
    public static String openKeyName() {
        return openKey == null || openKey.isUnbound() ? "unbound" : openKey.getTranslatedKeyMessage().getString();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, Theme.SCRIM);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        if (editing != null) {
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB) {
                commitEdit();
            } else if (key == InputConstants.KEY_ESCAPE) {
                editing = null;
            } else if (key == InputConstants.KEY_BACKSPACE && !editText.isEmpty()) {
                editText = editText.substring(0, editText.length() - 1);
            }
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            moveSelection(1);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            moveSelection(-1);
            return true;
        }
        if (key == InputConstants.KEY_PAGEDOWN) {
            moveSelection(visibleRows());
            return true;
        }
        if (key == InputConstants.KEY_PAGEUP) {
            moveSelection(-visibleRows());
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            toggle(selected);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (editing != null) {
            int c = input.codepoint();
            if ((Character.isDigit(c) || c == '.' || c == ',' || c == '-') && editText.length() < 12) {
                editText += (char) c;
            }
            return true;
        }
        return super.charTyped(input);
    }

    private void moveSelection(int delta) {
        if (shown.isEmpty()) {
            return;
        }
        selected = Math.clamp(selected + delta, 0, shown.size() - 1);
        settingsScroll = 0;
        int rows = visibleRows();
        if (selected < scroll) {
            scroll = selected;
        } else if (selected >= scroll + rows) {
            scroll = selected - rows + 1;
        }
    }

    private void toggle(int index) {
        if (index < 0 || index >= shown.size()) {
            return;
        }
        shown.get(index).toggleEnabled();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (editing != null) {
            commitEdit();
        }
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        double x = click.x();
        double y = click.y();
        int row = (int) ((y - TOP) / ROW_H);
        if (y < TOP || row >= visibleRows()) {
            return false;
        }

        if (x >= listLeft() && x <= listRight()) {
            int index = scroll + row;
            if (index >= shown.size()) {
                return false;
            }
            if (index != selected) {
                settingsScroll = 0;
            }
            selected = index;
            Module module = shown.get(index);
            if (x < listLeft() + STAR_W + 2) {
                ModuleStateStore.toggleFavorite(module);
            } else if (x >= listRight() - SWITCH_W - 10 || !split() || doubled) {
                // The switch toggles; without a settings panel (or on double-click) the whole row does.
                module.toggleEnabled();
            }
            return true;
        }

        if (split() && x >= settingsLeft() && x <= settingsRight() && row >= 1) {
            Module module = selectedModule();
            SettingRow settingRow = settingRowAt(y);
            if (module == null || settingRow == null) {
                return false;
            }
            NumericSetting numeric = settingRow.numeric();
            if (numeric != null) {
                int right = settingsRight();
                int trackL = trackLeft(settingsLeft(), right);
                int trackR = trackRight(right);
                if (x >= right - 6 - VALUE_BOX_W) {
                    editing = numeric;
                    editText = numeric.format(numeric.value());
                } else if (x >= trackL - 4 && x <= trackR + 4 && trackR > trackL) {
                    dragging = numeric;
                    applyNumeric(module, numeric, numeric.valueAt((x - trackL) / (double) (trackR - trackL)));
                }
                return true;
            }
            module.onSettingSelected(settingRow.index());
            ModuleStateStore.onSettingsChanged(module);
            flashSetting = settingRow.index();
            flashUntil = System.currentTimeMillis() + 300;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        Module module = selectedModule();
        if (dragging != null && module != null) {
            int trackL = trackLeft(settingsLeft(), settingsRight());
            int trackR = trackRight(settingsRight());
            if (trackR > trackL) {
                applyNumeric(module, dragging, dragging.valueAt((click.x() - trackL) / (double) (trackR - trackL)));
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = (int) Math.signum(verticalAmount) * 3;
        if (split() && mouseX >= settingsLeft()) {
            SettingRow settingRow = settingRowAt(mouseY);
            Module module = selectedModule();
            if (settingRow != null && settingRow.numeric() != null && module != null
                    && mouseX >= trackLeft(settingsLeft(), settingsRight()) - 4) {
                NumericSetting numeric = settingRow.numeric();
                double amount = numeric.step() * Math.signum(verticalAmount) * (hasShiftDownCompat() ? 10 : 1);
                applyNumeric(module, numeric, numeric.value() + amount);
                return true;
            }
            settingsScroll = Math.max(0, settingsScroll - step);
            return true;
        }
        int maxScroll = Math.max(0, shown.size() - visibleRows());
        scroll = Math.clamp(scroll - step, 0, maxScroll);
        return true;
    }

    private boolean hasShiftDownCompat() {
        return this.minecraft != null && (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));
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
