package me.m0dii.nbteditor.nbtreferences;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import net.minecraft.text.Text;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface NBTReferenceFilter extends Predicate<NBTReference<?>> {
    NBTReferenceFilter ANY = create(ref -> true, ref -> true, ref -> true,
            TextInst.translatable("nbteditor.no_ref.to_edit"), TextInst.translatable("nbteditor.no_hand.no_item.to_edit"));
    NBTReferenceFilter ANY_NBT = create(ref -> true, ref -> ref.getLocalNBT().isBlockEntity(), ref -> true,
            TextInst.translatable("nbteditor.no_ref.to_edit_nbt"), TextInst.translatable("nbteditor.no_hand.no_item.to_edit"));

    /**
     * Pass in <code>null</code> to the filters to always reject that type<br>
     * Avoid passing in <code>() -> false</code> as this is inefficient for blocks and entities
     */
    static NBTReferenceFilter create(
            Predicate<ItemReference> itemFilter,
            Predicate<BlockReference> blockFilter,
            Predicate<EntityReference> entityFilter,
            Supplier<Text> failMsg) {
        return new NBTReferenceFilter() {
            @Override
            public boolean test(NBTReference<?> ref) {
                return switch (ref) {
                    case ItemReference item -> itemFilter != null && itemFilter.test(item);
                    case BlockReference block -> blockFilter != null && blockFilter.test(block);
                    case EntityReference entity -> entityFilter != null && entityFilter.test(entity);
                    case null, default -> false;
                };
            }

            @Override
            public Text getFailMessage() {
                return failMsg.get();
            }

            @Override
            public boolean isItemAllowed() {
                return itemFilter != null;
            }

            @Override
            public boolean isBlockAllowed() {
                return blockFilter != null;
            }

            @Override
            public boolean isEntityAllowed() {
                return entityFilter != null;
            }
        };
    }

    /**
     * Pass in <code>null</code> to the filters to always reject that type<br>
     * Avoid passing in <code>() -> false</code> as this is inefficient for blocks and entities
     */
    static NBTReferenceFilter create(
            Predicate<ItemReference> itemFilter,
            Predicate<BlockReference> blockFilter,
            Predicate<EntityReference> entityFilter,
            Text expandedFailMsg,
            Text nonExpandedFailMsg) {
        return create(itemFilter, blockFilter, entityFilter, () -> M0DevToolsClient.SERVER_CONN.isEditingExpanded()
                ? expandedFailMsg
                : nonExpandedFailMsg);
    }

    static NBTReferenceFilter create(Predicate<NBTReference<?>> filter, Supplier<Text> failMsg) {
        return create(filter::test, filter::test, filter::test, failMsg);
    }

    static NBTReferenceFilter create(Predicate<NBTReference<?>> filter, Text expandedFailMsg, Text nonExpandedFailMsg) {
        return create(filter::test, filter::test, filter::test, expandedFailMsg, nonExpandedFailMsg);
    }

    Text getFailMessage();

    // Use to avoid requesting block or entity data when it will always be rejected
    default boolean isItemAllowed() {
        return true;
    }

    default boolean isBlockAllowed() {
        return true;
    }

    default boolean isEntityAllowed() {
        return true;
    }
}
