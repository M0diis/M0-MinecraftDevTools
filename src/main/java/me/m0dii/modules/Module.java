package me.m0dii.modules;

import lombok.Getter;
import me.m0dii.utils.KeybindManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Base class with common module utilities.
 * <p>
 * - Provides id/displayName/enabled state and toggle helpers
 * - Provides convenient keybind registration and cleanup
 * - Provides UI helpers (BooleanSupplier + Runnable) for ModulesScreen toggle wiring
 */
public abstract class Module {
    @Getter
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Getter
    private final ClientPlayerEntity player = client.player;

    @Getter
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private boolean enabled;

    // keep keybindings registered by this module so they can be cleaned up
    private final List<KeyBinding> registeredKeybinds = new ArrayList<>();

    protected Module(String id, String displayName, boolean defaultEnabled) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.enabled = defaultEnabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggleEnabled() {
        setEnabled(!isEnabled());

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(displayName + " " + (isEnabled() ? "enabled" : "disabled")), true);
        }
    }

    /**
     * Called when the module is enabled. Override to start listeners/overlays/etc.
     */
    protected void onEnable() {
    }

    /**
     * Called when the module is disabled. Override to stop listeners/overlays/etc.
     */
    protected void onDisable() {
    }

    /**
     * Called at mod initialization so modules can register things (keybinds, screens, etc.).
     */
    public void register() {
    }

    /**
     * Register a keybind that will call the provided action (Consumer<MinecraftClient>) when pressed.
     */
    protected KeyBinding registerPressedKeybind(@NotNull String translationKey,
                                                @NotNull InputUtil.Type type,
                                                int defaultKey,
                                                @NotNull Consumer<@NotNull MinecraftClient> action) {
        KeyBinding kb = KeybindManager.registerPressedKeybind(translationKey, type, defaultKey, action);
        registeredKeybinds.add(kb);
        return kb;
    }

    /**
     * Register a held keybind that will call the provided heldAction when pressed and releasedAction when released.
     */
    protected KeyBinding registerHeldKeybind(@NotNull String translationKey,
                                             @NotNull InputUtil.Type type,
                                             int defaultKey,
                                             @NotNull Consumer<@NotNull MinecraftClient> heldAction,
                                             @NotNull Consumer<@NotNull MinecraftClient> releasedAction) {
        KeyBinding kb = KeybindManager.registerHeldKeybind(translationKey, type, defaultKey, heldAction, releasedAction);
        registeredKeybinds.add(kb);
        return kb;
    }

    /**
     * Helper to produce a BooleanSupplier for UI wiring that returns this module's enabled state.
     */
    public BooleanSupplier stateSupplier() {
        return this::isEnabled;
    }

    /**
     * Helper to produce a Runnable for UI wiring that toggles this module.
     */
    public Runnable toggleRunnable() {
        return this::toggleEnabled;
    }

    /**
     * Create a ButtonWidget that can be used in the ModulesScreen to toggle this module.
     */
    public ButtonWidget getToggleButton() {
        return ButtonWidget.builder(buttonText(displayName, stateSupplier()), btn -> {
            toggleRunnable().run();
            btn.setMessage(buttonText(displayName, stateSupplier()));
        }).width(150).build();
    }

    private static Text buttonText(String name, BooleanSupplier state) {
        return Text.literal(name + ": " + (state.getAsBoolean() ? "ON" : "OFF"));
    }
}

