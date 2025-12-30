package me.m0dii.modules.chat;

import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SecondaryChatInputRouter {
    private SecondaryChatInputRouter() {
    }

    private static String lastRegex = null;
    private static Pattern lastPattern = null;

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!ModConfig.secondaryChatEnabled) {
                return true;
            }
            if (!ModConfig.secondaryChatRouteOutgoing) {
                return true;
            }
            if (message == null) {
                return true;
            }

            String regex = ModConfig.secondaryChatOutgoingRegex;
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

    private static Pattern compile(String regex) {
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

