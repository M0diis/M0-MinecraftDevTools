package me.m0dii.modules.macros.gui;

import me.m0dii.gui.GuiSystem;
import me.m0dii.gui.local.GuiTextEditingUtils;
import me.m0dii.modules.chat.SecondaryChatSettings;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.ProxyAdvancedLayout;
import me.m0dii.modules.macros.gui.MacroWorkbenchAdvancedLayouts.SecondaryAdvancedLayout;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.pickup.PickupFeedSettings;
import me.m0dii.utils.StringUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Locale;

public final class MacroWorkbenchProxyAdvancedModalRenderer {
    private MacroWorkbenchProxyAdvancedModalRenderer() {
    }

    public static void renderSecondary(DrawContext context,
                                       TextRenderer textRenderer,
                                       MacroHudDataHandler.HudElement selected,
                                       SecondaryAdvancedLayout layout,
                                       int boxX,
                                       int boxY,
                                       int mouseX,
                                       int mouseY,
                                       boolean showWhileGuiOpen,
                                       boolean fadeEnabled,
                                       boolean hoverReset,
                                       boolean noTransparencyWhenChatOpen,
                                       SecondaryChatSettings.InterceptMode interceptMode,
                                       double scale,
                                       int lineHeight,
                                       String advancedText,
                                       boolean advancedTextFocused,
                                       int advancedSelectionAnchor,
                                       int advancedCursor,
                                       String advancedAction,
                                       boolean advancedActionFocused,
                                       int advancedActionSelectionAnchor,
                                       int advancedActionCursor,
                                       int fadeDurationMs,
                                       int minAlpha,
                                       int maxLines,
                                       String advancedBgColor,
                                       boolean advancedBgColorFocused,
                                       int advancedBgSelectionAnchor,
                                       int advancedBgCursor,
                                       String advancedBorderColor,
                                       boolean advancedBorderColorFocused,
                                       int advancedBorderSelectionAnchor,
                                       int advancedBorderCursor) {
        context.drawTextWithShadow(textRenderer, "Secondary Chat Settings", boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Canvas controls position/size. This panel edits behavior.", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, textRenderer, layout.guiOpen(), "GUI Open: " + (showWhileGuiOpen ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.fadeToggle(), "Fade: " + (fadeEnabled ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.hoverReset(), "Hover Reset: " + (hoverReset ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.noTransparency(), "No Transparency In Chat: " + (noTransparencyWhenChatOpen ? "ON" : "OFF"), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.mode(), "Mode: " + (interceptMode == null ? "COPY" : interceptMode.name()), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.scaleMinus(), "S-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.scalePlus(), "S+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.lineMinus(), "LH-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.linePlus(), "LH+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.zMinus(), "Z-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.zPlus(), "Z+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer,
                "Scale: " + String.format("%.2f", scale) + "  Line: " + lineHeight + "  Z: " + (selected == null ? 0 : selected.zIndex),
                layout.metricsText().x(), layout.metricsText().y(), 0xFFEAEAEA);

        int regexX = layout.regexInput().x();
        int regexY = layout.regexInput().y();
        int regexW = layout.regexInput().width();
        int regexH = layout.regexInput().height();
        context.drawTextWithShadow(textRenderer, "Regex List (one pattern per line)", regexX, regexY - 10, 0xFFB8B8B8);
        context.fill(regexX, regexY, regexX + regexW, regexY + regexH, advancedTextFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(regexX, regexY, regexX + regexW, regexY + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawMultilineSelection(context, textRenderer, regexX + 4, regexY + 4, regexY + regexH - 12, advancedText, advancedSelectionAnchor, advancedCursor, 9);
        List<String> regexLines = StringUtils.splitLinesRaw(advancedText);
        int ry = regexY + 4;
        for (String line : regexLines) {
            if (ry > regexY + regexH - 12) {
                break;
            }
            context.drawTextWithShadow(textRenderer, line, regexX + 4, ry, 0xFFEAEAEA);
            ry += 9;
        }
        if (advancedTextFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int[] cursor = GuiTextEditingUtils.cursorPixel(textRenderer, regexX + 4, regexY + 4, advancedText, advancedCursor);
            context.fill(cursor[0], cursor[1], cursor[0] + 1, cursor[1] + 9, 0xFFFFFFFF);
        }

        int outgoingX = layout.outgoingInput().x();
        int outgoingY = layout.outgoingInput().y();
        int outgoingW = layout.outgoingInput().width();
        int outgoingH = layout.outgoingInput().height();
        context.drawTextWithShadow(textRenderer, "Outgoing Regex", outgoingX, outgoingY - 10, 0xFFB8B8B8);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + outgoingH, advancedActionFocused ? 0xFF0F0F0F : 0xFF161616);
        context.fill(outgoingX, outgoingY, outgoingX + outgoingW, outgoingY + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, outgoingX + 4, outgoingY + 5, advancedAction, advancedActionSelectionAnchor, advancedActionCursor);
        context.drawTextWithShadow(textRenderer, advancedAction, outgoingX + 4, outgoingY + 5, 0xFFEAEAEA);
        if (advancedActionFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ax = outgoingX + 4 + textRenderer.getWidth(advancedAction.substring(0, Math.clamp(advancedActionCursor, 0, advancedAction.length())));
            context.fill(ax, outgoingY + 4, ax + 1, outgoingY + 13, 0xFFFFFFFF);
        }

        drawModalButton(context, textRenderer, layout.fadeMinus(), "FD-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.fadePlus(), "FD+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.alphaMinus(), "A-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.alphaPlus(), "A+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.linesMinus(), "L-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.linesPlus(), "L+", mouseX, mouseY);
        int bgPercent = selected == null ? 0 : Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f);
        context.drawTextWithShadow(textRenderer,
                "Fade: " + fadeDurationMs + "ms  Alpha: " + minAlpha + "  Max: " + maxLines + "  BG: " + bgPercent + "%",
                layout.statsText().x(), layout.statsText().y(), 0xFFEAEAEA);

        int secondaryBgHexX = layout.bgHex().x();
        int secondaryBgHexY = layout.bgHex().y();
        int secondaryBgHexW = layout.bgHex().width();
        int secondaryTxHexX = layout.txHex().x();
        int secondaryTxHexY = layout.txHex().y();
        int secondaryTxHexW = layout.txHex().width();
        drawModalButton(context, textRenderer, layout.bgMinus(), "BG-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.bgPlus(), "BG+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.bgAlphaMinus(), "Opacity-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.bgAlphaPlus(), "Opacity+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer, "BG", secondaryBgHexX, secondaryBgHexY - 10, 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, "TX", secondaryTxHexX, secondaryTxHexY - 10, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(secondaryBgHexX, secondaryBgHexY, secondaryBgHexX + secondaryBgHexW, secondaryBgHexY + 18, bgInputBg);
        context.fill(secondaryBgHexX, secondaryBgHexY, secondaryBgHexX + secondaryBgHexW, secondaryBgHexY + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, secondaryBgHexX + 4, secondaryBgHexY + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        context.drawTextWithShadow(textRenderer, advancedBgColor, secondaryBgHexX + 4, secondaryBgHexY + 5, 0xFFEAEAEA);
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(secondaryTxHexX, secondaryTxHexY, secondaryTxHexX + secondaryTxHexW, secondaryTxHexY + 18, txInputBg);
        context.fill(secondaryTxHexX, secondaryTxHexY, secondaryTxHexX + secondaryTxHexW, secondaryTxHexY + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, secondaryTxHexX + 4, secondaryTxHexY + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(textRenderer, advancedBorderColor, secondaryTxHexX + 4, secondaryTxHexY + 5, 0xFFEAEAEA);

        drawModalButton(context, textRenderer, layout.apply(), "Apply", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.cancel(), "Cancel", mouseX, mouseY);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = secondaryBgHexX + 4 + textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, secondaryBgHexY + 4, cx + 1, secondaryBgHexY + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = secondaryTxHexX + 4 + textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, secondaryTxHexY + 4, cx + 1, secondaryTxHexY + 13, 0xFFFFFFFF);
        }
    }

    public static void renderProxy(DrawContext context,
                                   TextRenderer textRenderer,
                                   MacroHudDataHandler.HudElement selected,
                                   ProxyAdvancedLayout layout,
                                   int boxX,
                                   int boxY,
                                   int mouseX,
                                   int mouseY,
                                   String title,
                                   String backgroundLabel,
                                   String borderModeLabel,
                                   String advancedBgColor,
                                   boolean advancedBgColorFocused,
                                   int advancedBgSelectionAnchor,
                                   int advancedBgCursor,
                                   String advancedBorderColor,
                                   boolean advancedBorderColorFocused,
                                   int advancedBorderSelectionAnchor,
                                   int advancedBorderCursor,
                                   boolean pickupProxy) {
        context.drawTextWithShadow(textRenderer, title, boxX + 12, boxY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Use canvas for positioning, this panel for style", boxX + 12, boxY + 24, 0xFFB0B0B0);

        drawModalButton(context, textRenderer, layout.scaleMinus(), "S-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.scalePlus(), "S+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.lineMinus(), "LH-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.linePlus(), "LH+", mouseX, mouseY);
        context.drawTextWithShadow(textRenderer,
                "Scale: " + String.format("%.2f", selected == null ? 1.0f : selected.fontScale) +
                        "  Line: " + (selected == null ? 9 : selected.lineHeight)
                        + "  BG: " + (selected == null ? 0 : Math.round((Math.clamp(selected.backgroundAlpha, 0, 255) / 255.0f) * 100.0f)) + "%"
                        + "  Z: " + (selected == null ? 0 : selected.zIndex),
                layout.metrics().x(), layout.metrics().y() + 4, 0xFFEAEAEA);

        drawModalButton(context, textRenderer, layout.toggleBg(), backgroundLabel, mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.toggleBorder(), borderModeLabel, mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.toggleVisible(), "Visible: " + ((selected != null && selected.visible) ? "YES" : "NO"), mouseX, mouseY);

        drawModalButton(context, textRenderer, layout.colorBgMinus(), "BG-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.colorBgPlus(), "BG+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.colorTxMinus(), "TX-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.colorTxPlus(), "TX+", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.colorAlphaMinus(), "Opacity-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.colorAlphaPlus(), "Opacity+", mouseX, mouseY);

        drawModalButton(context, textRenderer, layout.alignH(), "H: " + (selected == null ? "-" : selected.horizontalAlign.name()), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.alignV(), "V: " + (selected == null ? "-" : selected.verticalAlign.name()), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.anchor(),
                "Anchor: " + (selected == null ? "-" : MacroWorkbenchCanvasUtils.shortAnchor(selected.anchor)), mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.zMinus(), "Z-", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.zPlus(), "Z+", mouseX, mouseY);

        int bgInputX = layout.bgInput().x();
        int bgInputY = layout.bgInput().y();
        int bgInputW = layout.bgInput().width();
        int txInputX = layout.txInput().x();
        int txInputY = layout.txInput().y();
        int txInputW = layout.txInput().width();
        context.drawTextWithShadow(textRenderer, "BG", bgInputX, bgInputY - 10, 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, "TX", txInputX, txInputY - 10, 0xFFEAEAEA);
        int bgInputBg = advancedBgColorFocused ? 0xFF0F0F0F : 0xFF161616;
        int txInputBg = advancedBorderColorFocused ? 0xFF0F0F0F : 0xFF161616;
        context.fill(bgInputX, bgInputY, bgInputX + bgInputW, bgInputY + 18, bgInputBg);
        context.fill(bgInputX, bgInputY, bgInputX + bgInputW, bgInputY + 1, 0x60FFFFFF);
        context.fill(txInputX, txInputY, txInputX + txInputW, txInputY + 18, txInputBg);
        context.fill(txInputX, txInputY, txInputX + txInputW, txInputY + 1, 0x60FFFFFF);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, bgInputX + 4, bgInputY + 5, advancedBgColor, advancedBgSelectionAnchor, advancedBgCursor);
        GuiTextEditingUtils.drawSingleLineSelection(context, textRenderer, txInputX + 4, txInputY + 5, advancedBorderColor, advancedBorderSelectionAnchor, advancedBorderCursor);
        context.drawTextWithShadow(textRenderer, advancedBgColor, bgInputX + 4, bgInputY + 5, 0xFFEAEAEA);
        context.drawTextWithShadow(textRenderer, advancedBorderColor, txInputX + 4, txInputY + 5, 0xFFEAEAEA);

        if (advancedBgColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = bgInputX + 4 + textRenderer.getWidth(advancedBgColor.substring(0, Math.clamp(advancedBgCursor, 0, advancedBgColor.length())));
            context.fill(cx, bgInputY + 4, cx + 1, bgInputY + 13, 0xFFFFFFFF);
        }
        if (advancedBorderColorFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = txInputX + 4 + textRenderer.getWidth(advancedBorderColor.substring(0, Math.clamp(advancedBorderCursor, 0, advancedBorderColor.length())));
            context.fill(cx, txInputY + 4, cx + 1, txInputY + 13, 0xFFFFFFFF);
        }

        if (pickupProxy) {
            PickupFeedSettings.Data pickup = PickupFeedSettings.get();
            drawModalButton(context, textRenderer, layout.pickupDuration(), "Duration", mouseX, mouseY);
            drawModalButton(context, textRenderer, layout.pickupLines(), "Lines", mouseX, mouseY);
            drawModalButton(context, textRenderer, layout.pickupIcon(), "Icon", mouseX, mouseY);
            drawModalButton(context, textRenderer, layout.pickupDirection(), "Direction", mouseX, mouseY);
            context.drawTextWithShadow(textRenderer,
                    "Dur: " + pickup.durationMs + "ms  Max: " + pickup.maxLines + "  Scale: "
                            + String.format(Locale.ROOT, "%.2f", pickup.iconScale) + "  Dir: " + pickup.direction.name(),
                    layout.pickupInfo().x(), layout.pickupInfo().y(), 0xFFEAEAEA);
        }

        drawModalButton(context, textRenderer, layout.apply(), "Apply", mouseX, mouseY);
        drawModalButton(context, textRenderer, layout.cancel(), "Cancel", mouseX, mouseY);
    }

    private static void drawModalButton(DrawContext context, TextRenderer textRenderer, me.m0dii.gui.local.UiRect rect, String label, int mouseX, int mouseY) {
        GuiSystem.drawButton(context, textRenderer, rect.x(), rect.y(), rect.width(), rect.height(), label, rect.contains(mouseX, mouseY), true);
    }
}
