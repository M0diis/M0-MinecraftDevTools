package me.m0dii.modules.entityradar;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EntityRadarModule extends Module {

    public static final EntityRadarModule INSTANCE = new EntityRadarModule();

    private EntityRadarModule() {
        super("entity_radar", "Entity Radar", false);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);

        EntityRadarOverlay.register();
        EntityHighlightRenderer.register();

        registerPressedKeybind("key.m0-dev-tools.open_entity_radar_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, client -> {
            client.setScreen(EntityRadarScreen.create(client.currentScreen));
        });
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!EntityRadarModule.INSTANCE.isEnabled()) {
            return;
        }

        if (getClient().player == null) {
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
        context.drawTextWithShadow(getClient().textRenderer, countText, 10, y, 0xFFFFFF);
        y += 12;

        context.drawTextWithShadow(getClient().textRenderer, "─────────────────", 10, y, 0x808080);
        y += 10;

        for (Entity entity : entities) {
            String text = String.format("%s - %.2fm", entity.getName().getString(), entity.distanceTo(getClient().player));
            context.drawTextWithShadow(getClient().textRenderer, text, 10, y, 0xFFFFFF);
            y += 10;
        }
    }

    public List<Entity> getEntities() {
        if (getClient().player == null || getClient().world == null) {
            return List.of();
        }

        Box box = new Box(getClient().player.getPos(), getClient().player.getPos()).expand(ModConfig.entityRadarRadius);
        return getClient().world.getOtherEntities(getClient().player, box).stream()
                .sorted((a, b) -> (int) (a.distanceTo(getClient().player) - b.distanceTo(getClient().player)))
                .toList();
    }

    public int getPassiveCount() {
        return (int) getEntities().stream()
                .filter(entity -> entity instanceof PassiveEntity || entity instanceof AnimalEntity)
                .count();
    }

    public int getHostileCount() {
        return (int) getEntities().stream()
                .filter(Monster.class::isInstance)
                .count();
    }

    public int getNeutralCount() {
        List<Entity> entities = getEntities();
        int total = entities.size();
        int passive = getPassiveCount();
        int hostile = getHostileCount();
        return total - passive - hostile;
    }
}

