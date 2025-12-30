package me.m0dii.nbteditor.async;

import lombok.Getter;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.OptionalLong;
import java.util.WeakHashMap;

public class ItemSize {

    private ItemSize() {
    }

    private static final WeakHashMap<ItemStack, OptionalLong> UNCOMPRESSED_SIZES = new WeakHashMap<>();
    private static final WeakHashMap<ItemStack, OptionalLong> COMPRESSED_SIZES = new WeakHashMap<>();

    public static OptionalLong getItemSize(ItemStack stack, boolean compressed) {
        if (!NBTManagers.ITEM.hasNbt(stack)) {
            return OptionalLong.of(calcItemSize(stack, compressed));
        }

        WeakHashMap<ItemStack, OptionalLong> sizes = (compressed ? COMPRESSED_SIZES : UNCOMPRESSED_SIZES);
        OptionalLong size;

        synchronized (sizes) {
            size = sizes.get(stack);
            if (size != null) {
                return size;
            }
            size = OptionalLong.empty();
            sizes.put(stack, size);
        }

        Thread thread = new Thread(() -> {
            long knownSize = calcItemSize(stack, compressed);
            synchronized (sizes) {
                sizes.put(stack, OptionalLong.of(knownSize));
            }
        }, "NBTEditor/Async/ItemSizeProcessor [" + MVRegistry.ITEM.getId(stack.getItem()) + "]");

        thread.setDaemon(true);
        thread.start();

        return size;
    }

    private static long calcItemSize(ItemStack stack, boolean compressed) {
        try (ByteCountingOutputStream stream = new ByteCountingOutputStream()) {
            NbtCompound nbt = NBTManagers.ITEM.serialize(stack, true);
            if (compressed) {
                MVMisc.writeCompressedNbt(nbt, stream);
            } else {
                MVMisc.writeNbt(nbt, stream);
            }

            return stream.getCount();
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error while getting the size of an item", e);
        }

        return 0;
    }

    @Getter
    private static class ByteCountingOutputStream extends OutputStream {
        private long count;

        @Override
        public void write(int b) {
            count++;
        }

        @Override
        public void write(byte[] b) {
            count += b.length;
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) {
            count += len;
        }
    }

}
