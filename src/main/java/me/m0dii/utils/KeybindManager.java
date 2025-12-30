package me.m0dii.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class KeybindManager {
    private KeybindManager() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("m0-dev-tools");

    private static final String CATEGORY = "category.m0-dev-tools";

    @Getter
    @RequiredArgsConstructor
    private static class HeldKeybind {
        private final Consumer<MinecraftClient> heldAction;
        private final Consumer<MinecraftClient> releasedAction;
        private boolean wasPressedLastTick = false;

        public void tick(MinecraftClient client, KeyBinding keyBinding) {
            boolean isPressed = keyBinding.isPressed();
            if (isPressed) {
                heldAction.accept(client);
            } else if (wasPressedLastTick) {
                releasedAction.accept(client);
            }
            wasPressedLastTick = isPressed;
        }
    }

    private static final Map<KeyBinding, Consumer<MinecraftClient>> PRESSED_KEYBINDS = new LinkedHashMap<>();
    private static final Map<KeyBinding, HeldKeybind> HELD_KEYBINDS = new LinkedHashMap<>();

    private static boolean tickRegistered = false;

    public static KeyBinding registerPressedKeybind(String translationKey, InputUtil.Type type, int defaultKey, Consumer<MinecraftClient> action) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                type,
                defaultKey,
                CATEGORY
        ));
        synchronized (PRESSED_KEYBINDS) {
            PRESSED_KEYBINDS.put(kb, action);
        }
        ensureTickHandlerRegistered();
        return kb;
    }

    public static KeyBinding registerHeldKeybind(String translationKey,
                                                 InputUtil.Type type,
                                                 int defaultKey,
                                                 Consumer<MinecraftClient> heldAction,
                                                 Consumer<MinecraftClient> releasedAction) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                type,
                defaultKey,
                CATEGORY
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
                for (Map.Entry<KeyBinding, Consumer<MinecraftClient>> entry : PRESSED_KEYBINDS.entrySet()) {
                    KeyBinding kb = entry.getKey();
                    Consumer<MinecraftClient> action = entry.getValue();
                    while (kb.wasPressed()) {
                        try {
                            action.accept(client);
                        } catch (Exception e) {
                            LOGGER.error("Error executing keybind action for {}", kb.getTranslationKey(), e);
                        }
                    }
                }
            }

            synchronized (HELD_KEYBINDS) {
                for (Map.Entry<KeyBinding, HeldKeybind> entry : HELD_KEYBINDS.entrySet()) {
                    KeyBinding kb = entry.getKey();
                    HeldKeybind heldKeybind = entry.getValue();
                    try {
                        heldKeybind.tick(client, kb);
                    } catch (Exception e) {
                        LOGGER.error("Error executing held keybind action for {}", kb.getTranslationKey(), e);
                    }
                }
            }
        });
    }
}
