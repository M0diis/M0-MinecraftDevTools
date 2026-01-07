package me.m0dii;

import eu.midnightdust.lib.config.MidnightConfig;
import lombok.Getter;
import me.m0dii.modules.blockstatecycler.BlockStateCycler;
import me.m0dii.modules.chat.SecondaryChatModule;
import me.m0dii.modules.clickgui.ClickGuiModule;
import me.m0dii.modules.clickgui.ModuleRegistry;
import me.m0dii.modules.commandhistory.CommandHistoryModule;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.fullbright.FullbrightModule;
import me.m0dii.modules.instantbreak.InstantBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.macros.MacrosModule;
import me.m0dii.modules.messagehistory.MessageHistoryModule;
import me.m0dii.modules.nbtget.NBTGetCommand;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.overlays.*;
import me.m0dii.modules.quicktp.QuickTeleportModule;
import me.m0dii.modules.scripting.ClientCommandRunScript;
import me.m0dii.modules.scripting.InGameScriptingKeybinds;
import me.m0dii.modules.waypoints.WaypointModule;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.nbteditor.commands.NBTEditorCommands;
import me.m0dii.nbteditor.containers.ContainerIO;
import me.m0dii.nbteditor.misc.NbtTypeModifier;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.packets.*;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.screens.containers.CursorManager;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.nbteditor.server.ServerMiscUtil;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.utils.KeybindManager;
import me.m0dii.utils.ModConfig;
import me.m0dii.utils.TickHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.InputUtil;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class M0DevToolsClient implements ClientModInitializer {
    public static final String MOD_ID = "m0-dev-tools";
    public static final File SETTINGS_FOLDER = new File("m0-dev-tools");

    public static NBTEditorServerConn SERVER_CONN;
    public static CursorManager CURSOR_MANAGER;

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, ModConfig.class);

        // Modules
        // XrayModule.INSTANCE.register(); // Not ready yet
        LightLevelOverlayModule.INSTANCE.register();
        RedstonePowerOverlayModule.INSTANCE.register();
        RedstoneBlockUpdateViewModule.INSTANCE.register();
        SlimeChunkOverlayModule.INSTANCE.register();
        EntityRadarModule.INSTANCE.register();
        CommandHistoryModule.INSTANCE.register();
        MessageHistoryModule.INSTANCE.register();
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
        InstantBreakModule.INSTANCE.register();
        NBTTooltipModule.INSTANCE.register();
        FullbrightModule.INSTANCE.register();
        InventoryMoveModule.INSTANCE.register();
        ClickGuiModule.INSTANCE.register();
        CommandBlockOverlayModule.INSTANCE.register();

        // Populate ModuleRegistry categories after modules have had a chance to create their singletons
        ModuleRegistry.initializeCategories();

        KeybindManager.registerPressedKeybind("key.m0-dev-tools.open_modules_screen",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O,
                client -> ClickGuiModule.INSTANCE.getRenderer().toggle());

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
        SERVER_CONN = new NBTEditorServerConn();
    }

    public static class NBTEditorServerConn implements ClientNetworking.PlayNetworkStateEvents.Start, ClientNetworking.PlayNetworkStateEvents.Stop {

        public enum Status {
            DISCONNECTED,
            CLIENT_ONLY,
            INCOMPATIBLE,
            BOTH
        }

        @Getter
        private Status status;
        @Getter
        private boolean containerScreen;
        private int lastRequestId;
        private final Map<Integer, CompletableFuture<ModPacket>> requests;

        public NBTEditorServerConn() {
            status = Status.DISCONNECTED;
            containerScreen = false;
            lastRequestId = -1;
            requests = new HashMap<>();

            ClientNetworking.registerListener(ProtocolVersionS2CPacket.ID, this::onProtocolVersionPacket);
            ClientNetworking.registerListener(ContainerScreenS2CPacket.ID, this::onContainerScreenPacket);
            ClientNetworking.registerListener(ViewBlockS2CPacket.ID, this::receiveRequest);
            ClientNetworking.registerListener(ViewEntityS2CPacket.ID, this::receiveRequest);

            ClientNetworking.PlayNetworkStateEvents.Start.EVENT.register(this);
            ClientNetworking.PlayNetworkStateEvents.Stop.EVENT.register(this);
        }

        public boolean isEditingExpanded() {
            if (status != Status.BOTH) {
                return false;
            }

            GameMode gameMode = MiscUtil.client.interactionManager.getCurrentGameMode();
            return (gameMode.isCreative() || gameMode.isSurvivalLike()) && ServerMiscUtil.hasPermissionLevel(MiscUtil.client.player, 2);
        }

        public boolean isEditingAllowed() {
            return MiscUtil.client.interactionManager.getCurrentGameMode().isCreative() || isEditingExpanded();
        }

        public boolean isScreenEditable() {
            Screen screen = MiscUtil.client.currentScreen;
            return screen instanceof CreativeInventoryScreen ||
                    isEditingExpanded() && (screen instanceof InventoryScreen || containerScreen);
        }

        public void closeContainerScreen() {
            containerScreen = false;
        }

        public <T extends ModPacket> CompletableFuture<Optional<T>> sendRequest(Function<Integer, ModPacket> packet, Class<T> responseType) {
            if (!isEditingExpanded()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            CompletableFuture<ModPacket> future = new CompletableFuture<>();
            int requestId = ++lastRequestId;
            requests.put(requestId, future);
            ClientNetworking.send(packet.apply(requestId));

            return future.thenApply(response -> {
                if (responseType.isInstance(response)) {
                    return Optional.of(responseType.cast(response));
                }

                return Optional.<T>empty();
            }).completeOnTimeout(Optional.empty(), 1000, TimeUnit.MILLISECONDS).thenApply(output -> {
                requests.remove(requestId);
                return output;
            });
        }

        private void receiveRequest(ResponsePacket packet) {
            CompletableFuture<ModPacket> receiver = requests.remove(packet.requestId());
            if (receiver != null) {
                receiver.complete(packet);
            }
        }

        @Override
        public void onPlayStart(ClientPlayNetworkHandler networkHandler) {
            status = Status.CLIENT_ONLY;
        }

        @Override
        public void onPlayStop() {
            status = Status.DISCONNECTED;
        }

        private void onProtocolVersionPacket(ProtocolVersionS2CPacket packet) {
            if (packet.version() == NBTEditorServer.PROTOCOL_VERSION) {
                status = Status.BOTH;
            } else {
                status = Status.INCOMPATIBLE;
            }
        }

        private void onContainerScreenPacket(ContainerScreenS2CPacket packet) {
            containerScreen = true;
        }

    }
}
