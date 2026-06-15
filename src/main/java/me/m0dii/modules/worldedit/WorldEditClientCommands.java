package me.m0dii.modules.worldedit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import me.m0dii.modules.debugdraw.DebugDrawManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class WorldEditClientCommands {
    private static final List<List<String>> WORLD_EDIT_SERVER_SIGNATURES = List.of(
            List.of("/set", "set"),
            List.of("/replace", "replace"),
            List.of("/pos1", "pos1"),
            List.of("/pos2", "pos2"),
            List.of("/wand", "wand")
    );
    private static final List<String> WORLD_EDIT_SERVER_ROOT_ALIASES = List.of("worldedit", "we");

    private WorldEditClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("/expand")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showExpandUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestExpandArgs)
                            .executes(ctx -> handleExpand(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/contract")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showContractUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestContractArgs)
                            .executes(ctx -> handleContract(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/shift")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showShiftUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestShiftArgs)
                            .executes(ctx -> handleShift(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/outset")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showOutsetUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestOutsetArgs)
                            .executes(ctx -> handleOutset(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/inset")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showInsetUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestInsetArgs)
                            .executes(ctx -> handleInset(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/set")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showSetUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSet(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/replace")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showReplaceUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestReplaceArgs)
                            .executes(ctx -> handleReplace(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/walls")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showWallsUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//walls", WorldEditSyncPayloads.Operation.WALLS))));

            dispatcher.register(ClientCommandManager.literal("/floor")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showFloorUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//floor", WorldEditSyncPayloads.Operation.FLOOR))));

            dispatcher.register(ClientCommandManager.literal("/roof")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showRoofUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//roof", WorldEditSyncPayloads.Operation.ROOF))));

            dispatcher.register(ClientCommandManager.literal("/enclose")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showEncloseUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//enclose", WorldEditSyncPayloads.Operation.ENCLOSE))));

            dispatcher.register(ClientCommandManager.literal("/faces")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showEncloseUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//faces", WorldEditSyncPayloads.Operation.ENCLOSE))));

            dispatcher.register(ClientCommandManager.literal("/outline")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showEncloseUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSetArgs)
                            .executes(ctx -> handleSurfaceFill(ctx, StringArgumentType.getString(ctx, "args"),
                                    "//outline", WorldEditSyncPayloads.Operation.ENCLOSE))));

            dispatcher.register(ClientCommandManager.literal("/stack")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showStackUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestStackArgs)
                            .executes(ctx -> handleStack(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/move")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showMoveUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestMoveArgs)
                            .executes(ctx -> handleMove(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/undo")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::handleUndo));

            dispatcher.register(ClientCommandManager.literal("/redo")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::handleRedo));

            dispatcher.register(ClientCommandManager.literal("/cyl")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showCylUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestCylArgs)
                            .executes(ctx -> handleCylinder(ctx, StringArgumentType.getString(ctx, "args"), false))));

            dispatcher.register(ClientCommandManager.literal("/hcyl")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showHCylUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestCylArgs)
                            .executes(ctx -> handleCylinder(ctx, StringArgumentType.getString(ctx, "args"), true))));

            dispatcher.register(ClientCommandManager.literal("/sphere")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showSphereUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSphereArgs)
                            .executes(ctx -> handleSphere(ctx, StringArgumentType.getString(ctx, "args"), false))));

            dispatcher.register(ClientCommandManager.literal("/hsphere")
                    .requires(WorldEditClientCommands::canUseEditCommands)
                    .executes(WorldEditClientCommands::showHSphereUsage)
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestSphereArgs)
                            .executes(ctx -> handleSphere(ctx, StringArgumentType.getString(ctx, "args"), true))));

            dispatcher.register(ClientCommandManager.literal("/wand")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(ctx -> handleWand(ctx, "default"))
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestWandArgs)
                            .executes(ctx -> handleWand(ctx, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/pos1")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(ctx -> handlePos(ctx, false, "look"))
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestPosArgs)
                            .executes(ctx -> handlePos(ctx, false, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/pos2")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(ctx -> handlePos(ctx, true, "look"))
                    .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                            .suggests(WorldEditClientCommands::suggestPosArgs)
                            .executes(ctx -> handlePos(ctx, true, StringArgumentType.getString(ctx, "args")))));

            dispatcher.register(ClientCommandManager.literal("/desel")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::clearSelection));

            dispatcher.register(ClientCommandManager.literal("/deselect")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::clearSelection));

            dispatcher.register(ClientCommandManager.literal("/size")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showSelectionSize));

            dispatcher.register(ClientCommandManager.literal("/sel")
                    .requires(WorldEditClientCommands::canUseLocalSelectionCommands)
                    .executes(WorldEditClientCommands::showSelectionSize));
        });
    }

    private static int showSetUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //set <block>"));
        return 1;
    }

    private static int showExpandUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //expand <amount|vert> [reverseAmount] [direction]"));
        context.getSource().sendFeedback(Text.literal("[WE] Directions: north south east west up down forward back left right"));
        return 1;
    }

    private static int showContractUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //contract <amount> [reverseAmount] [direction]"));
        context.getSource().sendFeedback(Text.literal("[WE] Directions: north south east west up down forward back left right"));
        return 1;
    }

    private static int showShiftUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //shift <amount> [direction]"));
        context.getSource().sendFeedback(Text.literal("[WE] Directions: north south east west up down forward back left right"));
        return 1;
    }

    private static int showOutsetUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //outset <amount> [-h] [-v]"));
        return 1;
    }

    private static int showInsetUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //inset <amount> [-h] [-v]"));
        return 1;
    }

    private static int showReplaceUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //replace <from> <to>"));
        return 1;
    }

    private static int showWallsUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //walls <block>"));
        return 1;
    }

    private static int showFloorUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //floor <block>"));
        return 1;
    }

    private static int showRoofUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //roof <block>"));
        return 1;
    }

    private static int showEncloseUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //enclose <block>"));
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

    private static int showCylUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //cyl <block> <radius> [height]"));
        context.getSource().sendFeedback(Text.literal("[WE]        //cyl <block> <radiusX> <radiusZ> <height>"));
        return 1;
    }

    private static int showHCylUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //hcyl <block> <radius> [height]"));
        context.getSource().sendFeedback(Text.literal("[WE]        //hcyl <block> <radiusX> <radiusZ> <height>"));
        return 1;
    }

    private static int showSphereUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //sphere <block> <radius> [-r]"));
        context.getSource().sendFeedback(Text.literal("[WE]        //sphere <block> <radiusX> <radiusY> <radiusZ> [-r]"));
        return 1;
    }

    private static int showHSphereUsage(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("[WE] Usage: //hsphere <block> <radius> [-r]"));
        context.getSource().sendFeedback(Text.literal("[WE]        //hsphere <block> <radiusX> <radiusY> <radiusZ> [-r]"));
        return 1;
    }

    private static int handleExpand(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showExpandUsage(context);
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        if ("vert".equalsIgnoreCase(tokens[0]) || "vertical".equalsIgnoreCase(tokens[0])) {
            if (tokens.length > 1) {
                return showExpandUsage(context);
            }

            MinecraftClient client = context.getSource().getClient();
            if (client.world == null) {
                return 0;
            }
            return applySelectionUpdate(context, "Expanded", selection.verticalColumn(
                    client.world.getBottomY(),
                    client.world.getTopYInclusive()
            ));
        }

        Integer amount = tryParseInt(tokens[0]);
        if (amount == null) {
            return showExpandUsage(context);
        }
        if (amount < 0) {
            context.getSource().sendFeedback(Text.literal("[WE] Expand amount must be >= 0."));
            return 0;
        }

        int reverseAmount = 0;
        int nextIndex = 1;
        if (tokens.length >= 2) {
            Integer parsedReverseAmount = tryParseInt(tokens[1]);
            if (parsedReverseAmount != null) {
                if (parsedReverseAmount < 0) {
                    context.getSource().sendFeedback(Text.literal("[WE] Reverse amount must be >= 0."));
                    return 0;
                }
                reverseAmount = parsedReverseAmount;
                nextIndex = 2;
            }
        }

        Direction direction = parseDirectionOrDefault(context, tokens, nextIndex);
        if (direction == null) {
            return 0;
        }
        int consumedTokens = nextIndex + (tokens.length > nextIndex ? 1 : 0);
        if (tokens.length > consumedTokens) {
            return showExpandUsage(context);
        }

        try {
            return applySelectionUpdate(context, "Expanded", selection.expand(direction, amount, reverseAmount));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("[WE] " + e.getMessage()));
            return 0;
        }
    }

    private static int handleContract(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showContractUsage(context);
        }

        Integer amount = tryParseInt(tokens[0]);
        if (amount == null) {
            return showContractUsage(context);
        }
        if (amount < 0) {
            context.getSource().sendFeedback(Text.literal("[WE] Contract amount must be >= 0."));
            return 0;
        }

        int reverseAmount = 0;
        int nextIndex = 1;
        if (tokens.length >= 2) {
            Integer parsedReverseAmount = tryParseInt(tokens[1]);
            if (parsedReverseAmount != null) {
                if (parsedReverseAmount < 0) {
                    context.getSource().sendFeedback(Text.literal("[WE] Reverse amount must be >= 0."));
                    return 0;
                }
                reverseAmount = parsedReverseAmount;
                nextIndex = 2;
            }
        }

        Direction direction = parseDirectionOrDefault(context, tokens, nextIndex);
        if (direction == null) {
            return 0;
        }
        int consumedTokens = nextIndex + (tokens.length > nextIndex ? 1 : 0);
        if (tokens.length > consumedTokens) {
            return showContractUsage(context);
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        try {
            return applySelectionUpdate(context, "Contracted", selection.contract(direction, amount, reverseAmount));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("[WE] " + e.getMessage()));
            return 0;
        }
    }

    private static int handleShift(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showShiftUsage(context);
        }

        Integer amount = tryParseInt(tokens[0]);
        if (amount == null) {
            return showShiftUsage(context);
        }

        Direction direction = parseDirectionOrDefault(context, tokens, 1);
        if (direction == null) {
            return 0;
        }
        if (tokens.length > 2) {
            return showShiftUsage(context);
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return applySelectionUpdate(context, "Shifted", selection.shift(direction, amount));
    }

    private static int handleOutset(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showOutsetUsage(context);
        }

        Integer amount = tryParseInt(tokens[0]);
        if (amount == null) {
            return showOutsetUsage(context);
        }
        if (amount < 0) {
            context.getSource().sendFeedback(Text.literal("[WE] Outset amount must be >= 0."));
            return 0;
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        AxisOptions options = parseAxisOptions(context, tokens, 1, "outset");
        if (options == null) {
            return 0;
        }

        try {
            return applySelectionUpdate(context, "Outset", selection.outset(amount, options.onlyHorizontal(), options.onlyVertical()));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("[WE] " + e.getMessage()));
            return 0;
        }
    }

    private static int handleInset(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 1) {
            return showInsetUsage(context);
        }

        Integer amount = tryParseInt(tokens[0]);
        if (amount == null) {
            return showInsetUsage(context);
        }
        if (amount < 0) {
            context.getSource().sendFeedback(Text.literal("[WE] Inset amount must be >= 0."));
            return 0;
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        AxisOptions options = parseAxisOptions(context, tokens, 1, "inset");
        if (options == null) {
            return 0;
        }

        try {
            return applySelectionUpdate(context, "Inset", selection.inset(amount, options.onlyHorizontal(), options.onlyVertical()));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(Text.literal("[WE] " + e.getMessage()));
            return 0;
        }
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

        return submitEdit(context, "//set", selection, new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.SET,
                selection.min(),
                selection.max(),
                currentSelectionShapeName(),
                block,
                "",
                0,
                "",
                false
        ));
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

        return submitEdit(context, "//replace", selection, new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.REPLACE,
                selection.min(),
                selection.max(),
                currentSelectionShapeName(),
                from,
                to,
                0,
                "",
                false
        ));
    }

    private static int handleSurfaceFill(CommandContext<FabricClientCommandSource> context,
                                         String rawArgs,
                                         String label,
                                         WorldEditSyncPayloads.Operation operation) {
        String block = rawArgs == null ? "" : rawArgs.trim();
        if (block.isEmpty()) {
            return switch (operation) {
                case WALLS -> showWallsUsage(context);
                case FLOOR -> showFloorUsage(context);
                case ROOF -> showRoofUsage(context);
                case ENCLOSE -> showEncloseUsage(context);
                default -> 0;
            };
        }

        WorldEditSelection selection = requireSelection(context);
        if (selection == null) {
            return 0;
        }

        return submitEdit(context, label, selection, new WorldEditSyncPayloads.EditRequestPayload(
                operation,
                selection.min(),
                selection.max(),
                currentSelectionShapeName(),
                block,
                "",
                0,
                "",
                false
        ));
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

        return submitEdit(context, "//stack", selection, new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.STACK,
                selection.min(),
                selection.max(),
                currentSelectionShapeName(),
                "",
                "",
                count,
                direction.asString(),
                masked
        ));
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

        return submitEdit(context, "//move", selection, new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.MOVE,
                selection.min(),
                selection.max(),
                currentSelectionShapeName(),
                "",
                "",
                distance,
                direction.asString(),
                masked
        ));
    }

    private static int handleUndo(CommandContext<FabricClientCommandSource> context) {
        if (!supportsServerBackedWorldEdit(context.getSource().getClient())) {
            context.getSource().sendFeedback(Text.literal("[WE] Server-side WorldEdit support is not available on this server."));
            return 0;
        }

        ClientPlayNetworking.send(new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.UNDO,
                BlockPos.ORIGIN,
                BlockPos.ORIGIN,
                WorldEditRegionShape.BOX.name(),
                "",
                "",
                0,
                "",
                false
        ));
        return 1;
    }

    private static int handleRedo(CommandContext<FabricClientCommandSource> context) {
        if (!supportsServerBackedWorldEdit(context.getSource().getClient())) {
            context.getSource().sendFeedback(Text.literal("[WE] Server-side WorldEdit support is not available on this server."));
            return 0;
        }

        ClientPlayNetworking.send(new WorldEditSyncPayloads.EditRequestPayload(
                WorldEditSyncPayloads.Operation.REDO,
                BlockPos.ORIGIN,
                BlockPos.ORIGIN,
                WorldEditRegionShape.BOX.name(),
                "",
                "",
                0,
                "",
                false
        ));
        return 1;
    }

    private static int handleCylinder(CommandContext<FabricClientCommandSource> context, String rawArgs, boolean hollow) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 2 || tokens.length > 4) {
            return hollow ? showHCylUsage(context) : showCylUsage(context);
        }

        String radiiSpec;
        int height = 1;
        if (tokens.length == 2) {
            radiiSpec = tokens[1];
        } else if (tokens.length == 3) {
            radiiSpec = tokens[1];
            Integer parsedHeight = tryParseInt(tokens[2]);
            if (parsedHeight == null || parsedHeight < 1) {
                return hollow ? showHCylUsage(context) : showCylUsage(context);
            }
            height = parsedHeight;
        } else {
            radiiSpec = tokens[1] + "," + tokens[2];
            Integer parsedHeight = tryParseInt(tokens[3]);
            if (parsedHeight == null || parsedHeight < 1) {
                return hollow ? showHCylUsage(context) : showCylUsage(context);
            }
            height = parsedHeight;
        }

        BlockPos anchor = resolvePlacementAnchor(context.getSource().getClient());
        if (anchor == null) {
            return 0;
        }

        return submitPlacementEdit(context, hollow ? "//hcyl" : "//cyl", new WorldEditSyncPayloads.EditRequestPayload(
                hollow ? WorldEditSyncPayloads.Operation.HCYL : WorldEditSyncPayloads.Operation.CYL,
                anchor,
                anchor,
                WorldEditRegionShape.BOX.name(),
                tokens[0],
                radiiSpec,
                height,
                "",
                false
        ));
    }

    private static int handleSphere(CommandContext<FabricClientCommandSource> context, String rawArgs, boolean hollow) {
        String[] tokens = tokenize(rawArgs);
        if (tokens.length < 2 || tokens.length > 5) {
            return hollow ? showHSphereUsage(context) : showSphereUsage(context);
        }

        String radiiSpec;
        boolean raised = false;
        if (tokens.length == 2) {
            radiiSpec = tokens[1];
        } else if (tokens.length == 3) {
            if (isRaisedFlag(tokens[2])) {
                radiiSpec = tokens[1];
                raised = true;
            } else {
                return hollow ? showHSphereUsage(context) : showSphereUsage(context);
            }
        } else if (tokens.length == 4) {
            radiiSpec = tokens[1] + "," + tokens[2] + "," + tokens[3];
        } else {
            if (!isRaisedFlag(tokens[4])) {
                return hollow ? showHSphereUsage(context) : showSphereUsage(context);
            }
            radiiSpec = tokens[1] + "," + tokens[2] + "," + tokens[3];
            raised = true;
        }

        BlockPos anchor = resolvePlacementAnchor(context.getSource().getClient());
        if (anchor == null) {
            return 0;
        }

        return submitPlacementEdit(context, hollow ? "//hsphere" : "//sphere", new WorldEditSyncPayloads.EditRequestPayload(
                hollow ? WorldEditSyncPayloads.Operation.HSPHERE : WorldEditSyncPayloads.Operation.SPHERE,
                anchor,
                anchor,
                WorldEditRegionShape.BOX.name(),
                tokens[0],
                radiiSpec,
                0,
                "",
                raised
        ));
    }

    private static int handleWand(CommandContext<FabricClientCommandSource> context, String rawArgs) {
        MinecraftClient client = context.getSource().getClient();
        String token = rawArgs == null ? "" : rawArgs.trim();
        String wandValue = token.isEmpty() ? "default" : token;

        if ("hand".equalsIgnoreCase(wandValue)) {
            if (client.player == null || client.player.getMainHandStack().isEmpty()) {
                context.getSource().sendFeedback(Text.literal("[WE] Hold an item in your main hand first."));
                return 0;
            }
            wandValue = Registries.ITEM.getId(client.player.getMainHandStack().getItem()).toString();
        }

        if (!DebugDrawManager.setSelectionWandItem(wandValue)) {
            context.getSource().sendFeedback(Text.literal("[WE] Invalid wand item: " + rawArgs));
            return 0;
        }

        DebugDrawManager.setSelectionEnabled(true);
        DebugDrawManager.setSelectionUseAnyClick(false);

        boolean given = giveSelectionWand(client);
        context.getSource().sendFeedback(Text.literal("[WE] Selection wand enabled with " + DebugDrawManager.getSelectionWandItemId() + "."));
        if (given) {
            context.getSource().sendFeedback(Text.literal("[WE] Placed the wand into your selected hotbar slot."));
        } else {
            context.getSource().sendFeedback(Text.literal("[WE] Hold " + DebugDrawManager.getSelectionWandItemId() + " to use the wand mode."));
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
        sendSelectionSize(context.getSource(), selection);
        return 1;
    }

    private static WorldEditSelection requireSelection(CommandContext<FabricClientCommandSource> context) {
        WorldEditSelection selection = WorldEditSelection.fromDebugDrawSelection();
        if (selection == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Set pos1 and pos2 first with //pos1 and //pos2 or /draw select."));
            return null;
        }
        return selection;
    }

    private static int applySelectionUpdate(CommandContext<FabricClientCommandSource> context,
                                            String action,
                                            WorldEditSelection selection) {
        DebugDrawManager.setSelectionPos(false, selection.min());
        DebugDrawManager.setSelectionPos(true, selection.max());
        context.getSource().sendFeedback(Text.literal("[WE] " + action + " selection to "
                + selection.min().toShortString() + " -> " + selection.max().toShortString() + "."));
        sendSelectionSize(context.getSource(), selection);
        return 1;
    }

    private static int submitEdit(CommandContext<FabricClientCommandSource> context,
                                  String label,
                                  WorldEditSelection selection,
                                  WorldEditSyncPayloads.EditRequestPayload payload) {
        MinecraftClient client = context.getSource().getClient();
        if (!supportsServerBackedWorldEdit(client)) {
            context.getSource().sendFeedback(Text.literal("[WE] Server-side WorldEdit support is not available on this server."));
            return 0;
        }

        ClientPlayNetworking.send(payload);
        context.getSource().sendFeedback(Text.literal("[WE] Sent " + label + " for "
                + selection.sizeX() + "x" + selection.sizeY() + "x" + selection.sizeZ()
                + " (" + selection.volume() + " blocks)."));
        return 1;
    }

    private static int submitPlacementEdit(CommandContext<FabricClientCommandSource> context,
                                           String label,
                                           WorldEditSyncPayloads.EditRequestPayload payload) {
        MinecraftClient client = context.getSource().getClient();
        if (!supportsServerBackedWorldEdit(client)) {
            context.getSource().sendFeedback(Text.literal("[WE] Server-side WorldEdit support is not available on this server."));
            return 0;
        }

        ClientPlayNetworking.send(payload);
        context.getSource().sendFeedback(Text.literal("[WE] Sent " + label + " at " + payload.min().toShortString() + "."));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSetArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return suggestBlockArgument(builder);
    }

    private static CompletableFuture<Suggestions> suggestCylArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestBlockArgument(builder);
        }
        if (tokenIndex == 1) {
            return suggestToken(builder, prefix, List.of("3", "5", "3,5"));
        }
        if (tokenIndex == 2) {
            return suggestToken(builder, prefix, List.of("1", "3", "5", "8"));
        }
        if (tokenIndex == 3) {
            return suggestToken(builder, prefix, List.of("1", "3", "5", "8"));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestSphereArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestBlockArgument(builder);
        }
        if (tokenIndex == 1) {
            return suggestToken(builder, prefix, List.of("3", "5", "3,5,3"));
        }
        if (tokenIndex == 2) {
            return suggestToken(builder, prefix, List.of("-r", "3", "5"));
        }
        if (tokenIndex == 3 || tokenIndex == 4) {
            return suggestToken(builder, prefix, List.of("3", "5", "-r"));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestExpandArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        String[] tokens = tokenize(builder.getRemaining());
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, expandAmountSuggestions());
        }
        if (tokens.length > 0 && "vert".equalsIgnoreCase(tokens[0])) {
            return builder.buildFuture();
        }
        if (tokenIndex == 1) {
            return suggestToken(builder, prefix, amountOrDirectionSuggestions());
        }
        if (tokenIndex == 2 && tokens.length > 1 && tryParseInt(tokens[1]) != null) {
            return suggestToken(builder, prefix, directionSuggestions());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestContractArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        String[] tokens = tokenize(builder.getRemaining());
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, numberSuggestions());
        }
        if (tokenIndex == 1) {
            return suggestToken(builder, prefix, amountOrDirectionSuggestions());
        }
        if (tokenIndex == 2 && tokens.length > 1 && tryParseInt(tokens[1]) != null) {
            return suggestToken(builder, prefix, directionSuggestions());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestShiftArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, numberSuggestions());
        }
        if (tokenIndex == 1) {
            return suggestToken(builder, prefix, directionSuggestions());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOutsetArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        int tokenIndex = currentTokenIndex(builder);
        String prefix = currentTokenPrefix(builder);
        if (tokenIndex == 0) {
            return suggestToken(builder, prefix, numberSuggestions());
        }
        return suggestToken(builder, prefix, axisOptionSuggestions());
    }

    private static CompletableFuture<Suggestions> suggestInsetArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return suggestOutsetArgs(context, builder);
    }

    private static CompletableFuture<Suggestions> suggestReplaceArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return currentTokenIndex(builder) <= 1 ? suggestBlockArgument(builder) : builder.buildFuture();
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

    private static CompletableFuture<Suggestions> suggestWandArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String prefix = currentTokenPrefix(builder);
        if (currentTokenIndex(builder) > 0) {
            return builder.buildFuture();
        }

        SuggestionsBuilder tokenBuilder = tokenBuilder(builder);
        if ("hand".startsWith(prefix)) {
            tokenBuilder.suggest("hand");
        }
        if ("default".startsWith(prefix)) {
            tokenBuilder.suggest("default");
        }
        for (Identifier id : Registries.ITEM.getIds()) {
            String full = id.toString();
            String path = id.getPath();
            String normalizedFull = full.toLowerCase(Locale.ROOT);
            String normalizedPath = path.toLowerCase(Locale.ROOT);
            if (prefix.contains(":")) {
                if (normalizedFull.startsWith(prefix)) {
                    tokenBuilder.suggest(full);
                }
            } else if (normalizedPath.startsWith(prefix)) {
                tokenBuilder.suggest(path);
            } else if (normalizedFull.startsWith(prefix)) {
                tokenBuilder.suggest(full);
            }
        }
        return tokenBuilder.buildFuture();
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

    private static CompletableFuture<Suggestions> suggestBlockArgument(SuggestionsBuilder builder) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return builder.buildFuture();
        }
        return BlockArgumentParser.getSuggestions(
                client.world.getRegistryManager().getOrThrow(RegistryKeys.BLOCK),
                tokenBuilder(builder),
                false,
                true
        );
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

    private static void sendSelectionSize(FabricClientCommandSource source, WorldEditSelection selection) {
        source.sendFeedback(Text.literal("[WE] Size: " + selection.sizeX() + " x " + selection.sizeY() + " x " + selection.sizeZ()
                + " (" + selection.volume() + " blocks)"));
    }

    private static String currentSelectionShapeName() {
        return switch (DebugDrawManager.getSelectionShape()) {
            case CIRCLE -> WorldEditRegionShape.CIRCLE.name();
            case CYLINDER -> WorldEditRegionShape.CYLINDER.name();
            case SPHERE -> WorldEditRegionShape.SPHERE.name();
            case BOX -> WorldEditRegionShape.BOX.name();
        };
    }

    private static BlockPos resolvePlacementAnchor(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }
        if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return client.player.getBlockPos();
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

    private static Integer tryParseInt(String token) {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static boolean isRaisedFlag(String token) {
        if (token == null) {
            return false;
        }
        String value = token.trim().toLowerCase(Locale.ROOT);
        return "-r".equals(value) || "raised".equals(value);
    }

    private static Direction parseDirectionOrDefault(CommandContext<FabricClientCommandSource> context, String[] tokens, int tokenIndex) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return null;
        }
        if (tokens.length <= tokenIndex) {
            return resolveDirection(client.player, "forward");
        }

        Direction direction = resolveDirection(client.player, tokens[tokenIndex]);
        if (direction == null) {
            context.getSource().sendFeedback(Text.literal("[WE] Unknown direction."));
        }
        return direction;
    }

    private static AxisOptions parseAxisOptions(CommandContext<FabricClientCommandSource> context,
                                                String[] tokens,
                                                int startIndex,
                                                String label) {
        boolean onlyHorizontal = false;
        boolean onlyVertical = false;
        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i].trim().toLowerCase(Locale.ROOT);
            switch (token) {
                case "-h" -> onlyHorizontal = true;
                case "-v" -> onlyVertical = true;
                default -> {
                    context.getSource().sendFeedback(Text.literal("[WE] Unknown " + label + " option: " + tokens[i]));
                    return null;
                }
            }
        }
        return new AxisOptions(onlyHorizontal, onlyVertical);
    }

    private static List<String> expandAmountSuggestions() {
        return List.of("1", "2", "3", "4", "8", "vert");
    }

    private static List<String> numberSuggestions() {
        return List.of("1", "2", "3", "4", "8");
    }

    private static List<String> amountOrDirectionSuggestions() {
        return List.of("1", "2", "3", "4", "8", "north", "south", "east", "west", "up", "down", "forward", "back", "left", "right");
    }

    private static List<String> axisOptionSuggestions() {
        return List.of("-h", "-v");
    }

    private static List<String> directionSuggestions() {
        return List.of("north", "south", "east", "west", "up", "down", "forward", "back", "left", "right");
    }

    private static List<String> directionSuggestionsWithMask() {
        return List.of("north", "south", "east", "west", "up", "down", "forward", "back", "left", "right", "-a");
    }

    private static boolean canUseEditCommands(FabricClientCommandSource source) {
        return isClientWorldEditActive(source.getClient());
    }

    private static boolean canUseLocalSelectionCommands(FabricClientCommandSource source) {
        return isClientWorldEditActive(source.getClient());
    }

    private static boolean isClientWorldEditActive(MinecraftClient client) {
        return WorldEditModule.INSTANCE.isEnabled() && !serverHasNativeWorldEdit(client);
    }

    private static boolean supportsServerBackedWorldEdit(MinecraftClient client) {
        return client != null
                && (client.isInSingleplayer()
                || (client.getNetworkHandler() != null && ClientPlayNetworking.canSend(WorldEditSyncPayloads.EditRequestPayload.ID)));
    }

    private static boolean serverHasNativeWorldEdit(MinecraftClient client) {
        if (client == null || client.getNetworkHandler() == null || client.getNetworkHandler().getCommandDispatcher() == null) {
            return false;
        }

        return hasNativeWorldEditCommandSignature(client.getNetworkHandler().getCommandDispatcher().getRoot());
    }

    static boolean hasNativeWorldEditCommandSignature(RootCommandNode<?> root) {
        if (root == null) {
            return false;
        }

        for (String alias : WORLD_EDIT_SERVER_ROOT_ALIASES) {
            if (root.getChild(alias) != null) {
                return true;
            }
        }

        // Bukkit/Paper WorldEdit commonly exposes the double-slash aliases as "/set", "/pos1", etc.
        for (List<String> signatureAliases : WORLD_EDIT_SERVER_SIGNATURES) {
            boolean matched = false;
            for (String alias : signatureAliases) {
                if (root.getChild(alias) != null) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private static boolean giveSelectionWand(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null || !client.player.getAbilities().creativeMode) {
            return false;
        }

        Identifier itemId = Identifier.tryParse(DebugDrawManager.getSelectionWandItemId());
        if (itemId == null || !Registries.ITEM.containsId(itemId)) {
            return false;
        }

        ItemStack stack = Registries.ITEM.get(itemId).getDefaultStack();
        if (stack.isEmpty()) {
            return false;
        }

        int selectedSlot = client.player.getInventory().getSelectedSlot();
        int creativeSlot = selectedSlot + 36;
        client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(creativeSlot, stack));
        client.player.getInventory().setStack(selectedSlot, stack);
        return true;
    }

    private record AxisOptions(boolean onlyHorizontal, boolean onlyVertical) {
    }
}
