package me.m0dii.modules.nbtcomplete;

import java.util.*;
import java.util.function.Function;

/**
 * Works out what can be typed at the end of a partial SNBT compound, NBT path or item component list, and
 * returns suggestions. Pure Java so it can be tested; the mixins feed it the text before the cursor.
 */
public final class NbtCompletion {
    /** Suggestions replace the input from {@code start} (relative to the text passed in) to its end. */
    public record Result(int start, List<String> suggestions) {
        public static Result none() {
            return new Result(0, List.of());
        }

        public boolean isEmpty() {
            return suggestions.isEmpty();
        }
    }

    private static final int LIMIT = 60;

    private NbtCompletion() {
    }

    private static final class Frame {
        final boolean list;
        final String node;
        final NbtSchema.Field element;
        final Set<String> keys = new HashSet<>();
        String key;
        boolean value;

        Frame(boolean list, String node, NbtSchema.Field element) {
            this.list = list;
            this.node = node;
            this.element = element;
        }
    }

    /**
     * Suggestions for the end of {@code text}, a partial compound starting with '{'.
     *
     * @param root     schema node of the outermost compound
     * @param registry ids per registry name ("item", "mob_effect", ...), used for id values
     */
    public static Result compound(String text, String root, Function<String, List<String>> registry) {
        if (text == null || text.isEmpty()) {
            return new Result(0, List.of("{"));
        }
        Deque<Frame> stack = new ArrayDeque<>();
        int tokenStart = 0;
        int quoteStart = -1;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            Frame top = stack.peek();
            switch (c) {
                case '"', '\'' -> {
                    quote = c;
                    quoteStart = i;
                }
                case '{' -> {
                    String node = top == null ? root : childNode(top);
                    if (top != null && !top.list) {
                        top.value = true;
                    }
                    stack.push(new Frame(false, node, null));
                    tokenStart = i + 1;
                }
                case '[' -> {
                    NbtSchema.Field element = top != null && !top.list && top.key != null ? NbtSchema.field(top.node, top.key) : null;
                    stack.push(new Frame(true, null, element));
                    tokenStart = i + 1;
                }
                case '}', ']' -> {
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                    tokenStart = i + 1;
                }
                case ':' -> {
                    if (top != null && !top.list && !top.value) {
                        top.key = unquote(text.substring(tokenStart, i).trim());
                        top.keys.add(top.key);
                        top.value = true;
                        tokenStart = i + 1;
                    }
                }
                case ',' -> {
                    if (top != null && !top.list) {
                        top.value = false;
                        top.key = null;
                    }
                    tokenStart = i + 1;
                }
                case ';' -> tokenStart = i + 1;
                default -> {
                    // part of a token
                }
            }
        }
        if (stack.isEmpty()) {
            return Result.none();
        }
        Frame top = stack.peek();
        int start = skipSpaces(text, tokenStart);
        if (quote != 0 && (quoteStart != start || top.list || !top.value)) {
            // Inside free text (a name, a quoted key...): only a quoted value right at the token start is completed.
            return Result.none();
        }
        String partial = text.substring(start);
        if (top.list) {
            if (top.element != null && top.element.kind() == NbtSchema.Kind.LIST_COMPOUND && partial.isEmpty()) {
                return new Result(start, List.of("{"));
            }
            return Result.none();
        }
        if (!top.value) {
            List<String> out = new ArrayList<>();
            for (String name : rank(names(NbtSchema.fields(top.node)), partial)) {
                if (!top.keys.contains(name)) {
                    out.add(name + ":");
                }
            }
            return new Result(start, cap(out));
        }
        NbtSchema.Field field = NbtSchema.field(top.node, top.key);
        if (field == null) {
            return Result.none();
        }
        return new Result(start, cap(rank(values(field, registry), partial)));
    }

    private static String childNode(Frame parent) {
        if (parent.list) {
            return parent.element != null && parent.element.kind() == NbtSchema.Kind.LIST_COMPOUND ? parent.element.child() : null;
        }
        if (parent.key == null) {
            return null;
        }
        NbtSchema.Field field = NbtSchema.field(parent.node, parent.key);
        return field != null && field.kind() == NbtSchema.Kind.COMPOUND ? field.child() : null;
    }

    /** Typical values to start typing for a field. */
    static List<String> values(NbtSchema.Field field, Function<String, List<String>> registry) {
        return switch (field.kind()) {
            case BOOL -> List.of("1b", "0b");
            case BYTE -> List.of("0b");
            case SHORT -> List.of("0s");
            case INT -> List.of("0");
            case LONG -> List.of("0L");
            case FLOAT -> List.of("0.0f", "1.0f");
            case DOUBLE -> List.of("0.0d", "1.0d");
            case STRING -> List.of("\"\"");
            case TEXT -> List.of("\"Name\"", "{text:\"Name\",color:\"gold\",italic:false}");
            case VEC3D -> List.of("[0.0d,0.0d,0.0d]");
            case VEC2F -> List.of("[0.0f,0.0f]");
            case FLOAT_LIST -> List.of("[0.0f,0.0f,0.0f]");
            case STRING_LIST -> List.of("[\"\"]");
            case COMPOUND, ANY -> List.of("{");
            case LIST_COMPOUND -> List.of("[{");
            case ID -> {
                List<String> out = new ArrayList<>();
                for (String id : registry == null ? List.<String>of() : registry.apply(field.child())) {
                    out.add("\"" + id + "\"");
                }
                yield out;
            }
        };
    }

    /**
     * Suggestions for an NBT path such as {@code equipment.mainhand.id} or {@code Items[0].count}.
     */
    public static Result path(String text, String root) {
        String value = text == null ? "" : text;
        int lastDot = Math.max(value.lastIndexOf('.'), value.lastIndexOf(']'));
        String node = root;
        if (lastDot >= 0) {
            for (String segment : value.substring(0, lastDot).split("\\.")) {
                String key = segment;
                int bracket = key.indexOf('[');
                if (bracket >= 0) {
                    key = key.substring(0, bracket);
                }
                if (key.isEmpty()) {
                    continue;
                }
                NbtSchema.Field field = NbtSchema.field(node, unquote(key));
                if (field == null || (field.kind() != NbtSchema.Kind.COMPOUND && field.kind() != NbtSchema.Kind.LIST_COMPOUND)) {
                    return Result.none();
                }
                node = field.child();
            }
        }
        int start = lastDot + 1;
        if (start < value.length() && value.charAt(start) == '.') {
            start++;
        }
        String partial = value.substring(Math.min(start, value.length()));
        if (partial.contains("[") || partial.contains("{")) {
            return Result.none();
        }
        List<String> out = new ArrayList<>();
        for (NbtSchema.Field field : NbtSchema.fields(node)) {
            if (field.kind() == NbtSchema.Kind.LIST_COMPOUND || field.kind() == NbtSchema.Kind.STRING_LIST
                    || field.kind() == NbtSchema.Kind.VEC3D || field.kind() == NbtSchema.Kind.VEC2F
                    || field.kind() == NbtSchema.Kind.FLOAT_LIST) {
                out.add(field.name() + "[");
            }
            out.add(field.name());
        }
        // A key that only appears with "[" (lists) is still offered plain, so either form can be picked.
        return new Result(start, cap(rank(new ArrayList<>(new LinkedHashSet<>(out)), partial)));
    }

    /** Common starting values for item components typed as {@code item[component=...]}. */
    private static final Map<String, List<String>> COMPONENT_VALUES = Map.ofEntries(
            Map.entry("custom_name", List.of("\"Name\"", "{text:\"Name\",color:\"gold\",italic:false}")),
            Map.entry("item_name", List.of("\"Name\"", "{text:\"Name\",color:\"aqua\"}")),
            Map.entry("lore", List.of("[\"Line 1\"]", "[{text:\"Line 1\",color:\"gray\",italic:false}]")),
            Map.entry("enchantments", List.of("{\"minecraft:sharpness\":5}", "{\"minecraft:efficiency\":5,\"minecraft:unbreaking\":3}")),
            Map.entry("stored_enchantments", List.of("{\"minecraft:mending\":1}")),
            Map.entry("damage", List.of("0")),
            Map.entry("max_damage", List.of("250")),
            Map.entry("max_stack_size", List.of("64", "99", "1")),
            Map.entry("unbreakable", List.of("{}")),
            Map.entry("enchantment_glint_override", List.of("true", "false")),
            Map.entry("rarity", List.of("\"common\"", "\"uncommon\"", "\"rare\"", "\"epic\"")),
            Map.entry("item_model", List.of("\"minecraft:diamond\"")),
            Map.entry("custom_model_data", List.of("{floats:[1.0f]}", "{strings:[\"name\"]}")),
            Map.entry("custom_data", List.of("{}")),
            Map.entry("dyed_color", List.of("16711680", "{rgb:16711680}")),
            Map.entry("tooltip_display", List.of("{hide_tooltip:true}", "{hidden_components:[\"minecraft:enchantments\"]}")),
            Map.entry("repair_cost", List.of("0")),
            Map.entry("food", List.of("{nutrition:4,saturation:2.4f,can_always_eat:true}")),
            Map.entry("consumable", List.of("{consume_seconds:1.6f,animation:\"eat\"}")),
            Map.entry("glider", List.of("{}")),
            Map.entry("fire_resistant", List.of("{}")),
            Map.entry("damage_resistant", List.of("{types:\"#minecraft:is_fire\"}")),
            Map.entry("potion_contents", List.of("{potion:\"minecraft:swiftness\"}", "{custom_color:16711680,custom_effects:[{id:\"minecraft:speed\",amplifier:1b,duration:600}]}")),
            Map.entry("attribute_modifiers", List.of("[{type:\"minecraft:attack_damage\",id:\"m0:bonus\",amount:5.0d,operation:\"add_value\",slot:\"mainhand\"}]")),
            Map.entry("profile", List.of("\"Notch\"", "{name:\"Notch\"}")),
            Map.entry("equippable", List.of("{slot:\"head\"}")),
            Map.entry("trim", List.of("{material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}")),
            Map.entry("can_break", List.of("{blocks:\"minecraft:stone\"}")),
            Map.entry("can_place_on", List.of("{blocks:\"minecraft:stone\"}")),
            Map.entry("use_cooldown", List.of("{seconds:1.0f}")),
            Map.entry("charged_projectiles", List.of("[{id:\"minecraft:arrow\"}]")),
            Map.entry("container", List.of("[{slot:0,item:{id:\"minecraft:diamond\",count:1}}]")),
            Map.entry("entity_data", List.of("{id:\"minecraft:zombie\"}")),
            Map.entry("block_state", List.of("{facing:\"north\"}")));

    /**
     * Value suggestions inside an item argument, e.g. {@code diamond_sword[custom_name=} or
     * {@code stone[minecraft:rarity="e}. Only the value right after '=' is completed; nested values are left
     * to the player.
     */
    public static Result componentValue(String text) {
        if (text == null) {
            return Result.none();
        }
        int depth = 0;
        int segmentStart = -1;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            switch (c) {
                case '"', '\'' -> quote = c;
                case '[', '{' -> {
                    depth++;
                    if (depth == 1 && c == '[') {
                        segmentStart = i + 1;
                    }
                }
                case ']', '}' -> depth--;
                case ',' -> {
                    if (depth == 1) {
                        segmentStart = i + 1;
                    }
                }
                default -> {
                    // token
                }
            }
        }
        if (segmentStart < 0) {
            return Result.none();
        }
        String segment = text.substring(segmentStart);
        int eq = segment.indexOf('=');
        if (eq < 0) {
            return Result.none();
        }
        String name = NbtSchema.stripNamespace(segment.substring(0, eq).replace("!", ""));
        String value = segment.substring(eq + 1);
        if (value.contains("{") || value.contains("[")) {
            return Result.none();
        }
        List<String> options = COMPONENT_VALUES.get(name);
        if (options == null) {
            return Result.none();
        }
        int start = segmentStart + eq + 1;
        return new Result(start, cap(rank(options, value)));
    }

    // ---- helpers ----

    private static List<String> names(List<NbtSchema.Field> fields) {
        List<String> names = new ArrayList<>();
        for (NbtSchema.Field field : fields) {
            names.add(field.name());
        }
        return names;
    }

    /** Prefix matches first (case-insensitive), then substring matches; everything when the partial is empty. */
    static List<String> rank(List<String> options, String partial) {
        String typed = unquote(partial.trim()).toLowerCase(Locale.ROOT);
        String rawTyped = partial.trim().toLowerCase(Locale.ROOT);
        if (typed.isEmpty() && rawTyped.isEmpty()) {
            return options;
        }
        List<String> prefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String option : options) {
            String lower = option.toLowerCase(Locale.ROOT);
            String bare = unquote(lower);
            if (lower.startsWith(rawTyped) || bare.startsWith(typed)
                    || NbtSchema.stripNamespace(bare).startsWith(typed)) {
                prefix.add(option);
            } else if (!typed.isEmpty() && bare.contains(typed)) {
                contains.add(option);
            }
        }
        prefix.addAll(contains);
        return prefix;
    }

    private static List<String> cap(List<String> list) {
        return list.size() > LIMIT ? list.subList(0, LIMIT) : list;
    }

    private static int skipSpaces(String text, int index) {
        int i = index;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    static String unquote(String token) {
        String t = token;
        if (!t.isEmpty() && (t.charAt(0) == '"' || t.charAt(0) == '\'')) {
            t = t.substring(1);
            if (!t.isEmpty() && (t.charAt(t.length() - 1) == '"' || t.charAt(t.length() - 1) == '\'')) {
                t = t.substring(0, t.length() - 1);
            }
        }
        return t;
    }
}
