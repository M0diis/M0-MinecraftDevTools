package me.m0dii.modules.redstonetimeline;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.state.ModuleStateStore;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Redstone timeline: while on, records every redstone component around the spot where you started it, tick by
 * tick, and shows the history as a logic-analyser style timeline ({@code /rstimeline open}).
 * <p>
 * Works client-side by reading block states each game tick, so it shows what the client was told: changes
 * that start and end inside one tick (0-tick pulses) are not visible.
 */
public class RedstoneTimelineModule extends Module implements PersistentSettings {
    public static final RedstoneTimelineModule INSTANCE = new RedstoneTimelineModule();
    public static final int MAX_TRACKS = 96;
    private static final int[] RADII = {4, 8, 12, 16, 24};
    private static final int RESCAN_TICKS = 40;

    public final RedstoneTimeline timeline = new RedstoneTimeline(MAX_TRACKS);
    private int radius = 8;
    private boolean showArea = true;
    private @Nullable BlockPos center;
    private @Nullable ClientLevel level;
    private final List<BlockPos> watched = new ArrayList<>();
    private long lastGameTime = Long.MIN_VALUE;
    private int ticksSinceScan;

    private RedstoneTimelineModule() {
        super("redstone_timeline", "Redstone Timeline", false);
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        OverlayRenderEvents.AFTER_ENTITIES.register(this::renderArea);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (isEnabled()) {
                setEnabled(false);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("rstimeline")
                        .executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> openScreen(client));
                            return 1;
                        })
                        .then(ClientCommands.literal("start")
                                .executes(ctx -> {
                                    ctx.getSource().sendFeedback(Component.literal("[Timeline] " + start(radius)));
                                    return 1;
                                })
                                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(1, 32)).executes(ctx -> {
                                    int r = IntegerArgumentType.getInteger(ctx, "radius");
                                    ctx.getSource().sendFeedback(Component.literal("[Timeline] " + start(r)));
                                    return 1;
                                })))
                        .then(ClientCommands.literal("stop").executes(ctx -> {
                            setEnabled(false);
                            ctx.getSource().sendFeedback(Component.literal("[Timeline] Stopped. " + summary()));
                            return 1;
                        }))
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            timeline.clear();
                            ctx.getSource().sendFeedback(Component.literal("[Timeline] Cleared."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("open").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> openScreen(client));
                            return 1;
                        }))));
    }

    /** Clears the timeline and starts recording around the player. */
    public String start(int newRadius) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return "Join a world first.";
        }
        radius = Math.clamp(newRadius, 1, 32);
        ModuleStateStore.onSettingsChanged(this);
        timeline.clear();
        if (isEnabled()) {
            setEnabled(false);
        }
        setEnabled(true);
        return "Recording redstone within " + radius + " blocks of " + center.toShortString() + " ("
                + watched.size() + " components). Open with /rstimeline.";
    }

    public String summary() {
        if (timeline.isEmpty()) {
            return "Nothing recorded.";
        }
        long span = timeline.lastTick() - timeline.firstTick() + 1;
        return timeline.activeTracks().size() + " of " + timeline.tracks().size() + " components changed over "
                + RedstoneTimeline.describeTicks(span) + ".";
    }

    public int radius() {
        return radius;
    }

    public @Nullable BlockPos center() {
        return center;
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            center = null;
            return;
        }
        center = client.player.blockPosition();
        level = client.level;
        lastGameTime = Long.MIN_VALUE;
        rescan(client.level);
        sample(client.level, client.level.getGameTime());
    }

    @Override
    protected void onDisable() {
        watched.clear();
        level = null;
    }

    private void tick(Minecraft client) {
        if (!isEnabled()) {
            return;
        }
        if (client.level == null || center == null) {
            return;
        }
        if (client.level != level) {
            // Changed dimension: the old positions mean nothing here.
            setEnabled(false);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("[Timeline] Stopped (changed world). " + summary()));
            }
            return;
        }
        long gameTime = client.level.getGameTime();
        if (gameTime == lastGameTime) {
            return; // game paused or no new tick
        }
        if (++ticksSinceScan >= RESCAN_TICKS) {
            rescan(client.level);
        }
        sample(client.level, gameTime);
    }

    private void sample(ClientLevel world, long gameTime) {
        lastGameTime = gameTime;
        timeline.noteTick(gameTime);
        for (BlockPos pos : watched) {
            if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            Integer signal = signalOf(state);
            if (signal == null) {
                // Block was removed or replaced: record it as off.
                timeline.record(pos.asLong(), labelOf(state, pos), gameTime, 0);
                continue;
            }
            boolean known = timeline.isTracked(pos.asLong());
            if (timeline.record(pos.asLong(), labelOf(state, pos), gameTime, signal) && known) {
                me.m0dii.modules.tickcontrol.TickControlModule.INSTANCE.onSignalChange();
            }
        }
    }

    /** Finds components in range, nearest first, until the track limit. */
    private void rescan(ClientLevel world) {
        ticksSinceScan = 0;
        if (center == null) {
            return;
        }
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                if (!world.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                for (int dy = -radius; dy <= radius; dy++) {
                    pos.set(x, center.getY() + dy, z);
                    if (signalOf(world.getBlockState(pos)) != null) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        found.sort((a, b) -> Double.compare(a.distSqr(center), b.distSqr(center)));
        for (BlockPos candidate : found) {
            if (timeline.isTracked(candidate.asLong()) || watched.contains(candidate)) {
                continue;
            }
            if (watched.size() >= MAX_TRACKS) {
                break;
            }
            watched.add(candidate);
        }
    }

    /** Outlines the recorded area (and each watched component) while recording. */
    private void renderArea(OverlayRenderEvents.OverlayRenderContext context) {
        BlockPos c = center;
        if (!isEnabled() || !showArea || c == null || context.consumers() == null || context.gameRenderer() == null) {
            return;
        }
        Vec3 cam = context.gameRenderer().mainCamera().position();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.LINES);
        DrawUtil.drawOutlinedBox(lines,
                c.getX() - radius - cam.x, c.getY() - radius - cam.y, c.getZ() - radius - cam.z,
                c.getX() + radius + 1 - cam.x, c.getY() + radius + 1 - cam.y, c.getZ() + radius + 1 - cam.z,
                0.9f, 0.33f, 0.29f, 0.6f, 1.5f);
        for (BlockPos pos : watched) {
            DrawUtil.drawOutlinedBox(lines,
                    pos.getX() + 0.02 - cam.x, pos.getY() + 0.02 - cam.y, pos.getZ() + 0.02 - cam.z,
                    pos.getX() + 0.98 - cam.x, pos.getY() + 0.98 - cam.y, pos.getZ() + 0.98 - cam.z,
                    0.9f, 0.33f, 0.29f, 0.35f, 1.0f);
        }
    }

    /** Signal strength 0-15 for blocks worth watching, or null for everything else. */
    static @Nullable Integer signalOf(BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWER)) {
            return state.getValue(BlockStateProperties.POWER);
        }
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            return state.getValue(BlockStateProperties.POWERED) ? 15 : 0;
        }
        if (state.hasProperty(BlockStateProperties.EXTENDED)) {
            return state.getValue(BlockStateProperties.EXTENDED) ? 15 : 0;
        }
        if (state.hasProperty(BlockStateProperties.TRIGGERED)) {
            return state.getValue(BlockStateProperties.TRIGGERED) ? 15 : 0;
        }
        if ((state.getBlock() instanceof RedstoneTorchBlock || state.getBlock() instanceof RedstoneLampBlock)
                && state.hasProperty(BlockStateProperties.LIT)) {
            return state.getValue(BlockStateProperties.LIT) ? 15 : 0;
        }
        return null;
    }

    private static String labelOf(BlockState state, BlockPos pos) {
        return state.getBlock().getName().getString() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    public static void openScreen(Minecraft client) {
        if (!(client.gui.screen() instanceof RedstoneTimelineScreen)) {
            client.gui.setScreen(new RedstoneTimelineScreen(client.gui.screen()));
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Recording: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Radius: " + radius);
        settings.add("Open timeline");
        settings.add("Clear");
        settings.add("Show area: " + (showArea ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> {
                if (isEnabled()) {
                    setEnabled(false);
                } else {
                    start(radius);
                }
            }
            case 1 -> {
                int next = RADII[0];
                for (int i = 0; i < RADII.length; i++) {
                    if (RADII[i] > radius) {
                        next = RADII[i];
                        break;
                    }
                }
                radius = next;
                ModuleStateStore.onSettingsChanged(this);
            }
            case 2 -> openScreen(Minecraft.getInstance());
            case 3 -> timeline.clear();
            case 4 -> {
                showArea = !showArea;
                ModuleStateStore.onSettingsChanged(this);
            }
            default -> {
                // nothing
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("radius", radius);
        json.addProperty("showArea", showArea);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        radius = PersistentSettings.readInt(settings, "radius", 8, 1, 32);
        showArea = PersistentSettings.readBoolean(settings, "showArea", true);
    }
}
