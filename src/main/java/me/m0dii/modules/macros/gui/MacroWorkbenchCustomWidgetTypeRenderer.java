package me.m0dii.modules.macros.gui;

import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Locale;

public final class MacroWorkbenchCustomWidgetTypeRenderer {
    private MacroWorkbenchCustomWidgetTypeRenderer() {
    }

    @FunctionalInterface
    public interface SelectionRenderer {
        void draw(DrawContext context, int textX, int textY, String text, int anchor, int cursor);
    }

    public static void render(DrawContext context,
                              TextRenderer textRenderer,
                              MacroHudDataHandler.HudElement selected,
                              CustomWidgetAdvancedLayout layout,
                              int mouseX,
                              int mouseY,
                              String advancedBgColor,
                              int advancedBgSelectionAnchor,
                              int advancedBgCursor,
                              boolean advancedBgColorFocused,
                              String advancedBorderColor,
                              int advancedBorderSelectionAnchor,
                              int advancedBorderCursor,
                              boolean advancedBorderColorFocused,
                              SelectionRenderer selectionRenderer) {
        if (selected == null) {
            return;
        }

        if (selected.type == MacroHudDataHandler.ElementType.ICON) {
            renderIcon(context, textRenderer, selected, layout, mouseX, mouseY);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.BAR) {
            renderBar(context, textRenderer, selected, layout, mouseX, mouseY, advancedBgColor, advancedBgCursor,
                    advancedBgColorFocused, advancedBorderColor, advancedBorderCursor, advancedBorderColorFocused);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.VALUE) {
            renderValue(context, textRenderer, selected, layout, mouseX, mouseY,
                    advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor, advancedBgColorFocused,
                    advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor, advancedBorderColorFocused,
                    selectionRenderer);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.LIST) {
            renderList(context, textRenderer, selected, layout, mouseX, mouseY);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.INVENTORY) {
            renderInventory(context, textRenderer, selected, layout, mouseX, mouseY);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.SHAPE) {
            renderShape(context, textRenderer, selected, layout, mouseX, mouseY);
            return;
        }
        if (selected.type == MacroHudDataHandler.ElementType.STATE_BADGE) {
            renderStateBadge(context, textRenderer, selected, layout, mouseX, mouseY,
                    advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor, advancedBgColorFocused,
                    advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor, advancedBorderColorFocused,
                    selectionRenderer);
        }
    }

