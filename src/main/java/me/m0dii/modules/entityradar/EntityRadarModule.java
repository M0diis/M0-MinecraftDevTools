package me.m0dii.modules.entityradar;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import me.m0dii.utils.KeybindCatalog;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EntityRadarModule extends Module implements me.m0dii.modules.NumericSetting.Provider {

    public static final EntityRadarModule INSTANCE = new EntityRadarModule();

    private final EntityHighlightRenderer worldRenderer = new EntityHighlightRenderer();
    private final EntityRadarHudOverlay hudRenderer = new EntityRadarHudOverlay();

    private int RADIUS = 64;

    private EntityRadarModule() {
        super("entity_radar", "Entity Radar", false);
    }

    @Override
    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "entity_radar_hud"),
                hudRenderer::onHudRender
        );

        worldRenderer.register();

        registerPressedKeybind(KeybindCatalog.ENTITY_RADAR_SCREEN.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.ENTITY_RADAR_SCREEN.defaultKey(),
                client -> client.gui.setScreen(EntityRadarScreen.create(client.gui.screen())));
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return RestrictedModuleOptInNetworking.ENTITY_RADAR_CHANNEL_ID;
    }

    @Override
    public List<me.m0dii.modules.NumericSetting> numericSettings() {
        return List.of(me.m0dii.modules.NumericSetting.of(3, "Radius", 1, 256, 1, 0,
                () -> RADIUS, v -> RADIUS = (int) Math.round(v), 4, 5));
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("World Renderer: " + (worldRenderer.isEnabled() ? "ON" : "OFF"));
        settings.add("Tracers: " + (worldRenderer.isTracersEnabled() ? "ON" : "OFF"));
        settings.add("HUD Renderer: " + (hudRenderer.isEnabled() ? "ON" : "OFF"));
        settings.add("Radius: " + (RADIUS));
        settings.add("Radius (+)");
        settings.add("Radius (-)");

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> worldRenderer.setEnabled(!worldRenderer.isEnabled());
            case 1 -> worldRenderer.setTracersEnabled(!worldRenderer.isTracersEnabled());
            case 2 -> hudRenderer.setEnabled(!hudRenderer.isEnabled());
            case 4 -> RADIUS++;
            case 5 -> RADIUS = Math.max(1, RADIUS - 1);
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
        if (getClient().player == null || getClient().level == null) {
            return List.of();
        }

        var playerPos = new Vec3(getClient().player.getX(), getClient().player.getY(), getClient().player.getZ());
        AABB box = new AABB(playerPos, playerPos).inflate(ModConfig.entityRadarRadius);
        return getClient().level.getEntities(getClient().player, box).stream()
                .sorted((a, b) -> Double.compare(a.distanceTo(getClient().player), b.distanceTo(getClient().player)))
                .toList();
    }

    public int getPassiveCount() {
        return (int) getEntities().stream()
                .filter(entity -> entity instanceof AgeableMob || entity instanceof Animal)
                .count();
    }

    public int getHostileCount() {
        return (int) getEntities().stream()
                .filter(Enemy.class::isInstance)
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
