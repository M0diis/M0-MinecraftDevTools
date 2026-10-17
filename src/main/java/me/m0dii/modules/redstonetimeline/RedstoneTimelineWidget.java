package me.m0dii.modules.redstonetimeline;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongToIntFunction;

/**
 * Compact live view of the redstone timeline for the HUD canvas: the most recent N ticks of the busiest tracks.
 */
public final class RedstoneTimelineWidget {
    private static final int HEADER_H = 11;

    private RedstoneTimelineWidget() {
    }

    /** One row to draw: a label and its value (0-15, or -1 for "no data") at a tick. */
    private record Row(String label, LongToIntFunction valueAt) {
    }

    /**
     * Draws the widget inside {@code (x, y, w, h)}. Backgrounds and borders are left to the caller.
     *
     * @param preview draw sample waveforms when nothing has been recorded (canvas editor)
     */
    public static void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                              String title, int rows, int windowTicks, boolean showLabels,
                              int highColor, int textColor, boolean preview) {
        RedstoneTimelineModule module = RedstoneTimelineModule.INSTANCE;
        RedstoneTimeline timeline = module.timeline;
        boolean recording = module.isEnabled();
        boolean empty = timeline.isEmpty();
        boolean sample = empty && preview;

        long end = sample ? 200 : (empty ? 0 : timeline.lastTick());
        long start = end - Math.max(1, windowTicks) + 1;

        // Header: title, then recording state and the visible span on the right.
        String status = recording ? "● REC" : (empty && !sample ? "idle" : "paused");
        int statusColor = recording ? 0xFFE5534B : 0xFF98A1B3;
        String span = windowTicks + " gt";
        int rightW = font.width(status) + 6 + font.width(span);
        g.text(font, fit(font, title, w - rightW - 8), x + 3, y + 2, textColor, false);
        g.text(font, span, x + w - 3 - font.width(span), y + 2, 0xFF5F6879, false);
        g.text(font, status, x + w - 3 - rightW, y + 2, statusColor, false);

        List<Row> data = sample ? sampleRows() : liveRows(timeline);
        int bodyY = y + HEADER_H;
        int bodyH = h - HEADER_H - 2;
        if (data.isEmpty() || bodyH < 4) {
            if (bodyH >= 9) {
                String hint = recording ? "Waiting for a signal change…" : "Start with /rstimeline start";
                g.text(font, fit(font, hint, w - 6), x + 3, bodyY + 2, 0xFF5F6879, false);
            }
            return;
        }

        int shownRows = Math.max(1, Math.min(rows, data.size()));
        int rowH = Math.max(4, bodyH / shownRows);
        shownRows = Math.min(shownRows, Math.max(1, bodyH / rowH));
        int labelW = showLabels && rowH >= 8 ? Math.min(72, w / 3) : 0;
        int plotX = x + 3 + labelW;
        int plotW = Math.max(1, x + w - 3 - plotX);

        for (int i = 0; i < shownRows; i++) {
            Row row = data.get(i);
            int ry = bodyY + i * rowH;
            if (labelW > 0) {
                g.text(font, fit(font, row.label(), labelW - 4), x + 3, ry + (rowH - 8) / 2, 0xFF98A1B3, false);
            }
            g.fill(plotX, ry + rowH - 1, plotX + plotW, ry + rowH, 0x20FFFFFF);
            drawWave(g, row.valueAt(), plotX, ry, plotW, rowH - 1, start, windowTicks, highColor);
        }
        // "Now" line on the right edge while recording.
        if (recording) {
            g.fill(plotX + plotW - 1, bodyY, plotX + plotW, bodyY + shownRows * rowH, 0x80FFFFFF);
        }
    }

    /** Run-length drawing: one fill per stretch of equal value across pixel columns. */
    private static void drawWave(GuiGraphicsExtractor g, LongToIntFunction valueAt, int x, int y, int w, int h,
                                 long start, int windowTicks, int highColor) {
        int runStart = 0;
        int runValue = valueAt.applyAsInt(start);
        for (int px = 1; px <= w; px++) {
            int value = px == w ? Integer.MIN_VALUE : valueAt.applyAsInt(start + (long) px * windowTicks / w);
            if (value == runValue) {
                continue;
            }
            drawRun(g, x + runStart, x + px, y, h, runValue, highColor);
            if (px < w && runValue >= 0 && value >= 0) {
                g.fill(x + px, y, x + px + 1, y + h, 0x50FFFFFF); // edge
            }
            runStart = px;
            runValue = value;
        }
    }

    private static void drawRun(GuiGraphicsExtractor g, int x1, int x2, int y, int h, int value, int highColor) {
        if (value < 0) {
            return;
        }
        if (value == 0) {
            g.fill(x1, y + h - 1, x2, y + h, 0xFF6E3A3A);
            return;
        }
        int barH = Math.max(1, (h - 1) * value / 15);
        int alpha = 0x90 + value * 0x6F / 15;
        g.fill(x1, y + h - barH, x2, y + h, (alpha << 24) | (highColor & 0xFFFFFF));
    }

    private static List<Row> liveRows(RedstoneTimeline timeline) {
        List<Row> rows = new ArrayList<>();
        // Busiest first: the tracks that switch most are usually the ones worth watching.
        List<RedstoneTimeline.Track> tracks = new ArrayList<>(timeline.activeTracks());
        tracks.sort((a, b) -> Integer.compare(b.changes(), a.changes()));
        for (RedstoneTimeline.Track track : tracks) {
            rows.add(new Row(shortLabel(track.label()), track::valueAt));
        }
        return rows;
    }

    private static List<Row> sampleRows() {
        return List.of(
                new Row("Clock", t -> (t / 4) % 2 == 0 ? 15 : 0),
                new Row("Repeater", t -> ((t - 2) / 4) % 2 == 0 ? 15 : 0),
                new Row("Comparator", t -> (int) ((t / 6) % 16)),
                new Row("Piston", t -> (t / 20) % 2 == 0 ? 15 : 0),
                new Row("Lamp", t -> (t / 10) % 3 == 0 ? 15 : 0));
    }

    /** "Redstone Repeater 12 64 -3" -> "Repeater 12 64 -3". */
    private static String shortLabel(String label) {
        return label.startsWith("Redstone ") ? label.substring("Redstone ".length()) : label;
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
