package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.util.BlockStateProperties;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetBlockCommand extends ClientCommand {

    @Override
    public String getName() {
        return "block";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        Command<FabricClientCommandSource> getBlock = context -> {
            PosArgument posArg = getDefaultArg(context, "pos", null, PosArgument.class);
            BlockPos pos = (posArg == null ? null : posArg.toAbsoluteBlockPos(MVMisc.getCommandSource(context.getSource().getPlayer())));
            if (pos != null && !MiscUtil.client.world.isInBuildLimit(pos)) {
                throw BlockPosArgumentType.OUT_OF_WORLD_EXCEPTION.create();
            }
            BlockStateArgument blockArg = context.getArgument("block", BlockStateArgument.class);
            NbtCompound nbt = blockArg.data;
            if (nbt == null) {
                nbt = new NbtCompound();
            }
            LocalBlock block = new LocalBlock(blockArg.getBlockState().getBlock(), new BlockStateProperties(blockArg.getBlockState()), nbt);

            if (pos == null) {
                block.toItem().ifPresentOrElse(MiscUtil::getWithMessage,
                        () -> MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.export.item.error"), false));
            } else if (M0DevToolsClient.SERVER_CONN.isEditingExpanded()) {
                block.place(pos);
            } else {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.requires_server"), false);
            }

            return Command.SINGLE_SUCCESS;
        };

        builder.then(argument("block", MVMisc.getBlockStateArg()).executes(getBlock))
                .then(argument("pos", BlockPosArgumentType.blockPos()).then(argument("block", MVMisc.getBlockStateArg()).executes(getBlock)));
    }

}
