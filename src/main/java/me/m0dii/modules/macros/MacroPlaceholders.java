package me.m0dii.modules.macros;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;

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

    private MacroPlaceholders() {
    }

    public static String expand(MinecraftClient client, String input) {
        return expandInternal(client, input, false);
    }

    public static String expandForCanvas(MinecraftClient client, String input) {
        return expandInternal(client, input, true);
    }

    private static String expandInternal(MinecraftClient client, String input, boolean canvasMode) {
        if (input == null || input.isEmpty() || client == null || client.player == null) {
            return input;
        }

        PlayerEntity p = client.player;
        var world = client.world;
        HitResult hit = client.crosshairTarget;
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
            String replacement = resolveToken(token, client, p, world != null ? world.getRegistryKey().getValue() : null, lookBlock, lookEntity, canvasMode);
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

    private static String resolveToken(String token, MinecraftClient client, PlayerEntity p, Identifier dimId, BlockPos lookBlock, Entity lookEntity, boolean canvasMode) {
        String keyState = resolveKeyStateToken(token, client);
        if (keyState != null) {
            return keyState;
        }

        String clientAndWorld = resolveClientAndWorldToken(token, client, p);
        if (clientAndWorld != null) {
            return clientAndWorld;
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

        // Player info
        switch (token) {
            case "player.name" -> {
                return p.getGameProfile().name();
            }
            case "player.uuid" -> {
                return p.getUuidAsString();
            }
            case "hp" -> {
                return Integer.toString((int) Math.floor(p.getHealth()));
            }
            case "max_hp" -> {
                return Integer.toString((int) Math.floor(p.getMaxHealth()));
            }
            case "food" -> {
                return Integer.toString(p.getHungerManager().getFoodLevel());
            }
            case "xp" -> {
                return Integer.toString(p.totalExperience);
            }
            case "level" -> {
                return Integer.toString(p.experienceLevel);
            }
            case "saturation" -> {
                return Integer.toString((int) p.getHungerManager().getSaturationLevel());
            }
            case "yaw" -> {
                // Keep yaw in the typical wrapped range to avoid unbounded growth while turning.
                return String.format("%.1f", MathHelper.wrapDegrees(p.getYaw()));
            }
            case "pitch" -> {
                return String.format("%.1f", p.getPitch());
            }
            case "player.gamemode" -> {
                // Provide a simple gamemode placeholder for client-side use.
                // Prefer spectator check, then creative. Default to "survival" when unknown.
                try {
                    if (p.isSpectator()) {
                        return "spectator";
                    }
                    var abilities = p.getAbilities();
                    if (abilities != null && abilities.creativeMode) {
                        return "creative";
                    }
                } catch (Exception ignored) {
                    // defensive: fall back to survival
                }
                return "survival";
            }
            case "player.sneaking" -> {
                return Boolean.toString(p.isSneaking());
            }
            case "player.sprinting" -> {
                return Boolean.toString(p.isSprinting());
            }
            case "player.on_ground" -> {
                return Boolean.toString(p.isOnGround());
            }
            case "player.swimming" -> {
                return Boolean.toString(p.isSwimming());
            }
            case "player.chunk.x" -> {
                return Integer.toString(p.getChunkPos().x);
            }
            case "player.chunk.z" -> {
                return Integer.toString(p.getChunkPos().z);
            }
            case "player.chunk" -> {
                return p.getChunkPos().x + " " + p.getChunkPos().z;
            }
            case "player.vel.x", "vel.x" -> {
                return formatDouble(p.getVelocity().x);
            }
            case "player.vel.y", "vel.y" -> {
                return formatDouble(p.getVelocity().y);
            }
            case "player.vel.z", "vel.z" -> {
                return formatDouble(p.getVelocity().z);
            }
            case "player.speed", "speed" -> {
                Vec3d v = p.getVelocity();
                return formatDouble(Math.sqrt(v.x * v.x + v.z * v.z) * 20.0);
            }
            default -> {
                // no-op
            }
        }

        switch (token) {
            case "pos.x" -> {
                return Integer.toString(p.getBlockX());
            }
            case "pos.y" -> {
                return Integer.toString(p.getBlockY());
            }
            case "pos.z" -> {
                return Integer.toString(p.getBlockZ());
            }
            case "pos.xyz" -> {
                return p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
            }
            case "pos.xf" -> {
                return formatDouble(p.getX());
            }
            case "pos.yf" -> {
                return formatDouble(p.getY());
            }
            case "pos.zf" -> {
                return formatDouble(p.getZ());
            }
            case "pos.biome" -> {
                if (p.getEntityWorld() != null) {
                    RegistryEntry<Biome> biomeRegistryEntry = p.getEntityWorld().getBiome(p.getBlockPos());

                    if (biomeRegistryEntry != null) {
                        RegistryKey<Biome> biomeKey = biomeRegistryEntry.getKey().orElse(null);
                        if (biomeKey != null) {
                            Identifier biomeId = biomeKey.getValue();
                            if (biomeId != null) {
                                return biomeId.toString();
                            }
                        }
                    }
                }
                return "";
            }
            case "pos.dim" -> {
                if (p.getEntityWorld() != null) {
                    RegistryKey<?> dimKey = p.getEntityWorld().getRegistryKey();
                    if (dimKey != null) {
                        Identifier dimIdentifier = dimKey.getValue();
                        if (dimIdentifier != null) {
                            return dimIdentifier.toString();
                        }
                    }
                }
                return "";
            }
            case "pos.light" -> {
                if (p.getEntityWorld() != null) {
                    return Integer.toString(p.getEntityWorld().getLightLevel(p.getBlockPos()));
                }
                return "0";
            }
            case "pos.facing" -> {
                return p.getHorizontalFacing().asString();
            }
            case "pos.facing.short", "dir.compass.short" -> {
                return yawToCompass(p.getYaw(), true);
            }
            case "dir.compass", "dir.facing" -> {
                return yawToCompass(p.getYaw(), false);
            }
            default -> {
                // no-op
            }
        }

        if ("dim".equals(token)) {
            return dimId != null ? dimId.toString() : "";
        }

        if (lookBlock != null) {
            switch (token) {
                case "look.block.x" -> {
                    return Integer.toString(lookBlock.getX());
                }
                case "look.block.y" -> {
                    return Integer.toString(lookBlock.getY());
                }
                case "look.block.z" -> {
                    return Integer.toString(lookBlock.getZ());
                }
                case "look.block.xyz" -> {
                    return lookBlock.getX() + " " + lookBlock.getY() + " " + lookBlock.getZ();
                }
                case "look.block.id" -> {
                    if (p.getEntityWorld() != null) {
                        return p.getEntityWorld().getBlockState(lookBlock).getBlock().toString();
                    }
                    return "";
                }
                case "look.block.light" -> {
                    if (p.getEntityWorld() != null) {
                        return Integer.toString(p.getEntityWorld().getLightLevel(lookBlock));
                    }
                    return "0";
                }
                default -> {
                    // no-op
                }
            }
        }

        if (lookEntity != null) {
            switch (token) {
                case "look.entity.name" -> {
                    return lookEntity.getName().getString();
                }
                case "look.entity.uuid" -> {
                    return lookEntity.getUuidAsString();
                }
                case "look.entity.id" -> {
                    return Integer.toString(lookEntity.getId());
                }
                case "look.entity.type" -> {
                    return lookEntity.getType().toString();
                }
                default -> {
                    // no-op
                }
            }
        }

        if (token.startsWith("look.dir")) {
            Vec3d vec = p.getRotationVec(1.0f).normalize();
            switch (token) {
                case "look.dir.x" -> {
                    return formatDouble(vec.x);
                }
                case "look.dir.y" -> {
                    return formatDouble(vec.y);
                }
                case "look.dir.z" -> {
                    return formatDouble(vec.z);
                }
                default -> {
                    // no-op
                }
            }
        }

        if (token.startsWith("hand")) {
            ItemStack hand = p.getMainHandStack();

            switch (token) {
                case "hand.item" -> {
                    return hand.getName().getString();
                }
                case "hand.id" -> {
                    // getTranslationKey is @VisibleForTesting in mappings; use a safe fallback
                    return hand.getItem().toString(); // e.g. "minecraft:diamond_sword"
                }
                case "hand.count" -> {
                    return Integer.toString(hand.getCount());
                }
                case "hand.damage" -> {
                    return Integer.toString(hand.getDamage());
                }
                case "hand.max_damage" -> {
                    return Integer.toString(hand.getMaxDamage());
                }
                case "hand.durability" -> {
                    int dmg = hand.getDamage();
                    int max = hand.getMaxDamage();
                    if (max <= 0) {
                        return "0";
                    }
                    return Integer.toString(max - dmg);
                }
                default -> {
                    // no-op
                }
            }
        }

        if (token.startsWith("offhand")) {
            ItemStack offhand = p.getOffHandStack();

            switch (token) {
                case "offhand.item" -> {
                    return offhand.getName().getString();
                }
                case "offhand.id" -> {
                    return offhand.getItem().toString();
                }
                case "offhand.count" -> {
                    return Integer.toString(offhand.getCount());
                }
                case "offhand.damage" -> {
                    return Integer.toString(offhand.getDamage());
                }
                case "offhand.max_damage" -> {
                    return Integer.toString(offhand.getMaxDamage());
                }
                case "offhand.durability" -> {
                    int dmg = offhand.getDamage();
                    int max = offhand.getMaxDamage();
                    if (max <= 0) {
                        return "0";
                    }
                    return Integer.toString(max - dmg);
                }
                default -> {
                    // no-op
                }
            }
        }

        return null;
    }

    private static String resolveClientAndWorldToken(String token, MinecraftClient client, PlayerEntity p) {
        if ("client.screen".equals(token)) {
            return client.currentScreen == null ? "none" : client.currentScreen.getClass().getSimpleName();
        }

        if ("client.fps".equals(token)) {
            Integer fps = resolveCurrentFps(client);
            return fps == null ? "0" : Integer.toString(Math.max(0, fps));
        }

        if ("client.server.singleplayer".equals(token)) {
            try {
                return Boolean.toString(client.isInSingleplayer());
            } catch (Exception ignored) {
                return "false";
            }
        }

        if (token.startsWith("client.server.")) {
            Object entry = invokeNoArg(client, "getCurrentServerEntry");
            if (entry == null) {
                return switch (token) {
                    case "client.server.address", "client.server.name" -> "";
                    default -> null;
                };
            }

            return switch (token) {
                case "client.server.address" -> Objects.toString(readField(entry, "address"), "");
                case "client.server.name" -> Objects.toString(readField(entry, "name"), "");
                default -> null;
            };
        }

        if (token.startsWith("world.time") || "world.day".equals(token) || "world.is_day".equals(token) || "world.is_night".equals(token)) {
            if (p.getEntityWorld() == null) {
                return "0";
            }

            long ticks = p.getEntityWorld().getTimeOfDay();
            long day = Math.floorDiv(ticks, 24000L);
            long dayTicks = Math.floorMod(ticks, 24000L);
            int hour = (int) ((dayTicks / 1000L + 6L) % 24L);
            int minute = (int) Math.floor(((dayTicks % 1000L) / 1000.0) * 60.0);
            boolean isDay = dayTicks < 12300L || dayTicks > 23850L;

            return switch (token) {
                case "world.time", "world.time.ticks" -> Long.toString(ticks);
                case "world.day", "world.time.day" -> Long.toString(day);
                case "world.time.day_ticks" -> Long.toString(dayTicks);
                case "world.time.clock" -> String.format(Locale.ROOT, "%02d:%02d", hour, minute);
                case "world.is_day" -> Boolean.toString(isDay);
                case "world.is_night" -> Boolean.toString(!isDay);
                default -> null;
            };
        }

        return null;
    }

    private static String resolveKeyStateToken(String token, MinecraftClient client) {
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

        long window = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;

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
            case "space" -> GLFW.GLFW_KEY_SPACE;
            case "tab" -> GLFW.GLFW_KEY_TAB;
            case "enter", "return" -> GLFW.GLFW_KEY_ENTER;
            case "esc", "escape" -> GLFW.GLFW_KEY_ESCAPE;
            case "shift" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "ctrl", "control" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "alt" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "up" -> GLFW.GLFW_KEY_UP;
            case "down" -> GLFW.GLFW_KEY_DOWN;
            case "left" -> GLFW.GLFW_KEY_LEFT;
            case "right" -> GLFW.GLFW_KEY_RIGHT;
            case "lmb", "mouse1" -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case "rmb", "mouse2" -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "mmb", "mouse3" -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default -> {
                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (c >= 'a' && c <= 'z') {
                        yield GLFW.GLFW_KEY_A + (c - 'a');
                    }
                    if (c >= '0' && c <= '9') {
                        yield GLFW.GLFW_KEY_0 + (c - '0');
                    }
                }
                if (key.startsWith("f")) {
                    try {
                        int fn = Integer.parseInt(key.substring(1));
                        if (fn >= 1 && fn <= 25) {
                            yield GLFW.GLFW_KEY_F1 + (fn - 1);
                        }
                    } catch (Exception ignored) {
                        // no-op
                    }
                }
                yield -1;
            }
        };
    }

    private static Integer resolveCurrentFps(MinecraftClient client) {
        try {
            Method m = MinecraftClient.class.getDeclaredMethod("getCurrentFps");
            m.setAccessible(true);
            Object value = m.invoke(null);
            if (value instanceof Integer i) {
                return i;
            }
        } catch (Exception ignored) {
            // Try instance method fallback below.
        }

        try {
            Method m = client.getClass().getMethod("getCurrentFps");
            Object value = m.invoke(client);
            if (value instanceof Integer i) {
                return i;
            }
        } catch (Exception ignored) {
            // no-op
        }
        return null;
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

    private static String resolveEntityListToken(String token, PlayerEntity p) {
        var world = p.getEntityWorld();
        if (world == null) {
            return null;
        }

        if ("entities.nearby.count".equals(token)) {
            long count = world.getOtherEntities(p, p.getBoundingBox().expand(64)).stream()
                    .filter(entity -> !(entity instanceof PlayerEntity))
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
        var nearbyEntities = world.getOtherEntities(p, p.getBoundingBox().expand(radius)).stream()
                .filter(entity -> !(entity instanceof PlayerEntity));

        if (sortByName) {
            nearbyEntities = nearbyEntities.sorted((a, b) -> a.getType().getName().getString().compareToIgnoreCase(b.getType().getName().getString()));
        } else {
            nearbyEntities = nearbyEntities.sorted(Comparator.comparingDouble(a -> a.squaredDistanceTo(p)));
        }

        List<String> names = nearbyEntities
                .map(entity -> {
                    String name = entity.getType().getName().getString();
                    if (!withDistanceFinal && !withDirectionFinal) {
                        return name;
                    }
                    List<String> extras = new ArrayList<>();
                    if (withDistanceFinal) {
                        int meters = (int) Math.round(Math.sqrt(entity.squaredDistanceTo(p)));
                        extras.add(meters + "m");
                    }
                    if (withDirectionFinal) {
                        extras.add(directionFromToShort(p.getX(), p.getZ(), entity.getX(), entity.getZ()));
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

    private static String resolvePlayerListToken(String token, PlayerEntity p) {
        var world = p.getEntityWorld();
        if (world == null) {
            return null;
        }

        if ("players.count".equals(token)) {
            return Integer.toString(world.getPlayers().size());
        }

        if ("players.count.other".equals(token)) {
            long others = world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
                    .count();
            return Long.toString(others);
        }

        if ("players.list".equals(token) || "players.list.csv".equals(token)) {
            return world.getPlayers().stream()
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining(", "));
        }

        if ("players.list.other".equals(token) || "players.list.other.csv".equals(token)) {
            return world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining(", "));
        }

        if ("players.list.nl".equals(token)) {
            return world.getPlayers().stream()
                    .map(other -> other.getGameProfile().name())
                    .collect(Collectors.joining("\n"));
        }

        if ("players.list.other.nl".equals(token)) {
            return world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
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
            var nearbyPlayers = world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
                    .filter(other -> p.squaredDistanceTo(other) <= maxRadiusSq);

            if (sortByName) {
                nearbyPlayers = nearbyPlayers.sorted((a, b) -> a.getGameProfile().name().compareToIgnoreCase(b.getGameProfile().name()));
            } else {
                nearbyPlayers = nearbyPlayers.sorted(Comparator.comparingDouble((PlayerEntity a) -> a.squaredDistanceTo(p)));
            }

            final boolean withDistanceFinal = withDistance;
            final boolean withDirectionFinal = withDirection;

            List<String> nearby = nearbyPlayers
                    .map(other -> {
                        String name = other.getGameProfile().name();
                        if (!withDistanceFinal && !withDirectionFinal) {
                            return name;
                        }
                        List<String> extras = new ArrayList<>();
                        if (withDistanceFinal) {
                            int meters = (int) Math.round(Math.sqrt(other.squaredDistanceTo(p)));
                            extras.add(meters + "m");
                        }
                        if (withDirectionFinal) {
                            extras.add(directionFromToShort(p.getX(), p.getZ(), other.getX(), other.getZ()));
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
            long count = world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
                    .count();
            return Long.toString(count);
        }

        return null;
    }

    private static String resolveSelectorToken(String token, PlayerEntity p, boolean canvasMode) {
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

        var world = p.getEntityWorld();
        if (world == null) {
            return "";
        }

        return switch (token) {
            case "sel.self" -> p.getGameProfile().name();
            case "sel.nearest" -> world.getPlayers().stream()
                    .filter(other -> !other.getUuid().equals(p.getUuid()))
                    .min(Comparator.comparingDouble((PlayerEntity a) -> a.squaredDistanceTo(p)))
                    .map(player -> player.getGameProfile().name())
                    .orElse("(none)");
            case "sel.random" -> {
                var others = world.getPlayers().stream()
                        .filter(other -> !other.getUuid().equals(p.getUuid()))
                        .toList();
                if (others.isEmpty()) {
                    yield "(none)";
                }
                yield others.get(RAND.nextInt(others.size())).getGameProfile().name();
            }
            case "sel.all" -> {
                var names = world.getPlayers().stream()
                        .filter(other -> !other.getUuid().equals(p.getUuid()))
                        .map(other -> other.getGameProfile().name())
                        .toList();
                yield names.isEmpty() ? "(none)" : String.join(", ", names);
            }
            case "sel.entities" -> world.getOtherEntities(p, p.getBoundingBox().expand(64)).stream()
                    .findFirst()
                    .map(entity -> entity.getName().getString())
                    .orElse("(none)");
            default -> null;
        };
    }

    public static List<String> PLACEHOLDER_DOCS = List.of(
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
            "{dir.compass} {dir.compass.short} => North / N (supports diagonals)",
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
            "{players.nearby.3} {.r128 .with_distance .with_direction .unique .sort=name}",
            "{entities.nearby.3} {.r64 .with_distance .with_direction .unique .sort=distance}",
            "",
            "[Item and misc]",
            "{hand.item} {hand.id} {hand.count}",
            "{hand.damage} {hand.max_damage} {hand.durability}",
            "{offhand.item} {offhand.id} {offhand.count}",
            "{offhand.damage} {offhand.max_damage} {offhand.durability}",
            "{dim} => current dimension id",
            "{rand.int(1,10)} => random integer in range",
            "",
            "[Client and server session]",
            "{client.fps} {client.screen}",
            "{client.server.singleplayer}",
            "{client.server.name} {client.server.address}",
            "",
            "[World time]",
            "{world.time} {world.time.ticks} {world.time.day} {world.day}",
            "{world.time.day_ticks} {world.time.clock}",
            "{world.is_day} {world.is_night}",
            "",
            "[Input state]",
            "{key.pressed.w} => true once when W is pressed",
            "{key.held.space} => true while Space is held",
            "",
            "[Conditionals in macro commands]",
            "if:<left>==<right>::<when_true>:else:<when_false>",
            "Example: if:{player.gamemode}==creative::cmd:/say yes:else:cmd:/say no"
    );

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }

    private static String yawToCompass(float yaw, boolean shortName) {
        String[] shortDirs = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        String[] longDirs = {"South", "South West", "West", "North West", "North", "North East", "East", "South East"};
        int idx = Math.floorMod(Math.round(yaw / 45.0f), 8);
        return shortName ? shortDirs[idx] : longDirs[idx];
    }

    private static String directionFromToShort(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        if (Math.abs(dx) < 0.0001 && Math.abs(dz) < 0.0001) {
            return "HERE";
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return yawToCompass(yaw, true);
    }
}

