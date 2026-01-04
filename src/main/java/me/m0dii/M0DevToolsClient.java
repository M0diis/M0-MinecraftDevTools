package me.m0dii;

import eu.midnightdust.lib.config.MidnightConfig;
import me.m0dii.modules.ModulesScreen;
import me.m0dii.modules.blockstatecycler.BlockStateCycler;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.commandhistory.CommandHistoryModule;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.instabreak.InstaBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.macros.MacrosModule;
import me.m0dii.modules.nbtget.NBTGetCommand;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.quicktp.QuickTeleportModule;
import me.m0dii.modules.scripting.ClientCommandRunScript;
import me.m0dii.modules.scripting.InGameScriptingKeybinds;
import me.m0dii.modules.spectatortoggle.SpectatorToggleModule;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.nbteditor.commands.NBTEditorCommands;
import me.m0dii.nbteditor.containers.ContainerIO;
import me.m0dii.nbteditor.misc.NbtTypeModifier;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.screens.containers.CursorManager;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.utils.KeybindManager;
import me.m0dii.utils.ModConfig;
import me.m0dii.utils.TickHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.File;

public class M0DevToolsClient implements ClientModInitializer {
    public static final String MOD_ID = "m0-dev-tools";
    public static final File SETTINGS_FOLDER = new File("m0-dev-tools");

    public static NBTInfoHudOverlayModule.NBTEditorServerConn SERVER_CONN;
    public static CursorManager CURSOR_MANAGER;

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, ModConfig.class);

        // Modules
        // XrayModule.INSTANCE.register(); // Not ready yet
        LightLevelOverlayModule.INSTANCE.register();
        RedstonePowerOverlayModule.INSTANCE.register();
        SlimeChunkOverlayModule.INSTANCE.register();
        EntityRadarModule.INSTANCE.register();
        CommandHistoryModule.INSTANCE.register();
        SecondaryChatModule.INSTANCE.register();
        NBTInfoHudOverlayModule.INSTANCE.register();
        ChunkBorderOverlayModule.INSTANCE.register();
        StructureBoundingBoxOverlay.INSTANCE.register();
        WaypointModule.INSTANCE.register();
        QuickTeleportModule.INSTANCE.register();
        BlockStateCycler.INSTANCE.register();
        MacrosModule.INSTANCE.register();
        FreecamModule.INSTANCE.register();
        ZoomModule.INSTANCE.register();
        SpectatorToggleModule.INSTANCE.register();
        InstaBreakModule.INSTANCE.register();
        NBTTooltipModule.INSTANCE.register();
        FullbrightModule.INSTANCE.register();
        InventoryMoveModule.INSTANCE.register();

        KeybindManager.registerPressedKeybind("key.m0-dev-tools.open_modules_screen",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, client -> {
                    client.setScreen(new ModulesScreen(client.currentScreen));
                });

        NBTGetCommand.register();

        ClientCommandRunScript.register();
        InGameScriptingKeybinds.register();

        NBTEditorCommands.register();
        NBTEditorServer.IS_DEDICATED = false;

        if (!SETTINGS_FOLDER.exists()) {
            SETTINGS_FOLDER.mkdir();
        }

        TickHandler.register();

        DynamicRegistryManagerHolder.onDefaultManagerLoad(this::onRegistriesLoad);
    }

    private void onRegistriesLoad() {
        NbtTypeModifier.loadClass();

        ContainerIO.loadClass();
        ConfigScreen.loadSettings();

        CURSOR_MANAGER = new CursorManager();
        SERVER_CONN = new NBTInfoHudOverlayModule.NBTEditorServerConn();
    }
}
