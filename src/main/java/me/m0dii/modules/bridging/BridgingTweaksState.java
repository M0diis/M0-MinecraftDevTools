package me.m0dii.modules.bridging;

import lombok.Getter;
import net.minecraft.client.Minecraft;

public final class BridgingTweaksState {
    @Getter
    private static BridgingTarget lastAssistTarget;
    static double lastKnownYFraction;

    private BridgingTweaksState() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            lastAssistTarget = null;
            return;
        }

        if (client.player.onGround()) {
            lastKnownYFraction = client.player.getY() - Math.floor(client.player.getY());
        }

        if (!BridgingTweaksModule.INSTANCE.isEnabled()) {
            lastAssistTarget = null;
            return;
        }

        lastAssistTarget = BridgingTweaksLogic.findAssistTarget(client, client.player);
    }

}
