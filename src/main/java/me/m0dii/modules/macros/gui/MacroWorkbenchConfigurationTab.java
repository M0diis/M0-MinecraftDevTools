package me.m0dii.modules.macros.gui;


import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.theme.SidebarNavButton;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.modules.bridging.BridgingTweaksModule;
import me.m0dii.modules.chat.SecondaryChatConfigScreen;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandblockui.BetterCommandBlockUiModule;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.heldlight.HeldLightModule;
import me.m0dii.modules.hudtweaks.HudTweaksModule;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import me.m0dii.modules.hungertweaks.HungerTweaksModule;
import me.m0dii.modules.instantbreak.InstantBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.mousetweaks.MouseTweaksModule;
import me.m0dii.modules.mousetweaks.MouseTweaksScrollItemScaling;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelScrollDirection;
import me.m0dii.modules.mousetweaks.MouseTweaksWheelSearchOrder;
import me.m0dii.modules.nbthud.NBTInfoHudModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.nbttooltip.ShulkerTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.modules.reach.ReachModule;
import me.m0dii.modules.tweaks.TweaksModule;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class MacroWorkbenchConfigurationTab {
    private static final int ROW_H = Theme.ROW_H;
    private static final int ROW_GAP = ROW_H + Theme.GAP_S;
    /** Space at the top of the settings card for the category title and description. */
    private static final int CARD_HEADER_H = 32;
    private static final int CARD_PADDING = Theme.GAP_M;

    enum Category {
        HUD("HUD", "HUD toggles and overlays rendered in screen space."),
        OVERLAYS("Overlays", "World overlays (chunks, structures, light, command blocks)."),
        MODULES("Modules", "General gameplay modules and tooltip behaviors."),
        SECONDARY_CHAT("Secondary Chat", ""),
        PICKUP_FEED("Pickup Feed", "Pick-up feed module toggle and behavior settings."),
        BLOCK_ATTRIBUTES("Block Attributes", "Block interaction and hitbox behavior overrides."),
        TWEAKS("Tweaks", "Visual and gameplay tweaks, plus reach controls."),
        MOUSE_TWEAKS("Mouse Tweaks", "Inventory mouse drag and wheel-transfer tweaks."),
        HUNGER_TWEAKS("Hunger Tweaks", "Food tooltip, saturation, exhaustion, and healing prediction overlays."),
        BRIDGING_TWEAKS("Bridging Tweaks", "Reacharound block placement, outline, crosshair, and targeting rules."),
        HUD_TWEAKS("HUD Tweaks", "Per-element scale, opacity, and offset controls for vanilla HUD parts.");

        private final String label;
        private final String description;

        Category(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private final MacroWorkbenchScreen owner;
    private final List<AbstractWidget> configWidgets;
    private final BooleanSupplier shiftDown;
    private final EnumMap<Category, AbstractWidget> categoryButtons = new EnumMap<>(Category.class);
    private final EnumMap<Category, List<AbstractWidget>> categoryWidgets = new EnumMap<>(Category.class);

    private Category category = Category.HUD;
    private final HudControls hudControls = new HudControls();
    private final SecondaryChatControls secondaryChatControls = new SecondaryChatControls();
    private final PickupFeedControls pickupFeedControls = new PickupFeedControls();
    private final ModulesControls modulesControls = new ModulesControls();
    private final OverlaysControls overlaysControls = new OverlaysControls();
    private final BlockAttributesControls blockAttributesControls = new BlockAttributesControls();
    private final TweaksControls tweaksControls = new TweaksControls();
    private final MouseTweaksControls mouseTweaksControls = new MouseTweaksControls();
    private final HungerTweaksControls hungerTweaksControls = new HungerTweaksControls();
    private final BridgingTweaksControls bridgingTweaksControls = new BridgingTweaksControls();
    private final HudTweaksControls hudTweaksControls = new HudTweaksControls();

    private int selectedRegexIndex = -1;
    private int regexScroll = 0;

    MacroWorkbenchConfigurationTab(MacroWorkbenchScreen owner,
                                   List<AbstractWidget> configWidgets,
                                   BooleanSupplier shiftDown) {
        this.owner = owner;
        this.configWidgets = configWidgets;
        this.shiftDown = shiftDown;
    }

    void initWidgets() {
        int listX = this.owner.contentLeft();
        int listY = this.owner.contentTop();
        int categoryW = categoryListWidth();
        int rightX = sectionViewportLeft();
        int settingW = sectionViewportWidth();

        initCategoryButtons(listX, listY, categoryW);
        this.hudControls.init(rightX, settingW);
        this.secondaryChatControls.init(rightX, settingW);
        this.pickupFeedControls.init(rightX, settingW);
        this.modulesControls.init(rightX, settingW);
        this.overlaysControls.init(rightX, settingW);
        this.blockAttributesControls.init(rightX, settingW);
        this.tweaksControls.init(rightX, settingW);
        this.mouseTweaksControls.init(rightX, settingW);
        this.hungerTweaksControls.init(rightX, settingW);
        this.bridgingTweaksControls.init(rightX, settingW);
        this.hudTweaksControls.init(rightX, settingW);
        initCategoryWidgetGroups();
        register(this.categoryButtons.values().toArray(new AbstractWidget[0]));
        for (List<AbstractWidget> widgets : this.categoryWidgets.values()) {
            register(widgets.toArray(new AbstractWidget[0]));
        }

        syncControls();
    }

    void syncControls() {
        if (this.categoryButtons.isEmpty()) {
            return;
        }

        syncCategoryButtons();
        this.hudControls.sync();
        this.secondaryChatControls.sync();
        this.pickupFeedControls.sync();
        this.modulesControls.sync();
        this.overlaysControls.sync();
        this.blockAttributesControls.sync();
        this.tweaksControls.sync();
        this.mouseTweaksControls.sync();
        this.hungerTweaksControls.sync();
        this.bridgingTweaksControls.sync();
        this.hudTweaksControls.sync();
        syncCategoryVisibility();
    }

    void extractRenderState(GuiGraphicsExtractor context) {
        syncControls();
        Font font = this.owner.workbenchTextRenderer();
        int cardX = cardLeft();
        int cardY = this.owner.contentTop();
        int cardW = this.owner.contentRight() - cardX;
        int cardH = this.owner.contentBottom() - cardY;
        ThemeDraw.card(context, cardX, cardY, cardW, cardH);
        int textW = cardW - CARD_PADDING * 2;
        context.text(font, ThemeDraw.fit(font, this.category.label, textW), cardX + CARD_PADDING, cardY + CARD_PADDING, Theme.TEXT, false);
        if (!this.category.description.isEmpty()) {
            context.text(font, ThemeDraw.fit(font, this.category.description, textW), cardX + CARD_PADDING, cardY + CARD_PADDING + 11, Theme.TEXT_MUTED, false);
        }
        ThemeDraw.hDivider(context, cardX + 1, cardY + CARD_HEADER_H - 4, cardW - 2);

        if (this.category == Category.SECONDARY_CHAT) {
            renderSecondaryRegexList(context);
        }
    }

    boolean handleMouseClick(double mouseX, double mouseY, int button) {
        ControlSection section = activeSection();
        if (section == null) {
            return false;
        }
        boolean handled = section.handleMouseClick(mouseX, mouseY, button);
        if (handled) {
            syncControls();
        }
        return handled;
    }

    boolean handleMouseClickBeforeWidgets(double mouseX, double mouseY, int button) {
        if (this.category != Category.HUD_TWEAKS) {
            return false;
        }
        return handleMouseClick(mouseX, mouseY, button);
    }

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        ControlSection section = activeSection();
        return section != null && section.handleMouseScroll(mouseX, mouseY, verticalAmount);
    }

    private void renderSecondaryRegexList(GuiGraphicsExtractor context) {
        int[] rect = regexListRect();
        int x = rect[0];
        int y = rect[1];
        int w = rect[2];
        int h = rect[3];
        int rowHeight = 12;

        context.fill(x, y, x + w, y + h, 0xAA111111);
        context.fill(x, y, x + w, y + 1, 0x60FFFFFF);

        List<String> regexes = SecondaryChatSettings.get().regexList;
        int visibleRows = Math.max(1, h / rowHeight);
        int max = Math.max(0, regexes.size() - visibleRows);
        this.regexScroll = Math.clamp(this.regexScroll, 0, max);

        for (int i = 0; i < visibleRows; i++) {
            int idx = this.regexScroll + i;
            if (idx >= regexes.size()) {
                break;
            }
            int yy = y + i * rowHeight;
            if (idx == this.selectedRegexIndex) {
                context.fill(x + 1, yy, x + w - 1, yy + rowHeight, 0x604A7CC7);
            }
            String line = regexes.get(idx);
            String text = (idx + 1) + ". " + line;
            if (this.owner.workbenchTextRenderer().width(text) > w - 6) {
                while (text.length() > 8 && this.owner.workbenchTextRenderer().width(text + "...") > w - 6) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }
            context.text(this.owner.workbenchTextRenderer(), text, x + 3, yy + 2, 0xFFE0E0E0);
        }

        if (regexes.isEmpty()) {
            context.text(this.owner.workbenchTextRenderer(), "(none)", x + 3, y + 3, 0xFF909090);
        }
    }

    private int[] regexListRect() {
        int x = this.secondaryChatControls.secondaryRegexInputField.getX();
        int y = this.secondaryChatControls.secondaryRegexInputField.getY() + ROW_GAP;
        return new int[]{x, y, sectionViewportWidth(), 58};
    }

    private void ensureRegexSelectionInBounds() {
        List<String> regexes = SecondaryChatSettings.get().regexList;
        if (regexes == null || regexes.isEmpty()) {
            this.selectedRegexIndex = -1;
            return;
        }
        this.selectedRegexIndex = Math.clamp(this.selectedRegexIndex, 0, regexes.size() - 1);
    }

    private void initCategoryButtons(int listX, int listY, int categoryW) {
        this.categoryButtons.clear();
        int itemH = categoryItemHeight();
        int index = 0;
        for (Category category : Category.values()) {
            AbstractWidget widget = new SidebarNavButton(listX, listY + (itemH + 1) * index, categoryW, itemH,
                    "", category.label, () -> this.category == category, () -> setCategory(category));
            this.categoryButtons.put(category, widget);
            index++;
        }
    }

    private void initCategoryWidgetGroups() {
        this.categoryWidgets.clear();
        addCategoryWidgets(Category.HUD, this.hudControls.widgets());
        addCategoryWidgets(Category.OVERLAYS, this.overlaysControls.widgets());
        addCategoryWidgets(Category.MODULES, this.modulesControls.widgets());
        addCategoryWidgets(Category.SECONDARY_CHAT, this.secondaryChatControls.widgets());
        addCategoryWidgets(Category.PICKUP_FEED, this.pickupFeedControls.widgets());
        addCategoryWidgets(Category.BLOCK_ATTRIBUTES, this.blockAttributesControls.widgets());
        addCategoryWidgets(Category.TWEAKS, this.tweaksControls.widgets());
        addCategoryWidgets(Category.MOUSE_TWEAKS, this.mouseTweaksControls.widgets());
        addCategoryWidgets(Category.HUNGER_TWEAKS, this.hungerTweaksControls.widgets());
        addCategoryWidgets(Category.BRIDGING_TWEAKS, this.bridgingTweaksControls.widgets());
        addCategoryWidgets(Category.HUD_TWEAKS, this.hudTweaksControls.widgets());
    }

    private void addCategoryWidgets(Category category, AbstractWidget... widgets) {
        List<AbstractWidget> list = this.categoryWidgets.computeIfAbsent(category, ignored -> new ArrayList<>());
        for (AbstractWidget widget : widgets) {
            list.add(widget);
        }
    }

    private void addCategoryWidgets(Category category, List<? extends AbstractWidget> widgets) {
        List<AbstractWidget> list = this.categoryWidgets.computeIfAbsent(category, ignored -> new ArrayList<>());
        list.addAll(widgets);
    }

    private void syncCategoryButtons() {
        // Selection is drawn by SidebarNavButton itself.
    }

    private void syncCategoryVisibility() {
        for (Category category : Category.values()) {
            ControlSection section = sectionFor(category);
            if (section != null) {
                section.layout(this.category == category);
                continue;
            }
            boolean visible = this.category == category;
            List<AbstractWidget> widgets = this.categoryWidgets.get(category);
            if (widgets == null) {
                continue;
            }
            for (AbstractWidget widget : widgets) {
                setVisible(widget, visible);
            }
        }
    }

    private final class BoundButton {
        private final Button widget;
        private final Supplier<Component> messageSupplier;

        private BoundButton(Button widget, Supplier<Component> messageSupplier) {
            this.widget = widget;
            this.messageSupplier = messageSupplier;
        }

        private void sync() {
            this.widget.setMessage(this.messageSupplier.get());
        }
    }

    private abstract class ControlSection {
        private final class PositionedWidget {
            private final AbstractWidget widget;
            private final int baseX;
            private final int baseY;

            private PositionedWidget(AbstractWidget widget, int baseX, int baseY) {
                this.widget = widget;
                this.baseX = baseX;
                this.baseY = baseY;
            }
        }

        private final List<BoundButton> boundButtons = new ArrayList<>();
        private final List<PositionedWidget> positionedWidgets = new ArrayList<>();
        private int scrollPixels = 0;

        protected abstract void init(int rightX, int settingW);

        final List<AbstractWidget> widgets() {
            List<AbstractWidget> widgets = new ArrayList<>(this.positionedWidgets.size());
            for (PositionedWidget positionedWidget : this.positionedWidgets) {
                widgets.add(positionedWidget.widget);
            }
            return widgets;
        }

        protected final Button addButton(Supplier<Component> messageSupplier,
                                               Runnable action,
                                               int x,
                                               int y,
                                               int width,
                                               int height) {
            Button widget = Button.builder(messageSupplier.get(), b -> {
                action.run();
                syncControls();
            }).bounds(x, y, width, height).build();
            this.boundButtons.add(new BoundButton(widget, messageSupplier));
            this.positionedWidgets.add(new PositionedWidget(widget, x, y));
            return widget;
        }

        protected final Button addButton(Supplier<Component> messageSupplier,
                                               Runnable action,
                                               int rightX,
                                               int settingW,
                                               int rowIndex) {
            return addButton(messageSupplier, action, rightX, rowY(rowIndex), settingW, ROW_H);
        }

        protected final EditBox addTextField(int x, int y, int width, int height, String placeholder) {
            EditBox field = new EditBox(owner.workbenchTextRenderer(), x, y, width, height, Component.literal(placeholder));
            this.positionedWidgets.add(new PositionedWidget(field, x, y));
            return field;
        }

        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            return false;
        }

        protected boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
            if (!contains(mouseX, mouseY, sectionViewportLeft(), sectionViewportTop(), sectionViewportWidth(), sectionViewportHeight())) {
                return false;
            }
            int maxScroll = maxScrollPixels();
            if (maxScroll <= 0) {
                return false;
            }
            int delta = verticalAmount > 0 ? -ROW_GAP : ROW_GAP;
            this.scrollPixels = Math.clamp(this.scrollPixels + delta, 0, maxScroll);
            syncCategoryVisibility();
            return true;
        }

        protected void syncCustom() {
        }

        private void layout(boolean active) {
            int top = sectionViewportTop();
            int bottom = sectionViewportBottom();
            int maxScroll = maxScrollPixels();
            this.scrollPixels = Math.clamp(this.scrollPixels, 0, maxScroll);
            for (PositionedWidget positionedWidget : this.positionedWidgets) {
                AbstractWidget widget = positionedWidget.widget;
                widget.setX(positionedWidget.baseX);
                widget.setY(positionedWidget.baseY - this.scrollPixels);
                boolean inView = widget.getY() + widget.getHeight() > top && widget.getY() < bottom;
                setVisible(widget, active && inView);
            }
        }

        final void sync() {
            for (BoundButton boundButton : this.boundButtons) {
                boundButton.sync();
            }
            syncCustom();
        }

        private int maxScrollPixels() {
            int maxBottom = sectionViewportTop();
            for (PositionedWidget positionedWidget : this.positionedWidgets) {
                maxBottom = Math.max(maxBottom, positionedWidget.baseY + positionedWidget.widget.getHeight());
            }
            return Math.max(0, maxBottom - sectionViewportBottom());
        }
    }

    private final class HudControls extends ControlSection {
        private Button macroOverlayToggleButton;
        private Button nbtHudToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.macroOverlayToggleButton = addButton(
                    () -> Component.literal("Macro Keybind HUD: " + (ModConfig.showMacroKeybindOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = !ModConfig.showMacroKeybindOverlay),
                    rightX, settingW, 0
            );
            this.nbtHudToggleButton = addButton(
                    () -> Component.literal("NBT Inspector HUD: " + (NBTInfoHudModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> NBTInfoHudModule.INSTANCE.setEnabled(!NBTInfoHudModule.INSTANCE.isEnabled()),
                    rightX, settingW, 1
            );
        }
    }

    private final class SecondaryChatControls extends ControlSection {
        private Button secondaryEnabledToggleButton;
        private Button secondaryEditorButton;
        private Button secondaryRegexAddButton;
        private Button secondaryRegexApplyButton;
        private Button secondaryRegexRemoveButton;
        private Button secondaryRegexClearButton;
        private Button secondaryOutgoingApplyButton;
        private EditBox secondaryRegexInputField;
        private EditBox secondaryOutgoingRegexField;

        @Override
        protected void init(int rightX, int settingW) {
            this.secondaryEnabledToggleButton = addButton(
                    () -> Component.literal("Secondary Chat: " + (SecondaryChatModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> {
                        boolean enabled = !SecondaryChatModule.INSTANCE.isEnabled();
                        SecondaryChatModule.INSTANCE.setEnabled(enabled);
                        if (enabled) {
                            SecondaryChatSettings.updateAndSave(() -> {
                                SecondaryChatSettings.get().showOverlay = true;
                                SecondaryChatSettings.get().renderMode = SecondaryChatSettings.RenderMode.REPLACE;
                                SecondaryChatSettings.get().showWhileGuiOpen = true;
                            });
                        }
                    },
                    rightX, settingW, 0
            );
            this.secondaryEditorButton = addButton(
                    () -> Component.literal("Open Secondary Chat Editor"),
                    () -> Minecraft.getInstance().gui.setScreen(new SecondaryChatConfigScreen(owner)),
                    rightX, settingW, 1
            );
            this.secondaryRegexInputField = addTextField(rightX, rowY(2), settingW - 114, ROW_H, "Regex");
            this.secondaryRegexInputField.setMaxLength(180);
            this.secondaryRegexAddButton = addButton(
                    () -> Component.literal("Add"),
                    () -> {
                        String regex = safeField(this.secondaryRegexInputField);
                        if (!regex.isEmpty()) {
                            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(regex));
                            this.secondaryRegexInputField.setValue("");
                            selectedRegexIndex = SecondaryChatSettings.get().regexList.size() - 1;
                            ensureRegexSelectionInBounds();
                        }
                    },
                    rightX + settingW - 110, rowY(2), 110, ROW_H
            );
            this.secondaryRegexApplyButton = addButton(
                    () -> Component.literal("Apply Selected"),
                    () -> {
                        String regex = safeField(this.secondaryRegexInputField);
                        if (regex.isEmpty()) {
                            return;
                        }
                        SecondaryChatSettings.updateAndSave(() -> {
                            List<String> list = SecondaryChatSettings.get().regexList;
                            if (list != null && selectedRegexIndex >= 0 && selectedRegexIndex < list.size()) {
                                list.set(selectedRegexIndex, regex);
                            }
                        });
                    },
                    rightX, rowY(8), 140, ROW_H
            );
            this.secondaryRegexRemoveButton = addButton(
                    () -> Component.literal("Remove Selected"),
                    () -> {
                        SecondaryChatSettings.updateAndSave(() -> {
                            List<String> list = SecondaryChatSettings.get().regexList;
                            if (list != null && selectedRegexIndex >= 0 && selectedRegexIndex < list.size()) {
                                list.remove(selectedRegexIndex);
                            }
                        });
                        if (selectedRegexIndex > 0) {
                            selectedRegexIndex--;
                        }
                        ensureRegexSelectionInBounds();
                    },
                    rightX + 144, rowY(8), 140, ROW_H
            );
            this.secondaryRegexClearButton = addButton(
                    () -> Component.literal("Clear All"),
                    () -> {
                        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList = new ArrayList<>());
                        selectedRegexIndex = -1;
                        regexScroll = 0;
                    },
                    rightX + 288, rowY(8), 100, ROW_H
            );
            this.secondaryOutgoingRegexField = addTextField(rightX, rowY(9), settingW - 110, ROW_H, "Outgoing regex");
            this.secondaryOutgoingRegexField.setMaxLength(180);
            this.secondaryOutgoingApplyButton = addButton(
                    () -> Component.literal("Apply"),
                    () -> {
                        String outgoing = safeField(this.secondaryOutgoingRegexField);
                        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().outgoingRegex = outgoing);
                    },
                    rightX + settingW - 106, rowY(9), 106, ROW_H
            );
        }

        @Override
        protected void syncCustom() {
            ensureRegexSelectionInBounds();
            SecondaryChatSettings.Data secondary = SecondaryChatSettings.get();
            if (!this.secondaryOutgoingRegexField.isFocused()) {
                this.secondaryOutgoingRegexField.setValue(secondary.outgoingRegex == null ? "" : secondary.outgoingRegex);
            }
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != InputConstants.MOUSE_BUTTON_LEFT) {
                return false;
            }
            int[] rect = regexListRect();
            if (!contains(mouseX, mouseY, rect[0], rect[1], rect[2], rect[3])) {
                return false;
            }

            List<String> regexes = SecondaryChatSettings.get().regexList;
            int rowHeight = 12;
            int clicked = (int) ((mouseY - rect[1]) / rowHeight);
            int index = regexScroll + clicked;
            if (index >= 0 && index < regexes.size()) {
                selectedRegexIndex = index;
                this.secondaryRegexInputField.setValue(regexes.get(index));
                this.secondaryRegexInputField.moveCursorToEnd(false);
                return true;
            }
            return false;
        }

        @Override
        protected boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
            int[] rect = regexListRect();
            if (contains(mouseX, mouseY, rect[0], rect[1], rect[2], rect[3])) {
                int delta = verticalAmount > 0 ? -1 : 1;
                int max = Math.max(0, SecondaryChatSettings.get().regexList.size() - Math.max(1, rect[3] / 12));
                regexScroll = Math.clamp(regexScroll + delta, 0, max);
                return true;
            }
            return super.handleMouseScroll(mouseX, mouseY, verticalAmount);
        }
    }

    private final class HudTweaksControls extends ControlSection {
        private HudTweaksSettings.ElementType selectedElement = HudTweaksSettings.ElementType.CROSSHAIR;

        private Button moduleToggleButton;
        private Button elementButton;
        private Button displayButton;
        private Button scaleButton;
        private Button opacityButton;
        private Button offsetXButton;
        private Button offsetYButton;
        private Button resetElementButton;
        private Button resetAllButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.moduleToggleButton = addButton(
                    () -> Component.literal("HUD Tweaks Module: " + (HudTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> HudTweaksModule.INSTANCE.setEnabled(!HudTweaksModule.INSTANCE.isEnabled()),
                    rightX, settingW, 0
            );
            this.elementButton = addButton(
                    () -> Component.literal("Element: " + label(this.selectedElement)),
                    () -> cycleSelectedElement(1),
                    rightX, settingW, 1
            );
            this.displayButton = addButton(
                    () -> Component.literal("Display: " + (selectedConfig().display ? "ON" : "OFF")),
                    this::toggleDisplay,
                    rightX, settingW, 2
            );
            this.scaleButton = addButton(
                    () -> Component.literal("Scale: " + String.format(Locale.ROOT, "%.2f", selectedConfig().scale)),
                    () -> adjustScale(1),
                    rightX, settingW, 3
            );
            this.opacityButton = addButton(
                    () -> Component.literal("Opacity: " + String.format(Locale.ROOT, "%.2f", selectedConfig().opacity)),
                    () -> adjustOpacity(1),
                    rightX, settingW, 4
            );
            this.offsetXButton = addButton(
                    () -> Component.literal("Offset X: " + selectedConfig().offsetX),
                    () -> adjustOffsetX(1),
                    rightX, settingW, 5
            );
            this.offsetYButton = addButton(
                    () -> Component.literal("Offset Y: " + selectedConfig().offsetY),
                    () -> adjustOffsetY(1),
                    rightX, settingW, 6
            );
            this.resetElementButton = addButton(
                    () -> Component.literal("Reset Selected Element"),
                    () -> HudTweaksSettings.resetElement(this.selectedElement),
                    rightX, settingW, 7
            );
            this.resetAllButton = addButton(
                    () -> Component.literal("Reset All HUD Tweaks"),
                    HudTweaksSettings::resetAll,
                    rightX, settingW, 8
            );
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            int direction = button == InputConstants.MOUSE_BUTTON_LEFT ? 1 : -1;
            if (contains(mouseX, mouseY, this.moduleToggleButton)) {
                HudTweaksModule.INSTANCE.setEnabled(!HudTweaksModule.INSTANCE.isEnabled());
                return true;
            }
            if (contains(mouseX, mouseY, this.elementButton)) {
                cycleSelectedElement(direction);
                return true;
            }
            if (contains(mouseX, mouseY, this.displayButton)) {
                toggleDisplay();
                return true;
            }
            if (contains(mouseX, mouseY, this.scaleButton)) {
                adjustScale(direction);
                return true;
            }
            if (contains(mouseX, mouseY, this.opacityButton)) {
                adjustOpacity(direction);
                return true;
            }
            if (contains(mouseX, mouseY, this.offsetXButton)) {
                adjustOffsetX(direction);
                return true;
            }
            if (contains(mouseX, mouseY, this.offsetYButton)) {
                adjustOffsetY(direction);
                return true;
            }
            if (contains(mouseX, mouseY, this.resetElementButton)) {
                HudTweaksSettings.resetElement(this.selectedElement);
                return true;
            }
            if (contains(mouseX, mouseY, this.resetAllButton)) {
                HudTweaksSettings.resetAll();
                return true;
            }
            return false;
        }

        private HudTweaksSettings.ElementConfig selectedConfig() {
            return HudTweaksSettings.getElement(this.selectedElement);
        }

        private void cycleSelectedElement(int direction) {
            HudTweaksSettings.ElementType[] values = HudTweaksSettings.ElementType.values();
            int next = (this.selectedElement.ordinal() + (direction > 0 ? 1 : -1) + values.length) % values.length;
            this.selectedElement = values[next];
        }

        private void toggleDisplay() {
            HudTweaksSettings.updateAndSave(() -> selectedConfig().display = !selectedConfig().display);
        }

        private void adjustScale(int direction) {
            HudTweaksSettings.updateAndSave(() -> {
                float step = shiftDown.getAsBoolean() ? 0.05f : 0.10f;
                selectedConfig().scale = Math.clamp(
                        selectedConfig().scale + (direction * step),
                        HudTweaksSettings.MIN_SCALE,
                        HudTweaksSettings.MAX_SCALE
                );
            });
        }

        private void adjustOpacity(int direction) {
            HudTweaksSettings.updateAndSave(() -> {
                float step = shiftDown.getAsBoolean() ? 0.02f : 0.05f;
                selectedConfig().opacity = Math.clamp(selectedConfig().opacity + (direction * step), 0.0f, 1.0f);
            });
        }

        private void adjustOffsetX(int direction) {
            HudTweaksSettings.updateAndSave(() -> {
                int step = shiftDown.getAsBoolean() ? 1 : 5;
                selectedConfig().offsetX = Math.clamp(selectedConfig().offsetX + (direction * step), -500, 500);
            });
        }

        private void adjustOffsetY(int direction) {
            HudTweaksSettings.updateAndSave(() -> {
                int step = shiftDown.getAsBoolean() ? 1 : 5;
                selectedConfig().offsetY = Math.clamp(selectedConfig().offsetY + (direction * step), -500, 500);
            });
        }

        private String label(HudTweaksSettings.ElementType type) {
            return switch (type) {
                case ACTION_BAR -> "Action Bar";
                case BOSS_BAR -> "Boss Bar";
                case CROSSHAIR -> "Crosshair";
                case DEBUG_SCREEN -> "Debug Screen";
                case HOTBAR_GROUP -> "Hotbar Group";
                case PLAYER_LIST -> "Player List";
                case SCOREBOARD -> "Scoreboard";
                case SCREEN_TITLE -> "Screen Title";
                case STATUS_EFFECT -> "Status Effect";
                case SUBTITLES -> "Subtitles";
                case TOAST -> "Toast";
                case TOOLTIP -> "Tooltip";
            };
        }
    }

    private final class PickupFeedControls extends ControlSection {
        private Button pickupEnabledButton;
        private Button pickupDurationButton;
        private Button pickupLinesButton;
        private Button pickupIconScaleButton;
        private Button pickupDirectionButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.pickupEnabledButton = addButton(
                    () -> Component.literal("Pickup Notifier: " + (ItemPickupNotifierModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ItemPickupNotifierModule.INSTANCE.setEnabled(!ItemPickupNotifierModule.INSTANCE.isEnabled()),
                    rightX, settingW, 0
            );
            this.pickupDurationButton = addButton(
                    () -> Component.literal("Pickup Duration: " + PickupFeedSettings.get().durationMs + "ms"),
                    () -> adjustDuration(1),
                    rightX, settingW, 1
            );
            this.pickupLinesButton = addButton(
                    () -> Component.literal("Pickup Max Lines: " + PickupFeedSettings.get().maxLines),
                    () -> adjustLines(1),
                    rightX, settingW, 2
            );
            this.pickupIconScaleButton = addButton(
                    () -> Component.literal("Pickup Icon Scale: " + String.format(Locale.ROOT, "%.2f", PickupFeedSettings.get().iconScale)),
                    () -> adjustIconScale(1),
                    rightX, settingW, 3
            );
            this.pickupDirectionButton = addButton(
                    () -> Component.literal("Pickup Direction: " + PickupFeedSettings.get().direction.name()),
                    () -> cycleDirection(true),
                    rightX, settingW, 4
            );
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT) {
                int direction = button == InputConstants.MOUSE_BUTTON_LEFT ? 1 : -1;
                if (contains(mouseX, mouseY, this.pickupDurationButton)) {
                    adjustDuration(direction);
                    return true;
                }
                if (contains(mouseX, mouseY, this.pickupLinesButton)) {
                    adjustLines(direction);
                    return true;
                }
                if (contains(mouseX, mouseY, this.pickupIconScaleButton)) {
                    adjustIconScale(direction);
                    return true;
                }
                if (contains(mouseX, mouseY, this.pickupDirectionButton)) {
                    cycleDirection(direction > 0);
                    return true;
                }
            }
            return false;
        }

        private void adjustDuration(int direction) {
            PickupFeedSettings.updateAndSave(() -> {
                int step = shiftDown.getAsBoolean() ? 250 : 500;
                PickupFeedSettings.get().durationMs += direction * step;
            });
        }

        private void adjustLines(int direction) {
            PickupFeedSettings.updateAndSave(() -> {
                int step = shiftDown.getAsBoolean() ? 1 : 2;
                PickupFeedSettings.get().maxLines += direction * step;
            });
        }

        private void adjustIconScale(int direction) {
            PickupFeedSettings.updateAndSave(() -> {
                float step = shiftDown.getAsBoolean() ? 0.05f : 0.1f;
                PickupFeedSettings.get().iconScale += direction * step;
            });
        }

        private void cycleDirection(boolean forward) {
            PickupFeedSettings.updateAndSave(() -> {
                PickupFeedSettings.Direction current = PickupFeedSettings.get().direction;
                if (forward) {
                    PickupFeedSettings.get().direction = current == PickupFeedSettings.Direction.UP
                            ? PickupFeedSettings.Direction.DOWN
                            : PickupFeedSettings.Direction.UP;
                } else {
                    PickupFeedSettings.get().direction = current == PickupFeedSettings.Direction.DOWN
                            ? PickupFeedSettings.Direction.UP
                            : PickupFeedSettings.Direction.DOWN;
                }
            });
        }
    }

    private final class ModulesControls extends ControlSection {
        private Button freecamToggleButton;
        private Button fullbrightToggleButton;
        private Button heldLightToggleButton;
        private Button inventoryMoveToggleButton;
        private Button instantBreakToggleButton;
        private Button fastBlockPlacementToggleButton;
        private Button betterCommandBlockUiToggleButton;
        private Button waypointsToggleButton;
        private Button nbtTooltipToggleButton;
        private Button shulkerTooltipToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.freecamToggleButton = addButton(() -> Component.literal("Freecam: " + (FreecamModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FreecamModule.INSTANCE.setEnabled(!FreecamModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.fullbrightToggleButton = addButton(() -> Component.literal("Fullbright: " + (FullbrightModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FullbrightModule.INSTANCE.setEnabled(!FullbrightModule.INSTANCE.isEnabled()), rightX, settingW, 1);
            this.heldLightToggleButton = addButton(() -> Component.literal("Held Light: " + (HeldLightModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> HeldLightModule.INSTANCE.setEnabled(!HeldLightModule.INSTANCE.isEnabled()), rightX, settingW, 2);
            this.inventoryMoveToggleButton = addButton(() -> Component.literal("Inventory Move: " + (InventoryMoveModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> InventoryMoveModule.INSTANCE.setEnabled(!InventoryMoveModule.INSTANCE.isEnabled()), rightX, settingW, 3);
            this.instantBreakToggleButton = addButton(() -> Component.literal("Instant Break: " + (InstantBreakModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> InstantBreakModule.INSTANCE.setEnabled(!InstantBreakModule.INSTANCE.isEnabled()), rightX, settingW, 4);
            this.fastBlockPlacementToggleButton = addButton(() -> Component.literal("Fast Place: " + (FastBlockPlacementModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FastBlockPlacementModule.INSTANCE.setEnabled(!FastBlockPlacementModule.INSTANCE.isEnabled()), rightX, settingW, 5);
            this.betterCommandBlockUiToggleButton = addButton(() -> Component.literal("Better Command Block UI: " + (BetterCommandBlockUiModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> BetterCommandBlockUiModule.INSTANCE.setEnabled(!BetterCommandBlockUiModule.INSTANCE.isEnabled()), rightX, settingW, 6);
            this.waypointsToggleButton = addButton(() -> Component.literal("Waypoints: " + (WaypointModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> WaypointModule.INSTANCE.setEnabled(!WaypointModule.INSTANCE.isEnabled()), rightX, settingW, 7);
            this.nbtTooltipToggleButton = addButton(() -> Component.literal("NBT Tooltip: " + (NBTTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> NBTTooltipModule.INSTANCE.setEnabled(!NBTTooltipModule.INSTANCE.isEnabled()), rightX, settingW, 8);
            this.shulkerTooltipToggleButton = addButton(() -> Component.literal("Shulker Preview Tooltip: " + (ShulkerTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ShulkerTooltipModule.INSTANCE.setEnabled(!ShulkerTooltipModule.INSTANCE.isEnabled()), rightX, settingW, 9);
        }
    }

    private final class OverlaysControls extends ControlSection {
        private Button biomeBorderToggleButton;
        private Button chunkBorderToggleButton;
        private Button slimeChunksToggleButton;
        private Button structureBoundsToggleButton;
        private Button commandBlockOverlayToggleButton;
        private Button lightOverlayToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.biomeBorderToggleButton = addButton(() -> Component.literal("Biome Border Overlay: " + (BiomeBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> BiomeBorderOverlayModule.INSTANCE.setEnabled(!BiomeBorderOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.chunkBorderToggleButton = addButton(() -> Component.literal("Chunk Border Overlay: " + (ChunkBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ChunkBorderOverlayModule.INSTANCE.setEnabled(!ChunkBorderOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 1);
            this.slimeChunksToggleButton = addButton(() -> Component.literal("Slime Chunk Overlay: " + (SlimeChunkOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> SlimeChunkOverlayModule.INSTANCE.setEnabled(!SlimeChunkOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 2);
            this.structureBoundsToggleButton = addButton(() -> Component.literal("Structure Bounding Boxes: " + (StructureBoundingBoxOverlay.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> StructureBoundingBoxOverlay.INSTANCE.setEnabled(!StructureBoundingBoxOverlay.INSTANCE.isEnabled()), rightX, settingW, 3);
            this.commandBlockOverlayToggleButton = addButton(() -> Component.literal("Command Block Overlay: " + (CommandBlockOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> CommandBlockOverlayModule.INSTANCE.setEnabled(!CommandBlockOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 4);
            this.lightOverlayToggleButton = addButton(() -> Component.literal("Light Level Overlay: " + (LightLevelOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> LightLevelOverlayModule.INSTANCE.setEnabled(!LightLevelOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 5);
        }
    }

    private final class BlockAttributesControls extends ControlSection {
        private Button collisionMeshToggleButton;
        private Button lightBlocksToggleButton;
        private Button preventInteractionsToggleButton;
        private Button solidFluidHitboxesToggleButton;
        private Button barrierBlocksToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.collisionMeshToggleButton = addButton(() -> Component.literal("Show Collision Mesh: " + (ModConfig.blockAttributesShowCollisionMesh ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowCollisionMesh = !ModConfig.blockAttributesShowCollisionMesh), rightX, settingW, 0);
            this.lightBlocksToggleButton = addButton(() -> Component.literal("Show Light Blocks: " + (ModConfig.blockAttributesShowLightBlocks ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowLightBlocks = !ModConfig.blockAttributesShowLightBlocks), rightX, settingW, 1);
            this.preventInteractionsToggleButton = addButton(() -> Component.literal("Prevent Interactions: " + (ModConfig.blockAttributesPreventInteractions ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesPreventInteractions = !ModConfig.blockAttributesPreventInteractions), rightX, settingW, 2);
            this.solidFluidHitboxesToggleButton = addButton(() -> Component.literal("Solid Fluid Hitboxes: " + (ModConfig.blockAttributesSolidFluidHitboxes ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesSolidFluidHitboxes = !ModConfig.blockAttributesSolidFluidHitboxes), rightX, settingW, 3);
            this.barrierBlocksToggleButton = addButton(() -> Component.literal("Show Barrier Blocks: " + (ModConfig.blockAttributesShowBarrierBlocks ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowBarrierBlocks = !ModConfig.blockAttributesShowBarrierBlocks), rightX, settingW, 4);
        }
    }

    private final class TweaksControls extends ControlSection {
        private Button tweaksModuleToggleButton;
        private Button hideOwnEffectsToggleButton;
        private Button hideOffhandItemToggleButton;
        private Button disableBlockBreakParticlesToggleButton;
        private Button disableEntityRenderingToggleButton;
        private Button disableNetherFogToggleButton;
        private Button disableRenderDistanceFogToggleButton;
        private Button disableRainEffectsToggleButton;
        private Button disableSoundsToggleButton;
        private Button disableWallUnsprintToggleButton;
        private Button angelBlockToggleButton;
        private Button permanentSneakToggleButton;
        private Button permanentSprintToggleButton;
        private Button disableHurtCameraToggleButton;
        private Button disableViewBobbingToggleButton;
        private Button reachToggleButton;
        private Button reachSafeClampToggleButton;
        private Button reachBlockDistanceButton;
        private Button reachEntityDistanceButton;
        private Button reachMpBlockExtraButton;
        private Button reachMpEntityExtraButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.tweaksModuleToggleButton = addButton(() -> Component.literal("Tweaks Module: " + (TweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> TweaksModule.INSTANCE.setEnabled(!TweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.hideOwnEffectsToggleButton = addButton(() -> Component.literal("Hide Own Effect Particles: " + (ModConfig.tweaksHideOwnEffectParticles ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOwnEffectParticles = !ModConfig.tweaksHideOwnEffectParticles), rightX, settingW, 1);
            this.hideOffhandItemToggleButton = addButton(() -> Component.literal("Hide Offhand Item: " + (ModConfig.tweaksHideOffhandItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOffhandItem = !ModConfig.tweaksHideOffhandItem), rightX, settingW, 2);
            this.disableBlockBreakParticlesToggleButton = addButton(() -> Component.literal("Disable Block Breaking Particles: " + (ModConfig.tweaksDisableBlockBreakingParticles ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableBlockBreakingParticles = !ModConfig.tweaksDisableBlockBreakingParticles), rightX, settingW, 3);
            this.disableEntityRenderingToggleButton = addButton(() -> Component.literal("Disable Entity Rendering: " + (ModConfig.tweaksDisableEntityRendering ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableEntityRendering = !ModConfig.tweaksDisableEntityRendering), rightX, settingW, 4);
            this.disableNetherFogToggleButton = addButton(() -> Component.literal("Disable Nether Fog: " + (ModConfig.tweaksDisableNetherFog ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableNetherFog = !ModConfig.tweaksDisableNetherFog), rightX, settingW, 5);
            this.disableRenderDistanceFogToggleButton = addButton(() -> Component.literal("Disable Render-Distance Fog: " + (ModConfig.tweaksDisableRenderDistanceFog ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRenderDistanceFog = !ModConfig.tweaksDisableRenderDistanceFog), rightX, settingW, 6);
            this.disableRainEffectsToggleButton = addButton(() -> Component.literal("Disable Rain Effects: " + (ModConfig.tweaksDisableRainEffects ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRainEffects = !ModConfig.tweaksDisableRainEffects), rightX, settingW, 7);
            this.disableSoundsToggleButton = addButton(() -> Component.literal("Disable Sounds: " + (ModConfig.tweaksDisableSounds ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableSounds = !ModConfig.tweaksDisableSounds), rightX, settingW, 8);
            this.disableWallUnsprintToggleButton = addButton(() -> Component.literal("Disable Wall Unsprint: " + (ModConfig.tweaksDisableWallUnsprint ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableWallUnsprint = !ModConfig.tweaksDisableWallUnsprint), rightX, settingW, 9);
            this.angelBlockToggleButton = addButton(() -> Component.literal("Angel Block: " + (ModConfig.tweaksAngelBlock ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksAngelBlock = !ModConfig.tweaksAngelBlock), rightX, settingW, 10);
            this.permanentSneakToggleButton = addButton(() -> Component.literal("Permanent Sneak: " + (ModConfig.tweaksPermanentSneak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSneak = !ModConfig.tweaksPermanentSneak), rightX, settingW, 11);
            this.permanentSprintToggleButton = addButton(() -> Component.literal("Permanent Sprint: " + (ModConfig.tweaksPermanentSprint ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSprint = !ModConfig.tweaksPermanentSprint), rightX, settingW, 12);
            this.disableHurtCameraToggleButton = addButton(() -> Component.literal("Disable Hurt Camera: " + (ModConfig.tweaksDisableHurtCamera ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableHurtCamera = !ModConfig.tweaksDisableHurtCamera), rightX, settingW, 13);
            this.disableViewBobbingToggleButton = addButton(() -> Component.literal("Disable View Bobbing: " + (ModConfig.tweaksDisableViewBobbing ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableViewBobbing = !ModConfig.tweaksDisableViewBobbing), rightX, settingW, 14);
            this.reachToggleButton = addButton(() -> Component.literal("Reach: " + (ReachModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ReachModule.INSTANCE.setEnabled(!ReachModule.INSTANCE.isEnabled()), rightX, settingW, 15);
            this.reachSafeClampToggleButton = addButton(() -> Component.literal("Reach Safe Multiplayer Clamp: " + (ModConfig.reachSafeMultiplayerClamp ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.reachSafeMultiplayerClamp = !ModConfig.reachSafeMultiplayerClamp), rightX, settingW, 16);
            this.reachBlockDistanceButton = addButton(() -> Component.literal("Reach Block Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachBlockDistance)),
                    () -> adjustReachBlockDistance(1), rightX, settingW, 17);
            this.reachEntityDistanceButton = addButton(() -> Component.literal("Reach Entity Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachEntityDistance)),
                    () -> adjustReachEntityDistance(1), rightX, settingW, 18);
            this.reachMpBlockExtraButton = addButton(() -> Component.literal("Reach MP Block Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerBlockExtra)),
                    () -> adjustReachMpBlockExtra(1), rightX, settingW, 19);
            this.reachMpEntityExtraButton = addButton(() -> Component.literal("Reach MP Entity Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerEntityExtra)),
                    () -> adjustReachMpEntityExtra(1), rightX, settingW, 20);
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            if (contains(mouseX, mouseY, this.reachBlockDistanceButton)) {
                adjustReachBlockDistance(-1);
                return true;
            }
            if (contains(mouseX, mouseY, this.reachEntityDistanceButton)) {
                adjustReachEntityDistance(-1);
                return true;
            }
            if (contains(mouseX, mouseY, this.reachMpBlockExtraButton)) {
                adjustReachMpBlockExtra(-1);
                return true;
            }
            if (contains(mouseX, mouseY, this.reachMpEntityExtraButton)) {
                adjustReachMpEntityExtra(-1);
                return true;
            }
            return false;
        }

        private void adjustReachBlockDistance(int direction) {
            ModConfig.updateAndSave(() -> {
                double step = shiftDown.getAsBoolean() ? 0.5 : 0.25;
                ModConfig.reachBlockDistance = Math.clamp(ModConfig.reachBlockDistance + (direction * step), 1.0, 16.0);
            });
        }

        private void adjustReachEntityDistance(int direction) {
            ModConfig.updateAndSave(() -> {
                double step = shiftDown.getAsBoolean() ? 0.5 : 0.25;
                ModConfig.reachEntityDistance = Math.clamp(ModConfig.reachEntityDistance + (direction * step), 1.0, 16.0);
            });
        }

        private void adjustReachMpBlockExtra(int direction) {
            ModConfig.updateAndSave(() -> {
                double step = shiftDown.getAsBoolean() ? 0.5 : 0.25;
                ModConfig.reachMultiplayerBlockExtra = Math.clamp(ModConfig.reachMultiplayerBlockExtra + (direction * step), 0.0, 4.0);
            });
        }

        private void adjustReachMpEntityExtra(int direction) {
            ModConfig.updateAndSave(() -> {
                double step = shiftDown.getAsBoolean() ? 0.5 : 0.25;
                ModConfig.reachMultiplayerEntityExtra = Math.clamp(ModConfig.reachMultiplayerEntityExtra + (direction * step), 0.0, 4.0);
            });
        }
    }

    private final class MouseTweaksControls extends ControlSection {
        private Button mouseTweaksModuleToggleButton;
        private Button mouseTweaksRmbToggleButton;
        private Button mouseTweaksLmbWithItemToggleButton;
        private Button mouseTweaksLmbWithoutItemToggleButton;
        private Button mouseTweaksWheelToggleButton;
        private Button mouseTweaksWheelSearchOrderButton;
        private Button mouseTweaksWheelScrollDirectionButton;
        private Button mouseTweaksScrollScalingButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.mouseTweaksModuleToggleButton = addButton(() -> Component.literal("Mouse Tweaks Module: " + (MouseTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> MouseTweaksModule.INSTANCE.setEnabled(!MouseTweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.mouseTweaksRmbToggleButton = addButton(() -> Component.literal("RMB Tweak: " + (ModConfig.mouseTweaksRmbTweak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksRmbTweak = !ModConfig.mouseTweaksRmbTweak), rightX, settingW, 1);
            this.mouseTweaksLmbWithItemToggleButton = addButton(() -> Component.literal("LMB Tweak With Item: " + (ModConfig.mouseTweaksLmbTweakWithItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithItem = !ModConfig.mouseTweaksLmbTweakWithItem), rightX, settingW, 2);
            this.mouseTweaksLmbWithoutItemToggleButton = addButton(() -> Component.literal("LMB Tweak Without Item: " + (ModConfig.mouseTweaksLmbTweakWithoutItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithoutItem = !ModConfig.mouseTweaksLmbTweakWithoutItem), rightX, settingW, 3);
            this.mouseTweaksWheelToggleButton = addButton(() -> Component.literal("Wheel Tweak: " + (ModConfig.mouseTweaksWheelTweak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelTweak = !ModConfig.mouseTweaksWheelTweak), rightX, settingW, 4);
            this.mouseTweaksWheelSearchOrderButton = addButton(() -> Component.literal("Wheel Search Order: " + ModConfig.mouseTweaksWheelSearchOrder),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelSearchOrder = nextWheelSearchOrder(ModConfig.mouseTweaksWheelSearchOrder)), rightX, settingW, 5);
            this.mouseTweaksWheelScrollDirectionButton = addButton(() -> Component.literal("Wheel Scroll Direction: " + ModConfig.mouseTweaksWheelScrollDirection),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelScrollDirection = nextWheelScrollDirection(ModConfig.mouseTweaksWheelScrollDirection)), rightX, settingW, 6);
            this.mouseTweaksScrollScalingButton = addButton(() -> Component.literal("Scroll Item Scaling: " + ModConfig.mouseTweaksScrollItemScaling),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksScrollItemScaling = nextScrollScaling(ModConfig.mouseTweaksScrollItemScaling)), rightX, settingW, 7);
        }
    }

    private final class HungerTweaksControls extends ControlSection {
        private Button hungerTweaksModuleToggleButton;
        private Button hungerTooltipToggleButton;
        private Button hungerTooltipAlwaysToggleButton;
        private Button hungerSaturationOverlayToggleButton;
        private Button hungerFoodValuesOverlayToggleButton;
        private Button hungerOffhandOverlayToggleButton;
        private Button hungerExhaustionUnderlayToggleButton;
        private Button hungerHealthOverlayToggleButton;
        private Button hungerDebugInfoToggleButton;
        private Button hungerVanillaAnimationToggleButton;
        private Button hungerMaxFlashAlphaButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.hungerTweaksModuleToggleButton = addButton(() -> Component.literal("Hunger Tweaks Module: " + (HungerTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> HungerTweaksModule.INSTANCE.setEnabled(!HungerTweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.hungerTooltipToggleButton = addButton(() -> Component.literal("Tooltip Food Values: " + (ModConfig.hungerTweaksShowFoodValuesInTooltip ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltip = !ModConfig.hungerTweaksShowFoodValuesInTooltip), rightX, settingW, 1);
            this.hungerTooltipAlwaysToggleButton = addButton(() -> Component.literal("Tooltip Always Visible: " + (ModConfig.hungerTweaksShowFoodValuesInTooltipAlways ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltipAlways = !ModConfig.hungerTweaksShowFoodValuesInTooltipAlways), rightX, settingW, 2);
            this.hungerSaturationOverlayToggleButton = addButton(() -> Component.literal("Saturation Overlay: " + (ModConfig.hungerTweaksShowSaturationHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowSaturationHudOverlay = !ModConfig.hungerTweaksShowSaturationHudOverlay), rightX, settingW, 3);
            this.hungerFoodValuesOverlayToggleButton = addButton(() -> Component.literal("Held Food Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlay = !ModConfig.hungerTweaksShowFoodValuesHudOverlay), rightX, settingW, 4);
            this.hungerOffhandOverlayToggleButton = addButton(() -> Component.literal("Offhand Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand = !ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand), rightX, settingW, 5);
            this.hungerExhaustionUnderlayToggleButton = addButton(() -> Component.literal("Exhaustion Underlay: " + (ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay = !ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay), rightX, settingW, 6);
            this.hungerHealthOverlayToggleButton = addButton(() -> Component.literal("Estimated Health Overlay: " + (ModConfig.hungerTweaksShowFoodHealthHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodHealthHudOverlay = !ModConfig.hungerTweaksShowFoodHealthHudOverlay), rightX, settingW, 7);
            this.hungerDebugInfoToggleButton = addButton(() -> Component.literal("Debug HUD Food Info: " + (ModConfig.hungerTweaksShowFoodDebugInfo ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodDebugInfo = !ModConfig.hungerTweaksShowFoodDebugInfo), rightX, settingW, 8);
            this.hungerVanillaAnimationToggleButton = addButton(() -> Component.literal("Match Vanilla Animation: " + (ModConfig.hungerTweaksShowVanillaAnimationsOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowVanillaAnimationsOverlay = !ModConfig.hungerTweaksShowVanillaAnimationsOverlay), rightX, settingW, 9);
            this.hungerMaxFlashAlphaButton = addButton(() -> Component.literal("Max Flash Alpha: " + HungerTweaksModule.formatFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha)),
                    () -> adjustFlashAlpha(1), rightX, settingW, 10);
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            if (contains(mouseX, mouseY, this.hungerMaxFlashAlphaButton)) {
                adjustFlashAlpha(button == InputConstants.MOUSE_BUTTON_LEFT ? 1 : -1);
                return true;
            }
            return false;
        }

        private void adjustFlashAlpha(int direction) {
            ModConfig.updateAndSave(() -> {
                float step = shiftDown.getAsBoolean() ? 0.01f : 0.05f;
                ModConfig.hungerTweaksMaxHudOverlayFlashAlpha = HungerTweaksModule.clampFlashAlpha(
                        ModConfig.hungerTweaksMaxHudOverlayFlashAlpha + (direction * step)
                );
            });
        }
    }

    private final class BridgingTweaksControls extends ControlSection {
        private final List<Button> buttons = new ArrayList<>();

        @Override
        protected void init(int rightX, int settingW) {
            this.buttons.clear();
            for (int i = 0; i < 20; i++) {
                final int index = i;
                this.buttons.add(addButton(
                        () -> bridgingMessage(index),
                        () -> adjustSetting(index, 1),
                        rightX, settingW, i
                ));
            }
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
                return false;
            }
            int direction = button == InputConstants.MOUSE_BUTTON_LEFT ? 1 : -1;
            for (int i = 0; i < this.buttons.size(); i++) {
                if (contains(mouseX, mouseY, this.buttons.get(i))) {
                    adjustSetting(i, direction);
                    return true;
                }
            }
            return false;
        }

        private Component bridgingMessage(int index) {
            return switch (index) {
                case 0 -> Component.literal("Bridging Tweaks: " + (BridgingTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF"));
                case 1 -> Component.literal("Minimum Distance: " + String.format(Locale.ROOT, "%.1f%%", ModConfig.bridgingMinBridgeDistance));
                case 2 -> Component.literal("Only When Crouched: " + (ModConfig.bridgingOnlyWhenCrouched ? "ON" : "OFF"));
                case 3 -> Component.literal("Bridge Axes: " + ModConfig.bridgingSupportedAxes);
                case 4 -> Component.literal("Crouched Axes: " + ModConfig.bridgingSupportedAxesWhenCrouched);
                case 5 -> Component.literal("Post Delay: " + ModConfig.bridgingDelayPostBridging + " ticks");
                case 6 -> Component.literal("Show Crosshair: " + (ModConfig.bridgingShowCrosshair ? "ON" : "OFF"));
                case 7 -> Component.literal("Show Outline: " + (ModConfig.bridgingShowOutline ? "ON" : "OFF"));
                case 8 -> Component.literal("Outline When Not Bridging: " + (ModConfig.bridgingShowOutlineWhenNotBridging ? "ON" : "OFF"));
                case 9 -> Component.literal("Outline Respects Crouch: " + (ModConfig.bridgingNonBridgeRespectsCrouchRules ? "ON" : "OFF"));
                case 10 -> Component.literal("Outline Color: " + String.format(Locale.ROOT, "#%08X", ModConfig.bridgingOutlineColor));
                case 11 -> Component.literal("Skip Torch Blocks: " + (ModConfig.bridgingSkipTorchBridging ? "ON" : "OFF"));
                case 12 -> Component.literal("Slab Assist: " + (ModConfig.bridgingEnableSlabAssist ? "ON" : "OFF"));
                case 13 -> Component.literal("Replaceable Targets: " + (ModConfig.bridgingEnableNonSolidReplace ? "ON" : "OFF"));
                case 14 -> Component.literal("Snap Strength: " + String.format(Locale.ROOT, "%.2f", ModConfig.bridgingSnapStrength));
                case 15 -> Component.literal("Adjacency: " + ModConfig.bridgingAdjacency);
                case 16 -> Component.literal("Perspective Lock: " + ModConfig.bridgingPerspectiveLock);
                case 17 -> Component.literal("Debug Highlight: " + (ModConfig.bridgingShowDebugHighlight ? "ON" : "OFF"));
                case 18 -> Component.literal("Debug Non-Bridging: " + (ModConfig.bridgingShowDebugNonBridgingHighlight ? "ON" : "OFF"));
                case 19 -> Component.literal("Debug Trace: " + (ModConfig.bridgingShowDebugTrace ? "ON" : "OFF"));
                default -> Component.literal("");
            };
        }

        private void adjustSetting(int index, int direction) {
            if (index == 0) {
                BridgingTweaksModule.INSTANCE.setEnabled(!BridgingTweaksModule.INSTANCE.isEnabled());
                return;
            }

            ModConfig.updateAndSave(() -> {
                switch (index) {
                    case 1 -> {
                        float step = shiftDown.getAsBoolean() ? 1.0f : 5.0f;
                        ModConfig.bridgingMinBridgeDistance = Math.clamp(ModConfig.bridgingMinBridgeDistance + (direction * step), 0.0f, 100.0f);
                    }
                    case 2 -> ModConfig.bridgingOnlyWhenCrouched = !ModConfig.bridgingOnlyWhenCrouched;
                    case 3 -> ModConfig.bridgingSupportedAxes = cycleEnum(ModConfig.bridgingSupportedAxes, direction);
                    case 4 -> ModConfig.bridgingSupportedAxesWhenCrouched = cycleEnum(ModConfig.bridgingSupportedAxesWhenCrouched, direction);
                    case 5 -> ModConfig.bridgingDelayPostBridging = Math.clamp(ModConfig.bridgingDelayPostBridging + direction, 0, 20);
                    case 6 -> ModConfig.bridgingShowCrosshair = !ModConfig.bridgingShowCrosshair;
                    case 7 -> ModConfig.bridgingShowOutline = !ModConfig.bridgingShowOutline;
                    case 8 -> ModConfig.bridgingShowOutlineWhenNotBridging = !ModConfig.bridgingShowOutlineWhenNotBridging;
                    case 9 -> ModConfig.bridgingNonBridgeRespectsCrouchRules = !ModConfig.bridgingNonBridgeRespectsCrouchRules;
                    case 10 -> ModConfig.bridgingOutlineColor = cycleBridgingOutlineColor(ModConfig.bridgingOutlineColor, direction > 0);
                    case 11 -> ModConfig.bridgingSkipTorchBridging = !ModConfig.bridgingSkipTorchBridging;
                    case 12 -> ModConfig.bridgingEnableSlabAssist = !ModConfig.bridgingEnableSlabAssist;
                    case 13 -> ModConfig.bridgingEnableNonSolidReplace = !ModConfig.bridgingEnableNonSolidReplace;
                    case 14 -> {
                        float step = shiftDown.getAsBoolean() ? 0.01f : 0.05f;
                        ModConfig.bridgingSnapStrength = Math.clamp(ModConfig.bridgingSnapStrength + (direction * step), 0.0f, 1.0f);
                    }
                    case 15 -> ModConfig.bridgingAdjacency = cycleEnum(ModConfig.bridgingAdjacency, direction);
                    case 16 -> ModConfig.bridgingPerspectiveLock = cycleEnum(ModConfig.bridgingPerspectiveLock, direction);
                    case 17 -> ModConfig.bridgingShowDebugHighlight = !ModConfig.bridgingShowDebugHighlight;
                    case 18 -> ModConfig.bridgingShowDebugNonBridgingHighlight = !ModConfig.bridgingShowDebugNonBridgingHighlight;
                    case 19 -> ModConfig.bridgingShowDebugTrace = !ModConfig.bridgingShowDebugTrace;
                    default -> {
                    }
                }
            });
        }
    }

    private void setCategory(Category category) {
        this.category = category == null ? Category.HUD : category;
        syncControls();
    }

    private static String safeField(EditBox field) {
        if (field == null || field.getValue() == null) {
            return "";
        }
        return field.getValue().trim();
    }

    private ControlSection activeSection() {
        return sectionFor(this.category);
    }

    private ControlSection sectionFor(Category category) {
        return switch (category) {
            case HUD -> this.hudControls;
            case OVERLAYS -> this.overlaysControls;
            case MODULES -> this.modulesControls;
            case SECONDARY_CHAT -> this.secondaryChatControls;
            case PICKUP_FEED -> this.pickupFeedControls;
            case BLOCK_ATTRIBUTES -> this.blockAttributesControls;
            case TWEAKS -> this.tweaksControls;
            case MOUSE_TWEAKS -> this.mouseTweaksControls;
            case HUNGER_TWEAKS -> this.hungerTweaksControls;
            case BRIDGING_TWEAKS -> this.bridgingTweaksControls;
            case HUD_TWEAKS -> this.hudTweaksControls;
        };
    }

    private int categoryListWidth() {
        return Math.clamp(Math.round(this.owner.contentWidth() * 0.28f), 92, 150);
    }

    private int categoryItemHeight() {
        int available = this.owner.contentHeight();
        return Math.clamp(available / Category.values().length - 1, 13, 18);
    }

    private int cardLeft() {
        return this.owner.contentLeft() + categoryListWidth() + Theme.GAP_L;
    }

    private int sectionViewportLeft() {
        return cardLeft() + CARD_PADDING;
    }

    private int sectionViewportTop() {
        return this.owner.contentTop() + CARD_HEADER_H;
    }

    private int sectionViewportBottom() {
        return this.owner.contentBottom() - Theme.GAP_S;
    }

    private int sectionViewportWidth() {
        return this.owner.contentRight() - CARD_PADDING - sectionViewportLeft();
    }

    private int sectionViewportHeight() {
        return sectionViewportBottom() - sectionViewportTop();
    }

    private int rowY(int index) {
        return sectionViewportTop() + (index * ROW_GAP);
    }

    private Button button(String label, Button.OnPress action, int x, int y, int width, int height) {
        return Button.builder(Component.literal(label), action)
                .bounds(x, y, width, height)
                .build();
    }

    private void register(AbstractWidget... widgets) {
        for (AbstractWidget widget : widgets) {
            this.configWidgets.add(widget);
            this.owner.addRenderableWidget(widget);
        }
    }

    private static void setVisible(AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }

    private static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static boolean contains(double mx, double my, AbstractWidget widget) {
        return widget != null
                && widget.visible
                && contains(mx, my, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
    }

    private static int cycleBridgingOutlineColor(int current, boolean forward) {
        int[] colors = new int[]{0x66000000, 0x6699E2FF, 0x66FFCC33, 0x66FF6B6B, 0x66FFFFFF, 0x6691FF8A};
        int index = 0;
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == current) {
                index = i;
                break;
            }
        }
        int next = forward ? index + 1 : index - 1;
        if (next < 0) {
            next = colors.length - 1;
        }
        if (next >= colors.length) {
            next = 0;
        }
        return colors[next];
    }

    private static <T extends Enum<T>> T cycleEnum(T current, int direction) {
        T[] values = current.getDeclaringClass().getEnumConstants();
        int next = (current.ordinal() + (direction > 0 ? 1 : -1) + values.length) % values.length;
        return values[next];
    }

    private static MouseTweaksWheelSearchOrder nextWheelSearchOrder(MouseTweaksWheelSearchOrder current) {
        return current == MouseTweaksWheelSearchOrder.FIRST_TO_LAST
                ? MouseTweaksWheelSearchOrder.LAST_TO_FIRST
                : MouseTweaksWheelSearchOrder.FIRST_TO_LAST;
    }

    private static MouseTweaksWheelScrollDirection nextWheelScrollDirection(MouseTweaksWheelScrollDirection current) {
        MouseTweaksWheelScrollDirection[] values = MouseTweaksWheelScrollDirection.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static MouseTweaksScrollItemScaling nextScrollScaling(MouseTweaksScrollItemScaling current) {
        return current == MouseTweaksScrollItemScaling.PROPORTIONAL
                ? MouseTweaksScrollItemScaling.ALWAYS_ONE
                : MouseTweaksScrollItemScaling.PROPORTIONAL;
    }
}
