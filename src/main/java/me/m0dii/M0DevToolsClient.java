package me.m0dii;

import com.mojang.blaze3d.platform.InputConstants;
import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.modules.automation.AutomationModule;
import me.m0dii.modules.bridging.BridgingTweaksModule;
import me.m0dii.modules.camera.CameraClientCommand;
import me.m0dii.modules.camera.CameraPathManager;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.clickgui.ClickHudModule;
import me.m0dii.modules.clickgui.ModuleRegistry;
import me.m0dii.modules.commandblockui.BetterCommandBlockUiModule;
import me.m0dii.modules.commandhistory.CommandHistoryModule;
import me.m0dii.modules.debugdraw.DebugDrawManager;
import me.m0dii.modules.debugdraw.DrawClientCommand;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.fastblockplacement.FastBlockPlacementModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.getdata.GetDataClientCommand;
import me.m0dii.modules.getdata.GetDataSyncClient;
import me.m0dii.modules.healthbars.EntityHealthBarsModule;
import me.m0dii.modules.heldlight.HeldLightModule;
import me.m0dii.modules.hudtweaks.HudTweaksModule;
import me.m0dii.modules.hungertweaks.HungerTweaksModule;
import me.m0dii.modules.instantbreak.InstantBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.itemdata.ItemDataClientCommand;
import me.m0dii.modules.itemdata.ItemDataSyncClient;
import me.m0dii.modules.macros.MacrosModule;
import me.m0dii.modules.messagehistory.MessageHistoryModule;
import me.m0dii.modules.mobai.MobAiDebugClient;
import me.m0dii.modules.mobai.MobAiVisualizerModule;
import me.m0dii.modules.mobdrops.MobDropTrackerClient;
import me.m0dii.modules.mobdrops.MobDropTrackerRenderer;
import me.m0dii.modules.mousetweaks.MouseTweaksModule;
import me.m0dii.modules.nbthud.NBTInfoHudModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.nbttooltip.ShulkerTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.performance.DynamicFpsModule;
import me.m0dii.modules.pickup.ItemPickupNotifierModule;
import me.m0dii.modules.quicktp.QuickTeleportModule;
import me.m0dii.modules.reach.ReachModule;
import me.m0dii.modules.scripting.ClientCommandRunScript;
import me.m0dii.modules.scripting.InGameScriptingKeybinds;
import me.m0dii.modules.tweaks.TweaksModule;
import me.m0dii.modules.utilitycommands.BlockScannerClientCommand;
import me.m0dii.modules.utilitycommands.ConvenienceClientCommands;
import me.m0dii.modules.utilitycommands.MathClientCommand;
import me.m0dii.modules.watson.WatsonCoreProtectModule;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.modules.worldedit.WorldEditModule;
import me.m0dii.modules.xray.XrayModule;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.utils.KeybindCatalog;
import me.m0dii.utils.KeybindManager;
import me.m0dii.utils.ModConfig;
import me.m0dii.utils.TickHandler;
import net.fabricmc.api.ClientModInitializer;
import java.io.File;

