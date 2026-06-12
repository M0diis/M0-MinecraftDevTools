package me.m0dii.modules.worldedit;

import me.m0dii.utils.NbtExtractors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Set;

final class WorldEditBlockSnapshot implements WorldEditBlockTarget {
    private final BlockState state;
    private final Set<Property<?>> properties;
    private final NbtCompound blockEntityNbt;

    private WorldEditBlockSnapshot(BlockState state, Set<Property<?>> properties, NbtCompound blockEntityNbt) {
        this.state = state;
        this.properties = Set.copyOf(properties);
        this.blockEntityNbt = blockEntityNbt == null ? null : blockEntityNbt.copy();
    }

    static WorldEditBlockSnapshot capture(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return new WorldEditBlockSnapshot(state, Set.copyOf(state.getProperties()), trimmedBlockEntityNbt(world, pos));
    }

    static WorldEditBlockSnapshot air() {
        BlockState state = Blocks.AIR.getDefaultState();
        return new WorldEditBlockSnapshot(state, Set.copyOf(state.getProperties()), null);
    }

    boolean matches(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).equals(this.state)) {
            return false;
        }
        return Objects.equals(trimmedBlockEntityNbt(world, pos), this.blockEntityNbt);
    }

    boolean isAir() {
        return this.state.isAir();
    }

    @Override
    public boolean apply(ServerWorld world, BlockPos pos) {
        BlockState oldState = world.getBlockState(pos);
        BlockStateArgument argument = new BlockStateArgument(this.state, this.properties, this.blockEntityNbt == null ? null : this.blockEntityNbt.copy());
        boolean applied = argument.setBlockState(world, pos, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        if (applied) {
            world.onStateReplacedWithCommands(pos, oldState);
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

    private static NbtCompound trimmedBlockEntityNbt(ServerWorld world, BlockPos pos) {
        NbtCompound data = NbtExtractors.extractBlockData(world, pos);
        if (data == null || data.isEmpty()) {
            return null;
        }

        NbtCompound trimmed = data.copy();
        trimmed.remove("id");
        trimmed.remove("Properties");
        trimmed.remove("x");
        trimmed.remove("y");
        trimmed.remove("z");
        trimmed.remove("keepPacked");
        return trimmed.isEmpty() ? null : trimmed;
    }
}
