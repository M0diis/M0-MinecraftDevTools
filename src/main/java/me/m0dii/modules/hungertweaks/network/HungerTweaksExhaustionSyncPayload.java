package me.m0dii.modules.hungertweaks.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HungerTweaksExhaustionSyncPayload(float exhaustion) implements CustomPayload {
    public static final PacketCodec<PacketByteBuf, HungerTweaksExhaustionSyncPayload> CODEC = CustomPayload.codecOf(HungerTweaksExhaustionSyncPayload::write, HungerTweaksExhaustionSyncPayload::new);
    public static final CustomPayload.Id<HungerTweaksExhaustionSyncPayload> ID = new Id<>(Identifier.of("m0-dev-tools", "hunger_tweaks_exhaustion"));

    public HungerTweaksExhaustionSyncPayload(PacketByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(PacketByteBuf buf) {
        buf.writeFloat(exhaustion);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
