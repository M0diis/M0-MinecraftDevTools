package me.m0dii.modules.hungertweaks.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HungerTweaksSaturationSyncPayload(float saturation) implements CustomPayload {
    public static final PacketCodec<PacketByteBuf, HungerTweaksSaturationSyncPayload> CODEC = CustomPayload.codecOf(HungerTweaksSaturationSyncPayload::write, HungerTweaksSaturationSyncPayload::new);
    public static final CustomPayload.Id<HungerTweaksSaturationSyncPayload> ID = new Id<>(Identifier.of("m0-dev-tools", "hunger_tweaks_saturation"));

    public HungerTweaksSaturationSyncPayload(PacketByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(PacketByteBuf buf) {
        buf.writeFloat(saturation);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
