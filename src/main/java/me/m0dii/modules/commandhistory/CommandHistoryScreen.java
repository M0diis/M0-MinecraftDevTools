package me.m0dii.modules.commandhistory;



import me.m0dii.gui.theme.ThemedScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CommandHistoryScreen extends Screen implements ThemedScreen {
    private final Screen parent;
    private final List<String> commands;
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 20;
    private static final int VISIBLE_LINES = 15;

    public CommandHistoryScreen(Screen parent) {
        super(Component.literal("Command History"));
        this.parent = parent;
        this.commands = new ArrayList<>(CommandHistoryManager.getHistory());
    }

    public static Screen create(Screen parent) {
        return new CommandHistoryScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(
                        Component.literal("Close"),
                        button -> onClose())
                .bounds(width / 2 - 100, height - 30, 100, 20)
                .build());

        if (!commands.isEmpty()) {
            addRenderableWidget(Button.builder(
                            Component.literal("Clear History").withStyle(ChatFormatting.RED),
                            button -> {
                                CommandHistoryManager.clearHistory();
                                if (minecraft != null) {
                                    minecraft.gui.setScreen(new CommandHistoryScreen(parent));
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

        String info = commands.isEmpty()
                ? "No commands in history yet."
                : "Click any command to copy it to clipboard";
        context.centeredText(font,
                Component.literal(info).withStyle(ChatFormatting.GRAY),
                width / 2, 30, 0xFFAAAAAA);

        if (!commands.isEmpty()) {
            // Draw commands
            int yStart = 50;
            int maxVisible = Math.min(VISIBLE_LINES, commands.size() - scrollOffset);

            for (int i = 0; i < maxVisible; i++) {
                int index = i + scrollOffset;
                if (index >= commands.size()) {
                    break;
                }

                String command = commands.get(index);
                int y = yStart + (i * LINE_HEIGHT);
                int boxX1 = 20;
                int boxX2 = width - 20;
                int boxY1 = y - 2;
                int boxY2 = y + LINE_HEIGHT - 2;

                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2
                        && mouseY >= boxY1 && mouseY <= boxY2;

                if (hovering) {
                    context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
                }

                String displayText = (index + 1) + ". " + command;
                int textColor = hovering ? 0xFFFFFF00 : 0xFFFFFFFF;

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

            if (commands.size() > VISIBLE_LINES) {
                int totalPages = (commands.size() + VISIBLE_LINES - 1) / VISIBLE_LINES;
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

        if (button == InputConstants.MOUSE_BUTTON_LEFT && !commands.isEmpty()) { // Left click
            int yStart = 50;
            int maxVisible = Math.min(VISIBLE_LINES, commands.size() - scrollOffset);

            for (int i = 0; i < maxVisible; i++) {
                int index = i + scrollOffset;
                if (index >= commands.size()) {
                    break;
                }

                String command = commands.get(index);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (commands.size() > VISIBLE_LINES) {
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (verticalAmount < 0) {
                scrollOffset = Math.min(commands.size() - VISIBLE_LINES, scrollOffset + 1);
            }
            return true;
        }
        return false;
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
