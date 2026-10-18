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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Search screen for the chest finder: every remembered item in this dimension as a grid (most plentiful first).
 * Type to filter, click an item to preview the containers that hold it (with their whole remembered contents);
 * Enter (or a click on a container row) highlights them.
 */
public class ChestFinderScreen extends Screen implements ThemedScreen {
    private static final int CELL = 22;
    private static final int TOP = 48;
    private static final int CONTAINER_ROW_H = 20;
    private static final int ITEM_ROW_H = 18;

    private final Screen parent;
    private final String initialQuery;
    private EditBox searchBox;
    private List<ContainerMemory.Total> totals = List.of();
    private String lastQuery;
    private int scrollRows;

    // ---- contents preview (part of this screen, laid over the grid) ----
    /** Non-null while the item-contents preview panel is open. */
    private String previewItemId;
    private ContainerMemory.Total previewTotal;
    private long previewExpandedPos = Long.MIN_VALUE;
    private int previewScroll;
    private boolean previewDirty = true;
    private List<PreviewRow> previewRows = List.of();

    /** One line inside the preview panel: a container header or one of its remembered items. */
    private record PreviewRow(int kind, ContainerMemory.Match match, String itemId, String name, int count, int height) {
        static final int CONTAINER = 0;
        static final int ITEM = 1;
    }

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
        if (previewItemId == null) {
            this.setFocused(searchBox);
        } else {
            searchBox.setVisible(false);
        }
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

    // ---- preview helpers ----

    private void openPreview(ContainerMemory.Total total) {
        previewTotal = total;
        previewItemId = total.itemId();
        previewExpandedPos = Long.MIN_VALUE;
        previewScroll = 0;
        previewDirty = true;
        this.setFocused(null);
        if (searchBox != null) {
            searchBox.setVisible(false);
        }
    }

    private void closePreview() {
        previewItemId = null;
        previewTotal = null;
        previewRows = List.of();
        if (searchBox != null) {
            searchBox.setVisible(true);
            this.setFocused(searchBox);
        }
    }

