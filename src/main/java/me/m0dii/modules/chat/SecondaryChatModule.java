package me.m0dii.modules.chat;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SecondaryChatModule extends Module {

    public static final SecondaryChatModule INSTANCE = new SecondaryChatModule();

    protected SecondaryChatModule() {
        super("secondary_chat", "Secondary Chat", true);
    }

    @Override
    public void register() {
        SecondaryChatOverlay.register();
        SecondaryChatInputRouter.register();
        SecondaryChatCommands.register();
        SecondaryChatInteraction.register();

        registerPressedKeybind("key.m0-dev-tools.toggle_secondary_chat",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                client -> {
                    ModConfig.updateAndSave(() -> ModConfig.secondaryChatEnabled = !ModConfig.secondaryChatEnabled);
                    setEnabled(ModConfig.secondaryChatEnabled);
                });
    }
}
