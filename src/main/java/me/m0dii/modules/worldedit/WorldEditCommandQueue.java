package me.m0dii.modules.worldedit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class WorldEditCommandQueue {
    private static final Deque<String> PENDING = new ArrayDeque<>();
    private static boolean registered;
    private static String activeLabel;
    private static int totalCommands;
    private static int sentCommands;
    private static Runnable pendingCompletion;

    private WorldEditCommandQueue() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(WorldEditCommandQueue::onEndTick);
    }

    public static boolean isBusy() {
        return activeLabel != null;
    }

    public static boolean submit(String label, List<String> commands) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || commands == null || commands.isEmpty() || isBusy()) {
            return false;
        }

        PENDING.clear();
        PENDING.addAll(commands);
        activeLabel = label;
        totalCommands = commands.size();
        sentCommands = 0;
        client.player.sendSystemMessage(Component.literal("[WE] Queued " + totalCommands + " command(s) for " + label + "."));
        return true;
    }

    public static void setPendingCompletion(Runnable completion) {
        pendingCompletion = completion;
    }

    private static void onEndTick(Minecraft client) {
        if (activeLabel == null || client.player == null || client.getConnection() == null) {
            return;
        }

        String command = PENDING.pollFirst();
        if (command == null) {
            client.player.sendSystemMessage(Component.literal("[WE] Finished " + activeLabel + " (" + sentCommands + " command(s))."));
            if (pendingCompletion != null) {
                pendingCompletion.run();
            }
            clearState();
            return;
        }

        client.getConnection().sendCommand(command);
        sentCommands++;

        if (PENDING.isEmpty()) {
            client.player.sendSystemMessage(Component.literal("[WE] Finished " + activeLabel + " (" + sentCommands + " command(s))."));
            if (pendingCompletion != null) {
                pendingCompletion.run();
            }
            clearState();
        }
    }

    private static void clearState() {
        PENDING.clear();
        activeLabel = null;
        totalCommands = 0;
        sentCommands = 0;
        pendingCompletion = null;
    }
}
