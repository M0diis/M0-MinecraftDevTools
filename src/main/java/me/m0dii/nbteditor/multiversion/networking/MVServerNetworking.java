package me.m0dii.nbteditor.multiversion.networking;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MVServerNetworking {

    private static final Map<Identifier, List<BiConsumer<ModPacket, ServerPlayerEntity>>> listeners = new HashMap<>();

    public static void onPlayStart(ServerPlayerEntity player) {
        PlayNetworkStateEvents.Start.EVENT.invoker().onPlayStart(player);
    }

    public static void onPlayStop(ServerPlayerEntity player) {
        PlayNetworkStateEvents.Stop.EVENT.invoker().onPlayStop(player);
    }

    @SuppressWarnings("deprecation")
    public static void send(ServerPlayerEntity player, ModPacket packet) {
        player.networkHandler.sendPacket(MVPacketCustomPayload.wrapS2C(packet));
    }

    @SuppressWarnings("unchecked")
    public static <T extends ModPacket> void registerListener(Identifier id, BiConsumer<T, ServerPlayerEntity> listener) {
        listeners.computeIfAbsent(id, key -> new ArrayList<>()).add((packet, player) -> listener.accept((T) packet, player));
    }

    public static void callListeners(ModPacket packet, ServerPlayerEntity player) {
        if (!player.server.isOnThread()) {
            player.server.execute(() -> callListeners(packet, player));
            return;
        }

        List<BiConsumer<ModPacket, ServerPlayerEntity>> specificListeners = listeners.get(packet.getPacketId());

        if (specificListeners == null) {
            return;
        }

        specificListeners.forEach(listener -> listener.accept(packet, player));
    }

    public static class PlayNetworkStateEvents {
        public interface Start {
            Event<Start> EVENT = EventFactory.createArrayBacked(Start.class, listeners -> player -> {
                for (Start listener : listeners) {
                    listener.onPlayStart(player);
                }
            });

            void onPlayStart(ServerPlayerEntity player);
        }

        public interface Stop {
            Event<Stop> EVENT = EventFactory.createArrayBacked(Stop.class, listeners -> player -> {
                for (Stop listener : listeners) {
                    listener.onPlayStop(player);
                }
            });

            void onPlayStop(ServerPlayerEntity player);
        }
    }

}
