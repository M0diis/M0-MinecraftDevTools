package me.m0dii.modules.entityradar;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;

import java.util.List;

public class EntityRadarHudOverlay implements Toggleable {

    @Getter
    @Setter
    private boolean enabled = false;

    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        List<Entity> entities = EntityRadarModule.INSTANCE.getEntities();
        int y = 10;

        int passiveCount = EntityRadarModule.INSTANCE.getPassiveCount();
        int hostileCount = EntityRadarModule.INSTANCE.getHostileCount();
        int neutralCount = EntityRadarModule.INSTANCE.getNeutralCount();
        int totalCount = entities.size();

        String countText = String.format("Entities: %d (§aP:%d §7N:%d §cH:%d§r)",
                totalCount, passiveCount, neutralCount, hostileCount);
        context.drawTextWithShadow(client.textRenderer, countText, 10, y, 0xFFFFFF);
        y += 12;

        context.drawTextWithShadow(client.textRenderer, "─────────────────", 10, y, 0x808080);
        y += 10;

        for (Entity entity : entities) {
            String text = String.format("%s - %.2fm", entity.getName().getString(), entity.distanceTo(client.player));
            context.drawTextWithShadow(client.textRenderer, text, 10, y, 0xFFFFFF);
            y += 10;
        }
    }
}

