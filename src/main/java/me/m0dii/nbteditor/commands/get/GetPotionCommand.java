package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.commands.arguments.EffectListArgumentType;
import me.m0dii.nbteditor.commands.arguments.EnumArgumentType;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.CustomPotionContents;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;

import java.util.*;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetPotionCommand extends ClientCommand {

    @Override
    public String getName() {
        return "potion";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(argument("type", EnumArgumentType.options(PotionTypeArgument.class))
                .then(argument("effects", EffectListArgumentType.effectList())
                        .executes(context -> {
                            ItemStack item = new ItemStack(context.getArgument("type", PotionTypeArgument.class).item, 1);
                            List<StatusEffectInstance> effects = new ArrayList<>(context.getArgument("effects", Collection.class));
                            Optional<Integer> color = Optional.empty();

                            if (!effects.isEmpty()) {
                                StatusEffectInstance effect = effects.getFirst();
                                Potion potion = MVRegistry.POTION.getEntrySet().stream().map(Map.Entry::getValue)
                                        .filter(pot -> !pot.getEffects().isEmpty() &&
                                                MVMisc.getEffectType(pot.getEffects().getFirst()) == MVMisc.getEffectType(effect))
                                        .findFirst()
                                        .orElse(null);

                                if (potion != null) {
                                    color = Optional.of(MVMisc.getEffectType(potion.getEffects().getFirst()).getColor());
                                }
                            }

                            ItemTagReferences.CUSTOM_POTION_CONTENTS.set(item, new CustomPotionContents(color, effects));
                            MiscUtil.getWithMessage(item);

                            return Command.SINGLE_SUCCESS;
                        })));
    }

    public enum PotionTypeArgument {
        NORMAL(Items.POTION),
        SPLASH(Items.SPLASH_POTION),
        LINGERING(Items.LINGERING_POTION);

        private final Item item;

        PotionTypeArgument(Item item) {
            this.item = item;
        }
    }

}
