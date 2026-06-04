package me.m0dii.modules.macros.gui;

import me.m0dii.gui.local.FormPanels;
import me.m0dii.gui.local.UiFlexLayout;
import me.m0dii.gui.local.UiRect;

import java.util.List;

/**
 * Extracted advanced-modal layout builders to keep the main screen focused on behavior.
 */
public final class MacroWorkbenchAdvancedLayouts {
    private MacroWorkbenchAdvancedLayouts() {
    }

    public static StandardAdvancedLayout standard(int boxX, int boxY, int modalW, int modalH) {
        UiRect content = FormPanels.panel(boxX + 12, boxY + 34, modalW - 24, modalH - 60);
        List<UiRect> col = FormPanels.column(content, 8, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(122),
                UiFlexLayout.Item.fixed(20),
                UiFlexLayout.Item.fixed(20),
                UiFlexLayout.Item.fixed(20),
                UiFlexLayout.Item.fixed(20),
                UiFlexLayout.Item.fixed(20),
                UiFlexLayout.Item.fixed(20)
        );

        UiRect textArea = col.get(0);
        UiRect actionField = col.get(1);

        List<UiRect> row3 = FormPanels.row(col.get(2), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(90), UiFlexLayout.Item.fixed(90), UiFlexLayout.Item.fixed(100),
                UiFlexLayout.Item.fixed(100), UiFlexLayout.Item.fixed(100), UiFlexLayout.Item.flex(100, -1, 1, 124)
        );

        List<UiRect> row4 = FormPanels.row(col.get(3), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(56), UiFlexLayout.Item.fixed(56),
                UiFlexLayout.Item.fixed(56), UiFlexLayout.Item.fixed(56),
                UiFlexLayout.Item.fixed(100), UiFlexLayout.Item.fixed(152), UiFlexLayout.Item.flex(92, -1, 1, 120)
        );

        List<UiRect> row5 = FormPanels.row(col.get(4), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(66), UiFlexLayout.Item.fixed(66),
                UiFlexLayout.Item.fixed(92), UiFlexLayout.Item.fixed(92),
                UiFlexLayout.Item.fixed(66), UiFlexLayout.Item.fixed(66),
                UiFlexLayout.Item.flex(120, 1)
        );

        List<UiRect> row6 = FormPanels.row(col.get(6), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(170),
                UiFlexLayout.Item.fixed(170),
                UiFlexLayout.Item.flex(120, 1)
        );
        List<UiRect> row6Right = FormPanels.row(row6.get(2), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(48),
                UiFlexLayout.Item.fixed(48),
                UiFlexLayout.Item.flex(60, 1)
        );

        UiRect apply = FormPanels.panel(boxX + modalW - 134, boxY + modalH - 24, 60, 18);
        UiRect cancel = FormPanels.panel(boxX + modalW - 70, boxY + modalH - 24, 58, 18);

        return new StandardAdvancedLayout(
                textArea,
                actionField,
                row3.get(0), row3.get(1), row3.get(2), row3.get(3), row3.get(4), row3.get(5),
                row4.get(0), row4.get(1), row4.get(2), row4.get(3), row4.get(4), row4.get(5), row4.get(6),
                row5.get(0), row5.get(1), row5.get(2), row5.get(3), row5.get(4), row5.get(5),
                row6.get(0), row6.get(1), row6Right.get(0), row6Right.get(1), row6Right.get(2),
                apply, cancel
        );
    }

    public static SecondaryAdvancedLayout secondary(int boxX, int boxY, int modalW, int modalH) {
        UiRect content = FormPanels.panel(boxX + 12, boxY + 44, modalW - 24, modalH - 68);
        List<UiRect> rows = FormPanels.column(content, 6, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.flex(120, -1, 1, 132),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(9),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18)
        );

        UiRect row1 = rows.get(0);
        UiRect row2 = rows.get(1);
        UiRect row3 = rows.get(2);
        UiRect regexInput = rows.get(3);
        UiRect outgoingInput = rows.get(4);
        UiRect statsButtons = rows.get(5);
        UiRect statsText = rows.get(6);
        UiRect colorButtons = rows.get(7);
        UiRect colorHex = rows.get(8);
        UiRect actions = rows.get(9);

