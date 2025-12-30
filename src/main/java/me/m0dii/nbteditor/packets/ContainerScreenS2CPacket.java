package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ContainerScreenS2CPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "container_screen");

    public ContainerScreenS2CPacket() {
    }

    public ContainerScreenS2CPacket(PacketByteBuf payload) {
    }

    @Override
    public void write(PacketByteBuf payload) {
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
