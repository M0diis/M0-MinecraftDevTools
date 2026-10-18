package me.m0dii.modules.macros.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Status widgets for the HUD canvas: armor and held-item durability, active effects with timers, and a
 * durability alert that only appears when something is about to break.
 */
public final class StatusHudWidget {
    private static final int ROW_H = 18;
    private static final int BENEFICIAL = 0xFF7FD47F;
    private static final int HARMFUL = 0xFFE5736B;
    private static final int NEUTRAL = 0xFFB8C0CC;

    private StatusHudWidget() {
    }

    private record ItemRow(ItemStack stack, Identifier emptySprite) {
    }

    private record EffectRow(String name, int amplifier, int ticks, int color, Identifier sprite) {
    }

    /** False when the widget has nothing to show and should not be drawn at all (alert with nothing low). */
    public static boolean hasContent(MacroHudDataHandler.HudElement element, boolean preview) {
        if (preview) {
            return true;
        }
        StatusFormat.Kind kind = StatusFormat.Kind.parse(element.statusKind);
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return switch (kind) {
            case ALERT -> !lowItems(player, element.statusWarnPercent).isEmpty();
            case EFFECTS -> !player.getActiveEffects().isEmpty();
            default -> true;
        };
    }

    public static void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                              MacroHudDataHandler.HudElement element, boolean preview) {
        Player player = Minecraft.getInstance().player;
        StatusFormat.Kind kind = StatusFormat.Kind.parse(element.statusKind);
        StatusFormat.TextMode mode = StatusFormat.TextMode.parse(element.statusTextMode);
        boolean sample = preview && player == null;
        switch (kind) {
            case ARMOR -> drawItems(g, font, x, y, w, h, sample ? sampleArmor() : armor(player), mode, element);
            case HELD -> drawItems(g, font, x, y, w, h, sample ? sampleHeld() : held(player), mode, element);
            case EFFECTS -> drawEffects(g, font, x, y, w, h, effects(player, sample || preview), element);
            case ALERT -> drawAlert(g, font, x, y, w, h, player, element, preview);
        }
    }

    // ---- items ----

    private static List<ItemRow> armor(Player player) {
        if (player == null) {
            return List.of();
        }
        return List.of(
                new ItemRow(player.getItemBySlot(EquipmentSlot.HEAD), Identifier.withDefaultNamespace("container/slot/helmet")),
                new ItemRow(player.getItemBySlot(EquipmentSlot.CHEST), Identifier.withDefaultNamespace("container/slot/chestplate")),
                new ItemRow(player.getItemBySlot(EquipmentSlot.LEGS), Identifier.withDefaultNamespace("container/slot/leggings")),
                new ItemRow(player.getItemBySlot(EquipmentSlot.FEET), Identifier.withDefaultNamespace("container/slot/boots")));
    }

    private static List<ItemRow> held(Player player) {
        if (player == null) {
            return List.of();
        }
        return List.of(new ItemRow(player.getMainHandItem(), null),
                new ItemRow(player.getOffhandItem(), Identifier.withDefaultNamespace("container/slot/shield")));
    }

    private static List<ItemRow> sampleArmor() {
        return List.of(new ItemRow(damaged(Items.DIAMOND_HELMET, 0.9), null),
                new ItemRow(damaged(Items.DIAMOND_CHESTPLATE, 0.6), null),
                new ItemRow(damaged(Items.IRON_LEGGINGS, 0.3), null),
                new ItemRow(damaged(Items.GOLDEN_BOOTS, 0.08), null));
    }

    private static List<ItemRow> sampleHeld() {
        return List.of(new ItemRow(damaged(Items.NETHERITE_PICKAXE, 0.72), null),
                new ItemRow(damaged(Items.SHIELD, 0.4), null));
    }

    private static ItemStack damaged(net.minecraft.world.item.Item item, double remaining) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            stack.setDamageValue((int) Math.round(stack.getMaxDamage() * (1.0 - remaining)));
        }
        return stack;
    }

    private static void drawItems(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                                  List<ItemRow> rows, StatusFormat.TextMode mode, MacroHudDataHandler.HudElement element) {
        if (rows.isEmpty()) {
            return;
        }
        if (element.statusHorizontal) {
            int cell = Math.max(18, (w - 4) / rows.size());
            for (int i = 0; i < rows.size(); i++) {
                int cx = x + 2 + i * cell;
                if (cx + 16 > x + w) {
                    break;
                }
                drawItemCell(g, font, cx + (cell - 16) / 2, y + 2, cell, rows.get(i), mode, element, h - 4);
            }
            return;
        }
        int shown = Math.max(1, Math.min(rows.size(), (h - 2) / ROW_H));
        for (int i = 0; i < shown; i++) {
            drawItemRow(g, font, x + 2, y + 2 + i * ROW_H, w - 4, rows.get(i), mode, element);
        }
    }

    /** Icon on the left, durability text right-aligned and a thin bar under it. */
    private static void drawItemRow(GuiGraphicsExtractor g, Font font, int x, int y, int w,
                                    ItemRow row, StatusFormat.TextMode mode, MacroHudDataHandler.HudElement element) {
        ItemStack stack = row.stack();
        if (stack.isEmpty()) {
            if (row.emptySprite() != null) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, row.emptySprite(), x, y, 16, 16);
            }
            return;
        }
        g.item(stack, x, y);
        int textX = x + 20;
        int barW = Math.max(0, w - 22);
        if (!stack.isDamageableItem()) {
            String count = stack.getCount() > 1 ? Integer.toString(stack.getCount()) : "";
            String name = stack.getHoverName().getString();
            g.text(font, fit(font, count.isEmpty() ? name : count + "× " + name, barW), textX, y + 4, element.textColor, true);
            return;
        }
        int max = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        double fraction = StatusFormat.fraction(max, damage);
        boolean low = StatusFormat.isLow(max, damage, element.statusWarnPercent);
        int color = StatusFormat.durabilityColor(fraction);
        String text = StatusFormat.durability(mode, max, damage);
        if (!text.isEmpty()) {
            boolean visible = !low || (System.currentTimeMillis() / 400) % 2 == 0;
            g.text(font, text, textX, y + 1, visible ? color : element.textColor, true);
        }
        int barY = text.isEmpty() ? y + 7 : y + 12;
        g.fill(textX, barY, textX + barW, barY + 2, 0xA0000000);
        g.fill(textX, barY, textX + (int) Math.round(barW * fraction), barY + 2, color);
    }

    private static void drawItemCell(GuiGraphicsExtractor g, Font font, int x, int y, int cell, ItemRow row,
                                     StatusFormat.TextMode mode, MacroHudDataHandler.HudElement element, int innerH) {
        ItemStack stack = row.stack();
        if (stack.isEmpty()) {
            if (row.emptySprite() != null) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, row.emptySprite(), x, y, 16, 16);
            }
            return;
        }
        g.item(stack, x, y);
        if (!stack.isDamageableItem()) {
            g.itemDecorations(font, stack, x, y);
            return;
        }
        int max = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        double fraction = StatusFormat.fraction(max, damage);
        int color = StatusFormat.durabilityColor(fraction);
        g.fill(x, y + 16, x + 16, y + 18, 0xA0000000);
        g.fill(x, y + 16, x + (int) Math.round(16 * fraction), y + 18, color);
        String text = StatusFormat.durability(mode, max, damage);
        if (!text.isEmpty() && innerH >= 27) {
            float scale = 0.75f;
            int tw = Math.round(font.width(text) * scale);
            g.pose().pushMatrix();
            g.pose().translate(x + 8 - tw / 2.0f, y + 20);
            g.pose().scale(scale, scale);
            g.text(font, text, 0, 0, color, true);
            g.pose().popMatrix();
        }
    }

    // ---- effects ----

    private static List<EffectRow> effects(Player player, boolean allowSample) {
        List<EffectRow> rows = new ArrayList<>();
        if (player != null) {
            List<MobEffectInstance> active = new ArrayList<>(player.getActiveEffects());
            active.sort(Comparator.comparingInt(StatusHudWidget::sortTicks));
            for (MobEffectInstance instance : active) {
                Holder<MobEffect> holder = instance.getEffect();
                MobEffect effect = holder.value();
                int color = effect.getCategory() == MobEffectCategory.BENEFICIAL ? BENEFICIAL
                        : effect.getCategory() == MobEffectCategory.HARMFUL ? HARMFUL : NEUTRAL;
                Identifier sprite = holder.unwrapKey()
                        .map(key -> Identifier.fromNamespaceAndPath(key.identifier().getNamespace(), "mob_effect/" + key.identifier().getPath()))
                        .orElse(null);
                rows.add(new EffectRow(effect.getDisplayName().getString(), instance.getAmplifier(),
                        instance.isInfiniteDuration() ? -1 : instance.getDuration(), color, sprite));
            }
        }
        if (rows.isEmpty() && allowSample) {
            rows.add(new EffectRow("Speed", 1, 1840, BENEFICIAL, Identifier.withDefaultNamespace("mob_effect/speed")));
            rows.add(new EffectRow("Night Vision", 0, -1, BENEFICIAL, Identifier.withDefaultNamespace("mob_effect/night_vision")));
            rows.add(new EffectRow("Poison", 0, 150, HARMFUL, Identifier.withDefaultNamespace("mob_effect/poison")));
        }
        return rows;
    }

    private static int sortTicks(MobEffectInstance instance) {
        return instance.isInfiniteDuration() ? Integer.MAX_VALUE : instance.getDuration();
    }

    private static void drawEffects(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                                    List<EffectRow> rows, MacroHudDataHandler.HudElement element) {
        if (rows.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int rowH = 12;
        if (element.statusHorizontal) {
            int cell = 22;
            for (int i = 0; i < rows.size(); i++) {
                int cx = x + 2 + i * cell;
                if (cx + 18 > x + w) {
                    break;
                }
                EffectRow row = rows.get(i);
                if (row.sprite() != null && StatusFormat.blinkVisible(row.ticks(), now)) {
                    g.blitSprite(RenderPipelines.GUI_TEXTURED, row.sprite(), cx + 1, y + 2, 18, 18);
                }
                String time = shortTime(row.ticks());
                float scale = 0.75f;
                int tw = Math.round(font.width(time) * scale);
                g.pose().pushMatrix();
                g.pose().translate(cx + 10 - tw / 2.0f, y + 21);
                g.pose().scale(scale, scale);
                g.text(font, time, 0, 0, row.color(), true);
                g.pose().popMatrix();
            }
            return;
        }
        int shown = Math.max(1, Math.min(rows.size(), (h - 2) / rowH));
        for (int i = 0; i < shown; i++) {
            EffectRow row = rows.get(i);
            int ry = y + 2 + i * rowH;
            if (row.sprite() != null && StatusFormat.blinkVisible(row.ticks(), now)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, row.sprite(), x + 2, ry, 10, 10);
            }
            String time = StatusFormat.duration(row.ticks());
            int timeW = font.width(time);
            String level = StatusFormat.level(row.amplifier());
            String name = level.isEmpty() ? row.name() : row.name() + " " + level;
            g.text(font, fit(font, name, w - timeW - 22), x + 15, ry + 1, row.color(), true);
            g.text(font, time, x + w - 3 - timeW, ry + 1, element.textColor, true);
        }
    }

    private static String shortTime(int ticks) {
        if (ticks < 0) {
            return "∞";
        }
        int seconds = ticks / 20;
        return seconds >= 60 ? (seconds / 60) + "m" : seconds + "s";
    }

    // ---- alert ----

    private static List<ItemStack> lowItems(Player player, int warnPercent) {
        List<ItemStack> low = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()
                    && StatusFormat.isLow(stack.getMaxDamage(), stack.getDamageValue(), warnPercent)) {
                low.add(stack);
            }
        }
        return low;
    }

    private static void drawAlert(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, Player player,
                                  MacroHudDataHandler.HudElement element, boolean preview) {
        List<ItemStack> low = player == null ? List.of() : lowItems(player, element.statusWarnPercent);
        if (low.isEmpty() && preview) {
            low = List.of(damaged(Items.DIAMOND_PICKAXE, 0.05));
        }
        if (low.isEmpty()) {
            return;
        }
        boolean on = (System.currentTimeMillis() / 350) % 2 == 0;
        int warn = element.colorWarn != 0 ? element.colorWarn : 0xFFE5534B;
        if (on) {
            g.fill(x, y, x + w, y + 1, warn);
            g.fill(x, y + h - 1, x + w, y + h, warn);
        }
        String title = element.label == null || element.label.isBlank() || "Status".equals(element.label)
                ? "Low durability!" : element.label;
        g.text(font, fit(font, title, w - 6), x + 3, y + 3, on ? warn : element.textColor, true);
        int iy = y + 13;
        int ix = x + 3;
        for (ItemStack stack : low) {
            if (ix + 16 > x + w || iy + 16 > y + h) {
                break;
            }
            g.item(stack, ix, iy);
            String text = StatusFormat.durability(StatusFormat.TextMode.parse(element.statusTextMode),
                    stack.getMaxDamage(), stack.getDamageValue());
            if (low.size() == 1 && !text.isEmpty()) {
                g.text(font, fit(font, stack.getHoverName().getString() + "  " + text, w - 26), ix + 20, iy + 4,
                        element.textColor, true);
            }
            ix += 18;
        }
    }

    private static String fit(Font font, String text, int width) {
        if (width <= 0) {
            return "";
        }
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > width) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
