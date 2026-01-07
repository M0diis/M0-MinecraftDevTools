package me.m0dii.modules;

import lombok.Getter;
import me.m0dii.utils.KeybindManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class with common module utilities.
 */
public abstract class Module {
    @Getter
    private final MinecraftClient client = MinecraftClient.getInstance();

    public ClientWorld getWorld() {
        return client.world;
    }

    @Getter
    protected final ClientPlayerEntity player = client.player;

    @Getter
    protected final String id;
    @Getter
    protected final String displayName;
    @Getter
    protected boolean enabled;

    @Getter
    protected KeyBinding keyBinding;

    protected Module(@NotNull String id, @NotNull String displayName, boolean defaultEnabled) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.enabled = defaultEnabled;
    }

    protected boolean isClientNull() {
        return getClient() == null || getClient().player == null || getClient().world == null;
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

        if (getClient().player != null) {
            getClient().player.sendMessage(Text.literal(displayName + " " + (isEnabled() ? "enabled" : "disabled")), true);
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
        this.keyBinding = kb;
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
        this.keyBinding = kb;
        return kb;
    }

    /**
     * Check if this module has configurable settings that can be shown in the ClickGUI.
     * Override this to return true if your module has settings.
     */
    public boolean hasSettings() {
        return true;
    }

    /**
     * Get a list of setting names and values for display in the ClickGUI.
     * Override this to provide your module's settings.
     * Format: ["Setting Name: value", "Another Setting: value"]
     */
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Key: " + (keyBinding != null ? keyBinding.getBoundKeyTranslationKey() : "None"));
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        return settings;
    }

    /**
     * Called when a setting is selected in the ClickGUI (Enter pressed).
     * Override this to handle setting changes.
     *
     * @param settingIndex The index of the selected setting
     */
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 1) {
            toggleEnabled();
        }
    }

}

