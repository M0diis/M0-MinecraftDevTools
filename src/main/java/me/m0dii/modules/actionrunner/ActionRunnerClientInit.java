package me.m0dii.modules.actionrunner;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class ActionRunnerClientInit {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ActionRunnerClientCommand.register(dispatcher);
        });
    }
}
