package me.m0dii.nbteditor.integrations;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NBTAutocompleteIntegration extends Integration {

    public static final Optional<NBTAutocompleteIntegration> INSTANCE = Integration.getOptional(NBTAutocompleteIntegration::new);

    private NBTAutocompleteIntegration() {
    }

    private static StringRange shiftRange(StringRange range, int shift) {
        return new StringRange(range.getStart() + shift, range.getEnd() + shift);
    }

    private static Suggestion shiftSuggestion(Suggestion suggestion, int shift) {
        Suggestion shiftedSuggestion = new Suggestion(shiftRange(suggestion.getRange(), shift), suggestion.getText(), suggestion.getTooltip());
        NbtSuggestionManager.subtextMap.put(shiftedSuggestion, NbtSuggestionManager.subtextMap.remove(suggestion));
        return shiftedSuggestion;
    }

    @Override
    public String getModId() {
        return "nbt_ac";
    }

    private CompletableFuture<Suggestions> getSuggestions(@NotNull SuggestionType type,
                                                          @NotNull Identifier id,
                                                          @NotNull NbtElement nbt,
                                                          @NotNull List<String> path,
                                                          @Nullable String key,
                                                          @Nullable String value,
                                                          int cursor,
                                                          @Nullable Collection<String> otherTags) {
        if (value != null && otherTags != null) {
            throw new IllegalArgumentException("Both value and otherTags can't be non-null at the same time!");
        }

        if (key == null && value == null) {
            throw new IllegalArgumentException("Both key and value can't be null at the same time!");
        }

        boolean components = type.equals(SuggestionType.ITEM);

        boolean nextTagAllowed;

        if (value == null) {
            key = key.substring(0, cursor);
            nextTagAllowed = false;
        } else {
            nextTagAllowed = (cursor < value.length());
            value = value.substring(0, cursor);
        }

        if (key != null && (key.contains("{") || key.contains("["))) {
            return new SuggestionsBuilder("", 0).buildFuture();
        }

        StringBuilder pathBuilder = new StringBuilder();

        boolean firstKey = true;

        if (nbt != null) {
            for (String piece : path) {
                switch (nbt) {
                    case NbtCompound compound -> {
                        if (firstKey && components) {
                            pathBuilder.append('[');
                            pathBuilder.append(piece);
                            pathBuilder.append('=');
                        } else {
                            pathBuilder.append('{');
                            pathBuilder.append(escapeKey(piece));
                            pathBuilder.append(':');
                        }
                        nbt = compound.get(piece);
                    }
                    case NbtList list -> {
                        pathBuilder.append('[');
                        nbt = list.get(Integer.parseInt(piece));
                    }
                    case null -> {
                        // No-op
                    }
                    default -> {
                        return new SuggestionsBuilder("", 0).buildFuture();
                    }
                }
                firstKey = false;
            }
        }

        int fieldStart = pathBuilder.length();

        if (key != null) {
            if (nbt instanceof NbtCompound) {
                if (firstKey && components) {
                    pathBuilder.append('[');
                } else {
                    pathBuilder.append('{');
                }
            } else if (nbt instanceof NbtList) {
                pathBuilder.append('[');
            } else {
                return new SuggestionsBuilder("", 0).buildFuture();
            }
            fieldStart = pathBuilder.length();

            if (nbt instanceof NbtCompound) {
                if (firstKey && components) {
                    pathBuilder.append(key);
                } else {
                    String escapedKey = escapeKey(key);
                    pathBuilder.append(key.equals(escapedKey) ? key : escapedKey.substring(0, escapedKey.length() - 1));
                }
            }

            if (value != null) {
                if (nbt instanceof NbtCompound) {
                    if (firstKey && components) {
                        pathBuilder.append('=');
                    } else {
                        pathBuilder.append(':');
                    }
                    fieldStart = pathBuilder.length();
                }
                pathBuilder.append(value);
            }
        } else {
            if (firstKey && components) {
                pathBuilder.append("[container=[{item:{id:\"" + id + "\",components:");
                fieldStart = pathBuilder.length();
            }
            pathBuilder.append(value);
        }
        String pathStr = pathBuilder.toString();

        String suggestionId = type + "/" + id;
        final int fieldStartFinal = fieldStart;
        final String valueFinal = value;
        final boolean firstKeyFinal = firstKey;
        return loadFromName(suggestionId, pathStr, components).thenApply(suggestions -> {
            List<Suggestion> shiftedSuggestions = suggestions.getList().stream()
                    .filter(suggestion ->
                            !(suggestion.getText().isEmpty()) &&
                                    !(valueFinal == null && suggestion.getText().contains(":")) &&
                                    !(valueFinal == null && firstKeyFinal && components && suggestion.getText().contains("{")) &&
                                    !(!nextTagAllowed && (suggestion.getText().contains(",") || suggestion.getText().contains("}"))) &&
                                    !(otherTags != null && otherTags.contains(suggestion.getText())))
                    .map(suggestion -> {
                        suggestion = shiftSuggestion(suggestion, -fieldStartFinal);
                        if (firstKeyFinal && components) {
                            if (suggestion.getText().endsWith("=")) {
                                String newText = suggestion.getText().substring(0, suggestion.getText().length() - 1);

                                if (otherTags.contains(newText) || otherTags.contains(MiscUtil.addNamespace(newText))) {
                                    return null;
                                }

                                suggestion = new Suggestion(suggestion.getRange(), newText, suggestion.getTooltip());
                            }
                        }
                        return suggestion;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            return new Suggestions(shiftRange(suggestions.getRange(), -fieldStartFinal), shiftedSuggestions);
        });
    }

    private String escapeKey(String key) {
        if (key.isEmpty() || Pattern.compile("[A-Za-z0-9._+-]+").matcher(key).matches()) {
            return key;
        }
        return NbtString.escape(key);
    }

    private CompletableFuture<Suggestions> loadFromName(String name, String tag, boolean components) {
        if (components) {
            name = name.substring("item/".length());
            int shift = name.length();
            SuggestionsBuilder builder = new SuggestionsBuilder(name + tag, 0);
            return new ItemStringReader(DynamicRegistryManagerHolder.get()).getSuggestions(builder).thenApply(suggestions -> {
                return new Suggestions(shiftRange(suggestions.getRange(), -shift), suggestions.getList().stream()
                        .map(suggestion -> shiftSuggestion(suggestion, -shift)).collect(Collectors.toList()));
            });
        }
        return NbtSuggestionManager.loadFromName(name, tag, new SuggestionsBuilder(tag, 0), false);
    }

    public CompletableFuture<Suggestions> getSuggestions(LocalNBT nbt, List<String> path, String key, String value, int cursor, Collection<String> otherTags) {
        return switch (nbt) {
            case LocalItem ignored ->
                    getSuggestions(SuggestionType.ITEM, nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
            case LocalBlock ignored ->
                    getSuggestions(SuggestionType.BLOCK, nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
            case LocalEntity ignored ->
                    getSuggestions(SuggestionType.ENTITY, nbt.getId(), nbt.getNBT(), path, key, value, cursor, otherTags);
            case null, default -> new SuggestionsBuilder("", 0).buildFuture();
        };
    }

    public CompletableFuture<Suggestions> getSuggestions(LocalNBT nbt, List<String> path, String key, String value, int cursor) {
        return getSuggestions(nbt, path, key, value, cursor, null);
    }

    enum SuggestionType {
        ITEM,
        BLOCK,
        ENTITY
    }
}
