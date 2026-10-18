package me.m0dii.modules.scripting.ide;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentFilesTest {
    @Test
    void newestFirstDedupedCappedAndPersisted() throws Exception {
        Path dir = Files.createTempDirectory("recent");
        Path file = dir.resolve("sub").resolve("recent.txt");
        RecentFiles recent = new RecentFiles(file);
        recent.touch("a.kts");
        recent.touch("b.js");
        recent.touch("a.kts");
        assertEquals(List.of("a.kts", "b.js"), recent.list(null));
        for (int i = 0; i < 20; i++) {
            recent.touch("f" + i + ".groovy");
        }
        assertEquals(RecentFiles.LIMIT, recent.list(null).size());
        assertEquals("f19.groovy", recent.list(null).getFirst());

        RecentFiles reloaded = new RecentFiles(file);
        assertEquals(recent.list(null), reloaded.list(null));
        assertEquals(List.of("f19.groovy"), reloaded.list(name -> name.equals("f19.groovy")));
    }

    @Test
    void renameAndRemove() throws Exception {
        RecentFiles recent = new RecentFiles(Files.createTempDirectory("recent2").resolve("r.txt"));
        recent.touch("old.kts");
        recent.touch("other.kts");
        recent.rename("old.kts", "new.kts");
        assertEquals(List.of("other.kts", "new.kts"), recent.list(null));
        recent.remove("other.kts");
        assertEquals(List.of("new.kts"), recent.list(null));
    }
}
