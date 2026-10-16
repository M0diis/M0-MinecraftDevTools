package me.m0dii.utils;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeybindManager {
    private KeybindManager() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(M0DevToolsClient.MOD_ID);

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "keybinds"));

    @Getter
    @RequiredArgsConstructor
    private static class HeldKeybind {
        private final Consumer<Minecraft> heldAction;
        private final Consumer<Minecraft> releasedAction;
        private boolean wasPressedLastTick = false;

        public void tick(Minecraft client, KeyMapping keyBinding) {
            boolean isPressed = keyBinding.isDown();
            if (isPressed) {
                heldAction.accept(client);
            } else if (wasPressedLastTick) {
                releasedAction.accept(client);
            }
            wasPressedLastTick = isPressed;
        }
    }

    private static final Map<KeyMapping, Consumer<Minecraft>> PRESSED_KEYBINDS = new LinkedHashMap<>();
    private static final Map<KeyMapping, HeldKeybind> HELD_KEYBINDS = new LinkedHashMap<>();

    private static boolean tickRegistered = false;

    public static KeyMapping registerPressedKeybind(String translationKey, InputConstants.Type type, int defaultKey, Consumer<Minecraft> action) {
        KeyMapping kb = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey,
                type,
                defaultKey,
                KEY_CATEGORY
        ));
        synchronized (PRESSED_KEYBINDS) {
            PRESSED_KEYBINDS.put(kb, action);
        }
        ensureTickHandlerRegistered();
        return kb;
    }

    public static KeyMapping registerHeldKeybind(String translationKey,
                                                 InputConstants.Type type,
                                                 int defaultKey,
                                                 Consumer<Minecraft> heldAction,
                                                 Consumer<Minecraft> releasedAction) {
        KeyMapping kb = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                translationKey,
                type,
                defaultKey,
                KEY_CATEGORY
        ));
        synchronized (PRESSED_KEYBINDS) {
            HELD_KEYBINDS.put(kb, new HeldKeybind(heldAction, releasedAction));
        }
        ensureTickHandlerRegistered();
        return kb;
    }

    private static void ensureTickHandlerRegistered() {
        if (tickRegistered) {
            return;
        }

        tickRegistered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            synchronized (PRESSED_KEYBINDS) {
                for (Map.Entry<KeyMapping, Consumer<Minecraft>> entry : PRESSED_KEYBINDS.entrySet()) {
                    KeyMapping kb = entry.getKey();
                    Consumer<Minecraft> action = entry.getValue();
                    while (kb.consumeClick()) {
                        try {
                            action.accept(client);
                        } catch (Exception e) {
                            LOGGER.error("Error executing keybind action for {}", kb, e);
                        }
                    }
                }
            }

            synchronized (HELD_KEYBINDS) {
                for (Map.Entry<KeyMapping, HeldKeybind> entry : HELD_KEYBINDS.entrySet()) {
                    KeyMapping kb = entry.getKey();
                    HeldKeybind heldKeybind = entry.getValue();
                    try {
                        heldKeybind.tick(client, kb);
                    } catch (Exception e) {
                        LOGGER.error("Error executing held keybind action for {}", kb, e);
                    }
                }
            }
        });
    }
}
