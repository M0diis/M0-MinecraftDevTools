package me.m0dii.modules.automation;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Copying rules between people/worlds as text. Imported rules that send commands, run scripts or toggle modules
 * are added switched off, because a shared rule could do anything once enabled.
 */
public final class AutomationTransfer {
    static final String FORMAT_KEY = "m0AutomationRules";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private AutomationTransfer() {
    }

    public record ImportResult(List<AutomationRule> rules, List<String> notes) {
    }

    public static String exportJson(@NotNull List<AutomationRule> rules) {
        JsonObject root = new JsonObject();
        root.addProperty(FORMAT_KEY, 1);
        JsonArray array = new JsonArray();
        for (AutomationRule rule : rules) {
            array.add(GSON.toJsonTree(rule.copy()));
        }
        root.add("rules", array);
        return GSON.toJson(root);
    }

    /**
     * Parses exported text (the wrapper object, a bare array, or a single rule). Ids are made unique against
     * {@code existingIds}. Throws {@link IllegalArgumentException} with a readable message on bad input.
     */
    public static ImportResult parse(String text, @NotNull Set<String> existingIds) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The clipboard is empty.");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(text.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The clipboard does not contain rule JSON.");
        }
        JsonArray array;
        if (root.isJsonArray()) {
            array = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().has("rules") && root.getAsJsonObject().get("rules").isJsonArray()) {
            array = root.getAsJsonObject().getAsJsonArray("rules");
        } else if (root.isJsonObject() && root.getAsJsonObject().has("eventType")) {
            array = new JsonArray();
            array.add(root);
        } else {
            throw new IllegalArgumentException("The clipboard does not contain automation rules.");
        }

        Set<String> seen = new LinkedHashSet<>(existingIds);
        List<AutomationRule> rules = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        for (JsonElement element : array) {
            AutomationRule raw;
            try {
                raw = GSON.fromJson(element, AutomationRule.class);
            } catch (RuntimeException e) {
                notes.add("Skipped an entry that is not a valid rule.");
                continue;
            }
            if (raw == null) {
                continue;
            }
            String requestedId = raw.id;
            AutomationRule rule = DataHandler.sanitizeRule(raw, seen);
            if (requestedId != null && !requestedId.isBlank() && !rule.id.equals(requestedId.trim().toLowerCase(java.util.Locale.ROOT))) {
                notes.add("'" + requestedId + "' already exists, imported as '" + rule.id + "'.");
            }
            List<String> risky = riskyActions(rule);
            if (!risky.isEmpty() && rule.enabled) {
                rule.enabled = false;
                notes.add("'" + rule.id + "' is OFF because it " + String.join(", ", risky)
                        + ". Check it, then /automation enable " + rule.id + ".");
            }
            for (AutomationRule.Action action : rule.actions) {
                if (action.type == AutomationActionType.RUN_SCRIPT) {
                    notes.add("'" + rule.id + "' needs script '" + action.target + "' in config/m0-dev-tools/scripts.");
                }
            }
            rules.add(rule);
        }
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found in the clipboard.");
        }
        return new ImportResult(rules, notes);
    }

    static List<String> riskyActions(AutomationRule rule) {
        Set<String> reasons = new LinkedHashSet<>();
        for (AutomationRule.Action action : rule.actions) {
            if (action == null || !action.enabled || action.type == null) {
                continue;
            }
            switch (action.type) {
                case SEND_CLIENT_COMMAND -> reasons.add("sends commands/chat");
                case RUN_SCRIPT -> reasons.add("runs a script");
                case TOGGLE_MODULE -> reasons.add("toggles modules");
                case RUN_MACRO -> reasons.add("runs a macro");
                case SHOW_MESSAGE -> {
                    // harmless
                }
            }
        }
        return new ArrayList<>(reasons);
    }
}
