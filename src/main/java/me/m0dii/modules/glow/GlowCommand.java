package me.m0dii.modules.glow;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Map;

/**
 * {@code /glow <type|group> [color]}: client-side glowing outlines for entity types (zombie, item, ...) or
 * groups (hostile, passive, items, players, all). Only you see them; nothing is sent to the server.
 */
public final class GlowCommand {
    public static final GlowTargets TARGETS = new GlowTargets();

    private GlowCommand() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TARGETS.clear());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("glow")
                        .executes(GlowCommand::list)
                        .then(ClientCommands.literal("clear").executes(ctx -> {
                            TARGETS.clear();
                            ctx.getSource().sendFeedback(Component.literal("[Glow] Cleared."));
                            return 1;
                        }))
                        .then(ClientCommands.argument("target", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
                                    for (String group : GlowTargets.GROUPS) {
                                        if (group.startsWith(typed)) {
                                            builder.suggest(group);
                                        }
                                    }
                                    for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                                        String path = id.getPath();
                                        if (path.startsWith(typed) || id.toString().startsWith(typed)) {
                                            builder.suggest(path);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> toggle(ctx, null))
                                .then(ClientCommands.argument("color", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            GlowTargets.colors().keySet().forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String raw = StringArgumentType.getString(ctx, "color");
                                            Integer color = GlowTargets.parseColor(raw);
                                            if (color == null) {
                                                ctx.getSource().sendError(Component.literal("[Glow] Unknown colour '" + raw
                                                        + "'. Use a name (" + String.join(", ", GlowTargets.colors().keySet()) + ") or #rrggbb."));
                                                return 0;
                                            }
                                            return toggle(ctx, color);
                                        })))));
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx, Integer color) {
        String target = StringArgumentType.getString(ctx, "target");
        String key = GlowTargets.normalize(target);
        boolean group = java.util.Arrays.asList(GlowTargets.GROUPS).contains(key);
        Identifier id = group ? null : Identifier.tryParse(key);
        if (!group && (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id))) {
            ctx.getSource().sendError(Component.literal("[Glow] Unknown entity type '" + target + "'."));
            return 0;
        }
        boolean on = TARGETS.toggle(key, color);
        ctx.getSource().sendFeedback(Component.literal("[Glow] " + key + (on ? " now glows (only you see it)." : " no longer glows.")));
        return 1;
    }

    private static int list(CommandContext<FabricClientCommandSource> ctx) {
        Map<String, Integer> snapshot = TARGETS.snapshot();
        if (snapshot.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("[Glow] Nothing glows. /glow <type|hostile|passive|items|players|all> [color]"));
            return 1;
        }
        snapshot.forEach((key, color) -> ctx.getSource().sendFeedback(
                Component.literal("[Glow] " + key + " · #" + String.format(Locale.ROOT, "%06x", color))
                        .setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(color)))));
        return 1;
    }

    /** Outline colour for an entity (RGB), or null; called from render mixins, so it must stay cheap. */
    public static Integer colorFor(Entity entity) {
        if (TARGETS.isEmpty() || entity == null || entity == Minecraft.getInstance().player) {
            return null;
        }
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String group = entity instanceof Enemy ? "hostile"
                : entity instanceof Animal ? "passive"
                : entity instanceof ItemEntity ? "items"
                : entity instanceof Player ? "players" : "";
        return TARGETS.colorFor(typeId, group);
    }
}
