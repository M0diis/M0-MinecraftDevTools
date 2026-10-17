package me.m0dii;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.clickgui.ClickHudModule;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.heldlight.HeldLightModule;
import me.m0dii.modules.macros.gui.MacroWorkbenchScreen;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.overlays.RedstonePowerOverlayModule;
import me.m0dii.modules.overlays.StructureBoundingBoxOverlay;
import me.m0dii.modules.redstonetimeline.RedstoneTimelineModule;
import me.m0dii.modules.redstonetimeline.RedstoneTimelineScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.StructureTags;

/**
 * Builds small test scenes and screenshots individual features (redstone overlay, timeline, freecam, ClickHUD,
 * structure boxes, held light). Run alone with {@code M0DEV_GAMETESTS=features}.
 */
public final class FeatureVisualTest implements FabricClientGameTest {
    private static final int Y = 120;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!GameTestFilter.enabled("features")) {
            return;
        }
        System.setProperty("m0dev.debug.bypassServerSideOptIn", "true");
        checkKeyConflicts(context);
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getConnection().waitForChunksRender();
            server.runCommand("time set noon");
            server.runCommand("gamerule doDaylightCycle false");
            server.runCommand("gamemode creative @a");

            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition().atY(Y));
            buildRedstoneScene(server, origin);
            stand(context, server, origin.offset(-1, 1, 2), 180f, 48f);
            context.waitTicks(20);
            singleplayer.getConnection().waitForChunksRender();

            redstoneOverlay(context);
            redstoneTimeline(context, server, origin);
            freecam(context);
            clickHud(context);
            heldLight(context, singleplayer, server, origin);
        }
        // Structures need real terrain generation; the default test world is superflat.
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(settings -> {
                    settings.setSeed("m0dev-structures");
                    settings.setGenerateStructures(true);
                })
                .create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("gamemode spectator @a");
            singleplayer.getServer().runCommand("time set noon");
            structures(context, singleplayer, singleplayer.getServer());
        }
    }

    /** Default mod keys must not share a key with any other mapping (F3+key debug shortcuts excepted). */
    private static void checkKeyConflicts(ClientGameTestContext context) {
        String conflicts = context.computeOnClient(client -> {
            var debug = java.util.Set.of(client.options.debugKeys);
            java.util.List<String> found = new java.util.ArrayList<>();
            var all = client.options.keyMappings;
            for (var mod : all) {
                boolean ours = mod.getName().startsWith("key.m0-dev-tools") || mod.getName().startsWith("key.bcbui");
                if (!ours || mod.isUnbound()) {
                    continue;
                }
                for (var other : all) {
                    if (other != mod && !debug.contains(other) && !other.isUnbound() && mod.same(other)) {
                        found.add(mod.getName() + " = " + other.getName() + " (" + mod.saveString() + ")");
                    }
                }
            }
            return String.join(", ", found);
        });
        System.out.println("[FeatureVisualTest] key conflicts: " + (conflicts.isEmpty() ? "none" : conflicts));
        if (!conflicts.isEmpty()) {
            throw new AssertionError("Default key conflicts: " + conflicts);
        }
    }

    private static void buildRedstoneScene(TestServerContext server, BlockPos o) {
        int x = o.getX();
        int y = o.getY();
        int z = o.getZ();
        server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 8, y, z - 8, x + 8, y, z + 8));
        server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y + 1, z - 8, x + 8, y + 6, z + 8));
        int r = y + 1;
        // A powered line: lever -> wire -> repeater -> wire -> comparator -> wire -> lamp
        server.runCommand("setblock %d %d %d minecraft:lever[face=floor,powered=true]".formatted(x - 4, r, z));
        server.runCommand("setblock %d %d %d minecraft:redstone_wire".formatted(x - 3, r, z));
        server.runCommand("setblock %d %d %d minecraft:repeater[facing=west,delay=3]".formatted(x - 2, r, z));
        server.runCommand("setblock %d %d %d minecraft:redstone_wire".formatted(x - 1, r, z));
        server.runCommand("setblock %d %d %d minecraft:comparator[facing=west,mode=subtract]".formatted(x, r, z));
        server.runCommand("setblock %d %d %d minecraft:redstone_wire".formatted(x + 1, r, z));
        server.runCommand("setblock %d %d %d minecraft:redstone_lamp".formatted(x + 2, r, z));
        // Unpowered parts on a second row.
        server.runCommand("setblock %d %d %d minecraft:repeater[facing=east,delay=1]".formatted(x - 2, r, z - 2));
        server.runCommand("setblock %d %d %d minecraft:comparator[facing=east]".formatted(x, r, z - 2));
        server.runCommand("setblock %d %d %d minecraft:redstone_torch".formatted(x + 2, r, z - 2));
        server.runCommand("setblock %d %d %d minecraft:observer[facing=up]".formatted(x + 4, r, z - 2));
        server.runCommand("setblock %d %d %d minecraft:daylight_detector".formatted(x - 4, r, z - 2));
    }

    private static void stand(ClientGameTestContext context, TestServerContext server, BlockPos feet, float yaw, float pitch) {
        server.runCommand("tp @a %.1f %d %.1f %.1f %.1f".formatted(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, pitch));
        context.waitTicks(5);
    }

    private static void redstoneOverlay(ClientGameTestContext context) {
        context.runOnClient(client -> RedstonePowerOverlayModule.INSTANCE.setEnabled(true));
        context.waitTicks(15);
        context.takeScreenshot("feature-redstone-overlay");
        context.runOnClient(client -> RedstonePowerOverlayModule.INSTANCE.setEnabled(false));
    }

    /** Records a lever being flipped, then screenshots the timeline screen and the HUD canvas widget. */
    private static void redstoneTimeline(ClientGameTestContext context, TestServerContext server, BlockPos o) {
        context.runOnClient(client -> RedstoneTimelineModule.INSTANCE.start(8));
        String lever = "%d %d %d".formatted(o.getX() - 4, o.getY() + 1, o.getZ());
        for (int i = 0; i < 6; i++) {
            context.waitTicks(10);
            server.runCommand("setblock " + lever + " minecraft:lever[face=floor,powered=" + (i % 2 == 1) + "]");
        }
        context.waitTicks(10);
        context.takeScreenshot("feature-timeline-world");
        int changed = context.computeOnClient(client -> RedstoneTimelineModule.INSTANCE.timeline.activeTracks().size());
        if (changed == 0) {
            throw new AssertionError("Redstone timeline recorded no changes");
        }
        context.setScreen(() -> new RedstoneTimelineScreen(null));
        context.waitTicks(2);
        // Place a mark with a left click, then hover further right to show the measured span.
        double[] at = context.computeOnClient(client -> {
            double scale = client.getWindow().getGuiScale();
            return new double[]{(client.gui.screen().width * 0.55) * scale, 66 * scale, scale};
        });
        context.getInput().setCursorPos(at[0], at[1]);
        context.waitTick();
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        context.getInput().setCursorPos(at[0] + 90 * at[2], at[1] + 13 * at[2]);
        context.waitTicks(2);
        context.takeScreenshot("feature-timeline-screen");
        context.setScreen(() -> null);

        // Canvas widget, as previewed in the workbench.
        context.setScreen(() -> new MacroWorkbenchScreen(null, MacroWorkbenchScreen.Tab.CANVAS));
        context.waitTicks(2);
        context.runOnClient(client -> invoke(client.gui.screen(), "createCanvasElement", MacroHudDataHandler.ElementType.REDSTONE_TIMELINE));
        context.waitTicks(2);
        context.takeScreenshot("feature-timeline-canvas-editor");
        // Discard the test element instead of saving it.
        context.setScreen(() -> null);
        context.runOnClient(client -> RedstoneTimelineModule.INSTANCE.setEnabled(false));
    }

    private static void invoke(Object screen, String method, Object... args) {
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

    private static void freecam(ClientGameTestContext context) {
        context.runOnClient(client -> FreecamModule.INSTANCE.setEnabled(true));
        context.waitTicks(5);
        // Pull the camera back and up so the player is in view.
        context.runOnClient(client -> {
            CameraEntity camera = CameraEntity.getCamera();
            if (camera != null) {
                camera.setPos(camera.getX(), camera.getY() + 2, camera.getZ() + 5);
                camera.setXRot(30f);
            }
        });
        context.waitTicks(10);
        context.takeScreenshot("feature-freecam-player");
        context.runOnClient(client -> FreecamModule.INSTANCE.setEnabled(false));
        context.waitTicks(5);
    }

    private static void clickHud(ClientGameTestContext context) {
        context.runOnClient(client -> ClickHudModule.INSTANCE.getRenderer().setEnabled(true));
        context.waitTicks(3);
        context.takeScreenshot("feature-clickhud-categories");
        context.getInput().holdKeyFor(InputConstants.KEY_RIGHT, 2);
        context.waitTicks(2);
        // Held Down auto-repeats: walk far enough down the Overlays list to make it scroll.
        context.getInput().holdKeyFor(InputConstants.KEY_DOWN, 30);
        context.waitTicks(2);
        context.takeScreenshot("feature-clickhud-modules");
        context.getInput().holdKeyFor(InputConstants.KEY_RIGHT, 2);
        context.waitTicks(3);
        context.takeScreenshot("feature-clickhud-settings");
        // N opens the mouse-driven browser on the module highlighted in ClickHUD.
        int originalScale = context.computeOnClient(client -> client.options.guiScale().get());
        context.runOnClient(client -> {
            client.options.guiScale().set(1);
            client.resizeGui();
        });
        context.getInput().pressKey(InputConstants.KEY_N);
        context.waitTicks(3);
        String focused = context.computeOnClient(client -> client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName());
        context.takeScreenshot("feature-module-browser");
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            client.options.guiScale().set(originalScale);
            client.resizeGui();
        });
        if (!"ModuleBrowserScreen".equals(focused)) {
            throw new AssertionError("O did not open the module browser; screen=" + focused);
        }
        context.getInput().holdKeyFor(InputConstants.KEY_LEFT, 2);
        context.waitTick();
        context.getInput().holdKeyFor(InputConstants.KEY_LEFT, 2);
        context.waitTick();
        context.runOnClient(client -> ClickHudModule.INSTANCE.getRenderer().setEnabled(false));
    }

    /** Holds a torch inside a sealed stone room, so any light on the walls comes from the held torch. */
    private static void heldLight(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server, BlockPos o) {
        BlockPos room = o.offset(40, -30, 0);
        server.runCommand("fill %d %d %d %d %d %d minecraft:stone hollow".formatted(
                room.getX() - 7, room.getY() - 1, room.getZ() - 7, room.getX() + 7, room.getY() + 5, room.getZ() + 7));
        server.runCommand("item replace entity @a weapon.mainhand with minecraft:torch");
        stand(context, server, room, 180f, 25f);
        context.waitTicks(10);
        singleplayer.getConnection().waitForChunksRender();
        context.takeScreenshot("feature-held-light-off");
        context.runOnClient(client -> HeldLightModule.INSTANCE.setEnabled(true));
        context.waitTicks(10);
        singleplayer.getConnection().waitForChunksRender();
        context.takeScreenshot("feature-held-light");

        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            stand(context, server, room.offset(i % 2 == 0 ? 2 : -2, 0, 3 - i % 3), 180f, 25f);
        }
        int fps = context.computeOnClient(client -> client.getFps());
        System.out.println("[FeatureVisualTest] held light: 10 moves in " + (System.nanoTime() - start) / 1_000_000 + " ms, fps " + fps);
        context.runOnClient(client -> HeldLightModule.INSTANCE.setEnabled(false));
        server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
    }

    private static void structures(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
        BlockPos village = server.computeOnServer(s -> {
            var level = s.overworld();
            return level.findNearestMapStructure(StructureTags.VILLAGE, level.getRespawnData().pos(), 64, false);
        });
        if (village == null) {
            System.out.println("[FeatureVisualTest] no village nearby; skipping structure screenshot");
            return;
        }
        server.runCommand("tp @a %d 200 %d -45 55".formatted(village.getX() - 55, village.getZ() - 55));
        context.waitTicks(40);
        singleplayer.getConnection().waitForChunksRender();
        context.runOnClient(client -> StructureBoundingBoxOverlay.INSTANCE.setEnabled(true));
        context.waitTicks(50);
        int found = context.computeOnClient(client -> StructureBoundingBoxOverlay.INSTANCE.structures().size());
        System.out.println("[FeatureVisualTest] structures: " + context.computeOnClient(client ->
                StructureBoundingBoxOverlay.INSTANCE.structures().stream().map(b -> b.name() + " " + b.bounds()).toList())
                + " camera " + context.computeOnClient(client -> client.player.position()) + " village " + village);
        context.takeScreenshot("feature-structure-boxes");
        context.runOnClient(client -> StructureBoundingBoxOverlay.INSTANCE.setEnabled(false));
        if (found == 0) {
            throw new AssertionError("Structure overlay found no structures next to the village at " + village);
        }
    }
}
