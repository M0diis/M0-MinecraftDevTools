package me.m0dii.modules.hudtweaks;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import java.util.List;
import java.util.Locale;

public final class HudTweaksModule extends Module {
    public static final HudTweaksModule INSTANCE = new HudTweaksModule();
    private boolean registered;
    private HudTweaksModule() {
        super("hud_tweaks", "HUD Tweaks", true);
    }
    @Override
    public void register() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        HudTweaksSettings.load();
        this.enabled = HudTweaksSettings.get().moduleEnabled;
        bind(VanillaHudElements.CROSSHAIR, HudTweaksSettings.ElementType.CROSSHAIR);
        bind(VanillaHudElements.SPECTATOR_MENU, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.HOTBAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.ARMOR_BAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.HEALTH_BAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.FOOD_BAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.AIR_BAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.MOUNT_HEALTH, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.INFO_BAR, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.EXPERIENCE_LEVEL, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.HELD_ITEM_TOOLTIP, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.SPECTATOR_TOOLTIP, HudTweaksSettings.ElementType.HOTBAR_GROUP);
        bind(VanillaHudElements.STATUS_EFFECTS, HudTweaksSettings.ElementType.STATUS_EFFECT);
        bind(VanillaHudElements.SCOREBOARD, HudTweaksSettings.ElementType.SCOREBOARD);
        bind(VanillaHudElements.OVERLAY_MESSAGE, HudTweaksSettings.ElementType.ACTION_BAR);
        bind(VanillaHudElements.TITLE_AND_SUBTITLE, HudTweaksSettings.ElementType.SCREEN_TITLE);
        bind(VanillaHudElements.PLAYER_LIST, HudTweaksSettings.ElementType.PLAYER_LIST);
    }
    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        HudTweaksSettings.updateAndSave(() -> HudTweaksSettings.get().moduleEnabled = this.enabled);
    }
    @Override
    public List<String> getSettingsDisplay() {
        HudTweaksSettings.Data data = HudTweaksSettings.get();
        HudTweaksSettings.ElementConfig crosshair = data.elements.get(HudTweaksSettings.ElementType.CROSSHAIR);
        HudTweaksSettings.ElementConfig hotbar = data.elements.get(HudTweaksSettings.ElementType.HOTBAR_GROUP);
        return List.of(
                "Toggle: " + (this.enabled ? "ON" : "OFF"),
                "Crosshair Scale: " + format(crosshair == null ? 1.0f : crosshair.scale),
                "Hotbar Scale: " + format(hotbar == null ? 1.0f : hotbar.scale),
                "Crosshair Opacity: " + format(crosshair == null ? 1.0f : crosshair.opacity),
                "Config: Macro Workbench -> Configuration -> HUD Tweaks"
        );
    }
    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            toggleEnabled();
        }
    }
    private static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
    private static void bind(Identifier elementId, HudTweaksSettings.ElementType type) {
        HudElementRegistry.replaceElement(elementId, original -> (context, tickCounter) -> {
            if (!INSTANCE.isEnabled()) {
                original.render(context, tickCounter);
                return;
            }
            HudTweaksSettings.ElementConfig cfg = HudTweaksSettings.getElement(type);
            if (!cfg.display) {
                return;
            }

            if (!HudTweaksRenderState.begin(type, context.getMatrices())) {
                return;
            }
            try {
                original.render(context, tickCounter);
            } finally {
                HudTweaksRenderState.end(context.getMatrices());
            }
        });
    }
}
