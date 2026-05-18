package me.m0dii.modules.actionrunner;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionRunnerModule {
    private static final ActionRunnerModule INSTANCE = new ActionRunnerModule();
    private final List<Action> queue = new ArrayList<>();
    private boolean running = false;
    private int currentIndex = 0;
    private int globalTick = 0;

    public static ActionRunnerModule getInstance() {
        return INSTANCE;
    }

    public void addAction(Action action) {
        queue.add(action);
    }

    public void clear() {
        queue.clear();
        running = false;
        currentIndex = 0;
    }

    public void run(ServerPlayerEntity player) {
        if (queue.isEmpty() || running) {
            return;
        }
        running = true;
        currentIndex = 0;
        player.sendMessage(Text.literal("[ActionRunner] Running queued actions..."));
    }

    public void runClient(FabricClientCommandSource source) {
        if (queue.isEmpty() || running) {
            return;
        }
        running = true;
        currentIndex = 0;
        globalTick = 0; // Reset global tick on run
        source.sendFeedback(Text.literal("[ActionRunner] Running queued actions..."));
    }

    public void onClientTick() {
        if (!running || currentIndex >= queue.size()) {
            return;
        }
        globalTick++;
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        var manager = mc.interactionManager;
        if (player == null || manager == null) {
            return;
        }
        boolean executedAny = false;
        // Execute all actions whose delay matches the current global tick
        while (currentIndex < queue.size()) {
            Action action = queue.get(currentIndex);
            if (action.delayTicks == globalTick) {
                executeAction(action, mc, player, manager, globalTick);
                currentIndex++;
                executedAny = true;
            } else if (action.delayTicks < globalTick) {
                // If somehow missed, execute immediately
                executeAction(action, mc, player, manager, globalTick);
                currentIndex++;
                executedAny = true;
            } else {
                // Next action is for a future tick
                break;
            }
        }
        if (currentIndex >= queue.size() && executedAny) {
            player.sendMessage(Text.literal("[ActionRunner] All actions executed."), false);
            clear();
        }
    }

    private void executeAction(Action action, MinecraftClient mc, ClientPlayerEntity player, ClientPlayerInteractionManager manager, int tick) {
        switch (action.type) {
            case ITEM_USE -> {
                manager.interactItem(player, Hand.MAIN_HAND);
                player.sendMessage(Text.literal("[ActionRunner] Executed ITEM_USE at tick " + tick), false);
            }
            case ITEM_USE_OFFHAND -> {
                manager.interactItem(player, Hand.OFF_HAND);
                player.sendMessage(Text.literal("[ActionRunner] Executed ITEM_USE_OFFHAND at tick " + tick), false);
            }
            case LEFT_CLICK -> {
                var crosshair = mc.crosshairTarget;
                if (crosshair != null && crosshair.getType() == HitResult.Type.BLOCK) {
                    var blockHit = (BlockHitResult) crosshair;
                    manager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                    player.sendMessage(Text.literal("[ActionRunner] Executed LEFT_CLICK on block at tick " + tick), false);
                } else {
                    player.sendMessage(Text.literal("[ActionRunner] No block to LEFT_CLICK at tick " + tick), false);
                }
            }
            case SNEAK -> {
                mc.options.sneakKey.setPressed(true);
                player.sendMessage(Text.literal("[ActionRunner] Executed SNEAK at tick " + tick), false);
            }
            case JUMP -> {
                mc.options.jumpKey.setPressed(true);
                player.sendMessage(Text.literal("[ActionRunner] Executed JUMP at tick " + tick), false);
            }
            case DROP -> {
                player.dropSelectedItem(false);
                player.sendMessage(Text.literal("[ActionRunner] Executed DROP at tick " + tick), false);
            }
            case COMMAND -> {
                player.networkHandler.sendChatCommand(action.command);
                player.sendMessage(Text.literal("[ActionRunner] Executed COMMAND: /" + action.command + " at tick " + tick), false);
            }
            case SWAP_OFFHAND -> {
                int mainSlot = player.getInventory().getSelectedSlot();
                int offhandSlot = 40;

                ItemStack main = player.getInventory().getStack(mainSlot);
                ItemStack off = player.getInventory().getStack(offhandSlot);

                player.getInventory().setStack(mainSlot, off);
                player.getInventory().setStack(offhandSlot, main);

                mc.player.getInventory().updateItems();
                player.playerScreenHandler.sendContentUpdates();
                player.sendMessage(Text.literal("[ActionRunner] Executed SWAP_OFFHAND at tick " + tick), false);
            }
            case SELECT_HOTBAR_SLOT -> {
                if (action.command == null) {
                    player.sendMessage(Text.literal("[ActionRunner] No slot provided for SELECT_HOTBAR_SLOT at tick " + tick), false);
                    break;
                }
                try {
                    int slot = Integer.parseInt(action.command);
                    if (slot >= 0 && slot <= 8) {
                        mc.player.getInventory().setSelectedSlot(slot);
                        player.sendMessage(Text.literal("[ActionRunner] Selected hotbar slot " + slot + " at tick " + tick), false);
                    } else {
                        player.sendMessage(Text.literal("[ActionRunner] Invalid hotbar slot: " + slot + " at tick " + tick), false);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.literal("[ActionRunner] Invalid hotbar slot command: " + action.command + " at tick " + tick), false);
                }
            }
        }
    }

    public Iterable<? extends Action> getActions() {
        return queue;
    }

    public record Action(int delayTicks, Type type, String command, Map<String, String> params) {
        public Action(int delayTicks, Type type, String command) {
            this(delayTicks, type, command, new HashMap<>());
        }

        public enum Type {
            ITEM_USE,
            ITEM_USE_OFFHAND,
            LEFT_CLICK,
            SNEAK,
            JUMP,
            DROP,
            COMMAND,
            SWAP_OFFHAND,
            SELECT_HOTBAR_SLOT
        }

        public String getActionInfo() {
            String info = "Type: " + type.name();
            if (command != null) {
                info += ", Command/Param: " + command;
            }
            if (!params.isEmpty()) {
                info += ", Params: " + params.toString();
            }
            return info;
        }
    }
}
