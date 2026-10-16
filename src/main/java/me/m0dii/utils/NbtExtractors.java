package me.m0dii.utils;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueOutput;

public final class NbtExtractors {
    private NbtExtractors() {
    }

    public static CompoundTag extractEntityNbt(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return null;
        }
        try {
            TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
            // 1.21.11+: entity serialization is WriteView-based
            if (!entity.save(view)) {
                return null;
            }
            CompoundTag out = view.buildResult();
            return out == null || out.isEmpty() ? null : out;
        } catch (Exception ignored) {
            // Keep callers stable in case modded entities throw while serializing.
            return null;
        }
    }

    public static CompoundTag extractBlockData(Level world, BlockPos pos) {
        CompoundTag out = new CompoundTag();
        if (world == null || pos == null) {
            return out;
        }

        BlockState state = world.getBlockState(pos);
        out.putString("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        out.putInt("x", pos.getX());
        out.putInt("y", pos.getY());
        out.putInt("z", pos.getZ());

        if (!state.getProperties().isEmpty()) {
            CompoundTag stateNbt = new CompoundTag();
            for (Property<?> property : state.getProperties()) {
                stateNbt.put(property.getName(), StringTag.valueOf(getPropertyValue(state, property)));
            }
            out.put("Properties", stateNbt);
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            // Different serializers can expose different fields. Merge as many as we can.
            mergeInto(out, safeSerialize(() -> blockEntity.saveWithFullMetadata(world.registryAccess())));
            mergeInto(out, safeSerialize(() -> blockEntity.saveWithoutMetadata(world.registryAccess())));
            mergeInto(out, safeSerialize(() -> blockEntity.getUpdateTag(world.registryAccess())));
        }

        try {
            // Chunk snapshot NBT may contain fields not present in live client BlockEntity instances.
            CompoundTag chunkNbt = world.getChunk(pos).getBlockEntityNbt(pos);
            mergeInto(out, chunkNbt);
        } catch (Exception ignored) {
            // Ignore and keep partial data.
        }
        return out;
    }

    private static void mergeInto(CompoundTag target, CompoundTag source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        for (String key : source.keySet()) {
            Tag element = source.get(key);
            if (element != null) {
                target.put(key, element.copy());
            }
        }
    }

    private static CompoundTag safeSerialize(Supplier<CompoundTag> serializer) {
        try {
            return serializer.get();
        } catch (Exception ignored) {
            // Modded block entities may throw while serializing.
            return null;
        }
    }

    private static <T extends Comparable<T>> String getPropertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public static net.minecraft.world.entity.Entity resolveEntityTarget(Level world,
                                                                  net.minecraft.world.entity.player.Player player,
                                                                  String token,
                                                                  net.minecraft.world.entity.Entity lookedEntity) {
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
            for (var entity : world.getEntities(player, player.getBoundingBox().inflate(512.0))) {
                if (uuid.equals(entity.getUUID())) {
                    return entity;
                }
            }
            if (uuid.equals(player.getUUID())) {
                return player;
            }
        } catch (Exception ignored) {
            // Not a UUID token.
        }

        return null;
    }
}
