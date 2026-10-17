package me.m0dii.modules.debugviews;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.math.Axis;
import me.m0dii.M0DevToolsClient;
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
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug views that sit next to the Mob AI Visualizer (paths, brains) and the redstone update order view:
 * collision vs. outline shapes, heightmaps, block entities (with counts per type) and fluid levels/flow.
 * Everything is worked out from what the client already has, so it works on any server.
 */
public class DebugViewsModule extends Module implements PersistentSettings {
    public static final DebugViewsModule INSTANCE = new DebugViewsModule();

    public enum View {
        COLLISION("collision", "Collision shapes"),
        HEIGHTMAPS("heightmaps", "Heightmaps"),
        BLOCK_ENTITIES("blockentities", "Block entities"),
        FLUIDS("fluids", "Fluid levels & flow");

        final String id;
        final String label;

        View(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private record Marker(BlockPos pos, String type) {
    }

    private static final int MAX_FLUID_LABELS = 250;

    private final Map<View, Boolean> views = new LinkedHashMap<>();
    private int radius = 6;
    private final List<Marker> blockEntities = new ArrayList<>();
    private List<BlockEntityCensus.Row> census = List.of();
    private int censusTotal;
    private String busiestChunk = "";
    private int scanTicker;

    private DebugViewsModule() {
        super("debug_views", "Debug Views", false);
        for (View view : View.values()) {
            views.put(view, view == View.COLLISION);
        }
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || isClientNull() || getClient().level == null || context.gameRenderer() == null) {
                return;
            }
            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }
            if (consumers == null) {
                return;
            }
            Camera camera = context.gameRenderer().mainCamera();
            VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
            Vec3 cam = camera.position();
            if (views.get(View.COLLISION)) {
                renderCollision(lines, cam);
            }
            if (views.get(View.HEIGHTMAPS)) {
                renderHeightmaps(lines, cam);
            }
            if (views.get(View.BLOCK_ENTITIES)) {
                renderBlockEntities(lines, cam);
            }
            if (views.get(View.FLUIDS)) {
                PoseStack matrices = context.matrices() == null ? new PoseStack() : context.matrices();
                renderFluids(lines, matrices, camera);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "debug_views"),
                this::renderHud);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommands.literal("debugview").executes(ctx -> reply(ctx.getSource(), status()));
            for (View view : View.values()) {
                root.then(ClientCommands.literal(view.id)
                        .executes(ctx -> reply(ctx.getSource(), toggle(view, !views.get(view))))
                        .then(ClientCommands.literal("on").executes(ctx -> reply(ctx.getSource(), toggle(view, true))))
                        .then(ClientCommands.literal("off").executes(ctx -> reply(ctx.getSource(), toggle(view, false)))));
            }
            root.then(ClientCommands.literal("radius")
                    .then(ClientCommands.argument("blocks", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                        radius = IntegerArgumentType.getInteger(ctx, "blocks");
                        ModuleStateStore.onSettingsChanged(this);
                        return reply(ctx.getSource(), "Radius " + radius + " (collision and fluid views).");
                    })));
            dispatcher.register(root);
        });
    }

    private static int reply(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[DebugView] " + text));
        return 1;
    }

    private String toggle(View view, boolean on) {
        views.put(view, on);
        if (on && !isEnabled()) {
            setEnabled(true);
        }
        scanTicker = 0;
        ModuleStateStore.onSettingsChanged(this);
        return view.label + ": " + (on ? "on" : "off");
    }

    private String status() {
        StringBuilder out = new StringBuilder(isEnabled() ? "On." : "Off.");
        for (View view : View.values()) {
            out.append(' ').append(view.id).append('=').append(views.get(view) ? "on" : "off");
        }
        return out.append(", radius ").append(radius).append('.').toString();
    }

    // ---- block entity scan -------------------------------------------------------------------------------

    private void tick(Minecraft client) {
        if (!isEnabled() || !views.get(View.BLOCK_ENTITIES) || client.level == null || client.player == null) {
            return;
        }
        if (++scanTicker < 20 && scanTicker > 1) {
            return;
        }
        scanTicker = 1;
        ClientLevel level = client.level;
        int chunkRadius = Math.max(2, client.options.renderDistance().get());
        int pcx = client.player.blockPosition().getX() >> 4;
        int pcz = client.player.blockPosition().getZ() >> 4;
        BlockEntityCensus counter = new BlockEntityCensus();
        blockEntities.clear();
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    String type = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
                    counter.add(type, cx, cz);
                    if (blockEntity.getBlockPos().distSqr(client.player.blockPosition()) <= 48 * 48) {
                        blockEntities.add(new Marker(blockEntity.getBlockPos(), type));
                    }
                }
            }
        }
        census = counter.top(8);
        censusTotal = counter.total();
        busiestChunk = counter.busiestChunk();
    }

    // ---- views -------------------------------------------------------------------------------------------

    private BlockPos focus() {
        Minecraft client = getClient();
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return client.player.blockPosition();
    }

    /** Red = collision box, yellow = outline of blocks you can walk through. Full cubes are skipped. */
    private void renderCollision(VertexConsumer lines, Vec3 cam) {
        ClientLevel level = getClient().level;
        BlockPos center = focus();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = Math.min(radius, 8);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    VoxelShape collision = state.getCollisionShape(level, pos);
                    if (collision.isEmpty()) {
                        VoxelShape outline = state.getShape(level, pos);
                        drawShape(lines, cam, pos, outline, 1.0f, 0.9f, 0.3f, 0.6f);
                    } else if (!Block.isShapeFullBlock(collision)) {
                        drawShape(lines, cam, pos, collision, 1.0f, 0.3f, 0.3f, 0.9f);
                    }
                }
            }
        }
    }

    private static void drawShape(VertexConsumer lines, Vec3 cam, BlockPos pos, VoxelShape shape, float r, float g, float b, float a) {
        for (AABB box : shape.toAabbs()) {
            DrawUtil.drawOutlinedBoxSafe(lines,
                    pos.getX() + box.minX - cam.x, pos.getY() + box.minY - cam.y, pos.getZ() + box.minZ - cam.z,
                    pos.getX() + box.maxX - cam.x, pos.getY() + box.maxY - cam.y, pos.getZ() + box.maxZ - cam.z,
                    r, g, b, a, 1.5f);
        }
    }

    /**
     * Green = WORLD_SURFACE (highest non-air), red = MOTION_BLOCKING (highest block that stops movement or holds
     * fluid; where rain stops and mobs spawn on). Where they match, only green is drawn.
     */
    private void renderHeightmaps(VertexConsumer lines, Vec3 cam) {
        ClientLevel level = getClient().level;
        BlockPos center = getClient().player.blockPosition();
        int r = 12;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int motion = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                flatSquare(lines, cam, x, surface + 0.02, z, 0.3f, 1.0f, 0.3f);
                if (motion != surface) {
                    flatSquare(lines, cam, x, motion + 0.04, z, 1.0f, 0.3f, 0.3f);
                }
            }
        }
    }

    private static void flatSquare(VertexConsumer lines, Vec3 cam, int x, double y, int z, float r, float g, float b) {
        double x1 = x + 0.1 - cam.x;
        double x2 = x + 0.9 - cam.x;
        double z1 = z + 0.1 - cam.z;
        double z2 = z + 0.9 - cam.z;
        double yy = y - cam.y;
        DrawUtil.drawLineSafe(lines, x1, yy, z1, x2, yy, z1, r, g, b, 0.8f, 1.5f);
        DrawUtil.drawLineSafe(lines, x2, yy, z1, x2, yy, z2, r, g, b, 0.8f, 1.5f);
        DrawUtil.drawLineSafe(lines, x2, yy, z2, x1, yy, z2, r, g, b, 0.8f, 1.5f);
        DrawUtil.drawLineSafe(lines, x1, yy, z2, x1, yy, z1, r, g, b, 0.8f, 1.5f);
    }

    private void renderBlockEntities(VertexConsumer lines, Vec3 cam) {
        for (Marker marker : blockEntities) {
            BlockPos p = marker.pos();
            DrawUtil.drawOutlinedBoxSafe(lines, p.getX() + 0.05 - cam.x, p.getY() + 0.05 - cam.y, p.getZ() + 0.05 - cam.z,
                    p.getX() + 0.95 - cam.x, p.getY() + 0.95 - cam.y, p.getZ() + 0.95 - cam.z, 0.4f, 0.6f, 1.0f, 0.8f, 1.5f);
        }
    }

    /** Arrow along the flow direction and the fluid amount (8 = source) on each fluid block nearby. */
    private void renderFluids(VertexConsumer lines, PoseStack matrices, Camera camera) {
        ClientLevel level = getClient().level;
        Vec3 cam = camera.position();
        BlockPos center = getClient().player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = Math.min(radius, 10);
        int labels = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.isEmpty()) {
                        continue;
                    }
                    double surface = pos.getY() + fluid.getHeight(level, pos);
                    Vec3 flow = fluid.getFlow(level, pos);
                    double cx = pos.getX() + 0.5;
                    double cz = pos.getZ() + 0.5;
                    if (flow.lengthSqr() > 1.0E-4) {
                        Vec3 dir = flow.normalize().scale(0.4);
                        DrawUtil.drawLineSafe(lines, cx - dir.x - cam.x, surface + 0.02 - cam.y, cz - dir.z - cam.z,
                                cx + dir.x - cam.x, surface + 0.02 - cam.y, cz + dir.z - cam.z, 0.3f, 0.7f, 1.0f, 0.9f, 2.0f);
                        // arrow head
                        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(0.4);
                        Vec3 tip = new Vec3(cx + dir.x, surface + 0.02, cz + dir.z);
                        Vec3 back = tip.subtract(dir.scale(0.35));
                        DrawUtil.drawLineSafe(lines, tip.x - cam.x, tip.y - cam.y, tip.z - cam.z,
                                back.x + side.x - cam.x, back.y - cam.y, back.z + side.z - cam.z, 0.3f, 0.7f, 1.0f, 0.9f, 2.0f);
                        DrawUtil.drawLineSafe(lines, tip.x - cam.x, tip.y - cam.y, tip.z - cam.z,
                                back.x - side.x - cam.x, back.y - cam.y, back.z - side.z - cam.z, 0.3f, 0.7f, 1.0f, 0.9f, 2.0f);
                    }
                    if (labels < MAX_FLUID_LABELS) {
                        labels++;
                        String text = fluid.isSource() ? "S" : Integer.toString(fluid.getAmount());
                        worldText(matrices, camera, new Vec3(cx, surface + 0.25, cz), text,
                                fluid.isSource() ? 0xFF7CE07C : 0xFFFFFFFF);
                    }
                }
            }
        }
    }

    private void worldText(PoseStack matrices, Camera camera, Vec3 at, String text, int color) {
        Vec3 cam = camera.position();
        matrices.pushPose();
        matrices.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.scale(-0.02f, -0.02f, 0.02f);
        Matrix4f matrix = matrices.last().pose();
        Font font = getClient().font;
        OverlayBufferSource.drawText(font, text, -font.width(text) / 2f, 0, color, false, matrix,
                OverlayBufferSource.current(), Font.DisplayMode.NORMAL, 0, 0x00F000F0);
        matrices.popPose();
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || !views.get(View.BLOCK_ENTITIES) || client.player == null || client.gui.screen() != null
                || client.gui.hud.isHidden()) {
            return;
        }
        int x = 4;
        int y = client.getWindow().getGuiScaledHeight() / 2 - 60;
        context.text(client.font, "Block entities loaded: " + censusTotal, x, y, 0xFF9AD0FF, true);
        y += 10;
        for (BlockEntityCensus.Row row : census) {
            context.text(client.font, row.count() + "  " + row.type().replace("minecraft:", ""), x, y, 0xFFDDDDDD, true);
            y += 9;
        }
        if (!busiestChunk.isEmpty()) {
            context.text(client.font, "Busiest chunk: " + busiestChunk, x, y + 2, 0xFFAAAAAA, true);
        }
    }

    // ---- settings ----------------------------------------------------------------------------------------

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        for (View view : View.values()) {
            settings.add(view.label + ": " + (views.get(view) ? "ON" : "OFF"));
        }
        settings.add("Radius: " + radius);
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            toggleEnabled();
            return;
        }
        View[] all = View.values();
        if (settingIndex - 1 < all.length) {
            View view = all[settingIndex - 1];
            views.put(view, !views.get(view));
            scanTicker = 0;
        } else {
            radius = radius >= 12 ? 2 : radius + 2;
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        for (View view : View.values()) {
            json.addProperty(view.id, views.get(view));
        }
        json.addProperty("radius", radius);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        for (View view : View.values()) {
            views.put(view, PersistentSettings.readBoolean(settings, view.id, views.get(view)));
        }
        radius = PersistentSettings.readInt(settings, "radius", radius, 1, 16);
    }
}
