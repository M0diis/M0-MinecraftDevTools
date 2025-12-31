package me.m0dii.modules.chat;

import lombok.Getter;
import me.m0dii.utils.ModConfig;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SecondaryChatManager {
    private SecondaryChatManager() {
    }

    private static final Deque<Text> buffer = new ArrayDeque<>();

    // Cached compiled regex
    private static String lastRegex = null;
    private static Pattern lastPattern = null;

    @Getter
    private static long lastMessageTime = System.currentTimeMillis();

    public static void clear() {
        buffer.clear();
    }

    public static List<Text> snapshot() {
        return new ArrayList<>(buffer);
    }

    public static void push(Text text) {
        if (text == null) {
            return;
        }
        int max = Math.max(1, ModConfig.secondaryChatMaxLines);
        // Deduplication: don't add if last message is identical
        if (!buffer.isEmpty() && buffer.getLast().equals(text)) {
            return;
        }
        buffer.addLast(text);
        while (buffer.size() > max) buffer.removeFirst();

        // Reset scroll to show latest messages
        SecondaryChatOverlay.resetScroll();
        lastMessageTime = System.currentTimeMillis();
    }

    public static boolean matchesFilter(Text text) {
        if (!ModConfig.secondaryChatEnabled) {
            return false;
        }
        if (text == null) {
            return false;
        }

        String s = text.getString();

        // Check regex list first (higher priority)
        List<String> regexList = ModConfig.secondaryChatRegexList;
        if (regexList != null && !regexList.isEmpty()) {
            for (String regex : regexList) {
                Pattern p = getCompiled(regex);
                if (p != null && p.matcher(s).find()) {
                    return true;
                }
            }
            return false; // no patterns matched
        }

        return false;
    }

    private static Pattern getCompiled(String regex) {
        if (regex.equals(lastRegex)) {
            return lastPattern;
        }
        lastRegex = regex;
        try {
            lastPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            // Silently fail on invalid regex
            lastPattern = null;
        }
        return lastPattern;
    }
}
