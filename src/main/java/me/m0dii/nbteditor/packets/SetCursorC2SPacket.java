package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record SetCursorC2SPacket(ItemStack item) implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "set_cursor");

    public SetCursorC2SPacket(PacketByteBuf payload) {
        this(payload.m0_dev_tools$readItemStack());
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.m0_dev_tools$writeItemStack(item);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
