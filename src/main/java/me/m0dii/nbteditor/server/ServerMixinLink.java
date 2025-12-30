package me.m0dii.nbteditor.server;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMixinLink {

    public static final Map<Thread, PlayerEntity> SCREEN_HANDLER_OWNER = new ConcurrentHashMap<>();
    public static final WeakHashMap<Slot, PlayerEntity> SLOT_OWNER = new WeakHashMap<>();
    public static final WeakHashMap<ServerPlayerEntity, Boolean> NO_SLOT_RESTRICTIONS_PLAYERS = new WeakHashMap<>();

    // Fake players show as a clientbound ClientConnection
    private static final Class<?> ClientPlayNetworkHandler = ClientPlayNetworkHandler.class;

    public static boolean isInstanceOfClientPlayNetworkHandlerSafely(PacketListener listener) {
        return ClientPlayNetworkHandler != null && ClientPlayNetworkHandler.isInstance(listener);
    }

}
