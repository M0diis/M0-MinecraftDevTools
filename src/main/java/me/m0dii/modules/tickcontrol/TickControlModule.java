package me.m0dii.modules.tickcontrol;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.utils.KeybindCatalog;
import me.m0dii.utils.KeybindManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.TickRateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keys and a small HUD for vanilla's {@code /tick} command: freeze/unfreeze, step N ticks, slow down and
 * reset the tick rate. Needs permission to run {@code /tick} (singleplayer with cheats, or op).
 * The Redstone Timeline uses it for its freeze/step buttons and "freeze on first change".
 */
public class TickControlModule extends Module implements PersistentSettings {
    public static final TickControlModule INSTANCE = new TickControlModule();
    private static final int[] STEPS = {1, 2, 5, 10, 20};
    private static final float[] SLOW_RATES = {20.0f, 10.0f, 5.0f, 2.0f, 1.0f};

    private int stepIndex;
    private int slowIndex;
    /** Armed by the Redstone Timeline: the next recorded signal change freezes the game. */
    private boolean freezeOnChange;

    private TickControlModule() {
        super("tick_control", "Tick Controls", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(KeybindCatalog.TICK_FREEZE.translationKey(), InputConstants.Type.KEYBOARD,
                KeybindCatalog.TICK_FREEZE.defaultKey(), client -> toggleFreeze());
        KeybindManager.registerPressedKeybind(KeybindCatalog.TICK_STEP.translationKey(), InputConstants.Type.KEYBOARD,
                KeybindCatalog.TICK_STEP.defaultKey(), client -> step(stepSize()));
        KeybindManager.registerPressedKeybind(KeybindCatalog.TICK_SLOWER.translationKey(), InputConstants.Type.KEYBOARD,
                KeybindCatalog.TICK_SLOWER.defaultKey(), client -> cycleRate());

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "tick_control_hud"), this::renderHud);
    }

    public int stepSize() {
        return STEPS[Math.floorMod(stepIndex, STEPS.length)];
    }

    /** Client copy of the server's tick state, or null outside a world. */
    private static TickRateManager manager() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? null : client.level.tickRateManager();
    }

    public static boolean isFrozen() {
        TickRateManager manager = manager();
        return manager != null && manager.isFrozen();
    }

    public static float tickRate() {
        TickRateManager manager = manager();
        return manager == null ? 20.0f : manager.tickrate();
    }

    public void toggleFreeze() {
        send(isFrozen() ? "tick unfreeze" : "tick freeze");
    }

    public void freeze() {
        if (!isFrozen()) {
            send("tick freeze");
        }
    }

    public void step(int ticks) {
        if (!isFrozen()) {
            send("tick freeze");
        }
        send("tick step " + Math.max(1, ticks));
    }

    public void cycleRate() {
        slowIndex = (slowIndex + 1) % SLOW_RATES.length;
        float rate = SLOW_RATES[slowIndex];
        send(String.format(Locale.ROOT, "tick rate %s", rate == Math.floor(rate) ? Integer.toString((int) rate) : Float.toString(rate)));
    }

    public boolean isFreezeOnChangeArmed() {
        return freezeOnChange;
    }

    public void setFreezeOnChange(boolean armed) {
        freezeOnChange = armed;
    }

    /** Called by the Redstone Timeline when a watched component changes its signal. */
    public void onSignalChange() {
        if (freezeOnChange) {
            freezeOnChange = false;
            freeze();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendOverlayMessage(Component.literal("[Tick] Frozen on the first signal change. Step with the Step button or key."));
            }
        }
    }

    private static void send(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().sendCommand(command);
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.level == null) {
            return;
        }
        boolean frozen = isFrozen();
        float rate = tickRate();
        if (!frozen && Math.abs(rate - 20.0f) < 0.01f && !freezeOnChange) {
            return;
        }
        String text;
        int color;
        if (frozen) {
            text = "❄ Ticks frozen · step " + stepSize();
            color = 0xFF7FC8FF;
        } else if (freezeOnChange) {
            text = "⏸ Freezes on next redstone change";
            color = 0xFFE5C07B;
        } else {
            text = String.format(Locale.ROOT, "⏱ %.1f ticks/s", rate);
            color = 0xFFE5C07B;
        }
        if (frozen && Math.abs(rate - 20.0f) >= 0.01f) {
            text += String.format(Locale.ROOT, " · %.1f tps", rate);
        }
        Font font = client.font;
        int w = font.width(text) + 8;
        int x = (client.getWindow().getGuiScaledWidth() - w) / 2;
        int y = 4;
        context.fill(x, y, x + w, y + 13, 0xA0101318);
        context.fill(x, y + 12, x + w, y + 13, color);
        context.text(font, text, x + 4, y + 2, color, true);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("HUD: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Freeze / unfreeze now");
        settings.add("Step size: " + stepSize() + " ticks");
        settings.add("Step now");
        settings.add("Reset tick rate (20)");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> toggleFreeze();
            case 2 -> stepIndex = (stepIndex + 1) % STEPS.length;
            case 3 -> step(stepSize());
            case 4 -> {
                slowIndex = 0;
                send("tick rate 20");
            }
            default -> {
                // nothing
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("stepIndex", stepIndex);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        stepIndex = PersistentSettings.readInt(settings, "stepIndex", 0, 0, STEPS.length - 1);
    }
}