    private static void renderIcon(DrawContext context,
                                   TextRenderer textRenderer,
                                   MacroHudDataHandler.HudElement selected,
                                   CustomWidgetAdvancedLayout layout,
                                   int mouseX,
                                   int mouseY) {
        List<UiRect> topButtons = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(60, 1)
        );
        drawModalButton(context, textRenderer, topButtons.get(0), "Kind: " + selected.iconKind, mouseX, mouseY);
        drawModalButton(context, textRenderer, topButtons.get(1), "Pick Id", mouseX, mouseY);
        if ("entity_model".equalsIgnoreCase(selected.iconKind)) {
            List<UiRect> row1 = layout.typeRow1();
            List<UiRect> row2 = layout.typeRow2();
            List<UiRect> row3 = layout.typeRow3();
            drawModalButton(context, textRenderer, row1.get(0), "Z-", mouseX, mouseY);
            drawModalButton(context, textRenderer, row1.get(1), "Z+", mouseX, mouseY);
            drawModalButton(context, textRenderer, row1.get(2), "Y-", mouseX, mouseY);
            drawModalButton(context, textRenderer, row1.get(3), "Y+", mouseX, mouseY);
            drawModalButton(context, textRenderer, row2.get(0), "P-", mouseX, mouseY);
            drawModalButton(context, textRenderer, row2.get(1), "P+", mouseX, mouseY);
            drawModalButton(context, textRenderer, row2.get(2), "OX-", mouseX, mouseY);
            drawModalButton(context, textRenderer, row2.get(3), "OX+", mouseX, mouseY);
            drawModalButton(context, textRenderer, row3.get(0), "OY-", mouseX, mouseY);
            drawModalButton(context, textRenderer, row3.get(1), "OY+", mouseX, mouseY);
            drawModalButton(context, textRenderer, row3.get(2), "Fit: " + (selected.modelAutoFit ? "ON" : "OFF"), mouseX, mouseY);
            drawModalButton(context, textRenderer, row3.get(3), "Look: " + (selected.modelFollowLook ? "ON" : "OFF"), mouseX, mouseY);
            context.drawTextWithShadow(textRenderer, "Id: " + selected.iconId, layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFEAEAEA);
            context.drawTextWithShadow(textRenderer,
                    String.format(Locale.ROOT, "Zoom %.2f  Yaw %.0f  Pitch %.0f", selected.modelZoom, selected.modelYaw, selected.modelPitch),
                    layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
            context.drawTextWithShadow(textRenderer,
                    "Offset X: " + selected.modelOffsetX + "  Y: " + selected.modelOffsetY,
                    layout.typeInfo2().x(), layout.typeInfo2().y(), 0xFFEAEAEA);
            return;
        }
        List<UiRect> row1 = layout.typeRow1();
        drawModalButton(context, textRenderer, row1.get(0), "Count: " + (selected.iconShowCount ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(1), "Dur: " + (selected.iconShowDurability ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(2), "CD: " + (selected.iconShowCooldown ? "ON" : "OFF"), mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Id: " + selected.iconId, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
    }

    private static void renderBar(DrawContext context,
                                  TextRenderer textRenderer,
                                  MacroHudDataHandler.HudElement selected,
                                  CustomWidgetAdvancedLayout layout,
                                  int mouseX,
                                  int mouseY,
                                  String advancedBgColor,
                                  int advancedBgCursor,
                                  boolean advancedBgColorFocused,
                                  String advancedBorderColor,
                                  int advancedBorderCursor,
                                  boolean advancedBorderColorFocused) {
        List<UiRect> top = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(110, 2), UiFlexLayout.Item.flex(60, 1)
        );
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        List<UiRect> row3 = layout.typeRow3();
        drawModalButton(context, textRenderer, top.get(0), "Max Src: " + (StringUtils.safe(selected.sourceTokenMax).isBlank() ? "(none)" : selected.sourceTokenMax), mouseX, mouseY);
        drawModalButton(context, textRenderer, top.get(1), "Segmented: " + (selected.segmented ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(0), "R-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(1), "R+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(2), "MIN-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(3), "MIN+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(0), "MAX-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(1), "MAX+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(2), "C1-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(3), "C1+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row3.get(0), "C2-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row3.get(1), "C2+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Range: " + String.format(Locale.ROOT, "%.1f..%.1f", selected.minValue, selected.maxValue) + "  Segments: " + selected.segments, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);

        UiRect rangeInput = layout.typeInputLeft();
        UiRect segInput = layout.typeInputRight();
        context.drawTextWithShadow(textRenderer, "Range (min,max)", rangeInput.x(), rangeInput.y() - 10, 0xFFB8B8B8);
        context.fill(rangeInput.x(), rangeInput.y(), rangeInput.right(), rangeInput.bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(rangeInput.x(), rangeInput.y(), rangeInput.right(), rangeInput.y() + 1, 0x60FFFFFF);
        context.drawTextWithShadow(textRenderer, advancedBgColor, rangeInput.x() + 4, rangeInput.y() + 5, 0xFFEAEAEA);

        context.drawTextWithShadow(textRenderer, "Segments", segInput.x(), segInput.y() - 10, 0xFFB8B8B8);
        context.fill(segInput.x(), segInput.y(), segInput.right(), segInput.bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(segInput.x(), segInput.y(), segInput.right(), segInput.y() + 1, 0x60FFFFFF);
        context.drawTextWithShadow(textRenderer, advancedBorderColor, segInput.x() + 4, segInput.y() + 5, 0xFFEAEAEA);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = rangeInput.x() + 4 + textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, rangeInput.y() + 4, cx + 1, rangeInput.y() + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = segInput.x() + 4 + textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, segInput.y() + 4, cx + 1, segInput.y() + 13, 0xFFFFFFFF);
        }
    }

    private static void renderValue(DrawContext context,
                                    TextRenderer textRenderer,
                                    MacroHudDataHandler.HudElement selected,
                                    CustomWidgetAdvancedLayout layout,
                                    int mouseX,
                                    int mouseY,
                                    String advancedBgColor,
                                    int advancedBgSelectionAnchor,
                                    int advancedBgCursor,
                                    boolean advancedBgColorFocused,
                                    String advancedBorderColor,
                                    int advancedBorderSelectionAnchor,
                                    int advancedBorderCursor,
                                    boolean advancedBorderColorFocused,
                                    SelectionRenderer selectionRenderer) {
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        List<UiRect> row3 = layout.typeRow3();
        List<UiRect> presets = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
        );
        drawModalButton(context, textRenderer, slot(row1, 0), "WRN-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 1), "WRN+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 2), "CRT-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 3), "CRT+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 0), "WarnClr-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 1), "WarnClr+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 2), "CritClr-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 3), "CritClr+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row3, 0), "BG-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row3, 1), "BG+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row3, 2), "TX-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row3, 3), "TX+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(presets, 0), "Prefix preset", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(presets, 1), "Suffix preset", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Warn/Crit: " + String.format(Locale.ROOT, "%.1f / %.1f", selected.warnThreshold, selected.critThreshold), layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, "Tip: right-click Warn/Crit/BG/TX color buttons to open picker", layout.typeInfo2().x(), layout.typeInfo2().y(), 0xFF98B8D8);
        context.drawTextWithShadow(textRenderer, "Prefix", layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(textRenderer, "Suffix", layout.typeInputRight().x(), layout.typeInputRight().y() - 10, 0xFFB8B8B8);
        context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().y() + 1, 0x60FFFFFF);
        context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().y() + 1, 0x60FFFFFF);
        selectionRenderer.draw(context, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        selectionRenderer.draw(context, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(textRenderer, advancedBgColor, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, advancedBorderColor, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, 0xFFEAEAEA);
        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.typeInputLeft().x() + 4 + textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, layout.typeInputLeft().y() + 4, cx + 1, layout.typeInputLeft().y() + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.typeInputRight().x() + 4 + textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, layout.typeInputRight().y() + 4, cx + 1, layout.typeInputRight().y() + 13, 0xFFFFFFFF);
        }
    }

    private static void renderList(DrawContext context,
                                   TextRenderer textRenderer,
                                   MacroHudDataHandler.HudElement selected,
                                   CustomWidgetAdvancedLayout layout,
                                   int mouseX,
                                   int mouseY) {
        drawModalButton(context, textRenderer, layout.typeWideTop(), "List source preset", mouseX, mouseY);
        List<UiRect> row1 = layout.typeRow1();
        drawModalButton(context, textRenderer, row1.get(0), "L-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(1), "L+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(2), "S-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(3), "S+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Max lines: " + selected.maxLines + "  Scroll: " + selected.listScroll, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
    }

    private static void renderShape(DrawContext context,
                                    TextRenderer textRenderer,
                                    MacroHudDataHandler.HudElement selected,
                                    CustomWidgetAdvancedLayout layout,
                                    int mouseX,
                                    int mouseY) {
        drawModalButton(context, textRenderer, layout.typeWideTop(), "Type: " + selected.shapeType, mouseX, mouseY);
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        drawModalButton(context, textRenderer, row1.get(0), "Filled: " + (selected.shapeFilled ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(1), "R-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.get(2), "R+", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(0), "T-", mouseX, mouseY);
        drawModalButton(context, textRenderer, row2.get(1), "T+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Radius: " + selected.shapeRadius + "  Thickness: " + selected.shapeThickness, layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
    }

    private static void renderInventory(DrawContext context,
                                        TextRenderer textRenderer,
                                        MacroHudDataHandler.HudElement selected,
                                        CustomWidgetAdvancedLayout layout,
                                        int mouseX,
                                        int mouseY) {
        MacroHudDataHandler.InventoryDisplayMode mode = selected.inventoryDisplayMode == null
                ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR
                : selected.inventoryDisplayMode;
        List<UiRect> row1 = layout.typeRow1();
        drawModalButton(context, textRenderer, layout.typeWideTop(), "Mode: " + mode.name(), mouseX, mouseY);
        drawModalButton(context, textRenderer, row1.getFirst(), "Count: " + ((selected.inventoryShowCount == null || selected.inventoryShowCount) ? "ON" : "OFF"), mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "Hotbar: 9 slots  |  Inventory: 27 slots  |  Armor: 4 + offhand", layout.typeInfo1().x(), layout.typeInfo1().y(), 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, "Mode auto-sizes. Count text scales from slot size.", layout.typeInfo2().x(), layout.typeInfo2().y(), 0xFF98B8D8);
    }

    private static void renderStateBadge(DrawContext context,
                                         TextRenderer textRenderer,
                                         MacroHudDataHandler.HudElement selected,
                                         CustomWidgetAdvancedLayout layout,
                                         int mouseX,
                                         int mouseY,
                                         String advancedBgColor,
                                         int advancedBgSelectionAnchor,
                                         int advancedBgCursor,
                                         boolean advancedBgColorFocused,
                                         String advancedBorderColor,
                                         int advancedBorderSelectionAnchor,
                                         int advancedBorderCursor,
                                         boolean advancedBorderColorFocused,
                                         SelectionRenderer selectionRenderer) {
        List<UiRect> stateText = FormPanels.row(layout.typeWideTop(), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(80, 1), UiFlexLayout.Item.flex(80, 1)
        );
        List<UiRect> row1 = layout.typeRow1();
        List<UiRect> row2 = layout.typeRow2();
        drawModalButton(context, textRenderer, slot(stateText, 0), "ON: " + selected.stateOnText, mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(stateText, 1), "OFF: " + selected.stateOffText, mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 0), "ON-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 1), "ON+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 2), "OFF-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row1, 3), "OFF+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 0), "Show Value: " + (selected.stateShowValue ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 1), "TX-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 2), "TX+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(row2, 3), "Src preset", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "True tokens (csv)", layout.typeInputLeft().x(), layout.typeInputLeft().y() - 10, 0xFFB8B8B8);
        context.drawTextWithShadow(textRenderer, "False tokens (csv)", layout.typeInputRight().x(), layout.typeInputRight().y() - 10, 0xFFB8B8B8);
        context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().bottom(), advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(layout.typeInputLeft().x(), layout.typeInputLeft().y(), layout.typeInputLeft().right(), layout.typeInputLeft().y() + 1, 0x60FFFFFF);
        context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().bottom(), advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(layout.typeInputRight().x(), layout.typeInputRight().y(), layout.typeInputRight().right(), layout.typeInputRight().y() + 1, 0x60FFFFFF);
        selectionRenderer.draw(context, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        selectionRenderer.draw(context, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(textRenderer, advancedBgColor, layout.typeInputLeft().x() + 4, layout.typeInputLeft().y() + 5, 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, advancedBorderColor, layout.typeInputRight().x() + 4, layout.typeInputRight().y() + 5, 0xFFEAEAEA);
        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.typeInputLeft().x() + 4 + textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, layout.typeInputLeft().y() + 4, cx + 1, layout.typeInputLeft().y() + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = layout.typeInputRight().x() + 4 + textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, layout.typeInputRight().y() + 4, cx + 1, layout.typeInputRight().y() + 13, 0xFFFFFFFF);
        }
    }

    private static void drawModalButton(DrawContext context,
                                        TextRenderer textRenderer,
                                        UiRect rect,
                                        String label,
                                        int mouseX,
                                        int mouseY) {
        GuiSystem.drawButton(context, textRenderer, rect.x(), rect.y(), rect.width(), rect.height(), label, rect.contains(mouseX, mouseY), true);
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }
}
