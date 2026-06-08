package me.m0dii.modules.automation;

import com.mojang.brigadier.CommandDispatcher;
import me.m0dii.M0DevTools;
import me.m0dii.modules.clickgui.ModuleRegistry;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.gui.MacroWorkbenchScreen;
import me.m0dii.modules.scripting.ScriptManager;
import me.m0dii.modules.scripting.ScriptStorage;
import me.m0dii.modules.scripting.ScriptTypes;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AutomationModule {
    public static final AutomationModule INSTANCE = new AutomationModule();

    private final EventBus eventBus = EventBus.getInstance();
    private final AutomationEngine engine = new AutomationEngine(eventBus, new ActionExecutor(new ClientBridge()), new ClientContextSnapshotProvider());

    private long clientTickCounter = 0L;
    private boolean wasWorldLoaded = false;
    private @Nullable String lastDimensionId = null;
    private @Nullable Vec3d lastPlayerPos = null;
    private @Nullable String lastScreenName = null;
    private int lastHotbarSlot = -1;
    private @Nullable String lastHeldItemId = null;
    private @Nullable String lastHeldItemName = null;
    private float lastHealth = Float.NaN;
    private int lastFood = Integer.MIN_VALUE;
    private @Nullable String lastChatMessage = null;
    private long lastChatAtMs = 0L;

    private AutomationModule() {
    }

    public void register() {
        try {
            ScriptStorage.ensureAutomationExamples();
        } catch (Exception e) {
            M0DevTools.LOGGER.warn("Failed to prepare automation example scripts: {}", e.getMessage());
        }
        engine.start();
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public AutomationEngine engine() {
        return engine;
    }

    public void openScreen(@Nullable Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(MacroWorkbenchScreen.create(parent, MacroWorkbenchScreen.Tab.AUTOMATION));
    }

    public void onChatMessage(@Nullable Text message) {
        if (message == null) {
            return;
        }

        String rendered = message.getString();
        long nowMs = System.currentTimeMillis();
        if (Objects.equals(rendered, lastChatMessage) && nowMs - lastChatAtMs < 50L) {
            return;
        }

        lastChatMessage = rendered;
        lastChatAtMs = nowMs;

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("message", rendered);
        attributes.put("messageLength", rendered.length());
        eventBus.post(new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, nowMs, clientTickCounter, attributes));
    }

    public void fireTickTest() {
        long nowMs = System.currentTimeMillis();
        Map<String, Object> attributes = buildTickAttributes(MinecraftClient.getInstance());
        eventBus.post(new AutomationEvent(AutomationEventType.TICK_INTERVAL, nowMs, ++clientTickCounter, attributes));
    }

    public void firePlayerMoveTest() {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d from = client.player == null ? new Vec3d(0.0, 0.0, 0.0) : currentPosition(client.player);
        Vec3d to = from.add(1.0, 0.0, 0.0);
        postPlayerMoveEvent(from, to);
    }

    public void fireWorldJoinTest() {
        MinecraftClient client = MinecraftClient.getInstance();
        String dimension = currentDimensionId(client.world);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("dimension", dimension);
        attributes.put("playerName", client.player == null ? "" : client.player.getName().getString());
        eventBus.post(new AutomationEvent(AutomationEventType.WORLD_JOIN, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void fireDimensionChangeTest(@NotNull String toDimension) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromDimension", currentDimensionId(client.world));
        attributes.put("toDimension", toDimension);
        eventBus.post(new AutomationEvent(AutomationEventType.DIMENSION_CHANGE, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void fireChatTest(@NotNull String message) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("message", message);
        attributes.put("messageLength", message.length());
        eventBus.post(new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void fireScreenChangeTest(@NotNull String toScreen) {
        String fromScreen = currentScreenName(MinecraftClient.getInstance().currentScreen);
        eventBus.post(new AutomationEvent(
                AutomationEventType.SCREEN_CHANGED,
                System.currentTimeMillis(),
                clientTickCounter,
                screenChangeAttributes(fromScreen, toScreen)
        ));
    }

    public void fireHotbarSlotChangeTest(int toSlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        int fromSlot = client.player == null ? 0 : client.player.getInventory().getSelectedSlot();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromSlot", fromSlot);
        attributes.put("toSlot", Math.clamp(toSlot, 0, 8));
        eventBus.post(new AutomationEvent(AutomationEventType.HOTBAR_SLOT_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void fireHeldItemChangeTest(@NotNull String toItemId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack stack = client.player == null ? ItemStack.EMPTY : client.player.getMainHandStack();
        String fromItemId = itemId(stack);
        String fromItemName = itemName(stack);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromItemId", fromItemId);
        attributes.put("toItemId", toItemId);
        attributes.put("fromItemName", fromItemName);
        attributes.put("toItemName", displayNameFromItemId(toItemId));
        eventBus.post(new AutomationEvent(AutomationEventType.HELD_ITEM_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void firePlayerHealthChangeTest(float fromHealth, float toHealth) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromHealth", fromHealth);
        attributes.put("toHealth", toHealth);
        attributes.put("deltaHealth", toHealth - fromHealth);
        eventBus.post(new AutomationEvent(AutomationEventType.PLAYER_HEALTH_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    public void firePlayerFoodChangeTest(int fromFood, int toFood) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromFood", fromFood);
        attributes.put("toFood", toFood);
        attributes.put("deltaFood", toFood - fromFood);
        eventBus.post(new AutomationEvent(AutomationEventType.PLAYER_FOOD_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess) {
        AutomationCommands.register(dispatcher, this);
    }

    private void onClientTick(MinecraftClient client) {
        clientTickCounter++;
        long nowMs = System.currentTimeMillis();
        eventBus.post(new AutomationEvent(AutomationEventType.TICK_INTERVAL, nowMs, clientTickCounter, buildTickAttributes(client)));
        detectScreenChange(client, nowMs);

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            resetPlayerTracking();
            return;
        }

        String currentDimension = currentDimensionId(world);
        if (!wasWorldLoaded) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("dimension", currentDimension);
            attributes.put("playerName", player.getName().getString());
            eventBus.post(new AutomationEvent(AutomationEventType.WORLD_JOIN, nowMs, clientTickCounter, attributes));
        } else if (lastDimensionId != null && !lastDimensionId.equals(currentDimension)) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("fromDimension", lastDimensionId);
            attributes.put("toDimension", currentDimension);
            eventBus.post(new AutomationEvent(AutomationEventType.DIMENSION_CHANGE, nowMs, clientTickCounter, attributes));
        }

        Vec3d currentPos = currentPosition(player);
        if (lastPlayerPos != null && currentPos.squaredDistanceTo(lastPlayerPos) > 0.000001D) {
            postPlayerMoveEvent(lastPlayerPos, currentPos);
        }

        int currentHotbarSlot = player.getInventory().getSelectedSlot();
        if (lastHotbarSlot >= 0 && currentHotbarSlot != lastHotbarSlot) {
            postHotbarSlotChangedEvent(lastHotbarSlot, currentHotbarSlot);
        }

        ItemStack mainHand = player.getMainHandStack();
        String currentItemId = itemId(mainHand);
        String currentItemName = itemName(mainHand);
        if (lastHeldItemId != null && (!lastHeldItemId.equals(currentItemId) || !Objects.equals(lastHeldItemName, currentItemName))) {
            postHeldItemChangedEvent(lastHeldItemId, currentItemId, safe(lastHeldItemName), currentItemName);
        }

        float currentHealth = player.getHealth();
        if (!Float.isNaN(lastHealth) && Math.abs(currentHealth - lastHealth) > 0.0001F) {
            postPlayerHealthChangedEvent(lastHealth, currentHealth);
        }

        int currentFood = player.getHungerManager().getFoodLevel();
        if (lastFood != Integer.MIN_VALUE && currentFood != lastFood) {
            postPlayerFoodChangedEvent(lastFood, currentFood);
        }

        wasWorldLoaded = true;
        lastDimensionId = currentDimension;
        lastPlayerPos = currentPos;
        lastHotbarSlot = currentHotbarSlot;
        lastHeldItemId = currentItemId;
        lastHeldItemName = currentItemName;
        lastHealth = currentHealth;
        lastFood = currentFood;
    }

    private void postPlayerMoveEvent(@NotNull Vec3d from, @NotNull Vec3d to) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromX", from.x);
        attributes.put("fromY", from.y);
        attributes.put("fromZ", from.z);
        attributes.put("toX", to.x);
        attributes.put("toY", to.y);
        attributes.put("toZ", to.z);
        attributes.put("deltaX", to.x - from.x);
        attributes.put("deltaY", to.y - from.y);
        attributes.put("deltaZ", to.z - from.z);
        attributes.put("distance", to.distanceTo(from));
        eventBus.post(new AutomationEvent(AutomationEventType.PLAYER_MOVE, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private void detectScreenChange(@NotNull MinecraftClient client, long nowMs) {
        String currentScreen = currentScreenName(client.currentScreen);
        if (lastScreenName == null) {
            lastScreenName = currentScreen;
            return;
        }
        if (Objects.equals(lastScreenName, currentScreen)) {
            return;
        }
        eventBus.post(new AutomationEvent(
                AutomationEventType.SCREEN_CHANGED,
                nowMs,
                clientTickCounter,
                screenChangeAttributes(lastScreenName, currentScreen)
        ));
        lastScreenName = currentScreen;
    }

    private void postHotbarSlotChangedEvent(int fromSlot, int toSlot) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromSlot", fromSlot);
        attributes.put("toSlot", toSlot);
        eventBus.post(new AutomationEvent(AutomationEventType.HOTBAR_SLOT_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private void postHeldItemChangedEvent(@NotNull String fromItemId,
                                          @NotNull String toItemId,
                                          @NotNull String fromItemName,
                                          @NotNull String toItemName) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromItemId", fromItemId);
        attributes.put("toItemId", toItemId);
        attributes.put("fromItemName", fromItemName);
        attributes.put("toItemName", toItemName);
        eventBus.post(new AutomationEvent(AutomationEventType.HELD_ITEM_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private void postPlayerHealthChangedEvent(float fromHealth, float toHealth) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromHealth", fromHealth);
        attributes.put("toHealth", toHealth);
        attributes.put("deltaHealth", toHealth - fromHealth);
        eventBus.post(new AutomationEvent(AutomationEventType.PLAYER_HEALTH_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private void postPlayerFoodChangedEvent(int fromFood, int toFood) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromFood", fromFood);
        attributes.put("toFood", toFood);
        attributes.put("deltaFood", toFood - fromFood);
        eventBus.post(new AutomationEvent(AutomationEventType.PLAYER_FOOD_CHANGED, System.currentTimeMillis(), clientTickCounter, attributes));
    }

    private Map<String, Object> buildTickAttributes(MinecraftClient client) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("tick", clientTickCounter);
        attributes.put("worldLoaded", client.world != null);
        attributes.put("playerLoaded", client.player != null);
        attributes.put("screen", currentScreenName(client.currentScreen));
        attributes.put("screenPresent", client.currentScreen != null);
        if (client.world != null) {
            attributes.put("dimension", currentDimensionId(client.world));
            attributes.put("worldTime", client.world.getTime());
        }
        return attributes;
    }

    private void resetPlayerTracking() {
        wasWorldLoaded = false;
        lastDimensionId = null;
        lastPlayerPos = null;
        lastHotbarSlot = -1;
        lastHeldItemId = null;
        lastHeldItemName = null;
        lastHealth = Float.NaN;
        lastFood = Integer.MIN_VALUE;
    }

    private static Map<String, Object> screenChangeAttributes(@NotNull String fromScreen, @NotNull String toScreen) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fromScreen", fromScreen);
        attributes.put("toScreen", toScreen);
        attributes.put("fromScreenPresent", !fromScreen.isBlank());
        attributes.put("toScreenPresent", !toScreen.isBlank());
        return attributes;
    }

    private static String currentDimensionId(@Nullable ClientWorld world) {
        return world == null ? "" : world.getRegistryKey().getValue().toString();
    }

    private static Vec3d currentPosition(@NotNull ClientPlayerEntity player) {
        return new Vec3d(player.getX(), player.getY(), player.getZ());
    }

    private static String currentScreenName(@Nullable Screen screen) {
        if (screen == null) {
            return "";
        }
        String simpleName = screen.getClass().getSimpleName();
        return simpleName.isBlank() ? screen.getClass().getName() : simpleName;
    }

    private static String itemId(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static String itemName(@Nullable ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : stack.getName().getString();
    }

    private static String displayNameFromItemId(@Nullable String itemId) {
        String raw = safe(itemId);
        if (raw.isBlank()) {
            return "";
        }
        int separator = raw.indexOf(':');
        String tail = separator >= 0 ? raw.substring(separator + 1) : raw;
        String[] parts = tail.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static final class ClientContextSnapshotProvider implements AutomationEngine.ContextSnapshotProvider {
        @Override
        public @NotNull Map<String, Object> snapshot(@NotNull AutomationRule.ConditionSource source, @NotNull AutomationEvent event) {
            MinecraftClient client = MinecraftClient.getInstance();
            return switch (source) {
                case EVENT -> eventSnapshot(event);
                case CLIENT -> clientSnapshot(client);
                case PLAYER -> playerSnapshot(client.player, client.world);
                case WORLD -> worldSnapshot(client.world);
                case PLACEHOLDER -> Map.of();
            };
        }

        @Override
        public @Nullable Object resolve(@NotNull AutomationRule.ConditionSource source,
                                        @NotNull String field,
                                        @NotNull AutomationEvent event) {
            if (source == AutomationRule.ConditionSource.PLACEHOLDER) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null || client.player == null || field.isBlank()) {
                    return null;
                }
                String token = stripPlaceholderBraces(field);
                String wrapped = "{" + token + "}";
                String resolved = MacroPlaceholders.expand(client, wrapped);
                return wrapped.equals(resolved) ? null : resolved;
            }
            return AutomationEngine.ContextSnapshotProvider.super.resolve(source, field, event);
        }

        private static Map<String, Object> eventSnapshot(AutomationEvent event) {
            Map<String, Object> values = new LinkedHashMap<>(event.attributes());
            values.put("type", event.type().name());
            values.put("eventType", event.type().name());
            values.put("timestampMs", event.timestampMs());
            values.put("clientTick", event.clientTick());
            return values;
        }

        private static Map<String, Object> clientSnapshot(MinecraftClient client) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("connected", client.player != null && client.world != null);
            values.put("screen", currentScreenName(client.currentScreen));
            values.put("screenPresent", client.currentScreen != null);
            values.put("paused", client.isPaused());
            values.put("worldLoaded", client.world != null);
            values.put("playerLoaded", client.player != null);
            return values;
        }

        private static Map<String, Object> playerSnapshot(@Nullable ClientPlayerEntity player, @Nullable ClientWorld world) {
            Map<String, Object> values = new LinkedHashMap<>();
            if (player == null) {
                return values;
            }
            values.put("name", player.getName().getString());
            values.put("x", player.getX());
            values.put("y", player.getY());
            values.put("z", player.getZ());
            values.put("health", player.getHealth());
            values.put("maxHealth", player.getMaxHealth());
            values.put("food", player.getHungerManager().getFoodLevel());
            values.put("sneaking", player.isSneaking());
            values.put("sprinting", player.isSprinting());
            values.put("dimension", currentDimensionId(world));
            values.put("hotbarSlot", player.getInventory().getSelectedSlot());
            values.put("mainHandItemId", itemId(player.getMainHandStack()));
            values.put("mainHandItemName", itemName(player.getMainHandStack()));
            return values;
        }

        private static Map<String, Object> worldSnapshot(@Nullable ClientWorld world) {
            Map<String, Object> values = new LinkedHashMap<>();
            if (world == null) {
                return values;
            }
            values.put("dimension", currentDimensionId(world));
            values.put("time", world.getTime());
            values.put("raining", world.isRaining());
            values.put("thundering", world.isThundering());
            return values;
        }

        private static String stripPlaceholderBraces(@NotNull String field) {
            String trimmed = field.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
            return trimmed;
        }
    }

    private static final class ClientBridge implements ActionExecutor.Bridge {
        @Override
        public boolean runMacro(@NotNull String macroId) {
            return CommandMacros.runMacroById(macroId);
        }

        @Override
        public ActionExecutor.ActionResult runScript(@NotNull String scriptFile, @NotNull Map<String, Object> context) {
            try {
                ScriptStorage.ensureAutomationExamples();
            } catch (Exception ignored) {
            }
            if (!ScriptStorage.exists(scriptFile)) {
                return ActionExecutor.ActionResult.error("Script '" + scriptFile + "' was not found in config/m0-dev-tools/scripts.");
            }
            try {
                String script = ScriptStorage.readScript(scriptFile);
                ScriptManager manager = managerForScript(scriptFile);
                Object result = manager.runScript(script, context);
                return ActionExecutor.ActionResult.ok("Script '" + scriptFile + "' result: " + result);
            } catch (Exception e) {
                M0DevTools.LOGGER.warn("Automation script '{}' failed: {}", scriptFile, e.getMessage());
                return ActionExecutor.ActionResult.error("Script '" + scriptFile + "' failed: " + e.getMessage());
            }
        }

        @Override
        public boolean sendClientCommand(@NotNull String command) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return false;
            }

            String raw = command.trim();
            if (raw.isEmpty()) {
                return false;
            }
            if (raw.startsWith("/")) {
                client.player.networkHandler.sendChatCommand(raw.substring(1));
            } else {
                client.player.networkHandler.sendChatMessage(raw);
            }
            return true;
        }

        @Override
        public boolean toggleModule(@NotNull String moduleId, @Nullable Boolean enabledState) {
            me.m0dii.modules.Module module = ModuleRegistry.findModuleById(moduleId);
            if (module == null) {
                return false;
            }
            if (enabledState == null) {
                module.toggleEnabled();
            } else {
                module.setEnabled(enabledState);
            }
            return true;
        }

        @Override
        public @Nullable MinecraftClient client() {
            return MinecraftClient.getInstance();
        }

        @Override
        public @Nullable ClientPlayerEntity player() {
            return MinecraftClient.getInstance().player;
        }

        @Override
        public @Nullable ClientWorld world() {
            return MinecraftClient.getInstance().world;
        }

        private static ScriptManager managerForScript(String scriptFile) {
            return ScriptTypes.managerFor(scriptFile);
        }
    }
}
