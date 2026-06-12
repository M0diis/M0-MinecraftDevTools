package me.m0dii.modules.worldedit;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEditRestorePlannerTest {
    @Test
    void mergesUniformVolumeIntoSingleFill() {
        WorldEditRestorePlanner planner = new WorldEditRestorePlanner();
        for (int y = 0; y <= 1; y++) {
            for (int z = 0; z <= 1; z++) {
                for (int x = 0; x <= 1; x++) {
                    planner.put(new BlockPos(x, y, z), "minecraft:stone", null);
                }
            }
        }

        assertEquals(List.of("fill 0 0 0 1 1 1 minecraft:stone"), planner.buildCommands());
    }

    @Test
    void keepsBlockEntityRestoresAsSetblock() {
        WorldEditRestorePlanner planner = new WorldEditRestorePlanner();
        planner.put(new BlockPos(0, 0, 0), "minecraft:chest[facing=north]", "{CustomName:'\"A\"'}");

        List<String> commands = planner.buildCommands();

        assertEquals(1, commands.size());
        assertEquals("setblock 0 0 0 minecraft:chest[facing=north]{CustomName:'\"A\"'}", commands.getFirst());
    }

    @Test
    void laterEntriesOverrideEarlierOnOverlap() {
        WorldEditRestorePlanner planner = new WorldEditRestorePlanner();
        planner.put(new BlockPos(0, 0, 0), "minecraft:stone", null);
        planner.put(new BlockPos(0, 0, 0), "minecraft:dirt", null);

        List<String> commands = planner.buildCommands();

        assertEquals(1, commands.size());
        assertTrue(commands.getFirst().endsWith(" minecraft:dirt"));
    }
}
