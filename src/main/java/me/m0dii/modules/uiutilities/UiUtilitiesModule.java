package me.m0dii.modules.uiutilities;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import lombok.Setter;
import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UiUtilitiesModule extends Module {

    public static final UiUtilitiesModule INSTANCE = new UiUtilitiesModule();

    @Setter
    @Getter
    private boolean sendUiPackets = true;
    @Getter
    @Setter
    private boolean shouldEditSign = true;
    @Setter
    @Getter
    private boolean delayUiPackets = false;
    @Getter
    public List<Packet<?>> delayedUiPackets = new ArrayList<>();

    @Getter
    public Screen storedScreen = null;
    @Getter
    public AbstractContainerMenu storedScreenHandler = null;

    @Getter
    @Setter
    private boolean bypassResourcePack = false;
    @Getter
    @Setter
    private boolean resourcePackForceDeny = false;

    @Getter
    private boolean expanded = false;

    private final int spacing = 22;
    private static int yPosition = 5;

    private UiUtilitiesModule() {
        super("ui_utilities", "UI Utilities", true);
    }

    @SuppressWarnings("java:S2696")
    public void createUtilityButtons(Minecraft mc, Screen screen) {
        if (!isEnabled()) {
            return;
        }

        yPosition = 5;

        if (!expanded) {
            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Expand ▼"), button -> {
                expanded = true;
                mc.gui.setScreen(screen);
            }).width(115).pos(5, yPosition).build());
        } else {
            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Collapse ▲"), button -> {
                expanded = false;
                mc.gui.setScreen(screen);
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Close without packet"), button -> {
                mc.gui.setScreen(null);
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("De-sync"), button -> {
                // Keeps the current gui open client-side and closed server-side
                if (mc.getConnection() != null && mc.player != null) {
                    mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                } else {
                    M0DevTools.LOGGER.warn("Minecraft network handler or player was null while using 'De-sync'.");
                }
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Send packets: " + boolToString(sendUiPackets)), button -> {
                sendUiPackets = !sendUiPackets;
                button.setMessage(Component.nullToEmpty("Send packets: " + boolToString(sendUiPackets)));
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Delay packets: " + boolToString(delayUiPackets)), button -> {
                delayUiPackets = !delayUiPackets;
                button.setMessage(Component.nullToEmpty("Delay packets: " + "Delay packets: " + boolToString(delayUiPackets)));
                if (!delayUiPackets && !delayedUiPackets.isEmpty() && mc.getConnection() != null) {
                    for (Packet<?> packet : delayedUiPackets) {
                        mc.getConnection().send(packet);
                    }
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.nullToEmpty("Sent " + delayedUiPackets.size() + " packets."));
                    }
                    delayedUiPackets.clear();
                }
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Save GUI"), button -> {
                if (mc.player != null) {
                    storedScreen = mc.gui.screen();
                    storedScreenHandler = mc.player.containerMenu;
                }
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Restore GUI"), button -> {
                if (mc.player != null) {
                    if (storedScreen != null && storedScreenHandler != null) {
                        mc.gui.setScreen(storedScreen);
                        mc.player.containerMenu = storedScreenHandler;
                    } else {
                        mc.player.sendSystemMessage(Component.nullToEmpty("No stored GUI to restore."));
                    }
                }
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Disconnect & Send"), button -> {
                delayUiPackets = false;
                if (mc.getConnection() != null) {
                    for (Packet<?> packet : delayedUiPackets) {
                        mc.getConnection().send(packet);
                    }
                    mc.getConnection().getConnection().disconnect(Component.nullToEmpty("Disconnecting (M0-Dev-Tools)"));
                } else {
                    M0DevTools.LOGGER.warn("Minecraft network handler is null while client is disconnecting.");
                }
                delayedUiPackets.clear();
            }).width(115).pos(5, yPosition).build());
            yPosition += spacing;

            screen.addRenderableWidget(Button.builder(Component.nullToEmpty("Copy GUI Title JSON"), button -> {
                try {
                    if (mc.gui.screen() != null) {
                        mc.keyboardHandler.setClipboard(mc.gui.screen().getTitle().getString());
                    } else {
                        M0DevTools.LOGGER.warn("Current screen was null while copying title JSON to clipboard.");
                    }
                } catch (IllegalStateException e) {
                    M0DevTools.LOGGER.error("Error while copying title JSON to clipboard", e);
                }
            }).width(115).pos(5, yPosition).build());
        }

        yPosition += spacing;
    }

    public static EditBox createInputField(Font textRenderer, Minecraft mc) {
        EditBox inputField = new EditBox(textRenderer, 5, yPosition, 115, 20, Component.nullToEmpty("")) {
            @Override
            public boolean keyPressed(KeyEvent input) {
                int keyCode = input.input();
                if (keyCode == InputConstants.KEY_RETURN) {
                    if (mc.getConnection() != null) {
                        if (this.getValue().startsWith("/")) {
                            mc.getConnection().sendCommand(this.getValue().replaceFirst(Pattern.quote("/"), ""));
                        } else {
                            mc.getConnection().sendChat(this.getValue());
                        }
                    } else {
                        M0DevTools.LOGGER.warn("Minecraft network handler was null while trying to send chat message.");
                    }

                    this.setValue("");
                }
                return super.keyPressed(input);
            }
        };

        inputField.setHint(Component.nullToEmpty("Command or message"));

        if (!UiUtilitiesModule.INSTANCE.isExpanded()) {
            inputField.setVisible(false);
        }

        inputField.setMaxLength(255);

        return inputField;
    }

    private String boolToString(boolean value) {
        return value ? "ON" : "OFF";
    }
}
