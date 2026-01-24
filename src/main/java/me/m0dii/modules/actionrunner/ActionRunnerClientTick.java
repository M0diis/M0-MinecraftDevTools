package me.m0dii.modules.actionrunner;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ActionRunnerClientTick {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ActionRunnerModule.getInstance().onClientTick();
        });
    }
}
