package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class GetPresetCommand extends ClientCommand {

    private static final Map<String, Supplier<ItemStack>> presetItems = new HashMap<>();
    public static final Supplier<ItemStack> COLOR_CODES = registerPresetItem("colorcodes");

    public static void registerPresetItem(String name, Supplier<ItemStack> item) {
        presetItems.put(name, item);
    }

    public static Supplier<ItemStack> registerPresetItem(String name) {
        Supplier<ItemStack> output = () -> Optional.ofNullable(getItem(name)).orElseGet(() -> new ItemStack(Items.BARRIER)
                .manager$setCustomName(TextInst.translatable("nbteditor.get.preset_item.missing")));
        presetItems.put(name, output);
        return output;
    }

    private static ItemStack getItem(String name) {
        try {
            return NBTManagers.ITEM.deserialize(MiscUtil.updateDynamic(TypeReferences.ITEM_STACK, MiscUtil.readNBT(
                    MVMisc.getResource(IdentifierInst.of("m0-dev-tools", "presetitems/" + name + ".nbt")).orElseThrow())), true);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error while loading preset item '" + name + "'", e);
            return null;
        }
    }

    @Override
    public String getName() {
        return "preset";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        presetItems.forEach((name, item) -> {
            builder.then(literal(name).executes(context -> {
                MiscUtil.getWithMessage(item.get().copy());
                return Command.SINGLE_SUCCESS;
            }));
        });
    }

}
