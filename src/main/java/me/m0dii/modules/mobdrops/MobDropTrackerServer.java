package me.m0dii.modules.mobdrops;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MobDropTrackerServer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE_NAME = "m0dev-mobdrops.json";
    private static final int DEFAULT_RADIUS = 8;
    private static final int MAX_RADIUS = 128;
    private static final long RATE_WINDOW_TICKS = 20L * 60L;

    private static final Map<String, TrackerData> TRACKERS = new LinkedHashMap<>();
    private static final Map<UUID, OverlayPreference> VIEWER_PREFERENCES = new HashMap<>();
    private static final ThreadLocal<Deque<DropContext>> DROP_CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    private static boolean registered;
    private static boolean loaded;
    private static boolean syncDirty;
    private static long tickCounter;
    private static long nextSyncTick;
    private static @Nullable MinecraftServer activeServer;

    private MobDropTrackerServer() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            load(server);
            syncDirty = true;
            nextSyncTick = 0L;
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            saveNow(server);
            activeServer = null;
            loaded = false;
            TRACKERS.clear();
            VIEWER_PREFERENCES.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(MobDropTrackerServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ensureLoaded(server);
            sendState(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VIEWER_PREFERENCES.remove(handler.player.getUuid()));
    }

    public static void beginDeathCapture(LivingEntity entity, DamageSource source) {
        if (entity == null || entity.getEntityWorld().isClient()) {
            return;
        }
        DROP_CONTEXT.get().push(new DropContext(entity));
    }

    public static void endDeathCapture() {
        Deque<DropContext> stack = DROP_CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            DROP_CONTEXT.remove();
        }
    }

    public static void captureDroppedStack(@Nullable ItemEntity itemEntity, ItemStack stack) {
        if (itemEntity == null || stack == null || stack.isEmpty()) {
            return;
        }

        Deque<DropContext> stackContext = DROP_CONTEXT.get();
        if (stackContext.isEmpty()) {
            return;
        }

        DropContext context = stackContext.peek();
        if (context == null || itemEntity.getEntityWorld().isClient()) {
            return;
        }

        recordDrop(context, itemEntity, stack);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("mobdrops")
                .executes(MobDropTrackerServer::showHelp)
                .then(argument("args", StringArgumentType.greedyString())
                        .suggests(MobDropTrackerServer::suggestArgs)
                        .executes(MobDropTrackerServer::handleArgs)));
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("[MobDrops] Commands").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/mobdrops list | status <name> | report <name|all>").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mobdrops add area <name> <x1 y1 z1> <x2 y2 z2> [anchorX anchorY anchorZ]").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mobdrops add block <name> [radius] [x y z]   (tracks mob drops around a block)").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mobdrops add container <name> [x y z]   (tracks throughput into a hopper/chest/container)").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mobdrops add hopper <name> [x y z] | /mobdrops add chest <name> [x y z]").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("/mobdrops remove <name> | reset <name|all> | overlay off|all|<name>").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int handleArgs(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ensureLoaded(source.getServer());

        String raw = StringArgumentType.getString(context, "args");
        String[] tokens = raw == null ? new String[0] : raw.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return showHelp(context);
        }

        try {
            String result = switch (tokens[0].toLowerCase(Locale.ROOT)) {
                case "list" -> listTrackers();
                case "status" -> {
                    requireLength(tokens, 2, "/mobdrops status <name>");
                    yield describeTracker(tokens[1], true);
                }
                case "report" -> {
                    requireLength(tokens, 2, "/mobdrops report <name|all>");
                    yield "all".equalsIgnoreCase(tokens[1]) ? describeAllTrackers() : describeTracker(tokens[1], false);
                }
                case "remove", "delete" -> {
                    requireLength(tokens, 2, "/mobdrops remove <name>");
                    yield removeTracker(tokens[1], source.getServer());
                }
                case "reset", "clear" -> {
                    requireLength(tokens, 2, "/mobdrops reset <name|all>");
                    yield resetTrackers(tokens[1], source.getServer());
                }
                case "overlay" -> {
                    requireLength(tokens, 2, "/mobdrops overlay off|all|<name>");
                    ServerPlayerEntity player = source.getPlayerOrThrow();
                    yield setOverlay(player, tokens[1]);
                }
                case "add" -> handleAdd(source, tokens);
                default -> {
                    showHelp(context);
                    yield null;
                }
            };

            if (result != null && !result.isBlank()) {
                source.sendFeedback(() -> Text.literal("[MobDrops] " + result).formatted(Formatting.GREEN), false);
            }
            return 1;
        } catch (IllegalStateException ex) {
            source.sendError(Text.literal("[MobDrops] " + ex.getMessage()));
            return 0;
        } catch (Exception ex) {
            source.sendError(Text.literal("[MobDrops] " + ex.getMessage()));
            return 0;
        }
    }

    private static String handleAdd(ServerCommandSource source, String[] tokens) throws Exception {
        requireLength(tokens, 3, "/mobdrops add <area|block|container> ...");
        String kind = tokens[1].toLowerCase(Locale.ROOT);
        String rawName = tokens[2];
        String key = normalizeName(rawName);
        if (key.isBlank()) {
            throw new IllegalStateException("Tracker name cannot be empty.");
        }
        if (TRACKERS.containsKey(key)) {
            throw new IllegalStateException("A tracker named '" + rawName + "' already exists.");
        }

        ServerPlayerEntity player = source.getPlayerOrThrow();
        TrackerData tracker = switch (kind) {
            case "block" -> buildBlockTracker(rawName, player, source, tokens);
            case "area" -> buildAreaTracker(rawName, source, tokens);
            case "container", "hopper", "chest" -> buildContainerTracker(rawName, player, source, tokens);
            default -> throw new IllegalStateException("Tracker kind must be 'area', 'block', or 'container'.");
        };

        TRACKERS.put(key, tracker);
        saveNow(source.getServer());
        sendStateToAll(source.getServer());
        return "Added " + tracker.displayKind() + " tracker '" + tracker.name + "'.";
    }

    private static TrackerData buildBlockTracker(String rawName,
                                                 ServerPlayerEntity player,
                                                 ServerCommandSource source,
                                                 String[] tokens) {
        int radius = DEFAULT_RADIUS;
        BlockPos anchor;

        if (tokens.length == 3) {
            anchor = resolveLookedOrFeetBlock(player, source);
        } else if (tokens.length == 4) {
            radius = parseRadius(tokens[3]);
            anchor = resolveLookedOrFeetBlock(player, source);
        } else if (tokens.length == 6) {
            anchor = parseBlockPos(source, tokens, 3);
        } else if (tokens.length == 7) {
            radius = parseRadius(tokens[3]);
            anchor = parseBlockPos(source, tokens, 4);
        } else {
            throw new IllegalStateException("Expected: /mobdrops add block <name> [radius] [x y z]");
        }

        BlockPos min = anchor.add(-radius, -radius, -radius);
        BlockPos max = anchor.add(radius, radius, radius);
        return new TrackerData(rawName.trim(), "block", worldId(player.getEntityWorld()), anchor, min, max);
    }

    private static TrackerData buildAreaTracker(String rawName, ServerCommandSource source, String[] tokens) {
        if (tokens.length != 9 && tokens.length != 12) {
            throw new IllegalStateException("Expected: /mobdrops add area <name> <x1 y1 z1> <x2 y2 z2> [anchorX anchorY anchorZ]");
        }

        BlockPos from = parseBlockPos(source, tokens, 3);
        BlockPos to = parseBlockPos(source, tokens, 6);
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );
        BlockPos anchor = tokens.length == 12
                ? parseBlockPos(source, tokens, 9)
                : new BlockPos((min.getX() + max.getX()) / 2, min.getY(), (min.getZ() + max.getZ()) / 2);

        return new TrackerData(rawName.trim(), "area", worldId(source.getWorld()), anchor, min, max);
    }

    private static TrackerData buildContainerTracker(String rawName,
                                                     ServerPlayerEntity player,
                                                     ServerCommandSource source,
                                                     String[] tokens) {
        BlockPos anchor;
        if (tokens.length == 3) {
            anchor = resolveLookedOrFeetBlock(player, source);
        } else if (tokens.length == 6) {
            anchor = parseBlockPos(source, tokens, 3);
        } else {
            throw new IllegalStateException("Expected: /mobdrops add container <name> [x y z]");
        }

        BlockEntity blockEntity = player.getEntityWorld().getBlockEntity(anchor);
        if (!(blockEntity instanceof Inventory inventory)) {
            throw new IllegalStateException("The selected block is not a container inventory.");
        }

        TrackerData tracker = new TrackerData(rawName.trim(), "container", worldId(player.getEntityWorld()), anchor, anchor, anchor);
        tracker.setContainerSnapshot(snapshotInventory(inventory));
        return tracker;
    }

    private static String listTrackers() {
        if (TRACKERS.isEmpty()) {
            return "No mob drop trackers are configured.";
        }

        List<String> parts = new ArrayList<>();
        for (TrackerData tracker : TRACKERS.values()) {
            parts.add(tracker.name + " [" + tracker.displayKind() + "] " + compactDimension(tracker.dimensionId)
                    + " items=" + tracker.totalItems
                    + " dpm=" + tracker.currentDropsPerMinute(tickCounter));
        }
        return String.join(" | ", parts);
    }

    private static String describeAllTrackers() {
        if (TRACKERS.isEmpty()) {
            return "No mob drop trackers are configured.";
        }

        List<String> parts = new ArrayList<>();
        for (TrackerData tracker : TRACKERS.values()) {
            parts.add(tracker.name + ": items=" + tracker.totalItems
                    + ", dpm=" + tracker.currentDropsPerMinute(tickCounter)
                    + (tracker.isContainerTracker() ? "" : ", kills=" + tracker.killCount)
                    + ", top=" + topSummary(tracker, 1));
        }
        return String.join(" | ", parts);
    }

    private static String describeTracker(String rawName, boolean includeBounds) {
        TrackerData tracker = requireTracker(rawName);
        StringBuilder builder = new StringBuilder()
                .append(tracker.name)
                .append(" [").append(tracker.displayKind()).append("] ")
                .append(compactDimension(tracker.dimensionId))
                .append(" items=").append(tracker.totalItems)
                .append(" dpm=").append(tracker.currentDropsPerMinute(tickCounter));

        if (tracker.isContainerTracker()) {
            builder.append(" updates=").append(tracker.stackCount);
        } else {
            builder.append(" kills=").append(tracker.killCount)
                    .append(" stacks=").append(tracker.stackCount);
        }

        if (!tracker.lastMobType.isBlank()) {
            builder.append(" last=").append(tracker.lastMobType);
        }

        if (includeBounds) {
            if (tracker.isContainerTracker()) {
                builder.append(" pos=").append(formatPos(tracker.anchor));
            } else {
                builder.append(" anchor=").append(formatPos(tracker.anchor));
                builder.append(" area=").append(formatPos(tracker.min)).append(" -> ").append(formatPos(tracker.max));
            }
        }

        String top = topSummary(tracker, 5);
        if (!top.isBlank()) {
            builder.append(" top=").append(top);
        }
        return builder.toString();
    }

    private static String removeTracker(String rawName, MinecraftServer server) {
        String key = normalizeName(rawName);
        TrackerData removed = TRACKERS.remove(key);
        if (removed == null) {
            throw new IllegalStateException("Unknown tracker '" + rawName + "'.");
        }
        for (OverlayPreference preference : VIEWER_PREFERENCES.values()) {
            preference.names.remove(key);
        }
        saveNow(server);
        sendStateToAll(server);
        return "Removed tracker '" + removed.name + "'.";
    }

    private static String resetTrackers(String rawName, MinecraftServer server) {
        if ("all".equalsIgnoreCase(rawName)) {
            for (TrackerData tracker : TRACKERS.values()) {
                tracker.resetCounts();
            }
            saveNow(server);
            sendStateToAll(server);
            return "Reset counts for all trackers.";
        }

        TrackerData tracker = requireTracker(rawName);
        tracker.resetCounts();
        saveNow(server);
        sendStateToAll(server);
        return "Reset counts for tracker '" + tracker.name + "'.";
    }

    private static String setOverlay(ServerPlayerEntity player, String token) {
        OverlayPreference preference = VIEWER_PREFERENCES.computeIfAbsent(player.getUuid(), ignored -> new OverlayPreference());
        String normalized = normalizeName(token);

        if ("off".equals(normalized)) {
            preference.showAll = false;
            preference.names.clear();
        } else if ("all".equals(normalized)) {
            preference.showAll = true;
            preference.names.clear();
        } else {
            TrackerData tracker = requireTracker(token);
            preference.showAll = false;
            preference.names.clear();
            preference.names.add(normalizeName(tracker.name));
        }

        sendState(player);
        return "Overlay set to " + (preference.showAll ? "all trackers" : preference.names.isEmpty() ? "off" : token) + ".";
    }

    private static TrackerData requireTracker(String rawName) {
        TrackerData tracker = TRACKERS.get(normalizeName(rawName));
        if (tracker == null) {
            throw new IllegalStateException("Unknown tracker '" + rawName + "'.");
        }
        return tracker;
    }

    private static void requireLength(String[] tokens, int length, String usage) {
        if (tokens.length < length) {
            throw new IllegalStateException("Expected: " + usage);
        }
    }

    private static int parseRadius(String token) {
        try {
            int radius = Integer.parseInt(token);
            return Math.clamp(radius, 0, MAX_RADIUS);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid radius: " + token);
        }
    }

    private static BlockPos parseBlockPos(ServerCommandSource source, String[] tokens, int start) {
        if (tokens.length <= start + 2) {
            throw new IllegalStateException("Expected three coordinates.");
        }
        Vec3d base = source.getPosition();
        return new BlockPos(
                parseBlockCoord(tokens[start], base.x),
                parseBlockCoord(tokens[start + 1], base.y),
                parseBlockCoord(tokens[start + 2], base.z)
        );
    }

    private static int parseBlockCoord(String token, double base) {
        try {
            if (token.startsWith("~")) {
                String remainder = token.substring(1);
                double offset = remainder.isBlank() ? 0.0 : Double.parseDouble(remainder);
                return MathHelper.floor(base + offset);
            }
            return MathHelper.floor(Double.parseDouble(token));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid coordinate: " + token);
        }
    }

    private static BlockPos resolveLookedOrFeetBlock(ServerPlayerEntity player, ServerCommandSource source) {
        HitResult hit = player.raycast(20.0, 1.0f, false);
        if (hit instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return player.getBlockPos();
    }

    private static void tick(MinecraftServer server) {
        if (!loaded) {
            return;
        }

        tickCounter++;
        pollContainerTrackers(server);

        if (syncDirty && tickCounter >= nextSyncTick) {
            syncDirty = false;
            nextSyncTick = tickCounter + 10L;
            saveNow(server);
            sendStateToAll(server);
            return;
        }

        if (!TRACKERS.isEmpty() && tickCounter % 20L == 0L) {
            sendStateToAll(server);
        }
    }

    private static void recordDrop(DropContext context, ItemEntity itemEntity, ItemStack stack) {
        ensureLoaded(activeServer);
        if (context.sourceEntity instanceof PlayerEntity) {
            return;
        }

        String dimensionId = worldId(itemEntity.getEntityWorld());
        BlockPos dropPos = itemEntity.getBlockPos();
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String mobType = compactId(Registries.ENTITY_TYPE.getId(context.sourceEntity.getType()).toString());

        boolean matched = false;
        for (TrackerData tracker : TRACKERS.values()) {
            if (tracker.isContainerTracker() || !tracker.dimensionId.equals(dimensionId) || !tracker.contains(dropPos)) {
                continue;
            }
            tracker.recordDropObservation(itemId, stack.getCount(), mobType, tickCounter);
            if (context.killCountedTrackers.add(normalizeName(tracker.name))) {
                tracker.killCount += 1;
            }
            matched = true;
        }

        if (matched) {
            scheduleDirtySync(5L);
        }
    }

    private static void pollContainerTrackers(MinecraftServer server) {
        for (TrackerData tracker : TRACKERS.values()) {
            tracker.trimRateWindow(tickCounter);
            if (!tracker.isContainerTracker()) {
                continue;
            }

            ServerWorld world = resolveWorld(server, tracker.dimensionId);
            if (world == null) {
                continue;
            }

            int chunkX = tracker.anchor.getX() >> 4;
            int chunkZ = tracker.anchor.getZ() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            BlockEntity blockEntity = world.getBlockEntity(tracker.anchor);
            if (!(blockEntity instanceof Inventory inventory)) {
                tracker.clearContainerSnapshot();
                continue;
            }

            Map<String, Long> snapshot = snapshotInventory(inventory);
            if (!tracker.hasContainerSnapshot()) {
                tracker.setContainerSnapshot(snapshot);
                continue;
            }

            Map<String, Long> delta = computePositiveDelta(tracker.containerSnapshot, snapshot);
            tracker.setContainerSnapshot(snapshot);

            long observedItems = delta.values().stream().mapToLong(Long::longValue).sum();
            if (observedItems <= 0L) {
                continue;
            }

            tracker.recordContainerObservation(delta, observedItems, tickCounter);
            scheduleDirtySync(5L);
        }
    }

    private static void scheduleDirtySync(long delayTicks) {
        syncDirty = true;
        long scheduledTick = tickCounter + Math.max(1L, delayTicks);
        nextSyncTick = nextSyncTick == 0L ? scheduledTick : Math.min(nextSyncTick, scheduledTick);
    }

    private static Map<String, Long> snapshotInventory(Inventory inventory) {
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            counts.merge(itemId, (long) stack.getCount(), Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> computePositiveDelta(Map<String, Long> previous, Map<String, Long> current) {
        LinkedHashMap<String, Long> delta = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            long before = previous.getOrDefault(entry.getKey(), 0L);
            long after = entry.getValue();
            if (after > before) {
                delta.put(entry.getKey(), after - before);
            }
        }
        return delta;
    }

    private static void ensureLoaded(@Nullable MinecraftServer server) {
        if (loaded || server == null) {
            return;
        }
        load(server);
    }

    private static void load(MinecraftServer server) {
        TRACKERS.clear();
        VIEWER_PREFERENCES.clear();
        loaded = true;

        Path path = savePath(server);
        try {
            if (!Files.exists(path)) {
                return;
            }

            JsonObject root = GSON.fromJson(Files.readString(path), JsonObject.class);
            if (root == null || !root.has("trackers") || !root.get("trackers").isJsonArray()) {
                return;
            }

            for (JsonElement element : root.getAsJsonArray("trackers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                TrackerData tracker = readTracker(element.getAsJsonObject());
                if (tracker != null) {
                    TRACKERS.put(normalizeName(tracker.name), tracker);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveNow(MinecraftServer server) {
        if (!loaded || server == null) {
            return;
        }

        JsonArray trackers = new JsonArray();
        for (TrackerData tracker : TRACKERS.values()) {
            trackers.add(writeTracker(tracker));
        }

        JsonObject root = new JsonObject();
        root.add("trackers", trackers);

        try {
            Path path = savePath(server);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (Exception ignored) {
        }
    }

    private static Path savePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(SAVE_FILE_NAME);
    }

    private static JsonObject writeTracker(TrackerData tracker) {
        JsonObject json = new JsonObject();
        json.addProperty("name", tracker.name);
        json.addProperty("kind", tracker.kind);
        json.addProperty("dimension", tracker.dimensionId);
        json.add("anchor", writePos(tracker.anchor));
        json.add("min", writePos(tracker.min));
        json.add("max", writePos(tracker.max));
        json.addProperty("totalItems", tracker.totalItems);
        json.addProperty("stackCount", tracker.stackCount);
        json.addProperty("killCount", tracker.killCount);
        json.addProperty("lastMobType", tracker.lastMobType);
        json.addProperty("lastUpdatedTick", tracker.lastUpdatedTick);

        JsonArray items = new JsonArray();
        for (Map.Entry<String, Long> entry : tracker.sortedItems()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", entry.getKey());
            item.addProperty("count", entry.getValue());
            items.add(item);
        }
        json.add("items", items);
        return json;
    }

    private static @Nullable TrackerData readTracker(JsonObject json) {
        try {
            String kind = normalizeLoadedKind(json.get("kind").getAsString());
            BlockPos anchor = readPos(json.getAsJsonObject("anchor"));
            BlockPos min = kind.equals("container") ? anchor : readPos(json.getAsJsonObject("min"));
            BlockPos max = kind.equals("container") ? anchor : readPos(json.getAsJsonObject("max"));

            TrackerData tracker = new TrackerData(
                    json.get("name").getAsString(),
                    kind,
                    json.get("dimension").getAsString(),
                    anchor,
                    min,
                    max
            );
            tracker.totalItems = json.has("totalItems") ? json.get("totalItems").getAsLong() : 0L;
            tracker.stackCount = json.has("stackCount") ? json.get("stackCount").getAsLong() : 0L;
            tracker.killCount = json.has("killCount") ? json.get("killCount").getAsLong() : 0L;
            tracker.lastMobType = json.has("lastMobType") ? json.get("lastMobType").getAsString() : "";
            tracker.lastUpdatedTick = json.has("lastUpdatedTick") ? json.get("lastUpdatedTick").getAsLong() : 0L;
            if (json.has("items") && json.get("items").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("items")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = element.getAsJsonObject();
                    tracker.itemCounts.put(item.get("id").getAsString(), item.get("count").getAsLong());
                }
            }
            return tracker;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject writePos(BlockPos pos) {
        JsonObject json = new JsonObject();
        json.addProperty("x", pos.getX());
        json.addProperty("y", pos.getY());
        json.addProperty("z", pos.getZ());
        return json;
    }

    private static BlockPos readPos(JsonObject json) {
        return new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
    }

    private static void sendStateToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendState(player);
        }
    }

    private static void sendState(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, MobDropTrackerPayloads.StatePayload.ID)) {
            return;
        }

        OverlayPreference preference = VIEWER_PREFERENCES.computeIfAbsent(player.getUuid(), ignored -> new OverlayPreference());
        ServerPlayNetworking.send(player, new MobDropTrackerPayloads.StatePayload(
                TRACKERS.values().stream().map(tracker -> tracker.toPayload(tickCounter)).toList(),
                preference.modeToken(),
                List.copyOf(preference.names)
        ));
    }

    private static CompletableFuture<Suggestions> suggestArgs(CommandContext<ServerCommandSource> context,
                                                              SuggestionsBuilder builder) {
        ensureLoaded(context.getSource().getServer());

        String remaining = builder.getRemaining();
        String trimmed = remaining.trim();
        boolean trailingSpace = remaining.endsWith(" ");
        String[] rawTokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        int index = trailingSpace ? rawTokens.length : Math.max(0, rawTokens.length - 1);

        if (rawTokens.length == 0 || (rawTokens.length == 1 && !trailingSpace)) {
            String prefix = rawTokens.length == 0 ? "" : rawTokens[0].toLowerCase(Locale.ROOT);
            for (String sub : List.of("list", "add", "status", "report", "remove", "reset", "overlay")) {
                if (sub.startsWith(prefix)) {
                    builder.suggest(sub);
                }
            }
            return builder.buildFuture();
        }

        String sub = rawTokens[0].toLowerCase(Locale.ROOT);
        if ("add".equals(sub)) {
            if (index <= 1) {
                builder.suggest("add area farm ~ ~ ~ ~ ~ ~");
                builder.suggest("add block farm 8");
                builder.suggest("add container output");
                builder.suggest("add hopper output");
                builder.suggest("add chest output");
            }
            return builder.buildFuture();
        }

        if ("overlay".equals(sub)) {
            builder.suggest("overlay off");
            builder.suggest("overlay all");
            for (String name : TRACKERS.values().stream().map(tracker -> tracker.name).sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
                builder.suggest("overlay " + name);
            }
            return builder.buildFuture();
        }

        if ("reset".equals(sub)) {
            builder.suggest("reset all");
        }

        if (List.of("status", "report", "remove", "reset").contains(sub)) {
            for (String name : TRACKERS.values().stream().map(tracker -> tracker.name).sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
                builder.suggest(sub + " " + name);
            }
        }

        return builder.buildFuture();
    }

    private static String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeLoadedKind(String rawKind) {
        String normalized = normalizeName(rawKind);
        return switch (normalized) {
            case "chest", "hopper", "container" -> "container";
            case "area" -> "area";
            default -> "block";
        };
    }

    private static String worldId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static @Nullable ServerWorld resolveWorld(MinecraftServer server, String dimensionId) {
        try {
            RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimensionId));
            return server.getWorld(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String compactDimension(String dimensionId) {
        return compactId(dimensionId);
    }

    private static String compactId(String id) {
        return id != null && id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String topSummary(TrackerData tracker, int limit) {
        if (tracker.itemCounts.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Long> entry : tracker.sortedItems()) {
            parts.add(compactId(entry.getKey()) + " x" + entry.getValue());
            count++;
            if (count >= limit) {
                break;
            }
        }
        return String.join(", ", parts);
    }

    private static final class TrackerData {
        private final String name;
        private final String kind;
        private final String dimensionId;
        private final BlockPos anchor;
        private final BlockPos min;
        private final BlockPos max;
        private final LinkedHashMap<String, Long> itemCounts = new LinkedHashMap<>();
        private final Deque<RateSample> recentItemEvents = new ArrayDeque<>();
        private final LinkedHashMap<String, Long> containerSnapshot = new LinkedHashMap<>();
        private long totalItems;
        private long stackCount;
        private long killCount;
        private long lastUpdatedTick;
        private long recentItemCount;
        private boolean hasContainerSnapshot;
        private String lastMobType = "";

        private TrackerData(String name, String kind, String dimensionId, BlockPos anchor, BlockPos min, BlockPos max) {
            this.name = name;
            this.kind = kind;
            this.dimensionId = dimensionId;
            this.anchor = anchor;
            this.min = min;
            this.max = max;
        }

        private boolean contains(BlockPos pos) {
            return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX()
                    && pos.getY() >= this.min.getY() && pos.getY() <= this.max.getY()
                    && pos.getZ() >= this.min.getZ() && pos.getZ() <= this.max.getZ();
        }

        private boolean isContainerTracker() {
            return "container".equals(this.kind);
        }

        private String displayKind() {
            return switch (this.kind) {
                case "block" -> "block-area";
                case "container" -> "container";
                default -> "area";
            };
        }

        private void recordDropObservation(String itemId, int itemCount, String mobType, long currentTick) {
            this.totalItems += itemCount;
            this.stackCount += 1;
            this.lastMobType = mobType;
            this.lastUpdatedTick = currentTick;
            this.itemCounts.merge(itemId, (long) itemCount, Long::sum);
            recordRate(currentTick, itemCount);
        }

        private void recordContainerObservation(Map<String, Long> deltaCounts, long observedItems, long currentTick) {
            this.totalItems += observedItems;
            this.stackCount += deltaCounts.size();
            this.lastUpdatedTick = currentTick;
            for (Map.Entry<String, Long> entry : deltaCounts.entrySet()) {
                this.itemCounts.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
            recordRate(currentTick, observedItems);
        }

        private void recordRate(long currentTick, long items) {
            if (items <= 0L) {
                trimRateWindow(currentTick);
                return;
            }
            this.recentItemEvents.addLast(new RateSample(currentTick, items));
            this.recentItemCount += items;
            trimRateWindow(currentTick);
        }

        private void trimRateWindow(long currentTick) {
            long threshold = currentTick - RATE_WINDOW_TICKS;
            while (!this.recentItemEvents.isEmpty() && this.recentItemEvents.peekFirst().tick <= threshold) {
                RateSample sample = this.recentItemEvents.removeFirst();
                this.recentItemCount -= sample.items;
            }
            if (this.recentItemCount < 0L) {
                this.recentItemCount = 0L;
            }
        }

        private long currentDropsPerMinute(long currentTick) {
            trimRateWindow(currentTick);
            return this.recentItemCount;
        }

        private void resetCounts() {
            this.itemCounts.clear();
            this.totalItems = 0L;
            this.stackCount = 0L;
            this.killCount = 0L;
            this.lastUpdatedTick = 0L;
            this.lastMobType = "";
            this.recentItemCount = 0L;
            this.recentItemEvents.clear();
        }

        private List<Map.Entry<String, Long>> sortedItems() {
            return this.itemCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .toList();
        }

        private boolean hasContainerSnapshot() {
            return this.hasContainerSnapshot;
        }

        private void setContainerSnapshot(Map<String, Long> snapshot) {
            this.containerSnapshot.clear();
            this.containerSnapshot.putAll(snapshot);
            this.hasContainerSnapshot = true;
        }

        private void clearContainerSnapshot() {
            this.containerSnapshot.clear();
            this.hasContainerSnapshot = false;
        }

        private MobDropTrackerPayloads.TrackerPayload toPayload(long currentTick) {
            List<MobDropTrackerPayloads.ItemCountEntry> items = this.sortedItems().stream()
                    .map(entry -> new MobDropTrackerPayloads.ItemCountEntry(entry.getKey(), entry.getValue()))
                    .toList();
            return new MobDropTrackerPayloads.TrackerPayload(
                    this.name,
                    this.displayKind(),
                    this.dimensionId,
                    this.anchor,
                    this.min,
                    this.max,
                    this.totalItems,
                    this.stackCount,
                    this.killCount,
                    this.currentDropsPerMinute(currentTick),
                    this.lastMobType,
                    items
            );
        }
    }

    private static final class OverlayPreference {
        private boolean showAll;
        private final LinkedHashSet<String> names = new LinkedHashSet<>();

        private String modeToken() {
            return this.showAll ? "all" : this.names.isEmpty() ? "off" : "selected";
        }
    }

    private record DropContext(LivingEntity sourceEntity, Set<String> killCountedTrackers) {
        private DropContext(LivingEntity sourceEntity) {
            this(sourceEntity, new LinkedHashSet<>());
        }
    }

    private record RateSample(long tick, long items) {
    }
}
