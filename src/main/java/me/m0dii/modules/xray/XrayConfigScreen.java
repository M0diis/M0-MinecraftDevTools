package me.m0dii.modules.xray;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class XrayConfigScreen extends Screen {
    private static final int[] COLOR_SWATCHES = {
            // Common X-Ray palette choices (ores/chests/structures)
            0x55FF55, 0x00FF66, 0x00FFFF, 0x55FFFF, 0x33CCFF, 0x4FA8FF,
            0xFF4444, 0xCC0000, 0xFFAA00, 0xFFD54A, 0xFFEE58, 0xA1887F,
            0xCE93D8, 0xAB47BC, 0xF06292, 0xFFFFFF, 0xB0BEC5, 0x78909C
    };

    private final Screen parent;
    private final List<ClickableWidget> dynamicWidgets = new ArrayList<>();
    private List<String> visibleIds = new ArrayList<>();

    private TextFieldWidget addField;
    private TextFieldWidget rangeField;
    private TextFieldWidget colorField;

    private int scroll = 0;
    private int availableRows = 1;
    private int selectedIndex = 0;

    // Cached geometry for manual swatch click handling
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listBottom;
    private int editorPaneX;
    private int editorPaneY;
    private int editorPaneW;
    private int swatchStartX;
    private int swatchStartY;
    private static final int SWATCH_SIZE = 16;
    private static final int SWATCH_GAP = 4;
    private static final int SWATCH_COLUMNS = 9;

    private XrayConfigScreen(Screen parent) {
        super(Text.literal("Xray Configuration"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new XrayConfigScreen(parent);
    }

    @Override
    protected void init() {
        String pendingAdd = this.addField == null ? "" : this.addField.getText();
        String pendingRange = this.rangeField == null ? Integer.toString(XrayManager.getDisplayRange()) : this.rangeField.getText();
        String pendingColor = this.colorField == null ? "" : this.colorField.getText();

        this.clearChildren();
        this.dynamicWidgets.clear();
        this.visibleIds = new ArrayList<>(XrayManager.getBlockIds());
        if (this.visibleIds.isEmpty()) {
            XrayManager.addBlock("minecraft:diamond_ore");
            this.visibleIds = new ArrayList<>(XrayManager.getBlockIds());
        }

        this.selectedIndex = Math.clamp(this.selectedIndex, 0, Math.max(0, this.visibleIds.size() - 1));

        int topY = 34;
        int compactThreshold = 760;
        boolean compactTop = this.width < compactThreshold;

        int addRowY = topY;
        int rangeRowY = compactTop ? (topY + 24) : topY;

        this.listTop = compactTop ? 108 : 86;
        this.listLeft = 12;
        this.listBottom = this.height - 40;

        int paneGap = 12;
        int minPaneWidth = 140;
        int usableContentWidth = Math.max(260, this.width - (this.listLeft * 2) - paneGap);
        int preferredListWidth = Math.max(180, this.width / 2 - 18);
        int maxListWidth = Math.max(140, usableContentWidth - minPaneWidth);
        this.listWidth = Math.clamp(preferredListWidth, 140, maxListWidth);

        int paneX = this.listLeft + this.listWidth + paneGap;
        int paneW = Math.max(minPaneWidth, this.width - paneX - this.listLeft);

        int rowHeight = 22;
        this.availableRows = Math.max(1, (this.listBottom - this.listTop) / rowHeight);
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, this.visibleIds.size() - this.availableRows));

        int addButtonW = 52;
        int addX = this.listLeft;
        int addButtonX;
        int addFieldW;
        if (compactTop) {
            addFieldW = Math.max(120, this.width - (this.listLeft * 2) - addButtonW - 6);
            addButtonX = addX + addFieldW + 6;
        } else {
            int rangeClusterW = 20 + 50 + 20 + 76 + (3 * 4);
            addButtonX = Math.max(addX + 126, this.width - this.listLeft - rangeClusterW - 6 - addButtonW);
            addFieldW = Math.max(120, addButtonX - addX - 6);
        }

        this.addField = new TextFieldWidget(this.textRenderer, addX, addRowY, addFieldW, 20, Text.literal("minecraft:block_id"));
        this.addField.setMaxLength(100);
        this.addField.setText(pendingAdd);
        this.addDrawableChild(this.addField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            String id = this.addField.getText() == null ? "" : this.addField.getText().trim();
            if (!id.isEmpty()) {
                XrayManager.addBlock(id);
                this.selectedIndex = Math.max(0, XrayManager.getBlockIds().size() - 1);
                this.addField.setText("");
                this.init();
            }
        }).dimensions(addButtonX, addRowY, addButtonW, 20).build());

        int doneW = 76;
        int doneX = Math.max(this.listLeft, this.width - this.listLeft - doneW);
        int toggleW = 128;
        int toggleX = Math.max(this.listLeft, doneX - 8 - toggleW);
        if (toggleX == this.listLeft && doneX - 8 - this.listLeft < toggleW) {
            toggleW = Math.max(80, doneX - 8 - this.listLeft);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Xray: " + (XrayModule.INSTANCE.isEnabled() ? "ON" : "OFF")), b -> {
            XrayModule.INSTANCE.setEnabled(!XrayModule.INSTANCE.isEnabled());
            this.init();
        }).dimensions(toggleX, 6, toggleW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(doneX, 6, doneW, 20)
                .build());

        int rangeStartX = compactTop ? this.listLeft : Math.max(this.listLeft, this.width - this.listLeft - (20 + 50 + 20 + 76 + (3 * 4)));
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            XrayManager.adjustDisplayRange(-4);
            this.rangeField.setText(Integer.toString(XrayManager.getDisplayRange()));
        }).dimensions(rangeStartX, rangeRowY, 20, 20).build());

        this.rangeField = new TextFieldWidget(this.textRenderer, rangeStartX + 24, rangeRowY, 50, 20, Text.literal("Range"));
        this.rangeField.setMaxLength(3);
        this.rangeField.setText(pendingRange);
        this.addDrawableChild(this.rangeField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            XrayManager.adjustDisplayRange(4);
            this.rangeField.setText(Integer.toString(XrayManager.getDisplayRange()));
        }).dimensions(rangeStartX + 78, rangeRowY, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyRangeField())
                .dimensions(rangeStartX + 102, rangeRowY, 76, 20)
                .build());

        rebuildRows(this.listTop, rowHeight, paneX, paneW, pendingColor);
    }

    private void rebuildRows(int listTop, int rowHeight, int paneX, int paneW, String pendingColor) {
        for (ClickableWidget widget : this.dynamicWidgets) {
            this.remove(widget);
        }
        this.dynamicWidgets.clear();

        int y = listTop;
        for (int i = this.scroll; i < this.visibleIds.size() && y < this.listBottom - 2; i++) {
            String id = this.visibleIds.get(i);
            final int index = i;
            XrayManager.XrayBlockConfig cfg = XrayManager.getOrCreate(id);
            boolean selected = i == this.selectedIndex;

            String label = (selected ? "> " : "") + shorten(id, 32);
            ButtonWidget select = ButtonWidget.builder(Text.literal(label), b -> {
                        this.selectedIndex = index;
                        this.init();
                    })
                    .dimensions(this.listLeft, y, Math.max(64, this.listWidth - 106), 20)
                    .build();

            ButtonWidget toggle = ButtonWidget.builder(Text.literal(cfg.enabled ? "ON" : "OFF"), b -> {
                        cfg.enabled = !cfg.enabled;
                        XrayManager.save();
                        this.init();
                    })
                    .dimensions(this.listLeft + this.listWidth - 100, y, 46, 20)
                    .build();

            ButtonWidget remove = ButtonWidget.builder(Text.literal("X"), b -> {
                        XrayManager.removeBlock(id);
                        this.selectedIndex = Math.max(0, this.selectedIndex - 1);
                        this.init();
                    })
                    .dimensions(this.listLeft + this.listWidth - 50, y, 44, 20)
                    .build();

            this.dynamicWidgets.add(select);
            this.dynamicWidgets.add(toggle);
            this.dynamicWidgets.add(remove);
            this.addDrawableChild(select);
            this.addDrawableChild(toggle);
            this.addDrawableChild(remove);

            y += rowHeight;
        }

        buildEditorPane(pendingColor, paneX, listTop, paneW);
    }

    private void buildEditorPane(String pendingColor, int paneX, int paneY, int paneW) {
        if (this.visibleIds.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= this.visibleIds.size()) {
            return;
        }

        String id = this.visibleIds.get(this.selectedIndex);
        XrayManager.XrayBlockConfig cfg = XrayManager.getOrCreate(id);

        this.editorPaneX = paneX;
        this.editorPaneY = paneY;
        this.editorPaneW = paneW;

        int colorFieldW = Math.max(40, paneW - 104);
        this.colorField = new TextFieldWidget(this.textRenderer, paneX, paneY + 15, colorFieldW, 20, Text.literal("#RRGGBB"));
        this.colorField.setMaxLength(7);
        this.colorField.setText((pendingColor == null || pendingColor.isBlank()) ? String.format("#%06X", cfg.color & 0xFFFFFF) : pendingColor);
        this.addDrawableChild(this.colorField);
        this.dynamicWidgets.add(this.colorField);

        ButtonWidget applyColor = ButtonWidget.builder(Text.literal("Set"), b -> applyColorFromField(cfg))
                .dimensions(paneX + paneW - 98, paneY + 15, 58, 20)
                .build();

        this.dynamicWidgets.add(applyColor);
        this.addDrawableChild(applyColor);
        this.swatchStartX = paneX + 4;
        this.swatchStartY = paneY + 44;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);
        context.drawTextWithShadow(this.textRenderer, "Xray Configuration", 12, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                "Manage tracked blocks, tune range, and set colors manually or via swatches.",
                12, 20, 0xFFB8B8B8);

        context.fill(this.listLeft - 2, this.listTop - 2, this.listLeft + this.listWidth + 2, this.listBottom + 2, 0x80303030);

        context.fill(this.editorPaneX - 2, this.listTop - 2, this.editorPaneX + this.editorPaneW + 2, this.listBottom + 2, 0x80303030);

        if (!this.visibleIds.isEmpty() && this.selectedIndex >= 0 && this.selectedIndex < this.visibleIds.size()) {
            String id = this.visibleIds.get(this.selectedIndex);
            XrayManager.XrayBlockConfig cfg = XrayManager.getOrCreate(id);
            context.drawTextWithShadow(this.textRenderer, "Color (hex)", this.editorPaneX + 4, this.listTop + 6, 0xFFD0D0D0);

            int previewColor = 0xFF000000 | (cfg.color & 0xFFFFFF);
            int previewX = this.editorPaneX + this.editorPaneW - 28;
            int previewY = this.listTop + 6;
            context.fill(previewX, previewY, previewX + 18, previewY + 18, previewColor);
            context.fill(previewX - 1, previewY - 1, previewX + 19, previewY, 0xFFFFFFFF);
            context.fill(previewX - 1, previewY + 18, previewX + 19, previewY + 19, 0xFFFFFFFF);
            context.fill(previewX - 1, previewY, previewX, previewY + 18, 0xFFFFFFFF);
            context.fill(previewX + 18, previewY, previewX + 19, previewY + 18, 0xFFFFFFFF);

            for (int i = 0; i < COLOR_SWATCHES.length; i++) {
                int x = this.swatchStartX + (i % SWATCH_COLUMNS) * (SWATCH_SIZE + SWATCH_GAP);
                int y = this.swatchStartY + (i / SWATCH_COLUMNS) * (SWATCH_SIZE + SWATCH_GAP);
                int color = 0xFF000000 | (COLOR_SWATCHES[i] & 0xFFFFFF);
                context.fill(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, color);
                int border = (COLOR_SWATCHES[i] == (cfg.color & 0xFFFFFF)) ? 0xFFFFFFFF : 0xFF505050;
                context.fill(x - 1, y - 1, x + SWATCH_SIZE + 1, y, border);
                context.fill(x - 1, y + SWATCH_SIZE, x + SWATCH_SIZE + 1, y + SWATCH_SIZE + 1, border);
                context.fill(x - 1, y, x, y + SWATCH_SIZE, border);
                context.fill(x + SWATCH_SIZE, y, x + SWATCH_SIZE + 1, y + SWATCH_SIZE, border);
            }
        } else {
            context.drawTextWithShadow(this.textRenderer, "No block selected.", this.editorPaneX, this.listTop + 8, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void applyRangeField() {
        try {
            int parsed = Integer.parseInt(this.rangeField.getText().trim());
            XrayManager.setDisplayRange(parsed);
            this.rangeField.setText(Integer.toString(XrayManager.getDisplayRange()));
        } catch (Exception ignored) {
            this.rangeField.setText(Integer.toString(XrayManager.getDisplayRange()));
        }
    }

    private static Integer parseHexColor(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyColorFromField(XrayManager.XrayBlockConfig cfg) {
        Integer parsed = parseHexColor(this.colorField.getText());
        if (parsed == null) {
            this.colorField.setText(String.format("#%06X", cfg.color & 0xFFFFFF));
            return;
        }
        cfg.color = parsed;
        XrayManager.save();
        this.colorField.setText(String.format("#%06X", parsed));
    }

    private static String shorten(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public void close() {
        XrayManager.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, this.visibleIds.size() - this.availableRows);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        this.scroll = Math.clamp(this.scroll + (verticalAmount > 0 ? -1 : 1), 0, maxScroll);
        this.init();
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (click.button() != 0 || this.visibleIds.isEmpty() || this.selectedIndex < 0 || this.selectedIndex >= this.visibleIds.size()) {
            return false;
        }

        int localY = (int) click.y() - this.swatchStartY;
        int localX = (int) click.x() - this.swatchStartX;
        if (localX < 0 || localY < 0) {
            return false;
        }

        int cellW = SWATCH_SIZE + SWATCH_GAP;
        int col = localX / cellW;
        int row = localY / cellW;
        if (col < 0 || col >= SWATCH_COLUMNS || row < 0) {
            return false;
        }

        int inCellX = localX % cellW;
        int inCellY = localY % cellW;
        if (inCellX >= SWATCH_SIZE || inCellY >= SWATCH_SIZE) {
            return false;
        }

        int swatchIndex = row * SWATCH_COLUMNS + col;
        if (swatchIndex < 0 || swatchIndex >= COLOR_SWATCHES.length) {
            return false;
        }

        XrayManager.XrayBlockConfig cfg = XrayManager.getOrCreate(this.visibleIds.get(this.selectedIndex));
        cfg.color = COLOR_SWATCHES[swatchIndex];
        XrayManager.save();
        if (this.colorField != null) {
            this.colorField.setText(String.format("#%06X", cfg.color & 0xFFFFFF));
        }
        return true;
    }
}

