package me.m0dii.modules.worldedit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public final class WorldEditSyncServer {
    private static boolean registered = false;

    private WorldEditSyncServer() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ServerPlayNetworking.registerGlobalReceiver(WorldEditSyncPayloads.EditRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> handleRequest(context.player(), payload)));
    }

    private static void handleRequest(ServerPlayerEntity player, WorldEditSyncPayloads.EditRequestPayload payload) {
        if (player == null) {
            return;
        }
        if (!CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK).test(player.getCommandSource())) {
            player.sendMessage(Text.literal("[WE] You need permission level 2 to use mod WorldEdit."), false);
            return;
        }

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
    }

    private static void executeSurfaceFill(ServerPlayerEntity player,
                                           String label,
                                           WorldEditRegionMask region,
                                           String rawBlock,
                                           WorldEditRegionSurface surface) {
        runTrackedEdit(player, label, List.of(region.bounds()), changes -> {
            ServerWorld world = player.getEntityWorld();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, region, surface, target);
        });
    }

    private static void executeReplace(ServerPlayerEntity player,
                                       WorldEditRegionMask region,
                                       String rawFrom,
                                       String rawTo) {
        runTrackedEdit(player, "//replace", List.of(region.bounds()), changes -> {
            ServerWorld world = player.getEntityWorld();
            WorldEditParsedBlock from = WorldEditParsedBlock.parse(world, rawFrom);
            WorldEditParsedBlock to = WorldEditParsedBlock.parse(world, rawTo);
            region.forEach(pos -> {
                if (from.matches(world, pos)) {
                    applyTarget(world, changes, pos, to);
                }
            });
        });
    }

    private static void executeStack(ServerPlayerEntity player,
                                     WorldEditRegionMask region,
                                     int count,
                                     String directionName,
                                     boolean masked) {
        Direction direction = Direction.byId(directionName);
        if (direction == null || count < 1) {
            player.sendMessage(Text.literal("[WE] Invalid stack request."), false);
            return;
        }

        runTrackedEdit(player, "//stack",
                touchedStackSelections(region, count, direction),
                changes -> {
                    ServerWorld world = player.getEntityWorld();
                    List<PositionedSnapshot> source = captureRegionSnapshots(world, region);
                    int stepX = direction.getOffsetX() * region.bounds().sizeX();
                    int stepY = direction.getOffsetY() * region.bounds().sizeY();
                    int stepZ = direction.getOffsetZ() * region.bounds().sizeZ();

                    for (int copyIndex = 1; copyIndex <= count; copyIndex++) {
                        int dx = stepX * copyIndex;
                        int dy = stepY * copyIndex;
                        int dz = stepZ * copyIndex;
                        for (PositionedSnapshot block : source) {
                            if (masked && block.snapshot.isAir()) {
                                continue;
                            }
                            applyTarget(world, changes, block.pos.add(dx, dy, dz), block.snapshot);
                        }
                    }
                });
    }

    private static void executeMove(ServerPlayerEntity player,
                                    WorldEditRegionMask region,
                                    int distance,
                                    String directionName,
                                    boolean masked) {
        Direction direction = Direction.byId(directionName);
        if (direction == null || distance == 0) {
            player.sendMessage(Text.literal("[WE] Invalid move request."), false);
            return;
        }

        WorldEditRegionMask destination = region.shifted(direction, distance);
        runTrackedEdit(player, "//move",
                List.of(region.bounds(), destination.bounds()),
                changes -> {
                    ServerWorld world = player.getEntityWorld();
                    List<PositionedSnapshot> source = captureRegionSnapshots(world, region);
                    int dx = direction.getOffsetX() * distance;
                    int dy = direction.getOffsetY() * distance;
                    int dz = direction.getOffsetZ() * distance;

                    for (PositionedSnapshot block : source) {
                        if (masked && block.snapshot.isAir()) {
                            continue;
                        }
                        applyTarget(world, changes, block.pos.add(dx, dy, dz), block.snapshot);
                    }

                    region.forEach(pos -> {
                        if (!destination.contains(pos)) {
                            applyTarget(world, changes, pos, WorldEditBlockSnapshot.air());
                        }
                    });
                });
    }

    private static void executeCylinder(ServerPlayerEntity player,
                                        BlockPos anchor,
                                        String rawBlock,
                                        String radiiSpec,
                                        int height,
                                        boolean hollow) {
        if (height < 1) {
            player.sendMessage(Text.literal("[WE] Cylinder height must be >= 1."), false);
            return;
        }

        double[] radii;
        try {
            radii = parseCylinderRadii(radiiSpec);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("[WE] " + e.getMessage()), false);
            return;
        }

        WorldEditRegionMask region = WorldEditRegionMask.cylinder(anchor, radii[0], radii[1], height);
        runTrackedEdit(player, hollow ? "//hcyl" : "//cyl", List.of(region.bounds()), changes -> {
            ServerWorld world = player.getEntityWorld();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, region, hollow ? WorldEditRegionSurface.SHELL : WorldEditRegionSurface.SOLID, target);
        });
    }

    private static void executeSphere(ServerPlayerEntity player,
                                      BlockPos anchor,
                                      String rawBlock,
                                      String radiiSpec,
                                      boolean raised,
                                      boolean hollow) {
        double[] radii;
        try {
            radii = parseSphereRadii(radiiSpec);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("[WE] " + e.getMessage()), false);
            return;
        }

        BlockPos center = raised
                ? anchor.add(0, (int) Math.floor(radii[1]), 0)
                : anchor;
        WorldEditRegionMask region = WorldEditRegionMask.sphere(center, radii[0], radii[1], radii[2]);
        runTrackedEdit(player, hollow ? "//hsphere" : "//sphere", List.of(region.bounds()), changes -> {
            ServerWorld world = player.getEntityWorld();
            WorldEditParsedBlock target = WorldEditParsedBlock.parse(world, rawBlock);
            applyRegion(world, changes, region, hollow ? WorldEditRegionSurface.SHELL : WorldEditRegionSurface.SOLID, target);
        });
    }

    private static void applyRegion(ServerWorld world,
                                    LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes,
                                    WorldEditRegionMask region,
                                    WorldEditRegionSurface surface,
                                    WorldEditBlockTarget target) {
        region.forEach(pos -> {
            if (region.matchesSurface(pos, surface)) {
                applyTarget(world, changes, pos, target);
            }
        });
    }

    private static void runTrackedEdit(ServerPlayerEntity player,
                                       String label,
                                       List<WorldEditSelection> loadedSelections,
                                       EditAction action) {
        ServerWorld world = player.getEntityWorld();
        if (!areChunksLoaded(world, loadedSelections)) {
            player.sendMessage(Text.literal("[WE] Load the affected chunks first."), false);
            return;
        }

        LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes = new LinkedHashMap<>();
        try {
            action.run(changes);
        } catch (CommandSyntaxException e) {
            rollback(world, changes);
            player.sendMessage(Text.literal("[WE] " + e.getMessage()), false);
            return;
        } catch (RuntimeException e) {
            rollback(world, changes);
            player.sendMessage(Text.literal("[WE] " + label + " failed: " + e.getMessage()), false);
            return;
        }

        if (changes.isEmpty()) {
            player.sendMessage(Text.literal("[WE] Nothing changed."), false);
            return;
        }

        List<WorldEditServerHistory.WorldEditServerChange> changeList = new ArrayList<>(changes.values());
        WorldEditServerHistory.pushUndo(player, label, world.getRegistryKey(), changeList);
        player.sendMessage(Text.literal("[WE] " + label + " changed " + changeList.size() + " block(s)."), false);
    }

    private static void applyTarget(ServerWorld world,
                                    LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes,
                                    BlockPos pos,
                                    WorldEditBlockTarget target) {
        BlockPos immutablePos = pos.toImmutable();
        WorldEditServerHistory.WorldEditServerChange existing = changes.get(immutablePos);
        WorldEditBlockSnapshot originalBefore = existing == null ? WorldEditBlockSnapshot.capture(world, immutablePos) : existing.before();
        WorldEditBlockSnapshot currentBefore = WorldEditBlockSnapshot.capture(world, immutablePos);
        boolean applied = target.apply(world, immutablePos);
        WorldEditBlockSnapshot after = WorldEditBlockSnapshot.capture(world, immutablePos);

        if (!applied && currentBefore.equals(after)) {
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

    private static void rollback(ServerWorld world, LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes) {
        List<WorldEditServerHistory.WorldEditServerChange> applied = new ArrayList<>(changes.values());
        for (int i = applied.size() - 1; i >= 0; i--) {
            WorldEditServerHistory.WorldEditServerChange change = applied.get(i);
            change.before().apply(world, change.pos());
        }
    }

    private static boolean areChunksLoaded(ServerWorld world, List<WorldEditSelection> selections) {
        HashSet<Long> seen = new HashSet<>();
        for (WorldEditSelection selection : selections) {
            int minChunkX = selection.min().getX() >> 4;
            int maxChunkX = selection.max().getX() >> 4;
            int minChunkZ = selection.min().getZ() >> 4;
            int maxChunkZ = selection.max().getZ() >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
                    if (seen.add(chunkKey) && !world.isChunkLoaded(chunkX, chunkZ)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static List<WorldEditSelection> touchedStackSelections(WorldEditRegionMask region, int count, Direction direction) {
        List<WorldEditSelection> touchedSelections = new ArrayList<>(count + 1);
        touchedSelections.add(region.bounds());
        int stepX = direction.getOffsetX() * region.bounds().sizeX();
        int stepY = direction.getOffsetY() * region.bounds().sizeY();
        int stepZ = direction.getOffsetZ() * region.bounds().sizeZ();
        for (int copyIndex = 1; copyIndex <= count; copyIndex++) {
            touchedSelections.add(region.shifted(stepX * copyIndex, stepY * copyIndex, stepZ * copyIndex).bounds());
        }
        return touchedSelections;
    }

    private static List<PositionedSnapshot> captureRegionSnapshots(ServerWorld world, WorldEditRegionMask region) {
        List<PositionedSnapshot> out = new ArrayList<>();
        region.forEach(pos -> out.add(new PositionedSnapshot(pos.toImmutable(), WorldEditBlockSnapshot.capture(world, pos))));
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
        void run(LinkedHashMap<BlockPos, WorldEditServerHistory.WorldEditServerChange> changes) throws CommandSyntaxException;
    }

    private record PositionedSnapshot(BlockPos pos, WorldEditBlockSnapshot snapshot) {
    }
}
