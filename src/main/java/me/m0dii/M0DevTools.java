package me.m0dii;

import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.modules.getdata.GetDataSyncPayloads;
import me.m0dii.modules.getdata.GetDataSyncServer;
import me.m0dii.modules.hungertweaks.network.HungerTweaksSyncHandler;
import me.m0dii.modules.itemdata.ItemDataPayloads;
import me.m0dii.modules.itemdata.ItemDataSyncServer;
import me.m0dii.modules.mobai.MobAiDebugCommands;
import me.m0dii.modules.mobai.MobAiDebugPayloads;
import me.m0dii.modules.mobdrops.MobDropTrackerPayloads;
import me.m0dii.modules.mobdrops.MobDropTrackerServer;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import me.m0dii.modules.utilitycommands.ConvenienceServerCommands;
import me.m0dii.modules.worldedit.WorldEditSyncPayloads;
import me.m0dii.modules.worldedit.WorldEditSyncServer;
import me.m0dii.modules.xray.network.XrayOptInNetworking;
import me.m0dii.utils.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class M0DevTools implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(M0DevToolsClient.MOD_ID);

    public void onInitialize() {
        MidnightConfig.init(M0DevToolsClient.MOD_ID, ModConfig.class);
        GetDataSyncPayloads.registerPayloadTypes();
        ItemDataPayloads.registerPayloadTypes();
        MobAiDebugPayloads.registerPayloadTypes();
        MobDropTrackerPayloads.registerPayloadTypes();
        HungerTweaksSyncHandler.registerPayloadTypes();
        XrayOptInNetworking.registerPayloadTypes();
        RestrictedModuleOptInNetworking.registerPayloadTypes();
        WorldEditSyncPayloads.registerPayloadTypes();
        GetDataSyncServer.registerReceivers();
        ItemDataSyncServer.registerReceivers();
        XrayOptInNetworking.registerReceivers();
        RestrictedModuleOptInNetworking.registerReceivers();
        WorldEditSyncServer.registerReceivers();
        MobAiDebugCommands.register();
        MobDropTrackerServer.register();
        ConvenienceServerCommands.register();
    }
}
