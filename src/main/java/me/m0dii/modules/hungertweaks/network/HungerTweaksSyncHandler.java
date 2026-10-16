package me.m0dii.modules.hungertweaks.network;

import me.m0dii.modules.hungertweaks.HungerTweaksFoodHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HungerTweaksSyncHandler {
    private static final Map<UUID, Float> LAST_SATURATION_LEVELS = new HashMap<>();
    private static final Map<UUID, Float> LAST_EXHAUSTION_LEVELS = new HashMap<>();

    private HungerTweaksSyncHandler() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.clientboundPlay().register(HungerTweaksExhaustionSyncPayload.ID, HungerTweaksExhaustionSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HungerTweaksSaturationSyncPayload.ID, HungerTweaksSaturationSyncPayload.CODEC);
    }

    public static void onPlayerUpdate(ServerPlayer player) {
        Float lastSaturationLevel = LAST_SATURATION_LEVELS.get(player.getUUID());
        Float lastExhaustionLevel = LAST_EXHAUSTION_LEVELS.get(player.getUUID());

        float saturation = player.getFoodData().getSaturationLevel();
        if (lastSaturationLevel == null || lastSaturationLevel != saturation) {
            ServerPlayNetworking.send(player, new HungerTweaksSaturationSyncPayload(saturation));
            LAST_SATURATION_LEVELS.put(player.getUUID(), saturation);
        }

        float exhaustionLevel = HungerTweaksFoodHelper.getExhaustion(player.getFoodData());
        if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f) {
            ServerPlayNetworking.send(player, new HungerTweaksExhaustionSyncPayload(exhaustionLevel));
            LAST_EXHAUSTION_LEVELS.put(player.getUUID(), exhaustionLevel);
        }
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        LAST_SATURATION_LEVELS.remove(player.getUUID());
        LAST_EXHAUSTION_LEVELS.remove(player.getUUID());
    }
}
