package me.m0dii.modules.packets;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Packet inspector: while on, records every packet the client sends and receives (play phase) into a ring
 * buffer you can browse with {@code /packets} or the keybind. Bundled packets are listed individually.
 */
public class PacketInspectorModule extends Module {
    public static final PacketInspectorModule INSTANCE = new PacketInspectorModule();
    public static final PacketLog LOG = new PacketLog(5000);
    private static final int DETAIL_CHARS = 600;

    private PacketInspectorModule() {
        super("packet_inspector", "Packet Inspector", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.PACKET_INSPECTOR.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.PACKET_INSPECTOR.defaultKey(),
                PacketInspectorModule::openScreen);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("packets")
                        .executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> openScreen(client));
                            return 1;
                        })
                        .then(ClientCommands.literal("start").executes(ctx -> {
                            setEnabled(true);
                            ctx.getSource().sendFeedback(Component.literal("[Packets] Recording."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("stop").executes(ctx -> {
                            setEnabled(false);
                            ctx.getSource().sendFeedback(Component.literal("[Packets] Stopped."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            LOG.clear();
                            ctx.getSource().sendFeedback(Component.literal("[Packets] Cleared."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("top").executes(ctx -> {
                            List<Map.Entry<String, Long>> top = LOG.topCounts(10);
                            if (top.isEmpty()) {
                                ctx.getSource().sendFeedback(Component.literal("[Packets] Nothing recorded yet. /packets start"));
                            }
                            for (Map.Entry<String, Long> entry : top) {
                                ctx.getSource().sendFeedback(Component.literal("[Packets] " + entry.getValue() + "× " + entry.getKey()));
                            }
                            return 1;
                        }))));
    }

    private static void openScreen(Minecraft client) {
        if (client.gui.screen() == null || !(client.gui.screen() instanceof PacketInspectorScreen)) {
            client.gui.setScreen(new PacketInspectorScreen(client.gui.screen()));
        }
    }

    @Override
    protected void onEnable() {
        LOG.setRecording(true);
    }

    @Override
    protected void onDisable() {
        LOG.setRecording(false);
    }

    /** Called from the network thread by the connection mixin. */
    public static void record(Connection connection, PacketLog.Direction direction, Object packet) {
        try {
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            // Only the client's own play connection; skips the integrated server's side in singleplayer.
            if (listener == null || listener.getConnection() != connection) {
                return;
            }
            long now = System.currentTimeMillis();
            for (Object single : unbundle(packet)) {
                LOG.add(direction, PacketLog.nameOf(single.getClass()), PacketLog.describe(single, DETAIL_CHARS), now);
            }
        } catch (RuntimeException ignored) {
            // Never let the inspector disturb networking.
        }
    }

    /** Expands bundle packets (entity spawns etc. arrive bundled) into their parts. */
    private static List<Object> unbundle(Object packet) {
        if (!packet.getClass().getSimpleName().contains("Bundle")) {
            return List.of(packet);
        }
        try {
            Method subPackets = packet.getClass().getMethod("subPackets");
            Object parts = subPackets.invoke(packet);
            if (parts instanceof Iterable<?> iterable) {
                List<Object> list = new ArrayList<>();
                for (Object part : iterable) {
                    list.add(part);
                }
                return list;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // not a bundle with parts
        }
        return List.of(packet);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Recording: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Open inspector");
        settings.add("Clear log");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> openScreen(Minecraft.getInstance());
            case 2 -> LOG.clear();
            default -> {
                // nothing
            }
        }
    }
}
