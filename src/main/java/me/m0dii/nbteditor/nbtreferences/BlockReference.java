package me.m0dii.nbteditor.nbtreferences;

import lombok.Getter;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.packets.GetBlockC2SPacket;
import me.m0dii.nbteditor.packets.GetLecternBlockC2SPacket;
import me.m0dii.nbteditor.packets.SetBlockC2SPacket;
import me.m0dii.nbteditor.packets.ViewBlockS2CPacket;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.BlockStateProperties;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BlockReference implements NBTReference<LocalBlock> {

    private static CompletableFuture<Optional<BlockReference>> getBlock(Function<Integer, ModPacket> packetFactory) {
        return M0DevToolsClient.SERVER_CONN
                .sendRequest(packetFactory, ViewBlockS2CPacket.class)
                .thenApply(optional -> optional.filter(ViewBlockS2CPacket::foundBlock)
                        .map(packet -> new BlockReference(packet.getWorld(), packet.getPos(),
                                MVRegistry.BLOCK.get(packet.getId()), packet.getState(), packet.getNbt())));
    }

    public static CompletableFuture<Optional<BlockReference>> getBlock(RegistryKey<World> world, BlockPos pos) {
        return getBlock(requestId -> new GetBlockC2SPacket(requestId, world, pos));
    }

    public static CompletableFuture<Optional<BlockReference>> getLecternBlock() {
        return getBlock(GetLecternBlockC2SPacket::new);
    }

    public static BlockReference getBlockWithoutNBT(BlockPos pos) {
        BlockState state = MiscUtil.client.world.getBlockState(pos);
        return new BlockReference(MiscUtil.client.world.getRegistryKey(), pos,
                state.getBlock(), new BlockStateProperties(state), new NbtCompound());
    }

    @Getter
    private final RegistryKey<World> world;
    @Getter
    private final BlockPos pos;
    @Getter
    private Block block;
    @Getter
    private BlockStateProperties state;
    private NbtCompound nbt;

    public BlockReference(RegistryKey<World> world, BlockPos pos, Block block, BlockStateProperties state, NbtCompound nbt) {
        this.world = world;
        this.pos = pos;
        this.block = block;
        this.state = state;
        this.nbt = nbt;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public LocalBlock getLocalNBT() {
        return new LocalBlock(block, state, nbt);
    }

    @Override
    public void saveLocalNBT(LocalBlock block, Runnable onFinished) {
        this.block = block.getBlock();
        this.state = block.getState();
        this.nbt = block.getNBT();
        ClientNetworking.send(new SetBlockC2SPacket(world, pos, block.getId(), state, nbt,
                ConfigScreen.isRecreateBlocksAndEntities(), ConfigScreen.isTriggerBlockUpdates()));
        onFinished.run();
    }

    @Override
    public Identifier getId() {
        return MVRegistry.BLOCK.getId(block);
    }

    @Override
    public NbtCompound getNBT() {
        return nbt;
    }

    @Override
    public void saveNBT(Identifier id, NbtCompound toSave, Runnable onFinished) {
        this.block = MVRegistry.BLOCK.get(id);
        this.nbt = toSave;
        ClientNetworking.send(new SetBlockC2SPacket(world, pos, id, state, toSave,
                ConfigScreen.isRecreateBlocksAndEntities(), ConfigScreen.isTriggerBlockUpdates()));
        onFinished.run();
    }

}
