package me.m0dii.utils;

import me.m0dii.M0DevTools;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helpers that keep user data safe when a config/data file cannot be read or is being rewritten.
 */
public final class ConfigFileSafety {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigFileSafety() {
    }

    /**
     * Copies a file that failed to load next to itself as {@code <name>.corrupt-<timestamp>} so a later save cannot
     * destroy the only copy. Returns the backup path, or {@code null} if nothing was backed up.
     */
    public static Path backupUnreadable(Path file, Throwable reason) {
        if (file == null || !Files.exists(file)) {
            return null;
        }
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(STAMP));
        try {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            M0DevTools.LOGGER.error("Could not read {} ({}). Kept a copy at {}; starting with empty data.",
                    file.getFileName(), reason == null ? "unknown error" : reason.getMessage(), backup.getFileName());
            return backup;
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Could not read {} and could not back it up: {}", file.getFileName(), e.getMessage());
            return null;
        }
    }

    /**
     * Writes text through a temporary file and moves it into place, so a crash mid-write cannot leave a half-written
     * (and therefore unreadable) file behind.
     */
    public static void writeAtomically(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, content);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
