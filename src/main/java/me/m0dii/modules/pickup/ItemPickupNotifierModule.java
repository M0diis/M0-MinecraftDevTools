package me.m0dii.modules.pickup;

import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public class ItemPickupNotifierModule extends Module {

    public static final ItemPickupNotifierModule INSTANCE = new ItemPickupNotifierModule();

    private static final long POPUP_FADE_MS = 1000L;

    private final Map<String, Integer> previousInventoryCounts = new HashMap<>();
    private final Deque<PickupPopup> popups = new ArrayDeque<>();

    private ItemPickupNotifierModule() {
        super("pickup_notifier", "Pick-up Notifier", true);
    }

    @Override
    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "pickup_notifier"),
                this::onHudRender
        );
    }

    private void onEndTick(MinecraftClient client) {
        if (!isEnabled() || client == null || client.player == null) {
            previousInventoryCounts.clear();
            return;
        }

        Map<String, Integer> currentCounts = new HashMap<>();
        Map<String, ItemStack> samples = new HashMap<>();

        int invSize = client.player.getInventory().size();
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String key = stackKey(stack);
            currentCounts.merge(key, stack.getCount(), Integer::sum);
            samples.putIfAbsent(key, stack.copy());
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            int prev = previousInventoryCounts.getOrDefault(entry.getKey(), 0);
            int delta = entry.getValue() - prev;
            if (delta > 0) {
                ItemStack sample = samples.get(entry.getKey());
                if (sample != null && !sample.isEmpty()) {
                    addPopup(sample, delta, now);
                }
            }
        }

        previousInventoryCounts.clear();
        previousInventoryCounts.putAll(currentCounts);
        pruneExpired(now);
    }

    private void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || client == null || client.player == null || popups.isEmpty()) {
            return;
        }

        HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_PICKUP_NOTIFIER,
                ItemPickupNotifierModule::defaultCanvasElement
        );
        if (!canvas.visible) {
            return;
        }

        long now = System.currentTimeMillis();
        pruneExpired(now);
        if (popups.isEmpty()) {
            return;
        }

        TextRenderer tr = client.textRenderer;
        float scale = Math.clamp(canvas.fontScale, 0.5f, 3.0f);
        int panelW = Math.max(120, canvas.width);
        int panelH = Math.max(40, canvas.height);
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int panelX = resolvePanelX(canvas, panelW, screenW);
        int panelY = resolvePanelY(canvas, panelH, screenH);
        int pad = Math.max(0, canvas.padding);
        int lineH = Math.max(18, canvas.lineHeight);
        PickupFeedSettings.Data settings = PickupFeedSettings.get();

        if (canvas.drawBackground) {
            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, canvas.backgroundColor);
        }
        if (canvas.drawBorder) {
            int border = canvas.borderColor;
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, border);
            ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, border);
            ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, border);
            ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, border);
        }

        int innerW = Math.max(1, panelW - (pad * 2));
        int innerH = Math.max(1, panelH - (pad * 2));
        int logicalInnerW = Math.max(1, (int) Math.floor(innerW / scale));
        int logicalInnerH = Math.max(1, (int) Math.floor(innerH / scale));
        int maxLines = Math.clamp(settings.maxLines, 1, Math.max(1, logicalInnerH / lineH));

        List<PickupPopup> newestFirst = new ArrayList<>(popups);
        java.util.Collections.reverse(newestFirst);
        List<PickupPopup> drawList = newestFirst.subList(0, Math.min(maxLines, newestFirst.size()));

        int rowCount = drawList.size();
        int textBlockHeight = rowCount * lineH;
        int offsetY = alignedTextY(canvas, logicalInnerH, textBlockHeight);
        int iconPx = Math.max(8, Math.round(16.0f * settings.iconScale));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        int baseX = Math.round(panelX / scale) + pad;
        int baseY = Math.round(panelY / scale) + pad + offsetY;
        int y = settings.direction == PickupFeedSettings.Direction.DOWN
                ? baseY
                : baseY + Math.max(0, (rowCount - 1) * lineH);

        for (PickupPopup popup : drawList) {
            long age = now - popup.createdAtMs;
            int lifetimeMs = settings.durationMs;
            int fadeMs = Math.clamp(lifetimeMs / 2, 250, (int) POPUP_FADE_MS);
            float alphaFactor = age >= (lifetimeMs - fadeMs)
                    ? Math.clamp((lifetimeMs - age) / (float) fadeMs, 0.0f, 1.0f)
                    : 1.0f;
            int textColor = withAlpha(canvas.textColor, alphaFactor);
            int bgColor = withAlpha(0xAA000000, alphaFactor * 0.45f);

            int rowY2 = y + lineH - 2;
            ctx.fill(baseX, y - 1, baseX + logicalInnerW, rowY2, bgColor);

            ItemStack icon = popup.stack.copy();
            icon.setCount(Math.clamp(popup.amount, 1, 99));
            int iconDrawY = y + Math.max(0, (lineH - iconPx) / 2);
            if (Math.abs(settings.iconScale - 1.0f) < 0.01f) {
                ctx.drawItem(icon, baseX + 1, iconDrawY);
            } else {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().scale(settings.iconScale, settings.iconScale);
                int iconXScaled = Math.round((baseX + 1) / settings.iconScale);
                int iconYScaled = Math.round(iconDrawY / settings.iconScale);
                ctx.drawItem(icon, iconXScaled, iconYScaled);
                ctx.getMatrices().popMatrix();
            }

            String line = "+" + popup.amount + " " + popup.stack.getName().getString();
            int textArea = Math.max(1, logicalInnerW - (iconPx + 8));
            String trimmed = trimTextToWidth(tr, line, textArea);
            int textX = baseX + iconPx + 6 + alignedTextX(canvas, tr, trimmed, textArea);
            ctx.drawText(tr, trimmed, textX, y + 4, textColor, false);
            y += (settings.direction == PickupFeedSettings.Direction.DOWN ? lineH : -lineH);
        }

        ctx.getMatrices().popMatrix();
    }

    private void addPopup(ItemStack stack, int amount, long now) {
        popups.addLast(new PickupPopup(stack.copy(), amount, now));
        while (popups.size() > PickupFeedSettings.get().maxLines) {
            popups.removeFirst();
        }
    }

    private void pruneExpired(long now) {
        int lifetimeMs = PickupFeedSettings.get().durationMs;
        while (!popups.isEmpty()) {
            PickupPopup oldest = popups.peekFirst();
            if (oldest == null || now - oldest.createdAtMs <= lifetimeMs) {
                break;
            }
            popups.removeFirst();
        }
    }

    private static String stackKey(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();
        return id + "|" + stack.getComponents();
    }

    private static int withAlpha(int argb, float alphaFactor) {
        int alpha = (argb >>> 24) & 0xFF;
        int outAlpha = Math.clamp((int) Math.floor(alpha * alphaFactor), 0, 255);
        return (argb & 0x00FFFFFF) | (outAlpha << 24);
    }

    private static HudCanvasDataHandler.HudCanvasElement defaultCanvasElement() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.BOTTOM_RIGHT;
        e.horizontalAlign = HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        e.verticalAlign = HudCanvasDataHandler.HudCanvasElement.VerticalAlign.BOTTOM;
        e.x = 12;
        e.y = 12;
        e.width = 220;
        e.height = 110;
        e.padding = 4;
        e.lineHeight = 18;
        e.fontScale = 1.0f;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFFFFFFF;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
    }

    private static int resolvePanelX(HudCanvasDataHandler.HudCanvasElement canvas, int panelW, int screenW) {
        return switch (canvas.anchor) {
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> Math.clamp((screenW - panelW) / 2 + canvas.x, 0, Math.max(0, screenW - panelW));
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> Math.clamp(screenW - panelW - canvas.x, 0, Math.max(0, screenW - panelW));
            default -> Math.clamp(canvas.x, 0, Math.max(0, screenW - panelW));
        };
    }

    private static int resolvePanelY(HudCanvasDataHandler.HudCanvasElement canvas, int panelH, int screenH) {
        return switch (canvas.anchor) {
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> Math.clamp((screenH - panelH) / 2 + canvas.y, 0, Math.max(0, screenH - panelH));
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.clamp(screenH - panelH - canvas.y, 0, Math.max(0, screenH - panelH));
            default -> Math.clamp(canvas.y, 0, Math.max(0, screenH - panelH));
        };
    }

    private static int alignedTextY(HudCanvasDataHandler.HudCanvasElement canvas, int logicalInnerH, int textBlockHeight) {
        return switch (canvas.verticalAlign) {
            case CENTER -> Math.max(0, (logicalInnerH - textBlockHeight) / 2);
            case BOTTOM -> Math.max(0, logicalInnerH - textBlockHeight);
            default -> 0;
        };
    }

    private static int alignedTextX(HudCanvasDataHandler.HudCanvasElement canvas, TextRenderer tr, String line, int logicalInnerW) {
        int textW = tr.getWidth(line);
        return switch (canvas.horizontalAlign) {
            case CENTER -> Math.max(0, (logicalInnerW - textW) / 2);
            case RIGHT -> Math.max(0, logicalInnerW - textW);
            default -> 0;
        };
    }

    private static String trimTextToWidth(TextRenderer tr, String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 0 || tr.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int keep = text.length();
        while (keep > 0 && tr.getWidth(text.substring(0, keep) + ellipsis) > maxWidth) {
            keep--;
        }
        return keep <= 0 ? ellipsis : text.substring(0, keep) + ellipsis;
    }

    private record PickupPopup(ItemStack stack, int amount, long createdAtMs) {}
}

