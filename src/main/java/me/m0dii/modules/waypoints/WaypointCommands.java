package me.m0dii.modules.waypoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class WaypointCommands {

    private WaypointCommands() {
        // Utility class
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                registerWaypointCommands(dispatcher)
        );
    }

    private static void registerWaypointCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("waypoint")
                // /waypoint list
                .then(ClientCommands.literal("list")
                        .executes(context -> {
                            WaypointHandler.listWaypoints();
                            return 1;
                        }))

                // /waypoint add
                .then(ClientCommands.literal("add")
                        .executes(context -> {
                            WaypointHandler.addWaypoint();
                            return 1;
                        }))

                // /waypoint track <id|nearest|off>
                .then(ClientCommands.literal("track")
                        .executes(context -> {
                            WaypointHandler.track("nearest");
                            return 1;
                        })
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    WaypointHandler.track(StringArgumentType.getString(context, "id"));
                                    return 1;
                                })))

                // /waypoint beams
                .then(ClientCommands.literal("beams")
                        .executes(context -> {
                            WaypointRenderer.setBeams(!WaypointRenderer.isBeams());
                            me.m0dii.modules.state.ModuleStateStore.onSettingsChanged(WaypointModule.INSTANCE);
                            context.getSource().sendFeedback(Component.literal("Waypoint beams "
                                    + (WaypointRenderer.isBeams() ? "on" : "off")));
                            return 1;
                        }))

                // /waypoint claim  (assign waypoints saved by older versions to this world)
                .then(ClientCommands.literal("claim")
                        .executes(context -> {
                            WaypointHandler.claimUnassignedWaypoints();
                            return 1;
                        }))

                // /waypoint gui
                .then(ClientCommands.literal("gui")
                        .executes(context -> {
                            if (context.getSource().getClient().gui.screen() == null) {
                                context.getSource().getClient().execute(() ->
                                        context.getSource().getClient().gui.setScreen(WaypointScreen.create(null))
                                );
                            }
                            return 1;
                        }))

                // /waypoint render [toggle]
                .then(ClientCommands.literal("render")
                        .executes(context -> {
                            WaypointRenderer.setEnabled(!WaypointRenderer.isEnabled());
                            String status = WaypointRenderer.isEnabled() ? "enabled" : "disabled";
                            if (context.getSource().getPlayer() != null) {
                                context.getSource().getPlayer().sendSystemMessage(
                                        Component.literal("Waypoint rendering " + status)
                                                .withStyle(WaypointRenderer.isEnabled()
                                                        ? ChatFormatting.GREEN
                                                        : ChatFormatting.RED));
                            }
                            return 1;
                        }))

                // /waypoint tp <id>
                .then(ClientCommands.literal("tp")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    WaypointHandler.teleportToWaypoint(id);
                                    return 1;
                                })))

                // /waypoint delete <id>
                .then(ClientCommands.literal("delete")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    WaypointHandler.deleteWaypoint(id);
                                    return 1;
                                })))

                // /waypoint rename <id> <name>
                .then(ClientCommands.literal("rename")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "id");
                                            String name = StringArgumentType.getString(context, "name");
                                            WaypointHandler.renameWaypoint(id, name);
                                            return 1;
                                        }))))
        );
    }
}
