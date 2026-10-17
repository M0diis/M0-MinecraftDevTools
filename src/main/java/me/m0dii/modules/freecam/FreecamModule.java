package me.m0dii.modules.freecam;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.macros.MacroPlaceholderProvider;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import me.m0dii.utils.ConfigFileSafety;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FreecamModule extends Module implements PersistentSettings {

    public static final FreecamModule INSTANCE = new FreecamModule();

    private static final MacroPlaceholderProvider PLACEHOLDER_PROVIDER = new MacroPlaceholderProvider() {
        @Override
        public String getProviderId() {
            return "freecam";
        }

        @Override
        public List<String> getPlaceholderDocs() {
            return List.of(
                    "[Module placeholders: Freecam]",
                    "{freecam.enabled} => true when freecam is active"
            );
        }

        @Override
        public List<String> getKnownPlaceholderTokens() {
            return List.of("freecam.enabled");
        }

        @Override
        public String resolvePlaceholder(String token, Minecraft client, Player player, boolean canvasMode) {
            if ("freecam.enabled".equals(token)) {
                return Boolean.toString(INSTANCE.isEnabled());
            }
            return null;
        }
    };

    private static final double[] SPEEDS = {0.25, 0.5, 1.0, 2.0, 4.0, 8.0};

    private boolean showPlayerModel = true;
    private double speed = 1.0;
    private boolean disableOnDamage = true;
    /** While on, movement keys and the mouse control your player again; the camera stays where it is. */
    private boolean controllingPlayer;
    private float lastHealth = -1;
    private int lastSpot;
    private FreecamSpots spots;

    protected FreecamModule() {
        super("freecam", "Freecam", false);
    }

    @Override
    public void register() {
        MacroPlaceholders.registerProvider(PLACEHOLDER_PROVIDER);
        registerPressedKeybind(KeybindCatalog.FREECAM_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FREECAM_TOGGLE.defaultKey(),
                client -> toggleEnabled());

        registerPressedKeybind(KeybindCatalog.FREECAM_CONTROL_PLAYER.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FREECAM_CONTROL_PLAYER.defaultKey(),
                client -> notify(client, toggleControlPlayer()));
        registerPressedKeybind(KeybindCatalog.FREECAM_SAVE_SPOT.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FREECAM_SAVE_SPOT.defaultKey(),
                client -> notify(client, saveSpot(0)));
        registerPressedKeybind(KeybindCatalog.FREECAM_NEXT_SPOT.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FREECAM_NEXT_SPOT.defaultKey(),
                client -> notify(client, goToSpot(spots().next(me.m0dii.modules.waypoints.WaypointHandler.currentWorldKey(), lastSpot))));
        registerCommands();

        // Freecam holds a camera entity bound to one world; drop it when that world goes away.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> forceDisable());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isEnabled() && CameraEntity.isStale()) {
                forceDisable();
                return;
            }
            checkDamage(client);
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("freecam")
                        .executes(ctx -> {
                            toggleEnabled();
                            return 1;
                        })
                        .then(ClientCommands.literal("control").executes(ctx -> reply(ctx.getSource(), toggleControlPlayer())))
                        .then(ClientCommands.literal("list").executes(ctx -> reply(ctx.getSource(), listSpots())))
                        .then(ClientCommands.literal("save")
                                .executes(ctx -> reply(ctx.getSource(), saveSpot(0)))
                                .then(ClientCommands.argument("slot", IntegerArgumentType.integer(1, FreecamSpots.SLOTS))
                                        .executes(ctx -> reply(ctx.getSource(), saveSpot(IntegerArgumentType.getInteger(ctx, "slot"))))))
                        .then(ClientCommands.literal("go")
                                .then(ClientCommands.argument("slot", IntegerArgumentType.integer(1, FreecamSpots.SLOTS))
                                        .executes(ctx -> reply(ctx.getSource(), goToSpot(IntegerArgumentType.getInteger(ctx, "slot"))))))
                        .then(ClientCommands.literal("clear")
                                .then(ClientCommands.argument("slot", IntegerArgumentType.integer(1, FreecamSpots.SLOTS))
                                        .executes(ctx -> reply(ctx.getSource(), clearSpot(IntegerArgumentType.getInteger(ctx, "slot"))))))
                        .then(ClientCommands.literal("speed")
                                .executes(ctx -> reply(ctx.getSource(), "Speed " + formatSpeed(speed) + "."))
                                .then(ClientCommands.argument("multiplier", DoubleArgumentType.doubleArg(0.05, 20))
                                        .executes(ctx -> {
                                            speed = DoubleArgumentType.getDouble(ctx, "multiplier");
                                            me.m0dii.modules.state.ModuleStateStore.onSettingsChanged(this);
                                            return reply(ctx.getSource(), "Speed set to " + formatSpeed(speed) + ".");
                                        })))));
    }

    private static int reply(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, String text) {
        source.sendFeedback(Component.literal("[Freecam] " + text));
        return 1;
    }

    private static void notify(Minecraft client, String text) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal("[Freecam] " + text));
        }
    }

    // ---- control player ----------------------------------------------------------------------------------

    public boolean isControllingPlayer() {
        return isEnabled() && controllingPlayer;
    }

    private String toggleControlPlayer() {
        if (!isEnabled()) {
            return "Freecam is off.";
        }
        controllingPlayer = !controllingPlayer;
        return controllingPlayer ? "Controlling your player; the camera stays put." : "Controlling the camera again.";
    }

    private void checkDamage(Minecraft client) {
        if (!isEnabled() || client.player == null) {
            lastHealth = -1;
            return;
        }
        float health = client.player.getHealth();
        if (disableOnDamage && lastHealth >= 0 && health < lastHealth) {
            forceDisable();
            notify(client, "Turned off because you took damage.");
            lastHealth = -1;
            return;
        }
        lastHealth = health;
    }

    // ---- saved spots -------------------------------------------------------------------------------------

    private static Path spotsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-freecam-spots.json");
    }

    private FreecamSpots spots() {
        if (spots == null) {
            spots = new FreecamSpots();
            Path file = spotsFile();
            if (Files.exists(file)) {
                try {
                    spots = FreecamSpots.fromJson(Files.readString(file));
                } catch (IOException | RuntimeException e) {
                    ConfigFileSafety.backupUnreadable(file, e);
                }
            }
        }
        return spots;
    }

    private void saveSpots() {
        try {
            ConfigFileSafety.writeAtomically(spotsFile(), spots().toJson());
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save freecam spots: {}", e.getMessage());
        }
    }

    /** Saves the camera (or, with freecam off, your view) into {@code slot}; 0 = first free slot. */
    private String saveSpot(int slot) {
        Minecraft client = getClient();
        if (client.level == null || client.player == null) {
            return "Join a world first.";
        }
        String world = me.m0dii.modules.waypoints.WaypointHandler.currentWorldKey();
        int target = slot > 0 ? slot : spots().firstFree(world);
        if (target < 0) {
            return "All " + FreecamSpots.SLOTS + " spots are used. Use /freecam save <1-9> to overwrite one.";
        }
        var source = isEnabled() && CameraEntity.getCamera() != null ? CameraEntity.getCamera() : client.player;
        Vec3 pos = source == client.player ? client.player.getEyePosition() : source.position();
        FreecamSpots.Spot spot = new FreecamSpots.Spot(client.level.dimension().identifier().toString(),
                pos.x, pos.y, pos.z, source.getYRot(), source.getXRot());
        spots().set(world, target, spot);
        saveSpots();
        lastSpot = target;
        return "Saved spot " + target + ": " + spot.describe();
    }

    private String goToSpot(int slot) {
        Minecraft client = getClient();
        if (client.level == null || client.player == null) {
            return "Join a world first.";
        }
        if (slot < 1) {
            return "No saved spots. Save one with /freecam save or the 'save spot' key.";
        }
        FreecamSpots.Spot spot = spots().get(me.m0dii.modules.waypoints.WaypointHandler.currentWorldKey(), slot);
        if (spot == null) {
            return "Spot " + slot + " is empty.";
        }
        if (!spot.dimension().equals(client.level.dimension().identifier().toString())) {
            return "Spot " + slot + " is in " + FreecamSpots.shortDimension(spot.dimension()) + ".";
        }
        if (!isEnabled()) {
            setEnabled(true);
        }
        CameraEntity camera = CameraEntity.getCamera();
        if (!isEnabled() || camera == null) {
            return "Freecam could not be turned on here.";
        }
        controllingPlayer = false;
        camera.setRenderPoseExact(new Vec3(spot.x(), spot.y(), spot.z()), spot.yaw(), spot.pitch());
        lastSpot = slot;
        return "Spot " + slot + ": " + spot.describe();
    }

    private String clearSpot(int slot) {
        boolean removed = spots().clear(me.m0dii.modules.waypoints.WaypointHandler.currentWorldKey(), slot);
        if (removed) {
            saveSpots();
        }
        return removed ? "Cleared spot " + slot + "." : "Spot " + slot + " was already empty.";
    }

    private String listSpots() {
        String world = me.m0dii.modules.waypoints.WaypointHandler.currentWorldKey();
        List<Integer> used = spots().used(world);
        if (used.isEmpty()) {
            return "No saved spots in this world.";
        }
        StringBuilder out = new StringBuilder("Saved spots:");
        for (int slot : used) {
            out.append("\n  ").append(slot).append(": ").append(spots().get(world, slot).describe());
        }
        return out.toString();
    }

    private static String formatSpeed(double value) {
        return String.format(Locale.ROOT, "%.2fx", value).replace(".00x", "x");
    }

    public double speedMultiplier() {
        return speed;
    }

    private void forceDisable() {
        if (this.enabled) {
            this.enabled = false;
            onDisable();
        } else {
            CameraEntity.setCameraState(false);
        }
    }

    @Override
    public void onEnable() {
        if (getClient().level == null || getClient().player == null) {
            this.enabled = false;
            return;
        }
        controllingPlayer = false;
        lastHealth = getClient().player.getHealth();
        CameraEntity.setCameraState(true);
    }

    @Override
    public void onDisable() {
        controllingPlayer = false;
        CameraEntity.setCameraState(false);
    }

    public boolean shouldShowPlayerModel() {
        return showPlayerModel;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Show Player Model: " + (showPlayerModel ? "ON" : "OFF"));
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Speed: " + formatSpeed(speed));
        settings.add("Turn off on damage: " + (disableOnDamage ? "ON" : "OFF"));
        settings.add("Control player: " + (isControllingPlayer() ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> showPlayerModel = !showPlayerModel;
            case 1 -> toggleEnabled();
            case 2 -> {
                double next = SPEEDS[0];
                for (double candidate : SPEEDS) {
                    if (candidate > speed + 1e-9) {
                        next = candidate;
                        break;
                    }
                }
                speed = next;
            }
            case 3 -> disableOnDamage = !disableOnDamage;
            case 4 -> toggleControlPlayer();
            default -> {
                // nothing
            }
        }
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return RestrictedModuleOptInNetworking.FREECAM_CHANNEL_ID;
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("showPlayerModel", showPlayerModel);
        json.addProperty("speed", speed);
        json.addProperty("disableOnDamage", disableOnDamage);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        showPlayerModel = PersistentSettings.readBoolean(settings, "showPlayerModel", showPlayerModel);
        disableOnDamage = PersistentSettings.readBoolean(settings, "disableOnDamage", disableOnDamage);
        try {
            if (settings != null && settings.has("speed")) {
                speed = Math.clamp(settings.get("speed").getAsDouble(), 0.05, 20.0);
            }
        } catch (RuntimeException ignored) {
            // keep the current speed
        }
    }
}
