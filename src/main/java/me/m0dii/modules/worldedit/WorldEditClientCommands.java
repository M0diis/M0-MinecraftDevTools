package me.m0dii.modules.worldedit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.debugdraw.DebugDrawManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class WorldEditClientCommands {
    private WorldEditClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("/set")
                    .executes(WorldEditClientCommands::showSetUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSet(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/replace")
                    .executes(WorldEditClientCommands::showReplaceUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestReplaceArgs)
                            .executes(ctx -> handleReplace(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/stack")
                    .executes(WorldEditClientCommands::showStackUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestStackArgs)
                            .executes(ctx -> handleStack(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/move")
                    .executes(WorldEditClientCommands::showMoveUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestMoveArgs)
                            .executes(ctx -> handleMove(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/undo")
                    .executes(WorldEditClientCommands::handleUndo));

            dispatcher.register(ClientCommandManager.literal("/redo")
                    .executes(WorldEditClientCommands::handleRedo));

            dispatcher.register(ClientCommandManager.literal("/pos1")
                    .executes(ctx -> handlePos(ctx, false, "look"))
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestPosArgs)
                            .executes(ctx -> handlePos(ctx, false, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/pos2")
                    .executes(ctx -> handlePos(ctx, true, "look"))
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestPosArgs)
                            .executes(ctx -> handlePos(ctx, true, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/desel")
                    .executes(WorldEditClientCommands::clearSelection));

            dispatcher.register(ClientCommandManager.literal("/size")
                    .executes(WorldEditClientCommands::showSelectionSize));

            dispatcher.register(ClientCommandManager.literal("/sel")
                    .executes(WorldEditClientCommands::showSelectionSize));
        });
    }

    private static int showSetUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //set <block>"));
        return 1;
    }

    private static int showReplaceUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //replace <from> <to>"));
        return 1;
    }

    private static int showStackUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //stack <count> [direction] [-a]"));
        context.getSource().sendFeedback(Text.literal("[WE] Directions: north south east west up down forward back left right"));
        return 1;
    }

    private static int showMoveUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //move <distance> [direction] [-a]"));
        context.getSource().sendFeedback(Text.literal("[WE] Directions: north south east west up down forward back left right"));
        return 1;
    }

    private static int handleSet(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String block = rawArgs == null ? "" : rawArgs.trim();
        if (block.isEmpty()) {
            return showSetUsage(context);
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return submitPlan(context, "//set", List.of(selection), selection, WorldEditCommandPlanner.planSet(selection, block));
    }

    private static int handleReplace(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String args = rawArgs == null ? "" : rawArgs.trim();
        if (args.isEmpty()) {
            return showReplaceUsage(context);
        }

        int firstWhitespace = firstWhitespace(args);
        if (firstWhitespace < 0) {
            return showReplaceUsage(context);
        }

        String from = args.substring(0, firstWhitespace).trim();
        String to = args.substring(firstWhitespace).trim();
        if (from.isEmpty() || to.isEmpty()) {
            return showReplaceUsage(context);
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return submitPlan(context, "//replace", List.of(selection), selection, WorldEditCommandPlanner.planReplace(selection, from, to));
    }

    private static int handleStack(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showStackUsage(context);
        }

        int count;
        try {
            count = Integer.parseInt(tokens[0]);
        } catch (NumberFormatException e) {
            return showStackUsage(context);
        }
        if (count < 1) {
            context.getSource().sendFeedback(Text.literal("[WE] Count must be >= 1."));
            return 0;
        }

        MinecraftClient client = context.getSource().getClient();
        int optionStart = 1;
        String directionToken = "forward";
        if (tokens.length >= 2 && !isMaskedFlag(tokens[1])) {
            directionToken = tokens[1];
            optionStart = 2;
        }
        Direction direction = resolveDirection(client.player, directionToken);
        if (direction == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Unknown direction."));
            return 0;
        }

        boolean masked = hasMaskedFlag(tokens, optionStart);
        String invalidStackOption = firstNonMaskedFlag(tokens, optionStart);
        if (invalidStackOption != null) {
            context.getSource().sendFeedback(Text.literal("[WE] Unknown stack option: " + invalidStackOption));
            return 0;
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return submitPlan(
                context,
                "//stack",
                touchedStackSelections(selection, count, direction),
                selection,
                WorldEditCommandPlanner.planStack(selection, count, direction, masked)
        );
    }

    private static int handleMove(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showMoveUsage(context);
        }

        int distance;
        try {
            distance = Integer.parseInt(tokens[0]);
        } catch (NumberFormatException e) {
            return showMoveUsage(context);
        }
        if (distance == 0) {
            context.getSource().sendFeedback(Text.literal("[WE] Distance must not be 0."));
            return 0;
        }

        MinecraftClient client = context.getSource().getClient();
        int optionStart = 1;
        String directionToken = "forward";
        if (tokens.length >= 2 && !isMaskedFlag(tokens[1])) {
            directionToken = tokens[1];
            optionStart = 2;
        }
        Direction direction = resolveDirection(client.player, directionToken);
        if (direction == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Unknown direction."));
            return 0;
        }

        boolean masked = hasMaskedFlag(tokens, optionStart);
        String invalidMoveOption = firstNonMaskedFlag(tokens, optionStart);
        if (invalidMoveOption != null) {
            context.getSource().sendFeedback(Text.literal("[WE] Unknown move option: " + invalidMoveOption));
            return 0;
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return submitPlan(
                context,
                "//move",
                touchedMoveSelections(selection, distance, direction),
                selection,
                WorldEditCommandPlanner.planMove(selection, distance, direction, masked)
        );
    }

    private static int handleUndo(CommandContext<FabricClientCommandSource> context) {
        if (WorldEditCommandQueue.isBusy()) {
            context.getSource().sendFeedback(Text.literal("[WE] Another edit batch is still running."));
            return 0;
        }
        if (!WorldEditHistoryManager.canUndo()) {
            context.getSource().sendFeedback(Text.literal("[WE] Nothing to undo."));
            return 0;
        }
        if (!WorldEditHistoryManager.queueUndo()) {
            context.getSource().sendFeedback(Text.literal("[WE] Failed to queue undo."));
            return 0;
        }
        return 1;
    }

    private static int handleRedo(CommandContext<FabricClientCommandSource> context) {
        if (WorldEditCommandQueue.isBusy()) {
            context.getSource().sendFeedback(Text.literal("[WE] Another edit batch is still running."));
            return 0;
        }
        if (!WorldEditHistoryManager.canRedo()) {
            context.getSource().sendFeedback(Text.literal("[WE] Nothing to redo."));
            return 0;
        }
        if (!WorldEditHistoryManager.queueRedo()) {
            context.getSource().sendFeedback(Text.literal("[WE] Failed to queue redo."));
            return 0;
        }
        return 1;
    }

    private static int handlePos(CommandContext<FabricClientCommandSource> context, boolean pos2, String rawArgs) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        String[] tokens = tokenize(rawArgs);
        if (tokens.length == 0 || "look".equalsIgnoreCase(tokens[0])) {
            boolean ok = DebugDrawManager.pickSelectionPos(pos2);
            context.getSource().sendFeedback(Text.literal(ok
                    ? "[WE] Set " + (pos2 ? "pos2" : "pos1") + " from crosshair."
                    : "[WE] No block under crosshair."));
            return ok ? 1 : 0;
        }

        if ("here".equalsIgnoreCase(tokens[0])) {
            BlockPos pos = client.player.getBlockPos();
            DebugDrawManager.setSelectionPos(pos2, pos);
            context.getSource().sendFeedback(Text.literal("[WE] Set " + (pos2 ? "pos2" : "pos1") + " to " + pos.toShortString() + "."));
            return 1;
        }

        if (tokens.length < 3) {
            context.getSource().sendFeedback(Text.literal("[WE] Usage: //" + (pos2 ? "pos2" : "pos1") + " [look|here|<x> <y> <z>]"));
            return 0;
        }

        try {
            BlockPos pos = new BlockPos(
                    (int) Math.floor(parseCoord(tokens[0], client.player.getX())),
                    (int) Math.floor(parseCoord(tokens[1], client.player.getY())),
                    (int) Math.floor(parseCoord(tokens[2], client.player.getZ()))
            );
            DebugDrawManager.setSelectionPos(pos2, pos);
            context.getSource().sendFeedback(Text.literal("[WE] Set " + (pos2 ? "pos2" : "pos1") + " to " + pos.toShortString() + "."));
            return 1;
        } catch (RuntimeException e) {
            context.getSource().sendFeedback(Text.literal("[WE] Invalid coordinates: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearSelection(CommandContext<FabricClientCommandSource> context) {
        DebugDrawManager.clearSelectionPositions();
        context.getSource().sendFeedback(Text.literal("[WE] Selection cleared."));
        return 1;
    }

    private static int showSelectionSize(CommandContext<FabricClientCommandSource> context) {
        WorldEditSelection selection = WorldEditSelection.fromDebugDrawSelection();
        if (selection == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Selection: " + DebugDrawManager.selectionStatus()));
            return 1;
        }

        context.getSource().sendFeedback(Text.literal("[WE] Selection: " + DebugDrawManager.selectionStatus()));
        context.getSource().sendFeedback(Text.literal("[WE] Size: " + selection.sizeX() + " x " + selection.sizeY() + " x " + selection.sizeZ()
                + " (" + selection.volume() + " blocks)"));
        return 1;
    }

    private static WorldEditSelection requireSelection(CommandContext<FabricClientCommandSource> context) {
        WorldEditSelection selection = WorldEditSelection.fromDebugDrawSelection();
        if (selection == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Set pos1 and pos2 first with //pos1 and //pos2 or /draw select."));
            return null;
        }

        if (DebugDrawManager.getSelectionShape() != DebugDrawManager.SelectionShape.BOX) {
            context.getSource().sendFeedback(Text.literal("[WE] Using cuboid bounds from the current /draw selection. Non-cuboid edit regions are not implemented yet."));
        }

        return selection;
    }

    private static int submitPlan(CommandContext<FabricClientCommandSource> context,
                                  String label,
                                  List<WorldEditSelection> touchedSelections,
                                  WorldEditSelection selection,
                                  List<String> commands) {
        if (commands.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("[WE] Nothing to do."));
            return 0;
        }
        if (WorldEditCommandQueue.isBusy()) {
            context.getSource().sendFeedback(Text.literal("[WE] Another edit batch is still running."));
            return 0;
        }

        List<String> trackedCommands = WorldEditHistoryManager.prepareTrackedOperation(label, touchedSelections, commands);
        if (trackedCommands == null) {
            return 0;
        }
        if (!WorldEditCommandQueue.submit(label, trackedCommands)) {
            WorldEditCommandQueue.setPendingCompletion(null);
            context.getSource().sendFeedback(Text.literal("[WE] Could not queue commands right now."));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("[WE] " + label + " selection "
                + selection.sizeX() + "x" + selection.sizeY() + "x" + selection.sizeZ()
                + " (" + selection.volume() + " blocks), " + trackedCommands.size() + " batched command(s)."));
        return 1;
    }

    private static List<WorldEditSelection> touchedStackSelections(WorldEditSelection selection, int count, Direction direction) {
        List<WorldEditSelection> touchedSelections = new ArrayList<>(count);
        int stepX = direction.getOffsetX() * selection.sizeX();
        int stepY = direction.getOffsetY() * selection.sizeY();
        int stepZ = direction.getOffsetZ() * selection.sizeZ();
        for (int copyIndex = 1; copyIndex <= count; copyIndex++) {
            touchedSelections.add(selection.shift(stepX * copyIndex, stepY * copyIndex, stepZ * copyIndex));
        }
        return touchedSelections;
    }

    private static List<WorldEditSelection> touchedMoveSelections(WorldEditSelection selection, int distance, Direction direction) {
        return List.of(
                selection,
                selection.shift(
                        direction.getOffsetX() * distance,
                        direction.getOffsetY() * distance,
                        direction.getOffsetZ() * distance
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestSetArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return suggestBlocks(builder, currentTokenPrefix(builder));
    }

    private static CompletableFuture<Suggestions> suggestReplaceArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        if (currentTokenIndex(builder) <= 1) {
            return suggestBlocks(builder, currentTokenPrefix(builder));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestStackArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, List.of("1", "2", "3", "4", "8"));
        } else if (tokenIndex == 1) {
            return suggestToken(builder, prefix, directionSuggestionsWithMask());
        } else {
            return suggestToken(builder, prefix, List.of("-a"));
        }
    }

    private static CompletableFuture<Suggestions> suggestMoveArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, List.of("1", "2", "3", "4", "8"));
        } else if (tokenIndex == 1) {
            return suggestToken(builder, prefix, directionSuggestionsWithMask());
        } else {
            return suggestToken(builder, prefix, List.of("-a"));
        }
    }

    private static CompletableFuture<Suggestions> suggestPosArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, List.of("look", "here", "~"));
        } else if (tokenIndex <= 2) {
            return suggestToken(builder, prefix, List.of("~"));
        }
        return builder.buildFuture();
    }

    private static double parseCoord(String token, double base) {
        if (token.startsWith("~")) {
            if (token.length() == 1) {
                return base;
            }
            return base + Double.parseDouble(token.substring(1));
        }
        return Double.parseDouble(token);
    }

    private static Direction resolveDirection(ClientPlayerEntity player, String token) {
        if (player == null || token == null || token.isBlank()) {
            return null;
        }

        String value = token.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "north", "n" -> Direction.NORTH;
            case "south", "s" -> Direction.SOUTH;
            case "east", "e" -> Direction.EAST;
            case "west", "w" -> Direction.WEST;
            case "up", "u" -> Direction.UP;
            case "down", "d" -> Direction.DOWN;
            case "forward", "f" -> player.getHorizontalFacing();
            case "back", "backward", "b" -> player.getHorizontalFacing().getOpposite();
            case "left", "l" -> rotateLeft(player.getHorizontalFacing());
            case "right", "r" -> rotateRight(player.getHorizontalFacing());
            default -> null;
        };
    }

    private static Direction rotateLeft(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    private static Direction rotateRight(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            default -> Direction.SOUTH;
        };
    }

    private static String[] tokenize(String rawArgs) {
        String trimmed = rawArgs == null ? "" : rawArgs.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static int currentTokenIndex(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String trimmed = remaining.trim();
        boolean trailingSpace = remaining.endsWith(" ");
        if (trimmed.isEmpty()) {
            return 0;
        }
        String[] tokens = trimmed.split("\\s+");
        return trailingSpace ? tokens.length : tokens.length - 1;
    }

    private static String currentTokenPrefix(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.isEmpty() || remaining.endsWith(" ")) {
            return "";
        }
        int lastSpace = remaining.lastIndexOf(' ');
        return (lastSpace >= 0 ? remaining.substring(lastSpace + 1) : remaining).toLowerCase(Locale.ROOT);
    }

    private static SuggestionsBuilder tokenBuilder(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        int offset = lastSpace < 0 ? builder.getStart() : builder.getStart() + lastSpace + 1;
        return builder.createOffset(offset);
    }

    private static CompletableFuture<Suggestions> suggestToken(SuggestionsBuilder builder, String prefix, List<String> candidates) {
        SuggestionsBuilder tokenBuilder = tokenBuilder(builder);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                tokenBuilder.suggest(candidate);
            }
        }
        return tokenBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBlocks(SuggestionsBuilder builder, String prefix) {
        SuggestionsBuilder tokenBuilder = tokenBuilder(builder);
        List<String> candidates = new ArrayList<>();
        for (Identifier id : Registries.BLOCK.getIds()) {
            String full = id.toString();
            String path = id.getPath();
            String normalizedFull = full.toLowerCase(Locale.ROOT);
            String normalizedPath = path.toLowerCase(Locale.ROOT);
            if (prefix.contains(":")) {
                if (normalizedFull.startsWith(prefix)) {
                    candidates.add(full);
                }
            } else if (normalizedPath.startsWith(prefix)) {
                candidates.add(path);
            } else if (normalizedFull.startsWith(prefix)) {
                candidates.add(full);
            }
        }
        candidates.stream().sorted().limit(50).forEach(tokenBuilder::suggest);
        return tokenBuilder.buildFuture();
    }

    private static int firstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasMaskedFlag(String[] tokens, int startIndex) {
        if (tokens.length <= startIndex) {
            return false;
        }
        return firstNonMaskedFlag(tokens, startIndex) == null;
    }

    private static String firstNonMaskedFlag(String[] tokens, int startIndex) {
        for (int i = startIndex; i < tokens.length; i++) {
            if (!isMaskedFlag(tokens[i])) {
                return tokens[i];
            }
        }
        return null;
    }

    private static boolean isMaskedFlag(String token) {
        if (token == null) {
            return false;
        }
        String value = token.trim().toLowerCase(Locale.ROOT);
        return "-a".equals(value) || "masked".equals(value) || "ignoreair".equals(value);
    }

    private static List<String> directionSuggestionsWithMask() {
        return List.of("north", "south", "east", "west", "up", "down", "forward", "back", "left", "right", "-a");
    }
}
