package me.m0dii.modules.nbtget;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.screens.util.TextAreaScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.NbtFormatter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class NBTGetCommand {
    private NBTGetCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("nbtget")
                        .executes(NBTGetCommand::run)
        );
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource src = ctx.getSource();
        MinecraftClient client = src.getClient();
        if (client.world == null || client.player == null) {
            src.sendError(Text.literal("No world / player loaded").formatted(Formatting.RED));
            return 0;
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            src.sendFeedback(Text.literal("Aim at an entity or block to read NBT").formatted(Formatting.GRAY));
            return 0;
        }

        try {
            NbtCompound compound = null;

            if (hit instanceof EntityHitResult ehr) {
                var ent = ehr.getEntity();
                compound = NBTManagers.ENTITY.getNbt(ent);
            } else if (hit instanceof BlockHitResult bhr) {
                var pos = bhr.getBlockPos();
                var be = client.world.getBlockEntity(pos);
                if (be != null) {
                    compound = NBTManagers.BLOCK_ENTITY.getNbt(be);
                } else {
                    src.sendFeedback(Text.literal("No block entity at target").formatted(Formatting.GRAY));
                    return 0;
                }
            }

            if (compound == null) {
                src.sendError(Text.literal("Could not read NBT from target").formatted(Formatting.RED));
                return 0;
            }

            NBTInfoScreen screen = new NBTInfoScreen(client.currentScreen, compound.toString(), NbtFormatter.FORMATTER);

            client.execute(() -> MiscUtil.client.setScreen(screen));

            src.sendFeedback(Text.literal("Opened NBT JSON viewer").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            src.sendError(Text.literal("Failed to read NBT: " + e.getMessage()).formatted(Formatting.RED));
            return 0;
        }
    }
}
