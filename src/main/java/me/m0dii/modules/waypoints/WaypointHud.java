package me.m0dii.modules.waypoints;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Small compass line at the top of the screen for the tracked waypoint ({@code /waypoint track}):
 * name, direction arrow relative to where you look, distance and height difference.
 */
public final class WaypointHud {
    private WaypointHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "waypoint_tracker"),
                WaypointHud::render);
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        WaypointHandler.Waypoint tracked = WaypointHandler.getTracked();
        if (tracked == null) {
            return;
        }
        String here = client.level.dimension().identifier().toString();
        Vec3 target = WaypointHandler.positionIn(tracked, here);
        if (target == null) {
            draw(context, client, "⌖ " + tracked.name + " · in " + shortDimension(tracked.dimension), 0xFFAAAAAA);
            return;
        }

        double dx = target.x - client.player.getX();
        double dy = target.y - client.player.getY();
        double dz = target.z - client.player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        String arrow = horizontal < 1.5 ? "●" : WaypointMath.arrowFor(dx, dz, client.player.getYRot());
        String height = Math.abs(dy) < 2 ? "" : (dy > 0 ? "  ▲" : "  ▼") + String.format(Locale.ROOT, "%.0f", Math.abs(dy));
        String via = tracked.dimension.equals(here) ? "" : "  (" + shortDimension(tracked.dimension) + " coords converted)";
        String text = arrow + " " + tracked.name + "  " + String.format(Locale.ROOT, "%.0fm", horizontal) + height + via;
        int color = 0xFF000000 | WaypointMath.colorFor(tracked.id);
        draw(context, client, text, color);
    }

    private static void draw(GuiGraphicsExtractor context, Minecraft client, String text, int color) {
        int width = client.font.width(text);
        int x = (client.getWindow().getGuiScaledWidth() - width) / 2;
        int y = 22;
        context.fill(x - 4, y - 3, x + width + 4, y + 11, 0x80000000);
        context.text(client.font, text, x, y, color, true);
    }

    private static String shortDimension(String dimension) {
        return dimension == null ? "?" : dimension.substring(dimension.indexOf(':') + 1);
    }
}
