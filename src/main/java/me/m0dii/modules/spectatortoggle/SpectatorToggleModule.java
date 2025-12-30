package me.m0dii.modules.spectatortoggle;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SpectatorToggleModule extends Module {

    public static final SpectatorToggleModule INSTANCE = new SpectatorToggleModule();

    private SpectatorToggleModule() {
        super("spectator_toggle", "Spectator Toggle", false);
    }

    @Override
    public void register() {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.m0-dev-tools.spectator_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.m0-dev-tools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (kb.wasPressed()) {
                if (client.player != null) {
                    if (client.player.hasPermissionLevel(2)) {
                        if (client.player.isSpectator()) {
                            client.player.networkHandler.sendCommand("gamemode creative");
                        } else {
                            client.player.networkHandler.sendCommand("gamemode spectator");
                        }
                    }
                }
            }
        });
    }
}
