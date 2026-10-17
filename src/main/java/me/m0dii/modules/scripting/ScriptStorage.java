package me.m0dii.modules.scripting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public final class ScriptStorage {
    private static final Path SCRIPTS_DIR = Paths.get("config/m0-dev-tools/scripts");
    private static final String AUTOMATION_HEARTBEAT = "heartbeat.kts";

    private ScriptStorage() {
    }

    public static void ensureDir() throws IOException {
        Files.createDirectories(SCRIPTS_DIR);
    }

    public static List<String> listScripts() {
        if (!Files.exists(SCRIPTS_DIR)) {
            return Collections.emptyList();
        }
        try (var stream = Files.list(SCRIPTS_DIR)) {
            return stream.filter(p -> ScriptTypes.isScriptFile(p.getFileName().toString()))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static String readScript(@NotNull String name) throws IOException {
        Path file = SCRIPTS_DIR.resolve(name);
        return Files.readString(file);
    }

    /** Last modification time in ms, or 0 when the file is missing. */
    public static long lastModified(@NotNull String name) {
        try {
            Path file = SCRIPTS_DIR.resolve(name);
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0L;
        } catch (IOException e) {
            return 0L;
        }
    }

    public static boolean exists(@NotNull String name) {
        return Files.exists(SCRIPTS_DIR.resolve(name));
    }

    public static void writeScript(@NotNull String name, @Nullable String content) throws IOException {
        ensureDir();
        Path file = SCRIPTS_DIR.resolve(name);
        Files.writeString(file, content);
    }

    public static boolean deleteScript(@NotNull String name) throws IOException {
        Path file = SCRIPTS_DIR.resolve(name);
        return Files.deleteIfExists(file);
    }

    public static boolean renameScript(@NotNull String oldName, @NotNull String newName) throws IOException {
        ensureDir();

        Path oldFile = SCRIPTS_DIR.resolve(oldName);
        Path newFile = SCRIPTS_DIR.resolve(newName);

        if (Files.exists(newFile)) {
            return false;
        }

        Files.move(oldFile, newFile);
        return true;
    }

    public static void ensureAutomationExamples() throws IOException {
        ensureDir();
        Path heartbeat = SCRIPTS_DIR.resolve(AUTOMATION_HEARTBEAT);
        if (!Files.exists(heartbeat)) {
            Files.writeString(heartbeat, """
                    // Example automation script. Available bindings: event, client, player, world
                    val eventType = event.type().name()
                    player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("[automation] " + eventType))
                    "ok"
                    """);
        }
    }
}
