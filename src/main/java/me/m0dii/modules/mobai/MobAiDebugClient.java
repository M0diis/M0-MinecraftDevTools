package me.m0dii.modules.mobai;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MobAiDebugClient {
    private static boolean registered;

    private MobAiDebugClient() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(MobAiDebugPayloads.InspectPayload.ID, (payload, context) ->
                context.client().execute(() -> MobAiDebugClientState.setInspect(payload.entityId(), payload.lines())));

        ClientPlayNetworking.registerGlobalReceiver(MobAiDebugPayloads.PathPreviewPayload.ID, (payload, context) ->
                context.client().execute(() -> MobAiDebugClientState.setPathPreview(payload)));

        ClientPlayNetworking.registerGlobalReceiver(MobAiDebugPayloads.ClearPayload.ID, (payload, context) ->
                context.client().execute(() -> MobAiDebugClientState.clear(payload.clearInspect(), payload.clearPathPreview())));

        ClientPlayNetworking.registerGlobalReceiver(MobAiDebugPayloads.TrackerConfigPayload.ID, (payload, context) ->
                context.client().execute(() -> MobAiDebugClientState.setTrackerConfig(payload.enabledDisplays(),
                        payload.showBoxes(),
                        payload.alpha(),
                        payload.radius(),
                        payload.hostileFocus())));
    }
}
