package me.m0dii.modules.itemdata;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import me.m0dii.M0DevTools;
import me.m0dii.utils.StringUtils;
import me.m0dii.utils.StyledTextParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public final class ItemDataCodec {
    private static final String COMPONENTS_KEY = "components";
    private static final String CUSTOM_NAME_COMPONENT = "minecraft:custom_name";
    private static final String ITEM_NAME_COMPONENT = "minecraft:item_name";
    private static final String LORE_COMPONENT = "minecraft:lore";

    private ItemDataCodec() {
    }

    public static Optional<CompoundTag> encode(ItemStack stack, HolderLookup.Provider registryLookup) {
        if (stack == null || stack.isEmpty() || registryLookup == null) {
            return Optional.empty();
        }

        return ItemStack.CODEC.encodeStart(registryLookup.createSerializationContext(NbtOps.INSTANCE), stack)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode item stack: {}", message))
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .map(CompoundTag::copy);
    }

    public static Optional<ItemStack> decode(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        if (nbt == null || nbt.isEmpty() || registryLookup == null) {
            return Optional.empty();
        }

        return ItemStack.CODEC.parse(registryLookup.createSerializationContext(NbtOps.INSTANCE), nbt)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to decode item stack: {}", message));
    }

    public static CompoundTag encodeOrEmpty(ItemStack stack, HolderLookup.Provider registryLookup) {
        return encode(stack, registryLookup).orElseGet(CompoundTag::new);
    }

    public static CompoundTag getComponents(CompoundTag root) {
        if (root == null) {
            return new CompoundTag();
        }
        if (root.get(COMPONENTS_KEY) instanceof CompoundTag components) {
            return components;
        }
        return new CompoundTag();
    }

    public static CompoundTag ensureComponents(CompoundTag root) {
        if (root == null) {
            return new CompoundTag();
        }
        if (root.get(COMPONENTS_KEY) instanceof CompoundTag components) {
            return components;
        }
        CompoundTag components = new CompoundTag();
        root.put(COMPONENTS_KEY, components);
        return components;
    }

    public static DataComponentType<?> resolveComponentType(String componentId) {
        if (componentId == null || componentId.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(componentId);
        if (id == null || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
    }

    public static Tag normalizeComponentValue(String componentId,
                                                     String raw,
                                                     HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        DataComponentType<?> componentType = resolveComponentType(componentId);
        if (componentType == null) {
            throw invalid("Unknown component id: " + componentId);
        }
        if (registryLookup == null) {
            throw invalid("Registry lookup is not available.");
        }

        try {
            Tag rawValue = TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(raw));
            return normalizeComponentValue(componentType, componentId, rawValue, registryLookup);
        } catch (CommandSyntaxException syntaxException) {
            Tag friendlyValue = tryNormalizeFriendlyComponentValue(componentType, componentId, raw, registryLookup);
            if (friendlyValue != null) {
                return friendlyValue;
            }
            throw syntaxException;
        }
    }

    public static Tag normalizeComponentValue(DataComponentType<?> componentType,
                                                     String componentId,
                                                     Tag rawValue,
                                                     HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        if (componentType == null) {
            throw invalid("Unknown component id: " + componentId);
        }
        Codec<Object> codec = (Codec<Object>) componentType.codecOrThrow();
        Object decoded = codec.parse(registryLookup.createSerializationContext(NbtOps.INSTANCE), rawValue)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to parse component {}: {}", componentId, message))
                .orElseThrow(() -> invalid("Invalid value for component " + componentId));
        return codec.encodeStart(registryLookup.createSerializationContext(NbtOps.INSTANCE), decoded)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode component {}: {}", componentId, message))
                .orElseThrow(() -> invalid("Invalid value for component " + componentId));
    }

    public static CompoundTag normalizeItemData(String raw, HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        CompoundTag parsed = TagParser.parseCompoundAsArgument(new StringReader(raw));
        ItemStack stack = decode(parsed, registryLookup)
                .orElseThrow(() -> invalid("Invalid serialized item data."));
        return encode(stack, registryLookup)
                .orElse(parsed.copy());
    }

    public static String readPlainDisplayName(CompoundTag itemData, HolderLookup.Provider registryLookup) {
        if (registryLookup == null || itemData == null) {
            return "";
        }
        CompoundTag components = getComponents(itemData);
        Tag custom = components.get(CUSTOM_NAME_COMPONENT);
        String fromCustom = decodeText(custom, registryLookup);
        if (!fromCustom.isBlank()) {
            return fromCustom;
        }
        return decodeText(components.get(ITEM_NAME_COMPONENT), registryLookup);
    }

    public static void applyPlainCustomName(CompoundTag itemData, String plainName, HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        if (itemData == null) {
            throw invalid("Item data is missing.");
        }
        if (registryLookup == null) {
            throw invalid("Registry lookup is not available.");
        }

        CompoundTag components = ensureComponents(itemData);
        if (plainName == null || plainName.isBlank()) {
            components.remove(CUSTOM_NAME_COMPONENT);
            if (components.isEmpty()) {
                itemData.remove(COMPONENTS_KEY);
            }
            return;
        }

        Tag encoded = encodeTextElement(StyledTextParser.parseText(StringUtils.normalizeSingleLineInput(plainName)), registryLookup);
        components.put(CUSTOM_NAME_COMPONENT, encoded);
    }

    public static String defaultTemplateForComponent(String componentId, HolderLookup.Provider registryLookup) {
        return ItemComponentMetadata.starterTemplate(componentId, registryLookup);
    }

    public static String exampleForComponent(String componentId, HolderLookup.Provider registryLookup) {
        return ItemComponentMetadata.example(componentId, registryLookup);
    }

    public static int readCount(CompoundTag itemData) {
        if (itemData == null) {
            return 1;
        }
        return itemData.get("count") instanceof IntTag nbtInt ? Math.max(1, nbtInt.intValue()) : 1;
    }

    public static void applyCount(CompoundTag itemData, String rawCount) throws CommandSyntaxException {
        if (itemData == null) {
            throw invalid("Item data is missing.");
        }
        if (rawCount == null || rawCount.isBlank()) {
            itemData.putInt("count", 1);
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(rawCount.trim());
        } catch (NumberFormatException exception) {
            throw invalid("Count must be a positive integer.");
        }
        if (parsed < 1) {
            throw invalid("Count must be at least 1.");
        }
        itemData.putInt("count", parsed);
    }

    static String encodeTextLiteral(String value, HolderLookup.Provider registryLookup) {
        if (registryLookup == null) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return ComponentSerialization.CODEC.encodeStart(registryLookup.createSerializationContext(NbtOps.INSTANCE), Component.literal(value))
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode text literal: {}", message))
                .map(Tag::toString)
                .orElse("\"" + value.replace("\"", "\\\"") + "\"");
    }

    private static String decodeText(Tag value, HolderLookup.Provider registryLookup) {
        if (value == null || registryLookup == null) {
            return "";
        }
        return ComponentSerialization.CODEC.parse(registryLookup.createSerializationContext(NbtOps.INSTANCE), value)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to decode text component: {}", message))
                .map(Component::getString)
                .orElse("");
    }

    private static Tag tryNormalizeFriendlyComponentValue(DataComponentType<?> componentType,
                                                                 String componentId,
                                                                 String raw,
                                                                 HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        if (!shouldTryFriendlyTextFallback(componentId, raw)) {
            return null;
        }

        if (CUSTOM_NAME_COMPONENT.equals(componentId) || ITEM_NAME_COMPONENT.equals(componentId)) {
            Component parsed = StyledTextParser.parseText(StringUtils.normalizeSingleLineInput(raw));
            return normalizeComponentValue(componentType, componentId, encodeTextElement(parsed, registryLookup), registryLookup);
        }

        if (LORE_COMPONENT.equals(componentId)) {
            ListTag list = new ListTag();
            for (Component line : StyledTextParser.parseLines(raw)) {
                list.add(encodeTextElement(line, registryLookup));
            }
            return normalizeComponentValue(componentType, componentId, list, registryLookup);
        }

        return null;
    }

    private static boolean shouldTryFriendlyTextFallback(String componentId, String raw) {
        if (!(CUSTOM_NAME_COMPONENT.equals(componentId) || ITEM_NAME_COMPONENT.equals(componentId) || LORE_COMPONENT.equals(componentId))) {
            return false;
        }
        String normalized = StringUtils.normalizeMultilineInput(raw).trim();
        if (normalized.isEmpty()) {
            return true;
        }
        if (normalized.contains("\n")) {
            return true;
        }
        if (normalized.contains("&") || normalized.contains("§") || normalized.contains("<#") || normalized.startsWith("#")) {
            return true;
        }
        return !(normalized.startsWith("{")
                || normalized.startsWith("[")
                || normalized.startsWith("\"")
                || normalized.startsWith("'"));
    }

    private static Tag encodeTextElement(Component text, HolderLookup.Provider registryLookup) throws CommandSyntaxException {
        return ComponentSerialization.CODEC.encodeStart(registryLookup.createSerializationContext(NbtOps.INSTANCE), text)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode text component: {}", message))
                .orElseThrow(() -> invalid("Unable to encode the text component."));
    }

    private static CommandSyntaxException invalid(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }
}
