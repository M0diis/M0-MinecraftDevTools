package me.m0dii.modules.worldedit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

final class WorldEditParsedBlock implements WorldEditBlockTarget {
    private final BlockStateArgument argument;

    private WorldEditParsedBlock(BlockStateArgument argument) {
        this.argument = argument;
    }

    static WorldEditParsedBlock parse(ServerWorld world, String input) throws CommandSyntaxException {
        BlockArgumentParser.BlockResult result = BlockArgumentParser.block(
                world.getRegistryManager().getOrThrow(RegistryKeys.BLOCK),
                input,
                true
        );
        return new WorldEditParsedBlock(new BlockStateArgument(
                result.blockState(),
                Set.copyOf(result.properties().keySet()),
                result.nbt() == null ? null : result.nbt().copy()
        ));
    }

    boolean matches(ServerWorld world, BlockPos pos) {
        return this.argument.test(world, pos);
    }

    @Override
    public boolean apply(ServerWorld world, BlockPos pos) {
        var oldState = world.getBlockState(pos);
        boolean applied = this.argument.setBlockState(world, pos, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        if (applied) {
            world.onStateReplacedWithCommands(pos, oldState);
        }
        return applied;
    }
}
