package me.m0dii.modules.debugdraw;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DrawClientCommand {
    private static final int DEFAULT_COLOR = 0x00FFFF;
    private static final double DEFAULT_SECONDS = 20.0;

    private static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
            Map.entry("red", 0xFF5555),
            Map.entry("green", 0x55FF55),
            Map.entry("blue", 0x55AAFF),
            Map.entry("cyan", 0x00FFFF),
            Map.entry("magenta", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("orange", 0xFFAA00),
            Map.entry("white", 0xFFFFFF),
            Map.entry("gray", 0xA0A0A0)
    );

    private DrawClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("draw")
                        .executes(DrawClientCommand::showHelp)
                        .then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                .suggests(DrawClientCommand::suggestArgs)
                                .executes(DrawClientCommand::handleArgs))));
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        send(context, "Usage:");
        send(context, "/draw line <x1> <y1> <z1> <x2> <y2> <z2> [color] [seconds]");
        send(context, "/draw box <x1> <y1> <z1> <x2> <y2> <z2> [color] [seconds]");
        send(context, "/draw circle <x> <y> <z> <radius> [color] [seconds] [segments]");
        send(context, "/draw cylinder <x> <y> <z> <radius> <height> [color] [seconds] [segments]");
        send(context, "/draw sphere <x> <y> <z> <radius> [color] [seconds] [segments]");
        send(context, "/draw boxlook [color] [seconds]");
        send(context, "/draw select <on|off|toggle|shape|left|right|add|status|save|load> [color] [seconds]");
        send(context, "/draw list | /draw remove <id> | /draw clear");
        send(context, "/draw save | /draw load | /draw ui");
        return 1;
    }

    private static int handleArgs(CommandContext<FabricClientCommandSource> context) {
        String[] tokens = StringArgumentType.getString(context, "args").trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return showHelp(context);
        }

        return switch (tokens[0].toLowerCase(Locale.ROOT)) {
            case "line" -> line(context, tokens);
            case "box" -> box(context, tokens);
            case "circle" -> circle(context, tokens);
            case "cylinder" -> cylinder(context, tokens);
            case "sphere" -> sphere(context, tokens);
            case "boxlook" -> boxLook(context, tokens);
            case "select" -> selection(context, tokens);
            case "list" -> list(context);
            case "remove" -> remove(context, tokens);
            case "clear" -> clear(context);
            case "save" -> save(context);
            case "load" -> load(context);
            case "ui" -> openUi(context);
            default -> showHelp(context);
        };
    }

    private static int line(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 7) {
            send(context, "Expected: /draw line <x1> <y1> <z1> <x2> <y2> <z2> [color] [seconds]");
            return 0;
        }
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        try {
            double x1 = parseCoord(t[1], client.player.getX());
            double y1 = parseCoord(t[2], client.player.getY());
            double z1 = parseCoord(t[3], client.player.getZ());
            double x2 = parseCoord(t[4], client.player.getX());
            double y2 = parseCoord(t[5], client.player.getY());
            double z2 = parseCoord(t[6], client.player.getZ());
            int color = t.length >= 8 ? parseColorToken(t[7]) : DEFAULT_COLOR;
            double seconds = t.length >= 9 ? parseSeconds(t[8]) : DEFAULT_SECONDS;
            int id = DebugDrawManager.addLine(x1, y1, z1, x2, y2, z2, color, seconds);
            send(context, "Added line #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid line args: " + e.getMessage());
            return 0;
        }
    }

    private static int box(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 7) {
            send(context, "Expected: /draw box <x1> <y1> <z1> <x2> <y2> <z2> [color] [seconds]");
            return 0;
        }
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        try {
            double x1 = parseCoord(t[1], client.player.getX());
            double y1 = parseCoord(t[2], client.player.getY());
            double z1 = parseCoord(t[3], client.player.getZ());
            double x2 = parseCoord(t[4], client.player.getX());
            double y2 = parseCoord(t[5], client.player.getY());
            double z2 = parseCoord(t[6], client.player.getZ());
            int color = t.length >= 8 ? parseColorToken(t[7]) : DEFAULT_COLOR;
            double seconds = t.length >= 9 ? parseSeconds(t[8]) : DEFAULT_SECONDS;
            int id = DebugDrawManager.addBox(x1, y1, z1, x2, y2, z2, color, seconds);
            send(context, "Added box #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid box args: " + e.getMessage());
            return 0;
        }
    }

    private static int circle(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 5) {
            send(context, "Expected: /draw circle <x> <y> <z> <radius> [color] [seconds] [segments]");
            return 0;
        }
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        try {
            double x = parseCoord(t[1], client.player.getX());
            double y = parseCoord(t[2], client.player.getY());
            double z = parseCoord(t[3], client.player.getZ());
            double radius = Double.parseDouble(t[4]);
            int color = t.length >= 6 ? parseColorToken(t[5]) : DEFAULT_COLOR;
            double seconds = t.length >= 7 ? parseSeconds(t[6]) : DEFAULT_SECONDS;
            int segments = t.length >= 8 ? Integer.parseInt(t[7]) : 36;
            int id = DebugDrawManager.addCircle(x, y, z, radius, color, seconds, segments);
            send(context, "Added circle #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid circle args: " + e.getMessage());
            return 0;
        }
    }

    private static int cylinder(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 6) {
            send(context, "Expected: /draw cylinder <x> <y> <z> <radius> <height> [color] [seconds] [segments]");
            return 0;
        }
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        try {
            double x = parseCoord(t[1], client.player.getX());
            double y = parseCoord(t[2], client.player.getY());
            double z = parseCoord(t[3], client.player.getZ());
            double radius = Double.parseDouble(t[4]);
            double height = Double.parseDouble(t[5]);
            int color = t.length >= 7 ? parseColorToken(t[6]) : DEFAULT_COLOR;
            double seconds = t.length >= 8 ? parseSeconds(t[7]) : DEFAULT_SECONDS;
            int segments = t.length >= 9 ? Integer.parseInt(t[8]) : 36;
            int id = DebugDrawManager.addCylinder(x, y, z, radius, height, color, seconds, segments);
            send(context, "Added cylinder #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid cylinder args: " + e.getMessage());
            return 0;
        }
    }

    private static int sphere(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 5) {
            send(context, "Expected: /draw sphere <x> <y> <z> <radius> [color] [seconds] [segments]");
            return 0;
        }
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null) {
            return 0;
        }

        try {
            double x = parseCoord(t[1], client.player.getX());
            double y = parseCoord(t[2], client.player.getY());
            double z = parseCoord(t[3], client.player.getZ());
            double radius = Double.parseDouble(t[4]);
            int color = t.length >= 6 ? parseColorToken(t[5]) : DEFAULT_COLOR;
            double seconds = t.length >= 7 ? parseSeconds(t[6]) : DEFAULT_SECONDS;
            int segments = t.length >= 8 ? Integer.parseInt(t[7]) : 36;
            int id = DebugDrawManager.addSphere(x, y, z, radius, color, seconds, segments);
            send(context, "Added sphere #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid sphere args: " + e.getMessage());
            return 0;
        }
    }

    private static int boxLook(CommandContext<FabricClientCommandSource> context, String[] t) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null || client.world == null) {
            return 0;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            send(context, "No block under crosshair.");
            return 0;
        }

        try {
            int color = t.length >= 2 ? parseColorToken(t[1]) : DEFAULT_COLOR;
            double seconds = t.length >= 3 ? parseSeconds(t[2]) : DEFAULT_SECONDS;
            double x = hit.getBlockPos().getX();
            double y = hit.getBlockPos().getY();
            double z = hit.getBlockPos().getZ();
            int id = DebugDrawManager.addBox(x, y, z, x + 1.0, y + 1.0, z + 1.0, color, seconds);
            send(context, "Added looked-block box #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid boxlook args: " + e.getMessage());
            return 0;
        }
    }

    private static int selection(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 2) {
            send(context, "Selection: " + DebugDrawManager.selectionStatus());
            return 1;
        }
        String sub = t[1].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "on" -> {
                DebugDrawManager.setSelectionEnabled(true);
                send(context, "Selection tool enabled.");
                yield 1;
            }
            case "off" -> {
                DebugDrawManager.setSelectionEnabled(false);
                send(context, "Selection tool disabled.");
                yield 1;
            }
            case "toggle" -> {
                boolean next = !DebugDrawManager.isSelectionEnabled();
                DebugDrawManager.setSelectionEnabled(next);
                send(context, "Selection tool " + (next ? "enabled" : "disabled") + ".");
                yield 1;
            }
            case "left" -> {
                boolean ok = DebugDrawManager.pickSelectionPos(false);
                send(context, ok ? "Set pos1 from crosshair block." : "No block under crosshair.");
                yield ok ? 1 : 0;
            }
            case "right" -> {
                boolean ok = DebugDrawManager.pickSelectionPos(true);
                send(context, ok ? "Set pos2 from crosshair block." : "No block under crosshair.");
                yield ok ? 1 : 0;
            }
            case "shape" -> {
                if (t.length < 3) {
                    send(context, "Current selection shape: " + DebugDrawManager.getSelectionShape().name().toLowerCase(Locale.ROOT));
                    send(context, "Usage: /draw select shape <box|circle|cylinder|sphere>");
                    yield 1;
                }
                DebugDrawManager.SelectionShape shape = parseSelectionShape(t[2]);
                if (shape == null) {
                    send(context, "Unknown selection shape: " + t[2]);
                    yield 0;
                }
                DebugDrawManager.setSelectionShape(shape);
                send(context, "Selection shape set to " + shape.name().toLowerCase(Locale.ROOT) + ".");
                yield 1;
            }
            case "add" -> {
                try {
                    int color = t.length >= 3 ? parseColorToken(t[2]) : DEFAULT_COLOR;
                    double seconds = t.length >= 4 ? parseSeconds(t[3]) : DEFAULT_SECONDS;
                    int id = DebugDrawManager.addSelectionShape(color, seconds);
                    if (id < 0) {
                        send(context, "Set both selection points first (left click + right click).");
                        yield 0;
                    }
                    send(context, "Added selection " + DebugDrawManager.getSelectionShape().name().toLowerCase(Locale.ROOT)
                            + " #" + id + " color=" + DebugDrawManager.formatColor(color));
                    yield 1;
                } catch (Exception e) {
                    send(context, "Invalid select add args: " + e.getMessage());
                    yield 0;
                }
            }
            case "status" -> {
                send(context, "Selection: " + DebugDrawManager.selectionStatus());
                yield 1;
            }
            case "save" -> {
                boolean ok = DebugDrawManager.saveSelection();
                send(context, ok ? "Saved selection tool state." : "Failed to save selection tool state.");
                yield ok ? 1 : 0;
            }
            case "load" -> {
                boolean ok = DebugDrawManager.loadSelection();
                send(context, ok ? "Loaded selection tool state." : "Failed to load selection tool state.");
                yield ok ? 1 : 0;
            }
            default -> showHelp(context);
        };
    }

    private static int list(CommandContext<FabricClientCommandSource> context) {
        List<String> active = DebugDrawManager.describeActive();
        if (active.isEmpty()) {
            send(context, "No active debug draw shapes.");
            return 1;
        }
        send(context, "Active debug draw shapes: " + active.size());
        for (String line : active) {
            send(context, " - " + line);
        }
        return 1;
    }

    private static int remove(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 2) {
            send(context, "Expected: /draw remove <id>");
            return 0;
        }
        try {
            int id = Integer.parseInt(t[1]);
            boolean removed = DebugDrawManager.remove(id);
            send(context, removed ? "Removed shape #" + id : "Shape #" + id + " not found.");
            return removed ? 1 : 0;
        } catch (Exception e) {
            send(context, "Invalid id: " + t[1]);
            return 0;
        }
    }

    private static int clear(CommandContext<FabricClientCommandSource> context) {
        int removed = DebugDrawManager.clear();
        send(context, "Cleared " + removed + " debug draw shape(s).");
        return 1;
    }

    private static int save(CommandContext<FabricClientCommandSource> context) {
        int saved = DebugDrawManager.saveToDisk();
        send(context, saved >= 0 ? "Saved " + saved + " shape(s)." : "Failed to save shapes.");
        return saved >= 0 ? 1 : 0;
    }

    private static int load(CommandContext<FabricClientCommandSource> context) {
        int loaded = DebugDrawManager.loadFromDisk();
        send(context, loaded >= 0 ? "Loaded " + loaded + " shape(s)." : "Failed to load shapes.");
        return loaded >= 0 ? 1 : 0;
    }

    private static int openUi(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        client.execute(() -> client.setScreen(DebugDrawScreen.create(client.currentScreen)));
        return 1;
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

    public static int parseColorToken(String token) {
        String t = token.trim().toLowerCase(Locale.ROOT);
        if (NAMED_COLORS.containsKey(t)) {
            return NAMED_COLORS.get(t);
        }
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        if (!t.matches("[0-9a-f]{6}")) {
            throw new IllegalArgumentException("color must be #RRGGBB or a known name");
        }
        return Integer.parseInt(t, 16) & 0xFFFFFF;
    }

    private static double parseSeconds(String token) {
        double value = Double.parseDouble(token);
        if (value <= 0.0) {
            throw new IllegalArgumentException("seconds must be > 0");
        }
        return Math.min(value, 3600.0);
    }

    private static void send(CommandContext<FabricClientCommandSource> context, String message) {
        var player = context.getSource().getPlayer();
        if (player != null) {
            player.sendMessage(Text.literal("[Draw] " + message), false);
        }
    }

    private static CompletableFuture<Suggestions> suggestArgs(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String trimmed = remaining.trim();
        boolean trailingSpace = remaining.endsWith(" ");
        String[] rawTokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        int index = trailingSpace ? rawTokens.length : Math.max(0, rawTokens.length - 1);

        if (rawTokens.length == 0 || (rawTokens.length == 1 && !trailingSpace)) {
            String prefix = rawTokens.length == 0 ? "" : rawTokens[0].toLowerCase(Locale.ROOT);
            for (String sub : List.of("line", "box", "circle", "cylinder", "sphere", "boxlook", "select", "list", "remove", "clear", "save", "load", "ui")) {
                if (sub.startsWith(prefix)) {
                    builder.suggest(sub);
                }
            }
            return builder.buildFuture();
        }

        String sub = rawTokens[0].toLowerCase(Locale.ROOT);
        if ("remove".equals(sub) && index >= 1) {
            for (DebugDrawManager.ShapeDescriptor shape : DebugDrawManager.getActiveShapeDescriptors()) {
                builder.suggest("remove " + shape.id());
            }
            return builder.buildFuture();
        }

        if (List.of("line", "box").contains(sub)) {
            if (index <= 6) {
                builder.suggest(sub + " ~ ~ ~ ~ ~ ~");
            }
            if (index == 7 || index == 8) {
                builder.suggest(sub + " ~ ~ ~ ~ ~ ~ #00FFFF 20");
                builder.suggest(sub + " ~ ~ ~ ~ ~ ~ red 20");
            }
            return builder.buildFuture();
        }

        if (List.of("circle", "sphere").contains(sub)) {
            if (index <= 4) {
                builder.suggest(sub + " ~ ~ ~ 3");
            }
            if (index >= 5) {
                builder.suggest(sub + " ~ ~ ~ 3 #00FFFF 20 36");
            }
            return builder.buildFuture();
        }

        if ("cylinder".equals(sub)) {
            if (index <= 5) {
                builder.suggest("cylinder ~ ~ ~ 3 4");
            }
            if (index >= 6) {
                builder.suggest("cylinder ~ ~ ~ 3 4 #00FFFF 20 36");
            }
            return builder.buildFuture();
        }

        if ("boxlook".equals(sub)) {
            builder.suggest("boxlook #00FFFF 20");
        }

        if ("select".equals(sub)) {
            builder.suggest("select toggle");
            builder.suggest("select shape box");
            builder.suggest("select shape circle");
            builder.suggest("select shape cylinder");
            builder.suggest("select shape sphere");
            builder.suggest("select left");
            builder.suggest("select right");
            builder.suggest("select add #00FFFF 20");
            builder.suggest("select status");
            builder.suggest("select save");
            builder.suggest("select load");
        }

        return builder.buildFuture();
    }

    private static DebugDrawManager.SelectionShape parseSelectionShape(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "box", "cuboid" -> DebugDrawManager.SelectionShape.BOX;
            case "circle", "disk" -> DebugDrawManager.SelectionShape.CIRCLE;
            case "cylinder" -> DebugDrawManager.SelectionShape.CYLINDER;
            case "sphere" -> DebugDrawManager.SelectionShape.SPHERE;
            default -> null;
        };
    }
}

