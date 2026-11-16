package me.m0dii.modules.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WorldEditLimitsTest {
    @Test
    void rejectsTallCylinderBeforeVisitingOutOfBuildHeightBlocks() {
        var region = WorldEditRegionMask.cylinder(new BlockPos(0, 64, 0), 2, 2, 10000);
        assertThrows(IllegalArgumentException.class, () -> WorldEditLimits.validate(List.of(region.bounds()), -64, 320));
        assertDoesNotThrow(() -> WorldEditLimits.validate(List.of(new WorldEditSelection(new BlockPos(0, -64, 0), new BlockPos(1, 319, 1))), -64, 320));
    }

    @Test
    void rejectsExcessiveVolumeCopiesAndNonfiniteRadii() {
        var box = new WorldEditSelection(BlockPos.ZERO, new BlockPos(1000, 100, 1000));
        assertThrows(IllegalArgumentException.class, () -> WorldEditLimits.validate(List.of(box), -64, 320));
        var one = new WorldEditSelection(BlockPos.ZERO, BlockPos.ZERO);
        assertThrows(IllegalArgumentException.class, () -> WorldEditLimits.validateCopies(one, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> WorldEditRegionMask.sphere(BlockPos.ZERO, Double.NaN, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> WorldEditRegionMask.cylinder(BlockPos.ZERO, Double.POSITIVE_INFINITY, 1, 1));
    }

    @Test
    void coordinatesAndVolumesCannotWrapAround() {
        var huge = new WorldEditSelection(new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE),
                new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> WorldEditLimits.volume(huge));
        var edge = new WorldEditSelection(new BlockPos(Integer.MAX_VALUE, 0, 0), new BlockPos(Integer.MAX_VALUE, 0, 0));
        assertThrows(ArithmeticException.class, () -> edge.shift(1, 0, 0));
        assertThrows(ArithmeticException.class, () -> edge.shift(Direction.WEST, Integer.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> WorldEditRegionMask.cylinder(new BlockPos(0, 100, 0), 1, 1, Integer.MAX_VALUE));
    }
}
