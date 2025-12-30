package me.m0dii.modules.waypoints;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class WaypointCommands {

    private WaypointCommands() {
        // Utility class - prevent instantiation
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerWaypointCommands(dispatcher);
        });
    }

    private static void registerWaypointCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("waypoint_teleport")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            WaypointHandler.teleportToWaypoint(id);
                            return 1;
                        })));

        dispatcher.register(ClientCommandManager.literal("waypoint_delete")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            WaypointHandler.deleteWaypoint(id);
                            return 1;
                        })));

        dispatcher.register(ClientCommandManager.literal("waypoint_rename")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String id = StringArgumentType.getString(context, "id");
                                    String name = StringArgumentType.getString(context, "name");
                                    WaypointHandler.renameWaypoint(id, name);
                                    return 1;
                                }))));
    }
}
