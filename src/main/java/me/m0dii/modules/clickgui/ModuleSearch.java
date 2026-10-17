package me.m0dii.modules.clickgui;

import me.m0dii.modules.Module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/** Filters and orders modules for the module browser. */
final class ModuleSearch {
    private ModuleSearch() {
    }

    /**
     * Returns the modules matching {@code query}, favourites first, then by how well they match
     * (name prefix, word prefix, anywhere in name/id/category), then alphabetically. Every word of the query
     * has to match somewhere.
     */
    static List<Module> rank(List<Module> modules,
                             String query,
                             Predicate<Module> isFavorite,
                             Function<Module, String> categoryOf) {
        String[] words = query == null ? new String[0] : query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Scored> scored = new ArrayList<>();
        for (Module module : modules) {
            if (module == null) {
                continue;
            }
            int score = score(module.getDisplayName(), module.getId(), categoryOf.apply(module), words);
            if (score >= 0) {
                scored.add(new Scored(module, isFavorite.test(module), score));
            }
        }
        scored.sort(Comparator.comparing((Scored s) -> !s.favorite)
                .thenComparingInt(s -> s.score)
                .thenComparing(s -> s.module.getDisplayName().toLowerCase(Locale.ROOT)));
        return scored.stream().map(Scored::module).toList();
    }

    /** Lower is better; -1 means no match. */
    static int score(String displayName, String id, String category, String[] words) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String haystack = name + " " + (id == null ? "" : id.toLowerCase(Locale.ROOT).replace('_', ' '))
                + " " + (category == null ? "" : category.toLowerCase(Locale.ROOT));
        int total = 0;
        boolean any = false;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            any = true;
            if (name.startsWith(word)) {
                total += 0;
            } else if (name.contains(" " + word)) {
                total += 1;
            } else if (haystack.contains(word)) {
                total += 3;
            } else {
                return -1;
            }
        }
        return any ? total : 0;
    }

    private record Scored(Module module, boolean favorite, int score) {
    }
}
