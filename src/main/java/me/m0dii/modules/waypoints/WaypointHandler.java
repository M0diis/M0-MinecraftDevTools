package me.m0dii.modules.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.m0dii.M0DevTools;
import me.m0dii.utils.ConfigFileSafety;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WaypointHandler {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-waypoints.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Waypoint {
        public String id;
        public String name;
        public Vec3 position;
        public String dimension;
        public long createdTime;
        /**
         * The world/server this waypoint belongs to ({@code mp:<address>} or {@code sp:<world name>}).
         * {@code null} for waypoints saved by older versions; those are shown everywhere until claimed
         * with {@code /waypoint claim}.
         */
        public String world;

        public Waypoint(String id, String name, Vec3 position, String dimension) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.dimension = dimension;
            this.createdTime = System.currentTimeMillis();
        }

        public boolean isUnassigned() {
            return world == null || world.isBlank();
        }
    }

    private static List<Waypoint> waypoints = new ArrayList<>();

    public static void register() {
        loadWaypoints();
    }

    /** Waypoints for the world you are in (plus unassigned legacy ones). Edits to the entries are saved by {@link #saveWaypoints()}. */
    public static List<Waypoint> getWaypoints() {
        String world = currentWorldKey();
        List<Waypoint> visible = new ArrayList<>();
        for (Waypoint waypoint : waypoints) {
            if (isVisibleIn(waypoint, world)) {
                visible.add(waypoint);
            }
        }
        return visible;
    }

    /** Every saved waypoint, across all worlds and servers. */
    public static List<Waypoint> getAllWaypoints() {
        return List.copyOf(waypoints);
    }

    static boolean isVisibleIn(Waypoint waypoint, String worldKey) {
        return waypoint != null && (waypoint.isUnassigned() || Objects.equals(waypoint.world, worldKey));
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
            List<Waypoint> valid = new ArrayList<>();
            if (loadedWaypoints != null) {
                for (Waypoint waypoint : loadedWaypoints) {
                    if (waypoint != null && waypoint.id != null && waypoint.position != null) {
                        if (waypoint.name == null) {
                            waypoint.name = "Waypoint";
                        }
                        if (waypoint.dimension == null) {
                            waypoint.dimension = "minecraft:overworld";
                        }
                        valid.add(waypoint);
                    }
                }
            }
            waypoints = valid;
        } catch (IOException | RuntimeException e) {
            // RuntimeException covers malformed JSON. Keep a copy of the file before anything overwrites it.
            ConfigFileSafety.backupUnreadable(CONFIG_FILE, e);
            waypoints = new ArrayList<>();
        }
    }

    public static void saveWaypoints() {
        try {
            ConfigFileSafety.writeAtomically(CONFIG_FILE, GSON.toJson(waypoints));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save waypoints: {}", e.getMessage());
        }
    }

    /**
     * Identifies the current world: the server address in multiplayer, the world name in singleplayer.
     * Returns {@code null} when not in a world.
     */
    public static String currentWorldKey() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        // Called every frame by the renderer; the answer only changes when the level object changes.
        if (client.level == cachedKeyLevel && cachedKey != null) {
            return cachedKey;
        }
        cachedKey = computeWorldKey(client);
        cachedKeyLevel = client.level;
        return cachedKey;
    }

    private static Object cachedKeyLevel;
    private static String cachedKey;

    private static String computeWorldKey(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            String levelName = singleplayerLevelName(client);
            return "sp:" + (levelName == null || levelName.isBlank() ? "world" : levelName);
        }
        Object serverData = client.getCurrentServer();
        String address = serverData == null ? null : readStringField(serverData, "ip");
        if (address == null || address.isBlank()) {
            return "mp:unknown";
        }
        return "mp:" + address.trim().toLowerCase(Locale.ROOT);
    }

    private static String singleplayerLevelName(Minecraft client) {
        try {
            Object server = client.getSingleplayerServer();
            Method getWorldData = server.getClass().getMethod("getWorldData");
            Object worldData = getWorldData.invoke(server);
            Method getLevelName = worldData.getClass().getMethod("getLevelName");
            return String.valueOf(getLevelName.invoke(worldData));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static String readStringField(Object target, String fieldName) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                var field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                return value == null ? null : value.toString();
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    public static void addWaypoint() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Vec3 pos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        String dimension = client.level.dimension().identifier().toString();
        String id = UUID.randomUUID().toString().substring(0, 8);
        String name = "Waypoint " + (getWaypoints().size() + 1);

        Waypoint waypoint = new Waypoint(id, name, pos, dimension);
        waypoint.world = currentWorldKey();
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

    /** Assigns all legacy (unassigned) waypoints to the current world. */
    public static void claimUnassignedWaypoints() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        String world = currentWorldKey();
        int claimed = 0;
        for (Waypoint waypoint : waypoints) {
            if (waypoint.isUnassigned()) {
                waypoint.world = world;
                claimed++;
            }
        }
        if (claimed > 0) {
            saveWaypoints();
        }
        client.player.sendSystemMessage(Component.literal(claimed == 0
                        ? "No unassigned waypoints."
                        : "Moved " + claimed + " older waypoint(s) to this world.")
                .withStyle(claimed == 0 ? ChatFormatting.GRAY : ChatFormatting.GREEN));
    }

    public static void listWaypoints() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        List<Waypoint> visible = getWaypoints();
        if (visible.isEmpty()) {
            Component message = Component.literal("No waypoints saved for this world").withStyle(ChatFormatting.GRAY);
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

        boolean anyUnassigned = false;
        for (int i = 0; i < visible.size(); i++) {
            Waypoint wp = visible.get(i);
            anyUnassigned |= wp.isUnassigned();
            String dimName = wp.dimension.substring(wp.dimension.lastIndexOf(":") + 1);
            double distance = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ()).distanceTo(wp.position);

            Component teleportButton = actionButton("TP", ChatFormatting.GREEN,
                    new ClickEvent.RunCommand("/waypoint tp " + wp.id),
                    "Teleport to " + wp.name + " (needs /tp permission, or creative in singleplayer)");

            Component deleteButton = actionButton("DEL", ChatFormatting.RED,
                    new ClickEvent.SuggestCommand("/waypoint delete " + wp.id),
                    "Puts the delete command in the chat box; press Enter to confirm");

            Component trackButton = actionButton("TRACK", ChatFormatting.AQUA,
                    new ClickEvent.RunCommand("/waypoint track " + wp.id),
                    "Show an arrow to this waypoint on the HUD");

            Component renameButton = actionButton("RENAME", ChatFormatting.YELLOW,
                    new ClickEvent.SuggestCommand("/waypoint rename " + wp.id + " "),
                    "Type the new name after the command");

            Component waypointInfo = Component.literal("")
                    .append(Component.literal((i + 1) + ". ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(wp.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .append(Component.literal(" [" + dimName + "]").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(wp.isUnassigned() ? " [any world]" : "").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" • ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format("%.0f, %.0f, %.0f", wp.position.x, wp.position.y, wp.position.z)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.0fm", distance)).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n   "))
                    .append(teleportButton)
                    .append(Component.literal("  "))
                    .append(trackButton)
                    .append(Component.literal("  "))
                    .append(renameButton)
                    .append(Component.literal("  "))
                    .append(deleteButton);

            client.player.sendSystemMessage(waypointInfo);

            if (i < visible.size() - 1) {
                client.player.sendSystemMessage(Component.literal("------------").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (anyUnassigned) {
            client.player.sendSystemMessage(Component.literal("Waypoints marked [any world] were saved by an older version. ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(actionButton("[Move them to this world]", ChatFormatting.AQUA,
                            new ClickEvent.SuggestCommand("/waypoint claim"),
                            "Assigns every [any world] waypoint to the world you are in")));
        }

        Component footer = Component.literal("════════════").withStyle(ChatFormatting.GOLD);

        client.player.sendSystemMessage(footer);
    }

    private static Component actionButton(String label, ChatFormatting color, ClickEvent click, String hover) {
        MutableComponent button = Component.literal(label);
        return button.setStyle(Style.EMPTY
                .withColor(color)
                .withBold(true)
                .withClickEvent(click)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    public static void teleportToWaypoint(String waypointId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Waypoint waypoint = findWaypointById(waypointId);
        if (waypoint == null) {
            client.player.sendSystemMessage(Component.literal("❌ Waypoint not found!").withStyle(ChatFormatting.RED));
            return;
        }

        String here = client.level.dimension().identifier().toString();
        boolean sameDimension = waypoint.dimension.equals(here);
        String coords = String.format(Locale.ROOT, "%.2f %.2f %.2f", waypoint.position.x, waypoint.position.y, waypoint.position.z);

        // Prefer the server's /tp when this player may use it: it works on servers and across dimensions.
        if (serverHasCommand(client, "tp") && (sameDimension || serverHasCommand(client, "execute"))) {
            String command = sameDimension
                    ? "tp @s " + coords
                    : "execute in " + waypoint.dimension + " run tp @s " + coords;
            client.player.connection.sendCommand(command);
            client.player.sendOverlayMessage(Component.literal("🚀 Teleporting to ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(waypoint.name).withStyle(ChatFormatting.AQUA)));
            return;
        }

        if (!sameDimension) {
            client.player.sendSystemMessage(Component.literal("❌ That waypoint is in " + waypoint.dimension
                    + " and you don't have /tp here.").withStyle(ChatFormatting.RED));
            return;
        }

        // Singleplayer without cheats: moving the client is accepted by the integrated server.
        if (client.getSingleplayerServer() != null && client.player.getAbilities().instabuild) {
            client.player.setPos(waypoint.position.x, waypoint.position.y, waypoint.position.z);
            client.player.sendOverlayMessage(Component.literal("🚀 Teleported to ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(waypoint.name).withStyle(ChatFormatting.AQUA)));
            return;
        }

        client.player.sendSystemMessage(Component.literal("❌ You don't have permission to use /tp here. Use ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("/waypoint track " + waypoint.id).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to follow it instead.").withStyle(ChatFormatting.RED)));
    }

    /** True when the server sent this player a command tree containing {@code name}, i.e. they may run it. */
    private static boolean serverHasCommand(Minecraft client, String name) {
        try {
            return client.getConnection() != null
                    && client.getConnection().getCommands() != null
                    && client.getConnection().getCommands().getRoot().getChild(name) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ---- tracking & other dimensions --------------------------------------------------------------------------

    private static String trackedId;

    /** The waypoint shown in the HUD arrow, if any (and still visible in this world). */
    public static Waypoint getTracked() {
        if (trackedId == null) {
            return null;
        }
        return findWaypointById(trackedId);
    }

    public static void track(String idOrNearest) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        if ("off".equalsIgnoreCase(idOrNearest) || "none".equalsIgnoreCase(idOrNearest)) {
            trackedId = null;
            client.player.sendOverlayMessage(Component.literal("Stopped tracking waypoints"));
            return;
        }
        Waypoint target = "nearest".equalsIgnoreCase(idOrNearest) ? nearestVisible() : findWaypointById(idOrNearest);
        if (target == null) {
            client.player.sendSystemMessage(Component.literal("❌ No such waypoint here.").withStyle(ChatFormatting.RED));
            return;
        }
        trackedId = target.id;
        client.player.sendOverlayMessage(Component.literal("Tracking ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(target.name).withStyle(ChatFormatting.AQUA)));
    }

    private static Waypoint nearestVisible() {
        Minecraft client = Minecraft.getInstance();
        String here = client.level.dimension().identifier().toString();
        Vec3 me = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        Waypoint best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Waypoint waypoint : getWaypoints()) {
            Vec3 pos = positionIn(waypoint, here);
            if (pos == null) {
                continue;
            }
            double distance = pos.distanceToSqr(me);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = waypoint;
            }
        }
        return best;
    }

    /**
     * Where the waypoint is from the point of view of {@code dimension}: its own position in the same dimension,
     * the matching nether/overworld coordinates across the nether link, or {@code null}.
     */
    public static Vec3 positionIn(Waypoint waypoint, String dimension) {
        double scale = WaypointMath.scale(waypoint.dimension, dimension);
        if (scale == 0.0) {
            return null;
        }
        if (scale == 1.0) {
            return waypoint.position;
        }
        return new Vec3(waypoint.position.x * scale, waypoint.position.y, waypoint.position.z * scale);
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

        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) {
            client.player.sendSystemMessage(Component.literal("Type a name after the command.").withStyle(ChatFormatting.RED));
            return;
        }

        String oldName = waypoint.name;
        waypoint.name = trimmed;
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

    /** Looks a waypoint up among the ones visible in the current world, so ids from other worlds can't be touched. */
    private static Waypoint findWaypointById(String id) {
        String world = currentWorldKey();
        return waypoints.stream()
                .filter(wp -> wp.id.equals(id) && isVisibleIn(wp, world))
                .findFirst()
                .orElse(null);
    }
}
