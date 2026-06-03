package me.m0dii.modules.macros.gui;

import me.m0dii.modules.bridging.BridgingTweaksModule;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.commandblockui.BetterCommandBlockUiModule;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.heldlight.HeldLightModule;
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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class MacroWorkbenchConfigurationTab {
    private static final int TOP_BAR_H = 54;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 24;
    private static final int CONTENT_START_Y = TOP_BAR_H + 34;

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
        BRIDGING_TWEAKS("Bridging Tweaks", "Reacharound block placement, outline, crosshair, and targeting rules.");

        private final String label;
        private final String description;

        Category(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private final MacroWorkbenchScreen owner;
    private final List<ClickableWidget> configWidgets;
    private final BooleanSupplier shiftDown;
    private final EnumMap<Category, ButtonWidget> categoryButtons = new EnumMap<>(Category.class);
    private final EnumMap<Category, List<ClickableWidget>> categoryWidgets = new EnumMap<>(Category.class);

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

    private int selectedRegexIndex = -1;
    private int regexScroll = 0;

    MacroWorkbenchConfigurationTab(MacroWorkbenchScreen owner,
                                   List<ClickableWidget> configWidgets,
                                   BooleanSupplier shiftDown) {
        this.owner = owner;
        this.configWidgets = configWidgets;
        this.shiftDown = shiftDown;
    }

    void initWidgets() {
        int listX = 12;
        int listY = TOP_BAR_H + 18;
        int categoryW = Math.max(130, (this.owner.width / 2) - 28);
        int rightX = (this.owner.width / 2) + 12;
        int settingW = Math.max(180, this.owner.width - rightX - 12);

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
        initCategoryWidgetGroups();
        register(this.categoryButtons.values().toArray(new ClickableWidget[0]));
        for (List<ClickableWidget> widgets : this.categoryWidgets.values()) {
            register(widgets.toArray(new ClickableWidget[0]));
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
        syncCategoryVisibility();
    }

    void render(DrawContext context) {
        syncControls();
        int splitX = this.owner.width / 2;
        context.fill(splitX - 1, TOP_BAR_H + 4, splitX, this.owner.height - 8, 0x50FFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Categories", 12, TOP_BAR_H + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "Configuration", splitX + 12, TOP_BAR_H + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(this.owner.workbenchTextRenderer(), this.category.description, splitX + 12, TOP_BAR_H + 18, 0xFFA8CFCF);

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

    boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        ControlSection section = activeSection();
        return section != null && section.handleMouseScroll(mouseX, mouseY, verticalAmount);
    }

    private void renderSecondaryRegexList(DrawContext context) {
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
            if (this.owner.workbenchTextRenderer().getWidth(text) > w - 6) {
                while (text.length() > 8 && this.owner.workbenchTextRenderer().getWidth(text + "...") > w - 6) {
                    text = text.substring(0, text.length() - 1);
                }
                text += "...";
            }
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), text, x + 3, yy + 2, 0xFFE0E0E0);
        }

        if (regexes.isEmpty()) {
            context.drawTextWithShadow(this.owner.workbenchTextRenderer(), "(none)", x + 3, y + 3, 0xFF909090);
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
        int index = 0;
        for (Category category : Category.values()) {
            ButtonWidget widget = button(category.label, b -> setCategory(category), listX, listY + (ROW_GAP * index), categoryW, ROW_H);
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
    }

    private void addCategoryWidgets(Category category, ClickableWidget... widgets) {
        List<ClickableWidget> list = this.categoryWidgets.computeIfAbsent(category, ignored -> new ArrayList<>());
        for (ClickableWidget widget : widgets) {
            list.add(widget);
        }
    }

    private void addCategoryWidgets(Category category, List<? extends ClickableWidget> widgets) {
        List<ClickableWidget> list = this.categoryWidgets.computeIfAbsent(category, ignored -> new ArrayList<>());
        list.addAll(widgets);
    }

    private void syncCategoryButtons() {
        for (Category category : Category.values()) {
            ButtonWidget widget = this.categoryButtons.get(category);
            if (widget != null) {
                widget.setMessage(Text.literal((this.category == category ? "> " : "") + category.label));
            }
        }
    }

    private void syncCategoryVisibility() {
        for (Category category : Category.values()) {
            ControlSection section = sectionFor(category);
            if (section != null) {
                section.layout(this.category == category);
                continue;
            }
            boolean visible = this.category == category;
            List<ClickableWidget> widgets = this.categoryWidgets.get(category);
            if (widgets == null) {
                continue;
            }
            for (ClickableWidget widget : widgets) {
                setVisible(widget, visible);
            }
        }
    }

    private final class BoundButton {
        private final ButtonWidget widget;
        private final Supplier<Text> messageSupplier;

        private BoundButton(ButtonWidget widget, Supplier<Text> messageSupplier) {
            this.widget = widget;
            this.messageSupplier = messageSupplier;
        }

        private void sync() {
            this.widget.setMessage(this.messageSupplier.get());
        }
    }

    private abstract class ControlSection {
        private final class PositionedWidget {
            private final ClickableWidget widget;
            private final int baseX;
            private final int baseY;

            private PositionedWidget(ClickableWidget widget, int baseX, int baseY) {
                this.widget = widget;
                this.baseX = baseX;
                this.baseY = baseY;
            }
        }

        private final List<BoundButton> boundButtons = new ArrayList<>();
        private final List<PositionedWidget> positionedWidgets = new ArrayList<>();
        private int scrollPixels = 0;

        protected abstract void init(int rightX, int settingW);

        final List<ClickableWidget> widgets() {
            List<ClickableWidget> widgets = new ArrayList<>(this.positionedWidgets.size());
            for (PositionedWidget positionedWidget : this.positionedWidgets) {
                widgets.add(positionedWidget.widget);
            }
            return widgets;
        }

        protected final ButtonWidget addButton(Supplier<Text> messageSupplier,
                                               Runnable action,
                                               int x,
                                               int y,
                                               int width,
                                               int height) {
            ButtonWidget widget = ButtonWidget.builder(messageSupplier.get(), b -> {
                action.run();
                syncControls();
            }).dimensions(x, y, width, height).build();
            this.boundButtons.add(new BoundButton(widget, messageSupplier));
            this.positionedWidgets.add(new PositionedWidget(widget, x, y));
            return widget;
        }

        protected final ButtonWidget addButton(Supplier<Text> messageSupplier,
                                               Runnable action,
                                               int rightX,
                                               int settingW,
                                               int rowIndex) {
            return addButton(messageSupplier, action, rightX, rowY(rowIndex), settingW, ROW_H);
        }

        protected final TextFieldWidget addTextField(int x, int y, int width, int height, String placeholder) {
            TextFieldWidget field = new TextFieldWidget(owner.workbenchTextRenderer(), x, y, width, height, Text.literal(placeholder));
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
                ClickableWidget widget = positionedWidget.widget;
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
        private ButtonWidget macroOverlayToggleButton;
        private ButtonWidget nbtHudToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.macroOverlayToggleButton = addButton(
                    () -> Text.literal("Macro Keybind HUD: " + (ModConfig.showMacroKeybindOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = !ModConfig.showMacroKeybindOverlay),
                    rightX, settingW, 0
            );
            this.nbtHudToggleButton = addButton(
                    () -> Text.literal("NBT Inspector HUD: " + (NBTInfoHudModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> NBTInfoHudModule.INSTANCE.setEnabled(!NBTInfoHudModule.INSTANCE.isEnabled()),
                    rightX, settingW, 1
            );
        }
    }

    private final class SecondaryChatControls extends ControlSection {
        private ButtonWidget secondaryEnabledToggleButton;
        private ButtonWidget secondaryOverlayToggleButton;
        private ButtonWidget secondaryInterceptModeButton;
        private ButtonWidget secondaryRegexAddButton;
        private ButtonWidget secondaryRegexApplyButton;
        private ButtonWidget secondaryRegexRemoveButton;
        private ButtonWidget secondaryRegexClearButton;
        private ButtonWidget secondaryOutgoingApplyButton;
        private TextFieldWidget secondaryRegexInputField;
        private TextFieldWidget secondaryOutgoingRegexField;

        @Override
        protected void init(int rightX, int settingW) {
            this.secondaryEnabledToggleButton = addButton(
                    () -> Text.literal("Secondary Chat: " + (SecondaryChatModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> SecondaryChatModule.INSTANCE.setEnabled(!SecondaryChatModule.INSTANCE.isEnabled()),
                    rightX, settingW, 0
            );
            this.secondaryOverlayToggleButton = addButton(
                    () -> Text.literal("Secondary Overlay: " + (SecondaryChatSettings.get().showOverlay ? "ON" : "OFF")),
                    () -> SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().showOverlay = !SecondaryChatSettings.get().showOverlay),
                    rightX, settingW, 1
            );
            this.secondaryInterceptModeButton = addButton(
                    () -> Text.literal("Intercept Mode: " + (SecondaryChatSettings.get().interceptMode == null ? "COPY" : SecondaryChatSettings.get().interceptMode.name())),
                    () -> SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().interceptMode =
                            SecondaryChatSettings.get().interceptMode == SecondaryChatSettings.InterceptMode.COPY
                                    ? SecondaryChatSettings.InterceptMode.MOVE
                                    : SecondaryChatSettings.InterceptMode.COPY),
                    rightX, settingW, 2
            );
            this.secondaryRegexInputField = addTextField(rightX, rowY(3), settingW - 114, ROW_H, "Regex");
            this.secondaryRegexInputField.setMaxLength(180);
            this.secondaryRegexAddButton = addButton(
                    () -> Text.literal("Add"),
                    () -> {
                        String regex = safeField(this.secondaryRegexInputField);
                        if (!regex.isEmpty()) {
                            SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(regex));
                            this.secondaryRegexInputField.setText("");
                            selectedRegexIndex = SecondaryChatSettings.get().regexList.size() - 1;
                            ensureRegexSelectionInBounds();
                        }
                    },
                    rightX + settingW - 110, rowY(3), 110, ROW_H
            );
            this.secondaryRegexApplyButton = addButton(
                    () -> Text.literal("Apply Selected"),
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
                    rightX, rowY(9), 140, ROW_H
            );
            this.secondaryRegexRemoveButton = addButton(
                    () -> Text.literal("Remove Selected"),
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
                    rightX + 144, rowY(9), 140, ROW_H
            );
            this.secondaryRegexClearButton = addButton(
                    () -> Text.literal("Clear All"),
                    () -> {
                        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList = new ArrayList<>());
                        selectedRegexIndex = -1;
                        regexScroll = 0;
                    },
                    rightX + 288, rowY(9), 100, ROW_H
            );
            this.secondaryOutgoingRegexField = addTextField(rightX, rowY(10), settingW - 110, ROW_H, "Outgoing regex");
            this.secondaryOutgoingRegexField.setMaxLength(180);
            this.secondaryOutgoingApplyButton = addButton(
                    () -> Text.literal("Apply"),
                    () -> {
                        String outgoing = safeField(this.secondaryOutgoingRegexField);
                        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().outgoingRegex = outgoing);
                    },
                    rightX + settingW - 106, rowY(10), 106, ROW_H
            );
        }

        @Override
        protected void syncCustom() {
            ensureRegexSelectionInBounds();
            SecondaryChatSettings.Data secondary = SecondaryChatSettings.get();
            if (!this.secondaryOutgoingRegexField.isFocused()) {
                this.secondaryOutgoingRegexField.setText(secondary.outgoingRegex == null ? "" : secondary.outgoingRegex);
            }
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != 0) {
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
                this.secondaryRegexInputField.setText(regexes.get(index));
                this.secondaryRegexInputField.setCursorToEnd(false);
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

    private final class PickupFeedControls extends ControlSection {
        private ButtonWidget pickupEnabledButton;
        private ButtonWidget pickupDurationButton;
        private ButtonWidget pickupLinesButton;
        private ButtonWidget pickupIconScaleButton;
        private ButtonWidget pickupDirectionButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.pickupEnabledButton = addButton(
                    () -> Text.literal("Pickup Notifier: " + (ItemPickupNotifierModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ItemPickupNotifierModule.INSTANCE.setEnabled(!ItemPickupNotifierModule.INSTANCE.isEnabled()),
                    rightX, settingW, 0
            );
            this.pickupDurationButton = addButton(
                    () -> Text.literal("Pickup Duration: " + PickupFeedSettings.get().durationMs + "ms"),
                    () -> adjustDuration(1),
                    rightX, settingW, 1
            );
            this.pickupLinesButton = addButton(
                    () -> Text.literal("Pickup Max Lines: " + PickupFeedSettings.get().maxLines),
                    () -> adjustLines(1),
                    rightX, settingW, 2
            );
            this.pickupIconScaleButton = addButton(
                    () -> Text.literal("Pickup Icon Scale: " + String.format(Locale.ROOT, "%.2f", PickupFeedSettings.get().iconScale)),
                    () -> adjustIconScale(1),
                    rightX, settingW, 3
            );
            this.pickupDirectionButton = addButton(
                    () -> Text.literal("Pickup Direction: " + PickupFeedSettings.get().direction.name()),
                    () -> cycleDirection(true),
                    rightX, settingW, 4
            );
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button == 0 || button == 1) {
                int direction = button == 0 ? 1 : -1;
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
        private ButtonWidget freecamToggleButton;
        private ButtonWidget fullbrightToggleButton;
        private ButtonWidget heldLightToggleButton;
        private ButtonWidget inventoryMoveToggleButton;
        private ButtonWidget instantBreakToggleButton;
        private ButtonWidget fastBlockPlacementToggleButton;
        private ButtonWidget betterCommandBlockUiToggleButton;
        private ButtonWidget waypointsToggleButton;
        private ButtonWidget nbtTooltipToggleButton;
        private ButtonWidget shulkerTooltipToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.freecamToggleButton = addButton(() -> Text.literal("Freecam: " + (FreecamModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FreecamModule.INSTANCE.setEnabled(!FreecamModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.fullbrightToggleButton = addButton(() -> Text.literal("Fullbright: " + (FullbrightModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FullbrightModule.INSTANCE.setEnabled(!FullbrightModule.INSTANCE.isEnabled()), rightX, settingW, 1);
            this.heldLightToggleButton = addButton(() -> Text.literal("Held Light: " + (HeldLightModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> HeldLightModule.INSTANCE.setEnabled(!HeldLightModule.INSTANCE.isEnabled()), rightX, settingW, 2);
            this.inventoryMoveToggleButton = addButton(() -> Text.literal("Inventory Move: " + (InventoryMoveModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> InventoryMoveModule.INSTANCE.setEnabled(!InventoryMoveModule.INSTANCE.isEnabled()), rightX, settingW, 3);
            this.instantBreakToggleButton = addButton(() -> Text.literal("Instant Break: " + (InstantBreakModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> InstantBreakModule.INSTANCE.setEnabled(!InstantBreakModule.INSTANCE.isEnabled()), rightX, settingW, 4);
            this.fastBlockPlacementToggleButton = addButton(() -> Text.literal("Fast Place: " + (FastBlockPlacementModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> FastBlockPlacementModule.INSTANCE.setEnabled(!FastBlockPlacementModule.INSTANCE.isEnabled()), rightX, settingW, 5);
            this.betterCommandBlockUiToggleButton = addButton(() -> Text.literal("Better Command Block UI: " + (BetterCommandBlockUiModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> BetterCommandBlockUiModule.INSTANCE.setEnabled(!BetterCommandBlockUiModule.INSTANCE.isEnabled()), rightX, settingW, 6);
            this.waypointsToggleButton = addButton(() -> Text.literal("Waypoints: " + (WaypointModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> WaypointModule.INSTANCE.setEnabled(!WaypointModule.INSTANCE.isEnabled()), rightX, settingW, 7);
            this.nbtTooltipToggleButton = addButton(() -> Text.literal("NBT Tooltip: " + (NBTTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> NBTTooltipModule.INSTANCE.setEnabled(!NBTTooltipModule.INSTANCE.isEnabled()), rightX, settingW, 8);
            this.shulkerTooltipToggleButton = addButton(() -> Text.literal("Shulker Preview Tooltip: " + (ShulkerTooltipModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ShulkerTooltipModule.INSTANCE.setEnabled(!ShulkerTooltipModule.INSTANCE.isEnabled()), rightX, settingW, 9);
        }
    }

    private final class OverlaysControls extends ControlSection {
        private ButtonWidget biomeBorderToggleButton;
        private ButtonWidget chunkBorderToggleButton;
        private ButtonWidget slimeChunksToggleButton;
        private ButtonWidget structureBoundsToggleButton;
        private ButtonWidget commandBlockOverlayToggleButton;
        private ButtonWidget lightOverlayToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.biomeBorderToggleButton = addButton(() -> Text.literal("Biome Border Overlay: " + (BiomeBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> BiomeBorderOverlayModule.INSTANCE.setEnabled(!BiomeBorderOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.chunkBorderToggleButton = addButton(() -> Text.literal("Chunk Border Overlay: " + (ChunkBorderOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ChunkBorderOverlayModule.INSTANCE.setEnabled(!ChunkBorderOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 1);
            this.slimeChunksToggleButton = addButton(() -> Text.literal("Slime Chunk Overlay: " + (SlimeChunkOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> SlimeChunkOverlayModule.INSTANCE.setEnabled(!SlimeChunkOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 2);
            this.structureBoundsToggleButton = addButton(() -> Text.literal("Structure Bounding Boxes: " + (StructureBoundingBoxOverlay.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> StructureBoundingBoxOverlay.INSTANCE.setEnabled(!StructureBoundingBoxOverlay.INSTANCE.isEnabled()), rightX, settingW, 3);
            this.commandBlockOverlayToggleButton = addButton(() -> Text.literal("Command Block Overlay: " + (CommandBlockOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> CommandBlockOverlayModule.INSTANCE.setEnabled(!CommandBlockOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 4);
            this.lightOverlayToggleButton = addButton(() -> Text.literal("Light Level Overlay: " + (LightLevelOverlayModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> LightLevelOverlayModule.INSTANCE.setEnabled(!LightLevelOverlayModule.INSTANCE.isEnabled()), rightX, settingW, 5);
        }
    }

    private final class BlockAttributesControls extends ControlSection {
        private ButtonWidget collisionMeshToggleButton;
        private ButtonWidget lightBlocksToggleButton;
        private ButtonWidget preventInteractionsToggleButton;
        private ButtonWidget solidFluidHitboxesToggleButton;
        private ButtonWidget barrierBlocksToggleButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.collisionMeshToggleButton = addButton(() -> Text.literal("Show Collision Mesh: " + (ModConfig.blockAttributesShowCollisionMesh ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowCollisionMesh = !ModConfig.blockAttributesShowCollisionMesh), rightX, settingW, 0);
            this.lightBlocksToggleButton = addButton(() -> Text.literal("Show Light Blocks: " + (ModConfig.blockAttributesShowLightBlocks ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowLightBlocks = !ModConfig.blockAttributesShowLightBlocks), rightX, settingW, 1);
            this.preventInteractionsToggleButton = addButton(() -> Text.literal("Prevent Interactions: " + (ModConfig.blockAttributesPreventInteractions ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesPreventInteractions = !ModConfig.blockAttributesPreventInteractions), rightX, settingW, 2);
            this.solidFluidHitboxesToggleButton = addButton(() -> Text.literal("Solid Fluid Hitboxes: " + (ModConfig.blockAttributesSolidFluidHitboxes ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesSolidFluidHitboxes = !ModConfig.blockAttributesSolidFluidHitboxes), rightX, settingW, 3);
            this.barrierBlocksToggleButton = addButton(() -> Text.literal("Show Barrier Blocks: " + (ModConfig.blockAttributesShowBarrierBlocks ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.blockAttributesShowBarrierBlocks = !ModConfig.blockAttributesShowBarrierBlocks), rightX, settingW, 4);
        }
    }

    private final class TweaksControls extends ControlSection {
        private ButtonWidget tweaksModuleToggleButton;
        private ButtonWidget hideOwnEffectsToggleButton;
        private ButtonWidget hideOffhandItemToggleButton;
        private ButtonWidget disableBlockBreakParticlesToggleButton;
        private ButtonWidget disableEntityRenderingToggleButton;
        private ButtonWidget disableNetherFogToggleButton;
        private ButtonWidget disableRenderDistanceFogToggleButton;
        private ButtonWidget disableRainEffectsToggleButton;
        private ButtonWidget disableSoundsToggleButton;
        private ButtonWidget disableWallUnsprintToggleButton;
        private ButtonWidget angelBlockToggleButton;
        private ButtonWidget permanentSneakToggleButton;
        private ButtonWidget permanentSprintToggleButton;
        private ButtonWidget disableHurtCameraToggleButton;
        private ButtonWidget disableViewBobbingToggleButton;
        private ButtonWidget reachToggleButton;
        private ButtonWidget reachSafeClampToggleButton;
        private ButtonWidget reachBlockDistanceButton;
        private ButtonWidget reachEntityDistanceButton;
        private ButtonWidget reachMpBlockExtraButton;
        private ButtonWidget reachMpEntityExtraButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.tweaksModuleToggleButton = addButton(() -> Text.literal("Tweaks Module: " + (TweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> TweaksModule.INSTANCE.setEnabled(!TweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.hideOwnEffectsToggleButton = addButton(() -> Text.literal("Hide Own Effect Particles: " + (ModConfig.tweaksHideOwnEffectParticles ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOwnEffectParticles = !ModConfig.tweaksHideOwnEffectParticles), rightX, settingW, 1);
            this.hideOffhandItemToggleButton = addButton(() -> Text.literal("Hide Offhand Item: " + (ModConfig.tweaksHideOffhandItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOffhandItem = !ModConfig.tweaksHideOffhandItem), rightX, settingW, 2);
            this.disableBlockBreakParticlesToggleButton = addButton(() -> Text.literal("Disable Block Breaking Particles: " + (ModConfig.tweaksDisableBlockBreakingParticles ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableBlockBreakingParticles = !ModConfig.tweaksDisableBlockBreakingParticles), rightX, settingW, 3);
            this.disableEntityRenderingToggleButton = addButton(() -> Text.literal("Disable Entity Rendering: " + (ModConfig.tweaksDisableEntityRendering ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableEntityRendering = !ModConfig.tweaksDisableEntityRendering), rightX, settingW, 4);
            this.disableNetherFogToggleButton = addButton(() -> Text.literal("Disable Nether Fog: " + (ModConfig.tweaksDisableNetherFog ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableNetherFog = !ModConfig.tweaksDisableNetherFog), rightX, settingW, 5);
            this.disableRenderDistanceFogToggleButton = addButton(() -> Text.literal("Disable Render-Distance Fog: " + (ModConfig.tweaksDisableRenderDistanceFog ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRenderDistanceFog = !ModConfig.tweaksDisableRenderDistanceFog), rightX, settingW, 6);
            this.disableRainEffectsToggleButton = addButton(() -> Text.literal("Disable Rain Effects: " + (ModConfig.tweaksDisableRainEffects ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRainEffects = !ModConfig.tweaksDisableRainEffects), rightX, settingW, 7);
            this.disableSoundsToggleButton = addButton(() -> Text.literal("Disable Sounds: " + (ModConfig.tweaksDisableSounds ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableSounds = !ModConfig.tweaksDisableSounds), rightX, settingW, 8);
            this.disableWallUnsprintToggleButton = addButton(() -> Text.literal("Disable Wall Unsprint: " + (ModConfig.tweaksDisableWallUnsprint ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableWallUnsprint = !ModConfig.tweaksDisableWallUnsprint), rightX, settingW, 9);
            this.angelBlockToggleButton = addButton(() -> Text.literal("Angel Block: " + (ModConfig.tweaksAngelBlock ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksAngelBlock = !ModConfig.tweaksAngelBlock), rightX, settingW, 10);
            this.permanentSneakToggleButton = addButton(() -> Text.literal("Permanent Sneak: " + (ModConfig.tweaksPermanentSneak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSneak = !ModConfig.tweaksPermanentSneak), rightX, settingW, 11);
            this.permanentSprintToggleButton = addButton(() -> Text.literal("Permanent Sprint: " + (ModConfig.tweaksPermanentSprint ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSprint = !ModConfig.tweaksPermanentSprint), rightX, settingW, 12);
            this.disableHurtCameraToggleButton = addButton(() -> Text.literal("Disable Hurt Camera: " + (ModConfig.tweaksDisableHurtCamera ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableHurtCamera = !ModConfig.tweaksDisableHurtCamera), rightX, settingW, 13);
            this.disableViewBobbingToggleButton = addButton(() -> Text.literal("Disable View Bobbing: " + (ModConfig.tweaksDisableViewBobbing ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableViewBobbing = !ModConfig.tweaksDisableViewBobbing), rightX, settingW, 14);
            this.reachToggleButton = addButton(() -> Text.literal("Reach: " + (ReachModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> ReachModule.INSTANCE.setEnabled(!ReachModule.INSTANCE.isEnabled()), rightX, settingW, 15);
            this.reachSafeClampToggleButton = addButton(() -> Text.literal("Reach Safe Multiplayer Clamp: " + (ModConfig.reachSafeMultiplayerClamp ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.reachSafeMultiplayerClamp = !ModConfig.reachSafeMultiplayerClamp), rightX, settingW, 16);
            this.reachBlockDistanceButton = addButton(() -> Text.literal("Reach Block Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachBlockDistance)),
                    () -> adjustReachBlockDistance(1), rightX, settingW, 17);
            this.reachEntityDistanceButton = addButton(() -> Text.literal("Reach Entity Distance: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachEntityDistance)),
                    () -> adjustReachEntityDistance(1), rightX, settingW, 18);
            this.reachMpBlockExtraButton = addButton(() -> Text.literal("Reach MP Block Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerBlockExtra)),
                    () -> adjustReachMpBlockExtra(1), rightX, settingW, 19);
            this.reachMpEntityExtraButton = addButton(() -> Text.literal("Reach MP Entity Extra: " + String.format(Locale.ROOT, "%.2f", ModConfig.reachMultiplayerEntityExtra)),
                    () -> adjustReachMpEntityExtra(1), rightX, settingW, 20);
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != 1) {
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
        private ButtonWidget mouseTweaksModuleToggleButton;
        private ButtonWidget mouseTweaksRmbToggleButton;
        private ButtonWidget mouseTweaksLmbWithItemToggleButton;
        private ButtonWidget mouseTweaksLmbWithoutItemToggleButton;
        private ButtonWidget mouseTweaksWheelToggleButton;
        private ButtonWidget mouseTweaksWheelSearchOrderButton;
        private ButtonWidget mouseTweaksWheelScrollDirectionButton;
        private ButtonWidget mouseTweaksScrollScalingButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.mouseTweaksModuleToggleButton = addButton(() -> Text.literal("Mouse Tweaks Module: " + (MouseTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> MouseTweaksModule.INSTANCE.setEnabled(!MouseTweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.mouseTweaksRmbToggleButton = addButton(() -> Text.literal("RMB Tweak: " + (ModConfig.mouseTweaksRmbTweak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksRmbTweak = !ModConfig.mouseTweaksRmbTweak), rightX, settingW, 1);
            this.mouseTweaksLmbWithItemToggleButton = addButton(() -> Text.literal("LMB Tweak With Item: " + (ModConfig.mouseTweaksLmbTweakWithItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithItem = !ModConfig.mouseTweaksLmbTweakWithItem), rightX, settingW, 2);
            this.mouseTweaksLmbWithoutItemToggleButton = addButton(() -> Text.literal("LMB Tweak Without Item: " + (ModConfig.mouseTweaksLmbTweakWithoutItem ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithoutItem = !ModConfig.mouseTweaksLmbTweakWithoutItem), rightX, settingW, 3);
            this.mouseTweaksWheelToggleButton = addButton(() -> Text.literal("Wheel Tweak: " + (ModConfig.mouseTweaksWheelTweak ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelTweak = !ModConfig.mouseTweaksWheelTweak), rightX, settingW, 4);
            this.mouseTweaksWheelSearchOrderButton = addButton(() -> Text.literal("Wheel Search Order: " + ModConfig.mouseTweaksWheelSearchOrder),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelSearchOrder = nextWheelSearchOrder(ModConfig.mouseTweaksWheelSearchOrder)), rightX, settingW, 5);
            this.mouseTweaksWheelScrollDirectionButton = addButton(() -> Text.literal("Wheel Scroll Direction: " + ModConfig.mouseTweaksWheelScrollDirection),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelScrollDirection = nextWheelScrollDirection(ModConfig.mouseTweaksWheelScrollDirection)), rightX, settingW, 6);
            this.mouseTweaksScrollScalingButton = addButton(() -> Text.literal("Scroll Item Scaling: " + ModConfig.mouseTweaksScrollItemScaling),
                    () -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksScrollItemScaling = nextScrollScaling(ModConfig.mouseTweaksScrollItemScaling)), rightX, settingW, 7);
        }
    }

    private final class HungerTweaksControls extends ControlSection {
        private ButtonWidget hungerTweaksModuleToggleButton;
        private ButtonWidget hungerTooltipToggleButton;
        private ButtonWidget hungerTooltipAlwaysToggleButton;
        private ButtonWidget hungerSaturationOverlayToggleButton;
        private ButtonWidget hungerFoodValuesOverlayToggleButton;
        private ButtonWidget hungerOffhandOverlayToggleButton;
        private ButtonWidget hungerExhaustionUnderlayToggleButton;
        private ButtonWidget hungerHealthOverlayToggleButton;
        private ButtonWidget hungerDebugInfoToggleButton;
        private ButtonWidget hungerVanillaAnimationToggleButton;
        private ButtonWidget hungerMaxFlashAlphaButton;

        @Override
        protected void init(int rightX, int settingW) {
            this.hungerTweaksModuleToggleButton = addButton(() -> Text.literal("Hunger Tweaks Module: " + (HungerTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF")),
                    () -> HungerTweaksModule.INSTANCE.setEnabled(!HungerTweaksModule.INSTANCE.isEnabled()), rightX, settingW, 0);
            this.hungerTooltipToggleButton = addButton(() -> Text.literal("Tooltip Food Values: " + (ModConfig.hungerTweaksShowFoodValuesInTooltip ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltip = !ModConfig.hungerTweaksShowFoodValuesInTooltip), rightX, settingW, 1);
            this.hungerTooltipAlwaysToggleButton = addButton(() -> Text.literal("Tooltip Always Visible: " + (ModConfig.hungerTweaksShowFoodValuesInTooltipAlways ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltipAlways = !ModConfig.hungerTweaksShowFoodValuesInTooltipAlways), rightX, settingW, 2);
            this.hungerSaturationOverlayToggleButton = addButton(() -> Text.literal("Saturation Overlay: " + (ModConfig.hungerTweaksShowSaturationHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowSaturationHudOverlay = !ModConfig.hungerTweaksShowSaturationHudOverlay), rightX, settingW, 3);
            this.hungerFoodValuesOverlayToggleButton = addButton(() -> Text.literal("Held Food Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlay = !ModConfig.hungerTweaksShowFoodValuesHudOverlay), rightX, settingW, 4);
            this.hungerOffhandOverlayToggleButton = addButton(() -> Text.literal("Offhand Overlay: " + (ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand = !ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand), rightX, settingW, 5);
            this.hungerExhaustionUnderlayToggleButton = addButton(() -> Text.literal("Exhaustion Underlay: " + (ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay = !ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay), rightX, settingW, 6);
            this.hungerHealthOverlayToggleButton = addButton(() -> Text.literal("Estimated Health Overlay: " + (ModConfig.hungerTweaksShowFoodHealthHudOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodHealthHudOverlay = !ModConfig.hungerTweaksShowFoodHealthHudOverlay), rightX, settingW, 7);
            this.hungerDebugInfoToggleButton = addButton(() -> Text.literal("Debug HUD Food Info: " + (ModConfig.hungerTweaksShowFoodDebugInfo ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodDebugInfo = !ModConfig.hungerTweaksShowFoodDebugInfo), rightX, settingW, 8);
            this.hungerVanillaAnimationToggleButton = addButton(() -> Text.literal("Match Vanilla Animation: " + (ModConfig.hungerTweaksShowVanillaAnimationsOverlay ? "ON" : "OFF")),
                    () -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowVanillaAnimationsOverlay = !ModConfig.hungerTweaksShowVanillaAnimationsOverlay), rightX, settingW, 9);
            this.hungerMaxFlashAlphaButton = addButton(() -> Text.literal("Max Flash Alpha: " + HungerTweaksModule.formatFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha)),
                    () -> adjustFlashAlpha(1), rightX, settingW, 10);
        }

        @Override
        protected boolean handleMouseClick(double mouseX, double mouseY, int button) {
            if (button != 0 && button != 1) {
                return false;
            }
            if (contains(mouseX, mouseY, this.hungerMaxFlashAlphaButton)) {
                adjustFlashAlpha(button == 0 ? 1 : -1);
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
        private final List<ButtonWidget> buttons = new ArrayList<>();

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
            if (button != 0 && button != 1) {
                return false;
            }
            int direction = button == 0 ? 1 : -1;
            for (int i = 0; i < this.buttons.size(); i++) {
                if (contains(mouseX, mouseY, this.buttons.get(i))) {
                    adjustSetting(i, direction);
                    return true;
                }
            }
            return false;
        }

        private Text bridgingMessage(int index) {
            return switch (index) {
                case 0 -> Text.literal("Bridging Tweaks: " + (BridgingTweaksModule.INSTANCE.isEnabled() ? "ON" : "OFF"));
                case 1 -> Text.literal("Minimum Distance: " + String.format(Locale.ROOT, "%.1f%%", ModConfig.bridgingMinBridgeDistance));
                case 2 -> Text.literal("Only When Crouched: " + (ModConfig.bridgingOnlyWhenCrouched ? "ON" : "OFF"));
                case 3 -> Text.literal("Bridge Axes: " + ModConfig.bridgingSupportedAxes);
                case 4 -> Text.literal("Crouched Axes: " + ModConfig.bridgingSupportedAxesWhenCrouched);
                case 5 -> Text.literal("Post Delay: " + ModConfig.bridgingDelayPostBridging + " ticks");
                case 6 -> Text.literal("Show Crosshair: " + (ModConfig.bridgingShowCrosshair ? "ON" : "OFF"));
                case 7 -> Text.literal("Show Outline: " + (ModConfig.bridgingShowOutline ? "ON" : "OFF"));
                case 8 -> Text.literal("Outline When Not Bridging: " + (ModConfig.bridgingShowOutlineWhenNotBridging ? "ON" : "OFF"));
                case 9 -> Text.literal("Outline Respects Crouch: " + (ModConfig.bridgingNonBridgeRespectsCrouchRules ? "ON" : "OFF"));
                case 10 -> Text.literal("Outline Color: " + String.format(Locale.ROOT, "#%08X", ModConfig.bridgingOutlineColor));
                case 11 -> Text.literal("Skip Torch Blocks: " + (ModConfig.bridgingSkipTorchBridging ? "ON" : "OFF"));
                case 12 -> Text.literal("Slab Assist: " + (ModConfig.bridgingEnableSlabAssist ? "ON" : "OFF"));
                case 13 -> Text.literal("Replaceable Targets: " + (ModConfig.bridgingEnableNonSolidReplace ? "ON" : "OFF"));
                case 14 -> Text.literal("Snap Strength: " + String.format(Locale.ROOT, "%.2f", ModConfig.bridgingSnapStrength));
                case 15 -> Text.literal("Adjacency: " + ModConfig.bridgingAdjacency);
                case 16 -> Text.literal("Perspective Lock: " + ModConfig.bridgingPerspectiveLock);
                case 17 -> Text.literal("Debug Highlight: " + (ModConfig.bridgingShowDebugHighlight ? "ON" : "OFF"));
                case 18 -> Text.literal("Debug Non-Bridging: " + (ModConfig.bridgingShowDebugNonBridgingHighlight ? "ON" : "OFF"));
                case 19 -> Text.literal("Debug Trace: " + (ModConfig.bridgingShowDebugTrace ? "ON" : "OFF"));
                default -> Text.literal("");
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

    private static String safeField(TextFieldWidget field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
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
        };
    }

    private int sectionViewportLeft() {
        return (this.owner.width / 2) + 12;
    }

    private int sectionViewportTop() {
        return CONTENT_START_Y;
    }

    private int sectionViewportBottom() {
        return this.owner.height - 30;
    }

    private int sectionViewportWidth() {
        return Math.max(180, this.owner.width - sectionViewportLeft() - 12);
    }

    private int sectionViewportHeight() {
        return sectionViewportBottom() - sectionViewportTop();
    }

    private int rowY(int index) {
        return CONTENT_START_Y + (index * ROW_GAP);
    }

    private ButtonWidget button(String label, ButtonWidget.PressAction action, int x, int y, int width, int height) {
        return ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, width, height)
                .build();
    }

    private void register(ClickableWidget... widgets) {
        for (ClickableWidget widget : widgets) {
            this.configWidgets.add(widget);
            this.owner.addDrawableChild(widget);
        }
    }

    private static void setVisible(ClickableWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }

    private static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static boolean contains(double mx, double my, ClickableWidget widget) {
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

