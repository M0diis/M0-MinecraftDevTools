package me.m0dii.modules.scripting.ide;

import me.m0dii.gui.theme.Theme;
import me.m0dii.modules.macros.MacroPlaceholders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static me.m0dii.modules.scripting.ide.ScriptText.isWord;
import static me.m0dii.modules.scripting.ide.ScriptText.normalize;

/**
 * Code completion for the script editor: reflective type index, member signatures, ranking and suggestion records.
 */
public final class ScriptCompletions {
    private ScriptCompletions() {
    }

    private static final String GLOBAL_COMPLETION_KEY = "__global__";

    private static final int MAX_COMPLETION_ITEMS_PER_TYPE = 4000;

    private static final Map<String, String> ROOT_TYPE_KEYS = new LinkedHashMap<>();

    private static final Map<String, Class<?>> KNOWN_TYPE_CLASSES = new LinkedHashMap<>();

    private static final Map<String, List<CompletionItem>> TYPE_COMPLETIONS = new LinkedHashMap<>();

    private static final Map<String, Map<String, String>> TYPE_MEMBER_TYPES = new LinkedHashMap<>();

    private static boolean completionCacheReady = false;

    public enum SuggestionKind {VARIABLE, PROPERTY, METHOD, KEYWORD, SNIPPET}

    public record Suggestion(String category, String token, String insertText, SuggestionKind kind, String detail,
                             int score) {
    }

    public record PlaceholderCtx(int replaceStart, String prefix) {
    }

    public record CodeCompletionContext(String qualifier, String prefix, int replaceStart, int replaceEnd) {
    }

    public record CompletionItem(String label, String insertText, String nextType, SuggestionKind kind,
                                 String detail) {
    }

    public record DisplaySuggestion(Suggestion suggestion, boolean groupHeader, String groupKey, int hiddenOverloads) {
    }

    public static String methodGroupKey(Suggestion suggestion) {
        String insert = suggestion.insertText();
        int idx = insert.indexOf('(');
        String base = idx > 0 ? insert.substring(0, idx) : insert;
        return suggestion.category() + "|" + base;
    }

    public static List<Suggestion> matchSuggestions(String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("Player", new ArrayList<>());
        grouped.put("Inventory", new ArrayList<>());
        grouped.put("Armor", new ArrayList<>());
        grouped.put("Container", new ArrayList<>());
        grouped.put("World", new ArrayList<>());
        grouped.put("Target", new ArrayList<>());
        grouped.put("Script", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());

        for (String token : MacroPlaceholders.getKnownPlaceholderTokens()) {
            grouped.computeIfAbsent(suggestionCategory(token), ignored -> new ArrayList<>()).add(token);
        }
        grouped.get("Inventory").add("inventory.count:diamond");
        grouped.get("Container").add("container.count:diamond");
        grouped.get("Script").add("script.name");
        grouped.get("Script").add("script.language");

        List<Suggestion> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : grouped.entrySet()) {
            LinkedHashSet<String> uniq = new LinkedHashSet<>(e.getValue());
            for (String token : uniq) {
                String lower = token.toLowerCase(Locale.ROOT);
                int rank = rankMatch(p, lower, lower, SuggestionKind.VARIABLE);
                if (rank >= 0) {
                    out.add(new Suggestion(e.getKey(), token, token, SuggestionKind.VARIABLE, "placeholder", rank));
                }
            }
        }

