package me.m0dii.modules.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
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
        public Vec3 position;
        public String dimension;
        public long createdTime;

        public Waypoint(String id, String name, Vec3 position, String dimension) {
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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Vec3 pos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        String dimension = client.level.dimension().identifier().toString();
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "Waypoint " + (waypoints.size() + 1);

        Waypoint waypoint = new Waypoint(id, name, pos, dimension);
        waypoints.add(waypoint);
        saveWaypoints();

        Component message = Component.literal("")
                .append(Component.literal("✓ ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("Waypoint added: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.0f, %.0f, %.0f", pos.x, pos.y, pos.z)).withStyle(ChatFormatting.WHITE));

        client.player.sendSystemMessage(message);
    }

    public static void listWaypoints() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        if (waypoints.isEmpty()) {
            Component message = Component.literal("No waypoints saved").withStyle(ChatFormatting.GRAY);
            client.player.sendSystemMessage(message);
            return;
        }

        Component header = Component.literal("")
                .append(Component.literal("════════════").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("\n"))
                .append(Component.literal("WAYPOINTS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\n"))
                .append(Component.literal("════════════").withStyle(ChatFormatting.GOLD));

        client.player.sendSystemMessage(header);

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint wp = waypoints.get(i);
            String dimName = wp.dimension.substring(wp.dimension.lastIndexOf(":") + 1);
            double distance = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ()).distanceTo(wp.position);

            Component teleportButton = Component.literal("TP")
                    .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.GREEN)
                            .withBold(true));

            Component deleteButton = Component.literal("DEL")
                    .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.RED)
                            .withBold(true));

            Component renameButton = Component.literal("RENAME")
                    .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.YELLOW)
                            .withBold(true));

            Component waypointInfo = Component.literal("")
                    .append(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wp.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .append(Component.literal(" [" + dimName + "]").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" • ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format("%.0f, %.0f, %.0f", wp.position.x, wp.position.y, wp.position.z)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.0fm", distance)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n   "))
                    .append(teleportButton)
                    .append(Component.literal("  "))
                    .append(renameButton)
                    .append(Component.literal("  "))
                    .append(deleteButton);

            client.player.sendSystemMessage(waypointInfo);

            if (i < waypoints.size() - 1) {
                client.player.sendSystemMessage(Component.literal("------------").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        Component footer = Component.literal("════════════").withStyle(ChatFormatting.GOLD);

        client.player.sendSystemMessage(footer);
    }

    public static void teleportToWaypoint(String waypointId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendSystemMessage(Component.literal("❌ Waypoint not found!").withStyle(ChatFormatting.RED));
            return;
        }

        // Check if in creative mode or has permissions
        if (!client.player.getAbilities().instabuild) {
            client.player.sendSystemMessage(Component.literal("❌ You need creative mode to teleport!").withStyle(ChatFormatting.RED));
            return;
        }

        // Teleport
        client.player.setPos(waypoint.position.x, waypoint.position.y, waypoint.position.z);

        // Success message
        Component message = Component.literal("")
                .append(Component.literal("🚀 ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("Teleported to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(waypoint.name).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("!").withStyle(ChatFormatting.GRAY));

        client.player.sendOverlayMessage(message); // Action bar
    }

    public static void deleteWaypoint(String waypointId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendSystemMessage(Component.literal("❌ Waypoint not found!").withStyle(ChatFormatting.RED));
            return;
        }

        waypoints.removeIf(wp -> wp.id.equals(waypointId));
        saveWaypoints();

        // Success message
        Component message = Component.literal("Deleted waypoint ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(waypoint.name).withStyle(ChatFormatting.AQUA));

        client.player.sendSystemMessage(message);
    }

    public static void renameWaypoint(String waypointId, String newName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendSystemMessage(Component.literal("Waypoint not found!").withStyle(ChatFormatting.RED));
            return;
        }

        String oldName = waypoint.name;
        waypoint.name = newName.trim();
        saveWaypoints();

        // Success message
        Component message = Component.literal("")
                .append(Component.literal("✏️ ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Renamed ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(oldName).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(waypoint.name).withStyle(ChatFormatting.AQUA));

        client.player.sendSystemMessage(message);
    }

    private static Waypoint findWaypointById(String id) {
        return waypoints.stream()
                .filter(wp -> wp.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
