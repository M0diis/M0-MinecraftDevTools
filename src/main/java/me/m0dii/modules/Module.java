package me.m0dii.modules;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import me.m0dii.utils.KeybindManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class with common module utilities.
 */
public abstract class Module {
    private static final String SERVER_SIDE_OPT_IN_DEBUG_BYPASS_PROPERTY = "m0dev.debug.bypassServerSideOptIn";

    @Getter
    private final Minecraft client = Minecraft.getInstance();

    public ClientLevel getWorld() {
        return client.level;
    }

    @Getter
    protected final LocalPlayer player = client.player;

    @Getter
    protected final String id;
    @Getter
    protected final String displayName;
    @Getter
    protected boolean enabled;

    @Getter
    protected KeyMapping keyBinding;

    protected Module(@NotNull String id, @NotNull String displayName, boolean defaultEnabled) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.enabled = defaultEnabled;
    }

    protected boolean isClientNull() {
        return getClient() == null || getClient().player == null || getClient().level == null;
    }

    public boolean requiresServerSideOptIn() {
        return false;
    }

    protected @Nullable Identifier getRequiredServerOptInChannel() {
        return null;
    }

    public boolean hasRequiredServerSideOptIn() {
        if (!requiresServerSideOptIn()) {
            return true;
        }

        if (isServerSideOptInDebugBypassEnabled()) {
            return true;
        }

        Minecraft mc = getClient();
        if (mc == null) {
            return false;
        }

        // Singleplayer worlds are local, so no server handshake is required.
        if (mc.isLocalServer()) {
            return true;
        }

        Identifier channel = getRequiredServerOptInChannel();
        return channel != null && ClientPlayNetworking.canSend(channel);
    }

    protected boolean isServerSideOptInDebugBypassEnabled() {
        return Boolean.getBoolean(SERVER_SIDE_OPT_IN_DEBUG_BYPASS_PROPERTY);
    }

    protected String getServerSideOptInStatus() {
        if (isServerSideOptInDebugBypassEnabled()) {
            return "DEBUG BYPASS";
        }
        return hasRequiredServerSideOptIn() ? "OK" : "MISSING";
    }

    protected void notifyMissingServerSideOptIn() {
        if (getClient().player != null) {
            getClient().player.sendOverlayMessage(
                    Component.literal(displayName + " requires server-side opt-in and is disabled on this server."));
        }
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        if (enabled && !hasRequiredServerSideOptIn()) {
            notifyMissingServerSideOptIn();
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }

        // onEnable may refuse (e.g. no world yet) and reset the flag; remember what actually happened.
        me.m0dii.modules.state.ModuleStateStore.onModuleToggled(this);
    }

    public void toggleEnabled() {
        boolean wasEnabled = isEnabled();
        setEnabled(!wasEnabled);

        if (getClient().player != null && wasEnabled != isEnabled()) {
            getClient().player.sendOverlayMessage(Component.literal(displayName + " " + (isEnabled() ? "enabled" : "disabled")));
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
    protected KeyMapping registerPressedKeybind(@NotNull String translationKey,
                                                @NotNull InputConstants.Type type,
                                                int defaultKey,
                                                @NotNull Consumer<@NotNull Minecraft> action) {
        KeyMapping kb = KeybindManager.registerPressedKeybind(translationKey, type, defaultKey, action);
        this.keyBinding = kb;
        return kb;
    }

    /**
     * Register a held keybind that will call the provided heldAction when pressed and releasedAction when released.
     */
    protected KeyMapping registerHeldKeybind(@NotNull String translationKey,
                                             @NotNull InputConstants.Type type,
                                             int defaultKey,
                                             @NotNull Consumer<@NotNull Minecraft> heldAction,
                                             @NotNull Consumer<@NotNull Minecraft> releasedAction) {
        KeyMapping kb = KeybindManager.registerHeldKeybind(translationKey, type, defaultKey, heldAction, releasedAction);
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
        settings.add("Key: " + (keyBinding != null ? keyBinding.saveString() : "None"));
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        if (requiresServerSideOptIn()) {
            settings.add("Server Opt-In: " + getServerSideOptInStatus());
        }
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
