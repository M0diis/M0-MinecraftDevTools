package me.m0dii.modules.worldedit;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEditRegionMaskTest {
    @Test
    void sphereSelectionExcludesBoundingBoxCorners() {
        WorldEditRegionMask region = WorldEditRegionMask.fromSelection(
                new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(4, 4, 4)),
                WorldEditRegionShape.SPHERE
        );

        assertTrue(region.contains(new BlockPos(2, 2, 2)));
        assertFalse(region.contains(new BlockPos(0, 0, 0)));
        assertFalse(region.contains(new BlockPos(4, 4, 4)));
    }

    @Test
    void cylinderSelectionKeepsCenterColumnAndCutsCorners() {
        WorldEditRegionMask region = WorldEditRegionMask.fromSelection(
                new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(4, 3, 4)),
                WorldEditRegionShape.CYLINDER
        );

        assertTrue(region.contains(new BlockPos(2, 0, 2)));
        assertTrue(region.contains(new BlockPos(2, 3, 2)));
        assertFalse(region.contains(new BlockPos(0, 1, 0)));
        assertFalse(region.contains(new BlockPos(4, 2, 4)));
    }

    @Test
    void circleSelectionUsesSmallestAxisAsNormal() {
        WorldEditRegionMask region = WorldEditRegionMask.fromSelection(
                new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(2, 4, 4)),
                WorldEditRegionShape.CIRCLE
        );

        assertTrue(region.contains(new BlockPos(0, 2, 2)));
        assertTrue(region.contains(new BlockPos(2, 2, 2)));
        assertFalse(region.contains(new BlockPos(1, 0, 0)));
        assertFalse(region.contains(new BlockPos(1, 4, 4)));
    }

    @Test
    void surfacePredicatesBehaveLikeWorldEditFaces() {
        WorldEditRegionMask region = WorldEditRegionMask.fromSelection(
                new WorldEditSelection(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2)),
                WorldEditRegionShape.BOX
        );

        assertTrue(region.matchesSurface(new BlockPos(1, 1, 1), WorldEditRegionSurface.SOLID));
        assertFalse(region.matchesSurface(new BlockPos(1, 1, 1), WorldEditRegionSurface.WALLS));
        assertFalse(region.matchesSurface(new BlockPos(1, 1, 1), WorldEditRegionSurface.FLOOR));
        assertFalse(region.matchesSurface(new BlockPos(1, 1, 1), WorldEditRegionSurface.ROOF));
        assertFalse(region.matchesSurface(new BlockPos(1, 1, 1), WorldEditRegionSurface.SHELL));

        assertTrue(region.matchesSurface(new BlockPos(0, 1, 1), WorldEditRegionSurface.WALLS));
        assertTrue(region.matchesSurface(new BlockPos(1, 0, 1), WorldEditRegionSurface.FLOOR));
        assertTrue(region.matchesSurface(new BlockPos(1, 2, 1), WorldEditRegionSurface.ROOF));
        assertTrue(region.matchesSurface(new BlockPos(1, 0, 1), WorldEditRegionSurface.SHELL));
    }

    @Test
    void generatedSphereUsesPlacementCenter() {
        WorldEditRegionMask region = WorldEditRegionMask.sphere(new BlockPos(10, 64, 10), 1.0, 1.0, 1.0);

        assertTrue(region.contains(new BlockPos(10, 64, 10)));
        assertTrue(region.contains(new BlockPos(11, 64, 10)));
        assertFalse(region.contains(new BlockPos(12, 64, 10)));
    }
}
