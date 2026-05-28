package me.m0dii.modules.chat;

import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.util.InputUtil;

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
                InputUtil.Type.KEYSYM,
                KeybindCatalog.SECONDARY_CHAT_TOGGLE.defaultKey(),
                client -> {
                    SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().enabled = !SecondaryChatSettings.get().enabled);
                    setEnabled(SecondaryChatSettings.get().enabled);
                });
    }
}
