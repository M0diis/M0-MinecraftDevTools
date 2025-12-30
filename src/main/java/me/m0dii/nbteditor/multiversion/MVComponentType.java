package me.m0dii.nbteditor.multiversion;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.item.BlockPredicatesChecker;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;

public class MVComponentType<T> implements ComponentType<T> {

    public static final MVComponentType<AttributeModifiersComponent> ATTRIBUTE_MODIFIERS =
            new MVComponentType<>(DataComponentTypes.ATTRIBUTE_MODIFIERS);
    public static final MVComponentType<NbtComponent> BLOCK_ENTITY_DATA =
            new MVComponentType<>(DataComponentTypes.BLOCK_ENTITY_DATA);
    public static final MVComponentType<BlockStateComponent> BLOCK_STATE =
            new MVComponentType<>(DataComponentTypes.BLOCK_STATE);
    public static final MVComponentType<BlockPredicatesChecker> CAN_BREAK =
            new MVComponentType<>(DataComponentTypes.CAN_BREAK);
    public static final MVComponentType<BlockPredicatesChecker> CAN_PLACE_ON =
            new MVComponentType<>(DataComponentTypes.CAN_PLACE_ON);
    public static final MVComponentType<NbtComponent> CUSTOM_DATA =
            new MVComponentType<>(DataComponentTypes.CUSTOM_DATA);
    public static final MVComponentType<Text> CUSTOM_NAME =
            new MVComponentType<>(DataComponentTypes.CUSTOM_NAME);
    public static final MVComponentType<DyedColorComponent> DYED_COLOR =
            new MVComponentType<>(DataComponentTypes.DYED_COLOR);
    public static final MVComponentType<ItemEnchantmentsComponent> ENCHANTMENTS =
            new MVComponentType<>(DataComponentTypes.ENCHANTMENTS);
    public static final MVComponentType<NbtComponent> ENTITY_DATA =
            new MVComponentType<>(DataComponentTypes.ENTITY_DATA);
    public static final MVComponentType<Unit> HIDE_ADDITIONAL_TOOLTIP =
            new MVComponentType<>(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP);
    public static final MVComponentType<Unit> HIDE_TOOLTIP =
            new MVComponentType<>(DataComponentTypes.HIDE_TOOLTIP);
    public static final MVComponentType<Text> ITEM_NAME =
            new MVComponentType<>(DataComponentTypes.ITEM_NAME);
    public static final MVComponentType<LoreComponent> LORE =
            new MVComponentType<>(DataComponentTypes.LORE);
    public static final MVComponentType<Integer> MAX_DAMAGE =
            new MVComponentType<>(DataComponentTypes.MAX_DAMAGE);
    public static final MVComponentType<Integer> MAX_STACK_SIZE =
            new MVComponentType<>(DataComponentTypes.MAX_STACK_SIZE);
    public static final MVComponentType<PotionContentsComponent> POTION_CONTENTS =
            new MVComponentType<>(DataComponentTypes.POTION_CONTENTS);
    public static final MVComponentType<ProfileComponent> PROFILE =
            new MVComponentType<>(DataComponentTypes.PROFILE);
    public static final MVComponentType<ItemEnchantmentsComponent> STORED_ENCHANTMENTS =
            new MVComponentType<>(DataComponentTypes.STORED_ENCHANTMENTS);
    public static final MVComponentType<SuspiciousStewEffectsComponent> SUSPICIOUS_STEW_EFFECTS =
            new MVComponentType<>(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS);
    public static final MVComponentType<ArmorTrim> TRIM =
            new MVComponentType<>(DataComponentTypes.TRIM);
    public static final MVComponentType<UnbreakableComponent> UNBREAKABLE =
            new MVComponentType<>(DataComponentTypes.UNBREAKABLE);
    public static final MVComponentType<WritableBookContentComponent> WRITABLE_BOOK_CONTENT =
            new MVComponentType<>(DataComponentTypes.WRITABLE_BOOK_CONTENT);
    public static final MVComponentType<WrittenBookContentComponent> WRITTEN_BOOK_CONTENT =
            new MVComponentType<>(DataComponentTypes.WRITTEN_BOOK_CONTENT);

    private final ComponentType<T> component;

    public MVComponentType(ComponentType<T> component) {
        this.component = component;
    }

    public Object getInternalValue() {
        return component;
    }

    @Override
    public @Nullable Codec<T> getCodec() {
        return null;
    }

    @Override
    public PacketCodec<? super RegistryByteBuf, T> getPacketCodec() {
        return null;
    }
}
