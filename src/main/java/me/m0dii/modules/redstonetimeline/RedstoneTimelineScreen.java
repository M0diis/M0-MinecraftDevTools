package me.m0dii.modules.redstonetimeline;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic-analyser view of {@link RedstoneTimelineModule#timeline}: one row per block, game ticks left to right.
 * Scroll = rows, Shift+scroll / arrows / drag = pan, Ctrl+scroll / +- = zoom.
 * Left-click sets a mark (snapping to the nearest signal edge) to measure delays; right-click clears it.
 */
public class RedstoneTimelineScreen extends Screen implements ThemedScreen {
    // Keypad minus; 26.x InputConstants has no field for it (SDL_SCANCODE_KP_MINUS).
    private static final int KEY_KP_MINUS = 86;

    private static final int ROW_H = 13;
    private static final int LABEL_W = 156;
    private static final int RULER_Y = 48;
    private static final int ROWS_TOP = 60;
    private static final int[] ZOOMS = {1, 2, 3, 4, 6, 8, 12, 16, 24};
    /** A mark placed within this many pixels of a signal edge snaps onto it. */
    private static final int SNAP_PX = 4;
    private static final int DRAG_THRESHOLD_PX = 3;

    private final Screen parent;
    private final RedstoneTimeline timeline = RedstoneTimelineModule.INSTANCE.timeline;
    private Button recordButton;
    private Button followButton;
    private Button showButton;

    private static int zoomIndex = 4;
    private static boolean showAll;
    private boolean follow = true;
    private long viewEnd;
    private int rowScroll;
    private long markTick = Long.MIN_VALUE;
    private List<RedstoneTimeline.Track> rows = List.of();

    // Left button in the plot: a click sets the mark, a drag pans.
    private boolean pressInPlot;
    private boolean dragging;
    private double pressX;
    private long pressViewEnd;
    private String toast;
    private long toastUntil;

    public RedstoneTimelineScreen(Screen parent) {
        super(Component.literal("Redstone Timeline"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int x = 8;
        recordButton = button(x, 92, () -> {
            RedstoneTimelineModule module = RedstoneTimelineModule.INSTANCE;
            if (module.isEnabled()) {
                module.setEnabled(false);
            } else {
                module.start(module.radius());
                follow = true;
            }
        });
        followButton = button(x += 96, 70, () -> follow = !follow);
        showButton = button(x += 74, 92, () -> showAll = !showAll);
        button(x += 96, 44, () -> {
            timeline.clear();
            markTick = Long.MIN_VALUE;
            follow = true;
        }).setMessage(Component.literal("Clear"));
        button(x += 48, 44, this::copyToClipboard).setMessage(Component.literal("Copy"));
        button(x += 48, 20, () -> zoom(-1)).setMessage(Component.literal("-"));
        button(x + 24, 20, () -> zoom(1)).setMessage(Component.literal("+"));
        updateLabels();
    }

    private Button button(int x, int width, Runnable action) {
        Button button = Button.builder(Component.literal(""), b -> {
            action.run();
            updateLabels();
        }).bounds(x, 24, width, 18).build();
        this.addRenderableWidget(button);
        return button;
    }

    private void updateLabels() {
        recordButton.setMessage(Component.literal(RedstoneTimelineModule.INSTANCE.isEnabled() ? "● Recording" : "○ Record here"));
        followButton.setMessage(Component.literal(follow ? "Follow: on" : "Follow: off"));
        showButton.setMessage(Component.literal(showAll ? "Show: all" : "Show: changed"));
    }

    private int px() {
        return ZOOMS[zoomIndex];
    }

    private int plotLeft() {
        return LABEL_W + 8;
    }

    private int plotRight() {
        return this.width - 8;
    }

    private int visibleTicks() {
        return Math.max(1, (plotRight() - plotLeft()) / px());
    }

    private int visibleRows() {
        return Math.max(1, (this.height - ROWS_TOP - 30) / ROW_H);
    }

    private long viewStart() {
        return viewEnd - visibleTicks() + 1;
    }

    private void zoom(int direction) {
        long centerTick = viewEnd - visibleTicks() / 2;
        zoomIndex = Math.clamp(zoomIndex + direction, 0, ZOOMS.length - 1);
        if (!follow) {
            viewEnd = centerTick + visibleTicks() / 2;
            clampView();
        }
    }

    private void pan(long ticks) {
        follow = false;
        viewEnd += ticks;
        clampView();
        updateLabels();
    }

    private void clampView() {
        if (timeline.isEmpty()) {
            return;
        }
        long min = timeline.firstTick() + Math.min(visibleTicks(), timeline.lastTick() - timeline.firstTick() + 1) - 1;
        viewEnd = Math.clamp(viewEnd, min, Math.max(min, timeline.lastTick()));
    }

    private int xOf(long tick) {
        return plotLeft() + (int) ((tick - viewStart()) * px());
    }

    private long tickAt(double mouseX) {
        return viewStart() + (long) ((mouseX - plotLeft()) / px());
    }

    private int rowsBottom() {
        return ROWS_TOP + Math.min(visibleRows(), Math.max(1, rows.size() - rowScroll)) * ROW_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateLabels();
        rows = showAll ? timeline.tracks() : timeline.activeTracks();
        rowScroll = Math.clamp(rowScroll, 0, Math.max(0, rows.size() - visibleRows()));
        if (follow && !timeline.isEmpty()) {
            viewEnd = timeline.lastTick();
        }
        this.extractBackground(context, mouseX, mouseY, delta);
        renderHeader(context);

        if (timeline.isEmpty()) {
            context.text(this.font, "Stand next to your contraption and press '○ Record here' (or /rstimeline start [radius]).",
                    8, ROWS_TOP, Theme.TEXT_MUTED, false);
            context.text(this.font, "Every redstone component in range is sampled each game tick. Tip: add a 'Redstone Timeline'",
                    8, ROWS_TOP + 14, Theme.TEXT_DIM, false);
            context.text(this.font, "widget on the HUD canvas to watch it live while you play.", 8, ROWS_TOP + 26, Theme.TEXT_DIM, false);
            super.extractRenderState(context, mouseX, mouseY, delta);
            return;
        }
        if (rows.isEmpty()) {
            context.text(this.font, "Nothing has switched yet. Trigger your circuit, or press 'Show: changed' to see all "
                    + timeline.tracks().size() + " components.", 8, ROWS_TOP, Theme.TEXT_MUTED, false);
        }

        ThemeDraw.card(context, 4, RULER_Y - 4, this.width - 8, rowsBottom() - RULER_Y + 8);
        renderRuler(context);
        int hoveredRow = hoveredRow(mouseX, mouseY);
        for (int i = 0; i < visibleRows() && rowScroll + i < rows.size(); i++) {
            renderRow(context, rows.get(rowScroll + i), ROWS_TOP + i * ROW_H, i, i == hoveredRow);
        }
        renderMeasurement(context, mouseX, mouseY);
        renderScrollbar(context);

        String help = "Drag / Shift+scroll / ←→: pan · Ctrl+scroll / +-: zoom · Click: mark (snaps to edges) · Right-click: clear"
                + " · F: follow · Scroll: rows · 1 rt = 2 gt";
        context.text(this.font, ThemeDraw.fit(this.font, help, this.width - 16), 8, this.height - 14, Theme.TEXT_DIM, false);
        renderHover(context, mouseX, mouseY);
        renderToast(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphicsExtractor context) {
        RedstoneTimelineModule module = RedstoneTimelineModule.INSTANCE;
        context.text(this.font, this.title.getString(), 8, 8, Theme.TEXT, false);
        int x = 8 + this.font.width(this.title) + 10;
        if (module.isEnabled()) {
            context.text(this.font, "● REC", x, 8, Theme.DANGER, false);
            x += this.font.width("● REC") + 8;
        }
        String where = module.center() == null ? "not started"
                : "radius " + module.radius() + " around " + module.center().toShortString();
        context.text(this.font, ThemeDraw.fit(this.font, where + "  ·  " + module.summary(), this.width - x - 8), x, 8, Theme.TEXT_MUTED, false);
    }

    private void renderRuler(GuiGraphicsExtractor context) {
        long start = viewStart();
        int bottom = rowsBottom();
        int labelEvery = px() >= 8 ? 10 : px() >= 3 ? 20 : px() >= 2 ? 40 : 100;
        for (long tick = Math.max(start, timeline.firstTick()); tick <= viewEnd; tick++) {
            long rel = tick - timeline.firstTick();
            int x = xOf(tick);
            if (rel % labelEvery == 0) {
                context.fill(x, RULER_Y + 8, x + 1, bottom, 0x30FFFFFF);
                context.text(this.font, Long.toString(rel), x + 2, RULER_Y, Theme.TEXT_MUTED, false);
            } else if (px() >= 4 && rel % 2 == 0) {
                context.fill(x, ROWS_TOP, x + 1, bottom, 0x12FFFFFF);
            }
        }
    }

    private void renderRow(GuiGraphicsExtractor context, RedstoneTimeline.Track track, int y, int index, boolean hover) {
        if (index % 2 == 1) {
            context.fill(6, y, this.width - 6, y + ROW_H, 0x0CFFFFFF);
        }
        if (hover) {
            context.fill(6, y, this.width - 6, y + ROW_H, 0x18FFFFFF);
        }
        int on = track.lastValue() > 0 ? Theme.DANGER : Theme.SWITCH_OFF;
        context.fill(8, y + 4, 12, y + 8, on);
        String label = ThemeDraw.fit(this.font, track.label(), LABEL_W - 12);
        context.text(this.font, label, 16, y + 3, hover ? Theme.TEXT : 0xFFC9CFDA, false);

        long start = viewStart();
        long end = viewEnd + 1;
        long tick = Math.max(start, track.firstStepTick());
        while (tick < end) {
            RedstoneTimeline.Segment segment = track.segmentAt(tick);
            if (segment == null) {
                break;
            }
            long segEnd = Math.min(segment.endTick(), end);
            int x1 = xOf(tick);
            int x2 = Math.max(x1 + 1, xOf(segEnd));
            int value = segment.value();
            if (value <= 0) {
                context.fill(x1, y + ROW_H - 3, x2, y + ROW_H - 2, 0xFF6E3A3A);
            } else {
                int h = 2 + (ROW_H - 4) * value / 15;
                int brightness = 0x80 + value * 0x7F / 15;
                int color = 0xFF000000 | brightness << 16 | 0x30 << 8 | 0x2C;
                context.fill(x1, y + ROW_H - 2 - h, x2, y + ROW_H - 2, color);
                if (value < 15 && x2 - x1 >= 12) {
                    context.text(this.font, Integer.toString(value), x1 + 2, y + 3, 0xFFFFFFFF, false);
                }
            }
            if (segment.startTick() >= start && segment.startTick() > track.firstStepTick()) {
                context.fill(x1, y + 1, x1 + 1, y + ROW_H - 1, 0x60FFFFFF); // edge marker
            }
            tick = segEnd;
        }
    }

    /** Mark line, plus a shaded span and its length between the mark and the cursor. */
    private void renderMeasurement(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (markTick == Long.MIN_VALUE) {
            return;
        }
        int bottom = rowsBottom();
        boolean markVisible = markTick >= viewStart() && markTick <= viewEnd;
        int mx = Math.clamp(xOf(markTick), plotLeft(), plotRight());
        if (inPlot(mouseX, mouseY)) {
            long cursorTick = snapTick(tickAt(mouseX), hoveredRow(mouseX, mouseY));
            int cx = Math.clamp(xOf(cursorTick), plotLeft(), plotRight());
            context.fill(Math.min(mx, cx), RULER_Y + 9, Math.max(mx, cx), bottom, 0x1A4C8BF5);
            context.fill(cx, RULER_Y + 9, cx + 1, bottom, 0x804C8BF5);
            String length = RedstoneTimeline.describeTicks(Math.abs(cursorTick - markTick));
            int w = this.font.width(length) + 6;
            int lx = Math.clamp((mx + cx) / 2 - w / 2, plotLeft(), plotRight() - w);
            ThemeDraw.box(context, lx, RULER_Y - 2, w, 11, Theme.ACCENT_PRESSED);
            context.text(this.font, length, lx + 3, RULER_Y, Theme.TEXT, false);
        }
        if (markVisible) {
            context.fill(mx, RULER_Y, mx + 1, bottom, Theme.ACCENT);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor context) {
        int total = rows.size();
        int visible = visibleRows();
        if (total <= visible) {
            return;
        }
        int trackTop = ROWS_TOP;
        int trackH = visible * ROW_H;
        int thumbH = Math.max(10, trackH * visible / total);
        int thumbY = trackTop + (trackH - thumbH) * rowScroll / Math.max(1, total - visible);
        context.fill(this.width - 7, trackTop, this.width - 5, trackTop + trackH, Theme.DIVIDER);
        context.fill(this.width - 7, thumbY, this.width - 5, thumbY + thumbH, Theme.BORDER_STRONG);
    }

    private int hoveredRow(double mouseX, double mouseY) {
        if (mouseY < ROWS_TOP || mouseX < 4 || mouseX >= this.width - 4) {
            return -1;
        }
        int row = (int) ((mouseY - ROWS_TOP) / ROW_H);
        return row < visibleRows() && rowScroll + row < rows.size() ? row : -1;
    }

    private boolean inPlot(double mouseX, double mouseY) {
        return mouseX >= plotLeft() && mouseX < plotRight() && mouseY >= RULER_Y && mouseY < rowsBottom();
    }

    /** {@code tick}, moved onto the nearest edge of the hovered track when one is within {@link #SNAP_PX}. */
    private long snapTick(long tick, int row) {
        if (row < 0) {
            return tick;
        }
        RedstoneTimeline.Track track = rows.get(rowScroll + row);
        int window = Math.max(1, SNAP_PX / px());
        long best = tick;
        long bestDistance = Long.MAX_VALUE;
        for (RedstoneTimeline.Step step : track.steps()) {
            long distance = Math.abs(step.tick() - tick);
            if (step.tick() > track.firstStepTick() && distance <= window && distance < bestDistance) {
                best = step.tick();
                bestDistance = distance;
            }
        }
        return best;
    }

    private void renderHover(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (dragging || mouseX < plotLeft() || mouseX >= plotRight()) {
            return;
        }
        int row = hoveredRow(mouseX, mouseY);
        if (row < 0) {
            return;
        }
        long tick = tickAt(mouseX);
        RedstoneTimeline.Track track = rows.get(rowScroll + row);
        RedstoneTimeline.Segment segment = track.segmentAt(tick);
        List<String> lines = new ArrayList<>();
        lines.add(track.label());
        lines.add("Tick " + (tick - timeline.firstTick()) + (segment == null ? "" : "  ·  signal " + segment.value()));
        if (segment != null) {
            boolean ongoing = segment.endTick() == Long.MAX_VALUE;
            long length = segment.length(timeline.lastTick());
            lines.add((segment.value() > 0 ? "On" : "Off") + " for " + RedstoneTimeline.describeTicks(length)
                    + (ongoing ? " so far" : ""));
        }
        lines.add(track.changes() + " changes");
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, this.font.width(line));
        }
        int x = Math.min(mouseX + 12, this.width - w - 10);
        int y = Math.min(mouseY + 12, this.height - lines.size() * 10 - 10);
        ThemeDraw.box(context, x - 4, y - 4, w + 8, lines.size() * 10 + 6, 0xF0171B22);
        ThemeDraw.outline(context, x - 4, y - 4, w + 8, lines.size() * 10 + 6, Theme.BORDER_STRONG);
        for (int i = 0; i < lines.size(); i++) {
            context.text(this.font, lines.get(i), x, y + i * 10, i == 0 ? Theme.ACCENT_TEXT : Theme.TEXT, false);
        }
    }

    private void renderToast(GuiGraphicsExtractor context) {
        if (toast == null || System.currentTimeMillis() > toastUntil) {
            return;
        }
        int w = this.font.width(toast) + 12;
        int x = (this.width - w) / 2;
        int y = this.height - 34;
        ThemeDraw.box(context, x, y, w, 16, Theme.SURFACE_RAISED);
        ThemeDraw.outline(context, x, y, w, 16, Theme.ACCENT);
        context.text(this.font, toast, x + 6, y + 4, Theme.TEXT, false);
    }

    /** Copies every recorded change as CSV: tick (relative to the start), component, signal. */
    private void copyToClipboard() {
        if (timeline.isEmpty() || this.minecraft == null) {
            showToast("Nothing recorded yet.");
            return;
        }
        StringBuilder csv = new StringBuilder("tick,component,signal\n");
        int count = 0;
        for (RedstoneTimeline.Track track : timeline.tracks()) {
            for (RedstoneTimeline.Step step : track.steps()) {
                csv.append(step.tick() - timeline.firstTick()).append(",\"").append(track.label()).append("\",")
                        .append(step.value()).append('\n');
                count++;
            }
        }
        this.minecraft.keyboardHandler.setClipboard(csv.toString());
        showToast("Copied " + count + " changes as CSV.");
    }

    private void showToast(String message) {
        toast = message;
        toastUntil = System.currentTimeMillis() + 2500;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        if (timeline.isEmpty() || !inPlot(click.x(), click.y())) {
            return false;
        }
        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            markTick = Long.MIN_VALUE;
            return true;
        }
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            pressInPlot = true;
            dragging = false;
            pressX = click.x();
            pressViewEnd = viewEnd;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (pressInPlot && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            double moved = click.x() - pressX;
            if (dragging || Math.abs(moved) > DRAG_THRESHOLD_PX) {
                dragging = true;
                follow = false;
                viewEnd = pressViewEnd - (long) (moved / px());
                clampView();
                updateLabels();
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (pressInPlot && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (!dragging) {
                markTick = snapTick(tickAt(click.x()), hoveredRow(click.x(), click.y()));
            }
            pressInPlot = false;
            dragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        boolean ctrl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
        boolean shift = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
        if (ctrl) {
            zoom(verticalAmount > 0 ? 1 : -1);
        } else if (shift || horizontalAmount != 0) {
            double amount = horizontalAmount != 0 ? horizontalAmount : verticalAmount;
            pan(-(long) Math.signum(amount) * Math.max(1, visibleTicks() / 10));
        } else {
            rowScroll = Math.clamp(rowScroll - (int) Math.signum(verticalAmount) * 3, 0, Math.max(0, rows.size() - visibleRows()));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) {
            pan((key == InputConstants.KEY_LEFT ? -1 : 1) * Math.max(1, visibleTicks() / 10));
            return true;
        }
        if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
            zoom(1);
            return true;
        }
        if (key == InputConstants.KEY_MINUS || key == KEY_KP_MINUS) {
            zoom(-1);
            return true;
        }
        if (key == InputConstants.KEY_F) {
            follow = !follow;
            updateLabels();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, Theme.SCRIM);
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
