package me.m0dii.nbteditor.nbtreferences;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.nbtreferences.itemreferences.HandItemReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface NBTReference<T extends LocalNBT> {
    static CompletableFuture<? extends Optional<? extends NBTReference<?>>> getReference(NBTReferenceFilter filter, boolean airable) {
        HitResult target = MiscUtil.client.crosshairTarget;

        if (target instanceof EntityHitResult entity && filter.isEntityAllowed()) {
            return EntityReference.getEntity(entity.getEntity().getEntityWorld().getRegistryKey(), entity.getEntity().getUuid())
                    .thenApply(ref -> ref.<NBTReference<?>>map(UnaryOperator.identity())
                            .filter(filter).or(() -> getClientReference(target, filter, airable)));
        }

        if (target instanceof BlockHitResult block && filter.isBlockAllowed()) {
            return BlockReference.getBlock(MiscUtil.client.world.getRegistryKey(), block.getBlockPos())
                    .thenApply(ref -> ref.<NBTReference<?>>map(UnaryOperator.identity())
                            .filter(filter).or(() -> getClientReference(target, filter, airable)));
        }
        return CompletableFuture.completedFuture(getClientReference(target, filter, airable));
    }

    private static Optional<? extends NBTReference<?>> getClientReference(HitResult target, NBTReferenceFilter filter, boolean airable) {
        boolean heldItemDisallowed = false;
        if (filter.isItemAllowed()) {
            try {
                ItemReference ref = ItemReference.getHeldItem();
                if (filter.test(ref)) {
                    return Optional.of(ref);
                }
                heldItemDisallowed = true;
            } catch (CommandSyntaxException ignored) {
            }
        }

        if (filter.isBlockAllowed() && M0DevToolsClient.SERVER_CONN.isEditingExpanded()) {
            if (target instanceof BlockHitResult block && block.getType() != HitResult.Type.MISS) {
                BlockReference ref = BlockReference.getBlockWithoutNBT(block.getBlockPos());
                if (filter.test(ref)) {
                    return Optional.of(ref);
                }
            }
        }

        if (airable && !heldItemDisallowed && filter.isItemAllowed()) {
            return Optional.of(new HandItemReference(Hand.MAIN_HAND));
        }

        return Optional.empty();
    }

    static void getReference(NBTReferenceFilter filter, boolean airable, Consumer<NBTReference<?>> consumer) {
        NBTReference.getReference(filter, airable).thenAccept(ref -> MiscUtil.client.execute(() -> {
            ref.ifPresentOrElse(consumer, () -> {
                if (MiscUtil.client.player != null) {
                    MiscUtil.client.player.sendMessage(filter.getFailMessage(), false);
                }
            });
        }));
    }

    boolean exists();

    T getLocalNBT();

    default void saveLocalNBT(T nbt, Runnable onFinished) {
        saveNBT(nbt.getId(), nbt.getNBT(), onFinished);
    }

    default void saveLocalNBT(T nbt, Text msg) {
        saveLocalNBT(nbt, () -> MiscUtil.client.player.sendMessage(msg, false));
    }

    default void saveLocalNBT(T nbt) {
        saveLocalNBT(nbt, () -> {
        });
    }

    default void modifyLocalNBT(Consumer<T> nbtConsumer, Runnable onFinished) {
        T nbt = getLocalNBT();
        nbtConsumer.accept(nbt);
        saveLocalNBT(nbt, onFinished);
    }

    default void modifyLocalNBT(Consumer<T> nbtConsumer, Text msg) {
        modifyLocalNBT(nbtConsumer, () -> MiscUtil.client.player.sendMessage(msg, false));
    }

    default void modifyLocalNBT(Consumer<T> nbtConsumer) {
        modifyLocalNBT(nbtConsumer, () -> {
        });
    }

    Identifier getId();

    NbtCompound getNBT();

    void saveNBT(Identifier id, NbtCompound toSave, Runnable onFinished);

    default void saveNBT(Identifier id, NbtCompound toSave, Text msg) {
        saveNBT(id, toSave, () -> MiscUtil.client.player.sendMessage(msg, false));
    }

    default void saveNBT(Identifier id, NbtCompound toSave) {
        saveNBT(id, toSave, () -> {
        });
    }

    default void showParent() {
        escapeParent();
    }

    default void escapeParent() {
        M0DevToolsClient.CURSOR_MANAGER.closeRoot();
    }
}
