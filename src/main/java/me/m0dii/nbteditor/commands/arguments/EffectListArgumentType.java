package me.m0dii.nbteditor.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.TextInst;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class EffectListArgumentType implements ArgumentType<Collection<StatusEffectInstance>> {

    public static final DynamicCommandExceptionType INVALID_EFFECT_EXCEPTION = new DynamicCommandExceptionType(id ->
            TextInst.translatable("effect.effectNotFound", id));

    private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:blindness -duration:1 -showparticles:false", "minecraft:jump_boost");

    private EffectListArgumentType() {
    }

    public static EffectListArgumentType effectList() {
        return new EffectListArgumentType();
    }

    public Collection<StatusEffectInstance> parse(@NotNull StringReader stringReader) throws CommandSyntaxException {
        List<StatusEffectInstance> effects = new ArrayList<>();

        while (stringReader.canRead()) {
            Identifier identifier = Identifier.fromCommandInput(stringReader);
            StatusEffect type = MVRegistry.STATUS_EFFECT.getOrEmpty(identifier)
                    .orElseThrow(() -> INVALID_EFFECT_EXCEPTION.create(identifier));

            if (!stringReader.canRead()) {
                effects.add(MVMisc.newStatusEffectInstance(type, 5 * 20));
                break;
            }

            if (stringReader.read() != ' ') {
                throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.effect_list_arg_type.expected.space")).createWithContext(stringReader);
            }

            StatusEffectInstance effect = MVMisc.newStatusEffectInstance(type, 5 * 20);

            while (stringReader.canRead() && stringReader.peek() == '-') {
                StringBuilder arg = new StringBuilder();

                while (stringReader.canRead() && stringReader.peek() != ':') {
                    arg.append(stringReader.read());
                }

                EffectArgument key = Arrays.stream(EffectArgument.values())
                        .filter(eff -> eff.name.equalsIgnoreCase(arg.toString()))
                        .findFirst()
                        .orElse(null);

                if (key == null) {
                    throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.effect_list_arg_type.invalid.arg")).createWithContext(stringReader);
                }
                if (!stringReader.canRead()) {
                    throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.effect_list_arg_type.expected.colon")).createWithContext(stringReader);
                }

                stringReader.read(); // Colon

                StringBuilder value = new StringBuilder();
                while (stringReader.canRead() && stringReader.peek() != ' ')
                    value.append(stringReader.read());

                try {
                    effect = key.apply.apply(effect, value.toString());
                } catch (Exception e) {
                    throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.effect_list_arg_type.invalid.value")).createWithContext(stringReader);
                }

                if (stringReader.canRead()) {
                    stringReader.read();
                }
            }

            effects.add(effect);
        }
        return effects;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        boolean afterFirstEffect = builder.getRemaining().contains(" ");
        builder = builder.createOffset(builder.getInput().lastIndexOf(" ") + 1);

        if (afterFirstEffect) {
            for (EffectArgument effectArgument : EffectArgument.values()) {
                if ((effectArgument.name + ":").startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(effectArgument.name + ":");
                }
            }

            if (builder.getRemaining().startsWith("-")) {
                int colonPos = builder.getRemaining().indexOf(":");
                if (colonPos != -1) {
                    String argStr = builder.getRemaining().substring(0, colonPos);
                    for (EffectArgument effectArgument : EffectArgument.values()) {
                        if (!effectArgument.name.equals(argStr)) {
                            continue;
                        }

                        if (effectArgument.isBoolean) {
                            String value = builder.getRemaining().substring(colonPos + 1);

                            if (Boolean.TRUE.toString().startsWith(value.toLowerCase())) {
                                builder.suggest(argStr + ":true");
                            }

                            if (Boolean.FALSE.toString().startsWith(value.toLowerCase())) {
                                builder.suggest(argStr + ":false");
                            }
                        }
                        break;
                    }
                }
            }
        }

        return CommandSource.suggestIdentifiers(MVRegistry.STATUS_EFFECT.getIds(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public enum EffectArgument {
        DURATION("-duration", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), Integer.parseInt(str) * 20, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), false),
        AMPLIFIER("-amplifier", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), effect.getDuration(), Integer.parseInt(str), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), false),
        AMBIENT("-ambient", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), effect.getDuration(), effect.getAmplifier(), parseBoolean(str), effect.shouldShowParticles(), effect.shouldShowIcon()), true),
        PERMANENT("-permanent", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), -1, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), true),
        SHOW_PARTICLES("-showparticles", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), parseBoolean(str), effect.shouldShowIcon()), true),
        SHOW_ICON("-showicon", (effect, str) -> MVMisc.newStatusEffectInstance(MVMisc.getEffectType(effect), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), parseBoolean(str)), true);

        private final String name;
        private final BiFunction<StatusEffectInstance, String, StatusEffectInstance> apply;
        private final boolean isBoolean;

        EffectArgument(String name, BiFunction<StatusEffectInstance, String, StatusEffectInstance> apply, boolean isBoolean) {
            this.name = name;
            this.apply = apply;
            this.isBoolean = isBoolean;
        }

        private static boolean parseBoolean(String str) {
            if (str.equalsIgnoreCase("true")) {
                return true;
            }
            if (str.equalsIgnoreCase("false")) {
                return false;
            }

            throw new IllegalArgumentException("Expected true or false");
        }
    }
}
