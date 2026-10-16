package me.m0dii.modules.watson;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

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
        dispatcher.register(ClientCommands.literal("watson")
                .executes(ctx -> {
                    send(ctx.getSource(), "Watson CP is " + (WatsonCoreProtectModule.INSTANCE.isEnabled() ? "enabled" : "disabled"), ChatFormatting.AQUA);
                    send(ctx.getSource(), "Entries: " + CoreProtectTracker.size(), ChatFormatting.GRAY);
                    return 1;
                })
                .then(ClientCommands.literal("toggle")
                        .executes(ctx -> {
                            WatsonCoreProtectModule.INSTANCE.toggleEnabled();
                            return 1;
                        }))
                .then(ClientCommands.literal("clear")
                        .executes(ctx -> {
                            CoreProtectTracker.clear();
                            send(ctx.getSource(), "Cleared Watson CP entries.", ChatFormatting.GREEN);
                            return 1;
                        }))
                .then(ClientCommands.literal("dump")
                        .executes(ctx -> {
                            List<CoreProtectEntry> entries = CoreProtectTracker.snapshot();
                            Map<BlockPos, Integer> ids = CoreProtectTracker.getPositionIndexMap();
                            send(ctx.getSource(), "Watson entries: " + entries.size(), ChatFormatting.GRAY);

                            LocalPlayer player = ctx.getSource().getPlayer();
                            Vec3 playerPos = player != null ? player.position() : null;
                            int shown = 0;
                            for (CoreProtectEntry entry : entries) {
                                if (shown >= 5) {
                                    break;
                                }

                                double dist = playerPos == null ? -1.0 : playerPos.distanceTo(Vec3.atCenterOf(entry.pos()));
                                String distText = dist < 0.0 ? "n/a" : String.format("%.1f", dist);
                                int id = ids.getOrDefault(entry.pos(), -1);
                                send(
                                        ctx.getSource(),
                                        "#" + (shown + 1) + " [id=" + id + "] " + entry.action() + " " + entry.actor() + " @ " + entry.pos().toShortString() + " (dist=" + distText + ")",
                                        ChatFormatting.DARK_AQUA
                                );
                                shown++;
                            }
                            return 1;
                        }))
                .then(ClientCommands.literal("tp")
                        .then(ClientCommands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int id = IntegerArgumentType.getInteger(ctx, "id");
                                    var posOpt = CoreProtectTracker.getPositionById(id);
                                    if (posOpt.isEmpty()) {
                                        send(ctx.getSource(), "Unknown id " + id + ". Use /watson dump to inspect known ids.", ChatFormatting.RED);
                                        return 0;
                                    }

                                    BlockPos pos = posOpt.get();
                                    if (ctx.getSource().getPlayer() == null || ctx.getSource().getPlayer().connection == null) {
                                        send(ctx.getSource(), "Unable to run tp command: player network handler missing.", ChatFormatting.RED);
                                        return 0;
                                    }

                                    String command = String.format("minecraft:tp %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                                    ctx.getSource().getPlayer().connection.sendCommand(command);
                                    send(ctx.getSource(), "Teleport command sent for id " + id + " -> " + pos.toShortString(), ChatFormatting.GREEN);
                                    return 1;
                                })))
                .then(ClientCommands.literal("tracers")
                        .executes(ctx -> {
                            CoreProtectRenderer.setTracersEnabled(!CoreProtectRenderer.isTracersEnabled());
                            send(ctx.getSource(), "Tracers " + (CoreProtectRenderer.isTracersEnabled() ? "enabled" : "disabled"), ChatFormatting.YELLOW);
                            return 1;
                        }))
                .then(ClientCommands.literal("vectors")
                        .executes(ctx -> {
                            CoreProtectRenderer.setVectorsEnabled(!CoreProtectRenderer.isVectorsEnabled());
                            send(ctx.getSource(), "Vectors " + (CoreProtectRenderer.isVectorsEnabled() ? "enabled" : "disabled"), ChatFormatting.LIGHT_PURPLE);
                            return 1;
                        }))
                .then(ClientCommands.literal("ttl")
                        .then(ClientCommands.argument("seconds", IntegerArgumentType.integer(5, 3600))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    CoreProtectTracker.setTtlSeconds(seconds);
                                    send(ctx.getSource(), "Watson CP TTL set to " + seconds + "s", ChatFormatting.GREEN);
                                    return 1;
                                })))
                .then(ClientCommands.literal("test")
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer())
                                .then(ClientCommands.argument("y", IntegerArgumentType.integer())
                                        .then(ClientCommands.argument("z", IntegerArgumentType.integer())
                                                .then(ClientCommands.argument("world", StringArgumentType.word())
                                                        .executes(ctx -> {
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            String world = StringArgumentType.getString(ctx, "world");

                                                            if (!WatsonCoreProtectModule.INSTANCE.isEnabled()) {
                                                                WatsonCoreProtectModule.INSTANCE.setEnabled(true);
                                                                send(ctx.getSource(), "Watson CP auto-enabled for test rendering.", ChatFormatting.YELLOW);
                                                            }

                                                            int before = CoreProtectTracker.size();
                                                            int inserted = CoreProtectTracker.injectSyntheticLookup(x, y, z, world);
                                                            int after = CoreProtectTracker.size();


                                                            send(ctx.getSource(), "Injected synthetic CoreProtect lookup for (" + x + ", " + y + ", " + z + ") in '" + world + "'.", ChatFormatting.AQUA);
                                                            send(ctx.getSource(), "Generated entries: " + inserted + " | Tracker count: " + before + " -> " + after, ChatFormatting.GRAY);
                                                            return 1;
                                                        })))))));
    }

    private static void send(FabricClientCommandSource source, String text, ChatFormatting color) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendSystemMessage(Component.literal(text).withStyle(color));
        }
    }
}

