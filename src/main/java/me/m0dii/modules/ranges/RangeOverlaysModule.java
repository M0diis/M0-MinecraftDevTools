package me.m0dii.modules.ranges;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.state.ModuleStateStore;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Range overlays: beacon and conduit reach, the spawn chunk area, the random-tick distance and the mob
 * spawn/despawn spheres. The sphere and random-tick rings follow you, or a point pinned with {@code /ranges pin}
 * (your AFK spot).
 */
public class RangeOverlaysModule extends Module implements PersistentSettings {
    public static final RangeOverlaysModule INSTANCE = new RangeOverlaysModule();

    public enum Layer {
        BEACONS("beacons", "Beacon ranges"),
        CONDUITS("conduits", "Conduit ranges"),
        SPAWN_CHUNKS("spawnchunks", "Spawn chunks"),
        RANDOM_TICK("randomtick", "Random tick range (128)"),
        SPAWN_SPHERES("spawnspheres", "Mob spawn spheres (24/32/128)");

        final String id;
        final String label;

        Layer(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private record Found(BlockPos pos, int range, String label) {
    }

    private static final int SCAN_CHUNK_RADIUS = 10;
    private static final int SCAN_INTERVAL_TICKS = 40;

    private final Map<Layer, Boolean> layers = new LinkedHashMap<>();
    private final List<Found> beacons = new ArrayList<>();
    private final List<Found> conduits = new ArrayList<>();
    private @Nullable Vec3 pinned;
    private int spawnChunkRadius = 2;
    private int scanTicker;

    private RangeOverlaysModule() {
        super("range_overlays", "Range Overlays", false);
        for (Layer layer : Layer.values()) {
            layers.put(layer, layer == Layer.BEACONS || layer == Layer.CONDUITS || layer == Layer.SPAWN_SPHERES);
        }
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            beacons.clear();
            conduits.clear();
            pinned = null;
        });
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || isClientNull() || getClient().level == null) {
                return;
            }
            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }
            if (consumers == null || context.gameRenderer() == null) {
                return;
            }
            render(consumers.getBuffer(RenderTypes.LINES), context.gameRenderer().mainCamera().position());
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommands.literal("ranges").executes(ctx -> reply(ctx.getSource(), status()));
            for (Layer layer : Layer.values()) {
                root.then(ClientCommands.literal(layer.id)
                        .executes(ctx -> reply(ctx.getSource(), toggle(layer, !layers.get(layer))))
                        .then(ClientCommands.literal("on").executes(ctx -> reply(ctx.getSource(), toggle(layer, true))))
                        .then(ClientCommands.literal("off").executes(ctx -> reply(ctx.getSource(), toggle(layer, false)))));
            }
            root.then(ClientCommands.literal("pin").executes(ctx -> {
                if (getClient().player == null) {
                    return 0;
                }
                pinned = getClient().player.position();
                return reply(ctx.getSource(), "Pinned the spawn spheres and random-tick ring at "
                        + BlockPos.containing(pinned).toShortString() + ".");
            }));
            root.then(ClientCommands.literal("unpin").executes(ctx -> {
                pinned = null;
                return reply(ctx.getSource(), "Rings follow you again.");
            }));
            root.then(ClientCommands.literal("spawnradius")
                    .then(ClientCommands.argument("chunks", IntegerArgumentType.integer(0, 32)).executes(ctx -> {
                        spawnChunkRadius = IntegerArgumentType.getInteger(ctx, "chunks");
                        ModuleStateStore.onSettingsChanged(this);
                        return reply(ctx.getSource(), "Spawn chunk radius " + spawnChunkRadius
                                + " (match the server's spawnChunkRadius gamerule).");
                    })));
            dispatcher.register(root);
        });
    }

    private static int reply(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Ranges] " + text));
        return 1;
    }

    private String toggle(Layer layer, boolean on) {
        layers.put(layer, on);
        if (on && !isEnabled()) {
            setEnabled(true);
        }
        ModuleStateStore.onSettingsChanged(this);
        return layer.label + ": " + (on ? "on" : "off");
    }

    private String status() {
        StringBuilder out = new StringBuilder(isEnabled() ? "On." : "Off (turn on in ClickHUD or with /ranges <layer> on).");
        for (Layer layer : Layer.values()) {
            out.append(" ").append(layer.id).append('=').append(layers.get(layer) ? "on" : "off");
        }
        out.append(". Found ").append(beacons.size()).append(" beacon(s), ").append(conduits.size()).append(" conduit(s).");
        return out.toString();
    }

    // ---- scanning ----------------------------------------------------------------------------------------

    private void tick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            return;
        }
        if (++scanTicker < SCAN_INTERVAL_TICKS && scanTicker > 1) {
            return;
        }
        scanTicker = 1;
        beacons.clear();
        conduits.clear();
        if (!layers.get(Layer.BEACONS) && !layers.get(Layer.CONDUITS)) {
            return;
        }
        ClientLevel level = client.level;
        int pcx = client.player.blockPosition().getX() >> 4;
        int pcz = client.player.blockPosition().getZ() >> 4;
        for (int cx = pcx - SCAN_CHUNK_RADIUS; cx <= pcx + SCAN_CHUNK_RADIUS; cx++) {
            for (int cz = pcz - SCAN_CHUNK_RADIUS; cz <= pcz + SCAN_CHUNK_RADIUS; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (blockEntity instanceof BeaconBlockEntity && layers.get(Layer.BEACONS)) {
                        addBeacon(level, blockEntity.getBlockPos());
                    } else if (blockEntity instanceof ConduitBlockEntity && layers.get(Layer.CONDUITS)) {
                        addConduit(level, blockEntity.getBlockPos());
                    }
                }
            }
        }
    }

    private void addBeacon(ClientLevel level, BlockPos pos) {
        int levels = RangeMath.beaconLevels((dx, dy, dz) ->
                level.getBlockState(pos.offset(dx, dy, dz)).is(BlockTags.BEACON_BASE_BLOCKS));
        int range = RangeMath.beaconRange(levels);
        if (range > 0) {
            beacons.add(new Found(pos, range, "Beacon L" + levels + " · " + range));
        }
    }

    private void addConduit(ClientLevel level, BlockPos pos) {
        int frames = 0;
        for (int[] offset : RangeMath.conduitFrameOffsets()) {
            BlockState state = level.getBlockState(pos.offset(offset[0], offset[1], offset[2]));
            if (state.is(Blocks.PRISMARINE) || state.is(Blocks.PRISMARINE_BRICKS) || state.is(Blocks.SEA_LANTERN)
                    || state.is(Blocks.DARK_PRISMARINE)) {
                frames++;
            }
        }
        int range = RangeMath.conduitRange(frames);
        if (range > 0) {
            conduits.add(new Found(pos, range, "Conduit " + frames + "/42 · " + range));
        }
    }

    // ---- rendering ---------------------------------------------------------------------------------------

    private void render(VertexConsumer lines, Vec3 camera) {
        Minecraft client = getClient();
        if (client.player == null || client.level == null) {
            return;
        }
        double eyeY = client.player.getY();
        int minY = client.level.getMinY();
        int maxY = client.level.getMaxY() + 1;

        if (layers.get(Layer.BEACONS)) {
            for (Found beacon : beacons) {
                BlockPos p = beacon.pos();
                int r = beacon.range();
                double x1 = p.getX() - r;
                double x2 = p.getX() + 1 + r;
                double z1 = p.getZ() - r;
                double z2 = p.getZ() + 1 + r;
                double y1 = Math.max(minY, p.getY() - r);
                double y2 = maxY;
                verticalCorners(lines, camera, x1, z1, x2, z2, y1, y2, 1.0f, 0.85f, 0.2f);
                rectangle(lines, camera, x1, z1, x2, z2, Math.clamp(eyeY, y1, y2), 1.0f, 0.85f, 0.2f);
            }
        }
        if (layers.get(Layer.CONDUITS)) {
            for (Found conduit : conduits) {
                Vec3 c = Vec3.atCenterOf(conduit.pos());
                sphere(lines, camera, c, conduit.range(), 0.3f, 0.9f, 1.0f);
            }
        }
        if (layers.get(Layer.SPAWN_CHUNKS)) {
            BlockPos spawn = worldSpawn(client.level);
            if (spawn != null) {
                int cx = spawn.getX() >> 4;
                int cz = spawn.getZ() >> 4;
                double x1 = (cx - spawnChunkRadius) * 16.0;
                double z1 = (cz - spawnChunkRadius) * 16.0;
                double x2 = (cx + spawnChunkRadius + 1) * 16.0;
                double z2 = (cz + spawnChunkRadius + 1) * 16.0;
                verticalCorners(lines, camera, x1, z1, x2, z2, minY, maxY, 0.4f, 1.0f, 0.4f);
                rectangle(lines, camera, x1, z1, x2, z2, eyeY, 0.4f, 1.0f, 0.4f);
            }
        }
        Vec3 anchor = pinned != null ? pinned : client.player.position();
        if (layers.get(Layer.RANDOM_TICK)) {
            ring(lines, camera, anchor.x, anchor.y + 0.05, anchor.z, RangeMath.INSTANT_DESPAWN_DISTANCE, 0.8f, 0.5f, 1.0f);
        }
        if (layers.get(Layer.SPAWN_SPHERES)) {
            Vec3 eye = pinned != null ? pinned.add(0, client.player.getEyeHeight(), 0) : client.player.getEyePosition();
            sphere(lines, camera, eye, RangeMath.SPAWN_MIN_DISTANCE, 0.3f, 1.0f, 0.3f);
            sphere(lines, camera, eye, RangeMath.RANDOM_DESPAWN_DISTANCE, 1.0f, 0.75f, 0.2f);
            sphere(lines, camera, eye, RangeMath.INSTANT_DESPAWN_DISTANCE, 1.0f, 0.3f, 0.3f);
            if (pinned != null) {
                DrawUtil.drawOutlinedBoxSafe(lines, pinned.x - 0.3 - camera.x, pinned.y - camera.y, pinned.z - 0.3 - camera.z,
                        pinned.x + 0.3 - camera.x, pinned.y + 1.8 - camera.y, pinned.z + 0.3 - camera.z, 1f, 1f, 1f, 0.9f, 2f);
            }
        }
    }

    private static void verticalCorners(VertexConsumer lines, Vec3 cam, double x1, double z1, double x2, double z2,
                                        double y1, double y2, float r, float g, float b) {
        double[][] corners = {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}};
        for (double[] corner : corners) {
            DrawUtil.drawLineSafe(lines, corner[0] - cam.x, y1 - cam.y, corner[1] - cam.z,
                    corner[0] - cam.x, y2 - cam.y, corner[1] - cam.z, r, g, b, 0.8f, 2.0f);
        }
    }

    private static void rectangle(VertexConsumer lines, Vec3 cam, double x1, double z1, double x2, double z2, double y,
                                  float r, float g, float b) {
        double yy = y - cam.y;
        DrawUtil.drawLineSafe(lines, x1 - cam.x, yy, z1 - cam.z, x2 - cam.x, yy, z1 - cam.z, r, g, b, 0.8f, 2.0f);
        DrawUtil.drawLineSafe(lines, x2 - cam.x, yy, z1 - cam.z, x2 - cam.x, yy, z2 - cam.z, r, g, b, 0.8f, 2.0f);
        DrawUtil.drawLineSafe(lines, x2 - cam.x, yy, z2 - cam.z, x1 - cam.x, yy, z2 - cam.z, r, g, b, 0.8f, 2.0f);
        DrawUtil.drawLineSafe(lines, x1 - cam.x, yy, z2 - cam.z, x1 - cam.x, yy, z1 - cam.z, r, g, b, 0.8f, 2.0f);
    }

    private static void ring(VertexConsumer lines, Vec3 cam, double cx, double y, double cz, double radius,
                             float r, float g, float b) {
        List<double[]> points = RangeMath.circle(cx, cz, radius, RangeMath.segmentsFor(radius));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] c = points.get(i);
            DrawUtil.drawLineSafe(lines, a[0] - cam.x, y - cam.y, a[1] - cam.z, c[0] - cam.x, y - cam.y, c[1] - cam.z,
                    r, g, b, 0.85f, 2.0f);
        }
    }

    /** A sphere as three great circles (horizontal, and two upright ones). */
    private static void sphere(VertexConsumer lines, Vec3 cam, Vec3 c, double radius, float r, float g, float b) {
        ring(lines, cam, c.x, c.y, c.z, radius, r, g, b);
        List<double[]> points = RangeMath.circle(0, 0, radius, RangeMath.segmentsFor(radius));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] d = points.get(i);
            // x-y plane
            DrawUtil.drawLineSafe(lines, c.x + a[0] - cam.x, c.y + a[1] - cam.y, c.z - cam.z,
                    c.x + d[0] - cam.x, c.y + d[1] - cam.y, c.z - cam.z, r, g, b, 0.6f, 1.5f);
            // z-y plane
            DrawUtil.drawLineSafe(lines, c.x - cam.x, c.y + a[1] - cam.y, c.z + a[0] - cam.z,
                    c.x - cam.x, c.y + d[1] - cam.y, c.z + d[0] - cam.z, r, g, b, 0.6f, 1.5f);
        }
    }

    /** World spawn from the level data; looked up by name so it survives small API renames. */
    private static @Nullable BlockPos worldSpawn(ClientLevel level) {
        Object data = level.getLevelData();
        try {
            Object result = data.getClass().getMethod("getSpawnPos").invoke(data);
            if (result instanceof BlockPos pos) {
                return pos;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // try the newer shape below
        }
        try {
            Object respawn = data.getClass().getMethod("getRespawnData").invoke(data);
            Object pos = respawn == null ? null : respawn.getClass().getMethod("pos").invoke(respawn);
            if (pos instanceof BlockPos blockPos) {
                return blockPos;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // unknown shape
        }
        return null;
    }

    // ---- settings ----------------------------------------------------------------------------------------

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        for (Layer layer : Layer.values()) {
            settings.add(layer.label + ": " + (layers.get(layer) ? "ON" : "OFF"));
        }
        settings.add(pinned == null ? "Pin rings here" : String.format(Locale.ROOT, "Unpin rings (%.0f %.0f %.0f)", pinned.x, pinned.y, pinned.z));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            toggleEnabled();
            return;
        }
        Layer[] all = Layer.values();
        if (settingIndex - 1 < all.length) {
            Layer layer = all[settingIndex - 1];
            layers.put(layer, !layers.get(layer));
            scanTicker = 0;
            return;
        }
        if (settingIndex == all.length + 1) {
            pinned = pinned != null || getClient().player == null ? null : getClient().player.position();
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        for (Layer layer : Layer.values()) {
            json.addProperty(layer.id, layers.get(layer));
        }
        json.addProperty("spawnChunkRadius", spawnChunkRadius);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        for (Layer layer : Layer.values()) {
            layers.put(layer, PersistentSettings.readBoolean(settings, layer.id, layers.get(layer)));
        }
        spawnChunkRadius = PersistentSettings.readInt(settings, "spawnChunkRadius", spawnChunkRadius, 0, 32);
    }
}
