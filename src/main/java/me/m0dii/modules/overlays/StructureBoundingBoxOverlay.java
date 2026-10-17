package me.m0dii.modules.overlays;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.*;

/**
 * Outlines generated structures (villages, monuments, strongholds, ...) and their pieces.
 * <p>
 * Structure data only exists on the server; the vanilla protocol never sends it to clients. In singleplayer it is
 * read from the integrated server (on the server thread, every couple of seconds). On remote servers there is
 * nothing to read, so the module says so instead of drawing nothing silently.
 */
public class StructureBoundingBoxOverlay extends Module implements PersistentSettings {

    public static final StructureBoundingBoxOverlay INSTANCE = new StructureBoundingBoxOverlay();

    private static final int REFRESH_TICKS = 40;
    private static final int[] RADII = {4, 6, 8, 12, 16};

    /** One structure with its overall box and the boxes of its pieces, in world coordinates. */
    public record StructureBoxes(String name, BoundingBox bounds, List<BoundingBox> pieces, int color) {
    }

    private volatile List<StructureBoxes> structures = List.of();
    private volatile boolean refreshPending;
    private int ticksUntilRefresh;
    private boolean warnedNoServer;

    private int chunkRadius = 8;
    private boolean showPieces = true;
    private boolean showLabels = true;

