package me.m0dii.modules.worldedit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldEditSelectionTest {
    @Test
    void expandGrowsForwardAndReverseSides() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 4, 0), new BlockPos(2, 6, 2));

        WorldEditSelection expanded = selection.expand(Direction.EAST, 2, 1);

        assertEquals(new WorldEditSelection(new BlockPos(-1, 4, 0), new BlockPos(4, 6, 2)), expanded);
    }

    @Test
    void contractShrinksBothSides() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(4, 0, 0));

        WorldEditSelection contracted = selection.contract(Direction.EAST, 2, 1);

        assertEquals(new WorldEditSelection(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)), contracted);
    }

    @Test
    void contractFailsWhenSelectionWouldCollapse() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> selection.contract(Direction.EAST, 2));

        assertEquals("Selection would be reduced past zero on the X axis.", exception.getMessage());
    }

    @Test
    void shiftMovesEntireSelectionAlongDirection() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 1, 0), new BlockPos(2, 3, 4));

        WorldEditSelection shifted = selection.shift(Direction.UP, 5);

        assertEquals(new WorldEditSelection(new BlockPos(0, 6, 0), new BlockPos(2, 8, 4)), shifted);
    }

    @Test
    void outsetAndInsetRespectAxisFlags() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));

        WorldEditSelection horizontalOutset = selection.outset(2, true, false);
        WorldEditSelection verticalInset = selection.inset(1, false, true);

        assertEquals(new WorldEditSelection(new BlockPos(-2, 0, -2), new BlockPos(4, 2, 4)), horizontalOutset);
        assertEquals(new WorldEditSelection(new BlockPos(0, 1, 0), new BlockPos(2, 1, 2)), verticalInset);
    }

    @Test
    void verticalColumnKeepsHorizontalBounds() {
        WorldEditSelection selection = new WorldEditSelection(new BlockPos(4, 10, 6), new BlockPos(8, 12, 7));

        WorldEditSelection column = selection.verticalColumn(-64, 319);

        assertEquals(new WorldEditSelection(new BlockPos(4, -64, 6), new BlockPos(8, 319, 7)), column);
    }
}
