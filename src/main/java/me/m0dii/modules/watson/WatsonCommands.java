package me.m0dii.modules.watson;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;

public final class WatsonCommands {
    private WatsonCommands() {
        // Utility class
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerWatson(dispatcher));
    }

    private static void registerWatson(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("watson")
                .executes(ctx -> {
                    send(ctx.getSource(), "Watson CP is " + (WatsonCoreProtectModule.INSTANCE.isEnabled() ? "enabled" : "disabled"), Formatting.AQUA);
                    send(ctx.getSource(), "Entries: " + CoreProtectTracker.size(), Formatting.GRAY);
                    return 1;
                })
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> {
                            WatsonCoreProtectModule.INSTANCE.toggleEnabled();
                            return 1;
                        }))
                .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            CoreProtectTracker.clear();
                            send(ctx.getSource(), "Cleared Watson CP entries.", Formatting.GREEN);
                            return 1;
                        }))
                .then(ClientCommandManager.literal("dump")
                        .executes(ctx -> {
                            List<CoreProtectEntry> entries = CoreProtectTracker.snapshot();
                            Map<BlockPos, Integer> ids = CoreProtectTracker.getPositionIndexMap();
                            send(ctx.getSource(), "Watson entries: " + entries.size(), Formatting.GRAY);

                            ClientPlayerEntity player = ctx.getSource().getPlayer();
                            Vec3d playerPos = player != null ? player.getEntityPos() : null;
                            int shown = 0;
                            for (CoreProtectEntry entry : entries) {
                                if (shown >= 5) {
                                    break;
                                }

                                double dist = playerPos == null ? -1.0 : playerPos.distanceTo(Vec3d.ofCenter(entry.pos()));
                                String distText = dist < 0.0 ? "n/a" : String.format("%.1f", dist);
                                int id = ids.getOrDefault(entry.pos(), -1);
                                send(
                                        ctx.getSource(),
                                        "#" + (shown + 1) + " [id=" + id + "] " + entry.action() + " " + entry.actor() + " @ " + entry.pos().toShortString() + " (dist=" + distText + ")",
                                        Formatting.DARK_AQUA
                                );
                                shown++;
                            }
                            return 1;
                        }))
                .then(ClientCommandManager.literal("tp")
                        .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int id = IntegerArgumentType.getInteger(ctx, "id");
                                    var posOpt = CoreProtectTracker.getPositionById(id);
                                    if (posOpt.isEmpty()) {
                                        send(ctx.getSource(), "Unknown id " + id + ". Use /watson dump to inspect known ids.", Formatting.RED);
                                        return 0;
                                    }

                                    BlockPos pos = posOpt.get();
                                    if (ctx.getSource().getPlayer() == null || ctx.getSource().getPlayer().networkHandler == null) {
                                        send(ctx.getSource(), "Unable to run tp command: player network handler missing.", Formatting.RED);
                                        return 0;
                                    }

                                    String command = String.format("minecraft:tp %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                                    ctx.getSource().getPlayer().networkHandler.sendChatCommand(command);
                                    send(ctx.getSource(), "Teleport command sent for id " + id + " -> " + pos.toShortString(), Formatting.GREEN);
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("tracers")
                        .executes(ctx -> {
                            CoreProtectRenderer.setTracersEnabled(!CoreProtectRenderer.isTracersEnabled());
                            send(ctx.getSource(), "Tracers " + (CoreProtectRenderer.isTracersEnabled() ? "enabled" : "disabled"), Formatting.YELLOW);
                            return 1;
                        }))
                .then(ClientCommandManager.literal("vectors")
                        .executes(ctx -> {
                            CoreProtectRenderer.setVectorsEnabled(!CoreProtectRenderer.isVectorsEnabled());
                            send(ctx.getSource(), "Vectors " + (CoreProtectRenderer.isVectorsEnabled() ? "enabled" : "disabled"), Formatting.LIGHT_PURPLE);
                            return 1;
                        }))
                .then(ClientCommandManager.literal("ttl")
                        .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 3600))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    CoreProtectTracker.setTtlSeconds(seconds);
                                    send(ctx.getSource(), "Watson CP TTL set to " + seconds + "s", Formatting.GREEN);
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("test")
                        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                                .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("world", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            String world = StringArgumentType.getString(ctx, "world");

                                                            if (!WatsonCoreProtectModule.INSTANCE.isEnabled()) {
                                                                WatsonCoreProtectModule.INSTANCE.setEnabled(true);
                                                                send(ctx.getSource(), "Watson CP auto-enabled for test rendering.", Formatting.YELLOW);
                                                            }

                                                            int before = CoreProtectTracker.size();
                                                            int inserted = CoreProtectTracker.injectSyntheticLookup(x, y, z, world);
                                                            int after = CoreProtectTracker.size();


                                                            send(ctx.getSource(), "Injected synthetic CoreProtect lookup for (" + x + ", " + y + ", " + z + ") in '" + world + "'.", Formatting.AQUA);
                                                            send(ctx.getSource(), "Generated entries: " + inserted + " | Tracker count: " + before + " -> " + after, Formatting.GRAY);
                                                            return 1;
                                                        })))))));
    }

    private static void send(FabricClientCommandSource source, String text, Formatting color) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal(text).formatted(color), false);
        }
    }
}

