package me.m0dii;

import bettercommandblockui.main.config.ConfigScreen;
import bettercommandblockui.main.ui.screen.BetterCommandBlockScreen;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.chat.SecondaryChatConfigScreen;
import me.m0dii.modules.commandhistory.CommandHistoryScreen;
import me.m0dii.modules.debugdraw.DebugDrawScreen;
import me.m0dii.modules.entityradar.EntityRadarScreen;
import me.m0dii.modules.macros.gui.*;
import me.m0dii.modules.messagehistory.MessageHistoryScreen;
import me.m0dii.modules.scripting.ScriptTypes;
import me.m0dii.modules.scripting.gui.ScriptEditorScreen;
import me.m0dii.modules.waypoints.WaypointScreen;
import me.m0dii.modules.xray.XrayConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class RuntimeSmokeTest implements FabricClientGameTest {
    private static final List<String> EXAMPLE_SCRIPTS = List.of(
            "client-effects/particle-ring",
            "inventory/give-starter-kit",
            "utility/report-player-status",
            "world/place-diamond-block-below"
    );
    private static final List<String> SCRIPT_EXTENSIONS = List.of(".groovy", ".js", ".kts");
    private static final List<String> SCRIPT_SUCCESS_PREFIXES = List.of("Spawned", "Gave", "Player ", "Placed");

    @Override
    public void runTest(ClientGameTestContext context) {
        // Force every mixin to apply, including those targeting lazily loaded classes (screens, etc.).
        context.runOnClient(client -> MixinEnvironment.getCurrentEnvironment().audit());

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.takeScreenshot("m0dev-26.3-world");

            openScreens(context, singleplayer);
            tourWorkbench(context);
            clickWorkbenchTab(context);
            convertLegacyMacroFile(context);
            renderMobAiVisualizer(context, singleplayer);
            dragCanvasElement(context);
            screenshotShapes(context);
            runExampleScripts(context);
        }
    }

    private static void openScreens(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockPos commandBlockPos = context.computeOnClient(client -> client.player.blockPosition().above(2));
        singleplayer.getServer().runCommand("setblock %d %d %d minecraft:command_block".formatted(
                commandBlockPos.getX(), commandBlockPos.getY(), commandBlockPos.getZ()));
        context.waitTicks(5);

        Map<String, Supplier<Screen>> screens = new LinkedHashMap<>();
        screens.put("bcbui-config", () -> new ConfigScreen(null, 0, 0));
        screens.put("bcbui-command-block", () -> {
            CommandBlockEntity blockEntity = (CommandBlockEntity) Minecraft.getInstance().level.getBlockEntity(commandBlockPos);
            return new BetterCommandBlockScreen(blockEntity, blockEntity.getCommandBlock());
        });
        screens.put("secondary-chat-config", () -> new SecondaryChatConfigScreen(null));
        screens.put("command-history", () -> new CommandHistoryScreen(null));
        screens.put("debug-draw", () -> DebugDrawScreen.create(null));
        screens.put("entity-radar", () -> new EntityRadarScreen(null));
        screens.put("macro-config", () -> MacroConfigScreen.create(null));
        screens.put("macro-hud-editor", () -> new MacroHudEditorScreen(null));
        screens.put("macro-panel-editor", () -> MacroPanelEditorScreen.create(null));
        screens.put("macro-panel", () -> new MacroPanelScreen(null));
        for (MacroWorkbenchScreen.Tab tab : MacroWorkbenchScreen.Tab.values()) {
            screens.put("macro-workbench-" + tab.name().toLowerCase(), () -> new MacroWorkbenchScreen(null, tab));
        }
        screens.put("message-history", () -> new MessageHistoryScreen(null));
        screens.put("script-editor", ScriptEditorScreen::new);
        screens.put("waypoints", () -> new WaypointScreen(null));
        screens.put("xray-config", () -> XrayConfigScreen.create(null));

        for (Map.Entry<String, Supplier<Screen>> entry : screens.entrySet()) {
            context.setScreen(entry.getValue());
            context.waitTicks(2);
            context.takeScreenshot("m0dev-screen-" + entry.getKey());
        }
        context.setScreen(() -> null);
    }

    /** Screenshots every workbench tab at a cramped (2) and a roomy (1) GUI scale for design review. */
    private static void tourWorkbench(ClientGameTestContext context) {
        int originalScale = context.computeOnClient(client -> client.options.guiScale().get());
        for (int scale : new int[]{2, 1}) {
            context.runOnClient(client -> {
                client.options.guiScale().set(scale);
                client.resizeGui();
            });
            for (MacroWorkbenchScreen.Tab tab : MacroWorkbenchScreen.Tab.values()) {
                context.setScreen(() -> new MacroWorkbenchScreen(null, tab));
                context.waitTicks(2);
                context.takeScreenshot("ui-s" + scale + "-" + tab.name().toLowerCase());
            }
            for (String overlay : new String[]{"canvasNavOpen", "addElementModalOpen"}) {
                context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CANVAS));
                context.waitTicks(2);
                context.runOnClient(client -> setScreenFlag(client.gui.screen(), overlay));
                context.waitTicks(2);
                context.takeScreenshot("ui-s" + scale + "-canvas-" + overlay);
            }
            context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CANVAS));
            context.waitTicks(2);
            context.runOnClient(client -> invokeScreen(client.gui.screen(), "createCanvasElement",
                    me.m0dii.modules.macros.hud.MacroHudDataHandler.ElementType.BUTTON));
            context.waitTicks(2);
            context.takeScreenshot("ui-s" + scale + "-canvas-selected");
            context.runOnClient(client -> invokeScreen(client.gui.screen(), "openAdvancedModal"));
            context.waitTicks(2);
            context.takeScreenshot("ui-s" + scale + "-canvas-advanced");
            // Discard the test element instead of saving it.
            context.setScreen(() -> null);
        }
        context.runOnClient(client -> {
            client.options.guiScale().set(originalScale);
            client.resizeGui();
        });
        context.setScreen(() -> null);
    }

    private static void invokeScreen(Object screen, String method, Object... args) {
        try {
            for (var m : MacroWorkbenchScreen.class.getDeclaredMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    m.invoke(screen, args);
                    return;
                }
            }
            throw new AssertionError("No method " + method);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot invoke " + method, e);
        }
    }

    /** A canvas element must follow a press-drag-release (left button state is tracked by the screen). */
    private static void dragCanvasElement(ClientGameTestContext context) {
        context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CANVAS));
        context.waitTicks(2);
        context.runOnClient(client -> invokeScreen(client.gui.screen(), "createCanvasElement",
                me.m0dii.modules.macros.hud.MacroHudDataHandler.ElementType.BUTTON));
        context.waitTicks(2);
        String before = context.computeOnClient(RuntimeSmokeTest::selectedPosition);
        double[] center = context.computeOnClient(client -> new double[]{
                client.getWindow().getScreenWidth() / 2.0, client.getWindow().getScreenHeight() / 2.0});
        context.getInput().setCursorPos(center[0], center[1]);
        context.waitTick();
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        context.getInput().moveCursor(60, 30);
        context.waitTicks(3);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        String after = context.computeOnClient(RuntimeSmokeTest::selectedPosition);
        context.setScreen(() -> null);
        if (before.equals(after)) {
            throw new AssertionError("Dragging a canvas element did not move it (" + before + ")");
        }
    }

    private static void screenshotShapes(ClientGameTestContext context) {
        for (String shape : new String[]{"rounded_rect", "circle"}) {
            context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CANVAS));
            context.waitTicks(2);
            context.runOnClient(client -> {
                invokeScreen(client.gui.screen(), "createCanvasElement", me.m0dii.modules.macros.hud.MacroHudDataHandler.ElementType.SHAPE);
                try {
                    var field = MacroWorkbenchScreen.class.getDeclaredField("selected");
                    field.setAccessible(true);
                    var e = (me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement) field.get(client.gui.screen());
                    e.shapeType = shape;
                    e.width = 80;
                    e.height = 50;
                    e.shapeRadius = 10;
                    e.shapeThickness = 2;
                    e.shapeFilled = true;
                    e.drawBorder = true;
                    e.backgroundColor = 0x804C8BF5;
                    e.borderColor = 0xFFFFFFFF;
                    field.set(client.gui.screen(), null);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError(ex);
                }
            });
            context.waitTicks(2);
            context.takeScreenshot("m0dev-shape-" + shape);
        }
        context.setScreen(() -> null);
    }

    private static String selectedPosition(net.minecraft.client.Minecraft client) {
        try {
            var field = MacroWorkbenchScreen.class.getDeclaredField("selected");
            field.setAccessible(true);
            var element = (me.m0dii.modules.macros.hud.MacroHudDataHandler.HudElement) field.get(client.gui.screen());
            return element == null ? "none" : element.x + "," + element.y;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** The visualizer draws see-through lines with a custom render type; it must render without crashing. */
    private static void renderMobAiVisualizer(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockPos pos = context.computeOnClient(client -> client.player.blockPosition().offset(3, 0, 3));
        singleplayer.getServer().runCommand("summon minecraft:zombie %d %d %d {NoAI:0b}".formatted(pos.getX(), pos.getY(), pos.getZ()));
        context.runOnClient(client -> me.m0dii.modules.mobai.MobAiVisualizerModule.INSTANCE.setEnabled(true));
        context.waitTicks(40);
        context.takeScreenshot("m0dev-mobai-visualizer");
        context.runOnClient(client -> me.m0dii.modules.mobai.MobAiVisualizerModule.INSTANCE.setEnabled(false));
    }

    private static void setScreenFlag(Object screen, String field) {
        try {
            var f = MacroWorkbenchScreen.class.getDeclaredField(field);
            f.setAccessible(true);
            f.setBoolean(screen, true);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot set " + field, e);
        }
    }

    /** A macros file written by a 1.21.x build (bare map, GLFW key codes) must load with converted keys. */
    private static void convertLegacyMacroFile(ClientGameTestContext context) {
        String result = context.computeOnClient(client -> {
            try {
                java.nio.file.Path file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macros.json");
                java.nio.file.Path backup = file.resolveSibling(file.getFileName() + ".pre-26.bak");
                java.nio.file.Files.deleteIfExists(backup);
                java.nio.file.Files.writeString(file, "{\"legacy\": {\"name\": \"Legacy\", \"commands\": [\"say hi\"], \"keyCode\": 75, \"delayTicks\": 0, \"showInOverlay\": false}}");
                me.m0dii.modules.macros.MacroDataHandler.loadMacros();
                int key = me.m0dii.modules.macros.MacroDataHandler.getMacro("legacy").keyCode;
                boolean versioned = java.nio.file.Files.readString(file).contains("\"formatVersion\"");
                me.m0dii.modules.macros.MacroDataHandler.removeMacro("legacy");
                return key + "|" + versioned + "|" + java.nio.file.Files.exists(backup);
            } catch (Exception e) {
                return "error: " + e;
            }
        });
        String expected = InputConstants.KEY_K + "|true|true";
        if (!expected.equals(result)) {
            throw new AssertionError("Legacy macro conversion failed; expected " + expected + " got " + result);
        }
    }

    private static void clickWorkbenchTab(ClientGameTestContext context) {
        // Start on the Configuration tab: its custom right-click routing used to swallow every left click
        // once 26.x renumbered mouse buttons (left = 1).
        context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CONFIGURATION));
        context.waitTicks(2);
        double[] keyboardNav = context.computeOnClient(client -> {
            double scale = client.getWindow().getGuiScale();
            for (var child : client.gui.screen().children()) {
                if (child instanceof me.m0dii.gui.theme.SidebarNavButton nav && "Keyboard".equals(nav.getMessage().getString())) {
                    return new double[]{(nav.getX() + nav.getWidth() / 2.0) * scale, (nav.getY() + nav.getHeight() / 2.0) * scale};
                }
            }
            throw new AssertionError("Keyboard nav item not found");
        });
        context.getInput().setCursorPos(keyboardNav[0], keyboardNav[1]);
        context.waitTick();
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        String tab = context.computeOnClient(client -> {
            try {
                var field = MacroWorkbenchScreen.class.getDeclaredField("tab");
                field.setAccessible(true);
                return String.valueOf(field.get(client.gui.screen()));
            } catch (ReflectiveOperationException e) {
                return "error: " + e;
            }
        });
        context.takeScreenshot("m0dev-click-workbench-tab");
        context.setScreen(() -> null);
        if (!"KEYBOARD".equals(tab)) {
            throw new AssertionError("Clicking the Keyboard nav item did not switch tabs; tab=" + tab);
        }
    }

    private static void runExampleScripts(ClientGameTestContext context) {
        List<String> failures = new ArrayList<>();
        for (String script : EXAMPLE_SCRIPTS) {
            for (String extension : SCRIPT_EXTENSIONS) {
                String name = script + extension;
                String source = readResource("example_scripts/" + name);
                String result = context.computeOnClient(client ->
                        String.valueOf(ScriptTypes.managerFor(name).runScript(source, ScriptTypes.defaultContext())));
                boolean ok = SCRIPT_SUCCESS_PREFIXES.stream().anyMatch(result::startsWith);
                System.out.println("[RuntimeSmokeTest] " + name + " -> " + result);
                if (!ok) {
                    failures.add(name + ": " + result);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Example scripts failed:\n" + String.join("\n", failures));
        }
    }

    private static String readResource(String path) {
        try (InputStream in = RuntimeSmokeTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new AssertionError("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + path, e);
        }
    }
}
