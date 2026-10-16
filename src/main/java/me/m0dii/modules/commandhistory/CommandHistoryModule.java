package me.m0dii.modules.commandhistory;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;

public class CommandHistoryModule extends Module {

    public static final CommandHistoryModule INSTANCE = new CommandHistoryModule();

    protected CommandHistoryModule() {
        super("command_history", "Command History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.COMMAND_HISTORY.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.COMMAND_HISTORY.defaultKey(),
                client -> client.gui.setScreen(CommandHistoryScreen.create(client.gui.screen())));
    }
}
