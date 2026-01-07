package me.m0dii.modules.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaypointHandler {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Waypoint {
        public String id;
        public String name;
        public Vec3d position;
        public String dimension;
        public long createdTime;

        public Waypoint(String id, String name, Vec3d position, String dimension) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.dimension = dimension;
            this.createdTime = System.currentTimeMillis();
        }
    }

    @Getter
    private static List<Waypoint> waypoints = new ArrayList<>();

    public static void register() {
        loadWaypoints();
    }

    public static void loadWaypoints() {
        if (!Files.exists(CONFIG_FILE)) {
            saveWaypoints();
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            TypeToken<List<Waypoint>> typeToken = new TypeToken<>() {
            };
            List<Waypoint> loadedWaypoints = GSON.fromJson(json, typeToken.getType());
            if (loadedWaypoints != null) {
                waypoints = loadedWaypoints;
            }
        } catch (IOException e) {
            System.err.println("Failed to load waypoints: " + e.getMessage());
        }
    }

    public static void saveWaypoints() {
        try {
            String json = GSON.toJson(waypoints);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            System.err.println("Failed to save waypoints: " + e.getMessage());
        }
    }

    public static void addWaypoint() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        Vec3d pos = client.player.getPos();
        String dimension = client.world.getRegistryKey().getValue().toString();
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "Waypoint " + (waypoints.size() + 1);

        Waypoint waypoint = new Waypoint(id, name, pos, dimension);
        waypoints.add(waypoint);
        saveWaypoints();

        Text message = Text.literal("")
                .append(Text.literal("✓ ").formatted(Formatting.GREEN))
                .append(Text.literal("Waypoint added: ").formatted(Formatting.GRAY))
                .append(Text.literal(name).formatted(Formatting.AQUA))
                .append(Text.literal(" at ").formatted(Formatting.GRAY))
                .append(Text.literal(String.format("%.0f, %.0f, %.0f", pos.x, pos.y, pos.z)).formatted(Formatting.WHITE));

        client.player.sendMessage(message, false);
    }

    public static void listWaypoints() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (waypoints.isEmpty()) {
            Text message = Text.literal("No waypoints saved").formatted(Formatting.GRAY);
            client.player.sendMessage(message, false);
            return;
        }

        Text header = Text.literal("")
                .append(Text.literal("════════════").formatted(Formatting.GOLD))
                .append(Text.literal("\n"))
                .append(Text.literal("WAYPOINTS").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\n"))
                .append(Text.literal("════════════").formatted(Formatting.GOLD));

        client.player.sendMessage(header, false);

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            String dimName = wp.dimension.substring(wp.dimension.lastIndexOf(":") + 1);
            double distance = client.player.getPos().distanceTo(wp.position);

            Text teleportButton = Text.literal("TP")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.GREEN)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/waypoint tp " + wp.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to teleport to ").formatted(Formatting.GREEN)
                                            .append(Text.literal(wp.name).formatted(Formatting.AQUA)))));

            Text deleteButton = Text.literal("DEL")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.RED)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/waypoint delete " + wp.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to delete ").formatted(Formatting.RED)
                                            .append(Text.literal(wp.name).formatted(Formatting.AQUA))
                                            .append(Text.literal("\nThis cannot be undone!").formatted(Formatting.YELLOW)))));

            Text renameButton = Text.literal("RENAME")
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.YELLOW)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/waypoint rename " + wp.id + " "))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to rename ").formatted(Formatting.YELLOW)
                                            .append(Text.literal(wp.name).formatted(Formatting.AQUA)))));

            Text waypointInfo = Text.literal("")
                    .append(Text.literal((i + 1) + ". ").formatted(Formatting.GRAY))
                    .append(Text.literal(wp.name).formatted(Formatting.AQUA, Formatting.BOLD))
                    .append(Text.literal(" [" + dimName + "]").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(" • ").formatted(Formatting.YELLOW))
                    .append(Text.literal(String.format("%.0f, %.0f, %.0f", wp.position.x, wp.position.y, wp.position.z)).formatted(Formatting.WHITE))
                    .append(Text.literal(" (").formatted(Formatting.GRAY))
                    .append(Text.literal(String.format("%.0fm", distance)).formatted(Formatting.GOLD))
                    .append(Text.literal(")").formatted(Formatting.GRAY))
                    .append(Text.literal("\n   "))
                    .append(teleportButton)
                    .append(Text.literal("  "))
                    .append(renameButton)
                    .append(Text.literal("  "))
                    .append(deleteButton);

            client.player.sendMessage(waypointInfo, false);

            if (i < waypoints.size() - 1) {
                client.player.sendMessage(Text.literal("------------").formatted(Formatting.DARK_GRAY), false);
            }
        }

        Text footer = Text.literal("════════════").formatted(Formatting.GOLD);

        client.player.sendMessage(footer, false);
    }

    public static void teleportToWaypoint(String waypointId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendMessage(Text.literal("❌ Waypoint not found!").formatted(Formatting.RED), false);
            return;
        }

        // Check if in creative mode or has permissions
        if (!client.player.getAbilities().creativeMode) {
            client.player.sendMessage(Text.literal("❌ You need creative mode to teleport!").formatted(Formatting.RED), false);
            return;
        }

        // Teleport
        client.player.setPosition(waypoint.position.x, waypoint.position.y, waypoint.position.z);

        // Success message
        Text message = Text.literal("")
                .append(Text.literal("🚀 ").formatted(Formatting.GREEN))
                .append(Text.literal("Teleported to ").formatted(Formatting.GRAY))
                .append(Text.literal(waypoint.name).formatted(Formatting.AQUA))
                .append(Text.literal("!").formatted(Formatting.GRAY));

        client.player.sendMessage(message, true); // Action bar
    }

    public static void deleteWaypoint(String waypointId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendMessage(Text.literal("❌ Waypoint not found!").formatted(Formatting.RED), false);
            return;
        }

        waypoints.removeIf(wp -> wp.id.equals(waypointId));
        saveWaypoints();

        // Success message
        Text message = Text.literal("Deleted waypoint ").formatted(Formatting.GRAY)
                .append(Text.literal(waypoint.name).formatted(Formatting.AQUA));

        client.player.sendMessage(message, false);
    }

    public static void renameWaypoint(String waypointId, String newName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendMessage(Text.literal("Waypoint not found!").formatted(Formatting.RED), false);
            return;
        }

        String oldName = waypoint.name;
        waypoint.name = newName.trim();
        saveWaypoints();

        // Success message
        Text message = Text.literal("")
                .append(Text.literal("✏️ ").formatted(Formatting.YELLOW))
                .append(Text.literal("Renamed ").formatted(Formatting.GRAY))
                .append(Text.literal(oldName).formatted(Formatting.DARK_AQUA))
                .append(Text.literal(" to ").formatted(Formatting.GRAY))
                .append(Text.literal(waypoint.name).formatted(Formatting.AQUA));

        client.player.sendMessage(message, false);
    }

    private static Waypoint findWaypointById(String id) {
        return waypoints.stream()
                .filter(wp -> wp.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
