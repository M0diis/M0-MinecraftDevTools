package me.m0dii.modules.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SecondaryChatCommands {
    private SecondaryChatCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("secondarychat")
                        .then(ClientCommandManager.literal("config")
                                .executes(SecondaryChatCommands::openConfig))
                        .then(ClientCommandManager.literal("toggle")
                                .executes(SecondaryChatCommands::toggleEnabled))
                        .then(ClientCommandManager.literal("clear")
                                .executes(SecondaryChatCommands::clearBuffer))
                        .then(ClientCommandManager.literal("render")
                                .then(ClientCommandManager.literal("addon")
                                        .executes(ctx -> setRenderMode(ctx, SecondaryChatSettings.RenderMode.ADDON)))
                                .then(ClientCommandManager.literal("replace")
                                        .executes(ctx -> setRenderMode(ctx, SecondaryChatSettings.RenderMode.REPLACE))))
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
                        .then(ClientCommandManager.literal("window")
                                .then(ClientCommandManager.literal("list")
                                        .executes(SecondaryChatCommands::listWindows))
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                                .then(ClientCommandManager.argument("title", StringArgumentType.greedyString())
                                                        .executes(SecondaryChatCommands::addWindow))))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::removeWindow)))
                                .then(ClientCommandManager.literal("toggle")
                                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::toggleWindow))))
                        .then(ClientCommandManager.literal("tab")
                                .then(ClientCommandManager.literal("list")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::listTabs)))
                                .then(ClientCommandManager.literal("add")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                                .executes(SecondaryChatCommands::addTab)))))
                                .then(ClientCommandManager.literal("remove")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::removeTab))))
                                .then(ClientCommandManager.literal("select")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::selectTab))))
                                .then(ClientCommandManager.literal("catchall")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::toggleCatchAll))))
                                .then(ClientCommandManager.literal("addregex")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
                                                                .executes(SecondaryChatCommands::addTabRegex)))))
                                .then(ClientCommandManager.literal("clearregex")
                                        .then(ClientCommandManager.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommandManager.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::clearTabRegex)))))
                        .then(ClientCommandManager.literal("status")
                                .executes(SecondaryChatCommands::showStatus))
        );
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new SecondaryChatConfigScreen(client.currentScreen));
        return 1;
    }

    private static int toggleEnabled(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatModule.INSTANCE.setEnabled(!SecondaryChatModule.INSTANCE.isEnabled());
        boolean enabled = SecondaryChatSettings.get().enabled;
        ctx.getSource().sendFeedback(Text.literal("Secondary Chat: ")
                .append(Text.literal(enabled ? "ON" : "OFF")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)));
        return 1;
    }

    private static int clearBuffer(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatManager.clear();
        ctx.getSource().sendFeedback(Text.literal("Secondary chat buffers cleared").formatted(Formatting.GREEN));
        return 1;
    }

    private static int setRenderMode(CommandContext<FabricClientCommandSource> ctx, SecondaryChatSettings.RenderMode mode) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().renderMode = mode);
        ctx.getSource().sendFeedback(Text.literal("Secondary chat render mode: ")
                .append(Text.literal(mode.name()).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int addRegex(CommandContext<FabricClientCommandSource> ctx) {
        String pattern = StringArgumentType.getString(ctx, "pattern");
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(pattern));
        ctx.getSource().sendFeedback(Text.literal("Added global regex: ")
                .append(Text.literal(pattern).formatted(Formatting.YELLOW)));

        return 1;
    }

    private static int listRegex(CommandContext<FabricClientCommandSource> ctx) {
        List<String> list = SecondaryChatSettings.get().regexList;
        if (list == null || list.isEmpty()) {
            ctx.getSource().sendFeedback(Text.literal("No global regex patterns configured").formatted(Formatting.GRAY));
            return 1;
        }

        ctx.getSource().sendFeedback(Text.literal("=== Global Regex Patterns ===").formatted(Formatting.GOLD));
        int i = 1;
        for (String p : list) {
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
        ctx.getSource().sendFeedback(Text.literal("Cleared all global regex patterns").formatted(Formatting.GREEN));
        return 1;
    }

    private static int setMode(CommandContext<FabricClientCommandSource> ctx, SecondaryChatSettings.InterceptMode mode) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().interceptMode = mode);
        ctx.getSource().sendFeedback(Text.literal("Secondary chat intercept mode: ")
                .append(Text.literal(mode.name()).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int listWindows(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("=== Secondary Chat Windows ===").formatted(Formatting.GOLD));
        for (SecondaryChatSettings.WindowConfig window : SecondaryChatSettings.get().windows) {
            ctx.getSource().sendFeedback(Text.literal(window.id + " ")
                    .formatted(window.visible ? Formatting.GREEN : Formatting.DARK_GRAY)
                    .append(Text.literal("\"" + window.title + "\" tabs=" + window.tabs.size()
                            + " selected=" + window.selectedTabId)));
        }
        return 1;
    }

    private static int addWindow(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String title = StringArgumentType.getString(ctx, "title");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
            if (findWindow(settings.windows, id) != null) {
                return;
            }
            SecondaryChatSettings.WindowConfig window = new SecondaryChatSettings.WindowConfig();
            window.id = id;
            window.title = title;
            window.useHudCanvas = false;
            window.x = 24 + settings.windows.size() * 18;
            window.y = 60 + settings.windows.size() * 18;
            SecondaryChatSettings.TabConfig all = new SecondaryChatSettings.TabConfig();
            all.id = "all";
            all.name = "All";
            all.catchAll = true;
            window.tabs.add(all);
            window.selectedTabId = all.id;
            settings.windows.add(window);
        });
        ctx.getSource().sendFeedback(Text.literal("Added window: ")
                .append(Text.literal(id).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int removeWindow(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if ("main".equals(id)) {
            ctx.getSource().sendError(Text.literal("The main Secondary Chat window cannot be removed."));
            return 0;
        }
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().windows.removeIf(window -> window.id.equals(id)));
        ctx.getSource().sendFeedback(Text.literal("Removed window: ")
                .append(Text.literal(id).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int toggleWindow(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, id);
            if (window != null) {
                window.visible = !window.visible;
            }
        });
        SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, id);
        ctx.getSource().sendFeedback(Text.literal("Window " + id + ": ")
                .append(Text.literal(window != null && window.visible ? "VISIBLE" : "HIDDEN")
                        .formatted(window != null && window.visible ? Formatting.GREEN : Formatting.RED)));
        return 1;
    }

    private static int listTabs(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
        if (window == null) {
            ctx.getSource().sendError(Text.literal("Unknown window: " + windowId));
            return 0;
        }
        ctx.getSource().sendFeedback(Text.literal("=== Tabs for " + window.id + " ===").formatted(Formatting.GOLD));
        for (SecondaryChatSettings.TabConfig tab : window.tabs) {
            ctx.getSource().sendFeedback(Text.literal(tab.id + " ")
                    .formatted(tab.id.equals(window.selectedTabId) ? Formatting.GREEN : Formatting.GRAY)
                    .append(Text.literal("\"" + tab.name + "\" regex=" + tab.regexList.size()
                            + " priority=" + tab.priority
                            + " catchAll=" + tab.catchAll)));
        }
        return 1;
    }

    private static int addTab(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "id");
        String name = StringArgumentType.getString(ctx, "name");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
            if (window == null || findTab(window.tabs, tabId) != null) {
                return;
            }
            SecondaryChatSettings.TabConfig tab = new SecondaryChatSettings.TabConfig();
            tab.id = tabId;
            tab.name = name;
            tab.catchAll = false;
            window.tabs.add(tab);
        });
        ctx.getSource().sendFeedback(Text.literal("Added tab: ")
                .append(Text.literal(tabId).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int removeTab(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
            if (window == null || window.tabs.size() <= 1) {
                return;
            }
            window.tabs.removeIf(tab -> tab.id.equals(tabId));
            if (findTab(window.tabs, window.selectedTabId) == null && !window.tabs.isEmpty()) {
                window.selectedTabId = window.tabs.getFirst().id;
            }
        });
        SecondaryChatManager.clear(windowId, tabId);
        ctx.getSource().sendFeedback(Text.literal("Removed tab: ")
                .append(Text.literal(tabId).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int selectTab(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        SecondaryChatManager.selectTab(windowId, tabId);
        ctx.getSource().sendFeedback(Text.literal("Selected tab: ")
                .append(Text.literal(windowId + "/" + tabId).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int toggleCatchAll(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
            SecondaryChatSettings.TabConfig tab = window == null ? null : findTab(window.tabs, tabId);
            if (tab != null) {
                tab.catchAll = !tab.catchAll;
            }
        });
        SecondaryChatSettings.TabConfig tab = tab(windowId, tabId);
        ctx.getSource().sendFeedback(Text.literal("Tab catch-all: ")
                .append(Text.literal(tab != null && tab.catchAll ? "ON" : "OFF")
                        .formatted(tab != null && tab.catchAll ? Formatting.GREEN : Formatting.RED)));
        return 1;
    }

    private static int addTabRegex(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        String pattern = StringArgumentType.getString(ctx, "pattern");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.TabConfig tab = tab(windowId, tabId);
            if (tab != null) {
                tab.regexList.add(pattern);
            }
        });
        ctx.getSource().sendFeedback(Text.literal("Added tab regex: ")
                .append(Text.literal(pattern).formatted(Formatting.YELLOW)));
        return 1;
    }

    private static int clearTabRegex(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.TabConfig tab = tab(windowId, tabId);
            if (tab != null) {
                tab.regexList = new ArrayList<>();
            }
        });
        ctx.getSource().sendFeedback(Text.literal("Cleared regexes for tab: ")
                .append(Text.literal(windowId + "/" + tabId).formatted(Formatting.YELLOW)));
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
        ctx.getSource().sendFeedback(Text.literal("Render: ")
                .append(Text.literal(settings.renderMode.name()).formatted(Formatting.YELLOW)));
        ctx.getSource().sendFeedback(Text.literal("Intercept: ")
                .append(Text.literal(settings.interceptMode.name()).formatted(Formatting.YELLOW)));
        ctx.getSource().sendFeedback(Text.literal("Windows: ")
                .append(Text.literal(String.valueOf(settings.windows.size())).formatted(Formatting.AQUA)));
        ctx.getSource().sendFeedback(Text.literal("Main position: ")
                .append(Text.literal(canvas.x + ", " + canvas.y).formatted(Formatting.GRAY)));
        ctx.getSource().sendFeedback(Text.literal("Main size: ")
                .append(Text.literal(canvas.width + "x" + canvas.height).formatted(Formatting.GRAY)));
        return 1;
    }

    private static SecondaryChatSettings.WindowConfig findWindow(List<SecondaryChatSettings.WindowConfig> windows, String id) {
        for (SecondaryChatSettings.WindowConfig window : windows) {
            if (window.id.equals(id)) {
                return window;
            }
        }
        return null;
    }

    private static SecondaryChatSettings.TabConfig findTab(List<SecondaryChatSettings.TabConfig> tabs, String id) {
        for (SecondaryChatSettings.TabConfig tab : tabs) {
            if (tab.id.equals(id)) {
                return tab;
            }
        }
        return null;
    }

    private static SecondaryChatSettings.TabConfig tab(String windowId, String tabId) {
        SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
        return window == null ? null : findTab(window.tabs, tabId);
    }

    private static CompletableFuture<Suggestions> suggestWindows(CommandContext<FabricClientCommandSource> ctx,
                                                                  SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(windowIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestTabs(CommandContext<FabricClientCommandSource> ctx,
                                                              SuggestionsBuilder builder) {
        String windowId;
        try {
            windowId = StringArgumentType.getString(ctx, "window");
        } catch (IllegalArgumentException ignored) {
            windowId = "";
        }
        return CommandSource.suggestMatching(tabIds(windowId), builder);
    }

    private static List<String> windowIds() {
        List<String> ids = new ArrayList<>();
        for (SecondaryChatSettings.WindowConfig window : SecondaryChatSettings.get().windows) {
            ids.add(window.id);
        }
        return ids;
    }

    private static List<String> tabIds(String windowId) {
        SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
        if (window == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (SecondaryChatSettings.TabConfig tab : window.tabs) {
            ids.add(tab.id);
        }
        return ids;
    }
}
