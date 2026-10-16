package me.m0dii.modules.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class WorldEditCommandPlanner {
    public static final int MAX_COMMAND_VOLUME = 32768;

    private WorldEditCommandPlanner() {
    }

    public static List<String> planSet(WorldEditSelection selection, String block) {
        List<String> commands = new ArrayList<>();
        for (WorldEditSelection part : selection.partition(MAX_COMMAND_VOLUME)) {
            commands.add("fill " + corners(part) + " " + block);
        }
        return commands;
    }

    public static List<String> planReplace(WorldEditSelection selection, String from, String to) {
        List<String> commands = new ArrayList<>();
        for (WorldEditSelection part : selection.partition(MAX_COMMAND_VOLUME)) {
            commands.add("fill " + corners(part) + " " + to + " replace " + from);
        }
        return commands;
    }

    public static List<String> planStack(WorldEditSelection selection, int count, Direction direction, boolean masked) {
        List<String> commands = new ArrayList<>();
        int stepX = direction.getStepX() * selection.sizeX();
        int stepY = direction.getStepY() * selection.sizeY();
        int stepZ = direction.getStepZ() * selection.sizeZ();
        String maskMode = masked ? "masked" : "replace";

        List<WorldEditSelection> sourceParts = selection.partition(MAX_COMMAND_VOLUME);
        for (int copyIndex = 1; copyIndex <= count; copyIndex++) {
            int dx = stepX * copyIndex;
            int dy = stepY * copyIndex;
            int dz = stepZ * copyIndex;
            for (WorldEditSelection part : sourceParts) {
                BlockPos destMin = part.min().offset(dx, dy, dz);
                commands.add("clone " + corners(part) + " " + coords(destMin) + " " + maskMode + " force");
            }
        }

        return commands;
    }

    public static List<String> planMove(WorldEditSelection selection, int distance, Direction direction, boolean masked) {
        List<String> commands = new ArrayList<>();
        int dx = direction.getStepX() * distance;
        int dy = direction.getStepY() * distance;
        int dz = direction.getStepZ() * distance;
        boolean descending = dx > 0 || dy > 0 || dz > 0;
        String maskMode = masked ? "masked" : "replace";

        for (WorldEditSelection part : selection.partitionOrdered(direction.getAxis(), descending, MAX_COMMAND_VOLUME)) {
            BlockPos destMin = part.min().offset(dx, dy, dz);
            commands.add("clone " + corners(part) + " " + coords(destMin) + " " + maskMode + " force");
        }

        WorldEditSelection destination = selection.shift(dx, dy, dz);
        WorldEditSelection overlap = selection.intersection(destination);
        for (WorldEditSelection clearRegion : selection.subtract(overlap)) {
            for (WorldEditSelection part : clearRegion.partition(MAX_COMMAND_VOLUME)) {
                commands.add("fill " + corners(part) + " air");
            }
        }

        return commands;
    }

    private static String corners(WorldEditSelection selection) {
        return coords(selection.min()) + " " + coords(selection.max());
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
