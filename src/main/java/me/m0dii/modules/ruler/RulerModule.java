package me.m0dii.modules.ruler;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import me.m0dii.utils.CustomRenderLayers;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Two-point ruler. Press the "Ruler: mark point" key (or {@code /ruler mark}) on a block to set the first point,
 * again for the second; a third press starts over. With only the first point set, the block you look at is
 * measured live. Shows size, block count, footprint, straight distance and walking (Manhattan) distance.
 */
public class RulerModule extends Module {
    public static final RulerModule INSTANCE = new RulerModule();

    private BlockPos pointA;
    private BlockPos pointB;

    private RulerModule() {
        super("ruler", "Ruler", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.RULER_MARK.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.RULER_MARK.defaultKey(),
                client -> mark());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("ruler")
                        .executes(ctx -> reply(ctx.getSource(), status()))
                        .then(ClientCommands.literal("mark").executes(ctx -> reply(ctx.getSource(), mark())))
                        .then(ClientCommands.literal("a").executes(ctx -> reply(ctx.getSource(), setPoint(true))))
                        .then(ClientCommands.literal("b").executes(ctx -> reply(ctx.getSource(), setPoint(false))))
                        .then(ClientCommands.literal("clear").executes(ctx -> reply(ctx.getSource(), clear())))));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pointA = null;
            pointB = null;
        });

        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || pointA == null || isClientNull()) {
                return;
            }
            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }
            if (consumers == null || context.gameRenderer() == null) {
                return;
            }
            BlockPos end = currentEnd();
            Vec3 camera = context.gameRenderer().mainCamera().position();
            VertexConsumer lines = consumers.getBuffer(CustomRenderLayers.LINES_NO_DEPTH);

            outlineBlock(lines, camera, pointA, 1.0f, 0.35f, 0.35f);
            if (end != null) {
                outlineBlock(lines, camera, end, 0.35f, 0.6f, 1.0f);
                int minX = Math.min(pointA.getX(), end.getX());
                int minY = Math.min(pointA.getY(), end.getY());
                int minZ = Math.min(pointA.getZ(), end.getZ());
                int maxX = Math.max(pointA.getX(), end.getX()) + 1;
                int maxY = Math.max(pointA.getY(), end.getY()) + 1;
                int maxZ = Math.max(pointA.getZ(), end.getZ()) + 1;
                DrawUtil.drawOutlinedBoxSafe(lines,
                        minX - camera.x, minY - camera.y, minZ - camera.z,
                        maxX - camera.x, maxY - camera.y, maxZ - camera.z,
                        1.0f, 0.85f, 0.3f, 0.7f, 1.5f);
                DrawUtil.drawLineSafe(lines,
                        pointA.getX() + 0.5 - camera.x, pointA.getY() + 0.5 - camera.y, pointA.getZ() + 0.5 - camera.z,
                        end.getX() + 0.5 - camera.x, end.getY() + 0.5 - camera.y, end.getZ() + 0.5 - camera.z,
                        1.0f, 1.0f, 1.0f, 0.9f, 2.0f);
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "ruler"),
                this::renderHud);
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || pointA == null || client.player == null) {
            return;
        }
        BlockPos end = currentEnd();
        String text = end == null
                ? "Ruler: point A set · look at a block or press the mark key again"
                : "Ruler" + (pointB == null ? " (live)" : "") + ": " + measure(pointA, end).summary();
        int width = client.font.width(text);
        int x = (client.getWindow().getGuiScaledWidth() - width) / 2;
        int y = 38;
        context.fill(x - 4, y - 3, x + width + 4, y + 11, 0x80000000);
        context.text(client.font, text, x, y, 0xFFFFE08A, true);
    }

    static RulerMath measure(BlockPos a, BlockPos b) {
        return RulerMath.between(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /** Second point, or the block under the crosshair while measuring live. */
    private BlockPos currentEnd() {
        if (pointB != null) {
            return pointB;
        }
        return targetedBlock(false);
    }

    private static BlockPos targetedBlock(boolean fallbackToFeet) {
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return fallbackToFeet && client.player != null ? client.player.blockPosition().below() : null;
    }

    private String mark() {
        if (!isEnabled()) {
            setEnabled(true);
        }
        BlockPos target = targetedBlock(true);
        if (target == null) {
            return "Look at a block first.";
        }
        if (pointA == null || pointB != null) {
            pointA = target;
            pointB = null;
            return "Point A: " + format(pointA) + ". Mark a second block to measure.";
        }
        pointB = target;
        String result = "Point B: " + format(pointB) + " · " + measure(pointA, pointB).summary();
        notify(result);
        return result;
    }

    private String setPoint(boolean first) {
        if (!isEnabled()) {
            setEnabled(true);
        }
        BlockPos target = targetedBlock(true);
        if (target == null) {
            return "Look at a block first.";
        }
        if (first) {
            pointA = target;
            return "Point A: " + format(pointA);
        }
        if (pointA == null) {
            pointA = target;
            return "Point A: " + format(pointA) + " (no first point yet)";
        }
        pointB = target;
        return "Point B: " + format(pointB) + " · " + measure(pointA, pointB).summary();
    }

    private String clear() {
        pointA = null;
        pointB = null;
        return "Ruler cleared.";
    }

    private String status() {
        if (pointA == null) {
            return "No points. Look at a block and run /ruler mark (or bind the 'Ruler: mark point' key).";
        }
        BlockPos end = currentEnd();
        return "A " + format(pointA) + (end == null ? "" : " → B " + format(end) + " · " + measure(pointA, end).summary());
    }

    private void notify(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(text));
        }
    }

    private static int reply(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Ruler] " + text));
        return 1;
    }

    private static String format(BlockPos pos) {
        return String.format(Locale.ROOT, "%d %d %d", pos.getX(), pos.getY(), pos.getZ());
    }

    private static void outlineBlock(VertexConsumer lines, Vec3 camera, BlockPos pos, float r, float g, float b) {
        DrawUtil.drawOutlinedBoxSafe(lines,
                pos.getX() - camera.x - 0.002, pos.getY() - camera.y - 0.002, pos.getZ() - camera.z - 0.002,
                pos.getX() + 1.002 - camera.x, pos.getY() + 1.002 - camera.y, pos.getZ() + 1.002 - camera.z,
                r, g, b, 1.0f, 2.0f);
    }

    @Override
    protected void onDisable() {
        pointA = null;
        pointB = null;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Mark point (look at a block)");
        settings.add("Clear points");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> notify(mark());
            case 2 -> notify(clear());
            default -> {
                // nothing
            }
        }
    }
}
