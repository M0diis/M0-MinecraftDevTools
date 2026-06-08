package me.m0dii.modules.itemdata;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import me.m0dii.M0DevTools;
import me.m0dii.utils.StringUtils;
import me.m0dii.utils.StyledTextParser;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class ItemDataCodec {
    private static final String COMPONENTS_KEY = "components";
    private static final String CUSTOM_NAME_COMPONENT = "minecraft:custom_name";
    private static final String ITEM_NAME_COMPONENT = "minecraft:item_name";
    private static final String LORE_COMPONENT = "minecraft:lore";

    private ItemDataCodec() {
    }

    public static Optional<NbtCompound> encode(ItemStack stack, RegistryWrapper.WrapperLookup registryLookup) {
        if (stack == null || stack.isEmpty() || registryLookup == null) {
            return Optional.empty();
        }

        return ItemStack.CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), stack)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode item stack: {}", message))
                .filter(NbtCompound.class::isInstance)
                .map(NbtCompound.class::cast)
                .map(NbtCompound::copy);
    }

    public static Optional<ItemStack> decode(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (nbt == null || nbt.isEmpty() || registryLookup == null) {
            return Optional.empty();
        }

        return ItemStack.CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), nbt)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to decode item stack: {}", message));
    }

    public static NbtCompound encodeOrEmpty(ItemStack stack, RegistryWrapper.WrapperLookup registryLookup) {
        return encode(stack, registryLookup).orElseGet(NbtCompound::new);
    }

    public static NbtCompound getComponents(NbtCompound root) {
        if (root == null) {
            return new NbtCompound();
        }
        if (root.get(COMPONENTS_KEY) instanceof NbtCompound components) {
            return components;
        }
        return new NbtCompound();
    }

    public static NbtCompound ensureComponents(NbtCompound root) {
        if (root == null) {
            return new NbtCompound();
        }
        if (root.get(COMPONENTS_KEY) instanceof NbtCompound components) {
            return components;
        }
        NbtCompound components = new NbtCompound();
        root.put(COMPONENTS_KEY, components);
        return components;
    }

    public static ComponentType<?> resolveComponentType(String componentId) {
        if (componentId == null || componentId.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(componentId);
        if (id == null || !Registries.DATA_COMPONENT_TYPE.containsId(id)) {
            return null;
        }
        return Registries.DATA_COMPONENT_TYPE.get(id);
    }

    public static NbtElement normalizeComponentValue(String componentId,
                                                     String raw,
                                                     RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        ComponentType<?> componentType = resolveComponentType(componentId);
        if (componentType == null) {
            throw invalid("Unknown component id: " + componentId);
        }
        if (registryLookup == null) {
            throw invalid("Registry lookup is not available.");
        }

        try {
            NbtElement rawValue = StringNbtReader.fromOps(NbtOps.INSTANCE).readAsArgument(new StringReader(raw));
            return normalizeComponentValue(componentType, componentId, rawValue, registryLookup);
        } catch (CommandSyntaxException syntaxException) {
            NbtElement friendlyValue = tryNormalizeFriendlyComponentValue(componentType, componentId, raw, registryLookup);
            if (friendlyValue != null) {
                return friendlyValue;
            }
            throw syntaxException;
        }
    }

    public static NbtElement normalizeComponentValue(ComponentType<?> componentType,
                                                     String componentId,
                                                     NbtElement rawValue,
                                                     RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        if (componentType == null) {
            throw invalid("Unknown component id: " + componentId);
        }
        Codec<Object> codec = (Codec<Object>) componentType.getCodecOrThrow();
        Object decoded = codec.parse(registryLookup.getOps(NbtOps.INSTANCE), rawValue)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to parse component {}: {}", componentId, message))
                .orElseThrow(() -> invalid("Invalid value for component " + componentId));
        return codec.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), decoded)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode component {}: {}", componentId, message))
                .orElseThrow(() -> invalid("Invalid value for component " + componentId));
    }

    public static NbtCompound normalizeItemData(String raw, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        NbtCompound parsed = StringNbtReader.readCompoundAsArgument(new StringReader(raw));
        ItemStack stack = decode(parsed, registryLookup)
                .orElseThrow(() -> invalid("Invalid serialized item data."));
        return encode(stack, registryLookup)
                .orElse(parsed.copy());
    }

    public static String readPlainDisplayName(NbtCompound itemData, RegistryWrapper.WrapperLookup registryLookup) {
        if (registryLookup == null || itemData == null) {
            return "";
        }
        NbtCompound components = getComponents(itemData);
        NbtElement custom = components.get(CUSTOM_NAME_COMPONENT);
        String fromCustom = decodeText(custom, registryLookup);
        if (!fromCustom.isBlank()) {
            return fromCustom;
        }
        return decodeText(components.get(ITEM_NAME_COMPONENT), registryLookup);
    }

    public static void applyPlainCustomName(NbtCompound itemData, String plainName, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        if (itemData == null) {
            throw invalid("Item data is missing.");
        }
        if (registryLookup == null) {
            throw invalid("Registry lookup is not available.");
        }

        NbtCompound components = ensureComponents(itemData);
        if (plainName == null || plainName.isBlank()) {
            components.remove(CUSTOM_NAME_COMPONENT);
            if (components.isEmpty()) {
                itemData.remove(COMPONENTS_KEY);
            }
            return;
        }

        NbtElement encoded = encodeTextElement(StyledTextParser.parseText(StringUtils.normalizeSingleLineInput(plainName)), registryLookup);
        components.put(CUSTOM_NAME_COMPONENT, encoded);
    }

    public static String defaultTemplateForComponent(String componentId, RegistryWrapper.WrapperLookup registryLookup) {
        return ItemComponentMetadata.starterTemplate(componentId, registryLookup);
    }

    public static String exampleForComponent(String componentId, RegistryWrapper.WrapperLookup registryLookup) {
        return ItemComponentMetadata.example(componentId, registryLookup);
    }

    public static int readCount(NbtCompound itemData) {
        if (itemData == null) {
            return 1;
        }
        return itemData.get("count") instanceof NbtInt nbtInt ? Math.max(1, nbtInt.intValue()) : 1;
    }

    public static void applyCount(NbtCompound itemData, String rawCount) throws CommandSyntaxException {
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

    static String encodeTextLiteral(String value, RegistryWrapper.WrapperLookup registryLookup) {
        if (registryLookup == null) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return TextCodecs.CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), Text.literal(value))
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode text literal: {}", message))
                .map(NbtElement::toString)
                .orElse("\"" + value.replace("\"", "\\\"") + "\"");
    }

    private static String decodeText(NbtElement value, RegistryWrapper.WrapperLookup registryLookup) {
        if (value == null || registryLookup == null) {
            return "";
        }
        return TextCodecs.CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), value)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to decode text component: {}", message))
                .map(Text::getString)
                .orElse("");
    }

    private static NbtElement tryNormalizeFriendlyComponentValue(ComponentType<?> componentType,
                                                                 String componentId,
                                                                 String raw,
                                                                 RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        if (!shouldTryFriendlyTextFallback(componentId, raw)) {
            return null;
        }

        if (CUSTOM_NAME_COMPONENT.equals(componentId) || ITEM_NAME_COMPONENT.equals(componentId)) {
            Text parsed = StyledTextParser.parseText(StringUtils.normalizeSingleLineInput(raw));
            return normalizeComponentValue(componentType, componentId, encodeTextElement(parsed, registryLookup), registryLookup);
        }

        if (LORE_COMPONENT.equals(componentId)) {
            NbtList list = new NbtList();
            for (Text line : StyledTextParser.parseLines(raw)) {
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

    private static NbtElement encodeTextElement(Text text, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
        return TextCodecs.CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), text)
                .resultOrPartial(message -> M0DevTools.LOGGER.warn("[ItemData] Failed to encode text component: {}", message))
                .orElseThrow(() -> invalid("Unable to encode the text component."));
    }

    private static CommandSyntaxException invalid(String message) {
        return new SimpleCommandExceptionType(Text.literal(message)).create();
    }
}
