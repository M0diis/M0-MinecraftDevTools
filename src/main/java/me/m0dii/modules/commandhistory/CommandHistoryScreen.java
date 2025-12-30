package me.m0dii.modules.commandhistory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class CommandHistoryScreen extends Screen {
    private final Screen parent;
    private final List<String> commands;
    private int scrollOffset = 0;
    private static final int LINE_HEIGHT = 20;
    private static final int VISIBLE_LINES = 15;

    public CommandHistoryScreen(Screen parent) {
        super(Text.literal("Command History"));
        this.parent = parent;
        this.commands = new ArrayList<>(CommandHistoryManager.getHistory());
    }

    public static Screen create(Screen parent) {
        return new CommandHistoryScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Close"),
                        button -> close())
                .dimensions(width / 2 - 100, height - 30, 100, 20)
                .build());

        if (!commands.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Clear History").formatted(Formatting.RED),
                            button -> {
                                CommandHistoryManager.clearHistory();
                                if (client != null) {
                                    client.setScreen(new CommandHistoryScreen(parent));
                                }
                            })
                    .dimensions(width / 2 + 5, height - 30, 100, 20)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        drawContents(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0101010);
    }

    private void drawContents(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);

        String info = commands.isEmpty()
                ? "No commands in history yet."
                : "Click any command to copy it to clipboard";
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(info).formatted(Formatting.GRAY),
                width / 2, 30, 0xAAAAAA);

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

                // Check if mouse is hovering over this command
                boolean hovering = mouseX >= boxX1 && mouseX <= boxX2
                        && mouseY >= boxY1 && mouseY <= boxY2;

                if (hovering) {
                    // Highlight
                    context.fill(boxX1, boxY1, boxX2, boxY2, 0x80FFFFFF);
                }

                // Command text
                String displayText = (index + 1) + ". " + command;
                int textColor = hovering ? 0xFFFF00 : 0xFFFFFF;

                // Truncate text if too long
                int maxWidth = width - 50;
                if (textRenderer.getWidth(displayText) > maxWidth) {
                    while (textRenderer.getWidth(displayText + "...") > maxWidth && displayText.length() > 10) {
                        displayText = displayText.substring(0, displayText.length() - 1);
                    }
                    displayText += "...";
                }

                context.drawTextWithShadow(textRenderer, displayText, 25, y, textColor);

                if (hovering) {
                    String hint = "[Click to copy]";
                    int hintX = boxX2 - textRenderer.getWidth(hint) - 5;
                    context.drawTextWithShadow(textRenderer,
                            Text.literal(hint).formatted(Formatting.YELLOW),
                            hintX, y, 0xFFFF00);
                }
            }

            if (commands.size() > VISIBLE_LINES) {
                int totalPages = (commands.size() + VISIBLE_LINES - 1) / VISIBLE_LINES;
                int currentPage = (scrollOffset / VISIBLE_LINES) + 1;
                String pageText = "Page " + currentPage + "/" + totalPages + " (Scroll to navigate)";
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(pageText).formatted(Formatting.DARK_GRAY),
                        width / 2, height - 50, 0x888888);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !commands.isEmpty()) { // Left click
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
                    if (client != null) {
                        client.keyboard.setClipboard(command);
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("✓ Copied: ").formatted(Formatting.GREEN)
                                            .append(Text.literal(command).formatted(Formatting.WHITE)),
                                    false
                            );
                        }
                        close();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (commands.size() > VISIBLE_LINES) {
            if (verticalAmount > 0) {
                // Up
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (verticalAmount < 0) {
                // Down
                scrollOffset = Math.min(commands.size() - VISIBLE_LINES, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
