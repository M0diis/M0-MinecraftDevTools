package me.m0dii.modules.worldedit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import me.m0dii.M0DevTools;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class WorldEditSyncServer {
    private static boolean registered = false;

    private WorldEditSyncServer() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;
        WorldEditTasks.register();

        ServerPlayNetworking.registerGlobalReceiver(WorldEditSyncPayloads.EditRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> handleRequest(context.player(), payload)));
    }

    private static void handleRequest(ServerPlayer player, WorldEditSyncPayloads.EditRequestPayload payload) {
        if (player == null) {
            return;
        }
        if (!Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) {
            player.sendSystemMessage(Component.literal("[WE] You need permission level 2 to use mod WorldEdit."));
            return;
        }

        if (WorldEditTasks.isBusy(player)) {
            return;
        }
        try {
            WorldEditRegionMask selectionRegion = regionFromPayload(payload);
            switch (payload.operation()) {
                case UNDO -> WorldEditServerHistory.undo(player);
                case REDO -> WorldEditServerHistory.redo(player);
                case SET -> executeSurfaceFill(player, "//set", selectionRegion, payload.primaryArg(), WorldEditRegionSurface.SOLID);
                case REPLACE -> executeReplace(player, selectionRegion, payload.primaryArg(), payload.secondaryArg());
                case WALLS -> executeSurfaceFill(player, "//walls", selectionRegion, payload.primaryArg(), WorldEditRegionSurface.WALLS);
                case FLOOR -> executeSurfaceFill(player, "//floor", selectionRegion, payload.primaryArg(), WorldEditRegionSurface.FLOOR);
                case ROOF -> executeSurfaceFill(player, "//roof", selectionRegion, payload.primaryArg(), WorldEditRegionSurface.ROOF);
                case ENCLOSE -> executeSurfaceFill(player, "//enclose", selectionRegion, payload.primaryArg(), WorldEditRegionSurface.SHELL);
                case STACK -> executeStack(player, selectionRegion, payload.amount(), payload.direction(), payload.masked());
                case MOVE -> executeMove(player, selectionRegion, payload.amount(), payload.direction(), payload.masked());
                case CYL -> executeCylinder(player, payload.min(), payload.primaryArg(), payload.secondaryArg(), payload.amount(), false);
                case HCYL -> executeCylinder(player, payload.min(), payload.primaryArg(), payload.secondaryArg(), payload.amount(), true);
                case SPHERE -> executeSphere(player, payload.min(), payload.primaryArg(), payload.secondaryArg(), payload.masked(), false);
                case HSPHERE -> executeSphere(player, payload.min(), payload.primaryArg(), payload.secondaryArg(), payload.masked(), true);
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            player.sendSystemMessage(Component.literal("[WE] " + e.getMessage()));
        }
    }

    private static void executeSurfaceFill(ServerPlayer player,
                                           String label,
                                           WorldEditRegionMask region,
                                           String rawBlock,
                                           WorldEditRegionSurface surface) {
        runTrackedEdit(player, label, List.of(region.bounds()), (changes, task) -> {
            ServerLevel world = player.level();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, task, region, surface, target);
        });
    }

    private static void executeReplace(ServerPlayer player,
                                       WorldEditRegionMask region,
                                       String rawFrom,
                                       String rawTo) {
        runTrackedEdit(player, "//replace", List.of(region.bounds()), (changes, task) -> {
            ServerLevel world = player.level();
            WorldEditParsedBlock from = WorldEditParsedBlock.parse(world, rawFrom);
            WorldEditParsedBlock to = WorldEditParsedBlock.parse(world, rawTo);
            task.add(BlockPos.betweenClosed(region.bounds().min(), region.bounds().max()), pos -> {
                if (region.contains(pos) && !world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    throw new IllegalStateException("Affected chunk unloaded; edit cancelled.");
                }
                if (region.contains(pos) && from.matches(world, pos)) {
                    applyTarget(world, changes, pos, to);
                }
            });
        });
    }

    private static void executeStack(ServerPlayer player,
                                     WorldEditRegionMask region,
                                     int count,
                                     String directionName,
                                     boolean masked) {
        Direction direction = Direction.byName(directionName);
        if (direction == null || count < 1) {
            player.sendSystemMessage(Component.literal("[WE] Invalid stack request."));
            return;
        }
        WorldEditLimits.validateCopies(region.bounds(), count);

        runTrackedEdit(player, "//stack",
                touchedStackSelections(region, count, direction),
                (changes, task) -> {
                    ServerLevel world = player.level();
                    List<PositionedSnapshot> source = captureRegionSnapshots(world, region, task);
                    int stepX = direction.getStepX() * region.bounds().sizeX();
                    int stepY = direction.getStepY() * region.bounds().sizeY();
                    int stepZ = direction.getStepZ() * region.bounds().sizeZ();

                    Iterable<PositionedSnapshot> copies = () -> new Iterator<>() {
                        private int copy = 1;
                        private int index;
                        public boolean hasNext() { return copy <= count && !source.isEmpty(); }
                        public PositionedSnapshot next() {
                            if (!hasNext()) throw new NoSuchElementException();
                            PositionedSnapshot original = source.get(index++);
                            PositionedSnapshot result = new PositionedSnapshot(original.pos.offset(stepX * copy, stepY * copy, stepZ * copy), original.snapshot);
                            if (index == source.size()) { index = 0; copy++; }
                            return result;
                        }
                    };
                    task.add(copies, block -> {
                        if (!masked || !block.snapshot.isAir()) {
                            applyTarget(world, changes, block.pos, block.snapshot);
                        }
                    });
                });
    }

    private static void executeMove(ServerPlayer player,
                                    WorldEditRegionMask region,
                                    int distance,
                                    String directionName,
                                    boolean masked) {
        Direction direction = Direction.byName(directionName);
        if (direction == null || distance == 0) {
            player.sendSystemMessage(Component.literal("[WE] Invalid move request."));
            return;
        }

        WorldEditRegionMask destination = region.shifted(direction, distance);
        runTrackedEdit(player, "//move",
                List.of(region.bounds(), destination.bounds()),
                (changes, task) -> {
                    ServerLevel world = player.level();
                    List<PositionedSnapshot> source = captureRegionSnapshots(world, region, task);
                    int dx = direction.getStepX() * distance;
                    int dy = direction.getStepY() * distance;
                    int dz = direction.getStepZ() * distance;

                    task.add(source, block -> {
                        if (masked && block.snapshot.isAir()) {
                            return;
                        }
                        applyTarget(world, changes, block.pos.offset(dx, dy, dz), block.snapshot);
                    });

                    task.add(BlockPos.betweenClosed(region.bounds().min(), region.bounds().max()), pos -> {
                        if (region.contains(pos) && !destination.contains(pos)) {
                            applyTarget(world, changes, pos, WorldEditBlockSnapshot.air());
                        }
                    });
                });
    }

    private static void executeCylinder(ServerPlayer player,
                                        BlockPos anchor,
                                        String rawBlock,
                                        String radiiSpec,
                                        int height,
                                        boolean hollow) {
        if (height < 1) {
            player.sendSystemMessage(Component.literal("[WE] Cylinder height must be >= 1."));
            return;
        }

        double[] radii;
        try {
            radii = parseCylinderRadii(radiiSpec);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("[WE] " + e.getMessage()));
            return;
        }

        WorldEditRegionMask region = WorldEditRegionMask.cylinder(anchor, radii[0], radii[1], height);
        runTrackedEdit(player, hollow ? "//hcyl" : "//cyl", List.of(region.bounds()), (changes, task) -> {
            ServerLevel world = player.level();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, task, region, hollow ? WorldEditRegionSurface.SHELL : WorldEditRegionSurface.SOLID, target);
        });
    }

    private static void executeSphere(ServerPlayer player,
                                      BlockPos anchor,
                                      String rawBlock,
                                      String radiiSpec,
                                      boolean raised,
                                      boolean hollow) {
        double[] radii;
        try {
            radii = parseSphereRadii(radiiSpec);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("[WE] " + e.getMessage()));
            return;
        }

        BlockPos center = raised
                ? anchor.offset(0, (int) Math.floor(radii[1]), 0)
                : anchor;
        WorldEditRegionMask region = WorldEditRegionMask.sphere(center, radii[0], radii[1], radii[2]);
        runTrackedEdit(player, hollow ? "//hsphere" : "//sphere", List.of(region.bounds()), (changes, task) -> {
            ServerLevel world = player.level();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, task, region, hollow ? WorldEditRegionSurface.SHELL : WorldEditRegionSurface.SOLID, target);
        });
    }

    private static void applyRegion(ServerLevel world,
                                    LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes,
                                    WorldEditTask task,
                                    WorldEditRegionMask region,
                                    WorldEditRegionSurface surface,
                                    WorldEditBlockTarget target) {
        task.add(BlockPos.betweenClosed(region.bounds().min(), region.bounds().max()), pos -> {
            if (region.matchesSurface(pos, surface)) {
                applyTarget(world, changes, pos, target);
            }
        });
    }

    private static void runTrackedEdit(ServerPlayer player,
                                       String label,
                                       List<WorldEditSelection> loadedSelections,
                                       EditAction action) {
        ServerLevel world = player.level();
        WorldEditLimits.validate(loadedSelections, world.getMinY(), world.getMaxY());
        if (!areChunksLoaded(world, loadedSelections)) {
            player.sendSystemMessage(Component.literal("[WE] Load the affected chunks first."));
            return;
        }

        TrackedChanges changes = new TrackedChanges();
        WorldEditTask task = new WorldEditTask(
                () -> WorldEditTask.work(new ArrayList<>(changes.values()).reversed(), change -> {
                    if (!change.before().apply(world, change.pos()) && !change.before().matches(world, change.pos())) {
                        throw new IllegalStateException("Failed to roll back " + change.pos().toShortString());
                    }
                }),
                () -> {
                    WorldEditServerHistory.pushUndo(player, label, world.dimension(), new ArrayList<>(changes.values()));
                    player.sendSystemMessage(Component.literal(changes.isEmpty() ? "[WE] Nothing changed."
                            : "[WE] " + label + " changed " + changes.size() + " block(s)."));
                },
                error -> reportFailure(player, label, error));
        try {
            action.run(changes, task);
        } catch (CommandSyntaxException e) {
            player.sendSystemMessage(Component.literal("[WE] " + e.getMessage()));
            return;
        }
        WorldEditTasks.submit(player, task);
        player.sendSystemMessage(Component.literal("[WE] Started " + label + "."));
    }

    private static void applyTarget(ServerLevel world,
                                    LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes,
                                    BlockPos pos,
                                    WorldEditBlockTarget target) {
        BlockPos immutablePos = pos.immutable();
        if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            throw new IllegalStateException("Affected chunk unloaded; edit cancelled.");
        }
        WorldEditServerHistory.WorldEditServerChange existing = changes.get(immutablePos);
        WorldEditBlockSnapshot currentBefore = WorldEditBlockSnapshot.capture(world, immutablePos);
        WorldEditBlockSnapshot originalBefore = existing == null ? currentBefore : existing.before();
        // Record the original before changing anything, including a partially failing block placement.
        if (existing == null) {
            changes.put(immutablePos, new WorldEditServerHistory.WorldEditServerChange(immutablePos, originalBefore, currentBefore));
        }
        boolean applied = target.apply(world, immutablePos);
        WorldEditBlockSnapshot after = WorldEditBlockSnapshot.capture(world, immutablePos);

        if (!applied && currentBefore.equals(after)) {
            if (existing == null) changes.remove(immutablePos);
            return;
        }
        if (!applied) {
            throw new IllegalStateException("Failed to edit " + immutablePos.toShortString());
        }

        if (originalBefore.equals(after)) {
            changes.remove(immutablePos);
            return;
        }

        changes.put(immutablePos, new WorldEditServerHistory.WorldEditServerChange(immutablePos, originalBefore, after));
    }

    static void reportFailure(ServerPlayer player, String label, RuntimeException error) {
        String rollback = error.getSuppressed().length == 0 ? "" : " Rollback was incomplete; see the game log.";
        player.sendSystemMessage(Component.literal("[WE] " + label + " failed: " + error.getMessage() + rollback));
        M0DevTools.LOGGER.warn("WorldEdit {} failed", label, error);
    }

    private static boolean areChunksLoaded(ServerLevel world, List<WorldEditSelection> selections) {
        HashSet<Long> seen = new HashSet<>();
        for (WorldEditSelection selection : selections) {
            int minChunkX = selection.min().getX() >> 4;
            int maxChunkX = selection.max().getX() >> 4;
            int minChunkZ = selection.min().getZ() >> 4;
            int maxChunkZ = selection.max().getZ() >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
                    if (seen.add(chunkKey) && !world.hasChunk(chunkX, chunkZ)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static List<WorldEditSelection> touchedStackSelections(WorldEditRegionMask region, int count, Direction direction) {
        int stepX = direction.getStepX() * region.bounds().sizeX();
        int stepY = direction.getStepY() * region.bounds().sizeY();
        int stepZ = direction.getStepZ() * region.bounds().sizeZ();
        WorldEditSelection source = region.bounds();
        WorldEditSelection last = source.shift(Math.multiplyExact(stepX, count), Math.multiplyExact(stepY, count), Math.multiplyExact(stepZ, count));
        return List.of(new WorldEditSelection(
                new BlockPos(Math.min(source.min().getX(), last.min().getX()), Math.min(source.min().getY(), last.min().getY()), Math.min(source.min().getZ(), last.min().getZ())),
                new BlockPos(Math.max(source.max().getX(), last.max().getX()), Math.max(source.max().getY(), last.max().getY()), Math.max(source.max().getZ(), last.max().getZ()))));
    }

    private static List<PositionedSnapshot> captureRegionSnapshots(ServerLevel world, WorldEditRegionMask region, WorldEditTask task) {
        List<PositionedSnapshot> out = new ArrayList<>();
        long[] bytes = {0};
        task.add(BlockPos.betweenClosed(region.bounds().min(), region.bounds().max()), pos -> {
            if (region.contains(pos)) {
                if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    throw new IllegalStateException("Source chunk unloaded; edit cancelled.");
                }
                WorldEditBlockSnapshot snapshot = WorldEditBlockSnapshot.capture(world, pos);
                bytes[0] += snapshot.estimatedBytes();
                if (bytes[0] > WorldEditLimits.MAX_SNAPSHOT_BYTES) {
                    throw new IllegalStateException("Source data exceeds the 16 MiB snapshot limit.");
                }
                out.add(new PositionedSnapshot(pos.immutable(), snapshot));
            }
        });
        return out;
    }

    private static WorldEditRegionMask regionFromPayload(WorldEditSyncPayloads.EditRequestPayload payload) {
        return WorldEditRegionMask.fromSelection(
                new WorldEditSelection(payload.min(), payload.max()),
                WorldEditRegionShape.fromName(payload.selectionShape())
        );
    }

    private static double[] parseCylinderRadii(String input) {
        String[] parts = tokenizeCsv(input);
        return switch (parts.length) {
            case 1 -> {
                double radius = parseRadius(parts[0], true, "Cylinder radius");
                yield new double[]{radius, radius};
            }
            case 2 -> new double[]{
                    parseRadius(parts[0], true, "Cylinder radius X"),
                    parseRadius(parts[1], true, "Cylinder radius Z")
            };
            default -> throw new IllegalArgumentException("Use //cyl <block> <radius|radiusX,radiusZ> [height].");
        };
    }

    private static double[] parseSphereRadii(String input) {
        String[] parts = tokenizeCsv(input);
        return switch (parts.length) {
            case 1 -> {
                double radius = parseRadius(parts[0], false, "Sphere radius");
                yield new double[]{radius, radius, radius};
            }
            case 3 -> new double[]{
                    parseRadius(parts[0], false, "Sphere radius X"),
                    parseRadius(parts[1], false, "Sphere radius Y"),
                    parseRadius(parts[2], false, "Sphere radius Z")
            };
            default -> throw new IllegalArgumentException("Use //sphere <block> <radius|radiusX,radiusY,radiusZ> [-r].");
        };
    }

    private static String[] tokenizeCsv(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s*,\\s*");
    }

    private static double parseRadius(String token, boolean requirePositive, String label) {
        try {
            double radius = Double.parseDouble(token);
            if (!Double.isFinite(radius) || radius > WorldEditLimits.MAX_VOLUME) {
                throw new IllegalArgumentException(label + " must be finite and no greater than " + WorldEditLimits.MAX_VOLUME + ".");
            }
            if (requirePositive && radius <= 0.0) {
                throw new IllegalArgumentException(label + " must be > 0.");
            }
            if (!requirePositive && radius < 0.0) {
                throw new IllegalArgumentException(label + " must be >= 0.");
            }
            return radius;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be numeric.");
        }
    }

    @FunctionalInterface
    private interface EditAction {
        void run(TrackedChanges changes, WorldEditTask task) throws CommandSyntaxException;
    }

    private static final class TrackedChanges extends LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> {
        private long bytes;

        @Override
        public WorldEditServerHistory.WorldEditServerChange put(BlockPos pos, WorldEditServerHistory.WorldEditServerChange change) {
            var old = get(pos);
            long next = bytes + change.estimatedBytes() - (old == null ? 0 : old.estimatedBytes());
            if (next > WorldEditLimits.MAX_SNAPSHOT_BYTES) {
                throw new IllegalStateException("Edit exceeds the 16 MiB undo snapshot limit.");
            }
            bytes = next;
            return super.put(pos, change);
        }

        @Override
        public WorldEditServerHistory.WorldEditServerChange remove(Object pos) {
            var old = super.remove(pos);
            if (old != null) bytes -= old.estimatedBytes();
            return old;
        }
    }

    private record PositionedSnapshot(BlockPos pos, WorldEditBlockSnapshot snapshot) {
    }
}
