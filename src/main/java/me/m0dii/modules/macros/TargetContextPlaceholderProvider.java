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

        if (token.startsWith(PETS_PREFIX)) {
            return resolvePetsToken(token, player);
        }

        return null;
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
}
