package me.m0dii.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;

public final class NbtEditorUtils {
    private NbtEditorUtils() {
    }

    public record ChildEntry(String segment, String label, Tag value) {
    }

    public static Tag parseElement(String raw) throws CommandSyntaxException {
        return TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(raw));
    }

    public static CompoundTag parseCompound(String raw) throws CommandSyntaxException {
        return TagParser.parseCompoundAsArgument(new StringReader(raw));
    }

    public static CompoundTag parseCompoundOrNull(String raw) {
        try {
            return parseCompound(raw);
        } catch (CommandSyntaxException ignored) {
            return null;
        }
    }

    public static String prettySnbt(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }

        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '"' && (i == 0 || raw.charAt(i - 1) != '\\')) {
                inString = !inString;
                out.append(ch);
                continue;
            }
            if (inString) {
                out.append(ch);
                continue;
            }
            switch (ch) {
                case '{', '[' -> {
                    out.append(ch).append('\n');
                    indent++;
                    appendIndent(out, indent);
                }
                case '}', ']' -> {
                    out.append('\n');
                    indent = Math.max(0, indent - 1);
                    appendIndent(out, indent);
                    out.append(ch);
                }
                case ',' -> {
                    out.append(ch).append('\n');
                    appendIndent(out, indent);
                }
                case ':' -> out.append(": ");
                default -> out.append(ch);
            }
        }
        return out.toString();
    }

    public static boolean isContainer(Tag element) {
        return element instanceof CompoundTag || element instanceof ListTag;
    }

    public static String typeName(Tag element) {
        return switch (element) {
            case null -> "null";
            case StringTag ignored -> "string";
            case CompoundTag ignored -> "compound";
            case ListTag ignored -> "list";
            case ByteTag ignored -> "byte/boolean";
            case ShortTag ignored -> "short";
            case IntTag ignored -> "int";
            case LongTag ignored -> "long";
            case FloatTag ignored -> "float";
            case DoubleTag ignored -> "double";
            case ByteArrayTag ignored -> "byte[]";
            case IntArrayTag ignored -> "int[]";
            case LongArrayTag ignored -> "long[]";
            default -> element.getClass().getSimpleName();
        };
    }

    public static String summary(Tag element) {
        switch (element) {
            case null -> {
                return "(missing)";
            }
            case CompoundTag compound -> {
                return "compound (" + compound.keySet().size() + " keys)";
            }
            case ListTag list -> {
                return "list (" + list.size() + " entries)";
            }
            default -> {
            }
        }
        String raw = element.toString();
        if (raw.length() <= 56) {
            return raw;
        }
        return raw.substring(0, 53) + "...";
    }

    public static Tag resolvePath(Tag root, List<String> pathSegments) {
        Tag current = root;
        if (pathSegments == null) {
            return current;
        }
        for (String segment : pathSegments) {
            current = resolveChild(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public static Tag resolveChild(Tag container, String segment) {
        if (container instanceof CompoundTag compound) {
            return compound.get(segment);
        }
        if (container instanceof ListTag list) {
            int index = parseIndex(segment);
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        }
        return null;
    }

    public static List<ChildEntry> childEntries(Tag container) {
        List<ChildEntry> entries = new ArrayList<>();
        if (container instanceof CompoundTag compound) {
            compound.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(key -> entries.add(new ChildEntry(key, key, compound.get(key))));
            return entries;
        }
        if (container instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                entries.add(new ChildEntry(Integer.toString(i), "[" + i + "]", list.get(i)));
            }
        }
        return entries;
    }

    public static void setChild(Tag container, String segment, Tag value) {
        if (container instanceof CompoundTag compound) {
            compound.put(segment, value.copy());
            return;
        }
        if (container instanceof ListTag list) {
            int index = parseIndex(segment);
            if (index >= 0 && index < list.size()) {
                list.set(index, value.copy());
            }
        }
    }

    public static void removeChild(Tag container, String segment) {
        if (container instanceof CompoundTag compound) {
            compound.remove(segment);
            return;
        }
        if (container instanceof ListTag list) {
            int index = parseIndex(segment);
            if (index >= 0 && index < list.size()) {
                list.remove(index);
            }
        }
    }

    public static String formatPath(List<String> pathSegments) {
        if (pathSegments == null || pathSegments.isEmpty()) {
            return "<root>";
        }
        StringBuilder builder = new StringBuilder();
        for (String segment : pathSegments) {
            if (isNumericSegment(segment)) {
                builder.append('[').append(segment).append(']');
            } else {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(segment);
            }
        }
        return builder.toString();
    }

    public static String rowLabel(ChildEntry entry) {
        String preview = summary(entry.value());
        if (preview.isBlank()) {
            return entry.label() + " [" + typeName(entry.value()) + "]";
        }
        return entry.label() + " = " + preview;
    }

    public static boolean isNumeric(Tag element) {
        return element instanceof NumericTag;
    }

    public static boolean isFloating(Tag element) {
        return element instanceof FloatTag || element instanceof DoubleTag;
    }

    private static int parseIndex(String segment) {
        if (!isNumericSegment(segment)) {
            return -1;
        }
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isNumericSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void appendIndent(StringBuilder builder, int indent) {
        builder.repeat("  ", Math.max(0, indent));
    }
}
