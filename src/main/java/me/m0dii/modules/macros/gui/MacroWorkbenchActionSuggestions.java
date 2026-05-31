package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.scripting.ScriptStorage;
import me.m0dii.utils.StringUtils;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

public final class MacroWorkbenchActionSuggestions {
    private static List<String> allItemIds;
    private static List<String> allBlockIds;
    private static List<String> allEntityIds;

    private MacroWorkbenchActionSuggestions() {
    }

    public static List<String> forAction(MacroHudDataHandler.HudElement selected, String actionPrefix) {
        if (selected != null && selected.type == MacroHudDataHandler.ElementType.ICON) {
            String prefix = StringUtils.safe(actionPrefix).toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String id : iconIdSuggestionsForKind(selected.iconKind)) {
                String candidate = StringUtils.safe(id);
                String lower = candidate.toLowerCase(Locale.ROOT);
                if (prefix.isBlank() || lower.startsWith(prefix) || lower.contains(prefix)) {
                    matches.add(candidate);
                }
            }
            return matches;
        }
        return sourceTokenSuggestions(actionPrefix);
    }

    public static int nextSelectableIndex(List<String> suggestions, int start, int direction) {
        if (suggestions == null || suggestions.isEmpty()) {
            return -1;
        }
        int dir = direction < 0 ? -1 : 1;
        int index = start;
        for (int i = 0; i < suggestions.size(); i++) {
            index += dir;
            if (index < 0) {
                index = suggestions.size() - 1;
            }
            if (index >= suggestions.size()) {
                index = 0;
            }
            if (!isHeader(suggestions.get(index))) {
                return index;
            }
        }
        return -1;
    }

    public static boolean isHeader(String suggestion) {
        return suggestion != null && suggestion.startsWith("[") && suggestion.endsWith("]");
    }

    public static String value(String suggestion) {
        if (suggestion == null) {
            return "";
        }
        int idx = suggestion.indexOf(" :: ");
        if (idx < 0) {
            return suggestion;
        }
        return suggestion.substring(idx + 4);
    }

    public static String[] iconIdSuggestionsForKind(String kind) {
        ensureIconSuggestionCaches();
        if ("block".equalsIgnoreCase(kind)) {
            return allBlockIds.toArray(String[]::new);
        }
        if ("entity".equalsIgnoreCase(kind)) {
            return allEntityIds.toArray(String[]::new);
        }
        if ("entity_model".equalsIgnoreCase(kind)) {
            return new String[]{"player", "minecraft:player"};
        }
        return allItemIds.toArray(String[]::new);
    }

    private static List<String> sourceTokenSuggestions(String prefix) {
        String p = StringUtils.safe(prefix).toLowerCase(Locale.ROOT);
        LinkedHashMap<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("Player", new ArrayList<>());
        grouped.put("Inventory", new ArrayList<>());
        grouped.put("Armor", new ArrayList<>());
        grouped.put("Container", new ArrayList<>());
        grouped.put("World", new ArrayList<>());
        grouped.put("Target", new ArrayList<>());
        grouped.put("Macro", new ArrayList<>());
        grouped.put("Script", new ArrayList<>());
        grouped.put("Variables", new ArrayList<>());
        grouped.put("Commands", new ArrayList<>());

        for (String token : MacroPlaceholders.getKnownPlaceholderTokens()) {
            String category = categorizeSuggestion(token);
            grouped.computeIfAbsent(category, ignored -> new ArrayList<>()).add(token);
        }

        grouped.get("Commands").addAll(List.of(
                "cmd:/",
                "msg:",
                "say:",
                "copy:",
                "bar:",
                "if:{left}=={right}::cmd:/say yes:else:cmd:/say no"
        ));

        grouped.get("Variables").addAll(List.of(
                "{player.name}",
                "{player.uuid}",
                "{pos.x} {pos.y} {pos.z}",
                "{client.fps}",
                "{world.time.clock}"
        ));

        grouped.get("Script").addAll(List.of(
                "groovy:player.sendMessage(net.minecraft.text.Text.literal('Hi from Groovy'), false)",
                "kotlin:player.sendMessage(net.minecraft.text.Text.literal(\"Hi from Kotlin\"), false)",
                "example.groovy",
                "example.kts"
        ));
        grouped.get("Script").addAll(ScriptStorage.listScripts());

        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> values = entry.getValue().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            List<String> filtered = new ArrayList<>();
            for (String value : values) {
                String lower = value.toLowerCase(Locale.ROOT);
                if (p.isBlank() || lower.startsWith(p) || lower.contains(p)) {
                    filtered.add(value);
                }
            }
            if (filtered.isEmpty()) {
                continue;
            }
            out.add("[" + entry.getKey() + "]");
            for (String value : filtered) {
                out.add(entry.getKey() + " :: " + value);
            }
        }
        return out;
    }

    private static String categorizeSuggestion(String token) {
        String t = StringUtils.safe(token).toLowerCase(Locale.ROOT);
        if (t.startsWith("inventory.") || t.startsWith("hand.") || t.startsWith("offhand.")) return "Inventory";
        if (t.startsWith("armor.")) return "Armor";
        if (t.startsWith("container.")) return "Container";
        if (t.startsWith("world.") || t.startsWith("dim") || t.startsWith("pos.")) return "World";
        if (t.startsWith("look.") || t.startsWith("sel.") || t.startsWith("entities.")) return "Target";
        if (t.startsWith("players.") || t.startsWith("player.") || t.equals("hp") || t.equals("food") || t.equals("xp") || t.equals("level")) return "Player";
        if (t.startsWith("key.") || t.startsWith("cps.")) return "Macro";
        return "Variables";
    }

    private static void ensureIconSuggestionCaches() {
        if (allItemIds != null && allBlockIds != null && allEntityIds != null) {
            return;
        }
        allItemIds = Registries.ITEM.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
        allBlockIds = Registries.BLOCK.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
        allEntityIds = Registries.ENTITY_TYPE.getIds().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();
    }
}
