package me.m0dii.modules.scripting.ide;

import me.m0dii.utils.ConfigFileSafety;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Most-recently-used script names, newest first, saved as one name per line.
 */
public final class RecentFiles {
    public static final int LIMIT = 15;

    private final Path file;
    private final List<String> names = new ArrayList<>();
    private boolean loaded;

    public RecentFiles(Path file) {
        this.file = file;
    }

    /** Moves (or adds) a name to the front and saves. */
    public synchronized void touch(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        load();
        names.remove(name);
        names.addFirst(name);
        while (names.size() > LIMIT) {
            names.removeLast();
        }
        save();
    }

    public synchronized void rename(String oldName, String newName) {
        load();
        int index = names.indexOf(oldName);
        if (index >= 0) {
            names.remove(newName);
            names.set(names.indexOf(oldName), newName);
            save();
        }
    }

    public synchronized void remove(String name) {
        load();
        if (names.remove(name)) {
            save();
        }
    }

    /** Recent names that still pass {@code exists}, newest first. */
    public synchronized List<String> list(Predicate<String> exists) {
        load();
        List<String> out = new ArrayList<>();
        for (String name : names) {
            if (exists == null || exists.test(name)) {
                out.add(name);
            }
        }
        return out;
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file)) {
                    String name = line.strip();
                    if (!name.isEmpty() && !names.contains(name) && names.size() < LIMIT) {
                        names.add(name);
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // A broken list just starts empty.
        }
    }

    private void save() {
        try {
            ConfigFileSafety.writeAtomically(file, String.join(System.lineSeparator(), names) + System.lineSeparator());
        } catch (IOException | RuntimeException ignored) {
            // Not worth interrupting the editor for.
        }
    }
}
