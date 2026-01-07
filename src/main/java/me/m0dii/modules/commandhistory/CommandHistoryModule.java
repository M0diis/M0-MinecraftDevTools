package me.m0dii.modules.commandhistory;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CommandHistoryModule extends Module {

    public static final CommandHistoryModule INSTANCE = new CommandHistoryModule();

    protected CommandHistoryModule() {
        super("command_history", "Command History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.command_history",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                client -> client.setScreen(CommandHistoryScreen.create(client.currentScreen)));
    }
}
