package me.m0dii.modules.actionrunner;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

    public void run(ServerPlayer player) {
        if (queue.isEmpty() || running) {
            return;
        }
        running = true;
        currentIndex = 0;
        player.sendSystemMessage(Component.literal("[ActionRunner] Running queued actions..."));
    }

    public void runClient(FabricClientCommandSource source) {
        if (queue.isEmpty() || running) {
            return;
        }
        running = true;
        currentIndex = 0;
        globalTick = 0; // Reset global tick on run
        source.sendFeedback(Component.literal("[ActionRunner] Running queued actions..."));
    }

    public void onClientTick() {
        if (!running || currentIndex >= queue.size()) {
            return;
        }
        globalTick++;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var manager = mc.gameMode;
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
            player.sendSystemMessage(Component.literal("[ActionRunner] All actions executed."));
            clear();
        }
    }

    private void executeAction(Action action, Minecraft mc, LocalPlayer player, MultiPlayerGameMode manager, int tick) {
        switch (action.type) {
            case ITEM_USE -> {
                manager.useItem(player, InteractionHand.MAIN_HAND);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed ITEM_USE at tick " + tick));
            }
            case ITEM_USE_OFFHAND -> {
                manager.useItem(player, InteractionHand.OFF_HAND);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed ITEM_USE_OFFHAND at tick " + tick));
            }
            case LEFT_CLICK -> {
                var crosshair = mc.hitResult;
                if (crosshair != null && crosshair.getType() == HitResult.Type.BLOCK) {
                    var blockHit = (BlockHitResult) crosshair;
                    manager.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
                    player.sendSystemMessage(Component.literal("[ActionRunner] Executed LEFT_CLICK on block at tick " + tick));
                } else {
                    player.sendSystemMessage(Component.literal("[ActionRunner] No block to LEFT_CLICK at tick " + tick));
                }
            }
            case SNEAK -> {
                mc.options.keyShift.setDown(true);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed SNEAK at tick " + tick));
            }
            case JUMP -> {
                mc.options.keyJump.setDown(true);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed JUMP at tick " + tick));
            }
            case DROP -> {
                mc.gameMode.dropItem(player, false);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed DROP at tick " + tick));
            }
            case COMMAND -> {
                player.connection.sendCommand(action.command);
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed COMMAND: /" + action.command + " at tick " + tick));
            }
            case SWAP_OFFHAND -> {
                int mainSlot = player.getInventory().getSelectedSlot();
                int offhandSlot = 40;

                ItemStack main = player.getInventory().getItem(mainSlot);
                ItemStack off = player.getInventory().getItem(offhandSlot);

                player.getInventory().setItem(mainSlot, off);
                player.getInventory().setItem(offhandSlot, main);

                mc.player.getInventory().tick();
                player.inventoryMenu.broadcastChanges();
                player.sendSystemMessage(Component.literal("[ActionRunner] Executed SWAP_OFFHAND at tick " + tick));
            }
            case SELECT_HOTBAR_SLOT -> {
                if (action.command == null) {
                    player.sendSystemMessage(Component.literal("[ActionRunner] No slot provided for SELECT_HOTBAR_SLOT at tick " + tick));
                    break;
                }
                try {
                    int slot = Integer.parseInt(action.command);
                    if (slot >= 0 && slot <= 8) {
                        mc.player.getInventory().setSelectedSlot(slot);
                        player.sendSystemMessage(Component.literal("[ActionRunner] Selected hotbar slot " + slot + " at tick " + tick));
                    } else {
                        player.sendSystemMessage(Component.literal("[ActionRunner] Invalid hotbar slot: " + slot + " at tick " + tick));
                    }
                } catch (NumberFormatException e) {
                    player.sendSystemMessage(Component.literal("[ActionRunner] Invalid hotbar slot command: " + action.command + " at tick " + tick));
                }
            }
        }
    }

    public Iterable<Action> getActions() {
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
                info += ", Params: " + params;
            }
            return info;
        }
    }
}
