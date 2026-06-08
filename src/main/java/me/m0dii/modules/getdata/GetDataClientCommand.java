package me.m0dii.modules.getdata;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.itemdata.ItemDataClientCommand;
import me.m0dii.utils.NbtExtractors;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

public final class GetDataClientCommand {
    private GetDataClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("getdata")
                        .executes(GetDataClientCommand::openFromCrosshair)
                        .then(ClientCommandManager.literal("item")
                                .executes(context -> ItemDataClientCommand.openDefault(context.getSource()))
                                .then(ClientCommandManager.literal("mainhand")
                                        .executes(context -> ItemDataClientCommand.openSlot(
                                                context.getSource(),
                                                context.getSource().getClient().player == null
                                                        ? -1
                                                        : context.getSource().getClient().player.getInventory().getSelectedSlot()
                                        )))
                                .then(ClientCommandManager.literal("offhand")
                                        .executes(context -> ItemDataClientCommand.openSlot(
                                                context.getSource(),
                                                net.minecraft.entity.player.PlayerInventory.OFF_HAND_SLOT
                                        ))))
                        .then(ClientCommandManager.argument("target", StringArgumentType.greedyString())
                                .suggests(GetDataClientCommand::suggestTargets)
                                .executes(GetDataClientCommand::openFromArgument))));
    }

    private static CompletableFuture<Suggestions> suggestTargets(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("@s");
        builder.suggest("@p");
        builder.suggest("@r");
        builder.suggest("@e");
        builder.suggest("block ~ ~ ~");

        MinecraftClient client = context.getSource().getClient();
        if (client != null && client.crosshairTarget instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            builder.suggest("block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
        if (client != null && client.crosshairTarget instanceof EntityHitResult entityHit) {
            builder.suggest(entityHit.getEntity().getUuidAsString());
            builder.suggest("@e[type=" + Registries.ENTITY_TYPE.getId(entityHit.getEntity().getType()) + ",limit=1,sort=nearest]");
        }
        return builder.buildFuture();
    }

    private static int openFromArgument(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null || client.world == null) {
            return 0;
        }

        String target = StringArgumentType.getString(context, "target").trim();
        if (target.isEmpty()) {
            return openFromCrosshair(context);
        }

        if (target.startsWith("block ")) {
            String[] split = target.substring("block ".length()).trim().split("\\s+");
            if (split.length >= 3) {
                try {
                    int x = parseCoord(split[0], client.player.getBlockPos().getX());
                    int y = parseCoord(split[1], client.player.getBlockPos().getY());
                    int z = parseCoord(split[2], client.player.getBlockPos().getZ());
                    BlockPos pos = new BlockPos(x, y, z);
                    String path = x + " " + y + " " + z;
                    final String display = "Block " + path;
                    final String targetToken = "block " + path;
                    openBlockScreenWithSync(client, pos, display, targetToken);
                    return 1;
                } catch (Exception ignored) {
                    // fall through to entity-style target handling
                }
            }
        }

        var lookedEntity = client.crosshairTarget instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        var resolved = NbtExtractors.resolveEntityTarget(client.world, client.player, target, lookedEntity);
        NbtCompound base = resolved == null ? new NbtCompound() : NbtExtractors.extractEntityNbt(resolved);
        if (base == null) {
            base = new NbtCompound();
        }
        if (base.isEmpty()) {
            base.putString("target", target);
        }

        final String selectorPayload = base.toString();
        final String selectorDisplay = resolved == null ? "Entity/Selector " + target : "Entity " + resolved.getName().getString();
        final String selectorToken = "entity " + target;
        client.execute(() -> client.setScreen(GetDataScreen.create(client.currentScreen, selectorDisplay, selectorToken, selectorPayload)));
        return 1;
    }

    private static int parseCoord(String token, int base) {
        if (token.startsWith("~")) {
            if (token.length() == 1) {
                return base;
            }
            return base + Integer.parseInt(token.substring(1));
        }
        return Integer.parseInt(token);
    }

    private static int openFromCrosshair(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player == null || client.world == null) {
            return 0;
        }

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            client.player.sendMessage(Text.literal("[GetData] No target under crosshair."), false);
            return 0;
        }

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            String path = pos.getX() + " " + pos.getY() + " " + pos.getZ();
            final String display = "Block " + path;
            final String targetToken = "block " + path;
            openBlockScreenWithSync(client, pos, display, targetToken);
            return 1;
        }

        if (hit instanceof EntityHitResult entityHit) {
            NbtCompound nbt = NbtExtractors.extractEntityNbt(entityHit.getEntity());
            if (nbt == null || nbt.isEmpty()) {
                nbt = new NbtCompound();
                nbt.putString("id", String.valueOf(Registries.ENTITY_TYPE.getId(entityHit.getEntity().getType())));
                nbt.putString("uuid", entityHit.getEntity().getUuidAsString());
            }
            String selector = entityHit.getEntity().getUuidAsString();
            final String payload = nbt.toString();
            final String display = "Entity " + selector;
            final String targetToken = "entity " + selector;
            client.execute(() -> client.setScreen(GetDataScreen.create(client.currentScreen, display, targetToken, payload)));
            return 1;
        }

        return 0;
    }

    private static void openBlockScreenWithSync(MinecraftClient client, BlockPos pos, String display, String targetToken) {
        NbtCompound localNbt = NbtExtractors.extractBlockData(client.world, pos);
        String localPayload = localNbt == null ? "{}" : localNbt.toString();
        client.execute(() -> client.setScreen(GetDataScreen.create(client.currentScreen, display, targetToken, localPayload)));

        GetDataSyncClient.requestBlockNbt(pos, syncedNbt -> {
            if (syncedNbt == null || syncedNbt.isEmpty()) {
                return;
            }
            String syncedPayload = syncedNbt.toString();
            if (client.currentScreen instanceof GetDataScreen existing && existing.matchesTarget(targetToken)) {
                existing.applySyncedPayload(syncedPayload);
            } else {
                client.execute(() -> client.setScreen(GetDataScreen.create(client.currentScreen, display, targetToken, syncedPayload)));
            }
        });
    }
}

