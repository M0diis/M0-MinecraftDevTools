package me.m0dii.modules.itemdata;

import me.m0dii.modules.getdata.GetDataScreen;
import net.minecraft.registry.Registries;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ItemComponentCatalog {
    private ItemComponentCatalog() {
    }

    public static List<String> filterComponentIds(String query, Set<String> presentIds) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return Registries.DATA_COMPONENT_TYPE.getIds().stream()
                .map(id -> id.toString())
                .filter(id -> matchesFilter(id, normalized))
                .sorted(componentComparator(normalized, presentIds))
                .toList();
    }

    public static String label(String componentId) {
        return ItemComponentMetadata.label(componentId);
    }

    public static String description(String componentId) {
        return ItemComponentMetadata.description(componentId);
    }

    public static String typeHint(String componentId) {
        return ItemComponentMetadata.typeHint(componentId);
    }

    public static GetDataScreen.InlineSuggestionProvider jsonSuggestionProvider() {
        return ItemComponentCatalog::suggestJsonComponents;
    }

    private static GetDataScreen.InlineSuggestions suggestJsonComponents(String editorText, int cursor) {
        if (editorText == null || editorText.isBlank() || cursor < 0 || cursor > editorText.length()) {
            return null;
        }

        TokenRange token = findCurrentQuotedKeyToken(editorText, cursor);
        if (token == null) {
            return null;
        }

        int componentsBrace = findComponentsObjectOpenBrace(editorText, token.contentStart());
        if (componentsBrace < 0) {
            return null;
        }

        int depth = objectDepthWithinComponents(editorText, componentsBrace, token.contentStart());
        if (depth != 1) {
            return null;
        }

        String typed = editorText.substring(token.contentStart(), Math.min(token.contentEnd(), editorText.length()));
        List<String> suggestions = filterComponentIds(typed, Set.of()).stream()
                .limit(12)
                .toList();
        if (suggestions.isEmpty()) {
            return null;
        }
        return new GetDataScreen.InlineSuggestions(token.contentStart(), token.contentEnd(), suggestions);
    }

    private static boolean matchesFilter(String componentId, String normalized) {
        if (normalized.isEmpty()) {
            return true;
        }
        String lowerId = componentId.toLowerCase(Locale.ROOT);
        if (lowerId.contains(normalized)) {
            return true;
        }
        if (label(componentId).toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        if (description(componentId).toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        return typeHint(componentId).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static Comparator<String> componentComparator(String normalized, Set<String> presentIds) {
        return Comparator
                .comparing((String id) -> presentIds == null || !presentIds.contains(id))
                .thenComparingInt(id -> score(id, normalized))
                .thenComparingInt(ItemComponentMetadata::priority)
                .thenComparing(String::toString);
    }

    private static int score(String componentId, String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return 2;
        }
        String lowerId = componentId.toLowerCase(Locale.ROOT);
        String lowerLabel = label(componentId).toLowerCase(Locale.ROOT);
        if (lowerId.startsWith(normalized) || lowerLabel.startsWith(normalized)) {
            return 0;
        }
        if (lowerId.contains(normalized) || lowerLabel.contains(normalized)) {
            return 1;
        }
        return 2;
    }

    private static TokenRange findCurrentQuotedKeyToken(String text, int cursor) {
        int lineStart = Math.max(0, text.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1);
        int lineEnd = text.indexOf('\n', cursor);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }

        boolean inString = false;
        int contentStart = -1;
        int quoteEnd = -1;
        for (int i = lineStart; i < lineEnd; i++) {
            char ch = text.charAt(i);
            if (ch == '"' && !isEscaped(text, i)) {
                if (!inString) {
                    inString = true;
                    contentStart = i + 1;
                    continue;
                }
                int contentEnd = i;
                if (cursor >= contentStart && cursor <= contentEnd && isKeyToken(text, i + 1, lineEnd)) {
                    return new TokenRange(contentStart, contentEnd, i);
                }
                inString = false;
                quoteEnd = i;
            }
        }

        if (inString && contentStart >= 0 && cursor >= contentStart && cursor <= lineEnd && isKeyToken(text, cursor, lineEnd)) {
            return new TokenRange(contentStart, cursor, quoteEnd < 0 ? cursor : quoteEnd);
        }
        return null;
    }

    private static boolean isKeyToken(String text, int from, int lineEnd) {
        for (int i = Math.max(0, from); i < lineEnd; i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            return ch == ':';
        }
        return false;
    }

    private static int findComponentsObjectOpenBrace(String text, int beforeIndex) {
        int searchIndex = beforeIndex;
        while (searchIndex >= 0) {
            int marker = text.lastIndexOf("\"components\"", searchIndex);
            if (marker < 0) {
                return -1;
            }
            int brace = text.indexOf('{', marker);
            if (brace >= 0 && brace < beforeIndex) {
                int depth = objectDepthWithinComponents(text, brace, beforeIndex);
                if (depth > 0) {
                    return brace;
                }
            }
            searchIndex = marker - 1;
        }
        return -1;
    }

    private static int objectDepthWithinComponents(String text, int openBraceIndex, int untilIndex) {
        boolean inString = false;
        int depth = 1;
        for (int i = openBraceIndex + 1; i < Math.min(untilIndex, text.length()); i++) {
            char ch = text.charAt(i);
            if (ch == '"' && !isEscaped(text, i)) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth <= 0) {
                    return depth;
                }
            }
        }
        return depth;
    }

    private static boolean isEscaped(String text, int quoteIndex) {
        int backslashes = 0;
        for (int i = quoteIndex - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes & 1) == 1;
    }

    private record TokenRange(int contentStart, int contentEnd, int quoteEnd) {
    }
}
