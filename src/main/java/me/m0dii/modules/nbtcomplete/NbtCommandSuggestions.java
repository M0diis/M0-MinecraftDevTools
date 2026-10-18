package me.m0dii.modules.nbtcomplete;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Glue between the argument-type mixins and {@link NbtCompletion}: picks the schema root from the command being
 * typed (the summoned entity type, or what you are looking at for {@code /data}) and builds brigadier suggestions.
 */
public final class NbtCommandSuggestions {
    private static final Map<String, List<String>> REGISTRY_CACHE = new HashMap<>();

    private NbtCommandSuggestions() {
    }

    public static CompletableFuture<Suggestions> compound(SuggestionsBuilder builder) {
        try {
            String root = rootFor(builder.getInput().substring(0, builder.getStart()));
            return toSuggestions(builder, NbtCompletion.compound(builder.getRemaining(), root, NbtCommandSuggestions::registryIds));
        } catch (RuntimeException e) {
            return Suggestions.empty();
        }
    }

    public static CompletableFuture<Suggestions> path(SuggestionsBuilder builder) {
        try {
            String root = rootFor(builder.getInput().substring(0, builder.getStart()));
            return toSuggestions(builder, NbtCompletion.path(builder.getRemaining(), root));
        } catch (RuntimeException e) {
            return Suggestions.empty();
        }
    }

    /** Extra value suggestions for {@code item[component=...]}; null when there is nothing to add. */
    public static Suggestions componentValues(SuggestionsBuilder builder) {
        try {
            NbtCompletion.Result result = NbtCompletion.componentValue(builder.getRemaining());
            if (result.isEmpty()) {
                return null;
            }
            SuggestionsBuilder offset = builder.createOffset(builder.getStart() + result.start());
            result.suggestions().forEach(offset::suggest);
            return offset.build();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static CompletableFuture<Suggestions> toSuggestions(SuggestionsBuilder builder, NbtCompletion.Result result) {
        if (result.isEmpty()) {
            return Suggestions.empty();
        }
        SuggestionsBuilder offset = builder.createOffset(builder.getStart() + result.start());
        result.suggestions().forEach(offset::suggest);
        return offset.buildFuture();
    }

    /** Schema root for the command text before the NBT argument. */
    static String rootFor(String before) {
        String[] words = before.trim().replaceFirst("^/", "").split("\\s+");
        for (int i = words.length - 1; i >= 0; i--) {
            String word = words[i].toLowerCase(Locale.ROOT);
            if (word.equals("summon") && i + 1 < words.length) {
                return NbtSchema.entityRoot(words[i + 1]);
            }
            if (word.equals("data") && i + 2 < words.length) {
                String kind = words[i + 2].toLowerCase(Locale.ROOT);
                if (kind.equals("entity") && i + 3 < words.length) {
                    return entityTarget(words[i + 3]);
                }
                if (kind.equals("block") && i + 5 < words.length) {
                    return blockTarget(words[i + 3], words[i + 4], words[i + 5]);
                }
                return "living";
            }
        }
        return "living";
    }

    private static String entityTarget(String selector) {
        Minecraft client = Minecraft.getInstance();
        if (selector.startsWith("@s") || selector.startsWith("@p")
                || client.player != null && selector.equalsIgnoreCase(client.player.getName().getString())) {
            return "living";
        }
        Entity looked = client.crosshairPickEntity;
        if (looked != null) {
            return NbtSchema.entityRoot(BuiltInRegistries.ENTITY_TYPE.getKey(looked.getType()).toString());
        }
        return "living";
    }

    private static String blockTarget(String xs, String ys, String zs) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return "block_base";
        }
        BlockPos pos = null;
        try {
            pos = new BlockPos(Integer.parseInt(xs), Integer.parseInt(ys), Integer.parseInt(zs));
        } catch (NumberFormatException ignored) {
            HitResult hit = client.hitResult;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                pos = blockHit.getBlockPos();
            }
        }
        if (pos == null) {
            return "block_base";
        }
        return NbtSchema.blockRoot(BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(pos).getBlock()).toString());
    }

    static synchronized List<String> registryIds(String registry) {
        return REGISTRY_CACHE.computeIfAbsent(registry == null ? "" : registry, name -> {
            List<String> ids = new ArrayList<>();
            var keys = switch (name) {
                case "item" -> BuiltInRegistries.ITEM.keySet();
                case "mob_effect" -> BuiltInRegistries.MOB_EFFECT.keySet();
                case "attribute" -> BuiltInRegistries.ATTRIBUTE.keySet();
                case "entity_type" -> BuiltInRegistries.ENTITY_TYPE.keySet();
                case "block" -> BuiltInRegistries.BLOCK.keySet();
                case "villager_profession" -> BuiltInRegistries.VILLAGER_PROFESSION.keySet();
                case "villager_type" -> BuiltInRegistries.VILLAGER_TYPE.keySet();
                default -> java.util.Set.<net.minecraft.resources.Identifier>of();
            };
            keys.forEach(id -> ids.add(id.toString()));
            ids.sort(null);
            return List.copyOf(ids);
        });
    }
}
