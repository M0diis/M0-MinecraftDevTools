package me.m0dii.modules.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class SecondaryChatCommands {
    private SecondaryChatCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("secondarychat")
                        .then(ClientCommandManager.literal("toggle")
                                .executes(SecondaryChatCommands::toggleEnabled))
                        .then(ClientCommandManager.literal("clear")
                                .executes(SecondaryChatCommands::clearBuffer))
                        .then(ClientCommandManager.literal("addregex")
                                .then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
                                        .executes(SecondaryChatCommands::addRegex)))
                        .then(ClientCommandManager.literal("listregex")
                                .executes(SecondaryChatCommands::listRegex))
                        .then(ClientCommandManager.literal("clearregex")
                                .executes(SecondaryChatCommands::clearRegex))
                        .then(ClientCommandManager.literal("mode")
                                .then(ClientCommandManager.literal("copy")
                                        .executes(ctx -> setMode(ctx, SecondaryChatSettings.InterceptMode.COPY)))
                                .then(ClientCommandManager.literal("move")
                                        .executes(ctx -> setMode(ctx, SecondaryChatSettings.InterceptMode.MOVE))))
                        .then(ClientCommandManager.literal("status")
                                .executes(SecondaryChatCommands::showStatus))
        );
    }

    private static int toggleEnabled(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().enabled = !SecondaryChatSettings.get().enabled);
        boolean enabled = SecondaryChatSettings.get().enabled;
        ctx.getSource().sendFeedback(Text.literal("Secondary Chat: ")
                .append(Text.literal(enabled ? "ON" : "OFF")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)));
        return 1;
    }

    private static int clearBuffer(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatManager.clear();
        ctx.getSource().sendFeedback(Text.literal("Secondary chat buffer cleared").formatted(Formatting.GREEN));
        return 1;
    }

    private static int addRegex(CommandContext<FabricClientCommandSource> ctx) {
        String pattern = StringArgumentType.getString(ctx, "pattern");
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(pattern));
        ctx.getSource().sendFeedback(Text.literal("Added regex: ")
                .append(Text.literal(pattern).formatted(Formatting.YELLOW)));

        return 1;
    }

    private static int listRegex(CommandContext<FabricClientCommandSource> ctx) {
        List<String> list = SecondaryChatSettings.get().regexList;
        if (list == null || list.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("No regex patterns configured").formatted(Formatting.GRAY));
            return 1;
        }

        ctx.getSource().sendFeedback(Text.literal("=== Regex Patterns ===").formatted(Formatting.GOLD));
        List<String> patterns = SecondaryChatSettings.get().regexList;
        int i = 1;
        for (String p : patterns) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                ctx.getSource().sendFeedback(Text.literal(i + ". ")
                        .append(Text.literal(trimmed).formatted(Formatting.YELLOW)));
                i++;
            }
        }
        return 1;
    }

    private static int clearRegex(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.clear());
        ctx.getSource().sendFeedback(Text.literal("Cleared all regex patterns").formatted(Formatting.GREEN));
        return 1;
    }

    private static int setMode(CommandContext<FabricClientCommandSource> ctx, SecondaryChatSettings.InterceptMode mode) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().interceptMode = mode);
        ctx.getSource().sendFeedback(Text.literal("Secondary chat mode: ")
                .append(Text.literal(mode.name()).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                SecondaryChatOverlay::defaultCanvasElement
        );
        ctx.getSource().sendFeedback(Text.literal("=== Secondary Chat Status ===").formatted(Formatting.GOLD));
        ctx.getSource().sendFeedback(Text.literal("Enabled: ")
                .append(Text.literal(String.valueOf(settings.enabled))
                        .formatted(settings.enabled ? Formatting.GREEN : Formatting.RED)));

        List<String> list = settings.regexList;
        if (list != null && !list.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("Regex List: ")
                    .append(Text.literal("[").formatted(Formatting.YELLOW)));
            int i = 0;
            for (String p : list) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    ctx.getSource().sendFeedback(Text.literal("  " + trimmed + (i < list.size() - 1 ? "," : ""))
                            .formatted(Formatting.YELLOW));
                    i++;
                }
            }
        }

        ctx.getSource().sendFeedback(Text.literal("Mode: ")
                .append(Text.literal(settings.interceptMode.name()).formatted(Formatting.YELLOW)));
        ctx.getSource().sendFeedback(Text.literal("Buffer size: ")
                .append(Text.literal(String.valueOf(SecondaryChatManager.snapshot().size())).formatted(Formatting.AQUA)));
        ctx.getSource().sendFeedback(Text.literal("Position: ")
                .append(Text.literal(canvas.x + ", " + canvas.y).formatted(Formatting.GRAY)));
        ctx.getSource().sendFeedback(Text.literal("Size: ")
                .append(Text.literal(canvas.width + "x" + canvas.height).formatted(Formatting.GRAY)));

        return 1;
    }
}