        out.sort(Comparator.comparingInt(Suggestion::score)
                .thenComparing(s -> s.category)
                .thenComparing(s -> s.token));
        return out;
    }

    public static PlaceholderCtx placeholderContext(String source, int cursor) {
        String text = normalize(source);
        int c = Math.clamp(cursor, 0, text.length());
        int start = c;
        while (start > 0 && isWord(text.charAt(start - 1))) {
            start--;
        }
        if (start <= 0 || text.charAt(start - 1) != '{') {
            return null;
        }
        return new PlaceholderCtx(start, text.substring(start, c));
    }

    public static CodeCompletionContext codeCompletionContext(String source, int cursor) {
        String text = normalize(source);
        int c = Math.clamp(cursor, 0, text.length());
        int start = c;
        while (start > 0 && isIdentifierChar(text.charAt(start - 1))) {
            start--;
        }
        String prefix = text.substring(start, c);

        if (start > 0 && text.charAt(start - 1) == '.') {
            int qEnd = start - 1;
            int qStart = qEnd;
            while (qStart > 0 && isQualifierChar(text.charAt(qStart - 1))) {
                qStart--;
            }
            if (qStart == qEnd) {
                return null;
            }
            String qualifier = text.substring(qStart, qEnd);
            return new CodeCompletionContext(qualifier, prefix, start, c);
        }

        if (!prefix.isBlank()) {
            return new CodeCompletionContext("", prefix, start, c);
        }
        return null;
    }

    public static List<Suggestion> resolveCodeSuggestions(CodeCompletionContext context) {
        if (context == null) {
            return List.of();
        }
        ensureCompletionCache();
        String qualifierRaw = context.qualifier() == null ? "" : context.qualifier().trim();
        String category = qualifierRaw.isBlank() ? "Global" : qualifierRaw;
        String typeKey = qualifierRaw.isBlank() ? GLOBAL_COMPLETION_KEY : resolveQualifierType(qualifierRaw);
        if (typeKey == null) {
            return List.of();
        }

        ensureTypeBuilt(typeKey);
        List<CompletionItem> tokens = TYPE_COMPLETIONS.getOrDefault(typeKey, List.of());
        String prefix = context.prefix() == null ? "" : context.prefix().toLowerCase(Locale.ROOT);
        List<Suggestion> out = new ArrayList<>();
        for (CompletionItem item : tokens) {
            String lower = item.label().toLowerCase(Locale.ROOT);
            String lowerInsert = item.insertText().toLowerCase(Locale.ROOT);
            int rank = rankMatch(prefix, lower, lowerInsert, item.kind());
            if (rank >= 0) {
                out.add(new Suggestion(category, item.label(), item.insertText(), item.kind(), item.detail(), rank));
            }
        }
        out.sort(Comparator.comparingInt(Suggestion::score)
                .thenComparing(s -> s.token().toLowerCase(Locale.ROOT)));
        return out;
    }

    private static synchronized void ensureCompletionCache() {
        if (completionCacheReady) {
            return;
        }
        completionCacheReady = true;

        registerRoot("client", net.minecraft.client.Minecraft.class.getName());
        registerRoot("player", net.minecraft.client.player.LocalPlayer.class.getName());
        registerRoot("world", net.minecraft.client.multiplayer.ClientLevel.class.getName());
        registerRoot("options", net.minecraft.client.Options.class.getName());
        registerRoot("server", net.minecraft.server.MinecraftServer.class.getName());
        registerRoot("source", net.minecraft.client.player.LocalPlayer.class.getName());
        registerRoot("m0", me.m0dii.modules.scripting.ScriptApi.class.getName());
        registerRoot("event", "me.m0dii.modules.automation.AutomationEvent");

        List<CompletionItem> global = new ArrayList<>();
        for (Map.Entry<String, String> root : ROOT_TYPE_KEYS.entrySet()) {
            global.add(new CompletionItem(root.getKey(), root.getKey(), root.getValue(), SuggestionKind.VARIABLE, "root"));
        }

        // Kotlin and Groovy useful built-ins/snippets.
        global.add(new CompletionItem("println(msg)", "println(msg)", null, SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("print(msg)", "print(msg)", null, SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("listOf(item1, item2)", "listOf(item1, item2)", "java.util.List", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("mutableListOf(item1, item2)", "mutableListOf(item1, item2)", "java.util.List", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("setOf(item1, item2)", "setOf(item1, item2)", "java.util.Set", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("mapOf(key to value)", "mapOf(key to value)", "java.util.Map", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("mutableMapOf(key to value)", "mutableMapOf(key to value)", "java.util.Map", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("arrayListOf(item1, item2)", "arrayListOf(item1, item2)", "java.util.ArrayList", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("hashMapOf(key to value)", "hashMapOf(key to value)", "java.util.HashMap", SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("forEach { item -> }", "forEach { item -> }", null, SuggestionKind.SNIPPET, "kotlin"));
        global.add(new CompletionItem("if", "if", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("else", "else", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("for", "for", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("while", "while", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("when", "when", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("return", "return", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("val", "val", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("var", "var", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("fun", "fun", null, SuggestionKind.KEYWORD, "keyword"));
        global.add(new CompletionItem("def", "def", null, SuggestionKind.KEYWORD, "groovy"));

        TYPE_COMPLETIONS.put(GLOBAL_COMPLETION_KEY, dedupeAndSort(global));
        for (String typeKey : new ArrayList<>(ROOT_TYPE_KEYS.values())) {
            ensureTypeBuilt(typeKey);
        }
    }

    private static void registerRoot(String rootName, String className) {
        Class<?> clazz = tryResolveClass(className);
        if (clazz != null) {
            String typeKey = clazz.getName();
            ROOT_TYPE_KEYS.put(rootName, typeKey);
            KNOWN_TYPE_CLASSES.put(typeKey, clazz);
            return;
        }

        // Keep fallback root for suggestions even if class cannot be resolved in this runtime.
        String fallbackTypeKey = "root:" + rootName;
        ROOT_TYPE_KEYS.put(rootName, fallbackTypeKey);
        TYPE_COMPLETIONS.putIfAbsent(fallbackTypeKey, List.of());
        TYPE_MEMBER_TYPES.putIfAbsent(fallbackTypeKey, new LinkedHashMap<>());
    }

    private static Class<?> tryResolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void ensureTypeBuilt(String typeKey) {
        if (typeKey == null || TYPE_COMPLETIONS.containsKey(typeKey)) {
            return;
        }
        Class<?> clazz = KNOWN_TYPE_CLASSES.get(typeKey);
        if (clazz == null) {
            TYPE_COMPLETIONS.put(typeKey, List.of());
            TYPE_MEMBER_TYPES.put(typeKey, new LinkedHashMap<>());
            return;
        }
        buildTypeCompletions(typeKey, clazz);
    }

    private static void buildTypeCompletions(String typeKey, Class<?> clazz) {
        List<CompletionItem> items = new ArrayList<>();
        Map<String, String> memberTypes = new LinkedHashMap<>();

        for (Field field : clazz.getFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            String name = field.getName();
            String nextType = toTypeKey(field.getType());
            items.add(new CompletionItem(name, name, nextType, SuggestionKind.PROPERTY, shortTypeName(field.getType())));
            memberTypes.putIfAbsent(name, nextType);
        }

        for (Method method : clazz.getMethods()) {
            if (method.isSynthetic() || Modifier.isPrivate(method.getModifiers())) {
                continue;
            }
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            String methodName = method.getName();
            String nextType = toTypeKey(method.getReturnType());
            String display = methodName + methodDisplaySignature(method) + " : " + shortTypeName(method.getReturnType());
            String insert = methodName + methodInsertSignature(method);
            String help = clazz == me.m0dii.modules.scripting.ScriptApi.class
                    ? me.m0dii.modules.scripting.ScriptApi.describe(methodName) : "";
            items.add(new CompletionItem(display, insert, nextType, SuggestionKind.METHOD,
                    help.isEmpty() ? shortTypeName(method.getReturnType()) : help));
            memberTypes.putIfAbsent(methodName, nextType);

            if (method.getParameterCount() == 0) {
                String property = propertyAliasForMethod(methodName);
                if (!property.isBlank()) {
                    items.add(new CompletionItem(property, property, nextType, SuggestionKind.PROPERTY, shortTypeName(method.getReturnType())));
                    memberTypes.putIfAbsent(property, nextType);
                }
            }

            if (nextType != null && shouldCacheType(method.getReturnType())) {
                KNOWN_TYPE_CLASSES.putIfAbsent(nextType, method.getReturnType());
            }
        }

        TYPE_COMPLETIONS.put(typeKey, dedupeAndSort(items));
        TYPE_MEMBER_TYPES.put(typeKey, memberTypes);
    }

    private static String methodDisplaySignature(Method method) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 0) {
            return "()";
        }
        StringBuilder out = new StringBuilder("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(shortTypeName(params[i]));
        }
        out.append(')');
        return out.toString();
    }

    private static String methodInsertSignature(Method method) {
        int count = method.getParameterCount();
        if (count == 0) {
            return "()";
        }
        StringBuilder out = new StringBuilder("(");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(parameterPlaceholder(method, i));
        }
        out.append(')');
        return out.toString();
    }

    private static String parameterPlaceholder(Method method, int index) {
        String realName = reflectedParameterName(method, index);
        if (!realName.isBlank()) {
            return realName;
        }
        Class<?> type = method.getParameterTypes()[index];
        if (type == null) {
            return "arg" + (index + 1);
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return "num" + (index + 1);
        }
        if (type == boolean.class || type == Boolean.class) {
            return "flag" + (index + 1);
        }
        if (type == String.class || CharSequence.class.isAssignableFrom(type)) {
            return "text" + (index + 1);
        }
        String base = shortTypeName(type);
        if (base.isBlank()) {
            base = "arg";
        }
        base = Character.toLowerCase(base.charAt(0)) + base.substring(1);
        return base + (index + 1);
    }

    private static String reflectedParameterName(Method method, int index) {
        try {
            String name = method.getParameters()[index].getName();
            if (name == null || name.isBlank()) {
                return "";
            }
            if (name.matches("arg\\d+")) {
                return "";
            }
            if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                return "";
            }
            for (int i = 1; i < name.length(); i++) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                    return "";
                }
            }
            return name;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String shortTypeName(Class<?> type) {
        if (type == null) {
            return "Any";
        }
        if (type.isArray()) {
            return shortTypeName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    private static String propertyAliasForMethod(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return "";
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        if (methodName.startsWith("has") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        return "";
    }

    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() == 1) {
            return value.toLowerCase(Locale.ROOT);
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean shouldCacheType(Class<?> type) {
        if (type == null || type.isPrimitive() || type.isArray() || type == Void.TYPE) {
            return false;
        }
        String name = type.getName();
        return name.startsWith("net.minecraft")
                || name.startsWith("java.util")
                || name.startsWith("java.lang");
    }

    private static String toTypeKey(Class<?> type) {
        if (!shouldCacheType(type)) {
            return null;
        }
        return type.getName();
    }

    private static List<CompletionItem> dedupeAndSort(List<CompletionItem> items) {
        LinkedHashMap<String, CompletionItem> uniq = new LinkedHashMap<>();
        for (CompletionItem item : items) {
            String key = item.label() + "|" + item.insertText();
            uniq.putIfAbsent(key, item);
        }
        List<CompletionItem> out = new ArrayList<>(uniq.values());
        out.sort(Comparator.comparing((CompletionItem i) -> i.label().toLowerCase(Locale.ROOT)));
        if (out.size() > MAX_COMPLETION_ITEMS_PER_TYPE) {
            return new ArrayList<>(out.subList(0, MAX_COMPLETION_ITEMS_PER_TYPE));
        }
        return out;
    }

    private static boolean isQualifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '(' || c == ')';
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static String resolveQualifierType(String qualifierRaw) {
        if (qualifierRaw == null || qualifierRaw.isBlank()) {
            return null;
        }
        String[] segments = qualifierRaw.split("\\.");
        if (segments.length == 0) {
            return null;
        }

        String typeKey = ROOT_TYPE_KEYS.get(segments[0]);
        if (typeKey == null) {
            return null;
        }
        for (int i = 1; i < segments.length; i++) {
            String rawMember = segments[i];
            if (rawMember.isBlank()) {
                break;
            }
            String member = rawMember.endsWith("()")
                    ? rawMember.substring(0, rawMember.length() - 2)
                    : rawMember;
            ensureTypeBuilt(typeKey);
            Map<String, String> memberTypes = TYPE_MEMBER_TYPES.get(typeKey);
            if (memberTypes == null) {
                return null;
            }
            String nextType = memberTypes.get(member);
            if (nextType == null) {
                return null;
            }
            typeKey = nextType;
        }
        return typeKey;
    }

    private static int rankMatch(String prefix, String labelLower, String insertLower, SuggestionKind kind) {
        if (prefix == null || prefix.isBlank()) {
            return kindPriority(kind) + 50;
        }
        if (labelLower.startsWith(prefix)) {
            return kindPriority(kind);
        }
        if (insertLower.startsWith(prefix)) {
            return kindPriority(kind) + 5;
        }
        if (labelLower.contains(prefix) || insertLower.contains(prefix)) {
            return kindPriority(kind) + 20;
        }
        return -1;
    }

    private static int kindPriority(SuggestionKind kind) {
        return switch (kind) {
            case PROPERTY -> 0;
            case METHOD -> 2;
            case VARIABLE -> 4;
            case KEYWORD -> 8;
            case SNIPPET -> 10;
        };
    }

    public static String suggestionKindTag(SuggestionKind kind) {
        return switch (kind) {
            case PROPERTY -> "[P]";
            case METHOD -> "[M]";
            case VARIABLE -> "[V]";
            case KEYWORD -> "[K]";
            case SNIPPET -> "[S]";
        };
    }

    public static int suggestionKindColor(SuggestionKind kind) {
        return switch (kind) {
            case PROPERTY -> 0xFFBDE8FF;
            case METHOD -> 0xFFE8D49E;
            case VARIABLE -> 0xFFD8E8FF;
            case KEYWORD -> Theme.ACCENT_TEXT;
            case SNIPPET -> 0xFFB9F7C0;
        };
    }

    private static String suggestionCategory(String token) {
        String t = token == null ? "" : token.toLowerCase(Locale.ROOT);
        if (t.startsWith("player") || t.equals("hp") || t.equals("food") || t.equals("xp") || t.equals("level")) {
            return "Player";
        }
        if (t.startsWith("inventory") || t.startsWith("hand") || t.startsWith("offhand")) {
            return "Inventory";
        }
        if (t.startsWith("armor")) {
            return "Armor";
        }
        if (t.startsWith("container")) {
            return "Container";
        }
        if (t.startsWith("world") || t.startsWith("pos") || t.startsWith("dim") || t.startsWith("dir")) {
            return "World";
        }
        if (t.startsWith("look") || t.startsWith("entities") || t.startsWith("players") || t.startsWith("sel")) {
            return "Target";
        }
        if (t.startsWith("script")) {
            return "Script";
        }
        return "Other";
    }
}
