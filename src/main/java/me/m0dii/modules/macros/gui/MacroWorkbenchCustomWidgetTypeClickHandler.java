package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.MacroPlaceholderCatalog;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.gui.Click;

import java.util.List;

public final class MacroWorkbenchCustomWidgetTypeClickHandler {
    private MacroWorkbenchCustomWidgetTypeClickHandler() {
    }

    public interface Ops {
        boolean contains(Click click, UiRect rect);

        int stepInt(int base);

        double stepDouble(double base);

        String cyclePreset(String current, String[] presets, boolean forward);

        int cycleStyleColor(int current, boolean forward);

        String[] iconIdSuggestionsForKind(String kind);

        String getAdvancedAction();

        void setAdvancedAction(String value);

        void setAdvancedActionCursor(int value);

        void setAdvancedActionSuggestionScroll(int value);

        void openColorPicker(java.util.function.IntConsumer onPick, String title, int x, int y);
    }

    public static boolean handleTypeClick(Click click,
                                          CustomWidgetAdvancedLayout layout,
                                          boolean forward,
                                          MacroHudDataHandler.HudElement selected,
                                          String[] iconKindPresets,
                                          String[] listSourcePresets,
                                          String[] stateSourcePresets,
                                          String[] shapeTypePresets,
                                          Ops ops) {
        if (selected == null) {
            return false;
        }
        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            return handleIconClick(click, layout, forward, selected, iconKindPresets, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            return handleBarClick(click, layout, forward, selected, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            return handleValueClick(click, layout, forward, selected, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            return handleListClick(click, layout, forward, selected, listSourcePresets, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.INVENTORY) {
            return handleInventoryClick(click, layout, forward, selected, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            return handleShapeClick(click, layout, forward, selected, shapeTypePresets, ops);
        }
        if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            return handleStateBadgeClick(click, layout, forward, selected, stateSourcePresets, ops);
        }
        return false;
    }

    private static boolean handleInventoryClick(Click click,
                                                CustomWidgetAdvancedLayout layout,
                                                boolean forward,
                                                MacroHudDataHandler.HudElement selected,
                                                Ops ops) {
        if (!layout.typeWideTop().contains(click.x(), click.y())) {
            return false;
        }
        selected.inventoryDisplayMode = cycleInventoryDisplayMode(selected.inventoryDisplayMode, forward);
        applyInventoryDefaultSize(selected);
        String mode = selected.inventoryDisplayMode.name();
        ops.setAdvancedAction(mode);
        ops.setAdvancedActionCursor(mode.length());
        ops.setAdvancedActionSuggestionScroll(0);
        return true;
    }

    private static MacroHudDataHandler.InventoryDisplayMode cycleInventoryDisplayMode(
            MacroHudDataHandler.InventoryDisplayMode current,
            boolean forward
    ) {
        MacroHudDataHandler.InventoryDisplayMode[] values = MacroHudDataHandler.InventoryDisplayMode.values();
        int index = current == null ? 0 : current.ordinal();
        int next = forward ? (index + 1) : (index - 1);
        if (next < 0) {
            next = values.length - 1;
        }
        if (next >= values.length) {
            next = 0;
        }
        return values[next];
    }

    private static void applyInventoryDefaultSize(MacroHudDataHandler.HudElement selected) {
        MacroHudDataHandler.InventoryDisplayMode mode = selected.inventoryDisplayMode == null
                ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR
                : selected.inventoryDisplayMode;
        switch (mode) {
            case HOTBAR -> {
                selected.width = 182;
                selected.height = 22;
            }
            case INVENTORY -> {
                selected.width = 182;
                selected.height = 62;
            }
            case ARMOR -> {
                selected.width = 22;
                selected.height = 102;
            }
        }
    }

    private static boolean handleIconClick(Click click,
                                           CustomWidgetAdvancedLayout layout,
                                           boolean forward,
                                           MacroHudDataHandler.HudElement selected,
                                           String[] iconKindPresets,
                                           Ops ops) {
        List<UiRect> topButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(60, 1)
        );
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        List<UiRect> row3 = layout.typeRow3();
        if (ops.contains(click, topButtons.get(0))) {
            selected.iconKind = ops.cyclePreset(selected.iconKind, iconKindPresets, forward);
            if ("entity_model".equalsIgnoreCase(selected.iconKind)
                    && (StringUtils.safe(selected.iconId).isBlank()
                    || "minecraft:stone".equalsIgnoreCase(StringUtils.safe(selected.iconId)))) {
                selected.iconId = "minecraft:player";
            }
            return true;
        }
        if (ops.contains(click, topButtons.get(1))) {
            String[] ids = ops.iconIdSuggestionsForKind(selected.iconKind);
            selected.iconId = ops.cyclePreset(selected.iconId, ids, forward);
            return true;
        }
        if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
            if (ops.contains(click, row1.get(0))) {
                selected.modelZoom = Math.clamp((float) (selected.modelZoom - ops.stepDouble(0.05)), 0.2f, 2.5f);
                return true;
            }
            if (ops.contains(click, row1.get(1))) {
                selected.modelZoom = Math.clamp((float) (selected.modelZoom + ops.stepDouble(0.05)), 0.2f, 2.5f);
                return true;
            }
            if (ops.contains(click, row1.get(2))) {
                selected.modelYaw = Math.clamp((float) (selected.modelYaw - ops.stepDouble(5.0)), -180.0f, 180.0f);
                return true;
            }
            if (ops.contains(click, row1.get(3))) {
                selected.modelYaw = Math.clamp((float) (selected.modelYaw + ops.stepDouble(5.0)), -180.0f, 180.0f);
                return true;
            }
            if (ops.contains(click, row2.get(0))) {
                selected.modelPitch = Math.clamp((float) (selected.modelPitch - ops.stepDouble(5.0)), -90.0f, 90.0f);
                return true;
            }
            if (ops.contains(click, row2.get(1))) {
                selected.modelPitch = Math.clamp((float) (selected.modelPitch + ops.stepDouble(5.0)), -90.0f, 90.0f);
                return true;
            }
            if (ops.contains(click, row2.get(2))) {
                selected.modelOffsetX = Math.clamp(selected.modelOffsetX - ops.stepInt(1), -200, 200);
                return true;
            }
            if (ops.contains(click, row2.get(3))) {
                selected.modelOffsetX = Math.clamp(selected.modelOffsetX + ops.stepInt(1), -200, 200);
                return true;
            }
            if (ops.contains(click, row3.get(0))) {
                selected.modelOffsetY = Math.clamp(selected.modelOffsetY - ops.stepInt(1), -200, 200);
                return true;
            }
            if (ops.contains(click, row3.get(1))) {
                selected.modelOffsetY = Math.clamp(selected.modelOffsetY + ops.stepInt(1), -200, 200);
                return true;
            }
            if (ops.contains(click, row3.get(2))) {
                selected.modelAutoFit = !selected.modelAutoFit;
                return true;
            }
            if (ops.contains(click, row3.get(3))) {
                selected.modelFollowLook = !selected.modelFollowLook;
                return true;
            }
            return false;
        }
        if (ops.contains(click, row1.get(0))) {
            selected.iconShowCount = !selected.iconShowCount;
            return true;
        }
        if (ops.contains(click, row1.get(1))) {
            selected.iconShowDurability = !selected.iconShowDurability;
            return true;
        }
        if (ops.contains(click, row1.get(2))) {
            selected.iconShowCooldown = !selected.iconShowCooldown;
            return true;
        }
        return false;
    }

    private static boolean handleBarClick(Click click,
                                          CustomWidgetAdvancedLayout layout,
                                          boolean forward,
                                          MacroHudDataHandler.HudElement selected,
                                          Ops ops) {
        List<UiRect> top = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(110, 2), UiFlexLayout.Item.flex(60, 1)
        );
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        List<UiRect> row3 = layout.typeRow3();
        if (ops.contains(click, top.get(0))) {
            selected.sourceTokenMax = ops.cyclePreset(selected.sourceTokenMax, MacroPlaceholderCatalog.BAR_MAX_SOURCE_PRESETS, forward);
            return true;
        }
        if (ops.contains(click, top.get(1))) {
            selected.segmented = !selected.segmented;
            return true;
        }
        if (ops.contains(click, row1.get(0))) {
            selected.segments = Math.max(1, selected.segments - ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(1))) {
            selected.segments = Math.min(120, selected.segments + ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(2))) {
            selected.minValue -= ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, row1.get(3))) {
            selected.minValue += ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, row2.get(0))) {
            selected.maxValue = Math.max(selected.minValue + 1.0, selected.maxValue - ops.stepDouble(1.0));
            return true;
        }
        if (ops.contains(click, row2.get(1))) {
            selected.maxValue = selected.maxValue + ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, row2.get(2))) {
            selected.colorStart = ops.cycleStyleColor(selected.colorStart, false);
            return true;
        }
        if (ops.contains(click, row2.get(3))) {
            selected.colorStart = ops.cycleStyleColor(selected.colorStart, true);
            return true;
        }
        if (ops.contains(click, row3.get(0))) {
            selected.colorEnd = ops.cycleStyleColor(selected.colorEnd, false);
            return true;
        }
        if (ops.contains(click, row3.get(1))) {
            selected.colorEnd = ops.cycleStyleColor(selected.colorEnd, true);
            return true;
        }
        return false;
    }

    private static boolean handleValueClick(Click click,
                                            CustomWidgetAdvancedLayout layout,
                                            boolean forward,
                                            MacroHudDataHandler.HudElement selected,
                                            Ops ops) {
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        List<UiRect> row3 = layout.typeRow3();
        List<UiRect> presets = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
        );
        if (ops.contains(click, slot(row1, 0))) {
            selected.warnThreshold -= ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, slot(row1, 1))) {
            selected.warnThreshold += ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, slot(row1, 2))) {
            selected.critThreshold -= ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, slot(row1, 3))) {
            selected.critThreshold += ops.stepDouble(1.0);
            return true;
        }
        if (ops.contains(click, slot(row2, 0))) {
            if (forward) {
                selected.colorWarn = ops.cycleStyleColor(selected.colorWarn, false);
            } else {
                UiRect target = slot(row2, 0);
                ops.openColorPicker(color -> selected.colorWarn = color, "Pick Warn Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 1))) {
            if (forward) {
                selected.colorWarn = ops.cycleStyleColor(selected.colorWarn, true);
            } else {
                UiRect target = slot(row2, 1);
                ops.openColorPicker(color -> selected.colorWarn = color, "Pick Warn Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 2))) {
            if (forward) {
                selected.colorCrit = ops.cycleStyleColor(selected.colorCrit, false);
            } else {
                UiRect target = slot(row2, 2);
                ops.openColorPicker(color -> selected.colorCrit = color, "Pick Crit Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 3))) {
            if (forward) {
                selected.colorCrit = ops.cycleStyleColor(selected.colorCrit, true);
            } else {
                UiRect target = slot(row2, 3);
                ops.openColorPicker(color -> selected.colorCrit = color, "Pick Crit Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row3, 0))) {
            if (forward) {
                selected.backgroundColor = ops.cycleStyleColor(selected.backgroundColor, false);
            } else {
                UiRect target = slot(row3, 0);
                ops.openColorPicker(color -> {
                    selected.backgroundColor = color;
                    selected.backgroundAlpha = (color >>> 24) & 0xFF;
                }, "Pick BG", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row3, 1))) {
            if (forward) {
                selected.backgroundColor = ops.cycleStyleColor(selected.backgroundColor, true);
            } else {
                UiRect target = slot(row3, 1);
                ops.openColorPicker(color -> {
                    selected.backgroundColor = color;
                    selected.backgroundAlpha = (color >>> 24) & 0xFF;
                }, "Pick BG", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row3, 2))) {
            if (forward) {
                selected.textColor = ops.cycleStyleColor(selected.textColor, false);
            } else {
                UiRect target = slot(row3, 2);
                ops.openColorPicker(color -> selected.textColor = color, "Pick Text Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row3, 3))) {
            if (forward) {
                selected.textColor = ops.cycleStyleColor(selected.textColor, true);
            } else {
                UiRect target = slot(row3, 3);
                ops.openColorPicker(color -> selected.textColor = color, "Pick Text Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(presets, 0))) {
            selected.prefix = ops.cyclePreset(selected.prefix, new String[]{"", "HP: ", "Food: ", "FPS: "}, forward);
            return true;
        }
        if (ops.contains(click, slot(presets, 1))) {
            selected.suffix = ops.cyclePreset(selected.suffix, new String[]{"", "%", " hp", " ms"}, forward);
            return true;
        }
        return false;
    }

    private static boolean handleListClick(Click click,
                                           CustomWidgetAdvancedLayout layout,
                                           boolean forward,
                                           MacroHudDataHandler.HudElement selected,
                                           String[] listSourcePresets,
                                           Ops ops) {
        List<UiRect> row1 = layout.typeRow1();
        if (ops.contains(click, layout.typeWideTop())) {
            String next = ops.cyclePreset(ops.getAdvancedAction(), listSourcePresets, forward);
            ops.setAdvancedAction(next);
            ops.setAdvancedActionCursor(next.length());
            return true;
        }
        if (ops.contains(click, row1.get(0))) {
            selected.maxLines = Math.max(1, selected.maxLines - ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(1))) {
            selected.maxLines = Math.min(200, selected.maxLines + ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(2))) {
            selected.listScroll = Math.max(0, selected.listScroll - ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(3))) {
            selected.listScroll = Math.min(500, selected.listScroll + ops.stepInt(1));
            return true;
        }
        return false;
    }

    private static boolean handleShapeClick(Click click,
                                            CustomWidgetAdvancedLayout layout,
                                            boolean forward,
                                            MacroHudDataHandler.HudElement selected,
                                            String[] shapeTypePresets,
                                            Ops ops) {
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        if (ops.contains(click, layout.typeWideTop())) {
            selected.shapeType = ops.cyclePreset(selected.shapeType, shapeTypePresets, forward);
            return true;
        }
        if (ops.contains(click, row1.get(0))) {
            selected.shapeFilled = !selected.shapeFilled;
            return true;
        }
        if (ops.contains(click, row1.get(1))) {
            selected.shapeRadius = Math.max(0, selected.shapeRadius - ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row1.get(2))) {
            selected.shapeRadius = Math.min(64, selected.shapeRadius + ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row2.get(0))) {
            selected.shapeThickness = Math.max(1, selected.shapeThickness - ops.stepInt(1));
            return true;
        }
        if (ops.contains(click, row2.get(1))) {
            selected.shapeThickness = Math.min(24, selected.shapeThickness + ops.stepInt(1));
            return true;
        }
        return false;
    }

    private static boolean handleStateBadgeClick(Click click,
                                                 CustomWidgetAdvancedLayout layout,
                                                 boolean forward,
                                                 MacroHudDataHandler.HudElement selected,
                                                 String[] stateSourcePresets,
                                                 Ops ops) {
        List<UiRect> textButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
        );
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        if (ops.contains(click, slot(textButtons, 0))) {
            selected.stateOnText = ops.cyclePreset(selected.stateOnText, new String[]{"ON", "YES", "ENABLED", "ACTIVE"}, forward);
            return true;
        }
        if (ops.contains(click, slot(textButtons, 1))) {
            selected.stateOffText = ops.cyclePreset(selected.stateOffText, new String[]{"OFF", "NO", "DISABLED", "IDLE"}, forward);
            return true;
        }
        if (ops.contains(click, slot(row1, 0))) {
            if (forward) {
                selected.colorStart = ops.cycleStyleColor(selected.colorStart, false);
            } else {
                UiRect target = slot(row1, 0);
                ops.openColorPicker(color -> selected.colorStart = color, "Pick ON Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row1, 1))) {
            if (forward) {
                selected.colorStart = ops.cycleStyleColor(selected.colorStart, true);
            } else {
                UiRect target = slot(row1, 1);
                ops.openColorPicker(color -> selected.colorStart = color, "Pick ON Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row1, 2))) {
            if (forward) {
                selected.colorEnd = ops.cycleStyleColor(selected.colorEnd, false);
            } else {
                UiRect target = slot(row1, 2);
                ops.openColorPicker(color -> selected.colorEnd = color, "Pick OFF Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row1, 3))) {
            if (forward) {
                selected.colorEnd = ops.cycleStyleColor(selected.colorEnd, true);
            } else {
                UiRect target = slot(row1, 3);
                ops.openColorPicker(color -> selected.colorEnd = color, "Pick OFF Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 0))) {
            selected.stateShowValue = !selected.stateShowValue;
            return true;
        }
        if (ops.contains(click, slot(row2, 1))) {
            if (forward) {
                selected.textColor = ops.cycleStyleColor(selected.textColor, false);
            } else {
                UiRect target = slot(row2, 1);
                ops.openColorPicker(color -> selected.textColor = color, "Pick Text Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 2))) {
            if (forward) {
                selected.textColor = ops.cycleStyleColor(selected.textColor, true);
            } else {
                UiRect target = slot(row2, 2);
                ops.openColorPicker(color -> selected.textColor = color, "Pick Text Color", target.right() + 8, target.y() - 6);
            }
            return true;
        }
        if (ops.contains(click, slot(row2, 3))) {
            String next = ops.cyclePreset(ops.getAdvancedAction(), stateSourcePresets, forward);
            ops.setAdvancedAction(next);
            ops.setAdvancedActionCursor(next.length());
            ops.setAdvancedActionSuggestionScroll(0);
            return true;
        }
        return false;
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }
}
