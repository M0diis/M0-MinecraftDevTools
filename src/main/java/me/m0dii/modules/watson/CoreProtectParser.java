package me.m0dii.modules.watson;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses chat output produced by CoreProtect commands.
 */
public final class CoreProtectParser {
    private static final Pattern CP_BUSY = Pattern.compile("^CoreProtect - Database busy\\. Please try again later\\.$");
    private static final Pattern CP_NO_RESULT = Pattern.compile("^CoreProtect - No results found\\.$");
    private static final Pattern CP_SEARCH = Pattern.compile("^CoreProtect - Lookup searching\\. Please wait\\.\\.\\.$");
    private static final Pattern CP_LOOKUP_HEADER = Pattern.compile("^-{5}\\s*CoreProtect(?:\\s*\\|\\s*Lookup\\s+Results|\\s+Lookup\\s+Results)?\\s*-{5}$");
    private static final Pattern CP_INSPECTOR_COORDS = Pattern.compile("^-{5} \\w+(?:\\s\\w+)* -{5} \\(x(-?\\d+)\\/y(-?\\d+)\\/z(-?\\d+)\\)$");
    private static final Pattern CP_LOOKUP_COORDS = Pattern.compile("^\\s*\\^ \\(x(-?\\d+)\\/y(-?\\d+)\\/z(-?\\d+)\\/([^\\)]+)\\)(?: \\(.+\\))?$");
    private static final Pattern CP_DETAILS = Pattern.compile("^(?:\\s+)?(\\d+[.,]\\d+\\/[mhd] ago|\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2})\\s+[-+]\\s+#?(\\w+)\\s+((?!.*logged).*?)\\s+(.+)\\.$");

    private BlockPos inspectorPos;
    private PendingDetails pendingDetails;
    private boolean lookupMode;

    CoreProtectParser() {
        // Utility class
    }

    public Optional<CoreProtectEntry> parse(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String unformatted = stripFormatting(message);
        String normalized = unformatted.trim();

        if (CP_BUSY.matcher(normalized).matches() || CP_NO_RESULT.matcher(normalized).matches() || CP_SEARCH.matcher(normalized).matches()) {
            resetTransientState();
            return Optional.empty();
        }

        if (CP_LOOKUP_HEADER.matcher(normalized).matches()) {
            lookupMode = true;
            inspectorPos = null;
            return Optional.empty();
        }

        Matcher inspectorCoords = CP_INSPECTOR_COORDS.matcher(normalized);
        if (inspectorCoords.matches()) {
            lookupMode = false;
            inspectorPos = new BlockPos(
                    Integer.parseInt(inspectorCoords.group(1)),
                    Integer.parseInt(inspectorCoords.group(2)),
                    Integer.parseInt(inspectorCoords.group(3))
            );
            return Optional.empty();
        }

        Matcher details = CP_DETAILS.matcher(normalized);
        if (details.matches()) {
            String actor = details.group(2);
            CoreProtectEntry.Action action = resolveAction(details.group(3));

            if (lookupMode) {
                pendingDetails = new PendingDetails(actor, action, normalized);
                return Optional.empty();
            }

            if (inspectorPos != null) {
                return Optional.of(new CoreProtectEntry(inspectorPos, actor, action, System.currentTimeMillis(), normalized));
            }

            return Optional.empty();
        }

        Matcher lookupCoords = CP_LOOKUP_COORDS.matcher(normalized);
        if (lookupCoords.matches() && pendingDetails != null) {
            BlockPos pos = new BlockPos(
                    Integer.parseInt(lookupCoords.group(1)),
                    Integer.parseInt(lookupCoords.group(2)),
                    Integer.parseInt(lookupCoords.group(3))
            );

            PendingDetails pending = pendingDetails;
            pendingDetails = null;
            return Optional.of(new CoreProtectEntry(pos, pending.actor(), pending.action(), System.currentTimeMillis(), pending.rawLine()));
        }

        return Optional.empty();
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("\\u00A7.", "");
    }

    private void resetTransientState() {
        inspectorPos = null;
        pendingDetails = null;
        lookupMode = false;
    }

    private static CoreProtectEntry.Action resolveAction(String actionFragment) {
        if (actionFragment == null) {
            return CoreProtectEntry.Action.UNKNOWN;
        }

        String lower = actionFragment.toLowerCase();
        if (lower.contains("placed")) {
            return CoreProtectEntry.Action.PLACE;
        }
        if (lower.contains("removed") || lower.contains("broke")) {
            return CoreProtectEntry.Action.REMOVE;
        }
        if (lower.contains("used")) {
            return CoreProtectEntry.Action.USE;
        }
        if (lower.contains("lookup")) {
            return CoreProtectEntry.Action.LOOKUP;
        }
        return CoreProtectEntry.Action.UNKNOWN;
    }

    private record PendingDetails(String actor, CoreProtectEntry.Action action, String rawLine) {
    }
}

