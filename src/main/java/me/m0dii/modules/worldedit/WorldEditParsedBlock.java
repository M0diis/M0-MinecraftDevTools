package me.m0dii.modules.worldedit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.Set;

final class WorldEditParsedBlock implements WorldEditBlockTarget {
    private final BlockInput argument;

    private WorldEditParsedBlock(BlockInput argument) {
        this.argument = argument;
    }

    static WorldEditParsedBlock parse(ServerLevel world, String input) throws CommandSyntaxException {
        BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(
                world.registryAccess().lookupOrThrow(Registries.BLOCK),
                input,
                true
        );
        return new WorldEditParsedBlock(new BlockInput(
                result.blockState(),
                Set.copyOf(result.properties().keySet()),
                result.nbt() == null ? null : result.nbt().copy()
        ));
    }

    boolean matches(ServerLevel world, BlockPos pos) {
        return this.argument.test(world, pos);
    }

    @Override
    public boolean apply(ServerLevel world, BlockPos pos) {
        var oldState = world.getBlockState(pos);
        boolean applied = this.argument.place(world, pos, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (applied) {
            world.updateNeighboursOnBlockSet(pos, oldState);
        }
        return applied;
    }
}