    /** Highlights this container in the world for five minutes and returns you to the game. */
    private void navigateTo(ContainerMemory.Match match) {
        ChestFinderModule.INSTANCE.highlightContainer(match.entry().pos);
        BlockPos pos = BlockPos.of(match.entry().pos);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal("[Find] Highlighted " + match.entry().title
                    + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " — follow the gold outline."));
        }
        this.onClose();
    }

    /** Flat row list for the preview: a header per container, plus the full contents of the expanded one. */
    private void rebuildPreviewRows() {
        List<PreviewRow> rows = new ArrayList<>();
        if (previewItemId != null) {
            List<ContainerMemory.Match> matches = ChestFinderModule.INSTANCE.memory().search(previewItemId, ChestFinderModule.dimension());
            for (ContainerMemory.Match match : matches) {
                rows.add(new PreviewRow(PreviewRow.CONTAINER, match, null, null, match.count(), CONTAINER_ROW_H));
                if (match.entry().pos == previewExpandedPos) {
                    for (Map.Entry<String, Integer> item : match.entry().counts.entrySet()) {
                        rows.add(new PreviewRow(PreviewRow.ITEM, match, item.getKey(),
                                match.entry().names.getOrDefault(item.getKey(), item.getKey()), item.getValue(), ITEM_ROW_H));
                    }
                }
            }
        }
        previewRows = List.copyOf(rows);
        previewDirty = false;
    }

    private int panelTop() {
        return TOP;
    }

    private int panelBottom() {
        return this.height - 34;
    }

    private int panelInnerTop() {
        return panelTop() + 26;
    }

    private int previewMaxScroll() {
        int total = 0;
        for (PreviewRow row : previewRows) {
            total += row.height();
        }
        int inner = panelBottom() - panelInnerTop() - 12;
        return Math.max(0, total - inner);
    }

    private void scrollPreview(int delta) {
        previewScroll = Math.clamp(previewScroll + delta, 0, previewMaxScroll());
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
        context.text(this.font, ThemeDraw.fit(this.font, "Click an item to see its containers · Enter: highlight all · Esc: close",
                this.width - 24), 12, this.height - 14, Theme.TEXT_DIM, false);
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (hovered != null && previewItemId == null) {
            drawTooltip(context, hovered, hoverX, hoverY);
        }
        if (previewItemId != null) {
            drawPreview(context, mouseX, mouseY);
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

    /** Overlay panel showing every remembered container with the previewed item, and the full contents of the expanded one. */
    private void drawPreview(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (previewDirty || previewItemId != null && previewTotal == null) {
            rebuildPreviewRows();
        }
        searchBox.setVisible(false);
        int left = 12;
        int right = this.width - 12;
        int top = panelTop();
        int bottom = panelBottom();
        context.fill(0, 0, this.width, this.height, Theme.SCRIM);
        ThemeDraw.card(context, left - 4, top - 4, right - left + 8, bottom - top + 8);

        String header = previewTotal.name();
        String header2 = previewTotal.count() + " in " + previewTotal.containers() + " container"
                + (previewTotal.containers() == 1 ? "" : "s") + " · " + previewTotal.itemId();
        context.item(stackOf(previewTotal.itemId()), left + 4, top + 2);
        context.text(this.font, ThemeDraw.fit(this.font, header, right - left - 28), left + 24, top + 2, Theme.TEXT, false);
        context.text(this.font, ThemeDraw.fit(this.font, header2, right - left - 28), left + 24, top + 13, Theme.TEXT_DIM, false);

        if (previewRows.isEmpty()) {
            context.text(this.font, "Entry forgotten — reopen its container.", left + 4, top + 34, Theme.TEXT_MUTED, false);
        }
        int y = panelInnerTop() - previewScroll;
        PreviewRow hoveredRowId = null;
        int hoverRowX = 0;
        int hoverRowY = 0;
        for (PreviewRow row : previewRows) {
            if (y + row.height() > bottom - 10) {
                break;
            }
            if (y >= panelInnerTop()) {
                if (row.kind() == PreviewRow.CONTAINER) {
                    ContainerMemory.Match match = row.match();
                    BlockPos pos = BlockPos.of(match.entry().pos);
                    String label = match.count() + "× " + match.entry().title + "  " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
                    boolean hover = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + CONTAINER_ROW_H;
                    if (hover) {
                        hoveredRowId = row;
                        hoverRowX = mouseX;
                        hoverRowY = mouseY;
                    }
                    ThemeDraw.box(context, left, y, right - left, CONTAINER_ROW_H, hover ? Theme.SURFACE_HOVER : Theme.SURFACE_RAISED);
                    boolean expanded = match.entry().pos == previewExpandedPos;
                    context.text(this.font, expanded ? "-" : "+", left + 6, y + 6, Theme.ACCENT_TEXT, false);
                    context.text(this.font, label, left + 18, y + 6, Theme.TEXT, false);
                    String age = ageOf(match.entry().updatedMs);
                    context.text(this.font, age, right - 4 - this.font.width(age), y + 6, Theme.TEXT_DIM, false);
                } else {
                    int indent = 14;
                    boolean hover = mouseX >= left + indent && mouseX < right && mouseY >= y && mouseY < y + ITEM_ROW_H;
                    if (hover) {
                        hoveredRowId = row;
                        hoverRowX = mouseX;
                        hoverRowY = mouseY;
                    }
                    context.item(stackOf(row.itemId()), left + indent, y + 1);
                    String label = row.count() + "× " + row.name();
                    context.text(this.font, ThemeDraw.fit(this.font, label, right - left - indent - 34), left + indent + 18, y + 5, Theme.TEXT, false);
                    context.text(this.font, ThemeDraw.fit(this.font, row.itemId(), right - left - indent - 34),
                            right - 4 - this.font.width(ThemeDraw.fit(this.font, row.itemId(), right - left - indent - 34)),
                            y + 5, Theme.TEXT_DIM, false);
                }
            }
            y += row.height();
        }
        // scrollbar
        int maxScroll = previewMaxScroll();
        if (maxScroll > 0) {
            int trackH = panelBottom() - panelInnerTop();
            int thumbH = Math.max(10, trackH * (100 - Math.min(100, maxScroll / 4)) / 100);
            int thumbY = panelInnerTop() + (trackH - thumbH) * previewScroll / maxScroll;
            context.fill(right - 2, thumbY, right, thumbY + thumbH, Theme.BORDER_STRONG);
        }
        if (hoveredRowId != null) {
            String name = hoveredRowId.kind() == PreviewRow.CONTAINER
                    ? hoveredRowId.match().entry().title
                    : hoveredRowId.name();
            String count = hoveredRowId.count() + (hoveredRowId.kind() == PreviewRow.CONTAINER
                    ? " matching here" : " in this container");
            String extra = hoveredRowId.kind() == PreviewRow.CONTAINER
                    ? "Click: highlight the way · +/−: contents"
                    : hoveredRowId.itemId() + " · click: highlight";
            drawInfoTooltip(context, name, count, extra, hoverRowX, hoverRowY);
        }
        context.text(this.font, ThemeDraw.fit(this.font,
                "Click a container to highlight the way there · +/−: its contents · Enter: highlight all · Esc: back",
                right - left - 12), left + 4, panelBottom() - 9, Theme.TEXT_MUTED, false);
    }

    private static String ageOf(long timeMs) {
        long minutes = Math.max(0, (System.currentTimeMillis() - timeMs) / 60_000L);
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        return hours < 48 ? hours + " h ago" : (hours / 24) + " days ago";
    }

    private void drawInfoTooltip(GuiGraphicsExtractor context, String line1, String line2, String line3, int mouseX, int mouseY) {
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
        if (previewItemId != null) {
            int left = 12;
            int right = this.width - 12;
            if (click.x() < left - 4 || click.x() > right + 4 || click.y() < panelTop() - 4 || click.y() > panelBottom() + 4) {
                closePreview();
                return true;
            }
            int y = panelInnerTop() - previewScroll;
            for (PreviewRow row : previewRows) {
                if (y + row.height() > panelBottom() - 10) {
                    break;
                }
                if (click.y() >= y && click.y() < y + row.height()) {
                    long pos = row.match().entry().pos;
                    if (row.kind() == PreviewRow.CONTAINER && click.x() < left + 18) {
                        previewExpandedPos = previewExpandedPos == pos ? Long.MIN_VALUE : pos;
                        previewDirty = true;
                        return true;
                    }
                    navigateTo(row.match());
                    return true;
                }
                y += row.height();
            }
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
        openPreview(totals.get(index));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (previewItemId != null) {
            scrollPreview(-((int) Math.signum(verticalAmount)) * 3 * ITEM_ROW_H);
            return true;
        }
        int cols = columns();
        int maxRows = (totals.size() + cols - 1) / cols;
        scrollRows = Math.clamp(scrollRows - (int) Math.signum(verticalAmount), 0, Math.max(0, maxRows - visibleRows()));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (previewItemId != null) {
            if (input.input() == InputConstants.KEY_ESCAPE) {
                closePreview();
                return true;
            }
            if (input.input() == InputConstants.KEY_RETURN || input.input() == InputConstants.KEY_NUMPADENTER) {
                findFromChat(previewItemId);
                this.onClose();
                return true;
            }
            return super.keyPressed(input);
        }
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

    private void findFromChat(String query) {
        List<String> lines = ChestFinderModule.INSTANCE.find(query);
        if (this.minecraft != null && this.minecraft.player != null) {
            for (String line : lines) {
                this.minecraft.player.sendSystemMessage(Component.literal("[Find] " + line));
            }
        }
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
