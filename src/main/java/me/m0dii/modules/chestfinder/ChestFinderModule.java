package me.m0dii.modules.chestfinder;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.m0dii.modules.Module;
import me.m0dii.modules.highlights.BlockHighlights;
import me.m0dii.modules.nbttooltip.ShulkerTooltipModule;
import me.m0dii.modules.utilitycommands.ScanPointer;
import me.m0dii.modules.waypoints.WaypointHandler;
import me.m0dii.utils.ConfigFileSafety;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chest finder: remembers what was in every container you open (per world and dimension, saved to disk),
 * so {@code /finditem <item>} or the search screen can tell you where you put things and highlight them.
 * Items inside shulker boxes count too.
 */
public class ChestFinderModule extends Module {
    public static final ChestFinderModule INSTANCE = new ChestFinderModule();
    private static final Path DIR = Paths.get("config/m0-dev-tools/chest-memory");
    private static final String HIGHLIGHT_KEY = "chest_finder";
    private static final int GOLD = 0xFFC040;

    private ContainerMemory memory = new ContainerMemory();
    private String loadedWorld;
    private boolean dirty;
    private long lastSaveMs;
    private int pruneTicks;

    /** The container screen being watched, and where its container is. */
    private Screen watchedScreen;
    private BlockPos watchedPos;
    private ContainerMemory.Entry watchedEntry;

