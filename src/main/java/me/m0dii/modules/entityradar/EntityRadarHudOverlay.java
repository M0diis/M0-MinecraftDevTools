package me.m0dii.modules.entityradar;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import java.util.List;

public class EntityRadarHudOverlay implements Toggleable {

    @Getter
    @Setter
    private boolean enabled = false;

    public void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (!enabled) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        List<Entity> entities = EntityRadarModule.INSTANCE.getEntities();

        int passiveCount = EntityRadarModule.INSTANCE.getPassiveCount();
        int hostileCount = EntityRadarModule.INSTANCE.getHostileCount();
        int neutralCount = EntityRadarModule.INSTANCE.getNeutralCount();
        int totalCount = entities.size();

        float scale = 0.75f;

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        int x = Math.round(10 / scale);
        int y = Math.round(10 / scale);
        int lineHeight = Math.round(10 / scale);

        String countText = String.format("Entities: %d (§aP:%d §7N:%d §cH:%d§r)",
                totalCount, passiveCount, neutralCount, hostileCount);
        context.text(client.font, countText, x, y, 0xFFFFFFFF);
        y += Math.round(12 / scale);

        context.text(client.font, "─────────────────", x, y, 0xFF808080);
        y += lineHeight;

        for (Entity entity : entities) {
            String text = String.format("%s - %.2fm", entity.getName().getString(), entity.distanceTo(client.player));
            context.text(client.font, text, x, y, 0xFFFFFFFF);
            y += lineHeight;
        }

        context.pose().popMatrix();
    }
}

