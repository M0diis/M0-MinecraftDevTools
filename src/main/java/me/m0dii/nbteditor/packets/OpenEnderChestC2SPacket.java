package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class OpenEnderChestC2SPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "open_ender_chest");

    public OpenEnderChestC2SPacket() {
    }

    public OpenEnderChestC2SPacket(PacketByteBuf payload) {
    }

    @Override
    public void write(PacketByteBuf payload) {
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
