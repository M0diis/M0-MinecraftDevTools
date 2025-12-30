package me.m0dii.nbteditor.localnbt;

import me.m0dii.nbteditor.multiversion.MVQuaternionf;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface LocalNBT {
    static Optional<LocalNBT> deserialize(NbtCompound nbt, int defaultDataVersion) {
        return Optional.ofNullable(switch (nbt.contains("type", NbtElement.STRING_TYPE) ? nbt.getString("type") : "item") {
            case "item" -> LocalItemStack.deserialize(nbt, defaultDataVersion);
            case "block" -> LocalBlock.deserialize(nbt, defaultDataVersion);
            case "entity" -> LocalEntity.deserialize(nbt, defaultDataVersion);
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    static <T extends LocalNBT> T copy(T localNBT) {
        return (T) localNBT.copy();
    }

    static MVQuaternionf makeRotatingIcon(MatrixStack matrices, int x, int y, float scale, boolean inverse) {
        matrices.translate(x + 8, y + 8, 8.0);
        matrices.scale(scale, scale, scale);
        matrices.scale(12, 12, 12);

        MVQuaternionf quatX = MVQuaternionf.ofXRotation((float) (-Math.PI / 6));
        MVQuaternionf quatY = MVQuaternionf.ofYRotation((float) (System.currentTimeMillis() % 2000 / 2000.0f * Math.PI * 2));
        MVQuaternionf quatZ = MVQuaternionf.ofZRotation((float) Math.PI);

        if (inverse) {
            quatX.conjugate().applyToMatrixStack(matrices);
            quatY.copy().conjugate().applyToMatrixStack(matrices);
        } else {
            quatX.applyToMatrixStack(matrices);
            quatY.applyToMatrixStack(matrices);
        }
        quatZ.applyToMatrixStack(matrices);

        return quatY;
    }

    default boolean isEmpty() {
        return isEmpty(getId());
    }

    boolean isEmpty(Identifier id);

    Text getName();

    void setName(Text name);

    String getDefaultName();

    Identifier getId();

    void setId(Identifier id);

    Set<Identifier> getIdOptions();

    NbtCompound getNBT();

    void setNBT(NbtCompound nbt);

    default NbtCompound getOrCreateNBT() {
        NbtCompound nbt = getNBT();
        if (nbt == null) {
            nbt = new NbtCompound();
            setNBT(nbt);
        }
        return nbt;
    }

    default void modifyNBT(UnaryOperator<NbtCompound> modifier) {
        NbtCompound nbt = getNBT();
        if (nbt == null) {
            nbt = new NbtCompound();
        }
        setNBT(modifier.apply(nbt));
    }

    default void modifyNBT(Consumer<NbtCompound> modifier) {
        modifyNBT(nbt -> {
            modifier.accept(nbt);
            return nbt;
        });
    }

    void renderIcon(MatrixStack matrices, int x, int y, float tickDelta);

    Optional<ItemStack> toItem();

    NbtCompound serialize();

    Text toHoverableText();

    LocalNBT copy();

    @Override
    boolean equals(Object nbt);
}
