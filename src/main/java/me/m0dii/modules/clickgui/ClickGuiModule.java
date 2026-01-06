package me.m0dii.modules.clickgui;

import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiModule extends Module {

    public static final ClickGuiModule INSTANCE = new ClickGuiModule();

    @Getter
    private ClickGuiRenderer renderer;

    private ClickGuiModule() {
        super("clickgui", "ClickGUI", true);
    }

    @Override
    public void register() {
        this.renderer = new ClickGuiRenderer();

        HudRenderCallback.EVENT.register(renderer::onHudRender);

        registerPressedKeybind(
                "key.m0-dev-tools.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                client -> renderer.toggle()
        );
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("GUI Scale: " + ModConfig.clickGuiTextScale);
        settings.add("Increase GUI Scale (+0.1)");
        settings.add("Increase GUI Scale (+1.0)");
        settings.add("Decrease GUI Scale (-0.1)");
        settings.add("Decrease GUI Scale (-1.0)");
        settings.add("Reset GUI Scale");
        settings.add("WASD navigation: " + (renderer != null && renderer.isWasdNavigation()));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        double newScale;

        switch (settingIndex) {
            case 1 -> newScale = Math.min(ModConfig.clickGuiTextScale + 0.1f, 5.0f);
            case 2 -> newScale = Math.min(ModConfig.clickGuiTextScale + 1.0f, 5.0f);
            case 3 -> newScale = Math.max(ModConfig.clickGuiTextScale - 0.1f, 0.5f);
            case 4 -> newScale = Math.max(ModConfig.clickGuiTextScale - 1.0f, 0.5f);
            case 5 -> newScale = 1.0f;
            case 6 -> {
                if (renderer != null) {
                    renderer.setWasdNavigation(!renderer.isWasdNavigation());
                }
                return;
            }
            default -> newScale = ModConfig.clickGuiTextScale;
        }

        ModConfig.updateAndSave(() -> ModConfig.clickGuiTextScale = newScale);
    }
}
