package me.m0dii.modules.mobai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MobAiDebugCommands {
    private static final int COLOR_HEADER = 0xFFF4F4F4;
    private static final int COLOR_INFO = 0xFF9EDBFF;
    private static final int COLOR_PATH = 0xFFF0BE66;
    private static final int COLOR_GOAL = 0xFFD9C0FF;
    private static final int COLOR_MEMORY = 0xFF8AD6E2;
    private static final int COLOR_WARN = 0xFFFFB36B;
    private static final int COLOR_ERROR = 0xFFFF8F8F;
    private static final SimpleCommandExceptionType NOT_MOB = new SimpleCommandExceptionType(Text.literal("Target must be a mob"));
    private static final SimpleCommandExceptionType WRONG_WORLD = new SimpleCommandExceptionType(Text.literal("Target mob must be in your world"));

    private MobAiDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mobai")
                .executes(context -> showHelp(context.getSource()))
                .then(literal("clear")
                        .executes(context -> clear(context.getSource(), true, true)))
                .then(literal("inspect")
                        .then(argument("entity", EntityArgumentType.entity())
                                .executes(context -> inspect(EntityArgumentType.getEntity(context, "entity"), context.getSource()))))
                .then(literal("brain")
                        .then(literal("inspect")
                                .then(argument("entity", EntityArgumentType.entity())
                                        .executes(context -> inspect(EntityArgumentType.getEntity(context, "entity"), context.getSource())))))
                .then(literal("goals")
                        .then(literal("inspect")
                                .then(argument("entity", EntityArgumentType.entity())
                                        .executes(context -> inspectGoals(EntityArgumentType.getEntity(context, "entity"), context.getSource())))))
                .then(literal("path")
                        .then(literal("clear")
                                .executes(context -> clear(context.getSource(), false, true)))
                        .then(literal("simulate")
                                .then(argument("entity", EntityArgumentType.entity())
                                        .then(argument("target", BlockPosArgumentType.blockPos())
                                                .executes(context -> simulatePath(
                                                        EntityArgumentType.getEntity(context, "entity"),
                                                        BlockPosArgumentType.getLoadedBlockPos(context, "target"),
                                                        context.getSource())))))));
    }

    private static int showHelp(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = requirePlayer(source);
        player.sendMessage(Text.literal("[MobAI] Commands:").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("/mobai inspect <entity>").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("/mobai brain inspect <entity>").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("/mobai goals inspect <entity>").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("/mobai path simulate <entity> <x> <y> <z>").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("/mobai path clear | /mobai clear").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int clear(ServerCommandSource source, boolean clearInspect, boolean clearPath) throws CommandSyntaxException {
        ServerPlayerEntity player = requirePlayer(source);
        ServerPlayNetworking.send(player, new MobAiDebugPayloads.ClearPayload(clearInspect, clearPath));
        player.sendMessage(Text.literal("[MobAI] Cleared " +
                (clearInspect && clearPath ? "inspect and path preview" : clearPath ? "path preview" : "inspect overlay"))
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int inspect(Entity entity, ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = requirePlayer(source);
        MobEntity mob = requireMobInPlayerWorld(entity, player);
        List<MobAiDebugPayloads.DebugLine> lines = buildInspectLines(mob);

        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Inspect", lines);
        return 1;
    }

    private static int inspectGoals(Entity entity, ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = requirePlayer(source);
        MobEntity mob = requireMobInPlayerWorld(entity, player);
        List<MobAiDebugPayloads.DebugLine> lines = buildGoalInspectLines(mob);

        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Goals", lines);
        return 1;
    }

    private static int simulatePath(Entity entity, BlockPos target, ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = requirePlayer(source);
        MobEntity mob = requireMobInPlayerWorld(entity, player);

        Path path = mob.getNavigation().findPathTo(target, 0);
        List<MobAiDebugPayloads.DebugLine> lines = buildPathSimulationLines(mob, target, path);
        MobAiDebugPayloads.PathPreviewPayload payload = buildPathPreviewPayload(mob, target, path, lines);

        ServerPlayNetworking.send(player, payload);
        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Path", lines);
        return 1;
    }

    private static MobEntity requireMobInPlayerWorld(Entity entity, ServerPlayerEntity player) throws CommandSyntaxException {
        if (!(entity instanceof MobEntity mob)) {
            throw NOT_MOB.create();
        }
        if (mob.getEntityWorld() != player.getEntityWorld()) {
            throw WRONG_WORLD.create();
        }
        return mob;
    }

    private static ServerPlayerEntity requirePlayer(ServerCommandSource source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    private static List<MobAiDebugPayloads.DebugLine> buildInspectLines(MobEntity mob) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        Brain<?> brain = mob.getBrain();
        Path currentPath = mob.getNavigation().getCurrentPath();

        lines.add(line("Type: " + compactId(Registries.ENTITY_TYPE.getId(mob.getType())), COLOR_HEADER));
        lines.add(line("Pos: " + formatPos(mob.getBlockPos()), COLOR_INFO));
        lines.add(line(formatPathState(currentPath), COLOR_PATH));

        List<String> activities = brain.getPossibleActivities().stream()
                .map(Activity::getId)
                .sorted()
                .map(MobAiDebugCommands::compactToken)
                .limit(3)
                .toList();
        if (!activities.isEmpty()) {
            lines.add(line("Act: " + String.join(", ", activities), COLOR_INFO));
        }

        List<String> tasks = new ArrayList<>();
        for (Task<?> task : brain.getRunningTasks()) {
            tasks.add(compactToken(task.getName()));
            if (tasks.size() >= 3) {
                break;
            }
        }
        lines.add(line(tasks.isEmpty() ? "Task: none" : "Task: " + String.join(", ", tasks), COLOR_INFO));

        addMemoryLine(lines, brain, MemoryModuleType.WALK_TARGET, "Mem walk");
        addMemoryLine(lines, brain, MemoryModuleType.LOOK_TARGET, "Mem look");
        addMemoryLine(lines, brain, MemoryModuleType.ATTACK_TARGET, "Mem attack");
        addMemoryLine(lines, brain, MemoryModuleType.INTERACTION_TARGET, "Mem interact");
        addMemoryLine(lines, brain, MemoryModuleType.HOME, "Mem home");
        addMemoryLine(lines, brain, MemoryModuleType.JOB_SITE, "Mem job");
        addMemoryLine(lines, brain, MemoryModuleType.DISTURBANCE_LOCATION, "Mem disturb");

        int mainGoalCount = ((MobEntityAccessor) mob).m0dev$getGoalSelector().getGoals().size();
        int targetGoalCount = ((MobEntityAccessor) mob).m0dev$getTargetSelector().getGoals().size();
        lines.add(line("Goals: main " + mainGoalCount + " | target " + targetGoalCount, COLOR_GOAL));

        if (mob instanceof LivingEntity living) {
            lines.add(line(String.format(Locale.ROOT, "HP: %.1f/%.1f", living.getHealth(), living.getMaxHealth()),
                    living.getHealth() < living.getMaxHealth() ? COLOR_WARN : COLOR_HEADER));
        }

        return clampLines(lines, 14);
    }

    private static List<MobAiDebugPayloads.DebugLine> buildGoalInspectLines(MobEntity mob) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        lines.add(line("Type: " + compactId(Registries.ENTITY_TYPE.getId(mob.getType())), COLOR_HEADER));
        lines.add(line("Pos: " + formatPos(mob.getBlockPos()), COLOR_INFO));
        addGoalLines(lines, "Main", ((MobEntityAccessor) mob).m0dev$getGoalSelector());
        addGoalLines(lines, "Target", ((MobEntityAccessor) mob).m0dev$getTargetSelector());
        return clampLines(lines, 16);
    }

    private static void addGoalLines(List<MobAiDebugPayloads.DebugLine> lines, String label, GoalSelector selector) {
        List<PrioritizedGoal> goals = selector.getGoals().stream()
                .sorted(Comparator.comparingInt(PrioritizedGoal::getPriority)
                        .thenComparing(goal -> !goal.isRunning()))
                .toList();

        if (goals.isEmpty()) {
            lines.add(line(label + ": none", COLOR_GOAL));
            return;
        }

        lines.add(line(label + ": " + goals.size(), COLOR_GOAL));
        for (PrioritizedGoal prioritizedGoal : goals) {
            Goal goal = prioritizedGoal.getGoal();
            StringBuilder builder = new StringBuilder()
                    .append(label.charAt(0))
                    .append(" P")
                    .append(prioritizedGoal.getPriority())
                    .append(' ')
                    .append(prioritizedGoal.isRunning() ? "run " : "idle ")
                    .append(compactToken(goal.getClass().getSimpleName()));

            String controls = prioritizedGoal.getControls().stream()
                    .map(Enum::name)
                    .map(MobAiDebugCommands::compactToken)
                    .collect(Collectors.joining(","));
            if (!controls.isBlank()) {
                builder.append(" [").append(controls).append(']');
            }

            if (prioritizedGoal.isRunning()) {
                builder.append(" keep=").append(prioritizedGoal.shouldContinue() ? "yes" : "no");
            } else {
                builder.append(" start=").append(prioritizedGoal.canStart() ? "yes" : "no");
            }

            lines.add(line(builder.toString(), prioritizedGoal.isRunning() ? COLOR_GOAL : COLOR_INFO));
            if (lines.size() >= 16) {
                break;
            }
        }
    }

    private static List<MobAiDebugPayloads.DebugLine> buildPathSimulationLines(MobEntity mob, BlockPos target, Path path) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        lines.add(line("Sim target: " + formatPos(target), COLOR_HEADER));

        if (path == null) {
            lines.add(line("Sim: no path", COLOR_ERROR));
            lines.add(line("Reach: no", COLOR_ERROR));
            return lines;
        }

        String state = path.reachesTarget() ? "complete" : "partial";
        lines.add(line("Sim: " + state + " " + path.getCurrentNodeIndex() + "/" + path.getLength(), path.reachesTarget() ? COLOR_PATH : COLOR_WARN));
        lines.add(line("Reach: " + (path.reachesTarget() ? "yes" : "no") + " | manhattan " + path.getManhattanDistanceFromTarget(),
                path.reachesTarget() ? COLOR_INFO : COLOR_WARN));

        Path.DebugNodeInfo debugNodeInfo = path.getDebugNodeInfos();
        if (debugNodeInfo != null) {
            lines.add(line("Open: " + debugNodeInfo.openSet().length + " | Closed: " + debugNodeInfo.closedSet().length, COLOR_INFO));
        }

        return clampLines(lines, 8);
    }

    private static MobAiDebugPayloads.PathPreviewPayload buildPathPreviewPayload(MobEntity mob,
                                                                                 BlockPos target,
                                                                                 Path path,
                                                                                 List<MobAiDebugPayloads.DebugLine> lines) {
        if (path == null) {
            return new MobAiDebugPayloads.PathPreviewPayload(
                    mob.getId(),
                    target,
                    false,
                    false,
                    0,
                    -1,
                    List.of(),
                    List.of(),
                    List.of(),
                    lines
            );
        }

        List<BlockPos> nodes = new ArrayList<>(path.getLength());
        for (int i = 0; i < path.getLength(); i++) {
            nodes.add(path.getNodePos(i));
        }

        List<BlockPos> openNodes = new ArrayList<>();
        List<BlockPos> closedNodes = new ArrayList<>();
        Path.DebugNodeInfo debugNodeInfo = path.getDebugNodeInfos();
        if (debugNodeInfo != null) {
            for (PathNode openNode : debugNodeInfo.openSet()) {
                openNodes.add(openNode.getBlockPos());
            }
            for (PathNode closedNode : debugNodeInfo.closedSet()) {
                closedNodes.add(closedNode.getBlockPos());
            }
        }

        return new MobAiDebugPayloads.PathPreviewPayload(
                mob.getId(),
                target,
                true,
                path.reachesTarget(),
                path.getCurrentNodeIndex(),
                Math.round(path.getManhattanDistanceFromTarget()),
                nodes,
                openNodes,
                closedNodes,
                lines
        );
    }

    private static <T> void addMemoryLine(List<MobAiDebugPayloads.DebugLine> lines, Brain<?> brain, MemoryModuleType<T> type, String label) {
        Optional<T> memory = brain.getOptionalMemory(type);
        if (memory == null || memory.isEmpty()) {
            return;
        }

        long expiry = brain.getMemoryExpiry(type);
        String expirySuffix = expiry == Long.MAX_VALUE ? "" : " (" + expiry + "t)";
        lines.add(line(label + ": " + formatMemoryValue(memory.get()) + expirySuffix, COLOR_MEMORY));
    }

    private static String formatPathState(Path path) {
        if (path == null) {
            return "Nav: idle";
        }
        return "Nav: " + path.getCurrentNodeIndex() + "/" + path.getLength() +
                (path.isFinished() ? " done" : " -> " + formatPos(path.getTarget()));
    }

    private static String formatMemoryValue(Object value) {
        return switch (value) {
            case WalkTarget walkTarget -> "walk " + formatPos(walkTarget.getLookTarget().getBlockPos());
            case LookTarget lookTarget -> "look " + formatPos(lookTarget.getBlockPos());
            case GlobalPos globalPos -> compactId(globalPos.dimension().getValue()) + " " + formatPos(globalPos.pos());
            case LivingEntity livingEntity -> compactId(Registries.ENTITY_TYPE.getId(livingEntity.getType())) + "#" + livingEntity.getId();
            case BlockPos blockPos -> formatPos(blockPos);
            default -> truncate(String.valueOf(value), 44);
        };
    }

    private static void sendLinesToChat(ServerPlayerEntity player, String label, List<MobAiDebugPayloads.DebugLine> lines) {
        player.sendMessage(Text.literal("[MobAI] " + label).formatted(Formatting.GOLD), false);
        for (MobAiDebugPayloads.DebugLine line : lines) {
            player.sendMessage(Text.literal(" - " + line.text()), false);
        }
    }

    private static MobAiDebugPayloads.DebugLine line(String text, int color) {
        return new MobAiDebugPayloads.DebugLine(text, color);
    }

    private static List<MobAiDebugPayloads.DebugLine> clampLines(List<MobAiDebugPayloads.DebugLine> lines, int max) {
        return lines.size() <= max ? List.copyOf(lines) : List.copyOf(lines.subList(0, max));
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String compactId(Identifier id) {
        if (id == null) {
            return "?";
        }
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    private static String compactToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 && lastDot + 1 < value.length() ? value.substring(lastDot + 1) : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
