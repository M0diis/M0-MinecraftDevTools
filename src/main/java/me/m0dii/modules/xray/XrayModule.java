package me.m0dii.modules.xray;

import me.m0dii.modules.Module;
import me.m0dii.modules.xray.network.XrayOptInNetworking;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class XrayModule extends Module {

    public static final XrayModule INSTANCE = new XrayModule();

    public XrayModule() {
        super("xray", "Xray", false);
    }

    @Override
    public void register() {
        XrayManager.load();
        XrayOutlineRenderer.register();

        registerPressedKeybind(KeybindCatalog.XRAY_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.XRAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());

        registerPressedKeybind(KeybindCatalog.XRAY_MENU.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.XRAY_MENU.defaultKey(),
                client -> client.setScreen(XrayConfigScreen.create(client.currentScreen)));
    }

    @Override
    protected void onDisable() {
        XrayOutlineRenderer.clearCache();
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return XrayOptInNetworking.CHANNEL_ID;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Server Opt-In: " + getServerSideOptInStatus());
        settings.add("Range +4: " + XrayManager.getDisplayRange());
        settings.add("Range -4: " + XrayManager.getDisplayRange());
        settings.add("Tracked Blocks: " + XrayManager.getBlockIds().size());
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 2 -> XrayManager.adjustDisplayRange(4);
            case 3 -> XrayManager.adjustDisplayRange(-4);
            default -> {
            }
        }
    }
}
