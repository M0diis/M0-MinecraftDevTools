package me.m0dii.modules.hungertweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HungerTweaksSaturationSyncPayload(float saturation) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, HungerTweaksSaturationSyncPayload> CODEC = CustomPacketPayload.codec(HungerTweaksSaturationSyncPayload::write, HungerTweaksSaturationSyncPayload::new);
    public static final CustomPacketPayload.Type<HungerTweaksSaturationSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("m0-dev-tools", "hunger_tweaks_saturation"));

    public HungerTweaksSaturationSyncPayload(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(saturation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
