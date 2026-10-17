package me.m0dii.modules.packets;

import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Browses {@link PacketInspectorModule#LOG}: filter, direction toggles, noisy-packet toggle, pause, details, copy. */
public class PacketInspectorScreen extends Screen implements ThemedScreen {
    private static final int ROW_H = 11;
    private static final int LIST_TOP = 48;
    private static final int DETAIL_H = 66;

    private final Screen parent;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private EditBox filterBox;
    private Button recordButton;
    private Button inButton;
    private Button outButton;
    private Button noisyButton;
    private Button pauseButton;

    private static boolean showIn = true;
    private static boolean showOut = true;
    private static boolean hideNoisy = true;
    private static String filter = "";
    private boolean paused;
    private List<PacketLog.Entry> rows = List.of();
    private long selectedId = -1;
    private int scroll;
    private int refreshTicker;

    public PacketInspectorScreen(Screen parent) {
        super(Component.literal("Packet Inspector"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = 8;
        filterBox = new EditBox(this.font, x, 22, Math.max(120, this.width - 470), 18, Component.literal("Filter"));
        filterBox.setMaxLength(120);
        filterBox.setHint(Component.literal("Filter: words must match, -word excludes (e.g. container -move)"));
        filterBox.setValue(filter);
        this.addRenderableWidget(filterBox);
        int bx = x + filterBox.getWidth() + 6;
        recordButton = toolbarButton(bx, 70, () -> PacketInspectorModule.INSTANCE.toggleEnabled());
        inButton = toolbarButton(bx += 74, 50, () -> showIn = !showIn);
        outButton = toolbarButton(bx += 54, 50, () -> showOut = !showOut);
        noisyButton = toolbarButton(bx += 54, 84, () -> hideNoisy = !hideNoisy);
        pauseButton = toolbarButton(bx += 88, 54, () -> paused = !paused);
        toolbarButton(bx += 58, 44, () -> {
            PacketInspectorModule.LOG.clear();
            selectedId = -1;
            refresh();
        }).setMessage(Component.literal("Clear"));
        toolbarButton(bx + 48, 44, this::copySelected).setMessage(Component.literal("Copy"));
        this.setFocused(filterBox);
        updateLabels();
        refresh();
    }

    private Button toolbarButton(int x, int width, Runnable action) {
        Button button = Button.builder(Component.literal(""), b -> {
            action.run();
            updateLabels();
            refresh();
        }).bounds(x, 22, width, 18).build();
        this.addRenderableWidget(button);
        return button;
    }

    private void updateLabels() {
        recordButton.setMessage(Component.literal(PacketInspectorModule.LOG.isRecording() ? "● Recording" : "○ Stopped"));
        inButton.setMessage(Component.literal((showIn ? "✔ " : "  ") + "In"));
        outButton.setMessage(Component.literal((showOut ? "✔ " : "  ") + "Out"));
        noisyButton.setMessage(Component.literal(hideNoisy ? "Noisy: hidden" : "Noisy: shown"));
        pauseButton.setMessage(Component.literal(paused ? "Resume" : "Pause"));
    }

    private void refresh() {
        filter = filterBox == null ? filter : filterBox.getValue();
        rows = PacketInspectorModule.LOG.snapshot(filter, showIn, showOut, hideNoisy);
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visibleRows()));
    }

    private int visibleRows() {
        return Math.max(1, (this.height - LIST_TOP - DETAIL_H - 8) / ROW_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!paused && ++refreshTicker % 10 == 0 || !filter.equals(filterBox.getValue())) {
            refresh();
        }
        updateLabels();
        this.extractBackground(context, mouseX, mouseY, delta);
        context.text(this.font, this.title.getString() + "  ·  " + rows.size() + " shown", 8, 8, 0xFFFFFFFF, false);
        if (!PacketInspectorModule.LOG.isRecording() && rows.isEmpty()) {
            context.text(this.font, "Not recording. Press '○ Stopped' to start.", 8, LIST_TOP, 0xFFAAAAAA, false);
        }

        int listBottom = this.height - DETAIL_H - 8;
        int rowsVisible = visibleRows();
        for (int i = 0; i < rowsVisible && scroll + i < rows.size(); i++) {
            PacketLog.Entry entry = rows.get(scroll + i);
            int y = LIST_TOP + i * ROW_H;
            boolean hover = mouseY >= y && mouseY < y + ROW_H && mouseX < this.width - 8 && y < listBottom;
            if (entry.id() == selectedId) {
                context.fill(6, y - 1, this.width - 6, y + ROW_H - 1, 0xFF2F5C8E);
            } else if (hover) {
                context.fill(6, y - 1, this.width - 6, y + ROW_H - 1, 0x40FFFFFF);
            }
            int color = entry.direction() == PacketLog.Direction.IN ? 0xFF8FD3FF : 0xFFFFC27A;
            String line = timeFormat.format(new Date(entry.timeMs())) + "  " + entry.direction().label + "  " + entry.name();
            context.text(this.font, line, 8, y, color, false);
        }

        renderDetails(context, listBottom + 4);
        renderTopCounts(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderDetails(GuiGraphicsExtractor context, int top) {
        context.fill(6, top, this.width - 6, this.height - 4, 0xAA151515);
        PacketLog.Entry entry = selected();
        if (entry == null) {
            context.text(this.font, "Click a packet to see its contents.", 10, top + 4, 0xFF888888, false);
            return;
        }
        int y = top + 4;
        for (String line : wrap(entry.details(), this.width - 24)) {
            if (y > this.height - 14) {
                break;
            }
            context.text(this.font, line, 10, y, 0xFFE0E0E0, false);
            y += 10;
        }
    }

    private void renderTopCounts(GuiGraphicsExtractor context) {
        List<Map.Entry<String, Long>> top = PacketInspectorModule.LOG.topCounts(6);
        int y = LIST_TOP;
        for (Map.Entry<String, Long> entry : top) {
            String text = entry.getValue() + "× " + entry.getKey();
            int x = this.width - 8 - this.font.width(text);
            context.text(this.font, text, x, y, 0xFF777777, false);
            y += 10;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char c : text.toCharArray()) {
            current.append(c);
            if (this.font.width(current.toString()) > maxWidth) {
                current.setLength(current.length() - 1);
                lines.add(current.toString());
                current.setLength(0);
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private PacketLog.Entry selected() {
        for (PacketLog.Entry entry : rows) {
            if (entry.id() == selectedId) {
                return entry;
            }
        }
        return null;
    }

    private void copySelected() {
        PacketLog.Entry entry = selected();
        if (entry != null && this.minecraft != null) {
            this.minecraft.keyboardHandler.setClipboard(entry.direction().name() + " " + entry.name() + ": " + entry.details());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int listBottom = this.height - DETAIL_H - 8;
        if (click.y() >= LIST_TOP && click.y() < listBottom) {
            int index = scroll + (int) ((click.y() - LIST_TOP) / ROW_H);
            if (index >= 0 && index < rows.size()) {
                selectedId = rows.get(index).id();
                paused = true;
                updateLabels();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.clamp(scroll - (int) Math.signum(verticalAmount) * 3, 0, Math.max(0, rows.size() - visibleRows()));
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xD0101010);
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
