package me.m0dii.modules.freecam;

import me.m0dii.modules.Module;
import me.m0dii.modules.macros.MacroPlaceholderProvider;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class FreecamModule extends Module {

    public static final FreecamModule INSTANCE = new FreecamModule();

    private static final MacroPlaceholderProvider PLACEHOLDER_PROVIDER = new MacroPlaceholderProvider() {
        @Override
        public String getProviderId() {
            return "freecam";
        }

        @Override
        public List<String> getPlaceholderDocs() {
            return List.of(
                    "[Module placeholders: Freecam]",
                    "{freecam.enabled} => true when freecam is active"
            );
        }

        @Override
        public List<String> getKnownPlaceholderTokens() {
            return List.of("freecam.enabled");
        }

        @Override
        public String resolvePlaceholder(String token, MinecraftClient client, PlayerEntity player, boolean canvasMode) {
            if ("freecam.enabled".equals(token)) {
                return Boolean.toString(INSTANCE.isEnabled());
            }
            return null;
        }
    };

    private boolean showPlayerModel = true;

    protected FreecamModule() {
        super("freecam", "Freecam", false);
    }

    @Override
    public void register() {
        MacroPlaceholders.registerProvider(PLACEHOLDER_PROVIDER);
        registerPressedKeybind(KeybindCatalog.FREECAM_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.FREECAM_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Override
    public void onEnable() {
        if (getClient().world == null || getClient().player == null) {
            this.enabled = false;
            return;
        }
        CameraEntity.setCameraState(true);
    }

    @Override
    public void onDisable() {
        CameraEntity.setCameraState(false);
    }

    public boolean shouldShowPlayerModel() {
        return showPlayerModel;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Show Player Model: " + (showPlayerModel ? "ON" : "OFF"));
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            showPlayerModel = !showPlayerModel;
            return;
        }
        if (settingIndex == 1) {
            toggleEnabled();
        }
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return RestrictedModuleOptInNetworking.FREECAM_CHANNEL_ID;
    }
}
