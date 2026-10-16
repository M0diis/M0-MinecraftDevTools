package me.m0dii.modules.macros.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MacroInventoryWidgetSupport {
    private static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final Identifier HOTBAR_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION_TEXTURE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HELMET_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/helmet");
    private static final Identifier CHESTPLATE_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/chestplate");
    private static final Identifier LEGGINGS_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/leggings");
    private static final Identifier BOOTS_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/boots");
    private static final Identifier SHIELD_SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/shield");

    private static final int ITEM_SIZE = 16;
    private static final int HOTBAR_TEXTURE_WIDTH = 182;
    private static final int HOTBAR_TEXTURE_HEIGHT = 22;
    private static final int HOTBAR_SLOT_STEP = 20;
    private static final int HOTBAR_SLOT_CONTENT_OFFSET = 3;
    private static final int HOTBAR_SLOT_WIDTH = 22;

    private MacroInventoryWidgetSupport() {
    }

    public record InventoryContents(List<ItemStack> stacks, List<Identifier> hintSprites, int columns, int rows, int selectedSlot) {
    }

    public static InventoryContents collectContents(Player player, MacroHudDataHandler.InventoryDisplayMode mode) {
        if (player == null) {
            return new InventoryContents(List.of(), List.of(), 1, 1, -1);
        }

        List<ItemStack> stacks = new ArrayList<>();
        List<Identifier> hintSprites = new ArrayList<>();
        int columns = defaultColumns(mode);
        int rows = defaultRows(mode);
        int selectedSlot = -1;
        MacroHudDataHandler.InventoryDisplayMode resolved = resolvedMode(mode);
        switch (resolved) {
            case HOTBAR -> {
                for (int i = 0; i < 9; i++) {
                    stacks.add(player.getInventory().getItem(i));
                    hintSprites.add(null);
                }
                selectedSlot = player.getInventory().getSelectedSlot();
            }
            case INVENTORY -> {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int index = 9 + row * 9 + col;
                        stacks.add(player.getInventory().getItem(index));
                        hintSprites.add(null);
                    }
                }
            }
            case ARMOR -> addArmorAndOffhand(stacks, hintSprites, player, true);
            case ARMOR_ONLY -> addArmorAndOffhand(stacks, hintSprites, player, false);
            case OFFHAND -> {
                stacks.add(player.getOffhandItem());
                hintSprites.add(SHIELD_SLOT_TEXTURE);
            }
        }
        return new InventoryContents(stacks, hintSprites, columns, rows, selectedSlot);
    }

    public static void applyDefaultSize(MacroHudDataHandler.HudElement element) {
        MacroHudDataHandler.InventoryDisplayMode mode = resolvedMode(element.inventoryDisplayMode);
        MacroHudDataHandler.InventorySlotStyle style = resolvedStyle(element.inventorySlotStyle);

        if (style == MacroHudDataHandler.InventorySlotStyle.HOTBAR) {
            element.width = hotbarStripWidth(defaultColumns(mode));
            element.height = defaultRows(mode) * HOTBAR_TEXTURE_HEIGHT;
            return;
        }

        switch (mode) {
            case HOTBAR -> {
                element.width = 182;
                element.height = 22;
            }
            case INVENTORY -> {
                element.width = 182;
                element.height = 62;
            }
            case ARMOR -> {
                element.width = 22;
                element.height = 102;
            }
            case ARMOR_ONLY -> {
                element.width = 22;
                element.height = 82;
            }
            case OFFHAND -> {
                element.width = 22;
                element.height = 22;
            }
        }
    }

    public static String modeLabel(MacroHudDataHandler.InventoryDisplayMode mode) {
        return switch (resolvedMode(mode)) {
            case HOTBAR -> "Hotbar";
            case INVENTORY -> "Inventory";
            case ARMOR -> "Armor + Offhand";
            case ARMOR_ONLY -> "Armor";
            case OFFHAND -> "Offhand";
        };
    }

    public static String slotStyleLabel(MacroHudDataHandler.InventorySlotStyle style) {
        return switch (resolvedStyle(style)) {
            case DEFAULT -> "Default";
            case MINECRAFT -> "Slot";
            case HOTBAR -> "Hotbar";
        };
    }

    public static MacroHudDataHandler.InventorySlotStyle cycleSlotStyle(MacroHudDataHandler.InventorySlotStyle current, boolean forward) {
        MacroHudDataHandler.InventorySlotStyle[] values = MacroHudDataHandler.InventorySlotStyle.values();
        int index = current == null ? 0 : current.ordinal();
        int next = forward ? index + 1 : index - 1;
        if (next < 0) {
            next = values.length - 1;
        }
        if (next >= values.length) {
            next = 0;
        }
        return values[next];
    }

    public static void extractRenderState(GuiGraphicsExtractor context,
                              Font textRenderer,
                              MacroHudDataHandler.HudElement element,
                              Player player,
                              int x,
                              int y,
                              int width,
                              int height) {
        InventoryContents contents = collectContents(player, element.inventoryDisplayMode);
        boolean showCount = element.inventoryShowCount == null || element.inventoryShowCount;
        MacroHudDataHandler.InventorySlotStyle style = resolvedStyle(element.inventorySlotStyle);
        MacroHudDataHandler.InventoryDisplayMode mode = resolvedMode(element.inventoryDisplayMode);

        if (style == MacroHudDataHandler.InventorySlotStyle.HOTBAR) {
            renderHotbarStyle(context, textRenderer, contents, mode, x, y, width, height, showCount);
            return;
        }
        if (style == MacroHudDataHandler.InventorySlotStyle.MINECRAFT
                && mode == MacroHudDataHandler.InventoryDisplayMode.HOTBAR) {
            renderVanillaHotbar(context, textRenderer, contents, x, y, width, height, showCount);
            return;
        }

        renderGrid(context, textRenderer, contents, mode, style, x, y, width, height, showCount);
    }

    private static void renderGrid(GuiGraphicsExtractor context,
                                   Font textRenderer,
                                   InventoryContents contents,
                                   MacroHudDataHandler.InventoryDisplayMode mode,
                                   MacroHudDataHandler.InventorySlotStyle style,
                                   int x,
                                   int y,
                                   int width,
                                   int height,
                                   boolean showCount) {
        int padding = 2;
        int gap = 2;
        int contentW = Math.max(1, width - padding * 2);
        int contentH = Math.max(1, height - padding * 2);
        int cellW = Math.max(1, (contentW - gap * Math.max(0, contents.columns() - 1)) / Math.max(1, contents.columns()));
        int cellH = Math.max(1, (contentH - gap * Math.max(0, contents.rows() - 1)) / Math.max(1, contents.rows()));
        int cell = Math.clamp(cellW, 1, cellH);
        int gridW = contents.columns() * cell + gap * Math.max(0, contents.columns() - 1);
        int gridH = contents.rows() * cell + gap * Math.max(0, contents.rows() - 1);
        int startX = x + padding + Math.max(0, (contentW - gridW) / 2);
        int startY = y + padding + Math.max(0, (contentH - gridH) / 2);

        for (int i = 0; i < contents.stacks().size(); i++) {
            int col = i % contents.columns();
            int row = i / contents.columns();
            int slotX = startX + col * (cell + gap);
            int slotY = startY + row * (cell + gap);
            boolean selected = mode == MacroHudDataHandler.InventoryDisplayMode.HOTBAR && i == contents.selectedSlot();
            drawSlot(context, textRenderer, slotX, slotY, cell, contents.stacks().get(i), contents.hintSprites().get(i), selected, showCount, style);
        }
    }

    private static void renderVanillaHotbar(GuiGraphicsExtractor context,
                                            Font textRenderer,
                                            InventoryContents contents,
                                            int x,
                                            int y,
                                            int width,
                                            int height,
                                            boolean showCount) {
        if (width < HOTBAR_TEXTURE_WIDTH || height < HOTBAR_TEXTURE_HEIGHT) {
            renderGrid(context, textRenderer, contents, MacroHudDataHandler.InventoryDisplayMode.HOTBAR,
                    MacroHudDataHandler.InventorySlotStyle.MINECRAFT, x, y, width, height, showCount);
            return;
        }

        int startX = x + Math.max(0, (width - HOTBAR_TEXTURE_WIDTH) / 2);
        int startY = y + Math.max(0, (height - HOTBAR_TEXTURE_HEIGHT) / 2);
        drawHotbarStrip(context, startX, startY, contents.columns());

        if (contents.selectedSlot() >= 0 && contents.selectedSlot() < contents.stacks().size()) {
            int selectedX = startX + contents.selectedSlot() * HOTBAR_SLOT_STEP - 1;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_TEXTURE, selectedX, startY - 1, 24, 24);
        }

        for (int i = 0; i < contents.stacks().size(); i++) {
            drawNativeHotbarSlotContents(context, textRenderer, contents.stacks().get(i), contents.hintSprites().get(i),
                    startX + HOTBAR_SLOT_CONTENT_OFFSET + i * HOTBAR_SLOT_STEP, startY + HOTBAR_SLOT_CONTENT_OFFSET, showCount);
        }
    }

    private static void renderHotbarStyle(GuiGraphicsExtractor context,
                                          Font textRenderer,
                                          InventoryContents contents,
                                          MacroHudDataHandler.InventoryDisplayMode mode,
                                          int x,
                                          int y,
                                          int width,
                                          int height,
                                          boolean showCount) {
        int stripWidth = hotbarStripWidth(contents.columns());
        int totalHeight = contents.rows() * HOTBAR_TEXTURE_HEIGHT;
        if (width < stripWidth || height < totalHeight) {
            renderGrid(context, textRenderer, contents, mode, MacroHudDataHandler.InventorySlotStyle.HOTBAR, x, y, width, height, showCount);
            return;
        }

        int startX = x + Math.max(0, (width - stripWidth) / 2);
        int startY = y + Math.max(0, (height - totalHeight) / 2);
        for (int row = 0; row < contents.rows(); row++) {
            int rowY = startY + row * HOTBAR_TEXTURE_HEIGHT;
            drawHotbarStrip(context, startX, rowY, contents.columns());
            if (mode == MacroHudDataHandler.InventoryDisplayMode.HOTBAR && row == 0
                    && contents.selectedSlot() >= 0 && contents.selectedSlot() < contents.columns()) {
                int selectedX = startX + contents.selectedSlot() * HOTBAR_SLOT_STEP - 1;
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_TEXTURE, selectedX, rowY - 1, 24, 24);
            }

            for (int col = 0; col < contents.columns(); col++) {
                int index = row * contents.columns() + col;
                if (index >= contents.stacks().size()) {
                    break;
                }
                drawNativeHotbarSlotContents(context, textRenderer, contents.stacks().get(index), contents.hintSprites().get(index),
                        startX + HOTBAR_SLOT_CONTENT_OFFSET + col * HOTBAR_SLOT_STEP, rowY + HOTBAR_SLOT_CONTENT_OFFSET, showCount);
            }
        }
    }

    private static void drawSlot(GuiGraphicsExtractor context,
                                 Font textRenderer,
                                 int x,
                                 int y,
                                 int size,
                                 ItemStack stack,
                                 Identifier hintSprite,
                                 boolean selected,
                                 boolean showCount,
                                 MacroHudDataHandler.InventorySlotStyle style) {
        int edge = Math.max(1, Math.round(size / 18.0f));
        switch (style) {
            case HOTBAR -> drawHotbarLikeSlot(context, x, y, size, selected);
            case MINECRAFT -> drawMinecraftLikeSlot(context, x, y, size, selected);
            default -> drawDefaultSlot(context, x, y, size, edge, selected);
        }

        if (stack.isEmpty()) {
            if (style != MacroHudDataHandler.InventorySlotStyle.DEFAULT && hintSprite != null) {
                drawHintSprite(context, hintSprite, x, y, size);
            }
            return;
        }

        drawSlotContents(context, textRenderer, x, y, size, edge, stack, showCount, style);
    }

    private static void drawSlotContents(GuiGraphicsExtractor context,
                                         Font textRenderer,
                                         int x,
                                         int y,
                                         int size,
                                         int edge,
                                         ItemStack stack,
                                         boolean showCount,
                                         MacroHudDataHandler.InventorySlotStyle style) {
        int inner = Math.max(1, size - edge * 2);
        if (style != MacroHudDataHandler.InventorySlotStyle.DEFAULT && inner >= ITEM_SIZE) {
            int ix = x + Math.max(0, (size - ITEM_SIZE) / 2);
            int iy = y + Math.max(0, (size - ITEM_SIZE) / 2);
            context.item(stack, ix, iy);
            drawNativeOverlay(context, textRenderer, stack, ix, iy, showCount);
            return;
        }

        float scale = inner / 16.0f;
        int ix = x + Math.max(0, (size - inner) / 2);
        int iy = y + Math.max(0, (size - inner) / 2);
        context.pose().pushMatrix();
        context.pose().translate(ix, iy);
        context.pose().scale(scale, scale);
        context.item(stack, 0, 0);
        context.pose().popMatrix();

        if (showCount && stack.getCount() > 1) {
            drawScaledCountText(context, textRenderer, stack.getCount(), x, y, size, edge);
        }
        drawScaledDurabilityBar(context, stack, x, y, size, edge);
    }

    private static void drawNativeHotbarSlotContents(GuiGraphicsExtractor context,
                                                     Font textRenderer,
                                                     ItemStack stack,
                                                     Identifier hintSprite,
                                                     int x,
                                                     int y,
                                                     boolean showCount) {
        if (stack.isEmpty()) {
            if (hintSprite != null) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, hintSprite, x + 1, y + 1, 14, 14);
            }
            return;
        }
        context.item(stack, x, y);
        drawNativeOverlay(context, textRenderer, stack, x, y, showCount);
    }

    private static void drawNativeOverlay(GuiGraphicsExtractor context,
                                          Font textRenderer,
                                          ItemStack stack,
                                          int x,
                                          int y,
                                          boolean showCount) {
        ItemStack overlayStack = stack;
        if (!showCount && stack.getCount() > 1) {
            overlayStack = stack.copy();
            overlayStack.setCount(1);
        }
        context.itemDecorations(textRenderer, overlayStack, x, y);
    }

    private static void drawDefaultSlot(GuiGraphicsExtractor context, int x, int y, int size, int edge, boolean selected) {
        int bg = selected ? 0xA0785A20 : 0xAA1A1A1A;
        context.fill(x, y, x + size, y + size, bg);
        context.fill(x, y, x + size, y + edge, 0x50FFFFFF);
        context.fill(x, y + size - edge, x + size, y + size, 0x50303030);
        context.fill(x, y, x + edge, y + size, 0x50FFFFFF);
        context.fill(x + size - edge, y, x + size, y + size, 0x50303030);
    }

    private static void drawMinecraftLikeSlot(GuiGraphicsExtractor context, int x, int y, int size, boolean selected) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x, y, size, size);
        if (selected) {
            int pad = Math.max(1, Math.round(size / 18.0f));
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_TEXTURE, x, y, size, size);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_TEXTURE, x - pad, y - pad, size + pad * 2, size + pad * 2);
            context.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_TEXTURE, x, y, size, size);
        }
    }

    private static void drawHotbarLikeSlot(GuiGraphicsExtractor context, int x, int y, int size, boolean selected) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT, 0, 0, x, y, size, size);
        if (selected) {
            int pad = Math.max(1, Math.round(size / 22.0f));
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_TEXTURE, x - pad, y - pad, size + pad * 2, size + pad * 2);
        }
    }

    private static void drawHintSprite(GuiGraphicsExtractor context, Identifier hintSprite, int x, int y, int size) {
        int iconInset = Math.max(1, Math.round(size / 6.0f));
        int iconSize = Math.max(1, size - iconInset * 2);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, hintSprite, x + iconInset, y + iconInset, iconSize, iconSize);
    }

    private static void drawHotbarStrip(GuiGraphicsExtractor context, int x, int y, int columns) {
        // Crop the vanilla 9-slot hotbar sprite to the exact number of columns we need.
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE,
                HOTBAR_TEXTURE_WIDTH, HOTBAR_TEXTURE_HEIGHT, 0, 0, x, y, hotbarStripWidth(columns), HOTBAR_TEXTURE_HEIGHT);
    }

    private static void addArmorAndOffhand(List<ItemStack> stacks, List<Identifier> hintSprites, Player player, boolean includeOffhand) {
        stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
        hintSprites.add(HELMET_SLOT_TEXTURE);
        stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
        hintSprites.add(CHESTPLATE_SLOT_TEXTURE);
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        hintSprites.add(LEGGINGS_SLOT_TEXTURE);
        stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        hintSprites.add(BOOTS_SLOT_TEXTURE);
        if (includeOffhand) {
            stacks.add(player.getOffhandItem());
            hintSprites.add(SHIELD_SLOT_TEXTURE);
        }
    }

    private static void drawScaledCountText(GuiGraphicsExtractor context, Font textRenderer, int count, int x, int y, int size, int padding) {
        String text = Integer.toString(count);
        float scale = Math.clamp(size / 18.0f, 0.35f, 4.0f);
        int textWidth = Math.max(1, Math.round(textRenderer.width(text) * scale));
        int textHeight = Math.max(1, Math.round(9 * scale));
        int screenX = x + Math.max(padding, size - textWidth - padding);
        int screenY = y + Math.max(padding, size - textHeight - padding);
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        context.text(textRenderer, text, Math.round(screenX / scale), Math.round(screenY / scale), 0xFFFFFFFF);
        context.pose().popMatrix();
    }

    private static void drawScaledDurabilityBar(GuiGraphicsExtractor context, ItemStack stack, int x, int y, int size, int padding) {
        if (!stack.isBarVisible()) {
            return;
        }

        int barX = x + Math.max(1, padding);
        int barWidth = Math.max(3, size - Math.max(1, padding) * 2);
        int barHeight = Math.max(1, Math.round(size / 16.0f));
        int barY = y + size - Math.max(1, padding) - barHeight;
        int filled = Math.clamp(Math.round(barWidth * (stack.getBarWidth() / 13.0f)), 0, barWidth);

        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);
        if (filled > 0) {
            context.fill(barX, barY, barX + filled, barY + barHeight, 0xFF000000 | stack.getBarColor());
        }
    }

    private static MacroHudDataHandler.InventoryDisplayMode resolvedMode(MacroHudDataHandler.InventoryDisplayMode mode) {
        return mode == null ? MacroHudDataHandler.InventoryDisplayMode.HOTBAR : mode;
    }

    private static MacroHudDataHandler.InventorySlotStyle resolvedStyle(MacroHudDataHandler.InventorySlotStyle style) {
        return style == null ? MacroHudDataHandler.InventorySlotStyle.DEFAULT : style;
    }

    private static int defaultColumns(MacroHudDataHandler.InventoryDisplayMode mode) {
        return switch (resolvedMode(mode)) {
            case HOTBAR, INVENTORY -> 9;
            case ARMOR, ARMOR_ONLY, OFFHAND -> 1;
        };
    }

    private static int defaultRows(MacroHudDataHandler.InventoryDisplayMode mode) {
        return switch (resolvedMode(mode)) {
            case HOTBAR, OFFHAND -> 1;
            case INVENTORY -> 3;
            case ARMOR -> 5;
            case ARMOR_ONLY -> 4;
        };
    }

    private static int hotbarStripWidth(int columns) {
        return 2 + Math.max(1, columns) * HOTBAR_SLOT_STEP;
    }
}
