package me.m0dii.nbteditor.tagreferences;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import me.m0dii.nbteditor.multiversion.MVComponentType;
import me.m0dii.nbteditor.tagreferences.general.ComponentTagReference;
import me.m0dii.nbteditor.tagreferences.general.TagReference;
import me.m0dii.nbteditor.tagreferences.specific.EnchantsTagReference;
import me.m0dii.nbteditor.tagreferences.specific.data.AttributeData;
import me.m0dii.nbteditor.tagreferences.specific.data.CustomPotionContents;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.component.type.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.*;

public class ItemTagReferences {

    private ItemTagReferences() {
    }

    public static final TagReference<CustomPotionContents, ItemStack> CUSTOM_POTION_CONTENTS = new ComponentTagReference<>(MVComponentType.POTION_CONTENTS,
            () -> new PotionContentsComponent(Optional.empty(), Optional.empty(), List.of(), Optional.empty()),
            contents -> new CustomPotionContents(contents.customColor(), contents.customEffects()),
            contents -> new PotionContentsComponent(Optional.empty(), contents.color(), contents.effects(), Optional.empty()));

    public static final TagReference<Optional<String>, ItemStack> PROFILE_NAME = new ComponentTagReference<>(MVComponentType.PROFILE,
            null,
            component -> component == null ? Optional.empty() : component.name(),
            name -> new ProfileComponent(name, Optional.empty(), new PropertyMap()));

    public static final TagReference<Optional<GameProfile>, ItemStack> PROFILE = new ComponentTagReference<>(MVComponentType.PROFILE,
            null,
            profile -> Optional.ofNullable(profile).map(ProfileComponent::gameProfile),
            profile -> profile.map(ProfileComponent::new).orElse(null));

    public static final TagReference<List<AttributeData>, ItemStack> ATTRIBUTES = new ComponentTagReference<>(MVComponentType.ATTRIBUTE_MODIFIERS,
            () -> new AttributeModifiersComponent(List.of(), true),
            component -> component.modifiers().stream().map(AttributeData::fromComponentEntry).toList(),
            (component, list) -> new AttributeModifiersComponent(
                    list.stream().map(AttributeData::toComponentEntry).toList(),
                    component == null || component.showInTooltip()));

    public static final TagReference<List<String>, ItemStack> WRITABLE_BOOK_PAGES = new ComponentTagReference<>(MVComponentType.WRITABLE_BOOK_CONTENT,
            () -> new WritableBookContentComponent(List.of()),
            content -> content.pages().stream().map(RawFilteredPair::raw).toList(),
            pages -> new WritableBookContentComponent(pages.stream().map(RawFilteredPair::of).toList()));

    public static final TagReference<Boolean, ItemStack> UNBREAKABLE = ComponentTagReference.forExistance(MVComponentType.UNBREAKABLE,
            () -> new UnbreakableComponent(true));

    public static final TagReference<NbtCompound, ItemStack> CUSTOM_DATA = getComponentTagRefOfNBT(MVComponentType.CUSTOM_DATA);

    public static final TagReference<Map<String, String>, ItemStack> BLOCK_STATE = new ComponentTagReference<>(MVComponentType.BLOCK_STATE,
            null,
            component -> component == null ? new HashMap<>() : new HashMap<>(component.properties()),
            BlockStateComponent::new);

    public static final TagReference<NbtCompound, ItemStack> BLOCK_ENTITY_DATA = getComponentTagRefOfNBT(MVComponentType.BLOCK_ENTITY_DATA);

    public static final TagReference<NbtCompound, ItemStack> ENTITY_DATA = getComponentTagRefOfNBT(MVComponentType.ENTITY_DATA);

    public static final TagReference<Enchants, ItemStack> ENCHANTMENTS = new EnchantsTagReference();

    public static final TagReference<List<Text>, ItemStack> LORE = new ComponentTagReference<>(MVComponentType.LORE,
            () -> LoreComponent.DEFAULT,
            component -> new ArrayList<>(component.lines()),
            lore -> new LoreComponent(lore.stream().limit(256).toList()));

    private static TagReference<NbtCompound, ItemStack> getComponentTagRefOfNBT(MVComponentType<NbtComponent> component) {
        return new ComponentTagReference<>(component,
                null,
                componentValue -> componentValue == null ? new NbtCompound() : componentValue.copyNbt(),
                NbtComponent::of);
    }

}
