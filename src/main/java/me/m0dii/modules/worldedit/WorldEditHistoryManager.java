package me.m0dii.modules.worldedit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class WorldEditHistoryManager {
    private static final int MAX_HISTORY = 15;
    private static final Deque<HistoryEntry> UNDO = new ArrayDeque<>();
    private static final Deque<HistoryEntry> REDO = new ArrayDeque<>();

    private WorldEditHistoryManager() {
    }

    public static boolean canUndo() {
        return !UNDO.isEmpty();
    }

    public static boolean canRedo() {
        return !REDO.isEmpty();
    }

    public static List<String> prepareTrackedOperation(String label, List<WorldEditSelection> touchedSelections, List<String> redoCommands) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return null;
        }
        if (touchedSelections == null || touchedSelections.isEmpty()) {
            return null;
        }
        if (!WorldEditUndoSnapshot.isFullyLoaded(client.world, touchedSelections)) {
            client.player.sendMessage(Text.literal("[WE] Selection includes unloaded chunks. Load the area first for tracked edits and undo/redo."), false);
            return null;
        }

        WorldEditUndoSnapshot undoSnapshot = WorldEditUndoSnapshot.capture(client.world, touchedSelections);
        if (undoSnapshot == null) {
            client.player.sendMessage(Text.literal("[WE] Failed to snapshot the current selection for undo."), false);
            return null;
        }

        if (undoSnapshot.isEmpty()) {
            client.player.sendMessage(Text.literal("[WE] Undo snapshot was empty; aborting tracked edit."), false);
            return null;
        }

        HistoryEntry entry = new HistoryEntry(label, undoSnapshot, redoCommands);
        WorldEditCommandQueue.setPendingCompletion(() -> {
            UNDO.push(entry);
            trim(UNDO);
            REDO.clear();
        });
        return redoCommands;
    }

    public static boolean queueUndo() {
        if (WorldEditCommandQueue.isBusy() || UNDO.isEmpty()) {
            return false;
        }
        HistoryEntry entry = UNDO.pop();
        List<String> undoCommands = entry.undoSnapshot.toCommands();
        if (undoCommands.isEmpty()) {
            UNDO.push(entry);
            return false;
        }
        WorldEditCommandQueue.setPendingCompletion(() -> {
            REDO.push(entry);
            trim(REDO);
        });
        boolean queued = WorldEditCommandQueue.submit("undo " + entry.label, undoCommands);
        if (!queued) {
            WorldEditCommandQueue.setPendingCompletion(null);
            UNDO.push(entry);
        }
        return queued;
    }

    public static boolean queueRedo() {
        if (WorldEditCommandQueue.isBusy() || REDO.isEmpty()) {
            return false;
        }
        HistoryEntry entry = REDO.pop();
        WorldEditCommandQueue.setPendingCompletion(() -> {
            UNDO.push(entry);
            trim(UNDO);
        });
        boolean queued = WorldEditCommandQueue.submit("redo " + entry.label, entry.redoCommands);
        if (!queued) {
            WorldEditCommandQueue.setPendingCompletion(null);
            REDO.push(entry);
        }
        return queued;
    }

    private static void trim(Deque<HistoryEntry> stack) {
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    private record HistoryEntry(String label, WorldEditUndoSnapshot undoSnapshot, List<String> redoCommands) {
    }
}
