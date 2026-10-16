package me.m0dii.modules.messagehistory;


import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MessageHistoryScreen extends Screen implements ThemedScreen {
    private final Screen parent;
    private final List<String> messages;
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 20;
    private static final int VISIBLE_LINES = 15;

    public MessageHistoryScreen(Screen parent) {
        super(Component.literal("Message History"));
        this.parent = parent;
        this.messages = new ArrayList<>(MessageHistoryManager.getHistory());
    }

    public static Screen create(Screen parent) {
        return new MessageHistoryScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                        Component.literal("Close"),
                        button -> onClose())
                .bounds(width / 2 - 100, height - 30, 100, 20)
                .build());

        if (!messages.isEmpty()) {
            addRenderableWidget(Button.builder(
                            Component.literal("Clear History").withStyle(ChatFormatting.RED),
                            button -> {
                                MessageHistoryManager.clearHistory();
                                if (minecraft != null) {
                                    minecraft.gui.setScreen(new MessageHistoryScreen(parent));
                                }
                            })
                    .bounds(width / 2 + 5, height - 30, 100, 20)
                    .build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawContents(context, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
    }

    private void drawContents(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);

        String info = messages.isEmpty()
                ? "No messages in history yet."
                : "Click any message to copy it to clipboard";
        context.centeredText(font,
                Component.literal(info).withStyle(ChatFormatting.GRAY),
                width / 2, 30, 0xFFAAAAAA);

        if (!messages.isEmpty()) {
            // Draw commands
            int yStart = 50;
            int maxVisible = Math.min(VISIBLE_LINES, messages.size() - scrollOffset);

            for (int i = 0; i < maxVisible; i++) {
                int index = i + scrollOffset;
                if (index >= messages.size()) {
                    break;
                }

                String command = messages.get(index);
                int y = yStart + (i * LINE_HEIGHT);
                int boxX1 = 20;
                int boxX2 = width - 20;
                int boxY1 = y - 2;
                int boxY2 = y + LINE_HEIGHT - 2;

                // Check if mouse is hovering over this command
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2
                        && mouseY >= boxY1 && mouseY <= boxY2;

                if (hovering) {
                    // Highlight
                    context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
                }

                // Command text
                String displayText = (index + 1) + ". " + command;
                int textColor = hovering ? 0xFFFFFF00 : 0xFFFFFFFF;

                // Truncate text if too long
                int maxWidth = width - 50;
                if (font.width(displayText) > maxWidth) {
                    while (font.width(displayText + "...") > maxWidth && displayText.length() > 10) {
                        displayText = displayText.substring(0, displayText.length() - 1);
                    }
                    displayText += "...";
                }

                context.text(font, displayText, 25, y, textColor);

                if (hovering) {
                    String hint = "[Click to copy]";
                    int hintX = boxX2 - font.width(hint) - 5;
                    context.text(font,
                            Component.literal(hint).withStyle(ChatFormatting.YELLOW),
                            hintX, y, 0xFFFFFF00);
                }
            }

            if (messages.size() > VISIBLE_LINES) {
                int totalPages = (messages.size() + VISIBLE_LINES - 1) / VISIBLE_LINES;
                int currentPage = (scrollOffset / VISIBLE_LINES) + 1;
                String pageText = "Page " + currentPage + "/" + totalPages + " (Scroll to navigate)";
                context.centeredText(font,
                        Component.literal(pageText).withStyle(ChatFormatting.DARK_GRAY),
                        width / 2, height - 50, 0xFF888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int button = click.button();
        double mouseY = click.y();
        double mouseX = click.x();

        if (button == InputConstants.MOUSE_BUTTON_LEFT && !messages.isEmpty()) { // Left click
            int yStart = 50;
            int maxVisible = Math.min(VISIBLE_LINES, messages.size() - scrollOffset);

            for (int i = 0; i < maxVisible; i++) {
                int index = i + scrollOffset;
                if (index >= messages.size()) {
                    break;
                }

                String command = messages.get(index);
                int y = yStart + (i * LINE_HEIGHT);
                int boxX1 = 20;
                int boxX2 = width - 20;
                int boxY1 = y - 2;
                int boxY2 = y + LINE_HEIGHT - 2;

                if (mouseX >= boxX1 && mouseX <= boxX2 && mouseY >= boxY1 && mouseY <= boxY2) {
                    if (minecraft != null) {
                        minecraft.keyboardHandler.setClipboard(command);
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(
                                    Component.literal("✓ Copied: ").withStyle(ChatFormatting.GREEN)
                                            .append(Component.literal(command).withStyle(ChatFormatting.WHITE)));
                        }
                        onClose();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (messages.size() > VISIBLE_LINES) {
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(messages.size() - VISIBLE_LINES, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
