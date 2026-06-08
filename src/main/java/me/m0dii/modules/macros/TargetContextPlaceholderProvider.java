package me.m0dii.modules.macros;

import me.m0dii.utils.NbtExtractors;
import me.m0dii.utils.ReflectionUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    public String resolvePlaceholder(String token, MinecraftClient client, PlayerEntity player, boolean canvasMode) {
        if (token == null || token.isBlank() || client == null || player == null || client.world == null) {
            return null;
        }

        if (token.startsWith(TARGET_PLAYER_PREFIX)) {
            Entity entity = targetedEntity(client);
            if (!(entity instanceof PlayerEntity targetPlayer)) {
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
            return resolveTargetBlockToken(token.substring(TARGET_BLOCK_PREFIX.length()), client.world, pos);
        }

        String queryValue = resolveQueryToken(token, client.world, player);
        if (queryValue != null) {
            return queryValue;
        }

        if (token.startsWith(PETS_PREFIX)) {
            return resolvePetsToken(token, player);
        }

        return null;
    }

    private static String resolveQueryToken(String token, World world, PlayerEntity player) {
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

    private static String resolveQueryPlayerToken(QueryToken query, World world, PlayerEntity sourcePlayer) {
        List<Entity> matches = resolveSelectorMatches(query.argument(), world, sourcePlayer, true);
        String aggregate = resolveAggregateEntityQuery(query.key(), matches, true);
        if (aggregate != null) {
            return aggregate;
        }
        if (matches.isEmpty() || !(matches.getFirst() instanceof PlayerEntity targetPlayer)) {
            return null;
        }
        return resolveTargetPlayerToken(query.key(), targetPlayer, sourcePlayer);
    }

    private static String resolveQueryEntityToken(QueryToken query, World world, PlayerEntity sourcePlayer) {
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

    private static Entity targetedEntity(MinecraftClient client) {
        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity();
        }
        return null;
    }

    private static BlockPos targetedBlock(MinecraftClient client) {
        HitResult hitResult = client.crosshairTarget;
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

    private static List<Entity> resolveSelectorMatches(String rawSelector, World world, PlayerEntity sourcePlayer, boolean playersOnly) {
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

    private static List<Entity> baseSelectorCandidates(SelectorQuery query, World world, PlayerEntity sourcePlayer, boolean playersOnly) {
        if (query.directName() != null) {
            String wanted = query.directName().trim();
            if (wanted.isBlank()) {
                return List.of();
            }

            List<Entity> direct = new ArrayList<>();
            for (PlayerEntity player : world.getPlayers()) {
                if (player.getGameProfile().name().equalsIgnoreCase(wanted) || player.getUuidAsString().equalsIgnoreCase(wanted)) {
                    direct.add(player);
                }
            }
            if (!playersOnly) {
                for (Entity entity : world.getOtherEntities(sourcePlayer, sourcePlayer.getBoundingBox().expand(512.0))) {
                    if (entity.getName().getString().equalsIgnoreCase(wanted) || entity.getUuidAsString().equalsIgnoreCase(wanted)) {
                        direct.add(entity);
                    }
                }
            }
            return direct;
        }

        List<Entity> allPlayers = new ArrayList<>(world.getPlayers());
        return switch (query.baseSelector()) {
            case 's' -> List.of(sourcePlayer);
            case 'p', 'a', 'r' -> playersOnly ? allPlayers : new ArrayList<>(allPlayers);
            case 'e' -> {
                List<Entity> entities = new ArrayList<>(allPlayers);
                entities.addAll(world.getOtherEntities(sourcePlayer, sourcePlayer.getBoundingBox().expand(512.0)));
                if (playersOnly) {
                    entities.removeIf(entity -> !(entity instanceof PlayerEntity));
                }
                yield entities;
            }
            default -> List.of();
        };
    }

    private static boolean matchesSelectorQuery(Entity entity, PlayerEntity sourcePlayer, SelectorQuery query, boolean playersOnly) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (playersOnly && !(entity instanceof PlayerEntity)) {
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
                    if (!entity.getUuidAsString().equalsIgnoreCase(unquote(value))) {
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
            return entity instanceof PlayerEntity;
        }
        if (playersOnly) {
            return false;
        }
        String typeId = String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType())).toLowerCase(Locale.ROOT);
        if (typeId.equals(normalized)) {
            return true;
        }
        return !normalized.contains(":") && typeId.equals("minecraft:" + normalized);
    }

    private static void sortSelectorCandidates(List<Entity> candidates, SelectorQuery query, PlayerEntity sourcePlayer) {
        String sortMode = query.arguments().getOrDefault("sort", defaultSelectorSort(query.baseSelector()));
        String normalized = sortMode == null ? "" : unquote(sortMode).trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "nearest", "distance" -> candidates.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(sourcePlayer)));
            case "furthest" -> candidates.sort(Comparator.comparingDouble((Entity entity) -> entity.squaredDistanceTo(sourcePlayer)).reversed());
            case "random" -> Collections.shuffle(candidates);
            case "name" -> candidates.sort(Comparator.comparing(entity -> entity.getName().getString(), String.CASE_INSENSITIVE_ORDER));
            case "", "arbitrary" -> {
            }
            default -> candidates.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(sourcePlayer)));
        }
    }

    private static int resolveSelectorLimit(SelectorQuery query) {
        String rawLimit = query.arguments().get("limit");
        if (rawLimit != null && !rawLimit.isBlank()) {
            try {
                int parsed = Integer.parseInt(unquote(rawLimit).trim());
                return parsed < 0 ? 0 : parsed;
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
            case "uuids" -> joinOrNone(matches.stream().map(Entity::getUuidAsString).toList());
            case "types" -> playersOnly
                    ? joinOrNone(matches.stream().map(entity -> "minecraft:player").toList())
                    : joinOrNone(matches.stream().map(entity -> String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType()))).toList());
            default -> null;
        };
    }

    private static BlockPos resolveQueryBlockPos(String raw, PlayerEntity player) {
        if (raw == null || player == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "feet", "self", "here" -> player.getBlockPos();
            case "below" -> player.getBlockPos().down();
            case "above", "head" -> player.getBlockPos().up();
            default -> parseRelativeBlockPos(raw, player);
        };
    }

    private static BlockPos parseRelativeBlockPos(String raw, PlayerEntity player) {
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
        return BlockPos.ofFloored(x, y, z);
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

    private static String resolveTargetPlayerToken(String key, PlayerEntity targetPlayer, PlayerEntity sourcePlayer) {
        String entityValue = resolveTargetEntityToken(key, targetPlayer, sourcePlayer);
        if (entityValue != null) {
            return entityValue;
        }

        return switch (key) {
            case "uuid" -> targetPlayer.getUuidAsString();
            default -> null;
        };
    }

    private static String resolveTargetEntityToken(String key, Entity entity, PlayerEntity player) {
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
            case "type" -> String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType()));
            case "distance" -> formatDouble(entity.distanceTo(player));
            case "uuid" -> entity.getUuidAsString();
            case "x" -> formatDouble(entity.getX());
            case "y" -> formatDouble(entity.getY());
            case "z" -> formatDouble(entity.getZ());
            case "velocity" -> formatVector(entity.getVelocity());
            case "velocity.x" -> formatDouble(entity.getVelocity().x);
            case "velocity.y" -> formatDouble(entity.getVelocity().y);
            case "velocity.z" -> formatDouble(entity.getVelocity().z);
            case "age" -> Integer.toString(resolveEntityAge(entity));
            case "is_hostile" -> Boolean.toString(entity instanceof Monster);
            case "is_passive" -> Boolean.toString(isPassiveEntity(entity));
            case "is_baby" -> Boolean.toString(isBabyEntity(entity));
            case "custom_name" -> entity.getCustomName() == null ? "" : entity.getCustomName().getString();
            default -> null;
        };
    }

    private static String resolveTargetBlockToken(String key, World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        String baseValue = switch (key) {
            case "name" -> state.getBlock().getName().getString();
            case "id" -> String.valueOf(Registries.BLOCK.getId(state.getBlock()));
            case "x" -> Integer.toString(pos.getX());
            case "y" -> Integer.toString(pos.getY());
            case "z" -> Integer.toString(pos.getZ());
            case "state" -> formatBlockState(state);
            case "light_level" -> Integer.toString(world.getLightLevel(pos));
            case "hardness" -> formatDouble(state.getHardness(world, pos));
            case "blast_resistance" -> formatDouble(state.getBlock().getBlastResistance());
            default -> null;
        };
        if (baseValue != null) {
            return baseValue;
        }

        NbtCompound nbt = NbtExtractors.extractBlockData(world, pos);
        String nbtValue = resolveNbtToken(key, nbt);
        if (nbtValue != null) {
            return nbtValue;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null) {
            return "components".equals(key) ? "(none)" : null;
        }

        String componentValue = resolveComponentToken(key, blockEntity.getComponents(), resolveAvailableComponentIds(blockEntity.getComponents()));
        if (componentValue != null) {
            return componentValue;
        }

        return null;
    }

    private static String resolvePetsToken(String token, PlayerEntity player) {
        List<Entity> pets = resolveFollowingPets(player);
        if ("pets.count".equals(token)) {
            return Integer.toString(pets.size());
        }
        if ("pets.names".equals(token)) {
            return joinOrNone(pets.stream().map(entity -> entity.getName().getString()).toList());
        }
        if ("pets.types".equals(token)) {
            return joinOrNone(pets.stream().map(entity -> String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType()))).toList());
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
            case "pets.nearest.type" -> String.valueOf(Registries.ENTITY_TYPE.getId(nearest.getType()));
            case "pets.nearest.distance" -> formatDouble(nearest.distanceTo(player));
            default -> null;
        };
    }

    private static List<Entity> resolveFollowingPets(PlayerEntity player) {
        if (player.getEntityWorld() == null) {
            return List.of();
        }

        return player.getEntityWorld()
                .getOtherEntities(player, player.getBoundingBox().expand(PET_SCAN_RADIUS))
                .stream()
                .filter(entity -> isFollowingPet(entity, player))
                .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)))
                .toList();
    }

    private static boolean isFollowingPet(Entity entity, PlayerEntity player) {
        if (entity == null || player == null || !entity.isAlive()) {
            return false;
        }

        Boolean tamed = ReflectionUtils.invokeBooleanNoArg(entity, "isTamed");
        if (!Boolean.TRUE.equals(tamed)) {
            return false;
        }

        UUID ownerUuid = resolveOwnerUuid(entity);
        if (ownerUuid == null || !ownerUuid.equals(player.getUuid())) {
            return false;
        }

        return !Boolean.TRUE.equals(ReflectionUtils.invokeBooleanNoArg(entity, "isSitting"))
                && !Boolean.TRUE.equals(ReflectionUtils.invokeBooleanNoArg(entity, "isInSittingPose"));
    }

    private static UUID resolveOwnerUuid(Entity entity) {
        Object ownerUuid = ReflectionUtils.invokeNoArg(entity, "getOwnerUuid");
        if (ownerUuid instanceof UUID uuid) {
            return uuid;
        }

        Object owner = ReflectionUtils.invokeNoArg(entity, "getOwner");
        if (owner instanceof Entity ownerEntity) {
            return ownerEntity.getUuid();
        }

        Object ownerReference = ReflectionUtils.invokeNoArg(entity, "getOwnerReference");
        if (ownerReference != null) {
            Object resolved = ReflectionUtils.invokeNoArg(ownerReference, "get");
            if (resolved instanceof Entity ownerEntity) {
                return ownerEntity.getUuid();
            }
        }

        return null;
    }

    private static int resolveEntityAge(Entity entity) {
        Integer reflectedAge = ReflectionUtils.readIntField(entity, "age");
        if (reflectedAge != null) {
            return reflectedAge;
        }

        NbtCompound nbt = NbtExtractors.extractEntityNbt(entity);
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
        return entity instanceof PassiveEntity || entity instanceof AnimalEntity;
    }

    private static boolean isBabyEntity(Entity entity) {
        Boolean reflected = ReflectionUtils.invokeBooleanNoArg(entity, "isBaby");
        if (reflected != null) {
            return reflected;
        }

        NbtCompound nbt = NbtExtractors.extractEntityNbt(entity);
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
        String id = String.valueOf(Registries.BLOCK.getId(state.getBlock()));
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
        return typed.name(state.get(typed));
    }

    private static String resolveNbtValue(NbtCompound root, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        NbtElement current = root;
        for (String part : path.split("\\.")) {
            if (part.isBlank()) {
                return null;
            }

            if (current instanceof NbtCompound compound) {
                current = compound.get(part);
                if (current == null) {
                    return null;
                }
                continue;
            }

            if (current instanceof NbtList list) {
                int index = parseIndex(part);
                if (index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
                continue;
            }

            return null;
        }
        if (current instanceof NbtString string) {
            return string.asString().orElse("");
        }
        return current.toString();
    }

    private static String resolveNbtToken(String key, NbtCompound nbt) {
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
    private static String resolveComponentValue(ComponentsAccess access, String componentId) {
        ComponentType componentType = resolveComponentType(componentId);
        if (componentType == null) {
            return null;
        }
        Object value = access.get(componentType);
        if (value == null) {
            return null;
        }
        if (value instanceof net.minecraft.text.Text text) {
            return text.getString();
        }
        return value.toString();
    }

    private static String resolveComponentToken(String key, ComponentsAccess access, List<String> componentIds) {
        if ("components".equals(key)) {
            return joinOrNone(componentIds);
        }
        if (!key.startsWith(COMPONENT_PREFIX)) {
            return null;
        }
        return resolveComponentValue(access, key.substring(COMPONENT_PREFIX.length()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> resolveAvailableComponentIds(ComponentsAccess access) {
        List<String> componentIds = new ArrayList<>();
        for (var id : Registries.DATA_COMPONENT_TYPE.getIds()) {
            ComponentType componentType = Registries.DATA_COMPONENT_TYPE.get(id);
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

    private static ComponentType<?> resolveComponentType(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        var id = normalized.contains(":")
                ? net.minecraft.util.Identifier.tryParse(normalized)
                : net.minecraft.util.Identifier.tryParse("minecraft:" + normalized);
        if (id == null || !Registries.DATA_COMPONENT_TYPE.containsId(id)) {
            return null;
        }
        return Registries.DATA_COMPONENT_TYPE.get(id);
    }

    private static int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String formatVector(Vec3d vector) {
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
