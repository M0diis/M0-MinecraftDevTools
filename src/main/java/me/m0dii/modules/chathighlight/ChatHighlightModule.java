package me.m0dii.modules.chathighlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
import me.m0dii.utils.ConfigFileSafety;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Colours chosen words or regex matches in chat and can play a sound. Rules live in
 * {@code config/m0-dev-tools-chat-highlights.json} and are managed with {@code /highlight}.
 * By default your own name ({@code {me}}) is highlighted with a sound.
 */
public class ChatHighlightModule extends Module {
    public static final ChatHighlightModule INSTANCE = new ChatHighlightModule();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-chat-highlights.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long SOUND_COOLDOWN_MS = 1_000L;

    private List<HighlightRules.Rule> rules = new ArrayList<>();
    private long lastSoundAt;

    private ChatHighlightModule() {
        super("chat_highlights", "Chat Highlights", true);
    }

    @Override
    public void register() {
        load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("highlight")
                        .executes(ctx -> list(ctx.getSource()))
                        .then(ClientCommands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(ClientCommands.literal("add")
                                .then(ClientCommands.argument("color", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            HighlightRules.NAMED_COLORS.keySet().stream().sorted().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "color"),
                                                        StringArgumentType.getString(ctx, "text"), false)))))
                        .then(ClientCommands.literal("addregex")
                                .then(ClientCommands.argument("color", StringArgumentType.word())
                                        .then(ClientCommands.argument("regex", StringArgumentType.greedyString())
                                                .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "color"),
                                                        StringArgumentType.getString(ctx, "regex"), true)))))
                        .then(ClientCommands.literal("remove")
                                .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                                        .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))))
                        .then(ClientCommands.literal("sound")
                                .then(ClientCommands.argument("number", IntegerArgumentType.integer(1))
                                        .executes(ctx -> toggleSound(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number")))))
                        .then(ClientCommands.literal("test")
                                .then(ClientCommands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String text = StringArgumentType.getString(ctx, "text");
                                            ctx.getSource().sendFeedback(highlight(Component.literal("[test] " + text), false));
                                            return 1;
                                        })))));
    }

    /** Called for every chat line before it is shown. Returns the message unchanged when nothing matches. */
    public static Component apply(Component message) {
        try {
            return INSTANCE.isEnabled() ? INSTANCE.highlight(message, true) : message;
        } catch (RuntimeException e) {
            return message; // never break chat
        }
    }

    private Component highlight(Component message, boolean allowSound) {
        if (message == null || rules.isEmpty()) {
            return message;
        }
        String plain = message.getString();
        HighlightRules.Result result = HighlightRules.match(rules, plain, playerName());
        if (!result.matched()) {
            return message;
        }
        if (allowSound && result.sound()) {
            ping();
        }

        List<Map.Entry<Style, String>> parts = new ArrayList<>();
        message.visit((style, text) -> {
            parts.add(new AbstractMap.SimpleEntry<>(style, text));
            return Optional.empty();
        }, Style.EMPTY);
        StringBuilder joined = new StringBuilder();
        parts.forEach(part -> joined.append(part.getValue()));
        if (!joined.toString().equals(plain)) {
            return message; // unusual component; leave it alone
        }

        MutableComponent out = Component.empty();
        for (HighlightRules.Segment<Style> segment : HighlightRules.split(parts, result.spans())) {
            Style style = segment.rgb() == null
                    ? segment.style()
                    : segment.style().withColor(TextColor.fromRgb(segment.rgb())).withBold(true);
            out.append(Component.literal(segment.text()).setStyle(style));
        }
        return out;
    }

    private void ping() {
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && now - lastSoundAt >= SOUND_COOLDOWN_MS) {
            lastSoundAt = now;
            client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7F, 1.6F);
        }
    }

    private static String playerName() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? null : client.player.getName().getString();
    }

    // ---- commands -------------------------------------------------------------------------------------------

    private int list(FabricClientCommandSource source) {
        if (rules.isEmpty()) {
            source.sendFeedback(Component.literal("[Highlight] No rules. /highlight add <color> <word> (use {me} for your name)"));
            return 1;
        }
        for (int i = 0; i < rules.size(); i++) {
            HighlightRules.Rule rule = rules.get(i);
            int rgb = Math.max(0, HighlightRules.parseColor(rule.color));
            source.sendFeedback(Component.literal((i + 1) + ". ")
                    .append(Component.literal(rule.pattern).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))))
                    .append(Component.literal((rule.regex ? "  (regex)" : "") + (rule.sound ? "  ♪" : ""))));
        }
        return 1;
    }

    private int add(FabricClientCommandSource source, String color, String text, boolean regex) {
        if (HighlightRules.parseColor(color) < 0) {
            source.sendError(Component.literal("[Highlight] Unknown colour. Use a name like gold/aqua/red or #RRGGBB."));
            return 0;
        }
        HighlightRules.Rule rule = new HighlightRules.Rule();
        rule.pattern = text.trim();
        rule.color = color;
        rule.regex = regex;
        if (HighlightRules.compile(rule, playerName()) == null) {
            source.sendError(Component.literal("[Highlight] That pattern is not valid."));
            return 0;
        }
        rules.add(rule);
        save();
        source.sendFeedback(Component.literal("[Highlight] Added #" + rules.size() + ". Toggle a sound with /highlight sound " + rules.size()));
        return 1;
    }

    private int remove(FabricClientCommandSource source, int number) {
        if (number > rules.size()) {
            source.sendError(Component.literal("[Highlight] No rule #" + number));
            return 0;
        }
        HighlightRules.Rule removed = rules.remove(number - 1);
        save();
        source.sendFeedback(Component.literal("[Highlight] Removed '" + removed.pattern + "'."));
        return 1;
    }

    private int toggleSound(FabricClientCommandSource source, int number) {
        if (number > rules.size()) {
            source.sendError(Component.literal("[Highlight] No rule #" + number));
            return 0;
        }
        HighlightRules.Rule rule = rules.get(number - 1);
        rule.sound = !rule.sound;
        save();
        source.sendFeedback(Component.literal("[Highlight] Sound " + (rule.sound ? "on" : "off") + " for '" + rule.pattern + "'."));
        return 1;
    }

    // ---- storage --------------------------------------------------------------------------------------------

    private void load() {
        if (!Files.exists(FILE)) {
            HighlightRules.Rule me = new HighlightRules.Rule();
            me.pattern = "{me}";
            me.color = "gold";
            me.sound = true;
            rules = new ArrayList<>(List.of(me));
            save();
            return;
        }
        try {
            List<HighlightRules.Rule> loaded = GSON.fromJson(Files.readString(FILE), new TypeToken<List<HighlightRules.Rule>>() {
            }.getType());
            rules = new ArrayList<>();
            if (loaded != null) {
                loaded.stream().filter(r -> r != null && r.pattern != null && !r.pattern.isBlank()).forEach(rules::add);
            }
        } catch (IOException | RuntimeException e) {
            ConfigFileSafety.backupUnreadable(FILE, e);
            rules = new ArrayList<>();
        }
    }

    private void save() {
        try {
            ConfigFileSafety.writeAtomically(FILE, GSON.toJson(rules));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save chat highlights: {}", e.getMessage());
        }
    }
}
