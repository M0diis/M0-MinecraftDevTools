package me.m0dii.modules.debugdraw;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

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
        send(context, "/draw diamond <x> <y> <z> <radius> [color] [seconds]");
        send(context, "/draw pyramid <x> <y> <z> <radius> <height> [color] [seconds]");
        send(context, "/draw cone <x> <y> <z> <radius> <height> [color] [seconds] [segments]");
        send(context, "/draw cuboid <x> <y> <z> <radius> <height> [color] [seconds]");
        send(context, "/draw boxlook [color] [seconds]");
        send(context, "/draw select on|off|toggle|status|save|load|clear");
        send(context, "/draw select mode <wand|any>");
        send(context, "/draw select wand [item_id|hand|default]");
        send(context, "/draw select shape [box|circle|cylinder|sphere]");
        send(context, "/draw select color [slot] [color] | /draw select color reset");
        send(context, "/draw select pos1|pos2 [look|here|<x> <y> <z>]");
        send(context, "/draw select set <x1> <y1> <z1> <x2> <y2> <z2>");
        send(context, "/draw select add [color] [seconds]");
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
            case "diamond" -> diamond(context, tokens);
            case "pyramid" -> pyramid(context, tokens);
            case "cone" -> cone(context, tokens);
            case "cuboid" -> cuboid(context, tokens);
            case "boxlook" -> boxLook(context, tokens);
            case "select", "sel" -> selection(context, tokens);
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

    private static int diamond(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 5) {
            send(context, "Expected: /draw diamond <x> <y> <z> <radius> [color] [seconds]");
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
            int id = DebugDrawManager.addDiamond(x, y, z, radius, color, seconds);
            send(context, "Added diamond #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid diamond args: " + e.getMessage());
            return 0;
        }
    }

    private static int pyramid(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 6) {
            send(context, "Expected: /draw pyramid <x> <y> <z> <radius> <height> [color] [seconds]");
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
            int id = DebugDrawManager.addPyramid(x, y, z, radius, height, color, seconds);
            send(context, "Added pyramid #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid pyramid args: " + e.getMessage());
            return 0;
        }
    }

    private static int cone(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 6) {
            send(context, "Expected: /draw cone <x> <y> <z> <radius> <height> [color] [seconds] [segments]");
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
            int id = DebugDrawManager.addCone(x, y, z, radius, height, color, seconds, segments);
            send(context, "Added cone #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid cone args: " + e.getMessage());
            return 0;
        }
    }

    private static int cuboid(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 6) {
            send(context, "Expected: /draw cuboid <x> <y> <z> <radius> <height> [color] [seconds]");
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
            int id = DebugDrawManager.addBox(x - radius, y, z - radius, x + radius, y + height, z + radius, color, seconds);
            send(context, "Added cuboid #" + id + " color=" + DebugDrawManager.formatColor(color));
            return 1;
        } catch (Exception e) {
            send(context, "Invalid cuboid args: " + e.getMessage());
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
                if (!DebugDrawManager.isSelectionUseAnyClick() && !DebugDrawManager.isSelectionWandEquipped()) {
                    send(context, "Hold " + DebugDrawManager.getSelectionWandItemId() + " to select with clicks, or use /draw select mode any.");
                }
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
            case "left", "pos1", "p1" -> {
                if (t.length >= 3 && "here".equalsIgnoreCase(t[2])) {
                    MinecraftClient client = context.getSource().getClient();
                    if (client.player == null) {
                        yield 0;
                    }
                    BlockPos pos = client.player.getBlockPos();
                    DebugDrawManager.setSelectionPos(false, pos);
                    send(context, "Set pos1 to " + pos.toShortString() + " (player).");
                    yield 1;
                }
                if (t.length >= 3 && "look".equalsIgnoreCase(t[2])) {
                    boolean ok = DebugDrawManager.pickSelectionPos(false);
                    send(context, ok ? "Set pos1 from crosshair block." : "No block under crosshair.");
                    yield ok ? 1 : 0;
                }
                if (t.length >= 5) {
                    MinecraftClient client = context.getSource().getClient();
                    if (client.player == null) {
                        yield 0;
                    }
                    try {
                        BlockPos pos = new BlockPos(
                                (int) Math.floor(parseCoord(t[2], client.player.getX())),
                                (int) Math.floor(parseCoord(t[3], client.player.getY())),
                                (int) Math.floor(parseCoord(t[4], client.player.getZ())));
                        DebugDrawManager.setSelectionPos(false, pos);
                        send(context, "Set pos1 to " + pos.toShortString() + ".");
                        yield 1;
                    } catch (Exception e) {
                        send(context, "Invalid pos1 coords: " + e.getMessage());
                        yield 0;
                    }
                }
                boolean ok = DebugDrawManager.pickSelectionPos(false);
                send(context, ok ? "Set pos1 from crosshair block." : "No block under crosshair.");
                yield ok ? 1 : 0;
            }
            case "right", "pos2", "p2" -> {
                if (t.length >= 3 && "here".equalsIgnoreCase(t[2])) {
                    MinecraftClient client = context.getSource().getClient();
                    if (client.player == null) {
                        yield 0;
                    }
                    BlockPos pos = client.player.getBlockPos();
                    DebugDrawManager.setSelectionPos(true, pos);
                    send(context, "Set pos2 to " + pos.toShortString() + " (player).");
                    yield 1;
                }
                if (t.length >= 3 && "look".equalsIgnoreCase(t[2])) {
                    boolean ok = DebugDrawManager.pickSelectionPos(true);
                    send(context, ok ? "Set pos2 from crosshair block." : "No block under crosshair.");
                    yield ok ? 1 : 0;
                }
                if (t.length >= 5) {
                    MinecraftClient client = context.getSource().getClient();
                    if (client.player == null) {
                        yield 0;
                    }
                    try {
                        BlockPos pos = new BlockPos(
                                (int) Math.floor(parseCoord(t[2], client.player.getX())),
                                (int) Math.floor(parseCoord(t[3], client.player.getY())),
                                (int) Math.floor(parseCoord(t[4], client.player.getZ())));
                        DebugDrawManager.setSelectionPos(true, pos);
                        send(context, "Set pos2 to " + pos.toShortString() + ".");
                        yield 1;
                    } catch (Exception e) {
                        send(context, "Invalid pos2 coords: " + e.getMessage());
                        yield 0;
                    }
                }
                boolean ok = DebugDrawManager.pickSelectionPos(true);
                send(context, ok ? "Set pos2 from crosshair block." : "No block under crosshair.");
                yield ok ? 1 : 0;
            }
            case "set" -> {
                if (t.length < 8) {
                    send(context, "Usage: /draw select set <x1> <y1> <z1> <x2> <y2> <z2>");
                    yield 0;
                }
                MinecraftClient client = context.getSource().getClient();
                if (client.player == null) {
                    yield 0;
                }
                try {
                    BlockPos pos1 = new BlockPos(
                            (int) Math.floor(parseCoord(t[2], client.player.getX())),
                            (int) Math.floor(parseCoord(t[3], client.player.getY())),
                            (int) Math.floor(parseCoord(t[4], client.player.getZ())));
                    BlockPos pos2 = new BlockPos(
                            (int) Math.floor(parseCoord(t[5], client.player.getX())),
                            (int) Math.floor(parseCoord(t[6], client.player.getY())),
                            (int) Math.floor(parseCoord(t[7], client.player.getZ())));
                    DebugDrawManager.setSelectionPos(false, pos1);
                    DebugDrawManager.setSelectionPos(true, pos2);
                    send(context, "Set both positions: pos1=" + pos1.toShortString() + " pos2=" + pos2.toShortString());
                    yield 1;
                } catch (Exception e) {
                    send(context, "Invalid set coords: " + e.getMessage());
                    yield 0;
                }
            }
            case "clear" -> {
                DebugDrawManager.clearSelectionPositions();
                send(context, "Cleared selection positions.");
                yield 1;
            }
            case "mode" -> {
                if (t.length < 3) {
                    send(context, "Selection mode: " + (DebugDrawManager.isSelectionUseAnyClick() ? "any" : "wand"));
                    send(context, "Usage: /draw select mode <wand|any>");
                    yield 1;
                }
                String mode = t[2].toLowerCase(Locale.ROOT);
                if ("any".equals(mode) || "all".equals(mode)) {
                    DebugDrawManager.setSelectionUseAnyClick(true);
                    send(context, "Selection mode set to any-click.");
                    yield 1;
                }
                if ("wand".equals(mode) || "tool".equals(mode)) {
                    DebugDrawManager.setSelectionUseAnyClick(false);
                    send(context, "Selection mode set to wand-only.");
                    yield 1;
                }
                send(context, "Unknown mode: " + t[2] + " (use wand or any)");
                yield 0;
            }
            case "wand" -> {
                MinecraftClient client = context.getSource().getClient();
                if (t.length < 3) {
                    send(context, "Selection wand: " + DebugDrawManager.getSelectionWandItemId());
                    send(context, "Wand equipped: " + (DebugDrawManager.isSelectionWandEquipped() ? "yes" : "no"));
                    send(context, "Usage: /draw select wand [item_id|hand|default]");
                    yield 1;
                }
                String token = t[2].toLowerCase(Locale.ROOT);
                String wandValue;
                if ("hand".equals(token)) {
                    if (client.player == null || client.player.getMainHandStack().isEmpty()) {
                        send(context, "Hold an item in your main hand first.");
                        yield 0;
                    }
                    wandValue = Registries.ITEM.getId(client.player.getMainHandStack().getItem()).toString();
                } else {
                    wandValue = token;
                }
                boolean ok = DebugDrawManager.setSelectionWandItem(wandValue);
                if (!ok) {
                    send(context, "Invalid item id: " + t[2]);
                    yield 0;
                }
                send(context, "Selection wand set to " + DebugDrawManager.getSelectionWandItemId() + ".");
                yield 1;
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
            case "color", "colors" -> handleSelectionColor(context, t);
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

    private static int handleSelectionColor(CommandContext<FabricClientCommandSource> context, String[] t) {
        if (t.length < 3) {
            sendSelectionColors(context);
            send(context, "Usage: /draw select color <pos1|pos2|connector|box|grid|shape|all> <color>");
            send(context, "Usage: /draw select color reset");
            return 1;
        }

        String token = t[2].toLowerCase(Locale.ROOT);
        if ("reset".equals(token) || "default".equals(token) || "defaults".equals(token)) {
            DebugDrawManager.resetSelectionColors();
            send(context, "Reset selection colors to defaults.");
            sendSelectionColors(context);
            return 1;
        }

        if (t.length < 4) {
            DebugDrawManager.SelectionColorSlot slot = parseSelectionColorSlot(token);
            if (slot == null && !"all".equals(token)) {
                send(context, "Unknown selection color slot: " + t[2]);
                return 0;
            }
            if ("all".equals(token)) {
                sendSelectionColors(context);
            } else {
                send(context, "Selection color " + token + "=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(slot)));
            }
            return 1;
        }

        int color;
        try {
            color = parseColorToken(t[3]);
        } catch (Exception e) {
            send(context, "Invalid color: " + e.getMessage());
            return 0;
        }

        if ("all".equals(token)) {
            for (DebugDrawManager.SelectionColorSlot slot : DebugDrawManager.SelectionColorSlot.values()) {
                DebugDrawManager.setSelectionColor(slot, color);
            }
            send(context, "Set all selection colors to " + DebugDrawManager.formatColor(color) + ".");
            return 1;
        }

        DebugDrawManager.SelectionColorSlot slot = parseSelectionColorSlot(token);
        if (slot == null) {
            send(context, "Unknown selection color slot: " + t[2]);
            return 0;
        }

        DebugDrawManager.setSelectionColor(slot, color);
        send(context, "Set selection color " + token + "=" + DebugDrawManager.formatColor(color) + ".");
        return 1;
    }

    private static void sendSelectionColors(CommandContext<FabricClientCommandSource> context) {
        send(context, "Selection colors:"
                + " pos1=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.POS1))
                + " pos2=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.POS2))
                + " connector=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.CONNECTOR))
                + " box=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.BOX))
                + " grid=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.GRID))
                + " shape=" + DebugDrawManager.formatColor(DebugDrawManager.getSelectionColor(DebugDrawManager.SelectionColorSlot.SHAPE)));
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
        String[] tokens = tokenizeSuggestionInput(builder.getRemaining());
        int index = suggestionTokenIndex(builder.getRemaining());
        String prefix = suggestionTokenPrefix(builder.getRemaining());

        if (tokens.length == 0 || index == 0) {
            return suggestToken(builder, prefix, List.of("line", "box", "circle", "cylinder", "sphere", "diamond", "pyramid", "cone", "cuboid", "boxlook", "select", "sel", "list", "remove", "clear", "save", "load", "ui"));
        }

        String sub = tokens[0].toLowerCase(Locale.ROOT);
        if ("remove".equals(sub) && index == 1) {
            SuggestionsBuilder tokenBuilder = tokenBuilder(builder);
            for (DebugDrawManager.ShapeDescriptor shape : DebugDrawManager.getActiveShapeDescriptors()) {
                String id = Integer.toString(shape.id());
                if (id.startsWith(prefix)) {
                    tokenBuilder.suggest(id);
                }
            }
            return tokenBuilder.buildFuture();
        }

        if (List.of("line", "box").contains(sub)) {
            if (index >= 1 && index <= 6) {
                return suggestToken(builder, prefix, List.of("~"));
            } else if (index == 7) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 8) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            }
            return builder.buildFuture();
        }

        if (List.of("circle", "sphere").contains(sub)) {
            if (index >= 1 && index <= 3) {
                return suggestToken(builder, prefix, List.of("~"));
            } else if (index == 4) {
                return suggestToken(builder, prefix, List.of("3", "5", "8"));
            } else if (index == 5) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 6) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            } else if (index == 7) {
                return suggestToken(builder, prefix, List.of("36", "48"));
            }
            return builder.buildFuture();
        }

        if ("diamond".equals(sub)) {
            if (index >= 1 && index <= 3) {
                return suggestToken(builder, prefix, List.of("~"));
            } else if (index == 4) {
                return suggestToken(builder, prefix, List.of("3", "5", "8"));
            } else if (index == 5) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 6) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            }
            return builder.buildFuture();
        }

        if ("cylinder".equals(sub)) {
            if (index >= 1 && index <= 3) {
                return suggestToken(builder, prefix, List.of("~"));
            } else if (index == 4 || index == 5) {
                return suggestToken(builder, prefix, List.of("3", "4", "8"));
            } else if (index == 6) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 7) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            } else if (index == 8) {
                return suggestToken(builder, prefix, List.of("36", "48"));
            }
            return builder.buildFuture();
        }

        if (List.of("pyramid", "cuboid", "cone").contains(sub)) {
            if (index >= 1 && index <= 3) {
                return suggestToken(builder, prefix, List.of("~"));
            } else if (index == 4 || index == 5) {
                return suggestToken(builder, prefix, List.of("3", "4", "8"));
            } else if (index == 6) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 7) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            } else if ("cone".equals(sub) && index == 8) {
                return suggestToken(builder, prefix, List.of("36", "48"));
            }
            return builder.buildFuture();
        }

        if ("boxlook".equals(sub)) {
            if (index == 1) {
                return suggestToken(builder, prefix, colorSuggestions());
            } else if (index == 2) {
                return suggestToken(builder, prefix, List.of("20", "60"));
            }
            return builder.buildFuture();
        }

        if ("select".equals(sub) || "sel".equals(sub)) {
            if (index == 1) {
                return suggestToken(builder, prefix, List.of("on", "off", "toggle", "status", "clear", "mode", "wand", "shape", "color", "pos1", "pos2", "set", "add", "save", "load"));
            }

            if (tokens.length >= 2) {
                String selectSub = tokens[1].toLowerCase(Locale.ROOT);
                if ("shape".equals(selectSub) && index == 2) {
                    return suggestToken(builder, prefix, List.of("box", "circle", "cylinder", "sphere"));
                } else if ("mode".equals(selectSub) && index == 2) {
                    return suggestToken(builder, prefix, List.of("wand", "any"));
                } else if ("color".equals(selectSub) || "colors".equals(selectSub)) {
                    if (index == 2) {
                        return suggestToken(builder, prefix, List.of("pos1", "pos2", "connector", "box", "grid", "shape", "all", "reset"));
                    } else if (index == 3 && !"reset".equals(tokens[2].toLowerCase(Locale.ROOT))) {
                        return suggestToken(builder, prefix, colorSuggestions());
                    }
                } else if ("wand".equals(selectSub) && index == 2) {
                    return suggestToken(builder, prefix, List.of("hand", "default", "minecraft:wooden_axe", "minecraft:blaze_rod", "minecraft:stick"));
                } else if (List.of("left", "right", "pos1", "pos2", "p1", "p2").contains(selectSub)) {
                    if (index == 2) {
                        return suggestToken(builder, prefix, List.of("look", "here", "~"));
                    } else if (index <= 4) {
                        return suggestToken(builder, prefix, List.of("~"));
                    }
                } else if ("set".equals(selectSub)) {
                    if (index >= 2 && index <= 7) {
                        return suggestToken(builder, prefix, List.of("~"));
                    }
                } else if ("add".equals(selectSub)) {
                    if (index == 2) {
                        return suggestToken(builder, prefix, colorSuggestions());
                    } else if (index == 3) {
                        return suggestToken(builder, prefix, List.of("20", "60"));
                    }
                }
            }
        }

        return builder.buildFuture();
    }

    private static String[] tokenizeSuggestionInput(String remaining) {
        String trimmed = remaining.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static int suggestionTokenIndex(String remaining) {
        String[] tokens = tokenizeSuggestionInput(remaining);
        if (tokens.length == 0) {
            return 0;
        }
        return remaining.endsWith(" ") ? tokens.length : tokens.length - 1;
    }

    private static String suggestionTokenPrefix(String remaining) {
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

    private static List<String> colorSuggestions() {
        return List.of("#00FFFF", "red", "green", "blue", "cyan", "magenta", "yellow", "orange", "white", "gray");
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

    private static DebugDrawManager.SelectionColorSlot parseSelectionColorSlot(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "pos1", "p1", "primary" -> DebugDrawManager.SelectionColorSlot.POS1;
            case "pos2", "p2", "secondary" -> DebugDrawManager.SelectionColorSlot.POS2;
            case "connector", "line", "link" -> DebugDrawManager.SelectionColorSlot.CONNECTOR;
            case "box", "bounds", "outline" -> DebugDrawManager.SelectionColorSlot.BOX;
            case "grid", "subdivides", "subdivisions" -> DebugDrawManager.SelectionColorSlot.GRID;
            case "shape", "preview" -> DebugDrawManager.SelectionColorSlot.SHAPE;
            default -> null;
        };
    }
}