        List<UiRect> toggleRow = FormPanels.row(row1, 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(160, 1),
                UiFlexLayout.Item.flex(140, 1),
                UiFlexLayout.Item.flex(180, 1)
        );
        List<UiRect> settingsRow = FormPanels.row(row2, 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(220, 1),
                UiFlexLayout.Item.fixed(82),
                UiFlexLayout.Item.fixed(46),
                UiFlexLayout.Item.fixed(46),
                UiFlexLayout.Item.fixed(52),
                UiFlexLayout.Item.fixed(52)
        );
        List<UiRect> metricsRow = FormPanels.row(row3, 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(240, 1),
                UiFlexLayout.Item.fixed(46),
                UiFlexLayout.Item.fixed(46)
        );
        List<UiRect> statButtons = FormPanels.row(statsButtons, 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(48, 1),
                UiFlexLayout.Item.flex(48, 1),
                UiFlexLayout.Item.flex(48, 1),
                UiFlexLayout.Item.flex(48, 1),
                UiFlexLayout.Item.flex(48, 1),
                UiFlexLayout.Item.flex(48, 1)
        );
        List<UiRect> colors = FormPanels.row(colorButtons, 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(64, 1),
                UiFlexLayout.Item.flex(64, 1),
                UiFlexLayout.Item.flex(84, 1),
                UiFlexLayout.Item.flex(84, 1)
        );
        List<UiRect> hexes = FormPanels.row(colorHex, 8, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(140),
                UiFlexLayout.Item.fixed(140)
        );
        List<UiRect> actionButtons = FormPanels.row(actions, 4, UiFlexLayout.Align.END,
                UiFlexLayout.Item.fixed(60),
                UiFlexLayout.Item.fixed(58)
        );