    private ChestFinderModule() {
        super("chest_finder", "Chest Finder", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.CHEST_FINDER.translationKey(), InputConstants.Type.KEYBOARD,
                KeybindCatalog.CHEST_FINDER.defaultKey(), client -> openScreen(client, ""));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            save(true);
            loadedWorld = null;
            memory = new ContainerMemory();
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("finditem")
                        .executes(ctx -> reply(ctx.getSource(), stats()))
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            BlockHighlights.clear(HIGHLIGHT_KEY);
                            return reply(ctx.getSource(), "Highlights cleared.");
                        }))
                        .then(ClientCommands.literal("gui").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> openScreen(client, ""));
                            return 1;
                        }))
                        .then(ClientCommands.literal("forget").executes(ctx -> {
                            int count = memory.size();
                            memory.clear();
                            dirty = true;
                            save(true);
                            BlockHighlights.clear(HIGHLIGHT_KEY);
                            return reply(ctx.getSource(), "Forgot " + count + " containers in this world.");
                        }))
                        .then(ClientCommands.argument("item", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
                                    for (String name : memory.knownItems(dimension())) {
                                        if (name.contains(typed)) {
                                            builder.suggest(name);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    for (String line : find(StringArgumentType.getString(ctx, "item"))) {
                                        ctx.getSource().sendFeedback(Component.literal("[Find] " + line));
                                    }
                                    return 1;
                                }))));
    }

    private static int reply(FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Find] " + text));
        return 1;
    }

    public ContainerMemory memory() {
        ensureLoaded();
        return memory;
    }

    static String dimension() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? "" : level.dimension().identifier().toString();
    }

    private String stats() {
        ensureLoaded();
        int here = memory.entries(dimension()).size();
        return "Remembering " + memory.size() + " containers in this world (" + here + " in this dimension)."
                + (isEnabled() ? "" : " Recording is off.") + " Use /finditem <item>, /finditem gui, /finditem clear, /finditem forget.";
    }

    /** Searches the current dimension, highlights the matches and returns chat lines. */
    public List<String> find(String query) {
        ensureLoaded();
        List<String> lines = new ArrayList<>();
        List<ContainerMemory.Match> matches = memory.search(query, dimension());
        Minecraft client = Minecraft.getInstance();
        if (matches.isEmpty()) {
            lines.add("No remembered container has '" + query + "'. Open containers once so they are remembered.");
            BlockHighlights.clear(HIGHLIGHT_KEY);
            return lines;
        }
        int total = matches.stream().mapToInt(ContainerMemory.Match::count).sum();
        lines.add(total + "× in " + matches.size() + " container" + (matches.size() == 1 ? "" : "s") + ":");
        List<BlockPos> positions = new ArrayList<>();
        for (ContainerMemory.Match match : matches) {
            BlockPos pos = BlockPos.of(match.entry().pos);
            positions.add(pos);
            BlockPos partner = partner(client.level, pos);
            if (partner != null) {
                positions.add(partner);
            }
            if (lines.size() <= 8) {
                String where = pos.getX() + " " + pos.getY() + " " + pos.getZ();
                String distance = client.player == null ? "" : " · " + ScanPointer.distance(
                        pos.getX() + 0.5 - client.player.getX(), pos.getY() + 0.5 - client.player.getEyeY(),
                        pos.getZ() + 0.5 - client.player.getZ());
                lines.add("  " + match.count() + "× " + match.entry().title + " at " + where + distance + " · " + age(match.entry().updatedMs));
            }
        }
        if (matches.size() > 8) {
            lines.add("  …and " + (matches.size() - 8) + " more (all highlighted).");
        }
        BlockHighlights.show(HIGHLIGHT_KEY, "\"" + query + "\"", GOLD, positions, null, 5 * 60_000L);
        return lines;
    }

    /** Highlights a single remembered container (and its double-chest partner) so you can walk to it. */
    public void highlightContainer(long packedPos) {
        BlockPos pos = BlockPos.of(packedPos);
        List<BlockPos> positions = new ArrayList<>();
        positions.add(pos);
        BlockPos partner = partner(Minecraft.getInstance().level, pos);
        if (partner != null) {
            positions.add(partner);
        }
        BlockHighlights.show(HIGHLIGHT_KEY, "container", GOLD, positions, null, 5 * 60_000L);
    }

    private static String age(long timeMs) {
        long minutes = Math.max(0, (System.currentTimeMillis() - timeMs) / 60_000L);
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        return hours < 48 ? hours + " h ago" : (hours / 24) + " days ago";
    }

    public static void openScreen(Minecraft client, String query) {
        client.gui.setScreen(new ChestFinderScreen(client.gui.screen(), query));
    }

    // ---- recording ----

    private void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }
        ensureLoaded();
        Screen screen = client.gui.screen();
        if (screen != watchedScreen) {
            finishWatching();
            if (isEnabled() && screen instanceof AbstractContainerScreen<?> containerScreen) {
                startWatching(client, containerScreen);
            }
        }
        if (watchedEntry != null && screen instanceof AbstractContainerScreen<?> containerScreen) {
            snapshot(client, containerScreen);
        }
        if (dirty && System.currentTimeMillis() - lastSaveMs > 5000) {
            save(false);
        }
        if (++pruneTicks >= 100) {
            pruneTicks = 0;
            prune(client.level);
        }
    }

    private void startWatching(Minecraft client, AbstractContainerScreen<?> screen) {
        watchedScreen = screen;
        HitResult hit = client.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        if (!(client.level.getBlockEntity(pos) instanceof Container)) {
            return; // crafting tables, ender chests, entity inventories...
        }
        BlockPos partner = partner(client.level, pos);
        if (partner != null && partner.asLong() < pos.asLong()) {
            pos = partner; // one key per double chest
        }
        watchedPos = pos;
        watchedEntry = new ContainerMemory.Entry(dimension(), pos.asLong(), screen.getTitle().getString(), System.currentTimeMillis());
    }

    private void snapshot(Minecraft client, AbstractContainerScreen<?> screen) {
        Inventory inventory = client.player.getInventory();
        ContainerMemory.Entry entry = new ContainerMemory.Entry(watchedEntry.dimension, watchedEntry.pos,
                watchedEntry.title, System.currentTimeMillis());
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.container == inventory || !slot.hasItem()) {
                continue;
            }
            addStack(entry, slot.getItem());
            for (ItemStack inner : ShulkerTooltipModule.readContainerStacks(slot.getItem())) {
                addStack(entry, inner);
            }
        }
        watchedEntry = entry;
    }

    private static void addStack(ContainerMemory.Entry entry, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        entry.add(id, stack.getHoverName().getString(), stack.getCount());
    }

    private void finishWatching() {
        if (watchedEntry != null && watchedPos != null) {
            memory.put(watchedEntry);
            dirty = true;
        }
        watchedScreen = null;
        watchedPos = null;
        watchedEntry = null;
    }

    /** Forgets containers that are gone (block broken or replaced) in loaded chunks. */
    private void prune(ClientLevel level) {
        for (ContainerMemory.Entry entry : memory.entries(dimension())) {
            BlockPos pos = BlockPos.of(entry.pos);
            if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && !(level.getBlockEntity(pos) instanceof Container)) {
                memory.remove(entry.dimension, entry.pos);
                dirty = true;
            }
        }
    }

    /** The other half of a double chest, or null. */
    static BlockPos partner(ClientLevel level, BlockPos pos) {
        if (level == null) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state));
        }
        return null;
    }

    // ---- persistence ----

    private void ensureLoaded() {
        String world = WaypointHandler.currentWorldKey();
        if (world == null || world.equals(loadedWorld)) {
            return;
        }
        save(true);
        loadedWorld = world;
        memory = new ContainerMemory();
        Path file = file(world);
        try {
            if (Files.exists(file)) {
                memory = ContainerMemory.fromJson(Files.readString(file));
            }
        } catch (IOException | RuntimeException e) {
            memory = new ContainerMemory();
        }
    }

    private void save(boolean force) {
        if (!dirty || loadedWorld == null || (!force && System.currentTimeMillis() - lastSaveMs < 5000)) {
            return;
        }
        try {
            Files.createDirectories(DIR);
            ConfigFileSafety.writeAtomically(file(loadedWorld), memory.toJson());
            dirty = false;
            lastSaveMs = System.currentTimeMillis();
        } catch (IOException | RuntimeException ignored) {
            // try again later
        }
    }

    private static Path file(String world) {
        return DIR.resolve(world.replaceAll("[^a-zA-Z0-9._-]", "_") + ".json");
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Recording: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Open search");
        settings.add("Remembered: " + memory().size());
        settings.add("Clear highlights");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> openScreen(Minecraft.getInstance(), "");
            case 3 -> BlockHighlights.clear(HIGHLIGHT_KEY);
            default -> {
                // info row
            }
        }
    }
}
