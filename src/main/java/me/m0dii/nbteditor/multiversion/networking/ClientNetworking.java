package me.m0dii.nbteditor.multiversion.networking;

import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.util.MiscUtil;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClientNetworking {

    private ClientNetworking() {
    }

    private static final Map<Identifier, List<Consumer<ModPacket>>> listeners = new HashMap<>();

    public static void onPlayStart(ClientPlayNetworkHandler networkHandler) {
        DynamicRegistryManagerHolder.setClientManager(networkHandler);
        PlayNetworkStateEvents.Start.EVENT.invoker().onPlayStart(networkHandler);
    }

    public static void onPlayJoin() {
        PlayNetworkStateEvents.Join.EVENT.invoker().onPlayJoin();
    }

    public static void onPlayStop() {
        PlayNetworkStateEvents.Stop.EVENT.invoker().onPlayStop();
        DynamicRegistryManagerHolder.setClientManager(null);
    }

    @SuppressWarnings("deprecation")
    public static void send(ModPacket packet) {
        MVMisc.sendC2SPacket(MVPacketCustomPayload.wrapC2S(packet));
    }

    @SuppressWarnings("unchecked")
    public static <T extends ModPacket> void registerListener(Identifier id, Consumer<T> listener) {
        listeners.computeIfAbsent(id, key -> new ArrayList<>()).add(packet -> listener.accept((T) packet));
    }

    public static void callListeners(ModPacket packet) {
        if (!MiscUtil.client.isOnThread()) {
            MiscUtil.client.execute(() -> callListeners(packet));
            return;
        }
        List<Consumer<ModPacket>> specificListeners = listeners.get(packet.getPacketId());
        if (specificListeners == null) {
            return;
        }
        specificListeners.forEach(listener -> listener.accept(packet));
    }

    public static class PlayNetworkStateEvents {
        public interface Start {
            Event<Start> EVENT = EventFactory.createArrayBacked(Start.class, listeners -> networkHandler -> {
                for (Start listener : listeners) {
                    listener.onPlayStart(networkHandler);
                }
            });

            void onPlayStart(ClientPlayNetworkHandler networkHandler);
        }

        public interface Join {
            Event<Join> EVENT = EventFactory.createArrayBacked(Join.class, listeners -> () -> {
                for (Join listener : listeners) {
                    listener.onPlayJoin();
                }
            });

            void onPlayJoin();
        }

        public interface Stop {
            Event<Stop> EVENT = EventFactory.createArrayBacked(Stop.class, listeners -> () -> {
                for (Stop listener : listeners) {
                    listener.onPlayStop();
                }
            });

            void onPlayStop();
        }
    }

}
