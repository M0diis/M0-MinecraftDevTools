package me.m0dii.modules.messagehistory;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MessageHistoryModule extends Module {

    public static final MessageHistoryModule INSTANCE = new MessageHistoryModule();

    protected MessageHistoryModule() {
        super("message_history", "Message History", true);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.message_history",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                client -> client.setScreen(MessageHistoryScreen.create(client.currentScreen)));
    }
}
