package me.m0dii.modules.chat;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;

public class SecondaryChatModule extends Module {

    public static final SecondaryChatModule INSTANCE = new SecondaryChatModule();

    protected SecondaryChatModule() {
        super("secondary_chat", "Secondary Chat", true);
    }

    @Override
    public void register() {
        setEnabled(SecondaryChatSettings.get().enabled);
        SecondaryChatOverlay.register();
        SecondaryChatInputRouter.register();
        SecondaryChatCommands.register();
        SecondaryChatInteraction.register();

        registerPressedKeybind(KeybindCatalog.SECONDARY_CHAT_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.SECONDARY_CHAT_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().enabled = this.enabled);
    }
}
