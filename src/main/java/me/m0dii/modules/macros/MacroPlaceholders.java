package me.m0dii.modules.macros;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.utils.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MacroPlaceholders {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");
    private static final Random RAND = new Random();
    private static final Map<Integer, Boolean> KEY_LAST_DOWN = new HashMap<>();
    private static final Map<String, MacroPlaceholderProvider> PROVIDERS = new LinkedHashMap<>();
    private static final PlaceholderRegistry REGISTRY = createRegistry();

    private MacroPlaceholders() {
    }

    public static synchronized void registerProvider(MacroPlaceholderProvider provider) {
        if (provider == null || provider.getProviderId() == null || provider.getProviderId().isBlank()) {
            return;
        }
        PROVIDERS.put(provider.getProviderId(), provider);
    }

    public static synchronized void unregisterProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return;
        }
        PROVIDERS.remove(providerId);
    }

    private static synchronized List<MacroPlaceholderProvider> getProvidersSnapshot() {
        return List.copyOf(PROVIDERS.values());
    }

    public static String expand(Minecraft client, String input) {
        return expandInternal(client, input, false);
    }

    public static String expandForCanvas(Minecraft client, String input) {
        return expandInternal(client, input, true);
    }

    private static String expandInternal(Minecraft client, String input, boolean canvasMode) {
        if (input == null || input.isEmpty() || client == null || client.player == null) {
            return input;
        }

        Player p = client.player;
        var world = client.level;
        HitResult hit = client.hitResult;
        BlockPos lookBlock = null;
        Entity lookEntity = null;
        if (hit instanceof BlockHitResult bhr) {
            lookBlock = bhr.getBlockPos();
        } else if (hit instanceof EntityHitResult ehr) {
            lookEntity = ehr.getEntity();
        }

        Matcher m = PLACEHOLDER.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(1).trim();
            String replacement = resolveToken(token, client, p, world != null ? world.dimension().identifier() : null, lookBlock, lookEntity, canvasMode);
            if (replacement == null) {
                replacement = "";
            }
            // Escape backslashes and dollars for regex append
            replacement = Matcher.quoteReplacement(replacement);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveToken(String token, Minecraft client, Player p, Identifier dimId, BlockPos lookBlock, Entity lookEntity, boolean canvasMode) {
        String transformed = tryResolveTransformedToken(token, client, p, dimId, lookBlock, lookEntity, canvasMode);
        if (transformed != null) {
            return transformed;
        }

        return resolveTokenCore(token, client, p, dimId, lookBlock, lookEntity, canvasMode);
    }

    private static String resolveTokenCore(String token, Minecraft client, Player p, Identifier dimId, BlockPos lookBlock, Entity lookEntity, boolean canvasMode) {
        String exact = REGISTRY.resolveExact(new PlaceholderRegistry.Context(client, p, dimId, lookBlock, lookEntity, canvasMode, token));
        if (exact != null) {
            return exact;
        }

        String armor = resolveArmorToken(token, p);
        if (armor != null) {
            return armor;
        }

        String itemCount = resolveItemCountToken(token, client, p);
        if (itemCount != null) {
            return itemCount;
        }

        String keyState = resolveKeyStateToken(token, client);
        if (keyState != null) {
            return keyState;
        }

        String playerList = resolvePlayerListToken(token, p);
        if (playerList != null) {
            return playerList;
        }

        String entityList = resolveEntityListToken(token, p);
        if (entityList != null) {
            return entityList;
        }

        // Functions with args: rand.int(a,b)
        if (token.startsWith("rand.int(") && token.endsWith(")")) {
            String inside = token.substring("rand.int(".length(), token.length() - 1);
            String[] parts = inside.split(",");
            try {
                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());
                if (a > b) {
                    int t = a;
                    a = b;
                    b = t;
                }
                int v = a + RAND.nextInt(b - a + 1);
                return Integer.toString(v);
            } catch (Exception ignored) {
                // ignore malformed args
            }
            return "0";
        }

        String selector = resolveSelectorToken(token, p, canvasMode);
        if (selector != null) {
            return selector;
        }

        for (MacroPlaceholderProvider provider : getProvidersSnapshot()) {
            try {
                String resolved = provider.resolvePlaceholder(token, client, p, canvasMode);
                if (resolved != null) {
                    return resolved;
                }
            } catch (Exception ignored) {
                // Keep placeholder expansion resilient if a custom provider throws.
            }
        }

        return null;
    }

    private static String resolveItemCountToken(String token, Minecraft client, Player player) {
        final String inventoryPrefix = "inventory.count:";
        final String containerPrefix = "container.count:";

        if (token.startsWith(inventoryPrefix)) {
            Item item = parseItemTokenArg(token.substring(inventoryPrefix.length()));
            if (item == null) {
                return "0";
            }
            return Integer.toString(countItemInInventory(player.getInventory(), item));
        }

        if (token.startsWith(containerPrefix)) {
            Item item = parseItemTokenArg(token.substring(containerPrefix.length()));
            if (item == null) {
                return "0";
            }
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen)) {
                return "0";
            }
            int total = 0;
            Inventory playerInventory = player.getInventory();
            for (Slot slot : handledScreen.getMenu().slots) {
                if (slot == null || !slot.hasItem() || slot.container == playerInventory) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty() && stack.is(item)) {
                    total += stack.getCount();
                }
            }
            return Integer.toString(total);
        }

        return null;
    }

    private static int countItemInInventory(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static Item parseItemTokenArg(String raw) {
        String trimmed = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }

        Identifier id = trimmed.indexOf(':') >= 0
                ? Identifier.tryParse(trimmed)
                : Identifier.tryParse("minecraft:" + trimmed);
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
    }

    private static String resolveArmorToken(String token, Player player) {
        if (!token.startsWith("armor.")) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        ItemStack stack = switch (parts[1]) {
            case "boots" -> player.getItemBySlot(EquipmentSlot.FEET);
            case "leggings" -> player.getItemBySlot(EquipmentSlot.LEGS);
            case "chestplate" -> player.getItemBySlot(EquipmentSlot.CHEST);
            case "helmet" -> player.getItemBySlot(EquipmentSlot.HEAD);
            default -> null;
        };
        if (stack == null) {
            return null;
        }

        return switch (parts[2]) {
            case "item" -> stack.getHoverName().getString();
            case "id" -> stack.getItem().toString();
            case "count" -> Integer.toString(stack.getCount());
            case "damage" -> Integer.toString(stack.getDamageValue());
            case "max_damage" -> Integer.toString(stack.getMaxDamage());
            case "durability" -> Integer.toString(durability(stack));
            default -> null;
        };
    }

    private static String tryResolveTransformedToken(String token,
                                                     Minecraft client,
                                                     Player p,
                                                     Identifier dimId,
                                                     BlockPos lookBlock,
                                                     Entity lookEntity,
                                                     boolean canvasMode) {
        if (token == null || token.indexOf('|') < 0) {
            return null;
        }

        String[] parts = token.split("\\|");
        if (parts.length < 2) {
            return null;
        }

        String baseToken = parts[0].trim();
        if (baseToken.isEmpty()) {
            return null;
        }

        String value = resolveTokenCore(baseToken, client, p, dimId, lookBlock, lookEntity, canvasMode);
        if (value == null) {
            return null;
        }

        for (int i = 1; i < parts.length; i++) {
            String transform = parts[i].trim().toLowerCase(Locale.ROOT);
            value = applyTransform(value, transform);
        }
        return value;
    }

    private static String applyTransform(String value, String transform) {
        if (value == null || transform == null || transform.isBlank()) {
            return value;
        }

        return switch (transform) {
            case "lower", "lowercase" -> value.toLowerCase(Locale.ROOT);
            case "upper", "uppercase" -> value.toUpperCase(Locale.ROOT);
            case "trim" -> value.trim();
            case "capitalize" -> capitalizeFirst(value);
            case "title", "titlecase" -> toTitleCase(value);
            case "basename", "non_namespaced", "non-namespaced", "nsless" -> stripNamespace(value);
            default -> value;
        };
    }

    private static String capitalizeFirst(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static String toTitleCase(String value) {
        String normalized = stripNamespace(value).replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        String[] words = normalized.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) {
                continue;
            }
            if (i > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static String stripNamespace(String value) {
        if (value == null) {
            return "";
        }
        int idx = value.indexOf(':');
        return idx >= 0 ? value.substring(idx + 1) : value;
    }

    private static String resolveKeyStateToken(String token, Minecraft client) {
        if (token == null || client == null || client.getWindow() == null) {
            return null;
        }
        String pressedPrefix = "key.pressed.";
        String heldPrefix = "key.held.";
        boolean pressed = token.startsWith(pressedPrefix);
        boolean held = token.startsWith(heldPrefix);
        if (!pressed && !held) {
            return null;
        }

        String keyName = token.substring((pressed ? pressedPrefix : heldPrefix).length()).trim();
        int keyCode = resolveNamedKeyCode(keyName);
        if (keyCode < 0) {
            return "false";
        }

        boolean down = switch (keyName.toLowerCase(Locale.ROOT)) {
            // Mouse aliases resolve to button ids, which are not keyboard scancodes.
            case "lmb", "mouse1" -> client.mouseHandler.isLeftPressed();
            case "rmb", "mouse2" -> client.mouseHandler.isRightPressed();
            case "mmb", "mouse3" -> client.mouseHandler.isMiddlePressed();
            default -> InputConstants.isKeyDown(keyCode);
        };

        if (held) {
            return Boolean.toString(down);
        }

        boolean last = KEY_LAST_DOWN.getOrDefault(keyCode, false);
        KEY_LAST_DOWN.put(keyCode, down);
        return Boolean.toString(down && !last);
    }

    private static int resolveNamedKeyCode(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return -1;
        }
        String key = rawName.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "space" -> InputConstants.KEY_SPACE;
            case "tab" -> InputConstants.KEY_TAB;
            case "enter", "return" -> InputConstants.KEY_RETURN;
            case "esc", "escape" -> InputConstants.KEY_ESCAPE;
            case "shift" -> InputConstants.KEY_LSHIFT;
            case "ctrl", "control" -> InputConstants.KEY_LCONTROL;
            case "alt" -> InputConstants.KEY_LALT;
            case "up" -> InputConstants.KEY_UP;
            case "down" -> InputConstants.KEY_DOWN;
            case "left" -> InputConstants.KEY_LEFT;
            case "right" -> InputConstants.KEY_RIGHT;
            case "lmb", "mouse1" -> InputConstants.MOUSE_BUTTON_LEFT;
            case "rmb", "mouse2" -> InputConstants.MOUSE_BUTTON_RIGHT;
            case "mmb", "mouse3" -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> {
                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (c >= 'a' && c <= 'z') {
                        yield InputConstants.KEY_A + (c - 'a');
                    }
                    // Scancode order is 1..9 then 0.
                    if (c == '0') {
                        yield InputConstants.KEY_0;
                    }
                    if (c >= '1' && c <= '9') {
                        yield InputConstants.KEY_1 + (c - '1');
                    }
                }
                if (key.startsWith("f")) {
                    try {
                        int fn = Integer.parseInt(key.substring(1));
                        // F1-F12 and F13-F24 are two separate scancode runs.
                        if (fn >= 1 && fn <= 12) {
                            yield InputConstants.KEY_F1 + (fn - 1);
                        }
                        if (fn >= 13 && fn <= 24) {
                            yield InputConstants.KEY_F13 + (fn - 13);
                        }
                    } catch (Exception ignored) {
                        // no-op
                    }
                }
                yield -1;
            }
        };
    }

    private static Integer resolveCurrentFps(Minecraft client) {
        try {
            return client.getFps();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (Exception ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static String resolveEntityListToken(String token, Player p) {
        var world = p.level();
        if (world == null) {
            return null;
        }

        if ("entities.nearby.count".equals(token)) {
            long count = world.getEntities(p, p.getBoundingBox().inflate(64)).stream()
                    .filter(entity -> !(entity instanceof Player))
                    .count();
            return Long.toString(count);
        }

        // entities.nearby.N, entities.nearby.N.nl, entities.nearby.N.rR, entities.nearby.N.rR.nl
        // entities.nearby.N.unique, entities.nearby.N.with_distance
        // entities.nearby.N.unique.with_distance.nl
        if (!token.startsWith("entities.nearby.")) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length < 3) {
            return null;
        }

        int limit;
        try {
            limit = Math.max(1, Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }

        int radius = 64;
        boolean newline = false;
        boolean unique = false;
        boolean withDistance = false;
        boolean withDirection = false;
        boolean withDirectionArrow = false;
        boolean sortByName = false;

        for (int i = 3; i < parts.length; i++) {
            String part = parts[i];
            if ("nl".equals(part)) {
                newline = true;
                continue;
            }
            if ("unique".equals(part)) {
                unique = true;
                continue;
            }
            if ("with_distance".equals(part)) {
                withDistance = true;
                continue;
            }
            if ("with_direction".equals(part)) {
                withDirection = true;
                continue;
            }
            if ("with_direction_arrow".equals(part)) {
                withDirection = true;
                withDirectionArrow = true;
                continue;
            }
            if ("sort_name".equals(part) || "sort=name".equals(part)) {
                sortByName = true;
                continue;
            }
            if ("sort_distance".equals(part) || "sort=distance".equals(part)) {
                sortByName = false;
                continue;
            }
            if (part.length() > 1 && part.charAt(0) == 'r') {
                try {
                    radius = Math.max(1, Integer.parseInt(part.substring(1)));
                } catch (NumberFormatException ignored) {
                    // ignore malformed radius segment
                }
            }
        }

        final boolean withDistanceFinal = withDistance;
        final boolean withDirectionFinal = withDirection;
        final boolean withDirectionArrowFinal = withDirectionArrow;
        var nearbyEntities = world.getEntities(p, p.getBoundingBox().inflate(radius)).stream()
                .filter(entity -> !(entity instanceof Player));

        if (sortByName) {
            nearbyEntities = nearbyEntities.sorted((a, b) -> a.getType().getDescription().getString().compareToIgnoreCase(b.getType().getDescription().getString()));
        } else {
            nearbyEntities = nearbyEntities.sorted(Comparator.comparingDouble(a -> a.distanceToSqr(p)));
        }

        List<String> names = nearbyEntities
                .map(entity -> {
                    String name = entity.getType().getDescription().getString();
                    if (!withDistanceFinal && !withDirectionFinal) {
                        return name;
                    }
                    List<String> extras = new ArrayList<>();
                    if (withDistanceFinal) {
                        int meters = (int) Math.round(Math.sqrt(entity.distanceToSqr(p)));
                        extras.add(meters + "m");
                    }
                    if (withDirectionFinal) {
                        extras.add(directionFromToShort(p.getX(), p.getZ(), entity.getX(), entity.getZ(), p.getYRot(), withDirectionArrowFinal));
                    }
                    return name + " (" + String.join(" ", extras) + ")";
                })
                .toList();

        if (unique) {
            names = new ArrayList<>(new LinkedHashSet<>(names));
        }

        if (names.size() > limit) {
            names = names.subList(0, limit);
        }

        return names.isEmpty() ? "(none)" : String.join(newline ? "\n" : ", ", names);
    }

    private static String resolvePlayerListToken(String token, Player p) {
        var world = p.level();
        if (world == null) {
            return null;
        }

        if ("players.count".equals(token)) {
            return Integer.toString(world.players().size());
        }

        if ("players.count.other".equals(token)) {
            long others = world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .count();
            return Long.toString(others);
        }

        if ("players.list".equals(token) || "players.list.csv".equals(token)) {
            return world.players().stream()
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining(", "));
        }

        if ("players.list.other".equals(token) || "players.list.other.csv".equals(token)) {
            return world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining(", "));
        }

        if ("players.list.nl".equals(token)) {
            return world.players().stream()
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining("\n"));
        }

        if ("players.list.other.nl".equals(token)) {
            return world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining("\n"));
        }

        // players.nearby.N                      -> CSV
        // players.nearby.N.nl                   -> newline-separated
        // players.nearby.N.rR                   -> CSV with radius R (blocks)
        // players.nearby.N.rR.nl                -> newline-separated with radius R
        // players.nearby.N.unique               -> dedupe names (safety)
        // players.nearby.N.with_distance        -> append distance (m)
        // players.nearby.N.unique.with_distance.nl
        if (token.startsWith("players.nearby.")) {
            String[] parts = token.split("\\.");
            if (parts.length < 3) {
                return null;
            }

            int limit;
            try {
                limit = Math.max(1, Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return null;
            }

            int radius = Integer.MAX_VALUE;
            boolean newline = false;
            boolean unique = false;
            boolean withDistance = false;
            boolean withDirection = false;
            boolean withDirectionArrow = false;
            boolean sortByName = false;

            for (int i = 3; i < parts.length; i++) {
                String part = parts[i];
                if ("nl".equals(part)) {
                    newline = true;
                    continue;
                }
                if ("unique".equals(part)) {
                    unique = true;
                    continue;
                }
                if ("with_distance".equals(part)) {
                    withDistance = true;
                    continue;
                }
                if ("with_direction".equals(part)) {
                    withDirection = true;
                    continue;
                }
                if ("with_direction_arrow".equals(part)) {
                    withDirection = true;
                    withDirectionArrow = true;
                    continue;
                }
                if ("sort_name".equals(part) || "sort=name".equals(part)) {
                    sortByName = true;
                    continue;
                }
                if ("sort_distance".equals(part) || "sort=distance".equals(part)) {
                    sortByName = false;
                    continue;
                }
                if (part.length() > 1 && part.charAt(0) == 'r') {
                    try {
                        radius = Math.max(1, Integer.parseInt(part.substring(1)));
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed radius suffix.
                    }
                }
            }

            final int maxRadiusSq = radius == Integer.MAX_VALUE ? Integer.MAX_VALUE : radius * radius;
            var nearbyPlayers = world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .filter(other -> p.distanceToSqr(other) <= maxRadiusSq);

            if (sortByName) {
                nearbyPlayers = nearbyPlayers.sorted((a, b) -> a.getGameProfile().name().compareToIgnoreCase(b.getGameProfile().name()));
            } else {
                nearbyPlayers = nearbyPlayers.sorted(Comparator.comparingDouble((Player a) -> a.distanceToSqr(p)));
            }

            final boolean withDistanceFinal = withDistance;
            final boolean withDirectionFinal = withDirection;
            final boolean withDirectionArrowFinal = withDirectionArrow;

            List<String> nearby = nearbyPlayers
                    .map(other -> {
                        String name = other.getGameProfile().name();
                        if (!withDistanceFinal && !withDirectionFinal) {
                            return name;
                        }
                        List<String> extras = new ArrayList<>();
                        if (withDistanceFinal) {
                            int meters = (int) Math.round(Math.sqrt(other.distanceToSqr(p)));
                            extras.add(meters + "m");
                        }
                        if (withDirectionFinal) {
                            extras.add(directionFromToShort(p.getX(), p.getZ(), other.getX(), other.getZ(), p.getYRot(), withDirectionArrowFinal));
                        }
                        return name + " (" + String.join(" ", extras) + ")";
                    })
                    .toList();

            if (unique) {
                nearby = new ArrayList<>(new LinkedHashSet<>(nearby));
            }

            if (nearby.size() > limit) {
                nearby = nearby.subList(0, limit);
            }

            return nearby.isEmpty() ? "(none)" : String.join(newline ? "\n" : ", ", nearby);
        }

        if ("players.nearby.count".equals(token)) {
            long count = world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .count();
            return Long.toString(count);
        }

        return null;
    }

    private static String resolveSelectorToken(String token, Player p, boolean canvasMode) {
        if (!canvasMode) {
            return switch (token) {
                case "sel.self" -> "@s";
                case "sel.nearest" -> "@p";
                case "sel.random" -> "@r";
                case "sel.all" -> "@a";
                case "sel.entities" -> "@e";
                default -> null;
            };
        }

        var world = p.level();
        if (world == null) {
            return "";
        }

        return switch (token) {
            case "sel.self" -> p.getGameProfile().name();
            case "sel.nearest" -> world.players().stream()
                    .filter(other -> !other.getUUID().equals(p.getUUID()))
                    .min(Comparator.comparingDouble((Player a) -> a.distanceToSqr(p)))
                    .map(player -> player.getGameProfile().name())
                    .orElse("(none)");
            case "sel.random" -> {
                var others = world.players().stream()
                        .filter(other -> !other.getUUID().equals(p.getUUID()))
                        .toList();
                if (others.isEmpty()) {
                    yield "(none)";
                }
                yield others.get(RAND.nextInt(others.size())).getGameProfile().name();
            }
            case "sel.all" -> {
                var names = world.players().stream()
                        .filter(other -> !other.getUUID().equals(p.getUUID()))
                        .map(other -> other.getGameProfile().name())
                        .toList();
                yield names.isEmpty() ? "(none)" : String.join(", ", names);
            }
            case "sel.entities" -> world.getEntities(p, p.getBoundingBox().inflate(64)).stream()
                    .findFirst()
                    .map(entity -> entity.getName().getString())
                    .orElse("(none)");
            default -> null;
        };
    }

    private static final List<String> BASE_PLACEHOLDER_DOCS = List.of(
            "[How to use]",
            "Write placeholders as {token}. Example: /msg {player.name} hi",
            "Button Action supports cmd:/ msg:/ say:/ copy:/ bar:/ plain chat text",
            "Button Action and macro commands both support placeholders",
            "",
            "[Player]",
            "{player.name} => your name",
            "{player.uuid} => your UUID",
            "{hp} {max_hp} {food} {saturation} {xp} {level}",
            "{yaw} {pitch} {player.gamemode}",
            "{player.sneaking} {player.sprinting} {player.on_ground} {player.swimming}",
            "{player.chunk} {player.chunk.x} {player.chunk.z}",
            "{player.vel.x} {player.vel.y} {player.vel.z} {player.speed}",
            "",
            "[Position]",
            "{pos.x} {pos.y} {pos.z} => block coords",
            "{pos.xf} {pos.yf} {pos.zf} => decimal coords",
            "{pos.xyz} => x y z",
            "{pos.biome} {pos.dim} {pos.light} {pos.facing}",
            "{pos.biome.pretty} => Plains (no namespace, title case)",
            "{dir.compass} {dir.compass.short} => North / N (supports diagonals)",
            "{dir.compass.arrow} {dir.compass.short_arrow} => North ↑ / N ↑",
            "",
            "[Look target]",
            "{look.block.xyz} {look.block.x} {look.block.y} {look.block.z}",
            "{look.block.id} {look.block.light}",
            "{look.entity.name} {look.entity.uuid} {look.entity.id}",
            "{look.entity.type}",
            "{look.dir.x} {look.dir.y} {look.dir.z}",
            "",
            "[Selectors and lists]",
            "{sel.self} {sel.nearest} {sel.random} {sel.all} {sel.entities}",
            "{players.count} {players.count.other} {players.nearby.count}",
            "{players.list} {players.list.other} {.nl for newline}",
            "{players.nearby.3} {.r128 .with_distance .with_direction .with_direction_arrow .unique .sort=name}",
            "{entities.nearby.3} {.r64 .with_distance .with_direction .with_direction_arrow .unique .sort=distance}",
            "",
            "[Item and misc]",
            "{hand.item} {hand.id} {hand.count}",
            "{hand.damage} {hand.max_damage} {hand.durability}",
            "{offhand.item} {offhand.id} {offhand.count}",
            "{offhand.damage} {offhand.max_damage} {offhand.durability}",
            "{armor.helmet.item} {armor.helmet.durability}",
            "{armor.chestplate.item} {armor.chestplate.durability}",
            "{armor.leggings.item} {armor.leggings.durability}",
            "{armor.boots.item} {armor.boots.durability}",
            "{inventory.count:diamond} {container.count:diamond}",
            "{dim} => current dimension id",
            "{rand.int(1,10)} => random integer in range",
            "",
            "[Client and server session]",
            "{client.fps} {client.screen}",
            "{client.screen.width} {client.screen.height}",
            "{client.server.singleplayer}",
            "{client.server.name} {client.server.address}",
            "",
            "[Clicks per second]",
            "{cps.left} {cps.right} {cps.total}",
            "",
            "[World time]",
            "{world.time} {world.time.ticks} {world.time.day} {world.day}",
            "{world.time.day_ticks} {world.time.clock}",
            "{world.is_day} {world.is_night}",
            "",
            "[Input state]",
            "{key.pressed.w} => true once when W is pressed",
            "{key.held.space} => true while Space is held",
            "{player.name|lower} {pos.biome|basename|title}",
            "",
            "[Conditionals in macro commands]",
            "if:<left>==<right>::<when_true>:else:<when_false>",
            "Example: if:{player.gamemode}==creative::cmd:/say yes:else:cmd:/say no"
    );

    @Deprecated
    public static final List<String> PLACEHOLDER_DOCS = BASE_PLACEHOLDER_DOCS;

    public static List<String> getPlaceholderDocs() {
        List<String> docs = new ArrayList<>(BASE_PLACEHOLDER_DOCS);
        for (MacroPlaceholderProvider provider : getProvidersSnapshot()) {
            try {
                List<String> providerDocs = provider.getPlaceholderDocs();
                if (providerDocs != null && !providerDocs.isEmpty()) {
                    docs.add("");
                    docs.addAll(providerDocs);
                }
            } catch (Exception ignored) {
                // Ignore provider doc errors to keep docs screen stable.
            }
        }
        return docs;
    }

    public static List<String> getKnownPlaceholderTokens() {
        LinkedHashSet<String> tokens = new LinkedHashSet<>(REGISTRY.tokens());
        tokens.addAll(MacroPlaceholderCatalog.supplementalPlaceholderTokens());
        for (MacroPlaceholderProvider provider : getProvidersSnapshot()) {
            try {
                List<String> providerTokens = provider.getKnownPlaceholderTokens();
                if (providerTokens == null || providerTokens.isEmpty()) {
                    continue;
                }
                for (String token : providerTokens) {
                    if (token != null && !token.isBlank()) {
                        tokens.add(token.trim());
                    }
                }
            } catch (Exception ignored) {
                // Keep suggestions resilient if a provider fails.
            }
        }
        return List.copyOf(tokens);
    }

    private static PlaceholderRegistry createRegistry() {
        PlaceholderRegistry registry = new PlaceholderRegistry();

        registry.register("dim", ctx -> ctx.dimensionId() == null ? "" : ctx.dimensionId().toString());
        registry.register("cps.left", ctx -> Integer.toString(CpsTracker.getLeftCps()));
        registry.register("cps.right", ctx -> Integer.toString(CpsTracker.getRightCps()));
        registry.register("cps.total", ctx -> Integer.toString(CpsTracker.getTotalCps()));

        registry.register("hp", ctx -> Integer.toString((int) Math.floor(ctx.player().getHealth())));
        registry.register("max_hp", ctx -> Integer.toString((int) Math.floor(ctx.player().getMaxHealth())));
        registry.register("food", ctx -> Integer.toString(ctx.player().getFoodData().getFoodLevel()));
        registry.register("xp", ctx -> Integer.toString(ctx.player().totalExperience));
        registry.register("level", ctx -> Integer.toString(ctx.player().experienceLevel));
        registry.register("saturation", ctx -> Integer.toString((int) ctx.player().getFoodData().getSaturationLevel()));
        registry.register("yaw", ctx -> String.format(Locale.ROOT, "%.1f", Mth.wrapDegrees(ctx.player().getYRot())));
        registry.register("pitch", ctx -> String.format(Locale.ROOT, "%.1f", ctx.player().getXRot()));
        registry.register("player.gamemode", ctx -> resolvePlayerGameMode(ctx.player()));
        registry.register("player.sneaking", ctx -> Boolean.toString(ctx.player().isShiftKeyDown()));
        registry.register("player.sprinting", ctx -> Boolean.toString(ctx.player().isSprinting()));
        registry.register("player.on_ground", ctx -> Boolean.toString(ctx.player().onGround()));
        registry.register("player.swimming", ctx -> Boolean.toString(ctx.player().isSwimming()));
        registry.register("player.chunk.x", ctx -> Integer.toString(ctx.player().chunkPosition().x()));
        registry.register("player.chunk.z", ctx -> Integer.toString(ctx.player().chunkPosition().z()));
        registry.register("player.chunk", ctx -> ctx.player().chunkPosition().x() + " " + ctx.player().chunkPosition().z());

        registry.register("player.name", ctx -> ctx.player().getGameProfile().name());
        registry.register("player.uuid", ctx -> ctx.player().getStringUUID());
        registerAliases(registry, ctx -> formatDouble(ctx.player().getDeltaMovement().x), "player.vel.x", "vel.x");
        registerAliases(registry, ctx -> formatDouble(ctx.player().getDeltaMovement().y), "player.vel.y", "vel.y");
        registerAliases(registry, ctx -> formatDouble(ctx.player().getDeltaMovement().z), "player.vel.z", "vel.z");
        registerAliases(registry, ctx -> {
            Vec3 velocity = ctx.player().getDeltaMovement();
            return formatDouble(Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0);
        }, "player.speed", "speed");

        registry.register("pos.x", ctx -> Integer.toString(ctx.player().getBlockX()));
        registry.register("pos.y", ctx -> Integer.toString(ctx.player().getBlockY()));
        registry.register("pos.z", ctx -> Integer.toString(ctx.player().getBlockZ()));
        registry.register("pos.xyz", ctx -> ctx.player().getBlockX() + " " + ctx.player().getBlockY() + " " + ctx.player().getBlockZ());
        registry.register("pos.xf", ctx -> formatDouble(ctx.player().getX()));
        registry.register("pos.yf", ctx -> formatDouble(ctx.player().getY()));
        registry.register("pos.zf", ctx -> formatDouble(ctx.player().getZ()));
        registry.register("pos.biome", ctx -> resolveCurrentBiomeId(ctx.player()));
        registry.register("pos.biome.pretty", ctx -> toTitleCase(resolveCurrentBiomeId(ctx.player())));
        registry.register("pos.dim", ctx -> resolveCurrentDimensionId(ctx.player()));
        registry.register("pos.light", ctx -> ctx.player().level() == null ? "0" : Integer.toString(ctx.player().level().getMaxLocalRawBrightness(ctx.player().blockPosition())));
        registry.register("pos.facing", ctx -> ctx.player().getDirection().getSerializedName());
        registerAliases(registry, ctx -> yawToCompass(ctx.player().getYRot(), true), "pos.facing.short", "dir.compass.short");
        registerAliases(registry, ctx -> yawToCompass(ctx.player().getYRot(), false), "dir.compass", "dir.facing");
        registerAliases(registry, ctx -> yawToCompassWithArrow(ctx.player().getYRot(), true), "dir.compass.short_arrow", "dir.compass.short.arrow");
        registerAliases(registry, ctx -> yawToCompassWithArrow(ctx.player().getYRot(), false), "dir.compass.arrow", "dir.facing.arrow");

        registry.register("look.block.x", ctx -> ctx.lookBlock() == null ? null : Integer.toString(ctx.lookBlock().getX()));
        registry.register("look.block.y", ctx -> ctx.lookBlock() == null ? null : Integer.toString(ctx.lookBlock().getY()));
        registry.register("look.block.z", ctx -> ctx.lookBlock() == null ? null : Integer.toString(ctx.lookBlock().getZ()));
        registry.register("look.block.xyz", ctx -> ctx.lookBlock() == null ? null : ctx.lookBlock().getX() + " " + ctx.lookBlock().getY() + " " + ctx.lookBlock().getZ());
        registry.register("look.block.id", ctx -> {
            if (ctx.lookBlock() == null) {
                return null;
            }
            if (ctx.player().level() == null) {
                return "";
            }
            return ctx.player().level().getBlockState(ctx.lookBlock()).getBlock().toString();
        });
        registry.register("look.block.light", ctx -> {
            if (ctx.lookBlock() == null) {
                return null;
            }
            if (ctx.player().level() == null) {
                return "0";
            }
            return Integer.toString(ctx.player().level().getMaxLocalRawBrightness(ctx.lookBlock()));
        });

        registry.register("look.entity.name", ctx -> ctx.lookEntity() == null ? null : ctx.lookEntity().getName().getString());
        registry.register("look.entity.uuid", ctx -> ctx.lookEntity() == null ? null : ctx.lookEntity().getStringUUID());
        registry.register("look.entity.id", ctx -> ctx.lookEntity() == null ? null : Integer.toString(ctx.lookEntity().getId()));
        registry.register("look.entity.type", ctx -> ctx.lookEntity() == null ? null : ctx.lookEntity().getType().toString());

        registry.register("look.dir.x", ctx -> formatDouble(ctx.player().getViewVector(1.0f).normalize().x));
        registry.register("look.dir.y", ctx -> formatDouble(ctx.player().getViewVector(1.0f).normalize().y));
        registry.register("look.dir.z", ctx -> formatDouble(ctx.player().getViewVector(1.0f).normalize().z));

        registry.register("hand.item", ctx -> ctx.player().getMainHandItem().getHoverName().getString());
        registry.register("hand.id", ctx -> ctx.player().getMainHandItem().getItem().toString());
        registry.register("hand.count", ctx -> Integer.toString(ctx.player().getMainHandItem().getCount()));
        registry.register("hand.damage", ctx -> Integer.toString(ctx.player().getMainHandItem().getDamageValue()));
        registry.register("hand.max_damage", ctx -> Integer.toString(ctx.player().getMainHandItem().getMaxDamage()));
        registry.register("hand.durability", ctx -> Integer.toString(durability(ctx.player().getMainHandItem())));

        registry.register("offhand.item", ctx -> ctx.player().getOffhandItem().getHoverName().getString());
        registry.register("offhand.id", ctx -> ctx.player().getOffhandItem().getItem().toString());
        registry.register("offhand.count", ctx -> Integer.toString(ctx.player().getOffhandItem().getCount()));
        registry.register("offhand.damage", ctx -> Integer.toString(ctx.player().getOffhandItem().getDamageValue()));
        registry.register("offhand.max_damage", ctx -> Integer.toString(ctx.player().getOffhandItem().getMaxDamage()));
        registry.register("offhand.durability", ctx -> Integer.toString(durability(ctx.player().getOffhandItem())));

        registry.register("client.screen", ctx -> ctx.client().gui.screen() == null ? "none" : ctx.client().gui.screen().getClass().getSimpleName());
        registry.register("client.screen.width", ctx -> ctx.client().getWindow() == null ? "0" : Integer.toString(Math.max(0, ctx.client().getWindow().getGuiScaledWidth())));
        registry.register("client.screen.height", ctx -> ctx.client().getWindow() == null ? "0" : Integer.toString(Math.max(0, ctx.client().getWindow().getGuiScaledHeight())));
        registry.register("client.fps", ctx -> {
            Integer fps = resolveCurrentFps(ctx.client());
            return fps == null ? "0" : Integer.toString(Math.max(0, fps));
        });
        registry.register("client.server.singleplayer", ctx -> {
            try {
                return Boolean.toString(ctx.client().isLocalServer());
            } catch (Exception ignored) {
                return "false";
            }
        });
        registry.register("client.server.address", ctx -> readCurrentServerField(ctx.client(), "ip"));
        registry.register("client.server.name", ctx -> readCurrentServerField(ctx.client(), "name"));

        registerAliases(registry, MacroPlaceholders::resolveWorldTimeTicks, "world.time", "world.time.ticks");
        registerAliases(registry, MacroPlaceholders::resolveWorldDay, "world.day", "world.time.day");
        registry.register("world.time.day_ticks", MacroPlaceholders::resolveWorldDayTicks);
        registry.register("world.time.clock", MacroPlaceholders::resolveWorldClock);
        registry.register("world.is_day", ctx -> Boolean.toString(resolveWorldIsDay(ctx)));
        registry.register("world.is_night", ctx -> Boolean.toString(!resolveWorldIsDay(ctx)));

        return registry;
    }

    private static void registerAliases(PlaceholderRegistry registry, PlaceholderRegistry.Resolver resolver, String... tokens) {
        for (String token : tokens) {
            registry.register(token, resolver);
        }
    }

    private static int durability(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 0;
        }
        return max - stack.getDamageValue();
    }

    private static String resolvePlayerGameMode(Player player) {
        try {
            if (player.isSpectator()) {
                return "spectator";
            }
            var abilities = player.getAbilities();
            if (abilities != null && abilities.instabuild) {
                return "creative";
            }
        } catch (Exception ignored) {
            // Defensive fallback for mapping/runtime differences.
        }
        return "survival";
    }

    private static String resolveCurrentBiomeId(Player player) {
        if (player.level() == null) {
            return "";
        }
        Holder<Biome> entry = player.level().getBiome(player.blockPosition());
        if (entry == null) {
            return "";
        }
        ResourceKey<Biome> key = entry.unwrapKey().orElse(null);
        if (key == null || key.identifier() == null) {
            return "";
        }
        return key.identifier().toString();
    }

    private static String resolveCurrentDimensionId(Player player) {
        if (player.level() == null) {
            return "";
        }
        ResourceKey<?> key = player.level().dimension();
        if (key == null || key.identifier() == null) {
            return "";
        }
        return key.identifier().toString();
    }

    private static String readCurrentServerField(Minecraft client, String fieldName) {
        Object entry = client.getCurrentServer();
        if (entry == null) {
            return "";
        }
        return Objects.toString(readField(entry, fieldName), "");
    }

    private static long worldTimeOrZero(PlaceholderRegistry.Context ctx) {
        if (ctx.player().level() == null) {
            return 0L;
        }
        return ctx.player().level().getOverworldClockTime();
    }

    private static String resolveWorldTimeTicks(PlaceholderRegistry.Context ctx) {
        return Long.toString(worldTimeOrZero(ctx));
    }

    private static String resolveWorldDay(PlaceholderRegistry.Context ctx) {
        return Long.toString(Math.floorDiv(worldTimeOrZero(ctx), 24000L));
    }

    private static String resolveWorldDayTicks(PlaceholderRegistry.Context ctx) {
        return Long.toString(Math.floorMod(worldTimeOrZero(ctx), 24000L));
    }

    private static String resolveWorldClock(PlaceholderRegistry.Context ctx) {
        long dayTicks = Math.floorMod(worldTimeOrZero(ctx), 24000L);
        int hour = (int) ((dayTicks / 1000L + 6L) % 24L);
        int minute = (int) Math.floor(((dayTicks % 1000L) / 1000.0) * 60.0);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private static boolean resolveWorldIsDay(PlaceholderRegistry.Context ctx) {
        long dayTicks = Math.floorMod(worldTimeOrZero(ctx), 24000L);
        return dayTicks < 12300L || dayTicks > 23850L;
    }

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    private static String yawToCompass(float yaw, boolean shortName) {
        String[] shortDirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        String[] longDirs = {"South", "South West", "West", "North West", "North", "North East", "East", "South East"};
        int idx = Math.floorMod(Math.round(yaw / 45.0f), 8);
        return shortName ? shortDirs[idx] : longDirs[idx];
    }

    private static String yawToCompassWithArrow(float yaw, boolean shortName) {
        String base = yawToCompass(yaw, shortName);
        int idx = Math.floorMod(Math.round(yaw / 45.0f), 8);
        return base + " " + compassArrow(idx);
    }

    private static String directionFromToShort(double fromX, double fromZ, double toX, double toZ, float playerYaw, boolean withArrow) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        if (Math.abs(dx) < 0.0001 && Math.abs(dz) < 0.0001) {
            return "HERE";
        }
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (!withArrow) {
            return yawToCompass(targetYaw, true);
        }
        return yawToCompass(targetYaw, true) + " " + relativeLookArrow(playerYaw, targetYaw);
    }

    private static String relativeLookArrow(float playerYaw, float targetYaw) {
        float delta = normalizeYaw(targetYaw - playerYaw);
        int idx = Math.floorMod(Math.round(delta / 45.0f), 8);
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        return arrows[idx];
    }

    private static float normalizeYaw(float yaw) {
        float wrapped = yaw % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private static String compassArrow(int idx) {
        String[] arrows = {"↓", "↙", "←", "↖", "↑", "↗", "→", "↘"};
        return arrows[Math.floorMod(idx, arrows.length)];
    }
}

