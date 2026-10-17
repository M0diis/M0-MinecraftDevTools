package me.m0dii.modules.scripting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Recent script output shown in the script editor's Console tab: {@code m0.print}/{@code m0.log} lines,
 * results of runs started from the editor or {@code /runscript}, and every script error.
 */
public final class ScriptConsole {
    public enum Level { INFO, RESULT, ERROR }

    public record Line(long timeMs, Level level, String text) {
    }

    private static final int CAPACITY = 300;
    private static final Deque<Line> LINES = new ArrayDeque<>();
    private static long version;

    private ScriptConsole() {
    }

    public static synchronized void add(Level level, String text) {
        String value = text == null ? "null" : text;
        for (String part : value.split("\\R", -1)) {
            LINES.addLast(new Line(System.currentTimeMillis(), level, part));
        }
        while (LINES.size() > CAPACITY) {
            LINES.removeFirst();
        }
        version++;
    }

    public static synchronized List<Line> lines() {
        return new ArrayList<>(LINES);
    }

    /** Changes whenever lines are added or cleared, so views know when to scroll to the bottom. */
    public static synchronized long version() {
        return version;
    }

    public static synchronized int size() {
        return LINES.size();
    }

    public static synchronized void clear() {
        LINES.clear();
        version++;
    }
}
