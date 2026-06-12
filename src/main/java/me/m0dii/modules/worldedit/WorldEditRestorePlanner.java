package me.m0dii.modules.worldedit;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class WorldEditRestorePlanner {
    private final Long2ObjectOpenHashMap<RestoreValue> cells = new Long2ObjectOpenHashMap<>();

    void put(BlockPos pos, String blockState, String blockEntityData) {
        if (pos == null || blockState == null || blockState.isBlank()) {
            return;
        }
        cells.put(pos.asLong(), new RestoreValue(blockState, blockEntityData));
    }

    List<String> buildCommands() {
        List<String> commands = new ArrayList<>();
        if (cells.isEmpty()) {
            return commands;
        }

        LongArrayList keys = new LongArrayList(cells.keySet());
        keys.sort(Comparator.comparingInt(WorldEditRestorePlanner::yOf)
                .thenComparingInt(WorldEditRestorePlanner::zOf)
                .thenComparingInt(WorldEditRestorePlanner::xOf));

        LongOpenHashSet consumed = new LongOpenHashSet(keys.size());
        for (long key : keys) {
            if (consumed.contains(key)) {
                continue;
            }

            RestoreValue value = cells.get(key);
            if (value == null) {
                continue;
            }

            BlockPos start = BlockPos.fromLong(key);
            if (value.blockEntityData() != null) {
                commands.add("setblock " + coords(start) + " " + value.blockState() + value.blockEntityData());
                consumed.add(key);
                continue;
            }

            BlockPos end = expandCuboid(start, value, consumed);
            markConsumed(consumed, start, end);
            if (start.equals(end)) {
                commands.add("setblock " + coords(start) + " " + value.blockState());
            } else {
                commands.add("fill " + coords(start) + " " + coords(end) + " " + value.blockState());
            }
        }

        return commands;
    }

    private BlockPos expandCuboid(BlockPos start, RestoreValue value, LongOpenHashSet consumed) {
        int minX = start.getX();
        int minY = start.getY();
        int minZ = start.getZ();

        int maxX = minX;
        while (matches(maxX + 1, minY, minZ, value, consumed)) {
            maxX++;
        }

        int maxZ = minZ;
        boolean canGrowZ = true;
        while (canGrowZ) {
            int candidateZ = maxZ + 1;
            for (int x = minX; x <= maxX; x++) {
                if (!matches(x, minY, candidateZ, value, consumed)) {
                    canGrowZ = false;
                    break;
                }
            }
            if (canGrowZ) {
                maxZ = candidateZ;
            }
        }

        int maxY = minY;
        boolean canGrowY = true;
        while (canGrowY) {
            int candidateY = maxY + 1;
            for (int z = minZ; z <= maxZ && canGrowY; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (!matches(x, candidateY, z, value, consumed)) {
                        canGrowY = false;
                        break;
                    }
                }
            }
            if (canGrowY) {
                maxY = candidateY;
            }
        }

        return new BlockPos(maxX, maxY, maxZ);
    }

    private boolean matches(int x, int y, int z, RestoreValue expected, LongOpenHashSet consumed) {
        long key = BlockPos.asLong(x, y, z);
        if (consumed.contains(key)) {
            return false;
        }
        RestoreValue actual = cells.get(key);
        return expected.equals(actual) && actual != null && actual.blockEntityData() == null;
    }

    private static void markConsumed(LongOpenHashSet consumed, BlockPos start, BlockPos end) {
        for (int y = start.getY(); y <= end.getY(); y++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int x = start.getX(); x <= end.getX(); x++) {
                    consumed.add(BlockPos.asLong(x, y, z));
                }
            }
        }
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static int xOf(long packedPos) {
        return BlockPos.fromLong(packedPos).getX();
    }

    private static int yOf(long packedPos) {
        return BlockPos.fromLong(packedPos).getY();
    }

    private static int zOf(long packedPos) {
        return BlockPos.fromLong(packedPos).getZ();
    }

    private record RestoreValue(String blockState, String blockEntityData) {
    }
}
