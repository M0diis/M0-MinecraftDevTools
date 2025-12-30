package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.commands.arguments.SummonableEntityArgumentType;
import me.m0dii.nbteditor.integrations.NBTAutocompleteIntegration;
import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetEntityCommand extends ClientCommand {

    @Override
    public String getName() {
        return "entity";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        Command<FabricClientCommandSource> getEntity = context -> {
            EntityType<?> entityType = context.getArgument("entity", EntityType.class);

            PosArgument posArg = getDefaultArg(context, "pos", null, PosArgument.class);
            Vec3d pos = (posArg == null ? null : posArg.getPos(MVMisc.getCommandSource(context.getSource().getPlayer())));

            NbtCompound nbtArg = getDefaultArg(context, "nbt", new NbtCompound(), NbtCompound.class);

            LocalEntity entity = new LocalEntity(entityType, nbtArg);

            if (pos == null) {
                entity.toItem().ifPresentOrElse(MiscUtil::getWithMessage,
                        () -> MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.export.item.error"), false));
            } else if (M0DevToolsClient.SERVER_CONN.isEditingExpanded()) {
                entity.summon(MiscUtil.client.world.getRegistryKey(), pos);
            } else {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.requires_server"), false);
            }

            return Command.SINGLE_SUCCESS;
        };
        SuggestionProvider<FabricClientCommandSource> nbtSuggestions = (context, suggestionsBuilder) -> {
            if (NBTAutocompleteIntegration.INSTANCE.isEmpty()) {
                return Suggestions.empty();
            }
            EntityType<?> entityType = context.getArgument("entity", EntityType.class);
            String name = "entity/" + EntityType.getId(entityType);
            String tag = suggestionsBuilder.getRemaining();
            return NbtSuggestionManager.loadFromName(name, tag, suggestionsBuilder, false);
        };

        builder.then(argument("entity", SummonableEntityArgumentType.summonableEntity())
                .then(argument("pos", Vec3ArgumentType.vec3())
                        .then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                .suggests(nbtSuggestions)
                                .executes(getEntity))
                        .executes(getEntity))
                .then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
                        .suggests(nbtSuggestions)
                        .executes(getEntity))
                .executes(getEntity));
    }

}
