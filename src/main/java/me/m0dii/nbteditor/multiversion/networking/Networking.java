package me.m0dii.nbteditor.multiversion.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Networking {

    private static final Map<Identifier, Function<PacketByteBuf, ModPacket>> constructors = new HashMap<>();

    public static void registerPacket(Identifier id, Function<PacketByteBuf, ModPacket> constructor) {
        constructors.put(id, constructor);
    }

    public static boolean isPacket(Identifier id) {
        return constructors.containsKey(id);
    }

    public static ModPacket readPacket(Identifier id, PacketByteBuf payload) {
        Function<PacketByteBuf, ModPacket> constructor = constructors.get(id);
        if (constructor == null) {
            return null;
        }
        return constructor.apply(payload);
    }

}
