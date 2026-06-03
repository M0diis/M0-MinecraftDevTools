package me.m0dii.modules.hungertweaks.network;

import me.m0dii.modules.hungertweaks.HungerTweaksFoodHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class HungerTweaksClientSyncHandler {
    private static boolean initialized;

    private HungerTweaksClientSyncHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayNetworking.registerGlobalReceiver(HungerTweaksExhaustionSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player != null) {
                        HungerTweaksFoodHelper.setExhaustion(context.client().player.getHungerManager(), payload.exhaustion());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(HungerTweaksSaturationSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player != null) {
                        context.client().player.getHungerManager().setSaturationLevel(payload.saturation());
                    }
                }));
    }
}
