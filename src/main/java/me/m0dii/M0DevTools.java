package me.m0dii;

import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.networking.Networking;
import me.m0dii.nbteditor.packets.*;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.utils.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class M0DevTools implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("m0-dev-tools");
    public static NBTEditorServer SERVER;

    public void onInitialize() {
        MidnightConfig.init("m0-dev-tools", ModConfig.class);

        Networking.registerPacket(ContainerScreenS2CPacket.ID, ContainerScreenS2CPacket::new);
        Networking.registerPacket(GetBlockC2SPacket.ID, GetBlockC2SPacket::new);
        Networking.registerPacket(GetEntityC2SPacket.ID, GetEntityC2SPacket::new);
        Networking.registerPacket(GetLecternBlockC2SPacket.ID, GetLecternBlockC2SPacket::new);
        Networking.registerPacket(OpenEnderChestC2SPacket.ID, OpenEnderChestC2SPacket::new);
        Networking.registerPacket(ProtocolVersionS2CPacket.ID, ProtocolVersionS2CPacket::new);
        Networking.registerPacket(SetBlockC2SPacket.ID, SetBlockC2SPacket::new);
        Networking.registerPacket(SetCursorC2SPacket.ID, SetCursorC2SPacket::new);
        Networking.registerPacket(SetEntityC2SPacket.ID, SetEntityC2SPacket::new);
        Networking.registerPacket(SetSlotC2SPacket.ID, SetSlotC2SPacket::new);
        Networking.registerPacket(SummonEntityC2SPacket.ID, SummonEntityC2SPacket::new);
        Networking.registerPacket(ViewBlockS2CPacket.ID, ViewBlockS2CPacket::new);
        Networking.registerPacket(ViewEntityS2CPacket.ID, ViewEntityS2CPacket::new);

        SERVER = new NBTEditorServer();

        ServerLifecycleEvents.SERVER_STARTING.register(DynamicRegistryManagerHolder::setServerManager);
    }
}