package me.m0dii.nbteditor.multiversion.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public interface ModPacket {
    void write(PacketByteBuf payload);

    Identifier getPacketId();
}
