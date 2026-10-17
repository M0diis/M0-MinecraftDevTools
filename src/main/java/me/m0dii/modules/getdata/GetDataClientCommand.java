package me.m0dii.modules.getdata;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.itemdata.ItemDataClientCommand;
import me.m0dii.utils.NbtExtractors;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.CompletableFuture;

public final class GetDataClientCommand {
    private GetDataClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("getdata")
                        .executes(GetDataClientCommand::openFromCrosshair)
                        .then(ClientCommands.literal("item")
                                .executes(context -> ItemDataClientCommand.openDefault(context.getSource()))
                                .then(ClientCommands.literal("mainhand")
                                        .executes(context -> ItemDataClientCommand.openSlot(
                                                context.getSource(),
                                                context.getSource().getClient().player == null
                                                        ? -1
                                                        : context.getSource().getClient().player.getInventory().getSelectedSlot()
                                        )))
                                .then(ClientCommands.literal("offhand")
                                        .executes(context -> ItemDataClientCommand.openSlot(
                                                context.getSource(),
                                                net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND
                                        ))))
                        .then(ClientCommands.literal("quick").executes(GetDataClientCommand::openQuickFromCrosshair))
                        .then(ClientCommands.argument("target", StringArgumentType.greedyString())
                                .suggests(GetDataClientCommand::suggestTargets)
                                .executes(GetDataClientCommand::openFromArgument))));
    }

    private static CompletableFuture<Suggestions> suggestTargets(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("@s");
        builder.suggest("@p");
        builder.suggest("@r");
        builder.suggest("@e");
        builder.suggest("block ~ ~ ~");

        Minecraft client = context.getSource().getClient();
        if (client != null && client.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            builder.suggest("block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        }
        if (client != null && client.hitResult instanceof EntityHitResult entityHit) {
            builder.suggest(entityHit.getEntity().getStringUUID());
            builder.suggest("@e[type=" + BuiltInRegistries.ENTITY_TYPE.getKey(entityHit.getEntity().getType()) + ",limit=1,sort=nearest]");
        }
        return builder.buildFuture();
    }

    private static int openFromArgument(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        if (client.player == null || client.level == null) {
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
                    int x = parseCoord(split[0], client.player.blockPosition().getX());
                    int y = parseCoord(split[1], client.player.blockPosition().getY());
                    int z = parseCoord(split[2], client.player.blockPosition().getZ());
                    BlockPos pos = new BlockPos(x, y, z);
                    String path = x + " " + y + " " + z;
                    final String display = "Block " + path;
                    final String targetToken = "block " + path;
                    openBlockScreenWithSync(client, pos, display, targetToken);
                    return 1;
                } catch (Exception ignored) {
                    client.player.sendSystemMessage(Component.literal("[GetData] Invalid block target. Use `block x y z` or `block ~ ~ ~`."));
                    return 0;
                }
            }
            client.player.sendSystemMessage(Component.literal("[GetData] Invalid block target. Use `block x y z` or `block ~ ~ ~`."));
            return 0;
        }

        var lookedEntity = client.hitResult instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        var resolved = NbtExtractors.resolveEntityTarget(client.level, client.player, target, lookedEntity);
        if (resolved == null) {
            client.player.sendSystemMessage(Component.literal("[GetData] Could not resolve target: " + target));
            return 0;
        }

        CompoundTag base = NbtExtractors.extractEntityNbt(resolved);
        if (base == null) {
            base = new CompoundTag();
        }

        final String selectorPayload = base.toString();
        final String selectorDisplay = "Entity " + resolved.getName().getString();
        final String selectorToken = "entity " + target;
        openBrowser(client, selectorDisplay, selectorToken, selectorPayload);
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
        Minecraft client = context.getSource().getClient();
        if (client.player == null || client.level == null) {
            return 0;
        }

        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            client.player.sendSystemMessage(Component.literal("[GetData] No target under crosshair."));
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
            CompoundTag nbt = NbtExtractors.extractEntityNbt(entityHit.getEntity());
            if (nbt == null || nbt.isEmpty()) {
                nbt = new CompoundTag();
                nbt.putString("id", String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityHit.getEntity().getType())));
                nbt.putString("uuid", entityHit.getEntity().getStringUUID());
            }
            String selector = entityHit.getEntity().getStringUUID();
            final String payload = nbt.toString();
            final String display = "Entity " + selector;
            final String targetToken = "entity " + selector;
            openBrowser(client, display, targetToken, payload);
            return 1;
        }

        return 0;
    }

    /** {@code /getdata quick}: the quick editor for the block or entity under the crosshair. */
    private static int openQuickFromCrosshair(CommandContext<FabricClientCommandSource> context) {
        Minecraft client = context.getSource().getClient();
        if (client.player == null || client.level == null) {
            return 0;
        }
        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            String path = pos.getX() + " " + pos.getY() + " " + pos.getZ();
            String token = "block " + path;
            CompoundTag localNbt = NbtExtractors.extractBlockData(client.level, pos);
            client.execute(() -> client.gui.setScreen(QuickEditScreen.create(client.gui.screen(), "Block " + path, token,
                    localNbt == null ? "{}" : localNbt.toString())));
            GetDataSyncClient.requestBlockNbt(pos, synced -> {
                if (synced != null && !synced.isEmpty()
                        && client.gui.screen() instanceof GetDataTargetView view && view.matchesTarget(token)) {
                    view.applySyncedPayload(synced.toString());
                }
            });
            return 1;
        }
        if (hit instanceof EntityHitResult entityHit) {
            CompoundTag nbt = NbtExtractors.extractEntityNbt(entityHit.getEntity());
            String uuid = entityHit.getEntity().getStringUUID();
            String payload = nbt == null ? "{}" : nbt.toString();
            client.execute(() -> client.gui.setScreen(QuickEditScreen.create(client.gui.screen(),
                    "Entity " + entityHit.getEntity().getName().getString(), "entity " + uuid, payload)));
            return 1;
        }
        client.player.sendSystemMessage(Component.literal("[GetData] Look at a block or entity first."));
        return 0;
    }

    private static void openBlockScreenWithSync(Minecraft client, BlockPos pos, String display, String targetToken) {
        CompoundTag localNbt = NbtExtractors.extractBlockData(client.level, pos);
        String localPayload = localNbt == null ? "{}" : localNbt.toString();
        openBrowser(client, display, targetToken, localPayload);

        GetDataSyncClient.requestBlockNbt(pos, syncedNbt -> {
            if (syncedNbt == null || syncedNbt.isEmpty()) {
                return;
            }
            String syncedPayload = syncedNbt.toString();
            if (client.gui.screen() instanceof GetDataTargetView existing && existing.matchesTarget(targetToken)) {
                existing.applySyncedPayload(syncedPayload);
            } else {
                openBrowser(client, display, targetToken, syncedPayload);
            }
        });
    }

    private static void openBrowser(Minecraft client, String display, String targetToken, String payload) {
        client.execute(() -> client.gui.setScreen(GetDataBrowserScreen.create(client.gui.screen(), display, targetToken, payload)));
    }
}

