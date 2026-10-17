package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Ready-made rules added with {@code /automation template <id>}. Templates that act on the server for you are
 * added switched off so you can check them first.
 */
public final class AutomationTemplates {
    public record Template(String id, String title, boolean startsEnabled, Function<Context, AutomationRule> factory) {
    }

    /** What a template may use from the current game (the player name for mention rules). */
    public record Context(@Nullable String playerName) {
    }

    public static final List<Template> ALL = List.of(
            new Template("mention_ping", "Ping me when someone writes my name in chat", true, ctx -> {
                AutomationRule rule = base("Mention ping", AutomationEventType.CHAT_RECEIVED_REGEX);
                String name = ctx.playerName() == null || ctx.playerName().isBlank() ? "YourName" : ctx.playerName();
                AutomationRule.EventFilter filter = AutomationRule.EventFilter.regex("message",
                        "(?<!\\w)" + Pattern.quote(name) + "(?!\\w)");
                filter.ignoreCase = true;
                rule.eventFilters.add(filter);
                // Skip your own chat lines ("<Name> ...").
                rule.eventFilters.add(AutomationRule.EventFilter.regex("message", "^(?!<" + Pattern.quote(name) + ">)"));
                rule.actions.add(AutomationRule.Action.message("Mentioned: {event.message}", "actionbar ping"));
                rule.cooldownMs = 3_000L;
                return rule;
            }),
            new Template("low_health_warning", "Warn when health drops to 3 hearts or less", true, ctx -> {
                AutomationRule rule = base("Low health warning", AutomationEventType.PLAYER_HEALTH_CHANGED);
                AutomationRule.EventFilter low = new AutomationRule.EventFilter();
                low.field = "toHealth";
                low.operator = AutomationRule.Operator.LESS_OR_EQUAL;
                low.value = "6";
                AutomationRule.EventFilter dropping = new AutomationRule.EventFilter();
                dropping.field = "deltaHealth";
                dropping.operator = AutomationRule.Operator.LESS_THAN;
                dropping.value = "0";
                rule.eventFilters.add(low);
                rule.eventFilters.add(dropping);
                rule.actions.add(AutomationRule.Action.message("⚠ Low health: {event.toHealth}", "actionbar ping"));
                rule.cooldownMs = 5_000L;
                return rule;
            }),
            new Template("death_coordinates", "Show where I died", true, ctx -> {
                AutomationRule rule = base("Death coordinates", AutomationEventType.PLAYER_DEATH);
                rule.actions.add(AutomationRule.Action.message(
                        "You died at {event.x} {event.y} {event.z} in {event.dimension}", ""));
                return rule;
            }),
            new Template("low_food_warning", "Warn when hunger gets low", true, ctx -> {
                AutomationRule rule = base("Low food warning", AutomationEventType.PLAYER_FOOD_CHANGED);
                AutomationRule.EventFilter low = new AutomationRule.EventFilter();
                low.field = "toFood";
                low.operator = AutomationRule.Operator.LESS_OR_EQUAL;
                low.value = "6";
                rule.eventFilters.add(low);
                rule.actions.add(AutomationRule.Action.message("Eat something (food {event.toFood})", "actionbar"));
                rule.cooldownMs = 30_000L;
                return rule;
            }),
            new Template("tpa_accept", "Accept teleport requests (/tpaccept) - added OFF, check the pattern first", false, ctx -> {
                AutomationRule rule = base("Auto-accept teleport requests", AutomationEventType.CHAT_RECEIVED_REGEX);
                rule.eventFilters.add(AutomationRule.EventFilter.regex("message",
                        "^(?<who>\\w{3,16}) has requested to teleport to you"));
                rule.actions.add(AutomationRule.Action.command("/tpaccept {event.group.who}"));
                rule.cooldownMs = 2_000L;
                return rule;
            })
    );

    private AutomationTemplates() {
    }

    public static @Nullable Template find(String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(t -> t.id().equals(wanted)).findFirst().orElse(null);
    }

    /** Builds the rule with a unique id among {@code existing}. */
    public static AutomationRule create(@NotNull Template template, @NotNull Context context, @NotNull List<AutomationRule> existing) {
        AutomationRule rule = template.factory().apply(context);
        String id = template.id();
        int suffix = 2;
        while (containsId(existing, id)) {
            id = template.id() + "_" + suffix++;
        }
        rule.id = id;
        rule.enabled = template.startsEnabled();
        return rule;
    }

    private static boolean containsId(List<AutomationRule> rules, String id) {
        return rules.stream().anyMatch(rule -> id.equals(rule.id));
    }

    private static AutomationRule base(String name, AutomationEventType type) {
        AutomationRule rule = new AutomationRule();
        rule.name = name;
        rule.eventType = type;
        return rule;
    }
}
