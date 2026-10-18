package me.m0dii.modules.highlights;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.utilitycommands.ScanPointer;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Shared world highlights for search commands ({@code /scanblocks}, {@code /find}): outlined blocks seen
 * through walls, a line to the nearest one and a HUD pointer that says which way to turn.
 */
public final class BlockHighlights {
    private static final int MAX_DRAWN = 512;
    private static final Map<String, HighlightSet> SETS = new LinkedHashMap<>();
    private static boolean registered;
    private static int pruneTicks;

    /**
     * @param stillValid checked every second; positions that fail it (block mined, chest emptied) are dropped
     */
    public record HighlightSet(String label, int color, List<BlockPos> positions, ClientLevel level,
                               BiPredicate<ClientLevel, BlockPos> stillValid, long expiresAtMs) {
    }

    private BlockHighlights() {
    }

    public static synchronized void show(String key, String label, int color, List<BlockPos> positions,
                                         BiPredicate<ClientLevel, BlockPos> stillValid, long durationMs) {
        ClientLevel level = Minecraft.getInstance().level;
        if (positions.isEmpty() || level == null) {
            SETS.remove(key);
            return;
        }
        SETS.remove(key);
        SETS.put(key, new HighlightSet(label, color, new ArrayList<>(positions), level, stillValid,
                durationMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + durationMs));
    }

    public static synchronized boolean clear(String key) {
        return SETS.remove(key) != null;
    }

    public static synchronized void clearAll() {
        SETS.clear();
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearAll());
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.gameRenderer() == null) {
                return;
            }
            OverlayBufferSource consumers = context.consumers() != null ? context.consumers() : OverlayBufferSource.current();
            if (consumers == null) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return;
            }
            Vec3 cam = context.gameRenderer().mainCamera().position();
            List<HighlightSet> sets;
            synchronized (BlockHighlights.class) {
                sets = new ArrayList<>(SETS.values());
            }
            VertexConsumer lines = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);
            for (HighlightSet set : sets) {
                if (set.level() != client.level) {
                    continue;
                }
                float r = ((set.color() >> 16) & 0xFF) / 255f;
                float g = ((set.color() >> 8) & 0xFF) / 255f;
                float b = (set.color() & 0xFF) / 255f;
                List<BlockPos> nearest = nearest(set.positions(), client.player.position(), MAX_DRAWN);
                for (BlockPos pos : nearest) {
                    DrawUtil.drawOutlinedBoxSafe(lines,
                            pos.getX() + 0.02 - cam.x, pos.getY() + 0.02 - cam.y, pos.getZ() + 0.02 - cam.z,
                            pos.getX() + 0.98 - cam.x, pos.getY() + 0.98 - cam.y, pos.getZ() + 0.98 - cam.z,
                            r, g, b, 0.85f, 1.5f);
                }
                if (!nearest.isEmpty()) {
                    BlockPos target = nearest.getFirst();
                    Vec3 from = client.player.getEyePosition().add(client.player.getLookAngle().scale(0.6));
                    DrawUtil.drawLineSafe(lines, from.x - cam.x, from.y - 0.15 - cam.y, from.z - cam.z,
                            target.getX() + 0.5 - cam.x, target.getY() + 0.5 - cam.y, target.getZ() + 0.5 - cam.z,
                            r, g, b, 0.9f, 2.0f);
                }
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "block_highlights"), BlockHighlights::renderHud);
    }

    private static synchronized void tick(Minecraft client) {
        if (SETS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        SETS.values().removeIf(set -> set.expiresAtMs() < now);
        if (++pruneTicks < 20 || client.level == null) {
            return;
        }
        pruneTicks = 0;
        SETS.values().removeIf(set -> {
            if (set.level() != client.level) {
                return true;
            }
            if (set.stillValid() != null) {
                set.positions().removeIf(pos -> client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && !set.stillValid().test(client.level, pos));
            }
            return set.positions().isEmpty();
        });
    }

    private static List<BlockPos> nearest(List<BlockPos> positions, Vec3 from, int limit) {
        if (positions.size() <= 1) {
            return positions;
        }
        List<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingDouble(pos -> {
            double dx = pos.getX() + 0.5 - from.x;
            double dy = pos.getY() + 0.5 - from.y;
            double dz = pos.getZ() + 0.5 - from.z;
            return dx * dx + dy * dy + dz * dz;
        }));
        return sorted.size() > limit ? sorted.subList(0, limit) : sorted;
    }

    private static void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        List<HighlightSet> sets;
        synchronized (BlockHighlights.class) {
            if (SETS.isEmpty()) {
                return;
            }
            sets = new ArrayList<>(SETS.values());
        }
        Font font = client.font;
        int y = client.getWindow().getGuiScaledHeight() / 2 + 14;
        int screenW = client.getWindow().getGuiScaledWidth();
        for (int i = sets.size() - 1; i >= 0 && y < client.getWindow().getGuiScaledHeight() - 40; i--) {
            HighlightSet set = sets.get(i);
            if (set.level() != client.level || set.positions().isEmpty()) {
                continue;
            }
            List<BlockPos> nearest = nearest(set.positions(), client.player.position(), 1);
            BlockPos target = nearest.getFirst();
            double dx = target.getX() + 0.5 - client.player.getX();
            double dy = target.getY() + 0.5 - client.player.getEyeY();
            double dz = target.getZ() + 0.5 - client.player.getZ();
            String arrow = ScanPointer.arrow(client.player.getYRot(), dx, dz);
            String text = arrow + " " + set.label() + " · " + ScanPointer.distance(dx, dy, dz)
                    + " (" + target.getX() + " " + target.getY() + " " + target.getZ() + ")"
                    + (set.positions().size() > 1 ? " · " + set.positions().size() + " found" : "");
            int w = font.width(text) + 8;
            int x = (screenW - w) / 2;
            context.fill(x, y, x + w, y + 12, 0x90101318);
            context.fill(x, y + 11, x + w, y + 12, 0xFF000000 | set.color());
            context.text(font, text, x + 4, y + 2, 0xFFE7EAF0, true);
            y += 14;
        }
    }
}
