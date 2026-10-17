package me.m0dii.modules.farmrate;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.worldedit.WorldEditSelection;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Farm Rate Meter: counts the items a farm produces and shows totals and items per hour. Works on any server:
 * it watches item entities appearing in a zone (around you, or your WorldEdit selection), or counts what lands
 * in your inventory. For exact numbers on a server that has this mod, see {@code /mobdrops}.
 */
public class FarmRateModule extends Module {
    public static final FarmRateModule INSTANCE = new FarmRateModule();

    private enum Source { ZONE, INVENTORY }

    private final FarmRateCounter counter = new FarmRateCounter();
    private final ItemFlowTracker flow = new ItemFlowTracker();
    private final Map<String, Integer> lastInventory = new HashMap<>();
    private Source source = Source.ZONE;
    private @Nullable AABB zone;
    private @Nullable Object zoneLevel;

    private FarmRateModule() {
        super("farm_rate", "Farm Rate Meter", false);
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (isEnabled()) {
                setEnabled(false);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "farm_rate"),
                this::renderHud);
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || source != Source.ZONE || zone == null || context.gameRenderer() == null) {
                return;
            }
            OverlayBufferSource consumers = context.consumers() != null ? context.consumers() : OverlayBufferSource.current();
            if (consumers == null) {
                return;
            }
            Vec3 cam = context.gameRenderer().mainCamera().position();
            VertexConsumer lines = consumers.getBuffer(RenderTypes.LINES);
            DrawUtil.drawOutlinedBoxSafe(lines, zone.minX - cam.x, zone.minY - cam.y, zone.minZ - cam.z,
                    zone.maxX - cam.x, zone.maxY - cam.y, zone.maxZ - cam.z, 0.4f, 1.0f, 0.5f, 0.8f, 2.0f);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("farmrate")
                        .executes(ctx -> reply(ctx.getSource(), summary()))
                        .then(ClientCommands.literal("start")
                                .executes(ctx -> reply(ctx.getSource(), startZone(6)))
                                .then(ClientCommands.literal("selection").executes(ctx -> reply(ctx.getSource(), startSelection())))
                                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> reply(ctx.getSource(), startZone(IntegerArgumentType.getInteger(ctx, "radius"))))))
                        .then(ClientCommands.literal("inventory").executes(ctx -> reply(ctx.getSource(), startInventory())))
                        .then(ClientCommands.literal("reset").executes(ctx -> {
                            counter.start(System.currentTimeMillis());
                            flow.reset();
                            lastInventory.clear();
                            return reply(ctx.getSource(), "Counts reset.");
                        }))
                        .then(ClientCommands.literal("copy").executes(ctx -> {
                            ctx.getSource().getClient().keyboardHandler.setClipboard(counter.csv(System.currentTimeMillis()));
                            return reply(ctx.getSource(), "Copied the counts as CSV.");
                        }))
                        .then(ClientCommands.literal("stop").executes(ctx -> {
                            String text = summary();
                            setEnabled(false);
                            return reply(ctx.getSource(), "Stopped. " + text);
                        }))));
    }

    private static int reply(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[FarmRate] " + text));
        return 1;
    }

    private String startZone(int radius) {
        Minecraft client = getClient();
        if (client.player == null || client.level == null) {
            return "Join a world first.";
        }
        BlockPos center = client.player.blockPosition();
        zone = new AABB(center).inflate(radius);
        return begin(Source.ZONE, "Counting items that appear within " + radius + " blocks of " + center.toShortString()
                + ". Stand where you won't pick them up, or use /farmrate inventory.");
    }

    private String startSelection() {
        WorldEditSelection selection = WorldEditSelection.fromDebugDrawSelection();
        if (selection == null) {
            return "No selection. Set //pos1 and //pos2 first.";
        }
        zone = new AABB(Vec3.atLowerCornerOf(selection.min()), Vec3.atLowerCornerOf(selection.max()).add(1, 1, 1));
        return begin(Source.ZONE, "Counting items that appear in the selection (" + selection.volume() + " blocks).");
    }

    private String startInventory() {
        zone = null;
        return begin(Source.INVENTORY, "Counting items that land in your inventory (ignored while a container is open).");
    }

    private String begin(Source newSource, String message) {
        source = newSource;
        counter.start(System.currentTimeMillis());
        flow.reset();
        lastInventory.clear();
        zoneLevel = getClient().level;
        if (!isEnabled()) {
            setEnabled(true);
        }
        return message;
    }

    @Override
    protected void onEnable() {
        if (!counter.isStarted()) {
            startZone(6);
        }
    }

    @Override
    protected void onDisable() {
        flow.reset();
        lastInventory.clear();
    }

    private String summary() {
        if (!counter.isStarted()) {
            return "Not running. /farmrate start [radius|selection] or /farmrate inventory.";
        }
        long now = System.currentTimeMillis();
        StringBuilder out = new StringBuilder(counter.total() + " items in " + FarmRateCounter.formatElapsed(counter.elapsedMs(now)));
        for (FarmRateCounter.Row row : counter.rows(now)) {
            out.append("\n  ").append(shortId(row.item())).append(": ").append(row.total())
                    .append(" (").append(FarmRateCounter.formatRate(row.perHour())).append("/h)");
        }
        return out.toString();
    }

    private void tick(Minecraft client) {
        if (!isEnabled() || client.level == null || client.player == null) {
            return;
        }
        if (client.level != zoneLevel) {
            setEnabled(false);
            client.player.sendSystemMessage(Component.literal("[FarmRate] Stopped (changed world). " + summary()));
            return;
        }
        long now = System.currentTimeMillis();
        if (source == Source.ZONE && zone != null) {
            Map<Integer, ItemFlowTracker.Seen> current = new HashMap<>();
            for (ItemEntity item : client.level.getEntitiesOfClass(ItemEntity.class, zone)) {
                ItemStack stack = item.getItem();
                if (!stack.isEmpty()) {
                    current.put(item.getId(), new ItemFlowTracker.Seen(itemId(stack), stack.getCount()));
                }
            }
            flow.update(current).forEach((item, count) -> counter.add(item, count, now));
        } else if (source == Source.INVENTORY) {
            Map<String, Integer> counts = new HashMap<>();
            var inventory = client.player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    counts.merge(itemId(stack), stack.getCount(), Integer::sum);
                }
            }
            // Moving items in and out of containers is not farming.
            if (client.gui.screen() == null && !lastInventory.isEmpty()) {
                counts.forEach((item, count) -> {
                    int gained = count - lastInventory.getOrDefault(item, 0);
                    if (gained > 0) {
                        counter.add(item, gained, now);
                    }
                });
            }
            lastInventory.clear();
            lastInventory.putAll(counts);
            if (lastInventory.isEmpty()) {
                lastInventory.put("", 0); // marks "seen once" so the first real pickup counts
            }
        }
    }

    private static String itemId(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.player == null || client.gui.hud.isHidden() || !counter.isStarted()) {
            return;
        }
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        lines.add("Farm rate · " + (source == Source.ZONE ? "zone" : "inventory") + " · "
                + FarmRateCounter.formatElapsed(counter.elapsedMs(now)) + " · " + counter.total() + " items");
        List<FarmRateCounter.Row> rows = counter.rows(now);
        for (int i = 0; i < Math.min(6, rows.size()); i++) {
            FarmRateCounter.Row row = rows.get(i);
            lines.add(shortId(row.item()) + "  " + row.total() + "  " + FarmRateCounter.formatRate(row.perHour()) + "/h"
                    + "  (5m: " + FarmRateCounter.formatRate(row.recentPerHour()) + "/h)");
        }
        if (rows.isEmpty()) {
            lines.add("Waiting for items…");
        }
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, client.font.width(line));
        }
        int x = client.getWindow().getGuiScaledWidth() - width - 6;
        int y = 40;
        context.fill(x - 3, y - 3, x + width + 3, y + lines.size() * 10, 0x80000000);
        for (int i = 0; i < lines.size(); i++) {
            context.text(client.font, lines.get(i), x, y + i * 10, i == 0 ? 0xFF9AFFB0 : 0xFFE0E0E0, false);
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Start here (radius 6)");
        settings.add("Count inventory instead");
        settings.add("Reset counts");
        settings.add("Copy CSV");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        Minecraft client = getClient();
        String message = switch (settingIndex) {
            case 0 -> {
                toggleEnabled();
                yield null;
            }
            case 1 -> startZone(6);
            case 2 -> startInventory();
            case 3 -> {
                counter.start(System.currentTimeMillis());
                flow.reset();
                lastInventory.clear();
                yield "Counts reset.";
            }
            case 4 -> {
                client.keyboardHandler.setClipboard(counter.csv(System.currentTimeMillis()));
                yield "Copied the counts as CSV.";
            }
            default -> null;
        };
        if (message != null && client.player != null) {
            client.player.sendOverlayMessage(Component.literal("[FarmRate] " + message));
        }
    }
}
