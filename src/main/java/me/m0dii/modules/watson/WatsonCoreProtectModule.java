package me.m0dii.modules.watson;

import me.m0dii.modules.Module;
import me.m0dii.modules.macros.MacroPlaceholderProvider;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side CoreProtect visualizer inspired by Watson-style inspection overlays.
 */
public final class WatsonCoreProtectModule extends Module {
    public static final WatsonCoreProtectModule INSTANCE = new WatsonCoreProtectModule();

    private static final MacroPlaceholderProvider PLACEHOLDER_PROVIDER = new MacroPlaceholderProvider() {
        @Override
        public String getProviderId() {
            return "watson";
        }

        @Override
        public List<String> getPlaceholderDocs() {
            return List.of(
                    "[Module placeholders: Watson]",
                    "{watson.enabled} => true when  watson is active",
                    "{watson.entries.count} => number of recent CP entries being tracked",
                    "{watson.entries.last.n} => returns specified amount of last entries, e.g. {watson.entries.last.3} for last 3 entries, or 'N/A' if none",
                    "{watson.entries.first.loc} => location of the oldest tracked CP entry, or 'N/A' if none",
                    "{watson.entries.first.loc.x} => x coordinate of the oldest tracked CP entry",
                    "{watson.entries.first.loc.y} => y coordinate of the oldest tracked CP entry",
                    "{watson.entries.first.loc.z} => z coordinate of the oldest tracked CP entry",
                    "{watson.entries.first.time} => timestamp of the oldest tracked CP entry, or 'N/A' if none",
                    "{watson.entries.first.action} => action of the oldest tracked CP entry, or 'N/A' if none",
                    "{watson.entries.last.loc} => location of the most recent tracked CP entry, or 'N/A' if none",
                    "{watson.entries.last.loc.x} => x coordinate of the most recent tracked CP entry",
                    "{watson.entries.last.loc.y} => y coordinate of the most recent tracked CP entry",
                    "{watson.entries.last.loc.z} => z coordinate of the most recent tracked CP entry",
                    "{watson.entries.last.time} => timestamp of the most recent tracked CP entry, or 'N/A' if none",
                    "{watson.entries.last.action} => action of the most recent tracked CP entry, or 'N/A' if none"
            );
        }

        @Override
        public List<String> getKnownPlaceholderTokens() {
            return List.of(
                    "watson.enabled",
                    "watson.entries.count",
                    "watson.entries.last.<n>",
                    "watson.entries.first.loc",
                    "watson.entries.first.loc.x",
                    "watson.entries.first.loc.y",
                    "watson.entries.first.loc.z",
                    "watson.entries.first.time",
                    "watson.entries.first.action",
                    "watson.entries.last.loc",
                    "watson.entries.last.loc.x",
                    "watson.entries.last.loc.y",
                    "watson.entries.last.loc.z",
                    "watson.entries.last.time",
                    "watson.entries.last.action"
            );
        }

        @Override
        public String resolvePlaceholder(String token, MinecraftClient client, PlayerEntity player, boolean canvasMode) {
            if (token != null && token.matches("watson\\.entries\\.last\\.\\d+")) {
                int amount = 0;

                try {
                    String[] parts = token.split("\\.");
                    amount = Integer.parseInt(parts[parts.length - 1]);
                } catch (NumberFormatException e) {
                    // Not a number, ignore and return null below.
                }

                List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                if (snapshot.isEmpty()) {
                    return "N/A";
                }

                return snapshot.stream()
                        .skip(Math.max(0, snapshot.size() - amount))
                        .map(entry -> String.format("%s | %s | %s",
                                Instant.ofEpochMilli(entry.observedAt()),
                                entry.action().name(),
                                entry.pos().toShortString()))
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("N/A");
            }

            return switch (token) {
                case "watson.enabled" -> String.valueOf(INSTANCE.enabled);
                case "watson.entries.count", "watson.entries" -> String.valueOf(CoreProtectTracker.size());
                case "watson.entries.first.loc" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : snapshot.getFirst().pos().toShortString();
                }
                case "watson.entries.first.loc.x" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getFirst().pos().getX());
                }
                case "watson.entries.first.loc.y" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getFirst().pos().getY());
                }
                case "watson.entries.first.loc.z" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getFirst().pos().getZ());
                }
                case "watson.entries.first.time" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(Instant.ofEpochMilli(snapshot.getFirst().observedAt()));
                }
                case "watson.entries.first.action" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : snapshot.getFirst().action().name();
                }
                case "watson.entries.last.loc" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : snapshot.getLast().pos().toShortString();
                }
                case "watson.entries.last.loc.x" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getLast().pos().getX());
                }
                case "watson.entries.last.loc.y" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getLast().pos().getY());
                }
                case "watson.entries.last.loc.z" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(snapshot.getLast().pos().getZ());
                }
                case "watson.entries.last.time" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : String.valueOf(Instant.ofEpochMilli(snapshot.getLast().observedAt()));
                }
                case "watson.entries.last.action" -> {
                    List<CoreProtectEntry> snapshot = CoreProtectTracker.snapshot();

                    yield snapshot.isEmpty() ? "N/A" : snapshot.getLast().action().name();
                }

                default -> null;
            };
        }
    };

    private WatsonCoreProtectModule() {
        super("watson_coreprotect", "Watson CP", false);
    }

    @Override
    public void register() {
        WatsonCommands.register();
        CoreProtectRenderer.register();
        MacroPlaceholders.registerProvider(PLACEHOLDER_PROVIDER);

        registerPressedKeybind(
                KeybindCatalog.WATSON_COREPROTECT_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.WATSON_COREPROTECT_TOGGLE.defaultKey(),
                client -> toggleEnabled()
        );
    }

    @Override
    protected void onDisable() {
        // Keep entries by default so players can toggle renderer without losing recent context.
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Entries: " + CoreProtectTracker.size());
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Tracers: " + (CoreProtectRenderer.isTracersEnabled() ? "ON" : "OFF"));
        settings.add("Vectors: " + (CoreProtectRenderer.isVectorsEnabled() ? "ON" : "OFF"));
        settings.add("Labels: " + (CoreProtectRenderer.isLabelsEnabled() ? "ON" : "OFF"));
        settings.add("Outline Width: " + String.format("%.1f", CoreProtectRenderer.getOutlineLineWidth()));
        settings.add("Outline Color: " + CoreProtectRenderer.getOutlineColorPresetName());
        settings.add("Vector Width: " + String.format("%.1f", CoreProtectRenderer.getVectorLineWidth()));
        settings.add("Vector Color: " + CoreProtectRenderer.getVectorColorPresetName());
        settings.add("TTL(s): " + (CoreProtectTracker.getTtlMs() / 1000L));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 1) {
            toggleEnabled();
            return;
        }

        if (settingIndex == 2) {
            CoreProtectRenderer.setTracersEnabled(!CoreProtectRenderer.isTracersEnabled());
            return;
        }

        if (settingIndex == 3) {
            CoreProtectRenderer.setVectorsEnabled(!CoreProtectRenderer.isVectorsEnabled());
            return;
        }

        if (settingIndex == 4) {
            CoreProtectRenderer.setLabelsEnabled(!CoreProtectRenderer.isLabelsEnabled());
            return;
        }

        if (settingIndex == 5) {
            float current = CoreProtectRenderer.getOutlineLineWidth();
            float next = current >= 5.0f ? 1.0f : current + 0.5f;
            CoreProtectRenderer.setOutlineLineWidth(next);
            return;
        }

        if (settingIndex == 6) {
            CoreProtectRenderer.cycleOutlineColorPreset();
            return;
        }

        if (settingIndex == 7) {
            float current = CoreProtectRenderer.getVectorLineWidth();
            float next = current >= 5.0f ? 1.0f : current + 0.5f;
            CoreProtectRenderer.setVectorLineWidth(next);
            return;
        }

        if (settingIndex == 8) {
            CoreProtectRenderer.cycleVectorColorPreset();
            return;
        }

        if (settingIndex == 9) {
            // Cycle quickly between 30s, 60s, 120s, 300s for UI convenience.
            long current = CoreProtectTracker.getTtlMs() / 1000L;
            int next = switch ((int) current) {
                case 30 -> 60;
                case 60 -> 120;
                case 120 -> 300;
                default -> 30;
            };
            CoreProtectTracker.setTtlSeconds(next);
        }
    }
}

