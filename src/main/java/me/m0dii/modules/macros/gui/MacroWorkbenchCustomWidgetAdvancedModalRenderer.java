package me.m0dii.modules.macros.gui;

import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.GuiTextEditingUtils;
import me.m0dii.gui.local.UiRect;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.CustomWidgetAdvancedLayout;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Locale;

public final class MacroWorkbenchCustomWidgetAdvancedModalRenderer {
    private MacroWorkbenchCustomWidgetAdvancedModalRenderer() {
    }

    public static void render(DrawContext context,
                              TextRenderer textRenderer,
                              MacroHudDataHandler.HudElement selected,
                              CustomWidgetAdvancedLayout layout,
                              int mouseX,
                              int mouseY,
                              int boxX,
                              int boxY,
                              String advancedText,
                              boolean advancedTextFocused,
                              int advancedSelectionAnchor,
                              int advancedCursor,
                              String advancedAction,
                              boolean advancedActionFocused,
                              int advancedActionSelectionAnchor,
                              int advancedActionCursor,
                              List<String> suggestions,
                              int advancedActionSuggestionScroll,
                              int advancedActionSuggestionIndex,
                              String advancedBgColor,
                              int advancedBgSelectionAnchor,
                              int advancedBgCursor,
                              boolean advancedBgColorFocused,
                              String advancedBorderColor,
                              int advancedBorderSelectionAnchor,
                              int advancedBorderCursor,
                              boolean advancedBorderColorFocused,
                              String backgroundLabel,
                              String borderModeLabel) {
        if (selected == null) {
            return;
        }
        UiRect labelInput = layout.labelInput();
        UiRect sourceInput = layout.sourceInput();
        UiRect suggestionsArea = layout.suggestionArea();
        List<UiRect> baseRow = layout.baseRow();
        List<UiRect> generalRow1 = layout.generalRow1();
        List<UiRect> generalRow2 = layout.generalRow2();

        renderHeader(context, textRenderer, selected, boxX, boxY, labelInput, sourceInput,
                advancedText, advancedTextFocused, advancedSelectionAnchor, advancedCursor,
                advancedAction, advancedActionFocused, advancedActionSelectionAnchor, advancedActionCursor);
        renderSuggestionDropdown(context, textRenderer, suggestionsArea, suggestions, advancedActionSuggestionScroll, advancedActionSuggestionIndex);
        renderMainInputCarets(context, textRenderer, labelInput, sourceInput,
                advancedText, advancedTextFocused, advancedCursor,
                advancedAction, advancedActionFocused, advancedActionCursor);
        renderGeneralButtons(context, textRenderer, selected, layout, mouseX, mouseY, generalRow1, generalRow2);

        UiRect typeHint = layout.typeHintText();
        context.drawTextWithShadow(textRenderer, "Type options", typeHint.x(), typeHint.y(), 0xFFB8B8B8);
        MacroWorkbenchCustomWidgetTypeRenderer.render(
                context,
                textRenderer,
                selected,
                layout,
                mouseX,
                mouseY,
                advancedBgColor,
                advancedBgSelectionAnchor,
                advancedBgCursor,
                advancedBgColorFocused,
                advancedBorderColor,
                advancedBorderSelectionAnchor,
                advancedBorderCursor,
                advancedBorderColorFocused,
                (ctx, textX, textY, text, anchor, cursor) -> GuiTextEditingUtils.drawSingleLineSelection(ctx, textRenderer, textX, textY, text, anchor, cursor)
        );

        drawModalButton(context, textRenderer, slot(baseRow, 0), "H: " + selected.horizontalAlign.name(), mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(baseRow, 1), "V: " + selected.verticalAlign.name(), mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(baseRow, 2), "Anchor: " + MacroWorkbenchCanvasUtils.shortAnchor(selected.anchor), mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(baseRow, 3), backgroundLabel, mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(baseRow, 4), borderModeLabel, mouseX, mouseY);

        drawModalButton(context, textRenderer, layout.apply(), "Apply", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.cancel(), "Cancel", mouseX, mouseY);
    }

