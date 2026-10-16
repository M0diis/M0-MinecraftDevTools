package me.m0dii.modules.messagehistory;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;

public class MessageHistoryModule extends Module {

    public static final MessageHistoryModule INSTANCE = new MessageHistoryModule();

    protected MessageHistoryModule() {
        super("message_history", "Message History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.MESSAGE_HISTORY.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.MESSAGE_HISTORY.defaultKey(),
                client -> client.gui.setScreen(MessageHistoryScreen.create(client.gui.screen())));
    }
}
