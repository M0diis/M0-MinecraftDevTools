package me.m0dii.gui.local;

import java.util.ArrayList;
import java.util.List;

public final class UiGridLayout {
    private UiGridLayout() {
    }

    public static List<UiRect> cells(UiRect bounds, int columns, int itemCount, int horizontalGap, int verticalGap) {
        return cells(bounds, columns, itemCount, horizontalGap, verticalGap, -1);
    }

    public static List<UiRect> cells(UiRect bounds, int columns, int itemCount, int horizontalGap, int verticalGap, int maxCellWidth) {
        if (bounds == null || columns <= 0 || itemCount <= 0) {
            return List.of();
        }

        int hGap = Math.max(0, horizontalGap);
        int vGap = Math.max(0, verticalGap);
        int rows = (int) Math.ceil(itemCount / (double) columns);

        int cellW = Math.max(1, (bounds.width() - hGap * (columns - 1)) / columns);
        if (maxCellWidth > 0) {
            cellW = Math.min(cellW, maxCellWidth);
        }
        int cellH = Math.max(1, (bounds.height() - vGap * (rows - 1)) / rows);

        List<UiRect> out = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            int col = i % columns;
            int row = i / columns;
            int x = bounds.x() + col * (cellW + hGap);
            int y = bounds.y() + row * (cellH + vGap);
            out.add(new UiRect(x, y, cellW, cellH));
        }
        return out;
    }
}

