package me.m0dii.modules.camera;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class CameraClientCommand {
    private static final List<String> ROOT_COMMANDS = List.of(
            "start", "clear", "status", "list",
            "add", "prepend", "move", "select", "duration",
            "split", "delete", "trim", "transpose", "stretch",
            "interpolation", "play", "stop", "show", "hide",
            "save", "load", "delete_saved"
    );

    private CameraClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("camera")
                        .executes(CameraClientCommand::showHelp)
                        .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                .suggests(CameraClientCommand::suggestArgs)
                                .executes(CameraClientCommand::handleArgs))));
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        send(context, "Usage:");
        send(context, "/camera start | clear | status | list");
        send(context, "/camera add <seconds> | prepend <seconds> | move");
        send(context, "/camera select nearest|<index> | duration <seconds> | split | delete | trim");
        send(context, "/camera interpolation <linear|catmull_rom> | stretch <percent> | transpose");
        send(context, "/camera play [loops] | stop");
        send(context, "/camera show | hide");
        send(context, "/camera save <name> | load <name> | delete_saved <name>");
        return 1;
    }

    private static int handleArgs(CommandContext<FabricClientCommandSource> context) {
        String raw = StringArgumentType.getString(context, "args");
        String[] tokens = raw == null ? new String[0] : raw.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return showHelp(context);
        }

        try {
            String result = switch (tokens[0].toLowerCase(Locale.ROOT)) {
                case "start" -> CameraPathManager.startPath();
                case "clear" -> CameraPathManager.clearPath();
                case "status" -> {
                    for (String line : CameraPathManager.statusLines()) {
                        send(context, line);
                    }
                    yield null;
                }
                case "list" -> CameraPathManager.listPaths();
                case "add" -> {
                    requireLength(tokens, 2, "/camera add <seconds>");
                    yield CameraPathManager.addPoint(parsePositiveDouble(tokens[1], "seconds"));
                }
                case "prepend" -> {
                    requireLength(tokens, 2, "/camera prepend <seconds>");
                    yield CameraPathManager.prependPoint(parsePositiveDouble(tokens[1], "seconds"));
                }
                case "move" -> CameraPathManager.moveSelectedToCurrent();
                case "select" -> {
                    requireLength(tokens, 2, "/camera select nearest|<index>");
                    yield "nearest".equalsIgnoreCase(tokens[1])
                            ? CameraPathManager.selectNearest()
                            : CameraPathManager.selectPoint(parsePositiveInt(tokens[1], "index"));
                }
                case "duration" -> {
                    requireLength(tokens, 2, "/camera duration <seconds>");
                    yield CameraPathManager.setSelectedSegmentDuration(parsePositiveDouble(tokens[1], "seconds"));
                }
                case "split", "split_point" -> CameraPathManager.splitSelectedSegment();
                case "delete", "delete_point" -> CameraPathManager.deleteSelectedPoint();
                case "trim", "trim_path" -> CameraPathManager.trimAfterSelected();
                case "transpose" -> CameraPathManager.transposeToCurrent();
                case "stretch" -> {
                    requireLength(tokens, 2, "/camera stretch <percent>");
                    yield CameraPathManager.stretchTimeline(parsePositiveInt(tokens[1], "percent"));
                }
                case "interpolation", "interp" -> {
                    requireLength(tokens, 2, "/camera interpolation <linear|catmull_rom>");
                    yield CameraPathManager.setInterpolation(tokens[1]);
                }
                case "play" -> CameraPathManager.play(tokens.length >= 2 ? parsePositiveInt(tokens[1], "loops") : 1);
                case "stop" -> CameraPathManager.stopPlayback();
                case "show" -> CameraPathManager.showOverlay();
                case "hide" -> CameraPathManager.hideOverlay();
                case "save", "save_as" -> {
                    requireLength(tokens, 2, "/camera save <name>");
                    yield CameraPathManager.saveWorkingPath(joinTail(tokens, 1));
                }
                case "load" -> {
                    requireLength(tokens, 2, "/camera load <name>");
                    yield CameraPathManager.loadPath(joinTail(tokens, 1));
                }
                case "delete_saved", "remove_saved" -> {
                    requireLength(tokens, 2, "/camera delete_saved <name>");
                    yield CameraPathManager.deleteSavedPath(joinTail(tokens, 1));
                }
                default -> {
                    showHelp(context);
                    yield null;
                }
            };

            if (result != null && !result.isBlank()) {
                send(context, result);
            }
            return 1;
        } catch (IllegalStateException ex) {
            send(context, ex.getMessage());
            return 0;
        }
    }

    private static void requireLength(String[] tokens, int required, String usage) {
        if (tokens.length < required) {
            throw new IllegalStateException("Expected: " + usage);
        }
    }

    private static double parsePositiveDouble(String token, String label) {
        try {
            double value = Double.parseDouble(token);
            if (value <= 0.0) {
                throw new IllegalStateException(label + " must be greater than 0.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid " + label + ": " + token);
        }
    }

    private static int parsePositiveInt(String token, String label) {
        try {
            int value = Integer.parseInt(token);
            if (value <= 0) {
                throw new IllegalStateException(label + " must be greater than 0.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid " + label + ": " + token);
        }
    }

    private static String joinTail(String[] tokens, int start) {
        if (start >= tokens.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder(tokens[start]);
        for (int i = start + 1; i < tokens.length; i++) {
            builder.append(' ').append(tokens[i]);
        }
        return builder.toString();
    }

    private static void send(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Text.literal("[Camera] " + message));
    }

    private static CompletableFuture<Suggestions> suggestArgs(CommandContext<FabricClientCommandSource> context,
                                                              SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String trimmed = remaining.trim();
        boolean trailingSpace = remaining.endsWith(" ");
        String[] rawTokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        int index = trailingSpace ? rawTokens.length : Math.max(0, rawTokens.length - 1);

        if (rawTokens.length == 0 || (rawTokens.length == 1 && !trailingSpace)) {
            String prefix = rawTokens.length == 0 ? "" : rawTokens[0].toLowerCase(Locale.ROOT);
            for (String root : ROOT_COMMANDS) {
                if (root.startsWith(prefix)) {
                    builder.suggest(root);
                }
            }
            return builder.buildFuture();
        }

        String sub = rawTokens[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "prepend", "duration" -> CommandSource.suggestMatching(
                    List.of(sub + " 0.5", sub + " 1", sub + " 2", sub + " 5"), builder);
            case "stretch" -> CommandSource.suggestMatching(
                    List.of("stretch 50", "stretch 75", "stretch 100", "stretch 150", "stretch 200"), builder);
            case "interpolation", "interp" -> CommandSource.suggestMatching(
                    List.of("interpolation linear", "interpolation catmull_rom"), builder);
            case "play" -> CommandSource.suggestMatching(
                    List.of("play 1", "play 2", "play 4"), builder);
            case "select" -> {
                if (index <= 1) {
                    builder.suggest("select nearest");
                    for (int i = 1; i <= CameraPathManager.pointCount(); i++) {
                        builder.suggest("select " + i);
                    }
                }
            }
            case "load" -> {
                for (String name : CameraPathManager.savedPathNames()) {
                    builder.suggest("load " + name);
                }
            }
            case "delete_saved", "remove_saved" -> {
                for (String name : CameraPathManager.savedPathNames()) {
                    builder.suggest("delete_saved " + name);
                }
            }
            case "save", "save_as" -> builder.suggest("save shot_a");
            default -> {
            }
        }

        return builder.buildFuture();
    }
}
