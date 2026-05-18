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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MacroPlaceholders {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");
    private static final Random RAND = new Random();

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
            String replacement = resolveToken(token, p, world != null ? world.getRegistryKey().getValue() : null, lookBlock, lookEntity, canvasMode);
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

    private static String resolveToken(String token, PlayerEntity p, Identifier dimId, BlockPos lookBlock, Entity lookEntity, boolean canvasMode) {
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
        var nearbyEntities = world.getOtherEntities(p, p.getBoundingBox().expand(radius)).stream()
                .filter(entity -> !(entity instanceof PlayerEntity));

        if (sortByName) {
            nearbyEntities = nearbyEntities.sorted((a, b) -> a.getType().getName().getString().compareToIgnoreCase(b.getType().getName().getString()));
        } else {
            nearbyEntities = nearbyEntities.sorted((a, b) -> Double.compare(a.squaredDistanceTo(p), b.squaredDistanceTo(p)));
        }

        List<String> names = nearbyEntities
                .map(entity -> {
                    String name = entity.getType().getName().getString();
                    if (!withDistanceFinal) {
                        return name;
                    }
                    int meters = (int) Math.round(Math.sqrt(entity.squaredDistanceTo(p)));
                    return name + " (" + meters + "m)";
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
                nearbyPlayers = nearbyPlayers.sorted((a, b) -> Double.compare(a.squaredDistanceTo(p), b.squaredDistanceTo(p)));
            }

            final boolean withDistanceFinal = withDistance;

            List<String> nearby = nearbyPlayers
                    .map(other -> {
                        String name = other.getGameProfile().name();
                        if (!withDistanceFinal) {
                            return name;
                        }
                        int meters = (int) Math.round(Math.sqrt(other.squaredDistanceTo(p)));
                        return name + " (" + meters + "m)";
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
                    .min((a, b) -> Double.compare(a.squaredDistanceTo(p), b.squaredDistanceTo(p)))
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

    private static String formatDouble(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }
}

