package me.m0dii.modules.uiutilities;

import lombok.Getter;
import lombok.Setter;
import me.m0dii.M0DevTools;
import me.m0dii.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class UiUtilitiesModule extends Module {

    public static final UiUtilitiesModule INSTANCE = new UiUtilitiesModule();

    @Getter
    private boolean sendUiPackets = true;
    @Getter
    @Setter
    private boolean shouldEditSign = true;
    @Getter
    private boolean delayUiPackets = false;
    @Getter
    public List<Packet<?>> delayedUiPackets = new ArrayList<>();

    @Getter
    public Screen storedScreen = null;
    @Getter
    public ScreenHandler storedScreenHandler = null;

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
    public void createUtilityButtons(MinecraftClient mc, Screen screen) {
        if (!isEnabled()) {
            return;
        }

        yPosition = 5;

        if (!expanded) {
            screen.addDrawableChild(ButtonWidget.builder(Text.of("Expand ▼"), button -> {
                expanded = true;
                mc.setScreen(screen);
            }).width(115).position(5, yPosition).build());
        } else {
            screen.addDrawableChild(ButtonWidget.builder(Text.of("Collapse ▲"), button -> {
                expanded = false;
                mc.setScreen(screen);
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Close without packet"), button -> {
                mc.setScreen(null);
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("De-sync"), button -> {
                // Keeps the current gui open client-side and closed server-side
                if (mc.getNetworkHandler() != null && mc.player != null) {
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                } else {
                    M0DevTools.LOGGER.warn("Minecraft network handler or player was null while using 'De-sync'.");
                }
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Send packets: " + boolToString(sendUiPackets)), button -> {
                sendUiPackets = !sendUiPackets;
                button.setMessage(Text.of("Send packets: " + boolToString(sendUiPackets)));
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Delay packets: " + boolToString(delayUiPackets)), button -> {
                delayUiPackets = !delayUiPackets;
                button.setMessage(Text.of("Delay packets: " + "Delay packets: " + boolToString(delayUiPackets)));
                if (!delayUiPackets && !delayedUiPackets.isEmpty() && mc.getNetworkHandler() != null) {
                    for (Packet<?> packet : delayedUiPackets) {
                        mc.getNetworkHandler().sendPacket(packet);
                    }
                    if (mc.player != null) {
                        mc.player.sendMessage(Text.of("Sent " + delayedUiPackets.size() + " packets."), false);
                    }
                    delayedUiPackets.clear();
                }
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Save GUI"), button -> {
                if (mc.player != null) {
                    storedScreen = mc.currentScreen;
                    storedScreenHandler = mc.player.currentScreenHandler;
                }
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Restore GUI"), button -> {
                if (mc.player != null) {
                    if (storedScreen != null && storedScreenHandler != null) {
                        mc.setScreen(storedScreen);
                        mc.player.currentScreenHandler = storedScreenHandler;
                    } else {
                        mc.player.sendMessage(Text.of("No stored GUI to restore."), false);
                    }
                }
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Disconnect & Send"), button -> {
                delayUiPackets = false;
                if (mc.getNetworkHandler() != null) {
                    for (Packet<?> packet : delayedUiPackets) {
                        mc.getNetworkHandler().sendPacket(packet);
                    }
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("Disconnecting (M0-Dev-Tools)"));
                } else {
                    M0DevTools.LOGGER.warn("Minecraft network handler is null while client is disconnecting.");
                }
                delayedUiPackets.clear();
            }).width(115).position(5, yPosition).build());
            yPosition += spacing;

            screen.addDrawableChild(ButtonWidget.builder(Text.of("Copy GUI Title JSON"), button -> {
                try {
                    if (mc.currentScreen != null) {
                        mc.keyboard.setClipboard(Text.Serialization.toJsonString(mc.currentScreen.getTitle(),
                                Objects.requireNonNull(MinecraftClient.getInstance().getServer()).getRegistryManager()));
                    } else {
                        M0DevTools.LOGGER.warn("Current screen was null while copying title JSON to clipboard.");
                    }
                } catch (IllegalStateException e) {
                    M0DevTools.LOGGER.error("Error while copying title JSON to clipboard", e);
                }
            }).width(115).position(5, yPosition).build());
        }

        yPosition += spacing;
    }

    public static TextFieldWidget createInputField(TextRenderer textRenderer, MinecraftClient mc) {
        TextFieldWidget inputField = new TextFieldWidget(textRenderer, 5, yPosition, 115, 20, Text.of("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    if (mc.getNetworkHandler() != null) {
                        if (this.getText().startsWith("/")) {
                            mc.getNetworkHandler().sendChatCommand(this.getText().replaceFirst(Pattern.quote("/"), ""));
                        } else {
                            mc.getNetworkHandler().sendChatMessage(this.getText());
                        }
                    } else {
                        M0DevTools.LOGGER.warn("Minecraft network handler was null while trying to send chat message.");
                    }

                    this.setText("");
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };

        inputField.setPlaceholder(Text.of("Command or message"));

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
