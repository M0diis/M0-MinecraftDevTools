package me.m0dii.nbteditor.multiversion.nbt.components;

import com.mojang.serialization.DataResult;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.nbt.Attempt;
import me.m0dii.nbteditor.multiversion.nbt.NBTManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

public class ComponentBlockEntityNBTManager implements NBTManager<BlockEntity> {

    @Override
    public Attempt<NbtCompound> trySerialize(BlockEntity subject) {
        RegistryWrapper.WrapperLookup registryLookup = DynamicRegistryManagerHolder.get();

        NbtCompound output = new NbtCompound();
        subject.writeNbt(output, registryLookup);
        DataResult<NbtElement> result = BlockEntity.Components.CODEC
                .encodeStart(registryLookup.getOps(NbtOps.INSTANCE), subject.getComponents());
        result.resultOrPartial().ifPresent(nbt -> output.copyFrom((NbtCompound) nbt));
        subject.writeIdToNbt(output);

        return new Attempt<>(output, result.error().map(DataResult.Error::message).orElse(null));
    }

    @Override
    public boolean hasNbt(BlockEntity subject) {
        return true;
    }

    @Override
    public NbtCompound getNbt(BlockEntity subject) {
        return subject.createNbt(DynamicRegistryManagerHolder.get());
    }

    @Override
    public NbtCompound getOrCreateNbt(BlockEntity subject) {
        return getNbt(subject);
    }

    @Override
    public void setNbt(BlockEntity subject, NbtCompound nbt) {
        subject.read(nbt, DynamicRegistryManagerHolder.get());
    }

}
