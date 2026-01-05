package me.m0dii.modules.entityradar;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class EntityRadarModule extends Module {

    public static final EntityRadarModule INSTANCE = new EntityRadarModule();

    private final EntityHighlightRenderer worldRenderer = new EntityHighlightRenderer();
    private final EntityRadarHudOverlay hudRenderer = new EntityRadarHudOverlay();

    private EntityRadarModule() {
        super("entity_radar", "Entity Radar", false);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(hudRenderer::onHudRender);

        worldRenderer.register();

        registerPressedKeybind("key.m0-dev-tools.open_entity_radar_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                client -> client.setScreen(EntityRadarScreen.create(client.currentScreen)));
    }


    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("World Renderer: " + (worldRenderer.isEnabled() ? "ON" : "OFF"));
        settings.add("HUD Renderer: " + (hudRenderer.isEnabled() ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> worldRenderer.setEnabled(!worldRenderer.isEnabled());
            case 1 -> hudRenderer.setEnabled(!hudRenderer.isEnabled());
            default -> {
                // Do nothing
            }
        }
    }

    @Override
    public void onEnable() {
        hudRenderer.setEnabled(true);
        worldRenderer.setEnabled(true);
    }

    @Override
    public void onDisable() {
        hudRenderer.setEnabled(false);
        worldRenderer.setEnabled(false);
    }

    public List<Entity> getEntities() {
        if (getClient().player == null || getClient().world == null) {
            return List.of();
        }

        Box box = new Box(getClient().player.getPos(), getClient().player.getPos()).expand(ModConfig.entityRadarRadius);
        return getClient().world.getOtherEntities(getClient().player, box).stream()
                .sorted((a, b) -> Double.compare(a.distanceTo(getClient().player), b.distanceTo(getClient().player)))
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

