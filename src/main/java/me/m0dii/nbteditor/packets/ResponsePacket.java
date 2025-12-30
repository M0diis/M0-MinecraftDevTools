package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.networking.ModPacket;

public interface ResponsePacket extends ModPacket {
    int requestId();
}
