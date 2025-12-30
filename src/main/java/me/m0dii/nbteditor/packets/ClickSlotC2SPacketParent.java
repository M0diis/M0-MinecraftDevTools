package me.m0dii.nbteditor.packets;

public interface ClickSlotC2SPacketParent {
    default boolean isNoSlotRestrictions() {
        throw new RuntimeException("Missing implementation for ClickSlotC2SPacketParent#isNoSlotRestrictions");
    }
}
