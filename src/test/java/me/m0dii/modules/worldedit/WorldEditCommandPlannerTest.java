package me.m0dii.modules.worldedit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEditCommandPlannerTest {
    @Test
    void stacksUsingSelectionSizeAsOffset() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(2, 1, 1));

        List<String> commands = WorldEditCommandPlanner.planStack(selection, 2, Direction.EAST, false);

        assertEquals("clone 0 0 0 2 1 1 3 0 0 replace force", commands.getFirst());
        assertEquals("clone 0 0 0 2 1 1 6 0 0 replace force", commands.getLast());
    }

    @Test
    void moveClearsOnlySourceOutsideOverlap() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(2, 0, 0));

        List<String> commands = WorldEditCommandPlanner.planMove(selection, 1, Direction.EAST, false);

        assertEquals(List.of(
                "clone 0 0 0 2 0 0 1 0 0 replace force",
                "fill 0 0 0 0 0 0 air"
        ), commands);
    }

    @Test
    void partitionsLargeFillIntoVanillaSizedCommands() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(255, 127, 3));

        List<String> commands = WorldEditCommandPlanner.planSet(selection, "stone");

        assertTrue(commands.size() > 1);
        for (String command : commands) {
            assertTrue(command.startsWith("fill "));
            assertTrue(command.endsWith(" stone"));
        }
    }
}
