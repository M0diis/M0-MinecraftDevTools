package me.m0dii.modules.portals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.waypoints.WaypointHandler;
import me.m0dii.utils.ConfigFileSafety;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Remembers nether portals you have been near (per world) and, when you stand by one, predicts where it leads:
 * the scaled destination point, the known portal the game would use there, or that a new portal would be built.
 * It also warns when the way back would come out at a different portal.
 * <p>
 * The client only knows portals it has seen, so a prediction of "no known portal" can be wrong if a portal
 * exists there that you never visited.
 */
public class PortalLinkerModule extends Module {
    public static final PortalLinkerModule INSTANCE = new PortalLinkerModule();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-portals.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCAN_RADIUS = 8;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int NEAR_DISTANCE = 6;
    private static final int MAX_PORTAL_BLOCKS = 23 * 23;

    private final List<KnownPortal> portals = new ArrayList<>();
    private int tickCounter;
    private List<String> hudLines = List.of();

    private PortalLinkerModule() {
        super("portal_linker", "Portal Linker", false);
    }

    @Override
    public void register() {
        load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled() || isClientNull()) {
                hudLines = List.of();
                return;
            }
            if (++tickCounter % SCAN_INTERVAL_TICKS == 0) {
                scanNearby(client);
                hudLines = describeNearest(client);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "portal_linker"),
                this::renderHud);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("portal")
                        .executes(ctx -> {
                            Minecraft client = Minecraft.getInstance();
                            if (!isClientNull()) {
                                scanNearby(client);
                            }
                            List<String> lines = isClientNull() ? List.of() : describeNearest(client);
                            if (lines.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("[Portals] Stand next to a nether portal. Known portals in this world: "
                                        + countInWorld()));
                            }
                            for (String line : lines) {
                                ctx.getSource().sendFeedback(Component.literal("[Portals] " + line));
                            }
                            return 1;
                        })
                        .then(ClientCommands.literal("forget").executes(ctx -> {
                            int removed = forgetWorld();
                            ctx.getSource().sendFeedback(Component.literal("[Portals] Forgot " + removed + " portal(s) in this world."));
                            return 1;
                        }))));
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        List<String> lines = hudLines;
        if (!isEnabled() || lines.isEmpty() || client.player == null) {
            return;
        }
        int y = 54;
        for (String line : lines) {
            int width = client.font.width(line);
            int x = (client.getWindow().getGuiScaledWidth() - width) / 2;
            context.fill(x - 4, y - 2, x + width + 4, y + 10, 0x80000000);
            int color = line.startsWith("⚠") ? 0xFFFFB74D : 0xFFD1A6FF;
            context.text(client.font, line, x, y, color, true);
            y += 12;
        }
    }

    // ---- scanning --------------------------------------------------------------------------------------------

    private void scanNearby(Minecraft client) {
        String world = WaypointHandler.currentWorldKey();
        String dimension = client.level.dimension().identifier().toString();
        if (world == null || PortalLinkMath.destination(dimension) == null) {
            return;
        }
        BlockPos center = client.player.blockPosition();
        Set<BlockPos> visited = new HashSet<>();
        List<KnownPortal> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!visited.contains(cursor) && isPortal(client, cursor)) {
                        KnownPortal portal = floodFill(client, cursor.immutable(), visited, world, dimension);
                        if (portal != null) {
                            found.add(portal);
                        }
                    }
                }
            }
        }

        boolean changed = false;
        // Forget remembered portals inside the scanned area that are gone.
        List<KnownPortal> stale = new ArrayList<>();
        for (KnownPortal known : portals) {
            if (known.sameWorldAndDimension(world, dimension) && insideScan(known, center)
                    && found.stream().noneMatch(known::touches)) {
                stale.add(known);
            }
        }
        if (!stale.isEmpty()) {
            portals.removeAll(stale);
            changed = true;
        }
        for (KnownPortal portal : found) {
            portals.removeIf(portal::touches);
            portals.add(portal);
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    private static boolean insideScan(KnownPortal portal, BlockPos center) {
        return portal.minX >= center.getX() - SCAN_RADIUS && portal.maxX <= center.getX() + SCAN_RADIUS
                && portal.minY >= center.getY() - SCAN_RADIUS && portal.maxY <= center.getY() + SCAN_RADIUS
                && portal.minZ >= center.getZ() - SCAN_RADIUS && portal.maxZ <= center.getZ() + SCAN_RADIUS;
    }

    private static boolean isPortal(Minecraft client, BlockPos pos) {
        return client.level.getBlockState(pos).is(Blocks.NETHER_PORTAL);
    }

    private static KnownPortal floodFill(Minecraft client, BlockPos start, Set<BlockPos> visited, String world, String dimension) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int minX = start.getX();
        int minY = start.getY();
        int minZ = start.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;
        int count = 0;
        while (!queue.isEmpty() && count < MAX_PORTAL_BLOCKS) {
            BlockPos pos = queue.poll();
            count++;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            for (BlockPos next : new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()}) {
                if (!visited.contains(next) && isPortal(client, next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return new KnownPortal(world, dimension, minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ---- prediction -----------------------------------------------------------------------------------------

    private List<String> describeNearest(Minecraft client) {
        String world = WaypointHandler.currentWorldKey();
        String dimension = client.level.dimension().identifier().toString();
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        KnownPortal here = null;
        double best = (double) NEAR_DISTANCE * NEAR_DISTANCE;
        for (KnownPortal portal : portals) {
            if (portal.sameWorldAndDimension(world, dimension)) {
                double distance = portal.distanceSq(px, py, pz);
                if (distance <= best) {
                    best = distance;
                    here = portal;
                }
            }
        }
        if (here == null) {
            return List.of();
        }

        PortalLinkMath.Target target = PortalLinkMath.targetOf(here);
        if (target == null) {
            return List.of();
        }
        String destinationName = shortName(target.dimension());
        List<String> lines = new ArrayList<>();
        lines.add(String.format(java.util.Locale.ROOT, "Portal %s → %s around %.0f %.0f %.0f",
                here.describe(), destinationName, target.x(), target.y(), target.z()));

        KnownPortal destination = PortalLinkMath.pick(portals, world, target);
        if (destination == null) {
            lines.add("No known portal within " + PortalLinkMath.searchRadius(target.dimension())
                    + " blocks there: the game will use one you haven't visited or build a new one.");
            return lines;
        }
        lines.add("Links to known portal at " + destination.describe() + " (" + destinationName + ")");

        KnownPortal back = PortalLinkMath.pick(portals, world, PortalLinkMath.targetOf(destination));
        if (back != null && back != here && !back.touches(here)) {
            lines.add("⚠ The way back comes out at " + back.describe() + ", not this portal.");
        }
        return lines;
    }

    private static String shortName(String dimension) {
        return dimension.substring(dimension.indexOf(':') + 1);
    }

    private int countInWorld() {
        String world = WaypointHandler.currentWorldKey();
        return (int) portals.stream().filter(p -> java.util.Objects.equals(p.world, world)).count();
    }

    private int forgetWorld() {
        String world = WaypointHandler.currentWorldKey();
        int before = portals.size();
        portals.removeIf(p -> java.util.Objects.equals(p.world, world));
        save();
        return before - portals.size();
    }

    // ---- storage --------------------------------------------------------------------------------------------

    private void load() {
        portals.clear();
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            List<KnownPortal> loaded = GSON.fromJson(Files.readString(FILE), new TypeToken<List<KnownPortal>>() {
            }.getType());
            if (loaded != null) {
                loaded.stream().filter(p -> p != null && p.world != null && p.dimension != null).forEach(portals::add);
            }
        } catch (IOException | RuntimeException e) {
            ConfigFileSafety.backupUnreadable(FILE, e);
        }
    }

    private void save() {
        try {
            ConfigFileSafety.writeAtomically(FILE, GSON.toJson(portals));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save known portals: {}", e.getMessage());
        }
    }
}
