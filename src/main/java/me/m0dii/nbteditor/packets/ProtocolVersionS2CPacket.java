package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record ProtocolVersionS2CPacket(int version) implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "protocol_version");

    public ProtocolVersionS2CPacket(PacketByteBuf payload) {
        this(payload.readVarInt());
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.writeVarInt(version);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