public class M0DevToolsClient implements ClientModInitializer {
    public static final String MOD_ID = "m0-dev-tools";
    public static final File SETTINGS_FOLDER = new File(MOD_ID);

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, ModConfig.class);

        // Modules
        XrayModule.INSTANCE.register();
        LightLevelOverlayModule.INSTANCE.register();
        RedstonePowerOverlayModule.INSTANCE.register();
        RedstoneBlockUpdateViewModule.INSTANCE.register();
        SlimeChunkOverlayModule.INSTANCE.register();
        BiomeBorderOverlayModule.INSTANCE.register();
        EntityRadarModule.INSTANCE.register();
        CommandHistoryModule.INSTANCE.register();
        MessageHistoryModule.INSTANCE.register();
        SecondaryChatModule.INSTANCE.register();
        MobAiVisualizerModule.INSTANCE.register();
        NBTInfoHudModule.INSTANCE.register();
        ChunkBorderOverlayModule.INSTANCE.register();
        StructureBoundingBoxOverlay.INSTANCE.register();
        WaypointModule.INSTANCE.register();
        QuickTeleportModule.INSTANCE.register();
        MacrosModule.INSTANCE.register();
        FreecamModule.INSTANCE.register();
        CameraPathManager.register();
        ZoomModule.INSTANCE.register();
        InstantBreakModule.INSTANCE.register();
        NBTTooltipModule.INSTANCE.register();
        ShulkerTooltipModule.INSTANCE.register();
        ItemPickupNotifierModule.INSTANCE.register();
        FullbrightModule.INSTANCE.register();
        HeldLightModule.INSTANCE.register();
        HudTweaksModule.INSTANCE.register();
        HungerTweaksModule.INSTANCE.register();
        BridgingTweaksModule.INSTANCE.register();
        InventoryMoveModule.INSTANCE.register();
        DynamicFpsModule.INSTANCE.register();
        FastBlockPlacementModule.INSTANCE.register();
        MouseTweaksModule.INSTANCE.register();
        ReachModule.INSTANCE.register();
        BetterCommandBlockUiModule.INSTANCE.register();
        TweaksModule.INSTANCE.register();
        WorldEditModule.INSTANCE.register();
        ClickHudModule.INSTANCE.register();
        EntityHealthBarsModule.INSTANCE.register();
        CommandBlockOverlayModule.INSTANCE.register();
        BlockAttributeOverlayRenderer.register();
        WatsonCoreProtectModule.INSTANCE.register();
        me.m0dii.modules.actionrunner.ActionRunnerClientInit.register();
        me.m0dii.modules.actionrunner.ActionRunnerClientTick.register();
        AutomationModule.INSTANCE.register();
        me.m0dii.modules.ruler.RulerModule.INSTANCE.register();
        me.m0dii.modules.portals.PortalLinkerModule.INSTANCE.register();
        me.m0dii.modules.packets.PacketInspectorModule.INSTANCE.register();
        me.m0dii.modules.chathighlight.ChatHighlightModule.INSTANCE.register();
        me.m0dii.modules.redstonetimeline.RedstoneTimelineModule.INSTANCE.register();
        me.m0dii.modules.placement.PlacementRestrictionModule.INSTANCE.register();
        me.m0dii.modules.ranges.RangeOverlaysModule.INSTANCE.register();
        me.m0dii.modules.debugviews.DebugViewsModule.INSTANCE.register();
        me.m0dii.modules.farmrate.FarmRateModule.INSTANCE.register();
        me.m0dii.modules.freelook.FreelookModule.INSTANCE.register();
        me.m0dii.modules.tickcontrol.TickControlModule.INSTANCE.register();
        me.m0dii.modules.chestfinder.ChestFinderModule.INSTANCE.register();

        // Populate ModuleRegistry categories after modules have had a chance to create their singletons
        ModuleRegistry.initializeCategories();
        // Saved settings, favourites and profiles; on/off states are restored on the first world join.
        me.m0dii.modules.state.ModuleStateStore.register();
        me.m0dii.modules.state.ProfileCommands.register();

        // Default N: vanilla 26.x took O for the Friends List.
        me.m0dii.modules.clickgui.ModuleBrowserScreen.openKey = KeybindManager.registerPressedKeybind(KeybindCatalog.OPEN_MODULES_SCREEN.translationKey(),
                InputConstants.Type.KEYBOARD, KeybindCatalog.OPEN_MODULES_SCREEN.defaultKey(),
                client -> {
                    var clickHud = me.m0dii.modules.clickgui.ClickHudModule.INSTANCE.getRenderer();
                    var focus = clickHud == null ? null : clickHud.focusedModule();
                    client.gui.setScreen(new me.m0dii.modules.clickgui.ModuleBrowserScreen(client.gui.screen(), focus));
                });
        KeybindManager.registerPressedKeybind(KeybindCatalog.CYCLE_PROFILE.translationKey(),
                InputConstants.Type.KEYBOARD, KeybindCatalog.CYCLE_PROFILE.defaultKey(),
                client -> {
                    String message = me.m0dii.modules.state.ModuleStateStore.cycleProfile();
                    if (client.player != null) {
                        client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(message));
                    }
                });

        ClientCommandRunScript.register();
        GetDataClientCommand.register();
        GetDataSyncClient.registerReceivers();
        ItemDataClientCommand.register();
        ItemDataSyncClient.registerReceivers();
        MobAiDebugClient.registerReceivers();
        MobDropTrackerClient.registerReceivers();
        DrawClientCommand.register();
        CameraClientCommand.register();
        MathClientCommand.register();
        BlockScannerClientCommand.register();
        me.m0dii.modules.highlights.BlockHighlights.register();
        me.m0dii.modules.glow.GlowCommand.register();
        ConvenienceClientCommands.register();
        InGameScriptingKeybinds.register();
        DebugDrawManager.registerRenderer();
        MobDropTrackerRenderer.register();

        if (!SETTINGS_FOLDER.exists()) {
            SETTINGS_FOLDER.mkdir();
        }

        TickHandler.register();
    }

}
