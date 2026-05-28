package me.m0dii.modules.nbttooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class ShulkerPreviewTooltipComponent implements TooltipComponent {
    private static final int COLS = 9;
    private static final int CELL = 18;
    private static final int INNER = 16;

    private final List<ItemStack> stacks;

    public ShulkerPreviewTooltipComponent(List<ItemStack> stacks) {
        this.stacks = stacks == null ? List.of() : stacks;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        int rows = Math.max(1, (int) Math.ceil(stacks.size() / (double) COLS));
        return rows * CELL + 2;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return COLS * CELL + 2;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        int rows = Math.max(1, (int) Math.ceil(stacks.size() / (double) COLS));
        int boxW = COLS * CELL + 2;
        int boxH = rows * CELL + 2;

        context.fill(x, y, x + boxW, y + boxH, 0xCC0B0B0B);
        context.fill(x, y, x + boxW, y + 1, 0x80FFFFFF);
        context.fill(x, y + boxH - 1, x + boxW, y + boxH, 0x60FFFFFF);
        context.fill(x, y, x + 1, y + boxH, 0x60FFFFFF);
        context.fill(x + boxW - 1, y, x + boxW, y + boxH, 0x60FFFFFF);

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int col = i % COLS;
            int row = i / COLS;
            int cellX = x + 1 + col * CELL;
            int cellY = y + 1 + row * CELL;

            context.fill(cellX, cellY, cellX + INNER, cellY + INNER, 0x44222222);
            context.drawItem(stack, cellX, cellY);
            context.drawStackOverlay(textRenderer, stack, cellX, cellY);
        }
    }
}

