package me.m0dii.modules.waypoints;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WorldKeysTest {
    private final Path directory = Path.of("build", "world-key-test-saves");

    @Test
    void distinctSaveFoldersHaveDistinctKeysRegardlessOfDisplayName() {
        assertNotEquals(WorldKeys.singleplayer(directory.resolve("New World")), WorldKeys.singleplayer(directory.resolve("New World (1)")));
        assertEquals(WorldKeys.singleplayer(directory.resolve("world")), WorldKeys.singleplayer(directory.resolve("nested/../world/.")));
    }

}
