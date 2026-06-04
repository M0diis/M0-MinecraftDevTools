package me.m0dii.gui.local;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class UiSelectionList {
    private int x;
    private int y;
    private int width;
    private int height;
    private int rowHeight = 16;
    private int scroll = 0;
    private int selectedIndex = -1;
    private String emptyText = "(empty)";
    private List<String> rows = List.of();

    public UiSelectionList(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        clampScroll();
    }

    public void setRows(List<String> rows) {
        this.rows = rows == null ? List.of() : new ArrayList<>(rows);
        if (this.rows.isEmpty()) {
            this.selectedIndex = -1;
        } else {
            this.selectedIndex = Math.clamp(this.selectedIndex, 0, this.rows.size() - 1);
        }
        clampScroll();
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = Math.max(10, rowHeight);
        clampScroll();
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText == null ? "(empty)" : emptyText;
    }

    public void setSelectedIndex(int index) {
        if (this.rows.isEmpty()) {
            this.selectedIndex = -1;
        } else {
            this.selectedIndex = Math.clamp(index, 0, this.rows.size() - 1);
        }
        ensureSelectionVisible();
    }

    public int selectedIndex() {
        return this.selectedIndex;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        UiForms.drawPanel(context, this.x, this.y, this.width, this.height);
        if (this.rows.isEmpty()) {
            context.drawTextWithShadow(textRenderer, this.emptyText, this.x + 6, this.y + 6, UiTheme.TEXT_MUTED);
            return;
        }

        int visibleRows = visibleRows();
        int end = Math.min(this.rows.size(), this.scroll + visibleRows);
        for (int row = this.scroll; row < end; row++) {
            int drawY = this.y + 4 + ((row - this.scroll) * this.rowHeight);
            boolean selected = row == this.selectedIndex;
            boolean hovered = mouseX >= this.x + 2
                    && mouseX <= this.x + this.width - 2
                    && mouseY >= drawY - 1
                    && mouseY <= drawY + this.rowHeight - 2;
            if (selected) {
                context.fill(this.x + 2, drawY - 1, this.x + this.width - 2, drawY + this.rowHeight - 2, 0x704A7CC7);
            } else if (hovered) {
                context.fill(this.x + 2, drawY - 1, this.x + this.width - 2, drawY + this.rowHeight - 2, 0x302B5C85);
            }

            String text = trimToWidth(textRenderer, this.rows.get(row), Math.max(8, this.width - 12));
            context.drawTextWithShadow(textRenderer, text, this.x + 6, drawY, selected ? 0xFFFFFFFF : UiTheme.TEXT_MUTED);
        }

        if (this.rows.size() > visibleRows) {
            int trackX = this.x + this.width - 4;
            int trackY = this.y + 3;
            int trackH = this.height - 6;
            context.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x40FFFFFF);
            int thumbH = Math.max(10, (trackH * visibleRows) / this.rows.size());
            int maxScroll = Math.max(1, this.rows.size() - visibleRows);
            int thumbY = trackY + ((trackH - thumbH) * this.scroll) / maxScroll;
            context.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xB0FFFFFF);
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && row < this.rows.size()) {
            this.selectedIndex = row;
            ensureSelectionVisible();
        }
        return true;
    }

    public boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return false;
        }
        int delta = verticalAmount > 0 ? -1 : 1;
        this.scroll = Math.clamp(this.scroll + delta, 0, maxScroll);
        return true;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        int localY = (int) mouseY - this.y - 4;
        if (localY < 0) {
            return -1;
        }
        int rowOffset = localY / this.rowHeight;
        return this.scroll + rowOffset;
    }

    private int visibleRows() {
        return Math.max(1, (this.height - 8) / this.rowHeight);
    }

    private int maxScroll() {
        return Math.max(0, this.rows.size() - visibleRows());
    }

    private void ensureSelectionVisible() {
        if (this.selectedIndex < 0) {
            clampScroll();
            return;
        }
        int visibleRows = visibleRows();
        if (this.selectedIndex < this.scroll) {
            this.scroll = this.selectedIndex;
        } else if (this.selectedIndex >= this.scroll + visibleRows) {
            this.scroll = this.selectedIndex - visibleRows + 1;
        }
        clampScroll();
    }

    private void clampScroll() {
        this.scroll = Math.clamp(this.scroll, 0, maxScroll());
    }

    private static String trimToWidth(TextRenderer textRenderer, String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        while (text.length() > 4 && textRenderer.getWidth(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
}
