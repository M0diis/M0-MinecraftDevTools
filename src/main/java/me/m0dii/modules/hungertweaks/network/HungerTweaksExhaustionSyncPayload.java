package me.m0dii.modules.hungertweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HungerTweaksExhaustionSyncPayload(float exhaustion) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, HungerTweaksExhaustionSyncPayload> CODEC = CustomPacketPayload.codec(HungerTweaksExhaustionSyncPayload::write, HungerTweaksExhaustionSyncPayload::new);
    public static final CustomPacketPayload.Type<HungerTweaksExhaustionSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("m0-dev-tools", "hunger_tweaks_exhaustion"));

    public HungerTweaksExhaustionSyncPayload(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(exhaustion);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
