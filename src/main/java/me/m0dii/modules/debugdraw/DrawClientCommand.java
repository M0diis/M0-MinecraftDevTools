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
            for (String sub : List.of("line", "box", "circle", "cylinder", "sphere", "diamond", "pyramid", "cone", "cuboid", "boxlook", "select", "sel", "list", "remove", "clear", "save", "load", "ui")) {
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

        if ("diamond".equals(sub)) {
            if (index <= 4) {
                builder.suggest("diamond ~ ~ ~ 3");
            }
            if (index >= 5) {
                builder.suggest("diamond ~ ~ ~ 3 #00FFFF 20");
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

        if (List.of("pyramid", "cuboid").contains(sub)) {
            if (index <= 5) {
                builder.suggest(sub + " ~ ~ ~ 3 4");
            }
            if (index >= 6) {
                builder.suggest(sub + " ~ ~ ~ 3 4 #00FFFF 20");
            }
            return builder.buildFuture();
        }

        if ("cone".equals(sub)) {
            if (index <= 5) {
                builder.suggest("cone ~ ~ ~ 3 4");
            }
            if (index >= 6) {
                builder.suggest("cone ~ ~ ~ 3 4 #00FFFF 20 36");
            }
            return builder.buildFuture();
        }

        if ("boxlook".equals(sub)) {
            builder.suggest("boxlook #00FFFF 20");
        }

        if ("select".equals(sub) || "sel".equals(sub)) {
            if (index <= 1) {
                builder.suggest("select on");
                builder.suggest("select off");
                builder.suggest("select toggle");
                builder.suggest("select status");
                builder.suggest("select clear");
                builder.suggest("select mode wand");
                builder.suggest("select mode any");
                builder.suggest("select wand hand");
                builder.suggest("select wand minecraft:wooden_axe");
                builder.suggest("select shape box");
                builder.suggest("select shape circle");
                builder.suggest("select shape cylinder");
                builder.suggest("select shape sphere");
                builder.suggest("select pos1 look");
                builder.suggest("select pos2 look");
                builder.suggest("select pos1 here");
                builder.suggest("select pos2 here");
                builder.suggest("select set ~ ~ ~ ~ ~ ~");
                builder.suggest("select add #00FFFF 20");
                builder.suggest("select save");
                builder.suggest("select load");
                return builder.buildFuture();
            }

            if (rawTokens.length >= 2) {
                String selectSub = rawTokens[1].toLowerCase(Locale.ROOT);
                if ("shape".equals(selectSub)) {
                    builder.suggest("select shape box");
                    builder.suggest("select shape circle");
                    builder.suggest("select shape cylinder");
                    builder.suggest("select shape sphere");
                } else if ("mode".equals(selectSub)) {
                    builder.suggest("select mode wand");
                    builder.suggest("select mode any");
                } else if ("wand".equals(selectSub)) {
                    builder.suggest("select wand hand");
                    builder.suggest("select wand default");
                    builder.suggest("select wand minecraft:wooden_axe");
                    builder.suggest("select wand minecraft:blaze_rod");
                    builder.suggest("select wand minecraft:stick");
                } else if (List.of("left", "right", "pos1", "pos2", "p1", "p2").contains(selectSub)) {
                    builder.suggest("select " + selectSub + " look");
                    builder.suggest("select " + selectSub + " here");
                    builder.suggest("select " + selectSub + " ~ ~ ~");
                } else if ("set".equals(selectSub)) {
                    builder.suggest("select set ~ ~ ~ ~ ~ ~");
                } else if ("add".equals(selectSub)) {
                    builder.suggest("select add #00FFFF 20");
                    builder.suggest("select add green 60");
                }
            }
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

