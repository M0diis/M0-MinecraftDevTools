package me.m0dii.nbteditor.multiversion.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;

/**
 * Used internally in multiversion.networking; DO NOT USE
 */
@Deprecated
public record MVPacketCustomPayload(ModPacket packet) implements CustomPayload {

    /**
     * Hides the {@link CustomPayload} in {@link CustomPayloadC2SPacket#CustomPayloadC2SPacket(CustomPayload)}
     */
    public static CustomPayloadC2SPacket wrapC2S(ModPacket packet) {
        return new CustomPayloadC2SPacket(new MVPacketCustomPayload(packet));
    }

    /**
     * Hides the {@link CustomPayload} in {@link CustomPayloadS2CPacket#CustomPayloadS2CPacket(CustomPayload)}
     */
    public static CustomPayloadS2CPacket wrapS2C(ModPacket packet) {
        return new CustomPayloadS2CPacket(new MVPacketCustomPayload(packet));
    }

    /**
     * Hides the {@link CustomPayload} in {@link CustomPayloadC2SPacket#payload()}
     */
    public static ModPacket unwrapC2S(CustomPayloadC2SPacket packet) {
        if (packet.payload() instanceof MVPacketCustomPayload mvPacket) {
            return mvPacket.packet();
        }
        return null;
    }

    /**
     * Hides the {@link CustomPayload} in {@link CustomPayloadS2CPacket#payload()}
     */
    public static ModPacket unwrapS2C(CustomPayloadS2CPacket packet) {
        if (packet.payload() instanceof MVPacketCustomPayload mvPacket) {
            return mvPacket.packet();
        }
        return null;
    }

    @Override
    public Id<MVPacketCustomPayload> getId() {
        return new Id<>(packet.getPacketId());
    }

    // write
    public void method_53028(PacketByteBuf payload) {
        packet.write(payload);
    }

    // id
    public Identifier comp_1678() {
        return packet.getPacketId();
    }

}
