package me.m0dii.modules.hungertweaks.network;

import me.m0dii.modules.hungertweaks.HungerTweaksFoodHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HungerTweaksSyncHandler {
    private static final Map<UUID, Float> LAST_SATURATION_LEVELS = new HashMap<>();
    private static final Map<UUID, Float> LAST_EXHAUSTION_LEVELS = new HashMap<>();

    private HungerTweaksSyncHandler() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(HungerTweaksExhaustionSyncPayload.ID, HungerTweaksExhaustionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HungerTweaksSaturationSyncPayload.ID, HungerTweaksSaturationSyncPayload.CODEC);
    }

    public static void onPlayerUpdate(ServerPlayerEntity player) {
        Float lastSaturationLevel = LAST_SATURATION_LEVELS.get(player.getUuid());
        Float lastExhaustionLevel = LAST_EXHAUSTION_LEVELS.get(player.getUuid());

        float saturation = player.getHungerManager().getSaturationLevel();
        if (lastSaturationLevel == null || lastSaturationLevel != saturation) {
            ServerPlayNetworking.send(player, new HungerTweaksSaturationSyncPayload(saturation));
            LAST_SATURATION_LEVELS.put(player.getUuid(), saturation);
        }

        float exhaustionLevel = HungerTweaksFoodHelper.getExhaustion(player.getHungerManager());
        if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f) {
            ServerPlayNetworking.send(player, new HungerTweaksExhaustionSyncPayload(exhaustionLevel));
            LAST_EXHAUSTION_LEVELS.put(player.getUuid(), exhaustionLevel);
        }
    }

    public static void onPlayerLoggedIn(ServerPlayerEntity player) {
        LAST_SATURATION_LEVELS.remove(player.getUuid());
        LAST_EXHAUSTION_LEVELS.remove(player.getUuid());
    }
}
