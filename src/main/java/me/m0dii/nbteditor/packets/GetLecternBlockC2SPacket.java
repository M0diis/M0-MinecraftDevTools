package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record GetLecternBlockC2SPacket(int requestId) implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "get_lectern_block");

    public GetLecternBlockC2SPacket(PacketByteBuf payload) {
        this(payload.readVarInt());
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.writeVarInt(requestId);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
