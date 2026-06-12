package me.m0dii.modules.mobdrops;

import me.m0dii.modules.macros.MacroPlaceholderProvider;
import me.m0dii.modules.macros.MacroPlaceholders;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.Locale;

public final class MobDropTrackerClient {
    private static boolean registered;

    private static final MacroPlaceholderProvider PLACEHOLDER_PROVIDER = new MacroPlaceholderProvider() {
        @Override
        public String getProviderId() {
            return "mobdrops";
        }

        @Override
        public List<String> getPlaceholderDocs() {
            return List.of(
                    "[Module placeholders: Mob Drops]",
                    "{mobdrops.trackers} => comma-separated tracker names",
                    "{mobdrops.count:<name>} => total item count for a tracker",
                    "{mobdrops.dpm:<name>} => observed items per minute for a tracker",
                    "{mobdrops.kills:<name>} => kill count for a tracker",
                    "{mobdrops.unique:<name>} => unique item ids for a tracker",
                    "{mobdrops.top:<name>} => top tracked item summary",
                    "{mobdrops.items:<name>} => compact multi-item summary"
            );
        }

        @Override
        public List<String> getKnownPlaceholderTokens() {
            return List.of(
                "mobdrops.trackers",
                "mobdrops.count:<name>",
                "mobdrops.dpm:<name>",
                "mobdrops.kills:<name>",
                "mobdrops.unique:<name>",
                "mobdrops.top:<name>",
                    "mobdrops.items:<name>"
            );
        }

        @Override
        public String resolvePlaceholder(String token, MinecraftClient client, PlayerEntity player, boolean canvasMode) {
            if ("mobdrops.trackers".equals(token)) {
                return MobDropTrackerClientState.trackersToken();
            }
            if (token == null) {
                return null;
            }

            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("mobdrops.count:")) {
                return MobDropTrackerClientState.totalCount(token.substring("mobdrops.count:".length()));
            }
            if (lower.startsWith("mobdrops.dpm:")) {
                return MobDropTrackerClientState.dropsPerMinute(token.substring("mobdrops.dpm:".length()));
            }
            if (lower.startsWith("mobdrops.kills:")) {
                return MobDropTrackerClientState.killCount(token.substring("mobdrops.kills:".length()));
            }
            if (lower.startsWith("mobdrops.unique:")) {
                return MobDropTrackerClientState.uniqueCount(token.substring("mobdrops.unique:".length()));
            }
            if (lower.startsWith("mobdrops.top:")) {
                return MobDropTrackerClientState.topSummary(token.substring("mobdrops.top:".length()));
            }
            if (lower.startsWith("mobdrops.items:")) {
                return MobDropTrackerClientState.itemSummary(token.substring("mobdrops.items:".length()), 4);
            }
            return null;
        }
    };

    private MobDropTrackerClient() {
    }

    public static void registerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        MacroPlaceholders.registerProvider(PLACEHOLDER_PROVIDER);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MobDropTrackerClientState.clear());
        ClientPlayNetworking.registerGlobalReceiver(MobDropTrackerPayloads.StatePayload.ID, (payload, context) ->
                context.client().execute(() -> MobDropTrackerClientState.apply(payload)));
    }
}
