package me.m0dii.modules.worldedit;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code //materials}: counts every block type in the current selection (what you would need to build it).
 * Counting is spread over ticks so big selections don't freeze the game; blocks in unloaded chunks are
 * reported separately. {@code //materials copy} copies the last result as CSV.
 */
public final class MaterialsCommand {
    private static final long MAX_VOLUME = 64L * 1024 * 1024;
    private static final int BLOCKS_PER_TICK = 200_000;
    private static final int SHOWN_ROWS = 20;

    private static Task running;
    private static String lastCsv;

    private MaterialsCommand() {
    }

    private static final class Task {
        final WorldEditSelection selection;
        final Map<Block, Long> counts = new HashMap<>();
        final long volume;
        long index;
        long unloaded;
        long air;
        int ticks;

        Task(WorldEditSelection selection) {
            this.selection = selection;
            this.volume = selection.volume();
        }
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("/materials")
                        .requires(source -> WorldEditModule.INSTANCE.isEnabled())
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Component.literal("[WE] " + start()));
                            return 1;
                        })
                        .then(ClientCommands.literal("copy").executes(ctx -> {
                            if (lastCsv == null) {
                                ctx.getSource().sendError(Component.literal("[WE] Run //materials first."));
                                return 0;
                            }
                            ctx.getSource().getClient().keyboardHandler.setClipboard(lastCsv);
                            ctx.getSource().sendFeedback(Component.literal("[WE] Copied the materials list as CSV."));
                            return 1;
                        }))
                        .then(ClientCommands.literal("cancel").executes(ctx -> {
                            boolean wasRunning = running != null;
                            running = null;
                            ctx.getSource().sendFeedback(Component.literal(wasRunning ? "[WE] Cancelled." : "[WE] Nothing to cancel."));
                            return 1;
                        }))));
        ClientTickEvents.END_CLIENT_TICK.register(MaterialsCommand::tick);
    }

    private static String start() {
        WorldEditSelection selection = WorldEditSelection.fromDebugDrawSelection();
        if (selection == null) {
            return "No selection. Set //pos1 and //pos2 first.";
        }
        if (selection.volume() > MAX_VOLUME) {
            return "Selection is too big (" + String.format(Locale.ROOT, "%,d", selection.volume()) + " blocks; max "
                    + String.format(Locale.ROOT, "%,d", MAX_VOLUME) + ").";
        }
        running = new Task(selection);
        return "Counting " + String.format(Locale.ROOT, "%,d", selection.volume()) + " blocks...";
    }

    private static void tick(Minecraft client) {
        Task task = running;
        if (task == null) {
            return;
        }
        if (client.level == null || client.player == null) {
            running = null;
            return;
        }
        WorldEditSelection sel = task.selection;
        long sizeX = sel.sizeX();
        long sizeZ = sel.sizeZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long end = Math.min(task.volume, task.index + BLOCKS_PER_TICK);
        for (long i = task.index; i < end; i++) {
            int x = sel.min().getX() + (int) (i % sizeX);
            int z = sel.min().getZ() + (int) ((i / sizeX) % sizeZ);
            int y = sel.min().getY() + (int) (i / (sizeX * sizeZ));
            if (!client.level.hasChunk(x >> 4, z >> 4)) {
                task.unloaded++;
                continue;
            }
            pos.set(x, y, z);
            BlockState state = client.level.getBlockState(pos);
            if (state.isAir()) {
                task.air++;
                continue;
            }
            task.counts.merge(state.getBlock(), 1L, Long::sum);
        }
        task.index = end;
        task.ticks++;
        if (task.index < task.volume) {
            if (task.ticks % 20 == 0) {
                client.player.sendOverlayMessage(Component.literal(String.format(Locale.ROOT, "Counting materials… %d%%",
                        task.index * 100 / task.volume)));
            }
            return;
        }
        running = null;
        report(client, task);
    }

    private static void report(Minecraft client, Task task) {
        List<Map.Entry<Block, Long>> sorted = MaterialsMath.sorted(task.counts, MaterialsCommand::nameOf);
        lastCsv = MaterialsMath.csv(sorted, MaterialsCommand::idOf, MaterialsCommand::nameOf);
        long solid = task.counts.values().stream().mapToLong(Long::longValue).sum();
        client.player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                "[WE] Materials: %,d blocks of %d types (%,d air%s)", solid, sorted.size(), task.air,
                task.unloaded > 0 ? String.format(Locale.ROOT, ", %,d in unloaded chunks not counted", task.unloaded) : ""))
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < Math.min(SHOWN_ROWS, sorted.size()); i++) {
            Map.Entry<Block, Long> entry = sorted.get(i);
            client.player.sendSystemMessage(Component.literal(String.format(Locale.ROOT, "  %,7d  ", entry.getValue()))
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(nameOf(entry.getKey())).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("  " + MaterialsMath.stacks(entry.getValue())).withStyle(ChatFormatting.GRAY)));
        }
        if (sorted.size() > SHOWN_ROWS) {
            client.player.sendSystemMessage(Component.literal("  … and " + (sorted.size() - SHOWN_ROWS) + " more")
                    .withStyle(ChatFormatting.GRAY));
        }
        client.player.sendSystemMessage(Component.literal("[Copy all as CSV]").setStyle(Style.EMPTY
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent.CopyToClipboard(lastCsv))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("block id, name, count, stacks")))));
    }

    private static String nameOf(Block block) {
        return block.getName().getString();
    }

    private static String idOf(Block block) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }
}