    private static void renderHeader(DrawContext context,
                                     TextRenderer textRenderer,
                                     MacroHudDataHandler.HudElement selected,
                                     int boxX,
                                     int boxY,
                                     UiRect labelInput,
                                     UiRect sourceInput,
                                     String advancedText,
                                     boolean advancedTextFocused,
                                     int advancedSelectionAnchor,
                                     int advancedCursor,
                                     String advancedAction,
                                     boolean advancedActionFocused,
                                     int advancedActionSelectionAnchor,
                                     int advancedActionCursor) {
        context.drawTextWithShadow(textRenderer, selected.type + " Widget", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON
                        ? "Label + icon id + type-specific controls"
                        : selected.type == MacroHudDataHandler.ElementType.INVENTORY
                        ? "Label + inventory mode + type-specific controls"
                        : "Label + source token + type-specific controls",
                boxX + 12, boxY + 24, 0xFFB0B0B0);

        context.drawTextWithShadow(textRenderer, "Label", labelInput.x(), labelInput.y() - 10, 0xFFB8B8B8);
        context.fill(labelInput.x(), labelInput.y(), labelInput.right(), labelInput.bottom(), advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(labelInput.x(), labelInput.y(), labelInput.right(), labelInput.y() + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, labelInput.x() + 4, labelInput.y() + 5, advancedText, advancedSelectionAnchor, advancedCursor);
        context.drawTextWithShadow(textRenderer, advancedText, labelInput.x() + 4, labelInput.y() + 5, 0xFFEAEAEA);

        context.drawTextWithShadow(textRenderer,
                selected.type == MacroHudDataHandler.ElementType.ICON
                        ? "Icon id"
                        : selected.type == MacroHudDataHandler.ElementType.INVENTORY
                        ? "Mode"
                        : "Source token",
                sourceInput.x(), sourceInput.y() - 10, 0xFFB8B8B8);
        context.fill(sourceInput.x(), sourceInput.y(), sourceInput.right(), sourceInput.bottom(), advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(sourceInput.x(), sourceInput.y(), sourceInput.right(), sourceInput.y() + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, sourceInput.x() + 4, sourceInput.y() + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(textRenderer, advancedAction, sourceInput.x() + 4, sourceInput.y() + 5, 0xFFEAEAEA);
    }

    private static void renderSuggestionDropdown(DrawContext context,
                                                 TextRenderer textRenderer,
                                                 UiRect suggestionsArea,
                                                 List<String> suggestions,
                                                 int suggestionScroll,
                                                 int selectedSuggestionIndex) {
        if (suggestions.isEmpty()) {
            return;
        }
        int dropX = suggestionsArea.x();
        int dropY = suggestionsArea.y();
        int dropW = suggestionsArea.width();
        int rowH = 10;
        int maxVisible = Math.clamp(Math.max(1, suggestionsArea.height() / rowH), 1, suggestions.size());
        int maxScroll = Math.max(0, suggestions.size() - maxVisible);
        int scroll = Math.clamp(suggestionScroll, 0, maxScroll);
        context.fill(dropX, dropY, dropX + dropW, dropY + maxVisible * rowH, 0xC0101010);
        for (int i = 0; i < maxVisible; i++) {
            int idx = scroll + i;
            String token = suggestions.get(idx);
            int yy = dropY + i * rowH;
            boolean selectedSuggestion = (selectedSuggestionIndex == idx);
            if (selectedSuggestion) {
                context.fill(dropX, yy, dropX + dropW, yy + rowH, 0x503777AA);
            }
            int color = MacroWorkbenchActionSuggestions.isHeader(token) ? 0xFFB8D8FF : 0xFF8FC8FF;
            context.drawTextWithShadow(textRenderer, token, dropX + 3, yy + 1, color);
        }
        if (suggestions.size() > maxVisible) {
            context.drawTextWithShadow(textRenderer,
                    "scroll " + (scroll + 1) + "/" + (maxScroll + 1),
                    dropX + 3, dropY + maxVisible * rowH + 2, 0xFF909090);
        }
    }

    private static void renderMainInputCarets(DrawContext context,
                                              TextRenderer textRenderer,
                                              UiRect labelInput,
                                              UiRect sourceInput,
                                              String advancedText,
                                              boolean advancedTextFocused,
                                              int advancedCursor,
                                              String advancedAction,
                                              boolean advancedActionFocused,
                                              int advancedActionCursor) {
        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = labelInput.x() + 4 + textRenderer.getWidth(advancedText.substring(0, Math.clamp(advancedCursor, 0, advancedText.length())));
            context.fill(cx, labelInput.y() + 4, cx + 1, labelInput.y() + 13, 0xFFFFFFFF);
        }
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = sourceInput.x() + 4 + textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(cx, sourceInput.y() + 4, cx + 1, sourceInput.y() + 13, 0xFFFFFFFF);
        }
    }

    private static void renderGeneralButtons(DrawContext context,
                                             TextRenderer textRenderer,
                                             MacroHudDataHandler.HudElement selected,
                                             CustomWidgetAdvancedLayout layout,
                                             int mouseX,
                                             int mouseY,
                                             List<UiRect> generalRow1,
                                             List<UiRect> generalRow2) {
        drawModalButton(context, textRenderer, slot(generalRow1, 0), "BG-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow1, 1), "BG+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow1, 2), "BR-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow1, 3), "BR+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow2, 0), "FS-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow2, 1), "FS+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow2, 2), "Z-", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow2, 3), "Z+", mouseX, mouseY);
        drawModalButton(context, textRenderer, slot(generalRow2, 4), "Pick Src", mouseX, mouseY);
        int bgPct = Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f);
        context.drawTextWithShadow(textRenderer,
                "Scale: " + String.format(Locale.ROOT, "%.2f", selected.fontScale) + "  BG Alpha: " + bgPct + "%  Z: " + selected.zIndex,
                layout.metricsText().x(), layout.metricsText().y(), 0xFFEAEAEA);
    }

    private static void drawModalButton(DrawContext context, TextRenderer textRenderer, UiRect rect, String label, int mouseX, int mouseY) {
        GuiSystem.drawButton(context, textRenderer, rect.x(), rect.y(), rect.width(), rect.height(), label, rect.contains(mouseX, mouseY), true);
    }

    private static UiRect slot(List<UiRect> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return new UiRect(0, 0, 0, 0);
        }
        return row.get(index);
    }
}
