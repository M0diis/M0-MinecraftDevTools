package me.m0dii.utils;

import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TickHandler {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (FreecamModule.INSTANCE.isEnabled()) {
                if (client.player == null || client.world == null) {
                    FreecamModule.INSTANCE.setEnabled(false);
                } else if (CameraEntity.getCamera() == null) {
                    CameraEntity.setCameraState(true);
                } else if (client.getCameraEntity() != CameraEntity.getCamera()) {
                    client.setCameraEntity(CameraEntity.getCamera());
                }
            } else if (CameraEntity.getCamera() != null) {
                CameraEntity.setCameraState(false);
            }

            CameraEntity.movementTick();
        });
    }
}
