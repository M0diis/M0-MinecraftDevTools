package me.m0dii.gui.local;

import java.util.List;

public final class FormPanels {
    private FormPanels() {
    }

    public static UiRect panel(int x, int y, int width, int height) {
        return new UiRect(x, y, width, height);
    }

    public static List<UiRect> row(UiRect bounds, int gap, UiFlexLayout.Align align, List<UiFlexLayout.Item> items) {
        return UiFlexLayout.row(bounds, gap, align, items);
    }

    public static List<UiRect> column(UiRect bounds, int gap, UiFlexLayout.Align align, List<UiFlexLayout.Item> items) {
        return UiFlexLayout.column(bounds, gap, align, items);
    }

    public static List<UiRect> grid(UiRect bounds, int columns, int itemCount, int horizontalGap, int verticalGap) {
        return UiGridLayout.cells(bounds, columns, itemCount, horizontalGap, verticalGap);
    }
}

