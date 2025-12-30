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
    private static final String GROOVY_EXT = ".groovy";
    private static final String KOTLIN_EXT = ".kts";

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
            return stream.filter(p -> p.getFileName().toString().endsWith(GROOVY_EXT)
                            || p.getFileName().toString().endsWith(KOTLIN_EXT))
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
}
