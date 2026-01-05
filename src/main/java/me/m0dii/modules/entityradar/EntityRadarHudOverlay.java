package me.m0dii.modules.entityradar;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.modules.Toggleable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;

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

        int passiveCount = EntityRadarModule.INSTANCE.getPassiveCount();
        int hostileCount = EntityRadarModule.INSTANCE.getHostileCount();
        int neutralCount = EntityRadarModule.INSTANCE.getNeutralCount();
        int totalCount = entities.size();

        float scale = 0.75f;
        scale = Math.clamp(scale, 0.5f, 3.0f);

        context.getMatrices().push();
        Matrix4f m = new Matrix4f(context.getMatrices().peek().getPositionMatrix());
        context.getMatrices().peek().getPositionMatrix().set(m.scale(scale, scale, 1.0f));

        int x = Math.round(10 / scale);
        int y = Math.round(10 / scale);
        int lineHeight = Math.round(10 / scale);

        String countText = String.format("Entities: %d (§aP:%d §7N:%d §cH:%d§r)",
                totalCount, passiveCount, neutralCount, hostileCount);
        context.drawTextWithShadow(client.textRenderer, countText, x, y, 0xFFFFFF);
        y += Math.round(12 / scale);

        context.drawTextWithShadow(client.textRenderer, "─────────────────", x, y, 0x808080);
        y += lineHeight;

        for (Entity entity : entities) {
            String text = String.format("%s - %.2fm", entity.getName().getString(), entity.distanceTo(client.player));
            context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
            y += lineHeight;
        }

        context.getMatrices().pop();
    }
}

