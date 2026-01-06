package me.m0dii.modules.waypoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        dispatcher.register(ClientCommandManager.literal("waypoint")
                // /waypoint list
                .then(ClientCommandManager.literal("list")
                        .executes(context -> {
                            WaypointHandler.listWaypoints();
                            return 1;
                        }))

                // /waypoint add
                .then(ClientCommandManager.literal("add")
                        .executes(context -> {
                            WaypointHandler.addWaypoint();
                            return 1;
                        }))

                // /waypoint gui
                .then(ClientCommandManager.literal("gui")
                        .executes(context -> {
                            if (context.getSource().getClient().currentScreen == null) {
                                context.getSource().getClient().execute(() ->
                                        context.getSource().getClient().setScreen(WaypointScreen.create(null))
                                );
                            }
                            return 1;
                        }))

                // /waypoint render [toggle]
                .then(ClientCommandManager.literal("render")
                        .executes(context -> {
                            WaypointRenderer.setEnabled(!WaypointRenderer.isEnabled());
                            String status = WaypointRenderer.isEnabled() ? "enabled" : "disabled";
                            if (context.getSource().getPlayer() != null) {
                                context.getSource().getPlayer().sendMessage(
                                        Text.literal("Waypoint rendering " + status)
                                                .formatted(WaypointRenderer.isEnabled()
                                                        ? Formatting.GREEN
                                                        : Formatting.RED),
                                        false
                                );
                            }
                            return 1;
                        }))

                // /waypoint tp <id>
                .then(ClientCommandManager.literal("tp")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    WaypointHandler.teleportToWaypoint(id);
                                    return 1;
                                })))

                // /waypoint delete <id>
                .then(ClientCommandManager.literal("delete")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    WaypointHandler.deleteWaypoint(id);
                                    return 1;
                                })))

                // /waypoint rename <id> <name>
                .then(ClientCommandManager.literal("rename")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "id");
                                            String name = StringArgumentType.getString(context, "name");
                                            WaypointHandler.renameWaypoint(id, name);
                                            return 1;
                                        }))))
        );
    }
}
