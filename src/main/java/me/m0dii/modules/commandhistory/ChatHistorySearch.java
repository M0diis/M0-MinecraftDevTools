package me.m0dii.modules.commandhistory;

import java.util.*;

/**
 * Ctrl+R reverse search for the chat box: the text you typed is the query; each Ctrl+R steps to the next older
 * match (Ctrl+Shift+R goes back). Entries containing the query come first, then fuzzy matches (the query's
 * letters in order). Typing anything else starts a new search.
 */
public final class ChatHistorySearch {
    private String query = "";
    private List<String> matches = List.of();
    private int index = -1;
    private String lastApplied;

    /** Newest-first history entries matching {@code query}: substring matches, then in-order letter matches. */
    public static List<String> find(List<String> historyNewestFirst, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Set<String> contains = new LinkedHashSet<>();
        Set<String> fuzzy = new LinkedHashSet<>();
        for (String entry : historyNewestFirst) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String lower = entry.toLowerCase(Locale.ROOT);
            if (q.isEmpty() || lower.contains(q)) {
                contains.add(entry);
            } else if (isSubsequence(q, lower)) {
                fuzzy.add(entry);
            }
        }
        List<String> result = new ArrayList<>(contains);
        fuzzy.removeAll(contains);
        result.addAll(fuzzy);
        return result;
    }

    static boolean isSubsequence(String needle, String haystack) {
        int j = 0;
        for (int i = 0; i < haystack.length() && j < needle.length(); i++) {
            if (haystack.charAt(i) == needle.charAt(j)) {
                j++;
            }
        }
        return j == needle.length();
    }

    /** Next older match for what is in the box; {@code null} if nothing matches. */
    public String next(List<String> historyNewestFirst, String currentInput) {
        return step(historyNewestFirst, currentInput, 1);
    }

    /** Next newer match (Ctrl+Shift+R). */
    public String previous(List<String> historyNewestFirst, String currentInput) {
        return step(historyNewestFirst, currentInput, -1);
    }

    private String step(List<String> history, String currentInput, int direction) {
        String input = currentInput == null ? "" : currentInput;
        if (lastApplied == null || !input.equals(lastApplied)) {
            query = input;
            matches = find(history, input);
            index = direction > 0 ? -1 : matches.size();
        }
        if (matches.isEmpty()) {
            lastApplied = null;
            return null;
        }
        index = Math.floorMod(index + direction, matches.size());
        lastApplied = matches.get(index);
        return lastApplied;
    }

    /** True while the box still shows a search result. */
    public boolean isShowing(String currentInput) {
        return lastApplied != null && lastApplied.equals(currentInput);
    }

    public String status() {
        if (matches.isEmpty()) {
            return "history search: no match for '" + query + "'";
        }
        return "history search '" + query + "'  " + (index + 1) + "/" + matches.size() + "  (Ctrl+R older, Ctrl+Shift+R newer)";
    }

    public boolean hadNoMatch(String currentInput) {
        return lastApplied == null && matches.isEmpty() && query.equals(currentInput) && !query.isEmpty();
    }

    public void reset() {
        query = "";
        matches = List.of();
        index = -1;
        lastApplied = null;
    }
}
