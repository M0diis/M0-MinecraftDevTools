package me.m0dii.modules.messagehistory;

import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.util.InputUtil;

public class MessageHistoryModule extends Module {

    public static final MessageHistoryModule INSTANCE = new MessageHistoryModule();

    protected MessageHistoryModule() {
        super("message_history", "Message History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.MESSAGE_HISTORY.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.MESSAGE_HISTORY.defaultKey(),
                client -> client.setScreen(MessageHistoryScreen.create(client.currentScreen)));
    }
}
