package me.m0dii.modules.mobai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class MobAiDebugCommands {
    private static final int COLOR_HEADER = 0xFFF4F4F4;
    private static final int COLOR_INFO = 0xFF9EDBFF;
    private static final int COLOR_PATH = 0xFFF0BE66;
    private static final int COLOR_GOAL = 0xFFD9C0FF;
    private static final int COLOR_MEMORY = 0xFF8AD6E2;
    private static final int COLOR_WARN = 0xFFFFB36B;
    private static final int COLOR_ERROR = 0xFFFF8F8F;

    private static final List<String> TRACKER_DISPLAYS = List.of(
            "health",
            "item_pickup",
            "pathfinding",
            "velocity",
            "villager_breeding",
            "villager_buddy_detection",
            "villager_hostile_detection",
            "villager_iron_golem_spawning"
    );

    private static final Map<String, Integer> HOSTILE_RANGES = Map.ofEntries(
            Map.entry("all", 15),
            Map.entry("drowned", 8),
            Map.entry("evoker", 12),
            Map.entry("husk", 8),
            Map.entry("illusioner", 12),
            Map.entry("pillager", 15),
            Map.entry("ravager", 12),
            Map.entry("vex", 8),
            Map.entry("vindicator", 10),
            Map.entry("zoglin", 10),
            Map.entry("zombie", 8),
            Map.entry("zombie_villager", 8)
    );

    private static final Map<UUID, TrackerConfigState> TRACKER_STATES = new HashMap<>();

    private static final SimpleCommandExceptionType NOT_MOB = new SimpleCommandExceptionType(Component.literal("Target must be a mob"));
    private static final SimpleCommandExceptionType WRONG_WORLD = new SimpleCommandExceptionType(Component.literal("Target mob must be in your world"));
    private static final SimpleCommandExceptionType UNKNOWN_TRACKER_DISPLAY = new SimpleCommandExceptionType(Component.literal("Unknown tracker display"));
    private static final SimpleCommandExceptionType UNKNOWN_HOSTILE = new SimpleCommandExceptionType(Component.literal("Unknown hostile type"));

    private MobAiDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TRACKER_STATES.remove(handler.player.getUUID()));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("mobai")
                .executes(context -> showHelp(context.getSource()))
                .then(literal("clear")
                        .executes(context -> clearAll(context.getSource())))
                .then(literal("status")
                        .executes(context -> showTrackerStatus(context.getSource())))
                .then(literal("toggle")
                        .then(argument("display", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(TRACKER_DISPLAYS, builder))
                                .executes(context -> toggleTrackerDisplay(
                                        StringArgumentType.getString(context, "display"),
                                        null,
                                        context.getSource()))
                                .then(argument("hostile", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(HOSTILE_RANGES.keySet(), builder))
                                        .executes(context -> toggleTrackerDisplay(
                                                StringArgumentType.getString(context, "display"),
                                                StringArgumentType.getString(context, "hostile"),
                                                context.getSource())))))
                .then(literal("boxes")
                        .executes(context -> toggleTrackerBoxes(context.getSource())))
                .then(literal("radius")
                        .then(argument("blocks", IntegerArgumentType.integer(8, 256))
                                .executes(context -> setTrackerRadius(IntegerArgumentType.getInteger(context, "blocks"), context.getSource()))))
                .then(literal("transparency")
                        .then(argument("alpha", IntegerArgumentType.integer(0, 255))
                                .executes(context -> setTrackerTransparency(IntegerArgumentType.getInteger(context, "alpha"), context.getSource()))))
                .then(literal("inspect")
                        .then(argument("entity", EntityArgument.entity())
                                .executes(context -> inspect(EntityArgument.getEntity(context, "entity"), context.getSource()))))
                .then(literal("brain")
                        .then(literal("inspect")
                                .then(argument("entity", EntityArgument.entity())
                                        .executes(context -> inspect(EntityArgument.getEntity(context, "entity"), context.getSource())))))
                .then(literal("goals")
                        .then(literal("inspect")
                                .then(argument("entity", EntityArgument.entity())
                                        .executes(context -> inspectGoals(EntityArgument.getEntity(context, "entity"), context.getSource())))))
                .then(literal("path")
                        .then(literal("clear")
                                .executes(context -> clear(context.getSource(), false, true)))
                        .then(literal("simulate")
                                .then(argument("entity", EntityArgument.entity())
                                        .then(argument("target", BlockPosArgument.blockPos())
                                                .executes(context -> simulatePath(
                                                        EntityArgument.getEntity(context, "entity"),
                                                        BlockPosArgument.getLoadedBlockPos(context, "target"),
                                                        context.getSource())))))));
    }

    private static int showHelp(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        player.sendSystemMessage(Component.literal("[MobAI] Commands:").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("/mobai toggle <display> [hostile]").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("/mobai boxes | /mobai radius <blocks> | /mobai transparency <0-255> | /mobai status").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("/mobai inspect <entity> | /mobai goals inspect <entity>").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("/mobai path simulate <entity> <x> <y> <z> | /mobai path clear | /mobai clear").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Displays: " + String.join(", ", TRACKER_DISPLAYS)).withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int clearAll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        clear(source, true, true);
        TRACKER_STATES.remove(player.getUUID());
        syncTrackerConfig(player, new TrackerConfigState());
        player.sendSystemMessage(Component.literal("[MobAI] Cleared tracker displays, inspect overlay, and path preview")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int clear(CommandSourceStack source, boolean clearInspect, boolean clearPath) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        ServerPlayNetworking.send(player, new MobAiDebugPayloads.ClearPayload(clearInspect, clearPath));
        player.sendSystemMessage(Component.literal("[MobAI] Cleared " +
                (clearInspect && clearPath ? "inspect and path preview" : clearPath ? "path preview" : "inspect overlay"))
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int showTrackerStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        TrackerConfigState state = trackerState(player);
        player.sendSystemMessage(Component.literal("[MobAI] Tracker").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(" - Displays: " + (state.enabledDisplays.isEmpty() ? "none" : String.join(", ", state.enabledDisplays))));
        player.sendSystemMessage(Component.literal(" - Boxes: " + (state.showBoxes ? "on" : "off")
                + " | Radius: " + state.radius
                + " | Alpha: " + state.alpha
                + " | Hostile focus: " + state.hostileFocus));
        return 1;
    }

    private static int toggleTrackerBoxes(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        TrackerConfigState state = trackerState(player);
        state.showBoxes = !state.showBoxes;
        syncTrackerConfig(player, state);
        player.sendSystemMessage(Component.literal("[MobAI] Tracker boxes " + (state.showBoxes ? "enabled" : "disabled"))
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int setTrackerRadius(int radius, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        TrackerConfigState state = trackerState(player);
        state.radius = Math.clamp(radius, 8, 256);
        syncTrackerConfig(player, state);
        player.sendSystemMessage(Component.literal("[MobAI] Tracker radius set to " + state.radius).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int setTrackerTransparency(int alpha, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        TrackerConfigState state = trackerState(player);
        state.alpha = Math.clamp(alpha, 0, 255);
        syncTrackerConfig(player, state);
        player.sendSystemMessage(Component.literal("[MobAI] Tracker transparency set to " + state.alpha).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int toggleTrackerDisplay(String display, String hostile, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        String normalizedDisplay = normalizeDisplay(display);
        if (!TRACKER_DISPLAYS.contains(normalizedDisplay)) {
            throw UNKNOWN_TRACKER_DISPLAY.create();
        }

        TrackerConfigState state = trackerState(player);
        if ("villager_hostile_detection".equals(normalizedDisplay)) {
            String normalizedHostile = normalizeHostile(hostile);
            boolean disable = state.enabledDisplays.contains(normalizedDisplay) && Objects.equals(state.hostileFocus, normalizedHostile);
            if (disable) {
                state.enabledDisplays.remove(normalizedDisplay);
            } else {
                state.enabledDisplays.add(normalizedDisplay);
                state.hostileFocus = normalizedHostile;
            }
            syncTrackerConfig(player, state);
            player.sendSystemMessage(Component.literal("[MobAI] " + normalizedDisplay + " "
                    + (disable ? "disabled" : "enabled")
                    + (disable ? "" : " (" + normalizedHostile + ")"))
                    .withStyle(disable ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
            return 1;
        }

        if (hostile != null && !hostile.isBlank()) {
            throw UNKNOWN_HOSTILE.create();
        }

        boolean enable = !state.enabledDisplays.contains(normalizedDisplay);
        if (enable) {
            state.enabledDisplays.add(normalizedDisplay);
        } else {
            state.enabledDisplays.remove(normalizedDisplay);
        }
        syncTrackerConfig(player, state);
        player.sendSystemMessage(Component.literal("[MobAI] " + normalizedDisplay + " " + (enable ? "enabled" : "disabled"))
                .withStyle(enable ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return 1;
    }

    private static TrackerConfigState trackerState(ServerPlayer player) {
        return TRACKER_STATES.computeIfAbsent(player.getUUID(), uuid -> new TrackerConfigState());
    }

    private static void syncTrackerConfig(ServerPlayer player, TrackerConfigState state) {
        ServerPlayNetworking.send(player, new MobAiDebugPayloads.TrackerConfigPayload(
                List.copyOf(state.enabledDisplays),
                state.showBoxes,
                state.alpha,
                state.radius,
                state.hostileFocus
        ));
    }

    private static String normalizeDisplay(String display) {
        return display == null ? "" : display.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHostile(String hostile) throws CommandSyntaxException {
        String normalized = hostile == null || hostile.isBlank() ? "all" : hostile.trim().toLowerCase(Locale.ROOT);
        if (!HOSTILE_RANGES.containsKey(normalized)) {
            throw UNKNOWN_HOSTILE.create();
        }
        return normalized;
    }

    private static int inspect(Entity entity, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        Mob mob = requireMobInPlayerWorld(entity, player);
        List<MobAiDebugPayloads.DebugLine> lines = buildInspectLines(mob);

        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Inspect", lines);
        return 1;
    }

    private static int inspectGoals(Entity entity, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        Mob mob = requireMobInPlayerWorld(entity, player);
        List<MobAiDebugPayloads.DebugLine> lines = buildGoalInspectLines(mob);

        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Goals", lines);
        return 1;
    }

    private static int simulatePath(Entity entity, BlockPos target, CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = requirePlayer(source);
        Mob mob = requireMobInPlayerWorld(entity, player);

        Path path = mob.getNavigation().createPath(target, 0);
        List<MobAiDebugPayloads.DebugLine> lines = buildPathSimulationLines(mob, target, path);
        MobAiDebugPayloads.PathPreviewPayload payload = buildPathPreviewPayload(mob, target, path, lines);

        ServerPlayNetworking.send(player, payload);
        ServerPlayNetworking.send(player, new MobAiDebugPayloads.InspectPayload(mob.getId(), lines));
        sendLinesToChat(player, "Path", lines);
        return 1;
    }

    private static Mob requireMobInPlayerWorld(Entity entity, ServerPlayer player) throws CommandSyntaxException {
        if (!(entity instanceof Mob mob)) {
            throw NOT_MOB.create();
        }
        if (mob.level() != player.level()) {
            throw WRONG_WORLD.create();
        }
        return mob;
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    private static List<MobAiDebugPayloads.DebugLine> buildInspectLines(Mob mob) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        Brain<?> brain = mob.getBrain();
        Path currentPath = mob.getNavigation().getPath();

        lines.add(line("Type: " + compactId(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())), COLOR_HEADER));
        lines.add(line("Pos: " + formatPos(mob.blockPosition()), COLOR_INFO));
        lines.add(line(formatPathState(currentPath), COLOR_PATH));

        List<String> activities = brain.getActiveActivities().stream()
                .map(Activity::getName)
                .sorted()
                .map(MobAiDebugCommands::compactToken)
                .limit(3)
                .toList();
        if (!activities.isEmpty()) {
            lines.add(line("Act: " + String.join(", ", activities), COLOR_INFO));
        }

        List<String> tasks = new ArrayList<>();
        for (BehaviorControl<?> task : brain.getRunningBehaviors()) {
            tasks.add(compactToken(task.debugString()));
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

        int mainGoalCount = ((MobEntityAccessor) mob).m0dev$getGoalSelector().getAvailableGoals().size();
        int targetGoalCount = ((MobEntityAccessor) mob).m0dev$getTargetSelector().getAvailableGoals().size();
        lines.add(line("Goals: main " + mainGoalCount + " | target " + targetGoalCount, COLOR_GOAL));

        if (mob instanceof LivingEntity living) {
            lines.add(line(String.format(Locale.ROOT, "HP: %.1f/%.1f", living.getHealth(), living.getMaxHealth()),
                    living.getHealth() < living.getMaxHealth() ? COLOR_WARN : COLOR_HEADER));
        }

        return clampLines(lines, 14);
    }

    private static List<MobAiDebugPayloads.DebugLine> buildGoalInspectLines(Mob mob) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        lines.add(line("Type: " + compactId(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())), COLOR_HEADER));
        lines.add(line("Pos: " + formatPos(mob.blockPosition()), COLOR_INFO));
        addGoalLines(lines, "Main", ((MobEntityAccessor) mob).m0dev$getGoalSelector());
        addGoalLines(lines, "Target", ((MobEntityAccessor) mob).m0dev$getTargetSelector());
        return clampLines(lines, 16);
    }

    private static void addGoalLines(List<MobAiDebugPayloads.DebugLine> lines, String label, GoalSelector selector) {
        List<WrappedGoal> goals = selector.getAvailableGoals().stream()
                .sorted(Comparator.comparingInt(WrappedGoal::getPriority)
                        .thenComparing(goal -> !goal.isRunning()))
                .toList();

        if (goals.isEmpty()) {
            lines.add(line(label + ": none", COLOR_GOAL));
            return;
        }

        lines.add(line(label + ": " + goals.size(), COLOR_GOAL));
        for (WrappedGoal prioritizedGoal : goals) {
            Goal goal = prioritizedGoal.getGoal();
            StringBuilder builder = new StringBuilder()
                    .append(label.charAt(0))
                    .append(" P")
                    .append(prioritizedGoal.getPriority())
                    .append(' ')
                    .append(prioritizedGoal.isRunning() ? "run " : "idle ")
                    .append(compactToken(goal.getClass().getSimpleName()));

            String controls = prioritizedGoal.getFlags().stream()
                    .map(Enum::name)
                    .map(MobAiDebugCommands::compactToken)
                    .collect(Collectors.joining(","));
            if (!controls.isBlank()) {
                builder.append(" [").append(controls).append(']');
            }

            if (prioritizedGoal.isRunning()) {
                builder.append(" keep=").append(prioritizedGoal.canContinueToUse() ? "yes" : "no");
            } else {
                builder.append(" start=").append(prioritizedGoal.canUse() ? "yes" : "no");
            }

            lines.add(line(builder.toString(), prioritizedGoal.isRunning() ? COLOR_GOAL : COLOR_INFO));
            if (lines.size() >= 16) {
                break;
            }
        }
    }

    private static List<MobAiDebugPayloads.DebugLine> buildPathSimulationLines(Mob mob, BlockPos target, Path path) {
        List<MobAiDebugPayloads.DebugLine> lines = new ArrayList<>();
        lines.add(line("Sim target: " + formatPos(target), COLOR_HEADER));

        if (path == null) {
            lines.add(line("Sim: no path", COLOR_ERROR));
            lines.add(line("Reach: no", COLOR_ERROR));
            return lines;
        }

        String state = path.canReach() ? "complete" : "partial";
        lines.add(line("Sim: " + state + " " + path.getNextNodeIndex() + "/" + path.getNodeCount(), path.canReach() ? COLOR_PATH : COLOR_WARN));
        lines.add(line("Reach: " + (path.canReach() ? "yes" : "no") + " | manhattan " + path.getDistToTarget(),
                path.canReach() ? COLOR_INFO : COLOR_WARN));

        Path.DebugData debugNodeInfo = path.debugData();
        if (debugNodeInfo != null) {
            lines.add(line("Open: " + debugNodeInfo.openSet().size() + " | Closed: " + debugNodeInfo.closedSet().size(), COLOR_INFO));
        }

        return clampLines(lines, 8);
    }

    private static MobAiDebugPayloads.PathPreviewPayload buildPathPreviewPayload(Mob mob,
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

        List<BlockPos> nodes = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            nodes.add(path.getNodePos(i));
        }

        List<BlockPos> openNodes = new ArrayList<>();
        List<BlockPos> closedNodes = new ArrayList<>();
        Path.DebugData debugNodeInfo = path.debugData();
        if (debugNodeInfo != null) {
            for (Node openNode : debugNodeInfo.openSet()) {
                openNodes.add(openNode.asBlockPos());
            }
            for (Node closedNode : debugNodeInfo.closedSet()) {
                closedNodes.add(closedNode.asBlockPos());
            }
        }

        return new MobAiDebugPayloads.PathPreviewPayload(
                mob.getId(),
                target,
                true,
                path.canReach(),
                path.getNextNodeIndex(),
                Math.round(path.getDistToTarget()),
                nodes,
                openNodes,
                closedNodes,
                lines
        );
    }

    private static <T> void addMemoryLine(List<MobAiDebugPayloads.DebugLine> lines, Brain<?> brain, MemoryModuleType<T> type, String label) {
        Optional<T> memory = brain.getMemoryInternal(type);
        if (memory == null || memory.isEmpty()) {
            return;
        }

        long expiry = brain.getTimeUntilExpiry(type);
        String expirySuffix = expiry == Long.MAX_VALUE ? "" : " (" + expiry + "t)";
        lines.add(line(label + ": " + formatMemoryValue(memory.get()) + expirySuffix, COLOR_MEMORY));
    }

    private static String formatPathState(Path path) {
        if (path == null) {
            return "Nav: idle";
        }
        return "Nav: " + path.getNextNodeIndex() + "/" + path.getNodeCount() +
                (path.isDone() ? " done" : " -> " + formatPos(path.getTarget()));
    }

    private static String formatMemoryValue(Object value) {
        return switch (value) {
            case WalkTarget walkTarget -> "walk " + formatPos(walkTarget.getTarget().currentBlockPosition());
            case PositionTracker lookTarget -> "look " + formatPos(lookTarget.currentBlockPosition());
            case GlobalPos globalPos -> compactId(globalPos.dimension().identifier()) + " " + formatPos(globalPos.pos());
            case LivingEntity livingEntity -> compactId(BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType())) + "#" + livingEntity.getId();
            case BlockPos blockPos -> formatPos(blockPos);
            default -> truncate(String.valueOf(value), 44);
        };
    }

    private static void sendLinesToChat(ServerPlayer player, String label, List<MobAiDebugPayloads.DebugLine> lines) {
        player.sendSystemMessage(Component.literal("[MobAI] " + label).withStyle(ChatFormatting.GOLD));
        for (MobAiDebugPayloads.DebugLine line : lines) {
            player.sendSystemMessage(Component.literal(" - " + line.text()));
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

    private static final class TrackerConfigState {
        private final LinkedHashSet<String> enabledDisplays = new LinkedHashSet<>();
        private boolean showBoxes = true;
        private int alpha = 144;
        private int radius = 48;
        private String hostileFocus = "all";
    }
}