    private StructureBoundingBoxOverlay() {
        super("structure_bounding_box_overlay", "Structure Bounding Box Overlay", false);
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        OverlayRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    @Override
    protected void onEnable() {
        ticksUntilRefresh = 0;
        warnedNoServer = false;
    }

    @Override
    protected void onDisable() {
        structures = List.of();
    }

    public List<StructureBoxes> structures() {
        return structures;
    }

    private void tick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) {
            return;
        }
        if (--ticksUntilRefresh > 0 || refreshPending) {
            return;
        }
        ticksUntilRefresh = REFRESH_TICKS;

        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            structures = List.of();
            if (!warnedNoServer) {
                warnedNoServer = true;
                client.player.sendOverlayMessage(Component.literal(
                        "Structure boxes need singleplayer: servers don't send structure data to clients."));
            }
            return;
        }

        var dimension = client.level.dimension();
        ChunkPos center = ChunkPos.containing(client.player.blockPosition());
        int radius = chunkRadius;
        boolean pieces = showPieces;
        refreshPending = true;
        server.execute(() -> {
            try {
                ServerLevel level = server.getLevel(dimension);
                structures = level == null ? List.of() : collect(level, center, radius, pieces);
            } finally {
                refreshPending = false;
            }
        });
    }

    /** Runs on the server thread. Only looks at chunks that are already loaded; never loads or generates any. */
    private static List<StructureBoxes> collect(ServerLevel level, ChunkPos center, int radius, boolean withPieces) {
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Set<Long> seenStarts = new HashSet<>();
        List<StructureBoxes> result = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(center.x() + dx, center.z() + dz);
                if (chunk == null) {
                    continue;
                }
                // Every chunk a structure touches holds a reference to the chunk where that structure starts.
                for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
                    Structure structure = entry.getKey();
                    for (long startChunkKey : entry.getValue()) {
                        long dedupeKey = startChunkKey * 31 + System.identityHashCode(structure);
                        if (!seenStarts.add(dedupeKey)) {
                            continue;
                        }
                        ChunkPos startPos = ChunkPos.unpack(startChunkKey);
                        ChunkAccess startChunk = level.getChunkSource().getChunk(startPos.x(), startPos.z(), ChunkStatus.STRUCTURE_STARTS, false);
                        if (startChunk == null) {
                            continue;
                        }
                        StructureStart start = startChunk.getStartForStructure(structure);
                        if (start == null || !start.isValid()) {
                            continue;
                        }
                        Identifier id = registry.getKey(structure);
                        String name = id == null ? "structure" : id.getPath();
                        List<BoundingBox> pieceBoxes = new ArrayList<>();
                        if (withPieces) {
                            for (StructurePiece piece : start.getPieces()) {
                                pieceBoxes.add(piece.getBoundingBox());
                            }
                        }
                        result.add(new StructureBoxes(name, start.getBoundingBox(), List.copyOf(pieceBoxes), colorFor(name)));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static int colorFor(String name) {
        // Stable, saturated colour per structure type.
        float hue = (name.hashCode() & 0xFFFF) / (float) 0xFFFF;
        return java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f) | 0xFF000000;
    }

    private void render(OverlayRenderEvents.OverlayRenderContext context) {
        List<StructureBoxes> current = structures;
        if (!isEnabled() || isClientNull() || current.isEmpty()) {
            return;
        }
        Camera camera = context.gameRenderer().mainCamera();
        OverlayBufferSource consumers = context.consumers();
        if (camera == null || consumers == null) {
            return;
        }
        Vec3 cam = camera.position();
        VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
        for (StructureBoxes structure : current) {
            int c = structure.color();
            float r = ((c >> 16) & 0xFF) / 255f;
            float g = ((c >> 8) & 0xFF) / 255f;
            float b = (c & 0xFF) / 255f;
            for (BoundingBox piece : structure.pieces()) {
                drawBox(lines, cam, piece, r, g, b, 0.35f, 1.0f, 0.02);
            }
            drawBox(lines, cam, structure.bounds(), r, g, b, 1.0f, 2.5f, 0.0);
        }
        if (showLabels) {
            renderLabels(context, cam, new Quaternionf(camera.rotation()), current);
        }
    }

    private static void drawBox(VertexConsumer lines, Vec3 cam, BoundingBox box, float r, float g, float b, float a, float width, double inset) {
        DrawUtil.drawOutlinedBox(lines,
                box.minX() + inset - cam.x, box.minY() + inset - cam.y, box.minZ() + inset - cam.z,
                box.maxX() + 1 - inset - cam.x, box.maxY() + 1 - inset - cam.y, box.maxZ() + 1 - inset - cam.z,
                r, g, b, a, width);
    }

    private void renderLabels(OverlayRenderEvents.OverlayRenderContext context, Vec3 cam, Quaternionf rotation, List<StructureBoxes> current) {
        Font font = getClient().font;
        PoseStack matrices = context.matrices() == null ? new PoseStack() : context.matrices();
        for (StructureBoxes structure : current) {
            BoundingBox box = structure.bounds();
            // Label above the middle of the box, clamped towards the camera height so tall boxes stay readable.
            double x = (box.minX() + box.maxX() + 1) / 2.0;
            double z = (box.minZ() + box.maxZ() + 1) / 2.0;
            double y = Math.clamp(cam.y + 2, box.minY() + 1, box.maxY() + 2);
            double distance = Math.sqrt(cam.distanceToSqr(x, y, z));
            float scale = (float) Math.max(0.03, distance * 0.004);
            String text = structure.name().replace('_', ' ');
            matrices.pushPose();
            matrices.translate(x - cam.x, y - cam.y, z - cam.z);
            matrices.rotate(rotation);
            matrices.scale(scale, -scale, scale);
            OverlayBufferSource.drawText(font, text, -font.width(text) / 2f, -4, structure.color(), false,
                    matrices.last().pose(), context.consumers(), Font.DisplayMode.SEE_THROUGH, 0x90000000, 0x00F000F0);
            matrices.popPose();
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Radius (chunks): " + chunkRadius);
        settings.add("Show pieces: " + (showPieces ? "ON" : "OFF"));
        settings.add("Show labels: " + (showLabels ? "ON" : "OFF"));
        settings.add("Found: " + structures.size() + (getClient().getSingleplayerServer() == null ? " (singleplayer only)" : ""));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> {
                int next = RADII[0];
                for (int radius : RADII) {
                    if (radius > chunkRadius) {
                        next = radius;
                        break;
                    }
                }
                chunkRadius = next;
                ticksUntilRefresh = 0;
            }
            case 2 -> {
                showPieces = !showPieces;
                ticksUntilRefresh = 0;
            }
            case 3 -> showLabels = !showLabels;
            default -> {
                // read-only
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("chunkRadius", chunkRadius);
        json.addProperty("showPieces", showPieces);
        json.addProperty("showLabels", showLabels);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        chunkRadius = PersistentSettings.readInt(settings, "chunkRadius", chunkRadius, 1, 16);
        showPieces = PersistentSettings.readBoolean(settings, "showPieces", showPieces);
        showLabels = PersistentSettings.readBoolean(settings, "showLabels", showLabels);
    }
}
