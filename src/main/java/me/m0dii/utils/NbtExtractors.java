package me.m0dii.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.UUID;

public final class NbtExtractors {
    private NbtExtractors() {
    }

    public static NbtCompound extractEntityNbt(net.minecraft.entity.Entity entity) {
        if (entity == null) {
            return null;
        }
        try {
            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, entity.getRegistryManager());
            // 1.21.11+: entity serialization is WriteView-based
            if (!entity.saveData(view)) {
                return null;
            }
            NbtCompound out = view.getNbt();
            return out == null || out.isEmpty() ? null : out;
        } catch (Exception ignored) {
            // Keep callers stable in case modded entities throw while serializing.
            return null;
        }
    }

    public static NbtCompound extractBlockData(World world, BlockPos pos) {
        NbtCompound out = new NbtCompound();
        if (world == null || pos == null) {
            return out;
        }

        BlockState state = world.getBlockState(pos);
        out.putString("id", String.valueOf(Registries.BLOCK.getId(state.getBlock())));
        out.putInt("x", pos.getX());
        out.putInt("y", pos.getY());
        out.putInt("z", pos.getZ());

        if (!state.getProperties().isEmpty()) {
            NbtCompound stateNbt = new NbtCompound();
            for (Property<?> property : state.getProperties()) {
                stateNbt.put(property.getName(), NbtString.of(getPropertyValue(state, property)));
            }
            out.put("Properties", stateNbt);
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            // Different implementations expose different serializers. Merge as many as we can.
            mergeInto(out, safeInvoke(blockEntity, "createNbtWithIdentifyingData", world.getRegistryManager()));
            mergeInto(out, safeInvoke(blockEntity, "createNbt", world.getRegistryManager()));
            mergeInto(out, safeInvoke(blockEntity, "toInitialChunkDataNbt", world.getRegistryManager()));
            mergeInto(out, safeInvoke(blockEntity, "createNbt"));
            mergeInto(out, safeInvoke(blockEntity, "toInitialChunkDataNbt"));
        }

        try {
            // Chunk snapshot NBT may contain fields not present in live client BlockEntity instances.
            NbtCompound chunkNbt = world.getChunk(pos).getBlockEntityNbt(pos);
            mergeInto(out, chunkNbt);
        } catch (Exception ignored) {
            // Ignore and keep partial data.
        }
        return out;
    }

    private static void mergeInto(NbtCompound target, NbtCompound source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (String key : source.getKeys()) {
            NbtElement element = source.get(key);
            if (element != null) {
                target.put(key, element.copy());
            }
        }
    }

    private static NbtCompound safeInvoke(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = findMethod(target.getClass(), methodName, args.length);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            Object value = method.invoke(target, args);
            return value instanceof NbtCompound nbt ? nbt : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private static <T extends Comparable<T>> String getPropertyValue(BlockState state, Property<T> property) {
        return property.name(state.get(property));
    }

    public static net.minecraft.entity.Entity resolveEntityTarget(World world,
                                                                   net.minecraft.entity.player.PlayerEntity player,
                                                                   String token,
                                                                   net.minecraft.entity.Entity lookedEntity) {
        if (world == null || player == null || token == null || token.isBlank()) {
            return null;
        }

        String t = token.trim();
        if ("@s".equals(t) || "@p".equals(t)) {
            return player;
        }
        if ("@e".equals(t) || t.startsWith("@e[") || "@r".equals(t)) {
            return lookedEntity;
        }

        try {
            UUID uuid = UUID.fromString(t);
            for (var entity : world.getOtherEntities(player, player.getBoundingBox().expand(512.0))) {
                if (uuid.equals(entity.getUuid())) {
                    return entity;
                }
            }
            if (uuid.equals(player.getUuid())) {
                return player;
            }
        } catch (Exception ignored) {
            // Not a UUID token.
        }

        return null;
    }
}