        return new SecondaryAdvancedLayout(
                toggleRow.get(0), toggleRow.get(1), toggleRow.get(2),
                settingsRow.get(0), settingsRow.get(1), settingsRow.get(2), settingsRow.get(3), settingsRow.get(4), settingsRow.get(5),
                regexInput,
                outgoingInput,
                statButtons.get(0), statButtons.get(1), statButtons.get(2), statButtons.get(3), statButtons.get(4), statButtons.get(5),
                statsText,
                colors.get(0), colors.get(1), colors.get(2), colors.get(3),
                hexes.get(0), hexes.get(1),
                new UiRect(metricsRow.get(0).x(), metricsRow.get(0).y() + 4, metricsRow.get(0).width(), 9),
                metricsRow.get(1), metricsRow.get(2),
                actionButtons.get(0), actionButtons.get(1)
        );
    }

    public static ProxyAdvancedLayout proxy(int boxX, int boxY, int modalW, int modalH, boolean pickup) {
        UiRect rowScale = FormPanels.panel(boxX + 12, boxY + 52, modalW - 24, 18);
        UiRect rowToggles = FormPanels.panel(boxX + 12, boxY + 78, modalW - 24, 18);
        UiRect rowColorButtons = FormPanels.panel(boxX + 12, boxY + 104, modalW - 24, 18);
        UiRect rowColorInputs = FormPanels.panel(boxX + 12, boxY + 136, modalW - 24, 18);
        UiRect rowAlign = FormPanels.panel(boxX + 12, boxY + 166, modalW - 24, 18);
        UiRect rowPickupButtons = FormPanels.panel(boxX + 12, boxY + 196, modalW - 24, 18);
        UiRect rowPickupInfo = FormPanels.panel(boxX + 12, boxY + 220, modalW - 24, 18);
        UiRect rowActions = FormPanels.panel(boxX + modalW - 134, boxY + modalH - 24, 122, 18);

        List<UiRect> scale = FormPanels.row(rowScale, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.flex(80, 1)
        );
        List<UiRect> toggles = FormPanels.row(rowToggles, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(120),
                UiFlexLayout.Item.fixed(120),
                UiFlexLayout.Item.flex(120, 1)
        );
        List<UiRect> colorButtons = FormPanels.row(rowColorButtons, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(64),
                UiFlexLayout.Item.fixed(84),
                UiFlexLayout.Item.fixed(84)
        );
        List<UiRect> colorInputs = FormPanels.row(rowColorInputs, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(140),
                UiFlexLayout.Item.fixed(140)
        );
        List<UiRect> align = FormPanels.row(rowAlign, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(88),
                UiFlexLayout.Item.fixed(88),
                UiFlexLayout.Item.flex(80, 1),
                UiFlexLayout.Item.fixed(48),
                UiFlexLayout.Item.fixed(48)
        );
        List<UiRect> pickupButtons = FormPanels.row(rowPickupButtons, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.flex(90, 1),
                UiFlexLayout.Item.flex(90, 1),
                UiFlexLayout.Item.flex(90, 1),
                UiFlexLayout.Item.flex(90, 1)
        );
        List<UiRect> actions = FormPanels.row(rowActions, 4, UiFlexLayout.Align.START,
                UiFlexLayout.Item.fixed(60),
                UiFlexLayout.Item.fixed(58)
        );

        UiRect empty = new UiRect(0, 0, 0, 0);
        return new ProxyAdvancedLayout(
                scale.get(0), scale.get(1), scale.get(2), scale.get(3), scale.get(4),
                toggles.get(0), toggles.get(1), toggles.get(2),
                colorButtons.get(0), colorButtons.get(1), colorButtons.get(2), colorButtons.get(3), colorButtons.get(4), colorButtons.get(5),
                colorInputs.get(0), colorInputs.get(1),
                align.get(0), align.get(1), align.get(2), align.get(3), align.get(4),
                pickup ? pickupButtons.get(0) : empty,
                pickup ? pickupButtons.get(1) : empty,
                pickup ? pickupButtons.get(2) : empty,
                pickup ? pickupButtons.get(3) : empty,
                pickup ? rowPickupInfo : empty,
                actions.get(0), actions.get(1)
        );
    }

    public static CustomWidgetAdvancedLayout custom(int boxX, int boxY, int modalW, int modalH) {
        UiRect content = FormPanels.panel(boxX + 12, boxY + 34, modalW - 24, modalH - 60);
        List<UiRect> rootRows = FormPanels.column(content, 8, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(220, 1),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18)
        );

        UiRect top = rootRows.getFirst();
        List<UiRect> topColumns = FormPanels.row(top, 8, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(220),
                UiFlexLayout.Item.flex(220, 1)
        );

        UiRect left = topColumns.get(0);
        UiRect right = topColumns.get(1);

        List<UiRect> leftRows = FormPanels.column(left, 8, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.flex(30, 1)
        );

        List<UiRect> rightRows = FormPanels.column(right, 6, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(9),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(18),
                UiFlexLayout.Item.fixed(9),
                UiFlexLayout.Item.fixed(9)
        );

        List<UiRect> generalRow1 = FormPanels.row(rightRows.get(0), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1)
        );
        List<UiRect> generalRow2 = FormPanels.row(rightRows.get(1), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(40, 1),
                UiFlexLayout.Item.flex(40, 1),
                UiFlexLayout.Item.flex(40, 1),
                UiFlexLayout.Item.flex(40, 1),
                UiFlexLayout.Item.flex(80, 2)
        );

        List<UiRect> typeRow1 = FormPanels.row(rightRows.get(5), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1)
        );
        List<UiRect> typeRow2 = FormPanels.row(rightRows.get(6), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1)
        );
        List<UiRect> typeRow3 = FormPanels.row(rightRows.get(7), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1), UiFlexLayout.Item.flex(40, 1)
        );
        List<UiRect> typeInputs = FormPanels.row(rightRows.get(8), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(60, 1), UiFlexLayout.Item.flex(60, 1)
        );

        List<UiRect> baseRow = FormPanels.row(rootRows.get(1), 4, UiFlexLayout.Align.STRETCH,
                UiFlexLayout.Item.flex(72, 1), UiFlexLayout.Item.flex(72, 1), UiFlexLayout.Item.flex(90, 1),
                UiFlexLayout.Item.flex(72, 1), UiFlexLayout.Item.flex(72, 1)
        );

        List<UiRect> actions = FormPanels.row(rootRows.get(2), 4, UiFlexLayout.Align.END,
                UiFlexLayout.Item.fixed(60), UiFlexLayout.Item.fixed(58)
        );

        return new CustomWidgetAdvancedLayout(
                leftRows.get(0),
                leftRows.get(1),
                leftRows.get(2),
                generalRow1,
                generalRow2,
                rightRows.get(2),
                rightRows.get(3),
                rightRows.get(4),
                typeRow1,
                typeRow2,
                typeRow3,
                typeInputs.get(0),
                typeInputs.get(1),
                rightRows.get(9),
                rightRows.get(10),
                baseRow,
                actions.get(0),
                actions.get(1)
        );
    }

    public record ProxyAdvancedLayout(
            UiRect scaleMinus,
            UiRect scalePlus,
            UiRect lineMinus,
            UiRect linePlus,
            UiRect metrics,
            UiRect toggleBg,
            UiRect toggleBorder,
            UiRect toggleVisible,
            UiRect colorBgMinus,
            UiRect colorBgPlus,
            UiRect colorTxMinus,
            UiRect colorTxPlus,
            UiRect colorAlphaMinus,
            UiRect colorAlphaPlus,
            UiRect bgInput,
            UiRect txInput,
            UiRect alignH,
            UiRect alignV,
            UiRect anchor,
            UiRect zMinus,
            UiRect zPlus,
            UiRect pickupDuration,
            UiRect pickupLines,
            UiRect pickupIcon,
            UiRect pickupDirection,
            UiRect pickupInfo,
            UiRect apply,
            UiRect cancel
    ) {
    }

    public record SecondaryAdvancedLayout(
            UiRect guiOpen,
            UiRect fadeToggle,
            UiRect hoverReset,
            UiRect noTransparency,
            UiRect mode,
            UiRect scaleMinus,
            UiRect scalePlus,
            UiRect lineMinus,
            UiRect linePlus,
            UiRect regexInput,
            UiRect outgoingInput,
            UiRect fadeMinus,
            UiRect fadePlus,
            UiRect alphaMinus,
            UiRect alphaPlus,
            UiRect linesMinus,
            UiRect linesPlus,
            UiRect statsText,
            UiRect bgMinus,
            UiRect bgPlus,
            UiRect bgAlphaMinus,
            UiRect bgAlphaPlus,
            UiRect bgHex,
            UiRect txHex,
            UiRect metricsText,
            UiRect zMinus,
            UiRect zPlus,
            UiRect apply,
            UiRect cancel
    ) {
    }

    public record StandardAdvancedLayout(
            UiRect textArea,
            UiRect actionField,
            UiRect bgToggle,
            UiRect bgOpaque,
            UiRect borderToggle,
            UiRect hAlign,
            UiRect vAlign,
            UiRect anchor,
            UiRect lineMinus,
            UiRect linePlus,
            UiRect fontMinus,
            UiRect fontPlus,
            UiRect visibility,
            UiRect execution,
            UiRect asyncToggle,
            UiRect bgMinus,
            UiRect bgPlus,
            UiRect alphaMinus,
            UiRect alphaPlus,
            UiRect borderMinus,
            UiRect borderPlus,
            UiRect bgHex,
            UiRect borderHex,
            UiRect zMinus,
            UiRect zPlus,
            UiRect visibilityType,
            UiRect apply,
            UiRect cancel
    ) {
    }

    public record CustomWidgetAdvancedLayout(
            UiRect labelInput,
            UiRect sourceInput,
            UiRect suggestionArea,
            List<UiRect> generalRow1,
            List<UiRect> generalRow2,
            UiRect metricsText,
            UiRect typeHintText,
            UiRect typeWideTop,
            List<UiRect> typeRow1,
            List<UiRect> typeRow2,
            List<UiRect> typeRow3,
            UiRect typeInputLeft,
            UiRect typeInputRight,
            UiRect typeInfo1,
            UiRect typeInfo2,
            List<UiRect> baseRow,
            UiRect apply,
            UiRect cancel
    ) {
    }
}


