package me.m0dii.modules.packets;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Packet inspector: while on, records every packet the client sends and receives (play phase) into a ring
 * buffer you can browse with {@code /packets} or the keybind. Bundled packets are listed individually.
 */
public class PacketInspectorModule extends Module {
    public static final PacketInspectorModule INSTANCE = new PacketInspectorModule();
    public static final PacketLog LOG = new PacketLog(5000);
    private static final int DETAIL_CHARS = 600;
    /** Size of the packet being decoded on this network thread, read by the next incoming record. */
    private static final ThreadLocal<int[]> DECODED_SIZE = ThreadLocal.withInitial(() -> new int[]{-1});
    /** Outgoing packets logged but not encoded yet: packet → entry id (encoding happens on the network thread). */
    private static final Map<Object, Long> PENDING_OUT = new IdentityHashMap<>();

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
                        .then(ClientCommands.literal("export").executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal("[Packets] " + export()));
                            return 1;
                        }))
                        .then(ClientCommands.literal("unpin").executes(ctx -> {
                            LOG.pin(null);
                            ctx.getSource().sendFeedback(Component.literal("[Packets] Unpinned."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("pin")
                                .then(ClientCommands.argument("packet", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String typed = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
                                            for (String name : LOG.knownNames()) {
                                                if (name.toLowerCase(java.util.Locale.ROOT).contains(typed)) {
                                                    builder.suggest(name);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "packet");
                                            LOG.pin(name);
                                            if (!isEnabled()) {
                                                setEnabled(true);
                                            }
                                            ctx.getSource().sendFeedback(Component.literal("[Packets] Graphing " + name
                                                    + " per second; open /packets to see it."));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("stats").executes(ctx -> {
                            for (String line : statsLines()) {
                                ctx.getSource().sendFeedback(Component.literal("[Packets] " + line));
                            }
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

    /** Writes the log (oldest first, tab separated) to config/m0-dev-tools/packet-logs/. */
    public static String export() {
        if (LOG.size() == 0) {
            return "Nothing to export. Start recording with /packets start.";
        }
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools").resolve("packet-logs");
        Path file = dir.resolve("packets-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".tsv");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, LOG.export(new SimpleDateFormat("HH:mm:ss.SSS")));
            return "Saved " + LOG.size() + " packets to config/m0-dev-tools/packet-logs/" + file.getFileName();
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }
    }

    public static List<String> statsLines() {
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        lines.add(String.format(java.util.Locale.ROOT, "In: %s/s, %.0f packets/s · Out: %s/s, %.0f packets/s (last 10 s)",
                PacketTraffic.bytes(LOG.bytesIn().average(now, 10)), LOG.packetsIn().average(now, 10),
                PacketTraffic.bytes(LOG.bytesOut().average(now, 10)), LOG.packetsOut().average(now, 10)));
        lines.add("Total: in " + PacketTraffic.bytes(LOG.bytesIn().total()) + ", out " + PacketTraffic.bytes(LOG.bytesOut().total())
                + (LOG.bytesIn().total() == 0 ? " (sizes are only known on servers, not in singleplayer)" : ""));
        for (Map.Entry<String, Long> entry : LOG.topBytes(5)) {
            lines.add("  " + PacketTraffic.bytes(entry.getValue()) + "  " + entry.getKey());
        }
        return lines;
    }

    /** Called by the decoder mixin with the size of the packet about to be read on this thread. */
    public static void noteDecodedSize(int bytes) {
        if (LOG.isRecording()) {
            DECODED_SIZE.get()[0] = bytes;
            LOG.countBytes(PacketLog.Direction.IN, bytes, System.currentTimeMillis());
        }
    }

    /** Called by the encoder mixin after an outgoing packet was encoded. */
    public static void noteEncodedSize(Object packet, int bytes) {
        if (!LOG.isRecording() || !PacketLog.nameOf(packet.getClass()).startsWith("Serverbound")) {
            return;
        }
        long now = System.currentTimeMillis();
        LOG.countBytes(PacketLog.Direction.OUT, bytes, now);
        Long id;
        synchronized (PENDING_OUT) {
            id = PENDING_OUT.remove(packet);
        }
        if (id != null) {
            LOG.setSize(id, PacketLog.Direction.OUT, PacketLog.nameOf(packet.getClass()), bytes, now);
        }
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
            List<Object> parts = unbundle(packet);
            int decoded = DECODED_SIZE.get()[0];
            DECODED_SIZE.get()[0] = -1;
            for (Object single : parts) {
                // Bundles are put together after decoding, so the size belongs to a part, not the bundle.
                int bytes = direction == PacketLog.Direction.IN && parts.size() == 1 ? decoded : -1;
                long id = LOG.add(direction, PacketLog.nameOf(single.getClass()), PacketLog.describe(single, DETAIL_CHARS), now, bytes);
                if (direction == PacketLog.Direction.OUT && id >= 0) {
                    synchronized (PENDING_OUT) {
                        if (PENDING_OUT.size() > 512) {
                            PENDING_OUT.clear();
                        }
                        PENDING_OUT.put(single, id);
                    }
                }
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
