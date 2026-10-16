package me.m0dii.modules.worldedit;

import me.m0dii.utils.NbtExtractors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.ArrayList;
import java.util.List;

public final class WorldEditUndoSnapshot {
    private final List<RestoreEntry> entries;

    private WorldEditUndoSnapshot(List<RestoreEntry> entries) {
        this.entries = entries;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public static WorldEditUndoSnapshot capture(ClientLevel world, WorldEditSelection selection) {
        return selection == null ? null : capture(world, List.of(selection));
    }

    public static WorldEditUndoSnapshot capture(ClientLevel world, List<WorldEditSelection> selections) {
        if (world == null || selections == null || selections.isEmpty()) {
            return null;
        }
        if (!isFullyLoaded(world, selections)) {
            return null;
        }

        List<RestoreEntry> entries = new ArrayList<>();
        for (WorldEditSelection selection : selections) {
            if (selection == null) {
                continue;
            }
            if (!captureSelection(world, selection, entries)) {
                return null;
            }
        }
        return new WorldEditUndoSnapshot(entries);
    }

    private static boolean captureSelection(ClientLevel world, WorldEditSelection selection, List<RestoreEntry> entries) {
        if (world == null || selection == null) {
            return false;
        }
        if (!isFullyLoaded(world, selection)) {
            return false;
        }

        for (int y = selection.min().getY(); y <= selection.max().getY(); y++) {
            for (int z = selection.min().getZ(); z <= selection.max().getZ(); z++) {
                int x = selection.min().getX();
                while (x <= selection.max().getX()) {
                    BlockPos pos = new BlockPos(x, y, z);
                    RestoreEntry current = captureEntry(world, pos);
                    if (current == null) {
                        return false;
                    }

                    if (current.blockEntityData != null) {
                        entries.add(current);
                        x++;
                        continue;
                    }

                    int runEndX = x;
                    while (runEndX + 1 <= selection.max().getX()) {
                        RestoreEntry next = captureEntry(world, new BlockPos(runEndX + 1, y, z));
                        if (next == null || next.blockEntityData != null || !current.blockState.equals(next.blockState)) {
                            break;
                        }
                        runEndX++;
                    }

                    entries.add(new RestoreEntry(
                            new BlockPos(x, y, z),
                            new BlockPos(runEndX, y, z),
                            current.blockState,
                            null
                    ));
                    x = runEndX + 1;
                }
            }
        }
        return true;
    }

    public List<String> toCommands() {
        WorldEditRestorePlanner planner = new WorldEditRestorePlanner();
        for (RestoreEntry entry : this.entries) {
            for (int y = entry.min.getY(); y <= entry.max.getY(); y++) {
                for (int z = entry.min.getZ(); z <= entry.max.getZ(); z++) {
                    for (int x = entry.min.getX(); x <= entry.max.getX(); x++) {
                        planner.put(new BlockPos(x, y, z), entry.blockState, entry.blockEntityData);
                    }
                }
            }
        }
        return planner.buildCommands();
    }

    public static boolean isFullyLoaded(ClientLevel world, WorldEditSelection selection) {
        return selection != null && isFullyLoaded(world, List.of(selection));
    }

    public static boolean isFullyLoaded(ClientLevel world, List<WorldEditSelection> selections) {
        if (world == null || selections == null || selections.isEmpty()) {
            return false;
        }
        for (WorldEditSelection selection : selections) {
            if (selection == null) {
                continue;
            }
            int minChunkX = selection.min().getX() >> 4;
            int maxChunkX = selection.max().getX() >> 4;
            int minChunkZ = selection.min().getZ() >> 4;
            int maxChunkZ = selection.max().getZ() >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    if (world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static RestoreEntry captureEntry(ClientLevel world, BlockPos pos) {
        var state = world.getBlockState(pos);
        String stateString = formatBlockState(state);

        CompoundTag blockData = NbtExtractors.extractBlockData(world, pos);
        if (blockData == null) {
            return new RestoreEntry(pos, pos, stateString, null);
        }

        CompoundTag blockEntityData = blockData.copy();
        blockEntityData.remove("id");
        blockEntityData.remove("Properties");
        blockEntityData.remove("x");
        blockEntityData.remove("y");
        blockEntityData.remove("z");
        blockEntityData.remove("keepPacked");

        String nbtSuffix = blockEntityData.isEmpty() ? null : blockEntityData.toString();
        return new RestoreEntry(pos, pos, stateString, nbtSuffix);
    }

    private static String formatBlockState(net.minecraft.world.level.block.state.BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String base = id == null ? "minecraft:air" : id.toString();
        if (state.getProperties().isEmpty()) {
            return base;
        }

        List<String> properties = new ArrayList<>(state.getProperties().size());
        for (Property<?> property : state.getProperties()) {
            properties.add(property.getName() + "=" + propertyValue(state, property));
        }
        return base + "[" + String.join(",", properties) + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String propertyValue(net.minecraft.world.level.block.state.BlockState state, Property<?> property) {
        Property<T> typed = (Property<T>) property;
        return typed.getName(state.getValue(typed));
    }

    private record RestoreEntry(BlockPos min, BlockPos max, String blockState, String blockEntityData) {
    }
}
