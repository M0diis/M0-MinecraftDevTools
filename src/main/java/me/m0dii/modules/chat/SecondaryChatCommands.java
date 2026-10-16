package me.m0dii.modules.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
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
                ClientCommands.literal("secondarychat")
                        .then(ClientCommands.literal("config")
                                .executes(SecondaryChatCommands::openConfig))
                        .then(ClientCommands.literal("toggle")
                                .executes(SecondaryChatCommands::toggleEnabled))
                        .then(ClientCommands.literal("clear")
                                .executes(SecondaryChatCommands::clearBuffer))
                        .then(ClientCommands.literal("render")
                                .then(ClientCommands.literal("addon")
                                        .executes(ctx -> setRenderMode(ctx, SecondaryChatSettings.RenderMode.ADDON)))
                                .then(ClientCommands.literal("replace")
                                        .executes(ctx -> setRenderMode(ctx, SecondaryChatSettings.RenderMode.REPLACE))))
                        .then(ClientCommands.literal("addregex")
                                .then(ClientCommands.argument("pattern", StringArgumentType.greedyString())
                                        .executes(SecondaryChatCommands::addRegex)))
                        .then(ClientCommands.literal("listregex")
                                .executes(SecondaryChatCommands::listRegex))
                        .then(ClientCommands.literal("clearregex")
                                .executes(SecondaryChatCommands::clearRegex))
                        .then(ClientCommands.literal("mode")
                                .then(ClientCommands.literal("copy")
                                        .executes(ctx -> setMode(ctx, SecondaryChatSettings.InterceptMode.COPY)))
                                .then(ClientCommands.literal("move")
                                        .executes(ctx -> setMode(ctx, SecondaryChatSettings.InterceptMode.MOVE))))
                        .then(ClientCommands.literal("window")
                                .then(ClientCommands.literal("list")
                                        .executes(SecondaryChatCommands::listWindows))
                                .then(ClientCommands.literal("add")
                                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                                .then(ClientCommands.argument("title", StringArgumentType.greedyString())
                                                        .executes(SecondaryChatCommands::addWindow))))
                                .then(ClientCommands.literal("remove")
                                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::removeWindow)))
                                .then(ClientCommands.literal("toggle")
                                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::toggleWindow))))
                        .then(ClientCommands.literal("tab")
                                .then(ClientCommands.literal("list")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .executes(SecondaryChatCommands::listTabs)))
                                .then(ClientCommands.literal("add")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("id", StringArgumentType.word())
                                                        .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                                                .executes(SecondaryChatCommands::addTab)))))
                                .then(ClientCommands.literal("remove")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::removeTab))))
                                .then(ClientCommands.literal("select")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::selectTab))))
                                .then(ClientCommands.literal("catchall")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::toggleCatchAll))))
                                .then(ClientCommands.literal("addregex")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .then(ClientCommands.argument("pattern", StringArgumentType.greedyString())
                                                                .executes(SecondaryChatCommands::addTabRegex)))))
                                .then(ClientCommands.literal("clearregex")
                                        .then(ClientCommands.argument("window", StringArgumentType.word())
                                                .suggests(SecondaryChatCommands::suggestWindows)
                                                .then(ClientCommands.argument("tab", StringArgumentType.word())
                                                        .suggests(SecondaryChatCommands::suggestTabs)
                                                        .executes(SecondaryChatCommands::clearTabRegex)))))
                        .then(ClientCommands.literal("status")
                                .executes(SecondaryChatCommands::showStatus))
        );
    }

    private static int openConfig(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        client.gui.setScreen(new SecondaryChatConfigScreen(client.gui.screen()));
        return 1;
    }

    private static int toggleEnabled(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatModule.INSTANCE.setEnabled(!SecondaryChatModule.INSTANCE.isEnabled());
        boolean enabled = SecondaryChatSettings.get().enabled;
        ctx.getSource().sendFeedback(Component.literal("Secondary Chat: ")
                .append(Component.literal(enabled ? "ON" : "OFF")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return 1;
    }

    private static int clearBuffer(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatManager.clear();
        ctx.getSource().sendFeedback(Component.literal("Secondary chat buffers cleared").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int setRenderMode(CommandContext<FabricClientCommandSource> ctx, SecondaryChatSettings.RenderMode mode) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().renderMode = mode);
        ctx.getSource().sendFeedback(Component.literal("Secondary chat render mode: ")
                .append(Component.literal(mode.name()).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int addRegex(CommandContext<FabricClientCommandSource> ctx) {
        String pattern = StringArgumentType.getString(ctx, "pattern");
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.add(pattern));
        ctx.getSource().sendFeedback(Component.literal("Added global regex: ")
                .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)));

        return 1;
    }

    private static int listRegex(CommandContext<FabricClientCommandSource> ctx) {
        List<String> list = SecondaryChatSettings.get().regexList;
        if (list == null || list.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("No global regex patterns configured").withStyle(ChatFormatting.GRAY));
            return 1;
        }

        ctx.getSource().sendFeedback(Component.literal("=== Global Regex Patterns ===").withStyle(ChatFormatting.GOLD));
        int i = 1;
        for (String p : list) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                ctx.getSource().sendFeedback(Component.literal(i + ". ")
                        .append(Component.literal(trimmed).withStyle(ChatFormatting.YELLOW)));
                i++;
            }
        }
        return 1;
    }

    private static int clearRegex(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().regexList.clear());
        ctx.getSource().sendFeedback(Component.literal("Cleared all global regex patterns").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int setMode(CommandContext<FabricClientCommandSource> ctx, SecondaryChatSettings.InterceptMode mode) {
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().interceptMode = mode);
        ctx.getSource().sendFeedback(Component.literal("Secondary chat intercept mode: ")
                .append(Component.literal(mode.name()).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int listWindows(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal("=== Secondary Chat Windows ===").withStyle(ChatFormatting.GOLD));
        for (SecondaryChatSettings.WindowConfig window : SecondaryChatSettings.get().windows) {
            ctx.getSource().sendFeedback(Component.literal(window.id + " ")
                    .withStyle(window.visible ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .append(Component.literal("\"" + window.title + "\" tabs=" + window.tabs.size()
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
        ctx.getSource().sendFeedback(Component.literal("Added window: ")
                .append(Component.literal(id).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int removeWindow(CommandContext<FabricClientCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if ("main".equals(id)) {
            ctx.getSource().sendError(Component.literal("The main Secondary Chat window cannot be removed."));
            return 0;
        }
        SecondaryChatSettings.updateAndSave(() -> SecondaryChatSettings.get().windows.removeIf(window -> window.id.equals(id)));
        ctx.getSource().sendFeedback(Component.literal("Removed window: ")
                .append(Component.literal(id).withStyle(ChatFormatting.YELLOW)));
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
        ctx.getSource().sendFeedback(Component.literal("Window " + id + ": ")
                .append(Component.literal(window != null && window.visible ? "VISIBLE" : "HIDDEN")
                        .withStyle(window != null && window.visible ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return 1;
    }

    private static int listTabs(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        SecondaryChatSettings.WindowConfig window = findWindow(SecondaryChatSettings.get().windows, windowId);
        if (window == null) {
            ctx.getSource().sendError(Component.literal("Unknown window: " + windowId));
            return 0;
        }
        ctx.getSource().sendFeedback(Component.literal("=== Tabs for " + window.id + " ===").withStyle(ChatFormatting.GOLD));
        for (SecondaryChatSettings.TabConfig tab : window.tabs) {
            ctx.getSource().sendFeedback(Component.literal(tab.id + " ")
                    .withStyle(tab.id.equals(window.selectedTabId) ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(Component.literal("\"" + tab.name + "\" regex=" + tab.regexList.size()
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
        ctx.getSource().sendFeedback(Component.literal("Added tab: ")
                .append(Component.literal(tabId).withStyle(ChatFormatting.YELLOW)));
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
        ctx.getSource().sendFeedback(Component.literal("Removed tab: ")
                .append(Component.literal(tabId).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int selectTab(CommandContext<FabricClientCommandSource> ctx) {
        String windowId = StringArgumentType.getString(ctx, "window");
        String tabId = StringArgumentType.getString(ctx, "tab");
        SecondaryChatManager.selectTab(windowId, tabId);
        ctx.getSource().sendFeedback(Component.literal("Selected tab: ")
                .append(Component.literal(windowId + "/" + tabId).withStyle(ChatFormatting.YELLOW)));
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
        ctx.getSource().sendFeedback(Component.literal("Tab catch-all: ")
                .append(Component.literal(tab != null && tab.catchAll ? "ON" : "OFF")
                        .withStyle(tab != null && tab.catchAll ? ChatFormatting.GREEN : ChatFormatting.RED)));
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
        ctx.getSource().sendFeedback(Component.literal("Added tab regex: ")
                .append(Component.literal(pattern).withStyle(ChatFormatting.YELLOW)));
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
        ctx.getSource().sendFeedback(Component.literal("Cleared regexes for tab: ")
                .append(Component.literal(windowId + "/" + tabId).withStyle(ChatFormatting.YELLOW)));
        return 1;
    }

    private static int showStatus(CommandContext<FabricClientCommandSource> ctx) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                SecondaryChatOverlay::defaultCanvasElement
        );
        ctx.getSource().sendFeedback(Component.literal("=== Secondary Chat Status ===").withStyle(ChatFormatting.GOLD));
        ctx.getSource().sendFeedback(Component.literal("Enabled: ")
                .append(Component.literal(String.valueOf(settings.enabled))
                        .withStyle(settings.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)));
        ctx.getSource().sendFeedback(Component.literal("Render: ")
                .append(Component.literal(settings.renderMode.name()).withStyle(ChatFormatting.YELLOW)));
        ctx.getSource().sendFeedback(Component.literal("Intercept: ")
                .append(Component.literal(settings.interceptMode.name()).withStyle(ChatFormatting.YELLOW)));
        ctx.getSource().sendFeedback(Component.literal("Windows: ")
                .append(Component.literal(String.valueOf(settings.windows.size())).withStyle(ChatFormatting.AQUA)));
        ctx.getSource().sendFeedback(Component.literal("Main position: ")
                .append(Component.literal(canvas.x + ", " + canvas.y).withStyle(ChatFormatting.GRAY)));
        ctx.getSource().sendFeedback(Component.literal("Main size: ")
                .append(Component.literal(canvas.width + "x" + canvas.height).withStyle(ChatFormatting.GRAY)));
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
        return SharedSuggestionProvider.suggest(windowIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestTabs(CommandContext<FabricClientCommandSource> ctx,
                                                              SuggestionsBuilder builder) {
        String windowId;
        try {
            windowId = StringArgumentType.getString(ctx, "window");
        } catch (IllegalArgumentException ignored) {
            windowId = "";
        }
        return SharedSuggestionProvider.suggest(tabIds(windowId), builder);
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
