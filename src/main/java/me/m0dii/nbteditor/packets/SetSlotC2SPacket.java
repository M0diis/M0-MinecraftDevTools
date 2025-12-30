package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class SetSlotC2SPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "set_slot");

    private final int slot;
    private final ItemStack item;

    public SetSlotC2SPacket(int slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public SetSlotC2SPacket(PacketByteBuf payload) {
        this.slot = payload.readVarInt();
        this.item = payload.m0_dev_tools$readItemStack();
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.writeVarInt(slot);
        payload.m0_dev_tools$writeItemStack(item);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
