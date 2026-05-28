package me.m0dii.modules.commandhistory;

import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.util.InputUtil;

public class CommandHistoryModule extends Module {

    public static final CommandHistoryModule INSTANCE = new CommandHistoryModule();

    protected CommandHistoryModule() {
        super("command_history", "Command History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.COMMAND_HISTORY.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.COMMAND_HISTORY.defaultKey(),
                client -> client.setScreen(CommandHistoryScreen.create(client.currentScreen)));
    }
}
