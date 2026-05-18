package me.m0dii.modules.nbthud;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.utils.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
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
        int startX = Math.max(0, canvas.x);
        int startY = Math.max(0, canvas.y);
        int lineH = Math.max(6, canvas.lineHeight);
        int padding = Math.max(0, canvas.padding);

        // Compute max width in unscaled text pixels, then scale for background
        int maxW = tr.getWidth("Inspector");
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));
        int maxWScaled = (int) Math.ceil(maxW * scale);

        // Compute total height in physical pixels
        int totalH = (int) Math.ceil((1 + lines.size()) * (lineH * scale) + (padding * 2));

        // Background panel (physical coords)
        if (canvas.drawBackground) {
            ctx.fill(startX - padding, startY - padding, startX + maxWScaled + padding + 2, startY + totalH, canvas.backgroundColor);
        }
        if (canvas.drawBorder) {
            int x1 = startX - padding;
            int y1 = startY - padding;
            int x2 = startX + maxWScaled + padding + 2;
            int y2 = startY + totalH;
            ctx.fill(x1, y1, x2, y1 + 1, canvas.borderColor);
            ctx.fill(x1, y2 - 1, x2, y2, canvas.borderColor);
            ctx.fill(x1, y1, x1 + 1, y2, canvas.borderColor);
            ctx.fill(x2 - 1, y1, x2, y2, canvas.borderColor);
        }

        // Draw text scaled: scale the matrix, and pass coordinates divided by scale so text appears at physical coords
        int y = startY;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        ctx.drawText(tr, "Inspector", (int) (startX / scale), (int) (y / scale), canvas.textColor, true);
        y += (int) Math.ceil(lineH * scale);

        for (String s : lines) {
            ctx.drawText(tr, s, (int) (startX / scale), (int) (y / scale), canvas.textColor, false);
            y += (int) Math.ceil(lineH * scale);
        }
        ctx.getMatrices().popMatrix();
    }

    private static HudCanvasDataHandler.HudCanvasElement defaultCanvasElement() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = Math.max(0, ModConfig.overlayInspectorMarginX);
        e.y = Math.max(0, ModConfig.overlayInspectorMarginY);
        e.width = 260;
        e.height = 120;
        e.fontScale = (float) Math.clamp(ModConfig.overlayInspectorTextScale, 0.25, 5.0);
        e.lineHeight = Math.max(6, ModConfig.overlayInspectorLineHeight);
        e.padding = Math.max(0, ModConfig.overlayInspectorPadding);
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
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

            // Entity NBT access changed across versions; keep inspector stable by omitting raw dump.
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
