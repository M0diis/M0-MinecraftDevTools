package me.m0dii.modules.worldedit;

import me.m0dii.utils.NbtExtractors;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.Objects;
import java.util.Set;

final class WorldEditBlockSnapshot implements WorldEditBlockTarget {
    private final BlockState state;
    private final Set<Property<?>> properties;
    private final CompoundTag blockEntityNbt;

    private WorldEditBlockSnapshot(BlockState state, Set<Property<?>> properties, CompoundTag blockEntityNbt) {
        this.state = state;
        this.properties = Set.copyOf(properties);
        this.blockEntityNbt = blockEntityNbt == null ? null : blockEntityNbt.copy();
    }

    static WorldEditBlockSnapshot capture(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return new WorldEditBlockSnapshot(state, Set.copyOf(state.getProperties()), trimmedBlockEntityNbt(world, pos));
    }

    static WorldEditBlockSnapshot air() {
        BlockState state = Blocks.AIR.defaultBlockState();
        return new WorldEditBlockSnapshot(state, Set.copyOf(state.getProperties()), null);
    }

    boolean matches(ServerLevel world, BlockPos pos) {
        if (!world.getBlockState(pos).equals(this.state)) {
            return false;
        }
        return Objects.equals(trimmedBlockEntityNbt(world, pos), this.blockEntityNbt);
    }

    boolean isAir() {
        return this.state.isAir();
    }

    @Override
    public boolean apply(ServerLevel world, BlockPos pos) {
        BlockState oldState = world.getBlockState(pos);
        BlockInput argument = new BlockInput(this.state, this.properties, this.blockEntityNbt == null ? null : this.blockEntityNbt.copy());
        boolean applied = argument.place(world, pos, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (applied) {
            world.updateNeighboursOnBlockSet(pos, oldState);
        }
        return applied;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorldEditBlockSnapshot other)) {
            return false;
        }
        return this.state.equals(other.state) && Objects.equals(this.blockEntityNbt, other.blockEntityNbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.state, this.blockEntityNbt);
    }

    private static CompoundTag trimmedBlockEntityNbt(ServerLevel world, BlockPos pos) {
        CompoundTag data = NbtExtractors.extractBlockData(world, pos);
        if (data == null || data.isEmpty()) {
            return null;
        }

        CompoundTag trimmed = data.copy();
        trimmed.remove("id");
        trimmed.remove("Properties");
        trimmed.remove("x");
        trimmed.remove("y");
        trimmed.remove("z");
        trimmed.remove("keepPacked");
        return trimmed.isEmpty() ? null : trimmed;
    }
}
