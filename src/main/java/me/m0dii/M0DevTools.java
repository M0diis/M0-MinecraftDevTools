package me.m0dii;

import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.modules.getdata.GetDataSyncPayloads;
import me.m0dii.modules.getdata.GetDataSyncServer;
import me.m0dii.utils.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class M0DevTools implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("m0-dev-tools");

    public void onInitialize() {
        MidnightConfig.init("m0-dev-tools", ModConfig.class);
        GetDataSyncPayloads.registerPayloadTypes();
        GetDataSyncServer.registerReceivers();
    }
}