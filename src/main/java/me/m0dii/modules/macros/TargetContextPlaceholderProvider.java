package me.m0dii.modules.macros;

import me.m0dii.utils.NbtExtractors;
import me.m0dii.utils.ReflectionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TargetContextPlaceholderProvider implements MacroPlaceholderProvider {
    private static final TargetContextPlaceholderProvider INSTANCE = new TargetContextPlaceholderProvider();
    private static final double PET_SCAN_RADIUS = 64.0;
    private static final String TARGET_PLAYER_PREFIX = "target.player.";
    private static final String TARGET_ENTITY_PREFIX = "target.entity.";
    private static final String TARGET_BLOCK_PREFIX = "target.block.";
    private static final String QUERY_PLAYER_PREFIX = "query.player(";
    private static final String QUERY_ENTITY_PREFIX = "query.entity(";
    private static final String QUERY_BLOCK_PREFIX = "query.block(";
    private static final String PETS_PREFIX = "pets.";
    private static final String NBT_PREFIX = "nbt.";
    private static final String COMPONENT_PREFIX = "component:";

    private TargetContextPlaceholderProvider() {
    }

    public static void register() {
        MacroPlaceholders.registerProvider(INSTANCE);
    }

    @Override
    public String getProviderId() {
        return "target_context";
    }

    @Override
    public List<String> getPlaceholderDocs() {
        return List.of(
                "[Target context]",
                "{target.entity.name} {target.entity.type} {target.entity.distance}",
                "{target.entity.health} {target.entity.max_health} {target.entity.health_percent}",
                "{target.entity.uuid} {target.entity.x} {target.entity.y} {target.entity.z}",
                "{target.entity.velocity} {target.entity.velocity.x} {target.entity.velocity.y} {target.entity.velocity.z}",
                "{target.entity.age} {target.entity.is_hostile} {target.entity.is_passive} {target.entity.is_baby}",
                "{target.entity.custom_name}",
                "{target.entity.nbt} {target.entity.nbt.Pos.0}",
                "{target.entity.components} {target.entity.component:minecraft:custom_name}",
                "{target.player.name} {target.player.uuid}",
                "{target.player.nbt} {target.player.component:minecraft:custom_name}",
                "",
                "{target.block.name} {target.block.id} {target.block.state}",
                "{target.block.x} {target.block.y} {target.block.z}",
                "{target.block.light_level} {target.block.hardness} {target.block.blast_resistance}",
                "{target.block.nbt} {target.block.nbt.Items}",
                "{target.block.components} {target.block.component:minecraft:container}",
                "",
                "[Query selectors and nearby blocks]",
                "{query.player(@p).name} {query.player(@a[distance=..16,sort=nearest,limit=1]).uuid}",
                "{query.player(@a[distance=..32]).count} {query.player(@a[distance=..32]).names}",
                "{query.entity(@e[type=minecraft:zombie,sort=nearest,limit=1]).health}",
                "{query.entity(@e[type=minecraft:item,distance=..5]).count}",
                "{query.block(below).id} {query.block(above).state}",
                "{query.block(~ ~-1 ~).id} {query.block(~,~1,~).name}",
                "Selector subset: @s @p @r @a @e with [type= name= uuid= distance= sort= limit=]",
                "",
                "{pets.count} {pets.names} {pets.types}",
                "{pets.nearest.name} {pets.nearest.type} {pets.nearest.distance}",
                "Pets include nearby tamed followers owned by you and exclude sitting pets."
        );
    }

    @Override
    public List<String> getKnownPlaceholderTokens() {
        return List.of(
                "target.entity.name",
                "target.entity.health",
                "target.entity.max_health",
                "target.entity.health_percent",
                "target.entity.type",
                "target.entity.distance",
                "target.entity.uuid",
                "target.entity.x",
                "target.entity.y",
                "target.entity.z",
                "target.entity.velocity",
                "target.entity.velocity.x",
                "target.entity.velocity.y",
                "target.entity.velocity.z",
                "target.entity.age",
                "target.entity.is_hostile",
                "target.entity.is_passive",
                "target.entity.is_baby",
                "target.entity.custom_name",
                "target.entity.nbt",
                "target.entity.nbt.Pos.0",
                "target.entity.components",
                "target.entity.component:minecraft:custom_name",
                "target.player.name",
                "target.player.uuid",
                "target.player.nbt",
                "target.player.component:minecraft:custom_name",
                "target.block.name",
                "target.block.id",
                "target.block.x",
                "target.block.y",
                "target.block.z",
                "target.block.state",
                "target.block.light_level",
                "target.block.hardness",
                "target.block.blast_resistance",
                "target.block.nbt",
                "target.block.nbt.Items",
                "target.block.components",
                "target.block.component:minecraft:container",
                "query.player(@p).name",
                "query.player(@a[distance=..32]).count",
                "query.player(@a[distance=..32]).names",
                "query.entity(@e[type=minecraft:zombie,sort=nearest,limit=1]).health",
                "query.entity(@e[type=minecraft:item,distance=..5]).count",
                "query.entity(@e[type=minecraft:item,distance=..5]).names",
                "query.block(below).id",
                "query.block(above).state",
                "query.block(~ ~-1 ~).id",
                "query.block(~,~1,~).name",
                "pets.count",
                "pets.names",
                "pets.types",
                "pets.nearest.name",
                "pets.nearest.type",
                "pets.nearest.distance"
        );
    }

    @Override
    public String resolvePlaceholder(String token, Minecraft client, Player player, boolean canvasMode) {
        if (token == null || token.isBlank() || client == null || player == null || client.level == null) {
            return null;
        }

        if (token.startsWith(TARGET_PLAYER_PREFIX)) {
            Entity entity = targetedEntity(client);
            if (!(entity instanceof Player targetPlayer)) {
                return null;
            }
            return resolveTargetPlayerToken(token.substring(TARGET_PLAYER_PREFIX.length()), targetPlayer, player);
        }

        if (token.startsWith(TARGET_ENTITY_PREFIX)) {
            Entity entity = targetedEntity(client);
            if (entity == null) {
                return null;
            }
            return resolveTargetEntityToken(token.substring(TARGET_ENTITY_PREFIX.length()), entity, player);
        }

        if (token.startsWith(TARGET_BLOCK_PREFIX)) {
            BlockPos pos = targetedBlock(client);
            if (pos == null) {
                return null;
            }
            return resolveTargetBlockToken(token.substring(TARGET_BLOCK_PREFIX.length()), client.level, pos);
        }

        String queryValue = resolveQueryToken(token, client.level, player);
        if (queryValue != null) {
            return queryValue;
        }

        if (token.startsWith(PETS_PREFIX)) {
            return resolvePetsToken(token, player);
        }

        return null;
    }

    private static String resolveQueryToken(String token, Level world, Player player) {
        QueryToken playerQuery = parseQueryToken(token, QUERY_PLAYER_PREFIX);
        if (playerQuery != null) {
            return resolveQueryPlayerToken(playerQuery, world, player);
        }

        QueryToken entityQuery = parseQueryToken(token, QUERY_ENTITY_PREFIX);
        if (entityQuery != null) {
            return resolveQueryEntityToken(entityQuery, world, player);
        }

        QueryToken blockQuery = parseQueryToken(token, QUERY_BLOCK_PREFIX);
        if (blockQuery != null) {
            BlockPos pos = resolveQueryBlockPos(blockQuery.argument(), player);
            if (pos == null) {
                return null;
            }
            return resolveTargetBlockToken(blockQuery.key(), world, pos);
        }

        return null;
    }

    private static String resolveQueryPlayerToken(QueryToken query, Level world, Player sourcePlayer) {
        List<Entity> matches = resolveSelectorMatches(query.argument(), world, sourcePlayer, true);
        String aggregate = resolveAggregateEntityQuery(query.key(), matches, true);
        if (aggregate != null) {
            return aggregate;
        }
        if (matches.isEmpty() || !(matches.getFirst() instanceof Player targetPlayer)) {
            return null;
        }
        return resolveTargetPlayerToken(query.key(), targetPlayer, sourcePlayer);
    }

    private static String resolveQueryEntityToken(QueryToken query, Level world, Player sourcePlayer) {
        List<Entity> matches = resolveSelectorMatches(query.argument(), world, sourcePlayer, false);
        String aggregate = resolveAggregateEntityQuery(query.key(), matches, false);
        if (aggregate != null) {
            return aggregate;
        }
        if (matches.isEmpty()) {
            return null;
        }
        return resolveTargetEntityToken(query.key(), matches.getFirst(), sourcePlayer);
    }

    private static Entity targetedEntity(Minecraft client) {
        HitResult hitResult = client.hitResult;
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }
        return null;
    }

    private static BlockPos targetedBlock(Minecraft client) {
        HitResult hitResult = client.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }
        return null;
    }

    private static QueryToken parseQueryToken(String token, String prefix) {
        if (token == null || !token.startsWith(prefix)) {
            return null;
        }
        int close = token.lastIndexOf(").");
        if (close < prefix.length()) {
            return null;
        }
        String argument = token.substring(prefix.length(), close).trim();
        String key = token.substring(close + 2).trim();
        if (argument.isBlank() || key.isBlank()) {
            return null;
        }
        return new QueryToken(argument, key);
    }

    private static List<Entity> resolveSelectorMatches(String rawSelector, Level world, Player sourcePlayer, boolean playersOnly) {
        SelectorQuery query = parseSelectorQuery(rawSelector);
        if (query == null || world == null || sourcePlayer == null) {
            return List.of();
        }

        List<Entity> candidates = new ArrayList<>(baseSelectorCandidates(query, world, sourcePlayer, playersOnly));
        candidates.removeIf(entity -> !matchesSelectorQuery(entity, sourcePlayer, query, playersOnly));
        sortSelectorCandidates(candidates, query, sourcePlayer);

        int limit = resolveSelectorLimit(query);
        if (limit >= 0 && candidates.size() > limit) {
            return List.copyOf(candidates.subList(0, limit));
        }
        return List.copyOf(candidates);
    }

    private static List<Entity> baseSelectorCandidates(SelectorQuery query, Level world, Player sourcePlayer, boolean playersOnly) {
        if (query.directName() != null) {
            String wanted = query.directName().trim();
            if (wanted.isBlank()) {
                return List.of();
            }

            List<Entity> direct = new ArrayList<>();
            for (Player player : world.players()) {
                if (player.getGameProfile().name().equalsIgnoreCase(wanted) || player.getStringUUID().equalsIgnoreCase(wanted)) {
                    direct.add(player);
                }
            }
            if (!playersOnly) {
                for (Entity entity : world.getEntities(sourcePlayer, sourcePlayer.getBoundingBox().inflate(512.0))) {
                    if (entity.getName().getString().equalsIgnoreCase(wanted) || entity.getStringUUID().equalsIgnoreCase(wanted)) {
                        direct.add(entity);
                    }
                }
            }
            return direct;
        }

        List<Entity> allPlayers = new ArrayList<>(world.players());
        return switch (query.baseSelector()) {
            case 's' -> List.of(sourcePlayer);
            case 'p', 'a', 'r' -> playersOnly ? allPlayers : new ArrayList<>(allPlayers);
            case 'e' -> {
                List<Entity> entities = new ArrayList<>(allPlayers);
                entities.addAll(world.getEntities(sourcePlayer, sourcePlayer.getBoundingBox().inflate(512.0)));
                if (playersOnly) {
                    entities.removeIf(entity -> !(entity instanceof Player));
                }
                yield entities;
            }
            default -> List.of();
        };
    }

    private static boolean matchesSelectorQuery(Entity entity, Player sourcePlayer, SelectorQuery query, boolean playersOnly) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (playersOnly && !(entity instanceof Player)) {
            return false;
        }
        if (query.directName() != null) {
            return true;
        }

        for (Map.Entry<String, String> entry : query.arguments().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            switch (key) {
                case "type" -> {
                    if (!matchesEntityType(entity, value, playersOnly)) {
                        return false;
                    }
                }
                case "name" -> {
                    if (!entity.getName().getString().equalsIgnoreCase(unquote(value))) {
                        return false;
                    }
                }
                case "uuid" -> {
                    if (!entity.getStringUUID().equalsIgnoreCase(unquote(value))) {
                        return false;
                    }
                }
                case "distance" -> {
                    NumericRange range = parseNumericRange(value);
                    if (range == null || !range.matches(entity.distanceTo(sourcePlayer))) {
                        return false;
                    }
                }
                case "sort", "limit" -> {
                    // Applied after filtering.
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchesEntityType(Entity entity, String rawType, boolean playersOnly) {
        String normalized = unquote(rawType).trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        if ("player".equals(normalized) || "minecraft:player".equals(normalized)) {
            return entity instanceof Player;
        }
        if (playersOnly) {
            return false;
        }
        String typeId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())).toLowerCase(Locale.ROOT);
        if (typeId.equals(normalized)) {
            return true;
        }
        return !normalized.contains(":") && typeId.equals("minecraft:" + normalized);
    }

    private static void sortSelectorCandidates(List<Entity> candidates, SelectorQuery query, Player sourcePlayer) {
        String sortMode = query.arguments().getOrDefault("sort", defaultSelectorSort(query.baseSelector()));
        String normalized = sortMode == null ? "" : unquote(sortMode).trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "nearest", "distance" -> candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(sourcePlayer)));
            case "furthest" -> candidates.sort(Comparator.comparingDouble((Entity entity) -> entity.distanceToSqr(sourcePlayer)).reversed());
            case "random" -> Collections.shuffle(candidates);
            case "name" -> candidates.sort(Comparator.comparing(entity -> entity.getName().getString(), String.CASE_INSENSITIVE_ORDER));
            case "", "arbitrary" -> {
            }
            default -> candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(sourcePlayer)));
        }
    }

    private static int resolveSelectorLimit(SelectorQuery query) {
        String rawLimit = query.arguments().get("limit");
        if (rawLimit != null && !rawLimit.isBlank()) {
            try {
                int parsed = Integer.parseInt(unquote(rawLimit).trim());
                return Math.max(parsed, 0);
            } catch (NumberFormatException ignored) {
                return defaultSelectorLimit(query.baseSelector());
            }
        }
        return defaultSelectorLimit(query.baseSelector());
    }

    private static String defaultSelectorSort(char baseSelector) {
        return switch (baseSelector) {
            case 'p' -> "nearest";
            case 'r' -> "random";
            default -> "arbitrary";
        };
    }

    private static int defaultSelectorLimit(char baseSelector) {
        return switch (baseSelector) {
            case 's', 'p', 'r' -> 1;
            default -> -1;
        };
    }

    private static SelectorQuery parseSelectorQuery(String rawSelector) {
        if (rawSelector == null) {
            return null;
        }
        String trimmed = rawSelector.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (!trimmed.startsWith("@")) {
            return new SelectorQuery((char) 0, trimmed, Map.of());
        }
        if (trimmed.length() < 2) {
            return null;
        }

        char baseSelector = Character.toLowerCase(trimmed.charAt(1));
        if ("sprae".indexOf(baseSelector) < 0) {
            return null;
        }
        if (trimmed.length() == 2) {
            return new SelectorQuery(baseSelector, null, Map.of());
        }
        if (trimmed.charAt(2) != '[' || !trimmed.endsWith("]")) {
            return null;
        }

        String inside = trimmed.substring(3, trimmed.length() - 1);
        Map<String, String> arguments = parseSelectorArguments(inside);
        if (arguments == null) {
            return null;
        }
        return new SelectorQuery(baseSelector, null, arguments);
    }

    private static Map<String, String> parseSelectorArguments(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            return Map.of();
        }

        Map<String, String> arguments = new LinkedHashMap<>();
        int start = 0;
        int quote = 0;
        for (int i = 0; i <= rawArguments.length(); i++) {
            boolean atEnd = i == rawArguments.length();
            char ch = atEnd ? ',' : rawArguments.charAt(i);
            if (!atEnd && (ch == '"' || ch == '\'')) {
                if (quote == 0) {
                    quote = ch;
                } else if (quote == ch && rawArguments.charAt(i - 1) != '\\') {
                    quote = 0;
                }
            }
            if (quote == 0 && (atEnd || ch == ',')) {
                String entry = rawArguments.substring(start, i).trim();
                if (!entry.isEmpty()) {
                    int equals = entry.indexOf('=');
                    if (equals <= 0 || equals >= entry.length() - 1) {
                        return null;
                    }
                    arguments.put(entry.substring(0, equals).trim().toLowerCase(Locale.ROOT), entry.substring(equals + 1).trim());
                }
                start = i + 1;
            }
        }
        return arguments;
    }

    private static String resolveAggregateEntityQuery(String key, List<Entity> matches, boolean playersOnly) {
        return switch (key) {
            case "count" -> Integer.toString(matches.size());
            case "names" -> joinOrNone(matches.stream().map(entity -> entity.getName().getString()).toList());
            case "uuids" -> joinOrNone(matches.stream().map(Entity::getStringUUID).toList());
            case "types" -> playersOnly
                    ? joinOrNone(matches.stream().map(entity -> "minecraft:player").toList())
                    : joinOrNone(matches.stream().map(entity -> String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))).toList());
            default -> null;
        };
    }

    private static BlockPos resolveQueryBlockPos(String raw, Player player) {
        if (raw == null || player == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "feet", "self", "here" -> player.blockPosition();
            case "below" -> player.blockPosition().below();
            case "above", "head" -> player.blockPosition().above();
            default -> parseRelativeBlockPos(raw, player);
        };
    }

    private static BlockPos parseRelativeBlockPos(String raw, Player player) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isBlank()) {
            return null;
        }
        String[] parts = normalized.contains(",")
                ? normalized.split("\\s*,\\s*")
                : normalized.split("\\s+");
        if (parts.length != 3) {
            return null;
        }

        Double x = parseCoordinate(parts[0], player.getX());
        Double y = parseCoordinate(parts[1], player.getY());
        Double z = parseCoordinate(parts[2], player.getZ());
        if (x == null || y == null || z == null) {
            return null;
        }
        return BlockPos.containing(x, y, z);
    }

    private static Double parseCoordinate(String raw, double base) {
        if (raw == null) {
            return null;
        }
        String token = raw.trim();
        if (token.isBlank()) {
            return null;
        }
        try {
            if (token.startsWith("~")) {
                return token.length() == 1 ? base : base + Double.parseDouble(token.substring(1));
            }
            return Double.parseDouble(token);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static NumericRange parseNumericRange(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = unquote(raw).trim();
        if (normalized.isBlank()) {
            return null;
        }
        try {
            if (normalized.contains("..")) {
                String[] parts = normalized.split("\\.\\.", -1);
                if (parts.length != 2) {
                    return null;
                }
                Double min = parts[0].isBlank() ? null : Double.parseDouble(parts[0]);
                Double max = parts[1].isBlank() ? null : Double.parseDouble(parts[1]);
                return new NumericRange(min, max);
            }
            double exact = Double.parseDouble(normalized);
            return new NumericRange(exact, exact);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String unquote(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw == null ? "" : raw;
        }
        char first = raw.charAt(0);
        char last = raw.charAt(raw.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String resolveTargetPlayerToken(String key, Player targetPlayer, Player sourcePlayer) {
        String entityValue = resolveTargetEntityToken(key, targetPlayer, sourcePlayer);
        if (entityValue != null) {
            return entityValue;
        }

        return switch (key) {
            case "uuid" -> targetPlayer.getStringUUID();
            default -> null;
        };
    }

    private static String resolveTargetEntityToken(String key, Entity entity, Player player) {
        String nbtValue = resolveNbtToken(key, NbtExtractors.extractEntityNbt(entity));
        if (nbtValue != null) {
            return nbtValue;
        }

        String componentValue = resolveComponentToken(key, entity, resolveAvailableComponentIds(entity));
        if (componentValue != null) {
            return componentValue;
        }

        return switch (key) {
            case "name" -> entity.getName().getString();
            case "health" -> entity instanceof LivingEntity living ? formatDouble(living.getHealth()) : null;
            case "max_health" -> entity instanceof LivingEntity living ? formatDouble(living.getMaxHealth()) : null;
            case "health_percent" -> entity instanceof LivingEntity living
                    ? formatDouble(percent(living.getHealth(), living.getMaxHealth()))
                    : null;
            case "type" -> String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
            case "distance" -> formatDouble(entity.distanceTo(player));
            case "uuid" -> entity.getStringUUID();
            case "x" -> formatDouble(entity.getX());
            case "y" -> formatDouble(entity.getY());
            case "z" -> formatDouble(entity.getZ());
            case "velocity" -> formatVector(entity.getDeltaMovement());
            case "velocity.x" -> formatDouble(entity.getDeltaMovement().x);
            case "velocity.y" -> formatDouble(entity.getDeltaMovement().y);
            case "velocity.z" -> formatDouble(entity.getDeltaMovement().z);
            case "age" -> Integer.toString(resolveEntityAge(entity));
            case "is_hostile" -> Boolean.toString(entity instanceof Enemy);
            case "is_passive" -> Boolean.toString(isPassiveEntity(entity));
            case "is_baby" -> Boolean.toString(isBabyEntity(entity));
            case "custom_name" -> entity.getCustomName() == null ? "" : entity.getCustomName().getString();
            default -> null;
        };
    }

    private static String resolveTargetBlockToken(String key, Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String baseValue = switch (key) {
            case "name" -> state.getBlock().getName().getString();
            case "id" -> String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            case "x" -> Integer.toString(pos.getX());
            case "y" -> Integer.toString(pos.getY());
            case "z" -> Integer.toString(pos.getZ());
            case "state" -> formatBlockState(state);
            case "light_level" -> Integer.toString(world.getMaxLocalRawBrightness(pos));
            case "hardness" -> formatDouble(state.getDestroySpeed(world, pos));
            case "blast_resistance" -> formatDouble(state.getBlock().getExplosionResistance());
            default -> null;
        };
        if (baseValue != null) {
            return baseValue;
        }

        CompoundTag nbt = NbtExtractors.extractBlockData(world, pos);
        String nbtValue = resolveNbtToken(key, nbt);
        if (nbtValue != null) {
            return nbtValue;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return "components".equals(key) ? "(none)" : null;
        }

        String componentValue = resolveComponentToken(key, blockEntity.components(), resolveAvailableComponentIds(blockEntity.components()));
        if (componentValue != null) {
            return componentValue;
        }

        return null;
    }

    private static String resolvePetsToken(String token, Player player) {
        List<Entity> pets = resolveFollowingPets(player);
        if ("pets.count".equals(token)) {
            return Integer.toString(pets.size());
        }
        if ("pets.names".equals(token)) {
            return joinOrNone(pets.stream().map(entity -> entity.getName().getString()).toList());
        }
        if ("pets.types".equals(token)) {
            return joinOrNone(pets.stream().map(entity -> String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))).toList());
        }

        Entity nearest = pets.isEmpty() ? null : pets.getFirst();
        if (nearest == null) {
            return switch (token) {
                case "pets.nearest.name", "pets.nearest.type", "pets.nearest.distance" -> "";
                default -> null;
            };
        }

        return switch (token) {
            case "pets.nearest.name" -> nearest.getName().getString();
            case "pets.nearest.type" -> String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(nearest.getType()));
            case "pets.nearest.distance" -> formatDouble(nearest.distanceTo(player));
            default -> null;
        };
    }

    private static List<Entity> resolveFollowingPets(Player player) {
        if (player.level() == null) {
            return List.of();
        }

        return player.level()
                .getEntities(player, player.getBoundingBox().inflate(PET_SCAN_RADIUS))
                .stream()
                .filter(entity -> isFollowingPet(entity, player))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .toList();
    }

    private static boolean isFollowingPet(Entity entity, Player player) {
        if (entity == null || player == null || !entity.isAlive()) {
            return false;
        }

        // TamableAnimal#isTame, AbstractHorse#isTamed
        Boolean tamed = ReflectionUtils.invokeBooleanNoArg(entity, "isTame");
        if (tamed == null) {
            tamed = ReflectionUtils.invokeBooleanNoArg(entity, "isTamed");
        }
        if (!Boolean.TRUE.equals(tamed)) {
            return false;
        }

        UUID ownerUuid = resolveOwnerUuid(entity);
        if (ownerUuid == null || !ownerUuid.equals(player.getUUID())) {
            return false;
        }

        return !Boolean.TRUE.equals(ReflectionUtils.invokeBooleanNoArg(entity, "isOrderedToSit"))
                && !Boolean.TRUE.equals(ReflectionUtils.invokeBooleanNoArg(entity, "isInSittingPose"));
    }

    private static UUID resolveOwnerUuid(Entity entity) {
        Object owner = ReflectionUtils.invokeNoArg(entity, "getOwner");
        if (owner instanceof Entity ownerEntity) {
            return ownerEntity.getUUID();
        }

        Object ownerReference = ReflectionUtils.invokeNoArg(entity, "getOwnerReference");
        if (ownerReference != null) {
            Object referencedUuid = ReflectionUtils.invokeNoArg(ownerReference, "getUUID");
            if (referencedUuid instanceof UUID uuid) {
                return uuid;
            }
        }

        return null;
    }

    private static int resolveEntityAge(Entity entity) {
        Integer reflectedAge = ReflectionUtils.readIntField(entity, "age");
        if (reflectedAge != null) {
            return reflectedAge;
        }

        CompoundTag nbt = NbtExtractors.extractEntityNbt(entity);
        if (nbt != null && nbt.contains("Age")) {
            try {
                return nbt.getInt("Age").orElse(0);
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static boolean isPassiveEntity(Entity entity) {
        return entity instanceof AgeableMob || entity instanceof Animal;
    }

    private static boolean isBabyEntity(Entity entity) {
        Boolean reflected = ReflectionUtils.invokeBooleanNoArg(entity, "isBaby");
        if (reflected != null) {
            return reflected;
        }

        CompoundTag nbt = NbtExtractors.extractEntityNbt(entity);
        if (nbt != null && nbt.contains("Age")) {
            try {
                return nbt.getInt("Age").orElse(0) < 0;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private static String formatBlockState(BlockState state) {
        String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (state.getProperties().isEmpty()) {
            return id;
        }

        List<String> parts = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            parts.add(property.getName() + "=" + propertyValue(state, property));
        }
        return id + "[" + String.join(",", parts) + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<?> property) {
        Property<T> typed = (Property<T>) property;
        return typed.getName(state.getValue(typed));
    }

    private static String resolveNbtValue(CompoundTag root, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Tag current = root;
        for (String part : path.split("\\.")) {
            if (part.isBlank()) {
                return null;
            }

            if (current instanceof CompoundTag compound) {
                current = compound.get(part);
                if (current == null) {
                    return null;
                }
                continue;
            }

            if (current instanceof ListTag list) {
                int index = parseIndex(part);
                if (index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
                continue;
            }

            return null;
        }
        if (current instanceof StringTag string) {
            return string.asString().orElse("");
        }
        return current.toString();
    }

    private static String resolveNbtToken(String key, CompoundTag nbt) {
        if ("nbt".equals(key)) {
            return nbt == null || nbt.isEmpty() ? "" : nbt.toString();
        }
        if (!key.startsWith(NBT_PREFIX)) {
            return null;
        }
        if (nbt == null || nbt.isEmpty()) {
            return null;
        }
        return resolveNbtValue(nbt, key.substring(NBT_PREFIX.length()));
    }

    @SuppressWarnings({"rawtypes"})
    private static String resolveComponentValue(DataComponentGetter access, String componentId) {
        DataComponentType componentType = resolveComponentType(componentId);
        if (componentType == null) {
            return null;
        }
        Object value = access.get(componentType);
        if (value == null) {
            return null;
        }
        if (value instanceof net.minecraft.network.chat.Component text) {
            return text.getString();
        }
        return value.toString();
    }

    private static String resolveComponentToken(String key, DataComponentGetter access, List<String> componentIds) {
        if ("components".equals(key)) {
            return joinOrNone(componentIds);
        }
        if (!key.startsWith(COMPONENT_PREFIX)) {
            return null;
        }
        return resolveComponentValue(access, key.substring(COMPONENT_PREFIX.length()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> resolveAvailableComponentIds(DataComponentGetter access) {
        List<String> componentIds = new ArrayList<>();
        for (var id : BuiltInRegistries.DATA_COMPONENT_TYPE.keySet()) {
            DataComponentType componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
            if (componentType == null) {
                continue;
            }
            Object value = access.get(componentType);
            if (value != null) {
                componentIds.add(id.toString());
            }
        }
        componentIds.sort(String.CASE_INSENSITIVE_ORDER);
        return componentIds;
    }

    private static DataComponentType<?> resolveComponentType(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        var id = normalized.contains(":")
                ? net.minecraft.resources.Identifier.tryParse(normalized)
                : net.minecraft.resources.Identifier.tryParse("minecraft:" + normalized);
        if (id == null || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
    }

    private static int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String formatVector(Vec3 vector) {
        return formatDouble(vector.x) + " " + formatDouble(vector.y) + " " + formatDouble(vector.z);
    }

    private static String joinOrNone(List<String> values) {
        return values.isEmpty() ? "(none)" : String.join(", ", values);
    }

    private static double percent(double value, double max) {
        if (max <= 0.0) {
            return 0.0;
        }
        return (value / max) * 100.0;
    }

    private static String formatDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record QueryToken(String argument, String key) {
    }

    private record SelectorQuery(char baseSelector, String directName, Map<String, String> arguments) {
    }

    private record NumericRange(Double min, Double max) {
        private boolean matches(double value) {
            if (this.min != null && value < this.min) {
                return false;
            }
            return this.max == null || value <= this.max;
        }
    }
}
