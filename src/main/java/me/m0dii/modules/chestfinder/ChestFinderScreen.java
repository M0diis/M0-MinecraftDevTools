package me.m0dii.modules.chestfinder;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * Search screen for the chest finder: every remembered item in this dimension as a grid (most plentiful first).
 * Type to filter, click an item to highlight the containers that have it.
 */
public class ChestFinderScreen extends Screen implements ThemedScreen {
    private static final int CELL = 22;
    private static final int TOP = 48;

    private final Screen parent;
    private final String initialQuery;
    private EditBox searchBox;
    private List<ContainerMemory.Total> totals = List.of();
    private String lastQuery;
    private int scrollRows;

    public ChestFinderScreen(Screen parent, String query) {
        super(Component.literal("Chest Finder"));
        this.parent = parent;
        this.initialQuery = query == null ? "" : query;
    }

    @Override
    protected void init() {
        super.init();
        int w = Math.min(360, this.width - 24);
        searchBox = new EditBox(this.font, (this.width - w) / 2, 22, w, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search remembered items (name or id)"));
        searchBox.setValue(lastQuery == null ? initialQuery : lastQuery);
        this.addRenderableWidget(searchBox);
        this.setFocused(searchBox);
        refresh();
    }

    private void refresh() {
        String query = searchBox.getValue();
        if (query.equals(lastQuery)) {
            return;
        }
        lastQuery = query;
        totals = ChestFinderModule.INSTANCE.memory().totals(ChestFinderModule.dimension(), query);
        scrollRows = 0;
    }

    private int columns() {
        return Math.max(1, Math.min(18, (gridRight() - gridLeft()) / CELL));
    }

    private int gridLeft() {
        return (this.width - Math.min(18 * CELL, this.width - 24)) / 2;
    }

    private int gridRight() {
        return gridLeft() + Math.min(18 * CELL, this.width - 24);
    }

    private int visibleRows() {
        return Math.max(1, (this.height - TOP - 30) / CELL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        refresh();
        context.fill(0, 0, this.width, this.height, Theme.SCRIM);
        context.text(this.font, "Chest Finder", gridLeft(), 8, Theme.TEXT, false);
        String info = totals.size() + " item types · " + ChestFinderModule.INSTANCE.memory().entries(ChestFinderModule.dimension()).size()
                + " containers here";
        context.text(this.font, info, gridRight() - this.font.width(info), 8, Theme.TEXT_DIM, false);

        int cols = columns();
        int rows = visibleRows();
        int left = gridLeft();
        ThemeDraw.card(context, left - 4, TOP - 4, cols * CELL + 8, rows * CELL + 8);
        if (totals.isEmpty()) {
            String empty = lastQuery == null || lastQuery.isBlank()
                    ? "Nothing remembered yet. Open chests, barrels or shulker boxes and they show up here."
                    : "No remembered container has that.";
            context.text(this.font, ThemeDraw.fit(this.font, empty, cols * CELL - 8), left + 4, TOP + 6, Theme.TEXT_MUTED, false);
        }
        ContainerMemory.Total hovered = null;
        int hoverX = 0;
        int hoverY = 0;
        for (int i = 0; i < rows * cols; i++) {
            int index = scrollRows * cols + i;
            if (index >= totals.size()) {
                break;
            }
            ContainerMemory.Total total = totals.get(index);
            int x = left + (i % cols) * CELL;
            int y = TOP + (i / cols) * CELL;
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            ThemeDraw.box(context, x + 1, y + 1, CELL - 2, CELL - 2, hover ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED);
            context.item(stackOf(total.itemId()), x + 3, y + 3);
            String count = shortCount(total.count());
            context.pose().pushMatrix();
            context.pose().translate(x + CELL - 2 - this.font.width(count) * 0.75f, y + CELL - 8);
            context.pose().scale(0.75f, 0.75f);
            context.text(this.font, count, 0, 0, Theme.TEXT, true);
            context.pose().popMatrix();
            if (hover) {
                hovered = total;
                hoverX = mouseX;
                hoverY = mouseY;
            }
        }
        int maxRows = (totals.size() + cols - 1) / cols;
        if (maxRows > rows) {
            int trackH = rows * CELL;
            int thumbH = Math.max(10, trackH * rows / maxRows);
            int thumbY = TOP + (trackH - thumbH) * scrollRows / Math.max(1, maxRows - rows);
            context.fill(left + cols * CELL + 5, thumbY, left + cols * CELL + 6, thumbY + thumbH, Theme.BORDER_STRONG);
        }
        context.text(this.font, ThemeDraw.fit(this.font, "Click an item to highlight its containers · /find <item> works too · Esc: close",
                this.width - 24), 12, this.height - 14, Theme.TEXT_DIM, false);
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (hovered != null) {
            drawTooltip(context, hovered, hoverX, hoverY);
        }
    }

    private void drawTooltip(GuiGraphicsExtractor context, ContainerMemory.Total total, int mouseX, int mouseY) {
        String line1 = total.name();
        String line2 = total.count() + " in " + total.containers() + " container" + (total.containers() == 1 ? "" : "s");
        String line3 = total.itemId();
        int w = Math.max(this.font.width(line1), Math.max(this.font.width(line2), this.font.width(line3))) + 8;
        int x = Math.min(mouseX + 10, this.width - w - 4);
        int y = Math.min(mouseY + 4, this.height - 40);
        ThemeDraw.box(context, x, y, w, 34, Theme.SURFACE_RAISED);
        ThemeDraw.outline(context, x, y, w, 34, Theme.BORDER_STRONG);
        context.text(this.font, line1, x + 4, y + 3, Theme.TEXT, false);
        context.text(this.font, line2, x + 4, y + 13, Theme.ACCENT_TEXT, false);
        context.text(this.font, line3, x + 4, y + 23, Theme.TEXT_DIM, false);
    }

    private static ItemStack stackOf(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? Items.BARRIER : BuiltInRegistries.ITEM.getValue(id);
        return new ItemStack(item == null ? Items.BARRIER : item);
    }

    private static String shortCount(int count) {
        if (count >= 10_000) {
            return (count / 1000) + "k";
        }
        if (count >= 1000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return Integer.toString(count);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int cols = columns();
        int col = (int) ((click.x() - gridLeft()) / CELL);
        int row = (int) ((click.y() - TOP) / CELL);
        if (click.x() < gridLeft() || click.y() < TOP || col >= cols || row >= visibleRows()) {
            return false;
        }
        int index = (scrollRows + row) * cols + col;
        if (index < 0 || index >= totals.size()) {
            return false;
        }
        String itemId = totals.get(index).itemId();
        List<String> lines = ChestFinderModule.INSTANCE.find(itemId);
        if (this.minecraft != null && this.minecraft.player != null) {
            for (String line : lines) {
                this.minecraft.player.sendSystemMessage(Component.literal("[Find] " + line));
            }
        }
        this.onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int cols = columns();
        int maxRows = (totals.size() + cols - 1) / cols;
        scrollRows = Math.clamp(scrollRows - (int) Math.signum(verticalAmount), 0, Math.max(0, maxRows - visibleRows()));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if ((input.input() == InputConstants.KEY_RETURN || input.input() == InputConstants.KEY_NUMPADENTER) && !totals.isEmpty()) {
            List<String> lines = ChestFinderModule.INSTANCE.find(searchBox.getValue().isBlank() ? totals.getFirst().itemId() : searchBox.getValue());
            if (this.minecraft != null && this.minecraft.player != null) {
                lines.forEach(line -> this.minecraft.player.sendSystemMessage(Component.literal("[Find] " + line)));
            }
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
