package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class AutomationEngine implements EventBus.Listener {
    public interface ContextSnapshotProvider {
        @NotNull Map<String, Object> snapshot(@NotNull AutomationRule.ConditionSource source, @NotNull AutomationEvent event);

        default @Nullable Object resolve(@NotNull AutomationRule.ConditionSource source,
                                         @NotNull String field,
                                         @NotNull AutomationEvent event) {
            return AutomationEngine.resolvePath(snapshot(source, event), field);
        }
    }

    public record RuleRuntimeSnapshot(@Nullable ExecutionRecord lastExecution,
                                      boolean pendingDebounce,
                                      long pendingDebounceDueAtMs) {
    }

    public record ExecutionRecord(boolean success,
                                  String message,
                                  long timestampMs,
                                  String eventSummary,
                                  long durationMs) {
    }

    public record LogEntry(long timestampMs, @Nullable String ruleId, String level, String message) {
    }

    private record PendingExecution(String ruleId, AutomationEvent event, long dueAtMs) {
    }

    private static final Comparator<AutomationRule> RULE_ORDER = Comparator
            .comparingInt((AutomationRule rule) -> rule.priority)
            .reversed()
            .thenComparing(rule -> rule.id == null ? "" : rule.id);

    private static final int DIAGNOSTIC_LIMIT = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final EventBus eventBus;
    private final ActionExecutor actionExecutor;
    private final ContextSnapshotProvider contextSnapshotProvider;

    private DataHandler.AutomationConfig config = new DataHandler.AutomationConfig();
    private List<AutomationRule> rules = new ArrayList<>();
    private final Map<String, RuleState> stateByRule = new HashMap<>();
    private final Map<String, PendingExecution> pendingDebounce = new HashMap<>();
    private final Deque<Long> globalExecutionTimes = new ArrayDeque<>();
    private final Deque<LogEntry> diagnostics = new ArrayDeque<>();
    private final Set<String> executingRuleIds = new HashSet<>();

    public AutomationEngine(@NotNull EventBus eventBus,
                            @NotNull ActionExecutor actionExecutor,
                            @NotNull ContextSnapshotProvider contextSnapshotProvider) {
        this.eventBus = Objects.requireNonNull(eventBus);
        this.actionExecutor = Objects.requireNonNull(actionExecutor);
        this.contextSnapshotProvider = Objects.requireNonNull(contextSnapshotProvider);
    }

    public void start() {
        DataHandler.load();
        setConfig(DataHandler.getConfigCopy(), false);
        eventBus.register(this);
    }

    public void stop() {
        eventBus.unregister(this);
    }

    public synchronized void reload() {
        DataHandler.load();
        setConfig(DataHandler.getConfigCopy(), false);
    }

    public synchronized void save() {
        DataHandler.AutomationConfig snapshot = DataHandler.getConfigCopy();
        snapshot.rules = copyRules(rules);
        snapshot.globalRateLimit = config.globalRateLimit;
        snapshot.loopPreventionBurstCount = config.loopPreventionBurstCount;
        snapshot.loopPreventionWindowMs = config.loopPreventionWindowMs;
        DataHandler.setConfig(snapshot);
        setConfig(snapshot, false);
    }

    public synchronized void setRules(@NotNull Collection<AutomationRule> nextRules) {
        DataHandler.AutomationConfig snapshot = DataHandler.getConfigCopy();
        snapshot.rules = copyRules(nextRules);
        DataHandler.setConfig(snapshot);
        setConfig(snapshot, false);
    }

    public synchronized void setRulesInMemory(@NotNull Collection<AutomationRule> nextRules) {
        this.rules = copyRules(nextRules);
        this.rules.sort(RULE_ORDER);
        this.stateByRule.keySet().retainAll(ruleIds(this.rules));
        this.pendingDebounce.keySet().retainAll(ruleIds(this.rules));
    }

    public synchronized void configureInMemory(int globalMaxExecutions,
                                               long globalWindowMs,
                                               int loopBurstCount,
                                               long loopWindowMs) {
        DataHandler.GlobalRateLimit limit = new DataHandler.GlobalRateLimit();
        limit.maxExecutions = Math.max(1, globalMaxExecutions);
        limit.windowMs = Math.max(250L, globalWindowMs);
        config.globalRateLimit = limit;
        config.loopPreventionBurstCount = Math.max(1, loopBurstCount);
        config.loopPreventionWindowMs = Math.max(250L, loopWindowMs);
    }

    public synchronized void upsertRule(@NotNull AutomationRule rule) {
        List<AutomationRule> nextRules = copyRules(rules);
        boolean replaced = false;
        for (int i = 0; i < nextRules.size(); i++) {
            if (Objects.equals(nextRules.get(i).id, rule.id)) {
                nextRules.set(i, rule.copy());
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            nextRules.add(rule.copy());
        }
        setRules(nextRules);
    }

    public synchronized boolean removeRule(@NotNull String ruleId) {
        List<AutomationRule> nextRules = copyRules(rules);
        boolean removed = nextRules.removeIf(rule -> Objects.equals(rule.id, ruleId));
        if (removed) {
            setRules(nextRules);
            stateByRule.remove(ruleId);
            pendingDebounce.remove(ruleId);
        }
        return removed;
    }

    public synchronized @Nullable AutomationRule findRule(String ruleId) {
        for (AutomationRule rule : rules) {
            if (Objects.equals(rule.id, ruleId)) {
                return rule.copy();
            }
        }
        return null;
    }

    public synchronized List<AutomationRule> getRules() {
        return copyRules(rules);
    }

    public synchronized RuleRuntimeSnapshot getRuntimeSnapshot(String ruleId) {
        RuleState state = stateByRule.computeIfAbsent(ruleId, ignored -> new RuleState());
        PendingExecution pending = pendingDebounce.get(ruleId);
        return new RuleRuntimeSnapshot(state.lastExecution, pending != null, pending == null ? 0L : pending.dueAtMs);
    }

    public synchronized List<LogEntry> getDiagnostics(int limit) {
        int max = Math.max(1, limit);
        List<LogEntry> entries = new ArrayList<>();
        int skipped = Math.max(0, diagnostics.size() - max);
        int index = 0;
        for (LogEntry entry : diagnostics) {
            if (index++ < skipped) {
                continue;
            }
            entries.add(entry);
        }
        return entries;
    }

    @Override
    public synchronized void onAutomationEvent(@NotNull AutomationEvent event) {
        flushDebounced(event.timestampMs());

        List<AutomationRule> activeRules = matchingRulesFor(event.type());
        for (AutomationRule rule : activeRules) {
            if (!matchesEventFilters(rule, event)) {
                continue;
            }
            if (!matchesConditions(rule, event)) {
                continue;
            }
            if (rule.debounceMs > 0L) {
                pendingDebounce.put(rule.id, new PendingExecution(rule.id, event, event.timestampMs() + rule.debounceMs));
                log(rule.id, "DEBUG", "Debounced until " + formatTime(event.timestampMs() + rule.debounceMs));
                continue;
            }
            executeRule(rule, event);
        }

        if (event.type() == AutomationEventType.TICK_INTERVAL) {
            flushDebounced(event.timestampMs());
        }
    }

    private void setConfig(DataHandler.AutomationConfig nextConfig, boolean persist) {
        this.config = nextConfig;
        this.rules = copyRules(nextConfig.rules);
        this.rules.sort(RULE_ORDER);
        this.stateByRule.keySet().retainAll(ruleIds(this.rules));
        this.pendingDebounce.keySet().retainAll(ruleIds(this.rules));
        if (persist) {
            save();
        }
    }

    private Set<String> ruleIds(List<AutomationRule> rules) {
        Set<String> ids = new HashSet<>();
        for (AutomationRule rule : rules) {
            ids.add(rule.id);
        }
        return ids;
    }

    private List<AutomationRule> matchingRulesFor(AutomationEventType type) {
        List<AutomationRule> matches = new ArrayList<>();
        for (AutomationRule rule : rules) {
            if (!rule.enabled || rule.eventType != type) {
                continue;
            }
            matches.add(rule);
        }
        matches.sort(RULE_ORDER);
        return matches;
    }

    private void flushDebounced(long nowMs) {
        if (pendingDebounce.isEmpty()) {
            return;
        }

        List<PendingExecution> due = pendingDebounce.values().stream()
                .filter(entry -> entry.dueAtMs <= nowMs)
                .sorted((left, right) -> {
                    AutomationRule leftRule = lookupRule(left.ruleId);
                    AutomationRule rightRule = lookupRule(right.ruleId);
                    int leftPriority = leftRule == null ? 0 : leftRule.priority;
                    int rightPriority = rightRule == null ? 0 : rightRule.priority;
                    return Integer.compare(rightPriority, leftPriority);
                })
                .toList();

        for (PendingExecution pending : due) {
            pendingDebounce.remove(pending.ruleId);
            AutomationRule rule = lookupRule(pending.ruleId);
            if (rule == null || !rule.enabled) {
                continue;
            }
            executeRule(rule, pending.event);
        }
    }

    private @Nullable AutomationRule lookupRule(String ruleId) {
        for (AutomationRule rule : rules) {
            if (Objects.equals(rule.id, ruleId)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesEventFilters(AutomationRule rule, AutomationEvent event) {
        if (rule.eventFilters == null || rule.eventFilters.isEmpty()) {
            return true;
        }

        for (AutomationRule.EventFilter filter : rule.eventFilters) {
            if (!matchesFilter(filter, event)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesFilter(AutomationRule.EventFilter filter, AutomationEvent event) {
        if (filter == null || filter.field == null || filter.field.isBlank()) {
            return true;
        }

        String field = filter.field.trim();
        if (event.type() == AutomationEventType.TICK_INTERVAL
                && (field.equalsIgnoreCase("intervalTicks") || field.equalsIgnoreCase("everyTicks"))) {
            long everyTicks = parseLong(filter.value, 0L);
            if (everyTicks <= 0L) {
                return false;
            }
            return event.clientTick() > 0L && event.clientTick() % everyTicks == 0L;
        }

        Object actual = event.attribute(field);
        return compareValues(actual, filter.operator, filter.value, filter.ignoreCase);
    }

    private boolean matchesConditions(AutomationRule rule, AutomationEvent event) {
        if (rule.conditions == null || rule.conditions.isEmpty()) {
            return true;
        }

        for (AutomationRule.Condition condition : rule.conditions) {
            if (condition == null) {
                continue;
            }
            AutomationRule.ConditionSource source = condition.source == null ? AutomationRule.ConditionSource.EVENT : condition.source;
            Object actual = contextSnapshotProvider.resolve(source, safeField(condition.field), event);
            if (!compareValues(actual, condition.operator, condition.value, condition.ignoreCase)) {
                return false;
            }
        }
        return true;
    }

    private boolean compareValues(@Nullable Object actual, @Nullable AutomationRule.Operator operator, @Nullable String expectedRaw, boolean ignoreCase) {
        AutomationRule.Operator op = operator == null ? AutomationRule.Operator.EQUALS : operator;
        String expected = expectedRaw == null ? "" : expectedRaw;

        return switch (op) {
            case EXISTS -> actual != null;
            case NOT_EXISTS -> actual == null;
            case EQUALS -> compareString(actual, expected, ignoreCase) == 0;
            case NOT_EQUALS -> compareString(actual, expected, ignoreCase) != 0;
            case CONTAINS -> stringValue(actual, ignoreCase).contains(normalize(expected, ignoreCase));
            case STARTS_WITH -> stringValue(actual, ignoreCase).startsWith(normalize(expected, ignoreCase));
            case ENDS_WITH -> stringValue(actual, ignoreCase).endsWith(normalize(expected, ignoreCase));
            case REGEX -> stringValue(actual, false).matches(expected);
            case GREATER_THAN -> compareNumbers(actual, expected) > 0;
            case GREATER_OR_EQUAL -> compareNumbers(actual, expected) >= 0;
            case LESS_THAN -> compareNumbers(actual, expected) < 0;
            case LESS_OR_EQUAL -> compareNumbers(actual, expected) <= 0;
            case IN_LIST -> {
                String actualString = stringValue(actual, ignoreCase);
                boolean found = false;
                for (String part : expected.split(",")) {
                    if (actualString.equals(normalize(part.trim(), ignoreCase))) {
                        found = true;
                        break;
                    }
                }
                yield found;
            }
        };
    }

    private int compareString(@Nullable Object actual, String expected, boolean ignoreCase) {
        String actualString = stringValue(actual, ignoreCase);
        String normalizedExpected = normalize(expected, ignoreCase);
        return actualString.compareTo(normalizedExpected);
    }

    private String stringValue(@Nullable Object value, boolean ignoreCase) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return normalize(text, ignoreCase);
    }

    private String normalize(String value, boolean ignoreCase) {
        if (value == null) {
            return "";
        }
        return ignoreCase ? value.toLowerCase(Locale.ROOT) : value;
    }

    private static String safeField(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private int compareNumbers(@Nullable Object actual, String expected) {
        double actualValue = parseDouble(actual == null ? null : String.valueOf(actual), Double.NaN);
        double expectedValue = parseDouble(expected, Double.NaN);
        if (Double.isNaN(actualValue) || Double.isNaN(expectedValue)) {
            return Integer.MIN_VALUE;
        }
        return Double.compare(actualValue, expectedValue);
    }

    private void executeRule(AutomationRule rule, AutomationEvent event) {
        RuleState state = stateByRule.computeIfAbsent(rule.id, ignored -> new RuleState());
        long nowMs = event.timestampMs();
        if (executingRuleIds.contains(rule.id)) {
            log(rule.id, "WARN", "Skipped due to active execution guard.");
            return;
        }
        if (!passesLoopGuard(rule, state, nowMs)) {
            log(rule.id, "WARN", "Skipped by loop prevention.");
            return;
        }
        if (!passesCooldown(rule, state, nowMs)) {
            log(rule.id, "DEBUG", "Skipped by cooldown.");
            return;
        }
        if (!passesRuleRateLimit(rule, state, nowMs)) {
            log(rule.id, "DEBUG", "Skipped by rule rate limit.");
            return;
        }
        if (!passesGlobalRateLimit(nowMs)) {
            log(rule.id, "WARN", "Skipped by global rate limit.");
            return;
        }

        long startedAt = System.currentTimeMillis();
        ActionExecutor.ActionResult result;
        executingRuleIds.add(rule.id);
        try {
            result = actionExecutor.execute(rule, event);
        } catch (Exception e) {
            result = ActionExecutor.ActionResult.error("Unexpected execution failure: " + e.getMessage());
        } finally {
            executingRuleIds.remove(rule.id);
        }

        markExecution(rule, state, nowMs);
        long durationMs = Math.max(0L, System.currentTimeMillis() - startedAt);
        state.lastExecution = new ExecutionRecord(result.success(), result.message(), nowMs, event.summary(), durationMs);
        log(rule.id, result.success() ? "INFO" : "ERROR", result.message());
    }

    private boolean passesCooldown(AutomationRule rule, RuleState state, long nowMs) {
        return rule.cooldownMs <= 0L
                || state.lastExecutionAtMs == Long.MIN_VALUE
                || nowMs - state.lastExecutionAtMs >= rule.cooldownMs;
    }

    private boolean passesRuleRateLimit(AutomationRule rule, RuleState state, long nowMs) {
        if (rule.rateLimitCount <= 0 || rule.rateLimitWindowMs <= 0L) {
            return true;
        }
        trimWindow(state.executionTimes, nowMs, rule.rateLimitWindowMs);
        return state.executionTimes.size() < rule.rateLimitCount;
    }

    private boolean passesGlobalRateLimit(long nowMs) {
        DataHandler.GlobalRateLimit limit = config.globalRateLimit == null ? new DataHandler.GlobalRateLimit() : config.globalRateLimit;
        trimWindow(globalExecutionTimes, nowMs, limit.windowMs);
        return globalExecutionTimes.size() < limit.maxExecutions;
    }

    private boolean passesLoopGuard(AutomationRule rule, RuleState state, long nowMs) {
        long loopWindowMs = Math.max(250L, config.loopPreventionWindowMs);
        int burstCount = Math.max(1, config.loopPreventionBurstCount);
        trimWindow(state.loopWindowExecutions, nowMs, loopWindowMs);
        return state.loopWindowExecutions.size() < burstCount;
    }

    private void markExecution(AutomationRule rule, RuleState state, long nowMs) {
        state.lastExecutionAtMs = nowMs;
        if (rule.rateLimitCount > 0 && rule.rateLimitWindowMs > 0L) {
            trimWindow(state.executionTimes, nowMs, rule.rateLimitWindowMs);
            state.executionTimes.addLast(nowMs);
        }
        trimWindow(state.loopWindowExecutions, nowMs, Math.max(250L, config.loopPreventionWindowMs));
        state.loopWindowExecutions.addLast(nowMs);

        DataHandler.GlobalRateLimit limit = config.globalRateLimit == null ? new DataHandler.GlobalRateLimit() : config.globalRateLimit;
        trimWindow(globalExecutionTimes, nowMs, limit.windowMs);
        globalExecutionTimes.addLast(nowMs);
    }

    private void trimWindow(Deque<Long> times, long nowMs, long windowMs) {
        long threshold = nowMs - Math.max(0L, windowMs);
        while (!times.isEmpty() && times.peekFirst() < threshold) {
            times.removeFirst();
        }
    }

    private void log(@Nullable String ruleId, String level, String message) {
        diagnostics.addLast(new LogEntry(System.currentTimeMillis(), ruleId, level, message));
        while (diagnostics.size() > DIAGNOSTIC_LIMIT) {
            diagnostics.removeFirst();
        }
    }

    private static List<AutomationRule> copyRules(Collection<AutomationRule> source) {
        List<AutomationRule> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (AutomationRule rule : source) {
            if (rule != null) {
                copy.add(rule.copy());
            }
        }
        copy.sort(RULE_ORDER);
        return copy;
    }

    private static @Nullable Object resolvePath(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isBlank()) {
            return null;
        }
        if (map.containsKey(path)) {
            return map.get(path);
        }
        Object current = map;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> nested)) {
                return null;
            }
            current = nested.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return Long.parseLong(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatTime(long timestampMs) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(timestampMs));
    }

    private static final class RuleState {
        private long lastExecutionAtMs = Long.MIN_VALUE;
        private final Deque<Long> executionTimes = new ArrayDeque<>();
        private final Deque<Long> loopWindowExecutions = new ArrayDeque<>();
        private ExecutionRecord lastExecution;
    }
}
