package me.m0dii.modules.nbthud;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.utils.NbtExtractors;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class NBTInfoHudRenderer implements Toggleable {

    @Getter
    @Setter
    private boolean enabled = false;

    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !enabled) {
            return;
        }

        List<String> lines = getInfoLines();
        if (lines.isEmpty()) {
            return;
        }

        HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_NBT_INSPECTOR,
                NBTInfoHudRenderer::defaultCanvasElement
        );
        if (!canvas.visible) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        float scale = Math.clamp(canvas.fontScale, 0.25f, 5.0f);
        int panelW = Math.max(40, canvas.width);
        int panelH = Math.max(20, canvas.height);
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int startX = resolvePanelX(canvas, panelW, screenW);
        int startY = resolvePanelY(canvas, panelH, screenH);
        int lineH = Math.max(6, canvas.lineHeight);
        int padding = Math.max(0, canvas.padding);

        int innerW = Math.max(1, panelW - (padding * 2));
        int logicalInnerW = Math.max(1, (int) Math.floor(innerW / scale));
        int logicalInnerH = Math.max(1, (int) Math.floor(Math.max(1, panelH - (padding * 2)) / scale));
        int maxLines = Math.max(1, logicalInnerH / lineH);

        List<String> drawLines = new ArrayList<>();
        drawLines.add(trimTextToWidth(tr, "Inspector", logicalInnerW));
        for (String s : lines) {
            if (drawLines.size() >= maxLines) {
                break;
            }
            drawLines.add(trimTextToWidth(tr, s, logicalInnerW));
        }

        if (canvas.drawBackground) {
            ctx.fill(startX, startY, startX + panelW, startY + panelH, canvas.backgroundColor);
        }
        if (canvas.drawBorder) {
            int x2 = startX + panelW;
            int y2 = startY + panelH;
            ctx.fill(startX, startY, x2, startY + 1, canvas.borderColor);
            ctx.fill(startX, y2 - 1, x2, y2, canvas.borderColor);
            ctx.fill(startX, startY, startX + 1, y2, canvas.borderColor);
            ctx.fill(x2 - 1, startY, x2, y2, canvas.borderColor);
        }

        int textBlockHeight = drawLines.size() * lineH;
        int textLogicalX = alignedTextX(canvas, tr, drawLines, logicalInnerW);
        int textLogicalY = alignedTextY(canvas, logicalInnerH, textBlockHeight);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        int logicalX = Math.round(startX / scale) + padding + textLogicalX;
        int logicalY = Math.round(startY / scale) + padding + textLogicalY;
        int y = logicalY;
        for (String s : drawLines) {
            ctx.drawText(tr, s, logicalX, y, canvas.textColor, false);
            y += lineH;
        }
        ctx.getMatrices().popMatrix();
    }

    private static String trimTextToWidth(TextRenderer tr, String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 0 || tr.getWidth(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        int keep = text.length();
        while (keep > 0 && tr.getWidth(text.substring(0, keep) + ellipsis) > maxWidth) {
            keep--;
        }
        return keep <= 0 ? ellipsis : text.substring(0, keep) + ellipsis;
    }

    private static HudCanvasDataHandler.HudCanvasElement defaultCanvasElement() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 8;
        e.width = 260;
        e.height = 120;
        e.fontScale = 1.0f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_LEFT;
        e.horizontalAlign = HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        e.verticalAlign = HudCanvasDataHandler.HudCanvasElement.VerticalAlign.TOP;
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

    private static int alignedTextX(HudCanvasDataHandler.HudCanvasElement canvas, TextRenderer tr, List<String> lines, int logicalInnerW) {
        if (canvas.horizontalAlign == HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT) {
            return 0;
        }
        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, tr.getWidth(line));
        }
        return switch (canvas.horizontalAlign) {
            case CENTER -> Math.max(0, (logicalInnerW - maxLineWidth) / 2);
            case RIGHT -> Math.max(0, logicalInnerW - maxLineWidth);
            default -> 0;
        };
    }

    private static int alignedTextY(HudCanvasDataHandler.HudCanvasElement canvas, int logicalInnerH, int textBlockHeight) {
        return switch (canvas.verticalAlign) {
            case CENTER -> Math.max(0, (logicalInnerH - textBlockHeight) / 2);
            case BOTTOM -> Math.max(0, logicalInnerH - textBlockHeight);
            default -> 0;
        };
    }

    private static List<String> getInfoLines() {
        List<String> lines = new ArrayList<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return List.of();
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return List.of();
        }

        if (hit instanceof BlockHitResult blockHit) {
            var pos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(pos);
            var block = state.getBlock();
            var biome = client.world.getBiome(pos).getIdAsString();
            int blockLight = client.world.getLightLevel(LightType.BLOCK, pos);
            int skyLight = client.world.getLightLevel(LightType.SKY, pos);
            int totalLight = client.world.getLightLevel(pos);
            int power = client.world.getReceivedRedstonePower(pos);

            lines.add("§f" + block.getName().getString());
            lines.add("§7Pos: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            lines.add("§7Light: §e" + totalLight + " §7(B:" + blockLight + " S:" + skyLight + ")");
            lines.add("§7Biome: §a" + biome);
            if (power > 0) {
                lines.add("§7Power: §c" + power);
            }

            // Show block states/properties
            var properties = state.getProperties();
            if (!properties.isEmpty()) {
                lines.add("§7States: §b" + properties.size());
                for (var property : properties) {
                    String propertyName = property.getName();
                    String propertyValue = getPropertyValueAsString(state, property);
                    lines.add(" §8- §7" + propertyName + ": §b" + propertyValue);
                }
            }

            // data for containers, crops, etc. can be added here
            if (state.hasBlockEntity()) {
                var blockEntity = client.world.getBlockEntity(pos);
                if (blockEntity != null) {
                    var nbt = blockEntity.createNbt(client.world.getRegistryManager());
                    if (!nbt.isEmpty()) {
                        lines.add("§7NBT: §e" + nbt.getSize() + " tags");
                        for (String key : nbt.getKeys()) {
                            var val = nbt.get(key);
                            lines.add(" §8- §7" + key + ": §f" + (val != null ? val.asString() : ""));
                        }
                    }
                }
            }
        }

        if (hit instanceof EntityHitResult entityHit) {
            var entity = entityHit.getEntity();
            String entityName = entity.getName().getString();
            String entityType = entity.getType().toString();
            if (entityType.contains(":")) {
                entityType = entityType.substring(entityType.lastIndexOf(":") + 1);
            }
            lines.add("§f" + entityName);
            lines.add("§7Type: §b" + entityType);
            lines.add("§7UUID: §8" + entity.getUuidAsString());
            if (entity instanceof net.minecraft.entity.LivingEntity living) {
                lines.add("§7Health: §c" + String.format("%.1f", living.getHealth()) + "/" + String.format("%.1f", living.getMaxHealth()));
            }

            NbtCompound entityNbt = NbtExtractors.extractEntityNbt(entity);
            if (entityNbt != null && !entityNbt.isEmpty()) {
                lines.add("§7Entity NBT: §e" + entityNbt.getSize() + " tags");
                int count = 0;
                for (String key : entityNbt.getKeys()) {
                    var val = entityNbt.get(key);
                    lines.add(" §8- §7" + key + ": §f" + (val != null ? val.asString() : ""));
                    count++;
                    if (count >= 12) {
                        lines.add(" §8- ...");
                        break;
                    }
                }
            }
        }

        return lines;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueAsString(BlockState state, Property<?> property) {
        Property<T> typedProperty = (Property<T>) property;
        T value = state.get(typedProperty);
        return typedProperty.name(value);
    }


}
