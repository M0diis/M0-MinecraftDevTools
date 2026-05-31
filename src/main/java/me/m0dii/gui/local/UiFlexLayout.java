package me.m0dii.gui.local;

import java.util.ArrayList;
import java.util.List;

public final class UiFlexLayout {
    public enum Direction {
        ROW,
        COLUMN
    }

    public enum Align {
        START,
        CENTER,
        END,
        STRETCH
    }

    public record Item(int basis, int cross, int grow, int maxBasis) {
        public Item {
            if (basis < 0 || cross < -1 || grow < 0 || maxBasis < -1) {
                throw new IllegalArgumentException("Invalid flex item values");
            }
            if (maxBasis >= 0 && maxBasis < basis) {
                throw new IllegalArgumentException("maxBasis must be >= basis or -1");
            }
        }

        public static Item fixed(int basis) {
            return new Item(basis, -1, 0, basis);
        }

        public static Item fixed(int basis, int cross) {
            return new Item(basis, cross, 0, basis);
        }

        public static Item flex(int minBasis, int grow) {
            return new Item(minBasis, -1, grow, -1);
        }


        public static Item flex(int minBasis, int cross, int grow) {
            return new Item(minBasis, cross, grow, -1);
        }

        public static Item flex(int minBasis, int cross, int grow, int maxBasis) {
            return new Item(minBasis, cross, grow, maxBasis);
        }
    }

    private UiFlexLayout() {
    }

    public static List<UiRect> row(UiRect bounds, int gap, Align align, List<Item> items) {
        return layout(Direction.ROW, bounds, gap, align, items);
    }

    public static List<UiRect> column(UiRect bounds, int gap, Align align, List<Item> items) {
        return layout(Direction.COLUMN, bounds, gap, align, items);
    }

    public static List<UiRect> layout(Direction direction, UiRect bounds, int gap, Align align, List<Item> items) {
        if (bounds == null || items == null || items.isEmpty()) {
            return List.of();
        }

        int safeGap = Math.max(0, gap);
        int count = items.size();
        int totalGap = safeGap * Math.max(0, count - 1);

        int mainSize = direction == Direction.ROW ? bounds.width() : bounds.height();
        int crossSize = direction == Direction.ROW ? bounds.height() : bounds.width();
        int availableMain = Math.max(0, mainSize - totalGap);

        int basisTotal = 0;
        int growTotal = 0;
        for (Item item : items) {
            basisTotal += item.basis();
            growTotal += item.grow();
        }

        int extra = Math.max(0, availableMain - basisTotal);
        int cursor = direction == Direction.ROW ? bounds.x() : bounds.y();
        int remainder = extra;
        List<UiRect> out = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Item item = items.get(i);
            int growShare = 0;
            if (growTotal > 0 && item.grow() > 0) {
                growShare = (extra * item.grow()) / growTotal;
                if (i == count - 1) {
                    growShare = remainder;
                } else {
                    remainder -= growShare;
                }
            }
            int itemMain = Math.max(0, item.basis() + growShare);
            if (item.maxBasis() >= 0) {
                itemMain = Math.min(itemMain, item.maxBasis());
            }
            // Keep each item inside the layout bounds even when basis totals exceed available space.
            int remainingMain = direction == Direction.ROW
                    ? Math.max(0, bounds.right() - cursor)
                    : Math.max(0, bounds.bottom() - cursor);
            itemMain = Math.min(itemMain, remainingMain);
            int desiredCross = item.cross() < 0 ? crossSize : Math.min(crossSize, item.cross());

            int crossPos;
            int itemCross;
            if (align == Align.STRETCH && item.cross() < 0) {
                crossPos = direction == Direction.ROW ? bounds.y() : bounds.x();
                itemCross = crossSize;
            } else {
                itemCross = desiredCross;
                int freeCross = Math.max(0, crossSize - itemCross);
                int offset = switch (align) {
                    case CENTER -> freeCross / 2;
                    case END -> freeCross;
                    default -> 0;
                };
                crossPos = (direction == Direction.ROW ? bounds.y() : bounds.x()) + offset;
            }

            if (direction == Direction.ROW) {
                out.add(new UiRect(cursor, crossPos, itemMain, itemCross));
            } else {
                out.add(new UiRect(crossPos, cursor, itemCross, itemMain));
            }

            cursor += itemMain + safeGap;
        }

        return out;
    }
}

