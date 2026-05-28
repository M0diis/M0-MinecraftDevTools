package me.m0dii.modules.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SecondaryChatInputRouter {
    private SecondaryChatInputRouter() {
    }

    private static String lastRegex = null;
    private static Pattern lastPattern = null;

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            if (!settings.enabled) {
                return true;
            }
            if (!settings.routeOutgoing) {
                return true;
            }
            if (message == null) {
                return true;
            }

            String regex = settings.outgoingRegex;
            if (regex == null || regex.isBlank()) {
                return true;
            }

            Pattern p = compile(regex);
            if (p == null) {
                return true;
            }

            // If it matches, we block it from going to the server.
            return !p.matcher(message).find();
        });
    }

    private static Pattern compile(@NotNull String regex) {
        if (regex.equals(lastRegex)) {
            return lastPattern;
        }
        lastRegex = regex;
        try {
            lastPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            lastPattern = null;
        }
        return lastPattern;
    }
}

