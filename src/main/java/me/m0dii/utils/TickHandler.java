package me.m0dii.utils;

import me.m0dii.modules.freecam.CameraEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TickHandler {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ;
            CameraEntity.movementTick();
        });
    }
}
